package com.porakhela.ui.screens.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porakhela.data.model.*
import com.porakhela.data.repository.OfflineLessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Enhanced Offline Lesson Player
 * Manages offline lesson state, progress tracking, and local gamification
 */
@HiltViewModel
class OfflineLessonPlayerViewModel @Inject constructor(
    private val offlineLessonRepository: OfflineLessonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineLessonPlayerUiState())
    val uiState: StateFlow<OfflineLessonPlayerUiState> = _uiState.asStateFlow()

    private var currentProgressId: String? = null
    private var questionStartTimes = mutableMapOf<String, Long>()

    /**
     * Load offline lesson and initialize player state
     */
    fun loadOfflineLesson(lessonId: String, childProfileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Load lesson with questions
                val lessonWithQuestions = offlineLessonRepository.getOfflineLessonWithQuestions(lessonId)
                
                if (lessonWithQuestions == null) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Lesson not found. Please download this lesson pack first."
                        )
                    }
                    return@launch
                }

                // Start lesson progress
                currentProgressId = offlineLessonRepository.startOfflineLesson(childProfileId, lessonId)

                // Load local points and achievements
                val localPoints = offlineLessonRepository.getLocalPoints(childProfileId).first()
                val pendingEvents = offlineLessonRepository.getPendingGamificationEvents(childProfileId).first()

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        lesson = lessonWithQuestions.lesson,
                        questions = lessonWithQuestions.questions,
                        currentQuestionIndex = 0,
                        currentQuestion = lessonWithQuestions.questions.firstOrNull(),
                        maxScore = lessonWithQuestions.questions.sumOf { it.points },
                        totalPointsBefore = localPoints?.totalPoints ?: 0,
                        currentStreak = localPoints?.currentStreak ?: 0,
                        pendingEvents = pendingEvents,
                        lessonStartTime = System.currentTimeMillis()
                    )
                }

                // Start timer for first question
                startQuestionTimer()

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Failed to load lesson: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Submit answer for current question
     */
    fun submitAnswer(questionId: String, selectedAnswer: Int, timeSpent: Long) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentQuestion = currentState.currentQuestion ?: return@launch
            val progressId = currentProgressId ?: return@launch

            try {
                // Submit answer to repository
                val result = offlineLessonRepository.submitOfflineAnswer(
                    progressId = progressId,
                    questionId = questionId,
                    selectedAnswer = selectedAnswer,
                    timeSpent = timeSpent
                )

                // Update UI state with result
                _uiState.update { state ->
                    val newQuestionResults = state.questionResults + OfflineQuestionResult(
                        questionId = questionId,
                        selectedAnswer = selectedAnswer,
                        isCorrect = result.isCorrect,
                        timeSpent = timeSpent,
                        attempts = 1,
                        answeredAt = System.currentTimeMillis()
                    )

                    val newScore = if (result.isCorrect) state.score + result.pointsEarned else state.score
                    val newCorrectAnswers = if (result.isCorrect) state.correctAnswers + 1 else state.correctAnswers
                    val newAnsweredQuestions = state.answeredQuestions + 1
                    val newTimeSpent = state.timeSpent + timeSpent

                    // Calculate streak
                    val newStreak = if (result.isCorrect) state.currentStreak + 1 else 0

                    state.copy(
                        selectedAnswer = selectedAnswer,
                        lastAnswerCorrect = result.isCorrect,
                        lastPointsEarned = result.pointsEarned,
                        showAnswerResult = true,
                        isCurrentQuestionAnswered = true,
                        questionResults = newQuestionResults,
                        score = newScore,
                        correctAnswers = newCorrectAnswers,
                        answeredQuestions = newAnsweredQuestions,
                        timeSpent = newTimeSpent,
                        currentStreak = newStreak,
                        averageTimePerQuestion = if (newAnsweredQuestions > 0) newTimeSpent / newAnsweredQuestions else 0L,
                        showPointsAnimation = result.isCorrect && result.pointsEarned > 0
                    )
                }

                // Trigger haptic feedback and show points animation
                if (result.isCorrect) {
                    showPointsEarnedAnimation(result.pointsEarned)
                }

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to submit answer: ${e.message}")
                }
            }
        }
    }

    /**
     * Navigate to next question
     */
    fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentQuestionIndex + 1
        
        if (nextIndex < currentState.questions.size) {
            _uiState.update { state ->
                state.copy(
                    currentQuestionIndex = nextIndex,
                    currentQuestion = state.questions[nextIndex],
                    selectedAnswer = null,
                    lastAnswerCorrect = null,
                    showAnswerResult = false,
                    isCurrentQuestionAnswered = false,
                    showPointsAnimation = false
                )
            }
            startQuestionTimer()
        }
    }

    /**
     * Navigate to previous question
     */
    fun previousQuestion() {
        val currentState = _uiState.value
        val prevIndex = currentState.currentQuestionIndex - 1
        
        if (prevIndex >= 0) {
            val previousQuestionResult = currentState.questionResults.find { 
                it.questionId == currentState.questions[prevIndex].id 
            }
            
            _uiState.update { state ->
                state.copy(
                    currentQuestionIndex = prevIndex,
                    currentQuestion = state.questions[prevIndex],
                    selectedAnswer = previousQuestionResult?.selectedAnswer,
                    lastAnswerCorrect = previousQuestionResult?.isCorrect,
                    showAnswerResult = previousQuestionResult != null,
                    isCurrentQuestionAnswered = previousQuestionResult != null,
                    showPointsAnimation = false
                )
            }
        }
    }

    /**
     * Complete the lesson
     */
    fun completeLesson() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val progressId = currentProgressId ?: return@launch

            try {
                // Complete lesson in repository
                val completionResult = offlineLessonRepository.completeOfflineLesson(progressId)

                // Calculate final stats
                val totalTimeSpent = System.currentTimeMillis() - currentState.lessonStartTime
                val finalScore = completionResult.score
                val accuracy = if (currentState.maxScore > 0) 
                    (finalScore * 100) / currentState.maxScore else 0

                // Check if this is a new personal best
                val isNewBest = completionResult.isNewBest

                // Update UI state
                _uiState.update { state ->
                    state.copy(
                        isCompleted = true,
                        score = finalScore,
                        timeSpent = totalTimeSpent,
                        totalPointsEarned = completionResult.pointsEarned,
                        newAchievements = completionResult.newAchievements,
                        isNewPersonalBest = isNewBest,
                        finalAccuracy = accuracy
                    )
                }

                // Create completion gamification events
                createCompletionEvents(completionResult)

            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to complete lesson: ${e.message}")
                }
            }
        }
    }

    /**
     * Mark gamification event as shown
     */
    fun markEventShown(eventId: String) {
        viewModelScope.launch {
            offlineLessonRepository.markEventShown(eventId)
            
            // Remove from pending events
            _uiState.update { state ->
                state.copy(
                    pendingEvents = state.pendingEvents.filter { it.id != eventId }
                )
            }
        }
    }

    /**
     * Retry loading lesson
     */
    fun retryLoadLesson(lessonId: String, childProfileId: String) {
        loadOfflineLesson(lessonId, childProfileId)
    }

    // Private helper methods

    private fun startQuestionTimer() {
        val currentQuestion = _uiState.value.currentQuestion ?: return
        questionStartTimes[currentQuestion.id] = System.currentTimeMillis()
    }

    private fun showPointsEarnedAnimation(points: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(showPointsAnimation = true) }
            
            kotlinx.coroutines.delay(2000) // Show animation for 2 seconds
            
            _uiState.update { it.copy(showPointsAnimation = false) }
        }
    }

    private fun createCompletionEvents(result: LessonCompletionResult) {
        viewModelScope.launch {
            // Points earned event
            if (result.pointsEarned > 0) {
                // Event creation is handled in the repository
            }

            // Achievement events
            result.newAchievements.forEach { achievement ->
                // Achievement events are created in the repository
            }

            // Reload pending events to show new ones
            val childProfileId = getCurrentChildProfileId() ?: return@launch
            val newPendingEvents = offlineLessonRepository.getPendingGamificationEvents(childProfileId).first()
            
            _uiState.update { it.copy(pendingEvents = newPendingEvents) }
        }
    }

    private fun getCurrentChildProfileId(): String? {
        // This would be extracted from the current progress or stored separately
        return currentProgressId?.let { 
            // Extract child profile ID from progress ID or store separately
            // For now, return null as placeholder
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        questionStartTimes.clear()
    }
}

/**
 * UI State for Offline Lesson Player
 */
data class OfflineLessonPlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lesson: OfflineLesson? = null,
    val questions: List<OfflineQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val currentQuestion: OfflineQuestion? = null,
    val selectedAnswer: Int? = null,
    val lastAnswerCorrect: Boolean? = null,
    val lastPointsEarned: Int = 0,
    val showAnswerResult: Boolean = false,
    val isCurrentQuestionAnswered: Boolean = false,
    val questionResults: List<OfflineQuestionResult> = emptyList(),
    
    // Scoring and progress
    val score: Int = 0,
    val maxScore: Int = 0,
    val correctAnswers: Int = 0,
    val answeredQuestions: Int = 0,
    val timeSpent: Long = 0L,
    val averageTimePerQuestion: Long = 0L,
    val lessonStartTime: Long = 0L,
    
    // Gamification
    val currentStreak: Int = 0,
    val totalPointsBefore: Int = 0,
    val totalPointsEarned: Int = 0,
    val showPointsAnimation: Boolean = false,
    val pendingEvents: List<GamificationEvent> = emptyList(),
    val newAchievements: List<LocalAchievement> = emptyList(),
    
    // Completion state
    val isCompleted: Boolean = false,
    val isNewPersonalBest: Boolean = false,
    val finalAccuracy: Int = 0,
    
    // Offline state
    val pendingSyncItems: Int = 0,
    val isOfflineMode: Boolean = true
)