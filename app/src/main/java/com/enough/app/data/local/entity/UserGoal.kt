package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.data.model.EstimateCalibration
import java.time.Instant

/**
 * The person's goals and onboarding preferences. Single-row table (always
 * [SINGLETON_ID]).
 *
 * - Weight: an **optional** 5–7% loss target. Null weight fields mean the person
 *   chose to focus on fiber and activity only — never defaulted on (SPEC §3).
 * - Activity: a weekly goal whose type may be steps, minutes, or a custom
 *   self-described goal (SPEC §5 accessibility requirement).
 * - Fiber: a daily gram target, 14g per 1,000 self-reported daily calories.
 * - Dietary restrictions, "personal why", GLP-1 use, and estimate calibration
 *   are captured in onboarding/settings and consumed by later tasks.
 */
@Entity(tableName = "user_goal")
data class UserGoal(
    @PrimaryKey val id: Int = SINGLETON_ID,
    /** Starting weight in kg, or null when there is no weight goal. */
    val startWeightKg: Double?,
    /** Target weight in kg, or null when there is no weight goal. */
    val targetWeightKg: Double?,
    /** Chosen 5–7% loss, or null when there is no weight goal. */
    val weightLossPercent: Double?,
    val activityGoalType: ActivityGoalType,
    /** Weekly minutes, daily steps, or a custom count, per [activityGoalType]. */
    val activityGoalValue: Int,
    /** Self-description shown for a CUSTOM activity goal; null otherwise. */
    val activityGoalCustomLabel: String?,
    val dailyCalorieEstimate: Int,
    val fiberGramsTarget: Int,
    val createdAt: Instant,
    val dietaryRestrictions: Set<DietaryRestriction> = emptySet(),
    /** Free-text "other" restriction/allergy; null when none given. */
    val dietaryRestrictionOther: String? = null,
    /** Optional free-text answer to "what's making you want to do this?". */
    val personalWhy: String? = null,
    val takesGLP1Medication: Boolean = false,
    val estimateCalibration: EstimateCalibration = EstimateCalibration.BALANCED,
) {
    /** True when the person opted into a weight goal (all weight fields set). */
    val hasWeightGoal: Boolean
        get() = startWeightKg != null && targetWeightKg != null && weightLossPercent != null

    companion object {
        const val SINGLETON_ID = 1
    }
}
