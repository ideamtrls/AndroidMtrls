package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.pickers.AMStepper
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Stepper catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMStepper].
 *
 * id: "pickers-stepper", section: [ComponentSection.Pickers].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Pickers
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val stepperEntry: ComponentEntry = ComponentEntry(
    id = "pickers-stepper",
    section = ComponentSection.Pickers,
    title = "Stepper",
    subtitle = "Integer increment control",
    intro = "Integer increment/decrement row control. Labels the value; the +/- buttons sit " +
        "trailing. Flat by nature — place it in a card or section for the surface.",
    icon = Icons.Default.Add,
    content = { StepperPage() },
)

// ---------------------------------------------------------------------------
// StepperPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMStepper].
 *
 * Two groups per the demo spec §3.4 (flat-first, card-second structure):
 *
 * - **Flat** — `catalogFlatDemo(padding = 0.dp)` hosting the stepper directly
 *   on a [AMTheme.Surface.primary] strip. The widget self-pads via row insets.
 * - **Card** — stepper inside a bare [AMCard] with horizontal page padding on
 *   the container, showing the standard card-wrapped usage.
 *
 * State: Quantity 1–20, initial value 1. Persisted across recompositions via
 * [rememberSaveable].
 */
@Composable
fun StepperPage() {
    var flatQty by rememberSaveable { mutableStateOf(1) }
    var cardQty by rememberSaveable { mutableStateOf(1) }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo(padding = 0.dp) {
            AMStepper(
                value = flatQty,
                onValueChange = { flatQty = it },
                valueRange = 1..20,
                label = "Quantity",
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
            AMStepper(
                value = cardQty,
                onValueChange = { cardQty = it },
                valueRange = 1..20,
                label = "Quantity",
            )
        }
    }

}
