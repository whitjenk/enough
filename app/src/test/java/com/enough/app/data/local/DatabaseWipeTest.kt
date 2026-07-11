package com.enough.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.local.entity.PrediabetesRiskResult
import com.enough.app.data.local.entity.RulesEngineState
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.model.MealSource
import com.enough.app.data.model.NudgeType
import com.enough.app.data.model.RiskResultSource
import com.enough.app.data.model.WeightTrendDirection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * Verifies "Delete my data" actually clears every local table, not just hides
 * rows — exercising the same clearAllTables() the app container calls.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseWipeTest {

    private lateinit var db: EnoughDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnoughDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `clearAllTables removes every user row`() = runBlocking {
        val now = Instant.now()
        db.foodDao().insertAll(listOf(Food(id = 1, name = "Banana", servingLabel = "1", carbsG = 27.0, fiberG = 3.1, proteinG = 1.3)))
        db.mealEntryDao().insert(MealEntry(foodId = 1, servingsMultiplier = 1.0, timestamp = now, source = MealSource.TEXT))
        db.weightEntryDao().insert(WeightEntry(weightKg = 80.0, timestamp = now))
        db.activityEntryDao().insert(ActivityEntry(unit = ActivityUnit.MINUTES, amount = 30, note = null, timestamp = now))
        db.userGoalDao().upsert(
            UserGoal(
                startWeightKg = 80.0, targetWeightKg = 76.0, weightLossPercent = 5.0,
                activityGoalType = ActivityGoalType.MINUTES, activityGoalValue = 150,
                activityGoalCustomLabel = null, dailyCalorieEstimate = 2000, fiberGramsTarget = 28, createdAt = now,
            ),
        )
        db.prediabetesRiskResultDao().insert(PrediabetesRiskResult(score = 5, dateTaken = now, source = RiskResultSource.CDC_ADA_RISK_TEST))
        db.rulesEngineStateDao().upsert(
            RulesEngineState(
                daysSinceLastLog = 0, weightTrendDirection = WeightTrendDirection.DOWN,
                activityMinutesThisWeek = 30, fiberGapToday = 22.0, lastNudgeType = NudgeType.FIBER_GAP, updatedAt = now,
            ),
        )

        db.clearAllTables()

        assertEquals(0, db.foodDao().count())
        assertEquals(0, db.mealEntryDao().getAll().size)
        assertEquals(0, db.weightEntryDao().getAll().size)
        assertEquals(0, db.activityEntryDao().getAll().size)
        assertNull(db.userGoalDao().get())
        assertEquals(0, db.prediabetesRiskResultDao().getAll().size)
        assertNull(db.rulesEngineStateDao().get())
    }
}
