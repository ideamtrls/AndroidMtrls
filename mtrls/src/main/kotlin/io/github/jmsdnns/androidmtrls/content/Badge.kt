package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// AMBadgeContent — sealed content model
// ---------------------------------------------------------------------------

/**
 * The two badge flavors: a bare attention dot, or a numeric count capsule.
 *
 * Port of `IMBadge.Content` from iOSMtrls.
 *
 * - [Dot]: 8dp unread/attention pip with no text.
 * - [Count]: Count capsule; zero hides the badge entirely; values over 99
 *   render "99+". An `@Immutable` data carrier so it can be passed to
 *   `AMIconButton` in Phase 5 without triggering unwanted recomposition.
 */
@Immutable
sealed class AMBadgeContent {
    /** 8dp unread/attention pip with no text. */
    object Dot : AMBadgeContent()

    /**
     * Count capsule. Zero hides the badge; values over 99 render "99+".
     *
     * @param value The integer count to display. Must be ≥ 0; negative values
     *              are treated as zero (hidden).
     */
    @Immutable
    data class Count(val value: Int) : AMBadgeContent()
}

// ---------------------------------------------------------------------------
// Internal metrics (ported from IMBadge private constants)
// ---------------------------------------------------------------------------

private val DOT_DIAMETER: Dp = 8.dp
private val COUNT_MIN_SIZE: Dp = 16.dp
private val COUNT_HORIZONTAL_PADDING: Dp = 5.dp
private val RING_WIDTH: Dp = 1.5.dp

// ---------------------------------------------------------------------------
// AMBadge — standalone badge composable
// ---------------------------------------------------------------------------

/**
 * Standalone badge composable — renders either a dot or a count capsule.
 *
 * Most callers should prefer [AMBadgedBox], which wraps the host and positions
 * the badge at its top-trailing corner. `AMBadge` is the building block for
 * that wrapper and for custom positioning in other layouts.
 *
 * **Dot variant:** 8dp circle with a 1.5dp `AMTheme.Surface.primary` ring.
 * Hidden from accessibility ([invisibleToUser]).
 *
 * **Count variant:** Capsule-shaped pill with monospaced digits. Displays
 * "99+" for values over 99. Hidden when `value ≤ 0`. Announces
 * "$value notifications" to TalkBack.
 *
 * Port of `IMBadge` from iOSMtrls.
 *
 * @param content The badge flavor — [AMBadgeContent.Dot] or [AMBadgeContent.Count].
 * @param tint    Fill color for the badge. `null` defaults to
 *                `tokens.semantic.destructive` (iOS system red).
 */
@Composable
fun AMBadge(
    content: AMBadgeContent,
    tint: Color? = null,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val resolvedTint = tint ?: tokens.semantic.destructive.resolve(isDark)
    val ringColor = AMTheme.Surface.primary.resolve(isDark)

    when (content) {
        is AMBadgeContent.Dot -> {
            Box(
                modifier = Modifier
                    .size(DOT_DIAMETER)
                    .clip(CircleShape)
                    .background(resolvedTint, CircleShape)
                    .border(width = RING_WIDTH, color = ringColor, shape = CircleShape)
                    .semantics { invisibleToUser() },
            )
        }

        is AMBadgeContent.Count -> {
            val value = content.value
            if (value <= 0) {
                // Zero-count: render nothing visible but hold no layout space.
                // Using an explicit zero-size Spacer keeps the caller's layout
                // stable so live count bindings don't cause measurement shifts.
                Spacer(modifier = Modifier.size(0.dp))
            } else {
                val displayText = if (value > 99) "99+" else "$value"
                val desc = "$value notifications"
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = COUNT_MIN_SIZE, minHeight = COUNT_MIN_SIZE)
                        .clip(RoundedCornerShape(50))
                        .background(resolvedTint, RoundedCornerShape(50))
                        .border(
                            width = RING_WIDTH,
                            color = ringColor,
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = COUNT_HORIZONTAL_PADDING)
                        .semantics { contentDescription = desc },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = displayText,
                        // Monospaced digits — iOS .monospacedDigit()
                        style = AMTheme.Type.labelSmall.toTextStyle(Color.White)
                            .copy(fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMBadgedBox — host-wrapping convenience composable
// ---------------------------------------------------------------------------

/**
 * Wraps a host composable and overlays a badge at its top-trailing corner,
 * centered on the corner edge (same placement as iOS `alignmentGuide` trick).
 *
 * The badge is offset by half its own footprint so it straddles the corner of
 * the host — matching the standard iOS notification-pip position.
 *
 * ```kotlin
 * AMBadgedBox(
 *     badge = { AMBadge(AMBadgeContent.Count(3)) }
 * ) {
 *     Icon(Icons.Default.Notifications, contentDescription = null)
 * }
 * ```
 *
 * @param badge   The badge composable to overlay. Pass [AMBadge] or a custom composable.
 * @param content The host composable to badge.
 */
@Composable
fun AMBadgedBox(
    badge: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box {
        content()
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Center the badge on the host's top-trailing corner whatever
                // its measured size (8dp dot or variable-width count capsule) —
                // the Compose equivalent of the iOS alignmentGuide(.center) trick.
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(placeable.width / 2, -placeable.height / 2)
                    }
                },
        ) {
            badge()
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark, as required by conventions
// ---------------------------------------------------------------------------

@Preview(
    name = "AMBadge · Light",
    group = "AMBadge",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    backgroundColor = 0xFFF2F2F7, // Surface.page light
)
@Composable
private fun AMBadgePreviewLight() {
    AMThemeRoot {
        AMBadgePreviewContent()
    }
}

@Preview(
    name = "AMBadge · Dark",
    group = "AMBadge",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D, // Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMBadgePreviewDark() {
    AMThemeRoot {
        AMBadgePreviewContent()
    }
}

@Composable
private fun AMBadgePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Box(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
    ) {
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                AMTheme.Spacing.xl.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Dot
            AMBadgedBox(badge = { AMBadge(AMBadgeContent.Dot) }) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AMTheme.Text.secondary.resolve(isDark), CircleShape),
                )
            }

            // Small count (3)
            AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(3)) }) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AMTheme.Text.secondary.resolve(isDark), CircleShape),
                )
            }

            // Overflow count (120 → "99+")
            AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(120)) }) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AMTheme.Text.secondary.resolve(isDark), CircleShape),
                )
            }

            // Zero count (hidden)
            AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(0)) }) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AMTheme.Text.secondary.resolve(isDark), CircleShape),
                )
            }

            // Custom tint
            AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(7), tint = AMTheme.Semantic.info.resolve(isDark)) }) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AMTheme.Text.secondary.resolve(isDark), CircleShape),
                )
            }
        }
    }
}
