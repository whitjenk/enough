package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.enough.app.data.model.DietaryTag

/**
 * A food in the local, bundled list. Carbs/fiber/protein are per one [servingLabel].
 * Fiber is the headline nutrient for this app, but carbs/protein are kept so the
 * numbers read as a normal, trustworthy food entry rather than a single cherry-picked stat.
 *
 * [dietaryTags] are explicit per-food dietary properties (authored in
 * `foods.json`) that [com.enough.app.domain.rules.DietaryFilter] reads to decide
 * whether a food is safe to *suggest* for a person's restrictions/allergies.
 */
@Entity(
    tableName = "food",
    indices = [Index(value = ["name"])],
)
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servingLabel: String,
    val carbsG: Double,
    val fiberG: Double,
    val proteinG: Double,
    val dietaryTags: Set<DietaryTag> = emptySet(),
)
