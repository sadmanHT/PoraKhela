package com.porakhela.data.repository

import android.content.Context
import com.porakhela.data.api.OfflineLessonService
import com.porakhela.data.local.dao.*
import com.porakhela.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing offline lesson downloads, storage, and playback
 * Handles lesson pack downloads, asset management, and offline progress tracking
 */
@Singleton
class OfflineLessonRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineLessonService: OfflineLessonService,
    private val offlineLessonDao: OfflineLessonDao,
    private val offlineQuestionDao: OfflineQuestionDao,
    private val lessonAssetDao: LessonAssetDao,
    private val offlineProgressDao: OfflineProgressDao,
    private val downloadStatusDao: DownloadStatusDao,
    private val localPointsDao: LocalPointsDao,
    private val localAchievementDao: LocalAchievementDao,
    private val gamificationEventDao: GamificationEventDao
) {
    
    private val lessonStorageDir = File(context.filesDir, "lessons")
    private val assetStorageDir = File(context.filesDir, "lesson_assets")
    
    init {
        // Ensure storage directories exist
        lessonStorageDir.mkdirs()
        assetStorageDir.mkdirs()
    }

    /**
     * Download a complete lesson pack for offline use
     */
    suspend fun downloadLessonPack(
        grade: String,
        subject: String,
        packSize: Int = 10
    ): Flow<DownloadProgress> = flow {
        try {
            emit(DownloadProgress.Started)
            
            // Check if already downloaded
            val existingPack = offlineLessonDao.getLessonsByGradeAndSubject(grade, subject).first()
            if (existingPack.isNotEmpty()) {
                emit(DownloadProgress.AlreadyExists)
                return@flow
            }
            
            // Download lesson pack metadata
            val response = offlineLessonService.downloadLessonPack(grade, subject, packSize)
            if (!response.isSuccessful) {
                emit(DownloadProgress.Failed("Failed to download lesson pack: ${response.message()}"))
                return@flow
            }
            
            val lessonPack = response.body() ?: run {
                emit(DownloadProgress.Failed("Empty response from server"))
                return@flow
            }
            
            // Create download status record
            val downloadStatus = DownloadStatus(
                packId = lessonPack.id,
                status = DownloadState.DOWNLOADING,
                progress = 0,
                downloadedBytes = 0L,
                totalBytes = lessonPack.totalSize,
                startedAt = System.currentTimeMillis()
            )
            downloadStatusDao.insertDownloadStatus(downloadStatus)
            
            emit(DownloadProgress.InProgress(0, lessonPack.totalSize, 0L))
            
            // Create pack directory
            val packDir = File(lessonStorageDir, lessonPack.id)
            packDir.mkdirs()
            
            var totalDownloaded = 0L
            val totalLessons = lessonPack.lessons.size
            
            // Download each lesson and its assets
            lessonPack.lessons.forEachIndexed { index, lesson ->
                // Download lesson assets first
                val downloadedAssets = mutableListOf<LessonAsset>()
                
                lesson.assets.forEach { asset ->
                    val assetFile = downloadAsset(asset, packDir)
                    if (assetFile != null) {
                        downloadedAssets.add(asset.copy(
                            localPath = assetFile.absolutePath,
                            downloadedAt = System.currentTimeMillis()
                        ))
                        totalDownloaded += assetFile.length()
                    }
                }
                
                // Save lesson with local asset paths
                val offlineLesson = lesson.copy(
                    packId = lessonPack.id,
                    localPath = packDir.absolutePath,
                    downloadedAt = System.currentTimeMillis(),
                    assets = downloadedAssets,
                    // Update asset paths in lesson content
                    thumbnailPath = downloadedAssets.find { it.type == AssetType.IMAGE }?.localPath,
                    videoPath = downloadedAssets.find { it.type == AssetType.VIDEO }?.localPath,
                    audioPath = downloadedAssets.find { it.type == AssetType.AUDIO }?.localPath
                )
                
                // Save to database
                offlineLessonDao.insertLesson(offlineLesson)
                offlineQuestionDao.insertQuestions(lesson.questions)
                lessonAssetDao.insertAssets(downloadedAssets)
                
                // Update progress
                val progress = ((index + 1) * 100) / totalLessons
                downloadStatusDao.updateDownloadProgress(
                    lessonPack.id,
                    DownloadState.DOWNLOADING,
                    progress,
                    totalDownloaded
                )
                
                emit(DownloadProgress.InProgress(progress, lessonPack.totalSize, totalDownloaded))
            }
            
            // Mark download as completed
            downloadStatusDao.markDownloadCompleted(
                lessonPack.id,
                DownloadState.COMPLETED,
                System.currentTimeMillis()
            )
            
            emit(DownloadProgress.Completed(lessonPack.lessons.size, totalDownloaded))
            
        } catch (e: Exception) {
            emit(DownloadProgress.Failed("Download failed: ${e.message}"))
        }
    }
    
    /**
     * Download individual asset file
     */
    private suspend fun downloadAsset(asset: LessonAsset, packDir: File): File? {
        return try {
            val response = offlineLessonService.downloadAsset(asset.id)
            if (!response.isSuccessful) return null
            
            val responseBody = response.body() ?: return null
            val assetFile = File(packDir, asset.fileName)
            
            withContext(Dispatchers.IO) {
                responseBody.byteStream().use { input ->
                    FileOutputStream(assetFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            // Verify file integrity if checksum provided
            asset.checksum?.let { expectedChecksum ->
                val actualChecksum = calculateFileChecksum(assetFile)
                if (actualChecksum != expectedChecksum) {
                    assetFile.delete()
                    return null
                }
            }
            
            assetFile
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get offline lessons for a specific grade and subject
     */
    fun getOfflineLessons(grade: String, subject: String): Flow<List<OfflineLesson>> {
        return offlineLessonDao.getLessonsByGradeAndSubject(grade, subject)
    }
    
    /**
     * Get specific offline lesson with questions
     */
    suspend fun getOfflineLessonWithQuestions(lessonId: String): LessonWithQuestions? {
        val lesson = offlineLessonDao.getLessonById(lessonId) ?: return null
        val questions = offlineQuestionDao.getQuestionsByLesson(lessonId).first()
        return LessonWithQuestions(lesson, questions)
    }
    
    /**
     * Start offline lesson and create progress record
     */
    suspend fun startOfflineLesson(childProfileId: String, lessonId: String): String {
        val progressId = UUID.randomUUID().toString()
        val progress = OfflineProgress(
            id = progressId,
            childProfileId = childProfileId,
            lessonId = lessonId,
            startedAt = System.currentTimeMillis(),
            completedAt = null,
            timeSpent = 0L,
            score = 0,
            maxScore = 0,
            questionsAnswered = 0,
            totalQuestions = 0,
            questionResults = emptyList(),
            deviceId = getDeviceId(),
            appVersion = getAppVersion()
        )
        
        offlineProgressDao.insertProgress(progress)
        return progressId
    }
    
    /**
     * Submit answer for offline question
     */
    suspend fun submitOfflineAnswer(
        progressId: String,
        questionId: String,
        selectedAnswer: Int,
        timeSpent: Long
    ): QuestionResult {
        val progress = offlineProgressDao.getProgressByLessonAndChild(
            getProgressById(progressId)?.lessonId ?: "",
            getProgressById(progressId)?.childProfileId ?: ""
        )
        val question = offlineQuestionDao.getQuestionById(questionId)
        
        if (progress == null || question == null) {
            throw IllegalStateException("Progress or question not found")
        }
        
        val isCorrect = selectedAnswer == question.correctAnswer
        val questionResult = OfflineQuestionResult(
            questionId = questionId,
            selectedAnswer = selectedAnswer,
            isCorrect = isCorrect,
            timeSpent = timeSpent,
            attempts = 1,
            answeredAt = System.currentTimeMillis()
        )
        
        // Update progress with new result
        val updatedResults = progress.questionResults + questionResult
        val updatedProgress = progress.copy(
            questionResults = updatedResults,
            questionsAnswered = updatedResults.size,
            score = updatedResults.count { it.isCorrect } * 10, // 10 points per correct answer
            timeSpent = progress.timeSpent + timeSpent
        )
        
        offlineProgressDao.updateProgress(updatedProgress)
        
        return QuestionResult(
            isCorrect = isCorrect,
            correctAnswer = question.correctAnswer,
            explanation = question.explanation,
            pointsEarned = if (isCorrect) question.points else 0
        )
    }
    
    /**
     * Complete offline lesson and trigger local gamification
     */
    suspend fun completeOfflineLesson(progressId: String): LessonCompletionResult {
        val progress = getProgressById(progressId) ?: throw IllegalStateException("Progress not found")
        val lesson = offlineLessonDao.getLessonById(progress.lessonId) ?: throw IllegalStateException("Lesson not found")
        
        val completedAt = System.currentTimeMillis()
        val finalScore = progress.questionResults.count { it.isCorrect } * 10
        val maxScore = progress.questionResults.size * 10
        
        // Update progress as completed
        val completedProgress = progress.copy(
            completedAt = completedAt,
            score = finalScore,
            maxScore = maxScore,
            totalQuestions = progress.questionResults.size
        )
        offlineProgressDao.updateProgress(completedProgress)
        
        // Mark lesson as completed
        offlineLessonDao.markLessonCompleted(lesson.id, true)
        
        // Update local points and check achievements
        val pointsEarned = finalScore + getBonusPoints(completedProgress)
        updateLocalPoints(progress.childProfileId, pointsEarned)
        val newAchievements = checkAndUnlockAchievements(progress.childProfileId, completedProgress)
        
        // Create gamification events
        createGamificationEvents(progress.childProfileId, pointsEarned, newAchievements)
        
        return LessonCompletionResult(
            score = finalScore,
            maxScore = maxScore,
            pointsEarned = pointsEarned,
            timeSpent = completedProgress.timeSpent,
            accuracy = (finalScore.toDouble() / maxScore.toDouble()) * 100,
            newAchievements = newAchievements,
            isNewBest = checkIfNewBest(progress.childProfileId, lesson.id, finalScore)
        )
    }
    
    /**
     * Get download status for lesson packs
     */
    fun getDownloadStatus(): Flow<List<DownloadStatus>> {
        return downloadStatusDao.getAllDownloads()
    }
    
    /**
     * Get local points for child
     */
    fun getLocalPoints(childProfileId: String): Flow<LocalPoints?> {
        return localPointsDao.getPointsByChild(childProfileId)
    }
    
    /**
     * Get local achievements for child
     */
    fun getLocalAchievements(childProfileId: String): Flow<List<LocalAchievement>> {
        return localAchievementDao.getAchievementsByChild(childProfileId)
    }
    
    /**
     * Get pending gamification events
     */
    fun getPendingGamificationEvents(childProfileId: String): Flow<List<GamificationEvent>> {
        return gamificationEventDao.getPendingEvents(childProfileId)
    }
    
    /**
     * Mark gamification event as shown
     */
    suspend fun markEventShown(eventId: String) {
        gamificationEventDao.markEventShown(eventId)
    }
    
    /**
     * Delete lesson pack and all associated data
     */
    suspend fun deleteLessonPack(packId: String) {
        // Delete from database
        offlineLessonDao.deleteLessonsByPack(packId)
        downloadStatusDao.deleteDownloadStatus(packId)
        
        // Delete files
        val packDir = File(lessonStorageDir, packId)
        if (packDir.exists()) {
            packDir.deleteRecursively()
        }
    }
    
    // Helper methods
    
    private suspend fun getProgressById(progressId: String): OfflineProgress? {
        // This would need to be implemented in DAO
        return null // Placeholder
    }
    
    private suspend fun updateLocalPoints(childProfileId: String, points: Int) {
        val currentTime = System.currentTimeMillis()
        
        // Get or create local points record
        val existingPoints = localPointsDao.getPointsByChild(childProfileId).first()
        if (existingPoints == null) {
            val newPoints = LocalPoints(
                childProfileId = childProfileId,
                totalPoints = points,
                dailyPoints = points,
                weeklyPoints = points,
                currentStreak = 1,
                longestStreak = 1,
                level = calculateLevel(points),
                lastUpdated = currentTime,
                pendingSyncPoints = points,
                isSynced = false
            )
            localPointsDao.insertPoints(newPoints)
        } else {
            localPointsDao.addPoints(childProfileId, points, currentTime)
        }
    }
    
    private suspend fun checkAndUnlockAchievements(
        childProfileId: String,
        progress: OfflineProgress
    ): List<LocalAchievement> {
        val newAchievements = mutableListOf<LocalAchievement>()
        
        // Check various achievement conditions
        val completedLessons = offlineProgressDao.getCompletedLessonCount(childProfileId)
        val averageScore = offlineProgressDao.getAverageScore(childProfileId) ?: 0.0
        
        // First lesson achievement
        if (completedLessons == 1) {
            newAchievements.add(createAchievement(
                childProfileId, "FIRST_LESSON", "First Lesson Complete!", 
                "You completed your first lesson!", 50
            ))
        }
        
        // Perfect score achievement
        if (progress.score == progress.maxScore && progress.score > 0) {
            newAchievements.add(createAchievement(
                childProfileId, "PERFECT_SCORE", "Perfect Score!", 
                "You got all questions correct!", 100
            ))
        }
        
        // Speed achievement (completed in less than average time)
        val averageTime = lesson.duration * 60 * 1000L // Convert minutes to milliseconds
        if (progress.timeSpent < averageTime * 0.8) {
            newAchievements.add(createAchievement(
                childProfileId, "SPEED_LEARNER", "Speed Learner!", 
                "You completed this lesson quickly!", 75
            ))
        }
        
        // Save achievements
        newAchievements.forEach { achievement ->
            localAchievementDao.insertAchievement(achievement)
        }
        
        return newAchievements
    }
    
    private fun createAchievement(
        childProfileId: String,
        type: String,
        title: String,
        description: String,
        points: Int
    ): LocalAchievement {
        return LocalAchievement(
            id = UUID.randomUUID().toString(),
            childProfileId = childProfileId,
            type = type,
            title = title,
            description = description,
            points = points,
            iconPath = null, // Would be set based on achievement type
            earnedAt = System.currentTimeMillis(),
            isUnlocked = true,
            isSynced = false
        )
    }
    
    private suspend fun createGamificationEvents(
        childProfileId: String,
        pointsEarned: Int,
        achievements: List<LocalAchievement>
    ) {
        // Points earned event
        val pointsEvent = GamificationEvent(
            id = UUID.randomUUID().toString(),
            childProfileId = childProfileId,
            type = GamificationEventType.POINTS_EARNED,
            title = "+$pointsEarned Porapoints! 🎉",
            message = "Great job completing the lesson!",
            points = pointsEarned,
            iconPath = null,
            animationType = "points_popup",
            createdAt = System.currentTimeMillis()
        )
        gamificationEventDao.insertEvent(pointsEvent)
        
        // Achievement events
        achievements.forEach { achievement ->
            val achievementEvent = GamificationEvent(
                id = UUID.randomUUID().toString(),
                childProfileId = childProfileId,
                type = GamificationEventType.ACHIEVEMENT_UNLOCKED,
                title = achievement.title,
                message = achievement.description,
                points = achievement.points,
                iconPath = achievement.iconPath,
                animationType = "achievement_unlock",
                createdAt = System.currentTimeMillis()
            )
            gamificationEventDao.insertEvent(achievementEvent)
        }
    }
    
    private fun getBonusPoints(progress: OfflineProgress): Int {
        var bonus = 0
        
        // Perfect score bonus
        if (progress.score == progress.maxScore && progress.score > 0) {
            bonus += 25
        }
        
        // Speed bonus
        val lesson = offlineLessonDao.getLessonById(progress.lessonId)
        lesson?.let {
            val expectedTime = it.duration * 60 * 1000L
            if (progress.timeSpent < expectedTime * 0.8) {
                bonus += 15
            }
        }
        
        return bonus
    }
    
    private fun calculateLevel(totalPoints: Int): Int {
        return (totalPoints / 1000) + 1 // 1000 points per level
    }
    
    private suspend fun checkIfNewBest(childProfileId: String, lessonId: String, score: Int): Boolean {
        val existingProgress = offlineProgressDao.getProgressByLessonAndChild(lessonId, childProfileId)
        return existingProgress?.score?.let { score > it } ?: true
    }
    
    private fun calculateFileChecksum(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
    
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }
    
    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
    }
}

/**
 * Download progress states
 */
sealed class DownloadProgress {
    object Started : DownloadProgress()
    object AlreadyExists : DownloadProgress()
    data class InProgress(val percentage: Int, val totalBytes: Long, val downloadedBytes: Long) : DownloadProgress()
    data class Completed(val lessonCount: Int, val totalSize: Long) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}

/**
 * Lesson with questions for offline use
 */
data class LessonWithQuestions(
    val lesson: OfflineLesson,
    val questions: List<OfflineQuestion>
)

/**
 * Result of answering a question
 */
data class QuestionResult(
    val isCorrect: Boolean,
    val correctAnswer: Int,
    val explanation: String?,
    val pointsEarned: Int
)

/**
 * Result of completing a lesson
 */
data class LessonCompletionResult(
    val score: Int,
    val maxScore: Int,
    val pointsEarned: Int,
    val timeSpent: Long,
    val accuracy: Double,
    val newAchievements: List<LocalAchievement>,
    val isNewBest: Boolean
)