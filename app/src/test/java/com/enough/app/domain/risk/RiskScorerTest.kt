package com.enough.app.domain.risk

import com.enough.app.domain.UnitConversions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerTest {

    /** A deliberately low-risk baseline: young, active, normal weight, no history. */
    private fun lowRiskAnswers() = RiskTestAnswers(
        ageBand = AgeBand.UNDER_40,
        sex = Sex.FEMALE,
        hadGestationalDiabetes = false,
        familyHistoryDiabetes = false,
        highBloodPressure = false,
        physicallyActive = true,
        heightCm = UnitConversions.feetInchesToCm(5, 6),
        weightKg = UnitConversions.lbToKg(140.0), // BMI ~22.6
    )

    private fun weightPointsAt(feet: Int, inches: Int, pounds: Double): Int =
        RiskScorer.weightPoints(
            weightKg = UnitConversions.lbToKg(pounds),
            heightCm = UnitConversions.feetInchesToCm(feet, inches),
        )

    /**
     * Every row of the published chart, as printed: height in inches to the
     * lowest pound scoring 1, 2, and 3 points. Six of these rows disagree with
     * `floor(bmi x inches^2 / 703)` by a pound, which is why the whole table is
     * pinned here rather than two sample rows.
     */
    private val publishedChart = listOf(
        Triple(58, Triple(119, 143, 191), "4'10\""),
        Triple(59, Triple(124, 148, 198), "4'11\""),
        Triple(60, Triple(128, 153, 204), "5'0\""),
        Triple(61, Triple(132, 158, 211), "5'1\""),
        Triple(62, Triple(136, 164, 218), "5'2\""),
        Triple(63, Triple(141, 169, 225), "5'3\""),
        Triple(64, Triple(145, 174, 232), "5'4\""),
        Triple(65, Triple(150, 180, 240), "5'5\""),
        Triple(66, Triple(155, 186, 247), "5'6\""),
        Triple(67, Triple(159, 191, 255), "5'7\""),
        Triple(68, Triple(164, 197, 262), "5'8\""),
        Triple(69, Triple(169, 203, 270), "5'9\""),
        Triple(70, Triple(174, 209, 278), "5'10\""),
        Triple(71, Triple(179, 215, 286), "5'11\""),
        Triple(72, Triple(184, 221, 294), "6'0\""),
        Triple(73, Triple(189, 227, 302), "6'1\""),
        Triple(74, Triple(194, 233, 311), "6'2\""),
        Triple(75, Triple(200, 240, 319), "6'3\""),
        Triple(76, Triple(205, 246, 328), "6'4\""),
    )

    @Test
    fun `every published chart row matches at both sides of each boundary`() {
        publishedChart.forEach { (inches, bounds, label) ->
            val (onePoint, twoPoints, threePoints) = bounds
            val feet = inches / 12
            val rem = inches % 12
            fun points(lb: Int) = weightPointsAt(feet, rem, lb.toDouble())

            assertEquals("$label at ${onePoint - 1} lb", 0, points(onePoint - 1))
            assertEquals("$label at $onePoint lb", 1, points(onePoint))
            assertEquals("$label at ${twoPoints - 1} lb", 1, points(twoPoints - 1))
            assertEquals("$label at $twoPoints lb", 2, points(twoPoints))
            assertEquals("$label at ${threePoints - 1} lb", 2, points(threePoints - 1))
            assertEquals("$label at $threePoints lb", 3, points(threePoints))
        }
    }

    @Test
    fun `the six rows the BMI formula gets wrong come from the chart`() {
        // Regression guard: these are exactly the boundaries where
        // floor(bmi x inches^2 / 703) lands a pound off the printed chart.
        assertEquals(0, weightPointsAt(4, 11, 123.0)) // formula said 1
        assertEquals(3, weightPointsAt(5, 4, 232.0)) // formula said 2
        assertEquals(0, weightPointsAt(5, 6, 154.0)) // formula said 1
        assertEquals(1, weightPointsAt(5, 6, 185.0)) // formula said 2
        assertEquals(3, weightPointsAt(5, 8, 262.0)) // formula said 2
        assertEquals(3, weightPointsAt(6, 1, 302.0)) // formula said 2
        assertEquals(3, weightPointsAt(6, 3, 319.0)) // formula said 2
    }

    @Test
    fun `heights outside the chart fall back to the BMI thresholds`() {
        // The chart stops at 4'10" and 6'4", but the height slider doesn't. These
        // are our own extrapolation, not the published instrument.
        assertEquals(0, weightPointsAt(4, 6, 102.0)) // 54 in: BMI 25 lands at 103 lb
        assertEquals(1, weightPointsAt(4, 6, 103.0))
        assertEquals(2, weightPointsAt(4, 6, 124.0))
        assertEquals(3, weightPointsAt(4, 6, 165.0))

        assertEquals(0, weightPointsAt(6, 8, 226.0)) // 80 in: BMI 25 lands at 227 lb
        assertEquals(1, weightPointsAt(6, 8, 227.0))
        assertEquals(2, weightPointsAt(6, 8, 273.0))
        assertEquals(3, weightPointsAt(6, 8, 364.0))
    }

    @Test
    fun `low risk baseline scores below threshold and is not high risk`() {
        val score = RiskScorer.score(lowRiskAnswers())
        assertEquals(0, score.total)
        assertFalse(score.isHighRisk)
    }

    @Test
    fun `each factor contributes its documented points`() {
        val base = lowRiskAnswers()
        assertEquals(2, RiskScorer.score(base.copy(ageBand = AgeBand.AGE_50_59)).total)
        assertEquals(1, RiskScorer.score(base.copy(sex = Sex.MALE)).total)
        assertEquals(1, RiskScorer.score(base.copy(familyHistoryDiabetes = true)).total)
        assertEquals(1, RiskScorer.score(base.copy(highBloodPressure = true)).total)
        assertEquals(1, RiskScorer.score(base.copy(physicallyActive = false)).total)
    }

    @Test
    fun `gestational diabetes only scores for women`() {
        val base = lowRiskAnswers()
        assertEquals(1, RiskScorer.score(base.copy(sex = Sex.FEMALE, hadGestationalDiabetes = true)).total)
        // A man's gestational answer is ignored; he only gets the +1 man point.
        assertEquals(1, RiskScorer.score(base.copy(sex = Sex.MALE, hadGestationalDiabetes = true)).total)
    }

    @Test
    fun `threshold of five is high risk`() {
        val base = lowRiskAnswers()
        val fourPoints = base.copy(
            ageBand = AgeBand.AGE_50_59, // 2
            familyHistoryDiabetes = true, // 1
            highBloodPressure = true, // 1
        )
        assertEquals(4, RiskScorer.score(fourPoints).total)
        assertFalse(RiskScorer.score(fourPoints).isHighRisk)

        val fivePoints = fourPoints.copy(physicallyActive = false) // +1 = 5
        assertEquals(5, RiskScorer.score(fivePoints).total)
        assertTrue(RiskScorer.score(fivePoints).isHighRisk)
    }

    @Test
    fun `maximum realistic score does not exceed ten`() {
        val maxWoman = RiskTestAnswers(
            ageBand = AgeBand.AGE_60_PLUS, // 3
            sex = Sex.FEMALE,
            hadGestationalDiabetes = true, // 1
            familyHistoryDiabetes = true, // 1
            highBloodPressure = true, // 1
            physicallyActive = false, // 1
            heightCm = UnitConversions.feetInchesToCm(5, 4),
            weightKg = UnitConversions.lbToKg(260.0), // BMI ~44 -> 3
        )
        assertEquals(RiskScorer.MAX_SCORE, RiskScorer.score(maxWoman).total)
    }
}
