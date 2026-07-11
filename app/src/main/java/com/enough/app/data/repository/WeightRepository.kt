package com.enough.app.data.repository

import com.enough.app.data.local.dao.WeightEntryDao
import com.enough.app.data.local.entity.WeightEntry
import kotlinx.coroutines.flow.Flow

/** Reads and writes body-weight entries. */
class WeightRepository(private val dao: WeightEntryDao) {
    val latest: Flow<WeightEntry?> = dao.observeLatest()
    val all: Flow<List<WeightEntry>> = dao.observeAll()

    suspend fun getLatest(): WeightEntry? = dao.getLatest()

    suspend fun add(entry: WeightEntry): Long = dao.insert(entry)

    suspend fun latestTimestampMillis(): Long? = dao.latestTimestamp()
}
