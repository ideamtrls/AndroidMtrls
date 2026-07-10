package io.github.jmsdnns.androidmtrls.demo.pages.textinputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.inputs.AMFieldLabel
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextField
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// FieldLabel catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMFieldLabel].
 *
 * id: "textinputs-fieldlabel", section: [ComponentSection.TextInputs].
 * Mirrors iOS `IMFieldLabelPage`.
 *
 * **Registration:** add this entry to the TextInputs aggregation list. This
 * file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val fieldLabelEntry: ComponentEntry = ComponentEntry(
    id = "textinputs-fieldlabel",
    section = ComponentSection.TextInputs,
    title = "Field Label",
    tileTitle = "Label",
    subtitle = "Standing label above an input",
    intro = "Standing micro-label placed above an input — the Mtrls labeling pattern. Inputs " +
        "carry no label of their own; the label is external, above the field or control. " +
        "Aligns to the input inset so its first glyph sits directly above the field's first " +
        "character.",
    icon = Icons.Default.Label,
    content = { FieldLabelPage() },
)

// ---------------------------------------------------------------------------
// FieldLabelPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMFieldLabel], mirroring iOS `IMFieldLabelPage`. Each group
 * pairs an [AMFieldLabel] with its [AMTextField] in a `Column` at
 * [AMTheme.Spacing] `.xs` — an empty "Title" field (placeholder as hint) and a
 * filled "Notes" field (placeholder gone, the standing label carries the name):
 *
 * - **Flat** — the labeled fields directly on the page: the label sits on the
 *   backdrop, outside each field's white surface.
 * - **Card** — the same pairing inside a bare [AMCard].
 */
@Composable
fun FieldLabelPage() {
    var flatTitle by rememberSaveable { mutableStateOf("") }
    var flatNotes by rememberSaveable { mutableStateOf("A filled field keeps its label.") }
    var cardTitle by rememberSaveable { mutableStateOf("") }
    var cardNotes by rememberSaveable { mutableStateOf("A filled field keeps its label.") }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                AMFieldLabel("Title")
                AMTextField(
                    value = flatTitle,
                    onValueChange = { flatTitle = it },
                    placeholder = "Title",
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                AMFieldLabel("Notes")
                AMTextField(
                    value = flatNotes,
                    onValueChange = { flatNotes = it },
                    placeholder = "Notes",
                )
            }
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Card") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            Column(
                modifier = Modifier.padding(vertical = AMTheme.Spacing.md.dp),
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                    AMFieldLabel("Title")
                    AMTextField(
                        value = cardTitle,
                        onValueChange = { cardTitle = it },
                        placeholder = "Title",
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                    AMFieldLabel("Notes")
                    AMTextField(
                        value = cardNotes,
                        onValueChange = { cardNotes = it },
                        placeholder = "Notes",
                    )
                }
            }
        }
    }

}
