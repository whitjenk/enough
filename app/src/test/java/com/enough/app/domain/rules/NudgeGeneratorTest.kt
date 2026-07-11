package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NudgeGeneratorTest {

    private fun food(name: String, fiber: Double) =
        Food(id = name.hashCode().toLong(), name = name, servingLabel = "1 serving", carbsG = 20.0, fiberG = fiber, proteinG = 5.0)

    private val suggestions = listOf(
        food("Chia seeds", 9.8),
        food("Lentils", 7.8),
        food("Raspberries", 8.0),
        food("Pear", 5.5),
        food("Almonds", 3.5),
        food("Broccoli", 2.4),
    )

    @Test
    fun `no goal yields no nudge`() {
        assertEquals(Nudge.None, NudgeGenerator.generate(fiberSoFarG = 5.0, targetG = 0, suggestions = suggestions))
    }

    @Test
    fun `meeting the target gives an on-track nudge, not a suggestion`() {
        val nudge = NudgeGenerator.generate(fiberSoFarG = 28.0, targetG = 28, suggestions = suggestions)
        assertEquals(Nudge.OnTrack(fiberSoFarG = 28, targetG = 28), nudge)
    }

    @Test
    fun `a tiny remaining gap is treated as on track`() {
        // 26 of 28 -> gap 2 (< meaningful threshold of 3) -> on track.
        val nudge = NudgeGenerator.generate(fiberSoFarG = 26.0, targetG = 28, suggestions = suggestions)
        assertTrue(nudge is Nudge.OnTrack)
    }

    @Test
    fun `a meaningful gap suggests a food sized to the gap`() {
        // 22 of 28 -> gap 6. Closest suggestion by fiber is Pear (5.5g).
        val nudge = NudgeGenerator.generate(fiberSoFarG = 22.0, targetG = 28, suggestions = suggestions)
        assertTrue(nudge is Nudge.FiberGap)
        nudge as Nudge.FiberGap
        assertEquals(22, nudge.fiberSoFarG)
        assertEquals(6, nudge.gapG)
        assertEquals("Pear", nudge.suggestionFood)
        assertEquals(6, nudge.suggestionFiberG) // 5.5 rounds to 6
    }

    @Test
    fun `a large gap still suggests the closest-fitting food, not the biggest`() {
        // 0 of 28 -> gap 28. Best single fit among suggestions is the highest fiber (Chia 9.8).
        val nudge = NudgeGenerator.generate(fiberSoFarG = 0.0, targetG = 28, suggestions = suggestions)
        assertTrue(nudge is Nudge.FiberGap)
        assertEquals("Chia seeds", (nudge as Nudge.FiberGap).suggestionFood)
    }

    @Test
    fun `no usable suggestions yields no nudge rather than an empty one`() {
        val weakOnly = listOf(food("Iceberg lettuce", 0.5))
        assertEquals(Nudge.None, NudgeGenerator.generate(fiberSoFarG = 5.0, targetG = 28, suggestions = weakOnly))
    }

    @Test
    fun `fiber gap nudge maps to the FIBER_GAP persisted type`() {
        val nudge = NudgeGenerator.generate(10.0, 28, suggestions)
        assertEquals(com.enough.app.data.model.NudgeType.FIBER_GAP, nudge.type)
    }
}
