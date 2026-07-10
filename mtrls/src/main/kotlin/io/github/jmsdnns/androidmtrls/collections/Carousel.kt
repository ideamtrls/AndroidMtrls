package io.github.jmsdnns.androidmtrls.collections

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// AMCarousel — horizontal pager with peek-of-neighbors
// ---------------------------------------------------------------------------

/**
 * Horizontal pager with peek-of-neighbors.
 *
 * Each item fills the container minus a symmetric [sideInset] on both edges,
 * leaving the previous and next cards peeking in as edge hints. The user drags
 * horizontally; native snap physics settles onto the next/previous page once
 * the drag crosses the halfway point or carries enough fling velocity, and
 * springs back to the current page when released early.
 *
 * **Visual mechanics only.** The caller owns the items list and the page
 * index. Pair with [AMPageIndicator] using the same `currentIndex`:
 *
 * ```kotlin
 * var page by rememberSaveable { mutableIntStateOf(0) }
 *
 * AMCarousel(
 *     items = stories,
 *     key = { it.id },
 *     currentIndex = page,
 *     onIndexChange = { page = it },
 *     modifier = Modifier.height(200.dp),
 * ) { story ->
 *     StoryCardView(story)
 * }
 * AMPageIndicator(count = stories.size, currentIndex = page)
 * ```
 *
 * **Pager-backed internals:**
 *
 * Paging is delegated to Compose Foundation's [HorizontalPager] so the gesture,
 * fling, and snap physics are the platform-native ones. The peek geometry comes
 * from `contentPadding = PaddingValues(horizontal = sideInset)` — each page
 * fills the container minus twice the inset, and the adjacent pages show through
 * the padding — with `pageSpacing = spacing` for the inter-item gap.
 *
 * The page index is a two-way binding kept in lockstep with the pager, each
 * direction guarded by an equality check so they can't feed back into a loop:
 *
 * - **Pager → caller.** A `snapshotFlow` on `pagerState.currentPage` reports the
 *   settled page through [onIndexChange] whenever it diverges from the current
 *   clamped index. `currentPage` updates mid-gesture the moment the drag crosses
 *   the halfway point, so [onIndexChange] can fire before the finger lifts.
 * - **Caller → pager.** A controlled change to [currentIndex] scrolls the pager
 *   to match (skipped while a drag is in flight, so a user gesture is never
 *   fought). The programmatic move animates normally, or snaps instantly under
 *   the house Reduce Motion policy (`LocalAMReduceMotion`).
 *
 * iOS equivalent: `IMCarousel` in `Sources/Collections/Carousel.swift`.
 *
 * @param items         Data items to display. Empty list renders nothing.
 * @param key           Stable identity per item; used as the pager page `key`.
 *                      Equivalent to iOS `id: KeyPath<Item, ID>`.
 * @param currentIndex  Currently visible page. Clamped to `[0, items.lastIndex]`
 *                      internally — briefly out-of-range state is safe.
 * @param onIndexChange Called when the pager's current page changes. Because the
 *                      pager reports the page the instant a drag crosses the
 *                      halfway point, this can fire mid-gesture rather than only
 *                      at release. Callers must update their own state.
 * @param sideInset     Symmetric horizontal inset. `null` → `tokens.spacing.lg`
 *                      (16dp). Same as iOS `sideInset: CGFloat? = nil`.
 * @param spacing       Gap between adjacent items. `null` → `tokens.spacing.md`
 *                      (12dp). Same as iOS `spacing: CGFloat? = nil`.
 * @param modifier      Applied to the [HorizontalPager].
 * @param content       Composable slot; invoked once per page. The pager
 *                      constrains each page to the peek width — callers should
 *                      not set a `width` modifier on the root of this slot.
 */
@Composable
fun <T> AMCarousel(
    items: List<T>,
    key: (T) -> Any,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    sideInset: Dp? = null,
    spacing: Dp? = null,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    // Empty list: nothing to render — early return is safe.
    if (items.isEmpty()) return

    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Resolve defaults from house tokens.
    val effectiveSideInset: Dp = sideInset ?: tokens.spacing.lg
    val effectiveSpacing: Dp = spacing ?: tokens.spacing.md

    // Defensively clamp — list may briefly shrink during recomposition.
    val clampedIndex = currentIndex.coerceIn(0, items.lastIndex)

    val pagerState = rememberPagerState(initialPage = clampedIndex) { items.size }

    // Two-way index sync (precedent: SwipeActions.kt). rememberUpdatedState keeps
    // the long-lived collector reading the latest values; the equality guards on
    // both sides make redundant emissions no-ops so the directions can't loop.
    val currentClampedIndex by rememberUpdatedState(clampedIndex)
    val reportIndexChange by rememberUpdatedState(onIndexChange)

    // pager → caller: report the pager's current page when it settles somewhere
    // other than the controlled index. `currentPage` flips mid-gesture once the
    // drag passes the halfway point, so this can fire before the finger lifts.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page != currentClampedIndex) reportIndexChange(page)
        }
    }

    // caller → pager: a controlled index change scrolls the pager to match.
    // Skipped while a drag is in flight so a user gesture is never fought.
    // Programmatic jumps respect the house Reduce Motion policy (instant snap).
    LaunchedEffect(clampedIndex) {
        if (clampedIndex != pagerState.currentPage && !pagerState.isScrollInProgress) {
            if (reduceMotion) {
                pagerState.scrollToPage(clampedIndex)
            } else {
                pagerState.animateScrollToPage(clampedIndex)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        // Symmetric inset padding reproduces the peek-of-neighbors geometry:
        // page width = container − 2·sideInset, with the adjacent pages showing
        // through the padding on either edge.
        contentPadding = PaddingValues(horizontal = effectiveSideInset),
        pageSpacing = effectiveSpacing,
        // The old hand-rolled Row aligned children to the top; the pager defaults
        // to CenterVertically, which would shift short content — Top preserves the
        // established look.
        verticalAlignment = Alignment.Top,
        key = { index -> key(items[index]) },
        modifier = modifier
            .fillMaxWidth()
            // TalkBack can't perform the drag, so expose the pager as an
            // adjustable element: a "Page X of N" state plus Next/Previous page
            // actions that write `currentIndex` through the same path an external
            // "next"/"back" button uses — mirrors iOS `IMCarousel`'s
            // accessibilityValue + accessibilityAdjustableAction. Reuses
            // AMPageIndicator's "Page X of N" phrasing.
            .semantics(mergeDescendants = true) {
                stateDescription = "Page ${clampedIndex + 1} of ${items.size}"
                customActions = listOf(
                    CustomAccessibilityAction(label = "Next page") {
                        val target = (clampedIndex + 1).coerceIn(0, items.lastIndex)
                        if (target != clampedIndex) {
                            onIndexChange(target)
                            true
                        } else {
                            false
                        }
                    },
                    CustomAccessibilityAction(label = "Previous page") {
                        val target = (clampedIndex - 1).coerceIn(0, items.lastIndex)
                        if (target != clampedIndex) {
                            onIndexChange(target)
                            true
                        } else {
                            false
                        }
                    },
                )
            },
    ) { page ->
        // The pager constrains each page to the peek width, so the content slot
        // renders directly with no width-constraint wrapper.
        content(items[page])
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

private data class CarouselPreviewStory(
    val id: Int,
    val title: String,
    val body: String,
    val color: Color,
)

@Composable
private fun CarouselPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    val stories = listOf(
        CarouselPreviewStory(1, "Morning Light", "A walk through the harbor", Color(0xFF4A90D9)),
        CarouselPreviewStory(2, "City of Doors", "Photo essay · 12 images", Color(0xFF7B68EE)),
        CarouselPreviewStory(3, "Field Notes", "Dispatches from the trail", Color(0xFF50C878)),
    )
    var page by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(vertical = tokens.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AMCarousel(
            items = stories,
            key = { it.id },
            currentIndex = page,
            onIndexChange = { page = it },
            modifier = Modifier.height(200.dp),
        ) { story ->
            val shape = amSquircle(tokens.radius.lg)
            Box(
                modifier = Modifier
                    .amElevation(level = AMElevation.Low, shape = shape)
                    .background(story.color, shape = shape)
                    .amCardHairline(shape)
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(tokens.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                ) {
                    BasicText(
                        text = story.title,
                        style = AMTheme.Type.title.toTextStyle(Color.White),
                    )
                    BasicText(
                        text = story.body,
                        style = AMTheme.Type.secondary.toTextStyle(
                            Color.White.copy(alpha = 0.8f),
                        ),
                    )
                }
            }
        }

        AMPageIndicator(
            count = stories.size,
            currentIndex = page,
        )
    }
}

@Preview(
    name = "AMCarousel · Light",
    group = "AMCarousel",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
    widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMCarouselPreviewLight() {
    AMThemeRoot {
        CarouselPreviewContent()
    }
}

@Preview(
    name = "AMCarousel · Dark",
    group = "AMCarousel",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    widthDp = 390,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMCarouselPreviewDark() {
    AMThemeRoot {
        CarouselPreviewContent()
    }
}
