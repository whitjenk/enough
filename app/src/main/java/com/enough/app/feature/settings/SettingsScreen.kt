package com.enough.app.feature.settings

import android.Manifest
import android.os.Build
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
import com.enough.app.feature.reminder.ReminderNotifier
import com.enough.app.domain.reminder.ReminderTimeOption
import com.enough.app.feature.reminder.ReminderScheduler
import com.enough.app.data.model.EstimateCalibration
import com.enough.app.data.model.Glp1Stance
import com.enough.app.di.AppViewModelProvider
import com.enough.app.domain.feedback.FeedbackSummary
import com.enough.app.ui.components.ChoiceList
import com.enough.app.ui.components.ChoiceOption
import com.enough.app.ui.theme.EnoughTheme

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)

    // Re-read on every resume: the person may have changed Enough's notification
    // access in system settings while the app was in the background.
    var canPostNotifications by remember { mutableStateOf(ReminderNotifier.canPost(context)) }
    LifecycleResumeEffect(Unit) {
        canPostNotifications = ReminderNotifier.canPost(context)
        onPauseOrDispose {}
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canPostNotifications = granted
        if (granted) {
            viewModel.setReminderEnabled(true)
            ReminderScheduler.schedule(context, uiState.reminderTime.minuteOfDay)
        }
    }
    SettingsScreen(
        uiState = uiState,
        onToggleSync = viewModel::setHealthConnectSyncEnabled,
        onSetCalibration = viewModel::setEstimateCalibration,
        onToggleHideNumbers = viewModel::setHideNumbersMode,
        onSetGlp1Stance = viewModel::setGlp1Stance,
        notificationsBlocked = !canPostNotifications,
        onToggleReminder = { enabled ->
            when {
                !enabled -> {
                    viewModel.setReminderEnabled(false)
                    ReminderScheduler.cancel(context)
                }
                // Turning reminders on from Settings has to ask for the OS
                // permission too — someone who declined (or never saw) the
                // onboarding offer would otherwise flip this switch, see it read
                // "On", and never receive anything (no silent failures).
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications ->
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> {
                    viewModel.setReminderEnabled(true)
                    ReminderScheduler.schedule(context, uiState.reminderTime.minuteOfDay)
                }
            }
        },
        onSetReminderTime = { option ->
            viewModel.setReminderTime(option)
            // Re-arm at the new time straight away, so a change takes effect
            // today rather than after the next launch.
            if (uiState.reminderEnabled) {
                ReminderScheduler.schedule(context, option.minuteOfDay)
            }
        },
        onPrepareFeedback = viewModel::prepareFeedback,
        onShareFeedback = { text ->
            // User-initiated only: they tap share and pick the destination in the
            // OS chooser. The app never sends anything on its own.
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(send, null))
        },
        onDeleteData = viewModel::deleteAllData,
        onOpenPrivacyPolicy = { uriHandler.openUri(privacyPolicyUrl) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onToggleSync: (Boolean) -> Unit,
    onSetCalibration: (EstimateCalibration) -> Unit,
    onToggleHideNumbers: (Boolean) -> Unit,
    onSetGlp1Stance: (Glp1Stance) -> Unit,
    notificationsBlocked: Boolean,
    onToggleReminder: (Boolean) -> Unit,
    onSetReminderTime: (ReminderTimeOption) -> Unit,
    onPrepareFeedback: () -> Unit,
    onShareFeedback: (String) -> Unit,
    onDeleteData: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Ordered by how often someone actually comes here for it. The
            // reminder is first because a person who skipped the onboarding
            // offer arrives looking for exactly this, and it used to be fifth.
            ReminderCard(
                enabled = uiState.reminderEnabled,
                notificationsBlocked = notificationsBlocked,
                selectedTime = uiState.reminderTime,
                onToggle = onToggleReminder,
                onSelectTime = onSetReminderTime,
            )
            SettingsDivider()
            HideNumbersCard(enabled = uiState.hideNumbersMode, onToggle = onToggleHideNumbers)
            SettingsDivider()
            SyncCard(enabled = uiState.healthConnectSyncEnabled, onToggle = onToggleSync)
            SettingsDivider()
            CalibrationCard(selected = uiState.estimateCalibration, onSelect = onSetCalibration)
            SettingsDivider()
            Glp1StanceCard(selected = uiState.glp1Stance, onSelect = onSetGlp1Stance)
            SettingsDivider()
            FeedbackCard(
                feedback = uiState.feedback,
                onPrepare = onPrepareFeedback,
                onShare = onShareFeedback,
            )
            SettingsDivider()
            SupportCard()
            SettingsDivider()
            PrivacyCard(onOpenPrivacyPolicy = onOpenPrivacyPolicy)
            SettingsDivider()
            DeleteCard(onDeleteClick = { showDeleteDialog = true })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteData()
                }) { Text(stringResource(R.string.settings_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.settings_delete_cancel))
                }
            },
        )
    }
}

/**
 * A hairline between settings groups. Replaces the nine stacked cards this
 * screen used to be (2026-08-24 warmth pass): identical rounded boxes made
 * every setting look like a separate widget and pushed the useful controls
 * below the fold behind ~280dp of pure card padding.
 */
@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun SyncCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_hc_sync_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_hc_sync_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun HideNumbersCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_hide_numbers_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_hide_numbers_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

/**
 * Daily-reminder control (SPEC §7.8). The time list only appears once reminders
 * are on — there is nothing to schedule otherwise, and showing a disabled picker
 * would read as a prompt to turn them on.
 */
@Composable
private fun ReminderCard(
    enabled: Boolean,
    notificationsBlocked: Boolean,
    selectedTime: ReminderTimeOption,
    onToggle: (Boolean) -> Unit,
    onSelectTime: (ReminderTimeOption) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_reminder_toggle),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (enabled) {
                            stringResource(
                                R.string.settings_reminder_on,
                                stringResource(selectedTime.settingsLabelRes()),
                            )
                        } else {
                            stringResource(R.string.settings_reminder_off)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            if (notificationsBlocked) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_reminder_blocked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (enabled) {
                Spacer(Modifier.height(12.dp))
                ChoiceList(
                    options = ReminderTimeOption.entries.map { option ->
                        ChoiceOption(option, stringResource(option.settingsLabelRes()))
                    },
                    selected = selectedTime,
                    onSelect = onSelectTime,
                )
            }
        }
    }
}

private fun ReminderTimeOption.settingsLabelRes(): Int = when (this) {
    ReminderTimeOption.MORNING -> R.string.reminder_time_morning
    ReminderTimeOption.MIDDAY -> R.string.reminder_time_midday
    ReminderTimeOption.AFTERNOON -> R.string.reminder_time_afternoon
    ReminderTimeOption.EVENING -> R.string.reminder_time_evening
}

@Composable
private fun Glp1StanceCard(
    selected: Glp1Stance,
    onSelect: (Glp1Stance) -> Unit,
) {
    val options = listOf(
        ChoiceOption(Glp1Stance.NOT, stringResource(R.string.settings_glp1_not)),
        ChoiceOption(Glp1Stance.ON, stringResource(R.string.settings_glp1_on)),
        ChoiceOption(Glp1Stance.COMING_OFF, stringResource(R.string.settings_glp1_coming_off)),
        ChoiceOption(Glp1Stance.PREFER_NOT_TO_SAY, stringResource(R.string.settings_glp1_prefer_not)),
    )
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_glp1_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_glp1_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChoiceList(options = options, selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun CalibrationCard(
    selected: EstimateCalibration,
    onSelect: (EstimateCalibration) -> Unit,
) {
    val options = listOf(
        ChoiceOption(EstimateCalibration.LOW, stringResource(R.string.settings_calibration_low)),
        ChoiceOption(EstimateCalibration.BALANCED, stringResource(R.string.settings_calibration_balanced)),
        ChoiceOption(EstimateCalibration.HIGH, stringResource(R.string.settings_calibration_high)),
    )
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_calibration_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_calibration_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ChoiceList(options = options, selected = selected, onSelect = onSelect)
        }
    }
}

@Composable
private fun FeedbackCard(
    feedback: FeedbackSummary?,
    onPrepare: () -> Unit,
    onShare: (String) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_feedback_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_feedback_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (feedback == null) {
                OutlinedButton(onClick = onPrepare) {
                    Text(stringResource(R.string.settings_feedback_prepare))
                }
            } else {
                val yes = stringResource(R.string.feedback_yes)
                val no = stringResource(R.string.feedback_no)
                // The person sees exactly the text that will be shared — nothing more.
                val shareText = stringResource(
                    R.string.feedback_share_text,
                    feedback.daysLogged,
                    feedback.mealsLogged,
                    feedback.daysHitFiberTarget,
                    feedback.fiberTargetG,
                    if (feedback.hasWeightGoal) yes else no,
                    if (feedback.everCheckedIn) yes else no,
                    if (feedback.everSharedCard) yes else no,
                )
                Text(
                    text = shareText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = { onShare(shareText) }) {
                    Text(stringResource(R.string.settings_feedback_share))
                }
            }
        }
    }
}

@Composable
private fun SupportCard() {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_support_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_support_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacyCard(onOpenPrivacyPolicy: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_privacy_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenPrivacyPolicy, contentPadding = PaddingValues(0.dp)) {
                Text(stringResource(R.string.settings_privacy_policy_link))
            }
        }
    }
}

@Composable
private fun DeleteCard(onDeleteClick: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_delete_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.settings_delete_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onDeleteClick) {
                Text(
                    text = stringResource(R.string.settings_delete_button),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    EnoughTheme(dynamicColor = false) {
        SettingsScreen(
            uiState = SettingsUiState(healthConnectSyncEnabled = true),
            onToggleSync = {},
            onSetCalibration = {},
            onToggleHideNumbers = {},
            onSetGlp1Stance = {},
            notificationsBlocked = false,
            onToggleReminder = {},
            onSetReminderTime = {},
            onPrepareFeedback = {},
            onShareFeedback = {},
            onDeleteData = {},
            onOpenPrivacyPolicy = {},
        )
    }
}
