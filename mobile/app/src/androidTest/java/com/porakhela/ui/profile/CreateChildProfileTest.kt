package com.porakhela.ui.profile

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
 * UI Tests for Child Profile Creation
 * Validates the complete child setup flow
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CreateChildProfileTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
        // Assume we've already gone through login/OTP flow
        // and are now at the child profile creation screen
    }

    @Test
    fun create_child_profile_with_valid_data_should_succeed() {
        // Navigate to create child screen (if not already there)
        onView(withId(R.id.button_create_child))
            .check(matches(isDisplayed()))
            .perform(click())

        // Enter child name
        onView(withId(R.id.edit_text_child_name))
            .perform(typeText("Arman Rahman"), closeSoftKeyboard())

        // Select grade (assuming we have a dropdown or picker)
        onView(withId(R.id.spinner_grade))
            .perform(click())
        onView(withText("Class 6"))
            .perform(click())

        // Select avatar (assuming we have avatar options)
        onView(withId(R.id.recycler_view_avatars))
            .check(matches(isDisplayed()))
        
        onView(allOf(
            withId(R.id.image_view_avatar),
            withContentDescription("Boy Avatar")
        )).perform(click())

        // Select subjects of interest
        onView(withText("Mathematics"))
            .perform(click())
        onView(withText("Science"))
            .perform(click())
        onView(withText("Bangla"))
            .perform(click())

        // Tap create profile button
        onView(withId(R.id.button_create_profile))
            .check(matches(isEnabled()))
            .perform(click())

        // Verify success message or navigation to dashboard
        onView(withText(containsString("Profile created")))
            .check(matches(isDisplayed()))
        
        // Or check if we navigated to dashboard
        onView(withId(R.id.text_view_welcome))
            .check(matches(withText(containsString("Welcome, Arman"))))
    }

    @Test
    fun create_child_profile_with_empty_name_should_show_error() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Leave name field empty
        onView(withId(R.id.edit_text_child_name))
            .perform(clearText())

        // Select grade
        onView(withId(R.id.spinner_grade))
            .perform(click())
        onView(withText("Class 5"))
            .perform(click())

        // Try to create profile
        onView(withId(R.id.button_create_profile))
            .perform(click())

        // Verify error message
        onView(withText(containsString("Please enter child's name")))
            .check(matches(isDisplayed()))

        // Verify we're still on the same screen
        onView(withId(R.id.edit_text_child_name))
            .check(matches(isDisplayed()))
    }

    @Test
    fun create_child_profile_without_grade_should_show_error() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Enter name but don't select grade
        onView(withId(R.id.edit_text_child_name))
            .perform(typeText("Fatima"), closeSoftKeyboard())

        // Select avatar
        onView(allOf(
            withId(R.id.image_view_avatar),
            withContentDescription("Girl Avatar")
        )).perform(click())

        // Try to create profile without selecting grade
        onView(withId(R.id.button_create_profile))
            .perform(click())

        // Verify error message
        onView(withText(containsString("Please select a grade")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun create_child_profile_without_avatar_should_use_default() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Enter valid data but skip avatar selection
        onView(withId(R.id.edit_text_child_name))
            .perform(typeText("Karim"), closeSoftKeyboard())

        onView(withId(R.id.spinner_grade))
            .perform(click())
        onView(withText("Class 7"))
            .perform(click())

        // Create profile without selecting avatar
        onView(withId(R.id.button_create_profile))
            .perform(click())

        // Should succeed with default avatar
        onView(withText(containsString("Profile created")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun avatar_selection_should_update_preview() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Check initial avatar preview (should be empty or default)
        onView(withId(R.id.image_view_avatar_preview))
            .check(matches(isDisplayed()))

        // Select different avatars and verify preview updates
        onView(allOf(
            withId(R.id.image_view_avatar),
            withContentDescription("Boy Avatar")
        )).perform(click())

        // Verify preview shows selected avatar
        onView(withId(R.id.image_view_avatar_preview))
            .check(matches(withContentDescription(containsString("Boy Avatar"))))

        // Select another avatar
        onView(allOf(
            withId(R.id.image_view_avatar),
            withContentDescription("Girl Avatar")
        )).perform(click())

        onView(withId(R.id.image_view_avatar_preview))
            .check(matches(withContentDescription(containsString("Girl Avatar"))))
    }

    @Test
    fun subject_selection_should_allow_multiple_choices() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Select multiple subjects
        onView(withText("Mathematics"))
            .perform(click())
        onView(withText("Science"))
            .perform(click())
        onView(withText("English"))
            .perform(click())

        // Verify subjects are selected (assuming checkboxes or similar)
        onView(allOf(
            withText("Mathematics"),
            isChecked()
        )).check(matches(isDisplayed()))
        
        onView(allOf(
            withText("Science"),
            isChecked()
        )).check(matches(isDisplayed()))

        // Deselect one subject
        onView(withText("English"))
            .perform(click())

        onView(allOf(
            withText("English"),
            not(isChecked())
        )).check(matches(isDisplayed()))
    }

    @Test
    fun back_button_should_return_to_previous_screen() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Verify we're on child creation screen
        onView(withId(R.id.edit_text_child_name))
            .check(matches(isDisplayed()))

        // Press back button
        onView(withId(R.id.button_back))
            .perform(click())

        // Verify we're back to previous screen
        onView(withId(R.id.button_create_child))
            .check(matches(isDisplayed()))
    }

    @Test
    fun create_child_profile_should_handle_long_names() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Enter a very long name
        val longName = "Mohammad Abdullah Rahman Al Mahmud"
        onView(withId(R.id.edit_text_child_name))
            .perform(typeText(longName), closeSoftKeyboard())

        onView(withId(R.id.spinner_grade))
            .perform(click())
        onView(withText("Class 4"))
            .perform(click())

        onView(withId(R.id.button_create_profile))
            .perform(click())

        // Should succeed or show appropriate validation
        onView(anyOf(
            withText(containsString("Profile created")),
            withText(containsString("Name too long"))
        )).check(matches(isDisplayed()))
    }

    @Test
    fun create_child_profile_with_bangla_name_should_work() {
        onView(withId(R.id.button_create_child))
            .perform(click())

        // Enter Bangla name
        onView(withId(R.id.edit_text_child_name))
            .perform(typeText("রাহুল খান"), closeSoftKeyboard())

        onView(withId(R.id.spinner_grade))
            .perform(click())
        onView(withText("Class 3"))
            .perform(click())

        onView(withId(R.id.button_create_profile))
            .perform(click())

        // Verify success with Bangla name
        onView(withText(containsString("Profile created")))
            .check(matches(isDisplayed()))
    }
}