package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.enough.app.data.local.entity.PrediabetesRiskResult
import kotlinx.coroutines.flow.Flow

@Dao
interface PrediabetesRiskResultDao {
    @Insert
    suspend fun insert(result: PrediabetesRiskResult): Long

    @Query("SELECT * FROM prediabetes_risk_result ORDER BY dateTaken DESC LIMIT 1")
    fun observeLatest(): Flow<PrediabetesRiskResult?>

    @Query("SELECT * FROM prediabetes_risk_result ORDER BY dateTaken DESC LIMIT 1")
    suspend fun getLatest(): PrediabetesRiskResult?

    @Query("SELECT * FROM prediabetes_risk_result ORDER BY dateTaken DESC")
    suspend fun getAll(): List<PrediabetesRiskResult>

    @Query("DELETE FROM prediabetes_risk_result")
    suspend fun deleteAll()
}
