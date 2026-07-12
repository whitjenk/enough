package com.enough.app.domain.nutrition

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.MealEntryType
import com.enough.app.data.model.MealSource
import com.enough.app.domain.rules.RulesEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MealNutritionTest {

    private fun meal(fiber: Double, carbs: Double, protein: Double, servings: Double): MealWithFood {
        val food = Food(id = 1, name = "Food", servingLabel = "1 cup", carbsG = carbs, fiberG = fiber, proteinG = protein)
        val entry = MealEntry(
            id = 1,
            foodId = 1,
            servingsMultiplier = servings,
            timestamp = Instant.EPOCH,
            source = MealSource.TEXT,
        )
        return MealWithFood(entry, food)
    }

    @Test
    fun `totals scale each meal by its serving multiplier and sum`() {
        val meals = listOf(
            meal(fiber = 7.5, carbs = 20.0, protein = 7.6, servings = 2.0), // black beans x2
            meal(fiber = 3.1, carbs = 27.0, protein = 1.3, servings = 1.0), // banana x1
        )
        val totals = MealNutrition.totals(meals)
        assertEquals(18.1, totals.fiberG, 1e-9)
        assertEquals(67.0, totals.carbsG, 1e-9)
        assertEquals(16.5, totals.proteinG, 1e-9)
        assertEquals(18, totals.fiberGRounded)
    }

    @Test
    fun `empty list totals to zero`() {
        val totals = MealNutrition.totals(emptyList())
        assertEquals(0.0, totals.fiberG, 1e-9)
        assertEquals(0, totals.fiberGRounded)
    }

    @Test
    fun `fractional servings are honored`() {
        assertEquals(4.9, MealNutrition.fiberGrams(listOf(meal(9.8, 12.0, 4.7, 0.5))), 1e-9)
    }

    @Test
    fun `a coarse category entry contributes a reasonable fiber estimate under each calibration`() {
        // A "veggie-heavy meal" quick-log: one serving of an 8g-fiber category
        // food. Its fiber flows through the same math as any entry, shifted by
        // calibration, so the day's total stays honest regardless of entry type.
        val coarse = MealWithFood(
            MealEntry(
                id = 2, foodId = 1, servingsMultiplier = 1.0, timestamp = Instant.EPOCH,
                source = MealSource.MANUAL, entryType = MealEntryType.COARSE_ESTIMATE,
            ),
            Food(id = 1, name = "Veggie-heavy meal", servingLabel = "1 meal", carbsG = 30.0, fiberG = 8.0, proteinG = 8.0, selectable = false),
        )
        val meals = listOf(coarse)
        assertEquals(6.8, MealNutrition.fiberGrams(meals, EstimateCalibration.LOW), 1e-9)
        assertEquals(8.0, MealNutrition.fiberGrams(meals, EstimateCalibration.BALANCED), 1e-9)
        assertEquals(9.2, MealNutrition.fiberGrams(meals, EstimateCalibration.HIGH), 1e-9)
    }

    @Test
    fun `calibration shifts logged fiber by plus or minus 15 percent`() {
        val meals = listOf(meal(fiber = 10.0, carbs = 20.0, protein = 5.0, servings = 2.0)) // 20g
        assertEquals(17.0, MealNutrition.fiberGrams(meals, EstimateCalibration.LOW), 1e-9)
        assertEquals(20.0, MealNutrition.fiberGrams(meals, EstimateCalibration.BALANCED), 1e-9)
        assertEquals(23.0, MealNutrition.fiberGrams(meals, EstimateCalibration.HIGH), 1e-9)
    }

    @Test
    fun `same logged day yields three ordered fiber-gap values across calibration`() {
        // A synthetic day of ~20g logged fiber against a 28g target.
        val meals = listOf(
            meal(fiber = 7.5, carbs = 20.0, protein = 7.6, servings = 2.0), // 15
            meal(fiber = 5.0, carbs = 15.0, protein = 1.5, servings = 1.0), // 5
        )
        val target = 28

        val lowGap = RulesEngine.fiberGapG(MealNutrition.fiberGrams(meals, EstimateCalibration.LOW), target)
        val balancedGap = RulesEngine.fiberGapG(MealNutrition.fiberGrams(meals, EstimateCalibration.BALANCED), target)
        val highGap = RulesEngine.fiberGapG(MealNutrition.fiberGrams(meals, EstimateCalibration.HIGH), target)

        // Leaning the estimate low leaves a bigger remaining gap; high, a smaller one.
        assertTrue(lowGap > balancedGap)
        assertTrue(balancedGap > highGap)
        assertEquals(11.0, lowGap, 1e-9) // 28 - 17
        assertEquals(8.0, balancedGap, 1e-9) // 28 - 20
        assertEquals(5.0, highGap, 1e-9) // 28 - 23
    }
}
