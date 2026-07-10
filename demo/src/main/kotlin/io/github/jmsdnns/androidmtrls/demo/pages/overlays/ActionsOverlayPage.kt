package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFABAlignment
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFloatingActionButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentPage
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMActionsOverlay
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AMActionsOverlay] demo page.
 *
 * id: "overlays-actionsoverlay", section: [ComponentSection.Overlays].
 */
val actionsOverlayEntry: ComponentEntry = ComponentEntry(
    id       = "overlays-actionsoverlay",
    section  = ComponentSection.Overlays,
    title    = "Actions Overlay",
    tileTitle = "Actions",
    subtitle = "Free-floating actions layer",
    intro    = "AMActionsOverlay wraps scene content and presents free-floating, " +
        "bottom-anchored action tiles over a blurred, scrimmed backdrop while isPresented is true. " +
        "Attach at the scene root to cover the full screen. " +
        "Tap outside, drag down, or press back to dismiss. Pass onDismiss = null for a non-dismissable overlay.",
    icon     = Icons.Default.GridView,
    ownsScaffold = true,
    content  = { ActionsOverlayPage() },
)

// ---------------------------------------------------------------------------
// ActionsOverlayPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMActionsOverlay].
 *
 * The entry sets `ownsScaffold = true`, so this composable — not `DemoNavHost`
 * — calls [ComponentPage], and [AMActionsOverlay] wraps the scaffold itself.
 * That gives the host the full page viewport (the scaffold's scroll column has
 * unbounded height — a host attached inside it collapses to wrap-content), so
 * the blur/scrim cover everything below the top bar and the grid anchors to
 * the bottom of the screen. Mirrors iOS, where `.imActionsOverlay` is applied
 * outside `ComponentPage` (`Pages/Overlays.swift`).
 *
 * **Group "Default":** a centered [AMFloatingActionButton] opens the overlay
 * (`isPresented = true`). The overlayContent is an [AMActionGrid] holding a
 * single 3-tile [AMActionGridRow] of [AMActionGridButton]s — each tap calls the
 * onDismiss callback passed from the host, dismissing the overlay.
 *
 * **Blur note:** `Modifier.amBackdropBlur` uses [BlurEffect] via
 * `graphicsLayer { renderEffect = … }`. Blur does not render in Android Studio
 * preview — validate on a physical device or emulator (API 31+).
 */
@Composable
fun ActionsOverlayPage() {
    var isPresented by remember { mutableStateOf(false) }

    // FAB offset — hoisted here so it persists across overlay open/close.
    // Per AMFloatingActionButton KDoc: "hoist to the scene root … so the
    // position resets on remount" — placing it here (the scene root for
    // this demo zone) satisfies that requirement.
    val fabOffset = remember { mutableStateOf(Offset.Zero) }

    // The host wraps ComponentPage (the whole page viewport), not the page
    // body — inside the scaffold's scroll column the host would collapse.
    AMActionsOverlay(
        isPresented    = isPresented,
        onDismiss      = { isPresented = false },
        overlayContent = {
            ActionsOverlayGrid(onDismiss = { isPresented = false })
        },
    ) {
        ComponentPage(entry = actionsOverlayEntry) {
            ActionsOverlayPageBody(
                fabOffset  = fabOffset,
                onOpenOverlay = { isPresented = true },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ActionsOverlayGrid — the 3-up action grid shown inside the overlay
// ---------------------------------------------------------------------------

/**
 * The canonical [AMActionGrid] filling [AMActionsOverlay]'s overlayContent — a
 * single 3-tile [AMActionGridRow]. The grid owns the bottom-anchor recipe (the
 * leading weighted spacer, `spacing.lg` horizontal inset, `spacing.xxl` bottom
 * lift) and the row owns its equal-width columns; the tiles float free over the
 * blurred backdrop at their default (self-carded) elevation and dismiss the
 * overlay via [onDismiss] after their own action.
 */
@Composable
private fun ActionsOverlayGrid(onDismiss: () -> Unit) {
    AMActionGrid {
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon      = Icons.Default.Search,
                    title     = "Find",
                    onClick   = { onDismiss() },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon      = Icons.Default.Delete,
                    title     = "Clean",
                    role      = AMButtonRole.Destructive,
                    onClick   = { onDismiss() },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon      = Icons.Default.Settings,
                    title     = "Settings",
                    onClick   = { onDismiss() },
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------
// ActionsOverlayPageBody — inner content (the hostContent passed to the host)
// ---------------------------------------------------------------------------

/**
 * The scrollable demo body — rendered as [AMActionsOverlay]'s [hostContent].
 * Placed in a separate composable so the host wrapping is clear at the
 * [ActionsOverlayPage] call site.
 */
@Composable
private fun ActionsOverlayPageBody(
    fabOffset: MutableState<Offset>,
    onOpenOverlay: () -> Unit,
) {
    catalogGroup(title = "Default") {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            contentAlignment = Alignment.Center,
        ) {
            AMFloatingActionButton(
                offset             = fabOffset,
                alignment          = AMFABAlignment.Center,
                accessibilityLabel = "Show Actions View",
                onClick            = onOpenOverlay,
            )
        }
    }
}
