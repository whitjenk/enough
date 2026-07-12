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
}
