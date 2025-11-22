package com.porakhela.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Room Entity for User data
 * Offline-first storage for user information
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val firstName: String,
    val lastName: String,
    val userType: String, // "parent" or "child"
    val isPhoneVerified: Boolean = false,
    val banglalinkMsisdn: String? = null,
    val avatarUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val fullName: String
        get() = "$firstName $lastName"
}

/**
 * Child Profile Entity
 */
@Entity(
    tableName = "child_profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("parentId")]
)
data class ChildProfileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val parentId: String,
    val grade: Int,
    val dateOfBirth: Long, // timestamp
    val avatarUrl: String? = null,
    val dailyScreenTimeLimit: Int = 60, // minutes
    val allowedSubjects: String = "", // JSON string of subject IDs
    val totalLessonsCompleted: Int = 0,
    val totalPorapointsEarned: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActivityDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Subject Entity
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val nameBn: String,
    val description: String,
    val iconUrl: String? = null,
    val colorCode: String = "#2196F3",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Chapter Entity
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index(value = ["subjectId", "grade", "chapterNumber"], unique = true)]
)
data class ChapterEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val grade: Int,
    val chapterNumber: Int,
    val title: String,
    val titleBn: String,
    val description: String,
    val learningObjectives: String = "", // JSON string
    val estimatedDurationMinutes: Int = 30,
    val order: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Lesson Entity
 */
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId")]
)
data class LessonEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val chapterId: String,
    val title: String,
    val titleBn: String,
    val description: String,
    val lessonType: String, // "video", "interactive", "quiz", "reading", "exercise"
    val difficulty: String = "medium", // "easy", "medium", "hard"
    val contentData: String = "", // JSON string
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val audioUrl: String? = null,
    val durationMinutes: Int = 15,
    val porapointsReward: Int = 10,
    val order: Int = 0,
    val isFree: Boolean = true,
    val isActive: Boolean = true,
    val isDownloadable: Boolean = true,
    val downloadSizeMb: Float = 0f,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Lesson Progress Entity
 */
@Entity(
    tableName = "lesson_progress",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("childId"), Index("lessonId"), Index(value = ["childId", "lessonId"], unique = true)]
)
data class LessonProgressEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val childId: String,
    val lessonId: String,
    val status: String = "not_started", // "not_started", "in_progress", "completed", "reviewed"
    val completionPercentage: Int = 0,
    val timeSpentMinutes: Int = 0,
    val quizScore: Float? = null,
    val attempts: Int = 0,
    val porapointsEarned: Int = 0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Quiz Question Entity
 */
@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lessonId")]
)
data class QuizQuestionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val lessonId: String,
    val questionText: String,
    val questionTextBn: String,
    val questionType: String, // "mcq", "true_false", "fill_blank", "drag_drop", "matching"
    val options: String = "", // JSON string for options
    val correctAnswer: String = "", // JSON string for correct answer
    val explanation: String = "",
    val explanationBn: String = "",
    val points: Int = 1,
    val order: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Child Avatar Entity - Pre-defined avatars for children to choose from
 */
@Entity(tableName = "child_avatars")
data class ChildAvatarEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameBn: String,
    val imageResourceId: String, // Local drawable resource name
    val category: String = "animal", // "animal", "robot", "cartoon", "superhero"
    val colorScheme: String = "#2196F3", // Primary color for UI theming
    val isUnlocked: Boolean = true,
    val unlockRequirement: String? = null, // JSON string for unlock conditions
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Download Entity - Track offline content downloads
 */
@Entity(
    tableName = "downloads",
    indices = [Index("contentId"), Index("contentType")]
)
data class DownloadEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val contentId: String, // lesson ID, video ID, etc.
    val contentType: String, // "lesson", "video", "audio", "image", "exercise"
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val downloadStatus: String = "pending", // "pending", "downloading", "completed", "failed", "paused"
    val downloadProgress: Int = 0, // 0-100
    val downloadUrl: String,
    val checksum: String? = null,
    val downloadSpeed: Long = 0, // bytes per second
    val priority: Int = 1, // 1=high, 2=medium, 3=low
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val downloadStartedAt: Long? = null,
    val downloadCompletedAt: Long? = null,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * App Settings Entity - Store user preferences and app configuration
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
    val dataType: String = "string", // "string", "int", "boolean", "float", "json"
    val userId: String? = null, // Null for global settings
    val isUserSpecific: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Sync Status Entity - Track what data needs to be synced when online
 */
@Entity(tableName = "sync_status")
data class SyncStatusEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: String, // "lesson_progress", "quiz_answer", "achievement", "points"
    val entityId: String,
    val action: String = "upsert", // "create", "update", "delete", "upsert"
    val localData: String, // JSON string of data to sync
    val syncStatus: String = "pending", // "pending", "syncing", "synced", "failed"
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val lastError: String? = null,
    val priority: Int = 2, // 1=high, 2=medium, 3=low
    val scheduledFor: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Session Activity Entity - Track user sessions for analytics and screen time
 */
@Entity(
    tableName = "session_activity",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("sessionDate")]
)
data class SessionActivityEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val sessionDate: String, // YYYY-MM-DD format
    val sessionStartTime: Long,
    val sessionEndTime: Long? = null,
    val screenTimeMinutes: Int = 0,
    val lessonsViewed: Int = 0,
    val quizzesAttempted: Int = 0,
    val porapointsEarned: Int = 0,
    val subjectsAccessed: String = "", // JSON array of subject IDs
    val deviceInfo: String = "", // JSON string with device details
    val appVersion: String = "",
    val isCompleted: Boolean = false, // true when session properly ended
    val syncStatus: String = "pending", // "pending", "synced"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Achievement Entity - User achievements and badges
 */
@Entity(
    tableName = "achievements",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("achievementType")]
)
data class AchievementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val achievementType: String, // "streak", "subject_master", "quiz_champion", "early_bird", etc.
    val title: String,
    val titleBn: String,
    val description: String,
    val descriptionBn: String,
    val iconUrl: String? = null,
    val badgeColor: String = "#FFD700",
    val porapointsReward: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0, // 0-100 percentage
    val maxProgress: Int = 100,
    val metadata: String = "", // JSON string for additional data
    val rarity: String = "common", // "common", "rare", "epic", "legendary"
    val category: String = "learning", // "learning", "social", "streak", "special"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// ========================================
// DAO Interfaces
// ========================================

/**
 * User DAO - Database access for users
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

/**
 * Child Profile DAO
 */
@Dao
interface ChildProfileDao {
    @Query("SELECT * FROM child_profiles WHERE parentId = :parentId")
    fun getChildrenByParent(parentId: String): Flow<List<ChildProfileEntity>>
    
    @Query("SELECT * FROM child_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getChildByUserId(userId: String): ChildProfileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildProfile(childProfile: ChildProfileEntity): Long
    
    @Update
    suspend fun updateChildProfile(childProfile: ChildProfileEntity)
    
    @Delete
    suspend fun deleteChildProfile(childProfile: ChildProfileEntity)
}

/**
 * Subject DAO
 */
@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE isActive = 1 ORDER BY createdAt ASC")
    fun getAllActiveSubjects(): Flow<List<SubjectEntity>>
    
    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubjects(subjects: List<SubjectEntity>)
    
    @Update
    suspend fun updateSubject(subject: SubjectEntity)
    
    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}

/**
 * Chapter DAO
 */
@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND grade = :grade AND isActive = 1 ORDER BY chapterNumber ASC")
    fun getChaptersBySubjectAndGrade(subjectId: String, grade: Int): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE id = :id LIMIT 1")
    suspend fun getChapterById(id: String): ChapterEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllChapters(chapters: List<ChapterEntity>)
    
    @Update
    suspend fun updateChapter(chapter: ChapterEntity)
    
    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)
}

/**
 * Lesson DAO
 */
@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE chapterId = :chapterId AND isActive = 1 ORDER BY `order` ASC")
    fun getLessonsByChapter(chapterId: String): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
    suspend fun getLessonById(id: String): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE isDownloaded = 1")
    fun getDownloadedLessons(): Flow<List<LessonEntity>>
    
    @Query("UPDATE lessons SET isDownloaded = :isDownloaded, downloadedAt = :downloadedAt WHERE id = :lessonId")
    suspend fun updateDownloadStatus(lessonId: String, isDownloaded: Boolean, downloadedAt: Long?)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLessons(lessons: List<LessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: LessonEntity)
    
    @Delete
    suspend fun deleteLesson(lesson: LessonEntity)
}

/**
 * Lesson Progress DAO
 */
@Dao
interface LessonProgressDao {
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId ORDER BY lastAccessedAt DESC")
    fun getProgressByChild(childId: String): Flow<List<LessonProgressEntity>>
    
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND lessonId = :lessonId LIMIT 1")
    suspend fun getProgressByChildAndLesson(childId: String, lessonId: String): LessonProgressEntity?
    
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND status = 'completed'")
    fun getCompletedLessonsByChild(childId: String): Flow<List<LessonProgressEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LessonProgressEntity): Long
    
    @Update
    suspend fun updateProgress(progress: LessonProgressEntity)
    
    @Delete
    suspend fun deleteProgress(progress: LessonProgressEntity)
}

/**
 * Quiz Question DAO
 */
@Dao
interface QuizQuestionDao {
    @Query("SELECT * FROM quiz_questions WHERE lessonId = :lessonId AND isActive = 1 ORDER BY `order` ASC")
    suspend fun getQuestionsByLesson(lessonId: String): List<QuizQuestionEntity>
    
    @Query("SELECT * FROM quiz_questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuizQuestionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuizQuestionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuestions(questions: List<QuizQuestionEntity>)
    
    @Update
    suspend fun updateQuestion(question: QuizQuestionEntity)
    
    @Delete
    suspend fun deleteQuestion(question: QuizQuestionEntity)
}

/**
 * Child Avatar DAO
 */
@Dao
interface ChildAvatarDao {
    @Query("SELECT * FROM child_avatars ORDER BY category ASC, `order` ASC")
    fun getAllAvatars(): Flow<List<ChildAvatarEntity>>
    
    @Query("SELECT * FROM child_avatars WHERE category = :category ORDER BY `order` ASC")
    fun getAvatarsByCategory(category: String): Flow<List<ChildAvatarEntity>>
    
    @Query("SELECT * FROM child_avatars WHERE id = :id LIMIT 1")
    suspend fun getAvatarById(id: String): ChildAvatarEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvatar(avatar: ChildAvatarEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAvatars(avatars: List<ChildAvatarEntity>)
    
    @Update
    suspend fun updateAvatar(avatar: ChildAvatarEntity)
    
    @Delete
    suspend fun deleteAvatar(avatar: ChildAvatarEntity)
}

/**
 * Download DAO
 */
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE downloadStatus IN ('pending', 'downloading') ORDER BY priority ASC, createdAt ASC")
    fun getPendingDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE contentId = :contentId AND contentType = :contentType LIMIT 1")
    suspend fun getDownloadByContent(contentId: String, contentType: String): DownloadEntity?
    
    @Query("SELECT * FROM downloads WHERE downloadStatus = 'completed'")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>
    
    @Query("UPDATE downloads SET downloadStatus = :status, downloadProgress = :progress, updatedAt = :updatedAt WHERE id = :downloadId")
    suspend fun updateDownloadProgress(downloadId: String, status: String, progress: Int, updatedAt: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long
    
    @Update
    suspend fun updateDownload(download: DownloadEntity)
    
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE downloadStatus = 'failed' AND retryCount >= maxRetries")
    suspend fun deleteFailedDownloads()
}

/**
 * App Settings DAO
 */
@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE key = :key AND (userId IS NULL OR userId = :userId) ORDER BY isUserSpecific DESC LIMIT 1")
    suspend fun getSetting(key: String, userId: String? = null): AppSettingsEntity?
    
    @Query("SELECT * FROM app_settings WHERE userId = :userId OR userId IS NULL")
    fun getSettingsForUser(userId: String): Flow<List<AppSettingsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettingsEntity): Long
    
    @Update
    suspend fun updateSetting(setting: AppSettingsEntity)
    
    @Delete
    suspend fun deleteSetting(setting: AppSettingsEntity)
    
    @Query("DELETE FROM app_settings WHERE userId = :userId")
    suspend fun deleteUserSettings(userId: String)
}

/**
 * Sync Status DAO
 */
@Dao
interface SyncStatusDao {
    @Query("SELECT * FROM sync_status WHERE syncStatus IN ('pending', 'failed') AND retryCount < maxRetries ORDER BY priority ASC, scheduledFor ASC")
    fun getPendingSyncItems(): Flow<List<SyncStatusEntity>>
    
    @Query("SELECT * FROM sync_status WHERE entityType = :entityType AND entityId = :entityId LIMIT 1")
    suspend fun getSyncStatusByEntity(entityType: String, entityId: String): SyncStatusEntity?
    
    @Query("UPDATE sync_status SET syncStatus = :status, retryCount = :retryCount, lastError = :error, updatedAt = :updatedAt WHERE id = :syncId")
    suspend fun updateSyncStatus(syncId: String, status: String, retryCount: Int, error: String?, updatedAt: Long)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(syncItem: SyncStatusEntity): Long
    
    @Update
    suspend fun updateSyncItem(syncItem: SyncStatusEntity)
    
    @Delete
    suspend fun deleteSyncItem(syncItem: SyncStatusEntity)
    
    @Query("DELETE FROM sync_status WHERE syncStatus = 'synced' AND createdAt < :cutoffTime")
    suspend fun deleteOldSyncedItems(cutoffTime: Long)
}

/**
 * Session Activity DAO
 */
@Dao
interface SessionActivityDao {
    @Query("SELECT * FROM session_activity WHERE userId = :userId ORDER BY sessionDate DESC, sessionStartTime DESC")
    fun getSessionsByUser(userId: String): Flow<List<SessionActivityEntity>>
    
    @Query("SELECT * FROM session_activity WHERE userId = :userId AND sessionDate = :date ORDER BY sessionStartTime DESC")
    suspend fun getSessionsByUserAndDate(userId: String, date: String): List<SessionActivityEntity>
    
    @Query("SELECT * FROM session_activity WHERE isCompleted = 0")
    suspend fun getIncompleteSession(): SessionActivityEntity?
    
    @Query("SELECT SUM(screenTimeMinutes) FROM session_activity WHERE userId = :userId AND sessionDate = :date")
    suspend fun getTotalScreenTimeForDate(userId: String, date: String): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionActivityEntity): Long
    
    @Update
    suspend fun updateSession(session: SessionActivityEntity)
    
    @Delete
    suspend fun deleteSession(session: SessionActivityEntity)
    
    @Query("DELETE FROM session_activity WHERE sessionDate < :cutoffDate")
    suspend fun deleteOldSessions(cutoffDate: String)
}

/**
 * Achievement DAO
 */
@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE userId = :userId ORDER BY isUnlocked DESC, unlockedAt DESC")
    fun getAchievementsByUser(userId: String): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE userId = :userId AND isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(userId: String): Flow<List<AchievementEntity>>
    
    @Query("SELECT * FROM achievements WHERE userId = :userId AND achievementType = :type LIMIT 1")
    suspend fun getAchievementByType(userId: String, type: String): AchievementEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAchievements(achievements: List<AchievementEntity>)
    
    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)
    
    @Delete
    suspend fun deleteAchievement(achievement: AchievementEntity)
}