package com.enough.app.domain.goals

import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCalculatorTest {

    @Test
    fun `target weight applies the chosen percent`() {
        assertEquals(95.0, GoalCalculator.targetWeightKg(100.0, 5.0), 1e-9)
        assertEquals(93.0, GoalCalculator.targetWeightKg(100.0, 7.0), 1e-9)
    }

    @Test
    fun `weight loss percent is clamped to the 5 to 7 band`() {
        // Below the band clamps up to 5%, above clamps down to 7%.
        assertEquals(95.0, GoalCalculator.targetWeightKg(100.0, 2.0), 1e-9)
        assertEquals(93.0, GoalCalculator.targetWeightKg(100.0, 15.0), 1e-9)
    }

    @Test
    fun `fiber target is 14g per 1000 kcal rounded to the nearest gram`() {
        assertEquals(28, GoalCalculator.fiberTargetGrams(2000))
        assertEquals(21, GoalCalculator.fiberTargetGrams(1500))
        assertEquals(34, GoalCalculator.fiberTargetGrams(2400)) // 33.6 -> 34
        assertEquals(25, GoalCalculator.fiberTargetGrams(1800)) // 25.2 -> 25
    }

    @Test
    fun `non-positive calories yield a zero fiber target`() {
        assertEquals(0, GoalCalculator.fiberTargetGrams(0))
        assertEquals(0, GoalCalculator.fiberTargetGrams(-500))
    }
}
