package com.porakhela.ui.screens.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.porakhela.ui.components.FunButton
import com.porakhela.ui.components.PorapointsBadge
import com.porakhela.ui.theme.*

/**
 * Interactive quiz screen with gamified elements
 */
@Composable
fun QuizScreen(
    quizId: String,
    onNavigateBack: () -> Unit,
    onQuizComplete: (Int, Int) -> Unit // score, points earned
) {
    // TODO: Load quiz data from repository
    val quiz = remember { getQuizById(quizId) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var showResult by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(quiz.timeLimit) }
    var isQuizComplete by remember { mutableStateOf(false) }
    
    // Timer
    LaunchedEffect(timeRemaining) {
        if (timeRemaining > 0 && !isQuizComplete) {
            delay(1000)
            timeRemaining--
        } else if (timeRemaining == 0) {
            isQuizComplete = true
            showResult = true
        }
    }
    
    val currentQuestion = quiz.questions[currentQuestionIndex]
    val progress = (currentQuestionIndex + 1).toFloat() / quiz.questions.size
    
    if (showResult) {
        QuizResultScreen(
            quiz = quiz,
            selectedAnswers = selectedAnswers,
            onNavigateBack = onNavigateBack,
            onQuizComplete = onQuizComplete
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            QuizHeader(
                title = quiz.title,
                progress = progress,
                timeRemaining = timeRemaining,
                questionNumber = currentQuestionIndex + 1,
                totalQuestions = quiz.questions.size,
                onNavigateBack = onNavigateBack
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Question content
            QuestionContent(
                question = currentQuestion,
                selectedAnswer = selectedAnswers[currentQuestionIndex],
                onAnswerSelected = { answer ->
                    selectedAnswers[currentQuestionIndex] = answer
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentQuestionIndex > 0) {
                    OutlinedButton(
                        onClick = { currentQuestionIndex-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Previous")
                    }
                }
                
                FunButton(
                    text = if (currentQuestionIndex == quiz.questions.size - 1) 
                        "Finish Quiz" 
                    else 
                        "Next Question",
                    onClick = {
                        if (currentQuestionIndex == quiz.questions.size - 1) {
                            isQuizComplete = true
                            showResult = true
                        } else {
                            currentQuestionIndex++
                        }
                    },
                    enabled = selectedAnswers[currentQuestionIndex] != null,
                    modifier = Modifier.weight(if (currentQuestionIndex > 0) 1f else 2f)
                )
            }
        }
    }
}

@Composable
private fun QuizHeader(
    title: String,
    progress: Float,
    timeRemaining: Int,
    questionNumber: Int,
    totalQuestions: Int,
    onNavigateBack: () -> Unit
) {
    Column {
        // Top bar
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
            
            // Timer
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (timeRemaining <= 60) 
                        MaterialTheme.colorScheme.errorContainer 
                    else 
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "⏰ ${formatTime(timeRemaining)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (timeRemaining <= 60) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quiz title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Question $questionNumber of $totalQuestions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QuestionContent(
    question: QuizQuestion,
    selectedAnswer: Int?,
    onAnswerSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Question text
            Text(
                text = question.question,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Answer options
            question.options.forEachIndexed { index, option ->
                AnswerOption(
                    text = option,
                    index = index,
                    isSelected = selectedAnswer == index,
                    onSelected = { onAnswerSelected(index) }
                )
                
                if (index < question.options.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            
            // Question points
            if (question.points > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "💰 This question is worth ${question.points} points",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    index: Int,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Card(
        onClick = onSelected,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) 
            androidx.compose.foundation.border.BorderStroke(
                2.dp, 
                MaterialTheme.colorScheme.primary
            ) 
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option label (A, B, C, D)
            Card(
                modifier = Modifier.size(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ('A' + index).toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) 
                            Color.White 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuizResultScreen(
    quiz: QuizData,
    selectedAnswers: Map<Int, Int>,
    onNavigateBack: () -> Unit,
    onQuizComplete: (Int, Int) -> Unit
) {
    var correctAnswers = 0
    var totalPoints = 0
    
    // Calculate score
    quiz.questions.forEachIndexed { index, question ->
        val selectedAnswer = selectedAnswers[index]
        if (selectedAnswer == question.correctAnswer) {
            correctAnswers++
            totalPoints += question.points
        }
    }
    
    val scorePercentage = (correctAnswers * 100) / quiz.questions.size
    val bonusPoints = when {
        scorePercentage >= 90 -> 100
        scorePercentage >= 80 -> 75
        scorePercentage >= 70 -> 50
        scorePercentage >= 60 -> 25
        else -> 0
    }
    
    LaunchedEffect(Unit) {
        onQuizComplete(scorePercentage, totalPoints + bonusPoints)
    }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Result emoji and message
            Text(
                text = when {
                    scorePercentage >= 90 -> "🏆"
                    scorePercentage >= 80 -> "🎉"
                    scorePercentage >= 70 -> "👏"
                    scorePercentage >= 60 -> "👍"
                    else -> "💪"
                },
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 120.sp
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = when {
                    scorePercentage >= 90 -> "Outstanding!"
                    scorePercentage >= 80 -> "Great Job!"
                    scorePercentage >= 70 -> "Well Done!"
                    scorePercentage >= 60 -> "Good Effort!"
                    else -> "Keep Trying!"
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You got $correctAnswers out of ${quiz.questions.size} questions correct",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Results card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Score
                    Text(
                        text = "$scorePercentage%",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Your Score",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Points earned
                    PorapointsBadge(
                        points = totalPoints + bonusPoints,
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        textColor = MaterialTheme.colorScheme.primary
                    )
                    
                    if (bonusPoints > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🎁 +$bonusPoints bonus points for great performance!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Performance message
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = when {
                                scorePercentage >= 90 -> "Perfect! You're a quiz champion! 🌟"
                                scorePercentage >= 80 -> "Excellent work! You really understand this topic! ✨"
                                scorePercentage >= 70 -> "Good job! You're getting the hang of it! 💪"
                                scorePercentage >= 60 -> "Nice try! Review the lesson and try again! 📚"
                                else -> "Don't give up! Practice makes perfect! 🎯"
                            },
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                FunButton(
                    text = "Continue Learning",
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (scorePercentage < 70) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { /* TODO: Retry quiz */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Retry Quiz")
                    }
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

// Data classes
data class QuizData(
    val id: String,
    val title: String,
    val timeLimit: Int, // in seconds
    val questions: List<QuizQuestion>
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val points: Int
)

// Sample data function
private fun getQuizById(id: String): QuizData {
    // TODO: Replace with actual data loading
    return QuizData(
        id = id,
        title = "Basic Addition Quiz",
        timeLimit = 300, // 5 minutes
        questions = listOf(
            QuizQuestion(
                question = "What is 5 + 3?",
                options = listOf("6", "7", "8", "9"),
                correctAnswer = 2,
                points = 10
            ),
            QuizQuestion(
                question = "What is 12 + 7?",
                options = listOf("18", "19", "20", "21"),
                correctAnswer = 1,
                points = 10
            ),
            QuizQuestion(
                question = "What is 25 + 16?",
                options = listOf("39", "40", "41", "42"),
                correctAnswer = 2,
                points = 15
            ),
            QuizQuestion(
                question = "If you have 8 apples and your friend gives you 5 more, how many apples do you have?",
                options = listOf("11", "12", "13", "14"),
                correctAnswer = 2,
                points = 15
            ),
            QuizQuestion(
                question = "What is 100 + 250?",
                options = listOf("340", "350", "360", "370"),
                correctAnswer = 1,
                points = 20
            )
        )
    )
}