package com.enough.app.data.repository

import com.enough.app.data.local.dao.UserGoalDao
import com.enough.app.data.local.entity.UserGoal
import kotlinx.coroutines.flow.Flow

/** Reads and writes the single [UserGoal] row. */
class GoalRepository(private val userGoalDao: UserGoalDao) {
    val goal: Flow<UserGoal?> = userGoalDao.observe()

    suspend fun getGoal(): UserGoal? = userGoalDao.get()

    suspend fun saveGoal(goal: UserGoal) = userGoalDao.upsert(goal)
}
