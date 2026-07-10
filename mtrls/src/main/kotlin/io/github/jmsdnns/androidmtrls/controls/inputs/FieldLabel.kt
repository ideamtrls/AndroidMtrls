package io.github.jmsdnns.androidmtrls.controls.inputs

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.controls.pickers.AMSegmentedControl
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMFieldLabel
// ---------------------------------------------------------------------------

/**
 * A standing micro-label placed above a control that carries no label of its
 * own — [AMSegmentedControl], a picker row, or a text field rendered without
 * its floating `label` (as a fixed-label alternative that never moves).
 *
 * Renders [text] in [AMSectionHeader] typography (uppercase 12sp Medium),
 * leading-aligned across the full width, with horizontal padding equal to the
 * input inset token ([AMTheme.Input] `.inset`) so the label tracks the input
 * metrics if they are re-tuned via `.amTokens`.
 *
 * Pair it with its field in a [Column] at [AMTheme.Spacing] `.xs`:
 *
 * ```kotlin
 * Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
 *     AMFieldLabel("Title")
 *     AMTextField(value = title, onValueChange = { title = it }, placeholder = "Title")
 * }
 * ```
 *
 * iOS equivalent: `IMFieldLabel` (`Sources/Controls/Inputs/FieldLabel.swift`).
 *
 * @param text     The label text. Rendered uppercased by [AMSectionHeader].
 * @param modifier Modifier applied to the full-width label container.
 */
@Composable
fun AMFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Input.inset.dp),
    ) {
        AMSectionHeader(title = text)
    }
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMFieldLabel · Light",
    group = "AMFieldLabel",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7, // AMTheme.Surface.page light
)
@Composable
private fun AMFieldLabelPreviewLight() {
    AMThemeRoot {
        AMFieldLabelPreviewContent()
    }
}

@Preview(
    name = "AMFieldLabel · Dark",
    group = "AMFieldLabel",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D, // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMFieldLabelPreviewDark() {
    AMThemeRoot {
        AMFieldLabelPreviewContent()
    }
}

@Composable
private fun AMFieldLabelPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("A filled field keeps its label.") }
    var scope by remember { mutableStateOf("all") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.primary.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Label above an empty AMTextField — the field has no label of its own.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Title")
            AMTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Title",
            )
        }

        // Label above a filled AMTextEditor — keeps the field identifiable once
        // its placeholder is gone.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Notes")
            AMTextEditor(
                value = notes,
                onValueChange = { notes = it },
                placeholder = "Notes",
                minHeight = 60.dp,
            )
        }

        // Label above an AMSegmentedControl — the control has no label of its own.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Scope")
            AMSegmentedControl(
                selection = scope,
                onSelectionChange = { scope = it },
                options = listOf(
                    AMSegmentedControl.Option("All", "all"),
                    AMSegmentedControl.Option("Mine", "mine"),
                    AMSegmentedControl.Option("Archived", "archived"),
                ),
            )
        }
    }
}
