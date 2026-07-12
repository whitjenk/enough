package com.enough.app.domain.risk

import com.enough.app.domain.UnitConversions
import kotlin.math.floor

/** The outcome of scoring the risk test: a total out of 10 and a risk flag. */
data class RiskScore(
    val total: Int,
    val isHighRisk: Boolean,
)

/**
 * Scores the CDC/ADA Prediabetes Risk Test (public domain, 7 questions, no blood
 * draw — SPEC.md §0.5).
 *
 * Point values follow the published test: age 0–3; man +1; gestational diabetes
 * +1 (women only); family history +1; high blood pressure +1; not physically
 * active +1; weight category 0–3. A total of [HIGH_RISK_THRESHOLD] or more (of a
 * maximum of 10) indicates elevated risk.
 *
 * The published weight chart is reproduced exactly: for a given height it floors
 * the BMI-threshold weight to whole pounds, so the point boundaries are
 * `floor(bmiCut × inches² / 703)`. Verified against official rows (4'10":
 * 119/143/191 lb; 5'0": 128/153/204 lb → 1/2/3 points). Computing it this way
 * generalizes to any height while matching the chart to the pound. Note: the
 * test advises that Asian Americans are at higher risk at ~15 lb lower; that is
 * surfaced as guidance in the UI and does not change this numeric scoring.
 */
object RiskScorer {
    const val HIGH_RISK_THRESHOLD = 5
    const val MAX_SCORE = 10

    const val BMI_1_POINT = 25.0
    const val BMI_2_POINTS = 30.0
    const val BMI_3_POINTS = 40.0

    /** The imperial BMI constant: BMI = 703 × pounds / inches². */
    private const val IMPERIAL_BMI_CONSTANT = 703.0

    /**
     * Weight-category points (0–3) from height and weight, matching the published
     * height/weight chart to the pound.
     */
    fun weightPoints(weightKg: Double, heightCm: Double): Int {
        val inches = heightCm / UnitConversions.CM_PER_INCH
        if (inches <= 0.0) return 0
        val pounds = UnitConversions.kgToLb(weightKg)
        val inchesSquared = inches * inches
        fun thresholdLb(bmi: Double): Double = floor(bmi * inchesSquared / IMPERIAL_BMI_CONSTANT)
        return when {
            pounds >= thresholdLb(BMI_3_POINTS) -> 3
            pounds >= thresholdLb(BMI_2_POINTS) -> 2
            pounds >= thresholdLb(BMI_1_POINT) -> 1
            else -> 0
        }
    }

    fun score(answers: RiskTestAnswers): RiskScore {
        var total = answers.ageBand.points
        if (answers.sex == Sex.MALE) total += 1
        if (answers.sex == Sex.FEMALE && answers.hadGestationalDiabetes) total += 1
        if (answers.familyHistoryDiabetes) total += 1
        if (answers.highBloodPressure) total += 1
        if (!answers.physicallyActive) total += 1
        total += weightPoints(answers.weightKg, answers.heightCm)

        return RiskScore(total = total, isHighRisk = total >= HIGH_RISK_THRESHOLD)
    }
}
