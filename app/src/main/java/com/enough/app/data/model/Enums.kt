package com.enough.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How a meal entry was created. Phase 0 supports fast text search and manual entry. */
enum class MealSource { TEXT, MANUAL }

/**
 * How precise a logged meal's nutrients are.
 *
 * [DATABASE_MATCHED] is an exact bundled food (text search / recent re-log).
 * [COARSE_ESTIMATE] is a low-friction category quick-log ("veggie-heavy meal")
 * backed by a representative synthetic food — inherently a rough estimate, so the
 * UI shows an honest range rather than a fake-precise gram number (SPEC §7.5/§15).
 */
enum class MealEntryType { DATABASE_MATCHED, COARSE_ESTIMATE }

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

/**
 * A dietary restriction or allergy the person logged in onboarding. Used to
 * filter food suggestions so a nudge never suggests something they can't eat.
 * "Other" is captured as free text on the goal, not as an enum value here.
 */
enum class DietaryRestriction { VEGETARIAN, VEGAN, GLUTEN_FREE, DAIRY_FREE, NUT_ALLERGY }

/**
 * Explicit dietary properties of a bundled [com.enough.app.data.local.entity.Food],
 * authored per-food in `foods.json` rather than inferred from the food's name.
 *
 * The first four are positive "safe for" attributes; [CONTAINS_NUTS] is the one
 * presence flag, used for nut-allergy filtering. Storing these as data (instead
 * of the old keyword classifier in [com.enough.app.domain.rules.DietaryFilter])
 * means allergy safety no longer depends on a substring guess. Coherence is
 * enforced by a unit test — e.g. anything [VEGAN] must also be [VEGETARIAN] and
 * [DAIRY_FREE]. The `@SerialName`s are the tokens used in `foods.json`.
 */
@Serializable
enum class DietaryTag {
    @SerialName("vegetarian")
    VEGETARIAN,

    @SerialName("vegan")
    VEGAN,

    @SerialName("gluten_free")
    GLUTEN_FREE,

    @SerialName("dairy_free")
    DAIRY_FREE,

    @SerialName("contains_nuts")
    CONTAINS_NUTS,
}

/**
 * How the person prefers uncertain fiber estimates handled (Settings, SPEC §7).
 * A personal preference about erring low or high, not a "right" answer: it
 * shifts each meal's fiber value by ±15% before the day's total is summed.
 */
enum class EstimateCalibration(val fiberMultiplier: Double) {
    LOW(0.85),
    BALANCED(1.0),
    HIGH(1.15),
}
