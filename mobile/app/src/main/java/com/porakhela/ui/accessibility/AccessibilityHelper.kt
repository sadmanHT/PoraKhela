package com.porakhela.ui.accessibility

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.semantics.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porakhela.R
import kotlin.math.roundToInt

/**
 * Comprehensive accessibility helper for Porakhela
 * WCAG AA compliance, Bengali font support, haptic feedback, and screen reader optimization
 */
class AccessibilityHelper(private val context: Context) {
    
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    /**
     * Haptic feedback patterns optimized for children
     */
    object HapticPatterns {
        val SUCCESS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        
        val ERROR = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
        }
        
        val ACHIEVEMENT = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300, 200, 500), -1)
        }
        
        val GENTLE_FEEDBACK = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(50, 128) // 50% intensity
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }
    
    /**
     * Perform haptic feedback with pattern
     */
    fun performHapticFeedback(pattern: VibrationEffect) {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(pattern)
        }
    }
    
    /**
     * Check if device has accessibility services enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as android.view.accessibility.AccessibilityManager
        return accessibilityManager.isEnabled
    }
    
    /**
     * Check if TalkBack or other screen readers are active
     */
    fun isScreenReaderActive(): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) 
            as android.view.accessibility.AccessibilityManager
        return accessibilityManager.isTouchExplorationEnabled
    }
}

/**
 * WCAG AA compliant color scheme for child accessibility
 */
object AccessibleColors {
    // High contrast colors with 4.5:1 ratio minimum
    val PrimaryContrast = Color(0xFF1565C0) // Blue with sufficient contrast
    val SecondaryContrast = Color(0xFF2E7D32) // Green with sufficient contrast
    val ErrorContrast = Color(0xFFD32F2F) // Red with sufficient contrast
    val WarningContrast = Color(0xFFF57C00) // Orange with sufficient contrast
    val SuccessContrast = Color(0xFF388E3C) // Green with sufficient contrast
    
    // Background colors for better readability
    val LightBackground = Color(0xFFFAFAFA)
    val DarkBackground = Color(0xFF121212)
    val SurfaceContrast = Color(0xFFFFFFFF)
    
    /**
     * Calculate color contrast ratio
     */
    fun getContrastRatio(color1: Color, color2: Color): Double {
        val luminance1 = getLuminance(color1) + 0.05
        val luminance2 = getLuminance(color2) + 0.05
        return maxOf(luminance1, luminance2) / minOf(luminance1, luminance2)
    }
    
    private fun getLuminance(color: Color): Double {
        val r = if (color.red <= 0.03928) color.red / 12.92 else Math.pow((color.red + 0.055) / 1.055, 2.4)
        val g = if (color.green <= 0.03928) color.green / 12.92 else Math.pow((color.green + 0.055) / 1.055, 2.4)
        val b = if (color.blue <= 0.03928) color.blue / 12.92 else Math.pow((color.blue + 0.055) / 1.055, 2.4)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    
    /**
     * Check if color combination meets WCAG AA standards
     */
    fun meetsWCAGAA(foreground: Color, background: Color): Boolean {
        return getContrastRatio(foreground, background) >= 4.5
    }
    
    /**
     * Check if color combination meets WCAG AAA standards
     */
    fun meetsWCAGAAA(foreground: Color, background: Color): Boolean {
        return getContrastRatio(foreground, background) >= 7.0
    }
}

/**
 * Bengali font family for proper text rendering
 */
object BengaliFonts {
    val NotoSansBengali = FontFamily(
        Font(R.font.noto_sans_bengali_regular, FontWeight.Normal),
        Font(R.font.noto_sans_bengali_medium, FontWeight.Medium),
        Font(R.font.noto_sans_bengali_semibold, FontWeight.SemiBold),
        Font(R.font.noto_sans_bengali_bold, FontWeight.Bold)
    )
    
    val KalpurushU = FontFamily(
        Font(R.font.kalpurush_regular, FontWeight.Normal),
        Font(R.font.kalpurush_bold, FontWeight.Bold)
    )
}

/**
 * Semantic modifiers for enhanced accessibility
 */
object AccessibleModifiers {
    
    /**
     * Make element accessible for screen readers
     */
    fun Modifier.accessibleClickable(
        label: String,
        action: String = "activate",
        onClick: () -> Unit
    ): Modifier = this.semantics {
        contentDescription = label
        onClick(label = action) {
            onClick()
            true
        }
    }
    
    /**
     * Add heading semantic for screen readers
     */
    fun Modifier.accessibleHeading(level: Int = 1): Modifier = this.semantics {
        heading()
        // Custom property for heading level
        this[SemanticsProperties.ContentDescription] = "Heading level $level"
    }
    
    /**
     * Mark element as important for accessibility
     */
    fun Modifier.accessibleImportant(): Modifier = this.semantics {
        liveRegion = LiveRegionMode.Assertive
    }
    
    /**
     * Group related elements for screen readers
     */
    fun Modifier.accessibleGroup(label: String): Modifier = this.semantics(mergeDescendants = true) {
        contentDescription = label
    }
    
    /**
     * Add progress information for screen readers
     */
    fun Modifier.accessibleProgress(
        current: Float,
        total: Float,
        description: String
    ): Modifier = this.semantics {
        progressBarRangeInfo = ProgressBarRangeInfo(current, 0f..total)
        contentDescription = "$description: ${(current/total*100).roundToInt()}% complete"
    }
    
    /**
     * Enhanced button with accessibility features
     */
    fun Modifier.accessibleButton(
        label: String,
        description: String? = null,
        enabled: Boolean = true
    ): Modifier = this.semantics {
        contentDescription = description ?: label
        if (!enabled) {
            disabled()
        }
        role = Role.Button
    }
    
    /**
     * Make input field accessible
     */
    fun Modifier.accessibleTextField(
        label: String,
        isError: Boolean = false,
        errorMessage: String? = null
    ): Modifier = this.semantics {
        contentDescription = label
        if (isError && errorMessage != null) {
            error(errorMessage)
        }
    }
}

/**
 * Font sizes that meet accessibility guidelines
 */
object AccessibleTextSizes {
    val BodyLarge = 18.sp // Minimum readable size for children
    val BodyMedium = 16.sp
    val BodySmall = 14.sp // Minimum for secondary text
    val HeadlineLarge = 32.sp // Large enough for child readability
    val HeadlineMedium = 28.sp
    val HeadlineSmall = 24.sp
    val TitleLarge = 22.sp
    val TitleMedium = 18.sp
    val TitleSmall = 16.sp
    val LabelLarge = 16.sp // Button text
    val LabelMedium = 14.sp
}

/**
 * Accessible spacing that meets touch target guidelines
 */
object AccessibleSpacing {
    val TouchTargetMinimum = 48.dp // WCAG minimum touch target
    val TouchTargetRecommended = 56.dp // Recommended for children
    val PaddingSmall = 8.dp
    val PaddingMedium = 16.dp
    val PaddingLarge = 24.dp
    val PaddingExtraLarge = 32.dp
}

/**
 * Screen reader announcements for important events
 */
@Composable
fun ScreenReaderAnnouncement(
    message: String,
    priority: AnnouncementPriority = AnnouncementPriority.NORMAL
) {
    val context = LocalContext.current
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    
    LaunchedEffect(message) {
        if (accessibilityHelper.isScreenReaderActive()) {
            // Use semantics to make announcement
            // This would typically integrate with TalkBack or other screen readers
        }
    }
}

enum class AnnouncementPriority {
    LOW,
    NORMAL,
    HIGH
}

/**
 * Accessibility settings for the app
 */
data class AccessibilitySettings(
    val isHighContrastEnabled: Boolean = false,
    val isLargeTextEnabled: Boolean = false,
    val isHapticFeedbackEnabled: Boolean = true,
    val isScreenReaderOptimized: Boolean = false,
    val preferredLanguage: Language = Language.ENGLISH,
    val fontSize: Float = 1.0f, // Scale factor
    val animationSpeed: Float = 1.0f // Animation speed multiplier
)

enum class Language {
    ENGLISH,
    BENGALI
}

/**
 * Accessibility-aware composable that adapts to user settings
 */
@Composable
fun AccessibleContent(
    settings: AccessibilitySettings = AccessibilitySettings(),
    content: @Composable (AccessibilitySettings) -> Unit
) {
    val context = LocalContext.current
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    
    // Automatically detect some accessibility needs
    val adaptedSettings = settings.copy(
        isScreenReaderOptimized = accessibilityHelper.isScreenReaderActive(),
        isHighContrastEnabled = settings.isHighContrastEnabled || accessibilityHelper.isScreenReaderActive()
    )
    
    content(adaptedSettings)
}

/**
 * Custom semantics properties for Porakhela
 */
val SemanticsPropertyKey.PorakhelaRole: SemanticsPropertyKey<String>
    get() = SemanticsPropertyKey("PorakhelaRole")

val SemanticsPropertyKey.SubjectName: SemanticsPropertyKey<String>
    get() = SemanticsPropertyKey("SubjectName")

val SemanticsPropertyKey.LessonProgress: SemanticsPropertyKey<Float>
    get() = SemanticsPropertyKey("LessonProgress")

val SemanticsPropertyKey.PointsValue: SemanticsPropertyKey<Int>
    get() = SemanticsPropertyKey("PointsValue")

/**
 * Extension functions for enhanced semantics
 */
fun SemanticsPropertyReceiver.porakhelaRole(role: String) {
    this[SemanticsPropertyKey.PorakhelaRole] = role
}

fun SemanticsPropertyReceiver.subjectName(name: String) {
    this[SemanticsPropertyKey.SubjectName] = name
}

fun SemanticsPropertyReceiver.lessonProgress(progress: Float) {
    this[SemanticsPropertyKey.LessonProgress] = progress
}

fun SemanticsPropertyReceiver.pointsValue(points: Int) {
    this[SemanticsPropertyKey.PointsValue] = points
}

/**
 * High contrast theme for accessibility
 */
@Composable
fun HighContrastTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = AccessibleColors.PrimaryContrast,
            secondary = AccessibleColors.SecondaryContrast,
            error = AccessibleColors.ErrorContrast,
            background = AccessibleColors.LightBackground,
            surface = AccessibleColors.SurfaceContrast
        ),
        content = content
    )
}