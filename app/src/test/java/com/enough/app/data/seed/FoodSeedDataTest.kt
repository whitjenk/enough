package com.enough.app.data.seed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Pure-JVM checks on the bundled food list and the parser. No Android/Room
 * dependency, so this runs fast and pins the data contract directly.
 */
class FoodSeedDataTest {

    private val foods by lazy {
        val json = File("src/main/assets/foods.json").readText()
        FoodSeeder().parseFoods(json)
    }

    @Test
    fun `food list parses to the expected row count`() {
        // Pinned to the current curated list; bump when foods are intentionally added.
        assertEquals(EXPECTED_FOOD_COUNT, foods.size)
        // And stays within the SPEC.md §"data model" target band of 150–300.
        assertTrue("food count ${foods.size} out of 150..300", foods.size in 150..300)
    }

    @Test
    fun `banana has plausible fiber and carb values`() {
        val banana = foods.single { it.name == "Banana" }
        assertEquals("1 medium", banana.servingLabel)
        assertEquals(27.0, banana.carbsG, 1.0)
        assertEquals(3.1, banana.fiberG, 0.5)
        assertEquals(1.3, banana.proteinG, 0.5)
    }

    @Test
    fun `every food has non-negative nutrients and fiber never exceeds carbs plus a margin`() {
        foods.forEach { food ->
            assertTrue("${food.name} carbs negative", food.carbsG >= 0.0)
            assertTrue("${food.name} fiber negative", food.fiberG >= 0.0)
            assertTrue("${food.name} protein negative", food.proteinG >= 0.0)
            // Fiber is a subset of carbohydrate; allow a small rounding margin.
            assertTrue(
                "${food.name} fiber ${food.fiberG} exceeds carbs ${food.carbsG}",
                food.fiberG <= food.carbsG + 0.5,
            )
        }
    }

    @Test
    fun `food names are unique`() {
        val names = foods.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }

    private companion object {
        const val EXPECTED_FOOD_COUNT = 172
    }
}
