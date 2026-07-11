package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enough.app.data.local.entity.ActivityEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEntryDao {
    @Insert
    suspend fun insert(entry: ActivityEntry): Long

    @Query(
        "SELECT * FROM activity_entry WHERE timestamp >= :startInclusive AND timestamp < :endExclusive " +
            "ORDER BY timestamp DESC",
    )
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<ActivityEntry>>

    @Query(
        "SELECT * FROM activity_entry WHERE timestamp >= :startInclusive AND timestamp < :endExclusive " +
            "ORDER BY timestamp DESC",
    )
    suspend fun getBetween(startInclusive: Long, endExclusive: Long): List<ActivityEntry>

    @Query("SELECT * FROM activity_entry ORDER BY timestamp DESC")
    suspend fun getAll(): List<ActivityEntry>

    @Query("DELETE FROM activity_entry")
    suspend fun deleteAll()
}
