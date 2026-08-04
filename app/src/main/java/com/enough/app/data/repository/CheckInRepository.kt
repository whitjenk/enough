package com.enough.app.data.repository

import com.enough.app.data.local.dao.DailyCheckInDao
import com.enough.app.data.local.entity.DailyCheckIn
import com.enough.app.data.model.FeltLevel
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** Reads and writes the optional daily felt check-in (SPEC §0.8 / §7.6 Step 1). */
class CheckInRepository(private val dao: DailyCheckInDao) {
    fun observeForDate(date: LocalDate): Flow<DailyCheckIn?> = dao.observeForDate(date)

    fun observeBetween(startInclusive: LocalDate, endInclusive: LocalDate): Flow<List<DailyCheckIn>> =
        dao.observeBetween(startInclusive, endInclusive)

    suspend fun setFelt(date: LocalDate, felt: FeltLevel, at: Instant) =
        dao.upsert(DailyCheckIn(date = date, felt = felt, createdAt = at))
}
