package com.enough.app.data.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.enough.app.data.local.EnoughDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real seeding path: reads the bundled asset, writes to an
 * in-memory Room DB, and confirms the row count, a spot-check food, and that
 * re-running is a no-op (idempotent across launches).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoodSeederRoomTest {

    private lateinit var db: EnoughDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, EnoughDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // 197 curated foods from foods.json plus the synthetic coarse-category foods.
    private val expectedTotal = 197 + CategoryFoods.ALL.size

    @Test
    fun `seeding populates the food table with the expected count`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val inserted = FoodSeeder().seedIfEmpty(context, db.foodDao())

        assertEquals(expectedTotal, inserted)
        assertEquals(expectedTotal, db.foodDao().count())
    }

    @Test
    fun `seeding is idempotent`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val seeder = FoodSeeder()

        val firstRun = seeder.seedIfEmpty(context, db.foodDao())
        val secondRun = seeder.seedIfEmpty(context, db.foodDao())

        assertEquals(expectedTotal, firstRun)
        assertEquals(0, secondRun)
        assertEquals(expectedTotal, db.foodDao().count())
    }

    @Test
    fun `search finds a seeded food case-insensitively`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FoodSeeder().seedIfEmpty(context, db.foodDao())

        val results = db.foodDao().search("banana")

        assertTrue("expected a Banana in results", results.any { it.name == "Banana" })
    }
}
