package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction

/**
 * Decides whether a food is safe to *suggest* given the person's logged dietary
 * restrictions and allergies (SPEC §3/§5). The rule is one-directional: when in
 * doubt, exclude. A wrongly-excluded food only costs us one alternative
 * suggestion, whereas suggesting something someone can't eat — an allergen
 * especially — is never acceptable.
 *
 * The bundled food list carries no dietary metadata, so this classifies by
 * keyword on the food name. That's deliberately conservative and imperfect: it
 * is only ever used to *narrow* the high-fiber suggestion pool, never to label a
 * food as safe to the person or to gate what they can log themselves. If the food
 * data later gains real dietary tags, this becomes the place to read them.
 */
object DietaryFilter {

    // Meat, poultry, and seafood — excluded for vegetarian and vegan.
    private val MEAT_AND_SEAFOOD = listOf(
        "beef", "steak", "chicken", "pork", "turkey", "ham", "bacon", "sausage",
        "lamb", "veal", "meat", "jerky", "pepperoni", "salami", "gelatin",
        "fish", "salmon", "tuna", "cod", "shrimp", "prawn", "crab", "lobster",
        "sardine", "anchovy", "tilapia", "trout", "oyster", "clam", "scallop",
    )

    // Animal products that are fine for vegetarians but not vegans.
    private val DAIRY = listOf("milk", "cheese", "yogurt", "yoghurt", "butter", "cream", "whey", "casein")
    private val EGG_AND_HONEY = listOf("egg", "honey")

    private val GLUTEN = listOf(
        "wheat", "bread", "pasta", "barley", "rye", "couscous", "cracker",
        "bran flake", "wheat cereal", "bagel", "tortilla", "noodle", "flour",
        "bun", "wrap", "pretzel", "cracked wheat", "bulgur",
    )

    // Common tree nuts and peanuts. Specific terms (not the bare substring "nut")
    // so safe foods like "butternut squash" or "coconut" aren't needlessly dropped.
    private val NUTS = listOf(
        "almond", "peanut", "walnut", "cashew", "pecan", "pistachio",
        "hazelnut", "macadamia", "brazil nut", "pine nut", "mixed nuts",
    )

    fun isSafe(
        food: Food,
        restrictions: Set<DietaryRestriction>,
        other: String? = null,
    ): Boolean {
        val name = food.name.lowercase()
        fun containsAny(keywords: List<String>) = keywords.any { name.contains(it) }

        restrictions.forEach { restriction ->
            val violates = when (restriction) {
                DietaryRestriction.VEGETARIAN -> containsAny(MEAT_AND_SEAFOOD)
                DietaryRestriction.VEGAN ->
                    containsAny(MEAT_AND_SEAFOOD) || containsAny(DAIRY) || containsAny(EGG_AND_HONEY)
                DietaryRestriction.GLUTEN_FREE -> containsAny(GLUTEN)
                DietaryRestriction.DAIRY_FREE -> containsAny(DAIRY)
                DietaryRestriction.NUT_ALLERGY -> containsAny(NUTS)
            }
            if (violates) return false
        }

        // Best-effort: honor a free-text restriction by excluding any suggestion
        // whose name contains it (e.g. "shellfish", "soy").
        val otherTerm = other?.trim()?.lowercase()
        if (!otherTerm.isNullOrEmpty() && name.contains(otherTerm)) return false

        return true
    }
}
