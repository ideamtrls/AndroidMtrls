package io.github.jmsdnns.androidmtrls.demo.pages.collections

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Collections-section entries in curated display order (demo spec §3.9):
 * Card Stack, Carousel, Chip Group, Page Indicator, Tabs.
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val collectionsEntries: List<ComponentEntry> = listOf(
    cardStackEntry,
    carouselEntry,
    chipGroupEntry,
    pageIndicatorEntry,
    tabsEntry,
)
