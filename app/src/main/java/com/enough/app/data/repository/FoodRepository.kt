package com.enough.app.data.repository

import com.enough.app.data.local.dao.FoodDao
import com.enough.app.data.local.entity.Food

/** Reads the bundled food list (search + lookup) for meal logging. */
class FoodRepository(private val dao: FoodDao) {
    /** Substring search over food names; a blank query returns nothing. */
    suspend fun search(query: String, limit: Int = 30): List<Food> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) emptyList() else dao.search(trimmed, limit)
    }

    suspend fun getById(id: Long): Food? = dao.getById(id)

    /** Highest-fiber foods, used to source nudge suggestions. */
    suspend fun topFiberFoods(limit: Int = 12): List<Food> = dao.topFiberFoods(limit)

    /** The synthetic coarse-category foods that back the one-tap quick-log. */
    suspend fun categoryFoods(): List<Food> = dao.categoryFoods()

    /** Most recently logged foods, for one-tap re-logging. */
    suspend fun recentFoods(limit: Int = 6): List<Food> = dao.recentlyLogged(limit)

    /**
     * Add a food the person entered themselves ("can't find it? add it"). Stored
     * as searchable + re-loggable but user-created (kept out of suggestions).
     * Returns the saved food, with its new id, ready to log immediately.
     */
    suspend fun addCustomFood(
        name: String,
        servingLabel: String,
        fiberG: Double,
        carbsG: Double = 0.0,
        proteinG: Double = 0.0,
    ): Food {
        val food = Food(
            name = name,
            servingLabel = servingLabel,
            carbsG = carbsG,
            fiberG = fiberG,
            proteinG = proteinG,
            selectable = true,
            userCreated = true,
        )
        val id = dao.insert(food)
        return food.copy(id = id)
    }
}
