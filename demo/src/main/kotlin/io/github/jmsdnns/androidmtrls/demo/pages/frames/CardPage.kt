package io.github.jmsdnns.androidmtrls.demo.pages.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextField
import io.github.jmsdnns.androidmtrls.controls.pickers.AMToggleRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Card catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMCard].
 *
 * id: "frames-card", section: Frames.
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Frames slot.
 * This file intentionally does not touch any shared registry — registration is
 * the task owner's responsibility per the task brief.
 */
val cardEntry: ComponentEntry = ComponentEntry(
    id = "frames-card",
    section = ComponentSection.Frames,
    title = "Card",
    subtitle = "The foundational surface card",
    intro = "The surface-card visual (Surface.primary fill + rounded corners + low elevation) " +
        "exposed as a one-line modifier. The visual foundation other primitives compose on.",
    icon = Icons.Default.CreditCard,
    content = { CardPage() },
)

// ---------------------------------------------------------------------------
// CardPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMCard].
 *
 * Two groups mirroring the iOS demo spec §3.5:
 * - **Default** — the exact iOS paragraph inside an [AMCard].
 * - **Nested** — an outer card containing an [AMTextField], an [AMToggleRow],
 *   and a nested [AMCard] (demoted to Flat), demonstrating the surface-style
 *   demotion contract. Mirrors iOS's IMCardPage nested group.
 */
@Composable
fun CardPage() {
    val isDark = LocalAMIsDarkTheme.current
    var nestedFieldText by rememberSaveable {
        mutableStateOf("Caret stays inside the parent card")
    }
    var nestedToggle by rememberSaveable { mutableStateOf(true) }

    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp, vertical = AMTheme.Spacing.sm.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                BasicText(
                    text = "The rain held off until evening, which gave the field crew a clean " +
                        "window to finish surveying the eastern fence line. By the time the first " +
                        "drops landed everyone was packed up and on the road back, comparing notes " +
                        "about which posts would need replacing before winter.",
                    style = TextStyle(
                        color = AMTheme.Text.primary.resolve(isDark),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    ),
                )
            }
        }
    }

    // ── Nested ───────────────────────────────────────────────────────────────
    // Widgets (text fields, toggles, pickers, etc.) are flat by nature —
    // they never self-card and always get their surface from the container.
    // The nested inner AMCard below demonstrates LocalAMSurfaceStyle environment
    // demotion: it loses its shadow and rounded corners because the parent card
    // sets the environment to Flat.
    catalogGroup(title = "Nested") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp, vertical = AMTheme.Spacing.sm.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            AMCard(
                modifier = Modifier.fillMaxWidth(),
                padding = AMPadding.block,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    AMTextField(
                        value = nestedFieldText,
                        onValueChange = { nestedFieldText = it },
                        placeholder = "",
                    )

                    AMToggleRow(
                        label = "Notifications",
                        checked = nestedToggle,
                        onCheckedChange = { nestedToggle = it },
                    )

                    // Nested AMCard — reads Flat from LocalAMSurfaceStyle and
                    // renders nothing: no fill, no shadow, no hairline, no padding.
                    // This demonstrates the nesting demotion contract.
                    AMCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = AMPadding.block,
                    ) {
                        BasicText(
                            text = "Nested card — demoted via LocalAMSurfaceStyle: no shadow, " +
                                "no rounded corners, flush against the parent's interior.",
                            style = TextStyle(
                                color = AMTheme.Text.primary.resolve(isDark),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}
