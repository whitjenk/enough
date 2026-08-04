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
import com.enough.app.data.model.FeltLevel
import com.enough.app.data.model.WeightTrendDirection
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.ActivityRepository
import com.enough.app.data.repository.CheckInRepository
import com.enough.app.data.repository.FoodRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.data.repository.RulesEngineStateRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.DayRange
import com.enough.app.domain.nutrition.MealNutrition
import com.enough.app.domain.rules.DailySwap
import com.enough.app.domain.rules.Nudge
import com.enough.app.domain.rules.NudgeGenerator
import com.enough.app.domain.rules.ResetMoment
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
import kotlinx.coroutines.flow.map
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
    val dailySwap: DailySwap.Swap? = null,
    val showResetMoment: Boolean = false,
    val todayFelt: FeltLevel? = null,
    val healthConnect: HealthConnectData = HealthConnectData(),
    val isLoading: Boolean = true,
) {
    val fiberTargetG: Int get() = goal?.fiberGramsTarget ?: 0
    val calibration: EstimateCalibration get() = goal?.estimateCalibration ?: EstimateCalibration.BALANCED

    /** Hide-numbers mode: show trend/qualitative signal instead of literal weight/fiber values (SPEC §23). */
    val hideNumbers: Boolean get() = goal?.hideNumbersMode == true
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
    private val checkInRepository: CheckInRepository,
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
        val todayDate = nowInstant.atZone(zone).toLocalDate()
        emitAll(
            combine(
                observeBaseState(today),
                healthData,
                checkInRepository.observeForDate(todayDate),
            ) { base, health, checkIn ->
                base.copy(healthConnect = health, todayFelt = checkIn?.felt)
            }.map { enrichAndPersist(it, nowInstant, today) },
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
        val restrictions = goal?.dietaryRestrictions ?: emptySet()
        val gentle = goal?.gentleFiber == true
        val comingOff = goal?.comingOffGlp1 == true
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
                restrictions = restrictions,
                otherRestriction = goal?.dietaryRestrictionOther,
                gentle = gentle,
            ),
            dailySwap = DailySwap.forDay(
                epochDay = today.start.atZone(zone).toLocalDate().toEpochDay(),
                candidates = suggestionFoods,
                restrictions = restrictions,
                otherRestriction = goal?.dietaryRestrictionOther,
                gentle = gentle,
                comingOff = comingOff,
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

    /**
     * One suspend step per emission: compute recency once, persist the rules
     * snapshot, and decide the reset-day moment. Returns the state enriched with
     * [TodayUiState.showResetMoment]. Best-effort.
     */
    private suspend fun enrichAndPersist(
        state: TodayUiState,
        now: Instant,
        today: DayRange,
    ): TodayUiState {
        val lastLogMillis = listOfNotNull(
            mealRepository.latestTimestampMillis(),
            activityRepository.latestTimestampMillis(),
            weightRepository.latestTimestampMillis(),
        ).maxOrNull()
        val daysSinceLastLog = RulesEngine.daysSinceLastLog(
            lastLog = lastLogMillis?.let(Instant::ofEpochMilli),
            now = now,
            zone = zone,
        )

        persistRulesState(state, now, daysSinceLastLog ?: 0)
        return state.copy(showResetMoment = evaluateResetMoment(state, today, daysSinceLastLog))
    }

    /** Cache the rules-engine inputs/outputs for later phases. Best-effort. */
    private suspend fun persistRulesState(state: TodayUiState, now: Instant, daysSinceLastLog: Int) {
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

    /**
     * Decide whether the reset-day ("Enough") moment shows. "Wide miss" is scored
     * against the last completed day. Deciding here is side-effect free; the shown
     * day is recorded by [onResetMomentShown] only when the card actually renders,
     * so the once-per-episode budget is never spent on an emission the person
     * never saw.
     */
    private suspend fun evaluateResetMoment(
        state: TodayUiState,
        today: DayRange,
        daysSinceLastLog: Int?,
    ): Boolean {
        val todayEpochDay = today.start.atZone(zone).toLocalDate().toEpochDay()
        val target = state.fiberTargetG

        val hadWideMiss = if (target > 0) {
            val yesterday = DayRange.of(today.start.atZone(zone).toLocalDate().minusDays(1), zone)
            val yesterdayMeals = mealRepository.mealsForDay(yesterday)
            yesterdayMeals.isNotEmpty() &&
                MealNutrition.fiberGrams(yesterdayMeals, state.calibration) < target * ResetMoment.WIDE_MISS_FRACTION
        } else {
            false
        }

        return ResetMoment.shouldShow(
            todayEpochDay = todayEpochDay,
            daysSinceLastLog = daysSinceLastLog,
            hadWideMissYesterday = hadWideMiss,
            lastShownEpochDay = userPreferencesRepository.resetMomentShownEpochDay.first(),
        )
    }

    /**
     * Called by the UI when the reset-day card composes. Recording the shown day
     * keeps the card up for the rest of today and suppresses it for the rest of
     * this rough patch (see [ResetMoment.shouldShow]).
     */
    fun onResetMomentShown() {
        viewModelScope.launch {
            userPreferencesRepository.setResetMomentShownEpochDay(
                now().atZone(zone).toLocalDate().toEpochDay(),
            )
        }
    }

    /**
     * Record (or change) today's optional felt check-in. Keyed by today's calendar
     * date, so re-tapping replaces it; there is no way to make this "wrong" and
     * skipping it entirely costs nothing.
     */
    fun onCheckIn(felt: FeltLevel) {
        viewModelScope.launch {
            checkInRepository.setFelt(now().atZone(zone).toLocalDate(), felt, now())
        }
    }

    /** Record the aggregate "ever shared a card" signal on an explicit share tap (§7.6 Step 3). */
    fun markCardShared() {
        viewModelScope.launch { userPreferencesRepository.setEverSharedCard() }
    }

    /** Remove a logged meal (e.g. an accidental quick-log tap). */
    fun deleteMeal(item: MealWithFood) {
        viewModelScope.launch { mealRepository.delete(item.meal) }
    }
}
