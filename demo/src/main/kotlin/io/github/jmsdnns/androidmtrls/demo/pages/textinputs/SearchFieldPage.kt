package io.github.jmsdnns.androidmtrls.demo.pages.textinputs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.inputs.AMSearchField
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// SearchField catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMSearchField].
 *
 * id: "textinputs-searchfield", section: [ComponentSection.TextInputs].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the TextInputs
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val searchFieldEntry: ComponentEntry = ComponentEntry(
    id = "textinputs-searchfield",
    section = ComponentSection.TextInputs,
    title = "Search Field",
    tileTitle = "Search",
    subtitle = "Search-specialized input",
    intro = "Search-specialized text input in the Android search-bar idiom: a full pill filled " +
        "with the neutral input grey, a static leading magnifier, and a trailing clear button " +
        "that appears whenever the field holds text. No bottom indicator and no label — search " +
        "fields don't label themselves. The Search key dismisses the keyboard.",
    icon = Icons.Rounded.Search,
    content = { SearchFieldPage() },
)

// ---------------------------------------------------------------------------
// SearchFieldPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSearchField].
 *
 * Two groups per the demo spec §3.2 (flat-first, card-second structure):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` wrapping the widget directly.
 *   The widget self-pads; no extra inset is applied.
 * - **Card** — widget inside a bare [AMCard] with horizontal page padding on
 *   the container, showing the standard card-wrapped usage.
 */
@Composable
fun SearchFieldPage() {
    // Persist field state across recompositions so the demo stays interactive.
    var flatQuery by rememberSaveable { mutableStateOf("") }
    var cardQuery by rememberSaveable { mutableStateOf("") }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMSearchField(
                value = flatQuery,
                onValueChange = { flatQuery = it },
                placeholder = "Search",
                onSubmit = {},
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
            AMSearchField(
                value = cardQuery,
                onValueChange = { cardQuery = it },
                placeholder = "Search",
                onSubmit = {},
                modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
            )
        }
    }

}
