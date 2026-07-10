package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMColorPicker
import io.github.jmsdnns.androidmtrls.controls.pickers.AMColorSaver
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// ColorPicker catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMColorPicker].
 *
 * id: "pickers-color", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val colorPickerEntry: ComponentEntry = ComponentEntry(
    id       = "pickers-color",
    section  = ComponentSection.Pickers,
    title    = "Color Picker",
    tileTitle = "Color",
    subtitle = "Row with color swatch",
    intro    = "Flat row wrapper around a color picker. Caller's label leading, a filled " +
        "color swatch trailing — tap to open the palette dialog and pick a color. Place it " +
        "in a card or section for the surface.",
    icon    = Icons.Default.Palette,
    content = { ColorPickerPage() },
)

// ---------------------------------------------------------------------------
// ColorPickerPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMColorPicker].
 *
 * Two groups per the demo spec §3.4 (flat-first, card-second structure):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` wrapping the widget directly.
 * - **Card** — widget inside a bare [AMCard] with horizontal page padding on
 *   the container, showing the standard card-wrapped usage.
 *
 * Both groups show the same "Accent" label with initial indigo color as
 * specified in the demo spec.
 *
 * Color state is persisted across process death via [AMColorSaver] which
 * encodes the [Color] as its ULong bit pattern.
 */
@Composable
fun ColorPickerPage() {
    // systemIndigo = 0xFF5856D6 — initial per demo spec §3.4.
    var flatAccent by rememberSaveable(stateSaver = AMColorSaver) {
        mutableStateOf(Color(0xFF5856D6))
    }
    var cardAccent by rememberSaveable(stateSaver = AMColorSaver) {
        mutableStateOf(Color(0xFF5856D6))
    }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMColorPicker(
                label             = "Accent",
                selection         = flatAccent,
                onSelectionChange = { flatAccent = it },
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
            AMColorPicker(
                label             = "Accent",
                selection         = cardAccent,
                onSelectionChange = { cardAccent = it },
            )
        }
    }

}
