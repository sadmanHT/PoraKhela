package com.porakhela.ui.screens.achievements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.*
import com.porakhela.ui.components.ChildFriendlyButton
import com.porakhela.ui.theme.PorakhelaColors
import com.porakhela.ui.viewmodels.AchievementViewModel
import com.porakhela.data.local.entity.Achievement

/**
 * Achievement Screen - Displays earned badges, progress, and motivational content
 * Gamified interface to encourage continued learning
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Load achievements on screen entry
    LaunchedEffect(Unit) {
        viewModel.loadAchievements()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PorakhelaColors.primaryLight.copy(alpha = 0.1f),
                        PorakhelaColors.backgroundLight,
                        PorakhelaColors.accentYellow.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with back button and title
            AchievementTopBar(
                onBack = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onBack()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    AchievementLoadingState()
                }
                uiState.error != null -> {
                    AchievementErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadAchievements() }
                    )
                }
                else -> {
                    AchievementContent(
                        achievements = uiState.achievements,
                        totalPoints = uiState.totalPoints,
                        currentLevel = uiState.currentLevel,
                        nextLevelProgress = uiState.nextLevelProgress,
                        completionRate = uiState.completionRate,
                        onAchievementClick = { achievement ->
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.selectAchievement(achievement)
                        }
                    )
                }
            }
        }

        // New achievement celebration overlay
        AnimatedVisibility(
            visible = uiState.showCelebration,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            AchievementCelebrationOverlay(
                achievement = uiState.selectedAchievement,
                onDismiss = { viewModel.hideCelebration() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementTopBar(
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
            contentColor = PorakhelaColors.textPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChildFriendlyButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PorakhelaColors.primaryLight,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🏆 Your Achievements",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "Keep learning to unlock more badges!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PorakhelaColors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AchievementContent(
    achievements: List<Achievement>,
    totalPoints: Int,
    currentLevel: Int,
    nextLevelProgress: Float,
    completionRate: Float,
    onAchievementClick: (Achievement) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress overview
        item {
            AchievementProgressCard(
                totalPoints = totalPoints,
                currentLevel = currentLevel,
                nextLevelProgress = nextLevelProgress,
                completionRate = completionRate
            )
        }

        // Achievement categories
        item {
            Text(
                text = "🎯 Learning Milestones",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PorakhelaColors.primaryDark,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Earned achievements
        val earnedAchievements = achievements.filter { it.isEarned }
        if (earnedAchievements.isNotEmpty()) {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height((earnedAchievements.size / 2 + 1) * 120.dp)
                ) {
                    items(earnedAchievements) { achievement ->
                        EarnedAchievementCard(
                            achievement = achievement,
                            onClick = { onAchievementClick(achievement) }
                        )
                    }
                }
            }
        }

        // Available achievements to unlock
        val availableAchievements = achievements.filter { !it.isEarned }
        if (availableAchievements.isNotEmpty()) {
            item {
                Text(
                    text = "🔒 Unlock Next",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = PorakhelaColors.textSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height((availableAchievements.size / 2 + 1) * 120.dp)
                ) {
                    items(availableAchievements) { achievement ->
                        LockedAchievementCard(
                            achievement = achievement,
                            onClick = { onAchievementClick(achievement) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementProgressCard(
    totalPoints: Int,
    currentLevel: Int,
    nextLevelProgress: Float,
    completionRate: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PorakhelaColors.primaryDark
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Level $currentLevel",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "$totalPoints Total Points",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Level",
                    tint = PorakhelaColors.accentYellow,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Level progress
            Column {
                Text(
                    text = "Progress to Level ${currentLevel + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = nextLevelProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PorakhelaColors.accentYellow,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Completion rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Overall Progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "${(completionRate * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = PorakhelaColors.accentGreen
                )
            }
        }
    }
}

@Composable
private fun EarnedAchievementCard(
    achievement: Achievement,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Badge icon with shimmer effect
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getAchievementIcon(achievement.type),
                    contentDescription = achievement.title,
                    tint = PorakhelaColors.accentYellow,
                    modifier = Modifier.size(32.dp)
                )
                
                // Shimmer effect for earned achievements
                val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                val shimmerAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "shimmerAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            PorakhelaColors.accentYellow.copy(alpha = shimmerAlpha * 0.2f),
                            CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = PorakhelaColors.primaryDark,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Text(
                text = "+${achievement.points} pts",
                style = MaterialTheme.typography.labelSmall,
                color = PorakhelaColors.accentOrange,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LockedAchievementCard(
    achievement: Achievement,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                ),
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Text(
                text = achievement.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AchievementCelebrationOverlay(
    achievement: Achievement?,
    onDismiss: () -> Unit
) {
    if (achievement == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Celebration animation
            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(com.porakhela.R.raw.celebration_animation)
            )
            LottieAnimation(
                composition = composition,
                modifier = Modifier.size(80.dp),
                iterations = 1
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Achievement icon
            Icon(
                imageVector = getAchievementIcon(achievement.type),
                contentDescription = achievement.title,
                tint = PorakhelaColors.accentYellow,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🎉 Achievement Unlocked!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PorakhelaColors.primaryDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PorakhelaColors.accentOrange,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodyLarge,
                color = PorakhelaColors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "+${achievement.points} Points Earned!",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PorakhelaColors.accentGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            ChildFriendlyButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PorakhelaColors.primaryDark,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Awesome! 🎊",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun AchievementLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = PorakhelaColors.primaryDark,
            strokeWidth = 4.dp
        )
    }
}

@Composable
private fun AchievementErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Failed to load achievements",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.Red
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = PorakhelaColors.textSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        ChildFriendlyButton(
            onClick = onRetry
        ) {
            Text("Try Again")
        }
    }
}

private fun getAchievementIcon(type: String): ImageVector {
    return when (type) {
        "first_lesson" -> Icons.Default.School
        "streak_7" -> Icons.Default.LocalFire
        "perfect_score" -> Icons.Default.Star
        "math_master" -> Icons.Default.Calculate
        "science_explorer" -> Icons.Default.Science
        "reading_champion" -> Icons.Default.MenuBook
        "quick_learner" -> Icons.Default.Speed
        "helper" -> Icons.Default.Help
        "collector" -> Icons.Default.Collections
        else -> Icons.Default.EmojiEvents
    }
}