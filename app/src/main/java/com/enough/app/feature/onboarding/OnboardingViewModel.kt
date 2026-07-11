package com.enough.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.entity.PrediabetesRiskResult
import com.enough.app.data.local.entity.UserGoal
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.model.RiskResultSource
import com.enough.app.data.preferences.UserPreferencesRepository
import com.enough.app.data.repository.GoalRepository
import com.enough.app.data.repository.RiskResultRepository
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.UnitConversions
import com.enough.app.domain.goals.GoalCalculator
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

    fun goToRiskTest() = setStep(OnboardingStep.RISK_TEST)

    /** Compute and show the score; only valid once the form is complete. */
    fun submitRiskTest() {
        val answers = _uiState.value.riskForm.toAnswers() ?: return
        _uiState.update {
            it.copy(step = OnboardingStep.RISK_RESULT, riskScore = RiskScorer.score(answers))
        }
    }

    fun goToGoals() = setStep(OnboardingStep.GOALS)

    fun goToHealthConnect() {
        refreshHealthConnect()
        setStep(OnboardingStep.HEALTH_CONNECT)
    }

    fun back() {
        val previous = when (_uiState.value.step) {
            OnboardingStep.WELCOME -> OnboardingStep.WELCOME
            OnboardingStep.RISK_TEST -> OnboardingStep.WELCOME
            OnboardingStep.RISK_RESULT -> OnboardingStep.RISK_TEST
            OnboardingStep.GOALS -> OnboardingStep.RISK_RESULT
            OnboardingStep.HEALTH_CONNECT -> OnboardingStep.GOALS
        }
        setStep(previous)
    }

    private fun setStep(step: OnboardingStep) = _uiState.update { it.copy(step = step) }

    // --- Form edits (state hoisted here; the UI is stateless) ---

    fun onRiskFormChange(form: RiskTestForm) = _uiState.update { it.copy(riskForm = form) }

    fun onGoalsFormChange(form: GoalsForm) = _uiState.update { it.copy(goalsForm = form) }

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

    /** Persist the risk result and all three goals, then mark onboarding done. */
    fun finishOnboarding() {
        val state = _uiState.value
        val answers = state.riskForm.toAnswers() ?: return
        val goalsForm = state.goalsForm
        if (!goalsForm.isComplete) return
        val dailyCalories = goalsForm.dailyCalories ?: return
        val score = state.riskScore ?: RiskScorer.score(answers)

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = Instant.now()
            val startWeightKg = answers.weightKg

            riskResultRepository.saveResult(
                PrediabetesRiskResult(
                    score = score.total,
                    dateTaken = now,
                    source = RiskResultSource.CDC_ADA_RISK_TEST,
                ),
            )

            // The weight entered during the risk test is the starting weight.
            weightRepository.add(WeightEntry(weightKg = startWeightKg, timestamp = now))

            goalRepository.saveGoal(
                UserGoal(
                    startWeightKg = startWeightKg,
                    targetWeightKg = GoalCalculator.targetWeightKg(
                        startWeightKg,
                        goalsForm.weightLossPercent.toDouble(),
                    ),
                    weightLossPercent = goalsForm.weightLossPercent.toDouble(),
                    activityGoalType = goalsForm.activityGoalType,
                    activityGoalValue = goalsForm.activityGoalValue,
                    activityGoalCustomLabel = goalsForm.activityCustomLabel
                        .trim().takeIf { it.isNotEmpty() },
                    dailyCalorieEstimate = dailyCalories,
                    fiberGramsTarget = GoalCalculator.fiberTargetGrams(dailyCalories),
                    createdAt = now,
                ),
            )

            userPreferencesRepository.setOnboardingCompleted(true)
            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }

    // Exposed for previews/tests that want to render a specific weight in kg.
    fun startWeightKgOrNull(): Double? = _uiState.value.riskForm.weightLb?.let(UnitConversions::lbToKg)
}
