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
 * Verifies the v1 -> v2 migration rebuilds `user_goal` correctly: existing weight
 * data is preserved, the new columns are added with their defaults, and the
 * resulting schema matches what Room expects (validated by the helper against the
 * exported 2.json).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserGoalMigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    @Test
    fun `migrate 1 to 2 preserves weight and adds new columns`() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO user_goal " +
                    "(id, startWeightKg, targetWeightKg, weightLossPercent, activityGoalType, " +
                    "activityGoalValue, activityGoalCustomLabel, dailyCalorieEstimate, " +
                    "fiberGramsTarget, createdAt) " +
                    "VALUES (1, 82.0, 78.0, 5.0, 'MINUTES', 150, NULL, 2000, 28, 1700000000000)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 2, true, EnoughDatabase.MIGRATION_1_2)
        db.query(
            "SELECT startWeightKg, weightLossPercent, estimateCalibration, " +
                "takesGLP1Medication, dietaryRestrictions, personalWhy FROM user_goal WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(82.0, cursor.getDouble(0), 1e-9)
            assertEquals(5.0, cursor.getDouble(1), 1e-9)
            assertEquals("BALANCED", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
            assertEquals("", cursor.getString(4))
            assertTrue("personalWhy should default to null", cursor.isNull(5))
        }
        db.close()
    }
}
