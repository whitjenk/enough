package com.enough.app.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The presentation arbitration pulled forward from §16: exactly one message, in
 * a fixed priority order, with the reset moment always outranking routine advice.
 */
class TodayMessageArbiterTest {

    private val nudge = Nudge.FiberGap(
        fiberSoFarG = 6,
        gapG = 22,
        suggestionFood = "Lentils",
        suggestionServingLabel = "1/2 cup cooked",
        suggestionFiberG = 8,
    )

    private val swap = DailySwap.Swap(
        food = "Chia seeds",
        servingLabel = "2 tbsp",
        fiberG = 10,
        gentle = false,
    )

    @Test
    fun `reset moment outranks both the nudge and the swap`() {
        // The §16 rule that matters most: on a rough day, advice is suppressed.
        val result = TodayMessageArbiter.select(
            showResetMoment = true,
            nudge = nudge,
            swap = swap,
        )
        assertEquals(TodayMessage.Reset, result)
    }

    @Test
    fun `the nudge outranks the swap when both are eligible`() {
        // Something specific about today beats the generic one-idea fallback.
        val result = TodayMessageArbiter.select(
            showResetMoment = false,
            nudge = nudge,
            swap = swap,
        )
        assertEquals(TodayMessage.FiberNudge(nudge), result)
    }

    @Test
    fun `the swap shows when there is no nudge to give`() {
        // Zero-input daily value: a day with nothing logged is still worth opening.
        val result = TodayMessageArbiter.select(
            showResetMoment = false,
            nudge = Nudge.None,
            swap = swap,
        )
        assertEquals(TodayMessage.Swap(swap), result)
    }

    @Test
    fun `an on-track nudge still outranks the swap`() {
        // "You're there" is a real thing to say about today, not a filler message.
        val onTrack = Nudge.OnTrack(fiberSoFarG = 30, targetG = 28)
        val result = TodayMessageArbiter.select(
            showResetMoment = false,
            nudge = onTrack,
            swap = swap,
        )
        assertEquals(TodayMessage.FiberNudge(onTrack), result)
    }

    @Test
    fun `nothing to say stays quiet rather than filling the space`() {
        val result = TodayMessageArbiter.select(
            showResetMoment = false,
            nudge = Nudge.None,
            swap = null,
        )
        assertEquals(TodayMessage.None, result)
    }

    @Test
    fun `exactly one message is ever selected`() {
        // Exhaustive over the eligibility combinations: the arbiter returns a
        // single message every time, so the three can never stack again.
        val resets = listOf(true, false)
        val nudges = listOf(Nudge.None, nudge)
        val swaps = listOf(null, swap)

        for (reset in resets) {
            for (n in nudges) {
                for (s in swaps) {
                    val result = TodayMessageArbiter.select(reset, n, s)
                    // A reset day never yields advice, whatever else is eligible.
                    if (reset) {
                        assertEquals(TodayMessage.Reset, result)
                    }
                    // The swap only ever wins when nothing louder is eligible.
                    if (result is TodayMessage.Swap) {
                        assertTrue(!reset && n == Nudge.None)
                    }
                }
            }
        }
    }
}
