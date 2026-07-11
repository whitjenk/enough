package com.enough.app.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.model.WeightTrendDirection
import com.enough.app.data.repository.ActivityRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.DayRange
import com.enough.app.domain.progress.DailyFiber
import com.enough.app.domain.progress.ProgressCalculations
import com.enough.app.domain.rules.RulesEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class ProgressUiState(
    val fiberSeries: List<DailyFiber> = emptyList(),
    val fiberTargetG: Int = 0,
    val loggedDaySeries: List<Boolean> = emptyList(),
    val daysLoggedLast7: Int = 0,
    val windowDays: Int = WINDOW_DAYS,
    val currentWeightKg: Double? = null,
    val startWeightKg: Double? = null,
    val targetWeightKg: Double? = null,
    val weightTrend: WeightTrendDirection = WeightTrendDirection.UNKNOWN,
    val activityGoalType: ActivityGoalType? = null,
    val weeklyActivityMinutes: Int = 0,
    val activityGoalMinutes: Int? = null,
    val isLoading: Boolean = true,
) {
    companion object {
        const val WINDOW_DAYS = 7
    }
}

/**
 * Observes the last-7-days history and the user's goals and derives the Progress
 * view: the fiber-gap trend, weight trend, weekly movement vs. goal, and a
 * streak-free consistency count. All aggregation is delegated to pure
 * calculators ([ProgressCalculations], [RulesEngine]).
 */
class ProgressViewModel(
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val activityRepository: ActivityRepository,
    private val weightRepository: WeightRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
) : ViewModel() {

    private val window = ProgressUiState.WINDOW_DAYS

    /**
     * "Now" is resolved at collection time (inside [flow]), not at construction,
     * so the window rolls over correctly when the screen is re-observed on a new
     * day — a retained ViewModel would otherwise keep showing the day it was
     * created. [stateIn] with [SharingStarted.WhileSubscribed] re-runs this on
     * return to the foreground.
     */
    val uiState: StateFlow<ProgressUiState> = flow {
        emitAll(observeProgress(now()))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState(),
    )

    private fun observeProgress(now: Instant): Flow<ProgressUiState> {
        val todayRange: DayRange = DayRange.today(zone, now)
        val windowStartMillis =
            todayRange.start.minus(Duration.ofDays((window - 1).toLong())).toEpochMilli()
        val windowEndMillis = todayRange.endMillis

        return combine(
            goalRepository.goal,
            mealRepository.observeBetween(windowStartMillis, windowEndMillis),
            activityRepository.observeBetween(windowStartMillis, windowEndMillis),
            weightRepository.all,
        ) { goal, meals, activities, weights ->
            val logInstants =
                meals.map { it.meal.timestamp } +
                    activities.map { it.timestamp } +
                    weights.map { it.timestamp }

            val weeklyMinutes = activities
                .filter { it.unit == ActivityUnit.MINUTES }
                .sumOf { it.amount }
            val loggedSeries = ProgressCalculations.loggedDaySeries(window, logInstants, now, zone)

            ProgressUiState(
                fiberSeries = ProgressCalculations.fiberSeries(window, meals, now, zone),
                fiberTargetG = goal?.fiberGramsTarget ?: 0,
                loggedDaySeries = loggedSeries,
                daysLoggedLast7 = loggedSeries.count { it },
                currentWeightKg = weights.lastOrNull()?.weightKg,
                startWeightKg = goal?.startWeightKg,
                targetWeightKg = goal?.targetWeightKg,
                weightTrend = RulesEngine.weightTrend(weights.map { it.weightKg }),
                activityGoalType = goal?.activityGoalType,
                weeklyActivityMinutes = weeklyMinutes,
                activityGoalMinutes = goal?.takeIf { it.activityGoalType == ActivityGoalType.MINUTES }
                    ?.activityGoalValue,
                isLoading = false,
            )
        }
    }
}
