package com.porakhela.ui.screens.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
 * Parent Dashboard - Control center for monitoring children's learning
 * Features: Child management, progress tracking, lesson downloads, parental controls
 */
@Composable
fun ParentDashboardScreen(
    onNavigateToChildProfile: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    // Sample data - replace with actual data from ViewModel
    val children = remember {
        listOf(
            ChildDashboardData(
                id = "1",
                name = "Arman",
                avatar = "🧒",
                grade = 6,
                totalPoints = 150,
                weeklyScreenTime = 45,
                lessonsCompleted = 8,
                currentStreak = 5,
                subjects = listOf("Math", "Bangla", "English"),
                recentActivity = "Completed Math Chapter 3",
                isOnline = false
            ),
            ChildDashboardData(
                id = "2", 
                name = "Sadia",
                avatar = "👧",
                grade = 4,
                totalPoints = 95,
                weeklyScreenTime = 32,
                lessonsCompleted = 6,
                currentStreak = 3,
                subjects = listOf("Bangla", "Science", "Art"),
                recentActivity = "Started Bangla lesson 'আমাদের ভাষা'",
                isOnline = true
            )
        )
    }
    
    val downloadStats = remember {
        DownloadStats(
            totalLessons = 124,
            downloadedLessons = 89,
            downloadProgress = 0.72f,
            storageUsed = "2.4 GB",
            storageTotal = "16 GB"
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PorakhelaBackground)
            .padding(20.dp)
    ) {
        // Header with parent info
        ParentDashboardHeader(
            onNavigateToSettings = onNavigateToSettings
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Children overview section
            item {
                Text(
                    text = "👨‍👩‍👧‍👦 Your Children",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(children) { child ->
                        ChildSummaryCard(
                            child = child,
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToChildProfile(child.id) 
                            }
                        )
                    }
                }
            }
            
            // Quick actions section
            item {
                Text(
                    text = "⚡ Quick Actions",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickActionCard(
                            title = "Download Lessons",
                            subtitle = "Offline learning",
                            icon = Icons.Default.CloudDownload,
                            backgroundColor = FunBlue,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToDownloads()
                            }
                        )
                    }
                    
                    item {
                        QuickActionCard(
                            title = "Progress Reports",
                            subtitle = "Weekly insights",
                            icon = Icons.Default.Analytics,
                            backgroundColor = FunGreen,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToReports()
                            }
                        )
                    }
                    
                    item {
                        QuickActionCard(
                            title = "Screen Time",
                            subtitle = "Set limits",
                            icon = Icons.Default.Timer,
                            backgroundColor = FunOrange,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Navigate to screen time settings
                            }
                        )
                    }
                    
                    item {
                        QuickActionCard(
                            title = "Rewards",
                            subtitle = "Approve requests",
                            icon = Icons.Default.Redeem,
                            backgroundColor = FunPink,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Navigate to rewards approval
                            }
                        )
                    }
                }
            }
            
            // Download status
            item {
                DownloadStatusCard(
                    stats = downloadStats,
                    onManageDownloads = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToDownloads()
                    }
                )
            }
            
            // Family learning insights
            item {
                FamilyInsightsCard(
                    children = children
                )
            }
            
            // USSD access for basic phones
            item {
                USSDAccessCard()
            }
        }
    }
}

@Composable
private fun ParentDashboardHeader(
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Good Morning! 🌅",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = "Ready to support your children's learning?",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
        
        IconButton(
            onClick = onNavigateToSettings
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ChildSummaryCard(
    child: ChildDashboardData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(280.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (child.isOnline) FunGreen.copy(alpha = 0.1f) 
                          else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Child header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (child.isOnline) FunGreen.copy(alpha = 0.2f) 
                                else Color.Gray.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = child.avatar,
                            fontSize = 24.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Grade ${child.grade}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
                
                // Online status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (child.isOnline) FunGreen else Color.Gray
                            )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (child.isOnline) "Online" else "Offline",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (child.isOnline) FunGreen else Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Star,
                    value = child.totalPoints.toString(),
                    label = "Points",
                    color = PointsGold
                )
                StatItem(
                    icon = Icons.Default.Timer,
                    value = "${child.weeklyScreenTime}m",
                    label = "This week",
                    color = FunBlue
                )
                StatItem(
                    icon = Icons.Default.LocalFireDepartment,
                    value = child.currentStreak.toString(),
                    label = "Day streak",
                    color = StreakFire
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recent activity
            Text(
                text = "📚 ${child.recentActivity}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = backgroundColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = backgroundColor,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DownloadStatusCard(
    stats: DownloadStats,
    onManageDownloads: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                    text = "💾 Downloaded Content",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                TextButton(onClick = onManageDownloads) {
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = FunBlue,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = stats.downloadProgress,
                modifier = Modifier.fillMaxWidth(),
                color = FunBlue,
                trackColor = FunBlue.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${stats.downloadedLessons}/${stats.totalLessons} lessons",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = "${stats.storageUsed} / ${stats.storageTotal}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
private fun FamilyInsightsCard(
    children: List<ChildDashboardData>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "📊 This Week's Insights",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val totalPoints = children.sumOf { it.totalPoints }
            val totalScreenTime = children.sumOf { it.weeklyScreenTime }
            val totalLessons = children.sumOf { it.lessonsCompleted }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    icon = Icons.Default.Star,
                    value = totalPoints.toString(),
                    label = "Family Points",
                    color = PointsGold
                )
                StatItem(
                    icon = Icons.Default.Schedule,
                    value = "${totalScreenTime}m",
                    label = "Screen Time",
                    color = FunBlue
                )
                StatItem(
                    icon = Icons.Default.MenuBook,
                    value = totalLessons.toString(),
                    label = "Lessons Done",
                    color = FunGreen
                )
            }
        }
    }
}

@Composable
private fun USSDAccessCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = FunPurple.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📞 For Basic Phones",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = FunPurple
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Monitor your children's progress even without a smartphone! Dial *123# from any phone to:",
                style = MaterialTheme.typography.bodyLarge.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column {
                Text("✓ Check children's Porapoints", style = MaterialTheme.typography.bodyMedium)
                Text("✓ View learning progress", style = MaterialTheme.typography.bodyMedium)
                Text("✓ Approve reward requests", style = MaterialTheme.typography.bodyMedium)
                Text("✓ Set screen time limits", style = MaterialTheme.typography.bodyMedium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ChildFriendlyButton(
                text = "Dial *123# Now",
                onClick = {
                    // TODO: Open dialer with *123#
                },
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = FunPurple,
                icon = Icons.Default.Phone
            )
        }
    }
}

// Data classes for dashboard
data class ChildDashboardData(
    val id: String,
    val name: String,
    val avatar: String,
    val grade: Int,
    val totalPoints: Int,
    val weeklyScreenTime: Int, // minutes
    val lessonsCompleted: Int,
    val currentStreak: Int,
    val subjects: List<String>,
    val recentActivity: String,
    val isOnline: Boolean
)

data class DownloadStats(
    val totalLessons: Int,
    val downloadedLessons: Int,
    val downloadProgress: Float,
    val storageUsed: String,
    val storageTotal: String
)