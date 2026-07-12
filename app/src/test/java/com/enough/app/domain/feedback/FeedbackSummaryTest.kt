package com.enough.app.domain.feedback

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.MealSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class FeedbackSummaryTest {

    private val zone = ZoneId.of("UTC")

    private fun meal(fiber: Double, daysAgo: Long): MealWithFood {
        val food = Food(id = 1, name = "Lentils", servingLabel = "1/2 cup", carbsG = 20.0, fiberG = fiber, proteinG = 9.0)
        val ts = Instant.EPOCH.plus(10, ChronoUnit.DAYS).minus(daysAgo, ChronoUnit.DAYS)
        return MealWithFood(MealEntry(foodId = 1, servingsMultiplier = 1.0, timestamp = ts, source = MealSource.TEXT), food)
    }

    private fun goal(fiberTarget: Int, weightGoal: Boolean) = UserGoal(
        startWeightKg = if (weightGoal) 82.0 else null,
        targetWeightKg = if (weightGoal) 78.0 else null,
        weightLossPercent = if (weightGoal) 5.0 else null,
        activityGoalType = ActivityGoalType.MINUTES,
        activityGoalValue = 150,
        activityGoalCustomLabel = null,
        dailyCalorieEstimate = 2000,
        fiberGramsTarget = fiberTarget,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `aggregates distinct days, meals, and target-hit days`() {
        val meals = listOf(
            meal(fiber = 20.0, daysAgo = 2), // day A: 20g (hits 15 target)
            meal(fiber = 5.0, daysAgo = 1),  // day B: two meals, 5 + 8 = 13g (misses)
            meal(fiber = 8.0, daysAgo = 1),
        )
        val summary = FeedbackSummary.from(meals, goal(fiberTarget = 15, weightGoal = false), zone)

        assertEquals(2, summary.daysLogged)
        assertEquals(3, summary.mealsLogged)
        assertEquals(1, summary.daysHitFiberTarget) // only day A cleared 15g
        assertEquals(15, summary.fiberTargetG)
        assertEquals(false, summary.hasWeightGoal)
    }

    @Test
    fun `reflects a set weight goal`() {
        val summary = FeedbackSummary.from(emptyList(), goal(fiberTarget = 28, weightGoal = true), zone)
        assertEquals(0, summary.daysLogged)
        assertEquals(0, summary.mealsLogged)
        assertEquals(0, summary.daysHitFiberTarget)
        assertEquals(true, summary.hasWeightGoal)
    }

    @Test
    fun `no goal yields zero target-hit days rather than dividing by a zero target`() {
        val summary = FeedbackSummary.from(listOf(meal(10.0, 0)), goal = null, zone = zone)
        assertEquals(1, summary.daysLogged)
        assertEquals(0, summary.daysHitFiberTarget)
        assertEquals(0, summary.fiberTargetG)
    }
}
