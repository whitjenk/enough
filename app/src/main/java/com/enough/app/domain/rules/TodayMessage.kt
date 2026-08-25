package com.enough.app.domain.rules

/**
 * The single message the Today screen speaks with on a given day.
 *
 * The reset moment, the fiber nudge and the daily swap are all the app *saying
 * something* to the person. Before this existed they stacked as three equal
 * cards, which reads as a wall of advice on the exact days someone is least able
 * to hear it — and lets them contradict each other (a "here's a gap" nudge under
 * a "today's a fresh start" card).
 *
 * This is the *presentation* half of the Phase 1 §16 arbitration layer, pulled
 * forward as §7.7 item 2: one voice at a time, on screen. The persistence half
 * (`lastProactiveMessageDate`, cross-day budgets, walk/sleep/stress candidates)
 * stays in §16 — this deliberately decides nothing about *notifications*, only
 * about what the screen shows right now.
 *
 * Deliberately NOT in this contest: the felt check-in. It isn't the app talking,
 * it's an input the person acts on (and the anchor for the opt-in daily share),
 * so it keeps its own quieter slot rather than losing a ranking to the swap on
 * nearly every day. See §7.6 Step 1/2.
 */
sealed interface TodayMessage {
    /** Nothing worth saying today — the screen stays quiet rather than filling space. */
    data object None : TodayMessage

    /** The forgiveness card. Outranks everything: a rough patch is not a nudge moment. */
    data object Reset : TodayMessage

    /** The routine fiber nudge, with its specific, doable suggestion. */
    data class FiberNudge(val nudge: Nudge) : TodayMessage

    /** The zero-input one-idea swap — useful even if nothing is ever logged. */
    data class Swap(val swap: DailySwap.Swap) : TodayMessage
}

/**
 * Picks the one message Today shows, in a fixed priority order:
 *
 *  1. [TodayMessage.Reset] — the reset moment always outranks routine nudges
 *     (`IMPLEMENTATION_PLAN.md` §16), because it fires precisely when advice
 *     would land as pressure.
 *  2. [TodayMessage.FiberNudge] — reacts to what actually happened today, so it
 *     beats the generic swap when there's something specific to say.
 *  3. [TodayMessage.Swap] — the always-available fallback that keeps a day with
 *     nothing logged still worth opening.
 *
 * Pure and total: same inputs, same answer, no clock and no I/O.
 */
object TodayMessageArbiter {

    fun select(
        showResetMoment: Boolean,
        nudge: Nudge,
        swap: DailySwap.Swap?,
    ): TodayMessage = when {
        showResetMoment -> TodayMessage.Reset
        nudge != Nudge.None -> TodayMessage.FiberNudge(nudge)
        swap != null -> TodayMessage.Swap(swap)
        else -> TodayMessage.None
    }
}
