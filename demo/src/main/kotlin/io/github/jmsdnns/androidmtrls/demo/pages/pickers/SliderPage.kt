package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMSlider
import io.github.jmsdnns.androidmtrls.controls.pickers.AMSliderDecoration
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Slider catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMSlider].
 *
 * id: "pickers-slider", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val sliderEntry: ComponentEntry = ComponentEntry(
    id       = "pickers-slider",
    section  = ComponentSection.Pickers,
    title    = "Slider",
    subtitle = "Continuous value picker",
    intro    = "Continuous value picker with optional leading/trailing icons. Flat by nature — " +
        "a thick split track with a narrow accent handle. Place it in a card or section for the surface.",
    icon    = Icons.AutoMirrored.Filled.VolumeUp,
    content = { SliderPage() },
)

// ---------------------------------------------------------------------------
// SliderPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSlider].
 *
 * Two groups per the demo spec §3.4 (flat-first, card-second structure):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` hosting the widget on the
 *   primary surface directly; the widget self-pads via its own row insets.
 * - **Card** — same widget wrapped in a bare [AMCard] with horizontal page
 *   padding, showing the standard card-wrapped usage.
 *
 * Both groups demonstrate a Volume slider from 0–1 with leading/trailing
 * speaker icons per demo spec §3.4. State is persisted via [rememberSaveable]
 * so the demo stays interactive across recompositions.
 */
@Composable
fun SliderPage() {
    var flatVolume by rememberSaveable { mutableFloatStateOf(0.4f) }
    var cardVolume by rememberSaveable { mutableFloatStateOf(0.4f) }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMSlider(
                value         = flatVolume,
                onValueChange  = { flatVolume = it },
                valueRange    = 0f..1f,
                label         = "Volume",
                leading       = AMSliderDecoration.Icon(Icons.AutoMirrored.Filled.VolumeDown),
                trailing      = AMSliderDecoration.Icon(Icons.AutoMirrored.Filled.VolumeUp),
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
            AMSlider(
                value         = cardVolume,
                onValueChange  = { cardVolume = it },
                valueRange    = 0f..1f,
                label         = "Volume",
                leading       = AMSliderDecoration.Icon(Icons.AutoMirrored.Filled.VolumeDown),
                trailing      = AMSliderDecoration.Icon(Icons.AutoMirrored.Filled.VolumeUp),
            )
        }
    }

}
