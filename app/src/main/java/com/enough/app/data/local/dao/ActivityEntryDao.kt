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

    @Query("SELECT MAX(timestamp) FROM activity_entry")
    suspend fun latestTimestamp(): Long?

    /** Total logged minutes-of-movement since [startInclusive] (MINUTES unit only). */
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM activity_entry " +
            "WHERE unit = 'MINUTES' AND timestamp >= :startInclusive",
    )
    suspend fun minutesLoggedSince(startInclusive: Long): Int

    @Query("DELETE FROM activity_entry")
    suspend fun deleteAll()
}
