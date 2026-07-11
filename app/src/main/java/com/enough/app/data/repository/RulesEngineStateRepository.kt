package com.enough.app.data.repository

import com.enough.app.data.local.dao.RulesEngineStateDao
import com.enough.app.data.local.entity.RulesEngineState
import kotlinx.coroutines.flow.Flow

/** Reads and writes the cached rules-engine snapshot. */
class RulesEngineStateRepository(private val dao: RulesEngineStateDao) {
    val state: Flow<RulesEngineState?> = dao.observe()

    suspend fun save(state: RulesEngineState) = dao.upsert(state)
}
