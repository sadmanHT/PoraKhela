package com.porakhela.ui.screens.auth

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.porakhela.data.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for OTPViewModel
 * Tests OTP validation, verification, timer functionality
 */
@ExperimentalCoroutinesApi
class OTPViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: OTPViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = OTPViewModel(mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `OTP input validation - complete 6-digit code`() = testScope.runTest {
        // Given
        val completeOTP = "123456"
        
        // When
        viewModel.updateOTP(completeOTP)
        
        // Then
        assertThat(viewModel.otpCode.value).isEqualTo(completeOTP)
        assertThat(viewModel.isOTPComplete.value).isTrue()
    }

    @Test
    fun `OTP input validation - incomplete code`() = testScope.runTest {
        // Given
        val incompleteOTP = "1234"
        
        // When
        viewModel.updateOTP(incompleteOTP)
        
        // Then
        assertThat(viewModel.otpCode.value).isEqualTo(incompleteOTP)
        assertThat(viewModel.isOTPComplete.value).isFalse()
    }

    @Test
    fun `OTP verification - success`() = testScope.runTest {
        // Given
        val phone = "01712345678"
        val otp = "123456"
        val authToken = "mock_auth_token"
        
        coEvery { 
            mockAuthRepository.verifyOTP(phone, otp) 
        } returns flow { 
            emit(Result.success(authToken)) 
        }
        
        // When
        viewModel.setPhoneNumber(phone)
        viewModel.updateOTP(otp)
        viewModel.verifyOTP()
        
        // Then
        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.errorMessage.value).isEmpty()
        
        coVerify { mockAuthRepository.verifyOTP(phone, otp) }
    }

    @Test
    fun `OTP verification - invalid code`() = testScope.runTest {
        // Given
        val phone = "01712345678"
        val otp = "123456"
        val errorMessage = "Invalid OTP code"
        
        coEvery { 
            mockAuthRepository.verifyOTP(phone, otp) 
        } returns flow { 
            emit(Result.failure(Exception(errorMessage))) 
        }
        
        // When
        viewModel.setPhoneNumber(phone)
        viewModel.updateOTP(otp)
        viewModel.verifyOTP()
        
        // Then
        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.errorMessage.value).contains(errorMessage)
    }

    @Test
    fun `resend OTP - success with timer reset`() = testScope.runTest {
        // Given
        val phone = "01712345678"
        coEvery { mockAuthRepository.sendOTP(phone) } returns flow { 
            emit(Result.success("OTP sent")) 
        }
        
        // When
        viewModel.setPhoneNumber(phone)
        viewModel.resendOTP()
        
        // Then
        assertThat(viewModel.canResend.value).isFalse() // Should start timer
        assertThat(viewModel.resendTimeLeft.value).isEqualTo(60) // 60 second timer
        
        coVerify { mockAuthRepository.sendOTP(phone) }
    }

    @Test
    fun `OTP timer - countdown functionality`() = testScope.runTest {
        // Given
        val phone = "01712345678"
        coEvery { mockAuthRepository.sendOTP(phone) } returns flow { 
            emit(Result.success("OTP sent")) 
        }
        
        // When
        viewModel.setPhoneNumber(phone)
        viewModel.resendOTP()
        
        // Initially timer should be at 60
        assertThat(viewModel.resendTimeLeft.value).isEqualTo(60)
        assertThat(viewModel.canResend.value).isFalse()
        
        // Advance time by 30 seconds
        testScheduler.advanceTimeBy(30_000)
        
        // Timer should be at 30
        assertThat(viewModel.resendTimeLeft.value).isEqualTo(30)
        assertThat(viewModel.canResend.value).isFalse()
        
        // Advance time by another 30 seconds
        testScheduler.advanceTimeBy(30_000)
        
        // Timer should be finished
        assertThat(viewModel.resendTimeLeft.value).isEqualTo(0)
        assertThat(viewModel.canResend.value).isTrue()
    }

    @Test
    fun `OTP auto-progression - moves to next field`() = testScope.runTest {
        // Given
        val firstDigit = "1"
        
        // When
        viewModel.updateOTPAtPosition(0, firstDigit)
        
        // Then
        assertThat(viewModel.getOTPAtPosition(0)).isEqualTo(firstDigit)
        assertThat(viewModel.currentOTPPosition.value).isEqualTo(1) // Should move to next
    }

    @Test
    fun `OTP clear functionality - clears all fields`() = testScope.runTest {
        // Given
        viewModel.updateOTP("123456")
        
        // When
        viewModel.clearOTP()
        
        // Then
        assertThat(viewModel.otpCode.value).isEmpty()
        assertThat(viewModel.currentOTPPosition.value).isEqualTo(0)
        assertThat(viewModel.isOTPComplete.value).isFalse()
    }
}