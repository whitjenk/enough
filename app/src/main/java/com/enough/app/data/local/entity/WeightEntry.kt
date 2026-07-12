package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/** A manually logged (or Health Connect synced) body-weight reading, in kilograms. */
@Entity(
    tableName = "weight_entry",
    indices = [Index("timestamp")],
)
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weightKg: Double,
    val timestamp: Instant,
)
