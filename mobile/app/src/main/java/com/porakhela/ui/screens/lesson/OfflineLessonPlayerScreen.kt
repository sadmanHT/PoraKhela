package com.porakhela.ui.screens.lesson

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porakhela.ui.components.*
import com.porakhela.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Offline-first lesson player with child-friendly UI and interactions
 * Supports video, text, quiz, and interactive content
 */
@Composable
fun OfflineLessonPlayerScreen(
    lessonId: String,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onNavigateToNextLesson: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Lesson state
    var currentStep by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    var lessonCompleted by remember { mutableStateOf(false) }
    var pointsEarned by remember { mutableStateOf(0) }
    
    // Sample lesson data - TODO: Get from ViewModel/Database
    val lesson = remember {
        LessonData(
            id = lessonId,
            title = "Number Systems",
            titleBn = "সংখ্যা পদ্ধতি",
            subject = "Mathematics",
            subjectBn = "গণিত",
            chapter = 1,
            lessonNumber = 3,
            totalSteps = 5,
            estimatedTimeMinutes = 15,
            isOfflineAvailable = true,
            steps = listOf(
                LessonStep("intro", "Introduction", "Welcome to Number Systems!", LessonStepType.TEXT),
                LessonStep("video1", "Basic Concepts", "Understanding different number types", LessonStepType.VIDEO),
                LessonStep("practice1", "Practice", "Try some examples", LessonStepType.INTERACTIVE),
                LessonStep("quiz1", "Quick Quiz", "Test your knowledge", LessonStepType.QUIZ),
                LessonStep("summary", "Summary", "What we learned today", LessonStepType.TEXT)
            )
        )
    }
    
    // Animations
    val progressAnimatedValue by animateFloatAsState(
        targetValue = if (lessonCompleted) 1f else (currentStep + 1).toFloat() / lesson.totalSteps,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    // Completion animation trigger
    LaunchedEffect(lessonCompleted) {
        if (lessonCompleted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2000) // Show celebration for 2 seconds
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PorakhelaBackground,
                        FunBlue.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header with lesson info and progress
            LessonHeader(
                lesson = lesson,
                currentStep = currentStep,
                progress = progressAnimatedValue,
                onBack = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lesson content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (!lessonCompleted) {
                    when (lesson.steps[currentStep].type) {
                        LessonStepType.TEXT -> {
                            TextLessonContent(
                                step = lesson.steps[currentStep],
                                onNext = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (currentStep < lesson.steps.size - 1) {
                                        currentStep++
                                    } else {
                                        lessonCompleted = true
                                        pointsEarned = 100
                                    }
                                }
                            )
                        }
                        
                        LessonStepType.VIDEO -> {
                            VideoLessonContent(
                                step = lesson.steps[currentStep],
                                isPlaying = isPlaying,
                                onPlayPause = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isPlaying = !isPlaying
                                },
                                onNext = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (currentStep < lesson.steps.size - 1) {
                                        currentStep++
                                    } else {
                                        lessonCompleted = true
                                        pointsEarned = 100
                                    }
                                }
                            )
                        }
                        
                        LessonStepType.INTERACTIVE -> {
                            InteractiveLessonContent(
                                step = lesson.steps[currentStep],
                                onNext = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pointsEarned += 50
                                    if (currentStep < lesson.steps.size - 1) {
                                        currentStep++
                                    } else {
                                        lessonCompleted = true
                                        pointsEarned += 100
                                    }
                                }
                            )
                        }
                        
                        LessonStepType.QUIZ -> {
                            QuizLessonContent(
                                step = lesson.steps[currentStep],
                                onNext = { score ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    pointsEarned += (score * 20) // 20 points per correct answer
                                    if (currentStep < lesson.steps.size - 1) {
                                        currentStep++
                                    } else {
                                        lessonCompleted = true
                                        pointsEarned += 100
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Lesson completion celebration
                    LessonCompletionContent(
                        lesson = lesson,
                        pointsEarned = pointsEarned,
                        onContinue = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete()
                        },
                        onNextLesson = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToNextLesson("next_lesson_id")
                        }
                    )
                }
            }
            
            // Bottom navigation
            if (!lessonCompleted && currentStep > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChildFriendlyButton(
                        text = "Previous",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentStep > 0) currentStep--
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = MaterialTheme.colorScheme.outline,
                        icon = Icons.Default.ArrowBack
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    ChildFriendlyButton(
                        text = if (currentStep == lesson.steps.size - 1) "Finish" else "Skip",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentStep < lesson.steps.size - 1) {
                                currentStep++
                            } else {
                                lessonCompleted = true
                                pointsEarned += 50 // Partial completion bonus
                            }
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = FunOrange,
                        icon = if (currentStep == lesson.steps.size - 1) Icons.Default.Check else Icons.Default.ArrowForward
                    )
                }
            }
        }
        
        // Offline indicator
        if (!lesson.isOfflineAvailable) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        contentDescription = "Offline",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Online Required",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonHeader(
    lesson: LessonData,
    currentStep: Int,
    progress: Float,
    onBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FunBlue.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Top row with back button and offline indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = FunBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                if (lesson.isOfflineAvailable) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "Available Offline",
                            tint = FunGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Offline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = FunGreen,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Lesson info
            Text(
                text = "${lesson.subject} • Chapter ${lesson.chapter} • Lesson ${lesson.lessonNumber}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = FunBlue,
                    fontWeight = FontWeight.Medium
                )
            )
            
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 24.sp
                )
            )
            
            Text(
                text = lesson.titleBn,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${currentStep + 1} of ${lesson.totalSteps}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                
                Text(
                    text = "⏱️ ${lesson.estimatedTimeMinutes} min",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = FunBlue,
                trackColor = FunBlue.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

@Composable
private fun TextLessonContent(
    step: LessonStep,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(FunBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📖",
                    fontSize = 40.sp
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = step.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    fontSize = 18.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Sample educational content
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = FunGreen.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "💡 Did you know?\nNumbers help us count, measure, and solve problems in our daily life!",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = FunGreen.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.padding(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            ChildFriendlyButton(
                text = "Got it! Continue",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunGreen,
                icon = Icons.Default.ArrowForward
            )
        }
    }
}

@Composable
private fun VideoLessonContent(
    step: LessonStep,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Video placeholder with play button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Play/Pause button
                    ChildFriendlyFAB(
                        onClick = onPlayPause,
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        backgroundColor = if (isPlaying) FunOrange else FunGreen,
                        contentDescription = if (isPlaying) "Pause Video" else "Play Video"
                    )
                    
                    // Video overlay info
                    if (!isPlaying) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "🎬 Video Lesson",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Tap to play",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = step.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            ChildFriendlyButton(
                text = "Continue to Practice",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunBlue,
                icon = Icons.Default.ArrowForward
            )
        }
    }
}

@Composable
private fun InteractiveLessonContent(
    step: LessonStep,
    onNext: () -> Unit
) {
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Interactive question
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = FunPink.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤔",
                        fontSize = 48.sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Which of these is a natural number?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Answer options
            val answers = listOf(
                Pair("A) -5", false),
                Pair("B) 0.5", false),
                Pair("C) 7", true),
                Pair("D) √2", false)
            )
            
            answers.forEachIndexed { index, (answer, isCorrect) ->
                ChildFriendlyButton(
                    text = answer,
                    onClick = {
                        selectedAnswer = index
                        showFeedback = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    backgroundColor = when {
                        selectedAnswer == null -> FunBlue
                        selectedAnswer == index && isCorrect -> FunGreen
                        selectedAnswer == index && !isCorrect -> MaterialTheme.colorScheme.error
                        isCorrect && showFeedback -> FunGreen
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
            
            if (showFeedback) {
                Spacer(modifier = Modifier.height(16.dp))
                
                val isCorrect = selectedAnswer?.let { answers[it].second } ?: false
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) FunGreen.copy(alpha = 0.1f) 
                                       else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isCorrect) "🎉 Correct! Natural numbers are positive counting numbers like 1, 2, 3, 7..."
                               else "🤗 Not quite! Natural numbers are positive counting numbers. Try again!",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                if (isCorrect) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ChildFriendlyButton(
                        text = "Great! Continue",
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FunGreen,
                        icon = Icons.Default.ArrowForward
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizLessonContent(
    step: LessonStep,
    onNext: (Int) -> Unit // Pass score to parent
) {
    var currentQuestion by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf(mutableListOf<Int>()) }
    var score by remember { mutableStateOf(0) }
    var showResults by remember { mutableStateOf(false) }
    
    // Sample quiz questions
    val questions = listOf(
        QuizQuestion("What is 2 + 2?", listOf("3", "4", "5", "6"), 1),
        QuizQuestion("Which is larger: 5 or 3?", listOf("3", "5", "Same", "Can't tell"), 1),
        QuizQuestion("What comes after 9?", listOf("8", "10", "11", "12"), 1)
    )
    
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showResults) {
                // Quiz header
                Text(
                    text = "🎯 Quick Quiz",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Progress
                Text(
                    text = "Question ${currentQuestion + 1} of ${questions.size}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
                
                LinearProgressIndicator(
                    progress = (currentQuestion + 1).toFloat() / questions.size,
                    modifier = Modifier.fillMaxWidth(),
                    color = FunBlue
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Question
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = FunOrange.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = questions[currentQuestion].question,
                        style = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Answer options
                questions[currentQuestion].options.forEachIndexed { index, option ->
                    ChildFriendlyButton(
                        text = option,
                        onClick = {
                            val isCorrect = index == questions[currentQuestion].correctAnswer
                            selectedAnswers.add(index)
                            
                            if (isCorrect) score++
                            
                            if (currentQuestion < questions.size - 1) {
                                currentQuestion++
                            } else {
                                showResults = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        backgroundColor = FunBlue
                    )
                }
            } else {
                // Quiz results
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏆",
                        fontSize = 64.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Quiz Complete!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You scored $score out of ${questions.size}!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            color = if (score >= questions.size * 0.7) FunGreen else FunOrange
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val percentage = (score.toFloat() / questions.size * 100).toInt()
                    
                    ProgressCircle(
                        progress = score.toFloat() / questions.size,
                        size = 120.dp,
                        strokeWidth = 12.dp,
                        color = if (percentage >= 70) FunGreen else FunOrange
                    ) {
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (percentage >= 70) FunGreen else FunOrange
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    AnimatedPorapointsBadge(
                        points = score * 20,
                        showAnimation = true
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ChildFriendlyButton(
                        text = "Continue Learning!",
                        onClick = { onNext(score) },
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = FunGreen,
                        icon = Icons.Default.ArrowForward
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCompletionContent(
    lesson: LessonData,
    pointsEarned: Int,
    onContinue: () -> Unit,
    onNextLesson: () -> Unit
) {
    // Completion animation
    val infiniteTransition = rememberInfiniteTransition()
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = AchievementGold.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated celebration
            Box(
                modifier = Modifier.scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎉",
                    fontSize = 80.sp,
                    modifier = Modifier.graphicsLayer(rotationZ = sparkleRotation)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Lesson Complete!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = AchievementGold,
                    fontSize = 32.sp
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Amazing work on completing\n\"${lesson.title}\"!",
                style = MaterialTheme.typography.titleLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            AnimatedPorapointsBadge(
                points = pointsEarned,
                showAnimation = true,
                modifier = Modifier.scale(1.2f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action buttons
            ChildFriendlyButton(
                text = "Next Lesson",
                onClick = onNextLesson,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunGreen,
                icon = Icons.Default.ArrowForward
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ChildFriendlyButton(
                text = "Back to Dashboard",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunBlue,
                icon = Icons.Default.Home
            )
        }
    }
}

// Data classes
data class LessonData(
    val id: String,
    val title: String,
    val titleBn: String,
    val subject: String,
    val subjectBn: String,
    val chapter: Int,
    val lessonNumber: Int,
    val totalSteps: Int,
    val estimatedTimeMinutes: Int,
    val isOfflineAvailable: Boolean,
    val steps: List<LessonStep>
)

data class LessonStep(
    val id: String,
    val title: String,
    val content: String,
    val type: LessonStepType
)

enum class LessonStepType {
    TEXT,
    VIDEO,
    INTERACTIVE,
    QUIZ
}

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)