package com.ideamtrls.androidmtrls

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * Dots showing which page of paged content is current: one dot per page, the current one
 * stretched into a short bar. It only shows the index it's given; paging is the pager's.
 *
 * ```kotlin
 * val pagerState = rememberPagerState { pages.size }
 * HorizontalPager(pagerState) { page -> Slide(pages[page]) }
 * AMPageIndicator(count = pages.size, currentIndex = pagerState.currentPage)
 * ```
 *
 * Dots are 6 dp, `spacing.sm` apart; the current one is 18 dp wide in `onSurface`, the others
 * circles in `outlineVariant`, so the strip's height never changes. A change of page stretches
 * the new dot and shrinks the old with Material's motion scheme. [currentIndex] is clamped into
 * the pages, so an index briefly out of range still marks a dot. TalkBack reads the strip as one
 * element, "Page 3 of 5".
 *
 * It lies on the page, or on the card the pager is in, never on a card of its own.
 *
 * @param count how many pages there are.
 * @param currentIndex the current page, from 0.
 * @param modifier the strip's placement.
 */
@Composable
public fun AMPageIndicator(count: Int, currentIndex: Int, modifier: Modifier = Modifier) {
    val current = currentIndex.coerceIn(0, (count - 1).coerceAtLeast(0))
    val pages = count.coerceAtLeast(1)
    Row(
        modifier.clearAndSetSemantics { contentDescription = "Page ${current + 1} of $pages" },
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index -> PageDot(current = index == current) }
    }
}

/** A dot's height, and an inactive dot's width. */
private val DotSize = 6.dp

/** The current dot's width: three dots long, so it reads as current without becoming a bar. */
private val CurrentDotWidth = 18.dp

/** One dot: stretched and in `onSurface` while it's the current page. */
@Composable
private fun PageDot(current: Boolean) {
    val width by animateDpAsState(
        targetValue = if (current) CurrentDotWidth else DotSize,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "AMPageIndicator width",
    )
    val color by animateColorAsState(
        targetValue = if (current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "AMPageIndicator color",
    )
    Box(Modifier.size(width, DotSize).background(color, CircleShape))
}
