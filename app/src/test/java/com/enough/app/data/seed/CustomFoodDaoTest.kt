package com.enough.app.data.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.enough.app.data.local.EnoughDatabase
import com.enough.app.data.repository.FoodRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A food the person adds themselves ("can't find it? add it") is searchable and
 * re-loggable, but deliberately kept out of the nudge/swap suggestion pool.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CustomFoodDaoTest {

    private lateinit var db: EnoughDatabase
    private lateinit var repository: FoodRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, EnoughDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FoodRepository(db.foodDao())
        runBlocking { FoodSeeder().seedIfEmpty(context, db.foodDao()) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a custom food is searchable but never suggested`() = runBlocking {
        // A high-fiber custom food would top the suggestion list if not excluded.
        val custom = repository.addCustomFood(
            name = "Grandma's bran muffin",
            servingLabel = "1 muffin",
            fiberG = 99.0,
        )
        assertTrue("new food has a real id", custom.id > 0)

        val searchHits = repository.search("bran muffin")
        assertTrue("custom food is findable in search", searchHits.any { it.id == custom.id })

        val suggestions = repository.topFiberFoods(30)
        assertFalse(
            "user-created food must not enter the suggestion pool",
            suggestions.any { it.id == custom.id },
        )
    }

    @Test
    fun `a custom food can be re-logged from recents`() = runBlocking {
        val custom = repository.addCustomFood(name = "Fairlife shake", servingLabel = "1 bottle", fiberG = 1.0)
        db.mealEntryDao().insert(
            com.enough.app.data.local.entity.MealEntry(
                foodId = custom.id,
                servingsMultiplier = 1.0,
                timestamp = java.time.Instant.ofEpochMilli(5_000),
                source = com.enough.app.data.model.MealSource.MANUAL,
            ),
        )
        val recents = repository.recentFoods(6)
        assertTrue(recents.any { it.name == "Fairlife shake" })
    }
}
