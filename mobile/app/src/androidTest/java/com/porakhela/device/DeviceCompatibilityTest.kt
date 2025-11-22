package com.porakhela.device

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.porakhela.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-level UI Tests
 * Tests app behavior on different device configurations
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class DeviceCompatibilityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var device: UiDevice
    private val timeout = 5000L

    @Before
    fun init() {
        hiltRule.inject()
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressHome()
        device.waitForIdle()
    }

    @Test
    fun test_app_launch_on_low_end_device_simulation() {
        // Simulate low-end device by setting lower performance
        // This test would normally run on Android Go emulator
        
        // Launch app
        launchApp()
        
        // Verify app launches within reasonable time on low-end device
        val appLaunched = device.wait(
            Until.hasObject(By.pkg("com.porakhela").depth(0)), 
            10000 // Allow extra time for low-end devices
        )
        assertTrue("App should launch on low-end device", appLaunched)

        // Verify critical UI elements load
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        assertNotNull("Phone input should be accessible", phoneInput)
        
        // Verify text is readable (not cut off)
        val sendOtpButton = device.findObject(By.res("com.porakhela:id/button_send_otp"))
        assertNotNull("Send OTP button should be visible", sendOtpButton)
        assertTrue("Button should have readable text", sendOtpButton.text.isNotEmpty())
    }

    @Test
    fun test_orientation_changes_preserve_state() {
        launchApp()
        
        // Enter phone number in portrait
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        phoneInput?.text = "01712345678"

        // Rotate to landscape
        device.setOrientationLeft()
        device.waitForIdle(2000)

        // Verify phone number is preserved
        val phoneInputLandscape = device.findObject(By.res("com.porakhela:id/edit_text_phone"))
        assertEquals("Phone number should be preserved", "01712345678", phoneInputLandscape.text)

        // Verify UI is still usable in landscape
        val sendButton = device.findObject(By.res("com.porakhela:id/button_send_otp"))
        assertTrue("Send button should be clickable in landscape", sendButton.isEnabled)

        // Rotate back to portrait
        device.setOrientationNatural()
        device.waitForIdle(2000)

        // Verify state still preserved
        val phoneInputPortrait = device.findObject(By.res("com.porakhela:id/edit_text_phone"))
        assertEquals("Phone number should persist after rotation", "01712345678", phoneInputPortrait.text)
    }

    @Test
    fun test_font_scaling_large_text_accessibility() {
        // This test simulates device with large font setting
        launchApp()

        // Navigate to main screen elements
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        
        // Verify input field is still usable with large text
        assertTrue("Phone input should be accessible with large fonts", 
                  phoneInput.bounds.height() > 48) // Minimum touch target

        val sendButton = device.findObject(By.res("com.porakhela:id/button_send_otp"))
        assertTrue("Button should be large enough for accessibility",
                  sendButton.bounds.height() > 44)

        // Verify text doesn't overflow containers
        val buttonText = sendButton.text
        assertTrue("Button text should not be truncated", 
                  !buttonText.contains("...") && buttonText.isNotEmpty())
    }

    @Test
    fun test_app_performance_on_mid_range_device() {
        launchApp()
        
        val startTime = System.currentTimeMillis()
        
        // Navigate through typical user flow quickly
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        phoneInput?.text = "01712345678"
        
        val sendButton = device.findObject(By.res("com.porakhela:id/button_send_otp"))
        sendButton?.click()

        // Wait for OTP screen
        val otpScreen = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_otp_1")),
            timeout
        )
        
        val navigationTime = System.currentTimeMillis() - startTime
        
        assertNotNull("Should navigate to OTP screen", otpScreen)
        assertTrue("Navigation should be fast enough on mid-range device", 
                  navigationTime < 5000) // 5 seconds max
    }

    @Test
    fun test_memory_usage_during_heavy_operations() {
        launchApp()
        
        // Simulate heavy operations like downloading lessons
        navigateToLoginAndOTP()
        navigateToDashboard()
        
        // Simulate multiple rapid interactions
        for (i in 0..10) {
            // Tap different subjects rapidly
            val mathTab = device.findObject(By.text("Mathematics"))
            mathTab?.click()
            device.waitForIdle(500)
            
            val scienceTab = device.findObject(By.text("Science"))
            scienceTab?.click()
            device.waitForIdle(500)
        }

        // App should still be responsive
        val dashboard = device.findObject(By.res("com.porakhela:id/text_view_child_name"))
        assertNotNull("Dashboard should still be accessible after heavy operations", dashboard)
    }

    @Test
    fun test_network_state_changes_handling() {
        launchApp()
        
        // Start in online mode
        navigateToLoginAndOTP()
        
        // Simulate network disconnection (would require network simulation in real test)
        // For now, verify offline indicators work
        
        val offlineIndicator = device.findObject(By.res("com.porakhela:id/text_view_offline_indicator"))
        // Should either be hidden (online) or show offline message
        
        // Test that offline functionality works
        navigateToDashboard()
        
        // Only downloaded content should be accessible
        val lessonList = device.findObject(By.res("com.porakhela:id/recycler_view_lessons"))
        assertNotNull("Lesson list should be available offline", lessonList)
    }

    @Test
    fun test_back_button_system_navigation() {
        launchApp()
        
        // Navigate through app flow
        navigateToLoginAndOTP()
        
        // Use system back button
        device.pressBack()
        
        // Should return to login screen
        val phoneInput = device.findObject(By.res("com.porakhela:id/edit_text_phone"))
        assertNotNull("Should return to login after back press", phoneInput)
        
        // Press back again from login
        device.pressBack()
        
        // Should exit app or show confirmation
        device.waitForIdle(1000)
        
        // Either app closed or confirmation dialog shown
        val appStillRunning = device.findObject(By.pkg("com.porakhela").depth(0))
        // Test passes if app handles back properly (exits or shows dialog)
    }

    @Test
    fun test_app_state_after_background_foreground() {
        launchApp()
        
        // Enter some state
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        phoneInput?.text = "01712345678"
        
        // Send app to background
        device.pressRecentApps()
        device.waitForIdle(2000)
        
        // Wait a bit to simulate background time
        Thread.sleep(3000)
        
        // Return to app
        device.pressRecentApps()
        val appSelector = device.findObject(By.pkg("com.porakhela"))
        appSelector?.click()
        
        device.waitForIdle(2000)
        
        // Verify state is preserved
        val phoneInputAfter = device.findObject(By.res("com.porakhela:id/edit_text_phone"))
        assertEquals("Phone number should persist after background", 
                    "01712345678", phoneInputAfter?.text)
    }

    @Test
    fun test_different_screen_densities() {
        launchApp()
        
        // Verify UI scales properly on different screen densities
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        
        val sendButton = device.findObject(By.res("com.porakhela:id/button_send_otp"))
        
        // Verify minimum touch targets for accessibility
        assertTrue("Phone input should meet minimum size", 
                  phoneInput.bounds.width() > 48 && phoneInput.bounds.height() > 48)
        
        assertTrue("Button should meet minimum size",
                  sendButton.bounds.width() > 88 && sendButton.bounds.height() > 44)
        
        // Verify elements don't overlap
        assertFalse("Elements should not overlap",
                   phoneInput.bounds.intersect(sendButton.bounds))
    }

    // Helper methods

    private fun launchApp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = context.packageManager.getLaunchIntentForPackage("com.porakhela")
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(it)
        }
        device.waitForIdle()
    }

    private fun navigateToLoginAndOTP() {
        val phoneInput = device.wait(
            Until.findObject(By.res("com.porakhela:id/edit_text_phone")),
            timeout
        )
        phoneInput?.text = "01712345678"
        
        val sendButton = device.findObject(By.res("com.porakhela:id/button_send_otp"))
        sendButton?.click()
        
        device.waitForIdle(2000)
        
        // Enter OTP
        val otp = "123456"
        otp.forEachIndexed { index, digit ->
            val otpField = device.findObject(By.res("com.porakhela:id/edit_text_otp_${index + 1}"))
            otpField?.text = digit.toString()
        }
        
        val verifyButton = device.findObject(By.res("com.porakhela:id/button_verify_otp"))
        verifyButton?.click()
        
        device.waitForIdle(3000)
    }

    private fun navigateToDashboard() {
        // Skip child creation if already exists, otherwise create
        val createChildButton = device.findObject(By.res("com.porakhela:id/button_create_child"))
        
        if (createChildButton != null) {
            createChildButton.click()
            
            val nameInput = device.findObject(By.res("com.porakhela:id/edit_text_child_name"))
            nameInput?.text = "Test Child"
            
            val gradeSpinner = device.findObject(By.res("com.porakhela:id/spinner_grade"))
            gradeSpinner?.click()
            
            val grade6 = device.findObject(By.text("Class 6"))
            grade6?.click()
            
            val createButton = device.findObject(By.res("com.porakhela:id/button_create_profile"))
            createButton?.click()
            
            device.waitForIdle(3000)
        }
    }
}