package com.enough.app.feature.onboarding

import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.domain.UnitConversions
import com.enough.app.domain.goals.GoalCalculator
import com.enough.app.domain.risk.AgeBand
import com.enough.app.domain.risk.RiskScore
import com.enough.app.domain.risk.RiskTestAnswers
import com.enough.app.domain.risk.Sex
import com.enough.app.health.HealthConnectAvailability

/**
 * The steps of the onboarding flow. WELCOME is an entry choice: the default
 * "build better habits" path goes straight to GOALS; the optional "curious about
 * your risk" path visits RISK_TEST/RISK_RESULT first. Both converge on GOALS ->
 * HEALTH_CONNECT (SPEC §3).
 */
enum class OnboardingStep {
    WELCOME,
    RISK_TEST,
    RISK_RESULT,
    GOALS,
    EXTRAS,
    HEALTH_CONNECT,
}

/**
 * In-progress answers to the risk test. Numeric weight is kept as raw text so
 * the field can be empty mid-typing; [weightLb] and [isComplete] derive the
 * validated view.
 */
data class RiskTestForm(
    val ageBand: AgeBand? = null,
    val sex: Sex? = null,
    val hadGestationalDiabetes: Boolean = false,
    val familyHistoryDiabetes: Boolean = false,
    val highBloodPressure: Boolean = false,
    val physicallyActive: Boolean = true,
    val heightFeet: Int = 5,
    val heightInches: Int = 6,
    val weightLbText: String = "",
) {
    val weightLb: Double? = weightLbText.trim().toDoubleOrNull()?.takeIf { it > 0 }

    val isComplete: Boolean
        get() = ageBand != null && sex != null && weightLb != null

    /** Convert to scorer input, or null if the form isn't complete yet. */
    fun toAnswers(): RiskTestAnswers? {
        val age = ageBand ?: return null
        val s = sex ?: return null
        val lb = weightLb ?: return null
        return RiskTestAnswers(
            ageBand = age,
            sex = s,
            hadGestationalDiabetes = hadGestationalDiabetes,
            familyHistoryDiabetes = familyHistoryDiabetes,
            highBloodPressure = highBloodPressure,
            physicallyActive = physicallyActive,
            heightCm = UnitConversions.feetInchesToCm(heightFeet, heightInches),
            weightKg = UnitConversions.lbToKg(lb),
        )
    }
}

/**
 * In-progress goal choices. Fiber target is derived, never entered directly.
 * The weight goal is opt-in ([includeWeightGoal], never defaulted on); when
 * opted in, [weightLbText] is the current weight it's computed from (prefilled
 * from the risk test if that path was taken).
 */
data class GoalsForm(
    val includeWeightGoal: Boolean = false,
    val weightLbText: String = "",
    val weightLossPercent: Int = GoalCalculator.DEFAULT_WEIGHT_LOSS_PERCENT.toInt(),
    val activityGoalType: ActivityGoalType = ActivityGoalType.MINUTES,
    val activityMinutes: Int = GoalCalculator.DEFAULT_WEEKLY_ACTIVITY_MINUTES,
    val activitySteps: Int = 7_000,
    val activityCustomLabel: String = "",
    val caloriesText: String = "2000",
) {
    val dailyCalories: Int? = caloriesText.trim().toIntOrNull()?.takeIf { it > 0 }

    val fiberTargetGrams: Int = dailyCalories?.let { GoalCalculator.fiberTargetGrams(it) } ?: 0

    /** Current weight in pounds, when a weight goal is included; else null. */
    val weightLb: Double? = weightLbText.trim().toDoubleOrNull()?.takeIf { it > 0 }

    val isComplete: Boolean
        get() = dailyCalories != null &&
            (activityGoalType != ActivityGoalType.CUSTOM || activityCustomLabel.isNotBlank()) &&
            (!includeWeightGoal || weightLb != null)

    /** The activity value that will be stored, interpreted by [activityGoalType]. */
    val activityGoalValue: Int
        get() = when (activityGoalType) {
            ActivityGoalType.MINUTES -> activityMinutes
            ActivityGoalType.STEPS -> activitySteps
            ActivityGoalType.CUSTOM -> 0
        }
}

/**
 * Optional extra questions gathered after goals (SPEC §3). Everything here is
 * skippable — the step is always completable regardless of what's filled in.
 * [takesGLP1] is null until the person explicitly answers.
 */
data class ExtrasForm(
    val dietaryRestrictions: Set<DietaryRestriction> = emptySet(),
    val dietaryOther: String = "",
    val personalWhy: String = "",
    val takesGLP1: Boolean? = null,
) {
    /** Toggle a restriction on/off, returning the updated form. */
    fun toggleRestriction(restriction: DietaryRestriction): ExtrasForm {
        val next = if (restriction in dietaryRestrictions) {
            dietaryRestrictions - restriction
        } else {
            dietaryRestrictions + restriction
        }
        return copy(dietaryRestrictions = next)
    }
}

/** Health Connect availability + whether the user has granted our read permissions. */
data class HealthConnectUiState(
    val availability: HealthConnectAvailability = HealthConnectAvailability.NOT_SUPPORTED,
    val permissionsGranted: Boolean = false,
)

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    /** True if the optional risk-test path was chosen at the entry screen. */
    val riskTestPathChosen: Boolean = false,
    val riskForm: RiskTestForm = RiskTestForm(),
    val riskScore: RiskScore? = null,
    val goalsForm: GoalsForm = GoalsForm(),
    val extrasForm: ExtrasForm = ExtrasForm(),
    val healthConnect: HealthConnectUiState = HealthConnectUiState(),
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
)
