package com.enough.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.entity.PrediabetesRiskResult
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.model.Glp1Stance
import com.enough.app.data.model.RiskResultSource
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.RiskResultRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.UnitConversions
import com.enough.app.domain.goals.GoalCalculator
import com.enough.app.domain.reminder.ReminderTimeOption
import com.enough.app.domain.risk.RiskScorer
import com.enough.app.health.HealthConnectManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Drives the onboarding flow. Holds a single [OnboardingUiState] exposed as a
 * [StateFlow] (unidirectional data flow); all scoring/goal math is delegated to
 * the plain-Kotlin [RiskScorer]/[GoalCalculator], never done here.
 */
class OnboardingViewModel(
    private val goalRepository: GoalRepository,
    private val riskResultRepository: RiskResultRepository,
    private val weightRepository: WeightRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthConnectManager: HealthConnectManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // --- Step navigation ---

    /** Default entry path: skip the quiz, go straight to goal-setting. */
    fun startDefaultPath() = _uiState.update {
        it.copy(step = OnboardingStep.GOALS, riskTestPathChosen = false)
    }

    /** Optional entry path: take the CDC/ADA risk test first. */
    fun startRiskTestPath() = _uiState.update {
        it.copy(step = OnboardingStep.RISK_TEST, riskTestPathChosen = true)
    }

    /** Compute and show the score; only valid once the form is complete. */
    fun submitRiskTest() {
        val answers = _uiState.value.riskForm.toAnswers() ?: return
        _uiState.update {
            it.copy(step = OnboardingStep.RISK_RESULT, riskScore = RiskScorer.score(answers))
        }
    }

    /**
     * From the risk result into goal-setting, prefilling the weight the risk test
     * already captured so opting into a weight goal doesn't require re-typing.
     */
    fun goToGoals() = _uiState.update { state ->
        val goals = if (state.goalsForm.weightLbText.isBlank() && state.riskForm.weightLb != null) {
            state.goalsForm.copy(weightLbText = state.riskForm.weightLbText.trim())
        } else {
            state.goalsForm
        }
        state.copy(step = OnboardingStep.GOALS, goalsForm = goals)
    }

    /** From goals into the optional extra questions. */
    fun goToExtras() = setStep(OnboardingStep.EXTRAS)

    /** From the extras into the one-time daily-reminder offer (SPEC §7.8). */
    fun goToReminder() = setStep(OnboardingStep.REMINDER)

    fun onReminderTimeChange(option: ReminderTimeOption) =
        _uiState.update { it.copy(reminderTime = option) }

    /**
     * The person accepted the reminder. [permissionGranted] is false when the OS
     * prompt was declined — in which case nothing is enabled, because a reminder
     * that can't be posted isn't one.
     *
     * Either way the offer is marked as shown, so it is never made again.
     */
    fun acceptReminder(permissionGranted: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setReminderOfferShown()
            if (permissionGranted) {
                userPreferencesRepository.setReminderMinuteOfDay(
                    _uiState.value.reminderTime.minuteOfDay,
                )
                userPreferencesRepository.setReminderEnabled(true)
            }
            goToHealthConnect()
        }
    }

    /**
     * The person declined. This is a first-class answer: the offer is recorded as
     * made, reminders stay off, and nothing asks again (CLAUDE.md — the app does
     * not escalate toward someone who said no).
     */
    fun declineReminder() {
        viewModelScope.launch {
            userPreferencesRepository.setReminderOfferShown()
            goToHealthConnect()
        }
    }

    fun goToHealthConnect() {
        refreshHealthConnect()
        setStep(OnboardingStep.HEALTH_CONNECT)
    }

    fun back() {
        _uiState.update { state ->
            val previous = when (state.step) {
                OnboardingStep.WELCOME -> OnboardingStep.WELCOME
                OnboardingStep.RISK_TEST -> OnboardingStep.WELCOME
                OnboardingStep.RISK_RESULT -> OnboardingStep.RISK_TEST
                OnboardingStep.GOALS ->
                    if (state.riskTestPathChosen) OnboardingStep.RISK_RESULT else OnboardingStep.WELCOME
                OnboardingStep.EXTRAS -> OnboardingStep.GOALS
                OnboardingStep.REMINDER -> OnboardingStep.EXTRAS
                OnboardingStep.HEALTH_CONNECT -> OnboardingStep.REMINDER
            }
            state.copy(step = previous)
        }
    }

    private fun setStep(step: OnboardingStep) = _uiState.update { it.copy(step = step) }

    // --- Form edits (state hoisted here; the UI is stateless) ---

    fun onRiskFormChange(form: RiskTestForm) = _uiState.update { it.copy(riskForm = form) }

    fun onGoalsFormChange(form: GoalsForm) = _uiState.update { it.copy(goalsForm = form) }

    fun onExtrasFormChange(form: ExtrasForm) = _uiState.update { it.copy(extrasForm = form) }

    // --- Health Connect ---

    fun refreshHealthConnect() {
        viewModelScope.launch {
            val availability = healthConnectManager.availability()
            val granted = healthConnectManager.hasAllPermissions()
            _uiState.update {
                it.copy(
                    healthConnect = it.healthConnect.copy(
                        availability = availability,
                        permissionsGranted = granted,
                    ),
                )
            }
        }
    }

    val healthConnectPermissions: Set<String> get() = healthConnectManager.permissions

    /** The ActivityResult contract used by the composable permission launcher. */
    fun permissionsRequestContract() = healthConnectManager.requestPermissionsContract()

    fun onPermissionsResult() = refreshHealthConnect()

    // --- Finish ---

    /**
     * Persist onboarding: the goals (weight optional), the risk result if the
     * optional risk-test path was taken, then mark onboarding done. Works with or
     * without the risk test — no risk answers are required.
     */
    fun finishOnboarding() {
        val state = _uiState.value
        val goalsForm = state.goalsForm
        if (!goalsForm.isComplete) return
        val dailyCalories = goalsForm.dailyCalories ?: return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()

            // Risk result is stored only when the optional risk test was taken.
            if (state.riskTestPathChosen) {
                val answers = state.riskForm.toAnswers()
                val score = state.riskScore ?: answers?.let(RiskScorer::score)
                if (answers != null && score != null) {
                    riskResultRepository.saveResult(
                        PrediabetesRiskResult(
                            score = score.total,
                            dateTaken = now,
                            source = RiskResultSource.CDC_ADA_RISK_TEST,
                        ),
                    )
                }
            }

            // Weight goal is opt-in. When included, the entered weight is the
            // starting weight and is logged as the first WeightEntry.
            val startWeightKg = if (goalsForm.includeWeightGoal) {
                goalsForm.weightLb?.let(UnitConversions::lbToKg)
            } else {
                null
            }
            val weightLossPercent =
                if (startWeightKg != null) goalsForm.weightLossPercent.toDouble() else null
            val targetWeightKg = if (startWeightKg != null && weightLossPercent != null) {
                GoalCalculator.targetWeightKg(startWeightKg, weightLossPercent)
            } else {
                null
            }

            if (startWeightKg != null) {
                weightRepository.add(WeightEntry(weightKg = startWeightKg, timestamp = now))
            }

            val extras = state.extrasForm
            goalRepository.saveGoal(
                UserGoal(
                    startWeightKg = startWeightKg,
                    targetWeightKg = targetWeightKg,
                    weightLossPercent = weightLossPercent,
                    activityGoalType = goalsForm.activityGoalType,
                    activityGoalValue = goalsForm.activityGoalValue,
                    activityGoalCustomLabel = goalsForm.activityCustomLabel
                        .trim().takeIf { it.isNotEmpty() },
                    dailyCalorieEstimate = dailyCalories,
                    fiberGramsTarget = GoalCalculator.fiberTargetGrams(dailyCalories),
                    createdAt = now,
                    dietaryRestrictions = extras.dietaryRestrictions,
                    dietaryRestrictionOther = extras.dietaryOther.trim().takeIf { it.isNotEmpty() },
                    personalWhy = extras.personalWhy.trim().takeIf { it.isNotEmpty() },
                    takesGLP1Medication = extras.takesGLP1 == true,
                    // Onboarding keeps the simple yes/no; the richer "coming off"
                    // stance is refined later in Settings (SPEC §7.6 Step 4).
                    glp1Stance = if (extras.takesGLP1 == true) Glp1Stance.ON else Glp1Stance.NOT,
                ),
            )

            userPreferencesRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }
}
