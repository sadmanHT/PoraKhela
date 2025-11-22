package com.porakhela.ui.screens.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.*
import com.porakhela.data.model.OfflineQuestion
import com.porakhela.data.model.QuestionType
import com.porakhela.ui.components.CountdownTimer
import com.porakhela.ui.components.ProgressBar
import com.porakhela.ui.components.AnimatedPointsPopup
import com.porakhela.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Enhanced Offline Lesson Player Screen
 * Fully functional offline lesson player with questions, timer, and local progress tracking
 * Includes gamification, animations, and offline-first design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedOfflineLessonPlayerScreen(
    lessonId: String,
    childProfileId: String,
    onNavigateBack: () -> Unit,
    onLessonComplete: (Int, Int, Long) -> Unit, // score, maxScore, timeSpent
    modifier: Modifier = Modifier,
    viewModel: OfflineLessonPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(lessonId, childProfileId) {
        viewModel.loadOfflineLesson(lessonId, childProfileId)
    }

    // Handle lesson completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            delay(3000) // Show completion animation
            onLessonComplete(uiState.score, uiState.maxScore, uiState.timeSpent)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                OfflineLoadingScreen()
            }
            uiState.error != null -> {
                OfflineErrorScreen(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadOfflineLesson(lessonId, childProfileId) },
                    onBack = onNavigateBack
                )
            }
            uiState.lesson != null -> {
                OfflineLessonPlayerContent(
                    uiState = uiState,
                    onAnswerSelected = { questionId, answer, timeSpent ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.submitAnswer(questionId, answer, timeSpent)
                    },
                    onNextQuestion = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.nextQuestion()
                    },
                    onPreviousQuestion = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.previousQuestion()
                    },
                    onCompletLesson = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.completeLesson()
                    },
                    onNavigateBack = onNavigateBack
                )
            }
        }

        // Floating animations for points, achievements, streak notifications
        uiState.pendingEvents.forEach { event ->
            key(event.id) {
                AnimatedVisibility(
                    visible = !event.isShown,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    when (event.type.name) {
                        "POINTS_EARNED" -> {
                            AnimatedPointsPopup(
                                points = event.points,
                                title = event.title,
                                onAnimationComplete = {
                                    viewModel.markEventShown(event.id)
                                }
                            )
                        }
                        "ACHIEVEMENT_UNLOCKED" -> {
                            AchievementUnlockedPopup(
                                title = event.title,
                                description = event.message,
                                points = event.points,
                                onAnimationComplete = {
                                    viewModel.markEventShown(event.id)
                                }
                            )
                        }
                        "STREAK_MILESTONE" -> {
                            StreakMilestonePopup(
                                title = event.title,
                                message = event.message,
                                onAnimationComplete = {
                                    viewModel.markEventShown(event.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineLessonPlayerContent(
    uiState: OfflineLessonPlayerUiState,
    onAnswerSelected: (String, Int, Long) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onCompletLesson: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Enhanced header with offline indicator
        OfflineLessonHeader(
            title = uiState.lesson?.title ?: "",
            isOffline = true,
            currentQuestion = uiState.currentQuestionIndex + 1,
            totalQuestions = uiState.questions.size,
            score = uiState.score,
            timeSpent = uiState.timeSpent,
            streak = uiState.currentStreak,
            onNavigateBack = onNavigateBack
        )

        // Main content with enhanced question display
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Current question with enhanced features
                uiState.currentQuestion?.let { question ->
                    EnhancedQuestionCard(
                        question = question,
                        questionNumber = uiState.currentQuestionIndex + 1,
                        selectedAnswer = uiState.selectedAnswer,
                        isAnswered = uiState.isCurrentQuestionAnswered,
                        isCorrect = uiState.lastAnswerCorrect,
                        showResult = uiState.showAnswerResult,
                        timeLimit = question.timeLimit,
                        pointsForQuestion = question.points,
                        difficulty = question.difficulty,
                        onAnswerSelected = { answer, timeSpent ->
                            onAnswerSelected(question.id, answer, timeSpent)
                        }
                    )
                }
            }

            item {
                // Enhanced explanation with animations
                AnimatedVisibility(
                    visible = uiState.showAnswerResult && uiState.currentQuestion?.explanation != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    EnhancedExplanationCard(
                        explanation = uiState.currentQuestion?.explanation ?: "",
                        isCorrect = uiState.lastAnswerCorrect,
                        pointsEarned = uiState.lastPointsEarned,
                        showPointsAnimation = true
                    )
                }
            }

            // Show progress indicators and stats
            item {
                OfflineLessonStats(
                    correctAnswers = uiState.correctAnswers,
                    totalAnswered = uiState.answeredQuestions,
                    averageTime = uiState.averageTimePerQuestion,
                    currentStreak = uiState.currentStreak,
                    accuracy = if (uiState.answeredQuestions > 0) 
                        (uiState.correctAnswers * 100) / uiState.answeredQuestions else 0
                )
            }
        }

        // Enhanced navigation with offline sync indicator
        EnhancedNavigationButtons(
            canGoPrevious = uiState.currentQuestionIndex > 0,
            canGoNext = uiState.currentQuestionIndex < uiState.questions.size - 1,
            isLastQuestion = uiState.currentQuestionIndex == uiState.questions.size - 1,
            isCurrentQuestionAnswered = uiState.isCurrentQuestionAnswered,
            pendingSyncCount = uiState.pendingSyncItems,
            onPreviousQuestion = onPreviousQuestion,
            onNextQuestion = onNextQuestion,
            onCompleteLesson = onCompletLesson
        )
    }

    // Enhanced completion celebration with achievements
    AnimatedVisibility(
        visible = uiState.isCompleted,
        enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        EnhancedLessonCompletionOverlay(
            score = uiState.score,
            maxScore = uiState.maxScore,
            timeSpent = uiState.timeSpent,
            accuracy = if (uiState.maxScore > 0) (uiState.score * 100) / uiState.maxScore else 0,
            newAchievements = uiState.newAchievements,
            pointsEarned = uiState.totalPointsEarned,
            newStreak = uiState.currentStreak,
            isNewBest = uiState.isNewPersonalBest
        )
    }
}

@Composable
private fun OfflineLessonHeader(
    title: String,
    isOffline: Boolean,
    currentQuestion: Int,
    totalQuestions: Int,
    score: Int,
    timeSpent: Long,
    streak: Int,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    // Offline indicator
                    if (isOffline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = "Offline",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Streak indicator
                    if (streak > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🔥",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "$streak",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }

                    // Score display
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "$score pts",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced progress indicator with animations
            val progressAnimated by animateFloatAsState(
                targetValue = (currentQuestion - 1).toFloat() / totalQuestions.toFloat(),
                animationSpec = tween(durationMillis = 500),
                label = "progress"
            )

            ProgressBar(
                progress = progressAnimated,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                height = 8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Question $currentQuestion of $totalQuestions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = formatTime(timeSpent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EnhancedQuestionCard(
    question: OfflineQuestion,
    questionNumber: Int,
    selectedAnswer: Int?,
    isAnswered: Boolean,
    isCorrect: Boolean?,
    showResult: Boolean,
    timeLimit: Int?,
    pointsForQuestion: Int,
    difficulty: String,
    onAnswerSelected: (Int, Long) -> Unit
) {
    var questionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(question.id) {
        questionStartTime = System.currentTimeMillis()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Question header with metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Q$questionNumber",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Difficulty indicator
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (difficulty.lowercase()) {
                                "easy" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                "medium" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                "hard" -> Color(0xFFF44336).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = difficulty.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Points indicator
                Text(
                    text = "$pointsForQuestion pts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Timer with enhanced visual design
            timeLimit?.let { limit ->
                AnimatedVisibility(visible = !isAnswered) {
                    CountdownTimer(
                        totalTimeSeconds = limit,
                        onTimeUp = {
                            val timeSpent = System.currentTimeMillis() - questionStartTime
                            onAnswerSelected(-1, timeSpent) // -1 for timeout
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        showWarningAt = 10 // Show warning when 10 seconds left
                    )
                }
            }

            // Question text with enhanced typography
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )

            // Local image display (if available)
            question.imagePath?.let { imagePath ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = imagePath.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Enhanced answer options with animations
            question.options.forEachIndexed { index, option ->
                EnhancedAnswerOption(
                    text = option,
                    index = index,
                    isSelected = selectedAnswer == index,
                    isCorrect = showResult && index == question.correctAnswer,
                    isWrong = showResult && selectedAnswer == index && index != question.correctAnswer,
                    isEnabled = !isAnswered,
                    animationDelay = index * 100L,
                    onClick = {
                        val timeSpent = System.currentTimeMillis() - questionStartTime
                        onAnswerSelected(index, timeSpent)
                    }
                )
            }
        }
    }
}

@Composable
private fun EnhancedAnswerOption(
    text: String,
    index: Int,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    isEnabled: Boolean,
    animationDelay: Long = 0L,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "option_scale"
    )

    val containerColor = when {
        isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        isWrong -> Color(0xFFF44336).copy(alpha = 0.15f)
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isCorrect -> Color(0xFF4CAF50)
        isWrong -> Color(0xFFF44336)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn()
    ) {
        Card(
            onClick = if (isEnabled) onClick else { {} },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = BorderStroke(2.dp, borderColor),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Enhanced option indicator with animation
                val optionBackgroundColor by animateColorAsState(
                    targetValue = borderColor.copy(alpha = 0.2f),
                    label = "option_bg"
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = optionBackgroundColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ('A' + index).toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = borderColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Enhanced result icon with animation
                when {
                    isCorrect -> {
                        val rotationState by rememberInfiniteTransition().animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ), label = "correct_rotation"
                        )
                        
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Correct",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(rotationZ = rotationState)
                        )
                    }
                    isWrong -> {
                        val shakeState by rememberInfiniteTransition().animateFloat(
                            initialValue = -5f,
                            targetValue = 5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(100, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "wrong_shake"
                        )
                        
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = "Wrong",
                            tint = Color(0xFFF44336),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(translationX = shakeState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedExplanationCard(
    explanation: String,
    isCorrect: Boolean,
    pointsEarned: Int,
    showPointsAnimation: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                if (isCorrect) Icons.Default.Lightbulb else Icons.Default.Info,
                contentDescription = null,
                tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCorrect) "Great job! 🎉" else "Not quite right 🤔",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )

                    // Points indicator with animation
                    if (isCorrect && pointsEarned > 0) {
                        AnimatedVisibility(
                            visible = showPointsAnimation,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "+$pointsEarned pts",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun OfflineLessonStats(
    correctAnswers: Int,
    totalAnswered: Int,
    averageTime: Long,
    currentStreak: Int,
    accuracy: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                label = "Accuracy",
                value = "$accuracy%",
                icon = Icons.Default.TrendingUp,
                color = if (accuracy >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800)
            )
            
            StatItem(
                label = "Correct",
                value = "$correctAnswers/$totalAnswered",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
            
            StatItem(
                label = "Avg Time",
                value = formatTime(averageTime),
                icon = Icons.Default.Timer,
                color = MaterialTheme.colorScheme.tertiary
            )
            
            if (currentStreak > 0) {
                StatItem(
                    label = "Streak",
                    value = "$currentStreak 🔥",
                    icon = Icons.Default.LocalFireDepartment,
                    color = Color(0xFFFF9800)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EnhancedNavigationButtons(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isLastQuestion: Boolean,
    isCurrentQuestionAnswered: Boolean,
    pendingSyncCount: Int,
    onPreviousQuestion: () -> Unit,
    onNextQuestion: () -> Unit,
    onCompleteLesson: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Sync status indicator
            if (pendingSyncCount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$pendingSyncCount items pending sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous button with enhanced styling
                OutlinedButton(
                    onClick = onPreviousQuestion,
                    enabled = canGoPrevious,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(
                        2.dp,
                        if (canGoPrevious) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Previous")
                }

                // Next/Complete button with enhanced styling
                if (isLastQuestion) {
                    Button(
                        onClick = onCompleteLesson,
                        enabled = isCurrentQuestionAnswered,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Complete Lesson")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = onNextQuestion,
                        enabled = canGoNext && isCurrentQuestionAnswered,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedLessonCompletionOverlay(
    score: Int,
    maxScore: Int,
    timeSpent: Long,
    accuracy: Int,
    newAchievements: List<Any>,
    pointsEarned: Int,
    newStreak: Int,
    isNewBest: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebration animation
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.celebration))
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    modifier = Modifier.size(120.dp)
                )

                Text(
                    text = if (accuracy >= 80) "Excellent Work! 🎉" 
                           else if (accuracy >= 60) "Good Job! 👏" 
                           else "Keep Learning! 💪",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                // Enhanced stats with animations
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EnhancedStatCard(label = "Score", value = "$score/$maxScore", color = MaterialTheme.colorScheme.primary)
                            EnhancedStatCard(label = "Accuracy", value = "$accuracy%", color = if (accuracy >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800))
                            EnhancedStatCard(label = "Time", value = formatTime(timeSpent), color = MaterialTheme.colorScheme.tertiary)
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            EnhancedStatCard(label = "Points Earned", value = "+$pointsEarned", color = Color(0xFF4CAF50))
                            if (newStreak > 0) {
                                EnhancedStatCard(label = "Streak", value = "$newStreak 🔥", color = Color(0xFFFF9800))
                            }
                            if (isNewBest) {
                                EnhancedStatCard(label = "Personal Best", value = "🏆", color = Color(0xFFFFD700))
                            }
                        }
                    }
                }

                // Achievement notifications with enhanced styling
                if (newAchievements.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏆 New Achievements Unlocked!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${newAchievements.size} new badge${if (newAchievements.size > 1) "s" else ""} earned!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedStatCard(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementUnlockedPopup(
    title: String,
    description: String,
    points: Int,
    onAnimationComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(3000)
        onAnimationComplete()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🏆", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "+$points points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StreakMilestonePopup(
    title: String,
    message: String,
    onAnimationComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2500)
        onAnimationComplete()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9800).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔥", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun OfflineLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading offline lesson...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Offline Mode",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun OfflineErrorScreen(
    error: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Offline Content Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Check if this lesson pack is downloaded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("Go Back")
                }

                Button(onClick = onRetry) {
                    Text("Try Again")
                }
            }
        }
    }
}

// Helper function to format time
private fun formatTime(timeInMillis: Long): String {
    val minutes = (timeInMillis / 60000).toInt()
    val seconds = ((timeInMillis % 60000) / 1000).toInt()
    return String.format("%02d:%02d", minutes, seconds)
}