package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enough.app.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightEntryDao {
    @Insert
    suspend fun insert(entry: WeightEntry): Long

    @Query("SELECT * FROM weight_entry ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntry?>

    @Query("SELECT * FROM weight_entry ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): WeightEntry?

    @Query("SELECT * FROM weight_entry ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entry ORDER BY timestamp ASC")
    suspend fun getAll(): List<WeightEntry>

    @Query("SELECT MAX(timestamp) FROM weight_entry")
    suspend fun latestTimestamp(): Long?

    @Query("DELETE FROM weight_entry")
    suspend fun deleteAll()
}
