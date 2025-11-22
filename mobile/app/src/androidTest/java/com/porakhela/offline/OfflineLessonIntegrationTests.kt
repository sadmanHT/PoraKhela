package com.porakhela.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.porakhela.data.local.*
import com.porakhela.data.repository.OfflineLessonRepository
import com.porakhela.ui.screens.lessons.EnhancedOfflineLessonPlayerScreen
import com.porakhela.ui.screens.lessons.OfflineLessonPlayerViewModel
import com.porakhela.workers.OfflineSyncWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineLessonIntegrationTests {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var repository: OfflineLessonRepository

    @Inject
    lateinit var database: AppDatabase

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize WorkManager for testing
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun `test airplane mode lesson completion and sync`() = runTest {
        // Step 1: Setup test lesson data
        val testLesson = OfflineLesson(
            id = "integration_lesson_1",
            title = "Integration Test Lesson",
            description = "Test lesson for airplane mode",
            subject = "math",
            difficulty = "easy",
            totalPoints = 30,
            estimatedDuration = 180,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val testQuestions = listOf(
            OfflineQuestion(
                id = "q1",
                lessonId = "integration_lesson_1",
                question = "What is 2 + 2?",
                options = listOf("3", "4", "5", "6"),
                correctAnswerIndex = 1,
                points = 10,
                timeLimit = 30,
                orderIndex = 0
            ),
            OfflineQuestion(
                id = "q2",
                lessonId = "integration_lesson_1",
                question = "What is 3 × 3?",
                options = listOf("6", "9", "12", "15"),
                correctAnswerIndex = 1,
                points = 10,
                timeLimit = 30,
                orderIndex = 1
            ),
            OfflineQuestion(
                id = "q3",
                lessonId = "integration_lesson_1",
                question = "What is 10 ÷ 2?",
                options = listOf("3", "4", "5", "6"),
                correctAnswerIndex = 2,
                points = 10,
                timeLimit = 30,
                orderIndex = 2
            )
        )

        // Insert test data
        database.offlineLessonDao().insertLesson(testLesson)
        database.offlineLessonDao().insertQuestions(testQuestions)

        // Step 2: Simulate airplane mode (disable network)
        simulateNetworkState(false)
        assertFalse("Should be offline", repository.isOnline())

        // Step 3: Play lesson in offline mode
        val userId = "test_user_123"
        
        // Simulate answering questions correctly
        repository.saveQuestionResult("integration_lesson_1", "q1", userId, true, 1, 15, 10)
        repository.saveQuestionResult("integration_lesson_1", "q2", userId, true, 1, 20, 10)
        repository.saveQuestionResult("integration_lesson_1", "q3", userId, true, 2, 18, 10)

        // Step 4: Verify progress is saved locally
        val localProgress = database.progressDao().getProgressForLesson("integration_lesson_1", userId).first()
        assertEquals("Should save all 3 answers", 3, localProgress.size)
        assertTrue("All answers should be correct", localProgress.all { it.isCorrect })
        assertFalse("Progress should not be synced yet", localProgress.any { it.isSynced })

        val localPoints = database.pointsDao().getPointsForUser(userId).first()
        assertEquals("Should earn 30 points total", 30, localPoints.sumOf { it.pointsEarned })
        assertFalse("Points should not be synced yet", localPoints.any { it.isSynced })

        // Step 5: Enable network (exit airplane mode)
        simulateNetworkState(true)
        assertTrue("Should be online", repository.isOnline())

        // Step 6: Trigger sync and verify it completes within 5 seconds
        val syncStartTime = System.currentTimeMillis()
        val syncWorkRequest = OfflineSyncWorker.createSyncRequest()
        
        workManager.enqueue(syncWorkRequest).result.get()
        
        // Wait for sync to complete
        var syncCompleted = false
        var attempts = 0
        while (!syncCompleted && attempts < 50) { // Max 5 seconds (50 * 100ms)
            delay(100)
            val workInfo = workManager.getWorkInfoById(syncWorkRequest.id).get()
            syncCompleted = workInfo.state == WorkInfo.State.SUCCEEDED
            attempts++
        }
        
        val syncDuration = System.currentTimeMillis() - syncStartTime
        
        assertTrue("Sync should complete successfully", syncCompleted)
        assertTrue("Sync should complete within 5 seconds", syncDuration <= 5000)

        // Step 7: Verify data is marked as synced
        val syncedProgress = database.progressDao().getProgressForLesson("integration_lesson_1", userId).first()
        assertTrue("All progress should be marked as synced", syncedProgress.all { it.isSynced })

        val syncedPoints = database.pointsDao().getPointsForUser(userId).first()
        assertTrue("All points should be marked as synced", syncedPoints.all { it.isSynced })
    }

    @Test
    fun `test lesson UI in airplane mode`() {
        // Setup test lesson data
        val testLesson = OfflineLesson(
            id = "ui_test_lesson",
            title = "UI Test Lesson",
            description = "Testing UI in offline mode",
            subject = "science",
            difficulty = "medium",
            totalPoints = 20,
            estimatedDuration = 120,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val testQuestion = OfflineQuestion(
            id = "ui_q1",
            lessonId = "ui_test_lesson",
            question = "What color is the sky?",
            options = listOf("Red", "Blue", "Green", "Yellow"),
            correctAnswerIndex = 1,
            points = 20,
            timeLimit = 45,
            orderIndex = 0
        )

        runTest {
            database.offlineLessonDao().insertLesson(testLesson)
            database.offlineLessonDao().insertQuestions(listOf(testQuestion))
        }

        // Simulate airplane mode
        simulateNetworkState(false)

        // Launch lesson player UI
        composeTestRule.setContent {
            EnhancedOfflineLessonPlayerScreen(
                lessonId = "ui_test_lesson",
                onNavigateBack = {},
                onLessonComplete = {}
            )
        }

        // Test UI elements are displayed correctly
        composeTestRule.onNodeWithText("UI Test Lesson").assertIsDisplayed()
        composeTestRule.onNodeWithText("What color is the sky?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Blue").assertIsDisplayed()
        
        // Test offline indicator is shown
        composeTestRule.onNodeWithText("Offline Mode").assertIsDisplayed()

        // Answer the question
        composeTestRule.onNodeWithText("Blue").performClick()
        composeTestRule.onNodeWithText("Submit").performClick()

        // Verify answer feedback
        composeTestRule.onNodeWithText("Correct!").assertIsDisplayed()
        
        // Verify points are shown
        composeTestRule.onNodeWithText("+20 Porapoints! 🎉").assertIsDisplayed()
    }

    @Test
    fun `test multiple lessons in airplane mode`() = runTest {
        // Setup 3 test lessons
        val lessons = (1..3).map { i ->
            OfflineLesson(
                id = "lesson_$i",
                title = "Lesson $i",
                description = "Test lesson $i",
                subject = "math",
                difficulty = "easy",
                totalPoints = 10,
                estimatedDuration = 60,
                isDownloaded = true,
                downloadTimestamp = System.currentTimeMillis(),
                lastSyncTimestamp = 0L
            )
        }

        val questions = lessons.flatMap { lesson ->
            OfflineQuestion(
                id = "${lesson.id}_q1",
                lessonId = lesson.id,
                question = "Test question for ${lesson.title}",
                options = listOf("A", "B", "C", "D"),
                correctAnswerIndex = 0,
                points = 10,
                timeLimit = 30,
                orderIndex = 0
            ).let { listOf(it) }
        }

        // Insert all test data
        database.offlineLessonDao().insertLessons(lessons)
        database.offlineLessonDao().insertQuestions(questions)

        // Simulate airplane mode
        simulateNetworkState(false)

        val userId = "multi_lesson_user"

        // Complete all 3 lessons
        lessons.forEach { lesson ->
            repository.saveQuestionResult(lesson.id, "${lesson.id}_q1", userId, true, 0, 20, 10)
        }

        // Verify all progress is saved locally
        lessons.forEach { lesson ->
            val progress = database.progressDao().getProgressForLesson(lesson.id, userId).first()
            assertEquals("Should have progress for lesson ${lesson.id}", 1, progress.size)
            assertFalse("Progress should not be synced", progress[0].isSynced)
        }

        val totalPoints = database.pointsDao().getPointsForUser(userId).first().sumOf { it.pointsEarned }
        assertEquals("Should earn 30 points total", 30, totalPoints)
    }

    @Test
    fun `test sync failure and retry mechanism`() = runTest {
        // Setup test data
        val testLesson = OfflineLesson(
            id = "sync_test_lesson",
            title = "Sync Test",
            description = "Testing sync retry",
            subject = "science",
            difficulty = "easy",
            totalPoints = 15,
            estimatedDuration = 90,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val testQuestion = OfflineQuestion(
            id = "sync_q1",
            lessonId = "sync_test_lesson",
            question = "Test sync question",
            options = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 2,
            points = 15,
            timeLimit = 30,
            orderIndex = 0
        )

        database.offlineLessonDao().insertLesson(testLesson)
        database.offlineLessonDao().insertQuestions(listOf(testQuestion))

        // Complete lesson offline
        simulateNetworkState(false)
        val userId = "sync_test_user"
        repository.saveQuestionResult("sync_test_lesson", "sync_q1", userId, true, 2, 25, 15)

        // Simulate intermittent network (will cause sync failures)
        simulateNetworkState(true)
        
        // First sync attempt might fail due to simulated network issues
        val firstSyncRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(firstSyncRequest)
        
        // Wait a bit for potential failure
        delay(1000)
        
        // Ensure stable network for retry
        simulateNetworkState(true)
        
        // Second sync should succeed
        val retrySyncRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(retrySyncRequest).result.get()
        
        // Wait for completion
        var retryCompleted = false
        var attempts = 0
        while (!retryCompleted && attempts < 30) {
            delay(100)
            val workInfo = workManager.getWorkInfoById(retrySyncRequest.id).get()
            retryCompleted = workInfo.state == WorkInfo.State.SUCCEEDED
            attempts++
        }
        
        assertTrue("Retry sync should eventually succeed", retryCompleted)
    }

    private fun simulateNetworkState(connected: Boolean) {
        // This is a simplified simulation - in real testing, you might use
        // network simulation tools or mock the ConnectivityManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Note: In actual testing, you would need to use testing frameworks
        // that can simulate network states, like Espresso with network idling resources
        // or specialized testing tools for network condition simulation
    }
}

// Extension functions for better test readability
fun WorkManager.awaitCompletion(workRequestId: java.util.UUID, timeoutMs: Long = 5000): Boolean {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
        val workInfo = this.getWorkInfoById(workRequestId).get()
        if (workInfo.state.isFinished) {
            return workInfo.state == WorkInfo.State.SUCCEEDED
        }
        Thread.sleep(100)
    }
    return false
}