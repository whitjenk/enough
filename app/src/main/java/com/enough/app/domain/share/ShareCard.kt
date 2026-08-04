package com.enough.app.domain.share

import com.enough.app.data.model.FeltLevel
import kotlin.math.roundToInt

/**
 * Pure content model for the opt-in shareable card (SPEC §7.6 Step 2). Decides
 * *what values* the card may show — never how it's drawn — so the guardrails are
 * directly unit-tested:
 *
 * - **No literal number leaks in hide-numbers mode** ([Data.averageFiberG] and
 *   [Data.daysLogged] are null when [hideNumbers], and the renderer's hidden
 *   variant carries no numerals at all).
 * - **No comparison to other people, ever** — this type has no field that could
 *   express one; the card is only ever about the person's own week (CLAUDE.md).
 * - **No PII** — only aggregates, never a timestamp, free-text, or identifier.
 */
object ShareCard {

    data class Data(
        val hasData: Boolean,
        val hideNumbers: Boolean,
        /** Average fiber over the days that had any fiber; null when hidden or no data. */
        val averageFiberG: Int?,
        /** Days logged in the window; null when hidden (so the card shows no numerals). */
        val daysLogged: Int?,
        val windowDays: Int,
    )

    /**
     * The feeling-first daily card (SPEC §7.6 Step 2, social variant). Deliberately
     * **number-free by construction** — it leads with how the day *felt*, not a
     * gram count, which is the whole counter-position: the trend flexes numbers,
     * this shares a feeling. Because the type carries no number, it is trivially
     * safe under hide-numbers and can never leak a value into a shared story.
     */
    data class DailyData(val felt: FeltLevel?)

    /** Build the daily story card from today's optional felt check-in. */
    fun daily(felt: FeltLevel?): DailyData = DailyData(felt)

    /**
     * @param fiberByDayValues the window's per-day fiber totals (zero-filled days included).
     * @param daysLogged how many days in the window had any log.
     * @param hideNumbers when true, no numeric value is exposed for the card at all.
     */
    fun build(
        fiberByDayValues: List<Double>,
        daysLogged: Int,
        windowDays: Int,
        hideNumbers: Boolean,
    ): Data {
        val withFiber = fiberByDayValues.filter { it > 0.0 }
        val hasData = withFiber.isNotEmpty()
        return Data(
            hasData = hasData,
            hideNumbers = hideNumbers,
            averageFiberG = if (hideNumbers || !hasData) null else withFiber.average().roundToInt(),
            daysLogged = if (hideNumbers) null else daysLogged,
            windowDays = windowDays,
        )
    }
}
