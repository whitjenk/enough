package com.enough.app.domain.risk

/** Age bands from the CDC/ADA Prediabetes Risk Test, with their point values. */
enum class AgeBand(val points: Int) {
    UNDER_40(0),
    AGE_40_49(1),
    AGE_50_59(2),
    AGE_60_PLUS(3),
}

/**
 * Sex as scored by the risk test. The test awards 1 point for "man"; women can
 * instead earn a point via the gestational-diabetes question. Named [Sex] rather
 * than gender because that is the variable the published test scores.
 */
enum class Sex { MALE, FEMALE }

/**
 * A completed set of answers to the CDC/ADA Prediabetes Risk Test. Height and
 * weight feed the weight-category point (via BMI) rather than making the person
 * self-classify from a chart.
 */
data class RiskTestAnswers(
    val ageBand: AgeBand,
    val sex: Sex,
    /** Only scored when [sex] is [Sex.FEMALE]; ignored otherwise. */
    val hadGestationalDiabetes: Boolean,
    val familyHistoryDiabetes: Boolean,
    val highBloodPressure: Boolean,
    val physicallyActive: Boolean,
    val heightCm: Double,
    val weightKg: Double,
)
