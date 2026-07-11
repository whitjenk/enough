package com.enough.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A titled screen with a text "Back" navigation control and an optional bottom
 * primary button. Shared by the logging screens. Back is a labelled text button
 * (not an icon-only control) so it needs no separate contentDescription.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTitleScaffold(
    title: String,
    backLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLabel: String? = null,
    primaryEnabled: Boolean = true,
    onPrimary: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(backLabel) }
                },
            )
        },
        bottomBar = {
            if (primaryLabel != null && onPrimary != null) {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Button(
                            onClick = onPrimary,
                            enabled = primaryEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(primaryLabel) }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}
