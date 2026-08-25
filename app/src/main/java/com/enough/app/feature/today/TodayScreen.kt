package com.enough.app.feature.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.enough.app.domain.rules.TodayMessage
import com.enough.app.feature.share.ShareCardLines
import com.enough.app.feature.share.ShareCardRenderer
import com.enough.app.ui.components.FiberRing
import com.enough.app.ui.components.Mascot
import com.enough.app.ui.theme.EnoughTheme
import kotlin.math.roundToInt

@Composable
fun TodayRoute(
    onAddMeal: () -> Unit,
    onLogWeight: () -> Unit,
    onLogActivity: () -> Unit,
    viewModel: TodayViewModel = viewModel(factory = AppViewModelProvider.Factory),
    undoMealId: Long? = null,
    onUndoHandled: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // A meal was just logged: offer to take it back. Prevention over cure for an
    // accidental one-tap quick-log (§7.7 item 6). Cleared as soon as it's shown
    // so it can't reappear on the next recomposition or return to this screen.
    LaunchedEffect(undoMealId) {
        val id = undoMealId ?: return@LaunchedEffect
        try {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.today_meal_logged),
                actionLabel = context.getString(R.string.today_meal_undo),
                withDismissAction = false,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoMeal(id)
        } finally {
            // Cleared only once the snackbar is done — clearing first would change
            // this effect's own key and cancel it before anything showed. The
            // `finally` also covers navigating away mid-snackbar, so a stale id
            // can't re-trigger on the next return to Today.
            onUndoHandled()
        }
    }

    TodayScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddMeal = onAddMeal,
        onLogWeight = onLogWeight,
        onLogActivity = onLogActivity,
        onDeleteMeal = viewModel::deleteMeal,
        onResetMomentShown = viewModel::onResetMomentShown,
        onCheckIn = viewModel::onCheckIn,
        onShareToday = {
            // Feeling-first, generated on this explicit tap, handed to the OS share
            // sheet as a 9:16 story. Number-free by design — the counter-position.
            val headline = context.getString(
                when (uiState.todayFelt) {
                    FeltLevel.GOOD -> R.string.share_daily_felt_good
                    FeltLevel.STEADY -> R.string.share_daily_felt_steady
                    FeltLevel.ROUGH -> R.string.share_daily_felt_rough
                    null -> R.string.share_daily_felt_generic
                },
            )
            val uri = ShareCardRenderer.renderStory(
                context,
                ShareCardLines(
                    title = context.getString(R.string.share_daily_title),
                    headline = headline,
                    subline = context.getString(R.string.share_card_subline),
                    footer = context.getString(R.string.share_card_footer),
                ),
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, null))
            viewModel.markCardShared()
        },
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
    onShareToday: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.today_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The #1 task (and the named churn driver) gets the thumb-reachable
        // primary slot instead of being one of three equal mid-screen buttons
        // (§7.7 item 3). Extended and labelled — the app uses no icon library, and
        // a visible label needs no separate contentDescription.
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddMeal) {
                Text(stringResource(R.string.today_add_meal))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            // Bottom inset so the FAB never sits on top of the last row.
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_greeting),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            // Tier 1 — the hero. The ring is the only extraLarge card on the
            // screen; everything below it is deliberately quieter.
            item { FiberCard(uiState) }
            // Tier 2 — exactly ONE thing the app says today (reset > nudge >
            // swap), chosen by the pure arbiter so the three can't stack into a
            // wall of advice or contradict each other on a rough day.
            if (uiState.todayMessage != TodayMessage.None) {
                item {
                    TodayMessageSlot(
                        message = uiState.todayMessage,
                        onAddMeal = onAddMeal,
                        onResetMomentShown = onResetMomentShown,
                    )
                }
            }
            // Sits above the check-in rather than below it so it clears the FAB's
            // resting position — an extended FAB is wide, and on device it covered
            // the "Log movement" button when this row sat lower.
            item {
                SecondaryLoggingActions(
                    onLogWeight = onLogWeight,
                    onLogActivity = onLogActivity,
                )
            }
            // Tier 3 — the person's own input, not the app talking. Keeps its own
            // quiet slot rather than competing with the messages above (§7.6 S1/S2).
            item {
                CheckInCard(
                    selected = uiState.todayFelt,
                    onCheckIn = onCheckIn,
                    onShareToday = onShareToday,
                )
            }
            // Tier 4 — today's numbers as quiet grouped rows, not equal-weight
            // cards competing with the ring.
            item { QuietStatsSection(uiState) }
            item { MealsHeader() }
            if (uiState.meals.isEmpty()) {
                item { FirstMealPrompt(onAddMeal = onAddMeal) }
            } else {
                items(uiState.meals, key = { "meal-${it.meal.id}" }) { meal ->
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
                items(uiState.activities, key = { "activity-${it.id}" }) { activity -> ActivityRow(activity) }
            }
        }
    }
}

/**
 * The single "today" message, with the app's springy entrance applied to the one
 * thing that actually changed (§7.7 item 7). [AnimatedContent] does not animate
 * its first content, so opening the app is still motionless — the transition
 * runs only when the message genuinely swaps (a nudge becoming the reset moment,
 * say), which is exactly DESIGN.md's "no motion on data the person didn't just
 * interact with."
 */
@Composable
private fun TodayMessageSlot(
    message: TodayMessage,
    onAddMeal: () -> Unit,
    onResetMomentShown: () -> Unit,
) {
    AnimatedContent(
        targetState = message,
        transitionSpec = {
            val enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                )
            enter togetherWith fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
        },
        label = "today-message",
    ) { current ->
        when (current) {
            TodayMessage.Reset ->
                ResetMomentCard(onLogSomething = onAddMeal, onShown = onResetMomentShown)
            is TodayMessage.FiberNudge -> NudgeCard(current.nudge)
            is TodayMessage.Swap -> DailySwapCard(current.swap)
            TodayMessage.None -> Unit
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
        // `large`, not `extraLarge` — the ring is the screen's only hero (§7.7 item 2).
        shape = MaterialTheme.shapes.large,
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
        // `large`, not `extraLarge` — the ring is the screen's only hero (§7.7 item 2).
        shape = MaterialTheme.shapes.large,
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
private fun CheckInCard(
    selected: FeltLevel?,
    onCheckIn: (FeltLevel) -> Unit,
    onShareToday: () -> Unit,
) {
    // A quieter surface than the message cards above it: this is the person's own
    // input, not the app speaking, and its chips already give it enough presence.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
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
            // Only once the person has reflected — the emotional peak — offer an
            // opt-in, feeling-first share. Never a popup, never before a check-in.
            if (selected != null) {
                TextButton(onClick = onShareToday, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.today_checkin_share))
                }
            }
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
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.today_fiber_headline),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
            )
            if (uiState.hideNumbers) {
                // Hide-numbers mode: no ring, no literal grams — just that today's
                // being logged. Keeps a hidden number from ever reaching the screen.
                Text(
                    text = stringResource(R.string.today_fiber_hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start),
                )
            } else {
                // The hero: an animated ring (handles the no-target and over-target
                // states itself). A zero target renders a neutral, unfilled track.
                FiberRing(
                    fiberSoFarG = uiState.fiberSoFarG.roundToInt(),
                    fiberTargetG = uiState.fiberTargetG,
                )
                if (uiState.fiberTargetG <= 0) {
                    Text(
                        text = stringResource(R.string.today_fiber_no_target),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Weight and movement as *secondary* entries (§7.7 item 3). Meal logging is the
 * primary action and lives in the FAB, so these two step down to outlined
 * buttons rather than competing as equal filled-tonal thirds.
 */
@Composable
private fun SecondaryLoggingActions(
    onLogWeight: () -> Unit,
    onLogActivity: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onLogWeight, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.today_log_weight))
        }
        OutlinedButton(onClick = onLogActivity, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.today_log_activity))
        }
    }
}

/**
 * Weight and the optional Health Connect readings, grouped into one quiet
 * surface of label/value rows (§7.7 item 2). These were three equal-weight cards
 * competing with the fiber ring; they're supporting context, so they recede
 * through size and grouping rather than through a different color.
 */
@Composable
private fun QuietStatsSection(uiState: TodayUiState) {
    val data = uiState.healthConnect
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            val weight = uiState.latestWeight
            QuietStatRow(
                label = stringResource(R.string.today_weight_header),
                value = when {
                    // Hide-numbers mode: the trend word instead of the literal weight.
                    weight != null && uiState.hideNumbers ->
                        stringResource(weightTrendCopy(uiState.weightTrend))
                    weight != null -> stringResource(
                        R.string.today_weight_value,
                        UnitConversions.kgToLb(weight.weightKg).roundToInt(),
                    )
                    else -> stringResource(R.string.today_weight_none)
                },
                muted = weight == null,
            )
            data.stepsToday?.let { steps ->
                QuietStatRow(
                    label = stringResource(R.string.today_hc_steps_label),
                    value = "%,d".format(steps),
                )
            }
            data.sleepMinutesLastNight?.let { minutes ->
                QuietStatRow(
                    label = stringResource(R.string.today_hc_sleep_label),
                    value = stringResource(R.string.today_hc_sleep_value, minutes / 60, minutes % 60),
                )
            }
        }
    }
}

/** One label/value line in the quiet stats group. */
@Composable
private fun QuietStatRow(label: String, value: String, muted: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (muted) FontWeight.Normal else FontWeight.Medium,
            color = if (muted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
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

/**
 * The empty meals state as a first-action moment rather than a gray dead end
 * (§7.7 item 5). The first session decides retention, so a day with nothing
 * logged gets a warm invitation and one clear way to act — reachable here as
 * well as from the FAB, since this is where the person is already looking.
 * No catch-up framing and nothing that counts what's missing.
 */
@Composable
private fun FirstMealPrompt(onAddMeal: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.today_no_meals),
                style = MaterialTheme.typography.bodyLarge,
            )
            FilledTonalButton(onClick = onAddMeal) {
                Text(stringResource(R.string.today_no_meals_action))
            }
        }
    }
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
            // Exercises the full hierarchy: hero ring with a real target, one
            // arbitrated message (the swap, since no nudge is set), the quiet
            // check-in, and the grouped stat rows.
            uiState = TodayUiState(
                goal = com.enough.app.data.local.entity.UserGoal(
                    startWeightKg = 82.0,
                    targetWeightKg = 78.0,
                    weightLossPercent = 5.0,
                    activityGoalType = com.enough.app.data.model.ActivityGoalType.MINUTES,
                    activityGoalValue = 150,
                    activityGoalCustomLabel = null,
                    dailyCalorieEstimate = 2000,
                    fiberGramsTarget = 28,
                    createdAt = java.time.Instant.EPOCH,
                ),
                fiberSoFarG = 18.0,
                healthConnect = HealthConnectData(stepsToday = 6432, sleepMinutesLastNight = 437),
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
