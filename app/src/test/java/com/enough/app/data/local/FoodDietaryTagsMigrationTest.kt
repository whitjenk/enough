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
 * Verifies the v2 -> v3 migration adds `food.dietaryTags` as a plain additive
 * column: existing food rows (and therefore the meals that cascade from them)
 * are preserved, and the new column defaults to an empty tag set.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoodDietaryTagsMigrationTest {

    private val dbName = "food-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    @Test
    fun `migrate 2 to 3 preserves foods and adds an empty dietaryTags column`() {
        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO food (id, name, servingLabel, carbsG, fiberG, proteinG) " +
                    "VALUES (1, 'Lentils', '1/2 cup cooked', 20.0, 7.8, 9.0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, EnoughDatabase.MIGRATION_2_3)
        db.query("SELECT name, dietaryTags FROM food WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Lentils", cursor.getString(0))
            assertEquals("", cursor.getString(1))
        }
        db.close()
    }
}
