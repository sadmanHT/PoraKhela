package com.porakhela.data.local.dao

import androidx.room.*
import com.porakhela.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for offline lessons management
 */
@Dao
interface OfflineLessonDao {
    
    @Query("SELECT * FROM offline_lessons WHERE packId = :packId ORDER BY orderIndex ASC")
    fun getLessonsByPack(packId: String): Flow<List<OfflineLesson>>
    
    @Query("SELECT * FROM offline_lessons WHERE grade = :grade AND subject = :subject")
    fun getLessonsByGradeAndSubject(grade: String, subject: String): Flow<List<OfflineLesson>>
    
    @Query("SELECT * FROM offline_lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: String): OfflineLesson?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: OfflineLesson)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<OfflineLesson>)
    
    @Update
    suspend fun updateLesson(lesson: OfflineLesson)
    
    @Query("UPDATE offline_lessons SET isCompleted = :isCompleted WHERE id = :lessonId")
    suspend fun markLessonCompleted(lessonId: String, isCompleted: Boolean)
    
    @Query("DELETE FROM offline_lessons WHERE packId = :packId")
    suspend fun deleteLessonsByPack(packId: String)
    
    @Query("SELECT COUNT(*) FROM offline_lessons WHERE packId = :packId")
    suspend fun getLessonCountByPack(packId: String): Int
    
    @Query("SELECT * FROM offline_lessons WHERE isCompleted = :completed")
    fun getCompletedLessons(completed: Boolean = true): Flow<List<OfflineLesson>>
}

/**
 * DAO for offline questions management
 */
@Dao
interface OfflineQuestionDao {
    
    @Query("SELECT * FROM offline_questions WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    fun getQuestionsByLesson(lessonId: String): Flow<List<OfflineQuestion>>
    
    @Query("SELECT * FROM offline_questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): OfflineQuestion?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: OfflineQuestion)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<OfflineQuestion>)
    
    @Query("DELETE FROM offline_questions WHERE lessonId = :lessonId")
    suspend fun deleteQuestionsByLesson(lessonId: String)
    
    @Query("SELECT COUNT(*) FROM offline_questions WHERE lessonId = :lessonId")
    suspend fun getQuestionCountByLesson(lessonId: String): Int
}

/**
 * DAO for lesson assets management
 */
@Dao
interface LessonAssetDao {
    
    @Query("SELECT * FROM lesson_assets WHERE lessonId = :lessonId")
    fun getAssetsByLesson(lessonId: String): Flow<List<LessonAsset>>
    
    @Query("SELECT * FROM lesson_assets WHERE type = :type AND lessonId = :lessonId")
    fun getAssetsByTypeAndLesson(type: AssetType, lessonId: String): Flow<List<LessonAsset>>
    
    @Query("SELECT * FROM lesson_assets WHERE id = :assetId")
    suspend fun getAssetById(assetId: String): LessonAsset?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: LessonAsset)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<LessonAsset>)
    
    @Query("DELETE FROM lesson_assets WHERE lessonId = :lessonId")
    suspend fun deleteAssetsByLesson(lessonId: String)
    
    @Query("SELECT SUM(fileSize) FROM lesson_assets WHERE lessonId = :lessonId")
    suspend fun getTotalAssetSizeByLesson(lessonId: String): Long?
}

/**
 * DAO for offline progress tracking
 */
@Dao
interface OfflineProgressDao {
    
    @Query("SELECT * FROM offline_progress WHERE childProfileId = :childProfileId ORDER BY startedAt DESC")
    fun getProgressByChild(childProfileId: String): Flow<List<OfflineProgress>>
    
    @Query("SELECT * FROM offline_progress WHERE lessonId = :lessonId AND childProfileId = :childProfileId")
    suspend fun getProgressByLessonAndChild(lessonId: String, childProfileId: String): OfflineProgress?
    
    @Query("SELECT * FROM offline_progress WHERE isSynced = :synced")
    fun getUnsyncedProgress(synced: Boolean = false): Flow<List<OfflineProgress>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: OfflineProgress)
    
    @Update
    suspend fun updateProgress(progress: OfflineProgress)
    
    @Query("UPDATE offline_progress SET isSynced = :synced, lastSyncAttempt = :syncTime WHERE id = :progressId")
    suspend fun markProgressSynced(progressId: String, synced: Boolean, syncTime: Long)
    
    @Query("UPDATE offline_progress SET syncAttempts = syncAttempts + 1, lastSyncAttempt = :attemptTime WHERE id = :progressId")
    suspend fun incrementSyncAttempts(progressId: String, attemptTime: Long)
    
    @Query("SELECT AVG(score) FROM offline_progress WHERE childProfileId = :childProfileId AND completedAt IS NOT NULL")
    suspend fun getAverageScore(childProfileId: String): Double?
    
    @Query("SELECT COUNT(*) FROM offline_progress WHERE childProfileId = :childProfileId AND completedAt IS NOT NULL")
    suspend fun getCompletedLessonCount(childProfileId: String): Int
    
    @Query("SELECT SUM(timeSpent) FROM offline_progress WHERE childProfileId = :childProfileId")
    suspend fun getTotalTimeSpent(childProfileId: String): Long?
}

/**
 * DAO for local points management
 */
@Dao
interface LocalPointsDao {
    
    @Query("SELECT * FROM local_points WHERE childProfileId = :childProfileId")
    fun getPointsByChild(childProfileId: String): Flow<LocalPoints?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: LocalPoints)
    
    @Update
    suspend fun updatePoints(points: LocalPoints)
    
    @Query("UPDATE local_points SET totalPoints = totalPoints + :points, pendingSyncPoints = pendingSyncPoints + :points, lastUpdated = :updateTime, isSynced = 0 WHERE childProfileId = :childProfileId")
    suspend fun addPoints(childProfileId: String, points: Int, updateTime: Long)
    
    @Query("UPDATE local_points SET dailyPoints = dailyPoints + :points WHERE childProfileId = :childProfileId")
    suspend fun addDailyPoints(childProfileId: String, points: Int)
    
    @Query("UPDATE local_points SET currentStreak = :streak, lastUpdated = :updateTime WHERE childProfileId = :childProfileId")
    suspend fun updateStreak(childProfileId: String, streak: Int, updateTime: Long)
    
    @Query("UPDATE local_points SET pendingSyncPoints = 0, isSynced = 1, lastUpdated = :syncTime WHERE childProfileId = :childProfileId")
    suspend fun markPointsSynced(childProfileId: String, syncTime: Long)
}

/**
 * DAO for local achievements management
 */
@Dao
interface LocalAchievementDao {
    
    @Query("SELECT * FROM local_achievements WHERE childProfileId = :childProfileId ORDER BY earnedAt DESC")
    fun getAchievementsByChild(childProfileId: String): Flow<List<LocalAchievement>>
    
    @Query("SELECT * FROM local_achievements WHERE childProfileId = :childProfileId AND isUnlocked = 1")
    fun getUnlockedAchievements(childProfileId: String): Flow<List<LocalAchievement>>
    
    @Query("SELECT * FROM local_achievements WHERE isSynced = :synced")
    fun getUnsyncedAchievements(synced: Boolean = false): Flow<List<LocalAchievement>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: LocalAchievement)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<LocalAchievement>)
    
    @Query("UPDATE local_achievements SET isUnlocked = :unlocked, earnedAt = :earnedTime WHERE id = :achievementId")
    suspend fun unlockAchievement(achievementId: String, unlocked: Boolean, earnedTime: Long)
    
    @Query("UPDATE local_achievements SET isSynced = :synced WHERE id = :achievementId")
    suspend fun markAchievementSynced(achievementId: String, synced: Boolean)
    
    @Query("SELECT SUM(points) FROM local_achievements WHERE childProfileId = :childProfileId AND isUnlocked = 1")
    suspend fun getTotalAchievementPoints(childProfileId: String): Int?
}

/**
 * DAO for download status management
 */
@Dao
interface DownloadStatusDao {
    
    @Query("SELECT * FROM download_status ORDER BY startedAt DESC")
    fun getAllDownloads(): Flow<List<DownloadStatus>>
    
    @Query("SELECT * FROM download_status WHERE status = :status")
    fun getDownloadsByStatus(status: DownloadState): Flow<List<DownloadStatus>>
    
    @Query("SELECT * FROM download_status WHERE packId = :packId")
    suspend fun getDownloadStatus(packId: String): DownloadStatus?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadStatus(status: DownloadStatus)
    
    @Update
    suspend fun updateDownloadStatus(status: DownloadStatus)
    
    @Query("UPDATE download_status SET status = :status, progress = :progress, downloadedBytes = :downloadedBytes WHERE packId = :packId")
    suspend fun updateDownloadProgress(packId: String, status: DownloadState, progress: Int, downloadedBytes: Long)
    
    @Query("UPDATE download_status SET status = :status, completedAt = :completedTime, progress = 100 WHERE packId = :packId")
    suspend fun markDownloadCompleted(packId: String, status: DownloadState, completedTime: Long)
    
    @Query("UPDATE download_status SET retryCount = retryCount + 1, errorMessage = :error WHERE packId = :packId")
    suspend fun incrementRetryCount(packId: String, error: String?)
    
    @Query("DELETE FROM download_status WHERE packId = :packId")
    suspend fun deleteDownloadStatus(packId: String)
}

/**
 * DAO for sync queue management
 */
@Dao
interface SyncQueueDao {
    
    @Query("SELECT * FROM sync_queue ORDER BY priority DESC, createdAt ASC")
    fun getAllQueueItems(): Flow<List<SyncQueueItem>>
    
    @Query("SELECT * FROM sync_queue WHERE type = :type ORDER BY priority DESC, createdAt ASC")
    fun getQueueItemsByType(type: SyncType): Flow<List<SyncQueueItem>>
    
    @Query("SELECT * FROM sync_queue WHERE attempts < 3 ORDER BY priority DESC, createdAt ASC LIMIT :limit")
    suspend fun getRetryableItems(limit: Int = 10): List<SyncQueueItem>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: SyncQueueItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItems(items: List<SyncQueueItem>)
    
    @Update
    suspend fun updateQueueItem(item: SyncQueueItem)
    
    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastAttempt = :attemptTime, errorMessage = :error WHERE id = :itemId")
    suspend fun incrementAttempts(itemId: String, attemptTime: Long, error: String?)
    
    @Query("DELETE FROM sync_queue WHERE id = :itemId")
    suspend fun deleteQueueItem(itemId: String)
    
    @Query("DELETE FROM sync_queue WHERE attempts >= 3")
    suspend fun deleteFailedItems()
    
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getQueueSize(): Int
}

/**
 * DAO for gamification events
 */
@Dao
interface GamificationEventDao {
    
    @Query("SELECT * FROM gamification_events WHERE childProfileId = :childProfileId AND isShown = 0 ORDER BY createdAt ASC")
    fun getPendingEvents(childProfileId: String): Flow<List<GamificationEvent>>
    
    @Query("SELECT * FROM gamification_events WHERE childProfileId = :childProfileId ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentEvents(childProfileId: String, limit: Int = 20): Flow<List<GamificationEvent>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: GamificationEvent)
    
    @Query("UPDATE gamification_events SET isShown = 1 WHERE id = :eventId")
    suspend fun markEventShown(eventId: String)
    
    @Query("DELETE FROM gamification_events WHERE createdAt < :cutoffTime")
    suspend fun deleteOldEvents(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM gamification_events WHERE childProfileId = :childProfileId AND isShown = 0")
    suspend fun getPendingEventCount(childProfileId: String): Int
}