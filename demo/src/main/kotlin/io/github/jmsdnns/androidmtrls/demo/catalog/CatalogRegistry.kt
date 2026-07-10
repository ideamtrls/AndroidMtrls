package io.github.jmsdnns.androidmtrls.demo.catalog

import io.github.jmsdnns.androidmtrls.demo.pages.buttons.buttonsEntries
import io.github.jmsdnns.androidmtrls.demo.pages.collections.collectionsEntries
import io.github.jmsdnns.androidmtrls.demo.pages.content.contentEntries
import io.github.jmsdnns.androidmtrls.demo.pages.frames.framesEntries
import io.github.jmsdnns.androidmtrls.demo.pages.lists.listsEntries
import io.github.jmsdnns.androidmtrls.demo.pages.overlays.overlaysEntries
import io.github.jmsdnns.androidmtrls.demo.pages.pickers.pickersEntries
import io.github.jmsdnns.androidmtrls.demo.pages.textinputs.textInputsEntries
import io.github.jmsdnns.androidmtrls.demo.pages.foundationsEntries
import io.github.jmsdnns.androidmtrls.demo.scenes.scenesEntries

/**
 * Global catalog registry — the single source of truth for all [ComponentEntry] objects.
 *
 * Mirrors iOS `CatalogRegistry` (`Catalog/ComponentEntry.swift`). The [all] list
 * concatenates per-section entry arrays in the curated [ComponentSection.allCases] order.
 *
 * **How to add entries (Task 2.3+):**
 * 1. Create (or edit) the page file under `pages/` that owns the section.
 * 2. Add a `val <section>Entries: List<ComponentEntry>` top-level val in that file.
 * 3. Append it to the [all] concatenation below — one line per section.
 *
 * Navigation looks entries up by [ComponentEntry.id] via [byId]. Entries never
 * travel through NavBackStackEntry arguments (lambdas cannot be serialized).
 */
object CatalogRegistry {

    /**
     * All registered entries in curated display order (screens → … → collections).
     *
     * Each per-section list is appended here. Later phases add their list to this
     * concatenation — that is the only change required to register a new section.
     */
    val all: List<ComponentEntry> by lazy {
        // Scenes lead the catalog (iOS section order). Empty until the scenes
        // land in later tasks, at which point the home Scenes band renders.
        scenesEntries +
        foundationsEntries +
        textInputsEntries +
        buttonsEntries +
        contentEntries +
        pickersEntries +
        framesEntries +
        overlaysEntries +
        listsEntries +
        collectionsEntries
    }

    /**
     * Fast lookup map from [ComponentEntry.id] to entry.
     *
     * Used by `ComponentEntryScreen` to resolve the composable page factory
     * after navigating to route `"page/{id}"`.
     */
    val byId: Map<String, ComponentEntry> by lazy {
        all.associateBy { it.id }
    }
}
