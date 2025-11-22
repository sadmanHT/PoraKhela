package com.porakhela.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Child-friendly color palette for Porakhela
 * Bright, engaging colors that appeal to kids
 */

// Primary Colors - Fun and vibrant
val PorakhelaPrimary = Color(0xFF6750A4) // Purple - learning
val PorakhelaPrimaryVariant = Color(0xFF8B5CF6) // Light purple
val PorakhelaSecondary = Color(0xFF03DAC6) // Teal - achievements

// Background Colors - Soft and easy on eyes
val PorakhelaBackground = Color(0xFFFEF7FF) // Very light purple
val PorakhelaSurface = Color(0xFFFFFFFF) // White
val PorakhelaSurfaceVariant = Color(0xFFF3E5F5) // Light purple tint

// Content Colors
val PorakhelaOnPrimary = Color(0xFFFFFFFF) // White text on primary
val PorakhelaOnSecondary = Color(0xFF000000) // Black text on secondary
val PorakhelaOnBackground = Color(0xFF1C1B1F) // Dark text on background
val PorakhelaOnSurface = Color(0xFF1C1B1F) // Dark text on surface

// Subject Colors - Each subject gets a unique color
val SubjectColors = mapOf(
    "bangla" to Color(0xFF4CAF50), // Green - Bengali
    "english" to Color(0xFF2196F3), // Blue - English
    "math" to Color(0xFFFF9800), // Orange - Mathematics
    "science" to Color(0xFF9C27B0), // Purple - Science
    "social" to Color(0xFF607D8B), // Blue Grey - Social Studies
    "islam" to Color(0xFF795548), // Brown - Islamic Studies
    "art" to Color(0xFFE91E63), // Pink - Arts & Crafts
)

// Gamification Colors
val PointsGold = Color(0xFFFFD700) // Gold for points
val AchievementBronze = Color(0xFFCD7F32) // Bronze achievement
val AchievementSilver = Color(0xFFC0C0C0) // Silver achievement
val AchievementGold = Color(0xFFFFD700) // Gold achievement
val StreakFire = Color(0xFFFF5722) // Orange-red for streaks

// Status Colors
val SuccessGreen = Color(0xFF4CAF50) // Success/completed
val WarningOrange = Color(0xFFFF9800) // Warning/in-progress
val ErrorRed = Color(0xFFF44336) // Error/failed
val InfoBlue = Color(0xFF2196F3) // Information

// Fun Accent Colors for UI elements
val FunPink = Color(0xFFE91E63)
val FunOrange = Color(0xFFFF9800)
val FunGreen = Color(0xFF4CAF50)
val FunBlue = Color(0xFF2196F3)
val FunPurple = Color(0xFF9C27B0)

// Gradient Colors for backgrounds
val GradientStart = Color(0xFF667eea)
val GradientEnd = Color(0xFFf093fb)