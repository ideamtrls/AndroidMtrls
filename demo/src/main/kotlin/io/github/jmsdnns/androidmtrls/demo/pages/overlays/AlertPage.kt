package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMGhostButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentPage
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlert
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlertAction
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlertItem
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the AMAlert component page.
 *
 * id: "overlays-alert", section: [ComponentSection.Overlays].
 *
 * Sets `ownsScaffold = true` so the page composable calls [ComponentPage]
 * itself and [AMAlert] wraps the whole scaffold — the modal blur/scrim then
 * cover the full viewport (below the top bar), matching iOS where `.imAlert`
 * is applied outside `ComponentPage`.
 */
val alertEntry: ComponentEntry = ComponentEntry(
    id       = "overlays-alert",
    section  = ComponentSection.Overlays,
    title    = "Alert",
    subtitle = "Modal are-you-sure prompt",
    intro    = "AMAlert presents a modal card that blocks all input until the " +
        "user taps a button. Attach at your scene root to cover the full screen. " +
        "Two-button alerts show a cancel alongside the primary confirm action; " +
        "one-button alerts require only a single acknowledge action.",
    icon     = Icons.Outlined.ErrorOutline,
    ownsScaffold = true,
    content  = { AlertPage() },
)

// ---------------------------------------------------------------------------
// AlertPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMAlert].
 *
 * The entry sets `ownsScaffold = true`, so this composable — not `DemoNavHost` —
 * calls [ComponentPage], and [AMAlert] wraps the scaffold itself. That gives the
 * host the full page viewport (inside the scaffold's scroll column a
 * `fillMaxSize` host would collapse to wrap-content), so the modal blur/scrim
 * cover everything below the top bar. Mirrors iOS, where `.imAlert` is applied
 * outside `ComponentPage`. Same pattern as [ActionsOverlayPage].
 *
 * Three trigger buttons exercise the common alert patterns:
 * - Confirm action: two-button confirm + cancel
 * - Destructive confirm: two-button confirm with [AMButtonRole.Destructive] + cancel
 * - Acknowledge: single-button dismiss
 */
@Composable
fun AlertPage() {
    // Alert item state — null = dismissed, non-null = presented
    var alertItem by remember { mutableStateOf<AMAlertItem?>(null) }

    // Last result readout — updated by each button's handler
    var lastResult by remember { mutableStateOf<String?>(null) }

    // AMAlert wraps ComponentPage (the whole page viewport), not just the page
    // body — inside the scaffold's scroll column the host would collapse.
    AMAlert(
        item      = alertItem,
        onDismiss = { alertItem = null },
        modifier  = Modifier.fillMaxSize(),
    ) {
      ComponentPage(entry = alertEntry) {
        AlertPageBody(
            onShowConfirm = {
                alertItem  = AMAlertItem(
                    title   = "Add to queue?",
                    message = "Episode 314 will start downloading immediately.",
                    confirm = AMAlertAction(
                        label   = "Add",
                        handler = { lastResult = "added" },
                    ),
                    cancel  = AMAlertAction(
                        label   = "Cancel",
                        handler = { lastResult = "cancelled" },
                    ),
                )
            },
            onShowDestructive = {
                alertItem  = AMAlertItem(
                    title   = "Delete this episode?",
                    message = "This permanently removes it from your queue and downloads.",
                    confirm = AMAlertAction(
                        label   = "Delete",
                        role    = AMButtonRole.Destructive,
                        handler = { lastResult = "deleted" },
                    ),
                    cancel  = AMAlertAction(
                        label   = "Cancel",
                        handler = { lastResult = "cancelled" },
                    ),
                )
            },
            onShowAcknowledge = {
                alertItem  = AMAlertItem(
                    title   = "Connection lost",
                    message = "Reconnect to continue syncing your library.",
                    confirm = AMAlertAction(
                        label   = "OK",
                        handler = { lastResult = "acknowledged" },
                    ),
                )
            },
            lastResult = lastResult,
        )
      }
    }
}

// ---------------------------------------------------------------------------
// AlertPageBody — the content rendered inside the AMAlert host
// ---------------------------------------------------------------------------

@Composable
private fun AlertPageBody(
    onShowConfirm:     () -> Unit,
    onShowDestructive: () -> Unit,
    onShowAcknowledge: () -> Unit,
    lastResult:        String?,
) {
    val tokens = LocalAMTokens.current
    val isDark  = LocalAMIsDarkTheme.current

    // ── Triggers group ───────────────────────────────────────────────────────
    // iOS insets the trigger VStack by the lg page margin (Overlays.swift) —
    // the buttons never run full-bleed to the screen edges.
    catalogGroup(title = "Triggers") {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            AMPrimaryButton(
                label    = "Confirm action",
                onClick  = onShowConfirm,
                modifier = Modifier.fillMaxWidth(),
            )
            AMSecondaryButton(
                label    = "Destructive confirm",
                onClick  = onShowDestructive,
                modifier = Modifier.fillMaxWidth(),
            )
            AMGhostButton(
                label    = "One-button acknowledge",
                onClick  = onShowAcknowledge,
                modifier = Modifier.fillMaxWidth(),
            )

            // Last result readout — mirrors iOS HStack: caption secondary label +
            // monospaced primary value + trailing Spacer
            Row(
                modifier = Modifier.padding(top = tokens.spacing.xs),
            ) {
                BasicText(
                    text  = "Last result:",
                    style = TextStyle(
                        color    = AMTheme.Text.secondary.resolve(isDark),
                        fontSize = 12.sp,
                    ),
                )
                BasicText(
                    text  = " ${lastResult ?: "—"}",
                    style = TextStyle(
                        color      = AMTheme.Text.primary.resolve(isDark),
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
