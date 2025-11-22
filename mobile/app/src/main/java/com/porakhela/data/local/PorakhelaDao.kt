package com.porakhela.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for User operations
 */
@Dao
interface UserDao {
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber")
    suspend fun getUserByPhone(phoneNumber: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE userType = 'child'")
    fun getAllChildren(): Flow<List<UserEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}

/**
 * Data Access Object for Child Profile operations
 */
@Dao
interface ChildProfileDao {
    
    @Query("SELECT * FROM child_profiles WHERE userId = :userId")
    suspend fun getProfileByUserId(userId: String): ChildProfileEntity?
    
    @Query("SELECT * FROM child_profiles WHERE parentId = :parentId")
    fun getChildrenByParentId(parentId: String): Flow<List<ChildProfileEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ChildProfileEntity)
    
    @Update
    suspend fun updateProfile(profile: ChildProfileEntity)
    
    @Delete
    suspend fun deleteProfile(profile: ChildProfileEntity)
}

/**
 * Data Access Object for Subject operations
 */
@Dao
interface SubjectDao {
    
    @Query("SELECT * FROM subjects WHERE isActive = 1 ORDER BY name")
    fun getAllActiveSubjects(): Flow<List<SubjectEntity>>
    
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: String): SubjectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)
    
    @Update
    suspend fun updateSubject(subject: SubjectEntity)
    
    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()
}

/**
 * Data Access Object for Chapter operations
 */
@Dao
interface ChapterDao {
    
    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId AND grade = :grade AND isActive = 1 ORDER BY `order`")
    fun getChaptersBySubjectAndGrade(subjectId: String, grade: Int): Flow<List<ChapterEntity>>
    
    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: String): ChapterEntity?
    
    @Query("SELECT * FROM chapters WHERE isActive = 1 ORDER BY subjectId, grade, `order`")
    fun getAllActiveChapters(): Flow<List<ChapterEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)
    
    @Update
    suspend fun updateChapter(chapter: ChapterEntity)
    
    @Query("DELETE FROM chapters")
    suspend fun deleteAllChapters()
}

/**
 * Data Access Object for Lesson operations
 */
@Dao
interface LessonDao {
    
    @Query("SELECT * FROM lessons WHERE chapterId = :chapterId AND isActive = 1 ORDER BY `order`")
    fun getLessonsByChapter(chapterId: String): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE id = :lessonId")
    suspend fun getLessonById(lessonId: String): LessonEntity?
    
    @Query("SELECT * FROM lessons WHERE isDownloaded = 1")
    fun getDownloadedLessons(): Flow<List<LessonEntity>>
    
    @Query("SELECT * FROM lessons WHERE lessonType = :type AND isActive = 1")
    fun getLessonsByType(type: String): Flow<List<LessonEntity>>
    
    @Query("UPDATE lessons SET isDownloaded = :isDownloaded, downloadedAt = :downloadedAt WHERE id = :lessonId")
    suspend fun updateDownloadStatus(lessonId: String, isDownloaded: Boolean, downloadedAt: Long?)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLessons(lessons: List<LessonEntity>)
    
    @Update
    suspend fun updateLesson(lesson: LessonEntity)
    
    @Query("DELETE FROM lessons")
    suspend fun deleteAllLessons()
}

/**
 * Data Access Object for Lesson Progress operations
 */
@Dao
interface LessonProgressDao {
    
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId")
    fun getProgressByChild(childId: String): Flow<List<LessonProgressEntity>>
    
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND lessonId = :lessonId")
    suspend fun getProgressByChildAndLesson(childId: String, lessonId: String): LessonProgressEntity?
    
    @Query("SELECT * FROM lesson_progress WHERE childId = :childId AND status = :status")
    fun getProgressByChildAndStatus(childId: String, status: String): Flow<List<LessonProgressEntity>>
    
    @Query("SELECT COUNT(*) FROM lesson_progress WHERE childId = :childId AND status = 'completed'")
    suspend fun getCompletedLessonsCount(childId: String): Int
    
    @Query("SELECT SUM(timeSpentMinutes) FROM lesson_progress WHERE childId = :childId")
    suspend fun getTotalTimeSpent(childId: String): Int?
    
    @Query("SELECT AVG(quizScore) FROM lesson_progress WHERE childId = :childId AND quizScore IS NOT NULL")
    suspend fun getAverageQuizScore(childId: String): Float?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LessonProgressEntity)
    
    @Update
    suspend fun updateProgress(progress: LessonProgressEntity)
    
    @Delete
    suspend fun deleteProgress(progress: LessonProgressEntity)
    
    @Query("DELETE FROM lesson_progress WHERE childId = :childId")
    suspend fun deleteProgressByChild(childId: String)
}

/**
 * Data Access Object for Quiz Question operations
 */
@Dao
interface QuizQuestionDao {
    
    @Query("SELECT * FROM quiz_questions WHERE lessonId = :lessonId AND isActive = 1 ORDER BY `order`")
    suspend fun getQuestionsByLesson(lessonId: String): List<QuizQuestionEntity>
    
    @Query("SELECT * FROM quiz_questions WHERE id = :questionId")
    suspend fun getQuestionById(questionId: String): QuizQuestionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuizQuestionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)
    
    @Update
    suspend fun updateQuestion(question: QuizQuestionEntity)
    
    @Query("DELETE FROM quiz_questions")
    suspend fun deleteAllQuestions()
}