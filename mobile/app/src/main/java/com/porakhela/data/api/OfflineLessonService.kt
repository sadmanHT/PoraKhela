package com.porakhela.data.api

import com.porakhela.data.model.LessonPack
import com.porakhela.data.model.ProgressSyncRequest
import com.porakhela.data.model.ProgressSyncResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service for offline lesson pack downloads and progress sync
 * Handles lesson pack downloads for offline use and progress synchronization
 */
interface OfflineLessonService {

    /**
     * Download lesson pack for specific grade
     * Returns complete lesson pack with questions, assets, and metadata
     */
    @GET("lessons/")
    suspend fun downloadLessonPack(
        @Query("grade") grade: String,
        @Query("subject") subject: String? = null,
        @Query("pack_size") packSize: Int = 10,
        @Query("offline") offline: Boolean = true
    ): Response<LessonPack>

    /**
     * Get available lesson packs for download
     * Returns list of available packs with metadata
     */
    @GET("lessons/packs/")
    suspend fun getAvailableLessonPacks(
        @Query("grade") grade: String
    ): Response<List<LessonPackMetadata>>

    /**
     * Download individual lesson asset (image/audio)
     * Returns raw asset data for local storage
     */
    @GET("lessons/assets/{assetId}")
    @Streaming
    suspend fun downloadAsset(
        @Path("assetId") assetId: String
    ): Response<okhttp3.ResponseBody>

    /**
     * Sync completed lesson progress to backend
     * Uploads offline progress and returns updated points/achievements
     */
    @POST("lesson/complete/")
    suspend fun syncLessonProgress(
        @Body request: ProgressSyncRequest
    ): Response<ProgressSyncResponse>

    /**
     * Batch sync multiple lesson completions
     * For efficient syncing of accumulated offline progress
     */
    @POST("lessons/sync/batch/")
    suspend fun batchSyncProgress(
        @Body requests: List<ProgressSyncRequest>
    ): Response<BatchSyncResponse>

    /**
     * Get latest user points and achievements
     * For updating local gamification data after sync
     */
    @GET("user/points/")
    suspend fun getUserPoints(
        @Header("Authorization") token: String
    ): Response<UserPointsResponse>

    /**
     * Check for lesson pack updates
     * Returns version info to determine if local packs need updating
     */
    @GET("lessons/version/")
    suspend fun checkLessonPackVersion(
        @Query("grade") grade: String,
        @Query("local_version") localVersion: String
    ): Response<VersionCheckResponse>
}

/**
 * Lesson pack metadata for download selection
 */
data class LessonPackMetadata(
    val id: String,
    val grade: String,
    val subject: String,
    val title: String,
    val description: String,
    val lessonCount: Int,
    val estimatedSize: Long, // Size in bytes
    val version: String,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val difficulty: String,
    val tags: List<String>
)

/**
 * Progress sync request for individual lesson completion
 */
data class ProgressSyncRequest(
    val childProfileId: String,
    val lessonId: String,
    val completedAt: Long,
    val timeSpent: Long,
    val score: Int,
    val maxScore: Int,
    val questionResults: List<QuestionResult>,
    val deviceId: String,
    val appVersion: String
)

/**
 * Individual question result for detailed analytics
 */
data class QuestionResult(
    val questionId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val timeSpent: Long,
    val attempts: Int
)

/**
 * Response from lesson completion sync
 */
data class ProgressSyncResponse(
    val success: Boolean,
    val message: String,
    val pointsEarned: Int,
    val totalPoints: Int,
    val newAchievements: List<AchievementData>,
    val streakCount: Int,
    val nextBadgeProgress: BadgeProgress?
)

/**
 * Achievement data from sync response
 */
data class AchievementData(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val points: Int,
    val iconUrl: String?,
    val unlockedAt: Long
)

/**
 * Badge progress information
 */
data class BadgeProgress(
    val badgeId: String,
    val badgeName: String,
    val currentProgress: Int,
    val requiredProgress: Int,
    val progressPercentage: Int,
    val iconUrl: String?
)

/**
 * Batch sync response for multiple lesson completions
 */
data class BatchSyncResponse(
    val success: Boolean,
    val syncedCount: Int,
    val failedCount: Int,
    val totalPointsEarned: Int,
    val newTotalPoints: Int,
    val newAchievements: List<AchievementData>,
    val errors: List<SyncError>?
)

/**
 * Sync error for failed items in batch sync
 */
data class SyncError(
    val lessonId: String,
    val error: String,
    val retryable: Boolean
)

/**
 * User points response for gamification updates
 */
data class UserPointsResponse(
    val totalPoints: Int,
    val dailyPoints: Int,
    val weeklyPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val level: Int,
    val badges: List<BadgeData>,
    val nextLevelProgress: LevelProgress
)

/**
 * Badge data for user profile
 */
data class BadgeData(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val earnedAt: Long?,
    val isLocked: Boolean
)

/**
 * Level progress information
 */
data class LevelProgress(
    val currentLevel: Int,
    val nextLevel: Int,
    val currentLevelPoints: Int,
    val nextLevelPoints: Int,
    val progressPercentage: Int
)

/**
 * Version check response for pack updates
 */
data class VersionCheckResponse(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val updateRequired: Boolean,
    val updateSize: Long?,
    val updateDescription: String?
)