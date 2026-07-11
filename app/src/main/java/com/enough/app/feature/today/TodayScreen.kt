package com.enough.app.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
import com.enough.app.data.local.dao.MealWithFood
import com.enough.app.data.local.entity.ActivityEntry
import com.enough.app.data.model.ActivityUnit
import com.enough.app.di.AppViewModelProvider
import com.enough.app.domain.UnitConversions
import kotlin.math.roundToInt

@Composable
fun TodayRoute(
    onAddMeal: () -> Unit,
    onLogWeight: () -> Unit,
    onLogActivity: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        uiState = uiState,
        onAddMeal = onAddMeal,
        onLogWeight = onLogWeight,
        onLogActivity = onLogActivity,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onAddMeal: () -> Unit,
    onLogWeight: () -> Unit,
    onLogActivity: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.today_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            item { FiberCard(uiState) }
            item {
                LoggingActions(
                    onAddMeal = onAddMeal,
                    onLogWeight = onLogWeight,
                    onLogActivity = onLogActivity,
                )
            }
            item { WeightCard(uiState) }
            item { MealsHeader() }
            if (uiState.meals.isEmpty()) {
                item { EmptyHint(stringResource(R.string.today_no_meals)) }
            } else {
                items(uiState.meals, key = { it.meal.id }) { meal -> MealRow(meal) }
            }
            item { ActivityHeader() }
            if (uiState.activities.isEmpty()) {
                item { EmptyHint(stringResource(R.string.today_activity_none)) }
            } else {
                items(uiState.activities, key = { it.id }) { activity -> ActivityRow(activity) }
            }
        }
    }
}

@Composable
private fun FiberCard(uiState: TodayUiState) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.today_fiber_headline), style = MaterialTheme.typography.titleMedium)
            if (uiState.fiberTargetG > 0) {
                Text(
                    text = stringResource(
                        R.string.today_fiber_progress,
                        uiState.fiberSoFarG.roundToInt(),
                        uiState.fiberTargetG,
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    text = stringResource(R.string.today_fiber_no_target),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoggingActions(
    onAddMeal: () -> Unit,
    onLogWeight: () -> Unit,
    onLogActivity: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = onAddMeal, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.today_add_meal))
        }
        FilledTonalButton(onClick = onLogWeight, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.today_log_weight))
        }
        FilledTonalButton(onClick = onLogActivity, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.today_log_activity))
        }
    }
}

@Composable
private fun WeightCard(uiState: TodayUiState) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.today_weight_header), style = MaterialTheme.typography.titleMedium)
            val weight = uiState.latestWeight
            if (weight != null) {
                Text(
                    text = stringResource(
                        R.string.today_weight_value,
                        UnitConversions.kgToLb(weight.weightKg).roundToInt(),
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(
                    text = stringResource(R.string.today_weight_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MealsHeader() {
    Text(stringResource(R.string.today_meals_header), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun ActivityHeader() {
    Text(stringResource(R.string.today_activity_header), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun MealRow(item: MealWithFood) {
    val fiber = (item.food.fiberG * item.meal.servingsMultiplier).roundToInt()
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(item.food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(
            text = stringResource(
                R.string.today_meal_secondary,
                stringResource(R.string.today_servings_format, formatServings(item.meal.servingsMultiplier)),
                fiber,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivityRow(activity: ActivityEntry) {
    val label = when (activity.unit) {
        ActivityUnit.MINUTES -> stringResource(R.string.activity_value_minutes, activity.amount)
        ActivityUnit.STEPS -> stringResource(R.string.activity_value_steps, activity.amount)
        ActivityUnit.CUSTOM -> activity.note
            ?: stringResource(R.string.activity_value_custom, activity.amount)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Show whole servings without a trailing ".0" (e.g. 1, 1.5). */
private fun formatServings(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun TodayPreview() {
    com.enough.app.ui.theme.EnoughTheme(dynamicColor = false) {
        TodayScreen(
            uiState = TodayUiState(isLoading = false),
            onAddMeal = {},
            onLogWeight = {},
            onLogActivity = {},
        )
    }
}
