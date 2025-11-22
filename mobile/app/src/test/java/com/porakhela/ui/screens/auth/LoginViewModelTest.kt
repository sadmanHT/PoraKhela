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
 * Unit tests for LoginViewModel
 * Tests phone number validation, OTP sending, error handling
 */
@ExperimentalCoroutinesApi
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockAuthRepository = mockk<AuthRepository>()
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(mockAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `phone number validation - valid Bangladesh number`() = testScope.runTest {
        // Given
        val validPhone = "01712345678"
        
        // When
        viewModel.updatePhoneNumber(validPhone)
        
        // Then
        assertThat(viewModel.phoneNumber.value).isEqualTo(validPhone)
        assertThat(viewModel.isValidPhone.value).isTrue()
        assertThat(viewModel.errorMessage.value).isEmpty()
    }

    @Test
    fun `phone number validation - invalid short number`() = testScope.runTest {
        // Given
        val invalidPhone = "0171234"
        
        // When
        viewModel.updatePhoneNumber(invalidPhone)
        
        // Then
        assertThat(viewModel.isValidPhone.value).isFalse()
        assertThat(viewModel.errorMessage.value).isNotEmpty()
    }

    @Test
    fun `phone number validation - invalid format`() = testScope.runTest {
        // Given
        val invalidPhone = "1234567890" // Doesn't start with 01
        
        // When
        viewModel.updatePhoneNumber(invalidPhone)
        
        // Then
        assertThat(viewModel.isValidPhone.value).isFalse()
        assertThat(viewModel.errorMessage.value).contains("01")
    }

    @Test
    fun `send OTP - success`() = testScope.runTest {
        // Given
        val validPhone = "01712345678"
        coEvery { mockAuthRepository.sendOTP(validPhone) } returns flow { 
            emit(Result.success("OTP sent successfully")) 
        }
        
        // When
        viewModel.updatePhoneNumber(validPhone)
        viewModel.sendOTP()
        
        // Then
        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.errorMessage.value).isEmpty()
        
        coVerify { mockAuthRepository.sendOTP(validPhone) }
    }

    @Test
    fun `send OTP - network error`() = testScope.runTest {
        // Given
        val validPhone = "01712345678"
        val errorMessage = "Network error"
        coEvery { mockAuthRepository.sendOTP(validPhone) } returns flow { 
            emit(Result.failure(Exception(errorMessage))) 
        }
        
        // When
        viewModel.updatePhoneNumber(validPhone)
        viewModel.sendOTP()
        
        // Then
        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(viewModel.errorMessage.value).contains(errorMessage)
    }

    @Test
    fun `phone number formatting - automatic formatting`() = testScope.runTest {
        // Given
        val unformattedPhone = "1712345678"
        
        // When
        viewModel.updatePhoneNumber(unformattedPhone)
        
        // Then - should auto-add 0 prefix for Bangladesh numbers
        assertThat(viewModel.phoneNumber.value).isEqualTo("01712345678")
    }

    @Test
    fun `loading state - correct during OTP send`() = testScope.runTest {
        // Given
        val validPhone = "01712345678"
        val slot = slot<String>()
        coEvery { mockAuthRepository.sendOTP(capture(slot)) } coAnswers {
            flow { 
                emit(Result.success("OTP sent")) 
            }
        }
        
        // When
        viewModel.updatePhoneNumber(validPhone)
        
        // Initially not loading
        assertThat(viewModel.isLoading.value).isFalse()
        
        viewModel.sendOTP()
        
        // Should complete quickly in test
        advanceUntilIdle()
        
        // Then
        assertThat(viewModel.isLoading.value).isFalse()
        assertThat(slot.captured).isEqualTo(validPhone)
    }
}