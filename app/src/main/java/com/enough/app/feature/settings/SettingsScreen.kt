package com.enough.app.feature.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
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
    SettingsScreen(
        uiState = uiState,
        onToggleSync = viewModel::setHealthConnectSyncEnabled,
        onSetCalibration = viewModel::setEstimateCalibration,
        onToggleHideNumbers = viewModel::setHideNumbersMode,
        onSetGlp1Stance = viewModel::setGlp1Stance,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SyncCard(enabled = uiState.healthConnectSyncEnabled, onToggle = onToggleSync)
            CalibrationCard(selected = uiState.estimateCalibration, onSelect = onSetCalibration)
            HideNumbersCard(enabled = uiState.hideNumbersMode, onToggle = onToggleHideNumbers)
            Glp1StanceCard(selected = uiState.glp1Stance, onSelect = onSetGlp1Stance)
            FeedbackCard(
                feedback = uiState.feedback,
                onPrepare = onPrepareFeedback,
                onShare = onShareFeedback,
            )
            SupportCard()
            PrivacyCard(onOpenPrivacyPolicy = onOpenPrivacyPolicy)
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

@Composable
private fun SyncCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.padding(20.dp),
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.padding(20.dp),
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            onPrepareFeedback = {},
            onShareFeedback = {},
            onDeleteData = {},
            onOpenPrivacyPolicy = {},
        )
    }
}
