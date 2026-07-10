package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToast
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastAnchor
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastItem
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AMToast] host composable.
 *
 * id: "overlays-toast", section: [ComponentSection.Overlays].
 */
val toastEntry: ComponentEntry = ComponentEntry(
    id       = "overlays-toast",
    section  = ComponentSection.Overlays,
    title    = "Toast",
    subtitle = "Bottom pill for confirmations",
    intro    = "AMToast wraps scene content and presents a small floating pill at the " +
        "bottom of the screen. Toasts are deliberately neutral — no semantic tint. " +
        "For tinted or action-required feedback use AMBanner. Toasts auto-dismiss " +
        "after their duration (default 2 s), or the user can dismiss by tapping or " +
        "swiping down. Attach at the scene root so the pill covers the full screen.",
    icon     = Icons.Filled.Chat,
    content  = { ToastDemoPage() },
)

// ---------------------------------------------------------------------------
// ToastDemoPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMToast].
 *
 * Wraps the page body in [AMToast] so the pill overlays the full content
 * area — the correct attachment pattern (attach at the scene root; comment
 * in this file mirrors the KDoc advice in Toast.kt).
 *
 * The "Variants" group shows three [AMSecondaryButton]s arranged to match iOS:
 * - **Plain** — text only ("Saved"), default 2 s dwell.
 * - **With icon** — leading icon glyph + text ("Copied"), default 2 s dwell.
 * - **Longer dwell** — text + checkmark icon ("Added to your queue"), 4 000 ms dwell.
 *
 * The "Anchor" group's "Trailing anchor" button raises a SECOND toast pinned to
 * the bottom-trailing corner ([AMToastAnchor.Trailing]) through a nested host —
 * mirroring iOS's two stacked `.imToast` modifiers (`Pages/Overlays.swift`).
 * Trigger rows are inset by the lg page margin (iOS insets them).
 */
@Composable
fun ToastDemoPage() {
    // Two independent toast states, one per host/anchor.
    var centerItem by remember { mutableStateOf<AMToastItem?>(null) }
    var trailingItem by remember { mutableStateOf<AMToastItem?>(null) }

    // Two stacked AMToast hosts (center + trailing anchor) wrap the page body —
    // attach at the page level so the pills cover the whole content area.
    AMToast(
        item      = centerItem,
        onDismiss = { centerItem = null },
    ) {
        AMToast(
            item      = trailingItem,
            onDismiss = { trailingItem = null },
            anchor    = AMToastAnchor.Trailing,
        ) {
            // ── Page body (hostContent) ───────────────────────────────────
            // AMToast's hostContent slot is a Box — stack the groups in an
            // explicit Column (xl gap, mirroring ComponentPage's arrangement)
            // or they superimpose.
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp)) {
            catalogGroup(title = "Variants") {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AMTheme.Spacing.lg.dp),
                    verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                    ) {
                        AMSecondaryButton(
                            label    = "Plain",
                            onClick  = {
                                centerItem = AMToastItem(text = "Saved")
                            },
                            modifier = Modifier.weight(1f),
                        )
                        AMSecondaryButton(
                            label    = "With icon",
                            onClick  = {
                                centerItem = AMToastItem(
                                    text = "Copied",
                                    icon = Icons.Filled.ContentCopy,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                    ) {
                        AMSecondaryButton(
                            label    = "Longer dwell",
                            onClick  = {
                                centerItem = AMToastItem(
                                    text     = "Added to your queue",
                                    icon     = Icons.Default.Check,
                                    duration = 4000L,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // ── Anchor group — trailing-corner toast via the second host ──
            catalogGroup(title = "Anchor") {
                AMSecondaryButton(
                    label    = "Trailing anchor",
                    onClick  = {
                        trailingItem = AMToastItem(
                            text = "Bottom-trailing",
                            icon = Icons.Filled.SouthEast,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AMTheme.Spacing.lg.dp),
                )
            }
            }
        }
    }
}
