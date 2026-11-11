package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A screen's title: Material's large top app bar, transparent at rest so it lies on the page, and
 * the page color once content scrolls under it, or over a picture the frosted fill.
 *
 * ```kotlin
 * val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
 * Scaffold(
 *     modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
 *     topBar = { AMPageHeader("Library", navigationIcon = { BackButton() }, scrollBehavior = scrollBehavior) },
 *     containerColor = AMTheme.Surface.page,
 * ) { … }
 * ```
 *
 * The title is the page-title role, `headlineMedium` expanded, written as given in sentence case.
 * With a [scrollBehavior] it collapses as the content scrolls, Material's way; without one it
 * stays expanded.
 *
 * **Over a picture** (inside an [AMBackdrop], on a transparent screen) the bar lies on the art at
 * rest, and as it collapses it fades in the frosted fill instead of the page color, at the rate
 * Material fades its color: the art under the bar blurred under [AMTheme.Frosted.fill], with the
 * frosted edge along its bottom, reaching behind the status bar (the solid surface until the art
 * is placed, never unblurred translucency). The title keeps the bar's own color, which reads on
 * the frosted fill; a screen over dark art gives its title the art text look itself, through the
 * slot form, while the bar is expanded.
 *
 * The bar's top row holds the navigation icon, the actions and, once the bar collapses, the small
 * title. A header with none of them (no icon, no actions, no [scrollBehavior]) would keep that row
 * empty for good, so it shows the large title's row alone: a title strip at Material's expanded
 * title metrics.
 *
 * @param title the screen's title.
 * @param modifier the bar's placement.
 * @param navigationIcon the leading icon button, usually back (`arrow_back`). Null shows none.
 * @param actions the trailing icon buttons. Null shows none.
 * @param scrollBehavior how the bar collapses as content scrolls, or null to stay expanded.
 * @param windowInsets the insets the bar keeps clear of: the status bar by default.
 */
@Composable
public fun AMPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
) {
    AMPageHeader(
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        windowInsets = windowInsets,
    ) {
        Text(title)
    }
}

/**
 * A screen's title with custom title content: Material's large top app bar, transparent at rest
 * and the page color once content scrolls under it (the frosted fill inside an [AMBackdrop], as
 * the string form describes). The bar gives [title] the page-title type and color, so custom
 * content (a title with a count beside it) stays on-system.
 *
 * @param modifier the bar's placement.
 * @param navigationIcon the leading icon button, usually back (`arrow_back`). Null shows none.
 * @param actions the trailing icon buttons. Null shows none.
 * @param scrollBehavior how the bar collapses as content scrolls, or null to stay expanded.
 * @param windowInsets the insets the bar keeps clear of: the status bar by default.
 * @param title the title content.
 */
@Composable
public fun AMPageHeader(
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    title: @Composable () -> Unit,
) {
    // The top row only earns its height when something can sit in it.
    val titleOnly = navigationIcon == null && actions == null && scrollBehavior == null
    val collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight
    val expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight
    // Over art, the bar collapsing brings in the frosted fill, never a page band.
    val frosted = LocalAMBackdrop.current != null && scrollBehavior != null
    val bar = @Composable { barModifier: Modifier ->
        LargeTopAppBar(
            title = title,
            modifier = barModifier,
            navigationIcon = navigationIcon ?: {},
            actions = actions ?: {},
            collapsedHeight = if (titleOnly) 0.dp else collapsedHeight,
            expandedHeight = if (titleOnly) expandedHeight - collapsedHeight else expandedHeight,
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = if (frosted) Color.Transparent else AMTheme.Surface.page,
            ),
            scrollBehavior = scrollBehavior,
        )
    }
    if (frosted) {
        FrostedBar(fill = { twoRowBarFill(scrollBehavior) }, modifier) { bar(Modifier) }
    } else {
        bar(modifier)
    }
}
