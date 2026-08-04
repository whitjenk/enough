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

    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<MealWithFood>> =
        dao.observeBetween(startMillis, endMillis)

    /** One-shot read of a day's meals (e.g. to score a completed day). */
    suspend fun mealsForDay(range: DayRange): List<MealWithFood> =
        dao.getBetween(range.startMillis, range.endMillis)

    /** One-shot read of all logged meals (e.g. to build an aggregate summary). */
    suspend fun allMeals(): List<MealWithFood> = dao.getAll()

    suspend fun add(entry: MealEntry): Long = dao.insert(entry)

    suspend fun delete(entry: MealEntry) = dao.delete(entry)

    suspend fun latestTimestampMillis(): Long? = dao.latestTimestamp()
}
