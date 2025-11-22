package com.porakhela.ui.screens.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.porakhela.ui.components.FunButton
import com.porakhela.ui.components.PorapointsBadge
import com.porakhela.ui.theme.*

/**
 * Subject detail screen showing lessons and chapters
 */
@Composable
fun SubjectDetailScreen(
    subjectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToLesson: (String) -> Unit
) {
    // TODO: Load subject data from repository
    val subject = remember { getSubjectById(subjectId) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with subject info
        SubjectHeader(
            subject = subject,
            onNavigateBack = onNavigateBack
        )
        
        // Lessons list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subject.chapters) { chapter ->
                ChapterCard(
                    chapter = chapter,
                    onLessonClick = onNavigateToLesson
                )
            }
        }
    }
}

@Composable
private fun SubjectHeader(
    subject: SubjectDetailData,
    onNavigateBack: () -> Unit
) {
    GradientBackground(
        color = Color(subject.color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                        tint = Color.White
                    )
                }
                
                PorapointsBadge(
                    points = 1250,
                    backgroundColor = Color.White.copy(alpha = 0.2f),
                    textColor = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subject info
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject.emoji,
                    style = MaterialTheme.typography.displayLarge
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    
                    Text(
                        text = "Class ${subject.className}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Overall Progress",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = subject.overallProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${(subject.overallProgress * 100).toInt()}% Complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "${subject.averageScore}/100",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Avg Score",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: ChapterData,
    onLessonClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Chapter header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chapter ${chapter.number}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                // Chapter status
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (chapter.status) {
                            ChapterStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            ChapterStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primaryContainer
                            ChapterStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = when (chapter.status) {
                            ChapterStatus.COMPLETED -> "✓ Complete"
                            ChapterStatus.IN_PROGRESS -> "📖 Learning"
                            ChapterStatus.LOCKED -> "🔒 Locked"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = when (chapter.status) {
                            ChapterStatus.COMPLETED -> Color.White
                            ChapterStatus.IN_PROGRESS -> MaterialTheme.colorScheme.onPrimaryContainer
                            ChapterStatus.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            if (chapter.status != ChapterStatus.LOCKED) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lessons
                chapter.lessons.forEach { lesson ->
                    LessonItem(
                        lesson = lesson,
                        onClick = { onLessonClick(lesson.id) }
                    )
                    
                    if (lesson != chapter.lessons.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonItem(
    lesson: LessonData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        enabled = lesson.status != LessonStatus.LOCKED
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lesson icon
            Card(
                modifier = Modifier.size(40.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (lesson.type) {
                        LessonType.VIDEO -> MaterialTheme.colorScheme.primary
                        LessonType.QUIZ -> MaterialTheme.colorScheme.secondary
                        LessonType.READING -> MaterialTheme.colorScheme.tertiary
                    }
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (lesson.type) {
                            LessonType.VIDEO -> "📺"
                            LessonType.QUIZ -> "❓"
                            LessonType.READING -> "📖"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Lesson info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lesson.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${lesson.duration} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (lesson.points > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• +${lesson.points} points",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Lesson status
            when (lesson.status) {
                LessonStatus.COMPLETED -> {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Completed",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                }
                LessonStatus.AVAILABLE -> {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                LessonStatus.LOCKED -> {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// Data classes
data class SubjectDetailData(
    val id: String,
    val name: String,
    val emoji: String,
    val color: Long,
    val className: String,
    val overallProgress: Float,
    val averageScore: Int,
    val chapters: List<ChapterData>
)

data class ChapterData(
    val id: String,
    val number: Int,
    val title: String,
    val status: ChapterStatus,
    val lessons: List<LessonData>
)

data class LessonData(
    val id: String,
    val title: String,
    val type: LessonType,
    val duration: Int,
    val points: Int,
    val status: LessonStatus
)

enum class ChapterStatus {
    COMPLETED, IN_PROGRESS, LOCKED
}

enum class LessonStatus {
    COMPLETED, AVAILABLE, LOCKED
}

enum class LessonType {
    VIDEO, QUIZ, READING
}

// Sample data function
private fun getSubjectById(id: String): SubjectDetailData {
    // TODO: Replace with actual data loading
    return SubjectDetailData(
        id = id,
        name = when (id) {
            "math" -> "Mathematics"
            "science" -> "Science"
            "bangla" -> "Bangla"
            "english" -> "English"
            "social" -> "Social Studies"
            "religion" -> "Religion"
            else -> "Unknown Subject"
        },
        emoji = when (id) {
            "math" -> "🔢"
            "science" -> "🧪"
            "bangla" -> "📖"
            "english" -> "🗣️"
            "social" -> "🌍"
            "religion" -> "📿"
            else -> "📚"
        },
        color = when (id) {
            "math" -> 0xFFFF9800
            "science" -> 0xFF9C27B0
            "bangla" -> 0xFF4CAF50
            "english" -> 0xFF2196F3
            "social" -> 0xFFF44336
            "religion" -> 0xFF795548
            else -> 0xFF607D8B
        },
        className = "VIII",
        overallProgress = 0.65f,
        averageScore = 87,
        chapters = sampleChapters
    )
}

private val sampleChapters = listOf(
    ChapterData(
        id = "ch1",
        number = 1,
        title = "Introduction to Numbers",
        status = ChapterStatus.COMPLETED,
        lessons = listOf(
            LessonData("l1", "What are Numbers?", LessonType.VIDEO, 15, 50, LessonStatus.COMPLETED),
            LessonData("l2", "Types of Numbers", LessonType.READING, 10, 25, LessonStatus.COMPLETED),
            LessonData("l3", "Number Quiz", LessonType.QUIZ, 5, 75, LessonStatus.COMPLETED)
        )
    ),
    ChapterData(
        id = "ch2",
        number = 2,
        title = "Basic Operations",
        status = ChapterStatus.IN_PROGRESS,
        lessons = listOf(
            LessonData("l4", "Addition & Subtraction", LessonType.VIDEO, 20, 50, LessonStatus.COMPLETED),
            LessonData("l5", "Multiplication", LessonType.VIDEO, 18, 50, LessonStatus.AVAILABLE),
            LessonData("l6", "Division", LessonType.VIDEO, 18, 50, LessonStatus.LOCKED),
            LessonData("l7", "Operations Quiz", LessonType.QUIZ, 10, 100, LessonStatus.LOCKED)
        )
    ),
    ChapterData(
        id = "ch3",
        number = 3,
        title = "Fractions",
        status = ChapterStatus.LOCKED,
        lessons = listOf(
            LessonData("l8", "Understanding Fractions", LessonType.VIDEO, 25, 50, LessonStatus.LOCKED),
            LessonData("l9", "Adding Fractions", LessonType.VIDEO, 20, 50, LessonStatus.LOCKED),
            LessonData("l10", "Fraction Quiz", LessonType.QUIZ, 8, 100, LessonStatus.LOCKED)
        )
    )
)