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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enough.app.R
import com.enough.app.data.model.ActivityUnit
import com.enough.app.di.AppViewModelProvider
import com.enough.app.ui.components.BackTitleScaffold

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
    ) {
        val amountHint = when (uiState.unit) {
            ActivityUnit.MINUTES -> stringResource(R.string.log_activity_minutes_hint)
            ActivityUnit.STEPS -> stringResource(R.string.log_activity_steps_hint)
            ActivityUnit.CUSTOM -> stringResource(R.string.log_activity_custom_hint)
        }

        if (uiState.unit == ActivityUnit.CUSTOM && !uiState.customGoalLabel.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.log_activity_custom_goal_reminder, uiState.customGoalLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = uiState.amountText,
            onValueChange = { onAmountChange(it.filter(Char::isDigit)) },
            label = { Text(amountHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = uiState.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.log_activity_note_hint)) },
            singleLine = true,
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
