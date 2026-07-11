package com.enough.app.feature.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.entity.WeightEntry
import com.enough.app.data.repository.WeightRepository
import com.enough.app.domain.UnitConversions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class LogWeightUiState(
    val weightLbText: String = "",
    val saved: Boolean = false,
) {
    val weightLb: Double? = weightLbText.trim().toDoubleOrNull()?.takeIf { it > 0 }
    val canSave: Boolean get() = weightLb != null
}

/** Backs the manual weight-entry screen. Input is in pounds; stored in kg. */
class LogWeightViewModel(
    private val weightRepository: WeightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogWeightUiState())
    val uiState: StateFlow<LogWeightUiState> = _uiState.asStateFlow()

    fun onWeightChange(text: String) = _uiState.update { it.copy(weightLbText = text) }

    fun save() {
        val lb = _uiState.value.weightLb ?: return
        viewModelScope.launch {
            weightRepository.add(
                WeightEntry(weightKg = UnitConversions.lbToKg(lb), timestamp = Instant.now()),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }
}
