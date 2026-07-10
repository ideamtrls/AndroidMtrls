package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMMenuPicker
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// MenuPicker catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMMenuPicker].
 *
 * id: "pickers-menu", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val menuPickerEntry: ComponentEntry = ComponentEntry(
    id = "pickers-menu",
    section = ComponentSection.Pickers,
    title = "Menu Picker",
    tileTitle = "Menu",
    subtitle = "Row that opens a menu",
    intro = "Flat row picker that opens a menu when tapped. Shows the current selection trailing " +
        "the label; tap to choose from a list. Place it in a card or section for the surface.",
    icon = Icons.Default.List,
    content = { MenuPickerPage() },
)

// ---------------------------------------------------------------------------
// MenuPickerPage
// ---------------------------------------------------------------------------

private val sortOptions = listOf("Newest", "Oldest", "Most replied")

/**
 * Demo page for [AMMenuPicker].
 *
 * Two groups per the demo spec §3.4 (flat-first, card-second structure):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` wrapping the widget directly on
 *   the `Surface.primary` strip. The widget self-pads via its internal row insets.
 * - **Card** — widget inside a bare [AMCard] with horizontal page padding on the
 *   container, showing the standard card-wrapped usage.
 *
 * Demo content: "Sort by" picker with options Newest / Oldest / Most replied.
 * State is persisted via [rememberSaveable] so the selection survives
 * recomposition.
 */
@Composable
fun MenuPickerPage() {
    var flatSort by rememberSaveable { mutableStateOf(sortOptions.first()) }
    var cardSort by rememberSaveable { mutableStateOf(sortOptions.first()) }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMMenuPicker(
                label = "Sort by",
                selection = flatSort,
                onSelectionChange = { flatSort = it },
                options = sortOptions,
                display = { it },
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
            AMMenuPicker(
                label = "Sort by",
                selection = cardSort,
                onSelectionChange = { cardSort = it },
                options = sortOptions,
                display = { it },
            )
        }
    }

}
