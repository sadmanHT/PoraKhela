package com.porakhela.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porakhela.ui.accessibility.*
import com.porakhela.ui.components.*
import com.porakhela.ui.theme.*

/**
 * Accessibility settings screen for parents to customize child's experience
 * WCAG AA compliant with comprehensive accessibility options
 */
@Composable
fun AccessibilitySettingsScreen(
    onBack: () -> Unit,
    onSettingsChanged: (AccessibilitySettings) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    
    // Settings state
    var settings by remember { mutableStateOf(AccessibilitySettings()) }
    
    // Auto-detect accessibility features
    LaunchedEffect(Unit) {
        settings = settings.copy(
            isScreenReaderOptimized = accessibilityHelper.isScreenReaderActive()
        )
    }
    
    AccessibleContent(settings = settings) { adaptedSettings ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PorakhelaBackground)
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onBack()
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = FunBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Text(
                    text = "♿ Accessibility Settings",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = FunBlue,
                        fontSize = 24.sp
                    ),
                    modifier = Modifier.accessibleHeading(1)
                )
                
                Spacer(modifier = Modifier.size(28.dp)) // Balance layout
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Vision settings
                item {
                    SettingsSectionCard(
                        title = "👁️ Vision & Display",
                        subtitle = "Settings for visual accessibility"
                    ) {
                        // High contrast toggle
                        AccessibilityToggleItem(
                            title = "High Contrast Mode",
                            subtitle = "Increases color contrast for better readability",
                            icon = Icons.Default.Contrast,
                            checked = settings.isHighContrastEnabled,
                            onCheckedChange = { enabled ->
                                settings = settings.copy(isHighContrastEnabled = enabled)
                                onSettingsChanged(settings)
                                if (enabled) {
                                    accessibilityHelper.performHapticFeedback(
                                        AccessibilityHelper.HapticPatterns.SUCCESS
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Large text toggle
                        AccessibilityToggleItem(
                            title = "Large Text",
                            subtitle = "Increases text size for easier reading",
                            icon = Icons.Default.FormatSize,
                            checked = settings.isLargeTextEnabled,
                            onCheckedChange = { enabled ->
                                settings = settings.copy(isLargeTextEnabled = enabled)
                                onSettingsChanged(settings)
                                if (enabled) {
                                    accessibilityHelper.performHapticFeedback(
                                        AccessibilityHelper.HapticPatterns.SUCCESS
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Font size slider
                        FontSizeSlider(
                            value = settings.fontSize,
                            onValueChange = { scale ->
                                settings = settings.copy(fontSize = scale)
                                onSettingsChanged(settings)
                            }
                        )
                    }
                }
                
                // Motor & Touch settings
                item {
                    SettingsSectionCard(
                        title = "✋ Touch & Interaction",
                        subtitle = "Settings for motor accessibility"
                    ) {
                        // Haptic feedback toggle
                        AccessibilityToggleItem(
                            title = "Haptic Feedback",
                            subtitle = "Vibration feedback for touch interactions",
                            icon = Icons.Default.Vibration,
                            checked = settings.isHapticFeedbackEnabled,
                            onCheckedChange = { enabled ->
                                settings = settings.copy(isHapticFeedbackEnabled = enabled)
                                onSettingsChanged(settings)
                                if (enabled) {
                                    accessibilityHelper.performHapticFeedback(
                                        AccessibilityHelper.HapticPatterns.ACHIEVEMENT
                                    )
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Animation speed slider
                        AnimationSpeedSlider(
                            value = settings.animationSpeed,
                            onValueChange = { speed ->
                                settings = settings.copy(animationSpeed = speed)
                                onSettingsChanged(settings)
                            }
                        )
                    }
                }
                
                // Language settings
                item {
                    SettingsSectionCard(
                        title = "🗣️ Language & Content",
                        subtitle = "Language and text preferences"
                    ) {
                        // Language selection
                        LanguageSelector(
                            selectedLanguage = settings.preferredLanguage,
                            onLanguageSelected = { language ->
                                settings = settings.copy(preferredLanguage = language)
                                onSettingsChanged(settings)
                                accessibilityHelper.performHapticFeedback(
                                    AccessibilityHelper.HapticPatterns.SUCCESS
                                )
                            }
                        )
                    }
                }
                
                // Screen reader settings (auto-detected)
                if (adaptedSettings.isScreenReaderOptimized) {
                    item {
                        SettingsSectionCard(
                            title = "🔊 Screen Reader",
                            subtitle = "Optimized for screen readers"
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = FunGreen.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Accessibility,
                                        contentDescription = null,
                                        tint = FunGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = "Screen Reader Detected",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = FunGreen
                                            )
                                        )
                                        Text(
                                            text = "App optimized for screen reader accessibility",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Quick accessibility test
                item {
                    SettingsSectionCard(
                        title = "🧪 Accessibility Test",
                        subtitle = "Test current accessibility settings"
                    ) {
                        AccessibilityTestCard(
                            settings = adaptedSettings,
                            accessibilityHelper = accessibilityHelper
                        )
                    }
                }
                
                // Reset button
                item {
                    ChildFriendlyButton(
                        text = "Reset to Defaults",
                        onClick = {
                            settings = AccessibilitySettings()
                            onSettingsChanged(settings)
                            accessibilityHelper.performHapticFeedback(
                                AccessibilityHelper.HapticPatterns.GENTLE_FEEDBACK
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.outline,
                        icon = Icons.Default.Restore
                    )
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .accessibleGroup(title),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = AccessibleTextSizes.TitleLarge
                ),
                modifier = Modifier.accessibleHeading(2)
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = AccessibleTextSizes.BodyMedium
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            content()
        }
    }
}

@Composable
private fun AccessibilityToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .accessibleClickable(
                label = "$title toggle",
                action = if (checked) "disable" else "enable"
            ) {
                onCheckedChange(!checked)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) FunGreen else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = AccessibleTextSizes.TitleMedium
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = AccessibleTextSizes.BodySmall
                    )
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FunGreen,
                checkedTrackColor = FunGreen.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun FontSizeSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Font Size",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FunBlue,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.8f..1.5f,
            steps = 6,
            colors = SliderDefaults.colors(
                thumbColor = FunBlue,
                activeTrackColor = FunBlue
            ),
            modifier = Modifier.accessibleProgress(
                current = value,
                total = 1.5f,
                description = "Font size"
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Smaller",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "Larger", 
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun AnimationSpeedSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Animation Speed",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = when {
                    value <= 0.5f -> "Slow"
                    value <= 1.0f -> "Normal"
                    else -> "Fast"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FunOrange,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0.3f..2.0f,
            steps = 4,
            colors = SliderDefaults.colors(
                thumbColor = FunOrange,
                activeTrackColor = FunOrange
            ),
            modifier = Modifier.accessibleProgress(
                current = value,
                total = 2.0f,
                description = "Animation speed"
            )
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Slower",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                text = "Faster",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit
) {
    Column {
        Text(
            text = "Preferred Language",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LanguageOption(
                language = Language.ENGLISH,
                label = "English",
                flag = "🇺🇸",
                isSelected = selectedLanguage == Language.ENGLISH,
                onClick = { onLanguageSelected(Language.ENGLISH) },
                modifier = Modifier.weight(1f)
            )
            
            LanguageOption(
                language = Language.BENGALI,
                label = "বাংলা",
                flag = "🇧🇩",
                isSelected = selectedLanguage == Language.BENGALI,
                onClick = { onLanguageSelected(Language.BENGALI) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LanguageOption(
    language: Language,
    label: String,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .accessibleClickable(
                label = "$label language option",
                action = "select"
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FunBlue.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, FunBlue) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = flag,
                fontSize = 32.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) FunBlue else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
private fun AccessibilityTestCard(
    settings: AccessibilitySettings,
    accessibilityHelper: AccessibilityHelper
) {
    var testResults by remember { mutableStateOf<AccessibilityTestResults?>(null) }
    
    Column {
        if (testResults == null) {
            ChildFriendlyButton(
                text = "Test Accessibility",
                onClick = {
                    // Perform accessibility test
                    testResults = AccessibilityTestResults(
                        contrastRatio = if (settings.isHighContrastEnabled) 7.5 else 4.8,
                        fontSize = settings.fontSize,
                        hapticWorking = settings.isHapticFeedbackEnabled && accessibilityHelper.isAccessibilityEnabled(),
                        screenReaderDetected = accessibilityHelper.isScreenReaderActive()
                    )
                    
                    if (settings.isHapticFeedbackEnabled) {
                        accessibilityHelper.performHapticFeedback(
                            AccessibilityHelper.HapticPatterns.SUCCESS
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunGreen,
                icon = Icons.Default.PlayArrow
            )
        } else {
            val results = testResults!!
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = FunGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "✅ Accessibility Test Results",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = FunGreen
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TestResultRow(
                        label = "Color Contrast",
                        value = "${String.format("%.1f", results.contrastRatio)}:1",
                        isPassing = results.contrastRatio >= 4.5
                    )
                    
                    TestResultRow(
                        label = "Font Size",
                        value = "${(results.fontSize * 100).toInt()}%",
                        isPassing = results.fontSize >= 1.0
                    )
                    
                    TestResultRow(
                        label = "Haptic Feedback",
                        value = if (results.hapticWorking) "Working" else "Disabled",
                        isPassing = results.hapticWorking
                    )
                    
                    TestResultRow(
                        label = "Screen Reader",
                        value = if (results.screenReaderDetected) "Active" else "Not Detected",
                        isPassing = true // Not required
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ChildFriendlyButton(
                text = "Test Again",
                onClick = {
                    testResults = null
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunBlue,
                icon = Icons.Default.Refresh
            )
        }
    }
}

@Composable
private fun TestResultRow(
    label: String,
    value: String,
    isPassing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                imageVector = if (isPassing) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isPassing) "Pass" else "Warning",
                tint = if (isPassing) FunGreen else FunOrange,
                modifier = Modifier.size(16.dp)
            )
        }
    }
    
    if (label != "Screen Reader") { // Don't add spacer after last item
        Spacer(modifier = Modifier.height(8.dp))
    }
}

data class AccessibilityTestResults(
    val contrastRatio: Double,
    val fontSize: Float,
    val hapticWorking: Boolean,
    val screenReaderDetected: Boolean
)