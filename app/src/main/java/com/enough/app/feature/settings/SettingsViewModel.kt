package com.enough.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.Glp1Stance
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.CheckInRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.MealRepository
import com.enough.app.domain.feedback.FeedbackSummary
import com.enough.app.domain.reminder.ReminderTimeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class SettingsUiState(
    val healthConnectSyncEnabled: Boolean = true,
    val estimateCalibration: EstimateCalibration = EstimateCalibration.BALANCED,
    val hideNumbersMode: Boolean = false,
    val glp1Stance: Glp1Stance = Glp1Stance.NOT,
    /** Daily reminder on/off and its time (SPEC §7.8). */
    val reminderEnabled: Boolean = false,
    val reminderTime: ReminderTimeOption = ReminderTimeOption.DEFAULT,
    /** The anonymous "help improve" summary once the person asks to see it; null until then. */
    val feedback: FeedbackSummary? = null,
)

/**
 * Backs the Settings screen: the Health Connect sync toggle, the fiber-estimate
 * calibration preference, the anonymous "help improve" summary, and the
 * destructive "delete my data" action. The wipe itself is delegated to
 * [wipeAllUserData] (the app container), which clears every local table and
 * preference.
 */
class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val goalRepository: GoalRepository,
    private val mealRepository: MealRepository,
    private val checkInRepository: CheckInRepository,
    private val wipeAllUserData: suspend () -> Unit,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val feedback = MutableStateFlow<FeedbackSummary?>(null)

    val uiState: StateFlow<SettingsUiState> =
        combine(
            userPreferencesRepository.healthConnectSyncEnabled,
            goalRepository.goal,
            feedback,
            userPreferencesRepository.reminderEnabled,
            userPreferencesRepository.reminderMinuteOfDay,
        ) { syncEnabled, goal, feedbackSummary, reminderEnabled, reminderMinuteOfDay ->
            SettingsUiState(
                healthConnectSyncEnabled = syncEnabled,
                estimateCalibration = goal?.estimateCalibration ?: EstimateCalibration.BALANCED,
                hideNumbersMode = goal?.hideNumbersMode ?: false,
                glp1Stance = goal?.glp1Stance ?: Glp1Stance.NOT,
                feedback = feedbackSummary,
                reminderEnabled = reminderEnabled,
                reminderTime = ReminderTimeOption.nearest(reminderMinuteOfDay),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    /**
     * Build the anonymous aggregate summary on-device so the person can review
     * exactly what it contains before choosing to share it. Nothing is sent here.
     */
    fun prepareFeedback() {
        viewModelScope.launch {
            feedback.value = FeedbackSummary.from(
                meals = mealRepository.allMeals(),
                goal = goalRepository.getGoal(),
                zone = zone,
                everCheckedIn = checkInRepository.everCheckedIn(),
                everSharedCard = userPreferencesRepository.everSharedCard.first(),
            )
        }
    }

    fun setHealthConnectSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHealthConnectSyncEnabled(enabled)
        }
    }

    /**
     * Persists a new fiber-estimate lean. A no-op if there's no goal row yet
     * (calibration lives on the goal, which onboarding always creates first).
     */
    fun setEstimateCalibration(calibration: EstimateCalibration) {
        viewModelScope.launch {
            val goal = goalRepository.getGoal() ?: return@launch
            goalRepository.saveGoal(goal.copy(estimateCalibration = calibration))
        }
    }

    /** Toggle hide-numbers mode. No-op until onboarding has created the goal row. */
    fun setHideNumbersMode(enabled: Boolean) {
        viewModelScope.launch {
            val goal = goalRepository.getGoal() ?: return@launch
            goalRepository.saveGoal(goal.copy(hideNumbersMode = enabled))
        }
    }

    /**
     * Turn the daily reminder on or off, and persist the time (SPEC §7.8).
     *
     * Only the preference is written here; scheduling and cancelling the actual
     * work needs a Context and is done by the screen, keeping Android out of the
     * ViewModel.
     */
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setReminderEnabled(enabled) }
    }

    fun setReminderTime(option: ReminderTimeOption) {
        viewModelScope.launch {
            userPreferencesRepository.setReminderMinuteOfDay(option.minuteOfDay)
        }
    }

    /** Update the GLP-1 stance (drives fiber tone; SPEC §7.6 Step 4). No-op until a goal exists. */
    fun setGlp1Stance(stance: Glp1Stance) {
        viewModelScope.launch {
            val goal = goalRepository.getGoal() ?: return@launch
            goalRepository.saveGoal(goal.copy(glp1Stance = stance))
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            wipeAllUserData()
        }
    }
}
