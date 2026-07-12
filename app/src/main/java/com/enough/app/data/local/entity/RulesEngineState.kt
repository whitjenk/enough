package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.enough.app.data.model.NudgeType
import com.enough.app.data.model.WeightTrendDirection
import java.time.Instant

/**
 * A persisted snapshot of the inputs and last output of the rules engine.
 * Single-row table ([SINGLETON_ID]). The engine's calculations themselves live
 * in plain-Kotlin classes (testable, no Room/Android deps); this only caches the
 * latest result so the Today screen and daily nudge don't recompute from scratch.
 */
@Entity(tableName = "rules_engine_state")
data class RulesEngineState(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val daysSinceLastLog: Int,
    val weightTrendDirection: WeightTrendDirection,
    val activityMinutesThisWeek: Int,
    val fiberGapToday: Double,
    val lastNudgeType: NudgeType,
    val updatedAt: Instant,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
