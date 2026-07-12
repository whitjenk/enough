package com.enough.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.RulesEngineState
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.WeightTrendDirection
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.ActivityRepository
import com.enough.app.data.repository.FoodRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.data.repository.RulesEngineStateRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.DayRange
import com.enough.app.domain.nutrition.MealNutrition
import com.enough.app.domain.rules.Nudge
import com.enough.app.domain.rules.NudgeGenerator
import com.enough.app.domain.rules.RulesEngine
import com.enough.app.health.HealthConnectManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** Steps/sleep read from Health Connect (optional; null when unavailable). */
data class HealthConnectData(
    val stepsToday: Long? = null,
    val sleepMinutesLastNight: Long? = null,
) {
    val hasAny: Boolean get() = stepsToday != null || sleepMinutesLastNight != null
}

data class TodayUiState(
    val goal: UserGoal? = null,
    val meals: List<MealWithFood> = emptyList(),
    val fiberSoFarG: Double = 0.0,
    val latestWeight: WeightEntry? = null,
    val weightTrend: WeightTrendDirection = WeightTrendDirection.UNKNOWN,
    val activities: List<ActivityEntry> = emptyList(),
    val nudge: Nudge = Nudge.None,
    val healthConnect: HealthConnectData = HealthConnectData(),
    val isLoading: Boolean = true,
) {
    val fiberTargetG: Int get() = goal?.fiberGramsTarget ?: 0
}

/**
 * Observes today's logged data and the user's goals, runs the pure rules engine
 * over them (fiber gap → daily nudge, weight trend), reads optional steps/sleep
 * from Health Connect, and exposes a single [TodayUiState]. Also persists a
 * [RulesEngineState] snapshot for later phases.
 */
class TodayViewModel(
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val weightRepository: WeightRepository,
    private val activityRepository: ActivityRepository,
    private val foodRepository: FoodRepository,
    private val rulesEngineStateRepository: RulesEngineStateRepository,
    private val healthConnectManager: HealthConnectManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Instant = Instant::now,
) : ViewModel() {

    private val suggestions = MutableStateFlow<List<Food>>(emptyList())
    private val healthData = MutableStateFlow(HealthConnectData())

    init {
        viewModelScope.launch { suggestions.value = foodRepository.topFiberFoods() }
        loadHealthConnectData()
    }

    /**
     * "Now"/"today" are resolved at collection time (inside [flow]), not at
     * construction, so a retained ViewModel rolls over to the new day when the
     * screen is re-observed instead of pinning the day it was created on.
     * [stateIn] with [SharingStarted.WhileSubscribed] re-runs this on return to
     * the foreground.
     */
    val uiState: StateFlow<TodayUiState> = flow {
        val nowInstant = now()
        val today = DayRange.today(zone, nowInstant)
        emitAll(
            combine(observeBaseState(today), healthData) { base, health ->
                base.copy(healthConnect = health)
            }.onEach { persistRulesState(it, nowInstant) },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )

    private fun observeBaseState(today: DayRange): Flow<TodayUiState> = combine(
        goalRepository.goal,
        mealRepository.observeForDay(today),
        weightRepository.all,
        activityRepository.observeForDay(today),
        suggestions,
    ) { goal, meals, weights, activities, suggestionFoods ->
        val calibration = goal?.estimateCalibration ?: EstimateCalibration.BALANCED
        val fiberSoFar = MealNutrition.fiberGrams(meals, calibration)
        val target = goal?.fiberGramsTarget ?: 0
        TodayUiState(
            goal = goal,
            meals = meals,
            fiberSoFarG = fiberSoFar,
            latestWeight = weights.lastOrNull(),
            weightTrend = RulesEngine.weightTrend(weights.map { it.weightKg }),
            activities = activities,
            nudge = NudgeGenerator.generate(
                fiberSoFarG = fiberSoFar,
                targetG = target,
                suggestions = suggestionFoods,
                restrictions = goal?.dietaryRestrictions ?: emptySet(),
                otherRestriction = goal?.dietaryRestrictionOther,
                gentle = goal?.takesGLP1Medication == true,
            ),
            isLoading = false,
        )
    }

    private fun loadHealthConnectData() {
        viewModelScope.launch {
            if (!userPreferencesRepository.healthConnectSyncEnabled.first()) return@launch
            val nowInstant = now()
            val today = DayRange.today(zone, nowInstant)
            val steps = healthConnectManager.readSteps(today.start, today.endExclusive)
            // "Last night": from mid-evening yesterday through this morning.
            val sleepStart = today.start.minus(Duration.ofHours(6))
            val sleep = healthConnectManager.readSleepMinutes(sleepStart, nowInstant)
            healthData.value = HealthConnectData(stepsToday = steps, sleepMinutesLastNight = sleep)
        }
    }

    /** Cache the rules-engine inputs/outputs for later phases. Best-effort. */
    private suspend fun persistRulesState(state: TodayUiState, now: Instant) {
        val lastLogMillis = listOfNotNull(
            mealRepository.latestTimestampMillis(),
            activityRepository.latestTimestampMillis(),
            weightRepository.latestTimestampMillis(),
        ).maxOrNull()
        val daysSinceLastLog = RulesEngine.daysSinceLastLog(
            lastLog = lastLogMillis?.let(Instant::ofEpochMilli),
            now = now,
            zone = zone,
        ) ?: 0
        val weekStartMillis = now.minus(Duration.ofDays(7)).toEpochMilli()

        rulesEngineStateRepository.save(
            RulesEngineState(
                daysSinceLastLog = daysSinceLastLog,
                weightTrendDirection = state.weightTrend,
                activityMinutesThisWeek = activityRepository.minutesLoggedSince(weekStartMillis),
                fiberGapToday = RulesEngine.fiberGapG(state.fiberSoFarG, state.fiberTargetG),
                lastNudgeType = state.nudge.type,
                updatedAt = now,
            ),
        )
    }
}
