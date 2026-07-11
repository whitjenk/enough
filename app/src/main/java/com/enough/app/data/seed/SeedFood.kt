package com.enough.app.data.seed

import com.enough.app.data.local.entity.Food
import kotlinx.serialization.Serializable

/**
 * JSON shape of one bundled food (assets/foods.json). Kept separate from the
 * [Food] Room entity so the on-disk data format and the DB schema can evolve
 * independently.
 */
@Serializable
data class SeedFood(
    val name: String,
    val servingLabel: String,
    val carbsG: Double,
    val fiberG: Double,
    val proteinG: Double,
) {
    fun toEntity(): Food = Food(
        name = name,
        servingLabel = servingLabel,
        carbsG = carbsG,
        fiberG = fiberG,
        proteinG = proteinG,
    )
}
