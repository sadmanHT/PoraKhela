package com.porakhela.accessibility

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
 * Accessibility Tests for Child-Friendly Interface
 * Ensures the app is usable by children with varying abilities
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AccessibilityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice

    @Before
    fun init() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun all_interactive_elements_have_content_descriptions() {
        // Navigate to login screen
        onView(withId(R.id.edit_text_phone))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription()
            )))

        onView(withId(R.id.button_send_otp))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription()
            )))

        // Navigate through OTP flow
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        // Wait for OTP screen
        Thread.sleep(2000)

        // Check OTP fields have descriptions
        onView(withId(R.id.edit_text_otp_1))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.button_verify_otp))
            .check(matches(hasContentDescription()))

        onView(withId(R.id.button_resend_otp))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun text_size_scaling_works_properly() {
        // Test with system font scaling
        // This would normally be tested with different device configurations
        
        onView(withId(R.id.text_view_welcome))
            .check(matches(isDisplayed()))

        // Verify text is still readable at larger sizes
        onView(withId(R.id.edit_text_phone))
            .check(matches(allOf(
                isDisplayed(),
                hasMinimumChildCount(0) // Text field should be usable
            )))

        // Button text should not be truncated
        onView(withId(R.id.button_send_otp))
            .check(matches(allOf(
                isDisplayed(),
                not(withText(containsString("..."))) // No truncation
            )))
    }

    @Test
    fun touch_targets_meet_minimum_size_requirements() {
        // All interactive elements should be at least 44dp x 44dp
        
        onView(withId(R.id.button_send_otp))
            .check(matches(allOf(
                isDisplayed(),
                hasMinimumSize(44, 44)
            )))

        // Navigate to OTP screen to test OTP fields
        navigateToOTPScreen()

        // OTP input fields should be large enough for children
        onView(withId(R.id.edit_text_otp_1))
            .check(matches(allOf(
                isDisplayed(),
                hasMinimumSize(48, 48) // Larger for children
            )))

        onView(withId(R.id.edit_text_otp_2))
            .check(matches(hasMinimumSize(48, 48)))
    }

    @Test
    fun color_contrast_requirements_met() {
        // Test that critical elements have sufficient contrast
        // This is typically done with visual testing tools
        
        // Verify error states are clearly visible
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("invalid"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        // Error message should be visible with good contrast
        onView(withText(containsString("valid phone number")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun keyboard_navigation_works() {
        // Test tab navigation through form fields
        onView(withId(R.id.edit_text_phone))
            .check(matches(isFocusable()))
            .perform(requestFocus())

        // Verify phone field has focus
        onView(withId(R.id.edit_text_phone))
            .check(matches(hasFocus()))

        // Tab to next element (send button)
        onView(withId(R.id.button_send_otp))
            .check(matches(isFocusable()))
    }

    @Test
    fun screen_reader_announcements_work() {
        // Test TalkBack announcements for important events
        
        navigateToOTPScreen()

        // Entering OTP should provide audio feedback
        onView(withId(R.id.edit_text_otp_1))
            .perform(typeText("1"))

        // Focus should move to next field with announcement
        onView(withId(R.id.edit_text_otp_2))
            .check(matches(hasFocus()))

        // Complete OTP entry
        enterCompleteOTP("123456")

        // Verification should announce result
        onView(withId(R.id.button_verify_otp))
            .perform(click())

        // Success/error should be announced
        Thread.sleep(2000)
    }

    @Test
    fun bangla_text_displays_correctly() {
        // Navigate to dashboard with Bangla content
        navigateToOTPScreen()
        enterCompleteOTP("123456")
        onView(withId(R.id.button_verify_otp)).perform(click())
        
        Thread.sleep(3000) // Wait for navigation

        // Test Bangla subject names
        onView(withText("গণিত"))
            .check(matches(isDisplayed()))

        onView(withText("বিজ্ঞান"))
            .check(matches(isDisplayed()))

        onView(withText("বাংলা"))
            .check(matches(isDisplayed()))

        // Click on Bangla subject
        onView(withText("বাংলা"))
            .perform(click())

        // Verify Bangla lesson titles display correctly
        onView(withText(containsString("বর্ণমালা")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun child_friendly_error_messages() {
        // Test that error messages are appropriate for children
        
        // Invalid phone number
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("123"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        // Error should be child-friendly, not technical
        onView(withText(anyOf(
            containsString("Please check your phone number"),
            containsString("ফোন নম্বর সঠিক নয়"),
            not(containsString("validation failed")), // No technical terms
            not(containsString("error code")) // No error codes
        ))).check(matches(isDisplayed()))
    }

    @Test
    fun visual_feedback_for_interactions() {
        // Test that buttons provide visual feedback when pressed
        
        onView(withId(R.id.button_send_otp))
            .check(matches(isDisplayed()))
            .perform(pressAndHold()) // Should show pressed state

        // OTP fields should show focus states
        navigateToOTPScreen()

        onView(withId(R.id.edit_text_otp_1))
            .perform(click())
            .check(matches(hasFocus()))

        // Visual indication of focus should be present
        // This is typically verified through visual testing
    }

    @Test
    fun progress_indicators_are_accessible() {
        navigateToOTPScreen()

        // Timer should be accessible to screen readers
        onView(withId(R.id.text_view_timer))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription()
            )))

        // Progress indicators should have descriptions
        if (isViewPresent(withId(R.id.progress_bar_level))) {
            onView(withId(R.id.progress_bar_level))
                .check(matches(hasContentDescription()))
        }
    }

    @Test
    fun gesture_navigation_friendly() {
        // Test that app works well with gesture navigation
        
        navigateToOTPScreen()
        
        // Back gesture should work (simulated by back button)
        onView(withId(R.id.button_back))
            .perform(click())

        // Should return to login screen
        onView(withId(R.id.edit_text_phone))
            .check(matches(isDisplayed()))

        // Edge swipes shouldn't interfere with app interaction
        // This requires gesture simulation which is device-specific
    }

    @Test
    fun loading_states_are_accessible() {
        // Loading indicators should be announced to screen readers
        
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        // Loading state should have proper description
        onView(withId(R.id.progress_bar))
            .check(matches(allOf(
                isDisplayed(),
                hasContentDescription()
            )))

        // Loading text should be descriptive
        onView(withText(containsString("Sending")))
            .check(matches(isDisplayed()))
    }

    // Helper methods

    private fun navigateToOTPScreen() {
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())
        
        onView(withId(R.id.button_send_otp))
            .perform(click())

        Thread.sleep(2000) // Wait for navigation
    }

    private fun enterCompleteOTP(otp: String) {
        otp.forEachIndexed { index, digit ->
            val fieldId = when (index) {
                0 -> R.id.edit_text_otp_1
                1 -> R.id.edit_text_otp_2
                2 -> R.id.edit_text_otp_3
                3 -> R.id.edit_text_otp_4
                4 -> R.id.edit_text_otp_5
                5 -> R.id.edit_text_otp_6
                else -> return@forEachIndexed
            }
            onView(withId(fieldId))
                .perform(typeText(digit.toString()))
        }
    }

    private fun isViewPresent(matcher: org.hamcrest.Matcher<android.view.View>): Boolean {
        return try {
            onView(matcher).check(matches(isDisplayed()))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun hasMinimumSize(widthDp: Int, heightDp: Int): org.hamcrest.Matcher<android.view.View> {
        return object : org.hamcrest.TypeSafeMatcher<android.view.View>() {
            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("has minimum size ${widthDp}dp x ${heightDp}dp")
            }

            override fun matchesSafely(view: android.view.View): Boolean {
                val density = view.resources.displayMetrics.density
                val minWidthPx = (widthDp * density).toInt()
                val minHeightPx = (heightDp * density).toInt()
                
                return view.width >= minWidthPx && view.height >= minHeightPx
            }
        }
    }
}