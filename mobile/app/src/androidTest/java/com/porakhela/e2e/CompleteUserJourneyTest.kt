package com.porakhela.e2e

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiDevice
import androidx.test.platform.app.InstrumentationRegistry
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
 * End-to-End Test for Complete User Journey
 * Tests the entire flow from app launch to lesson completion
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CompleteUserJourneyTest {

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
    fun complete_user_journey_new_user_to_lesson_completion() {
        // ==== PHASE 1: App Launch & Onboarding ====
        
        // Verify splash screen or welcome screen appears
        onView(anyOf(
            withId(R.id.image_view_logo),
            withId(R.id.text_view_welcome)
        )).check(matches(isDisplayed()))

        // Skip onboarding if present, or proceed directly to login
        tryPerformAction(withId(R.id.button_skip_intro)) { 
            perform(click()) 
        }

        // Wait for login screen
        waitForView(withId(R.id.edit_text_phone), 3000)

        // ==== PHASE 2: Authentication Flow ====
        
        // Enter phone number
        onView(withId(R.id.edit_text_phone))
            .perform(typeText("01712345678"), closeSoftKeyboard())

        // Send OTP
        onView(withId(R.id.button_send_otp))
            .check(matches(isEnabled()))
            .perform(click())

        // Wait for OTP screen
        waitForView(withId(R.id.text_view_otp_sent), 5000)

        // Verify phone number is displayed correctly
        onView(withText(containsString("01712345678")))
            .check(matches(isDisplayed()))

        // Enter OTP (using test OTP)
        enterOTP("123456")

        // Verify OTP and proceed
        onView(withId(R.id.button_verify_otp))
            .check(matches(isEnabled()))
            .perform(click())

        // Wait for verification
        Thread.sleep(3000)

        // ==== PHASE 3: Child Profile Creation ====
        
        // Should navigate to child profile creation or dashboard
        if (isViewDisplayed(withId(R.id.button_create_child))) {
            // New user - create child profile
            createChildProfile()
        }

        // Wait for dashboard to load
        waitForView(withId(R.id.text_view_child_name), 5000)

        // ==== PHASE 4: Dashboard Interaction ====
        
        // Verify child dashboard is displayed
        onView(withId(R.id.text_view_child_name))
            .check(matches(allOf(
                isDisplayed(),
                withText(containsString("Arman"))
            )))

        // Verify level and points are displayed
        onView(withId(R.id.text_view_level))
            .check(matches(withText(containsString("Level"))))

        onView(withId(R.id.text_view_points))
            .check(matches(withText(containsString("points"))))

        // ==== PHASE 5: Subject Selection & Lesson Browsing ====
        
        // Select Mathematics subject
        onView(withText("Mathematics"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify lessons are loaded
        waitForView(withId(R.id.recycler_view_lessons), 3000)

        // Scroll through lessons to verify they load properly
        onView(withId(R.id.recycler_view_lessons))
            .perform(scrollToPosition(2))

        // ==== PHASE 6: Lesson Selection & Detail View ====
        
        // Select first lesson
        onView(withId(R.id.recycler_view_lessons))
            .perform(actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(0, click()))

        // Wait for lesson detail screen
        waitForView(withId(R.id.text_view_lesson_detail_title), 3000)

        // Verify lesson details are displayed
        onView(withId(R.id.text_view_lesson_description))
            .check(matches(isDisplayed()))

        onView(withId(R.id.text_view_lesson_duration))
            .check(matches(isDisplayed()))

        onView(withId(R.id.button_start_lesson))
            .check(matches(allOf(isDisplayed(), isEnabled())))

        // ==== PHASE 7: Start Lesson ====
        
        onView(withId(R.id.button_start_lesson))
            .perform(click())

        // Wait for lesson content to load
        waitForView(withId(R.id.text_view_lesson_content_title), 5000)

        // ==== PHASE 8: Lesson Content Interaction ====
        
        // Verify lesson content is displayed
        onView(withId(R.id.text_view_lesson_content))
            .check(matches(isDisplayed()))

        // If video is present, verify it loads
        if (isViewDisplayed(withId(R.id.video_view_lesson))) {
            onView(withId(R.id.video_view_lesson))
                .check(matches(isDisplayed()))
        }

        // Scroll through lesson content
        onView(withId(R.id.scroll_view_lesson_content))
            .perform(scrollTo())

        // ==== PHASE 9: Exercise Completion ====
        
        // Navigate to exercises
        onView(withId(R.id.button_start_exercises))
            .perform(click())

        waitForView(withId(R.id.text_view_exercise_question), 3000)

        // Complete first exercise
        completeExercise()

        // Verify exercise feedback
        onView(withText(anyOf(
            containsString("Correct"),
            containsString("Great job"),
            containsString("সঠিক")
        ))).check(matches(isDisplayed()))

        // Continue to next exercise if available
        if (isViewDisplayed(withId(R.id.button_next_exercise))) {
            onView(withId(R.id.button_next_exercise))
                .perform(click())
            
            // Complete second exercise
            completeExercise()
        }

        // ==== PHASE 10: Lesson Completion ====
        
        // Complete all exercises and finish lesson
        if (isViewDisplayed(withId(R.id.button_finish_lesson))) {
            onView(withId(R.id.button_finish_lesson))
                .perform(click())
        }

        // Wait for completion screen
        waitForView(withId(R.id.text_view_lesson_completed), 3000)

        // Verify completion rewards
        onView(withText(containsString("Points Earned")))
            .check(matches(isDisplayed()))

        onView(withId(R.id.text_view_points_earned))
            .check(matches(isDisplayed()))

        // Check for achievement unlock
        if (isViewDisplayed(withId(R.id.text_view_achievement_unlocked))) {
            onView(withId(R.id.text_view_achievement_unlocked))
                .check(matches(withText(containsString("Achievement"))))
        }

        // ==== PHASE 11: Return to Dashboard ====
        
        onView(withId(R.id.button_back_to_dashboard))
            .perform(click())

        // Wait for dashboard
        waitForView(withId(R.id.text_view_child_name), 3000)

        // ==== PHASE 12: Verify Progress Updated ====
        
        // Verify points increased
        onView(withId(R.id.text_view_points))
            .check(matches(withText(containsString("100")))) // Assuming lesson gave 100 points

        // Verify level progress updated
        onView(withId(R.id.progress_bar_level))
            .check(matches(isDisplayed()))

        // Check if lesson shows as completed
        onView(withText("Mathematics"))
            .perform(click())

        // Verify completed lesson has different visual state
        onView(allOf(
            withId(R.id.icon_lesson_status),
            withContentDescription("Completed")
        )).check(matches(isDisplayed()))

        // ==== PHASE 13: Parent Dashboard Verification ====
        
        // Switch to parent mode if available
        if (isViewDisplayed(withId(R.id.action_parent_mode))) {
            onView(withId(R.id.action_parent_mode))
                .perform(click())

            // Verify progress shows in parent dashboard
            waitForView(withText(containsString("Progress Report")), 3000)
            
            onView(withText(containsString("Lessons Completed: 1")))
                .check(matches(isDisplayed()))

            onView(withText(containsString("Time Spent")))
                .check(matches(isDisplayed()))

            // Return to child mode
            onView(withId(R.id.action_child_mode))
                .perform(click())
        }

        // ==== JOURNEY COMPLETION ====
        
        // Verify we're back at dashboard with updated state
        onView(withId(R.id.text_view_child_name))
            .check(matches(isDisplayed()))

        // Journey completed successfully!
    }

    // Helper Functions

    private fun enterOTP(otp: String) {
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

    private fun createChildProfile() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        waitForView(withId(R.id.edit_text_child_name), 2000)

        // Enter child details
        onView(withId(R.id.edit_text_child_name))
            .perform(typeText("Arman Rahman"), closeSoftKeyboard())

        // Select grade
        onView(withId(R.id.spinner_grade))
            .perform(click())
        onView(withText("Class 6"))
            .perform(click())

        // Select avatar
        onView(allOf(
            withId(R.id.image_view_avatar),
            withContentDescription("Boy Avatar")
        )).perform(click())

        // Select subjects
        onView(withText("Mathematics"))
            .perform(click())
        onView(withText("Science"))
            .perform(click())

        // Create profile
        onView(withId(R.id.button_create_profile))
            .perform(click())

        Thread.sleep(3000) // Wait for profile creation
    }

    private fun completeExercise() {
        // Verify exercise question is displayed
        onView(withId(R.id.text_view_exercise_question))
            .check(matches(isDisplayed()))

        // Select first answer option (assuming it's correct for testing)
        onView(allOf(
            withId(R.id.button_option_a),
            isDisplayed()
        )).perform(click())

        // Submit answer
        onView(withId(R.id.button_submit_answer))
            .perform(click())

        Thread.sleep(1500) // Wait for feedback
    }

    private fun waitForView(matcher: org.hamcrest.Matcher<android.view.View>, timeoutMs: Long) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (e: Exception) {
                Thread.sleep(100)
            }
        }
    }

    private fun isViewDisplayed(matcher: org.hamcrest.Matcher<android.view.View>): Boolean {
        return try {
            onView(matcher).check(matches(isDisplayed()))
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tryPerformAction(
        matcher: org.hamcrest.Matcher<android.view.View>, 
        action: () -> Unit
    ) {
        try {
            onView(matcher).check(matches(isDisplayed()))
            action()
        } catch (e: Exception) {
            // View not found, continue
        }
    }
}