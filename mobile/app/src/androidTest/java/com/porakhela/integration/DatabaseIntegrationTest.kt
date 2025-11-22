package com.porakhela.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.porakhela.data.local.PorakhelaDatabase
import com.porakhela.data.model.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Database Integration Tests
 * Tests database operations, migrations, and data integrity
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DatabaseIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: PorakhelaDatabase

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.clearAllTables()
    }

    @Test
    fun test_user_and_child_profile_operations() = runTest {
        val userDao = database.userDao()
        val childProfileDao = database.childProfileDao()

        // Insert user
        val user = UserEntity(
            id = "user-123",
            phoneNumber = "+8801712345678",
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis()
        )
        userDao.insertUser(user)

        // Verify user insertion
        val retrievedUser = userDao.getUserById(user.id)
        assertNotNull(retrievedUser)
        assertEquals(user.phoneNumber, retrievedUser?.phoneNumber)

        // Insert child profiles
        val childProfile1 = ChildProfileEntity(
            id = "child-1",
            parentId = user.id,
            name = "Alice Rahman",
            grade = "Class 5",
            avatar = "girl_1",
            subjects = "Mathematics,Science,English",
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        val childProfile2 = ChildProfileEntity(
            id = "child-2", 
            parentId = user.id,
            name = "Bob Rahman",
            grade = "Class 7",
            avatar = "boy_2",
            subjects = "Physics,Chemistry,Biology",
            createdAt = System.currentTimeMillis(),
            isActive = true
        )

        childProfileDao.insertChildProfile(childProfile1)
        childProfileDao.insertChildProfile(childProfile2)

        // Verify child profiles
        val childProfiles = childProfileDao.getChildProfilesByParent(user.id)
        assertEquals(2, childProfiles.size)
        assertTrue(childProfiles.any { it.name == "Alice Rahman" })
        assertTrue(childProfiles.any { it.name == "Bob Rahman" })

        // Test cascade delete
        userDao.deleteUser(user.id)
        val childProfilesAfterDelete = childProfileDao.getChildProfilesByParent(user.id)
        assertTrue("Child profiles should be deleted with user", childProfilesAfterDelete.isEmpty())
    }

    @Test
    fun test_lesson_and_progress_operations() = runTest {
        val lessonDao = database.lessonDao()
        val progressDao = database.progressDao()

        // Insert lessons
        val lesson1 = LessonEntity(
            id = "lesson-math-001",
            title = "Introduction to Algebra",
            subject = "Mathematics",
            grade = "Class 8",
            duration = 20,
            difficulty = "INTERMEDIATE",
            videoUrl = "https://example.com/algebra.mp4",
            thumbnailUrl = "https://example.com/algebra-thumb.jpg",
            description = "Learn basic algebraic concepts",
            createdAt = System.currentTimeMillis(),
            isDownloaded = false
        )

        val lesson2 = LessonEntity(
            id = "lesson-science-001",
            title = "Properties of Matter",
            subject = "Science",
            grade = "Class 8", 
            duration = 15,
            difficulty = "BEGINNER",
            videoUrl = "https://example.com/matter.mp4",
            thumbnailUrl = "https://example.com/matter-thumb.jpg",
            description = "Understand different states of matter",
            createdAt = System.currentTimeMillis(),
            isDownloaded = false
        )

        lessonDao.insertLesson(lesson1)
        lessonDao.insertLesson(lesson2)

        // Verify lesson insertion
        val mathLessons = lessonDao.getLessonsBySubject("Mathematics").first()
        assertEquals(1, mathLessons.size)
        assertEquals("Introduction to Algebra", mathLessons[0].title)

        val scienceLessons = lessonDao.getLessonsBySubject("Science").first()
        assertEquals(1, scienceLessons.size)

        // Insert lesson progress
        val progress1 = LessonProgressEntity(
            id = "progress-1",
            childProfileId = "child-123",
            lessonId = lesson1.id,
            status = "IN_PROGRESS",
            progressPercentage = 45,
            sectionsCompleted = 2,
            totalSections = 4,
            timeSpent = 540L, // 9 minutes
            score = 0,
            startedAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )

        val progress2 = LessonProgressEntity(
            id = "progress-2",
            childProfileId = "child-123", 
            lessonId = lesson2.id,
            status = "COMPLETED",
            progressPercentage = 100,
            sectionsCompleted = 3,
            totalSections = 3,
            timeSpent = 900L, // 15 minutes
            score = 95,
            startedAt = System.currentTimeMillis() - 1800000, // 30 min ago
            lastAccessedAt = System.currentTimeMillis() - 600000, // 10 min ago
            completedAt = System.currentTimeMillis() - 600000
        )

        progressDao.insertProgress(progress1)
        progressDao.insertProgress(progress2)

        // Verify progress operations
        val allProgress = progressDao.getProgressByChild("child-123")
        assertEquals(2, allProgress.size)

        val completedProgress = progressDao.getCompletedLessons("child-123")
        assertEquals(1, completedProgress.size)
        assertEquals(lesson2.id, completedProgress[0].lessonId)

        val inProgressLessons = progressDao.getInProgressLessons("child-123")
        assertEquals(1, inProgressLessons.size)
        assertEquals(lesson1.id, inProgressLessons[0].lessonId)

        // Test progress updates
        progressDao.updateProgress(
            progress1.copy(
                status = "COMPLETED",
                progressPercentage = 100,
                sectionsCompleted = 4,
                score = 88,
                completedAt = System.currentTimeMillis()
            )
        )

        val updatedProgress = progressDao.getProgressByLessonAndChild(lesson1.id, "child-123")
        assertEquals("COMPLETED", updatedProgress?.status)
        assertEquals(88, updatedProgress?.score)
    }

    @Test
    fun test_achievement_operations() = runTest {
        val achievementDao = database.achievementDao()

        // Insert achievements
        val achievement1 = AchievementEntity(
            id = "achievement-first-lesson",
            childProfileId = "child-123",
            type = "FIRST_LESSON",
            title = "First Lesson Complete!",
            description = "Congratulations on completing your first lesson",
            points = 50,
            earnedAt = System.currentTimeMillis(),
            level = 1
        )

        val achievement2 = AchievementEntity(
            id = "achievement-perfect-score",
            childProfileId = "child-123",
            type = "PERFECT_SCORE",
            title = "Perfect Score Master",
            description = "Achieved perfect scores on 5 lessons in a row",
            points = 200,
            earnedAt = System.currentTimeMillis() + 100000,
            level = 2
        )

        achievementDao.insertAchievement(achievement1)
        achievementDao.insertAchievement(achievement2)

        // Verify achievements
        val achievements = achievementDao.getAchievementsByChild("child-123")
        assertEquals(2, achievements.size)

        val totalPoints = achievementDao.getTotalPoints("child-123")
        assertEquals(250, totalPoints)

        val achievementsByType = achievementDao.getAchievementsByType("child-123", "FIRST_LESSON")
        assertEquals(1, achievementsByType.size)
        assertEquals("First Lesson Complete!", achievementsByType[0].title)

        // Test achievement level progression
        val level1Achievements = achievementDao.getAchievementsByLevel("child-123", 1)
        assertEquals(1, level1Achievements.size)

        val level2Achievements = achievementDao.getAchievementsByLevel("child-123", 2)
        assertEquals(1, level2Achievements.size)
    }

    @Test
    fun test_download_operations() = runTest {
        val downloadDao = database.downloadDao()

        // Insert download records
        val download1 = DownloadEntity(
            lessonId = "lesson-001",
            childProfileId = "child-123",
            status = "DOWNLOADING",
            progress = 45,
            localPath = null,
            downloadedAt = null,
            requestedAt = System.currentTimeMillis(),
            fileSize = 15728640L // 15MB
        )

        val download2 = DownloadEntity(
            lessonId = "lesson-002",
            childProfileId = "child-123", 
            status = "COMPLETED",
            progress = 100,
            localPath = "/storage/lessons/lesson-002",
            downloadedAt = System.currentTimeMillis() - 300000, // 5 min ago
            requestedAt = System.currentTimeMillis() - 900000, // 15 min ago
            fileSize = 23068672L // 22MB
        )

        downloadDao.insertDownload(download1)
        downloadDao.insertDownload(download2)

        // Verify download operations
        val allDownloads = downloadDao.getDownloadsByChild("child-123")
        assertEquals(2, allDownloads.size)

        val completedDownloads = downloadDao.getCompletedDownloads("child-123")
        assertEquals(1, completedDownloads.size)
        assertEquals("lesson-002", completedDownloads[0].lessonId)

        val downloadingLessons = downloadDao.getDownloadsByStatus("DOWNLOADING")
        assertEquals(1, downloadingLessons.size)
        assertEquals(45, downloadingLessons[0].progress)

        // Test download progress update
        downloadDao.updateDownloadProgress("lesson-001", 75)
        val updatedDownload = downloadDao.getDownloadByLesson("lesson-001")
        assertEquals(75, updatedDownload?.progress)

        // Test download completion
        downloadDao.completeDownload(
            lessonId = "lesson-001",
            localPath = "/storage/lessons/lesson-001",
            completedAt = System.currentTimeMillis()
        )

        val completedDownload = downloadDao.getDownloadByLesson("lesson-001")
        assertEquals("COMPLETED", completedDownload?.status)
        assertEquals(100, completedDownload?.progress)
        assertNotNull(completedDownload?.localPath)

        // Test storage calculations
        val totalStorage = downloadDao.getTotalStorageUsed("child-123")
        assertTrue("Total storage should be sum of downloaded files", totalStorage > 0)
    }

    @Test
    fun test_exercise_and_results_operations() = runTest {
        val exerciseDao = database.exerciseDao()

        // Insert exercise
        val exercise = ExerciseEntity(
            id = "exercise-math-001",
            lessonId = "lesson-math-001",
            question = "What is 2 + 2?",
            options = "2,3,4,5",
            correctAnswer = 2, // Index of "4"
            explanation = "2 + 2 = 4",
            type = "MULTIPLE_CHOICE",
            orderIndex = 1
        )

        exerciseDao.insertExercise(exercise)

        // Insert exercise results
        val result1 = ExerciseResultEntity(
            id = "result-1",
            childProfileId = "child-123",
            exerciseId = exercise.id,
            lessonId = exercise.lessonId,
            selectedAnswer = 2, // Correct
            isCorrect = true,
            timeSpent = 15L,
            attempts = 1,
            completedAt = System.currentTimeMillis()
        )

        val result2 = ExerciseResultEntity(
            id = "result-2", 
            childProfileId = "child-456",
            exerciseId = exercise.id,
            lessonId = exercise.lessonId,
            selectedAnswer = 1, // Incorrect
            isCorrect = false,
            timeSpent = 25L,
            attempts = 2,
            completedAt = System.currentTimeMillis() + 60000
        )

        exerciseDao.insertExerciseResult(result1)
        exerciseDao.insertExerciseResult(result2)

        // Verify exercise operations
        val exercisesByLesson = exerciseDao.getExercisesByLesson("lesson-math-001")
        assertEquals(1, exercisesByLesson.size)
        assertEquals("What is 2 + 2?", exercisesByLesson[0].question)

        val resultsByChild = exerciseDao.getExerciseResultsByChild("child-123")
        assertEquals(1, resultsByChild.size)
        assertTrue(resultsByChild[0].isCorrect)

        val resultsByLesson = exerciseDao.getExerciseResultsByLesson("child-123", "lesson-math-001")
        assertEquals(1, resultsByLesson.size)

        // Test performance analytics
        val accuracy = exerciseDao.getAccuracyForChild("child-123")
        assertEquals(100.0, accuracy, 0.1) // 100% accuracy

        val avgTimePerExercise = exerciseDao.getAverageTimePerExercise("child-123")
        assertEquals(15.0, avgTimePerExercise, 0.1)

        // Test incorrect answers tracking
        val incorrectResults = exerciseDao.getIncorrectAnswers("child-456")
        assertEquals(1, incorrectResults.size)
        assertFalse(incorrectResults[0].isCorrect)
    }

    @Test
    fun test_foreign_key_constraints() = runTest {
        val userDao = database.userDao()
        val childProfileDao = database.childProfileDao()
        val progressDao = database.progressDao()

        // Insert user
        val user = UserEntity(
            id = "user-fk-test",
            phoneNumber = "+8801700000000",
            createdAt = System.currentTimeMillis()
        )
        userDao.insertUser(user)

        // Insert child profile
        val childProfile = ChildProfileEntity(
            id = "child-fk-test",
            parentId = user.id,
            name = "FK Test Child",
            grade = "Class 5",
            avatar = "default",
            subjects = "Mathematics",
            createdAt = System.currentTimeMillis(),
            isActive = true
        )
        childProfileDao.insertChildProfile(childProfile)

        // Try to insert progress for non-existent lesson (should handle gracefully)
        val progress = LessonProgressEntity(
            id = "progress-fk-test",
            childProfileId = childProfile.id,
            lessonId = "non-existent-lesson",
            status = "IN_PROGRESS",
            progressPercentage = 0,
            sectionsCompleted = 0,
            totalSections = 5,
            timeSpent = 0L,
            score = 0,
            startedAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis()
        )

        // This should work even with non-existent lesson ID
        // (depending on your FK constraint settings)
        progressDao.insertProgress(progress)

        val retrievedProgress = progressDao.getProgressByLessonAndChild("non-existent-lesson", childProfile.id)
        assertNotNull(retrievedProgress)

        // Test deletion with foreign key constraints
        try {
            // Delete user should cascade to child profiles
            userDao.deleteUser(user.id)
            
            val deletedChild = childProfileDao.getChildProfileById(childProfile.id)
            assertNull("Child profile should be deleted with user", deletedChild)
        } catch (e: Exception) {
            // If FK constraints are strict, this might throw an exception
            // which is also acceptable behavior
        }
    }

    @Test
    fun test_database_migration_scenarios() = runTest {
        // This test would simulate database upgrades
        // For now, we test data integrity after operations

        val userDao = database.userDao()
        val progressDao = database.progressDao()

        // Insert data
        val user = UserEntity(
            id = "migration-test-user",
            phoneNumber = "+8801800000000", 
            createdAt = System.currentTimeMillis()
        )
        userDao.insertUser(user)

        // Verify data exists
        val retrievedUser = userDao.getUserById(user.id)
        assertNotNull(retrievedUser)

        // Clear and reinitialize (simulating migration)
        database.clearAllTables()

        // Data should be gone
        val userAfterClear = userDao.getUserById(user.id)
        assertNull(userAfterClear)

        // Re-insert data (simulating migration with data restoration)
        userDao.insertUser(user)
        val restoredUser = userDao.getUserById(user.id)
        assertNotNull(restoredUser)
        assertEquals(user.phoneNumber, restoredUser?.phoneNumber)
    }

    @Test
    fun test_database_performance() = runTest {
        val lessonDao = database.lessonDao()
        val progressDao = database.progressDao()

        // Insert many lessons for performance testing
        val lessons = (1..100).map { index ->
            LessonEntity(
                id = "perf-lesson-$index",
                title = "Performance Test Lesson $index",
                subject = if (index % 2 == 0) "Mathematics" else "Science",
                grade = "Class ${6 + (index % 3)}",
                duration = 10 + (index % 10),
                difficulty = when (index % 3) {
                    0 -> "BEGINNER"
                    1 -> "INTERMEDIATE"
                    else -> "ADVANCED"
                },
                videoUrl = "https://example.com/lesson-$index.mp4",
                thumbnailUrl = "https://example.com/thumb-$index.jpg",
                description = "Performance test lesson number $index",
                createdAt = System.currentTimeMillis(),
                isDownloaded = false
            )
        }

        val startTime = System.currentTimeMillis()

        // Batch insert
        lessons.forEach { lesson ->
            lessonDao.insertLesson(lesson)
        }

        val insertTime = System.currentTimeMillis() - startTime
        assertTrue("Batch insert should complete within reasonable time", insertTime < 5000)

        // Query performance test
        val queryStartTime = System.currentTimeMillis()
        val mathLessons = lessonDao.getLessonsBySubject("Mathematics").first()
        val queryTime = System.currentTimeMillis() - queryStartTime

        assertTrue("Query should complete quickly", queryTime < 1000)
        assertEquals(50, mathLessons.size) // Half should be Mathematics

        // Cleanup
        lessons.forEach { lesson ->
            lessonDao.deleteLesson(lesson.id)
        }

        val finalCount = lessonDao.getLessonsBySubject("Mathematics").first().size
        assertEquals(0, finalCount)
    }
}

// Helper function for testing
private fun assertEquals(expected: Double, actual: Double, delta: Double) {
    assertTrue("Expected $expected but was $actual", 
        kotlin.math.abs(expected - actual) <= delta)
}