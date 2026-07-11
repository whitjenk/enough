package com.enough.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.enough.app.EnoughApplication
import com.enough.app.feature.main.MainViewModel
import com.enough.app.feature.onboarding.OnboardingViewModel

/**
 * Single ViewModel factory wiring app-scoped dependencies (from [AppContainer])
 * into ViewModels. Keeps construction out of composables without pulling in a
 * DI framework. New ViewModels add an [initializer] block here.
 */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            MainViewModel(enoughApplication().container.userPreferencesRepository)
        }
        initializer {
            val container = enoughApplication().container
            OnboardingViewModel(
                goalRepository = container.goalRepository,
                riskResultRepository = container.riskResultRepository,
                weightRepository = container.weightRepository,
                userPreferencesRepository = container.userPreferencesRepository,
                healthConnectManager = container.healthConnectManager,
            )
        }
    }
}

/** The application instance, resolved from ViewModel [CreationExtras]. */
private fun CreationExtras.enoughApplication(): EnoughApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EnoughApplication
