package com.porakhela.data.api

import com.google.common.truth.Truth.assertThat
import com.porakhela.data.network.AuthApiService
import com.porakhela.data.network.dto.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for API response handling
 * Tests proper mock responses and error handling
 */
@ExperimentalCoroutinesApi
class ApiResponseTest {

    private lateinit var mockApiService: AuthApiService

    @Before
    fun setup() {
        mockApiService = mockk<AuthApiService>()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `send OTP API - success response`() = runTest {
        // Given
        val phone = "01712345678"
        val expectedResponse = SendOTPResponse(
            success = true,
            message = "OTP sent successfully",
            otpId = "otp_123456"
        )
        
        coEvery { 
            mockApiService.sendOTP(SendOTPRequest(phone)) 
        } returns Response.success(expectedResponse)

        // When
        val response = mockApiService.sendOTP(SendOTPRequest(phone))

        // Then
        assertThat(response.isSuccessful).isTrue()
        assertThat(response.body()?.success).isTrue()
        assertThat(response.body()?.message).isEqualTo("OTP sent successfully")
        assertThat(response.body()?.otpId).isEqualTo("otp_123456")
    }

    @Test
    fun `send OTP API - error response`() = runTest {
        // Given
        val phone = "01712345678"
        val errorResponse = SendOTPResponse(
            success = false,
            message = "Invalid phone number",
            otpId = null
        )
        
        coEvery { 
            mockApiService.sendOTP(SendOTPRequest(phone)) 
        } returns Response.success(errorResponse)

        // When
        val response = mockApiService.sendOTP(SendOTPRequest(phone))

        // Then
        assertThat(response.isSuccessful).isTrue() // HTTP success but business logic error
        assertThat(response.body()?.success).isFalse()
        assertThat(response.body()?.message).contains("Invalid")
    }

    @Test
    fun `verify OTP API - success with auth token`() = runTest {
        // Given
        val phone = "01712345678"
        val otp = "123456"
        val expectedResponse = VerifyOTPResponse(
            success = true,
            message = "OTP verified successfully",
            authToken = "auth_token_abc123",
            user = UserDTO(
                id = "user_1",
                phoneNumber = phone,
                role = "parent",
                isVerified = true
            )
        )
        
        coEvery { 
            mockApiService.verifyOTP(VerifyOTPRequest(phone, otp)) 
        } returns Response.success(expectedResponse)

        // When
        val response = mockApiService.verifyOTP(VerifyOTPRequest(phone, otp))

        // Then
        assertThat(response.isSuccessful).isTrue()
        val body = response.body()
        assertThat(body?.success).isTrue()
        assertThat(body?.authToken).isEqualTo("auth_token_abc123")
        assertThat(body?.user?.phoneNumber).isEqualTo(phone)
        assertThat(body?.user?.role).isEqualTo("parent")
    }

    @Test
    fun `verify OTP API - invalid OTP error`() = runTest {
        // Given
        val phone = "01712345678"
        val otp = "000000"
        val errorResponse = VerifyOTPResponse(
            success = false,
            message = "Invalid OTP code",
            authToken = null,
            user = null
        )
        
        coEvery { 
            mockApiService.verifyOTP(VerifyOTPRequest(phone, otp)) 
        } returns Response.success(errorResponse)

        // When
        val response = mockApiService.verifyOTP(VerifyOTPRequest(phone, otp))

        // Then
        assertThat(response.isSuccessful).isTrue()
        assertThat(response.body()?.success).isFalse()
        assertThat(response.body()?.message).contains("Invalid OTP")
        assertThat(response.body()?.authToken).isNull()
    }

    @Test
    fun `API network error - HTTP 500`() = runTest {
        // Given
        val phone = "01712345678"
        
        coEvery { 
            mockApiService.sendOTP(SendOTPRequest(phone)) 
        } returns Response.error(500, "Server Error".toResponseBody())

        // When
        val response = mockApiService.sendOTP(SendOTPRequest(phone))

        // Then
        assertThat(response.isSuccessful).isFalse()
        assertThat(response.code()).isEqualTo(500)
    }

    @Test
    fun `create child profile API - success`() = runTest {
        // Given
        val createChildRequest = CreateChildProfileRequest(
            name = "Arman",
            grade = 6,
            avatar = "🧒",
            subjects = listOf("math", "science", "bangla")
        )
        
        val expectedResponse = CreateChildProfileResponse(
            success = true,
            message = "Child profile created successfully",
            child = ChildProfileDTO(
                id = "child_1",
                name = "Arman",
                grade = 6,
                avatar = "🧒",
                totalPoints = 0,
                level = 1,
                parentId = "user_1",
                isActive = true
            )
        )
        
        coEvery { 
            mockApiService.createChildProfile("auth_token", createChildRequest) 
        } returns Response.success(expectedResponse)

        // When
        val response = mockApiService.createChildProfile("auth_token", createChildRequest)

        // Then
        assertThat(response.isSuccessful).isTrue()
        val body = response.body()
        assertThat(body?.success).isTrue()
        assertThat(body?.child?.name).isEqualTo("Arman")
        assertThat(body?.child?.grade).isEqualTo(6)
        assertThat(body?.child?.totalPoints).isEqualTo(0)
    }

    @Test
    fun `get lesson content API - success`() = runTest {
        // Given
        val lessonId = "math_lesson_1"
        val expectedResponse = LessonContentResponse(
            success = true,
            lesson = LessonContentDTO(
                id = lessonId,
                title = "Introduction to Numbers",
                titleBn = "সংখ্যার পরিচয়",
                subjectId = "math",
                chapterTitle = "Number Systems",
                content = "Today we will learn about numbers...",
                videoUrl = "https://example.com/video.mp4",
                exercises = listOf(
                    ExerciseDTO(
                        id = "ex_1",
                        question = "What is 2 + 2?",
                        options = listOf("3", "4", "5", "6"),
                        correctAnswer = 1,
                        explanation = "2 plus 2 equals 4"
                    )
                ),
                estimatedMinutes = 15,
                points = 100
            )
        )
        
        coEvery { 
            mockApiService.getLessonContent("auth_token", lessonId) 
        } returns Response.success(expectedResponse)

        // When
        val response = mockApiService.getLessonContent("auth_token", lessonId)

        // Then
        assertThat(response.isSuccessful).isTrue()
        val lesson = response.body()?.lesson
        assertThat(lesson?.title).isEqualTo("Introduction to Numbers")
        assertThat(lesson?.titleBn).isEqualTo("সংখ্যার পরিচয়")
        assertThat(lesson?.exercises).hasSize(1)
        assertThat(lesson?.estimatedMinutes).isEqualTo(15)
    }

    @Test
    fun `sync progress API - success`() = runTest {
        // Given
        val progressData = SyncProgressRequest(
            childId = "child_1",
            sessions = listOf(
                SessionDataDTO(
                    lessonId = "math_lesson_1",
                    completedAt = System.currentTimeMillis(),
                    pointsEarned = 100,
                    timeSpentMinutes = 15,
                    exercises = listOf(
                        ExerciseResultDTO(
                            exerciseId = "ex_1",
                            isCorrect = true,
                            timeSpentSeconds = 30
                        )
                    )
                )
            ),
            achievements = listOf(
                AchievementDataDTO(
                    achievementId = "first_lesson",
                    unlockedAt = System.currentTimeMillis()
                )
            )
        )
        
        val expectedResponse = SyncProgressResponse(
            success = true,
            message = "Progress synced successfully",
            newAchievements = listOf(),
            updatedLevel = 2,
            bonusPoints = 50
        )
        
        coEvery { 
            mockApiService.syncProgress("auth_token", progressData) 
        } returns Response.success(expectedResponse)

        // When
        val response = mockApiService.syncProgress("auth_token", progressData)

        // Then
        assertThat(response.isSuccessful).isTrue()
        val body = response.body()
        assertThat(body?.success).isTrue()
        assertThat(body?.updatedLevel).isEqualTo(2)
        assertThat(body?.bonusPoints).isEqualTo(50)
    }
}