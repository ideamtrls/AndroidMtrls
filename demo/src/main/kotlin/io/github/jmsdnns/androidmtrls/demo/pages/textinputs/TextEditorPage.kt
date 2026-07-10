package io.github.jmsdnns.androidmtrls.demo.pages.textinputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.inputs.AMFieldLabel
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextEditor
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// TextEditor catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMTextEditor].
 *
 * id: "textinputs-texteditor", section: [ComponentSection.TextInputs].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the TextInputs
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val textEditorEntry: ComponentEntry = ComponentEntry(
    id = "textinputs-texteditor",
    section = ComponentSection.TextInputs,
    title = "Text Editor",
    tileTitle = "Editor",
    subtitle = "Auto-growing multiline input",
    intro = "Multiline text input with the filled block anatomy: a white floating container " +
        "with all four corners rounded 4dp and an accent bottom indicator on focus. Auto-grows " +
        "between minHeight and maxHeight, then scrolls. Label it with a standing Field Label.",
    icon = Icons.Default.Notes,
    content = { TextEditorPage() },
)

// ---------------------------------------------------------------------------
// TextEditorPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMTextEditor] (flat-first, card-second):
 *
 * - **Label** — an editor under a standing [AMFieldLabel], both directly on the
 *   page (the label sits on the backdrop, outside the editor's white surface).
 * - **Card** — a placeholder-only editor inset inside an [AMCard].
 */
@Composable
fun TextEditorPage() {
    // Persist field state across recompositions so the demo stays interactive.
    var labeledText by rememberSaveable { mutableStateOf("") }
    var cardText by rememberSaveable { mutableStateOf("") }

    // ── Label ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Label") {
        Column(
            modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp),
        ) {
            AMFieldLabel("Notes")
            AMTextEditor(
                value = labeledText,
                onValueChange = { labeledText = it },
                placeholder = "Tap to write…",
            )
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Card") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMTextEditor(
                value = cardText,
                onValueChange = { cardText = it },
                placeholder = "Tap to write…",
                modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
            )
        }
    }

}
