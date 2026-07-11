package com.enough.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DayRangeTest {

    private val zone = ZoneId.of("America/New_York")

    @Test
    fun `of covers a full calendar day in the given zone`() {
        val date = LocalDate.of(2026, 7, 11)
        val range = DayRange.of(date, zone)

        assertEquals(date.atStartOfDay(zone).toInstant(), range.start)
        assertEquals(date.plusDays(1).atStartOfDay(zone).toInstant(), range.endExclusive)
        assertEquals(Duration.ofHours(24), Duration.between(range.start, range.endExclusive))
    }

    @Test
    fun `today maps a late-night instant to that calendar day, not a rolling 24h`() {
        val lateNight = LocalDate.of(2026, 7, 11)
            .atTime(LocalTime.of(23, 45))
            .atZone(zone)
            .toInstant()

        val range = DayRange.today(zone, lateNight)

        assertEquals(DayRange.of(LocalDate.of(2026, 7, 11), zone), range)
    }
}
