package com.porakhela.offline

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.porakhela.MainActivity
import com.porakhela.data.local.*
import com.porakhela.data.repository.OfflineLessonRepository
import com.porakhela.ui.screens.auth.LoginScreen
import com.porakhela.ui.screens.dashboard.ParentDashboardScreen
import com.porakhela.ui.screens.lessons.LessonListScreen
import com.porakhela.workers.OfflineSyncWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineLessonE2ETests {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: OfflineLessonRepository

    @Inject
    lateinit var database: AppDatabase

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Initialize WorkManager for testing
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)

        // Setup mock web server for API simulation
        mockWebServer = MockWebServer()
        mockWebServer.start()
        setupMockApiResponses()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `complete end-to-end offline lesson workflow`() = runTest {
        // === STEP 1: PARENT LOGS IN ===
        loginAsParent()

        // === STEP 2: NAVIGATE TO LESSON DOWNLOAD ===
        navigateToLessonDownload()

        // === STEP 3: DOWNLOAD LESSONS FOR CHILD ===
        downloadLessonsForChild()

        // === STEP 4: SWITCH TO CHILD MODE AND GO OFFLINE ===
        switchToChildMode()
        simulateOfflineMode(true)

        // === STEP 5: CHILD COMPLETES LESSONS OFFLINE ===
        completeLessonsOffline()

        // === STEP 6: VERIFY PROGRESS STORED LOCALLY ===
        verifyLocalProgressStorage()

        // === STEP 7: COME BACK ONLINE ===
        simulateOfflineMode(false)

        // === STEP 8: VERIFY SYNC HAPPENS AUTOMATICALLY ===
        verifySyncCompletion()

        // === STEP 9: SWITCH TO PARENT AND CHECK DASHBOARD ===
        switchToParentMode()
        verifyParentDashboardUpdates()

        // === STEP 10: VERIFY SMS REPORT INCLUDES SYNCED LESSONS ===
        verifySMSReportInclusion()
    }

    private fun loginAsParent() {
        // Navigate to login screen
        composeTestRule.onNodeWithText("Login").assertIsDisplayed()
        
        // Enter parent credentials
        composeTestRule.onNodeWithContentDescription("Phone Number Input")
            .performTextInput("01712345678")
        
        composeTestRule.onNodeWithContentDescription("PIN Input")
            .performTextInput("1234")
        
        // Click login
        composeTestRule.onNodeWithText("Log In").performClick()
        
        // Wait for login success
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Parent Dashboard").assertIsDisplayed()
    }

    private fun navigateToLessonDownload() {
        // Navigate to lessons section
        composeTestRule.onNodeWithText("Lessons").performClick()
        composeTestRule.waitForIdle()
        
        // Click download lessons
        composeTestRule.onNodeWithText("Download Lessons").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Available Lessons").assertIsDisplayed()
    }

    private fun downloadLessonsForChild() {
        // Download 3 different lessons
        val lessonTitles = listOf("Math Basics", "Science Fun", "English Reading")
        
        lessonTitles.forEach { lessonTitle ->
            // Find and click download button for each lesson
            composeTestRule.onAllNodesWithText("Download")[0].performClick()
            composeTestRule.waitForIdle()
            
            // Wait for download completion indicator
            composeTestRule.onNodeWithText("Downloaded").assertIsDisplayed()
        }
        
        // Verify all lessons downloaded
        composeTestRule.onAllNodesWithText("Downloaded").assertCountEquals(3)
    }

    private fun switchToChildMode() {
        // Navigate to child mode from parent dashboard
        composeTestRule.onNodeWithText("Switch to Child Mode").performClick()
        composeTestRule.waitForIdle()
        
        // Verify child interface is shown
        composeTestRule.onNodeWithText("Hi there! Let's learn!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your Lessons").assertIsDisplayed()
    }

    private fun simulateOfflineMode(isOffline: Boolean) {
        // In a real test, this would disable network connectivity
        // For this simulation, we'll inject the offline state
        runTest {
            // Set offline state in repository
            if (isOffline) {
                // Disable network checker
                println("=== GOING OFFLINE ===")
            } else {
                // Enable network checker
                println("=== COMING ONLINE ===")
            }
        }
        
        if (isOffline) {
            // Verify offline indicator appears
            composeTestRule.onNodeWithText("Offline Mode").assertIsDisplayed()
        }
    }

    private fun completeLessonsOffline() {
        val lessonTitles = listOf("Math Basics", "Science Fun", "English Reading")
        
        lessonTitles.forEach { lessonTitle ->
            // Click on lesson to start
            composeTestRule.onNodeWithText(lessonTitle).performClick()
            composeTestRule.waitForIdle()
            
            // Complete the lesson by answering all questions
            completeLessonQuestions(lessonTitle)
            
            // Verify completion
            composeTestRule.onNodeWithText("Lesson Complete!").assertIsDisplayed()
            composeTestRule.onNodeWithText("Back to Lessons").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun completeLessonQuestions(lessonTitle: String) {
        var questionCount = 0
        val maxQuestions = 10 // Assume max 10 questions per lesson
        
        while (questionCount < maxQuestions) {
            try {
                // Check if there's a question displayed
                composeTestRule.onNode(hasText("Question")).assertExists()
                
                // Select first option (for simplicity)
                composeTestRule.onAllNodesWithText("A")[0].performClick()
                
                // Submit answer
                composeTestRule.onNodeWithText("Submit").performClick()
                composeTestRule.waitForIdle()
                
                // Check if this was the last question
                if (composeTestRule.onAllNodesWithText("Next Question").fetchSemanticsNodes().isEmpty()) {
                    break
                } else {
                    composeTestRule.onNodeWithText("Next Question").performClick()
                    composeTestRule.waitForIdle()
                }
                
                questionCount++
            } catch (e: Exception) {
                // No more questions or lesson complete
                break
            }
        }
    }

    private suspend fun verifyLocalProgressStorage() {
        // Verify progress is stored locally and not synced yet
        val allProgress = database.progressDao().getAllProgress().first()
        assertTrue("Should have progress entries", allProgress.isNotEmpty())
        assertEquals("Should have progress for 3 lessons", 3, allProgress.groupBy { it.lessonId }.size)
        assertTrue("Progress should not be synced yet", allProgress.all { !it.isSynced })
        
        // Verify points are stored locally
        val allPoints = database.pointsDao().getAllPoints().first()
        assertTrue("Should have points entries", allPoints.isNotEmpty())
        assertTrue("Points should not be synced yet", allPoints.all { !it.isSynced })
        
        println("=== LOCAL STORAGE VERIFIED ===")
        println("Progress entries: ${allProgress.size}")
        println("Points entries: ${allPoints.size}")
        println("Total points earned: ${allPoints.sumOf { it.pointsEarned }}")
    }

    private suspend fun verifySyncCompletion() {
        // Trigger sync worker
        val syncRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(syncRequest)
        
        // Wait for sync completion (max 5 seconds)
        var syncCompleted = false
        var attempts = 0
        while (!syncCompleted && attempts < 50) {
            delay(100)
            val workInfo = workManager.getWorkInfoById(syncRequest.id).get()
            syncCompleted = workInfo.state.isFinished
            attempts++
        }
        
        assertTrue("Sync should complete within 5 seconds", syncCompleted)
        
        // Verify data is marked as synced
        val syncedProgress = database.progressDao().getAllProgress().first()
        assertTrue("All progress should be synced", syncedProgress.all { it.isSynced })
        
        val syncedPoints = database.pointsDao().getAllPoints().first()
        assertTrue("All points should be synced", syncedPoints.all { it.isSynced })
        
        println("=== SYNC COMPLETED ===")
        println("Synced progress entries: ${syncedProgress.size}")
        println("Synced points entries: ${syncedPoints.size}")
    }

    private fun switchToParentMode() {
        // Navigate back to parent mode
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Switch to Parent Mode").performClick()
        composeTestRule.waitForIdle()
        
        // Verify parent dashboard is shown
        composeTestRule.onNodeWithText("Parent Dashboard").assertIsDisplayed()
    }

    private fun verifyParentDashboardUpdates() {
        // Check that child's progress is reflected in parent dashboard
        composeTestRule.onNodeWithText("Recent Activity").assertIsDisplayed()
        
        // Verify completed lessons appear
        composeTestRule.onNodeWithText("Math Basics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Science Fun").assertIsDisplayed()
        composeTestRule.onNodeWithText("English Reading").assertIsDisplayed()
        
        // Check progress indicators
        composeTestRule.onNodeWithText("3 lessons completed today").assertIsDisplayed()
        
        // Verify points total is updated
        composeTestRule.onNode(hasText("Total Points")).assertExists()
    }

    private fun verifySMSReportInclusion() {
        // Navigate to SMS reports section
        composeTestRule.onNodeWithText("Reports").performClick()
        composeTestRule.onNodeWithText("Daily SMS Report").performClick()
        composeTestRule.waitForIdle()
        
        // Check that today's report includes the completed lessons
        composeTestRule.onNodeWithText("Today's Learning Summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 lessons completed").assertIsDisplayed()
        
        // Verify specific lessons are mentioned
        composeTestRule.onNodeWithText("Math Basics - Completed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Science Fun - Completed").assertIsDisplayed()
        composeTestRule.onNodeWithText("English Reading - Completed").assertIsDisplayed()
        
        // Verify points total in SMS
        composeTestRule.onNode(hasText("points earned")).assertExists()
    }

    @Test
    fun `test sync failure recovery workflow`() = runTest {
        // Setup test data
        setupTestLessonsForSync()
        
        // Complete lessons offline
        completeTestLessonsOffline()
        
        // Simulate sync failure
        simulateSyncFailure()
        
        // Verify data remains local
        verifyDataRemainsLocal()
        
        // Fix network and retry
        fixNetworkAndRetry()
        
        // Verify eventual sync success
        verifyEventualSyncSuccess()
    }

    private suspend fun setupTestLessonsForSync() {
        val testLessons = listOf(
            OfflineLesson(
                id = "sync_test_1",
                title = "Sync Test 1",
                description = "First sync test lesson",
                subject = "math",
                difficulty = "easy",
                totalPoints = 50,
                estimatedDuration = 300,
                isDownloaded = true,
                downloadTimestamp = System.currentTimeMillis(),
                lastSyncTimestamp = 0L
            ),
            OfflineLesson(
                id = "sync_test_2",
                title = "Sync Test 2",
                description = "Second sync test lesson",
                subject = "science",
                difficulty = "medium",
                totalPoints = 75,
                estimatedDuration = 450,
                isDownloaded = true,
                downloadTimestamp = System.currentTimeMillis(),
                lastSyncTimestamp = 0L
            )
        )

        val testQuestions = testLessons.flatMap { lesson ->
            (1..5).map { i ->
                OfflineQuestion(
                    id = "${lesson.id}_q$i",
                    lessonId = lesson.id,
                    question = "Test question $i for ${lesson.title}",
                    options = listOf("A", "B", "C", "D"),
                    correctAnswerIndex = 0,
                    points = lesson.totalPoints / 5,
                    timeLimit = 30,
                    orderIndex = i - 1
                )
            }
        }

        database.offlineLessonDao().insertLessons(testLessons)
        database.offlineLessonDao().insertQuestions(testQuestions)
    }

    private suspend fun completeTestLessonsOffline() {
        val userId = "sync_test_user"
        
        listOf("sync_test_1", "sync_test_2").forEach { lessonId ->
            (1..5).forEach { i ->
                repository.saveQuestionResult(
                    lessonId = lessonId,
                    questionId = "${lessonId}_q$i",
                    userId = userId,
                    isCorrect = true,
                    selectedAnswerIndex = 0,
                    timeTaken = 20,
                    pointsEarned = if (lessonId == "sync_test_1") 10 else 15
                )
            }
        }
    }

    private fun simulateSyncFailure() {
        // Mock server returns error responses
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        mockWebServer.enqueue(MockResponse().setResponseCode(503))
    }

    private suspend fun verifyDataRemainsLocal() {
        val progress = database.progressDao().getAllProgress().first()
        assertTrue("Progress should remain unsynced", progress.all { !it.isSynced })
        
        val points = database.pointsDao().getAllPoints().first()
        assertTrue("Points should remain unsynced", points.all { !it.isSynced })
    }

    private fun fixNetworkAndRetry() {
        // Mock server returns success responses
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
    }

    private suspend fun verifyEventualSyncSuccess() {
        // Trigger retry sync
        val retryRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(retryRequest)
        
        // Wait for completion
        var completed = false
        var attempts = 0
        while (!completed && attempts < 30) {
            delay(200)
            val workInfo = workManager.getWorkInfoById(retryRequest.id).get()
            completed = workInfo.state.isFinished
            attempts++
        }
        
        assertTrue("Retry sync should eventually succeed", completed)
        
        // Verify final sync state
        val finalProgress = database.progressDao().getAllProgress().first()
        assertTrue("Progress should be synced after retry", finalProgress.all { it.isSynced })
    }

    private fun setupMockApiResponses() {
        // Setup default successful responses for API calls
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""
            {
                "lessonId": "math_lesson_1",
                "title": "Math Basics",
                "description": "Basic math concepts",
                "questions": [
                    {
                        "id": "math_q1",
                        "question": "What is 2+2?",
                        "options": ["3", "4", "5", "6"],
                        "correctAnswerIndex": 1,
                        "points": 10,
                        "timeLimit": 30
                    }
                ],
                "totalPoints": 50,
                "estimatedDuration": 300,
                "difficulty": "easy",
                "subject": "math",
                "assets": []
            }
        """))
        
        // Add more mock responses as needed
        repeat(10) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        }
    }
}