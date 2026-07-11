package com.enough.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val healthConnectSyncEnabled: Boolean = true,
)

/**
 * Backs the Settings screen: the Health Connect sync toggle and the destructive
 * "delete my data" action. The wipe itself is delegated to [wipeAllUserData]
 * (the app container), which clears every local table and preference.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val wipeAllUserData: suspend () -> Unit,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        userPreferencesRepository.healthConnectSyncEnabled
            .map { SettingsUiState(healthConnectSyncEnabled = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SettingsUiState(),
            )

    fun setHealthConnectSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHealthConnectSyncEnabled(enabled)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            wipeAllUserData()
        }
    }
}
