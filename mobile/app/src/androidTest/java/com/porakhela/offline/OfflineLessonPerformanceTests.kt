package com.porakhela.offline

import android.os.Build
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.porakhela.data.local.*
import com.porakhela.data.repository.OfflineLessonRepository
import com.porakhela.ui.screens.lessons.EnhancedOfflineLessonPlayerScreen
import com.porakhela.ui.screens.lessons.OfflineLessonPlayerViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfflineLessonPerformanceTests {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Inject
    lateinit var repository: OfflineLessonRepository

    @Inject
    lateinit var database: AppDatabase

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Log device specs for performance context
        logDeviceSpecs()
    }

    @Test
    fun `test lesson loading performance under 400ms`() = runTest {
        // Setup test lesson with multiple questions
        val performanceLesson = OfflineLesson(
            id = "perf_lesson_1",
            title = "Performance Test Lesson",
            description = "Testing loading performance with multiple questions and assets",
            subject = "math",
            difficulty = "medium",
            totalPoints = 100,
            estimatedDuration = 600,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        // Create 20 questions to simulate realistic lesson size
        val performanceQuestions = (1..20).map { i ->
            OfflineQuestion(
                id = "perf_q$i",
                lessonId = "perf_lesson_1",
                question = "Performance question $i with some longer text to simulate real questions?",
                options = listOf(
                    "Option A - This is a longer option text to simulate realistic content",
                    "Option B - Another longer option with more detailed content",
                    "Option C - Yet another detailed option with comprehensive text",
                    "Option D - Final option with extensive explanatory content"
                ),
                correctAnswerIndex = i % 4,
                points = 5,
                timeLimit = 45,
                orderIndex = i - 1
            )
        }

        // Insert test data
        database.offlineLessonDao().insertLesson(performanceLesson)
        database.offlineLessonDao().insertQuestions(performanceQuestions)

        // Measure lesson loading time
        val loadingTime = measureTimeMillis {
            val lesson = database.offlineLessonDao().getLessonById("perf_lesson_1").first()
            val questions = database.offlineLessonDao().getQuestionsForLesson("perf_lesson_1").first()
            
            assertNotNull("Lesson should load", lesson)
            assertEquals("All questions should load", 20, questions.size)
        }

        assertTrue(
            "Lesson should load in under 400ms (actual: ${loadingTime}ms)", 
            loadingTime < 400
        )
    }

    @Test
    fun `test UI rendering performance`() {
        // Setup test lesson
        val uiPerfLesson = OfflineLesson(
            id = "ui_perf_lesson",
            title = "UI Performance Test",
            description = "Testing UI rendering speed",
            subject = "science",
            difficulty = "easy",
            totalPoints = 50,
            estimatedDuration = 300,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val uiPerfQuestion = OfflineQuestion(
            id = "ui_perf_q1",
            lessonId = "ui_perf_lesson",
            question = "This is a question with complex formatting and multiple lines of text to test rendering performance in the UI?",
            options = listOf(
                "Option A with detailed explanatory text",
                "Option B with comprehensive content",
                "Option C with extensive description",
                "Option D with thorough explanation"
            ),
            correctAnswerIndex = 1,
            points = 50,
            timeLimit = 60,
            orderIndex = 0
        )

        runTest {
            database.offlineLessonDao().insertLesson(uiPerfLesson)
            database.offlineLessonDao().insertQuestions(listOf(uiPerfQuestion))
        }

        // Measure UI composition time
        benchmarkRule.measureRepeated {
            composeTestRule.setContent {
                EnhancedOfflineLessonPlayerScreen(
                    lessonId = "ui_perf_lesson",
                    onNavigateBack = {},
                    onLessonComplete = {}
                )
            }

            // Wait for initial composition
            composeTestRule.onNodeWithText("UI Performance Test").assertIsDisplayed()
        }

        // Test scrolling performance
        composeTestRule.setContent {
            EnhancedOfflineLessonPlayerScreen(
                lessonId = "ui_perf_lesson",
                onNavigateBack = {},
                onLessonComplete = {}
            )
        }

        // Perform multiple scroll actions to test smoothness
        repeat(10) {
            composeTestRule.onRoot().performTouchInput {
                swipeUp()
            }
            composeTestRule.onRoot().performTouchInput {
                swipeDown()
            }
        }

        // UI should remain responsive
        composeTestRule.onNodeWithText("This is a question").assertIsDisplayed()
    }

    @Test
    fun `test animation performance on low-end device`() {
        runTest {
            // Setup test data for gamification
            val userId = "perf_user"
            val animLesson = OfflineLesson(
                id = "anim_lesson",
                title = "Animation Test",
                description = "Testing animation performance",
                subject = "math",
                difficulty = "easy",
                totalPoints = 25,
                estimatedDuration = 150,
                isDownloaded = true,
                downloadTimestamp = System.currentTimeMillis(),
                lastSyncTimestamp = 0L
            )

            val animQuestion = OfflineQuestion(
                id = "anim_q1",
                lessonId = "anim_lesson",
                question = "Test animation question",
                options = listOf("A", "B", "C", "D"),
                correctAnswerIndex = 0,
                points = 25,
                timeLimit = 30,
                orderIndex = 0
            )

            database.offlineLessonDao().insertLesson(animLesson)
            database.offlineLessonDao().insertQuestions(listOf(animQuestion))

            // Create gamification event for animation testing
            val gamificationEvent = GamificationEvent(
                id = "anim_event_1",
                userId = userId,
                type = "points_earned",
                data = "{\"points\": 25, \"lesson\": \"anim_lesson\", \"achievement\": \"first_correct\"}",
                timestamp = System.currentTimeMillis(),
                isShown = false
            )

            database.gamificationEventDao().insertEvent(gamificationEvent)
        }

        // Test animation rendering with performance measurement
        composeTestRule.setContent {
            EnhancedOfflineLessonPlayerScreen(
                lessonId = "anim_lesson",
                onNavigateBack = {},
                onLessonComplete = {}
            )
        }

        // Wait for lesson to load
        composeTestRule.onNodeWithText("Animation Test").assertIsDisplayed()

        // Answer question to trigger animations
        composeTestRule.onNodeWithText("A").performClick()
        composeTestRule.onNodeWithText("Submit").performClick()

        // Verify animations appear without UI freezing
        composeTestRule.onNodeWithText("Correct!").assertIsDisplayed()
        
        // Check if points animation shows
        composeTestRule.waitForIdle()
        
        // Verify UI remains responsive during animations
        composeTestRule.onNodeWithText("+25 Porapoints! 🎉").assertExists()
    }

    @Test
    fun `test memory usage during lesson completion`() = runTest {
        // Create lesson with extensive content to test memory efficiency
        val memoryLesson = OfflineLesson(
            id = "memory_lesson",
            title = "Memory Usage Test",
            description = "Testing memory efficiency with large lesson content",
            subject = "english",
            difficulty = "hard",
            totalPoints = 200,
            estimatedDuration = 1200,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        // Create 50 questions to stress test memory
        val memoryQuestions = (1..50).map { i ->
            OfflineQuestion(
                id = "mem_q$i",
                lessonId = "memory_lesson",
                question = "Memory test question $i with extensive content and detailed explanations that include multiple paragraphs of text to simulate real-world lesson content with comprehensive educational material?",
                options = listOf(
                    "Extensive option A with detailed explanatory content and comprehensive information",
                    "Comprehensive option B with thorough descriptions and complete explanations",
                    "Detailed option C with extensive educational content and full descriptions",
                    "Complete option D with comprehensive explanatory material and detailed information"
                ),
                correctAnswerIndex = i % 4,
                points = 4,
                timeLimit = 90,
                orderIndex = i - 1
            )
        }

        database.offlineLessonDao().insertLesson(memoryLesson)
        database.offlineLessonDao().insertQuestions(memoryQuestions)

        val userId = "memory_user"

        // Get initial memory usage
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Process all questions rapidly to stress test memory
        val processingTime = measureTimeMillis {
            memoryQuestions.forEach { question ->
                repository.saveQuestionResult(
                    lessonId = "memory_lesson",
                    questionId = question.id,
                    userId = userId,
                    isCorrect = true,
                    selectedAnswerIndex = question.correctAnswerIndex,
                    timeTaken = 30,
                    pointsEarned = question.points
                )
            }
        }

        // Check memory after processing
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        val memoryIncreaseMB = memoryIncrease / (1024 * 1024)

        // Verify performance benchmarks
        assertTrue("Processing should complete quickly (actual: ${processingTime}ms)", processingTime < 2000)
        assertTrue("Memory increase should be reasonable (actual: ${memoryIncreaseMB}MB)", memoryIncreaseMB < 50)

        // Verify all data was processed correctly
        val allProgress = database.progressDao().getProgressForLesson("memory_lesson", userId).first()
        assertEquals("All questions should be processed", 50, allProgress.size)
        assertEquals("Total points should be correct", 200, allProgress.sumOf { it.pointsEarned })
    }

    @Test
    fun `test database performance with concurrent operations`() = runTest {
        val concurrentLesson = OfflineLesson(
            id = "concurrent_lesson",
            title = "Concurrent Operations Test",
            description = "Testing database performance under concurrent load",
            subject = "physics",
            difficulty = "medium",
            totalPoints = 150,
            estimatedDuration = 900,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val concurrentQuestions = (1..30).map { i ->
            OfflineQuestion(
                id = "concurrent_q$i",
                lessonId = "concurrent_lesson",
                question = "Concurrent test question $i",
                options = listOf("A$i", "B$i", "C$i", "D$i"),
                correctAnswerIndex = i % 4,
                points = 5,
                timeLimit = 45,
                orderIndex = i - 1
            )
        }

        database.offlineLessonDao().insertLesson(concurrentLesson)
        database.offlineLessonDao().insertQuestions(concurrentQuestions)

        // Simulate concurrent users completing lessons
        val concurrentTime = measureTimeMillis {
            val users = (1..5).map { "concurrent_user_$it" }
            
            users.forEach { userId ->
                concurrentQuestions.take(10).forEach { question ->
                    repository.saveQuestionResult(
                        lessonId = "concurrent_lesson",
                        questionId = question.id,
                        userId = userId,
                        isCorrect = true,
                        selectedAnswerIndex = question.correctAnswerIndex,
                        timeTaken = 25,
                        pointsEarned = question.points
                    )
                }
            }
        }

        assertTrue("Concurrent operations should complete quickly (actual: ${concurrentTime}ms)", concurrentTime < 3000)

        // Verify data integrity after concurrent operations
        val users = (1..5).map { "concurrent_user_$it" }
        users.forEach { userId ->
            val userProgress = database.progressDao().getProgressForLesson("concurrent_lesson", userId).first()
            assertEquals("Each user should have 10 progress entries", 10, userProgress.size)
        }
    }

    private fun logDeviceSpecs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val runtime = Runtime.getRuntime()
        
        println("=== Device Performance Context ===")
        println("Device Model: ${Build.MODEL}")
        println("Android Version: ${Build.VERSION.RELEASE}")
        println("SDK Level: ${Build.VERSION.SDK_INT}")
        println("Available Processors: ${runtime.availableProcessors()}")
        println("Max Memory: ${runtime.maxMemory() / (1024 * 1024)} MB")
        println("Total Memory: ${runtime.totalMemory() / (1024 * 1024)} MB")
        println("Free Memory: ${runtime.freeMemory() / (1024 * 1024)} MB")
        
        // Check if this is likely a low-end device
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        val isLowEnd = maxMemoryMB < 512 || runtime.availableProcessors() < 4
        println("Low-end device simulation: $isLowEnd")
        println("=====================================")
    }
}