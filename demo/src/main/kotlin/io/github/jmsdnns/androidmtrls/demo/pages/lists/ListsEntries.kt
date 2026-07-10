package io.github.jmsdnns.androidmtrls.demo.pages.lists

import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry

/**
 * All Lists-section entries in curated display order: List Row, Reorderable Row
 * List, List Card, Reorderable Card List, Swipe Actions (demo spec §3.7 tile
 * order — 2×2 root grid — with Swipe Actions appended last).
 *
 * Each component page lives in its own file in this package and exposes a
 * single `val <component>Entry`; this list is the only aggregation point.
 */
val listsEntries: List<ComponentEntry> = listOf(
    listRowEntry,
    reorderableRowListEntry,
    listCardEntry,
    reorderableCardListEntry,
    swipeActionsEntry,
)
