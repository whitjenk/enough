package com.enough.app.feature.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Top-level app state: whether onboarding is finished, which decides the start
 * destination. A null value means "still loading" so we don't flash the wrong
 * screen before the preference is read.
 */
class MainViewModel(
    userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    val onboardingCompleted: StateFlow<Boolean?> =
        userPreferencesRepository.onboardingCompleted
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )
}
