package com.porakhela.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.porakhela.data.repository.AchievementRepository
import com.porakhela.data.repository.ProgressRepository
import com.porakhela.data.local.entity.Achievement
import javax.inject.Inject

/**
 * ViewModel for Achievement Screen
 * Manages achievement data, progress tracking, and celebration animations
 */
@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementUiState())
    val uiState: StateFlow<AchievementUiState> = _uiState.asStateFlow()

    fun loadAchievements() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load all achievements
                val achievements = achievementRepository.getAllAchievements()
                
                // Get user progress data
                val userProgress = progressRepository.getUserProgress()
                
                // Calculate completion rate
                val earnedCount = achievements.count { it.isEarned }
                val completionRate = if (achievements.isNotEmpty()) {
                    earnedCount.toFloat() / achievements.size
                } else 0f
                
                _uiState.value = _uiState.value.copy(
                    achievements = achievements,
                    totalPoints = userProgress.totalPoints,
                    currentLevel = calculateLevel(userProgress.totalPoints),
                    nextLevelProgress = calculateNextLevelProgress(userProgress.totalPoints),
                    completionRate = completionRate,
                    isLoading = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load achievements: ${e.message}"
                )
            }
        }
    }

    fun selectAchievement(achievement: Achievement) {
        _uiState.value = _uiState.value.copy(
            selectedAchievement = achievement,
            showCelebration = achievement.isEarned
        )
    }

    fun hideCelebration() {
        _uiState.value = _uiState.value.copy(
            showCelebration = false,
            selectedAchievement = null
        )
    }

    /**
     * Check for newly earned achievements based on recent activity
     */
    fun checkNewAchievements(
        lessonsCompleted: Int = 0,
        perfectScores: Int = 0,
        streakDays: Int = 0,
        subjectMastery: Map<String, Boolean> = emptyMap()
    ) {
        viewModelScope.launch {
            val newAchievements = mutableListOf<Achievement>()
            
            // Check lesson completion milestones
            when (lessonsCompleted) {
                1 -> newAchievements.add(createAchievement("first_lesson", "First Lesson Complete!", "You completed your first lesson", 100))
                5 -> newAchievements.add(createAchievement("5_lessons", "Learning Star!", "5 lessons completed", 250))
                10 -> newAchievements.add(createAchievement("10_lessons", "Knowledge Seeker!", "10 lessons completed", 500))
                25 -> newAchievements.add(createAchievement("25_lessons", "Super Learner!", "25 lessons completed", 1000))
                50 -> newAchievements.add(createAchievement("50_lessons", "Learning Champion!", "50 lessons completed", 2500))
            }
            
            // Check perfect score achievements
            when (perfectScores) {
                1 -> newAchievements.add(createAchievement("first_perfect", "Perfect Score!", "Got 100% on an exercise", 150))
                5 -> newAchievements.add(createAchievement("5_perfect", "Accuracy Expert!", "5 perfect scores", 500))
                10 -> newAchievements.add(createAchievement("10_perfect", "Perfection Master!", "10 perfect scores", 1000))
            }
            
            // Check streak achievements
            when (streakDays) {
                3 -> newAchievements.add(createAchievement("streak_3", "Consistent Learner!", "3-day learning streak", 200))
                7 -> newAchievements.add(createAchievement("streak_7", "Week Warrior!", "7-day learning streak", 500))
                14 -> newAchievements.add(createAchievement("streak_14", "Two-Week Champion!", "14-day learning streak", 1000))
                30 -> newAchievements.add(createAchievement("streak_30", "Monthly Master!", "30-day learning streak", 2500))
            }
            
            // Check subject mastery achievements
            subjectMastery.forEach { (subject, isMastered) ->
                if (isMastered) {
                    when (subject) {
                        "mathematics" -> newAchievements.add(createAchievement("math_master", "Math Master!", "Mastered mathematics", 1500))
                        "science" -> newAchievements.add(createAchievement("science_explorer", "Science Explorer!", "Mastered science", 1500))
                        "bangla" -> newAchievements.add(createAchievement("bangla_scholar", "Bangla Scholar!", "Mastered Bangla", 1500))
                        "english" -> newAchievements.add(createAchievement("english_expert", "English Expert!", "Mastered English", 1500))
                    }
                }
            }
            
            // Save new achievements
            if (newAchievements.isNotEmpty()) {
                achievementRepository.saveAchievements(newAchievements)
                
                // Show celebration for the most recent achievement
                val latestAchievement = newAchievements.last()
                _uiState.value = _uiState.value.copy(
                    selectedAchievement = latestAchievement,
                    showCelebration = true
                )
                
                // Auto-hide celebration after 5 seconds
                delay(5000)
                hideCelebration()
                
                // Reload achievements to reflect changes
                loadAchievements()
            }
        }
    }

    private fun createAchievement(
        id: String,
        title: String,
        description: String,
        points: Int
    ): Achievement {
        return Achievement(
            id = id,
            title = title,
            description = description,
            points = points,
            type = id,
            isEarned = true,
            earnedDate = System.currentTimeMillis(),
            iconUrl = null
        )
    }

    private fun calculateLevel(totalPoints: Int): Int {
        return when {
            totalPoints < 500 -> 1
            totalPoints < 1500 -> 2
            totalPoints < 3000 -> 3
            totalPoints < 5000 -> 4
            totalPoints < 8000 -> 5
            totalPoints < 12000 -> 6
            totalPoints < 17000 -> 7
            totalPoints < 23000 -> 8
            totalPoints < 30000 -> 9
            else -> 10
        }
    }

    private fun calculateNextLevelProgress(totalPoints: Int): Float {
        val currentLevel = calculateLevel(totalPoints)
        val currentLevelThreshold = getLevelThreshold(currentLevel)
        val nextLevelThreshold = getLevelThreshold(currentLevel + 1)
        
        if (currentLevel >= 10) return 1f
        
        val progressInCurrentLevel = totalPoints - currentLevelThreshold
        val pointsNeededForNextLevel = nextLevelThreshold - currentLevelThreshold
        
        return (progressInCurrentLevel.toFloat() / pointsNeededForNextLevel).coerceIn(0f, 1f)
    }

    private fun getLevelThreshold(level: Int): Int {
        return when (level) {
            1 -> 0
            2 -> 500
            3 -> 1500
            4 -> 3000
            5 -> 5000
            6 -> 8000
            7 -> 12000
            8 -> 17000
            9 -> 23000
            10 -> 30000
            else -> 30000
        }
    }

    /**
     * Generate achievement suggestions based on current progress
     */
    fun getAchievementSuggestions(): List<String> {
        val currentState = _uiState.value
        val suggestions = mutableListOf<String>()
        
        val unearned = currentState.achievements.filter { !it.isEarned }
        
        // Suggest closest achievements
        unearned.take(3).forEach { achievement ->
            when (achievement.type) {
                "streak_7" -> suggestions.add("Learn for 7 days in a row to earn '${achievement.title}'")
                "perfect_score" -> suggestions.add("Get 100% on any exercise to earn '${achievement.title}'")
                "math_master" -> suggestions.add("Complete all math lessons to earn '${achievement.title}'")
                else -> suggestions.add("Work towards '${achievement.title}' - ${achievement.description}")
            }
        }
        
        return suggestions
    }
}

/**
 * UI State for Achievement Screen
 */
data class AchievementUiState(
    val achievements: List<Achievement> = emptyList(),
    val totalPoints: Int = 0,
    val currentLevel: Int = 1,
    val nextLevelProgress: Float = 0f,
    val completionRate: Float = 0f,
    val selectedAchievement: Achievement? = null,
    val showCelebration: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)