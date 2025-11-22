package com.porakhela.data.offline

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.porakhela.data.database.PorakhelaDatabase
import com.porakhela.data.entities.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * Unit tests for offline caching logic
 * Tests lesson downloads, cache management, sync functionality
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class OfflineCachingTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: PorakhelaDatabase
    private lateinit var downloadDao: DownloadDao
    private lateinit var cacheDao: CacheDao
    private lateinit var lessonDao: LessonDao

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PorakhelaDatabase::class.java
        ).allowMainThreadQueries().build()
        
        downloadDao = database.downloadDao()
        cacheDao = database.cacheDao()
        lessonDao = database.lessonDao()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `insert and retrieve download entity`() = runTest(testDispatcher) {
        // Given
        val download = DownloadEntity(
            id = "lesson_1",
            lessonId = "math_lesson_1",
            contentType = "lesson",
            status = DownloadStatus.IN_PROGRESS,
            progress = 50.0f,
            totalSizeBytes = 1024000L,
            downloadedSizeBytes = 512000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            priority = 2
        )

        // When
        downloadDao.insertDownload(download)
        val retrieved = downloadDao.getDownload("lesson_1").first()

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.lessonId).isEqualTo("math_lesson_1")
        assertThat(retrieved?.status).isEqualTo(DownloadStatus.IN_PROGRESS)
        assertThat(retrieved?.progress).isEqualTo(50.0f)
    }

    @Test
    fun `update download progress`() = runTest(testDispatcher) {
        // Given
        val download = DownloadEntity(
            id = "lesson_1",
            lessonId = "math_lesson_1",
            contentType = "lesson",
            status = DownloadStatus.IN_PROGRESS,
            progress = 25.0f,
            totalSizeBytes = 1024000L,
            downloadedSizeBytes = 256000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            priority = 2
        )
        downloadDao.insertDownload(download)

        // When
        downloadDao.updateDownloadProgress("lesson_1", 75.0f, 768000L)
        val updated = downloadDao.getDownload("lesson_1").first()

        // Then
        assertThat(updated?.progress).isEqualTo(75.0f)
        assertThat(updated?.downloadedSizeBytes).isEqualTo(768000L)
    }

    @Test
    fun `cache asset for lesson`() = runTest(testDispatcher) {
        // Given
        val cachedAsset = CachedAssetEntity(
            id = "lesson_1_video",
            lessonId = "math_lesson_1",
            assetId = "video_intro",
            assetType = "video",
            localPath = "/cache/videos/intro.mp4",
            originalUrl = "https://example.com/video.mp4",
            sizeBytes = 1024000L,
            cachedAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L // 24 hours
        )

        // When
        cacheDao.insertCachedAsset(cachedAsset)
        val assets = cacheDao.getCachedAssetsForLesson("math_lesson_1").first()

        // Then
        assertThat(assets).hasSize(1)
        assertThat(assets[0].assetType).isEqualTo("video")
        assertThat(assets[0].localPath).isEqualTo("/cache/videos/intro.mp4")
    }

    @Test
    fun `get expired assets`() = runTest(testDispatcher) {
        // Given
        val currentTime = System.currentTimeMillis()
        val expiredAsset = CachedAssetEntity(
            id = "lesson_1_image",
            lessonId = "math_lesson_1",
            assetId = "diagram_1",
            assetType = "image",
            localPath = "/cache/images/diagram.png",
            originalUrl = "https://example.com/image.png",
            sizeBytes = 256000L,
            cachedAt = currentTime - 172800000L, // 2 days ago
            lastAccessedAt = currentTime - 172800000L,
            expiresAt = currentTime - 86400000L // Expired 1 day ago
        )
        
        val validAsset = CachedAssetEntity(
            id = "lesson_1_audio",
            lessonId = "math_lesson_1",
            assetId = "audio_intro",
            assetType = "audio",
            localPath = "/cache/audio/intro.mp3",
            originalUrl = "https://example.com/audio.mp3",
            sizeBytes = 512000L,
            cachedAt = currentTime,
            lastAccessedAt = currentTime,
            expiresAt = currentTime + 86400000L // Expires in 24 hours
        )

        cacheDao.insertCachedAsset(expiredAsset)
        cacheDao.insertCachedAsset(validAsset)

        // When
        val expiredAssets = cacheDao.getExpiredAssets(currentTime).first()

        // Then
        assertThat(expiredAssets).hasSize(1)
        assertThat(expiredAssets[0].id).isEqualTo("lesson_1_image")
    }

    @Test
    fun `mark lesson as downloaded`() = runTest(testDispatcher) {
        // Given
        val lesson = LessonEntity(
            id = "math_lesson_1",
            title = "Introduction to Numbers",
            titleBn = "সংখ্যার পরিচয়",
            subjectId = "math",
            chapterId = "chapter_1",
            orderIndex = 1,
            durationMinutes = 15,
            points = 100,
            isDownloaded = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        lessonDao.insertLesson(lesson)

        // When
        lessonDao.markLessonAsDownloaded("math_lesson_1", true)
        val updatedLesson = lessonDao.getLessonById("math_lesson_1").first()

        // Then
        assertThat(updatedLesson?.isDownloaded).isTrue()
    }

    @Test
    fun `get all active downloads`() = runTest(testDispatcher) {
        // Given
        val download1 = DownloadEntity(
            id = "lesson_1",
            lessonId = "math_lesson_1",
            contentType = "lesson",
            status = DownloadStatus.IN_PROGRESS,
            progress = 50.0f,
            totalSizeBytes = 1024000L,
            downloadedSizeBytes = 512000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            priority = 2
        )
        
        val download2 = DownloadEntity(
            id = "lesson_2",
            lessonId = "science_lesson_1",
            contentType = "lesson",
            status = DownloadStatus.COMPLETED,
            progress = 100.0f,
            totalSizeBytes = 2048000L,
            downloadedSizeBytes = 2048000L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            priority = 1
        )
        
        downloadDao.insertDownload(download1)
        downloadDao.insertDownload(download2)

        // When
        val allDownloads = downloadDao.getAllDownloads().first()

        // Then
        assertThat(allDownloads).hasSize(2)
        assertThat(allDownloads.map { it.status }).contains(DownloadStatus.IN_PROGRESS)
        assertThat(allDownloads.map { it.status }).contains(DownloadStatus.COMPLETED)
    }

    @Test
    fun `delete cached assets for lesson`() = runTest(testDispatcher) {
        // Given
        val asset1 = CachedAssetEntity(
            id = "lesson_1_video",
            lessonId = "math_lesson_1",
            assetId = "video_intro",
            assetType = "video",
            localPath = "/cache/videos/intro.mp4",
            originalUrl = "https://example.com/video.mp4",
            sizeBytes = 1024000L,
            cachedAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L
        )
        
        val asset2 = CachedAssetEntity(
            id = "lesson_2_video",
            lessonId = "science_lesson_1", // Different lesson
            assetId = "video_intro",
            assetType = "video",
            localPath = "/cache/videos/science_intro.mp4",
            originalUrl = "https://example.com/science_video.mp4",
            sizeBytes = 1024000L,
            cachedAt = System.currentTimeMillis(),
            lastAccessedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L
        )
        
        cacheDao.insertCachedAsset(asset1)
        cacheDao.insertCachedAsset(asset2)

        // When
        cacheDao.deleteAssetsForLesson("math_lesson_1")
        val remainingAssets = cacheDao.getCachedAssetsForLesson("math_lesson_1").first()
        val otherLessonAssets = cacheDao.getCachedAssetsForLesson("science_lesson_1").first()

        // Then
        assertThat(remainingAssets).isEmpty()
        assertThat(otherLessonAssets).hasSize(1) // Should not affect other lesson
    }
}