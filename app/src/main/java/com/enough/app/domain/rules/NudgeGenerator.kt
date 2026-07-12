package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction
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
     * @param fiberSoFarG fiber logged so far today (already calibrated)
     * @param targetG the day's fiber target (0 means no goal set)
     * @param suggestions candidate high-fiber foods to draw a suggestion from
     * @param restrictions logged dietary restrictions/allergies; suggestions that
     *   violate any are dropped before one is chosen (never suggest the unsafe)
     * @param otherRestriction free-text restriction, honored best-effort
     * @param gentle when true (GLP-1 medication), suggest a smaller increment and
     *   mark the nudge so the UI drops any "hit your target" framing
     */
    fun generate(
        fiberSoFarG: Double,
        targetG: Int,
        suggestions: List<Food>,
        restrictions: Set<DietaryRestriction> = emptySet(),
        otherRestriction: String? = null,
        gentle: Boolean = false,
    ): Nudge {
        if (targetG <= 0) return Nudge.None

        val soFar = fiberSoFarG.roundToInt()
        val gap = targetG - fiberSoFarG

        // Met or within a rounding hair of the target: celebrate, don't nag.
        if (gap.roundToInt() < MEANINGFUL_GAP_G) {
            return Nudge.OnTrack(fiberSoFarG = soFar, targetG = targetG)
        }

        val safe = suggestions.filter { DietaryFilter.isSafe(it, restrictions, otherRestriction) }
        val suggestion = pickSuggestion(safe, gap, gentle) ?: return Nudge.None
        return Nudge.FiberGap(
            fiberSoFarG = soFar,
            gapG = gap.roundToInt(),
            suggestionFood = suggestion.name,
            suggestionServingLabel = suggestion.servingLabel,
            suggestionFiberG = suggestion.fiberG.roundToInt(),
            gentle = gentle,
        )
    }

    /**
     * Choose the suggestion food. Normally the one whose fiber best fits the
     * remaining [gap] (closest without being trivial), preferring a slightly
     * bigger lever on ties — a small gap gets a small, realistic swap, not "eat
     * 10g of chia." When [gentle], pick the smallest qualifying food instead: a
     * tiny, no-pressure add that suits a reduced appetite.
     */
    private fun pickSuggestion(suggestions: List<Food>, gap: Double, gentle: Boolean): Food? {
        val usable = suggestions.filter { it.fiberG >= MIN_SUGGESTION_FIBER_G }
        return if (gentle) {
            usable.minWithOrNull(compareBy<Food> { it.fiberG }.thenBy { it.name })
        } else {
            usable.minWithOrNull(compareBy<Food> { abs(it.fiberG - gap) }.thenByDescending { it.fiberG })
        }
    }
}
