package com.enough.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.MealEntryType
import com.enough.app.data.model.MealSource
import com.enough.app.data.repository.MealRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The lookup that makes "undo a just-logged meal" safe (§7.7 item 6): an undo
 * resolves the row by id rather than trusting a stale copy, and has to stay a
 * no-op when the row is already gone.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UndoMealTest {

    private lateinit var db: EnoughDatabase
    private lateinit var repository: MealRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, EnoughDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MealRepository(db.mealEntryDao())
        runBlocking {
            db.foodDao().insertAll(
                listOf(
                    Food(
                        id = 1,
                        name = "Lentils",
                        servingLabel = "1/2 cup",
                        carbsG = 20.0,
                        fiberG = 8.0,
                        proteinG = 9.0,
                    ),
                ),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `a logged meal can be found by the id the insert returned and then removed`() = runBlocking {
        val id = repository.add(
            MealEntry(
                foodId = 1,
                servingsMultiplier = 1.0,
                timestamp = Instant.EPOCH,
                source = MealSource.MANUAL,
                entryType = MealEntryType.COARSE_ESTIMATE,
            ),
        )

        val found = repository.findById(id)
        assertNotNull("the insert's id must resolve back to the row", found)

        repository.delete(found!!)
        assertNull("undo removes the row", repository.findById(id))
        assertEquals(0, repository.allMeals().size)
    }

    @Test
    fun `looking up an already-removed meal returns null rather than throwing`() = runBlocking {
        // Undo can arrive twice (a double tap), or after the row was deleted from
        // Today. Both have to be harmless no-ops, not crashes.
        val id = repository.add(
            MealEntry(
                foodId = 1,
                servingsMultiplier = 1.0,
                timestamp = Instant.EPOCH,
                source = MealSource.MANUAL,
                entryType = MealEntryType.COARSE_ESTIMATE,
            ),
        )
        repository.delete(repository.findById(id)!!)

        assertNull(repository.findById(id))
    }

    @Test
    fun `undoing one meal leaves the others alone`() = runBlocking {
        val keep = repository.add(
            MealEntry(
                foodId = 1,
                servingsMultiplier = 1.0,
                timestamp = Instant.EPOCH,
                source = MealSource.MANUAL,
                entryType = MealEntryType.COARSE_ESTIMATE,
            ),
        )
        val undo = repository.add(
            MealEntry(
                foodId = 1,
                servingsMultiplier = 2.0,
                timestamp = Instant.EPOCH,
                source = MealSource.MANUAL,
                entryType = MealEntryType.COARSE_ESTIMATE,
            ),
        )

        repository.delete(repository.findById(undo)!!)

        assertNull(repository.findById(undo))
        assertNotNull(repository.findById(keep))
        assertEquals(1, repository.allMeals().size)
    }
}
