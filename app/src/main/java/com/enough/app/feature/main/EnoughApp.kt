package com.enough.app.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.di.AppViewModelProvider
import com.enough.app.feature.onboarding.OnboardingRoute
import com.enough.app.ui.theme.EnoughTheme

/**
 * Root of the app UI. Routes to onboarding until it is finished, then to the
 * main app ([MainNavHost] with the Today/Progress/Settings tabs). A null
 * onboarding flag means "still loading" so no screen flashes before the
 * preference is read.
 */
@Composable
fun EnoughApp(
    viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    // The time-of-day wash is painted once here, behind everything, and the
    // Scaffolds above it stay transparent (§7.10 B1). One continuous field
    // behind the whole app rather than a decoration repeated per screen.
    Box(Modifier.fillMaxSize().background(EnoughTheme.backgroundBrush)) {
        when (onboardingCompleted) {
            // Still reading the preference — hold blank to avoid a flash.
            null -> Unit
            false -> OnboardingRoute(onComplete = {})
            true -> MainNavHost()
        }
    }
}
