package com.enough.app.feature.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.MealSource
import com.enough.app.data.repository.FoodRepository
import com.enough.app.data.repository.MealRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class AddMealUiState(
    val query: String = "",
    val results: List<Food> = emptyList(),
    val isSearching: Boolean = false,
    val selectedFood: Food? = null,
    val servingsText: String = "1",
    val saved: Boolean = false,
) {
    val servings: Double? = servingsText.trim().toDoubleOrNull()?.takeIf { it > 0 }
    val canSave: Boolean get() = selectedFood != null && servings != null
}

/**
 * Backs the add-meal screen: debounced text search over the local food list,
 * a serving-size multiplier, and a save into [MealEntry]. Timestamps the entry
 * as now with [MealSource.TEXT] (chosen from the searchable list).
 */
class AddMealViewModel(
    private val foodRepository: FoodRepository,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMealUiState())
    val uiState: StateFlow<AddMealUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, selectedFood = null) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(SEARCH_DEBOUNCE_MS)
            val results = foodRepository.search(query)
            _uiState.update { it.copy(results = results, isSearching = false) }
        }
    }

    fun onSelectFood(food: Food) {
        _uiState.update { it.copy(selectedFood = food, query = food.name, results = emptyList()) }
    }

    fun onServingsChange(text: String) {
        _uiState.update { it.copy(servingsText = text) }
    }

    fun save() {
        val state = _uiState.value
        val food = state.selectedFood ?: return
        val servings = state.servings ?: return
        viewModelScope.launch {
            mealRepository.add(
                MealEntry(
                    foodId = food.id,
                    servingsMultiplier = servings,
                    timestamp = Instant.now(),
                    source = MealSource.TEXT,
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
