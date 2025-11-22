package com.porakhela.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Child-friendly color schemes for Porakhela
 * Bright and engaging for day use, gentle for evening
 */

private val LightColorScheme = lightColorScheme(
    primary = PorakhelaPrimary,
    onPrimary = PorakhelaOnPrimary,
    primaryContainer = PorakhelaPrimaryVariant,
    onPrimaryContainer = PorakhelaOnPrimary,
    secondary = PorakhelaSecondary,
    onSecondary = PorakhelaOnSecondary,
    secondaryContainer = PorakhelaSurfaceVariant,
    onSecondaryContainer = PorakhelaOnSurface,
    tertiary = FunPink,
    onTertiary = PorakhelaOnPrimary,
    background = PorakhelaBackground,
    onBackground = PorakhelaOnBackground,
    surface = PorakhelaSurface,
    onSurface = PorakhelaOnSurface,
    surfaceVariant = PorakhelaSurfaceVariant,
    onSurfaceVariant = PorakhelaOnSurface,
    error = ErrorRed,
    onError = PorakhelaOnPrimary,
)

private val DarkColorScheme = darkColorScheme(
    primary = PorakhelaPrimaryVariant,
    onPrimary = PorakhelaOnPrimary,
    primaryContainer = PorakhelaPrimary,
    onPrimaryContainer = PorakhelaOnPrimary,
    secondary = PorakhelaSecondary,
    onSecondary = PorakhelaOnSecondary,
    background = PorakhelaOnSurface,
    onBackground = PorakhelaSurface,
    surface = PorakhelaOnBackground,
    onSurface = PorakhelaSurface,
    error = ErrorRed,
    onError = PorakhelaOnPrimary,
)

/**
 * Main theme composable for Porakhela
 * Provides child-friendly Material Design 3 theming
 */
@Composable
fun PorakhelaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PorakhelaTypography,
        content = content
    )
}