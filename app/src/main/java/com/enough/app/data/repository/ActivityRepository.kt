package com.enough.app.data.repository

import com.enough.app.data.local.dao.ActivityEntryDao
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.domain.DayRange
import kotlinx.coroutines.flow.Flow

/** Reads and writes activity entries. */
class ActivityRepository(private val dao: ActivityEntryDao) {
    fun observeForDay(range: DayRange): Flow<List<ActivityEntry>> =
        dao.observeBetween(range.startMillis, range.endMillis)

    suspend fun add(entry: ActivityEntry): Long = dao.insert(entry)

    suspend fun latestTimestampMillis(): Long? = dao.latestTimestamp()

    suspend fun minutesLoggedSince(startMillis: Long): Int = dao.minutesLoggedSince(startMillis)
}
