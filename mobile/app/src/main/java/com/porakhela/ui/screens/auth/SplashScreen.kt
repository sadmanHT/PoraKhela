package com.porakhela.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import com.porakhela.ui.theme.*
import com.porakhela.ui.components.GradientBackground
import kotlin.math.cos
import kotlin.math.sin

/**
 * Child-friendly splash screen with animated branding
 */
@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Multiple animation states for fun effect
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")
    
    // Logo bounce animation
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )
    
    // Rainbow rotation for colorful effect
    val rainbowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow_rotation"
    )
    
    // Floating elements animation
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_offset"
    )
    
    // Progress for text reveal
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "text_alpha"
    )
    
    // Auto-navigate after splash duration
    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        delay(3500) // Longer duration for child-friendly experience
        // TODO: Check if user has seen onboarding before
        // For now, always go to onboarding for first-time experience
        onNavigateToOnboarding()
    }
    
    // Beautiful gradient background
    GradientBackground(
        startColor = FunPurple,
        endColor = FunBlue
    ) {
        // Floating decorative elements
        FloatingElements(
            rotation = rainbowRotation,
            offset = floatingOffset
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Logo Container
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale),
                contentAlignment = Alignment.Center
            ) {
                // Rainbow gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rainbowRotation)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    FunPink,
                                    FunOrange,
                                    FunGreen,
                                    FunBlue,
                                    FunPurple,
                                    FunPink
                                )
                            )
                        )
                )
                
                // Logo container
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Porakhela Logo",
                        tint = FunPurple,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name with animated appearance
            Text(
                text = "Porakhela",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = Color.White.copy(alpha = textAlpha),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 48.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bangla name
            Text(
                text = "পড়াখেলা",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White.copy(alpha = textAlpha * 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Fun tagline
            Text(
                text = "🎓 Learn • 🎮 Play • ⭐ Earn",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White.copy(alpha = textAlpha * 0.8f),
                    fontSize = 18.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Fun loading animation
            FunLoadingIndicator(
                rotation = rainbowRotation,
                scale = logoScale
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Getting ready for an amazing learning adventure! 🚀",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = textAlpha * 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

/**
 * Floating decorative elements for child-friendly atmosphere
 */
@Composable
private fun FloatingElements(
    rotation: Float,
    offset: Float
) {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.minDimension * 0.4f
        
        // Draw floating emojis/shapes
        val elements = listOf("⭐", "🎈", "🎯", "🎨", "📚", "🎮")
        elements.forEachIndexed { index, emoji ->
            val angle = (rotation + index * 60f) * Math.PI / 180f
            val x = centerX + (radius + offset * index) * cos(angle).toFloat()
            val y = centerY + (radius + offset * index) * sin(angle).toFloat()
            
            // Note: In a real implementation, we'd draw actual icons or shapes
            // For now, this demonstrates the animation structure
        }
    }
}

/**
 * Fun loading indicator with colorful animation
 */
@Composable
private fun FunLoadingIndicator(
    rotation: Float,
    scale: Float
) {
    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        CircularProgressIndicator(
            progress = 0.75f,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
                .scale(scale),
            color = PointsGold,
            strokeWidth = 4.dp,
            trackColor = Color.White.copy(alpha = 0.3f)
        )
        
        // Inner ring
        CircularProgressIndicator(
            progress = 0.5f,
            modifier = Modifier
                .size(40.dp)
                .rotate(-rotation * 1.5f),
            color = FunPink,
            strokeWidth = 3.dp,
            trackColor = Color.Transparent
        )
        
        // Center dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}