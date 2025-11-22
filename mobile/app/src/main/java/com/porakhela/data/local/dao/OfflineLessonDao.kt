package com.porakhela.data.local.dao

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import kotlinx.coroutines.flow.Flow

/**
 * Room entities for offline lesson storage
 */
@Entity(
    tableName = "offline_lessons",
    indices = [Index(value = ["grade", "subject"])]
)
data class OfflineLessonEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val subject: String,
    val grade: String,
    val duration: Int,
    val difficulty: String,
    val description: String,
    val thumbnailPath: String? = null,
    val videoPath: String? = null,
    val audioPath: String? = null,
    val packVersion: String,
    val downloadedAt: Long,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isComplete: Boolean = true,
    val totalQuestions: Int = 0,
    val tags: String = "" // JSON array as string
)

@Entity(
    tableName = "offline_lesson_sections",
    foreignKeys = [ForeignKey(
        entity = OfflineLessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = CASCADE
    )],
    indices = [Index(value = ["lessonId"])]
)
data class OfflineLessonSectionEntity(
    @PrimaryKey
    val id: String,
    val lessonId: String,
    val title: String,
    val content: String,
    val type: String, // VIDEO, AUDIO, TEXT, INTERACTIVE
    val duration: Int,
    val assetPath: String? = null,
    val orderIndex: Int
)

@Entity(
    tableName = "offline_questions",
    foreignKeys = [ForeignKey(
        entity = OfflineLessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = CASCADE
    )],
    indices = [Index(value = ["lessonId"])]
)
data class OfflineQuestionEntity(
    @PrimaryKey
    val id: String,
    val lessonId: String,
    val question: String,
    val options: String, // JSON array as string
    val correctAnswer: Int,
    val explanation: String,
    val points: Int = 10,
    val timeLimit: Int = 30,
    val imagePath: String? = null,
    val audioPath: String? = null
)

@Entity(
    tableName = "offline_lesson_progress",
    foreignKeys = [ForeignKey(
        entity = OfflineLessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = CASCADE
    )],
    indices = [Index(value = ["childProfileId", "lessonId"], unique = true)]
)
data class OfflineLessonProgressEntity(
    @PrimaryKey
    val id: String,
    val childProfileId: String,
    val lessonId: String,
    val status: String, // NOT_STARTED, IN_PROGRESS, COMPLETED
    val currentQuestionIndex: Int = 0,
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val pointsEarned: Int = 0,
    val timeSpent: Long = 0,
    val streak: Int = 0,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastAccessedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncedAt: Long? = null
)

@Entity(
    tableName = "offline_question_results",
    foreignKeys = [
        ForeignKey(
            entity = OfflineQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = CASCADE
        )
    ],
    indices = [Index(value = ["questionId", "childProfileId"])]
)
data class OfflineQuestionResultEntity(
    @PrimaryKey
    val id: String,
    val questionId: String,
    val childProfileId: String,
    val lessonId: String,
    val selectedAnswer: Int,
    val isCorrect: Boolean,
    val timeSpent: Long,
    val pointsEarned: Int,
    val answeredAt: Long,
    val isSynced: Boolean = false
)

@Entity(
    tableName = "lesson_assets",
    indices = [Index(value = ["url"], unique = true)]
)
data class LessonAssetEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val localPath: String,
    val type: String, // IMAGE, AUDIO, VIDEO
    val filename: String,
    val size: Long,
    val checksum: String,
    val downloadedAt: Long,
    val isValid: Boolean = true
)

@Entity(tableName = "lesson_pack_info")
data class LessonPackInfoEntity(
    @PrimaryKey
    val grade: String,
    val currentVersion: String,
    val downloadedVersion: String,
    val lastChecked: Long,
    val lastDownloaded: Long,
    val totalLessons: Int,
    val downloadedLessons: Int,
    val totalSize: Long,
    val downloadedSize: Long,
    val isUpdateAvailable: Boolean = false
)

/**
 * DAOs for offline lesson data access
 */
@Dao
interface OfflineLessonDao {

    @Query("SELECT * FROM offline_lessons WHERE grade = :grade ORDER BY title ASC")
    fun getLessonsByGrade(grade: String): Flow<List<OfflineLessonEntity>>

    @Query("SELECT * FROM offline_lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: String): OfflineLessonEntity?

    @Query("SELECT * FROM offline_lessons WHERE subject = :subject AND grade = :grade")
    fun getLessonsBySubjectAndGrade(subject: String, grade: String): Flow<List<OfflineLessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: OfflineLessonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<OfflineLessonEntity>)

    @Delete
    suspend fun deleteLesson(lesson: OfflineLessonEntity)

    @Query("DELETE FROM offline_lessons WHERE grade = :grade")
    suspend fun deleteLessonsByGrade(grade: String)

    @Query("UPDATE offline_lessons SET lastAccessedAt = :timestamp WHERE id = :lessonId")
    suspend fun updateLastAccessed(lessonId: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM offline_lessons WHERE grade = :grade")
    suspend fun getLessonCountByGrade(grade: String): Int
}

@Dao
interface OfflineLessonSectionDao {

    @Query("SELECT * FROM offline_lesson_sections WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    suspend fun getSectionsByLesson(lessonId: String): List<OfflineLessonSectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<OfflineLessonSectionEntity>)

    @Delete
    suspend fun deleteSection(section: OfflineLessonSectionEntity)
}

@Dao
interface OfflineQuestionDao {

    @Query("SELECT * FROM offline_questions WHERE lessonId = :lessonId ORDER BY RANDOM()")
    suspend fun getQuestionsByLesson(lessonId: String): List<OfflineQuestionEntity>

    @Query("SELECT * FROM offline_questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): OfflineQuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<OfflineQuestionEntity>)

    @Delete
    suspend fun deleteQuestion(question: OfflineQuestionEntity)

    @Query("SELECT COUNT(*) FROM offline_questions WHERE lessonId = :lessonId")
    suspend fun getQuestionCountByLesson(lessonId: String): Int
}

@Dao
interface OfflineLessonProgressDao {

    @Query("SELECT * FROM offline_lesson_progress WHERE childProfileId = :childProfileId AND lessonId = :lessonId")
    suspend fun getProgress(childProfileId: String, lessonId: String): OfflineLessonProgressEntity?

    @Query("SELECT * FROM offline_lesson_progress WHERE childProfileId = :childProfileId")
    fun getAllProgress(childProfileId: String): Flow<List<OfflineLessonProgressEntity>>

    @Query("SELECT * FROM offline_lesson_progress WHERE childProfileId = :childProfileId AND status = 'COMPLETED'")
    fun getCompletedLessons(childProfileId: String): Flow<List<OfflineLessonProgressEntity>>

    @Query("SELECT * FROM offline_lesson_progress WHERE isSynced = 0")
    suspend fun getUnsyncedProgress(): List<OfflineLessonProgressEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: OfflineLessonProgressEntity)

    @Update
    suspend fun updateProgress(progress: OfflineLessonProgressEntity)

    @Query("UPDATE offline_lesson_progress SET isSynced = 1, syncedAt = :timestamp WHERE id = :progressId")
    suspend fun markAsSynced(progressId: String, timestamp: Long)

    @Query("SELECT SUM(pointsEarned) FROM offline_lesson_progress WHERE childProfileId = :childProfileId")
    suspend fun getTotalPointsEarned(childProfileId: String): Int?

    @Query("SELECT MAX(streak) FROM offline_lesson_progress WHERE childProfileId = :childProfileId")
    suspend fun getMaxStreak(childProfileId: String): Int?
}

@Dao
interface OfflineQuestionResultDao {

    @Query("SELECT * FROM offline_question_results WHERE questionId = :questionId AND childProfileId = :childProfileId")
    suspend fun getQuestionResult(questionId: String, childProfileId: String): OfflineQuestionResultEntity?

    @Query("SELECT * FROM offline_question_results WHERE lessonId = :lessonId AND childProfileId = :childProfileId")
    suspend fun getResultsByLesson(lessonId: String, childProfileId: String): List<OfflineQuestionResultEntity>

    @Query("SELECT * FROM offline_question_results WHERE isSynced = 0")
    suspend fun getUnsyncedResults(): List<OfflineQuestionResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: OfflineQuestionResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<OfflineQuestionResultEntity>)

    @Query("UPDATE offline_question_results SET isSynced = 1 WHERE id = :resultId")
    suspend fun markResultAsSynced(resultId: String)

    @Query("SELECT COUNT(*) FROM offline_question_results WHERE childProfileId = :childProfileId AND isCorrect = 1")
    suspend fun getTotalCorrectAnswers(childProfileId: String): Int
}

@Dao
interface LessonAssetDao {

    @Query("SELECT * FROM lesson_assets WHERE url = :url")
    suspend fun getAssetByUrl(url: String): LessonAssetEntity?

    @Query("SELECT * FROM lesson_assets WHERE localPath = :path")
    suspend fun getAssetByPath(path: String): LessonAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: LessonAssetEntity)

    @Query("DELETE FROM lesson_assets WHERE id = :assetId")
    suspend fun deleteAsset(assetId: String)

    @Query("SELECT SUM(size) FROM lesson_assets")
    suspend fun getTotalAssetSize(): Long?

    @Query("SELECT * FROM lesson_assets WHERE isValid = 0")
    suspend fun getInvalidAssets(): List<LessonAssetEntity>

    @Query("UPDATE lesson_assets SET isValid = :isValid WHERE id = :assetId")
    suspend fun updateAssetValidity(assetId: String, isValid: Boolean)
}

@Dao
interface LessonPackInfoDao {

    @Query("SELECT * FROM lesson_pack_info WHERE grade = :grade")
    suspend fun getPackInfo(grade: String): LessonPackInfoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackInfo(packInfo: LessonPackInfoEntity)

    @Query("UPDATE lesson_pack_info SET lastChecked = :timestamp WHERE grade = :grade")
    suspend fun updateLastChecked(grade: String, timestamp: Long)

    @Query("SELECT * FROM lesson_pack_info WHERE isUpdateAvailable = 1")
    suspend fun getPacksWithUpdates(): List<LessonPackInfoEntity>
}