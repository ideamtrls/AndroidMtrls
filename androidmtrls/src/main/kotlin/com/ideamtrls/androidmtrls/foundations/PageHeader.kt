package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A screen's title: Material's top app bar with the page title on one row, the navigation icon
 * before it and the actions after it, transparent at rest so it lies on the page, and the page
 * color once content scrolls under it, or over a picture the frosted fill.
 *
 * ```kotlin
 * val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
 * Scaffold(
 *     modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
 *     topBar = { AMPageHeader("Library", navigationIcon = { BackButton() }, scrollBehavior = scrollBehavior) },
 *     containerColor = AMTheme.Surface.page,
 * ) { … }
 * ```
 *
 * The title is the page-title role, `headlineMedium`, written as given in sentence case, on one
 * line. The bar is 72 dp tall below its insets, room for that line, and stays put as content scrolls:
 * give it a pinned [scrollBehavior] so it knows when content is under it. Without one it stays
 * transparent. Over pull to refresh, hand the scroll behavior to the [AMPullToRefreshBox] in place
 * of the `Scaffold`, so the pull never counts as content under the bar.
 *
 * **Over a picture** (inside an [AMBackdrop], on a transparent screen) the bar lies on the art at
 * rest, and once content scrolls under it fades in the frosted fill instead of the page color: the
 * art under the bar blurred under [AMTheme.Frosted.fill], with the frosted edge along its bottom,
 * reaching behind the status bar (the solid surface until the art is placed, never unblurred
 * translucency). On art (an [AMBackdrop], an [AMArtRowsBackdrop]) the title is the ink of the
 * art's tone until content scrolls under the bar, then the bar's own color: on the library's art,
 * washed toward the mode, they're the same `onSurface`, so it reads throughout; on art given the
 * other tone (white text on dark art in light mode) it fades over as the fill comes in.
 *
 * @param title the screen's title, one line.
 * @param modifier the bar's placement.
 * @param navigationIcon the leading icon button, usually back (`arrow_back`). Null shows none.
 * @param actions the trailing icon buttons. Null shows none.
 * @param scrollBehavior tells the bar when content has scrolled under it, so it takes the page
 *   color (the frosted fill over a picture): usually `TopAppBarDefaults.pinnedScrollBehavior()`,
 *   its `nestedScrollConnection` on the screen. Null stays transparent.
 * @param windowInsets the insets the bar keeps clear of: the status bar by default. The bar's
 *   color reaches behind them.
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
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A screen's title with custom title content: Material's top app bar with the title on one row,
 * transparent at rest and the page color once content scrolls under it (the frosted fill inside
 * an [AMBackdrop], as the string form describes). The bar gives [title] the page-title type and
 * color, so custom content (a title with a count beside it) stays on-system; keep it to one line.
 *
 * @param modifier the bar's placement.
 * @param navigationIcon the leading icon button, usually back (`arrow_back`). Null shows none.
 * @param actions the trailing icon buttons. Null shows none.
 * @param scrollBehavior tells the bar when content has scrolled under it: usually
 *   `TopAppBarDefaults.pinnedScrollBehavior()`. Null stays transparent.
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
    // Over art, content scrolling under the bar brings in the frosted fill, never a page band.
    val frosted = LocalAMBackdrop.current != null && scrollBehavior != null
    // On art the title is the art tone's ink until content scrolls under the bar.
    val scrolled by barScrolledState(scrollBehavior)
    val titleColor = barLineColor(scrolled, MaterialTheme.colorScheme.onSurface)
    val bar = @Composable { barModifier: Modifier ->
        TopAppBar(
            title = { ProvideTextStyle(MaterialTheme.typography.headlineMedium) { title() } },
            modifier = barModifier,
            navigationIcon = navigationIcon ?: {},
            actions = actions ?: {},
            expandedHeight = PageHeaderHeight,
            windowInsets = windowInsets,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = if (frosted) Color.Transparent else AMTheme.Surface.page,
                titleContentColor = titleColor,
            ),
            scrollBehavior = scrollBehavior,
        )
    }
    if (frosted) {
        val fill by barFill(scrollBehavior)
        FrostedBar(fill = { fill }, covers = scrolled, modifier) { bar(Modifier) }
    } else {
        bar(modifier)
    }
}

/**
 * The page header's height below its insets: the `headlineMedium` title's line with room around
 * it, so the title, the 48 dp navigation icon and the actions share one row.
 */
internal val PageHeaderHeight = 72.dp
