package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction
import kotlin.math.roundToInt

/**
 * "Today's one swap" — a single, zero-input daily idea for a little more fiber,
 * useful even if the person logs nothing at all that day (SPEC §7.5).
 *
 * Deliberately NOT a nudge to log: it never references what has or hasn't been
 * logged, sets no target, and carries no catch-up framing — the copy that renders
 * it always offers an out ("today can just be today"), per the buddy voice in
 * `CLAUDE.md`. It reuses the same safe, restriction-filtered high-fiber pool the
 * fiber-gap nudge draws from, so a suggestion here is subject to the exact same
 * allergy/restriction safety as [NudgeGenerator].
 *
 * Pure and testable — no Room/Android dependency.
 */
object DailySwap {

    /** One picked idea for the day, or none when no safe suggestion exists. */
    data class Swap(
        val food: String,
        val servingLabel: String,
        val fiberG: Int,
        val gentle: Boolean,
        /** The "coming off a GLP-1" framing: fiber as the satiety bridge (SPEC §7.6 Step 4). */
        val bridge: Boolean = false,
    )

    /**
     * Pick one idea deterministically for [epochDay] so it stays stable across
     * recompositions and rotates day to day rather than flickering. Given the
     * same day and goal (same [restrictions]/[gentle]) the result is fixed.
     *
     * @param epochDay the local date as an epoch-day, the rotation key
     * @param candidates the high-fiber pool (e.g. [Food]s from `topFiberFoods`)
     * @param restrictions logged restrictions/allergies; unsafe foods are dropped
     *   first via [DietaryFilter] — never surface something the person can't eat
     * @param otherRestriction free-text restriction, honored best-effort
     * @param gentle when true (on or coming off a GLP-1), bias toward a smaller,
     *   easier add and let the UI drop any number-to-hit framing
     * @param comingOff when true, use the "satiety bridge" framing (implies gentle
     *   sizing); the UI renders a distinct, reassuring coming-off line
     */
    fun forDay(
        epochDay: Long,
        candidates: List<Food>,
        restrictions: Set<DietaryRestriction> = emptySet(),
        otherRestriction: String? = null,
        gentle: Boolean = false,
        comingOff: Boolean = false,
    ): Swap? {
        val gentleSizing = gentle || comingOff
        val safe = candidates
            .filter { it.fiberG >= NudgeGenerator.MIN_SUGGESTION_FIBER_G }
            .filter { DietaryFilter.isSafe(it, restrictions, otherRestriction) }
        if (safe.isEmpty()) return null

        // Impose a stable ordering so the daily rotation is deterministic
        // regardless of the pool's incoming order. Gentle prefers the
        // smaller-fiber end (an easier add for a reduced appetite); otherwise
        // lead with the bigger levers. Name breaks ties for full determinism.
        val ordered = safe.sortedWith(
            if (gentleSizing) {
                compareBy<Food> { it.fiberG }.thenBy { it.name }
            } else {
                compareByDescending<Food> { it.fiberG }.thenBy { it.name }
            },
        )
        val pick = ordered[epochDay.mod(ordered.size.toLong()).toInt()]
        return Swap(
            food = pick.name,
            servingLabel = pick.servingLabel,
            fiberG = pick.fiberG.roundToInt(),
            gentle = gentleSizing,
            bridge = comingOff,
        )
    }
}
