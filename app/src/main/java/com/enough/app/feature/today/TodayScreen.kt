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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.FeltLevel
import com.enough.app.data.model.MealEntryType
import com.enough.app.data.model.WeightTrendDirection
import com.enough.app.di.AppViewModelProvider
import com.enough.app.domain.UnitConversions
import com.enough.app.domain.nutrition.MealNutrition
import com.enough.app.domain.rules.DailySwap
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
        onDeleteMeal = viewModel::deleteMeal,
        onResetMomentShown = viewModel::onResetMomentShown,
        onCheckIn = viewModel::onCheckIn,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onAddMeal: () -> Unit,
    onLogWeight: () -> Unit,
    onLogActivity: () -> Unit,
    onDeleteMeal: (MealWithFood) -> Unit,
    onResetMomentShown: () -> Unit,
    onCheckIn: (FeltLevel) -> Unit,
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
            // The reset-day moment outranks the routine fiber nudge: when it
            // shows, the nudge is suppressed so the two never contradict on a
            // rough day (a light inline arbitration ahead of Phase 1's layer).
            if (uiState.showResetMoment) {
                item { ResetMomentCard(onLogSomething = onAddMeal, onShown = onResetMomentShown) }
            } else {
                item { NudgeCard(uiState.nudge) }
            }
            item { FiberCard(uiState) }
            uiState.dailySwap?.let { swap -> item { DailySwapCard(swap) } }
            // The felt check-in is a reflection, so it sits just below the day's
            // fiber and the one-idea swap — present, never the first thing pushed.
            item { CheckInCard(selected = uiState.todayFelt, onCheckIn = onCheckIn) }
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
                items(uiState.meals, key = { it.meal.id }) { meal ->
                    MealRow(
                        item = meal,
                        calibration = uiState.calibration,
                        onDelete = { onDeleteMeal(meal) },
                    )
                }
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
private fun ResetMomentCard(onLogSomething: () -> Unit, onShown: () -> Unit) {
    // Record "shown" only when the card actually composes, so the
    // once-per-rough-patch budget is never spent on a state the person never saw.
    LaunchedEffect(Unit) { onShown() }

    // The app's name rendered as a felt moment: warm, no catch-up math, no red.
    // Uses the secondary container (a distinct warm surface, not the success role
    // reserved for goal-met) with the mascot for warmth.
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Mascot(
                    color = EnoughTheme.successColors.success,
                    contentDescription = stringResource(R.string.cd_mascot),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.reset_moment_header),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.reset_moment_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            FilledTonalButton(onClick = onLogSomething) {
                Text(stringResource(R.string.reset_moment_log_action))
            }
        }
    }
}

/**
 * The optional one-tap felt check-in — fiber's same-day payoff, the daily loop a
 * calorie tracker can't offer (SPEC §0.8 / §7.6 Step 1). Genuinely skippable: no
 * option is "wrong", nothing marks a day you didn't answer, and the copy stays a
 * note-to-self, never a nudge to log.
 */
@Composable
private fun CheckInCard(selected: FeltLevel?, onCheckIn: (FeltLevel) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.today_checkin_header), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeltLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selected == level,
                        onClick = { onCheckIn(level) },
                        label = { Text(stringResource(feltLabelRes(level))) },
                    )
                }
            }
            Text(
                text = stringResource(
                    if (selected == null) R.string.today_checkin_hint else R.string.today_checkin_done,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** String resource for a felt level's label, used by the Today check-in chips. */
private fun feltLabelRes(level: FeltLevel): Int = when (level) {
    FeltLevel.ROUGH -> R.string.felt_rough
    FeltLevel.STEADY -> R.string.felt_steady
    FeltLevel.GOOD -> R.string.felt_good
}

/** Weight-trend sentence shown in hide-numbers mode instead of the literal weight (SPEC §23). */
private fun weightTrendCopy(trend: WeightTrendDirection): Int = when (trend) {
    WeightTrendDirection.DOWN -> R.string.progress_weight_trend_down
    WeightTrendDirection.FLAT -> R.string.progress_weight_trend_flat
    WeightTrendDirection.UP -> R.string.progress_weight_trend_up
    WeightTrendDirection.UNKNOWN -> R.string.metric_hidden
}

@Composable
private fun DailySwapCard(swap: DailySwap.Swap) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.today_swap_header), style = MaterialTheme.typography.titleMedium)
            val bodyRes = when {
                swap.bridge -> R.string.today_swap_body_bridge
                swap.gentle -> R.string.today_swap_body_gentle
                else -> R.string.today_swap_body
            }
            Text(
                text = stringResource(bodyRes, swap.food, swap.servingLabel, swap.fiberG),
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
            if (uiState.hideNumbers) {
                // Hide-numbers mode: no literal grams, just that today's being logged.
                Text(
                    text = stringResource(R.string.today_fiber_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (uiState.fiberTargetG > 0) {
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
            if (weight != null && uiState.hideNumbers) {
                // Hide-numbers mode: the trend word instead of the literal weight.
                Text(
                    text = stringResource(weightTrendCopy(uiState.weightTrend)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else if (weight != null) {
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
private fun MealRow(
    item: MealWithFood,
    calibration: EstimateCalibration,
    onDelete: () -> Unit,
) {
    // Coarse category logs are inherently estimates, so show an honest ±band
    // rather than a fake-precise gram number (SPEC §7.5). Real entries stay exact.
    val secondary = if (item.meal.entryType == MealEntryType.COARSE_ESTIMATE) {
        val range = MealNutrition.coarseFiberRange(item, calibration)
        stringResource(R.string.today_meal_secondary_estimate, range.lowG, range.highG)
    } else {
        stringResource(
            R.string.today_meal_secondary,
            stringResource(R.string.today_servings_format, formatServings(item.meal.servingsMultiplier)),
            (item.food.fiberG * item.meal.servingsMultiplier).roundToInt(),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.food.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.today_meal_remove))
        }
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
                dailySwap = DailySwap.Swap(
                    food = "Lentils",
                    servingLabel = "1/2 cup",
                    fiberG = 8,
                    gentle = false,
                ),
            ),
            onAddMeal = {},
            onLogWeight = {},
            onLogActivity = {},
            onDeleteMeal = {},
            onResetMomentShown = {},
            onCheckIn = {},
        )
    }
}
