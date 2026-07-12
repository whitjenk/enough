package com.enough.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConversionsTest {

    @Test
    fun `pounds convert to kilograms`() {
        assertEquals(45.359237, UnitConversions.lbToKg(100.0), 1e-6)
    }

    @Test
    fun `feet and inches convert to centimeters`() {
        assertEquals(152.4, UnitConversions.feetInchesToCm(5, 0), 1e-6)
        assertEquals(180.34, UnitConversions.feetInchesToCm(5, 11), 1e-6)
    }

    @Test
    fun `bmi matches the known chart anchor points`() {
        // 5'0" at 128 lb is the 1-point floor (BMI ~25) on the official chart.
        val heightCm = UnitConversions.feetInchesToCm(5, 0)
        assertEquals(25.0, UnitConversions.bmi(UnitConversions.lbToKg(128.0), heightCm), 0.15)
    }

    @Test
    fun `bmi guards against non-positive height`() {
        assertEquals(0.0, UnitConversions.bmi(70.0, 0.0), 1e-9)
    }
}
