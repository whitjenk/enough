package com.enough.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.enough.app.data.local.dao.ActivityEntryDao
import com.enough.app.data.local.dao.FoodDao
import com.enough.app.data.local.dao.MealEntryDao
import com.enough.app.data.local.dao.PrediabetesRiskResultDao
import com.enough.app.data.local.dao.RulesEngineStateDao
import com.enough.app.data.local.dao.UserGoalDao
import com.enough.app.data.local.dao.WeightEntryDao
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.local.entity.PrediabetesRiskResult
import com.enough.app.data.local.entity.RulesEngineState
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry

/**
 * The single on-device SQLite database. Local-first, no cloud mirror
 * (CLAUDE.md non-negotiable). Phase 0 schema only; new tables are added as
 * later phases need them.
 */
@Database(
    entities = [
        Food::class,
        MealEntry::class,
        WeightEntry::class,
        ActivityEntry::class,
        UserGoal::class,
        PrediabetesRiskResult::class,
        RulesEngineState::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class EnoughDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun activityEntryDao(): ActivityEntryDao
    abstract fun userGoalDao(): UserGoalDao
    abstract fun prediabetesRiskResultDao(): PrediabetesRiskResultDao
    abstract fun rulesEngineStateDao(): RulesEngineStateDao

    companion object {
        const val NAME = "enough.db"
    }
}
