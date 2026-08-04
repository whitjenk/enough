package com.enough.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.enough.app.data.local.dao.ActivityEntryDao
import com.enough.app.data.local.dao.DailyCheckInDao
import com.enough.app.data.local.dao.FoodDao
import com.enough.app.data.local.dao.MealEntryDao
import com.enough.app.data.local.dao.PrediabetesRiskResultDao
import com.enough.app.data.local.dao.RulesEngineStateDao
import com.enough.app.data.local.dao.UserGoalDao
import com.enough.app.data.local.dao.WeightEntryDao
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.DailyCheckIn
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
        DailyCheckIn::class,
    ],
    version = 7,
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
    abstract fun dailyCheckInDao(): DailyCheckInDao

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

        /**
         * v3 -> v4: the low-friction coarse quick-log (SPEC §7.5). Adds
         * `food.selectable` (real curated foods stay searchable/suggestable at the
         * default 1; the synthetic category foods are 0) and `meal_entry.entryType`
         * (existing entries default to database-matched). Both are additive so
         * logged meals are preserved. Also inserts the synthetic category foods for
         * existing installs — a fresh install seeds them via the seeder instead,
         * and this migration never runs there.
         *
         * The inserted rows are a frozen literal snapshot of the category foods as
         * they were at v4. A migration must never read live code (like the seeder's
         * current list): editing that code later would silently rewrite what this
         * historical migration does and diverge upgraded installs from each other.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food` ADD COLUMN `selectable` INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "ALTER TABLE `meal_entry` ADD COLUMN `entryType` TEXT NOT NULL " +
                        "DEFAULT 'DATABASE_MATCHED'",
                )
                db.execSQL(
                    "INSERT INTO `food` " +
                        "(`name`,`servingLabel`,`carbsG`,`fiberG`,`proteinG`,`dietaryTags`,`selectable`) VALUES " +
                        "('Veggie-heavy meal','1 meal',30.0,8.0,8.0,'',0)," +
                        "('Mixed meal','1 meal',40.0,5.0,20.0,'',0)," +
                        "('Protein-heavy meal','1 meal',15.0,3.0,35.0,'',0)," +
                        "('Carb-heavy meal','1 meal',55.0,2.0,8.0,'',0)",
                )
            }
        }

        /**
         * v4 -> v5: custom foods ("can't find it? add it"). Adds
         * `food.userCreated` (existing curated/category rows default to 0). A user
         * food is searchable and re-loggable but excluded from the suggestion pool.
         * Additive, so nothing is dropped.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `food` ADD COLUMN `userCreated` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v5 -> v6: the optional daily felt check-in (SPEC §0.8 / §7.6 Step 1).
         * Adds a new `daily_check_in` table keyed by calendar day; purely additive,
         * so nothing existing is touched. `date` is stored as an epoch day and
         * `createdAt` as epoch millis (see [Converters]).
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_check_in` (" +
                        "`date` INTEGER NOT NULL, " +
                        "`felt` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`date`))",
                )
            }
        }

        /**
         * v6 -> v7: hide-numbers mode (SPEC §23, pulled forward for §7.6 Step 2).
         * Adds `user_goal.hideNumbersMode`, a plain additive boolean column
         * (stored as INTEGER, default 0 = off). Nothing existing is touched.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_goal` ADD COLUMN `hideNumbersMode` INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
