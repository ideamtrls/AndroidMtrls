package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes

/**
 * The catalog's top-level grouping. Declaration order is list order: the ten component
 * categories as a UI expert expects to find them, then the Showcase scenes last.
 */
internal enum class CatalogCategory(
    /** The subheader over the category's rows, in sentence case. */
    val title: String,
    @param:DrawableRes val icon: Int,
) {
    Foundations("Foundations", R.drawable.ic_palette),
    Actions("Actions", R.drawable.ic_touch_app),
    Inputs("Inputs", R.drawable.ic_keyboard),
    Selection("Selection", R.drawable.ic_checklist),
    Navigation("Navigation", R.drawable.ic_side_navigation),
    Layout("Layout", R.drawable.ic_dashboard),
    Lists("Lists", R.drawable.ic_list),
    Feedback("Feedback", R.drawable.ic_notifications),
    Overlays("Overlays", R.drawable.ic_layers),
    DataDisplay("Data display", R.drawable.ic_sell),
    Showcase("Showcase", R.drawable.ic_auto_awesome);

    /**
     * This category's entries in list order: the `entries` of its own `<Category>Pages`, owned by
     * its folder under `catalog/pages/`.
     */
    val componentEntries: List<ComponentEntry>
        get() = when (this) {
            Foundations -> FoundationsPages.entries
            Actions -> ActionsPages.entries
            Inputs -> InputsPages.entries
            Selection -> SelectionPages.entries
            Navigation -> NavigationPages.entries
            Layout -> LayoutPages.entries
            Lists -> ListsPages.entries
            Feedback -> FeedbackPages.entries
            Overlays -> OverlaysPages.entries
            DataDisplay -> DataDisplayPages.entries
            Showcase -> ShowcasePages.entries
        }
}
