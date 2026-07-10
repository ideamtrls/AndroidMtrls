package io.github.jmsdnns.androidmtrls.collections

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.resolved

// ---------------------------------------------------------------------------
// Private constants
// ---------------------------------------------------------------------------

/** Diameter of an inactive dot (also the strip height — never changes). */
private val dotSize: Dp = 6.dp

/** Width the active dot elongates to (3× dotSize). */
private val activeDotWidth: Dp = 18.dp

/**
 * Opacity applied to [AMTheme.Text.secondary] for inactive dots.
 *
 * Pushes inactive dots into quiet background texture so the active
 * capsule carries the state. Matches `IMPageIndicator.inactiveDotOpacity`.
 */
private const val inactiveDotOpacity: Float = 0.35f

/**
 * Dp-typed spring equivalent to [AMTheme.Motion.modalSpring].
 *
 * [AMTheme.Motion.modalSpring] is declared as `spring<Float>` (the house
 * token type). [animateDpAsState] requires `AnimationSpec<Dp>` — a different
 * type parameter that cannot be assigned from the Float token directly.
 * This private val reproduces the identical spring parameters
 * (dampingRatio = 0.86, stiffness = 224) as a `spring<Dp>` so the active dot
 * elongates on the house modal-spring character — the gentle, well-settled
 * feel used for page-scale motion, deliberately slower than the snappier
 * `snapSpring`. Chosen to sit comfortably beside a paired [AMCarousel], whose
 * HorizontalPager settles on its own native snap physics; there is no shared
 * spring the two lock to. If the token values are ever tuned, this val must be
 * updated in lockstep.
 */
private val modalSpringDp = spring<Dp>(dampingRatio = 0.86f, stiffness = 224f)

/** Capsule shape shared across all dots — allocated once at file level. */
private val dotShape = RoundedCornerShape(50)

// ---------------------------------------------------------------------------
// AMPageIndicator
// ---------------------------------------------------------------------------

/**
 * Dot-strip indicator for paginated content.
 *
 * Pairs with [AMCarousel] (or any other current-index-driven primitive) to
 * reflect which page of N is currently visible. The active dot elongates to
 * an 18×6dp capsule in [AMTheme.Text.primary]; inactive dots stay 6×6dp
 * circles in a muted gray at 35% opacity.
 *
 * **Active fill is always [AMTheme.Text.primary] — never the accent/tint
 * color.** This is hardcoded in the iOS source (`IMPageIndicator`) and must
 * remain hardcoded here. Callers that want accent-colored dots must build
 * their own indicator.
 *
 * Visual-only — read-only, no click handling. Callers own the index state
 * and pass it down.
 *
 * The width animation rides [AMCarousel]'s own `modalSpring` paging curve
 * (response=0.42s, dampingFraction=0.86), so paging the carousel and the dot
 * strip settle in genuine lockstep — the dots move *with* the page, never
 * ahead of it (the iOS-faithful pairing). The width spring is
 * reduce-motion-aware via `tokens.motion.resolved` (`tokens/Motion.kt`):
 * under Reduce Motion the elongation softens to the overlay-fade tween,
 * matching [AMCarousel]'s reduce-motion page settle.
 *
 * ```kotlin
 * AMCarousel(items = stories, key = { it.id }, currentIndex = page, onIndexChange = { page = it }) { … }
 * AMPageIndicator(count = stories.size, currentIndex = page)
 * ```
 *
 * @param count        Total number of pages. A count of 0 renders an empty strip.
 * @param currentIndex Current active page index. Clamped to `0..<count` internally
 *                     so a briefly out-of-range value never breaks the layout.
 * @param modifier     Modifier applied to the root [Row].
 */
@Composable
fun AMPageIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val clamped = currentIndex.coerceIn(0, maxOf(0, count - 1))
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    val activeFill = AMTheme.Text.primary.resolve(isDark)
    val inactiveFill = AMTheme.Text.secondary.resolve(isDark).copy(alpha = inactiveDotOpacity)

    // Dot-width spring routed through the house Reduce-Motion policy: under RM the
    // capsule elongation softens to the overlay-fade tween instead of the
    // modalSpring (tokens/Motion.kt). Read once — the same spec drives every dot.
    val dotWidthSpec = tokens.motion.resolved(modalSpringDp, reduceMotion)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Page ${clamped + 1} of ${maxOf(count, 1)}"
        },
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
    ) {
        repeat(count) { index ->
            val dotWidth by animateDpAsState(
                targetValue = if (index == clamped) activeDotWidth else dotSize,
                animationSpec = dotWidthSpec,
                label = "AMPageIndicator.dot.$index",
            )

            val fill = if (index == clamped) activeFill else inactiveFill

            Box(
                modifier = Modifier
                    .width(dotWidth)
                    .height(dotSize)
                    .clip(dotShape)
                    .background(fill),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(
    name = "AMPageIndicator · Light",
    group = "AMPageIndicator",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMPageIndicatorPreviewLight() {
    AMThemeRoot {
        AMPageIndicatorPreviewContent()
    }
}

@Preview(
    name = "AMPageIndicator · Dark",
    group = "AMPageIndicator",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMPageIndicatorPreviewDark() {
    AMThemeRoot {
        AMPageIndicatorPreviewContent()
    }
}

@Composable
private fun AMPageIndicatorPreviewContent() {
    Column(
        modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
    ) {
        // 3-dot — first active
        AMPageIndicator(count = 3, currentIndex = 0)
        // 5-dot — middle active
        AMPageIndicator(count = 5, currentIndex = 2)
        // 8-dot — last active
        AMPageIndicator(count = 8, currentIndex = 7)
    }
}
