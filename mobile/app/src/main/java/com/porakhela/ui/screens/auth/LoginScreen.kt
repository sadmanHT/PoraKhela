package com.porakhela.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.porakhela.ui.components.ChildFriendlyButton
import com.porakhela.ui.components.GradientBackground
import com.porakhela.ui.components.ChildFriendlyLoadingAnimation
import com.porakhela.ui.theme.*

/**
 * Child-friendly phone number input screen with large touch targets
 */
@Composable
fun LoginScreen(
    onNavigateToOtp: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToParentalPin: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current
    
    // Animation for the welcome icon
    val infiniteTransition = rememberInfiniteTransition()
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Phone number validation
    val isValidPhone = phoneNumber.length >= 11 && phoneNumber.startsWith("01")
    val buttonColor by animateColorAsState(
        targetValue = if (isValidPhone) FunGreen else MaterialTheme.colorScheme.primary,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    if (isLoading) {
        ChildFriendlyLoadingAnimation(
            message = "Connecting to Porakhela..."
        )
        return
    }
    
    GradientBackground(
        startColor = FunBlue,
        endColor = FunPurple
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Welcome animation
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Let's Get Started!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "চলুন শুরু করি!",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter your phone number to continue your learning adventure",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Phone input card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📞 Phone Number",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = FunBlue,
                            fontSize = 20.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Large phone number input
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            // Only allow digits and limit to 11 characters
                            val filtered = input.filter { it.isDigit() }.take(11)
                            phoneNumber = filtered
                            errorMessage = null
                        },
                        label = { 
                            Text(
                                "Phone Number",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        placeholder = { 
                            Text(
                                "01XXXXXXXXX",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Phone",
                                tint = if (isValidPhone) FunGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        trailingIcon = {
                            if (phoneNumber.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        phoneNumber = ""
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp), // Large touch target
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isValidPhone) FunGreen else FunBlue,
                            focusedLabelColor = if (isValidPhone) FunGreen else FunBlue,
                            cursorColor = FunBlue
                        )
                    )
                    
                    // Phone number hints
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Banglalink numbers work best",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray
                            )
                        )
                        
                        if (isValidPhone) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Valid",
                                tint = FunGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Error message
                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = ErrorRed,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Continue button
                    ChildFriendlyButton(
                        text = if (isValidPhone) "Continue! 🚀" else "Enter Phone Number",
                        onClick = {
                            when {
                                phoneNumber.isEmpty() -> {
                                    errorMessage = "Please enter your phone number"
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                !isValidPhone -> {
                                    errorMessage = "Please enter a valid 11-digit phone number"
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                else -> {
                                    isLoading = true
                                    errorMessage = null
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Simulate API call delay
                                    // TODO: Implement actual authentication logic
                                    onNavigateToOtp(phoneNumber)
                                }
                            }
                        },
                        enabled = phoneNumber.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = buttonColor,
                        icon = if (isValidPhone) Icons.Default.Send else Icons.Default.Phone,
                        elevation = 12.dp
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // New user option
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToRegister()
                        }
                    ) {
                        Text(
                            text = "🌟 New to Porakhela? Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = FunBlue,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Parental controls access
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👨‍👩‍👧‍👦 For Parents",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToParentalPin()
                        }
                    ) {
                        Text(
                            text = "Access Parental Controls",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}