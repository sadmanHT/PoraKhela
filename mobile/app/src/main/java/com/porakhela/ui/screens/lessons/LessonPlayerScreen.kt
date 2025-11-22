package com.porakhela.ui.screens.lessons

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.*
import com.porakhela.R
import com.porakhela.ui.components.*
import com.porakhela.ui.theme.*
import com.porakhela.ui.viewmodels.LessonPlayerViewModel
import com.porakhela.data.local.entity.LessonContent

/**
 * Lesson Player Screen - Interactive lesson content with video, animations, and progress tracking
 * Child-friendly design with large touch targets and engaging animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonPlayerScreen(
    viewModel: LessonPlayerViewModel = hiltViewModel(),
    lessonId: String,
    onStartExercises: () -> Unit,
    onLessonComplete: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Initialize lesson
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    // Handle lesson completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onLessonComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PorakhelaColors.backgroundLight,
                        PorakhelaColors.backgroundDark.copy(alpha = 0.1f)
                    )
                )
            )
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with progress
            LessonPlayerTopBar(
                lessonTitle = uiState.lesson?.title ?: "",
                progress = uiState.progress,
                onBack = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onBack()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lesson content
            when {
                uiState.isLoading -> {
                    LessonLoadingState()
                }
                uiState.error != null -> {
                    LessonErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadLesson(lessonId) }
                    )
                }
                uiState.lesson != null -> {
                    LessonContentDisplay(
                        lesson = uiState.lesson!!,
                        currentSection = uiState.currentSection,
                        isPlaying = uiState.isPlaying,
                        onPlayPause = viewModel::togglePlayback,
                        onNextSection = viewModel::nextSection,
                        onPreviousSection = viewModel::previousSection,
                        onSectionComplete = viewModel::markSectionComplete,
                        onStartExercises = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onStartExercises()
                        }
                    )
                }
            }
        }

        // Achievement animation overlay
        AnimatedVisibility(
            visible = uiState.showAchievementAnimation,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            AchievementAnimationOverlay(
                achievementText = "Lesson Completed! 🎉",
                onAnimationComplete = { viewModel.hideAchievementAnimation() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonPlayerTopBar(
    lessonTitle: String,
    progress: Float,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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

                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = progress,
                        modifier = Modifier.size(36.dp),
                        color = PorakhelaColors.primaryDark,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PorakhelaColors.primaryDark,
                trackColor = PorakhelaColors.primaryLight.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun LessonContentDisplay(
    lesson: LessonContent,
    currentSection: Int,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNextSection: () -> Unit,
    onPreviousSection: () -> Unit,
    onSectionComplete: () -> Unit,
    onStartExercises: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Video/Animation section
        item {
            LessonVideoSection(
                videoUrl = lesson.videoUrl,
                animationUrl = lesson.animationUrl,
                isPlaying = isPlaying,
                onPlayPause = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onPlayPause()
                }
            )
        }

        // Content sections
        item {
            LessonContentSection(
                title = lesson.title,
                titleBn = lesson.titleBn,
                content = lesson.content,
                contentBn = lesson.contentBn,
                currentSection = currentSection
            )
        }

        // Interactive elements
        item {
            LessonInteractiveElements(
                interactiveElements = lesson.interactiveElements,
                onElementComplete = { elementId ->
                    onSectionComplete()
                }
            )
        }

        // Navigation controls
        item {
            LessonNavigationControls(
                canGoPrevious = currentSection > 0,
                canGoNext = currentSection < lesson.totalSections - 1,
                isLastSection = currentSection == lesson.totalSections - 1,
                onPrevious = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onPreviousSection()
                },
                onNext = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onNextSection()
                },
                onStartExercises = onStartExercises
            )
        }

        // Points and achievements display
        item {
            LessonPointsDisplay(
                pointsEarned = lesson.pointsEarned,
                totalPoints = lesson.totalPoints
            )
        }
    }
}

@Composable
private fun LessonVideoSection(
    videoUrl: String?,
    animationUrl: String?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PorakhelaColors.backgroundDark
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                animationUrl != null -> {
                    // Lottie animation
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.Url(animationUrl)
                    )
                    val animationState = rememberLottieAnimatable()
                    
                    LaunchedEffect(isPlaying) {
                        if (isPlaying) {
                            animationState.animate(composition)
                        } else {
                            animationState.pauseAnimation()
                        }
                    }

                    LottieAnimation(
                        composition = composition,
                        progress = { animationState.progress },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                videoUrl != null -> {
                    // Video player placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Video Player",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    // Placeholder illustration
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Lesson",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(96.dp)
                    )
                }
            }

            // Play/Pause button overlay
            ChildFriendlyButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PorakhelaColors.primaryDark,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LessonContentSection(
    title: String,
    titleBn: String?,
    content: String,
    contentBn: String?,
    currentSection: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Title in both languages
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = PorakhelaColors.textPrimary
            )

            if (titleBn != null) {
                Text(
                    text = titleBn,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp
                    ),
                    color = PorakhelaColors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content with animated reveal
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                }
            ) { section ->
                Column {
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        color = PorakhelaColors.textPrimary
                    )

                    if (contentBn != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = contentBn,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            ),
                            color = PorakhelaColors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonInteractiveElements(
    interactiveElements: List<InteractiveElement>,
    onElementComplete: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PorakhelaColors.primaryLight.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Try It Yourself! 🎯",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = PorakhelaColors.primaryDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive elements
            interactiveElements.forEach { element ->
                InteractiveElementCard(
                    element = element,
                    onComplete = { onElementComplete(element.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LessonNavigationControls(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isLastSection: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStartExercises: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        ChildFriendlyButton(
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier.size(width = 120.dp, height = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canGoPrevious) PorakhelaColors.primaryLight else Color.Gray.copy(alpha = 0.3f),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Previous", fontSize = 14.sp)
        }

        // Next/Exercise button
        ChildFriendlyButton(
            onClick = if (isLastSection) onStartExercises else onNext,
            enabled = canGoNext || isLastSection,
            modifier = Modifier.size(width = 120.dp, height = 48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLastSection) PorakhelaColors.accentGreen else PorakhelaColors.primaryDark,
                contentColor = Color.White
            )
        ) {
            Text(
                if (isLastSection) "Exercises" else "Next",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (isLastSection) Icons.Default.Quiz else Icons.Default.ArrowForward,
                contentDescription = if (isLastSection) "Start Exercises" else "Next",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LessonPointsDisplay(
    pointsEarned: Int,
    totalPoints: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PorakhelaColors.accentYellow.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Points Earned",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PorakhelaColors.textSecondary
                )
                Text(
                    text = "$pointsEarned / $totalPoints",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = PorakhelaColors.accentOrange
                )
            }

            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Points",
                tint = PorakhelaColors.accentYellow,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun LessonLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = PorakhelaColors.primaryDark,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading lesson...",
                style = MaterialTheme.typography.bodyLarge,
                color = PorakhelaColors.textSecondary
            )
        }
    }
}

@Composable
private fun LessonErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Oops! Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = PorakhelaColors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            ChildFriendlyButton(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PorakhelaColors.primaryDark,
                    contentColor = Color.White
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

// Data classes for lesson content
data class LessonContent(
    val id: String,
    val title: String,
    val titleBn: String?,
    val content: String,
    val contentBn: String?,
    val videoUrl: String?,
    val animationUrl: String?,
    val interactiveElements: List<InteractiveElement>,
    val totalSections: Int,
    val pointsEarned: Int,
    val totalPoints: Int
)

data class InteractiveElement(
    val id: String,
    val type: String, // "tap", "drag", "draw", etc.
    val instruction: String,
    val instructionBn: String?,
    val isCompleted: Boolean = false
)