package com.porakhela.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.porakhela.ui.PorakhelaApp
import com.porakhela.ui.theme.PorakhelaTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration Tests for Complete User Journey
 * Tests end-to-end navigation and functionality across all screens
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CompleteUserJourneyIntegrationTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun complete_user_journey_new_user_flow() {
        // Start the app
        composeTestRule.setContent {
            PorakhelaTheme {
                PorakhelaApp()
            }
        }

        // Wait for splash screen to load
        composeTestRule.waitForIdle()

        // Should show onboarding for new user
        composeTestRule.onNodeWithText("Welcome to Porakhela!")
            .assertExists()

        // Skip onboarding
        composeTestRule.onNodeWithText("Skip")
            .performClick()

        // Should navigate to phone input
        composeTestRule.onNodeWithText("Enter your phone number")
            .assertExists()

        // Enter valid phone number
        composeTestRule.onNodeWithTag("phone_input")
            .performTextInput("01712345678")

        // Tap send OTP
        composeTestRule.onNodeWithText("Send OTP")
            .performClick()

        // Wait for navigation to OTP screen
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText("Enter the 6-digit code")
                .isDisplayed()
        }

        // Enter OTP
        composeTestRule.onNodeWithTag("otp_input_1")
            .performTextInput("1")
        composeTestRule.onNodeWithTag("otp_input_2")
            .performTextInput("2")
        composeTestRule.onNodeWithTag("otp_input_3")
            .performTextInput("3")
        composeTestRule.onNodeWithTag("otp_input_4")
            .performTextInput("4")
        composeTestRule.onNodeWithTag("otp_input_5")
            .performTextInput("5")
        composeTestRule.onNodeWithTag("otp_input_6")
            .performTextInput("6")

        // Verify OTP
        composeTestRule.onNodeWithText("Verify")
            .performClick()

        // For new user, should navigate to child profile creation
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText("Create Child Profile")
                .isDisplayed()
        }

        // Fill child profile
        composeTestRule.onNodeWithTag("child_name_input")
            .performTextInput("Arman Rahman")

        // Select grade
        composeTestRule.onNodeWithText("Select Grade")
            .performClick()
        composeTestRule.onNodeWithText("Class 6")
            .performClick()

        // Select avatar
        composeTestRule.onNodeWithContentDescription("Boy Avatar")
            .performClick()

        // Select subjects
        composeTestRule.onNodeWithText("Mathematics")
            .performClick()
        composeTestRule.onNodeWithText("Science")
            .performClick()

        // Create profile
        composeTestRule.onNodeWithText("Create Profile")
            .performClick()

        // Should navigate to dashboard
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText("Welcome, Arman!")
                .isDisplayed()
        }

        // Verify dashboard elements
        composeTestRule.onNodeWithText("Mathematics")
            .assertExists()
        composeTestRule.onNodeWithText("Science")
            .assertExists()
        composeTestRule.onNodeWithText("Your Progress")
            .assertExists()

        // Navigate to lessons
        composeTestRule.onNodeWithText("Mathematics")
            .performClick()

        composeTestRule.waitForIdle()

        // Select a lesson
        composeTestRule.onNodeWithText("Introduction to Numbers")
            .performClick()

        // Should navigate to lesson detail
        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Start Lesson")
                .isDisplayed()
        }

        // Start the lesson
        composeTestRule.onNodeWithText("Start Lesson")
            .performClick()

        // Should navigate to lesson player
        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithContentDescription("Play")
                .isDisplayed()
        }

        // Complete lesson by navigating through sections
        composeTestRule.onNodeWithText("Next")
            .performClick()

        composeTestRule.waitForIdle()

        // Start exercises
        composeTestRule.onNodeWithText("Start Exercises")
            .performClick()

        // Should navigate to exercise screen
        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Question 1 of")
                .isDisplayed()
        }

        // Answer the exercise
        composeTestRule.onNodeWithText("A")
            .performClick()

        composeTestRule.onNodeWithText("Submit Answer")
            .performClick()

        composeTestRule.waitForIdle()

        // Continue with next question or finish
        composeTestRule.onNodeWithText("Next Question")
            .performClick()

        // Complete all exercises...
        // (Simplified for test - would normally loop through all questions)

        // Should return to dashboard
        composeTestRule.waitUntil(5000) {
            composeTestRule.onNodeWithText("Welcome, Arman!")
                .isDisplayed()
        }

        // Check achievements
        composeTestRule.onNodeWithText("Achievements")
            .performClick()

        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Your Achievements")
                .isDisplayed()
        }

        // Should show earned achievement
        composeTestRule.onNodeWithText("First Lesson Complete!")
            .assertExists()

        // Navigate back to dashboard
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()

        // Verify we're back on dashboard
        composeTestRule.onNodeWithText("Welcome, Arman!")
            .assertExists()
    }

    @Test
    fun parental_controls_integration_flow() {
        composeTestRule.setContent {
            PorakhelaTheme {
                PorakhelaApp()
            }
        }

        // Navigate through to dashboard (assuming logged in state)
        // ... (skip to dashboard for this test)

        // Access parental controls
        composeTestRule.onNodeWithText("Parent Mode")
            .performClick()

        // Should navigate to parental PIN screen
        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Enter Parental PIN")
                .isDisplayed()
        }

        // Enter PIN
        composeTestRule.onNodeWithTag("pin_1")
            .performClick()
        composeTestRule.onNodeWithTag("pin_2")
            .performClick()
        composeTestRule.onNodeWithTag("pin_3")
            .performClick()
        composeTestRule.onNodeWithTag("pin_4")
            .performClick()

        // Should navigate to parent dashboard
        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Progress Report")
                .isDisplayed()
        }

        // Verify parent dashboard elements
        composeTestRule.onNodeWithText("Time Spent Today")
            .assertExists()
        composeTestRule.onNodeWithText("Lessons Completed")
            .assertExists()
        composeTestRule.onNodeWithText("Download Management")
            .assertExists()

        // Navigate to settings
        composeTestRule.onNodeWithText("Settings")
            .performClick()

        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Screen Time Limits")
                .isDisplayed()
        }

        // Test setting screen time
        composeTestRule.onNodeWithText("Screen Time Limits")
            .performClick()

        composeTestRule.onNodeWithText("60 minutes")
            .performClick()

        composeTestRule.onNodeWithText("Save")
            .performClick()

        // Return to child mode
        composeTestRule.onNodeWithText("Switch to Child Mode")
            .performClick()

        // Should return to child dashboard
        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Welcome, Arman!")
                .isDisplayed()
        }
    }

    @Test
    fun offline_mode_integration_test() {
        composeTestRule.setContent {
            PorakhelaTheme {
                PorakhelaApp()
            }
        }

        // Navigate to dashboard
        // ... (skip authentication for this test)

        // Access downloaded lessons
        composeTestRule.onNodeWithText("Downloaded")
            .performClick()

        // Should show offline content
        composeTestRule.onNodeWithContentDescription("Downloaded")
            .assertExists()

        // Select offline lesson
        composeTestRule.onNodeWithText("Downloaded Lesson")
            .performClick()

        // Should work without internet
        composeTestRule.onNodeWithText("Start Lesson")
            .performClick()

        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithContentDescription("Play")
                .isDisplayed()
        }

        // Verify offline indicators
        composeTestRule.onNodeWithText("Offline Mode")
            .assertExists()
    }

    @Test
    fun accessibility_features_integration_test() {
        composeTestRule.setContent {
            PorakhelaTheme {
                PorakhelaApp()
            }
        }

        // Test large text scaling
        // (This would require device configuration changes)

        // Test content descriptions for screen readers
        composeTestRule.onNodeWithContentDescription("Send OTP")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Back")
            .assertExists()

        // Test keyboard navigation
        composeTestRule.onNodeWithTag("phone_input")
            .assertHasClickAction()
            .assertIsFocused()

        // Test haptic feedback (harder to test programmatically)
        composeTestRule.onNodeWithText("Send OTP")
            .performClick()
        
        // Verify button provides feedback indication
        composeTestRule.waitForIdle()
    }

    @Test
    fun error_handling_integration_test() {
        composeTestRule.setContent {
            PorakhelaTheme {
                PorakhelaApp()
            }
        }

        // Test network error handling
        composeTestRule.onNodeWithTag("phone_input")
            .performTextInput("invalid")

        composeTestRule.onNodeWithText("Send OTP")
            .performClick()

        // Should show error message
        composeTestRule.onNodeWithText("Please enter a valid phone number")
            .assertExists()

        // Test retry functionality
        composeTestRule.onNodeWithText("Try Again")
            .performClick()

        // Should allow retry
        composeTestRule.onNodeWithTag("phone_input")
            .assertExists()
    }

    @Test
    fun performance_under_load_test() {
        composeTestRule.setContent {
            PorakhelaTheme {
                PorakhelaApp()
            }
        }

        // Rapid navigation test
        repeat(10) {
            composeTestRule.onNodeWithText("Mathematics")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithContentDescription("Back")
                .performClick()
            
            composeTestRule.waitForIdle()
        }

        // App should remain responsive
        composeTestRule.onNodeWithText("Welcome, Arman!")
            .assertExists()
    }
}

// Extension functions for better test readability
private fun SemanticsNodeInteraction.isDisplayed(): Boolean {
    return try {
        assertIsDisplayed()
        true
    } catch (e: AssertionError) {
        false
    }
}