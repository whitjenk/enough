package com.enough.app.data.seed

import com.enough.app.data.local.entity.Food
import com.enough.app.data.model.DietaryTag
import kotlinx.serialization.Serializable

/**
 * JSON shape of one bundled food (assets/foods.json). Kept separate from the
 * [Food] Room entity so the on-disk data format and the DB schema can evolve
 * independently.
 *
 * [dietaryTags] defaults to empty so older data files still parse, but every
 * food in the bundled list is expected to carry explicit tags (enforced by
 * FoodSeedDataTest).
 */
@Serializable
data class SeedFood(
    val name: String,
    val servingLabel: String,
    val carbsG: Double,
    val fiberG: Double,
    val proteinG: Double,
    val dietaryTags: Set<DietaryTag> = emptySet(),
) {
    fun toEntity(): Food = Food(
        name = name,
        servingLabel = servingLabel,
        carbsG = carbsG,
        fiberG = fiberG,
        proteinG = proteinG,
        dietaryTags = dietaryTags,
    )
}
