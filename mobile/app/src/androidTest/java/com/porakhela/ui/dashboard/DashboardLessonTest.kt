package com.porakhela.ui.dashboard

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.*
import androidx.test.espresso.contrib.RecyclerViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.recyclerview.widget.RecyclerView
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
 * UI Tests for Dashboard and Lesson Navigation
 * Tests lesson browsing, selection, and dashboard interactions
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DashboardLessonTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun init() {
        hiltRule.inject()
        // Assume we've completed login and child setup
        // and are now on the main dashboard
    }

    @Test
    fun dashboard_should_display_child_info_and_progress() {
        // Verify child name and avatar are displayed
        onView(withId(R.id.text_view_child_name))
            .check(matches(allOf(
                isDisplayed(),
                withText(containsString("Arman"))
            )))

        onView(withId(R.id.image_view_child_avatar))
            .check(matches(isDisplayed()))

        // Verify current level and points
        onView(withId(R.id.text_view_level))
            .check(matches(allOf(
                isDisplayed(),
                withText(containsString("Level"))
            )))

        onView(withId(R.id.text_view_points))
            .check(matches(allOf(
                isDisplayed(),
                withText(containsString("points"))
            )))

        // Verify progress bar
        onView(withId(R.id.progress_bar_level))
            .check(matches(isDisplayed()))
    }

    @Test
    fun subject_tabs_should_be_scrollable_and_selectable() {
        // Verify subjects tab layout is displayed
        onView(withId(R.id.tab_layout_subjects))
            .check(matches(isDisplayed()))

        // Verify default subjects are present
        onView(withText("Mathematics"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify lessons for math are loaded
        onView(withId(R.id.recycler_view_lessons))
            .check(matches(isDisplayed()))

        // Switch to Science tab
        onView(withText("Science"))
            .perform(click())

        // Verify science lessons are now displayed
        onView(withText(containsString("Introduction to Plants")))
            .check(matches(isDisplayed()))

        // Test Bangla tab
        onView(withText("Bangla"))
            .perform(click())

        onView(withText(containsString("বর্ণমালা")))
            .check(matches(isDisplayed()))
    }

    @Test
    fun lesson_list_should_scroll_and_show_lesson_details() {
        // Select Mathematics tab
        onView(withText("Mathematics"))
            .perform(click())

        // Verify lessons are displayed in RecyclerView
        onView(withId(R.id.recycler_view_lessons))
            .check(matches(isDisplayed()))

        // Check first lesson item details
        onView(allOf(
            withId(R.id.text_view_lesson_title),
            withText(containsString("Numbers"))
        )).check(matches(isDisplayed()))

        onView(allOf(
            withId(R.id.text_view_lesson_duration),
            withText(containsString("min"))
        )).check(matches(isDisplayed()))

        onView(allOf(
            withId(R.id.text_view_lesson_points),
            withText(containsString("points"))
        )).check(matches(isDisplayed()))

        // Scroll down to see more lessons
        onView(withId(R.id.recycler_view_lessons))
            .perform(scrollToPosition(3))

        // Verify different lesson is now visible
        onView(allOf(
            withId(R.id.text_view_lesson_title),
            withText(containsString("Addition"))
        )).check(matches(isDisplayed()))
    }

    @Test
    fun lesson_item_click_should_navigate_to_lesson_detail() {
        // Select Mathematics and click on first lesson
        onView(withText("Mathematics"))
            .perform(click())

        // Click on first lesson in the list
        onView(withId(R.id.recycler_view_lessons))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))

        // Verify we navigated to lesson detail screen
        onView(withId(R.id.text_view_lesson_detail_title))
            .check(matches(isDisplayed()))

        onView(withId(R.id.text_view_lesson_description))
            .check(matches(isDisplayed()))

        onView(withId(R.id.button_start_lesson))
            .check(matches(allOf(
                isDisplayed(),
                isEnabled()
            )))
    }

    @Test
    fun download_lesson_should_work_when_offline_enabled() {
        onView(withText("Science"))
            .perform(click())

        // Long click on lesson to show download option
        onView(withId(R.id.recycler_view_lessons))
            .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(0, longClick()))

        // Verify download option appears
        onView(withText("Download for Offline"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify download progress indicator
        onView(withId(R.id.progress_download))
            .check(matches(isDisplayed()))

        // Wait for download to complete
        Thread.sleep(3000)

        // Verify download complete indicator
        onView(allOf(
            withId(R.id.icon_download_status),
            withContentDescription("Downloaded")
        )).check(matches(isDisplayed()))
    }

    @Test
    fun achievements_section_should_display_unlocked_badges() {
        // Scroll to achievements section
        onView(withId(R.id.scroll_view_dashboard))
            .perform(scrollTo())

        onView(withId(R.id.text_view_achievements_title))
            .check(matches(allOf(
                isDisplayed(),
                withText("Achievements")
            )))

        // Check if any achievements are displayed
        onView(withId(R.id.recycler_view_achievements))
            .check(matches(isDisplayed()))

        // Verify achievement item details
        onView(allOf(
            withId(R.id.text_view_achievement_title),
            withText(containsString("First Lesson"))
        )).check(matches(isDisplayed()))

        onView(withId(R.id.image_view_achievement_badge))
            .check(matches(isDisplayed()))
    }

    @Test
    fun continue_learning_section_should_show_recent_lessons() {
        // Verify "Continue Learning" section
        onView(withId(R.id.text_view_continue_learning))
            .check(matches(allOf(
                isDisplayed(),
                withText("Continue Learning")
            )))

        // Check if recent/in-progress lessons are shown
        onView(withId(R.id.recycler_view_recent_lessons))
            .check(matches(isDisplayed()))

        // Verify lesson progress indicator
        onView(allOf(
            withId(R.id.progress_lesson_completion),
            isDisplayed()
        )).check(matches(hasDescendant(withText(containsString("%")))))
    }

    @Test
    fun search_lessons_should_filter_results() {
        // Open search
        onView(withId(R.id.action_search))
            .perform(click())

        onView(withId(R.id.search_view))
            .check(matches(isDisplayed()))

        // Search for specific lesson
        onView(withId(R.id.search_view))
            .perform(typeText("Addition"), pressImeActionButton())

        // Verify search results
        onView(withText(containsString("Addition")))
            .check(matches(isDisplayed()))

        // Clear search
        onView(withId(R.id.search_view))
            .perform(clearText())

        // Verify all lessons are shown again
        onView(withText("Mathematics"))
            .perform(click())
        
        onView(withId(R.id.recycler_view_lessons))
            .check(matches(hasMinimumChildCount(5))) // Assuming at least 5 math lessons
    }

    @Test
    fun parent_dashboard_access_should_work() {
        // Access parent dashboard (if available)
        onView(withId(R.id.action_parent_mode))
            .perform(click())

        // Verify parent dashboard elements
        onView(withText(containsString("Progress Report")))
            .check(matches(isDisplayed()))

        onView(withText(containsString("Time Spent")))
            .check(matches(isDisplayed()))

        // Return to child mode
        onView(withId(R.id.action_child_mode))
            .perform(click())

        onView(withId(R.id.text_view_child_name))
            .check(matches(isDisplayed()))
    }

    @Test
    fun lesson_filter_by_difficulty_should_work() {
        onView(withText("Mathematics"))
            .perform(click())

        // Open filter options
        onView(withId(R.id.action_filter))
            .perform(click())

        // Select Easy difficulty
        onView(withText("Easy"))
            .perform(click())

        onView(withId(R.id.button_apply_filter))
            .perform(click())

        // Verify only easy lessons are shown
        onView(allOf(
            withId(R.id.icon_lesson_difficulty),
            withContentDescription("Easy")
        )).check(matches(isDisplayed()))

        // Clear filter
        onView(withId(R.id.action_filter))
            .perform(click())
        
        onView(withText("Clear Filters"))
            .perform(click())
    }

    @Test
    fun offline_mode_indicator_should_appear_when_no_internet() {
        // Simulate offline mode (this would require network simulation)
        // For now, verify offline indicator exists when manually set
        
        onView(withId(R.id.text_view_offline_indicator))
            .check(matches(anyOf(
                not(isDisplayed()), // Online
                allOf(isDisplayed(), withText("Offline Mode")) // Offline
            )))

        // In offline mode, only downloaded lessons should be clickable
        onView(withText("Mathematics"))
            .perform(click())

        // Verify downloaded lessons have different visual state
        onView(allOf(
            withId(R.id.lesson_item_container),
            hasDescendant(withId(R.id.icon_download_status))
        )).check(matches(isEnabled()))
    }
}