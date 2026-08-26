package com.enough.app.feature.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

private data class TopLevelDestination(val route: String, val labelRes: Int, val iconRes: Int)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.TODAY, R.string.nav_today, R.drawable.ic_today),
    TopLevelDestination(Routes.PROGRESS, R.string.nav_progress, R.drawable.ic_progress),
    TopLevelDestination(Routes.SETTINGS, R.string.nav_settings, R.drawable.ic_settings),
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
        containerColor = Color.Transparent,
        // Transparent has no `contentColorFor` mapping, so M3 falls back to
        // black and every Text that doesn't set its own colour goes unreadable
        // in dark mode. Name the content colour explicitly (§7.10 B1).
        contentColor = MaterialTheme.colorScheme.onBackground,
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
 * The app's bottom navigation, using M3's own [NavigationBar].
 *
 * This replaced a hand-rolled `Row` of `TextButton`s (§7.10 A2). That version
 * was the loudest "unfinished" signal in the app: no icons, no selection
 * indicator, ~40dp touch targets against the 80dp a navigation bar is supposed
 * to give, and — because a bare `Surface` carries no window insets of its own —
 * labels that sat directly on the gesture pill. `NavigationBar` handles its own
 * insets, so the explicit `navigationBarsPadding()` A1 added is no longer
 * needed here.
 *
 * Selection is still conveyed by more than color: the selected item gets M3's
 * pill indicator behind its icon, which is a shape difference (DESIGN.md /
 * accessibility). The icons carry no `contentDescription` because each item's
 * visible label is its accessible name — describing both would make a screen
 * reader announce every tab twice.
 */
@Composable
private fun EnoughBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar {
        topLevelDestinations.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination.route) },
                icon = {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}
