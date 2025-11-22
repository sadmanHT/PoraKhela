package com.porakhela.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.porakhela.data.local.Converters

/**
 * Complete lesson pack for offline use
 * Contains all lessons, questions, and asset references for a grade/subject
 */
data class LessonPack(
    val id: String,
    val grade: String,
    val subject: String,
    val title: String,
    val description: String,
    val version: String,
    val lessons: List<OfflineLesson>,
    val totalSize: Long,
    val createdAt: Long
)

/**
 * Offline lesson with complete data for local playback
 */
@Entity(tableName = "offline_lessons")
@TypeConverters(Converters::class)
data class OfflineLesson(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val subject: String,
    val grade: String,
    val duration: Int, // in minutes
    val difficulty: String,
    val thumbnailPath: String?, // Local file path
    val videoPath: String?, // Local file path
    val audioPath: String?, // Local file path
    val content: String, // Lesson content/instructions
    val questions: List<OfflineQuestion>,
    val assets: List<LessonAsset>,
    val packId: String,
    val orderIndex: Int,
    val downloadedAt: Long,
    val isCompleted: Boolean = false,
    val localPath: String? // Root path for lesson files
)

/**
 * Offline question for local quiz functionality
 */
@Entity(tableName = "offline_questions")
@TypeConverters(Converters::class)
data class OfflineQuestion(
    @PrimaryKey
    val id: String,
    val lessonId: String,
    val questionText: String,
    val questionType: QuestionType,
    val options: List<String>,
    val correctAnswer: Int, // Index of correct option
    val explanation: String?,
    val points: Int,
    val timeLimit: Int?, // in seconds
    val orderIndex: Int,
    val imagePath: String?, // Local image path
    val audioPath: String?, // Local audio path
    val difficulty: String,
    val tags: List<String>
)

/**
 * Types of questions supported offline
 */
enum class QuestionType {
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    FILL_BLANK,
    MATCHING,
    ORDERING,
    DRAWING // For math problems
}

/**
 * Asset files associated with lessons
 */
@Entity(tableName = "lesson_assets")
data class LessonAsset(
    @PrimaryKey
    val id: String,
    val lessonId: String,
    val type: AssetType,
    val originalUrl: String,
    val localPath: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val downloadedAt: Long,
    val checksum: String? // For integrity verification
)

/**
 * Types of lesson assets
 */
enum class AssetType {
    IMAGE,
    AUDIO,
    VIDEO,
    ANIMATION,
    DOCUMENT
}

/**
 * Offline progress tracking for local gameplay
 */
@Entity(tableName = "offline_progress")
@TypeConverters(Converters::class)
data class OfflineProgress(
    @PrimaryKey
    val id: String,
    val childProfileId: String,
    val lessonId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val timeSpent: Long,
    val score: Int,
    val maxScore: Int,
    val questionsAnswered: Int,
    val totalQuestions: Int,
    val questionResults: List<OfflineQuestionResult>,
    val isSynced: Boolean = false,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Long? = null,
    val deviceId: String,
    val appVersion: String
)

/**
 * Individual question result for offline tracking
 */
data class OfflineQuestionResult(
    val questionId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val timeSpent: Long,
    val attempts: Int,
    val answeredAt: Long
)

/**
 * Local points and achievements tracking
 */
@Entity(tableName = "local_points")
data class LocalPoints(
    @PrimaryKey
    val childProfileId: String,
    val totalPoints: Int,
    val dailyPoints: Int,
    val weeklyPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val level: Int,
    val lastUpdated: Long,
    val pendingSyncPoints: Int = 0, // Points earned offline
    val isSynced: Boolean = true
)

/**
 * Local achievement tracking
 */
@Entity(tableName = "local_achievements")
@TypeConverters(Converters::class)
data class LocalAchievement(
    @PrimaryKey
    val id: String,
    val childProfileId: String,
    val type: String,
    val title: String,
    val description: String,
    val points: Int,
    val iconPath: String?,
    val earnedAt: Long,
    val isUnlocked: Boolean,
    val isSynced: Boolean = false,
    val triggerData: Map<String, Any>? = null // Additional context
)

/**
 * Lesson pack download status tracking
 */
@Entity(tableName = "download_status")
data class DownloadStatus(
    @PrimaryKey
    val packId: String,
    val status: DownloadState,
    val progress: Int, // 0-100
    val downloadedBytes: Long,
    val totalBytes: Long,
    val startedAt: Long,
    val completedAt: Long?,
    val errorMessage: String?,
    val retryCount: Int = 0
)

/**
 * Download states for lesson packs
 */
enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    COMPLETED,
    PAUSED,
    FAILED,
    CANCELLED
}

/**
 * Sync queue for offline progress
 */
@Entity(tableName = "sync_queue")
@TypeConverters(Converters::class)
data class SyncQueueItem(
    @PrimaryKey
    val id: String,
    val type: SyncType,
    val data: String, // JSON serialized data
    val priority: Int,
    val attempts: Int = 0,
    val lastAttempt: Long? = null,
    val createdAt: Long,
    val scheduledAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Types of data to sync
 */
enum class SyncType {
    LESSON_PROGRESS,
    ACHIEVEMENT,
    POINTS,
    USER_ACTIVITY
}

/**
 * Local gamification event for animations
 */
@Entity(tableName = "gamification_events")
data class GamificationEvent(
    @PrimaryKey
    val id: String,
    val childProfileId: String,
    val type: GamificationEventType,
    val title: String,
    val message: String,
    val points: Int,
    val iconPath: String?,
    val animationType: String,
    val createdAt: Long,
    val isShown: Boolean = false,
    val metadata: String? = null // JSON for additional data
)

/**
 * Types of gamification events
 */
enum class GamificationEventType {
    POINTS_EARNED,
    LEVEL_UP,
    ACHIEVEMENT_UNLOCKED,
    STREAK_MILESTONE,
    BADGE_EARNED,
    CHALLENGE_COMPLETED
}

/**
 * Local streak tracking
 */
@Entity(tableName = "streak_tracking")
data class StreakTracking(
    @PrimaryKey
    val childProfileId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActivityDate: String, // YYYY-MM-DD format
    val streakType: StreakType,
    val milestones: List<Int>, // Streak milestones achieved
    val lastMilestone: Int,
    val nextMilestone: Int
)

/**
 * Types of streaks to track
 */
enum class StreakType {
    DAILY_LESSON,
    PERFECT_SCORE,
    FAST_COMPLETION,
    SUBJECT_FOCUS
}

/**
 * Badge progress tracking
 */
@Entity(tableName = "badge_progress")
data class BadgeProgressEntity(
    @PrimaryKey
    val id: String,
    val childProfileId: String,
    val badgeType: String,
    val currentProgress: Int,
    val requiredProgress: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val iconPath: String?,
    val title: String,
    val description: String
)

/**
 * Level progression data
 */
@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey
    val childProfileId: String,
    val currentLevel: Int,
    val currentLevelPoints: Int,
    val nextLevelPoints: Int,
    val totalPoints: Int,
    val levelUpAt: Long?,
    val levelUpAnimationShown: Boolean = false
)