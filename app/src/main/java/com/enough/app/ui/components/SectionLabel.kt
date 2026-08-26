package com.enough.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.enough.app.ui.theme.EnoughTheme

/**
 * The quiet name of a section — "Consistency", "Fiber trend", "Meals" (§7.10 B1).
 *
 * Section headings used to be `titleMedium`, the same weight as the content
 * under them, so every section read as a quiet label shouting a bold sentence
 * and the eye had nowhere to rest. A label is scaffolding: it should name the
 * section and then get out of the way, letting the value carry the screen.
 *
 * Deliberately **not** uppercase. Uppercase reads as a raised voice, which is
 * wrong for this app in particular, and some screen readers announce it letter
 * by letter. The small size, letter spacing and muted colour do the same
 * typographic job without either problem.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun SectionLabelPreview() {
    EnoughTheme(dynamicColor = false) {
        SectionLabel("Fiber trend")
    }
}
