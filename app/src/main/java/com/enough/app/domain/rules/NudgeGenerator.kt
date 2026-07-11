package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Turns today's fiber progress into a single supportive nudge. Pure and
 * testable. Tone rules (CLAUDE.md / SPEC.md §8): never shame, never streaks,
 * always one small, specific, doable next step.
 */
object NudgeGenerator {

    /** A gap smaller than this isn't worth nudging about — the person is basically there. */
    const val MEANINGFUL_GAP_G = 3

    /** Only suggest foods that add at least this much fiber, so the tip is worth it. */
    const val MIN_SUGGESTION_FIBER_G = 2.0

    /**
     * @param fiberSoFarG fiber logged so far today
     * @param targetG the day's fiber target (0 means no goal set)
     * @param suggestions candidate high-fiber foods to draw a suggestion from
     */
    fun generate(fiberSoFarG: Double, targetG: Int, suggestions: List<Food>): Nudge {
        if (targetG <= 0) return Nudge.None

        val soFar = fiberSoFarG.roundToInt()
        val gap = targetG - fiberSoFarG

        // Met or within a rounding hair of the target: celebrate, don't nag.
        if (gap.roundToInt() < MEANINGFUL_GAP_G) {
            return Nudge.OnTrack(fiberSoFarG = soFar, targetG = targetG)
        }

        val suggestion = pickSuggestion(suggestions, gap) ?: return Nudge.None
        return Nudge.FiberGap(
            fiberSoFarG = soFar,
            gapG = gap.roundToInt(),
            suggestionFood = suggestion.name,
            suggestionServingLabel = suggestion.servingLabel,
            suggestionFiberG = suggestion.fiberG.roundToInt(),
        )
    }

    /**
     * Choose the suggestion whose fiber best fits the remaining gap (closest
     * without being trivial), preferring a slightly bigger lever on ties. This
     * keeps the tip proportional — a small gap gets a small, realistic swap, not
     * "eat 10g of chia."
     */
    private fun pickSuggestion(suggestions: List<Food>, gap: Double): Food? =
        suggestions
            .filter { it.fiberG >= MIN_SUGGESTION_FIBER_G }
            .minWithOrNull(
                compareBy<Food> { abs(it.fiberG - gap) }.thenByDescending { it.fiberG },
            )
}
