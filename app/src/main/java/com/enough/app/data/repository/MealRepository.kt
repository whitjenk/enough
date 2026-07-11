package com.enough.app.data.repository

import com.enough.app.data.local.dao.MealEntryDao
import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.domain.DayRange
import kotlinx.coroutines.flow.Flow

/** Reads and writes meal entries. */
class MealRepository(private val dao: MealEntryDao) {
    fun observeForDay(range: DayRange): Flow<List<MealWithFood>> =
        dao.observeBetween(range.startMillis, range.endMillis)

    suspend fun add(entry: MealEntry): Long = dao.insert(entry)

    suspend fun delete(entry: MealEntry) = dao.delete(entry)

    suspend fun latestTimestampMillis(): Long? = dao.latestTimestamp()
}
