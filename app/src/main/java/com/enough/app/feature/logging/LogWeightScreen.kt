package com.enough.app.feature.logging

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import com.enough.app.di.AppViewModelProvider
import com.enough.app.ui.components.BackTitleScaffold

@Composable
fun LogWeightRoute(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: LogWeightViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.saved) { if (uiState.saved) onSaved() }

    LogWeightScreen(
        uiState = uiState,
        onWeightChange = viewModel::onWeightChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@Composable
fun LogWeightScreen(
    uiState: LogWeightUiState,
    onWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    BackTitleScaffold(
        title = stringResource(R.string.log_weight_title),
        backLabel = stringResource(R.string.action_back),
        onBack = onBack,
        primaryLabel = stringResource(R.string.action_save),
        primaryEnabled = uiState.canSave,
        onPrimary = onSave,
    ) {
        OutlinedTextField(
            value = uiState.weightLbText,
            onValueChange = { onWeightChange(it.filter { c -> c.isDigit() || c == '.' }) },
            label = { Text(stringResource(R.string.log_weight_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun LogWeightPreview() {
    com.enough.app.ui.theme.EnoughTheme(dynamicColor = false) {
        LogWeightScreen(
            uiState = LogWeightUiState(weightLbText = "182"),
            onWeightChange = {}, onSave = {}, onBack = {},
        )
    }
}
