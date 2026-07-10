package io.github.jmsdnns.androidmtrls.demo.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One catalog entry — identifies a component page in the demo app.
 *
 * Mirrors iOS `ComponentEntry` (`Catalog/ComponentEntry.swift`). The [content]
 * lambda is a type-erased page factory, equivalent to `makeView: () -> AnyView`.
 *
 * **Navigation:** entries navigate by [id] string, not by lambda. `DemoNavHost`
 * looks up entries in [CatalogRegistry.byId] at the destination and calls
 * [content]. Lambdas cannot be serialized into a NavBackStackEntry argument.
 *
 * @param id          Stable, globally unique string used as the navigation route key.
 *                    Use kebab-case: "foundations-theme", "buttons-fab".
 * @param section     Which top-level catalog section this entry belongs to.
 * @param title       Page title shown in the scaffold top bar and the detail page header.
 * @param tileTitle   Short label shown on the home-grid tile (defaults to [title] if null).
 * @param subtitle    Optional one-line description shown under the tile title on the home grid.
 * @param intro       Optional longer introductory paragraph shown at the top of the component page.
 * @param icon        Optional tile/page icon — Material [ImageVector] replacing iOS SF Symbol.
 * @param showsTitle  When false, [ComponentPage] suppresses the page header. Use for pages
 *                    where every header IS a specimen (e.g., Typography). Mirrors iOS
 *                    `ComponentPage(entry:showsTitle:)` parameter.
 * @param ownsScaffold When true, `DemoNavHost` renders [content] raw (top bar still shown)
 *                    and the page composable calls [ComponentPage] itself. Required when a
 *                    full-coverage overlay host (e.g. `AMActionsOverlay`) must wrap the
 *                    scaffold: inside the scaffold's scroll column height constraints are
 *                    unbounded, so the host would collapse to wrap-content. Mirrors iOS,
 *                    where pages own their `ComponentPage` call and apply overlay
 *                    modifiers outside it.
 * @param content     Composable factory for the page content. Called inside [ComponentPage]
 *                    unless [ownsScaffold] is set.
 */
@Stable
data class ComponentEntry(
    val id: String,
    val section: ComponentSection,
    val title: String,
    val tileTitle: String? = null,
    val subtitle: String? = null,
    val intro: String? = null,
    val icon: ImageVector? = null,
    val showsTitle: Boolean = true,
    val ownsScaffold: Boolean = false,
    val content: @Composable () -> Unit,
) {
    /** Short label to render on the tile; falls back to [title] when unset. */
    val effectiveTileTitle: String get() = tileTitle ?: title
}
