package com.porakhela.ui.navigation

/**
 * Navigation routes for Porakhela app
 * Defines all screen destinations
 */
object PorakhelaRoutes {
    // Authentication Flow
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val PHONE_INPUT = "phone_input"
    const val OTP_VERIFICATION = "otp_verification"
    const val USER_TYPE_SELECTION = "user_type_selection"
    const val PROFILE_SETUP = "profile_setup"
    
    // Parental Controls
    const val PARENTAL_PIN_SETUP = "parental_pin_setup"
    const val PARENTAL_PIN_VERIFY = "parental_pin_verify"
    const val CHILD_PROFILE_SETUP = "child_profile_setup"
    
    // Main App Flow
    const val CHILD_DASHBOARD = "child_dashboard"
    const val PARENT_DASHBOARD = "parent_dashboard"
    
    // Learning Flow
    const val SUBJECTS = "subjects"
    const val CHAPTERS = "chapters/{subjectId}"
    const val LESSONS = "lessons/{chapterId}"
    const val LESSON_PLAYER = "lesson_player/{lessonId}"
    const val QUIZ = "quiz/{lessonId}"
    
    // Gamification
    const val ACHIEVEMENTS = "achievements"
    const val LEADERBOARD = "leaderboard"
    const val REWARDS = "rewards"
    const val POINTS_HISTORY = "points_history"
    
    // Profile & Settings
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val PARENTAL_CONTROLS = "parental_controls"
    
    // Helper functions for parameterized routes
    fun chapters(subjectId: String) = "chapters/$subjectId"
    fun lessons(chapterId: String) = "lessons/$chapterId"
    fun lessonPlayer(lessonId: String) = "lesson_player/$lessonId"
    fun quiz(lessonId: String) = "quiz/$lessonId"
}