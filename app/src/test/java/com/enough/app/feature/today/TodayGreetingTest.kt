package com.enough.app.feature.today

import com.enough.app.domain.theme.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The greeting mapping (§7.10 B2). A band quietly falling back to another band's
 * line would make the whole time-aware greeting decorative without failing any
 * render test, so the mapping is pinned directly.
 */
class TodayGreetingTest {

    @Test
    fun `every time of day has its own greeting`() {
        val byBand = TimeOfDay.entries.associateWith { greetingRes(it) }
        assertEquals("all four bands should map to distinct copy", 4, byBand.values.toSet().size)
    }

    @Test
    fun `every time of day maps to something`() {
        TimeOfDay.entries.forEach { band ->
            assertEquals("band $band should resolve", true, greetingRes(band) != 0)
        }
    }
}
