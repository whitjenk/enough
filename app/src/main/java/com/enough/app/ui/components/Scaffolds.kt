package com.enough.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        // This screen renders inside MainNavHost's Scaffold, which has already
        // consumed the system-bar insets. Consuming them again double-counted
        // the status bar and cost every screen ~54dp of dead space at the top
        // (§7.10 B3).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.Transparent,
        // Transparent has no `contentColorFor` mapping, so M3 falls back to
        // black and every Text that doesn't set its own colour goes unreadable
        // in dark mode. Name the content colour explicitly (§7.10 B1).
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                // Same reason as contentWindowInsets above — the outer Scaffold
                // already handled the status bar.
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(backLabel) }
                },
            )
        },
        bottomBar = {
            if (primaryLabel != null && onPrimary != null) {
                Surface {
                    // Edge-to-edge: a bare Surface applies no window insets of its
                    // own, so without this the button lands in the gesture inset.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
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
