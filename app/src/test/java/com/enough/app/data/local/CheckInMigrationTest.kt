package com.enough.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the v5 -> v6 migration adds the `daily_check_in` table without
 * touching existing data (SPEC §7.6 Step 1). Additive-only: a pre-existing food
 * row survives, and the new table starts empty and writable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CheckInMigrationTest {

    private val dbName = "check-in-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    @Test
    fun `migrate 5 to 6 adds an empty daily_check_in table and preserves existing data`() {
        helper.createDatabase(dbName, 5).use { db ->
            db.execSQL(
                "INSERT INTO food (id, name, servingLabel, carbsG, fiberG, proteinG, dietaryTags, selectable, userCreated) " +
                    "VALUES (1, 'Lentils', '1/2 cup cooked', 20.0, 7.8, 9.0, '', 1, 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, EnoughDatabase.MIGRATION_5_6)

        // Existing food is untouched.
        db.query("SELECT name FROM food WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Lentils", cursor.getString(0))
        }
        // The new table exists and is empty...
        db.query("SELECT COUNT(*) FROM daily_check_in").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        // ...and is writable with the expected columns.
        db.execSQL("INSERT INTO daily_check_in (date, felt, createdAt) VALUES (20304, 'STEADY', 1000)")
        db.query("SELECT felt FROM daily_check_in WHERE date = 20304").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("STEADY", cursor.getString(0))
            assertFalse(cursor.moveToNext())
        }
        db.close()
    }
}
