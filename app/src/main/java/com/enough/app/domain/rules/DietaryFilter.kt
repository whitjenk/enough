package com.enough.app.domain.rules

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryRestriction
import com.enough.app.data.model.DietaryTag

/**
 * Decides whether a food is safe to *suggest* given the person's logged dietary
 * restrictions and allergies (SPEC §3/§5). The rule is one-directional: when in
 * doubt, exclude. A wrongly-excluded food only costs us one alternative
 * suggestion, whereas suggesting something someone can't eat — an allergen
 * especially — is never acceptable.
 *
 * Safety is read from the food's explicit [Food.dietaryTags] (authored in
 * `foods.json`), not guessed from its name. A positive restriction is satisfied
 * only when the food carries the matching positive tag, so a food with unknown
 * tags is treated as unsafe for that restriction rather than assumed fine. A nut
 * allergy excludes anything flagged [DietaryTag.CONTAINS_NUTS].
 *
 * This is still only ever used to *narrow* the suggestion pool — never to label
 * a food "safe" to the person or to gate what they can log themselves.
 */
object DietaryFilter {

    fun isSafe(
        food: Food,
        restrictions: Set<DietaryRestriction>,
        other: String? = null,
    ): Boolean {
        val tags = food.dietaryTags

        restrictions.forEach { restriction ->
            val satisfied = when (restriction) {
                DietaryRestriction.VEGETARIAN -> DietaryTag.VEGETARIAN in tags
                DietaryRestriction.VEGAN -> DietaryTag.VEGAN in tags
                DietaryRestriction.GLUTEN_FREE -> DietaryTag.GLUTEN_FREE in tags
                DietaryRestriction.DAIRY_FREE -> DietaryTag.DAIRY_FREE in tags
                DietaryRestriction.NUT_ALLERGY -> DietaryTag.CONTAINS_NUTS !in tags
            }
            if (!satisfied) return false
        }

        // Best-effort: honor a free-text restriction the tag vocabulary can't
        // capture (e.g. "shellfish", "soy") by excluding any suggestion whose
        // name contains it.
        val otherTerm = other?.trim()?.lowercase()
        if (!otherTerm.isNullOrEmpty() && food.name.lowercase().contains(otherTerm)) return false

        return true
    }
}
