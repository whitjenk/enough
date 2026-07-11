package com.enough.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.enough.app.EnoughApplication
import com.enough.app.feature.logging.AddMealViewModel
import com.enough.app.feature.logging.LogActivityViewModel
import com.enough.app.feature.logging.LogWeightViewModel
import com.enough.app.feature.main.MainViewModel
import com.enough.app.feature.onboarding.OnboardingViewModel
import com.enough.app.feature.progress.ProgressViewModel
import com.enough.app.feature.settings.SettingsViewModel
import com.enough.app.feature.today.TodayViewModel

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
        initializer {
            val container = enoughApplication().container
            TodayViewModel(
                goalRepository = container.goalRepository,
                mealRepository = container.mealRepository,
                weightRepository = container.weightRepository,
                activityRepository = container.activityRepository,
                foodRepository = container.foodRepository,
                rulesEngineStateRepository = container.rulesEngineStateRepository,
                healthConnectManager = container.healthConnectManager,
                userPreferencesRepository = container.userPreferencesRepository,
            )
        }
        initializer {
            val container = enoughApplication().container
            ProgressViewModel(
                goalRepository = container.goalRepository,
                mealRepository = container.mealRepository,
                activityRepository = container.activityRepository,
                weightRepository = container.weightRepository,
            )
        }
        initializer {
            val container = enoughApplication().container
            AddMealViewModel(
                foodRepository = container.foodRepository,
                mealRepository = container.mealRepository,
            )
        }
        initializer {
            LogWeightViewModel(enoughApplication().container.weightRepository)
        }
        initializer {
            val container = enoughApplication().container
            SettingsViewModel(
                userPreferencesRepository = container.userPreferencesRepository,
                goalRepository = container.goalRepository,
                wipeAllUserData = container::wipeAllUserData,
            )
        }
        initializer {
            val container = enoughApplication().container
            LogActivityViewModel(
                activityRepository = container.activityRepository,
                goalRepository = container.goalRepository,
            )
        }
    }
}

/** The application instance, resolved from ViewModel [CreationExtras]. */
private fun CreationExtras.enoughApplication(): EnoughApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EnoughApplication
