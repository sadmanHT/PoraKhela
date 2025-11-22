package com.porakhela.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
import com.porakhela.ui.components.ChildFriendlyButton
import com.porakhela.ui.components.GradientBackground
import com.porakhela.ui.theme.*

/**
 * Child-friendly OTP verification with large input boxes
 */
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var canResend by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    val haptic = LocalHapticFeedback.current
    
    // Animation for successful input
    var showSuccessAnimation by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    // Auto-verify when 6 digits are entered
    LaunchedEffect(otp) {
        if (otp.length == 6) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showSuccessAnimation = true
            delay(500) // Brief delay to show completion
            // TODO: Auto-verify OTP
        }
    }
    
    // Countdown timer for resend
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }
    
    GradientBackground(
        startColor = FunGreen,
        endColor = FunBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Top navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "Step 2 of 3",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Header with animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp
                    ),
                    modifier = Modifier.scale(if (showSuccessAnimation) pulseScale else 1f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Check Your Phone!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "আপনার ফোন চেক করুন!",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "We sent a 6-digit code to:",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // OTP input card
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
                        text = "🔢 Enter the 6-digit code",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = FunGreen,
                            fontSize = 20.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Large OTP input boxes
                    OtpInputBoxes(
                        otpValue = otp,
                        onValueChange = { newValue ->
                            if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                                otp = newValue
                                errorMessage = null
                            }
                        },
                        isError = errorMessage != null
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Progress indicator
                    LinearProgressIndicator(
                        progress = otp.length / 6f,
                        modifier = Modifier.fillMaxWidth(),
                        color = if (otp.length == 6) FunGreen else FunBlue,
                        trackColor = Color.Gray.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "${otp.length} of 6 digits entered",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (otp.length == 6) FunGreen else Color.Gray
                        )
                    )
                    
                    // Success animation
                    AnimatedVisibility(
                        visible = otp.length == 6,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Complete",
                                tint = FunGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ready to verify!",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = FunGreen,
                                    fontWeight = FontWeight.Bold
                                )
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
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Verify button
                    ChildFriendlyButton(
                        text = if (isLoading) "Verifying... 🔄" 
                               else if (otp.length == 6) "Verify Code! ✨" 
                               else "Enter 6-digit code",
                        onClick = {
                            if (otp.length == 6) {
                                isLoading = true
                                errorMessage = null
                                // TODO: Implement actual OTP verification
                                // For demo, navigate immediately
                                onNavigateToHome()
                            } else {
                                errorMessage = "Please enter the complete 6-digit code"
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (otp.length == 6) FunGreen else MaterialTheme.colorScheme.primary,
                        icon = if (otp.length == 6) Icons.Default.CheckCircle else Icons.Default.Message,
                        elevation = 12.dp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Resend section
                    if (canResend) {
                        ChildFriendlyButton(
                            text = "Resend Code 🔄",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // TODO: Implement resend OTP
                                canResend = false
                                countdown = 60
                            },
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color.Gray.copy(alpha = 0.1f),
                            contentColor = FunBlue,
                            icon = Icons.Default.Refresh
                        )
                    } else {
                        Text(
                            text = "📱 Resend code in ${countdown}s",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Help section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💡 Need help?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check your SMS messages or ask a parent for help",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpInputBoxes(
    otpValue: String,
    onValueChange: (String) -> Unit,
    isError: Boolean = false
) {
    BasicTextField(
        value = otpValue,
        onValueChange = onValueChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(6) { index ->
                    val char = when {
                        index >= otpValue.length -> ""
                        else -> otpValue[index].toString()
                    }
                    
                    val isFocused = index == otpValue.length
                    val isCompleted = index < otpValue.length
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isError -> ErrorRed.copy(alpha = 0.1f)
                                    isCompleted -> FunGreen.copy(alpha = 0.1f)
                                    isFocused -> FunBlue.copy(alpha = 0.1f)
                                    else -> Color.Gray.copy(alpha = 0.1f)
                                }
                            )
                            .border(
                                width = 2.dp,
                                color = when {
                                    isError -> ErrorRed
                                    isCompleted -> FunGreen
                                    isFocused -> FunBlue
                                    else -> Color.Gray.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = when {
                                    isError -> ErrorRed
                                    isCompleted -> FunGreen
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )
                    }
                }
            }
        }
    )
}