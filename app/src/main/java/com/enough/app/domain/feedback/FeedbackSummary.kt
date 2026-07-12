package com.enough.app.domain.feedback

import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.domain.progress.ProgressCalculations
import java.time.ZoneId

/**
 * An anonymous, aggregate-only snapshot the person can *choose* to share to help
 * improve the app (SPEC §7.5) — the constraint-legal replacement for the
 * server-side analytics this app deliberately can't have.
 *
 * It holds only counts: no timestamps, no logged foods, no "why", no weight
 * values, nothing that identifies anyone. That is enforced structurally — the
 * type has only [Int]/[Boolean] fields — so a leak can't sneak in through it.
 *
 * This is deliberately NOT the optional anonymous outcomes ping (§21): nothing is
 * ever sent automatically. The app builds this, shows the person exactly what it
 * contains, and only they can send it (via the OS share sheet). Pure and testable.
 */
data class FeedbackSummary(
    val daysLogged: Int,
    val mealsLogged: Int,
    val daysHitFiberTarget: Int,
    val fiberTargetG: Int,
    val hasWeightGoal: Boolean,
) {
    companion object {
        fun from(
            meals: List<MealWithFood>,
            goal: UserGoal?,
            zone: ZoneId,
        ): FeedbackSummary {
            val calibration = goal?.estimateCalibration ?: EstimateCalibration.BALANCED
            val target = goal?.fiberGramsTarget ?: 0
            val fiberByDay = ProgressCalculations.fiberByDay(meals, zone, calibration)
            return FeedbackSummary(
                daysLogged = fiberByDay.size,
                mealsLogged = meals.size,
                daysHitFiberTarget = if (target > 0) fiberByDay.count { it.value >= target } else 0,
                fiberTargetG = target,
                hasWeightGoal = goal?.hasWeightGoal == true,
            )
        }
    }
}
