package com.porakhela.ui.components.gamification

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.porakhela.R
import com.porakhela.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Animated Points Popup Component
 * Shows "+10 Porapoints! 🎉" with smooth animations
 */
@Composable
fun AnimatedPointsPopup(
    points: Int,
    title: String = "+$points Porapoints! 🎉",
    modifier: Modifier = Modifier,
    onAnimationComplete: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "points_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearOutSlowInEasing
        ),
        label = "points_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2500) // Show for 2.5 seconds
        visible = false
        delay(300) // Wait for fade out
        onAnimationComplete?.invoke()
    }

    Box(
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated coin icon
                val rotation by rememberInfiniteTransition().animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ), label = "coin_rotation"
                )

                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationY = rotation)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Streak Milestone Popup Component
 * Shows streak achievements with fire animations
 */
@Composable
fun StreakMilestonePopup(
    streakCount: Int,
    milestoneText: String = "🔥 $streakCount Day Streak!",
    modifier: Modifier = Modifier,
    onAnimationComplete: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    var showFireworks by remember { mutableStateOf(false) }

    val slideOffset by animateIntAsState(
        targetValue = if (visible) 0 else -200,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "streak_slide"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "streak_scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(500)
        showFireworks = true
        delay(3000) // Show for 3 seconds
        visible = false
        delay(500)
        onAnimationComplete?.invoke()
    }

    Box(
        modifier = modifier.offset(y = slideOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFF6F00).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.scale(scale)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fire animation
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.fire_animation))
                LottieAnimation(
                    composition = composition,
                    iterations = if (showFireworks) LottieConstants.IterateForever else 1,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = milestoneText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Keep it up! You're on fire! 🚀",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Fireworks animation overlay
        if (showFireworks) {
            val fireworksComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.fireworks))
            LottieAnimation(
                composition = fireworksComposition,
                iterations = 2,
                modifier = Modifier.size(200.dp)
            )
        }
    }
}

/**
 * Achievement Unlocked Popup Component
 * Shows new badge/achievement with celebration animation
 */
@Composable
fun AchievementUnlockedPopup(
    title: String,
    description: String,
    points: Int,
    iconResId: Int? = null,
    modifier: Modifier = Modifier,
    onAnimationComplete: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    var showCelebration by remember { mutableStateOf(false) }

    val bounceScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "achievement_bounce"
    )

    val glowAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "achievement_glow"
    )

    LaunchedEffect(Unit) {
        delay(300)
        visible = true
        delay(500)
        showCelebration = true
        delay(3500) // Show for 3.5 seconds
        visible = false
        delay(500)
        onAnimationComplete?.invoke()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Celebration background
        if (showCelebration) {
            val celebrationComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.achievement_celebration))
            LottieAnimation(
                composition = celebrationComposition,
                iterations = 1,
                modifier = Modifier.size(300.dp)
            )
        }

        // Achievement card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                2.dp, 
                Color(0xFFFFD700).copy(alpha = glowAlpha)
            ),
            modifier = Modifier
                .scale(bounceScale)
                .width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Achievement header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🏆",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        text = "Achievement Unlocked!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }

                Divider(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // Achievement icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.3f),
                                    Color(0xFFFF6F00).copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconResId != null) {
                        // Would load custom icon here
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFFFD700)
                        )
                    } else {
                        Text(
                            text = "🎖️",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // Achievement details
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Points earned
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "+$points Points",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

/**
 * Animated Progress Bar to Next Badge
 * Shows progress towards next achievement level
 */
@Composable
fun BadgeProgressBar(
    currentProgress: Int,
    maxProgress: Int,
    badgeName: String,
    modifier: Modifier = Modifier,
    animationDuration: Int = 1000
) {
    val progressPercentage = (currentProgress.toFloat() / maxProgress.toFloat()).coerceIn(0f, 1f)
    
    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) progressPercentage else 0f,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = FastOutSlowInEasing
        ),
        label = "badge_progress"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next Badge: $badgeName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "$currentProgress / $maxProgress",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF8BC34A),
                                    Color(0xFFCDDC39)
                                )
                            )
                        )
                )
            }

            // Progress percentage
            Text(
                text = "${(progressPercentage * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Level Up Celebration Animation
 * Shows when user reaches a new level
 */
@Composable
fun LevelUpCelebration(
    newLevel: Int,
    modifier: Modifier = Modifier,
    onAnimationComplete: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    var showLevelText by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "level_scale"
    )

    val textScale by animateFloatAsState(
        targetValue = if (showLevelText) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "text_scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(800)
        showLevelText = true
        delay(3000)
        visible = false
        delay(500)
        onAnimationComplete?.invoke()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background celebration effect
        val celebrationComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.level_up_celebration))
        LottieAnimation(
            composition = celebrationComposition,
            iterations = 1,
            modifier = Modifier
                .size(400.dp)
                .scale(scale)
        )

        // Level up text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.scale(textScale)
        ) {
            Text(
                text = "LEVEL UP!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD700),
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Level $newLevel",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "🌟 Amazing progress! Keep learning! 🌟",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Daily Challenge Completion Popup
 * Shows when user completes daily challenges
 */
@Composable
fun DailyChallengeCompletePopup(
    challengeName: String,
    rewardPoints: Int,
    modifier: Modifier = Modifier,
    onAnimationComplete: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }

    val slideUp by animateIntAsState(
        targetValue = if (visible) 0 else 100,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "challenge_slide"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "challenge_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(3000)
        visible = false
        delay(500)
        onAnimationComplete?.invoke()
    }

    Box(
        modifier = modifier
            .offset(y = slideUp.dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF673AB7).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Challenge icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✅",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                Column {
                    Text(
                        text = "Daily Challenge Complete!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = challengeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "+$rewardPoints bonus points! 🎁",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

/**
 * Points Counter with Animation
 * Shows running total of points with smooth counting animation
 */
@Composable
fun AnimatedPointsCounter(
    targetPoints: Int,
    modifier: Modifier = Modifier,
    animationDuration: Int = 2000,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    var animatedPoints by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetPoints) {
        val startTime = System.currentTimeMillis()
        val duration = animationDuration.toLong()

        while (System.currentTimeMillis() - startTime < duration) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            
            // Ease out animation
            val easedProgress = 1f - (1f - progress) * (1f - progress)
            animatedPoints = (targetPoints * easedProgress).toInt()
            
            delay(16) // ~60fps
        }
        animatedPoints = targetPoints
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.MonetizationOn,
            contentDescription = "Points",
            tint = Color(0xFFFFD700),
            modifier = Modifier.size(textStyle.fontSize.value.dp)
        )

        Text(
            text = animatedPoints.toString(),
            style = textStyle,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFD700)
        )

        Text(
            text = "Porapoints",
            style = textStyle.copy(fontSize = textStyle.fontSize * 0.7f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}