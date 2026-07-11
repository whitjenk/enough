package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.enough.app.data.model.ActivityGoalType
import java.time.Instant

/**
 * The person's three Phase 0 goals. Single-row table (always [SINGLETON_ID]).
 *
 * - Weight: a 5–7% loss target computed from a starting weight.
 * - Activity: a weekly goal whose type may be steps, minutes, or a custom
 *   self-described goal (SPEC.md §5 accessibility requirement).
 * - Fiber: a daily gram target, 14g per 1,000 self-reported daily calories.
 */
@Entity(tableName = "user_goal")
data class UserGoal(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val startWeightKg: Double,
    val targetWeightKg: Double,
    val weightLossPercent: Double,
    val activityGoalType: ActivityGoalType,
    /** Weekly minutes, daily steps, or a custom count, per [activityGoalType]. */
    val activityGoalValue: Int,
    /** Self-description shown for a CUSTOM activity goal; null otherwise. */
    val activityGoalCustomLabel: String?,
    val dailyCalorieEstimate: Int,
    val fiberGramsTarget: Int,
    val createdAt: Instant,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
