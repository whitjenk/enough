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
     * Case-insensitive substring search over food names, alphabetized.
     * Backs the fast add-meal search; [limit] keeps the result list short.
     */
    @Query(
        "SELECT * FROM food WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int = 30): List<Food>

    @Query("SELECT * FROM food ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<Food>

    @Query("DELETE FROM food")
    suspend fun deleteAll()
}
