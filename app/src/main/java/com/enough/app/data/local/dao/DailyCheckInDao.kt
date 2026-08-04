package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.enough.app.data.local.entity.DailyCheckIn
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyCheckInDao {
    /** Upsert by day: re-tapping a different answer today replaces today's row. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DailyCheckIn)

    @Query("SELECT * FROM daily_check_in WHERE date = :date LIMIT 1")
    fun observeForDate(date: LocalDate): Flow<DailyCheckIn?>

    @Query("SELECT * FROM daily_check_in WHERE date >= :startInclusive AND date <= :endInclusive ORDER BY date")
    fun observeBetween(startInclusive: LocalDate, endInclusive: LocalDate): Flow<List<DailyCheckIn>>

    @Query("SELECT * FROM daily_check_in ORDER BY date DESC")
    suspend fun getAll(): List<DailyCheckIn>

    @Query("DELETE FROM daily_check_in")
    suspend fun deleteAll()
}
