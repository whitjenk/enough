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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.enough.app.domain.rules.Nudge
import com.enough.app.ui.components.Mascot
import com.enough.app.ui.theme.EnoughTheme
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
            item { NudgeCard(uiState.nudge) }
            item { FiberCard(uiState) }
            uiState.dailySwap?.let { swap -> item { DailySwapCard(swap) } }
            item {
                LoggingActions(
                    onAddMeal = onAddMeal,
                    onLogWeight = onLogWeight,
                    onLogActivity = onLogActivity,
                )
            }
            item { WeightCard(uiState) }
            item { HealthConnectCard(uiState.healthConnect) }
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
private fun NudgeCard(nudge: Nudge) {
    val message = when (nudge) {
        is Nudge.FiberGap -> stringResource(
            if (nudge.gentle) R.string.nudge_fiber_gap_gentle else R.string.nudge_fiber_gap,
            nudge.fiberSoFarG,
            nudge.suggestionFood,
            nudge.suggestionServingLabel,
            nudge.suggestionFiberG,
        )
        is Nudge.OnTrack -> stringResource(R.string.nudge_on_track, nudge.fiberSoFarG, nudge.targetG)
        Nudge.None -> null
    } ?: return

    // Reserve the success role for the goal-met moment. A gap is supportive, not
    // a "you did it" — and never red/gray-as-failure — so it uses a neutral
    // surface with the green mascot for warmth.
    val onTrack = nudge is Nudge.OnTrack
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = if (onTrack) {
            CardDefaults.cardColors(
                containerColor = EnoughTheme.successColors.successContainer,
                contentColor = EnoughTheme.successColors.onSuccessContainer,
            )
        } else {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) {
        Row(
            Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Mascot(
                color = EnoughTheme.successColors.success,
                contentDescription = stringResource(R.string.cd_mascot),
                pulsing = nudge is Nudge.FiberGap,
            )
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DailySwapCard(swap: com.enough.app.domain.rules.DailySwap.Swap) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.today_swap_header), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    if (swap.gentle) R.string.today_swap_body_gentle else R.string.today_swap_body,
                    swap.food,
                    swap.servingLabel,
                    swap.fiberG,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
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
private fun HealthConnectCard(data: HealthConnectData) {
    if (!data.hasAny) return
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.today_hc_header), style = MaterialTheme.typography.titleMedium)
            data.stepsToday?.let { steps ->
                Text(
                    text = stringResource(R.string.today_hc_steps, "%,d".format(steps)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            data.sleepMinutesLastNight?.let { minutes ->
                Text(
                    text = stringResource(R.string.today_hc_sleep, minutes / 60, minutes % 60),
                    style = MaterialTheme.typography.bodyLarge,
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
            uiState = TodayUiState(
                isLoading = false,
                dailySwap = com.enough.app.domain.rules.DailySwap.Swap(
                    food = "Lentils",
                    servingLabel = "1/2 cup",
                    fiberG = 8,
                    gentle = false,
                ),
            ),
            onAddMeal = {},
            onLogWeight = {},
            onLogActivity = {},
        )
    }
}
