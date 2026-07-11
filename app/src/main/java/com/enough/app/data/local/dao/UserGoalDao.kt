package com.enough.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.enough.app.data.local.entity.UserGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGoalDao {
    @Upsert
    suspend fun upsert(goal: UserGoal)

    @Query("SELECT * FROM user_goal WHERE id = :id LIMIT 1")
    fun observe(id: Int = UserGoal.SINGLETON_ID): Flow<UserGoal?>

    @Query("SELECT * FROM user_goal WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = UserGoal.SINGLETON_ID): UserGoal?

    @Query("DELETE FROM user_goal")
    suspend fun deleteAll()
}
