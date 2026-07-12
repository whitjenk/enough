package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.enough.app.data.local.entity.RulesEngineState
import kotlinx.coroutines.flow.Flow

@Dao
interface RulesEngineStateDao {
    @Upsert
    suspend fun upsert(state: RulesEngineState)

    @Query("SELECT * FROM rules_engine_state WHERE id = :id LIMIT 1")
    fun observe(id: Int = RulesEngineState.SINGLETON_ID): Flow<RulesEngineState?>

    @Query("SELECT * FROM rules_engine_state WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = RulesEngineState.SINGLETON_ID): RulesEngineState?

    @Query("DELETE FROM rules_engine_state")
    suspend fun deleteAll()
}
