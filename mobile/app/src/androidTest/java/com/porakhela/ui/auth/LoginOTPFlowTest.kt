package com.porakhela.ui.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.porakhela.MainActivity
import com.porakhela.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests for Login and OTP Flow
 * Tests the complete authentication user journey
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LoginOTPFlowTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun login_with_valid_phone_should_navigate_to_otp_screen() {
        // Enter valid Bangladesh phone number
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())

        // Tap send OTP button
        onView(withId(R.id.button_send_otp))
            .check(matches(isEnabled()))
            .perform(click())

        // Verify loading state appears
        onView(withId(R.id.progress_bar))
            .check(matches(isDisplayed()))

        // Wait for navigation to OTP screen
        Thread.sleep(2000) // In real test, use IdlingResource

        // Verify OTP screen is displayed
        onView(withId(R.id.text_view_otp_sent))
            .check(matches(isDisplayed()))
        
        onView(withText(containsString("01712345678")))
            .check(matches(isDisplayed()))

        // Verify OTP input fields are present
        onView(withId(R.id.edit_text_otp_1))
            .check(matches(isDisplayed()))
    }

    @Test
    fun login_with_invalid_phone_should_show_error() {
        // Test various invalid phone numbers
        val invalidPhones = listOf(
            "123456789",     // Too short
            "12345678901",   // Too long  
            "02123456789",   // Wrong prefix
            "abc1234567",    // Contains letters
            "+8801712345678" // Contains plus sign
        )

        for (invalidPhone in invalidPhones) {
            onView(withId(R.id.edit_text_phone))
                .perform(clearText(), typeText(invalidPhone), closeSoftKeyboard())

            onView(withId(R.id.button_send_otp))
                .perform(click())

            // Verify error message appears
            onView(withText(containsString("valid phone number")))
                .check(matches(isDisplayed()))

            // Verify we're still on login screen
            onView(withId(R.id.edit_text_phone))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun phone_number_should_format_automatically() {
        // Enter phone number without prefix
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("1712345678"))

        // Verify it gets formatted to 01712345678
        onView(withId(R.id.edit_text_phone))
            .check(matches(withText("01712345678")))

        // Enter with spaces
        onView(withId(R.id.edit_text_phone))
            .perform(clearText(), typeText("017 1234 5678"))

        // Verify spaces are removed
        onView(withId(R.id.edit_text_phone))
            .check(matches(withText("01712345678")))
    }

    @Test
    fun otp_verification_with_valid_code_should_succeed() {
        // First navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000) // Wait for navigation

        // Enter valid OTP (for testing, assume 123456 is always valid)
        onView(withId(R.id.edit_text_otp_1))
            .perform(typeText("1"))
        onView(withId(R.id.edit_text_otp_2))
            .perform(typeText("2"))
        onView(withId(R.id.edit_text_otp_3))
            .perform(typeText("3"))
        onView(withId(R.id.edit_text_otp_4))
            .perform(typeText("4"))
        onView(withId(R.id.edit_text_otp_5))
            .perform(typeText("5"))
        onView(withId(R.id.edit_text_otp_6))
            .perform(typeText("6"))

        // Verify verify button is enabled
        onView(withId(R.id.button_verify_otp))
            .check(matches(isEnabled()))
            .perform(click())

        // Wait for verification
        Thread.sleep(3000)

        // Verify navigation to dashboard or child profile creation
        onView(anyOf(
            withId(R.id.button_create_child),
            withId(R.id.text_view_welcome)
        )).check(matches(isDisplayed()))
    }

    @Test
    fun otp_verification_with_invalid_code_should_show_error() {
        // Navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000)

        // Enter invalid OTP
        onView(withId(R.id.edit_text_otp_1))
            .perform(typeText("0"))
        onView(withId(R.id.edit_text_otp_2))
            .perform(typeText("0"))
        onView(withId(R.id.edit_text_otp_3))
            .perform(typeText("0"))
        onView(withId(R.id.edit_text_otp_4))
            .perform(typeText("0"))
        onView(withId(R.id.edit_text_otp_5))
            .perform(typeText("0"))
        onView(withId(R.id.edit_text_otp_6))
            .perform(typeText("0"))

        onView(withId(R.id.button_verify_otp))
            .perform(click())

        // Verify error message
        onView(withText(containsString("Invalid OTP")))
            .check(matches(isDisplayed()))

        // Verify we're still on OTP screen
        onView(withId(R.id.edit_text_otp_1))
            .check(matches(isDisplayed()))
    }

    @Test
    fun otp_auto_progression_should_work() {
        // Navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000)

        // Type first digit
        onView(withId(R.id.edit_text_otp_1))
            .perform(typeText("1"))

        // Verify focus moves to second field
        onView(withId(R.id.edit_text_otp_2))
            .check(matches(hasFocus()))

        // Continue typing
        onView(withId(R.id.edit_text_otp_2))
            .perform(typeText("2"))
        
        onView(withId(R.id.edit_text_otp_3))
            .check(matches(hasFocus()))
    }

    @Test
    fun otp_timer_should_countdown_and_enable_resend() {
        // Navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000)

        // Verify timer is displayed
        onView(withId(R.id.text_view_timer))
            .check(matches(allOf(
                isDisplayed(),
                withText(containsString("1:"))
            )))

        // Verify resend button is disabled initially
        onView(withId(R.id.button_resend_otp))
            .check(matches(allOf(
                isDisplayed(),
                not(isEnabled())
            )))

        // Wait for timer to count down (testing with shorter wait)
        Thread.sleep(5000)

        // Timer should show less time
        onView(withId(R.id.text_view_timer))
            .check(matches(withText(containsString("0:5"))))
    }

    @Test
    fun resend_otp_should_work_after_timer_expires() {
        // Navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000)

        // Wait for timer to expire (in real app, this would be 60 seconds)
        // For testing purposes, assume timer is shorter or we mock it
        Thread.sleep(1000) // Simulate timer expiry

        // Resend button should be enabled
        onView(withId(R.id.button_resend_otp))
            .check(matches(isEnabled()))
            .perform(click())

        // Verify success message
        onView(withText(containsString("OTP sent")))
            .check(matches(isDisplayed()))

        // Timer should restart
        onView(withId(R.id.text_view_timer))
            .check(matches(withText(containsString("1:0"))))
    }

    @Test
    fun back_button_from_otp_screen_should_return_to_login() {
        // Navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000)

        // Verify we're on OTP screen
        onView(withId(R.id.text_view_otp_sent))
            .check(matches(isDisplayed()))

        // Press back button
        onView(withId(R.id.button_back))
            .perform(click())

        // Verify we're back to login screen
        onView(withId(R.id.edit_text_phone))
            .check(matches(isDisplayed()))
        onView(withId(R.id.button_send_otp))
            .check(matches(isDisplayed()))
    }

    @Test
    fun otp_fields_should_handle_backspace_properly() {
        // Navigate to OTP screen
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000)

        // Fill first few fields
        onView(withId(R.id.edit_text_otp_1))
            .perform(typeText("1"))
        onView(withId(R.id.edit_text_otp_2))
            .perform(typeText("2"))
        onView(withId(R.id.edit_text_otp_3))
            .perform(typeText("3"))

        // Now focus should be on field 4
        onView(withId(R.id.edit_text_otp_4))
            .check(matches(hasFocus()))

        // Press backspace (simulate by clearing field 3)
        onView(withId(R.id.edit_text_otp_3))
            .perform(click(), clearText())

        // Focus should move back to field 3
        onView(withId(R.id.edit_text_otp_3))
            .check(matches(hasFocus()))
    }
}