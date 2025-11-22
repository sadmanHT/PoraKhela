package com.porakhela.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.porakhela.ui.screens.auth.*
import com.porakhela.ui.screens.dashboard.*
import com.porakhela.ui.screens.learning.*

/**
 * Main navigation component for Porakhela
 * Handles all screen navigation with child-friendly transitions
 */
@Composable
fun PorakhelaNavigation(
    isLoggedIn: Boolean,
    onLoginStateChange: (Boolean) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) PorakhelaRoutes.DASHBOARD else PorakhelaRoutes.SPLASH
    ) {
        // Auth screens
        composable(PorakhelaRoutes.SPLASH) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(PorakhelaRoutes.ONBOARDING) {
                        popUpTo(PorakhelaRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(PorakhelaRoutes.LOGIN) {
                        popUpTo(PorakhelaRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(PorakhelaRoutes.ONBOARDING) {
            OnboardingScreen(
                onNavigateToLogin = {
                    navController.navigate(PorakhelaRoutes.LOGIN) {
                        popUpTo(PorakhelaRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(PorakhelaRoutes.LOGIN) {
            LoginScreen(
                onNavigateToOtp = { phoneNumber ->
                    navController.navigate("${PorakhelaRoutes.OTP_VERIFICATION}/$phoneNumber")
                },
                onNavigateToRegister = {
                    navController.navigate(PorakhelaRoutes.REGISTER)
                },
                onNavigateToParentalPin = {
                    navController.navigate(PorakhelaRoutes.PARENTAL_PIN)
                }
            )
        }

        composable("${PorakhelaRoutes.OTP_VERIFICATION}/{phoneNumber}") { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToHome = {
                    onLoginStateChange(true)
                    navController.navigate(PorakhelaRoutes.DASHBOARD) {
                        popUpTo(PorakhelaRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(PorakhelaRoutes.PARENTAL_PIN) {
            ParentalPinScreen(
                onNavigateToParentalDashboard = {
                    navController.navigate(PorakhelaRoutes.PARENTAL_DASHBOARD)
                },
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }

        // Main app screens
        composable(PorakhelaRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToSubject = { subjectId ->
                    navController.navigate("${PorakhelaRoutes.SUBJECT_DETAIL}/$subjectId")
                },
                onNavigateToProfile = {
                    navController.navigate(PorakhelaRoutes.PROFILE)
                },
                onNavigateToLeaderboard = {
                    navController.navigate(PorakhelaRoutes.LEADERBOARD)
                },
                onNavigateToSettings = {
                    navController.navigate(PorakhelaRoutes.SETTINGS)
                },
                onNavigateToAchievements = {
                    navController.navigate("achievements")
                },
                onNavigateToLesson = { lessonId ->
                    navController.navigate("${PorakhelaRoutes.LESSON_PLAYER}/$lessonId")
                }
            )
        }

        composable("${PorakhelaRoutes.SUBJECT_DETAIL}/{subjectId}") { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            SubjectDetailScreen(
                subjectId = subjectId,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onNavigateToLesson = { lessonId ->
                    navController.navigate("${PorakhelaRoutes.LESSON_PLAYER}/$lessonId")
                }
            )
        }

        composable("${PorakhelaRoutes.LESSON_PLAYER}/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId") ?: ""
            LessonPlayerScreen(
                lessonId = lessonId,
                onStartExercises = {
                    navController.navigate("${PorakhelaRoutes.QUIZ}/$lessonId")
                },
                onLessonComplete = {
                    navController.navigate(PorakhelaRoutes.DASHBOARD) {
                        popUpTo("${PorakhelaRoutes.LESSON_PLAYER}/{lessonId}") { inclusive = true }
                    }
                },
                onBack = {
                    navController.navigateUp()
                }
            )
        }

        composable("${PorakhelaRoutes.QUIZ}/{quizId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId") ?: ""
            QuizScreen(
                quizId = quizId,
                onNavigateBack = {
                    navController.navigateUp()
                },
                onQuizComplete = { score, points ->
                    // TODO: Handle quiz completion
                    navController.navigateUp()
                }
            )
        }

        // Placeholder screens for other routes
        composable(PorakhelaRoutes.REGISTER) {
            // TODO: Implement RegisterScreen
        }
        
        composable(PorakhelaRoutes.PARENTAL_DASHBOARD) {
            ParentDashboardScreen(
                onNavigateToChildDashboard = {
                    navController.navigate(PorakhelaRoutes.DASHBOARD) {
                        popUpTo(PorakhelaRoutes.PARENTAL_DASHBOARD) { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(PorakhelaRoutes.SETTINGS)
                },
                onLogout = {
                    onLoginStateChange(false)
                    navController.navigate(PorakhelaRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(PorakhelaRoutes.PROFILE) {
            ProfileManagementScreen(
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        composable("achievements") {
            AchievementScreen(
                onBack = {
                    navController.navigateUp()
                }
            )
        }
        
        composable(PorakhelaRoutes.LEADERBOARD) {
            // TODO: Implement LeaderboardScreen
        }
        
        composable(PorakhelaRoutes.SETTINGS) {
            SettingsScreen(
                onBack = {
                    navController.navigateUp()
                },
                onLogout = {
                    onLoginStateChange(false)
                    navController.navigate(PorakhelaRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}