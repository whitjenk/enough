package com.enough.app.data.repository

import com.enough.app.data.local.dao.PrediabetesRiskResultDao
import com.enough.app.data.local.entity.PrediabetesRiskResult
import kotlinx.coroutines.flow.Flow

/** Reads and writes prediabetes risk-test results (kept as a history). */
class RiskResultRepository(private val dao: PrediabetesRiskResultDao) {
    val latest: Flow<PrediabetesRiskResult?> = dao.observeLatest()

    suspend fun getLatest(): PrediabetesRiskResult? = dao.getLatest()

    suspend fun saveResult(result: PrediabetesRiskResult): Long = dao.insert(result)
}
