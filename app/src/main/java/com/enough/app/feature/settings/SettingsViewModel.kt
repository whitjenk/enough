package com.enough.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.domain.feedback.FeedbackSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class SettingsUiState(
    val healthConnectSyncEnabled: Boolean = true,
    val estimateCalibration: EstimateCalibration = EstimateCalibration.BALANCED,
    /** The anonymous "help improve" summary once the person asks to see it; null until then. */
    val feedback: FeedbackSummary? = null,
)

/**
 * Backs the Settings screen: the Health Connect sync toggle, the fiber-estimate
 * calibration preference, the anonymous "help improve" summary, and the
 * destructive "delete my data" action. The wipe itself is delegated to
 * [wipeAllUserData] (the app container), which clears every local table and
 * preference.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val wipeAllUserData: suspend () -> Unit,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val feedback = MutableStateFlow<FeedbackSummary?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            userPreferencesRepository.healthConnectSyncEnabled,
            goalRepository.goal,
            feedback,
        ) { syncEnabled, goal, feedbackSummary ->
            SettingsUiState(
                healthConnectSyncEnabled = syncEnabled,
                estimateCalibration = goal?.estimateCalibration ?: EstimateCalibration.BALANCED,
                feedback = feedbackSummary,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    /**
     * Build the anonymous aggregate summary on-device so the person can review
     * exactly what it contains before choosing to share it. Nothing is sent here.
     */
    fun prepareFeedback() {
        viewModelScope.launch {
            feedback.value = FeedbackSummary.from(mealRepository.allMeals(), goalRepository.getGoal(), zone)
        }
    }

    fun setHealthConnectSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHealthConnectSyncEnabled(enabled)
        }
    }

    /**
     * Persists a new fiber-estimate lean. A no-op if there's no goal row yet
     * (calibration lives on the goal, which onboarding always creates first).
     */
    fun setEstimateCalibration(calibration: EstimateCalibration) {
        viewModelScope.launch {
            val goal = goalRepository.getGoal() ?: return@launch
            goalRepository.saveGoal(goal.copy(estimateCalibration = calibration))
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            wipeAllUserData()
        }
    }
}
