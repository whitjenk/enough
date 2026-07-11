package com.enough.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val healthConnectSyncEnabled: Boolean = true,
    val estimateCalibration: EstimateCalibration = EstimateCalibration.BALANCED,
)

/**
 * Backs the Settings screen: the Health Connect sync toggle, the fiber-estimate
 * calibration preference, and the destructive "delete my data" action. The wipe
 * itself is delegated to [wipeAllUserData] (the app container), which clears
 * every local table and preference.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val goalRepository: GoalRepository,
    private val wipeAllUserData: suspend () -> Unit,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        combine(
            userPreferencesRepository.healthConnectSyncEnabled,
            goalRepository.goal,
        ) { syncEnabled, goal ->
            SettingsUiState(
                healthConnectSyncEnabled = syncEnabled,
                estimateCalibration = goal?.estimateCalibration ?: EstimateCalibration.BALANCED,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

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
