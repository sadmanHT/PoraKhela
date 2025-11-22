package com.porakhela.ui.screens.lessons

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.*
import com.porakhela.R
import com.porakhela.ui.components.ChildFriendlyButton
import com.porakhela.ui.theme.PorakhelaColors
import com.porakhela.ui.viewmodels.ExerciseViewModel
import com.porakhela.data.local.entity.Exercise

/**
 * Exercise Screen - Interactive exercises and quizzes for lesson reinforcement
 * Child-friendly interface with immediate feedback and encouragement
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = hiltViewModel(),
    lessonId: String,
    onExercisesComplete: (score: Int) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Load exercises when screen appears
    LaunchedEffect(lessonId) {
        viewModel.loadExercises(lessonId)
    }

    // Handle completion
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onExercisesComplete(uiState.finalScore)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PorakhelaColors.backgroundLight,
                        PorakhelaColors.primaryLight.copy(alpha = 0.1f),
                        PorakhelaColors.accentBlue.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar with progress
            ExerciseTopBar(
                currentQuestion = uiState.currentExerciseIndex + 1,
                totalQuestions = uiState.exercises.size,
                score = uiState.score,
                onBack = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onBack()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main content
            when {
                uiState.isLoading -> {
                    ExerciseLoadingState()
                }
                uiState.error != null -> {
                    ExerciseErrorState(
                        error = uiState.error!!,
                        onRetry = { viewModel.loadExercises(lessonId) }
                    )
                }
                uiState.showResults -> {
                    ExerciseResultsScreen(
                        score = uiState.score,
                        totalQuestions = uiState.exercises.size,
                        correctAnswers = uiState.correctAnswers,
                        timeSpent = uiState.totalTimeSpent,
                        onContinue = { viewModel.completeExercises() }
                    )
                }
                uiState.exercises.isNotEmpty() -> {
                    val currentExercise = uiState.exercises.getOrNull(uiState.currentExerciseIndex)
                    if (currentExercise != null) {
                        ExerciseQuestionDisplay(
                            exercise = currentExercise,
                            selectedAnswer = uiState.selectedAnswer,
                            showFeedback = uiState.showFeedback,
                            isCorrect = uiState.lastAnswerCorrect,
                            onAnswerSelected = { answer ->
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.selectAnswer(answer)
                            },
                            onSubmitAnswer = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.submitAnswer()
                            },
                            onNextQuestion = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.nextQuestion()
                            }
                        )
                    }
                }
            }
        }

        // Celebration animation overlay
        AnimatedVisibility(
            visible = uiState.showCelebration,
            enter = scaleIn(animationSpec = spring(dampingRatio = 0.6f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CelebrationOverlay(
                message = if (uiState.lastAnswerCorrect) "Correct! 🎉" else "Good try! 💪",
                isCorrect = uiState.lastAnswerCorrect,
                pointsEarned = if (uiState.lastAnswerCorrect) 50 else 10
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseTopBar(
    currentQuestion: Int,
    totalQuestions: Int,
    score: Int,
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Question $currentQuestion of $totalQuestions",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "🏆 Score: $score points",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PorakhelaColors.accentOrange
                    )
                }

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = currentQuestion.toFloat() / totalQuestions.toFloat(),
                        modifier = Modifier.size(36.dp),
                        color = PorakhelaColors.accentGreen,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "$currentQuestion",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = currentQuestion.toFloat() / totalQuestions.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PorakhelaColors.accentGreen,
                trackColor = PorakhelaColors.accentGreen.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun ExerciseQuestionDisplay(
    exercise: Exercise,
    selectedAnswer: Int?,
    showFeedback: Boolean,
    isCorrect: Boolean,
    onAnswerSelected: (Int) -> Unit,
    onSubmitAnswer: () -> Unit,
    onNextQuestion: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Question card
        item {
            QuestionCard(
                question = exercise.question,
                questionBn = exercise.questionBn
            )
        }

        // Answer options
        items(exercise.options.size) { index ->
            AnswerOptionCard(
                option = exercise.options[index],
                optionBn = exercise.optionsBn?.getOrNull(index),
                index = index,
                isSelected = selectedAnswer == index,
                showFeedback = showFeedback,
                isCorrect = index == exercise.correctAnswer,
                wasSelected = selectedAnswer == index,
                onSelected = { onAnswerSelected(index) }
            )
        }

        // Submit/Next button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            if (showFeedback) {
                // Show feedback and explanation
                FeedbackCard(
                    isCorrect = isCorrect,
                    explanation = exercise.explanation,
                    explanationBn = exercise.explanationBn
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ChildFriendlyButton(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PorakhelaColors.accentGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Next Question →",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                ChildFriendlyButton(
                    onClick = onSubmitAnswer,
                    enabled = selectedAnswer != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedAnswer != null) 
                            PorakhelaColors.primaryDark else Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Submit Answer ✓",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    question: String,
    questionBn: String?
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
            Text(
                text = "🤔 Question",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = question,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 28.sp
                ),
                color = Color.White
            )
            
            if (questionBn != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = questionBn,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun AnswerOptionCard(
    option: String,
    optionBn: String?,
    index: Int,
    isSelected: Boolean,
    showFeedback: Boolean,
    isCorrect: Boolean,
    wasSelected: Boolean,
    onSelected: () -> Unit
) {
    val optionLabel = listOf("A", "B", "C", "D")[index]
    
    val backgroundColor = when {
        showFeedback && isCorrect -> PorakhelaColors.accentGreen
        showFeedback && wasSelected && !isCorrect -> Color.Red.copy(alpha = 0.8f)
        isSelected -> PorakhelaColors.primaryLight
        else -> Color.White
    }
    
    val textColor = when {
        showFeedback && (isCorrect || (wasSelected && !isCorrect)) -> Color.White
        isSelected -> Color.White
        else -> PorakhelaColors.textPrimary
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !showFeedback) { onSelected() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option label circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (showFeedback && isCorrect) {
                            Color.White
                        } else if (isSelected) {
                            Color.White.copy(alpha = 0.2f)
                        } else {
                            PorakhelaColors.primaryLight
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionLabel,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (showFeedback && isCorrect) {
                        PorakhelaColors.accentGreen
                    } else if (isSelected) {
                        backgroundColor
                    } else {
                        Color.White
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Option text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = textColor
                )
                
                if (optionBn != null) {
                    Text(
                        text = optionBn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Feedback icon
            if (showFeedback) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else if (wasSelected) Icons.Default.Close else Icons.Default.Circle,
                    contentDescription = if (isCorrect) "Correct" else "Incorrect",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    isCorrect: Boolean,
    explanation: String?,
    explanationBn: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) 
                PorakhelaColors.accentGreen.copy(alpha = 0.1f) else 
                Color.Red.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.Check else Icons.Default.Info,
                    contentDescription = if (isCorrect) "Correct" else "Explanation",
                    tint = if (isCorrect) PorakhelaColors.accentGreen else PorakhelaColors.accentOrange,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = if (isCorrect) "🎉 Excellent!" else "💡 Learn More",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isCorrect) PorakhelaColors.accentGreen else PorakhelaColors.accentOrange
                )
            }
            
            if (explanation != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PorakhelaColors.textPrimary
                )
                
                if (explanationBn != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = explanationBn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PorakhelaColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseResultsScreen(
    score: Int,
    totalQuestions: Int,
    correctAnswers: Int,
    timeSpent: Long,
    onContinue: () -> Unit
) {
    val percentage = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
    val isExcellent = percentage >= 80
    val isGood = percentage >= 60
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration animation
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(
                if (isExcellent) R.raw.celebration_animation else R.raw.good_job_animation
            )
        )
        LottieAnimation(
            composition = composition,
            modifier = Modifier.size(120.dp),
            iterations = 1
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Results card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        isExcellent -> "🌟 Excellent Work!"
                        isGood -> "👏 Good Job!"
                        else -> "💪 Keep Trying!"
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = PorakhelaColors.primaryDark,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Score display
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp
                    ),
                    color = PorakhelaColors.accentOrange
                )
                
                Text(
                    text = "Points Earned",
                    style = MaterialTheme.typography.titleMedium,
                    color = PorakhelaColors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        value = "$correctAnswers/$totalQuestions",
                        label = "Correct",
                        color = PorakhelaColors.accentGreen
                    )
                    
                    StatCard(
                        value = "$percentage%",
                        label = "Accuracy",
                        color = PorakhelaColors.accentBlue
                    )
                    
                    StatCard(
                        value = "${timeSpent / 1000}s",
                        label = "Time",
                        color = PorakhelaColors.accentPurple
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                ChildFriendlyButton(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PorakhelaColors.accentGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Continue Learning! 🚀",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PorakhelaColors.textSecondary
        )
    }
}

@Composable
private fun CelebrationOverlay(
    message: String,
    isCorrect: Boolean,
    pointsEarned: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) PorakhelaColors.accentGreen else PorakhelaColors.accentOrange
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            if (isCorrect) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "+$pointsEarned points",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ExerciseLoadingState() {
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
private fun ExerciseErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops! Something went wrong",
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