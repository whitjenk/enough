package com.enough.app.feature.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.di.AppViewModelProvider
import com.enough.app.feature.onboarding.OnboardingRoute

/**
 * Root of the app UI. Routes to onboarding until it is finished, then to the
 * main app. Kept minimal for Phase 0 — the Today/Progress/Settings nav graph is
 * introduced as those screens land.
 */
@Composable
fun EnoughApp(
    viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    when (onboardingCompleted) {
        // Still reading the preference — hold on a blank surface to avoid a flash.
        null -> Surface(Modifier.fillMaxSize()) {}
        false -> OnboardingRoute(onComplete = {})
        true -> MainNavHost()
    }
}
