package com.enough.app.feature.logging

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
import com.enough.app.data.model.ActivityUnit
import com.enough.app.di.AppViewModelProvider
import com.enough.app.ui.components.BackTitleScaffold
import com.enough.app.ui.components.HeroNumberField

@Composable
fun LogActivityRoute(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: LogActivityViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    LogActivityScreen(
        uiState = uiState,
        onAmountChange = viewModel::onAmountChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@Composable
fun LogActivityScreen(
    uiState: LogActivityUiState,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    BackTitleScaffold(
        title = stringResource(R.string.log_activity_title),
        backLabel = stringResource(R.string.action_back),
        onBack = onBack,
        primaryLabel = stringResource(R.string.action_save),
        primaryEnabled = uiState.canSave,
        onPrimary = onSave,
        // The amount is the point of the screen; centring keeps it from sitting
        // at the top of an empty page (§7.11).
        centerContent = true,
    ) {
        val amountHint = when (uiState.unit) {
            ActivityUnit.MINUTES -> stringResource(R.string.log_activity_minutes_hint)
            ActivityUnit.STEPS -> stringResource(R.string.log_activity_steps_hint)
            ActivityUnit.CUSTOM -> stringResource(R.string.log_activity_custom_hint)
        }
        val amountUnit = when (uiState.unit) {
            ActivityUnit.MINUTES -> stringResource(R.string.unit_minutes)
            ActivityUnit.STEPS -> stringResource(R.string.unit_steps)
            ActivityUnit.CUSTOM -> ""
        }

        if (uiState.unit == ActivityUnit.CUSTOM && !uiState.customGoalLabel.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.log_activity_custom_goal_reminder, uiState.customGoalLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        HeroNumberField(
            value = uiState.amountText,
            onValueChange = { onAmountChange(it.filter(Char::isDigit)) },
            label = amountHint,
            unitLabel = amountUnit,
        )

        // The note is genuinely secondary — narrower than full width so it
        // reads as an addition to the number above, not a second question.
        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.log_activity_note_hint)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LogActivityPreview() {
    com.enough.app.ui.theme.EnoughTheme(dynamicColor = false) {
        LogActivityScreen(
            uiState = LogActivityUiState(unit = ActivityUnit.MINUTES, amountText = "30"),
            onAmountChange = {}, onNoteChange = {}, onSave = {}, onBack = {},
        )
    }
}
