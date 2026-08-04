package com.enough.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.enough.app.data.model.FeltLevel
import java.time.Instant
import java.time.LocalDate

/**
 * An optional, one-per-day felt reflection (SPEC §0.8 / IMPLEMENTATION_PLAN §7.6
 * Step 1). Keyed by calendar [date] so re-tapping a different answer the same day
 * replaces it rather than stacking rows. Entirely opt-in: a day with no row is a
 * skipped day, and skipping carries no penalty anywhere in the app.
 *
 * This is a separate, additive loop — it deliberately does **not** feed the
 * consistency count or the reset-day absence timer, so it can never turn a
 * reflection into an obligation.
 */
@Entity(tableName = "daily_check_in")
data class DailyCheckIn(
    @PrimaryKey val date: LocalDate,
    val felt: FeltLevel,
    val createdAt: Instant,
)
