package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Pull to refresh around a screen's scrolling content, Material's: its pull state and its
 * indicator, which comes down from the top of the box. Under an [AMPageHeader], it works with the
 * header: pass the header's [scrollBehavior] here, and leave its connection off the `Scaffold`.
 *
 * ```kotlin
 * val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
 * Scaffold(
 *     topBar = { AMPageHeader("Feed", actions = { … }, scrollBehavior = scrollBehavior) },
 *     containerColor = AMTheme.Surface.page,
 * ) { padding ->
 *     AMPullToRefreshBox(
 *         isRefreshing = refreshing,
 *         onRefresh = ::refresh,
 *         modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
 *         scrollBehavior = scrollBehavior,
 *     ) {
 *         LazyColumn(Modifier.fillMaxSize()) { … }
 *     }
 * }
 * ```
 *
 * The box owns the header's connection and puts it inside the pull, nearer the content, so the
 * header hears only the content's own scrolling: a downward drag scrolls the content to its top,
 * then pulls; an upward drag puts a pulled indicator away, then scrolls the content, and the
 * header takes its fill once content is under it. Padded below the header, as above, the
 * indicator comes out from under it. The header's connection belongs in one place: on the
 * `Scaffold` as well, it would count the pull as content scrolled away from under the header, and
 * the header would stay clear over content after the pull.
 *
 * [isRefreshing] is the caller's: a release past the threshold calls [onRefresh], and the
 * indicator spins while [isRefreshing] is true and leaves when it turns false.
 *
 * @param isRefreshing whether a refresh is running; the indicator spins while it is.
 * @param onRefresh called when a pull is released past the threshold. Start the refresh and set
 *   [isRefreshing].
 * @param modifier the box's placement: usually the screen's content area, padded below its header.
 * @param scrollBehavior the scroll behavior of the [AMPageHeader] above the content, whose
 *   connection the box installs, or null for a screen without one.
 * @param state the pull's state, Material's, for reading the pull's progress.
 * @param enabled whether a pull can start; while false, drags only scroll.
 * @param content the scrolling content.
 */
@Composable
public fun AMPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    state: PullToRefreshState = rememberPullToRefreshState(),
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val pull = modifier.pullToRefresh(
        isRefreshing = isRefreshing,
        state = state,
        enabled = resolveEnabled(enabled),
        onRefresh = onRefresh,
    )
    // Later in the chain is nearer the content: the content's leftover drag reaches the header
    // before the pull takes it, and a drag's first share reaches the pull first.
    Box(if (scrollBehavior != null) pull.nestedScroll(scrollBehavior.nestedScrollConnection) else pull) {
        content()
        PullToRefreshDefaults.Indicator(
            state = state,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
