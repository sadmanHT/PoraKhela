package com.porakhela.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porakhela.ui.accessibility.*
import com.porakhela.ui.theme.*

/**
 * Child-friendly UI components for Porakhela
 * 48dp touch targets, large fonts, haptic feedback
 */

/**
 * Large 48dp button for child-friendly interaction
 */
@Composable
fun ChildFriendlyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null,
    elevation: Dp = 8.dp,
    hapticEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val accessibilityHelper = remember { AccessibilityHelper(context) }
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Button(
        onClick = {
            if (hapticEnabled) {
                accessibilityHelper.performHapticFeedback(AccessibilityHelper.HapticPatterns.GENTLE_FEEDBACK)
            }
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(AccessibleSpacing.TouchTargetRecommended) // 56dp for children
            .scale(scale)
            .accessibleButton(
                label = text,
                description = icon?.let { "$text button" } ?: text,
                enabled = enabled
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevation),
        interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
            LaunchedEffect(interactionSource) {
                interactionSource.interactions.collect { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release -> isPressed = false
                        is PressInteraction.Cancel -> isPressed = false
                    }
                }
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = AccessibleTextSizes.LabelLarge,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp // Larger text for children
                )
            )
        }
    }
}

/**
 * Child avatar selector component
 */
@Composable
fun ChildAvatarCard(
    avatarName: String,
    avatarImageRes: Int? = null, // Drawable resource ID
    avatarEmoji: String? = null, // Fallback emoji
    isSelected: Boolean = false,
    isUnlocked: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val borderColor = when {
        isSelected -> FunBlue
        isUnlocked -> Color.Transparent
        else -> Color.Gray
    }
    
    Card(
        onClick = {
            if (isUnlocked) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .scale(scale)
            .semantics { contentDescription = "Avatar: $avatarName" },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FunBlue.copy(alpha = 0.1f) 
                          else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 4.dp
        ),
        border = if (borderColor != Color.Transparent) BorderStroke(2.dp, borderColor) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Image/Emoji
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) FunBlue.copy(alpha = 0.1f) 
                        else Color.Gray.copy(alpha = 0.3f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (avatarImageRes != null && isUnlocked) {
                    // TODO: Load image from resources when drawable is ready
                    Text(
                        text = avatarEmoji ?: "🐱",
                        fontSize = 32.sp
                    )
                } else if (avatarEmoji != null) {
                    Text(
                        text = if (isUnlocked) avatarEmoji else "🔒",
                        fontSize = 32.sp,
                        color = if (isUnlocked) Color.Unspecified else Color.Gray
                    )
                } else {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Person else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isUnlocked) FunBlue else Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = avatarName,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
                ),
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * Subject card with large touch areas and color coding
 */
@Composable
fun SubjectSelectionCard(
    subjectName: String,
    subjectNameBn: String,
    subjectColor: Color,
    subjectIcon: ImageVector,
    progress: Float = 0f, // 0.0 to 1.0
    lessonsCount: Int = 0,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp) // Large touch area
            .scale(scale)
            .semantics { contentDescription = "Subject: $subjectName" },
        colors = CardDefaults.cardColors(
            containerColor = subjectColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = subjectColor,
                                fontSize = 20.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subjectNameBn,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = subjectColor.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                        )
                    }
                    
                    // Subject Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(subjectColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = subjectIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                // Progress and Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        if (progress > 0) {
                            Text(
                                text = "${(progress * 100).toInt()}% Complete",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = subjectColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.width(100.dp),
                                color = subjectColor,
                                trackColor = subjectColor.copy(alpha = 0.3f)
                            )
                        } else {
                            Text(
                                text = "$lessonsCount lessons",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = subjectColor.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                    
                    // Download Status
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Downloaded",
                            tint = SuccessGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fun loading animation with bouncing elements
 */
@Composable
fun ChildFriendlyLoadingAnimation(
    modifier: Modifier = Modifier,
    message: String = "Loading fun lessons...",
    showMessage: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Fun rotating loading indicator
        Box(
            modifier = Modifier
                .size(80.dp)
                .rotate(rotation)
                .scale(bounceScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            FunBlue,
                            FunPink,
                            FunOrange,
                            FunGreen,
                            FunBlue
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Loading",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        if (showMessage) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

/**
 * Points display with animation
 */
@Composable
fun AnimatedPorapointsBadge(
    points: Int,
    modifier: Modifier = Modifier,
    showAnimation: Boolean = true
) {
    var previousPoints by remember { mutableStateOf(points) }
    val animatedPoints by animateIntAsState(
        targetValue = points,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val scale by animateFloatAsState(
        targetValue = if (showAnimation && points > previousPoints) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    LaunchedEffect(points) {
        if (points > previousPoints) {
            // Trigger haptic feedback for point gain
            // haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        previousPoints = points
    }
    
    Card(
        modifier = modifier.scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = PointsGold
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Porapoints",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = animatedPoints.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            )
        }
    }
}

/**
 * Achievement unlock animation
 */
@Composable
fun AchievementUnlockAnimation(
    title: String,
    description: String,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        kotlinx.coroutines.delay(3000) // Show for 3 seconds
        onAnimationComplete()
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it }
        ) + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AchievementGold
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = "Achievement Unlocked",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Achievement Unlocked!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

/**
 * Legacy components - kept for backward compatibility
 */
@Composable
fun PorapointsBadge(
    points: Int,
    modifier: Modifier = Modifier
) {
    AnimatedPorapointsBadge(points = points, modifier = modifier, showAnimation = false)
}

@Composable
fun FunButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null
) {
    ChildFriendlyButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        icon = icon
    )
}

@Composable
fun GradientBackground(
    startColor: Color = GradientStart,
    endColor: Color = GradientEnd,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(startColor, endColor),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        content()
    }
}

@Composable
fun SubjectCard(
    subjectName: String,
    subjectColor: Color,
    progress: Float, // 0.0 to 1.0
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SubjectSelectionCard(
        subjectName = subjectName,
        subjectNameBn = subjectName, // Fallback to English if no Bengali provided
        subjectColor = subjectColor,
        subjectIcon = Icons.Default.School, // Default icon
        progress = progress,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun AchievementBadge(
    title: String,
    description: String,
    isUnlocked: Boolean,
    progress: Float = 0f, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val badgeColor = if (isUnlocked) AchievementGold else Color.Gray
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = badgeColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isUnlocked) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badge Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Achievement",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                        else Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                        else Color.Gray
                    )
                )
                
                if (!isUnlocked && progress > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressCircle(
    progress: Float, // 0.0 to 1.0
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = strokeWidth,
            trackColor = color.copy(alpha = 0.2f)
        )
        content()
    }
}

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    ChildFriendlyLoadingAnimation(
        modifier = modifier,
        message = message
    )
}