package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentPage
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMLoadingOverlay
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AMLoadingOverlay] demo page.
 *
 * id: "overlays-loadingoverlay", section: [ComponentSection.Overlays].
 */
val loadingOverlayEntry: ComponentEntry = ComponentEntry(
    id       = "overlays-loadingoverlay",
    section  = ComponentSection.Overlays,
    title    = "Loading Overlay",
    tileTitle = "Loading",
    subtitle = "Blocking spinner over blur",
    intro    = "AMLoadingOverlay wraps scene content and presents a blurred, " +
        "scrimmed spinner while an async operation is in progress. " +
        "Attach at the scene root to cover the full screen. " +
        "No dismiss gesture — the caller controls isPresented.",
    icon     = Icons.Filled.HourglassEmpty,
    ownsScaffold = true,
    content  = { LoadingOverlayPage() },
)

// ---------------------------------------------------------------------------
// LoadingOverlayPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMLoadingOverlay].
 *
 * The entry sets `ownsScaffold = true`, so this composable — not `DemoNavHost` —
 * calls [ComponentPage], and [AMLoadingOverlay] wraps the scaffold itself. That
 * gives the host the full page viewport (inside the scaffold's scroll column a
 * `fillMaxSize` host would collapse to wrap-content), so the blur/scrim cover
 * everything below the top bar. Mirrors iOS, where `.imLoadingOverlay` is
 * applied outside `ComponentPage`. Same pattern as [ActionsOverlayPage].
 *
 * **Group "Default":** an [AMSecondaryButton] (inset by the lg page margin, per
 * iOS) sets `isLoading = true`; a [LaunchedEffect] delays 2000ms then clears it.
 *
 * **Blur note:** `Modifier.amBackdropBlur` uses [BlurEffect] via
 * `graphicsLayer { renderEffect = … }`. Blur does not render in Android
 * Studio preview — validate on a physical device or emulator (API 31+).
 */
@Composable
fun LoadingOverlayPage() {
    var isLoading by remember { mutableStateOf(false) }

    // When isLoading flips to true, wait 2 s then auto-hide.
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(2_000L)
            isLoading = false
        }
    }

    // AMLoadingOverlay wraps ComponentPage (the whole page viewport), not just
    // the page body — inside the scaffold's scroll column the host would collapse.
    AMLoadingOverlay(
        isPresented = isLoading,
        message     = "Saving…",
    ) {
        ComponentPage(entry = loadingOverlayEntry) {
            LoadingOverlayPageBody(onShowOverlay = { isLoading = true })
        }
    }
}

// ---------------------------------------------------------------------------
// LoadingOverlayPageBody — inner content (the hostContent passed to the host)
// ---------------------------------------------------------------------------

/**
 * The scrollable demo body — rendered inside [ComponentPage]. The single
 * trigger button matches iOS (`Pages/Overlays.swift`): no caption, never
 * disabled, inset by the lg page margin.
 */
@Composable
private fun LoadingOverlayPageBody(onShowOverlay: () -> Unit) {
    // ── Default group ─────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        AMSecondaryButton(
            label    = "Show 2-second overlay",
            onClick  = onShowOverlay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        )
    }
}
