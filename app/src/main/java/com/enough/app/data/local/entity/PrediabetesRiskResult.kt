package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.enough.app.data.model.RiskResultSource
import java.time.Instant

/**
 * The result of taking the CDC/ADA Prediabetes Risk Test. Stored as a history
 * (autoGenerate id) so retakes over time are preserved rather than overwritten.
 */
@Entity(
    tableName = "prediabetes_risk_result",
    indices = [Index("dateTaken")],
)
data class PrediabetesRiskResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val score: Int,
    val dateTaken: Instant,
    val source: RiskResultSource,
)
