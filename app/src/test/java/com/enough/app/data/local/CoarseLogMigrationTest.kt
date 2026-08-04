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
 * Verifies the v3 -> v4 migration (coarse quick-log): it adds `food.selectable`
 * and `meal_entry.entryType` as additive columns — preserving existing foods and
 * their logged meals — and inserts the synthetic category foods for installs that
 * upgrade (fresh installs get them from the seeder instead).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoarseLogMigrationTest {

    private val dbName = "coarse-log-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    @Test
    fun `migrate 3 to 4 preserves data and seeds category foods`() {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO food (id, name, servingLabel, carbsG, fiberG, proteinG, dietaryTags) " +
                    "VALUES (1, 'Lentils', '1/2 cup cooked', 20.0, 7.8, 9.0, '')",
            )
            db.execSQL(
                "INSERT INTO meal_entry (id, foodId, servingsMultiplier, timestamp, source) " +
                    "VALUES (1, 1, 1.0, 1000, 'TEXT')",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, EnoughDatabase.MIGRATION_3_4)

        // The pre-existing real food keeps its data and defaults to selectable.
        db.query("SELECT selectable FROM food WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        // The logged meal survives and defaults to database-matched.
        db.query("SELECT entryType FROM meal_entry WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("DATABASE_MATCHED", cursor.getString(0))
        }
        // The synthetic category foods are inserted, non-selectable. Asserted as
        // literals (not against the live seeder list) because the migration is a
        // frozen snapshot — this test should fail if someone edits the migration,
        // not silently follow a change to the current category foods.
        db.query(
            "SELECT name FROM food WHERE selectable = 0 ORDER BY fiberG DESC",
        ).use { cursor ->
            val names = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            assertEquals(
                listOf("Veggie-heavy meal", "Mixed meal", "Protein-heavy meal", "Carb-heavy meal"),
                names,
            )
        }
        db.close()
    }
}
