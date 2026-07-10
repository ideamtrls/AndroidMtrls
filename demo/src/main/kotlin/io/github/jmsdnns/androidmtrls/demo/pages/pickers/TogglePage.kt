package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMToggleRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Toggle catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMToggleRow].
 *
 * id: "pickers-toggle", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val toggleEntry: ComponentEntry = ComponentEntry(
    id       = "pickers-toggle",
    section  = ComponentSection.Pickers,
    title    = "Toggle",
    subtitle = "Flat row switch",
    intro    = "Flat row style for a Toggle. Renders the caller's label leading and the " +
        "accent-tracked switch trailing. Place it in a card or section for the surface.",
    icon    = Icons.Default.ToggleOn,
    content = { TogglePage() },
)

// ---------------------------------------------------------------------------
// TogglePage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMToggleRow].
 *
 * Two groups per the demo spec §3.4 (flat-first, card-second structure):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` hosting the widget on the
 *   primary surface directly; the widget self-pads via its own row insets.
 * - **Card** — same widget wrapped in a bare [AMCard] with horizontal page
 *   padding, showing the standard card-wrapped usage.
 *
 * Both groups demonstrate the "Notifications" toggle named in the demo spec.
 * State is persisted via [rememberSaveable] so the demo stays interactive
 * across recompositions.
 */
@Composable
fun TogglePage() {
    var flatNotifications by rememberSaveable { mutableStateOf(true) }
    var cardNotifications by rememberSaveable { mutableStateOf(true) }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMToggleRow(
                label           = "Notifications",
                checked         = flatNotifications,
                onCheckedChange = { flatNotifications = it },
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
            AMToggleRow(
                label           = "Notifications",
                checked         = cardNotifications,
                onCheckedChange = { cardNotifications = it },
            )
        }
    }

}
