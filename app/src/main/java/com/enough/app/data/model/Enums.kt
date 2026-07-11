package com.enough.app.data.model

/** How a meal entry was created. Phase 0 supports fast text search and manual entry. */
enum class MealSource { TEXT, MANUAL }

/**
 * The kind of weekly activity goal the person chose in onboarding.
 *
 * Deliberately not step-only: the accessibility fix in SPEC.md §5 requires a
 * non-step option so the app works for people with atypical mobility.
 */
enum class ActivityGoalType {
    /** A step-count target (e.g. 7,000 steps/day). */
    STEPS,

    /** Minutes of any movement (the CDC 150 min/week framing). */
    MINUTES,

    /** A self-described goal (seated/adaptive movement, or anything else). */
    CUSTOM,
}

/** Source of a logged activity entry; mirrors the chosen [ActivityGoalType]. */
enum class ActivityUnit { STEPS, MINUTES, CUSTOM }

/** Provenance of a risk-test result. One value for Phase 0; kept as an enum for Phase 1 (doctor-reported). */
enum class RiskResultSource { CDC_ADA_RISK_TEST }

/** Direction of the recent weight trend, computed from logged history. Never rendered in red. */
enum class WeightTrendDirection { DOWN, FLAT, UP, UNKNOWN }

/** The kind of nudge shown. Phase 0 only generates fiber-gap nudges. */
enum class NudgeType { NONE, FIBER_GAP }
