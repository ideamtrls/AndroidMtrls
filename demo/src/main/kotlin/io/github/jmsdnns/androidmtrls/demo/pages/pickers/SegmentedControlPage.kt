package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMSegmentedControl
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Segmented Control catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMSegmentedControl].
 *
 * id: "pickers-segmented", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val segmentedControlEntry: ComponentEntry = ComponentEntry(
    id = "pickers-segmented",
    section = ComponentSection.Pickers,
    title = "Segmented Control",
    tileTitle = "Segmented",
    subtitle = "One choice from a small set",
    intro = "Self-framed connected button group — an outlined capsule of equal segments. " +
        "For choosing one option from a small fixed set.",
    icon = Icons.Default.LinearScale,
    content = { SegmentedControlPage() },
)

// ---------------------------------------------------------------------------
// SegmentedControlPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSegmentedControl].
 *
 * Single "Default" group per demo spec §3.4 — no flat/card split because
 * [AMSegmentedControl] is already self-framed. Options: All / Mine / Archived.
 */
@Composable
fun SegmentedControlPage() {
    // Persist selection across recompositions so the demo stays interactive.
    var selection by rememberSaveable { mutableStateOf("all") }

    // ── Default ───────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        AMSegmentedControl(
            selection = selection,
            onSelectionChange = { selection = it },
            options = listOf(
                AMSegmentedControl.Option("All", "all"),
                AMSegmentedControl.Option("Mine", "mine"),
                AMSegmentedControl.Option("Archived", "archived"),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        )
    }

}
