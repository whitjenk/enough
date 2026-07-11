package com.enough.app.domain.rules

import com.enough.app.data.model.WeightTrendDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RulesEngineTest {

    private val zone = ZoneId.of("America/New_York")

    @Test
    fun `fiber gap is target minus logged, floored at zero`() {
        assertEquals(16.0, RulesEngine.fiberGapG(12.0, 28), 1e-9)
        assertEquals(0.0, RulesEngine.fiberGapG(30.0, 28), 1e-9) // over target -> no gap
        assertEquals(28.0, RulesEngine.fiberGapG(0.0, 28), 1e-9)
    }

    @Test
    fun `days since last log counts calendar days in zone`() {
        val now = LocalDate.of(2026, 7, 11).atTime(9, 0).atZone(zone).toInstant()
        val yesterday = LocalDate.of(2026, 7, 10).atTime(20, 0).atZone(zone).toInstant()
        val earlierToday = LocalDate.of(2026, 7, 11).atTime(1, 0).atZone(zone).toInstant()

        assertEquals(1, RulesEngine.daysSinceLastLog(yesterday, now, zone))
        assertEquals(0, RulesEngine.daysSinceLastLog(earlierToday, now, zone))
        assertNull(RulesEngine.daysSinceLastLog(null, now, zone))
    }

    @Test
    fun `weight trend needs two points and respects the deadband`() {
        assertEquals(WeightTrendDirection.UNKNOWN, RulesEngine.weightTrend(emptyList()))
        assertEquals(WeightTrendDirection.UNKNOWN, RulesEngine.weightTrend(listOf(82.0)))
        assertEquals(WeightTrendDirection.DOWN, RulesEngine.weightTrend(listOf(82.0, 80.5)))
        assertEquals(WeightTrendDirection.UP, RulesEngine.weightTrend(listOf(80.0, 81.0)))
        // Within the 0.3kg deadband -> flat, not a trend.
        assertEquals(WeightTrendDirection.FLAT, RulesEngine.weightTrend(listOf(82.0, 82.1)))
    }
}
