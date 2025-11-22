package com.porakhela.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.porakhela.data.repository.LessonRepository
import com.porakhela.data.repository.ProgressRepository
import com.porakhela.ui.screens.lessons.LessonContent
import com.porakhela.ui.screens.lessons.InteractiveElement
import javax.inject.Inject

/**
 * ViewModel for Lesson Player Screen
 * Manages lesson content, playback, progress tracking, and user interactions
 */
@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonPlayerUiState())
    val uiState: StateFlow<LessonPlayerUiState> = _uiState.asStateFlow()

    private var lessonStartTime = 0L
    private var currentSectionStartTime = 0L

    fun loadLesson(lessonId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val lesson = lessonRepository.getLessonContent(lessonId)
                lessonStartTime = System.currentTimeMillis()
                currentSectionStartTime = lessonStartTime
                
                _uiState.value = _uiState.value.copy(
                    lesson = lesson,
                    isLoading = false,
                    currentSection = 0,
                    progress = 0f
                )
                
                // Track lesson start
                progressRepository.trackLessonStart(lessonId)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load lesson: ${e.message}"
                )
            }
        }
    }

    fun togglePlayback() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isPlaying = !currentState.isPlaying
        )
        
        // Track play/pause events
        viewModelScope.launch {
            if (currentState.isPlaying) {
                progressRepository.trackLessonPause(
                    lessonId = currentState.lesson?.id ?: "",
                    timeSpent = System.currentTimeMillis() - currentSectionStartTime
                )
            } else {
                currentSectionStartTime = System.currentTimeMillis()
                progressRepository.trackLessonResume(currentState.lesson?.id ?: "")
            }
        }
    }

    fun nextSection() {
        val currentState = _uiState.value
        val lesson = currentState.lesson ?: return
        
        if (currentState.currentSection < lesson.totalSections - 1) {
            val newSection = currentState.currentSection + 1
            val newProgress = (newSection + 1).toFloat() / lesson.totalSections
            
            _uiState.value = currentState.copy(
                currentSection = newSection,
                progress = newProgress
            )
            
            // Track section completion
            viewModelScope.launch {
                progressRepository.trackSectionComplete(
                    lessonId = lesson.id,
                    sectionIndex = currentState.currentSection,
                    timeSpent = System.currentTimeMillis() - currentSectionStartTime
                )
                currentSectionStartTime = System.currentTimeMillis()
                
                // Check if lesson is completed
                if (newSection == lesson.totalSections - 1) {
                    markLessonComplete()
                }
            }
        }
    }

    fun previousSection() {
        val currentState = _uiState.value
        val lesson = currentState.lesson ?: return
        
        if (currentState.currentSection > 0) {
            val newSection = currentState.currentSection - 1
            val newProgress = (newSection + 1).toFloat() / lesson.totalSections
            
            _uiState.value = currentState.copy(
                currentSection = newSection,
                progress = newProgress
            )
            
            currentSectionStartTime = System.currentTimeMillis()
        }
    }

    fun markSectionComplete() {
        val currentState = _uiState.value
        val lesson = currentState.lesson ?: return
        
        viewModelScope.launch {
            progressRepository.trackSectionComplete(
                lessonId = lesson.id,
                sectionIndex = currentState.currentSection,
                timeSpent = System.currentTimeMillis() - currentSectionStartTime
            )
            
            // Award points for section completion
            val pointsEarned = calculateSectionPoints(currentState.currentSection)
            progressRepository.awardPoints(pointsEarned, "Section ${currentState.currentSection + 1} completed")
            
            // Update lesson with new points
            val updatedLesson = lesson.copy(
                pointsEarned = lesson.pointsEarned + pointsEarned
            )
            _uiState.value = currentState.copy(lesson = updatedLesson)
            
            // Show mini achievement animation
            showMiniAchievement("+$pointsEarned points!")
        }
    }

    private fun markLessonComplete() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val lesson = currentState.lesson ?: return@launch
            
            val totalTimeSpent = System.currentTimeMillis() - lessonStartTime
            
            // Mark lesson as completed
            progressRepository.completeLessonProgress(
                lessonId = lesson.id,
                totalTimeSpent = totalTimeSpent,
                pointsEarned = lesson.pointsEarned
            )
            
            // Check for achievements
            val achievements = progressRepository.checkLessonAchievements(lesson.id)
            if (achievements.isNotEmpty()) {
                _uiState.value = currentState.copy(
                    showAchievementAnimation = true,
                    newAchievements = achievements
                )
            }
            
            // Mark as completed and trigger navigation
            delay(1500) // Allow time for celebration
            _uiState.value = currentState.copy(isCompleted = true)
        }
    }

    fun hideAchievementAnimation() {
        _uiState.value = _uiState.value.copy(
            showAchievementAnimation = false,
            newAchievements = emptyList()
        )
    }

    private fun showMiniAchievement(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(miniAchievementText = message)
            delay(2000)
            _uiState.value = _uiState.value.copy(miniAchievementText = null)
        }
    }

    private fun calculateSectionPoints(sectionIndex: Int): Int {
        // Base points per section with bonus for completion speed
        return 50 + (sectionIndex * 10)
    }

    fun completeInteractiveElement(elementId: String) {
        val currentState = _uiState.value
        val lesson = currentState.lesson ?: return
        
        viewModelScope.launch {
            // Mark interactive element as completed
            progressRepository.trackInteractiveElementComplete(lesson.id, elementId)
            
            // Award bonus points
            val bonusPoints = 25
            progressRepository.awardPoints(bonusPoints, "Interactive element completed")
            
            val updatedLesson = lesson.copy(
                pointsEarned = lesson.pointsEarned + bonusPoints,
                interactiveElements = lesson.interactiveElements.map { element ->
                    if (element.id == elementId) {
                        element.copy(isCompleted = true)
                    } else {
                        element
                    }
                }
            )
            
            _uiState.value = currentState.copy(lesson = updatedLesson)
            showMiniAchievement("+$bonusPoints bonus points!")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Track lesson exit
        viewModelScope.launch {
            val currentState = _uiState.value
            currentState.lesson?.let { lesson ->
                progressRepository.trackLessonExit(
                    lessonId = lesson.id,
                    totalTimeSpent = System.currentTimeMillis() - lessonStartTime,
                    sectionsCompleted = currentState.currentSection + 1
                )
            }
        }
    }
}

/**
 * UI State for Lesson Player
 */
data class LessonPlayerUiState(
    val lesson: LessonContent? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentSection: Int = 0,
    val progress: Float = 0f,
    val isPlaying: Boolean = false,
    val isCompleted: Boolean = false,
    val showAchievementAnimation: Boolean = false,
    val newAchievements: List<String> = emptyList(),
    val miniAchievementText: String? = null
)