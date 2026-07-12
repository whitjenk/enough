package com.enough.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The half-open instant range [start, endExclusive) covering a calendar day in a
 * given time zone. "Today" is a calendar day in the device's zone, not a rolling
 * 24 hours, so meals logged late at night still count for the right day.
 */
data class DayRange(val start: Instant, val endExclusive: Instant) {
    val startMillis: Long get() = start.toEpochMilli()
    val endMillis: Long get() = endExclusive.toEpochMilli()

    companion object {
        fun of(date: LocalDate, zone: ZoneId): DayRange {
            val start = date.atStartOfDay(zone).toInstant()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant()
            return DayRange(start, end)
        }

        fun today(zone: ZoneId, now: Instant): DayRange =
            of(now.atZone(zone).toLocalDate(), zone)
    }
}
