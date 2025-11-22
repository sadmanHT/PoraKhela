package com.porakhela.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.porakhela.data.local.dao.LessonDao
import com.porakhela.data.local.dao.DownloadDao
import com.porakhela.data.local.entity.LessonEntity
import com.porakhela.data.local.entity.DownloadEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Performance Tests for Critical App Operations
 * Measures performance of database operations, UI rendering, and data processing
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class PerformanceTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val benchmarkRule = BenchmarkRule()

    @Inject
    lateinit var lessonDao: LessonDao

    @Inject
    lateinit var downloadDao: DownloadDao

    @Test
    fun benchmark_database_lesson_insertion() {
        hiltRule.inject()
        
        benchmarkRule.measureRepeated {
            val lessons = generateSampleLessons(100)
            
            runWithTimingDisabled {
                // Setup before each iteration
                lessonDao.clearAll()
            }
            
            // Measure insertion performance
            lessonDao.insertLessons(lessons)
        }
    }

    @Test
    fun benchmark_database_lesson_query() {
        hiltRule.inject()
        
        // Setup test data
        val lessons = generateSampleLessons(500)
        lessonDao.insertLessons(lessons)
        
        benchmarkRule.measureRepeated {
            // Measure query performance
            lessonDao.getLessonsBySubject("mathematics")
        }
    }

    @Test
    fun benchmark_download_progress_updates() {
        hiltRule.inject()
        
        benchmarkRule.measureRepeated {
            val downloads = generateSampleDownloads(50)
            
            runWithTimingDisabled {
                downloadDao.clearAll()
                downloadDao.insertDownloads(downloads)
            }
            
            // Measure progress update performance
            downloads.forEach { download ->
                downloadDao.updateProgress(download.id, (0..100).random())
            }
        }
    }

    @Test
    fun benchmark_lesson_content_processing() {
        benchmarkRule.measureRepeated {
            val largeContent = generateLargeLessonContent()
            
            // Measure content processing time
            processLessonContent(largeContent)
        }
    }

    @Test
    fun benchmark_offline_cache_operations() {
        hiltRule.inject()
        
        benchmarkRule.measureRepeated {
            val cacheData = generateCacheData(200)
            
            runWithTimingDisabled {
                // Clear cache before each test
                downloadDao.clearAll()
            }
            
            // Measure cache storage performance
            downloadDao.insertDownloads(cacheData)
            
            // Measure cache retrieval
            downloadDao.getAllDownloads()
        }
    }

    // Helper methods for generating test data

    private fun generateSampleLessons(count: Int): List<LessonEntity> {
        return (1..count).map { index ->
            LessonEntity(
                id = "lesson_$index",
                title = "Lesson $index",
                titleBn = "পাঠ $index",
                subjectId = when (index % 3) {
                    0 -> "mathematics"
                    1 -> "science"
                    else -> "bangla"
                },
                chapterTitle = "Chapter ${(index - 1) / 10 + 1}",
                content = "This is the content for lesson $index. ".repeat(50),
                videoUrl = "https://example.com/video_$index.mp4",
                estimatedMinutes = 10 + (index % 20),
                points = 50 + (index % 100),
                isDownloaded = false,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun generateSampleDownloads(count: Int): List<DownloadEntity> {
        return (1..count).map { index ->
            DownloadEntity(
                id = "download_$index",
                lessonId = "lesson_$index",
                status = when (index % 3) {
                    0 -> "completed"
                    1 -> "downloading"
                    else -> "pending"
                },
                progress = if (index % 3 == 0) 100 else (index % 100),
                totalSizeMB = 10 + (index % 50),
                downloadedSizeMB = if (index % 3 == 0) (10 + (index % 50)) else ((index % 100) * (10 + (index % 50)) / 100),
                downloadPath = "/storage/downloads/lesson_$index/",
                createdAt = System.currentTimeMillis() - (index * 1000),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun generateLargeLessonContent(): String {
        return buildString {
            repeat(1000) { index ->
                append("This is line $index of the lesson content. ")
                append("It contains educational material about various topics. ")
                append("The content is designed to be engaging for children. ")
                append("\n")
            }
        }
    }

    private fun generateCacheData(count: Int): List<DownloadEntity> {
        return (1..count).map { index ->
            DownloadEntity(
                id = "cache_$index",
                lessonId = "lesson_cache_$index",
                status = "completed",
                progress = 100,
                totalSizeMB = 5 + (index % 20),
                downloadedSizeMB = 5 + (index % 20),
                downloadPath = "/cache/lesson_$index/",
                createdAt = System.currentTimeMillis() - (index * 60000), // Vary creation time
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun processLessonContent(content: String): ProcessedContent {
        // Simulate content processing operations
        val words = content.split(" ")
        val sentences = content.split(".")
        val paragraphs = content.split("\n\n")
        
        return ProcessedContent(
            wordCount = words.size,
            sentenceCount = sentences.size,
            paragraphCount = paragraphs.size,
            readingTime = words.size / 200 // Assume 200 words per minute
        )
    }

    data class ProcessedContent(
        val wordCount: Int,
        val sentenceCount: Int,
        val paragraphCount: Int,
        val readingTime: Int
    )
}