package com.enough.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.enough.app.data.local.EnoughDatabase
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.ActivityRepository
import com.enough.app.data.repository.FoodRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.data.repository.RiskResultRepository
import com.enough.app.data.repository.RulesEngineStateRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.data.seed.FoodSeeder
import com.enough.app.health.HealthConnectManager

private val Context.dataStore by preferencesDataStore(name = "enough_prefs")

/**
 * Minimal manual dependency container. Holds the single Room database, the
 * preferences store, repositories, and the Health Connect manager — the
 * app-wide singletons the UI/ViewModel layer depends on. Kept deliberately
 * simple (no DI framework) at Phase 0 scale while keeping construction out of
 * the Compose/ViewModel layers.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: EnoughDatabase = Room.databaseBuilder(
        appContext,
        EnoughDatabase::class.java,
        EnoughDatabase.NAME,
    ).build()

    val foodSeeder: FoodSeeder = FoodSeeder()

    val userPreferencesRepository: UserPreferencesRepository =
        UserPreferencesRepository(appContext.dataStore)

    val goalRepository: GoalRepository = GoalRepository(database.userGoalDao())

    val riskResultRepository: RiskResultRepository =
        RiskResultRepository(database.prediabetesRiskResultDao())

    val weightRepository: WeightRepository = WeightRepository(database.weightEntryDao())

    val foodRepository: FoodRepository = FoodRepository(database.foodDao())

    val mealRepository: MealRepository = MealRepository(database.mealEntryDao())

    val activityRepository: ActivityRepository = ActivityRepository(database.activityEntryDao())

    val rulesEngineStateRepository: RulesEngineStateRepository =
        RulesEngineStateRepository(database.rulesEngineStateDao())

    val healthConnectManager: HealthConnectManager = HealthConnectManager(appContext)

    /** Seed the bundled food list on first launch; no-op once populated. */
    suspend fun seedIfNeeded() {
        foodSeeder.seedIfEmpty(appContext, database.foodDao())
    }
}
