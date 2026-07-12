package com.enough.app.domain

import kotlin.math.pow

/**
 * Pure unit conversions. The app stores metric (kg, cm) internally, but the
 * CDC/ADA risk test and most US testers think in pounds and feet/inches, so the
 * onboarding UI collects imperial and converts here. Kept dependency-free and
 * unit-tested.
 */
object UnitConversions {
    const val KG_PER_LB = 0.45359237
    const val CM_PER_INCH = 2.54

    fun lbToKg(pounds: Double): Double = pounds * KG_PER_LB

    fun kgToLb(kg: Double): Double = kg / KG_PER_LB

    fun feetInchesToCm(feet: Int, inches: Int): Double = (feet * 12 + inches) * CM_PER_INCH

    /** Body Mass Index from metric inputs. Returns 0.0 for a non-positive height. */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0.0) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / heightM.pow(2)
    }
}
