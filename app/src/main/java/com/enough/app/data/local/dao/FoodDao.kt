package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enough.app.data.local.entity.Food

@Dao
interface FoodDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(foods: List<Food>)

    @Query("SELECT COUNT(*) FROM food")
    suspend fun count(): Int

    @Query("SELECT * FROM food WHERE id = :id")
    suspend fun getById(id: Long): Food?

    /**
     * Case-insensitive substring search over real food names, alphabetized.
     * Backs the "search an exact food" path; synthetic coarse-category foods
     * (`selectable = 0`) are excluded so they never appear in search results.
     */
    @Query(
        "SELECT * FROM food WHERE selectable = 1 AND name LIKE '%' || :query || '%' " +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int = 30): List<Food>

    @Query("SELECT * FROM food ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<Food>

    /** Highest-fiber real foods, for nudge/swap suggestions (excludes synthetic). */
    @Query(
        "SELECT * FROM food WHERE selectable = 1 " +
            "ORDER BY fiberG DESC, name COLLATE NOCASE ASC LIMIT :limit",
    )
    suspend fun topFiberFoods(limit: Int): List<Food>

    /** The synthetic coarse-category foods, highest-fiber first, for quick-log chips. */
    @Query("SELECT * FROM food WHERE selectable = 0 ORDER BY fiberG DESC, name COLLATE NOCASE ASC")
    suspend fun categoryFoods(): List<Food>

    /**
     * Distinct foods most recently logged, newest first — for one-tap re-logging.
     * Includes both real and synthetic foods (whatever the person actually logs).
     */
    @Query(
        "SELECT food.* FROM food JOIN meal_entry ON meal_entry.foodId = food.id " +
            "GROUP BY food.id ORDER BY MAX(meal_entry.timestamp) DESC LIMIT :limit",
    )
    suspend fun recentlyLogged(limit: Int): List<Food>

    @Query("DELETE FROM food")
    suspend fun deleteAll()
}
