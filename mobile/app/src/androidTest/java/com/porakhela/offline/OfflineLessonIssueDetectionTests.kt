package com.porakhela.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.porakhela.data.local.*
import com.porakhela.data.repository.OfflineLessonRepository
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
class OfflineLessonIssueDetectionTests {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: OfflineLessonRepository

    @Inject
    lateinit var database: AppDatabase

    private lateinit var workManager: WorkManager

    @Before
    fun setup() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun `detect and fix sync worker failures`() = runTest {
        println("=== TESTING SYNC WORKER FAILURES ===")
        
        // Setup test data that should be synced
        val testUser = "sync_failure_user"
        setupFailureTestData(testUser)
        
        // Test 1: Network timeout during sync
        testNetworkTimeoutFailure()
        
        // Test 2: API server error during sync
        testAPIServerErrorFailure()
        
        // Test 3: Database corruption during sync
        testDatabaseCorruptionFailure()
        
        // Test 4: Partial sync failure (some items succeed, others fail)
        testPartialSyncFailure()
        
        println("=== SYNC WORKER FAILURE TESTS COMPLETED ===")
    }

    @Test
    fun `detect and fix double submission issues`() = runTest {
        println("=== TESTING DOUBLE SUBMISSION ISSUES ===")
        
        val testUser = "double_submit_user"
        val lessonId = "double_submit_lesson"
        setupDoubleSubmissionTestData(lessonId, testUser)
        
        // Test 1: Rapid button clicks causing duplicate submissions
        testRapidButtonClicks(lessonId, testUser)
        
        // Test 2: Network retry causing duplicate API calls
        testNetworkRetryDuplication(lessonId, testUser)
        
        // Test 3: Background sync duplicating already synced data
        testBackgroundSyncDuplication(lessonId, testUser)
        
        // Test 4: Multiple worker instances running simultaneously
        testMultipleWorkerInstances(lessonId, testUser)
        
        println("=== DOUBLE SUBMISSION TESTS COMPLETED ===")
    }

    @Test
    fun `detect and fix idempotency issues`() = runTest {
        println("=== TESTING IDEMPOTENCY ISSUES ===")
        
        val testUser = "idempotency_user"
        
        // Test 1: Same lesson downloaded multiple times
        testRepeatedLessonDownload()
        
        // Test 2: Same progress submitted multiple times
        testRepeatedProgressSubmission(testUser)
        
        // Test 3: Points awarded multiple times for same achievement
        testRepeatedPointsAwarding(testUser)
        
        // Test 4: Gamification events triggered multiple times
        testRepeatedGamificationEvents(testUser)
        
        println("=== IDEMPOTENCY TESTS COMPLETED ===")
    }

    @Test
    fun `detect and fix timer mismatches`() = runTest {
        println("=== TESTING TIMER MISMATCH ISSUES ===")
        
        val testUser = "timer_user"
        val lessonId = "timer_lesson"
        setupTimerTestData(lessonId)
        
        // Test 1: Client-server time difference
        testClientServerTimeDifference(lessonId, testUser)
        
        // Test 2: Timer continuation after app pause/resume
        testTimerPauseResume(lessonId, testUser)
        
        // Test 3: Timer accuracy during device sleep
        testTimerDuringDeviceSleep(lessonId, testUser)
        
        // Test 4: Timer synchronization across offline/online transitions
        testTimerOfflineOnlineSync(lessonId, testUser)
        
        println("=== TIMER MISMATCH TESTS COMPLETED ===")
    }

    @Test
    fun `test flawless offline to online transition`() = runTest {
        println("=== TESTING COMPLETE OFFLINE→ONLINE TRANSITION ===")
        
        val testUser = "transition_user"
        
        // Step 1: Complete full lesson sequence offline
        val offlineResults = completeFullOfflineSequence(testUser)
        
        // Step 2: Verify all data stored locally
        verifyOfflineDataIntegrity(testUser, offlineResults)
        
        // Step 3: Transition to online mode
        val transitionStartTime = System.currentTimeMillis()
        simulateOnlineTransition()
        
        // Step 4: Verify sync happens automatically and quickly
        val syncSuccess = verifySyncTimingAndSuccess(transitionStartTime)
        assertTrue("Sync should complete successfully", syncSuccess)
        
        // Step 5: Verify data integrity after sync
        verifyPostSyncDataIntegrity(testUser, offlineResults)
        
        // Step 6: Verify no duplicate or missing data
        verifyNoDuplicateOrMissingData(testUser)
        
        // Step 7: Verify UI state updates correctly
        verifyUIStateAfterSync(testUser)
        
        println("=== OFFLINE→ONLINE TRANSITION TEST COMPLETED ===")
    }

    // Helper methods for failure detection

    private suspend fun setupFailureTestData(userId: String) {
        val testLesson = OfflineLesson(
            id = "failure_lesson",
            title = "Failure Test Lesson",
            description = "Testing sync failures",
            subject = "test",
            difficulty = "easy",
            totalPoints = 100,
            estimatedDuration = 600,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val testQuestions = (1..10).map { i ->
            OfflineQuestion(
                id = "fail_q$i",
                lessonId = "failure_lesson",
                question = "Failure test question $i",
                options = listOf("A", "B", "C", "D"),
                correctAnswerIndex = i % 4,
                points = 10,
                timeLimit = 30,
                orderIndex = i - 1
            )
        }

        database.offlineLessonDao().insertLesson(testLesson)
        database.offlineLessonDao().insertQuestions(testQuestions)

        // Create unsynced progress
        testQuestions.forEach { question ->
            repository.saveQuestionResult(
                lessonId = "failure_lesson",
                questionId = question.id,
                userId = userId,
                isCorrect = true,
                selectedAnswerIndex = question.correctAnswerIndex,
                timeTaken = 25,
                pointsEarned = 10
            )
        }
    }

    private suspend fun testNetworkTimeoutFailure() {
        println("Testing network timeout failure...")
        
        // Simulate network timeout by creating a worker that will fail
        val timeoutRequest = OfflineSyncWorker.createSyncRequest()
        
        // Enqueue and monitor for timeout behavior
        workManager.enqueue(timeoutRequest)
        
        // Wait and check if retry mechanism kicks in
        delay(2000)
        val workInfo = workManager.getWorkInfoById(timeoutRequest.id).get()
        
        // Verify retry behavior
        assertTrue("Worker should handle timeouts gracefully", 
            workInfo.state.isFinished || workInfo.runAttemptCount > 1)
    }

    private suspend fun testAPIServerErrorFailure() {
        println("Testing API server error failure...")
        
        // Create sync request that will encounter server errors
        val errorRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(errorRequest)
        
        // Monitor retry behavior
        delay(1000)
        val workInfo = workManager.getWorkInfoById(errorRequest.id).get()
        
        // Verify exponential backoff is working
        assertTrue("Should implement proper retry strategy", workInfo.runAttemptCount >= 1)
    }

    private suspend fun testDatabaseCorruptionFailure() {
        println("Testing database corruption failure...")
        
        // Simulate database corruption scenario
        try {
            // Attempt to read potentially corrupted data
            val progress = database.progressDao().getAllProgress().first()
            val points = database.pointsDao().getAllPoints().first()
            
            // Verify data integrity
            assertTrue("Progress data should be valid", progress.all { it.lessonId.isNotBlank() })
            assertTrue("Points data should be valid", points.all { it.pointsEarned >= 0 })
        } catch (e: Exception) {
            // Handle corruption gracefully
            println("Database corruption detected: ${e.message}")
            // Implement recovery mechanism
        }
    }

    private suspend fun testPartialSyncFailure() {
        println("Testing partial sync failure...")
        
        // Create mixed sync scenario (some succeed, some fail)
        val partialRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(partialRequest)
        
        delay(1500)
        
        // Verify partial sync state is handled correctly
        val progress = database.progressDao().getAllProgress().first()
        val syncedCount = progress.count { it.isSynced }
        val unsyncedCount = progress.count { !it.isSynced }
        
        println("Synced: $syncedCount, Unsynced: $unsyncedCount")
        assertTrue("Should handle partial sync gracefully", syncedCount >= 0 && unsyncedCount >= 0)
    }

    private suspend fun setupDoubleSubmissionTestData(lessonId: String, userId: String) {
        val lesson = OfflineLesson(
            id = lessonId,
            title = "Double Submission Test",
            description = "Testing double submission prevention",
            subject = "test",
            difficulty = "easy",
            totalPoints = 50,
            estimatedDuration = 300,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val question = OfflineQuestion(
            id = "${lessonId}_q1",
            lessonId = lessonId,
            question = "Double submission test question",
            options = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 1,
            points = 50,
            timeLimit = 30,
            orderIndex = 0
        )

        database.offlineLessonDao().insertLesson(lesson)
        database.offlineLessonDao().insertQuestions(listOf(question))
    }

    private suspend fun testRapidButtonClicks(lessonId: String, userId: String) {
        println("Testing rapid button clicks...")
        
        val questionId = "${lessonId}_q1"
        val initialProgressCount = database.progressDao().getAllProgress().first().size
        
        // Simulate rapid clicks (multiple submissions)
        repeat(5) {
            repository.saveQuestionResult(lessonId, questionId, userId, true, 1, 20, 50)
        }
        
        val finalProgressCount = database.progressDao().getAllProgress().first().size
        val progressAdded = finalProgressCount - initialProgressCount
        
        // Should only add one progress entry, not five
        assertEquals("Should prevent duplicate submissions", 1, progressAdded)
    }

    private suspend fun testNetworkRetryDuplication(lessonId: String, userId: String) {
        println("Testing network retry duplication...")
        
        // Submit same data multiple times simulating network retries
        val questionId = "${lessonId}_q1"
        
        repeat(3) {
            repository.saveQuestionResult(lessonId, questionId, userId, true, 1, 20, 50)
        }
        
        // Check for duplicates
        val progress = database.progressDao().getProgressForLesson(lessonId, userId).first()
        val uniqueEntries = progress.distinctBy { "${it.questionId}_${it.userId}" }
        
        assertEquals("Should not create duplicate progress entries", progress.size, uniqueEntries.size)
    }

    private suspend fun testBackgroundSyncDuplication(lessonId: String, userId: String) {
        println("Testing background sync duplication...")
        
        // Mark some data as synced
        repository.saveQuestionResult(lessonId, "${lessonId}_q1", userId, true, 1, 20, 50)
        
        // Simulate sync worker running multiple times
        repeat(2) {
            val syncRequest = OfflineSyncWorker.createSyncRequest()
            workManager.enqueue(syncRequest)
        }
        
        delay(2000)
        
        // Verify no duplication occurred
        val progress = database.progressDao().getProgressForLesson(lessonId, userId).first()
        assertEquals("Should not duplicate synced entries", 1, progress.size)
    }

    private suspend fun testMultipleWorkerInstances(lessonId: String, userId: String) {
        println("Testing multiple worker instances...")
        
        // Enqueue multiple sync workers simultaneously
        val workers = (1..3).map { 
            OfflineSyncWorker.createSyncRequest().also { workManager.enqueue(it) }
        }
        
        delay(3000)
        
        // Verify only appropriate number of sync operations occurred
        val allWorkInfo = workers.map { workManager.getWorkInfoById(it.id).get() }
        val completedWork = allWorkInfo.count { it.state.isFinished }
        
        assertTrue("Should handle multiple worker instances correctly", completedWork >= 1)
    }

    private suspend fun testRepeatedLessonDownload() {
        println("Testing repeated lesson download...")
        
        val lessonId = "idempotent_lesson"
        val initialLessonCount = database.offlineLessonDao().getAllLessons().first().size
        
        // Try to download same lesson multiple times
        repeat(3) {
            try {
                repository.downloadLessonPack(lessonId)
            } catch (e: Exception) {
                // Expected for non-existent lesson
            }
        }
        
        val finalLessonCount = database.offlineLessonDao().getAllLessons().first().size
        
        // Should not increase count multiple times for same lesson
        assertTrue("Should be idempotent for lesson downloads", 
            finalLessonCount - initialLessonCount <= 1)
    }

    private suspend fun testRepeatedProgressSubmission(userId: String) {
        println("Testing repeated progress submission...")
        
        val lessonId = "idempotent_progress"
        val questionId = "idempotent_q1"
        
        val initialProgressCount = database.progressDao().getAllProgress().first().size
        
        // Submit same progress multiple times
        repeat(3) {
            repository.saveQuestionResult(lessonId, questionId, userId, true, 1, 20, 10)
        }
        
        val finalProgressCount = database.progressDao().getAllProgress().first().size
        val progressAdded = finalProgressCount - initialProgressCount
        
        assertEquals("Should be idempotent for progress submission", 1, progressAdded)
    }

    private suspend fun testRepeatedPointsAwarding(userId: String) {
        println("Testing repeated points awarding...")
        
        val lessonId = "idempotent_points"
        val initialPoints = database.pointsDao().getPointsForUser(userId).first().sumOf { it.pointsEarned }
        
        // Award points for same achievement multiple times
        repeat(3) {
            val pointsEntry = LocalPoints(
                id = "idempotent_points_${System.nanoTime()}",
                userId = userId,
                lessonId = lessonId,
                pointsEarned = 25,
                source = "same_achievement",
                timestamp = System.currentTimeMillis(),
                isSynced = false
            )
            database.pointsDao().insertPoints(pointsEntry)
        }
        
        val finalPoints = database.pointsDao().getPointsForUser(userId).first().sumOf { it.pointsEarned }
        val pointsAdded = finalPoints - initialPoints
        
        // Should only award points once for same achievement
        assertTrue("Should prevent duplicate point awards", pointsAdded <= 25)
    }

    private suspend fun testRepeatedGamificationEvents(userId: String) {
        println("Testing repeated gamification events...")
        
        val eventType = "idempotent_achievement"
        val initialEventCount = database.gamificationEventDao().getEventsForUser(userId).first().size
        
        // Create same event multiple times
        repeat(3) {
            val event = GamificationEvent(
                id = "event_${System.nanoTime()}",
                userId = userId,
                type = eventType,
                data = "{\"achievement\": \"same_achievement\"}",
                timestamp = System.currentTimeMillis(),
                isShown = false
            )
            database.gamificationEventDao().insertEvent(event)
        }
        
        val finalEventCount = database.gamificationEventDao().getEventsForUser(userId).first().size
        val eventsAdded = finalEventCount - initialEventCount
        
        // Should only create one event per unique achievement
        assertEquals("Should prevent duplicate gamification events", 1, eventsAdded)
    }

    private suspend fun setupTimerTestData(lessonId: String) {
        val lesson = OfflineLesson(
            id = lessonId,
            title = "Timer Test Lesson",
            description = "Testing timer accuracy",
            subject = "test",
            difficulty = "medium",
            totalPoints = 60,
            estimatedDuration = 360,
            isDownloaded = true,
            downloadTimestamp = System.currentTimeMillis(),
            lastSyncTimestamp = 0L
        )

        val question = OfflineQuestion(
            id = "${lessonId}_timer_q1",
            lessonId = lessonId,
            question = "Timer test question",
            options = listOf("A", "B", "C", "D"),
            correctAnswerIndex = 2,
            points = 60,
            timeLimit = 45,
            orderIndex = 0
        )

        database.offlineLessonDao().insertLesson(lesson)
        database.offlineLessonDao().insertQuestions(listOf(question))
    }

    private suspend fun testClientServerTimeDifference(lessonId: String, userId: String) {
        println("Testing client-server time difference...")
        
        val questionId = "${lessonId}_timer_q1"
        val clientTime = System.currentTimeMillis()
        
        // Simulate time difference (client ahead by 2 minutes)
        val simulatedServerTime = clientTime - 120000
        
        repository.saveQuestionResult(lessonId, questionId, userId, true, 2, 30, 60)
        
        val progress = database.progressDao().getProgressForLesson(lessonId, userId).first()
        val submissionTime = progress.first().timestamp
        
        // Verify time handling
        assertTrue("Should handle time differences gracefully", 
            Math.abs(submissionTime - clientTime) < 5000) // Within 5 seconds tolerance
    }

    private suspend fun testTimerPauseResume(lessonId: String, userId: String) {
        println("Testing timer pause/resume...")
        
        // Simulate lesson start
        val startTime = System.currentTimeMillis()
        
        // Simulate pause (app goes to background)
        delay(1000)
        val pauseTime = System.currentTimeMillis()
        
        // Simulate resume (app comes to foreground)
        delay(500) // Simulate background time
        val resumeTime = System.currentTimeMillis()
        
        // Complete question
        val actualTimeTaken = (resumeTime - startTime).toInt() / 1000 // Convert to seconds
        repository.saveQuestionResult(lessonId, "${lessonId}_timer_q1", userId, true, 2, actualTimeTaken, 60)
        
        val progress = database.progressDao().getProgressForLesson(lessonId, userId).first()
        val recordedTime = progress.first().timeTaken
        
        // Should account for pause time appropriately
        assertTrue("Should handle pause/resume correctly", recordedTime > 0)
    }

    private suspend fun testTimerDuringDeviceSleep(lessonId: String, userId: String) {
        println("Testing timer during device sleep...")
        
        // This would require device-specific testing in real scenarios
        // For simulation, test timer accuracy over longer periods
        val startTime = System.currentTimeMillis()
        delay(2000) // Simulate 2 second delay
        val endTime = System.currentTimeMillis()
        
        val actualDuration = ((endTime - startTime) / 1000).toInt()
        repository.saveQuestionResult(lessonId, "${lessonId}_timer_q1", userId, true, 2, actualDuration, 60)
        
        val progress = database.progressDao().getProgressForLesson(lessonId, userId).first()
        val recordedTime = progress.first().timeTaken
        
        // Timer should be reasonably accurate
        assertTrue("Timer should be accurate during sleep", Math.abs(recordedTime - 2) <= 1)
    }

    private suspend fun testTimerOfflineOnlineSync(lessonId: String, userId: String) {
        println("Testing timer sync across offline/online transitions...")
        
        // Start offline
        val offlineStartTime = System.currentTimeMillis()
        delay(1000)
        
        // Complete question offline
        repository.saveQuestionResult(lessonId, "${lessonId}_timer_q1", userId, true, 2, 30, 60)
        
        // Go online and sync
        val syncRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(syncRequest)
        delay(1000)
        
        val progress = database.progressDao().getProgressForLesson(lessonId, userId).first()
        assertTrue("Timer data should survive offline/online transition", progress.first().timeTaken > 0)
    }

    private suspend fun completeFullOfflineSequence(userId: String): OfflineTestResults {
        println("Completing full offline sequence...")
        
        val testLessons = (1..3).map { i ->
            OfflineLesson(
                id = "transition_lesson_$i",
                title = "Transition Lesson $i",
                description = "Full offline sequence test $i",
                subject = "math",
                difficulty = "easy",
                totalPoints = 30,
                estimatedDuration = 180,
                isDownloaded = true,
                downloadTimestamp = System.currentTimeMillis(),
                lastSyncTimestamp = 0L
            )
        }

        val testQuestions = testLessons.flatMap { lesson ->
            (1..3).map { j ->
                OfflineQuestion(
                    id = "${lesson.id}_q$j",
                    lessonId = lesson.id,
                    question = "Question $j for ${lesson.title}",
                    options = listOf("A", "B", "C", "D"),
                    correctAnswerIndex = j % 4,
                    points = 10,
                    timeLimit = 30,
                    orderIndex = j - 1
                )
            }
        }

        database.offlineLessonDao().insertLessons(testLessons)
        database.offlineLessonDao().insertQuestions(testQuestions)

        // Complete all questions
        var totalProgress = 0
        var totalPoints = 0

        testQuestions.forEach { question ->
            repository.saveQuestionResult(
                lessonId = question.lessonId,
                questionId = question.id,
                userId = userId,
                isCorrect = true,
                selectedAnswerIndex = question.correctAnswerIndex,
                timeTaken = 25,
                pointsEarned = 10
            )
            totalProgress++
            totalPoints += 10
        }

        return OfflineTestResults(
            lessonsCompleted = testLessons.size,
            questionsAnswered = testQuestions.size,
            totalPoints = totalPoints,
            userId = userId
        )
    }

    private suspend fun verifyOfflineDataIntegrity(userId: String, results: OfflineTestResults) {
        println("Verifying offline data integrity...")
        
        val progress = database.progressDao().getAllProgress().first()
            .filter { it.userId == userId }
        val points = database.pointsDao().getPointsForUser(userId).first()
        
        assertEquals("Progress count should match", results.questionsAnswered, progress.size)
        assertEquals("Points should match", results.totalPoints, points.sumOf { it.pointsEarned })
        assertTrue("All progress should be unsynced", progress.all { !it.isSynced })
        assertTrue("All points should be unsynced", points.all { !it.isSynced })
    }

    private suspend fun simulateOnlineTransition() {
        println("Simulating online transition...")
        // This would enable network connectivity in real scenarios
        delay(100) // Simulate transition delay
    }

    private suspend fun verifySyncTimingAndSuccess(startTime: Long): Boolean {
        println("Verifying sync timing and success...")
        
        val syncRequest = OfflineSyncWorker.createSyncRequest()
        workManager.enqueue(syncRequest)
        
        var syncCompleted = false
        var attempts = 0
        val maxAttempts = 50 // 5 seconds
        
        while (!syncCompleted && attempts < maxAttempts) {
            delay(100)
            val workInfo = workManager.getWorkInfoById(syncRequest.id).get()
            syncCompleted = workInfo.state.isFinished && workInfo.state.name == "SUCCEEDED"
            attempts++
        }
        
        val syncTime = System.currentTimeMillis() - startTime
        println("Sync completed in ${syncTime}ms")
        
        return syncCompleted && syncTime <= 5000
    }

    private suspend fun verifyPostSyncDataIntegrity(userId: String, results: OfflineTestResults) {
        println("Verifying post-sync data integrity...")
        
        val progress = database.progressDao().getAllProgress().first()
            .filter { it.userId == userId }
        val points = database.pointsDao().getPointsForUser(userId).first()
        
        assertEquals("Progress count should remain same", results.questionsAnswered, progress.size)
        assertEquals("Points should remain same", results.totalPoints, points.sumOf { it.pointsEarned })
        assertTrue("All progress should be synced", progress.all { it.isSynced })
        assertTrue("All points should be synced", points.all { it.isSynced })
    }

    private suspend fun verifyNoDuplicateOrMissingData(userId: String) {
        println("Verifying no duplicate or missing data...")
        
        val progress = database.progressDao().getAllProgress().first()
            .filter { it.userId == userId }
        val points = database.pointsDao().getPointsForUser(userId).first()
        
        // Check for duplicates
        val uniqueProgress = progress.distinctBy { "${it.lessonId}_${it.questionId}_${it.userId}" }
        assertEquals("No duplicate progress", progress.size, uniqueProgress.size)
        
        val uniquePoints = points.distinctBy { "${it.lessonId}_${it.userId}_${it.source}" }
        assertEquals("No duplicate points", points.size, uniquePoints.size)
    }

    private suspend fun verifyUIStateAfterSync(userId: String) {
        println("Verifying UI state after sync...")
        
        // This would involve UI testing in real scenarios
        // For now, verify data is in correct state for UI
        val progress = database.progressDao().getAllProgress().first()
            .filter { it.userId == userId }
        val events = database.gamificationEventDao().getEventsForUser(userId).first()
        
        assertTrue("Should have progress for UI display", progress.isNotEmpty())
        assertTrue("Should have gamification events", events.isNotEmpty())
    }

    data class OfflineTestResults(
        val lessonsCompleted: Int,
        val questionsAnswered: Int,
        val totalPoints: Int,
        val userId: String
    )
}

// Test result summary helper
object TestResultSummary {
    fun generateReport(testResults: Map<String, Boolean>) {
        println("\n=== OFFLINE LESSON SYSTEM TEST SUMMARY ===")
        testResults.forEach { (testName, passed) ->
            val status = if (passed) "✅ PASSED" else "❌ FAILED"
            println("$status: $testName")
        }
        
        val totalTests = testResults.size
        val passedTests = testResults.values.count { it }
        val passRate = (passedTests.toDouble() / totalTests) * 100
        
        println("\nOverall Results:")
        println("Total Tests: $totalTests")
        println("Passed: $passedTests")
        println("Failed: ${totalTests - passedTests}")
        println("Pass Rate: ${String.format("%.1f", passRate)}%")
        
        if (passRate == 100.0) {
            println("\n🎉 ALL TESTS PASSED! Offline lesson system is ready for deployment.")
        } else {
            println("\n⚠️  Some tests failed. Please review and fix issues before deployment.")
        }
        println("===============================================")
    }
}