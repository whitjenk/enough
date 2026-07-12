package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.data.model.DietaryTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySwapTest {

    private val plant = setOf(
        DietaryTag.VEGETARIAN, DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE,
    )

    private fun food(name: String, fiber: Double, tags: Set<DietaryTag> = plant) =
        Food(
            id = name.hashCode().toLong(),
            name = name,
            servingLabel = "1 serving",
            carbsG = 20.0,
            fiberG = fiber,
            proteinG = 5.0,
            dietaryTags = tags,
        )

    private val pool = listOf(
        food("Chia seeds", 9.8),
        food("Raspberries", 8.0),
        food("Lentils", 7.8),
        food("Pear", 5.5),
        food("Almonds", 3.5, plant + DietaryTag.CONTAINS_NUTS),
        food("Broccoli", 2.4),
    )

    @Test
    fun `picks a real safe food from the pool`() {
        val swap = DailySwap.forDay(epochDay = 0, candidates = pool)
        assertTrue(swap != null)
        assertTrue(pool.any { it.name == swap!!.food })
    }

    @Test
    fun `same day and inputs are deterministic while different days rotate`() {
        val a1 = DailySwap.forDay(epochDay = 20_000, candidates = pool)
        val a2 = DailySwap.forDay(epochDay = 20_000, candidates = pool)
        assertEquals(a1, a2)

        val b = DailySwap.forDay(epochDay = 20_001, candidates = pool)
        assertNotEquals(a1!!.food, b!!.food)
    }

    @Test
    fun `gentle biases toward a smaller add than non-gentle for the same day`() {
        val normal = DailySwap.forDay(epochDay = 0, candidates = pool, gentle = false)!!
        val gentle = DailySwap.forDay(epochDay = 0, candidates = pool, gentle = true)!!
        // Non-gentle leads with the biggest lever (Chia 9.8); gentle leads with
        // the smallest (Broccoli 2.4).
        assertEquals("Chia seeds", normal.food)
        assertEquals("Broccoli", gentle.food)
        assertTrue(gentle.gentle)
        assertTrue(gentle.fiberG < normal.fiberG)
    }

    @Test
    fun `never surfaces a food that violates a restriction`() {
        // A nut allergy must exclude Almonds on every day of the rotation.
        (0L until 30L).forEach { day ->
            val swap = DailySwap.forDay(
                epochDay = day,
                candidates = pool,
                restrictions = setOf(DietaryRestriction.NUT_ALLERGY),
            )
            assertNotEquals("Almonds", swap?.food)
        }
    }

    @Test
    fun `all-unsafe pool yields no swap rather than an unsafe one`() {
        val meatTags = setOf(DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE)
        val meatOnly = listOf(
            food("Grilled chicken", 6.0, meatTags),
            food("Beef jerky", 4.0, meatTags),
        )
        val swap = DailySwap.forDay(
            epochDay = 3,
            candidates = meatOnly,
            restrictions = setOf(DietaryRestriction.VEGETARIAN),
        )
        assertNull(swap)
    }

    @Test
    fun `foods below the minimum fiber threshold are never picked`() {
        val weakOnly = listOf(food("Iceberg lettuce", 0.5), food("Cucumber", 0.8))
        assertNull(DailySwap.forDay(epochDay = 1, candidates = weakOnly))
    }
}
