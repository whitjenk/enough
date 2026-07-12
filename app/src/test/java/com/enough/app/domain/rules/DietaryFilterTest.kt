package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.data.model.DietaryTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DietaryFilterTest {

    private fun food(name: String, vararg tags: DietaryTag) =
        Food(
            id = 1,
            name = name,
            servingLabel = "1 serving",
            carbsG = 10.0,
            fiberG = 5.0,
            proteinG = 3.0,
            dietaryTags = tags.toSet(),
        )

    private val chicken = food("Chicken breast", DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE)
    private val lentils = food(
        "Lentils",
        DietaryTag.VEGETARIAN, DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE,
    )
    private val yogurt = food("Greek yogurt", DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE)
    private val almonds = food(
        "Almonds",
        DietaryTag.VEGETARIAN, DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE,
        DietaryTag.DAIRY_FREE, DietaryTag.CONTAINS_NUTS,
    )
    private val wheatBread = food(
        "Whole wheat bread",
        DietaryTag.VEGETARIAN, DietaryTag.VEGAN, DietaryTag.DAIRY_FREE,
    )

    @Test
    fun `no restrictions means everything is safe`() {
        assertTrue(DietaryFilter.isSafe(chicken, emptySet()))
    }

    @Test
    fun `vegetarian requires the vegetarian tag, allowing dairy but not meat`() {
        val veg = setOf(DietaryRestriction.VEGETARIAN)
        assertFalse(DietaryFilter.isSafe(chicken, veg))
        assertTrue(DietaryFilter.isSafe(lentils, veg))
        assertTrue(DietaryFilter.isSafe(yogurt, veg)) // dairy is fine for vegetarians
    }

    @Test
    fun `vegan requires the vegan tag, excluding dairy`() {
        val vegan = setOf(DietaryRestriction.VEGAN)
        assertFalse(DietaryFilter.isSafe(yogurt, vegan))
        assertFalse(DietaryFilter.isSafe(chicken, vegan))
        assertTrue(DietaryFilter.isSafe(lentils, vegan))
    }

    @Test
    fun `gluten-free requires the gluten-free tag`() {
        val gf = setOf(DietaryRestriction.GLUTEN_FREE)
        assertFalse(DietaryFilter.isSafe(wheatBread, gf))
        assertTrue(DietaryFilter.isSafe(lentils, gf))
    }

    @Test
    fun `dairy-free requires the dairy-free tag`() {
        val df = setOf(DietaryRestriction.DAIRY_FREE)
        assertFalse(DietaryFilter.isSafe(yogurt, df))
        assertTrue(DietaryFilter.isSafe(chicken, df)) // meat, but dairy-free
    }

    @Test
    fun `nut allergy excludes only foods flagged as containing nuts`() {
        val nut = setOf(DietaryRestriction.NUT_ALLERGY)
        assertFalse(DietaryFilter.isSafe(almonds, nut))
        // Not flagged contains_nuts -> safe, even for a nutty-sounding name.
        assertTrue(DietaryFilter.isSafe(food("Butternut squash", DietaryTag.VEGAN, DietaryTag.VEGETARIAN), nut))
    }

    @Test
    fun `a food with no tags is unsafe for any positive restriction`() {
        val untagged = food("Mystery casserole")
        assertFalse(DietaryFilter.isSafe(untagged, setOf(DietaryRestriction.VEGETARIAN)))
        assertFalse(DietaryFilter.isSafe(untagged, setOf(DietaryRestriction.GLUTEN_FREE)))
        // But a nut allergy is a presence flag: no flag -> treated as nut-free.
        assertTrue(DietaryFilter.isSafe(untagged, setOf(DietaryRestriction.NUT_ALLERGY)))
    }

    @Test
    fun `free-text restriction excludes by name substring`() {
        val shrimp = food("Shrimp stir fry", DietaryTag.GLUTEN_FREE, DietaryTag.DAIRY_FREE)
        assertFalse(DietaryFilter.isSafe(shrimp, emptySet(), other = "shrimp"))
        assertTrue(DietaryFilter.isSafe(lentils, emptySet(), other = "shrimp"))
        assertTrue(DietaryFilter.isSafe(lentils, emptySet(), other = "   "))
    }

    @Test
    fun `multiple restrictions all apply`() {
        val both = setOf(DietaryRestriction.VEGETARIAN, DietaryRestriction.NUT_ALLERGY)
        assertFalse(DietaryFilter.isSafe(chicken, both)) // not vegetarian
        assertFalse(DietaryFilter.isSafe(almonds, both)) // contains nuts
        assertTrue(DietaryFilter.isSafe(lentils, both))
    }
}
