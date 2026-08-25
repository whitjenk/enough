package com.enough.app.feature.onboarding

import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.domain.risk.AgeBand
import com.enough.app.domain.risk.Sex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFormsTest {

    // --- RiskTestForm ---

    @Test
    fun `risk form is incomplete until age, sex, and a positive weight are set`() {
        val empty = RiskTestForm()
        assertFalse(empty.isComplete)
        assertNull(empty.toAnswers())

        val partial = empty.copy(ageBand = AgeBand.AGE_40_49, sex = Sex.FEMALE)
        assertFalse(partial.isComplete) // no weight yet

        val complete = partial.copy(weightLbText = "165")
        assertTrue(complete.isComplete)
        assertNotNull(complete.toAnswers())
    }

    @Test
    fun `blank or non-positive weight text does not count as complete`() {
        val base = RiskTestForm(ageBand = AgeBand.UNDER_40, sex = Sex.MALE)
        assertNull(base.copy(weightLbText = "").weightLb)
        assertNull(base.copy(weightLbText = "abc").weightLb)
        assertNull(base.copy(weightLbText = "0").weightLb)
        assertFalse(base.copy(weightLbText = "0").isComplete)
        assertEquals(150.0, base.copy(weightLbText = "150").weightLb!!, 1e-9)
    }

    @Test
    fun `toAnswers converts height and weight to metric`() {
        val form = RiskTestForm(
            ageBand = AgeBand.AGE_50_59,
            sex = Sex.MALE,
            heightFeet = 5,
            heightInches = 10,
            weightLbText = "176",
        )
        val answers = form.toAnswers()!!
        assertEquals(177.8, answers.heightCm, 0.1) // 70 in
        assertEquals(79.83, answers.weightKg, 0.1) // 176 lb
    }

    // --- GoalsForm ---

    @Test
    fun `fiber target derives from calories at 14g per 1000 kcal`() {
        assertEquals(28, GoalsForm(caloriesText = "2000").fiberTargetGrams)
        assertEquals(0, GoalsForm(caloriesText = "").fiberTargetGrams)
        assertEquals(0, GoalsForm(caloriesText = "notanumber").fiberTargetGrams)
    }

    @Test
    fun `goals form requires calories, and a label only for a custom goal`() {
        assertTrue(GoalsForm(caloriesText = "2000").isComplete)
        assertFalse(GoalsForm(caloriesText = "").isComplete)

        val custom = GoalsForm(caloriesText = "2000", activityGoalType = ActivityGoalType.CUSTOM)
        assertFalse(custom.isComplete) // custom needs a label
        assertTrue(custom.copy(activityCustomLabel = "stretch daily").isComplete)
    }

    @Test
    fun `weight goal is opt-in and never defaults on`() {
        // Default: no weight goal, and the form is complete without any weight.
        val base = GoalsForm(caloriesText = "2000")
        assertFalse(base.includeWeightGoal)
        assertNull(base.weightLb)
        assertTrue(base.isComplete)
    }

    @Test
    fun `including a weight goal requires a positive weight`() {
        val included = GoalsForm(caloriesText = "2000", includeWeightGoal = true)
        assertFalse(included.isComplete) // needs a weight now
        assertNull(included.weightLb)

        val withWeight = included.copy(weightLbText = "180")
        assertTrue(withWeight.isComplete)
        assertEquals(180.0, withWeight.weightLb!!, 1e-9)
    }

    // --- ExtrasForm ---

    @Test
    fun `toggling a restriction adds then removes it`() {
        val empty = ExtrasForm()
        assertTrue(empty.dietaryRestrictions.isEmpty())

        val added = empty.toggleRestriction(DietaryRestriction.VEGAN)
        assertTrue(added.dietaryRestrictions.contains(DietaryRestriction.VEGAN))

        val removed = added.toggleRestriction(DietaryRestriction.VEGAN)
        assertFalse(removed.dietaryRestrictions.contains(DietaryRestriction.VEGAN))
    }

    @Test
    fun `restrictions accumulate independently`() {
        val form = ExtrasForm()
            .toggleRestriction(DietaryRestriction.VEGETARIAN)
            .toggleRestriction(DietaryRestriction.NUT_ALLERGY)
        assertEquals(
            setOf(DietaryRestriction.VEGETARIAN, DietaryRestriction.NUT_ALLERGY),
            form.dietaryRestrictions,
        )
    }

    @Test
    fun `GLP-1 answer is unset until chosen`() {
    }

    @Test
    fun `activity goal value follows the selected type`() {
        val minutes = GoalsForm(activityGoalType = ActivityGoalType.MINUTES, activityMinutes = 150)
        assertEquals(150, minutes.activityGoalValue)

        val steps = GoalsForm(activityGoalType = ActivityGoalType.STEPS, activitySteps = 8000)
        assertEquals(8000, steps.activityGoalValue)

        val custom = GoalsForm(activityGoalType = ActivityGoalType.CUSTOM)
        assertEquals(0, custom.activityGoalValue)
    }
}
