package io.github.jmsdnns.androidmtrls.demo.catalog

/**
 * The ten top-level sections of the component catalog.
 *
 * Mirrors iOS `ComponentSection` (`Catalog/ComponentSection.swift`) — same 10 cases,
 * same curated display order returned by [allCases].
 *
 * Section header labels: [Screens] renders as "Scenes" in the UI (same special-case as iOS).
 */
enum class ComponentSection {
    /** Full-screen composed demo experiences. Displayed as "Scenes" in the catalog header. */
    Screens,

    /** Core design tokens — colors, typography, spacing, elevation. */
    Foundations,

    /** Single-line and multi-line text input primitives. */
    TextInputs,

    /** All button variants: primary, secondary, ghost, icon, card, action-grid, FAB. */
    Buttons,

    /** Read-only display components: avatars, tags, badges, skeletons, etc. */
    Content,

    /** Value-selection controls: toggles, sliders, segmented controls, date pickers. */
    Pickers,

    /** Surface containers: cards, sections, toolbars, panels, disclosure groups. */
    Frames,

    /** Modal overlays: sheets, alerts, banners, toasts, action grids, loading overlays. */
    Overlays,

    /** List row and card primitives (plain and reorderable). */
    Lists,

    /** Multi-item layout components: carousels, card stacks, chip groups, tabs. */
    Collections;

    /**
     * The display label shown in the catalog home list and section headers.
     *
     * [Screens] renders as "Scenes" to match the iOS special-case.
     */
    val displayName: String
        get() = when (this) {
            Screens     -> "Scenes"
            Foundations -> "Foundations"
            TextInputs  -> "Text Inputs"
            Buttons     -> "Buttons"
            Content     -> "Content"
            Pickers     -> "Pickers"
            Frames      -> "Frames"
            Overlays    -> "Overlays"
            Lists       -> "Lists"
            Collections -> "Collections"
        }

    companion object {
        /**
         * All sections in the curated catalog display order.
         *
         * Mirrors iOS `ComponentSection.allCases`:
         * screens → foundations → textInputs → buttons → content →
         * pickers → frames → overlays → lists → collections
         */
        val allCases: List<ComponentSection> = listOf(
            Screens,
            Foundations,
            TextInputs,
            Buttons,
            Content,
            Pickers,
            Frames,
            Overlays,
            Lists,
            Collections,
        )
    }
}
