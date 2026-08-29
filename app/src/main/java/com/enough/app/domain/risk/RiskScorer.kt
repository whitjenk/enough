package com.enough.app.domain.risk

import com.enough.app.domain.UnitConversions
import kotlin.math.floor
import kotlin.math.roundToInt

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
 * The weight category comes from [CHART_LB], the published height/weight table
 * transcribed row for row. It is not a clean function of any single BMI formula:
 * `floor(bmiCut × inches² / 703)` reproduces 13 of its 19 rows and is off by a
 * pound on the other six (4'11", 5'4", 5'6", 5'8", 6'1", 6'3"), so the table is
 * the source of truth and the formula is only a fallback for heights the chart
 * doesn't cover. Note: the test advises that Asian Americans are at higher risk
 * at ~15 lb lower; that is surfaced as guidance in the UI and does not change
 * this numeric scoring.
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
     * Tolerance for the whole-pound chart boundaries. Weight is stored in
     * kilograms, so a weight entered as exactly 232 lb comes back as 231.999…
     * and would otherwise fall on the wrong side of its own boundary.
     */
    private const val POUND_EPSILON = 1e-6

    /** Shortest and tallest heights the published chart covers: 4'10" and 6'4". */
    const val CHART_MIN_INCHES = 58
    const val CHART_MAX_INCHES = 76

    /**
     * The published height/weight chart, indexed by whole inches of height. Each
     * entry is the lowest weight in pounds that earns 1, 2, and 3 points; below
     * the first is 0 points. Transcribed from the ADA/CDC/Ad Council risk test
     * sheet — the chart prints closed ranges (4'10": 119–142, 143–190, 191+), and
     * every range ends one pound below the next one starts, so the three lower
     * bounds carry the whole table.
     */
    private val CHART_LB: Map<Int, Triple<Int, Int, Int>> = mapOf(
        58 to Triple(119, 143, 191), // 4'10"
        59 to Triple(124, 148, 198), // 4'11"
        60 to Triple(128, 153, 204), // 5'0"
        61 to Triple(132, 158, 211), // 5'1"
        62 to Triple(136, 164, 218), // 5'2"
        63 to Triple(141, 169, 225), // 5'3"
        64 to Triple(145, 174, 232), // 5'4"
        65 to Triple(150, 180, 240), // 5'5"
        66 to Triple(155, 186, 247), // 5'6"
        67 to Triple(159, 191, 255), // 5'7"
        68 to Triple(164, 197, 262), // 5'8"
        69 to Triple(169, 203, 270), // 5'9"
        70 to Triple(174, 209, 278), // 5'10"
        71 to Triple(179, 215, 286), // 5'11"
        72 to Triple(184, 221, 294), // 6'0"
        73 to Triple(189, 227, 302), // 6'1"
        74 to Triple(194, 233, 311), // 6'2"
        75 to Triple(200, 240, 319), // 6'3"
        76 to Triple(205, 246, 328), // 6'4"
    )

    /**
     * Weight-category points (0–3) from height and weight.
     *
     * Heights the chart covers are read straight off it, rounded to the nearest
     * whole inch — the same row a person would pick up from the printed sheet.
     * Outside 4'10"–6'4" the chart simply has no row, so [bmiThresholdLb]
     * extrapolates; that is our own extension of the test, not the published
     * instrument.
     */
    fun weightPoints(weightKg: Double, heightCm: Double): Int {
        val inches = heightCm / UnitConversions.CM_PER_INCH
        if (inches <= 0.0) return 0
        val pounds = UnitConversions.kgToLb(weightKg)
        val row = CHART_LB[inches.roundToInt()]
        val (onePoint, twoPoints, threePoints) = row
            ?: Triple(
                bmiThresholdLb(BMI_1_POINT, inches),
                bmiThresholdLb(BMI_2_POINTS, inches),
                bmiThresholdLb(BMI_3_POINTS, inches),
            )
        return when {
            pounds >= threePoints - POUND_EPSILON -> 3
            pounds >= twoPoints - POUND_EPSILON -> 2
            pounds >= onePoint - POUND_EPSILON -> 1
            else -> 0
        }
    }

    /**
     * The lowest whole pound reaching [bmi] at [inches] of height. Used only for
     * heights outside the published chart — see [weightPoints].
     */
    private fun bmiThresholdLb(bmi: Double, inches: Double): Int =
        floor(bmi * inches * inches / IMPERIAL_BMI_CONSTANT).toInt()

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
