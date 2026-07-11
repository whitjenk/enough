package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import kotlinx.coroutines.flow.Flow

/** A meal entry joined with the [Food] it references, for display and fiber math. */
data class MealWithFood(
    @Embedded val meal: MealEntry,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: Food,
)

@Dao
interface MealEntryDao {
    @Insert
    suspend fun insert(entry: MealEntry): Long

    @Delete
    suspend fun delete(entry: MealEntry)

    /**
     * Meals with timestamps in [startInclusive, endExclusive), newest first.
     * Callers pass a day's [startInclusive]/[endExclusive] boundaries in the
     * device time zone so "today" is a calendar day, not a rolling 24h.
     */
    @Transaction
    @Query(
        "SELECT * FROM meal_entry WHERE timestamp >= :startInclusive AND timestamp < :endExclusive " +
            "ORDER BY timestamp DESC",
    )
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<MealWithFood>>

    @Transaction
    @Query(
        "SELECT * FROM meal_entry WHERE timestamp >= :startInclusive AND timestamp < :endExclusive " +
            "ORDER BY timestamp DESC",
    )
    suspend fun getBetween(startInclusive: Long, endExclusive: Long): List<MealWithFood>

    @Transaction
    @Query("SELECT * FROM meal_entry ORDER BY timestamp DESC")
    suspend fun getAll(): List<MealWithFood>

    @Query("DELETE FROM meal_entry")
    suspend fun deleteAll()
}
