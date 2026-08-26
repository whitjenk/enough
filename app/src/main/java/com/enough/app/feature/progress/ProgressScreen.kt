package com.enough.app.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import com.enough.app.R
import com.enough.app.data.model.FeltLevel
import com.enough.app.data.model.WeightTrendDirection
import com.enough.app.di.AppViewModelProvider
import com.enough.app.domain.UnitConversions
import com.enough.app.domain.progress.ProgressCalculations
import com.enough.app.domain.progress.DailyFiber
import com.enough.app.domain.share.ShareCard
import com.enough.app.feature.share.ShareCardLines
import com.enough.app.feature.share.ShareCardRenderer
import com.enough.app.ui.theme.EnoughTheme
import kotlin.math.max
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProgressRoute(
    viewModel: ProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ProgressScreen(
        uiState = uiState,
        onShareWeek = {
            // User-initiated only: the card is generated on this tap and handed to
            // the OS share sheet. Nothing is shared unless the person picks a target.
            val data = ShareCard.build(
                fiberByDayValues = uiState.fiberSeries.map { it.fiberG },
                daysLogged = uiState.daysLoggedLast7,
                windowDays = uiState.windowDays,
                hideNumbers = uiState.hideNumbers,
            )
            val headline = when {
                data.hideNumbers -> context.getString(R.string.share_card_headline_hidden)
                data.averageFiberG != null ->
                    context.getString(R.string.share_card_headline_avg, data.averageFiberG)
                else -> context.getString(R.string.share_card_headline_empty)
            }
            val uri = ShareCardRenderer.render(
                context,
                ShareCardLines(
                    title = context.getString(R.string.share_card_title),
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
            // Record the aggregate "ever shared a card" signal (§7.6 Step 3).
            viewModel.markCardShared()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(uiState: ProgressUiState, onShareWeek: () -> Unit = {}) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.progress_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        ) {
            // Sections separated by hairlines rather than boxed in cards, so the
            // screen reads as one trend view instead of five widgets
            // (2026-08-24 warmth pass — matches Today and Settings).
            item { ConsistencyCard(uiState) }
            item { ProgressDivider() }
            item { CheckInReflectionCard(uiState) }
            item { ProgressDivider() }
            item { FiberTrendCard(uiState) }
            item { ProgressDivider() }
            item { WeightCard(uiState) }
            item { ProgressDivider() }
            item { MovementCard(uiState) }
            item { ProgressDivider() }
            item { ShareWeekCard(onShareWeek = onShareWeek) }
        }
    }
}

/**
 * "X of the last N days", or bespoke day-one copy when there is no window to
 * count against yet. On a fresh install the window is one day long (see
 * [ProgressCalculations.visibleWindowDays]), and "1 of the last 1 days" is both
 * ungrammatical and slightly absurd.
 */
@Composable
private fun countSentence(
    count: Int,
    windowDays: Int,
    windowRes: Int,
    dayOneRes: Int,
    dayOneEmptyRes: Int,
): String = when {
    windowDays > 1 -> stringResource(windowRes, count, windowDays)
    count > 0 -> stringResource(dayOneRes)
    else -> stringResource(dayOneEmptyRes)
}

@Composable
private fun ProgressDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun ConsistencyCard(uiState: ProgressUiState) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.progress_consistency_header), style = MaterialTheme.typography.titleMedium)
            Text(
                text = countSentence(
                    count = uiState.daysLoggedLast7,
                    windowDays = uiState.windowDays,
                    windowRes = R.string.progress_consistency_value,
                    dayOneRes = R.string.progress_consistency_value_today,
                    dayOneEmptyRes = R.string.progress_consistency_value_today_empty,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val loggedLabel = stringResource(R.string.cd_day_logged)
                val notLoggedLabel = stringResource(R.string.cd_day_not_logged)
                uiState.loggedDaySeries.forEachIndexed { index, logged ->
                    ConsistencyDot(
                        logged = logged,
                        contentDescription = if (logged) loggedLabel else notLoggedLabel,
                    )
                }
            }
            Text(
                text = stringResource(R.string.progress_consistency_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConsistencyDot(logged: Boolean, contentDescription: String) {
    // Filled vs. hollow shape carries the meaning — not color alone (DESIGN.md).
    val base = Modifier
        .size(20.dp)
        .semantics { this.contentDescription = contentDescription }
    if (logged) {
        androidx.compose.foundation.layout.Box(
            base.clip(CircleShape).then(
                Modifier.background(EnoughTheme.successColors.success),
            ),
        )
    } else {
        androidx.compose.foundation.layout.Box(
            base.clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
        )
    }
}

/**
 * A gentle, non-scored reflection of the optional felt check-in (SPEC §7.6
 * Step 1). Mirrors the consistency dots — a checked-in day is filled (shape, not
 * color alone), a skipped day is hollow — and never ranks the levels or forms a
 * streak. A day with no check-in is simply hollow, carrying no penalty.
 */
@Composable
private fun CheckInReflectionCard(uiState: ProgressUiState) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.progress_checkin_header), style = MaterialTheme.typography.titleMedium)
            if (uiState.checkInFeltSeries.all { it == null }) {
                Text(
                    text = stringResource(R.string.progress_checkin_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    // The all-null case is handled by the empty state above, so
                    // day one here always means "checked in today".
                    text = if (uiState.windowDays <= 1) {
                        stringResource(R.string.progress_checkin_value_today)
                    } else {
                        stringResource(
                            R.string.progress_checkin_value,
                            uiState.checkInDaysLast7,
                            uiState.windowDays,
                        )
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val noCheckIn = stringResource(R.string.cd_day_no_checkin)
                    uiState.checkInFeltSeries.forEach { felt ->
                        ConsistencyDot(
                            logged = felt != null,
                            contentDescription = felt?.let { stringResource(feltLabelRes(it)) } ?: noCheckIn,
                        )
                    }
                }
            }
        }
    }
}

/** String resource for a felt level's label, used as the check-in dot's accessible description. */
private fun feltLabelRes(level: FeltLevel): Int = when (level) {
    FeltLevel.ROUGH -> R.string.felt_rough
    FeltLevel.STEADY -> R.string.felt_steady
    FeltLevel.GOOD -> R.string.felt_good
}

@Composable
private fun FiberTrendCard(uiState: ProgressUiState) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.progress_fiber_header), style = MaterialTheme.typography.titleMedium)
            val hasData = uiState.fiberSeries.any { it.fiberG > 0.0 }
            if (!hasData) {
                Text(
                    text = stringResource(R.string.progress_fiber_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Hide-numbers mode: keep the shape-only trend chart, drop the literal grams.
                if (!uiState.hideNumbers) {
                    val todayFiber = uiState.fiberSeries.lastOrNull()?.fiberG?.roundToInt() ?: 0
                    Text(
                        text = stringResource(R.string.progress_fiber_today_value, todayFiber),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                FiberBarChart(
                    days = uiState.fiberSeries,
                    targetG = uiState.fiberTargetG,
                    modifier = Modifier.fillMaxWidth(),
                    chartDescription = stringResource(R.string.cd_fiber_chart, uiState.windowDays),
                )
                if (uiState.fiberTargetG > 0 && !uiState.hideNumbers) {
                    Text(
                        text = stringResource(R.string.progress_fiber_target_label, uiState.fiberTargetG),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The week's fiber as bars, with the daily target as a reference line.
 *
 * Rebuilt in §7.10 A6. The previous version had three problems, all of which
 * made it read as broken rather than quiet:
 *
 *  - The vertical scale topped out at exactly `max(target, highest day)`, so
 *    whenever no day beat the target — the common case — the target line was
 *    drawn at y=0, a solid full-width rule across the very top. Next to this
 *    screen's own solid hairline dividers it parsed as an underline for the
 *    value above it, not as a reference line. There is headroom above the
 *    ceiling now, and the line is dashed so it cannot be confused with a
 *    divider.
 *  - Days with no fiber drew nothing at all, so a week with one logged day was
 *    a single bar floating in empty space. Every day now draws at least a
 *    baseline nub in the neutral track color: the day is present, it just has
 *    nothing on it. Never red, never a "missed" marker (DESIGN.md).
 *  - There were no day labels, so no bar could be matched to a day.
 *
 * Bars use the `success` role, matching the consistency dots above and the
 * fiber ring on Today. They were `primary`, which put two different greens for
 * the same week on the same screen, and `DESIGN.md` reserves primary for
 * identity rather than for logged progress.
 *
 * The whole chart is one semantics node with a single spoken description; the
 * day letters are decorative here and would otherwise be read out as "M T W T
 * F S S" before anything meaningful.
 */
@Composable
private fun FiberBarChart(
    days: List<DailyFiber>,
    targetG: Int,
    chartDescription: String,
    modifier: Modifier = Modifier,
    barHeight: Dp = 120.dp,
) {
    if (days.isEmpty()) return

    val highest = days.maxOf { it.fiberG }
    // Headroom above whichever is taller, so a target line never lands on the
    // top edge and a target-beating day never touches it either.
    val ceiling = (max(targetG.toDouble(), highest) * 1.18).coerceAtLeast(1.0)

    val barColor = EnoughTheme.successColors.success
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val targetLineColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = chartDescription
        },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(barHeight)
                .drawBehind {
                    if (targetG <= 0) return@drawBehind
                    val y = size.height - (targetG / ceiling).toFloat() * size.height
                    drawLine(
                        color = targetLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2f,
                        cap = StrokeCap.Round,
                        // Dashed, so it reads as a reference line rather than as
                        // another of this screen's solid hairline dividers.
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                    )
                },
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val fraction = (day.fiberG / ceiling).toFloat().coerceIn(0f, 1f)
                    val logged = day.fiberG > 0.0
                    Box(
                        Modifier
                            .weight(1f)
                            // A day with nothing logged still occupies its slot:
                            // a neutral nub at the baseline, not a blank gap.
                            .then(
                                if (logged) {
                                    Modifier.fillMaxHeight(fraction)
                                } else {
                                    Modifier.height(BAR_EMPTY_HEIGHT)
                                },
                            )
                            .heightIn(min = BAR_EMPTY_HEIGHT)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(if (logged) barColor else trackColor),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            days.forEach { day ->
                Text(
                    text = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Baseline nub for a day with nothing logged — present, but carrying nothing. */
private val BAR_EMPTY_HEIGHT = 4.dp

@Composable
private fun WeightCard(uiState: ProgressUiState) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.progress_weight_header), style = MaterialTheme.typography.titleMedium)
            val current = uiState.currentWeightKg
            if (current == null) {
                Text(
                    text = stringResource(R.string.progress_weight_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                // Hide-numbers mode: keep the trend sentence, drop the literal pounds.
                if (!uiState.hideNumbers) {
                    Text(
                        text = stringResource(R.string.progress_weight_current, UnitConversions.kgToLb(current).roundToInt()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (uiState.startWeightKg != null && uiState.targetWeightKg != null) {
                        Text(
                            text = stringResource(
                                R.string.progress_weight_start_target,
                                UnitConversions.kgToLb(uiState.startWeightKg).roundToInt(),
                                UnitConversions.kgToLb(uiState.targetWeightKg).roundToInt(),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text = stringResource(weightTrendCopy(uiState.weightTrend)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun weightTrendCopy(trend: WeightTrendDirection): Int = when (trend) {
    WeightTrendDirection.DOWN -> R.string.progress_weight_trend_down
    WeightTrendDirection.FLAT -> R.string.progress_weight_trend_flat
    WeightTrendDirection.UP -> R.string.progress_weight_trend_up
    WeightTrendDirection.UNKNOWN -> R.string.progress_weight_trend_unknown
}

@Composable
private fun MovementCard(uiState: ProgressUiState) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.progress_movement_header), style = MaterialTheme.typography.titleMedium)
            val goalMinutes = uiState.activityGoalMinutes
            val text = when {
                goalMinutes != null -> stringResource(
                    R.string.progress_movement_minutes,
                    uiState.weeklyActivityMinutes,
                    goalMinutes,
                )
                uiState.weeklyActivityMinutes > 0 -> stringResource(
                    R.string.progress_movement_minutes_no_goal,
                    uiState.weeklyActivityMinutes,
                )
                // daysMovedLast7, not daysLoggedLast7 — this sentence is about
                // movement, and counting any log at all made it claim a walk
                // whenever a meal had been logged.
                else -> countSentence(
                    count = uiState.daysMovedLast7,
                    windowDays = uiState.windowDays,
                    windowRes = R.string.progress_movement_days,
                    dayOneRes = R.string.progress_movement_days_today,
                    dayOneEmptyRes = R.string.progress_movement_days_today_empty,
                )
            }
            Text(text = text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * The opt-in share affordance (SPEC §7.6 Step 2): a plain button that generates a
 * card and opens the OS share sheet only on an explicit tap. Never auto-suggested,
 * never a popup; the copy states the "nothing leaves your phone unless you send it"
 * promise directly.
 */
@Composable
private fun ShareWeekCard(onShareWeek: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.progress_share_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.progress_share_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onShareWeek) {
                Text(stringResource(R.string.progress_share_button))
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ProgressPreview() {
    EnoughTheme(dynamicColor = false) {
        ProgressScreen(
            ProgressUiState(
                fiberSeries = listOf(0, 12, 18, 9, 24, 15, 20).mapIndexed { i, v ->
                    com.enough.app.domain.progress.DailyFiber(java.time.LocalDate.now().minusDays((6 - i).toLong()), v.toDouble())
                },
                fiberTargetG = 28,
                loggedDaySeries = listOf(false, true, true, true, true, true, true),
                daysLoggedLast7 = 6,
                currentWeightKg = 82.0,
                startWeightKg = 84.0,
                targetWeightKg = 80.0,
                weightTrend = WeightTrendDirection.DOWN,
                weeklyActivityMinutes = 90,
                activityGoalMinutes = 150,
                isLoading = false,
            ),
        )
    }
}
