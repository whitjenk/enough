package com.enough.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
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

        /**
         * v1 -> v2: the revised onboarding (SPEC §3/§5/§7). Makes the weight-goal
         * columns nullable (weight goal is now optional) and adds the new
         * onboarding/settings columns: dietary restrictions, "personal why",
         * GLP-1 use, and estimate calibration. SQLite can't relax NOT NULL in
         * place, so `user_goal` is rebuilt; existing rows are preserved.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `user_goal_new` (" +
                        "`id` INTEGER NOT NULL, " +
                        "`startWeightKg` REAL, " +
                        "`targetWeightKg` REAL, " +
                        "`weightLossPercent` REAL, " +
                        "`activityGoalType` TEXT NOT NULL, " +
                        "`activityGoalValue` INTEGER NOT NULL, " +
                        "`activityGoalCustomLabel` TEXT, " +
                        "`dailyCalorieEstimate` INTEGER NOT NULL, " +
                        "`fiberGramsTarget` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`dietaryRestrictions` TEXT NOT NULL DEFAULT '', " +
                        "`dietaryRestrictionOther` TEXT, " +
                        "`personalWhy` TEXT, " +
                        "`takesGLP1Medication` INTEGER NOT NULL DEFAULT 0, " +
                        "`estimateCalibration` TEXT NOT NULL DEFAULT 'BALANCED', " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "INSERT INTO `user_goal_new` (" +
                        "`id`, `startWeightKg`, `targetWeightKg`, `weightLossPercent`, " +
                        "`activityGoalType`, `activityGoalValue`, `activityGoalCustomLabel`, " +
                        "`dailyCalorieEstimate`, `fiberGramsTarget`, `createdAt`) " +
                        "SELECT `id`, `startWeightKg`, `targetWeightKg`, `weightLossPercent`, " +
                        "`activityGoalType`, `activityGoalValue`, `activityGoalCustomLabel`, " +
                        "`dailyCalorieEstimate`, `fiberGramsTarget`, `createdAt` FROM `user_goal`",
                )
                db.execSQL("DROP TABLE `user_goal`")
                db.execSQL("ALTER TABLE `user_goal_new` RENAME TO `user_goal`")
            }
        }

        /**
         * v2 -> v3: adds explicit dietary tags to `food` (SPEC §5 follow-up), so
         * suggestion filtering reads real per-food data instead of guessing from
         * the name. A plain additive column keeps existing rows and — crucially —
         * the user's logged meals, which cascade-delete from `food` (so we must
         * not drop/rebuild the food table). Rows seeded before v3 keep an empty
         * tag set; a fresh install or a "delete my data" re-seed loads the fully
         * tagged list from `foods.json`.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food` ADD COLUMN `dietaryTags` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
