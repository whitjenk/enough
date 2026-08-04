package com.enough.app.domain.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCardTest {

    private val week = listOf(0.0, 20.0, 24.0, 0.0, 28.0, 16.0, 22.0)

    @Test
    fun `averages fiber over the days that had fiber, not the whole window`() {
        val data = ShareCard.build(week, daysLogged = 5, windowDays = 7, hideNumbers = false)
        assertTrue(data.hasData)
        // (20+24+28+16+22)/5 = 22
        assertEquals(22, data.averageFiberG)
        assertEquals(5, data.daysLogged)
    }

    @Test
    fun `hide-numbers mode exposes no numeric value at all`() {
        val data = ShareCard.build(week, daysLogged = 5, windowDays = 7, hideNumbers = true)
        // The card must be able to leak nothing numeric — no average, no day count.
        assertNull(data.averageFiberG)
        assertNull(data.daysLogged)
        assertTrue(data.hideNumbers)
    }

    @Test
    fun `an empty week has no data and no average`() {
        val data = ShareCard.build(List(7) { 0.0 }, daysLogged = 0, windowDays = 7, hideNumbers = false)
        assertFalse(data.hasData)
        assertNull(data.averageFiberG)
    }
}
