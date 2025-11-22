package com.porakhela.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.porakhela.ui.components.FunButton
import com.porakhela.ui.theme.*

/**
 * Parental PIN screen for accessing parental controls
 */
@Composable
fun ParentalPinScreen(
    onNavigateToParentalDashboard: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var isFirstTimeSetup by remember { mutableStateOf(true) } // TODO: Get from preferences
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Security icon
            Card(
                modifier = Modifier.size(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (isFirstTimeSetup) "Create Parental PIN" else "Enter Parental PIN",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isFirstTimeSetup) 
                    "Create a 4-6 digit PIN to protect parental settings" 
                else 
                    "Enter your PIN to access parental controls",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.9f)
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // PIN form
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
                    // PIN input
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { 
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                pin = it
                            }
                        },
                        label = { Text(if (isFirstTimeSetup) "Create PIN" else "Enter PIN") },
                        placeholder = { Text("4-6 digits") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "PIN"
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { pinVisible = !pinVisible }
                            ) {
                                Icon(
                                    if (pinVisible) 
                                        Icons.Default.VisibilityOff 
                                    else 
                                        Icons.Default.Visibility,
                                    contentDescription = if (pinVisible) 
                                        "Hide PIN" 
                                    else 
                                        "Show PIN"
                                )
                            }
                        },
                        visualTransformation = if (pinVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = {
                            Text("${pin.length}/6 digits")
                        }
                    )
                    
                    // Confirm PIN for first-time setup
                    if (isFirstTimeSetup) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { 
                                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                    confirmPin = it
                                }
                            },
                            label = { Text("Confirm PIN") },
                            placeholder = { Text("Re-enter PIN") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Confirm PIN"
                                )
                            },
                            visualTransformation = if (pinVisible) 
                                VisualTransformation.None 
                            else 
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = confirmPin.isNotEmpty() && pin != confirmPin
                        )
                        
                        if (confirmPin.isNotEmpty() && pin != confirmPin) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "PINs don't match",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    // Error message
                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Confirm button
                    FunButton(
                        text = if (isLoading) 
                            "Verifying..." 
                        else if (isFirstTimeSetup) 
                            "Create PIN" 
                        else 
                            "Access Controls",
                        onClick = {
                            val isValid = if (isFirstTimeSetup) {
                                pin.length >= 4 && pin == confirmPin
                            } else {
                                pin.length >= 4
                            }
                            
                            if (isValid) {
                                isLoading = true
                                errorMessage = null
                                // TODO: Implement actual PIN verification/creation
                                onNavigateToParentalDashboard()
                            } else {
                                errorMessage = if (isFirstTimeSetup) {
                                    if (pin.length < 4) "PIN must be at least 4 digits"
                                    else "PINs don't match"
                                } else {
                                    "Please enter your PIN"
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Back to login
                    TextButton(
                        onClick = onNavigateBack
                    ) {
                        Text(
                            text = "Back to Login",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Security notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛡️ Security Notice",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your PIN protects parental settings and spending controls",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}