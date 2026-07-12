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

    @Test
    fun `official chart row 4 foot 10 matches documented pound thresholds`() {
        // CDC chart: 4'10" -> 119-142 (1pt), 143-190 (2pt), 191+ (3pt).
        assertEquals(0, weightPointsAt(4, 10, 118.0))
        assertEquals(1, weightPointsAt(4, 10, 119.0))
        assertEquals(1, weightPointsAt(4, 10, 142.0))
        assertEquals(2, weightPointsAt(4, 10, 143.0))
        assertEquals(2, weightPointsAt(4, 10, 190.0))
        assertEquals(3, weightPointsAt(4, 10, 191.0))
    }

    @Test
    fun `official chart row 5 foot 0 matches documented pound thresholds`() {
        // CDC chart: 5'0" -> 128-152 (1pt), 153-203 (2pt), 204+ (3pt).
        assertEquals(0, weightPointsAt(5, 0, 127.0))
        assertEquals(1, weightPointsAt(5, 0, 128.0))
        assertEquals(2, weightPointsAt(5, 0, 153.0))
        assertEquals(3, weightPointsAt(5, 0, 204.0))
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
