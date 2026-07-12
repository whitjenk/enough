package com.enough.app.data.seed

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.enough.app.data.local.EnoughDatabase
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.MealEntryType
import com.enough.app.data.model.MealSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The coarse quick-log DAO behavior: the synthetic category foods are seeded but
 * kept out of search and the suggestion pool, and recently-logged foods surface
 * for one-tap re-logging.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CoarseLogDaoTest {

    private lateinit var db: EnoughDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, EnoughDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        runBlocking { FoodSeeder().seedIfEmpty(context, db.foodDao()) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `category foods are seeded, non-selectable, and hidden from search`() = runBlocking {
        val categories = db.foodDao().categoryFoods()
        assertEquals(CategoryFoods.ALL.size, categories.size)
        assertTrue("all category foods are non-selectable", categories.all { !it.selectable })

        // "meal" would match every category name, but search must return none of them.
        val searchHits = db.foodDao().search("meal")
        assertTrue("search must exclude synthetic category foods", searchHits.none { !it.selectable })
    }

    @Test
    fun `category foods never enter the suggestion pool`() = runBlocking {
        // Category foods carry non-trivial fiber (up to 8g), so without the
        // selectable filter they could outrank real foods as suggestions.
        val topFiber = db.foodDao().topFiberFoods(20)
        assertTrue("suggestions must be real foods only", topFiber.all { it.selectable })
    }

    @Test
    fun `recently logged foods surface newest-first for one-tap re-logging`() = runBlocking {
        val lentils = db.foodDao().search("lentils").first { it.name == "Lentils" }
        val veggieMeal = db.foodDao().categoryFoods().first { it.name == "Veggie-heavy meal" }

        db.mealEntryDao().insert(
            MealEntry(foodId = lentils.id, servingsMultiplier = 1.0, timestamp = Instant.ofEpochMilli(1_000), source = MealSource.TEXT),
        )
        db.mealEntryDao().insert(
            MealEntry(
                foodId = veggieMeal.id, servingsMultiplier = 1.0, timestamp = Instant.ofEpochMilli(2_000),
                source = MealSource.MANUAL, entryType = MealEntryType.COARSE_ESTIMATE,
            ),
        )

        val recents = db.foodDao().recentlyLogged(6)
        // Most recent first, and a coarse category re-log is eligible too.
        assertEquals("Veggie-heavy meal", recents.first().name)
        assertTrue(recents.any { it.name == "Lentils" })
        assertFalse("only logged foods appear", recents.any { it.name == "Banana" })
    }
}
