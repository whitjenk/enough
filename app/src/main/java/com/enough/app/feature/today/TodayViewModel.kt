package com.enough.app.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.repository.ActivityRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.DayRange
import com.enough.app.domain.nutrition.MealNutrition
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

data class TodayUiState(
    val goal: UserGoal? = null,
    val meals: List<MealWithFood> = emptyList(),
    val fiberSoFarG: Double = 0.0,
    val latestWeight: WeightEntry? = null,
    val activities: List<ActivityEntry> = emptyList(),
    val isLoading: Boolean = true,
) {
    val fiberTargetG: Int get() = goal?.fiberGramsTarget ?: 0
}

/**
 * Observes today's logged data (meals, latest weight, activity) plus the user's
 * goals, and exposes it as a single [TodayUiState]. Fiber math is delegated to
 * the pure [MealNutrition]. The daily fiber-gap nudge is layered on in Task 5.
 */
class TodayViewModel(
    goalRepository: GoalRepository,
    mealRepository: MealRepository,
    weightRepository: WeightRepository,
    activityRepository: ActivityRepository,
    zone: ZoneId = ZoneId.systemDefault(),
    now: Instant = Instant.now(),
) : ViewModel() {

    private val today: DayRange = DayRange.today(zone, now)

    val uiState: StateFlow<TodayUiState> = combine(
        goalRepository.goal,
        mealRepository.observeForDay(today),
        weightRepository.latest,
        activityRepository.observeForDay(today),
    ) { goal, meals, weight, activities ->
        TodayUiState(
            goal = goal,
            meals = meals,
            fiberSoFarG = MealNutrition.fiberGrams(meals),
            latestWeight = weight,
            activities = activities,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState(),
    )
}
