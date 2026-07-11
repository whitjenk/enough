package com.enough.app.domain.rules

import com.enough.app.data.model.NudgeType

/**
 * The daily nudge as structured data, not a finished string. Keeping it
 * structured means the copy lives in string resources (localizable, tone-
 * reviewable) while the decision logic stays pure and testable.
 */
sealed interface Nudge {
    /** No nudge — e.g. no goal set yet, or nothing worth saying today. */
    data object None : Nudge

    /** Fiber goal reached (or effectively reached). Supportive, success-toned. */
    data class OnTrack(val fiberSoFarG: Int, val targetG: Int) : Nudge

    /**
     * A meaningful fiber gap with one specific, doable food suggestion that would
     * help close it. Never framed as a shortfall or failure.
     */
    data class FiberGap(
        val fiberSoFarG: Int,
        val gapG: Int,
        val suggestionFood: String,
        val suggestionServingLabel: String,
        val suggestionFiberG: Int,
    ) : Nudge

    /** The persisted nudge category for [com.enough.app.data.local.entity.RulesEngineState]. */
    val type: NudgeType
        get() = when (this) {
            is FiberGap -> NudgeType.FIBER_GAP
            else -> NudgeType.NONE
        }
}
