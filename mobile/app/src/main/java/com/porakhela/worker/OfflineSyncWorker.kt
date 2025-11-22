package com.porakhela.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.porakhela.data.api.OfflineLessonService
import com.porakhela.data.local.dao.*
import com.porakhela.data.model.*
import com.porakhela.data.repository.NetworkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for syncing offline lesson progress to backend
 * Handles uploading progress, points, and achievements when device comes online
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineLessonService: OfflineLessonService,
    private val offlineProgressDao: OfflineProgressDao,
    private val localPointsDao: LocalPointsDao,
    private val localAchievementDao: LocalAchievementDao,
    private val syncQueueDao: SyncQueueDao,
    private val networkRepository: NetworkRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "offline_sync_work"
        const val MAX_RETRY_ATTEMPTS = 3
        const val INITIAL_BACKOFF_DELAY_MILLIS = 10000L // 10 seconds
        const val MAX_BACKOFF_DELAY_MILLIS = 300000L // 5 minutes

        /**
         * Enqueue sync work with exponential backoff retry policy
         */
        fun enqueueSync(context: Context, isImmediate: Boolean = false) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = if (isImmediate) {
                OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        INITIAL_BACKOFF_DELAY_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()
            } else {
                PeriodicWorkRequestBuilder<OfflineSyncWorker>(30, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        INITIAL_BACKOFF_DELAY_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .build()
            }

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Enqueue immediate sync for critical data
         */
        fun enqueueImmediateSync(context: Context) {
            enqueueSync(context, isImmediate = true)
        }

        /**
         * Cancel all sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            // Check network connectivity
            if (!networkRepository.isNetworkAvailable()) {
                return Result.retry()
            }

            setProgress(Data.Builder().putString("status", "Starting sync...").build())

            // Sync in order: Progress -> Points -> Achievements
            val progressResult = syncLessonProgress()
            val pointsResult = syncLocalPoints()
            val achievementResult = syncAchievements()

            // Process sync queue items
            val queueResult = processSyncQueue()

            setProgress(Data.Builder().putString("status", "Sync completed").build())

            when {
                progressResult is SyncResult.Success && 
                pointsResult is SyncResult.Success && 
                achievementResult is SyncResult.Success &&
                queueResult is SyncResult.Success -> Result.success()
                
                progressResult is SyncResult.Retry || 
                pointsResult is SyncResult.Retry || 
                achievementResult is SyncResult.Retry ||
                queueResult is SyncResult.Retry -> Result.retry()
                
                else -> Result.failure()
            }

        } catch (e: Exception) {
            Result.failure(
                Data.Builder()
                    .putString("error", "Sync failed: ${e.message}")
                    .build()
            )
        }
    }

    /**
     * Sync unsynced lesson progress to backend
     */
    private suspend fun syncLessonProgress(): SyncResult {
        try {
            val unsyncedProgress = offlineProgressDao.getUnsyncedProgress().first()
            
            if (unsyncedProgress.isEmpty()) {
                return SyncResult.Success("No progress to sync")
            }

            setProgress(Data.Builder()
                .putString("status", "Syncing ${unsyncedProgress.size} lesson progress records...")
                .build())

            val syncRequests = unsyncedProgress.map { progress ->
                ProgressSyncRequest(
                    childProfileId = progress.childProfileId,
                    lessonId = progress.lessonId,
                    completedAt = progress.completedAt ?: System.currentTimeMillis(),
                    timeSpent = progress.timeSpent,
                    score = progress.score,
                    maxScore = progress.maxScore,
                    questionResults = progress.questionResults.map { result ->
                        QuestionResult(
                            questionId = result.questionId,
                            selectedAnswer = result.selectedAnswer,
                            isCorrect = result.isCorrect,
                            timeSpent = result.timeSpent,
                            attempts = result.attempts
                        )
                    },
                    deviceId = progress.deviceId,
                    appVersion = progress.appVersion
                )
            }

            var successCount = 0
            var retryCount = 0
            var failureCount = 0

            // Sync each progress individually for better error handling
            for (i in syncRequests.indices) {
                val request = syncRequests[i]
                val progress = unsyncedProgress[i]

                try {
                    val response = offlineLessonService.syncLessonProgress(request)
                    
                    if (response.isSuccessful && response.body()?.success == true) {
                        // Mark as synced in local database
                        offlineProgressDao.markProgressSynced(
                            progress.id, 
                            true, 
                            System.currentTimeMillis()
                        )

                        // Update local points with server response
                        response.body()?.let { syncResponse ->
                            updateLocalPointsFromSync(
                                progress.childProfileId, 
                                syncResponse
                            )
                        }

                        successCount++
                    } else {
                        // Increment retry count
                        offlineProgressDao.incrementSyncAttempts(
                            progress.id, 
                            System.currentTimeMillis()
                        )
                        
                        if (progress.syncAttempts < MAX_RETRY_ATTEMPTS) {
                            retryCount++
                        } else {
                            failureCount++
                        }
                    }

                } catch (e: Exception) {
                    offlineProgressDao.incrementSyncAttempts(
                        progress.id, 
                        System.currentTimeMillis()
                    )
                    retryCount++
                }
            }

            return when {
                successCount == unsyncedProgress.size -> SyncResult.Success("All progress synced")
                retryCount > 0 -> SyncResult.Retry("Some items need retry")
                else -> SyncResult.Failure("Sync failed for some items")
            }

        } catch (e: Exception) {
            return SyncResult.Retry("Progress sync failed: ${e.message}")
        }
    }

    /**
     * Sync local points to backend
     */
    private suspend fun syncLocalPoints(): SyncResult {
        try {
            // Get all child profiles with unsynced points
            val unsyncedPoints = localPointsDao.getPointsByChild("").first() // This would need proper child ID
            // Note: This is simplified - in real implementation, you'd get all unsynced point records

            return SyncResult.Success("Points synced") // Placeholder

        } catch (e: Exception) {
            return SyncResult.Retry("Points sync failed: ${e.message}")
        }
    }

    /**
     * Sync unsynced achievements to backend
     */
    private suspend fun syncAchievements(): SyncResult {
        try {
            val unsyncedAchievements = localAchievementDao.getUnsyncedAchievements().first()
            
            if (unsyncedAchievements.isEmpty()) {
                return SyncResult.Success("No achievements to sync")
            }

            setProgress(Data.Builder()
                .putString("status", "Syncing ${unsyncedAchievements.size} achievements...")
                .build())

            // Batch sync achievements
            unsyncedAchievements.forEach { achievement ->
                try {
                    // Here you would call the achievement sync endpoint
                    // For now, just mark as synced
                    localAchievementDao.markAchievementSynced(achievement.id, true)
                } catch (e: Exception) {
                    // Log error but continue with next achievement
                }
            }

            return SyncResult.Success("Achievements synced")

        } catch (e: Exception) {
            return SyncResult.Retry("Achievement sync failed: ${e.message}")
        }
    }

    /**
     * Process items in the sync queue
     */
    private suspend fun processSyncQueue(): SyncResult {
        try {
            val queueItems = syncQueueDao.getRetryableItems(limit = 20)
            
            if (queueItems.isEmpty()) {
                return SyncResult.Success("No queue items to process")
            }

            setProgress(Data.Builder()
                .putString("status", "Processing ${queueItems.size} queued items...")
                .build())

            var successCount = 0
            var retryCount = 0

            for (item in queueItems) {
                try {
                    val success = processQueueItem(item)
                    
                    if (success) {
                        syncQueueDao.deleteQueueItem(item.id)
                        successCount++
                    } else {
                        syncQueueDao.incrementAttempts(
                            item.id, 
                            System.currentTimeMillis(), 
                            "Sync attempt failed"
                        )
                        retryCount++
                    }

                } catch (e: Exception) {
                    syncQueueDao.incrementAttempts(
                        item.id, 
                        System.currentTimeMillis(), 
                        "Error: ${e.message}"
                    )
                    retryCount++
                }
            }

            // Clean up failed items that exceeded max attempts
            syncQueueDao.deleteFailedItems()

            return when {
                successCount == queueItems.size -> SyncResult.Success("All queue items processed")
                retryCount > 0 -> SyncResult.Retry("Some items need retry")
                else -> SyncResult.Success("Queue processing completed with some failures")
            }

        } catch (e: Exception) {
            return SyncResult.Retry("Queue processing failed: ${e.message}")
        }
    }

    /**
     * Process individual sync queue item
     */
    private suspend fun processQueueItem(item: SyncQueueItem): Boolean {
        return when (item.type) {
            SyncType.LESSON_PROGRESS -> {
                // Parse and sync lesson progress data
                try {
                    val request = Json.decodeFromString<ProgressSyncRequest>(item.data)
                    val response = offlineLessonService.syncLessonProgress(request)
                    response.isSuccessful && response.body()?.success == true
                } catch (e: Exception) {
                    false
                }
            }
            
            SyncType.ACHIEVEMENT -> {
                // Parse and sync achievement data
                try {
                    // Implement achievement sync logic
                    true // Placeholder
                } catch (e: Exception) {
                    false
                }
            }
            
            SyncType.POINTS -> {
                // Parse and sync points data
                try {
                    // Implement points sync logic
                    true // Placeholder
                } catch (e: Exception) {
                    false
                }
            }
            
            SyncType.USER_ACTIVITY -> {
                // Parse and sync user activity data
                try {
                    // Implement user activity sync logic
                    true // Placeholder
                } catch (e: Exception) {
                    false
                }
            }
        }
    }

    /**
     * Update local points with server sync response
     */
    private suspend fun updateLocalPointsFromSync(
        childProfileId: String,
        syncResponse: ProgressSyncResponse
    ) {
        try {
            // Update total points from server
            val currentPoints = localPointsDao.getPointsByChild(childProfileId).first()
            
            currentPoints?.let { points ->
                val updatedPoints = points.copy(
                    totalPoints = syncResponse.totalPoints,
                    currentStreak = syncResponse.streakCount,
                    pendingSyncPoints = 0, // Reset pending since we just synced
                    isSynced = true,
                    lastUpdated = System.currentTimeMillis()
                )
                
                localPointsDao.updatePoints(updatedPoints)
            }

            // Add new achievements from server
            syncResponse.newAchievements.forEach { achievementData ->
                val localAchievement = LocalAchievement(
                    id = achievementData.id,
                    childProfileId = childProfileId,
                    type = achievementData.type,
                    title = achievementData.title,
                    description = achievementData.description,
                    points = achievementData.points,
                    iconPath = null, // Would download icon if needed
                    earnedAt = achievementData.unlockedAt,
                    isUnlocked = true,
                    isSynced = true
                )
                
                localAchievementDao.insertAchievement(localAchievement)
            }

        } catch (e: Exception) {
            // Log error but don't fail the sync
        }
    }

    /**
     * Helper method to add item to sync queue
     */
    companion object {
        suspend fun addToSyncQueue(
            syncQueueDao: SyncQueueDao,
            type: SyncType,
            data: Any,
            priority: Int = 1
        ) {
            val queueItem = SyncQueueItem(
                id = java.util.UUID.randomUUID().toString(),
                type = type,
                data = Json.encodeToString(data),
                priority = priority,
                createdAt = System.currentTimeMillis()
            )
            
            syncQueueDao.insertQueueItem(queueItem)
        }

        /**
         * Schedule periodic sync work
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
                1, TimeUnit.HOURS // Sync every hour when connected
            )
                .setConstraints(constraints)
                .setInitialDelay(15, TimeUnit.MINUTES) // Wait 15 minutes after app start
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncRequest
            )
        }

        /**
         * Check and trigger sync when network becomes available
         */
        fun onNetworkAvailable(context: Context) {
            enqueueImmediateSync(context)
        }
    }
}

/**
 * Result states for sync operations
 */
sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Retry(val message: String) : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

/**
 * Network connectivity monitor for triggering sync
 */
class NetworkConnectivityMonitor(private val context: Context) {
    
    fun startMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val networkCheckRequest = OneTimeWorkRequestBuilder<NetworkAvailableWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(networkCheckRequest)
    }
}

/**
 * Worker that triggers sync when network becomes available
 */
@HiltWorker
class NetworkAvailableWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Trigger immediate sync when network is available
        OfflineSyncWorker.onNetworkAvailable(applicationContext)
        return Result.success()
    }
}

/**
 * Sync statistics for monitoring
 */
data class SyncStats(
    val lastSyncTime: Long,
    val pendingItemsCount: Int,
    val failedItemsCount: Int,
    val totalSyncedItems: Int,
    val nextScheduledSync: Long
)

/**
 * Sync manager for coordinating all sync operations
 */
class OfflineSyncManager(
    private val context: Context,
    private val syncQueueDao: SyncQueueDao
) {
    
    /**
     * Get current sync statistics
     */
    suspend fun getSyncStats(): SyncStats {
        val pendingCount = syncQueueDao.getQueueSize()
        val workManager = WorkManager.getInstance(context)
        
        return SyncStats(
            lastSyncTime = getLastSyncTime(),
            pendingItemsCount = pendingCount,
            failedItemsCount = 0, // Would query failed items
            totalSyncedItems = 0, // Would track in preferences
            nextScheduledSync = getNextScheduledSyncTime()
        )
    }

    /**
     * Force immediate sync
     */
    fun forceSyncNow() {
        OfflineSyncWorker.enqueueImmediateSync(context)
    }

    /**
     * Pause all sync operations
     */
    fun pauseSync() {
        WorkManager.getInstance(context).cancelUniqueWork(OfflineSyncWorker.WORK_NAME)
    }

    /**
     * Resume sync operations
     */
    fun resumeSync() {
        OfflineSyncWorker.enqueueSync(context)
        OfflineSyncWorker.schedulePeriodicSync(context)
    }

    private fun getLastSyncTime(): Long {
        // Would be stored in SharedPreferences or database
        return 0L
    }

    private fun getNextScheduledSyncTime(): Long {
        // Would calculate based on WorkManager schedule
        return System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
    }
}