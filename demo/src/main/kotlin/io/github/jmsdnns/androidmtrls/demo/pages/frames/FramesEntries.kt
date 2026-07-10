package io.github.jmsdnns.androidmtrls.demo.pages.frames

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Frames-section entries in curated display order (mirrors the iOS
 * catalog's Frames ordering / demo spec §3.5: Card, Section, Toolbar,
 * Panel, Disclosure).
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val framesEntries: List<ComponentEntry> = listOf(
    cardEntry,
    sectionEntry,
    toolbarEntry,
    panelEntry,
    disclosureEntry,
)
