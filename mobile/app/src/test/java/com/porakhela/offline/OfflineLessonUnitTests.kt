package com.porakhela.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.porakhela.data.local.*
import com.porakhela.data.remote.OfflineLessonService
import com.porakhela.data.repository.OfflineLessonRepository
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import retrofit2.Response
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class OfflineLessonUnitTests {

    private val mockOfflineLessonDao = mockk<OfflineLessonDao>()
    private val mockProgressDao = mockk<OfflineProgressDao>()
    private val mockPointsDao = mockk<LocalPointsDao>()
    private val mockGamificationDao = mockk<GamificationEventDao>()
    private val mockApiService = mockk<OfflineLessonService>()
    private val mockNetworkChecker = mockk<NetworkChecker>()
    
    private lateinit var repository: OfflineLessonRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        repository = OfflineLessonRepository(
            offlineLessonDao = mockOfflineLessonDao,
            progressDao = mockProgressDao,
            pointsDao = mockPointsDao,
            gamificationDao = mockGamificationDao,
            apiService = mockApiService,
            networkChecker = mockNetworkChecker
        )
    }

    @Test
    fun `test lesson download success`() = runTest {
        // Arrange
        val lessonId = "lesson_123"
        val mockLessonResponse = LessonPackResponse(
            lessonId = lessonId,
            title = "Test Lesson",
            description = "Test Description",
            questions = listOf(
                QuestionResponse(
                    id = "q1",
                    question = "What is 2+2?",
                    options = listOf("3", "4", "5", "6"),
                    correctAnswerIndex = 1,
                    points = 10,
                    timeLimit = 30
                )
            ),
            totalPoints = 10,
            estimatedDuration = 300,
            difficulty = "easy",
            subject = "math",
            assets = listOf(
                AssetResponse(
                    id = "asset_1",
                    type = "image",
                    url = "https://example.com/image.png",
                    localPath = null
                )
            )
        )

        every { mockNetworkChecker.isConnected() } returns true
        coEvery { mockApiService.downloadLessonPack(lessonId) } returns Response.success(mockLessonResponse)
        coEvery { mockOfflineLessonDao.insertLesson(any()) } just Runs
        coEvery { mockOfflineLessonDao.insertQuestions(any()) } just Runs

        // Act
        val result = repository.downloadLessonPack(lessonId)

        // Assert
        assertTrue("Download should succeed", result.isSuccess)
        coVerify { mockOfflineLessonDao.insertLesson(any()) }
        coVerify { mockOfflineLessonDao.insertQuestions(any()) }
    }

    @Test
    fun `test lesson download network failure`() = runTest {
        // Arrange
        val lessonId = "lesson_123"
        every { mockNetworkChecker.isConnected() } returns false

        // Act
        val result = repository.downloadLessonPack(lessonId)

        // Assert
        assertTrue("Download should fail when offline", result.isFailure)
        assertEquals("Network not available", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test lesson download API failure`() = runTest {
        // Arrange
        val lessonId = "lesson_123"
        every { mockNetworkChecker.isConnected() } returns true
        coEvery { mockApiService.downloadLessonPack(lessonId) } throws IOException("API Error")

        // Act
        val result = repository.downloadLessonPack(lessonId)

        // Assert
        assertTrue("Download should fail on API error", result.isFailure)
        assertTrue("Should be IOException", result.exceptionOrNull() is IOException)
    }

    @Test
    fun `test local DB insert success`() = runTest {
        // Arrange
        val lesson = OfflineLesson(
            id = "lesson_123",
            title = "Test Lesson",
            description = "Test Description",
            subject = "math",
            difficulty = "easy",
            totalPoints = 100,
            estimatedDuration = 300,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val questions = listOf(
            OfflineQuestion(
                id = "q1",
                lessonId = "lesson_123",
                question = "What is 2+2?",
                options = listOf("3", "4", "5", "6"),
                correctAnswerIndex = 1,
                points = 10,
                timeLimit = 30,
                orderIndex = 0
            )
        )

        coEvery { mockOfflineLessonDao.insertLesson(lesson) } just Runs
        coEvery { mockOfflineLessonDao.insertQuestions(questions) } just Runs

        // Act & Assert (no exception should be thrown)
        repository.insertLessonLocally(lesson, questions)

        // Verify
        coVerify { mockOfflineLessonDao.insertLesson(lesson) }
        coVerify { mockOfflineLessonDao.insertQuestions(questions) }
    }

    @Test
    fun `test offline mode detection`() = runTest {
        // Test connected state
        every { mockNetworkChecker.isConnected() } returns true
        assertTrue("Should detect online state", repository.isOnline())

        // Test disconnected state
        every { mockNetworkChecker.isConnected() } returns false
        assertFalse("Should detect offline state", repository.isOnline())
    }

    @Test
    fun `test results storage with gamification events`() = runTest {
        // Arrange
        val lessonId = "lesson_123"
        val questionId = "q1"
        val userId = "user_456"
        val isCorrect = true
        val earnedPoints = 10
        val timeTaken = 15

        val progressEntry = OfflineProgress(
            id = "progress_1",
            lessonId = lessonId,
            questionId = questionId,
            userId = userId,
            isCorrect = isCorrect,
            selectedAnswerIndex = 1,
            timeTaken = timeTaken,
            pointsEarned = earnedPoints,
            timestamp = System.currentTimeMillis(),
            isSynced = false
        )

        val pointsEntry = LocalPoints(
            id = "points_1",
            userId = userId,
            lessonId = lessonId,
            pointsEarned = earnedPoints,
            source = "lesson_completion",
            timestamp = System.currentTimeMillis(),
            isSynced = false
        )

        val gamificationEvent = GamificationEvent(
            id = "event_1",
            userId = userId,
            type = "points_earned",
            data = "{\"points\": $earnedPoints, \"lesson\": \"$lessonId\"}",
            timestamp = System.currentTimeMillis(),
            isShown = false
        )

        coEvery { mockProgressDao.insertProgress(progressEntry) } just Runs
        coEvery { mockPointsDao.insertPoints(pointsEntry) } just Runs
        coEvery { mockGamificationDao.insertEvent(gamificationEvent) } just Runs

        // Act
        repository.saveQuestionResult(
            lessonId, questionId, userId, isCorrect, 1, timeTaken, earnedPoints
        )

        // Assert
        coVerify { mockProgressDao.insertProgress(any()) }
        coVerify { mockPointsDao.insertPoints(any()) }
        coVerify { mockGamificationDao.insertEvent(any()) }
    }

    @Test
    fun `test lesson completion tracking`() = runTest {
        // Arrange
        val lessonId = "lesson_123"
        val userId = "user_456"
        val totalQuestions = 5
        val correctAnswers = 4
        val totalPoints = 40
        val completionTime = 120

        val existingProgress = listOf(
            OfflineProgress("p1", lessonId, "q1", userId, true, 0, 30, 10, System.currentTimeMillis(), false),
            OfflineProgress("p2", lessonId, "q2", userId, true, 1, 25, 10, System.currentTimeMillis(), false),
            OfflineProgress("p3", lessonId, "q3", userId, false, 2, 20, 0, System.currentTimeMillis(), false),
            OfflineProgress("p4", lessonId, "q4", userId, true, 0, 15, 10, System.currentTimeMillis(), false),
            OfflineProgress("p5", lessonId, "q5", userId, true, 1, 30, 10, System.currentTimeMillis(), false)
        )

        every { mockProgressDao.getProgressForLesson(lessonId, userId) } returns flowOf(existingProgress)
        coEvery { mockGamificationDao.insertEvent(any()) } just Runs

        // Act
        val completionResult = repository.getLessonCompletion(lessonId, userId).first()

        // Assert
        assertEquals("Should track correct answers", 4, completionResult.correctAnswers)
        assertEquals("Should track total questions", 5, completionResult.totalQuestions)
        assertEquals("Should track total points", 40, completionResult.totalPoints)
        assertTrue("Should mark as completed", completionResult.isCompleted)
    }

    @Test
    fun `test sync queue management`() = runTest {
        // Arrange
        val unsyncedProgress = listOf(
            OfflineProgress("p1", "lesson_1", "q1", "user_1", true, 0, 30, 10, System.currentTimeMillis(), false),
            OfflineProgress("p2", "lesson_1", "q2", "user_1", false, 1, 25, 0, System.currentTimeMillis(), false)
        )

        val unsyncedPoints = listOf(
            LocalPoints("points_1", "user_1", "lesson_1", 10, "question_correct", System.currentTimeMillis(), false)
        )

        every { mockProgressDao.getUnsyncedProgress() } returns flowOf(unsyncedProgress)
        every { mockPointsDao.getUnsyncedPoints() } returns flowOf(unsyncedPoints)

        // Act
        val progressQueue = repository.getUnsyncedProgress().first()
        val pointsQueue = repository.getUnsyncedPoints().first()

        // Assert
        assertEquals("Should return unsynced progress", 2, progressQueue.size)
        assertEquals("Should return unsynced points", 1, pointsQueue.size)
        assertFalse("Progress should not be synced", progressQueue[0].isSynced)
        assertFalse("Points should not be synced", pointsQueue[0].isSynced)
    }

    @Test
    fun `test idempotency in lesson download`() = runTest {
        // Arrange
        val lessonId = "lesson_123"
        val existingLesson = OfflineLesson(
            id = lessonId,
            title = "Existing Lesson",
            description = "Already downloaded",
            subject = "math",
            difficulty = "easy",
            totalPoints = 100,
            estimatedDuration = 300,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis() - 1000,
            lastSyncTimestamp = 0L
        )

        every { mockOfflineLessonDao.getLessonById(lessonId) } returns flowOf(existingLesson)
        every { mockNetworkChecker.isConnected() } returns true

        // Act
        val result = repository.downloadLessonPack(lessonId)

        // Assert
        assertTrue("Should succeed without re-downloading", result.isSuccess)
        coVerify(exactly = 0) { mockApiService.downloadLessonPack(lessonId) }
    }
}

// Mock classes for testing
class NetworkChecker {
    fun isConnected(): Boolean = true
}

data class LessonCompletion(
    val correctAnswers: Int,
    val totalQuestions: Int,
    val totalPoints: Int,
    val isCompleted: Boolean,
    val completionPercentage: Float = (correctAnswers.toFloat() / totalQuestions) * 100f
)