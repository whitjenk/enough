package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietaryFilterTest {

    private fun food(name: String) =
        Food(id = 1, name = name, servingLabel = "1 serving", carbsG = 10.0, fiberG = 5.0, proteinG = 3.0)

    @Test
    fun `no restrictions means everything is safe`() {
        assertTrue(DietaryFilter.isSafe(food("Grilled chicken"), emptySet()))
    }

    @Test
    fun `vegetarian excludes meat and seafood but keeps plants and dairy`() {
        val veg = setOf(DietaryRestriction.VEGETARIAN)
        assertFalse(DietaryFilter.isSafe(food("Chicken breast"), veg))
        assertFalse(DietaryFilter.isSafe(food("Canned tuna"), veg))
        assertTrue(DietaryFilter.isSafe(food("Lentils"), veg))
        assertTrue(DietaryFilter.isSafe(food("Greek yogurt"), veg)) // dairy is fine for vegetarians
    }

    @Test
    fun `vegan also excludes dairy, egg, and honey`() {
        val vegan = setOf(DietaryRestriction.VEGAN)
        assertFalse(DietaryFilter.isSafe(food("Greek yogurt"), vegan))
        assertFalse(DietaryFilter.isSafe(food("Boiled egg"), vegan))
        assertFalse(DietaryFilter.isSafe(food("Honey"), vegan))
        assertTrue(DietaryFilter.isSafe(food("Black beans"), vegan))
    }

    @Test
    fun `gluten-free excludes wheat-based foods`() {
        val gf = setOf(DietaryRestriction.GLUTEN_FREE)
        assertFalse(DietaryFilter.isSafe(food("Whole wheat bread"), gf))
        assertFalse(DietaryFilter.isSafe(food("Whole wheat pasta"), gf))
        assertTrue(DietaryFilter.isSafe(food("Brown rice"), gf))
    }

    @Test
    fun `dairy-free excludes dairy only`() {
        val df = setOf(DietaryRestriction.DAIRY_FREE)
        assertFalse(DietaryFilter.isSafe(food("Cheddar cheese"), df))
        assertFalse(DietaryFilter.isSafe(food("Milk"), df))
        assertTrue(DietaryFilter.isSafe(food("Boiled egg"), df)) // egg isn't dairy
    }

    @Test
    fun `nut allergy excludes named nuts but not lookalikes`() {
        val nut = setOf(DietaryRestriction.NUT_ALLERGY)
        assertFalse(DietaryFilter.isSafe(food("Almonds"), nut))
        assertFalse(DietaryFilter.isSafe(food("Peanut butter"), nut))
        // Conservative but not overzealous: "coconut"/"butternut" aren't tree nuts.
        assertTrue(DietaryFilter.isSafe(food("Butternut squash"), nut))
        assertTrue(DietaryFilter.isSafe(food("Coconut"), nut))
    }

    @Test
    fun `free-text restriction excludes by name substring`() {
        assertFalse(
            DietaryFilter.isSafe(food("Shrimp stir fry"), emptySet(), other = "shrimp"),
        )
        assertTrue(DietaryFilter.isSafe(food("Broccoli"), emptySet(), other = "shrimp"))
        assertTrue(DietaryFilter.isSafe(food("Broccoli"), emptySet(), other = "   "))
    }

    @Test
    fun `multiple restrictions all apply`() {
        val both = setOf(DietaryRestriction.VEGETARIAN, DietaryRestriction.NUT_ALLERGY)
        assertFalse(DietaryFilter.isSafe(food("Chicken"), both))
        assertFalse(DietaryFilter.isSafe(food("Walnuts"), both))
        assertTrue(DietaryFilter.isSafe(food("Chickpeas"), both))
    }
}
