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
 * Verifies the v4 -> v5 migration adds `food.userCreated` as a plain additive
 * column defaulting to 0, preserving existing foods (and their logged meals).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomFoodMigrationTest {

    private val dbName = "custom-food-migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughDatabase::class.java,
    )

    @Test
    fun `migrate 4 to 5 preserves foods and defaults userCreated to zero`() {
        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO food (id, name, servingLabel, carbsG, fiberG, proteinG, dietaryTags, selectable) " +
                    "VALUES (1, 'Lentils', '1/2 cup cooked', 20.0, 7.8, 9.0, '', 1)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, EnoughDatabase.MIGRATION_4_5)
        db.query("SELECT name, userCreated FROM food WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Lentils", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        db.close()
    }
}
