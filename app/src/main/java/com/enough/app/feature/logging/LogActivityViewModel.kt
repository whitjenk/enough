package com.enough.app.feature.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.model.ActivityGoalType
import com.enough.app.data.model.ActivityUnit
import com.enough.app.data.repository.ActivityRepository
import com.enough.app.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class LogActivityUiState(
    val unit: ActivityUnit = ActivityUnit.MINUTES,
    val customGoalLabel: String? = null,
    val amountText: String = "",
    val note: String = "",
    val saved: Boolean = false,
) {
    val amount: Int? = amountText.trim().toIntOrNull()?.takeIf { it > 0 }
    val canSave: Boolean get() = amount != null
}

/**
 * Backs the manual activity-entry screen. It respects the activity goal type
 * chosen during onboarding (minutes / steps / custom) so the person logs in the
 * same terms they set their goal in (SPEC.md §5).
 */
class LogActivityViewModel(
    private val activityRepository: ActivityRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogActivityUiState())
    val uiState: StateFlow<LogActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val goal = goalRepository.getGoal()
            val unit = when (goal?.activityGoalType) {
                ActivityGoalType.STEPS -> ActivityUnit.STEPS
                ActivityGoalType.CUSTOM -> ActivityUnit.CUSTOM
                else -> ActivityUnit.MINUTES
            }
            _uiState.update { it.copy(unit = unit, customGoalLabel = goal?.activityGoalCustomLabel) }
        }
    }

    fun onAmountChange(text: String) = _uiState.update { it.copy(amountText = text) }

    fun onNoteChange(text: String) = _uiState.update { it.copy(note = text) }

    fun save() {
        val state = _uiState.value
        val amount = state.amount ?: return
        viewModelScope.launch {
            activityRepository.add(
                ActivityEntry(
                    unit = state.unit,
                    amount = amount,
                    note = state.note.trim().takeIf { it.isNotEmpty() },
                    timestamp = Instant.now(),
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
