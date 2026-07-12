package com.enough.app.data.seed

import com.enough.app.data.local.entity.Food

/**
 * The synthetic foods that back the low-friction coarse quick-log (SPEC §7.5).
 * Each is a rough, representative "kind of meal" a person can log in one tap when
 * the exact food isn't worth searching for — the interaction-design fix for the
 * #1 churn driver (precise text-search logging), not an ML one.
 *
 * They are real [Food] rows (so the existing FK-based read/aggregation path works
 * unchanged) but marked `selectable = false`, which keeps them out of text search
 * and out of the nudge/swap suggestion pool. Fiber values are deliberately modest
 * and honest; because a coarse entry is an estimate, the UI shows a range rather
 * than a fake-precise gram number. Tags are intentionally empty — these are never
 * used as suggestions, and logging is never gated by dietary restrictions.
 *
 * Ordered high-to-low fiber so the chips read veggie → carb.
 */
object CategoryFoods {
    private const val SERVING = "1 meal"

    val ALL: List<Food> = listOf(
        Food(name = "Veggie-heavy meal", servingLabel = SERVING, carbsG = 30.0, fiberG = 8.0, proteinG = 8.0, selectable = false),
        Food(name = "Mixed meal", servingLabel = SERVING, carbsG = 40.0, fiberG = 5.0, proteinG = 20.0, selectable = false),
        Food(name = "Protein-heavy meal", servingLabel = SERVING, carbsG = 15.0, fiberG = 3.0, proteinG = 35.0, selectable = false),
        Food(name = "Carb-heavy meal", servingLabel = SERVING, carbsG = 55.0, fiberG = 2.0, proteinG = 8.0, selectable = false),
    )
}
