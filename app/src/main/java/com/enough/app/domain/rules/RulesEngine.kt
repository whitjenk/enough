package com.enough.app.domain.rules

import com.enough.app.data.model.WeightTrendDirection
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure rules-engine calculations. No Room/Android/Compose dependency — this is
 * where the fiber-gap, recency, and weight-trend logic lives so it is directly
 * unit-tested and the ViewModel only orchestrates. Phase 0 uses fiber only.
 */
object RulesEngine {

    /** How much weight change (kg) counts as a real trend rather than noise. */
    const val WEIGHT_TREND_DEADBAND_KG = 0.3

    /** Remaining fiber to reach the day's target; never negative. */
    fun fiberGapG(fiberSoFarG: Double, targetG: Int): Double =
        (targetG - fiberSoFarG).coerceAtLeast(0.0)

    /**
     * Whole calendar days between the last log and today, in the device zone.
     * Null when nothing has ever been logged. 0 means logged today.
     */
    fun daysSinceLastLog(lastLog: Instant?, now: Instant, zone: ZoneId): Int? {
        if (lastLog == null) return null
        val lastDate = lastLog.atZone(zone).toLocalDate()
        val today = now.atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(lastDate, today).toInt().coerceAtLeast(0)
    }

    /**
     * Weight-trend direction from a chronologically-ordered list of weights.
     * Compares the earliest and latest in the provided window; a small deadband
     * avoids calling normal fluctuation a trend. Fewer than two points is
     * [WeightTrendDirection.UNKNOWN]. Down is neutral/positive here and is never
     * rendered in red (DESIGN.md).
     */
    fun weightTrend(weightsChronological: List<Double>): WeightTrendDirection {
        if (weightsChronological.size < 2) return WeightTrendDirection.UNKNOWN
        val delta = weightsChronological.last() - weightsChronological.first()
        return when {
            delta <= -WEIGHT_TREND_DEADBAND_KG -> WeightTrendDirection.DOWN
            delta >= WEIGHT_TREND_DEADBAND_KG -> WeightTrendDirection.UP
            else -> WeightTrendDirection.FLAT
        }
    }
}
