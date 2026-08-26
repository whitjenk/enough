package com.enough.app.domain.theme

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeOfDayTest {

    private fun at(hour: Int, minute: Int = 0) = TimeOfDay.at(LocalTime.of(hour, minute))

    @Test
    fun `each band covers its whole range`() {
        (5..10).forEach { assertEquals("hour $it", TimeOfDay.MORNING, at(it)) }
        (11..16).forEach { assertEquals("hour $it", TimeOfDay.DAY, at(it)) }
        (17..20).forEach { assertEquals("hour $it", TimeOfDay.EVENING, at(it)) }
        (listOf(21, 22, 23) + (0..4)).forEach { assertEquals("hour $it", TimeOfDay.NIGHT, at(it)) }
    }

    @Test
    fun `boundaries flip on the hour, not before it`() {
        assertEquals(TimeOfDay.NIGHT, at(4, 59))
        assertEquals(TimeOfDay.MORNING, at(5, 0))
        assertEquals(TimeOfDay.MORNING, at(10, 59))
        assertEquals(TimeOfDay.DAY, at(11, 0))
        assertEquals(TimeOfDay.DAY, at(16, 59))
        assertEquals(TimeOfDay.EVENING, at(17, 0))
        assertEquals(TimeOfDay.EVENING, at(20, 59))
        assertEquals(TimeOfDay.NIGHT, at(21, 0))
    }

    @Test
    fun `midnight and noon land where a person would expect`() {
        assertEquals(TimeOfDay.NIGHT, at(0, 0))
        assertEquals(TimeOfDay.DAY, at(12, 0))
    }

    @Test
    fun `every hour of the day maps to exactly one band`() {
        val covered = (0..23).map { at(it) }
        assertEquals(24, covered.size)
        // All four bands are reachable — no dead enum value, no gap.
        assertEquals(TimeOfDay.entries.toSet(), covered.toSet())
    }
}
