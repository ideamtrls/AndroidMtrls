package io.github.jmsdnns.androidmtrls.demo.pages.textinputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextField
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// TextField catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMTextField].
 *
 * id: "textinputs-textfield", section: [ComponentSection.TextInputs].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the TextInputs
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val textFieldEntry: ComponentEntry = ComponentEntry(
    id = "textinputs-textfield",
    section = ComponentSection.TextInputs,
    title = "Text Field",
    tileTitle = "Text",
    subtitle = "Single-line text input",
    intro = "Single-line text input with the Android filled anatomy on the Mtrls surface: a " +
        "white floating container with 4dp top corners and a bottom indicator line that " +
        "thickens to the accent on focus. Labels are external — pair with a standing Field " +
        "Label. A trailing Close button clears the text while the field is focused.",
    icon = Icons.Default.TextFields,
    content = { TextFieldPage() },
)

// ---------------------------------------------------------------------------
// TextFieldPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMTextField] (flat-first, card-second):
 *
 * - **Flat** — a placeholder-only field on a flat strip (the field carries no
 *   label; labeling is external — see the Field Label page).
 * - **Card** — a placeholder-only field inset inside an [AMCard].
 */
@Composable
fun TextFieldPage() {
    // Persist field state across recompositions so the demo stays interactive.
    var flatText by rememberSaveable { mutableStateOf("") }
    var cardText by rememberSaveable { mutableStateOf("") }

    // ── Flat ──────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMTextField(
                value = flatText,
                onValueChange = { flatText = it },
                placeholder = "you@example.com",
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
            AMTextField(
                value = cardText,
                onValueChange = { cardText = it },
                placeholder = "No label · placeholder only",
                modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
            )
        }
    }

}
