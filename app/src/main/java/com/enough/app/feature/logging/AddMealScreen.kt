package com.enough.app.feature.logging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
import com.enough.app.data.local.entity.Food
import com.enough.app.di.AppViewModelProvider

@Composable
fun AddMealRoute(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddMealViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    AddMealScreen(
        uiState = uiState,
        onLogCategory = viewModel::onLogCategory,
        onLogRecent = viewModel::onLogRecent,
        onQueryChange = viewModel::onQueryChange,
        onSelectFood = viewModel::onSelectFood,
        onServingsChange = viewModel::onServingsChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    uiState: AddMealUiState,
    onLogCategory: (Food) -> Unit,
    onLogRecent: (Food) -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectFood: (Food) -> Unit,
    onServingsChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_meal_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
        bottomBar = {
            if (uiState.selectedFood != null) {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Button(
                            onClick = onSave,
                            enabled = uiState.canSave,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.action_save)) }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            val selected = uiState.selectedFood

            // Default view: the low-friction quick-log. Once the person starts a
            // precise search (or picks a food), it recedes to keep the screen calm.
            if (selected == null && uiState.query.isBlank()) {
                QuickLogSections(
                    categories = uiState.categories,
                    recents = uiState.recents,
                    onLogCategory = onLogCategory,
                    onLogRecent = onLogRecent,
                )
                HorizontalDivider(Modifier.padding(top = 8.dp))
            }

            Text(
                text = stringResource(R.string.add_meal_search_header),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.add_meal_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )

            if (selected != null) {
                SelectedFoodEditor(
                    food = selected,
                    servingsText = uiState.servingsText,
                    servings = uiState.servings,
                    onServingsChange = onServingsChange,
                )
            } else {
                FoodResults(uiState = uiState, onSelectFood = onSelectFood)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLogSections(
    categories: List<Food>,
    recents: List<Food>,
    onLogCategory: (Food) -> Unit,
    onLogRecent: (Food) -> Unit,
) {
    Text(
        text = stringResource(R.string.add_meal_quick_header),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            AssistChip(
                onClick = { onLogCategory(category) },
                label = { Text(category.name) },
            )
        }
    }
    if (recents.isNotEmpty()) {
        Text(
            text = stringResource(R.string.add_meal_recent_header),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            recents.forEach { food ->
                AssistChip(
                    onClick = { onLogRecent(food) },
                    label = { Text(food.name) },
                )
            }
        }
    }
}

@Composable
private fun FoodResults(uiState: AddMealUiState, onSelectFood: (Food) -> Unit) {
    when {
        uiState.query.isBlank() -> HintText(stringResource(R.string.add_meal_empty_prompt))
        uiState.results.isEmpty() && !uiState.isSearching ->
            HintText(stringResource(R.string.add_meal_no_results))
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(uiState.results, key = { it.id }) { food ->
                FoodRow(food = food, onClick = { onSelectFood(food) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            text = stringResource(
                R.string.add_meal_nutrition_per_serving,
                food.fiberG, food.carbsG, food.proteinG, food.servingLabel,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectedFoodEditor(
    food: Food,
    servingsText: String,
    servings: Double?,
    onServingsChange: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(food.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(
                    R.string.add_meal_nutrition_per_serving,
                    food.fiberG, food.carbsG, food.proteinG, food.servingLabel,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = servingsText,
                onValueChange = { onServingsChange(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text(stringResource(R.string.add_meal_servings_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            if (servings != null) {
                Text(
                    text = stringResource(R.string.add_meal_selected_fiber, food.fiberG * servings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun AddMealPreview() {
    com.enough.app.ui.theme.EnoughTheme(dynamicColor = false) {
        AddMealScreen(
            uiState = AddMealUiState(
                categories = listOf(
                    Food(id = 10, name = "Veggie-heavy meal", servingLabel = "1 meal", carbsG = 30.0, fiberG = 8.0, proteinG = 8.0, selectable = false),
                    Food(id = 11, name = "Mixed meal", servingLabel = "1 meal", carbsG = 40.0, fiberG = 5.0, proteinG = 20.0, selectable = false),
                ),
                recents = listOf(
                    Food(id = 1, name = "Black beans", servingLabel = "1/2 cup cooked", carbsG = 20.0, fiberG = 7.5, proteinG = 7.6),
                ),
            ),
            onLogCategory = {}, onLogRecent = {},
            onQueryChange = {}, onSelectFood = {}, onServingsChange = {}, onSave = {}, onBack = {},
        )
    }
}
