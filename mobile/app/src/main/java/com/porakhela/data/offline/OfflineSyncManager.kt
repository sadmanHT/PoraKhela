package com.porakhela.data.offline

import android.content.Context
import androidx.room.*
import androidx.work.*
import com.porakhela.data.database.PorakhelaDatabase
import com.porakhela.data.entities.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive offline-first sync manager for Porakhela
 * Handles lesson downloads, caching, and background synchronization
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val context: Context,
    private val database: PorakhelaDatabase,
    private val workManager: WorkManager
) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Storage paths
    private val cacheDir = File(context.cacheDir, "porakhela_lessons")
    private val videoDir = File(cacheDir, "videos")
    private val audioDir = File(cacheDir, "audio")
    private val imageDir = File(cacheDir, "images")
    
    init {
        // Create cache directories
        arrayOf(cacheDir, videoDir, audioDir, imageDir).forEach { dir ->
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
        
        // Schedule periodic sync
        schedulePeriodicSync()
    }
    
    /**
     * Download a complete lesson with all assets for offline use
     */
    suspend fun downloadLesson(lessonId: String, priority: DownloadPriority = DownloadPriority.NORMAL): Flow<DownloadProgress> = flow {
        try {
            emit(DownloadProgress.Started)
            
            // Create download entity
            val downloadEntity = DownloadEntity(
                id = "lesson_$lessonId",
                lessonId = lessonId,
                contentType = "lesson",
                status = DownloadStatus.IN_PROGRESS,
                progress = 0f,
                totalSizeBytes = 0L,
                downloadedSizeBytes = 0L,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                priority = priority.value
            )
            
            database.downloadDao().insertDownload(downloadEntity)
            
            // Get lesson metadata
            val lesson = getLessonMetadata(lessonId)
            val totalAssets = lesson.assets.size
            var downloadedAssets = 0
            
            // Update total size
            val totalSize = lesson.assets.sumOf { it.sizeBytes }
            database.downloadDao().updateDownloadSize(downloadEntity.id, totalSize)
            
            var totalDownloaded = 0L
            
            // Download each asset
            for (asset in lesson.assets) {
                try {
                    emit(DownloadProgress.InProgress(
                        progress = (downloadedAssets.toFloat() / totalAssets * 100),
                        downloadedBytes = totalDownloaded,
                        totalBytes = totalSize,
                        currentAsset = asset.name
                    ))
                    
                    val assetFile = downloadAsset(asset)
                    
                    // Update progress
                    totalDownloaded += asset.sizeBytes
                    downloadedAssets++
                    
                    // Update database
                    database.downloadDao().updateDownloadProgress(
                        downloadEntity.id,
                        downloadedAssets.toFloat() / totalAssets * 100,
                        totalDownloaded
                    )
                    
                    // Cache asset info
                    val cacheEntity = CachedAssetEntity(
                        id = "${lessonId}_${asset.id}",
                        lessonId = lessonId,
                        assetId = asset.id,
                        assetType = asset.type,
                        localPath = assetFile.absolutePath,
                        originalUrl = asset.url,
                        sizeBytes = asset.sizeBytes,
                        cachedAt = System.currentTimeMillis(),
                        lastAccessedAt = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
                    )
                    
                    database.cacheDao().insertCachedAsset(cacheEntity)
                    
                } catch (e: Exception) {
                    // Handle individual asset download failure
                    emit(DownloadProgress.Error("Failed to download ${asset.name}: ${e.message}"))
                    continue
                }
            }
            
            // Mark lesson as downloaded
            database.downloadDao().updateDownloadStatus(downloadEntity.id, DownloadStatus.COMPLETED)
            
            // Update lesson entity
            database.lessonDao().markLessonAsDownloaded(lessonId, true)
            
            emit(DownloadProgress.Completed)
            
        } catch (e: Exception) {
            database.downloadDao().updateDownloadStatus("lesson_$lessonId", DownloadStatus.FAILED)
            emit(DownloadProgress.Error(e.message ?: "Download failed"))
        }
    }
    
    /**
     * Download lesson asset with progress tracking
     */
    private suspend fun downloadAsset(asset: LessonAsset): File = withContext(Dispatchers.IO) {
        val targetDir = when (asset.type) {
            "video" -> videoDir
            "audio" -> audioDir
            "image" -> imageDir
            else -> cacheDir
        }
        
        val targetFile = File(targetDir, "${asset.id}_${asset.name}")
        
        if (targetFile.exists() && targetFile.length() == asset.sizeBytes) {
            return@withContext targetFile // Already downloaded
        }
        
        val url = URL(asset.url)
        val connection = url.openConnection() as HttpURLConnection
        
        try {
            connection.connect()
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Server returned HTTP ${connection.responseCode}")
            }
            
            connection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            targetFile
            
        } finally {
            connection.disconnect()
        }
    }
    
    /**
     * Get cached lesson content for offline playback
     */
    fun getCachedLesson(lessonId: String): Flow<CachedLesson?> = flow {
        val assets = database.cacheDao().getCachedAssetsForLesson(lessonId).first()
        
        if (assets.isEmpty()) {
            emit(null)
            return@flow
        }
        
        val cachedLesson = CachedLesson(
            lessonId = lessonId,
            assets = assets.map { asset ->
                CachedAssetInfo(
                    id = asset.assetId,
                    type = asset.assetType,
                    localPath = asset.localPath,
                    isAvailable = File(asset.localPath).exists()
                )
            },
            isFullyAvailable = assets.all { File(it.localPath).exists() },
            lastAccessedAt = System.currentTimeMillis()
        )
        
        // Update last accessed time
        database.cacheDao().updateLastAccessTime(lessonId, System.currentTimeMillis())
        
        emit(cachedLesson)
    }
    
    /**
     * Pause an active download
     */
    suspend fun pauseDownload(downloadId: String) {
        database.downloadDao().updateDownloadStatus(downloadId, DownloadStatus.PAUSED)
    }
    
    /**
     * Resume a paused download
     */
    suspend fun resumeDownload(downloadId: String) {
        val download = database.downloadDao().getDownload(downloadId).first()
        download?.let {
            if (it.status == DownloadStatus.PAUSED) {
                database.downloadDao().updateDownloadStatus(downloadId, DownloadStatus.IN_PROGRESS)
                // Re-trigger download process
                downloadLesson(it.lessonId)
            }
        }
    }
    
    /**
     * Cancel and remove a download
     */
    suspend fun cancelDownload(downloadId: String) {
        val download = database.downloadDao().getDownload(downloadId).first()
        download?.let {
            // Remove cached files
            val assets = database.cacheDao().getCachedAssetsForLesson(it.lessonId).first()
            assets.forEach { asset ->
                File(asset.localPath).delete()
                database.cacheDao().deleteCachedAsset(asset.id)
            }
            
            // Remove download record
            database.downloadDao().deleteDownload(downloadId)
            
            // Update lesson status
            database.lessonDao().markLessonAsDownloaded(it.lessonId, false)
        }
    }
    
    /**
     * Get all active downloads with progress
     */
    fun getAllDownloads(): Flow<List<DownloadEntity>> {
        return database.downloadDao().getAllDownloads()
    }
    
    /**
     * Clean up expired cache files
     */
    suspend fun cleanupExpiredCache() {
        val currentTime = System.currentTimeMillis()
        val expiredAssets = database.cacheDao().getExpiredAssets(currentTime).first()
        
        expiredAssets.forEach { asset ->
            File(asset.localPath).delete()
            database.cacheDao().deleteCachedAsset(asset.id)
        }
    }
    
    /**
     * Get cache usage statistics
     */
    fun getCacheStats(): Flow<CacheStats> = flow {
        val totalFiles = cacheDir.walkTopDown().filter { it.isFile }.count()
        val totalSizeBytes = cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val availableSpaceBytes = cacheDir.freeSpace
        
        emit(CacheStats(
            totalFiles = totalFiles,
            totalSizeBytes = totalSizeBytes,
            availableSpaceBytes = availableSpaceBytes,
            cacheDirectory = cacheDir.absolutePath
        ))
    }
    
    /**
     * Sync user progress and achievements with server
     */
    suspend fun syncUserProgress(childId: String) {
        try {
            // Get unsynced session activities
            val unsyncedSessions = database.sessionActivityDao()
                .getUnsyncedSessions(childId)
                .first()
            
            // Upload to server
            unsyncedSessions.forEach { session ->
                try {
                    uploadSessionToServer(session)
                    // Mark as synced
                    database.sessionActivityDao().markAsSynced(session.id)
                } catch (e: Exception) {
                    // Log error but continue with other sessions
                    e.printStackTrace()
                }
            }
            
            // Download latest achievements and progress from server
            syncAchievementsFromServer(childId)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Schedule periodic background sync
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val syncWork = PeriodicWorkRequestBuilder<SyncWorker>(
            6, TimeUnit.HOURS // Sync every 6 hours
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            "porakhela_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }
    
    /**
     * Force immediate sync with server
     */
    suspend fun forceSync() {
        val syncWork = OneTimeWorkRequestBuilder<SyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        workManager.enqueue(syncWork)
    }
    
    // Mock functions for demonstration - replace with actual API calls
    private suspend fun getLessonMetadata(lessonId: String): LessonMetadata {
        // This would typically make an API call to get lesson metadata
        return LessonMetadata(
            id = lessonId,
            title = "Sample Lesson",
            assets = listOf(
                LessonAsset("1", "intro_video.mp4", "video", "https://example.com/video.mp4", 1024000),
                LessonAsset("2", "lesson_audio.mp3", "audio", "https://example.com/audio.mp3", 512000),
                LessonAsset("3", "diagram.png", "image", "https://example.com/image.png", 256000)
            )
        )
    }
    
    private suspend fun uploadSessionToServer(session: SessionActivityEntity) {
        // Mock upload - replace with actual API call
        delay(100)
    }
    
    private suspend fun syncAchievementsFromServer(childId: String) {
        // Mock sync - replace with actual API call
        delay(100)
    }
}

/**
 * Background worker for periodic sync
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Get current child ID from shared preferences or database
            val childId = "current_child_id" // TODO: Get actual child ID
            
            // Perform sync operations
            // Note: In a real implementation, you'd inject dependencies properly
            // syncManager.syncUserProgress(childId)
            // syncManager.cleanupExpiredCache()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Data classes for offline sync
data class DownloadProgress(
    val status: DownloadProgressStatus,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val currentAsset: String = "",
    val error: String? = null
) {
    companion object {
        val Started = DownloadProgress(DownloadProgressStatus.STARTED)
        val Completed = DownloadProgress(DownloadProgressStatus.COMPLETED, 100f)
        
        fun InProgress(
            progress: Float,
            downloadedBytes: Long,
            totalBytes: Long,
            currentAsset: String
        ) = DownloadProgress(
            status = DownloadProgressStatus.IN_PROGRESS,
            progress = progress,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            currentAsset = currentAsset
        )
        
        fun Error(message: String) = DownloadProgress(
            status = DownloadProgressStatus.ERROR,
            error = message
        )
    }
}

enum class DownloadProgressStatus {
    STARTED,
    IN_PROGRESS,
    COMPLETED,
    ERROR
}

enum class DownloadPriority(val value: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3)
}

data class LessonMetadata(
    val id: String,
    val title: String,
    val assets: List<LessonAsset>
)

data class LessonAsset(
    val id: String,
    val name: String,
    val type: String,
    val url: String,
    val sizeBytes: Long
)

data class CachedLesson(
    val lessonId: String,
    val assets: List<CachedAssetInfo>,
    val isFullyAvailable: Boolean,
    val lastAccessedAt: Long
)

data class CachedAssetInfo(
    val id: String,
    val type: String,
    val localPath: String,
    val isAvailable: Boolean
)

data class CacheStats(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long,
    val cacheDirectory: String
)

// Additional entities for the database
@Entity(
    tableName = "cached_assets",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId"), Index("assetId")]
)
data class CachedAssetEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val assetId: String,
    val assetType: String,
    val localPath: String,
    val originalUrl: String,
    val sizeBytes: Long,
    val cachedAt: Long,
    val lastAccessedAt: Long,
    val expiresAt: Long
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_assets WHERE lessonId = :lessonId")
    fun getCachedAssetsForLesson(lessonId: String): Flow<List<CachedAssetEntity>>
    
    @Query("SELECT * FROM cached_assets WHERE expiresAt < :currentTime")
    fun getExpiredAssets(currentTime: Long): Flow<List<CachedAssetEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAsset(asset: CachedAssetEntity)
    
    @Query("UPDATE cached_assets SET lastAccessedAt = :accessTime WHERE lessonId = :lessonId")
    suspend fun updateLastAccessTime(lessonId: String, accessTime: Long)
    
    @Query("DELETE FROM cached_assets WHERE id = :assetId")
    suspend fun deleteCachedAsset(assetId: String)
    
    @Query("DELETE FROM cached_assets WHERE lessonId = :lessonId")
    suspend fun deleteAssetsForLesson(lessonId: String)
}