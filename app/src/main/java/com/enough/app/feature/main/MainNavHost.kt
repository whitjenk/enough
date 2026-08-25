package com.enough.app.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.enough.app.R
import com.enough.app.feature.logging.AddMealRoute
import com.enough.app.feature.logging.LogActivityRoute
import com.enough.app.feature.logging.LogWeightRoute
import com.enough.app.feature.progress.ProgressRoute
import com.enough.app.feature.settings.SettingsRoute
import com.enough.app.feature.today.TodayRoute

/** Route identifiers for the post-onboarding app. */
object Routes {
    const val TODAY = "today"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val ADD_MEAL = "add_meal"
    const val LOG_WEIGHT = "log_weight"
    const val LOG_ACTIVITY = "log_activity"
}

/** Saved-state key carrying a just-logged meal id back to Today for undo. */
private const val UNDO_MEAL_ID_KEY = "undo_meal_id"

private data class TopLevelDestination(val route: String, val labelRes: Int)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.TODAY, R.string.nav_today),
    TopLevelDestination(Routes.PROGRESS, R.string.nav_progress),
    TopLevelDestination(Routes.SETTINGS, R.string.nav_settings),
)

/**
 * The main navigation graph shown after onboarding. Today, Progress, and
 * Settings are top-level tabs (bottom bar); logging screens push on top of Today
 * and pop back on save. There is deliberately no account/login destination —
 * the app is fully usable with no sign-up (see CLAUDE.md constraints).
 */
@Composable
fun MainNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelDestinations.map { it.route }.toSet()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                EnoughBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.navigateToTab(route) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.TODAY) { entry ->
                // Set by AddMeal on the way back; consumed once by Today.
                val undoMealId by entry.savedStateHandle
                    .getStateFlow<Long?>(UNDO_MEAL_ID_KEY, null)
                    .collectAsStateWithLifecycle()
                TodayRoute(
                    onAddMeal = { navController.navigate(Routes.ADD_MEAL) },
                    onLogWeight = { navController.navigate(Routes.LOG_WEIGHT) },
                    onLogActivity = { navController.navigate(Routes.LOG_ACTIVITY) },
                    undoMealId = undoMealId,
                    onUndoHandled = { entry.savedStateHandle[UNDO_MEAL_ID_KEY] = null },
                )
            }
            composable(Routes.PROGRESS) { ProgressRoute() }
            composable(Routes.SETTINGS) { SettingsRoute() }
            composable(Routes.ADD_MEAL) {
                AddMealRoute(
                    onSaved = { mealId ->
                        // Hand the new row's id to Today so it can offer an undo
                        // snackbar for a mis-tapped quick-log (§7.7 item 6).
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(UNDO_MEAL_ID_KEY, mealId)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LOG_WEIGHT) {
                LogWeightRoute(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LOG_ACTIVITY) {
                LogActivityRoute(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * A simple text-label bottom bar. Selection is conveyed by weight and color
 * together, not color alone (DESIGN.md / accessibility).
 */
@Composable
private fun EnoughBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            topLevelDestinations.forEach { destination ->
                val selected = currentRoute == destination.route
                TextButton(onClick = { onSelect(destination.route) }) {
                    Text(
                        text = stringResource(destination.labelRes),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}
