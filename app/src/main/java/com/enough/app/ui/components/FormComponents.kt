package com.enough.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import kotlin.math.roundToInt

/** One selectable option and its human-readable label. */
data class ChoiceOption<T>(val value: T, val label: String)

/**
 * An accessible single-choice list rendered as radio rows. The whole row is the
 * touch target and is exposed with [Role.RadioButton] semantics; selection state
 * is conveyed by the control, not by color alone (DESIGN.md).
 */
@Composable
fun <T> ChoiceList(
    options: List<ChoiceOption<T>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.selectableGroup()) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option.value) },
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = isSelected, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * An accessible multi-choice list rendered as checkbox rows. The whole row is the
 * touch target with [Role.Checkbox] semantics; state is conveyed by the control,
 * not by color alone (DESIGN.md).
 */
@Composable
fun <T> MultiChoiceList(
    options: List<ChoiceOption<T>>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        options.forEach { option ->
            val isChecked = option.value in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .toggleable(
                        value = isChecked,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(option.value) },
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isChecked, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Text(option.label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * A slider over an integer range that snaps to [step]. The live value readout
 * above it is weighted heavier than surrounding labels so the number draws the
 * eye without relying on color (DESIGN.md typography rule). The readout also
 * gives the slider a meaningful spoken value.
 */
@Composable
fun LabeledSlider(
    valueLabel: String,
    value: Int,
    valueRange: IntRange,
    step: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        val stepsBetween = (((valueRange.last - valueRange.first) / step) - 1).coerceAtLeast(0)
        Slider(
            value = value.toFloat(),
            onValueChange = { raw ->
                val snapped = valueRange.first +
                    (((raw - valueRange.first) / step).roundToInt() * step)
                onValueChange(snapped.coerceIn(valueRange.first, valueRange.last))
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = stepsBetween,
        )
    }
}

/**
 * One titled section of a form, sitting directly on the background (§7.10 B4).
 *
 * This replaced `SectionCard`, which boxed every section in a grey `Card`.
 * Onboarding was the last place that pattern survived — the 2026-08-24 warmth
 * pass removed it from Today, Progress and Settings but did not reach here, so
 * the first screens anyone sees were still a stack of grey widgets fighting the
 * warm background. Sections are separated by a hairline and by type instead.
 *
 * The title is `titleMedium`, not the `titleLarge` the card used: these are
 * questions a person answers, and eight stacked headline-sized questions shout.
 * They stay full-weight rather than becoming a [SectionLabel] for the same
 * reason Settings' item titles did — a label names a section, but this names
 * something the person has to read and act on.
 *
 * @param topDivider draws the hairline above this section. False for the first
 *   section on a step, where there is nothing above it to separate from.
 */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    topDivider: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        if (topDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
