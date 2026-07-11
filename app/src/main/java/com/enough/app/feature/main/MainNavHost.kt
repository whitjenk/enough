package com.enough.app.feature.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enough.app.feature.logging.AddMealRoute
import com.enough.app.feature.logging.LogActivityRoute
import com.enough.app.feature.logging.LogWeightRoute
import com.enough.app.feature.today.TodayRoute

/** Route identifiers for the post-onboarding app. */
object Routes {
    const val TODAY = "today"
    const val ADD_MEAL = "add_meal"
    const val LOG_WEIGHT = "log_weight"
    const val LOG_ACTIVITY = "log_activity"
}

/**
 * The main navigation graph shown after onboarding. Today is the home; logging
 * screens are pushed on top and pop back to Today on save. Progress and Settings
 * join this graph in later tasks.
 */
@Composable
fun MainNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.TODAY) {
        composable(Routes.TODAY) {
            TodayRoute(
                onAddMeal = { navController.navigate(Routes.ADD_MEAL) },
                onLogWeight = { navController.navigate(Routes.LOG_WEIGHT) },
                onLogActivity = { navController.navigate(Routes.LOG_ACTIVITY) },
            )
        }
        composable(Routes.ADD_MEAL) {
            AddMealRoute(
                onSaved = { navController.popBackStack() },
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
