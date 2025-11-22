package com.porakhela.data.remote

import com.porakhela.data.model.LessonPack
import com.porakhela.data.model.LessonAsset
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Service for downloading lesson packs and assets for offline use
 */
interface LessonPackService {

    /**
     * Get lesson pack for specific grade
     * Returns JSON with lessons, questions, and asset URLs
     */
    @GET("lessons/")
    suspend fun getLessonPackByGrade(
        @Query("grade") grade: String,
        @Query("limit") limit: Int = 10,
        @Query("format") format: String = "pack"
    ): Response<LessonPack>

    /**
     * Download specific lesson asset (image/audio)
     * @param url - Asset URL from lesson JSON
     */
    @GET
    @Streaming
    suspend fun downloadAsset(
        @Url url: String
    ): Response<ResponseBody>

    /**
     * Get lesson pack metadata to check for updates
     */
    @GET("lessons/pack-info/")
    suspend fun getLessonPackInfo(
        @Query("grade") grade: String
    ): Response<LessonPackInfo>

    /**
     * Download compressed lesson pack (alternative approach)
     */
    @GET("lessons/pack-download/")
    @Streaming
    suspend fun downloadLessonPack(
        @Query("grade") grade: String,
        @Query("version") version: String? = null
    ): Response<ResponseBody>
}

/**
 * Data classes for lesson pack API responses
 */
data class LessonPack(
    val grade: String,
    val version: String,
    val totalLessons: Int,
    val lessons: List<OfflineLessonData>,
    val assets: List<LessonAssetInfo>
)

data class OfflineLessonData(
    val id: String,
    val title: String,
    val subject: String,
    val grade: String,
    val duration: Int,
    val difficulty: String,
    val description: String,
    val thumbnailUrl: String?,
    val videoUrl: String?,
    val audioUrl: String?,
    val sections: List<OfflineLessonSection>,
    val questions: List<OfflineQuestion>,
    val tags: List<String> = emptyList()
)

data class OfflineLessonSection(
    val id: String,
    val title: String,
    val content: String,
    val type: String, // VIDEO, AUDIO, TEXT, INTERACTIVE
    val duration: Int,
    val assetUrl: String?,
    val orderIndex: Int
)

data class OfflineQuestion(
    val id: String,
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String,
    val points: Int = 10,
    val timeLimit: Int = 30, // seconds
    val imageUrl: String? = null,
    val audioUrl: String? = null
)

data class LessonAssetInfo(
    val id: String,
    val url: String,
    val type: String, // IMAGE, AUDIO, VIDEO
    val filename: String,
    val size: Long,
    val checksum: String
)

data class LessonPackInfo(
    val grade: String,
    val currentVersion: String,
    val lastUpdated: Long,
    val totalSize: Long,
    val lessonCount: Int,
    val assetCount: Int
)