package com.porakhela.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.porakhela.data.repository.LessonRepository
import com.porakhela.data.repository.ProgressRepository
import com.porakhela.data.local.entity.Exercise
import javax.inject.Inject

/**
 * ViewModel for Exercise Screen
 * Manages exercise questions, answers, scoring, and progress tracking
 */
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private var exerciseStartTime = 0L
    private var currentQuestionStartTime = 0L

    fun loadExercises(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val exercises = lessonRepository.getExercises(lessonId)
                exerciseStartTime = System.currentTimeMillis()
                currentQuestionStartTime = exerciseStartTime
                
                _uiState.value = _uiState.value.copy(
                    exercises = exercises,
                    isLoading = false,
                    currentExerciseIndex = 0,
                    score = 0,
                    correctAnswers = 0
                )
                
                // Track exercise session start
                progressRepository.trackExerciseStart(lessonId, exercises.size)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load exercises: ${e.message}"
                )
            }
        }
    }

    fun selectAnswer(answerIndex: Int) {
        val currentState = _uiState.value
        if (currentState.showFeedback) return
        
        _uiState.value = currentState.copy(
            selectedAnswer = answerIndex
        )
    }

    fun submitAnswer() {
        val currentState = _uiState.value
        val selectedAnswer = currentState.selectedAnswer ?: return
        val currentExercise = currentState.exercises.getOrNull(currentState.currentExerciseIndex) ?: return
        
        val isCorrect = selectedAnswer == currentExercise.correctAnswer
        val pointsEarned = if (isCorrect) 50 else 10
        val questionTime = System.currentTimeMillis() - currentQuestionStartTime
        
        _uiState.value = currentState.copy(
            showFeedback = true,
            lastAnswerCorrect = isCorrect,
            score = currentState.score + pointsEarned,
            correctAnswers = currentState.correctAnswers + if (isCorrect) 1 else 0,
            showCelebration = true
        )
        
        // Track answer submission
        viewModelScope.launch {
            progressRepository.trackExerciseAnswer(
                lessonId = currentExercise.lessonId,
                exerciseId = currentExercise.id,
                selectedAnswer = selectedAnswer,
                correctAnswer = currentExercise.correctAnswer,
                isCorrect = isCorrect,
                timeSpent = questionTime,
                pointsEarned = pointsEarned
            )
            
            // Award points
            progressRepository.awardPoints(pointsEarned, "Exercise answer")
            
            // Hide celebration after delay
            delay(2000)
            _uiState.value = _uiState.value.copy(showCelebration = false)
        }
    }

    fun nextQuestion() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentExerciseIndex + 1
        
        if (nextIndex < currentState.exercises.size) {
            // Move to next question
            _uiState.value = currentState.copy(
                currentExerciseIndex = nextIndex,
                selectedAnswer = null,
                showFeedback = false,
                lastAnswerCorrect = false
            )
            currentQuestionStartTime = System.currentTimeMillis()
        } else {
            // All questions completed - show results
            val totalTimeSpent = System.currentTimeMillis() - exerciseStartTime
            _uiState.value = currentState.copy(
                showResults = true,
                totalTimeSpent = totalTimeSpent,
                finalScore = currentState.score
            )
            
            // Track exercise completion
            viewModelScope.launch {
                progressRepository.completeExerciseSession(
                    lessonId = currentState.exercises.first().lessonId,
                    totalQuestions = currentState.exercises.size,
                    correctAnswers = currentState.correctAnswers,
                    totalScore = currentState.score,
                    timeSpent = totalTimeSpent
                )
                
                // Check for achievements
                checkExerciseAchievements(currentState)
            }
        }
    }

    fun completeExercises() {
        _uiState.value = _uiState.value.copy(isCompleted = true)
    }

    private suspend fun checkExerciseAchievements(state: ExerciseUiState) {
        val exercises = state.exercises
        val score = state.score
        val correctAnswers = state.correctAnswers
        val accuracy = if (exercises.isNotEmpty()) {
            (correctAnswers.toFloat() / exercises.size) * 100
        } else 0f
        
        // Check for perfect score achievement
        if (accuracy == 100f && exercises.isNotEmpty()) {
            progressRepository.unlockAchievement("perfect_score", "Perfect Score!", 200)
        }
        
        // Check for high score achievement
        if (accuracy >= 90f && exercises.size >= 5) {
            progressRepository.unlockAchievement("high_scorer", "High Scorer!", 150)
        }
        
        // Check for fast completion achievement
        val avgTimePerQuestion = state.totalTimeSpent / exercises.size
        if (avgTimePerQuestion < 30000 && accuracy >= 80f) { // Less than 30 seconds per question with good accuracy
            progressRepository.unlockAchievement("speed_demon", "Speed Demon!", 300)
        }
        
        // Check for improvement achievement
        val previousBestScore = progressRepository.getBestExerciseScore(exercises.first().lessonId)
        if (score > previousBestScore) {
            progressRepository.unlockAchievement("improver", "Getting Better!", 100)
        }
    }

    fun retryExercises() {
        val currentState = _uiState.value
        if (currentState.exercises.isNotEmpty()) {
            _uiState.value = ExerciseUiState(
                exercises = currentState.exercises,
                currentExerciseIndex = 0,
                score = 0,
                correctAnswers = 0
            )
            
            exerciseStartTime = System.currentTimeMillis()
            currentQuestionStartTime = exerciseStartTime
        }
    }

    fun skipQuestion() {
        val currentState = _uiState.value
        
        // Mark as incorrect and move to next
        _uiState.value = currentState.copy(
            showFeedback = true,
            lastAnswerCorrect = false,
            score = currentState.score + 5, // Small participation points
            showCelebration = false
        )
        
        viewModelScope.launch {
            val currentExercise = currentState.exercises.getOrNull(currentState.currentExerciseIndex)
            currentExercise?.let { exercise ->
                progressRepository.trackExerciseAnswer(
                    lessonId = exercise.lessonId,
                    exerciseId = exercise.id,
                    selectedAnswer = -1, // Indicate skipped
                    correctAnswer = exercise.correctAnswer,
                    isCorrect = false,
                    timeSpent = System.currentTimeMillis() - currentQuestionStartTime,
                    pointsEarned = 5
                )
            }
            
            delay(1500)
            nextQuestion()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Track exercise exit if not completed
        viewModelScope.launch {
            val currentState = _uiState.value
            if (!currentState.isCompleted && currentState.exercises.isNotEmpty()) {
                progressRepository.trackExerciseExit(
                    lessonId = currentState.exercises.first().lessonId,
                    questionsAttempted = currentState.currentExerciseIndex + 1,
                    timeSpent = System.currentTimeMillis() - exerciseStartTime
                )
            }
        }
    }
}

/**
 * UI State for Exercise Screen
 */
data class ExerciseUiState(
    val exercises: List<Exercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val selectedAnswer: Int? = null,
    val showFeedback: Boolean = false,
    val lastAnswerCorrect: Boolean = false,
    val score: Int = 0,
    val correctAnswers: Int = 0,
    val showResults: Boolean = false,
    val totalTimeSpent: Long = 0L,
    val finalScore: Int = 0,
    val isCompleted: Boolean = false,
    val showCelebration: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)