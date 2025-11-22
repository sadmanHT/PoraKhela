package com.porakhela.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.porakhela.ui.components.*
import com.porakhela.ui.theme.*

/**
 * Child-friendly main dashboard with large touch targets, animations, and engaging UI
 */
@Composable
fun DashboardScreen(
    onNavigateToSubject: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAvatarSelection: () -> Unit,
    onNavigateToAchievements: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Child profile data - TODO: Get from ViewModel
    val childProfile = remember {
        ChildProfile(
            name = "Arman",
            avatar = "🧒",
            grade = 6,
            totalPoints = 1250,
            currentStreak = 7,
            level = 15,
            nextLevelPoints = 1500,
            todayLessons = 3,
            weeklyGoal = 5
        )
    }
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition()
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val pointsScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PorakhelaBackground)
            .padding(20.dp)
    ) {
        // Animated header with child info
        ChildDashboardHeader(
            profile = childProfile,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAvatarSelection = onNavigateToAvatarSelection,
            breathingScale = breathingScale,
            pointsScale = pointsScale
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Streak and daily goals
            item {
                StreakAndGoalsCard(
                    profile = childProfile,
                    onNavigateToLeaderboard = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToLeaderboard()
                    }
                )
            }
            
            // Continue learning section
            item {
                Text(
                    text = "📚 Continue Learning",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(continueSubjects) { subject ->
                        ContinueLearningCard(
                            subject = subject,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToSubject(subject.id) 
                            }
                        )
                    }
                }
            }
            
            // All subjects grid with child-friendly design
            item {
                Text(
                    text = "🎓 All Subjects",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 2-column grid for subjects
                val chunkedSubjects = subjects.chunked(2)
                chunkedSubjects.forEach { rowSubjects ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowSubjects.forEach { subject ->
                            SubjectSelectionCard(
                                subjectName = subject.name,
                                subjectNameBn = subject.nameBn,
                                subjectColor = Color(subject.color),
                                subjectIcon = subject.icon,
                                progress = subject.progress,
                                lessonsCount = subject.totalLessons,
                                isDownloaded = subject.isDownloaded,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onNavigateToSubject(subject.id)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if odd number
                        if (rowSubjects.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    
                    if (rowSubjects != chunkedSubjects.last()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            
            // Recent achievements with animations
            item {
                RecentAchievementsCard(
                    achievements = recentAchievements,
                    onNavigateToAchievements = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToAchievements()
                    }
                )
            }
            
            // Fun motivational message
            item {
                MotivationalCard(childName = childProfile.name)
            }
        }
    }
}

@Composable
private fun ChildDashboardHeader(
    profile: ChildProfile,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAvatarSelection: () -> Unit,
    breathingScale: Float,
    pointsScale: Float
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
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Child info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated avatar
                    Card(
                        onClick = onNavigateToAvatarSelection,
                        modifier = Modifier
                            .size(80.dp)
                            .scale(breathingScale),
                        colors = CardDefaults.cardColors(
                            containerColor = FunBlue.copy(alpha = 0.2f)
                        ),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.avatar,
                                fontSize = 40.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Good morning! 🌅",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp,
                                color = FunBlue
                            )
                        )
                        Text(
                            text = "Grade ${profile.grade} • Level ${profile.level}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                // Settings button
                IconButton(
                    onClick = onNavigateToSettings
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = FunBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Points and progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated points display
                AnimatedPorapointsBadge(
                    points = profile.totalPoints,
                    modifier = Modifier.scale(pointsScale),
                    showAnimation = true
                )
                
                // Level progress
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Next level: ${profile.nextLevelPoints - profile.totalPoints} points",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = profile.totalPoints.toFloat() / profile.nextLevelPoints,
                        modifier = Modifier.width(120.dp),
                        color = PointsGold,
                        trackColor = PointsGold.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakAndGoalsCard(
    profile: ChildProfile,
    onNavigateToLeaderboard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = StreakFire.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🔥 ${profile.currentStreak} Day Streak!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = StreakFire,
                            fontSize = 22.sp
                        )
                    )
                    Text(
                        text = "You're on fire! Keep it up!",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    )
                }
                
                ChildFriendlyButton(
                    text = "Leaderboard",
                    onClick = onNavigateToLeaderboard,
                    modifier = Modifier.height(40.dp),
                    backgroundColor = StreakFire,
                    icon = Icons.Default.Leaderboard
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Daily goal progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Goal: ${profile.todayLessons}/${profile.weeklyGoal} lessons",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                ProgressCircle(
                    progress = profile.todayLessons.toFloat() / profile.weeklyGoal,
                    size = 40.dp,
                    strokeWidth = 4.dp,
                    color = if (profile.todayLessons >= profile.weeklyGoal) FunGreen else FunOrange
                ) {
                    Text(
                        text = "${(profile.todayLessons.toFloat() / profile.weeklyGoal * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueLearningCard(
    subject: SubjectData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(subject.color).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subject.emoji,
                    fontSize = 32.sp
                )
                
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Continue",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            )
            
            Text(
                text = subject.nameBn,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Chapter ${subject.currentChapter} • Lesson ${subject.currentLesson}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.8f)
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = subject.progress,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "${(subject.progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
private fun RecentAchievementsCard(
    achievements: List<AchievementData>,
    onNavigateToAchievements: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Recent Achievements",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AchievementGold,
                        fontSize = 20.sp
                    )
                )
                
                TextButton(onClick = onNavigateToAchievements) {
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = FunBlue,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            achievements.forEach { achievement ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AchievementGold.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = achievement.emoji,
                            fontSize = 24.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = achievement.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                    
                    AnimatedPorapointsBadge(
                        points = achievement.points,
                        showAnimation = false
                    )
                }
                
                if (achievement != achievements.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MotivationalCard(
    childName: String
) {
    val motivationalMessages = listOf(
        "🌟 You're doing amazing, $childName!",
        "🚀 Keep exploring and learning!",
        "💪 Every lesson makes you stronger!",
        "🎯 You're on the path to greatness!",
        "✨ Your curiosity is your superpower!"
    )
    
    val message = remember { motivationalMessages.random() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FunPink.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = FunPink,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                )
            )
        }
    }
}

// Data classes
data class ChildProfile(
    val name: String,
    val avatar: String,
    val grade: Int,
    val totalPoints: Int,
    val currentStreak: Int,
    val level: Int,
    val nextLevelPoints: Int,
    val todayLessons: Int,
    val weeklyGoal: Int
)

data class SubjectData(
    val id: String,
    val name: String,
    val nameBn: String,
    val emoji: String,
    val icon: ImageVector,
    val color: Long,
    val currentChapter: Int = 1,
    val currentLesson: Int = 1,
    val progress: Float = 0f,
    val totalLessons: Int = 0,
    val isDownloaded: Boolean = false
)

data class AchievementData(
    val emoji: String,
    val title: String,
    val description: String,
    val points: Int,
    val unlockedAt: String = "Today"
)

// Sample data
private val subjects = listOf(
    SubjectData("bangla", "Bangla", "বাংলা", "📖", Icons.Default.MenuBook, 0xFF4CAF50, totalLessons = 45, isDownloaded = true),
    SubjectData("english", "English", "ইংরেজি", "🗣️", Icons.Default.Language, 0xFF2196F3, totalLessons = 38, isDownloaded = false),
    SubjectData("math", "Mathematics", "গণিত", "🔢", Icons.Default.Calculate, 0xFFFF9800, totalLessons = 52, isDownloaded = true),
    SubjectData("science", "Science", "বিজ্ঞান", "🧪", Icons.Default.Science, 0xFF9C27B0, totalLessons = 41, isDownloaded = false),
    SubjectData("social", "Social Studies", "সমাজ", "🌍", Icons.Default.Public, 0xFFF44336, totalLessons = 35, isDownloaded = true),
    SubjectData("religion", "Religion", "ধর্ম", "📿", Icons.Default.Book, 0xFF795548, totalLessons = 28, isDownloaded = false)
)

private val continueSubjects = listOf(
    SubjectData("math", "Mathematics", "গণিত", "🔢", Icons.Default.Calculate, 0xFFFF9800, 3, 2, 0.65f),
    SubjectData("science", "Science", "বিজ্ঞান", "🧪", Icons.Default.Science, 0xFF9C27B0, 2, 4, 0.45f),
    SubjectData("bangla", "Bangla", "বাংলা", "📖", Icons.Default.MenuBook, 0xFF4CAF50, 4, 1, 0.80f)
)

private val recentAchievements = listOf(
    AchievementData("🎯", "Quiz Master", "Perfect score in Math Quiz!", 50),
    AchievementData("🔥", "Week Warrior", "7-day learning streak achieved", 100),
    AchievementData("📚", "Chapter Champion", "Completed Science Chapter 2", 75)
)