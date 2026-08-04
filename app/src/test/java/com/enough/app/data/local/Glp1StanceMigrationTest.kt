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
 * Verifies the v7 -> v8 migration adds `user_goal.glp1Stance` and seeds it from
 * the legacy boolean: an existing "yes" carries over as ON, everyone else as NOT
 * (SPEC §7.6 Step 4).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Glp1StanceMigrationTest {

    private val dbName = "glp1-stance-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    private fun insertGoal(db: androidx.sqlite.db.SupportSQLiteDatabase, id: Int, takesGlp1: Int) {
        db.execSQL(
            "INSERT INTO user_goal (" +
                "id, startWeightKg, targetWeightKg, weightLossPercent, activityGoalType, " +
                "activityGoalValue, activityGoalCustomLabel, dailyCalorieEstimate, fiberGramsTarget, " +
                "createdAt, dietaryRestrictions, dietaryRestrictionOther, personalWhy, " +
                "takesGLP1Medication, estimateCalibration, hideNumbersMode) " +
                "VALUES ($id, NULL, NULL, NULL, 'MINUTES', 150, NULL, 2000, 28, 1000, '', NULL, NULL, $takesGlp1, 'BALANCED', 0)",
        )
    }

    @Test
    fun `migrate 7 to 8 seeds stance from the legacy boolean`() {
        helper.createDatabase(dbName, 7).use { db ->
            insertGoal(db, id = 1, takesGlp1 = 1) // was on a GLP-1
            insertGoal(db, id = 2, takesGlp1 = 0) // was not
        }

        val db = helper.runMigrationsAndValidate(dbName, 8, true, EnoughDatabase.MIGRATION_7_8)
        db.query("SELECT id, glp1Stance FROM user_goal ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals("ON", cursor.getString(1))
            assertTrue(cursor.moveToNext())
            assertEquals(2, cursor.getInt(0))
            assertEquals("NOT", cursor.getString(1))
        }
        db.close()
    }
}
