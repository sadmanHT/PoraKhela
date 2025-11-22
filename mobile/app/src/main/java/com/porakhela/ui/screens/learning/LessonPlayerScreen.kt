package com.porakhela.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.porakhela.ui.components.FunButton
import com.porakhela.ui.theme.*

/**
 * Lesson player screen for video lessons with interactive elements
 */
@Composable
fun LessonPlayerScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onLessonComplete: () -> Unit
) {
    // TODO: Load lesson data from repository
    val lesson = remember { getLessonById(lessonId) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    
    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }
    
    // Progress simulation
    LaunchedEffect(isPlaying) {
        while (isPlaying && currentTime < lesson.duration * 60) {
            delay(1000)
            currentTime++
        }
        if (currentTime >= lesson.duration * 60) {
            isPlaying = false
            onLessonComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video player area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            // Video placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (lesson.type == LessonType.VIDEO) {
                    VideoPlayerPlaceholder(
                        isPlaying = isPlaying,
                        showControls = showControls,
                        onPlayPause = { isPlaying = !isPlaying },
                        onShowControls = { showControls = true }
                    )
                }
            }
            
            // Top controls
            if (showControls) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    IconButton(
                        onClick = { /* TODO: Toggle fullscreen */ },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Bottom progress bar
            if (showControls) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.7f)
                        )
                        .padding(16.dp)
                ) {
                    // Progress bar
                    val progress = currentTime.toFloat() / (lesson.duration * 60)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Time and controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentTime),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { isPlaying = !isPlaying }
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = { /* TODO: Toggle mute */ }
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        Text(
                            text = formatTime(lesson.duration * 60),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Lesson content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            // Lesson header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    Text(
                        text = "Chapter ${lesson.chapterNumber} • ${lesson.duration} minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "+${lesson.points} points",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Lesson description
            if (lesson.description.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📝 What you'll learn:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = lesson.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Interactive elements
            if (lesson.hasQuiz) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🧠 Ready for a quiz?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Test your understanding with a fun quiz!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        FunButton(
                            text = "Take Quiz (+50 bonus points)",
                            onClick = onNavigateToQuiz
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Next lesson button
            FunButton(
                text = if (currentTime >= lesson.duration * 60) 
                    "Continue to Next Lesson" 
                else 
                    "Mark as Complete",
                onClick = onLessonComplete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VideoPlayerPlaceholder(
    isPlaying: Boolean,
    showControls: Boolean,
    onPlayPause: () -> Unit,
    onShowControls: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Simulated video content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📺",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 80.sp
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isPlaying) "Video Playing..." else "Video Paused",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        // Play/pause overlay
        if (!isPlaying && showControls) {
            Card(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
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

// Data class for lesson
data class LessonPlayerData(
    val id: String,
    val title: String,
    val chapterNumber: Int,
    val duration: Int, // in minutes
    val points: Int,
    val description: String,
    val hasQuiz: Boolean,
    val type: LessonType,
    val videoUrl: String? = null
)

// Sample data function
private fun getLessonById(id: String): LessonPlayerData {
    // TODO: Replace with actual data loading
    return LessonPlayerData(
        id = id,
        title = "Understanding Basic Addition",
        chapterNumber = 2,
        duration = 15,
        points = 50,
        description = "In this lesson, we'll explore how to add numbers step by step. You'll learn different strategies for adding single-digit and multi-digit numbers, understand the concept of carrying over, and practice with fun examples.",
        hasQuiz = true,
        type = LessonType.VIDEO,
        videoUrl = "https://example.com/video.mp4"
    )
}