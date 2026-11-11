package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive

/**
 * A paging row with its neighbors peeking in: Android's pager, one page a swipe. It draws no
 * surface of its own; each page is whatever [content] draws, so a card wears `Modifier.amCard()`
 * itself, and a pager of bare photos runs edge to edge with `sideInset = 0.dp`.
 *
 * ```kotlin
 * var page by rememberSaveable { mutableIntStateOf(0) }
 * Column(horizontalAlignment = Alignment.CenterHorizontally) {
 *     AMCarousel(stories, currentIndex = page, onCurrentIndexChange = { page = it }, Modifier.fillMaxWidth().height(186.dp)) { story ->
 *         StoryCard(story, Modifier.fillMaxSize().amCard())
 *     }
 *     AMPageIndicator(count = stories.size, currentIndex = page)
 * }
 * ```
 *
 * - **Pages.** Every page is the carousel's width less [sideInset] at each side, [spacing] apart,
 *   and comes to rest [sideInset] in from the start, so the pages beside it peek in at either side.
 *   Give the carousel a height; a page gets up to that height and the full page width.
 * - **Shadows.** The carousel cuts its pages off only at its own start and end, so a card's shadow
 *   falls below it onto the page. To let the neighbors peek in from the screen's edges, run the
 *   carousel edge to edge and set [sideInset] to where the cards stand.
 * - **The current page.** A swipe or a fling moves one page, and the carousel reports the page
 *   nearest rest through [onCurrentIndexChange] as it goes. Change [currentIndex] yourself (a Next
 *   button) and the carousel scrolls there with Material's motion. An index out of range draws
 *   clamped.
 * - Pair it with [AMPageIndicator], on the page under it, never on a card of its own.
 * - TalkBack pages it with the pager's scroll actions and says where it is, "Page 2 of 4".
 *
 * @param items the pages, in order.
 * @param currentIndex the current page's index.
 * @param onCurrentIndexChange called with the page nearest rest as a swipe or fling moves the
 *   pages. Apply it to [currentIndex].
 * @param modifier the carousel's placement and height.
 * @param sideInset how far a page at rest stands in from the carousel's edges; also what each page
 *   is narrower than the carousel by, at each side: `spacing.lg` by default.
 * @param spacing the gap between pages: `spacing.md` by default.
 * @param content one page, filling it: a card of your own (`Modifier.amCard()`), a photo, anything.
 */
@Composable
public fun <T> AMCarousel(
    items: List<T>,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sideInset: Dp = AMTheme.tokens.spacing.lg,
    spacing: Dp = AMTheme.tokens.spacing.md,
    content: @Composable (item: T) -> Unit,
) {
    val currentItems by rememberUpdatedState(items)
    val index = rememberUpdatedState(clampIndex(currentIndex, items.size))
    val state = rememberPagerState(initialPage = index.value) { currentItems.size }
    CarouselIndexSync(state, index, rememberUpdatedState(onCurrentIndexChange))

    val count = items.size
    val page = state.currentPage
    HorizontalPager(
        state = state,
        modifier = modifier.semantics {
            if (count > 0) stateDescription = "Page ${clampIndex(page, count) + 1} of $count"
        },
        contentPadding = PaddingValues(horizontal = sideInset),
        pageSpacing = spacing,
        flingBehavior = PagerDefaults.flingBehavior(state, pagerSnapDistance = PagerSnapDistance.atMost(1)),
    ) { itemIndex ->
        val item = currentItems.getOrNull(itemIndex) ?: return@HorizontalPager
        content(item)
    }
}

/**
 * Keeps the pager and the caller's index together: a change of [index] scrolls the pager there,
 * and a swipe reports the page it brings to rest. While the pager scrolls to the caller's index it
 * reports nothing, so the pages it passes never pull the index back; a swipe that interrupts it
 * takes over, reporting from where it caught the pager.
 */
@Composable
private fun CarouselIndexSync(state: PagerState, index: State<Int>, onIndexChange: State<(Int) -> Unit>) {
    val scheme = rememberUpdatedState(MaterialTheme.motionScheme)
    val seek = remember { CarouselSeek() }
    LaunchedEffect(state) {
        snapshotFlow { index.value }.collectLatest { target ->
            if (state.currentPage == target) return@collectLatest
            seek.seeking = true
            try {
                state.animateScrollToPage(target, animationSpec = scheme.value.defaultSpatialSpec())
            } catch (interrupted: CancellationException) {
                // A new index, or the carousel leaving, cancels this coroutine: let that through.
                // A swipe cancels only the scroll, and takes over from here.
                if (!currentCoroutineContext().isActive) throw interrupted
            } finally {
                seek.seeking = false
            }
            if (state.currentPage != index.value) onIndexChange.value(state.currentPage)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.currentPage }.collect { page ->
            if (!seek.seeking && page != index.value) onIndexChange.value(page)
        }
    }
}

/** Whether the pager is scrolling to the caller's index, when it reports nothing. */
private class CarouselSeek {
    var seeking = false
}
