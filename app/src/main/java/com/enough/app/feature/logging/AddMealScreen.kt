package com.enough.app.feature.logging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onSaved: (mealId: Long?) -> Unit,
    onBack: () -> Unit,
    viewModel: AddMealViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The id travels back with the navigation so Today can offer an undo for a
    // mis-tap, rather than the log being recoverable only by hunting for the row.
    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved(uiState.savedMealId) }

    AddMealScreen(
        uiState = uiState,
        onLogCategory = viewModel::onLogCategory,
        onLogRecent = viewModel::onLogRecent,
        onQueryChange = viewModel::onQueryChange,
        onSelectFood = viewModel::onSelectFood,
        onServingsChange = viewModel::onServingsChange,
        onSave = viewModel::save,
        onStartAddCustom = viewModel::onStartAddCustom,
        onCustomNameChange = viewModel::onCustomNameChange,
        onCustomFiberChange = viewModel::onCustomFiberChange,
        onCustomServingChange = viewModel::onCustomServingChange,
        onSaveCustom = viewModel::onSaveCustom,
        onCancelCustom = viewModel::onCancelAddCustom,
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
    onStartAddCustom: () -> Unit,
    onCustomNameChange: (String) -> Unit,
    onCustomFiberChange: (String) -> Unit,
    onCustomServingChange: (String) -> Unit,
    onSaveCustom: () -> Unit,
    onCancelCustom: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        // Transparent has no `contentColorFor` mapping, so M3 falls back to
        // black and every Text that doesn't set its own colour goes unreadable
        // in dark mode. Name the content colour explicitly (§7.10 B1).
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                title = { Text(stringResource(R.string.add_meal_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
        bottomBar = {
            if (uiState.selectedFood != null) {
                Surface {
                    // Edge-to-edge: a bare Surface applies no window insets of its
                    // own, so without this the button lands in the gesture inset.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
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

            if (uiState.addingCustom) {
                CustomFoodForm(
                    uiState = uiState,
                    onNameChange = onCustomNameChange,
                    onFiberChange = onCustomFiberChange,
                    onServingChange = onCustomServingChange,
                    onSave = onSaveCustom,
                    onCancel = onCancelCustom,
                )
                return@Column
            }

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
                FoodResults(uiState = uiState, onSelectFood = onSelectFood, onAddCustom = onStartAddCustom)
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
            QuickAddButton(label = category.name, onClick = { onLogCategory(category) })
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
                QuickAddButton(label = food.name, onClick = { onLogRecent(food) })
            }
        }
    }
}

/**
 * A one-tap quick-log control (§7.7 item 6). These commit a meal immediately, so
 * they use a filled-tonal button rather than an `AssistChip` — a chip reads as a
 * passive suggestion, which misrepresents what tapping actually does. Compact
 * padding keeps the chip-like density in the flow row.
 */
@Composable
private fun QuickAddButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FoodResults(
    uiState: AddMealUiState,
    onSelectFood: (Food) -> Unit,
    onAddCustom: () -> Unit,
) {
    when {
        uiState.query.isBlank() -> Column {
            HintText(stringResource(R.string.add_meal_empty_prompt))
            // Discoverable even before searching: a packaged item read off its
            // label (e.g. a specific protein pasta) is saved once, then re-loggable.
            TextButton(onClick = onAddCustom) {
                Text(stringResource(R.string.add_meal_add_custom_blank))
            }
        }
        uiState.results.isEmpty() && !uiState.isSearching -> Column {
            HintText(stringResource(R.string.add_meal_no_results))
            TextButton(onClick = onAddCustom) {
                Text(stringResource(R.string.add_meal_add_custom, uiState.query.trim()))
            }
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(uiState.results, key = { it.id }) { food ->
                FoodRow(food = food, onClick = { onSelectFood(food) })
                HorizontalDivider()
            }
            item {
                TextButton(onClick = onAddCustom, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.add_meal_add_custom_more, uiState.query.trim()))
                }
            }
        }
    }
}

@Composable
private fun CustomFoodForm(
    uiState: AddMealUiState,
    onNameChange: (String) -> Unit,
    onFiberChange: (String) -> Unit,
    onServingChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.add_meal_custom_title), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.customName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.add_meal_custom_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.customServingText,
                onValueChange = onServingChange,
                label = { Text(stringResource(R.string.add_meal_custom_serving)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.customFiberText,
                // Keep "," as well as "." — the Decimal keyboard produces a comma
                // in many locales, and stripping it would silently 10x the value.
                onValueChange = { onFiberChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
                label = { Text(stringResource(R.string.add_meal_custom_fiber)) },
                supportingText = { Text(stringResource(R.string.add_meal_custom_fiber_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = uiState.canSaveCustom, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.add_meal_custom_save))
                }
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.add_meal_custom_cancel))
                }
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
            text = nutritionLine(food),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * A custom food only ever had fiber entered; its carbs/protein default to 0 and
 * showing them as "0.0g" would present a placeholder as data. Show only what
 * the person actually told us.
 */
@Composable
private fun nutritionLine(food: Food): String = if (food.userCreated) {
    stringResource(R.string.add_meal_nutrition_fiber_only, food.fiberG, food.servingLabel)
} else {
    stringResource(
        R.string.add_meal_nutrition_per_serving,
        food.fiberG, food.carbsG, food.proteinG, food.servingLabel,
    )
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
                text = nutritionLine(food),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = servingsText,
                onValueChange = { onServingsChange(it.filter { c -> c.isDigit() || c == '.' || c == ',' }) },
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
            onQueryChange = {}, onSelectFood = {}, onServingsChange = {}, onSave = {},
            onStartAddCustom = {}, onCustomNameChange = {}, onCustomFiberChange = {},
            onCustomServingChange = {}, onSaveCustom = {}, onCancelCustom = {},
            onBack = {},
        )
    }
}
