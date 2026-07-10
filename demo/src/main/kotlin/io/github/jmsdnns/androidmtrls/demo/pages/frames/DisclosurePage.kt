package io.github.jmsdnns.androidmtrls.demo.pages.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
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
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMDisclosure
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Disclosure catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMDisclosure].
 *
 * id: "frames-disclosure", section: Frames.
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Frames slot.
 * This file intentionally does not touch any shared registry — registration is
 * the task owner's responsibility per the task brief.
 */
val disclosureEntry: ComponentEntry = ComponentEntry(
    id = "frames-disclosure",
    section = ComponentSection.Frames,
    title = "Disclosure",
    subtitle = "Expandable card with chevron",
    intro = "Surface-card framing for an expandable group. Rotating chevron header that " +
        "toggles a content slot below, all inside one card surface.",
    icon = Icons.Default.UnfoldMore,
    content = { DisclosurePage() },
)

// ---------------------------------------------------------------------------
// DisclosurePage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMDisclosure].
 *
 * Three groups mirroring the iOS demo (DisclosureGroupCardPage):
 * - **Flat** — disclosure on a flat-demo strip with a "Strict mode" [AMToggleRow].
 * - **Card** — disclosure with horizontal page padding and a "Strict mode" toggle.
 * - **Nested** — disclosure inside an outer [AMCard] with a "Strict mode" toggle,
 *   demonstrating Flat demotion: the inner disclosure renders without
 *   shadow/hairline, riding on the parent card's surface.
 *
 * All three groups start expanded (mirrors iOS `isExpanded = true` initial state).
 */
@Composable
fun DisclosurePage() {
    var flatExpanded by rememberSaveable { mutableStateOf(true) }
    var flatStrict by rememberSaveable { mutableStateOf(true) }
    var cardExpanded by rememberSaveable { mutableStateOf(true) }
    var cardStrict by rememberSaveable { mutableStateOf(true) }
    var nestedExpanded by rememberSaveable { mutableStateOf(true) }
    var nestedStrict by rememberSaveable { mutableStateOf(true) }

    // ── Flat ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Flat") {
        catalogFlatDemo {
            AMDisclosure(
                label = "Advanced",
                expanded = flatExpanded,
                onExpandedChange = { flatExpanded = it },
            ) {
                AMToggleRow(
                    label = "Strict mode",
                    checked = flatStrict,
                    onCheckedChange = { flatStrict = it },
                )
            }
        }
    }

    // ── Card ─────────────────────────────────────────────────────────────────
    catalogGroup(title = "Card") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            AMDisclosure(
                label = "Advanced",
                expanded = cardExpanded,
                onExpandedChange = { cardExpanded = it },
            ) {
                AMToggleRow(
                    label = "Strict mode",
                    checked = cardStrict,
                    onCheckedChange = { cardStrict = it },
                )
            }
        }
    }

    // ── Nested ───────────────────────────────────────────────────────────────
    // Disclosure inside a card — demonstrates LocalAMSurfaceStyle demotion:
    // the nested disclosure's card renders without shadow or hairline.
    catalogGroup(title = "Nested") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                AMDisclosure(
                    label = "Advanced",
                    expanded = nestedExpanded,
                    onExpandedChange = { nestedExpanded = it },
                ) {
                    AMToggleRow(
                        label = "Strict mode",
                        checked = nestedStrict,
                        onCheckedChange = { nestedStrict = it },
                    )
                }
            }
        }
    }
}
