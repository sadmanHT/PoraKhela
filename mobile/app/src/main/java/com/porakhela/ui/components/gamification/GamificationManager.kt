package com.porakhela.ui.components.gamification

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.porakhela.data.model.*
import com.porakhela.data.repository.OfflineLessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Gamification Manager Component
 * Orchestrates all gamification animations and events in proper sequence
 */
@Composable
fun GamificationManager(
    childProfileId: String,
    modifier: Modifier = Modifier,
    viewModel: GamificationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(childProfileId) {
        viewModel.loadGamificationData(childProfileId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Points popup
        uiState.currentPointsEvent?.let { event ->
            AnimatedPointsPopup(
                points = event.points,
                title = event.title,
                onAnimationComplete = {
                    viewModel.dismissEvent(event.id)
                }
            )
        }

        // Achievement popup
        uiState.currentAchievementEvent?.let { event ->
            AchievementUnlockedPopup(
                title = event.title,
                description = event.message,
                points = event.points,
                onAnimationComplete = {
                    viewModel.dismissEvent(event.id)
                }
            )
        }

        // Streak popup
        uiState.currentStreakEvent?.let { event ->
            StreakMilestonePopup(
                streakCount = event.points, // Using points field for streak count
                milestoneText = event.title,
                onAnimationComplete = {
                    viewModel.dismissEvent(event.id)
                }
            )
        }

        // Level up celebration
        uiState.levelUpEvent?.let { event ->
            LevelUpCelebration(
                newLevel = event.points, // Using points field for level
                onAnimationComplete = {
                    viewModel.dismissEvent(event.id)
                }
            )
        }

        // Daily challenge completion
        uiState.dailyChallengeEvent?.let { event ->
            DailyChallengeCompletePopup(
                challengeName = event.title,
                rewardPoints = event.points,
                onAnimationComplete = {
                    viewModel.dismissEvent(event.id)
                }
            )
        }
    }
}

/**
 * Gamification Dashboard Component
 * Shows current progress, badges, and achievements
 */
@Composable
fun GamificationDashboard(
    childProfileId: String,
    modifier: Modifier = Modifier,
    viewModel: GamificationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(childProfileId) {
        viewModel.loadGamificationData(childProfileId)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Points counter
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedPointsCounter(
                    targetPoints = uiState.totalPoints,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Today: ${uiState.dailyPoints} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Level ${uiState.currentLevel}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Current streak
        if (uiState.currentStreak > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFFF6F00).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Column {
                        Text(
                            text = "${uiState.currentStreak} Day Streak!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFFFF6F00)
                        )
                        Text(
                            text = "Keep learning every day!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Badge progress
        uiState.nextBadge?.let { badgeProgress ->
            BadgeProgressBar(
                currentProgress = badgeProgress.currentProgress,
                maxProgress = badgeProgress.requiredProgress,
                badgeName = badgeProgress.badgeName,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recent achievements
        if (uiState.recentAchievements.isNotEmpty()) {
            Text(
                text = "Recent Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            uiState.recentAchievements.take(3).forEach { achievement ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏆",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = achievement.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "+${achievement.points}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ViewModel for managing gamification state and events
 */
@HiltViewModel
class GamificationViewModel @Inject constructor(
    private val offlineLessonRepository: OfflineLessonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamificationUiState())
    val uiState: StateFlow<GamificationUiState> = _uiState.asStateFlow()

    private var eventQueue = mutableListOf<GamificationEvent>()
    private var isProcessingEvents = false

    /**
     * Load gamification data for child profile
     */
    fun loadGamificationData(childProfileId: String) {
        viewModelScope.launch {
            try {
                // Load local points
                offlineLessonRepository.getLocalPoints(childProfileId).collect { localPoints ->
                    localPoints?.let { points ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                totalPoints = points.totalPoints,
                                dailyPoints = points.dailyPoints,
                                weeklyPoints = points.weeklyPoints,
                                currentStreak = points.currentStreak,
                                longestStreak = points.longestStreak,
                                currentLevel = points.level,
                                pendingSyncPoints = points.pendingSyncPoints
                            )
                        }
                    }
                }

                // Load recent achievements
                offlineLessonRepository.getLocalAchievements(childProfileId).collect { achievements ->
                    val recent = achievements
                        .filter { it.isUnlocked }
                        .sortedByDescending { it.earnedAt }
                        .take(5)

                    _uiState.update { it.copy(recentAchievements = recent) }
                }

                // Load pending gamification events
                loadPendingEvents(childProfileId)

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Load and process pending gamification events
     */
    private fun loadPendingEvents(childProfileId: String) {
        viewModelScope.launch {
            offlineLessonRepository.getPendingGamificationEvents(childProfileId).collect { events ->
                eventQueue.clear()
                eventQueue.addAll(events)
                
                if (!isProcessingEvents && eventQueue.isNotEmpty()) {
                    processNextEvent()
                }
            }
        }
    }

    /**
     * Process the next event in the queue
     */
    private suspend fun processNextEvent() {
        if (eventQueue.isEmpty() || isProcessingEvents) return

        isProcessingEvents = true
        val event = eventQueue.removeFirstOrNull()

        event?.let { currentEvent ->
            _uiState.update { currentState ->
                when (currentEvent.type) {
                    GamificationEventType.POINTS_EARNED -> {
                        currentState.copy(currentPointsEvent = currentEvent)
                    }
                    GamificationEventType.ACHIEVEMENT_UNLOCKED -> {
                        currentState.copy(currentAchievementEvent = currentEvent)
                    }
                    GamificationEventType.STREAK_MILESTONE -> {
                        currentState.copy(currentStreakEvent = currentEvent)
                    }
                    GamificationEventType.LEVEL_UP -> {
                        currentState.copy(levelUpEvent = currentEvent)
                    }
                    GamificationEventType.CHALLENGE_COMPLETED -> {
                        currentState.copy(dailyChallengeEvent = currentEvent)
                    }
                    else -> currentState
                }
            }
        }
    }

    /**
     * Dismiss current event and process next
     */
    fun dismissEvent(eventId: String) {
        viewModelScope.launch {
            // Mark event as shown in repository
            offlineLessonRepository.markEventShown(eventId)

            // Clear current event from UI state
            _uiState.update { currentState ->
                currentState.copy(
                    currentPointsEvent = if (currentState.currentPointsEvent?.id == eventId) null else currentState.currentPointsEvent,
                    currentAchievementEvent = if (currentState.currentAchievementEvent?.id == eventId) null else currentState.currentAchievementEvent,
                    currentStreakEvent = if (currentState.currentStreakEvent?.id == eventId) null else currentState.currentStreakEvent,
                    levelUpEvent = if (currentState.levelUpEvent?.id == eventId) null else currentState.levelUpEvent,
                    dailyChallengeEvent = if (currentState.dailyChallengeEvent?.id == eventId) null else currentState.dailyChallengeEvent
                )
            }

            isProcessingEvents = false

            // Process next event after a short delay
            kotlinx.coroutines.delay(500)
            processNextEvent()
        }
    }

    /**
     * Trigger specific gamification event manually
     */
    fun triggerEvent(event: GamificationEvent) {
        eventQueue.add(0, event) // Add to front of queue
        viewModelScope.launch {
            if (!isProcessingEvents) {
                processNextEvent()
            }
        }
    }

    /**
     * Calculate badge progress
     */
    private fun calculateBadgeProgress(): BadgeProgress? {
        val currentState = _uiState.value
        
        // Example badge calculations
        return when {
            currentState.totalPoints < 1000 -> BadgeProgress(
                badgeId = "bronze_learner",
                badgeName = "Bronze Learner",
                currentProgress = currentState.totalPoints,
                requiredProgress = 1000,
                progressPercentage = (currentState.totalPoints * 100) / 1000,
                iconUrl = null
            )
            currentState.totalPoints < 5000 -> BadgeProgress(
                badgeId = "silver_scholar",
                badgeName = "Silver Scholar",
                currentProgress = currentState.totalPoints - 1000,
                requiredProgress = 4000,
                progressPercentage = ((currentState.totalPoints - 1000) * 100) / 4000,
                iconUrl = null
            )
            currentState.totalPoints < 15000 -> BadgeProgress(
                badgeId = "gold_genius",
                badgeName = "Gold Genius",
                currentProgress = currentState.totalPoints - 5000,
                requiredProgress = 10000,
                progressPercentage = ((currentState.totalPoints - 5000) * 100) / 10000,
                iconUrl = null
            )
            else -> null // Max level reached
        }
    }

    init {
        // Update badge progress when points change
        viewModelScope.launch {
            _uiState.collect { state ->
                val badgeProgress = calculateBadgeProgress()
                if (badgeProgress != state.nextBadge) {
                    _uiState.update { it.copy(nextBadge = badgeProgress) }
                }
            }
        }
    }
}

/**
 * UI State for gamification components
 */
data class GamificationUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    
    // Points and level data
    val totalPoints: Int = 0,
    val dailyPoints: Int = 0,
    val weeklyPoints: Int = 0,
    val currentLevel: Int = 1,
    val pendingSyncPoints: Int = 0,
    
    // Streak data
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    
    // Achievements
    val recentAchievements: List<LocalAchievement> = emptyList(),
    val nextBadge: BadgeProgress? = null,
    
    // Active events (only one at a time)
    val currentPointsEvent: GamificationEvent? = null,
    val currentAchievementEvent: GamificationEvent? = null,
    val currentStreakEvent: GamificationEvent? = null,
    val levelUpEvent: GamificationEvent? = null,
    val dailyChallengeEvent: GamificationEvent? = null
)

/**
 * Helper functions for creating gamification events
 */
object GamificationEventFactory {
    
    fun createPointsEvent(childProfileId: String, points: Int): GamificationEvent {
        return GamificationEvent(
            id = java.util.UUID.randomUUID().toString(),
            childProfileId = childProfileId,
            type = GamificationEventType.POINTS_EARNED,
            title = "+$points Porapoints! 🎉",
            message = "Great job completing the lesson!",
            points = points,
            iconPath = null,
            animationType = "points_popup",
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun createAchievementEvent(childProfileId: String, achievement: LocalAchievement): GamificationEvent {
        return GamificationEvent(
            id = java.util.UUID.randomUUID().toString(),
            childProfileId = childProfileId,
            type = GamificationEventType.ACHIEVEMENT_UNLOCKED,
            title = achievement.title,
            message = achievement.description,
            points = achievement.points,
            iconPath = achievement.iconPath,
            animationType = "achievement_unlock",
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun createStreakEvent(childProfileId: String, streakCount: Int): GamificationEvent {
        return GamificationEvent(
            id = java.util.UUID.randomUUID().toString(),
            childProfileId = childProfileId,
            type = GamificationEventType.STREAK_MILESTONE,
            title = "🔥 $streakCount Day Streak!",
            message = "You're on fire! Keep it up!",
            points = streakCount,
            iconPath = null,
            animationType = "streak_celebration",
            createdAt = System.currentTimeMillis()
        )
    }
    
    fun createLevelUpEvent(childProfileId: String, newLevel: Int): GamificationEvent {
        return GamificationEvent(
            id = java.util.UUID.randomUUID().toString(),
            childProfileId = childProfileId,
            type = GamificationEventType.LEVEL_UP,
            title = "LEVEL UP!",
            message = "You've reached Level $newLevel! Amazing progress!",
            points = newLevel,
            iconPath = null,
            animationType = "level_up_celebration",
            createdAt = System.currentTimeMillis()
        )
    }
}