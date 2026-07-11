package com.enough.app.di

import android.content.Context
import androidx.room.Room
import com.enough.app.data.local.EnoughDatabase
import com.enough.app.data.seed.FoodSeeder

/**
 * Minimal manual dependency container. Holds the single Room database and the
 * app-wide singletons the UI layer depends on. Kept deliberately simple — no DI
 * framework needed at Phase 0 scale — while still keeping construction out of
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

    /** Seed the bundled food list on first launch; no-op once populated. */
    suspend fun seedIfNeeded() {
        foodSeeder.seedIfEmpty(appContext, database.foodDao())
    }
}
