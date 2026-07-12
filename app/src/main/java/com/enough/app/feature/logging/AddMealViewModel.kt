package com.enough.app.feature.logging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enough.app.data.local.entity.Food
import com.enough.app.data.local.entity.MealEntry
import com.enough.app.data.model.MealEntryType
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
    val categories: List<Food> = emptyList(),
    val recents: List<Food> = emptyList(),
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
 * Backs the add-meal screen. The default path is the low-friction coarse
 * quick-log: one tap on a category chip ("veggie-heavy meal") or a recently
 * logged food saves immediately (SPEC §7.5). Precise text search is the
 * secondary "search an exact food" path, kept intact but no longer the front
 * door. Coarse entries are stored as [MealEntryType.COARSE_ESTIMATE].
 */
class AddMealViewModel(
    private val foodRepository: FoodRepository,
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMealUiState())
    val uiState: StateFlow<AddMealUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    categories = foodRepository.categoryFoods(),
                    recents = foodRepository.recentFoods(),
                )
            }
        }
    }

    /** Log a coarse category in one tap: one "serving" of the category, now. */
    fun onLogCategory(category: Food) = quickLog(category, MealEntryType.COARSE_ESTIMATE)

    /**
     * Re-log a recently logged food in one tap. A synthetic category food stays a
     * coarse estimate; a real food is database-matched.
     */
    fun onLogRecent(food: Food) =
        quickLog(food, if (food.selectable) MealEntryType.DATABASE_MATCHED else MealEntryType.COARSE_ESTIMATE)

    private fun quickLog(food: Food, entryType: MealEntryType) {
        viewModelScope.launch {
            mealRepository.add(
                MealEntry(
                    foodId = food.id,
                    servingsMultiplier = 1.0,
                    timestamp = Instant.now(),
                    source = MealSource.MANUAL,
                    entryType = entryType,
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

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
                    entryType = MealEntryType.DATABASE_MATCHED,
                ),
            )
            _uiState.update { it.copy(saved = true) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
