package com.enough.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the v6 -> v7 migration adds `user_goal.hideNumbersMode` as a plain
 * additive column defaulting to 0 (off), preserving the existing goal row
 * (SPEC §23, pulled forward for §7.6 Step 2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HideNumbersMigrationTest {

    private val dbName = "hide-numbers-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    @Test
    fun `migrate 6 to 7 defaults hideNumbersMode off and preserves the goal`() {
        helper.createDatabase(dbName, 6).use { db ->
            db.execSQL(
                "INSERT INTO user_goal (" +
                    "id, startWeightKg, targetWeightKg, weightLossPercent, activityGoalType, " +
                    "activityGoalValue, activityGoalCustomLabel, dailyCalorieEstimate, fiberGramsTarget, " +
                    "createdAt, dietaryRestrictions, dietaryRestrictionOther, personalWhy, " +
                    "takesGLP1Medication, estimateCalibration) " +
                    "VALUES (1, NULL, NULL, NULL, 'MINUTES', 150, NULL, 2000, 28, 1000, '', NULL, NULL, 0, 'BALANCED')",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 7, true, EnoughDatabase.MIGRATION_6_7)
        db.query("SELECT fiberGramsTarget, hideNumbersMode FROM user_goal WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(28, cursor.getInt(0)) // existing data preserved
            assertEquals(0, cursor.getInt(1)) // hide-numbers defaults off
        }
        db.close()
    }
}
