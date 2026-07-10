package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Overlays-section entries in curated display order (demo spec §3.6):
 * Sheet, Alert, Banner, Toast, Actions Overlay, Loading Overlay.
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val overlaysEntries: List<ComponentEntry> = listOf(
    sheetEntry,
    alertEntry,
    bannerEntry,
    toastEntry,
    actionsOverlayEntry,
    loadingOverlayEntry,
)
