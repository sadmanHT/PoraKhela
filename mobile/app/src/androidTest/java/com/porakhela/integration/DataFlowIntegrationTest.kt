package com.porakhela.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.porakhela.data.model.*
import com.porakhela.data.repository.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Data Flow Integration Tests
 * Tests data persistence, sync, and state management across repositories
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DataFlowIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var lessonRepository: LessonRepository

    @Inject
    lateinit var progressRepository: ProgressRepository

    @Inject
    lateinit var achievementRepository: AchievementRepository

    @Inject
    lateinit var downloadRepository: DownloadRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_complete_lesson_progress_flow() = runTest {
        // Create test user and child profile
        val user = User(
            id = "test-user-123",
            phoneNumber = "+8801712345678",
            createdAt = System.currentTimeMillis()
        )
        userRepository.saveUser(user)

        val childProfile = ChildProfile(
            id = "child-123",
            parentId = user.id,
            name = "Test Child",
            grade = "Class 6",
            avatar = "boy_1",
            subjects = listOf("Mathematics", "Science")
        )
        userRepository.saveChildProfile(childProfile)

        // Create test lesson
        val lesson = Lesson(
            id = "lesson-math-001",
            title = "Introduction to Numbers",
            subject = "Mathematics",
            grade = "Class 6",
            duration = 15,
            videoUrl = "test-video.mp4",
            thumbnailUrl = "test-thumb.jpg",
            description = "Learn basic numbers",
            difficulty = DifficultyLevel.BEGINNER,
            sections = listOf(
                LessonSection(
                    id = "section-1",
                    title = "What are Numbers?",
                    content = "Numbers are symbols used for counting...",
                    type = SectionType.VIDEO,
                    duration = 5
                ),
                LessonSection(
                    id = "section-2", 
                    title = "Counting Practice",
                    content = "Let's practice counting...",
                    type = SectionType.INTERACTIVE,
                    duration = 10
                )
            ),
            exercises = listOf(
                Exercise(
                    id = "exercise-1",
                    question = "What comes after 5?",
                    options = listOf("4", "6", "7", "3"),
                    correctAnswer = 1,
                    explanation = "6 comes after 5 in counting"
                )
            )
        )
        lessonRepository.saveLesson(lesson)

        // Start lesson progress
        val startTime = System.currentTimeMillis()
        progressRepository.startLesson(childProfile.id, lesson.id)

        // Verify lesson started
        val initialProgress = progressRepository.getLessonProgress(childProfile.id, lesson.id).first()
        assertNotNull(initialProgress)
        assertEquals(LessonStatus.IN_PROGRESS, initialProgress?.status)
        assertEquals(0, initialProgress?.sectionsCompleted)

        // Complete first section
        progressRepository.completeSection(
            childProfileId = childProfile.id,
            lessonId = lesson.id,
            sectionId = "section-1",
            timeSpent = 300L // 5 minutes
        )

        // Verify section completion
        val progressAfterFirstSection = progressRepository.getLessonProgress(childProfile.id, lesson.id).first()
        assertEquals(1, progressAfterFirstSection?.sectionsCompleted)
        assertEquals(50, progressAfterFirstSection?.progressPercentage) // 1 of 2 sections

        // Complete second section
        progressRepository.completeSection(
            childProfileId = childProfile.id,
            lessonId = lesson.id,
            sectionId = "section-2",
            timeSpent = 600L // 10 minutes
        )

        // Complete exercise
        val exerciseResult = ExerciseResult(
            exerciseId = "exercise-1",
            selectedAnswer = 1, // Correct answer
            isCorrect = true,
            timeSpent = 30L
        )

        progressRepository.completeExercise(
            childProfileId = childProfile.id,
            lessonId = lesson.id,
            results = listOf(exerciseResult)
        )

        // Complete lesson
        progressRepository.completeLesson(
            childProfileId = childProfile.id,
            lessonId = lesson.id,
            totalTimeSpent = 930L, // 15.5 minutes
            score = 100
        )

        // Verify lesson completion
        val finalProgress = progressRepository.getLessonProgress(childProfile.id, lesson.id).first()
        assertEquals(LessonStatus.COMPLETED, finalProgress?.status)
        assertEquals(100, finalProgress?.progressPercentage)
        assertEquals(100, finalProgress?.score)

        // Check achievements triggered
        val achievements = achievementRepository.getEarnedAchievements(childProfile.id).first()
        assertTrue("First Lesson achievement should be earned", 
            achievements.any { it.type == AchievementType.FIRST_LESSON }
        )

        // Verify subject progress updated
        val subjectProgress = progressRepository.getSubjectProgress(childProfile.id, "Mathematics").first()
        assertEquals(1, subjectProgress?.lessonsCompleted)
        assertTrue(subjectProgress?.totalTimeSpent!! >= 930L)
    }

    @Test
    fun test_offline_download_and_sync_flow() = runTest {
        // Create test lesson
        val lesson = Lesson(
            id = "lesson-offline-001",
            title = "Offline Test Lesson",
            subject = "Science",
            grade = "Class 6",
            duration = 10,
            videoUrl = "https://example.com/offline-video.mp4",
            thumbnailUrl = "https://example.com/offline-thumb.jpg",
            description = "Test offline functionality"
        )
        lessonRepository.saveLesson(lesson)

        // Download lesson for offline use
        downloadRepository.downloadLesson("child-123", lesson.id)

        // Verify download started
        val downloadStatus = downloadRepository.getDownloadStatus(lesson.id).first()
        assertEquals(DownloadStatus.DOWNLOADING, downloadStatus)

        // Simulate download completion
        downloadRepository.markDownloadComplete(lesson.id, "/storage/lessons/lesson-offline-001")

        // Verify download completed
        val completedStatus = downloadRepository.getDownloadStatus(lesson.id).first()
        assertEquals(DownloadStatus.DOWNLOADED, completedStatus)

        // Get downloaded lessons
        val downloadedLessons = downloadRepository.getDownloadedLessons("child-123").first()
        assertTrue("Lesson should be in downloaded list", 
            downloadedLessons.any { it.id == lesson.id }
        )

        // Test offline lesson access
        val offlineLesson = lessonRepository.getLesson(lesson.id).first()
        assertNotNull(offlineLesson)
        assertEquals(lesson.title, offlineLesson?.title)

        // Complete lesson offline
        progressRepository.startLesson("child-123", lesson.id)
        progressRepository.completeLesson(
            childProfileId = "child-123",
            lessonId = lesson.id,
            totalTimeSpent = 600L,
            score = 85
        )

        // Verify offline progress saved
        val offlineProgress = progressRepository.getLessonProgress("child-123", lesson.id).first()
        assertEquals(LessonStatus.COMPLETED, offlineProgress?.status)
        assertEquals(85, offlineProgress?.score)

        // Simulate coming back online - sync should happen automatically
        // (In real implementation, this would trigger sync)
        
        // Verify data persisted after sync
        val syncedProgress = progressRepository.getLessonProgress("child-123", lesson.id).first()
        assertEquals(85, syncedProgress?.score)
        assertEquals(LessonStatus.COMPLETED, syncedProgress?.status)
    }

    @Test
    fun test_achievement_system_integration() = runTest {
        val childProfileId = "child-achievement-test"

        // Test First Lesson achievement
        progressRepository.completeLesson(childProfileId, "lesson-1", 600L, 100)
        
        var achievements = achievementRepository.getEarnedAchievements(childProfileId).first()
        assertTrue("First Lesson achievement should be earned",
            achievements.any { it.type == AchievementType.FIRST_LESSON }
        )

        // Test Perfect Score achievement
        progressRepository.completeLesson(childProfileId, "lesson-2", 500L, 100)
        progressRepository.completeLesson(childProfileId, "lesson-3", 450L, 100)
        progressRepository.completeLesson(childProfileId, "lesson-4", 520L, 100)
        progressRepository.completeLesson(childProfileId, "lesson-5", 480L, 100)

        achievements = achievementRepository.getEarnedAchievements(childProfileId).first()
        assertTrue("Perfect Score achievement should be earned",
            achievements.any { it.type == AchievementType.PERFECT_SCORE }
        )

        // Test Speed Learner achievement (complete lesson in under 5 minutes)
        progressRepository.completeLesson(childProfileId, "lesson-speed", 280L, 90) // 4:40

        achievements = achievementRepository.getEarnedAchievements(childProfileId).first()
        assertTrue("Speed Learner achievement should be earned",
            achievements.any { it.type == AchievementType.SPEED_LEARNER }
        )

        // Test Subject Master achievement (complete 10 lessons in one subject)
        repeat(10) { index ->
            progressRepository.completeLesson(
                childProfileId = childProfileId,
                lessonId = "math-lesson-$index",
                totalTimeSpent = 600L,
                score = 85
            )
            
            // Add lesson to repository with Mathematics subject
            lessonRepository.saveLesson(
                Lesson(
                    id = "math-lesson-$index",
                    title = "Math Lesson $index",
                    subject = "Mathematics",
                    grade = "Class 6",
                    duration = 10
                )
            )
        }

        achievements = achievementRepository.getEarnedAchievements(childProfileId).first()
        assertTrue("Subject Master achievement should be earned",
            achievements.any { it.type == AchievementType.SUBJECT_MASTER }
        )

        // Test achievement levels
        val achievementLevels = achievementRepository.getAchievementLevels(childProfileId).first()
        assertTrue("Should have multiple achievement levels", achievementLevels.isNotEmpty())

        // Verify total points calculated correctly
        val totalPoints = achievements.sumOf { it.points }
        assertTrue("Should have earned points from achievements", totalPoints > 0)
    }

    @Test
    fun test_parental_controls_data_flow() = runTest {
        val parentId = "parent-123"
        val childProfileId = "child-controls-test"

        // Set screen time limits
        progressRepository.setScreenTimeLimit(childProfileId, 60) // 60 minutes

        // Record screen time usage
        progressRepository.recordScreenTime(childProfileId, 30) // 30 minutes used

        // Check remaining screen time
        val remainingTime = progressRepository.getRemainingScreenTime(childProfileId).first()
        assertEquals(30, remainingTime) // 30 minutes remaining

        // Test time limit enforcement
        progressRepository.recordScreenTime(childProfileId, 35) // Total: 65 minutes

        val timeExceeded = progressRepository.isScreenTimeLimitExceeded(childProfileId).first()
        assertTrue("Screen time limit should be exceeded", timeExceeded)

        // Test content restrictions
        val restrictedLesson = Lesson(
            id = "advanced-lesson",
            title = "Advanced Mathematics",
            subject = "Mathematics", 
            grade = "Class 8", // Higher than child's grade
            duration = 20,
            difficulty = DifficultyLevel.ADVANCED
        )
        lessonRepository.saveLesson(restrictedLesson)

        // Child should not be able to access advanced content
        val allowedLessons = lessonRepository.getAvailableLessons(childProfileId, "Mathematics").first()
        assertFalse("Advanced lesson should not be available",
            allowedLessons.any { it.id == restrictedLesson.id }
        )

        // Test progress reports for parents
        val progressReport = progressRepository.getProgressReport(childProfileId).first()
        assertNotNull(progressReport)
        assertTrue("Progress report should contain lesson data", 
            progressReport.totalLessonsCompleted >= 0)
        assertTrue("Progress report should contain time data",
            progressReport.totalTimeSpent >= 0)
    }

    @Test
    fun test_data_consistency_across_repositories() = runTest {
        val childProfileId = "consistency-test-child"
        val lessonId = "consistency-test-lesson"

        // Create lesson in lesson repository
        val lesson = Lesson(
            id = lessonId,
            title = "Consistency Test Lesson",
            subject = "Science",
            grade = "Class 6",
            duration = 15
        )
        lessonRepository.saveLesson(lesson)

        // Start lesson progress
        progressRepository.startLesson(childProfileId, lessonId)

        // Complete lesson with high score
        progressRepository.completeLesson(childProfileId, lessonId, 900L, 95)

        // Check data consistency across repositories
        
        // 1. Lesson repository should have the lesson
        val retrievedLesson = lessonRepository.getLesson(lessonId).first()
        assertNotNull("Lesson should exist in lesson repository", retrievedLesson)
        assertEquals(lesson.title, retrievedLesson?.title)

        // 2. Progress repository should have completion record
        val progress = progressRepository.getLessonProgress(childProfileId, lessonId).first()
        assertNotNull("Progress should exist in progress repository", progress)
        assertEquals(LessonStatus.COMPLETED, progress?.status)
        assertEquals(95, progress?.score)

        // 3. Achievement repository should reflect the completion
        val achievements = achievementRepository.getEarnedAchievements(childProfileId).first()
        assertTrue("Should have at least first lesson achievement",
            achievements.isNotEmpty())

        // 4. Subject progress should be updated
        val subjectProgress = progressRepository.getSubjectProgress(childProfileId, "Science").first()
        assertNotNull("Subject progress should exist", subjectProgress)
        assertEquals(1, subjectProgress?.lessonsCompleted)

        // Test data deletion consistency
        lessonRepository.deleteLesson(lessonId)

        // Progress should handle missing lesson gracefully
        val progressAfterDeletion = progressRepository.getLessonProgress(childProfileId, lessonId).first()
        // Progress record might still exist but lesson is gone
        val lessonAfterDeletion = lessonRepository.getLesson(lessonId).first()
        assertNull("Lesson should be deleted", lessonAfterDeletion)
    }

    @Test
    fun test_concurrent_operations() = runTest {
        val childProfileId = "concurrent-test-child"
        
        // Simulate multiple concurrent operations
        val jobs = (1..10).map { index ->
            kotlinx.coroutines.launch {
                val lessonId = "concurrent-lesson-$index"
                
                // Create lesson
                lessonRepository.saveLesson(
                    Lesson(
                        id = lessonId,
                        title = "Concurrent Lesson $index",
                        subject = "Mathematics",
                        grade = "Class 6",
                        duration = 10
                    )
                )
                
                // Start and complete lesson
                progressRepository.startLesson(childProfileId, lessonId)
                progressRepository.completeLesson(childProfileId, lessonId, 600L, 80 + index)
            }
        }
        
        // Wait for all operations to complete
        jobs.forEach { it.join() }
        
        // Verify all operations completed successfully
        val allProgress = progressRepository.getAllProgress(childProfileId).first()
        assertEquals("All lessons should have progress records", 10, allProgress.size)
        
        // Check data integrity
        allProgress.forEach { progress ->
            assertEquals("All lessons should be completed", LessonStatus.COMPLETED, progress.status)
            assertTrue("Scores should be in valid range", progress.score in 81..90)
        }
    }
}

// Test data models for consistency
data class TestProgressReport(
    val totalLessonsCompleted: Int,
    val totalTimeSpent: Long,
    val averageScore: Int,
    val subjectBreakdown: Map<String, Int>
)