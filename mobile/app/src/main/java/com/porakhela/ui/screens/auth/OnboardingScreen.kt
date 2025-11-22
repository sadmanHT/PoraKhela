package com.porakhela.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.launch
import com.porakhela.ui.components.ChildFriendlyButton
import com.porakhela.ui.components.GradientBackground
import com.porakhela.ui.theme.*

/**
 * Child-friendly onboarding with engaging animations and large touch targets
 */
@Composable
fun OnboardingScreen(
    onNavigateToLogin: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Animation for page transitions
    val infiniteTransition = rememberInfiniteTransition()
    val animationScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    GradientBackground(
        startColor = onboardingPages[pagerState.currentPage].backgroundColor,
        endColor = onboardingPages[pagerState.currentPage].backgroundColor.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Top navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (hidden on first page)
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
                
                // Page indicator
                Text(
                    text = "${pagerState.currentPage + 1} of ${onboardingPages.size}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                )
                
                // Skip button
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToLogin()
                    }
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { onboardingPages[it].title }
            ) { page ->
                OnboardingPage(
                    page = onboardingPages[page],
                    animationScale = animationScale,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Page dots indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotSize by animateDpAsState(
                        targetValue = if (isSelected) 16.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .padding(horizontal = 4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = if (pagerState.currentPage == onboardingPages.size - 1) {
                    Arrangement.Center
                } else {
                    Arrangement.SpaceBetween
                }
            ) {
                val isLastPage = pagerState.currentPage == onboardingPages.size - 1
                
                if (!isLastPage) {
                    // Previous button (if not first page)
                    if (pagerState.currentPage > 0) {
                        ChildFriendlyButton(
                            text = "Previous",
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            icon = Icons.Default.ArrowBack
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    // Next button
                    ChildFriendlyButton(
                        text = "Next",
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color.White,
                        contentColor = onboardingPages[pagerState.currentPage].backgroundColor,
                        icon = Icons.Default.ArrowForward
                    )
                } else {
                    // Get Started button
                    ChildFriendlyButton(
                        text = "🚀 Let's Start Learning!",
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color.White,
                        contentColor = onboardingPages[pagerState.currentPage].backgroundColor,
                        elevation = 12.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageData,
    animationScale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Icon
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(animationScale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Title
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 32.sp,
                lineHeight = 40.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Bengali title
        Text(
            text = page.titleBn,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 20.sp
            ),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

private data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val titleBn: String,
    val description: String,
    val backgroundColor: Color
)

private val onboardingPages = listOf(
    OnboardingPageData(
        icon = Icons.Default.School,
        title = "Learn with Fun!",
        titleBn = "মজার সাথে শিখুন!",
        description = "📚 Discover exciting lessons from your NCTB curriculum with videos, games, and interactive content that makes learning an adventure!",
        backgroundColor = FunBlue
    ),
    OnboardingPageData(
        icon = Icons.Default.Star,
        title = "Earn Porapoints!",
        titleBn = "পরাপয়েন্ট অর্জন করুন!",
        description = "⭐ Complete lessons and quizzes to earn points. The more you learn, the more you earn! Every achievement brings you closer to amazing rewards.",
        backgroundColor = FunOrange
    ),
    OnboardingPageData(
        icon = Icons.Default.Redeem,
        title = "Redeem Real Rewards!",
        titleBn = "সত্যিকারের পুরস্কার জিতুন!",
        description = "🎁 Use your Porapoints to get mobile data, talk time, and other exciting rewards from Banglalink. Learning pays off!",
        backgroundColor = FunGreen
    ),
    OnboardingPageData(
        icon = Icons.Default.EmojiEvents,
        title = "Become a Champion!",
        titleBn = "চ্যাম্পিয়ন হয়ে উঠুন!",
        description = "🏆 Unlock achievements, maintain learning streaks, and compete with friends on the leaderboard. Show everyone how smart you are!",
        backgroundColor = FunPink
    ),
    OnboardingPageData(
        icon = Icons.Default.Family,
        title = "Parents Stay Connected!",
        titleBn = "বাবা-মা থাকুন সংযুক্ত!",
        description = "👨‍👩‍👧‍👦 Parents can monitor progress, approve rewards, and support their child's learning journey even from basic phones using USSD!",
        backgroundColor = FunPurple
    )
)