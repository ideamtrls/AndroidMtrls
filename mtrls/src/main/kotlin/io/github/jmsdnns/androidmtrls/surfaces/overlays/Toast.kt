package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.AMTokens
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// AMToastItem — data carrier
// ---------------------------------------------------------------------------

/**
 * Data carrier for a single toast presentation.
 *
 * Identity is stable across recomposition via [id] (UUID). Pass a new instance
 * with a fresh UUID to replace a running toast; reusing the same [id] leaves
 * the auto-dismiss timer unchanged.
 *
 * @param id       Stable identity. Defaults to a random UUID. Reset the timer
 *                 by replacing the item with a new UUID.
 * @param text     Single-line message. Rendered with [AMTheme.Type.secondary].
 * @param icon     Optional leading icon glyph. Drawn at ~15dp with
 *                 [AMTheme.Text.primary] tint. Pass `null` for text-only.
 * @param duration Auto-dismiss delay in milliseconds. Default: 2000 ms (2 s),
 *                 shorter than [AMBannerItem.duration] because toasts are brief
 *                 confirmations rather than action-required alerts.
 */
@Immutable
data class AMToastItem(
    val id: UUID = UUID.randomUUID(),
    val text: String,
    val icon: ImageVector? = null,
    val duration: Long = 2000L,
)

// ---------------------------------------------------------------------------
// AMToastAnchor — bottom-edge alignment axis
// ---------------------------------------------------------------------------

/**
 * Bottom-edge alignment for the toast pill inside its host [Box].
 *
 * - [Center]   — `BottomCenter`. Default. Use when no FAB occupies the bottom.
 * - [Trailing] — `BottomEnd`.   Use when a [AMFloatingActionButton] or other
 *               bottom-trailing control would overlap a centred toast.
 * - [Leading]  — `BottomStart`. Mirrors the iOS `.leading` case; rarely used
 *               on LTR layouts but available for completeness.
 *
 * iOS equivalent: `IMToast.Anchor`.
 */
@Immutable
enum class AMToastAnchor {

    /** Bottom-centre. Default. */
    Center,

    /** Bottom-trailing (end). Use when a FAB occupies the bottom-trailing corner. */
    Trailing,

    /** Bottom-leading (start). */
    Leading;

    /** Compose [Alignment] that places the pill at the correct bottom edge. */
    val alignment: Alignment get() = when (this) {
        Center   -> Alignment.BottomCenter
        Trailing -> Alignment.BottomEnd
        Leading  -> Alignment.BottomStart
    }
}

// ---------------------------------------------------------------------------
// presentSpringIntOffset — typed mirror of AMTheme.Motion.presentSpring
//
// AMTheme.Motion.presentSpring is AnimationSpec<Float>. slideInVertically /
// slideOutVertically require FiniteAnimationSpec<IntOffset>. Values mirror
// the token exactly: dampingRatio = 0.85, stiffness = 321 (response = 0.35s).
// House convention: file-level val with a lockstep comment naming the token
// (precedent: Banner.kt, PageIndicator.kt, Tabs.kt snapSpringDp).
// ---------------------------------------------------------------------------

/**
 * Mirror of [AMTheme.Motion.presentSpring] typed as `FiniteAnimationSpec<IntOffset>`.
 *
 * [slideInVertically] / [slideOutVertically] require `FiniteAnimationSpec<IntOffset>`.
 * The motion token is `AnimationSpec<Float>`, so this file-level spring mirrors
 * it with identical values and `IntOffset.VisibilityThreshold`. dampingRatio = 0.85,
 * stiffness = 321.
 */
private val presentSpringIntOffset = spring<IntOffset>(
    dampingRatio        = 0.85f,
    stiffness           = 321f,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

/**
 * Mirror of [AMTheme.Motion.presentSpring] typed as `FiniteAnimationSpec<Float>`.
 *
 * [fadeIn] / [fadeOut] require `FiniteAnimationSpec<Float>`, and the motion token is
 * typed as the wider `AnimationSpec<Float>`. Driving the pill fade on the *same*
 * present spring as the slide (instead of the 180ms overlayFade tween) makes
 * present/dismiss read as one motion — iOS animates the whole present, opacity
 * included, on `presentSpring` (`Toast.swift:93`). dampingRatio = 0.85, stiffness = 321.
 */
private val presentSpringFloat = spring<Float>(dampingRatio = 0.85f, stiffness = 321f)

// ---------------------------------------------------------------------------
// AMToast — host composable
// ---------------------------------------------------------------------------

/**
 * Bottom-anchored transient toast host that wraps [hostContent].
 *
 * Presents a floating pill at the bottom of the screen while [item] is
 * non-nil. The toast auto-dismisses after [AMToastItem.duration] milliseconds,
 * or the user can dismiss it by tapping the pill or swiping downward.
 *
 * **Attach at the scene root** so the toast overlays the full screen. Wrapping
 * a sub-composable clips the overlay to that node's measured size — same
 * constraint as iOS `.imToast($item)`.
 *
 * **Dismissal modes:**
 * - **Auto:** `LaunchedEffect(item?.id)` fires [onDismiss] after `item.duration`.
 *   Cancels automatically when [item] becomes null or the host leaves composition.
 * - **Tap:** single tap on the pill calls [onDismiss] immediately.
 * - **Swipe down:** drag downward past `tokens.motion.dismissThresholdCompact`
 *   (default 30 dp, overridable via AMTokensProvider) triggers [onDismiss] via
 *   [amDragDismiss].
 *
 * **Style semantics:** toasts are deliberately neutral — no semantic tint
 * (no red/orange/green). For tinted or action-required feedback use [AMBanner].
 *
 * **Anchor:** defaults to [AMToastAnchor.Center]. Switch to [AMToastAnchor.Trailing]
 * when a FAB occupies the bottom-trailing corner to prevent overlap.
 *
 * @param item        Currently-visible toast data, or `null` to show no toast.
 * @param onDismiss   Called when any dismissal path fires. Caller must set
 *                    [item] to `null` in response.
 * @param anchor      Bottom-edge alignment of the toast pill.
 * @param modifier    Modifier applied to the host root [Box].
 * @param hostContent Scene content wrapped by this host. Rendered behind the
 *                    toast pill.
 */
@Composable
fun AMToast(
    item: AMToastItem?,
    onDismiss: () -> Unit,
    anchor: AMToastAnchor = AMToastAnchor.Center,
    modifier: Modifier = Modifier,
    hostContent: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Keep the last non-null item in memory during the exit animation so the
    // pill has content to render while it slides out after item → null.
    var lastItem by remember { mutableStateOf(item) }
    if (item != null) lastItem = item
    val visibleItem = lastItem

    // dragOffset is caller-owned MutableState — hot gesture state that the
    // drag modifier writes per-frame and the offset modifier reads. Keyed on
    // item?.id so it resets to 0f whenever a new toast appears (house rule:
    // FAB precedent in conventions.md §Gesture rules).
    val dragOffset: MutableState<Float> = remember(item?.id) { mutableStateOf(0f) }

    // Auto-dismiss timer. LaunchedEffect cancels and restarts on item?.id change
    // (new toast → fresh countdown) and cancels when item becomes null or the
    // host leaves composition. Mirrors iOS `.task(id: item?.id)` semantics.
    // rememberUpdatedState keeps the long-lived timer coroutine calling the latest
    // onDismiss rather than the one captured at the presentation's first frame
    // (house stale-closure pattern — precedent: SwipeActions.kt reportOpen).
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(item?.id) {
        val current = item ?: return@LaunchedEffect
        delay(current.duration)
        currentOnDismiss()
    }

    // Read the inherited overlay-active signal BEFORE providing it below, so a
    // nested host ORs its own state with every ancestor's (see LocalAMOverlayActive).
    val inheritedOverlayActive = LocalAMOverlayActive.current

    Box(modifier = modifier) {
        // Scene content — rendered behind the toast pill. Reports "an overlay is up"
        // (this toast OR any ancestor host) down to hostContent so a wrapped FAB hub
        // fades out of the way.
        CompositionLocalProvider(
            LocalAMOverlayActive provides (item != null || inheritedOverlayActive),
        ) {
            hostContent()
        }

        // ── Toast pill ────────────────────────────────────────────────────
        // Slides in from below combined with a fade, both spring-driven at
        // presentSpring so the slide and opacity move in lockstep (iOS drives the
        // whole present on presentSpring). The pill is offset live by dragOffset
        // during swipe-down gesture tracking. Under Reduce Motion the slide drops to
        // a snap (resolvedFiniteOrNull → null) so the pill appears in place, while
        // the fade softens via resolvedFinite.
        AnimatedVisibility(
            visible  = item != null,
            enter    = slideInVertically(
                animationSpec  = tokens.motion.resolvedFiniteOrNull(presentSpringIntOffset, reduceMotion) ?: snap(),
                initialOffsetY = { fullHeight -> fullHeight },
            ) + fadeIn(animationSpec = tokens.motion.resolvedFinite(presentSpringFloat, reduceMotion)),
            exit     = slideOutVertically(
                animationSpec  = tokens.motion.resolvedFiniteOrNull(presentSpringIntOffset, reduceMotion) ?: snap(),
                targetOffsetY  = { fullHeight -> fullHeight },
            ) + fadeOut(animationSpec = tokens.motion.resolvedFinite(presentSpringFloat, reduceMotion)),
            modifier = Modifier.align(anchor.alignment),
        ) {
            if (visibleItem != null) {
                ToastPill(
                    item       = visibleItem,
                    dragOffset = dragOffset,
                    onDismiss  = onDismiss,
                    tokens     = tokens,
                    isDark     = isDark,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ToastPill — internal pill composable
// ---------------------------------------------------------------------------

/**
 * The visible toast pill: optional icon + text inside a capsule background
 * with unconditional hairline, elevation, swipe-down and tap-to-dismiss.
 *
 * Chrome modifier chain (elevation → background+clip → hairline):
 * 1. `amElevation(Low, capsuleShape)` — shadow drawn *behind* the fill.
 * 2. `background(Surface.primary, capsuleShape)` — fills and clips content.
 * 3. `amHairline(capsuleShape)` — unconditional ring inside bounds.
 *
 * This ordering mirrors [AMCard]'s layering (elevation → fill → hairline) and
 * the iOS `ToastView` recipe: `.background(Capsule…) → .imHairline(Capsule…)
 * → .imElevation(.low)` — note iOS applies elevation last because SwiftUI
 * modifiers build inside-out; the visual result is identical.
 *
 * Outer padding: `xl` bottom, `lg` horizontal (positions pill above the
 * screen edge with comfortable breathing room). Vertical inner padding:
 * `sm + 2dp` (the `verticalPadBump = 2` constant from iOS, preventing the
 * single text line from pinching against the capsule curve).
 */
@Composable
private fun ToastPill(
    item: AMToastItem,
    dragOffset: MutableState<Float>,
    onDismiss: () -> Unit,
    tokens: AMTokens,
    isDark: Boolean,
) {
    // Capsule shape: RoundedCornerShape(50%) is the idiomatic Compose capsule.
    // CircleShape applies equal radii in all directions and doesn't scale with
    // non-square bounds — use 50% corner fraction instead.
    val capsuleShape = RoundedCornerShape(50)

    // Extra vertical padding to prevent the single text line from pinching
    // against the capsule curve — mirrors ToastView.verticalPadBump = 2 in iOS.
    val verticalPadBump = 2.dp

    Row(
        modifier = Modifier
            // Clear the gesture nav bar / home indicator on an edge-to-edge host,
            // mirroring iOS respecting the bottom safe area (applied before the xl
            // breathing-room pad so the pill sits xl above the nav bar).
            .navigationBarsPadding()
            .padding(
                bottom = tokens.spacing.xl,
                start  = tokens.spacing.lg,
                end    = tokens.spacing.lg,
            )
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .amDragDismiss(
                direction  = AMDragDismissDirection.Down,
                threshold  = tokens.motion.dismissThresholdCompact,
                dragOffset = dragOffset,
                onDismiss  = onDismiss,
            )
            .pointerInput(onDismiss) {
                // Tap-to-dismiss: detectTapGestures so indication = null is
                // implicit (no ripple — house opacity-based press feedback rule).
                detectTapGestures(onTap = { onDismiss() })
            }
            // Announce the toast to TalkBack when it appears (polite: waits for the
            // current utterance to finish rather than interrupting).
            .semantics { liveRegion = LiveRegionMode.Polite }
            // Chrome modifier chain — elevation → background+clip → hairline.
            // Shadow drawn behind the fill so blur bleeds onto the substrate.
            .amElevation(level = AMElevation.Low, shape = capsuleShape)
            .background(
                color = AMTheme.Surface.primary.resolve(isDark),
                shape = capsuleShape,
            )
            // Unconditional hairline — toasts always show the ring regardless
            // of light/dark mode (same rule as AMBanner: .imHairline not
            // .imCardHairline).
            .amHairline(capsuleShape)
            .padding(
                horizontal = tokens.spacing.md,
                vertical   = tokens.spacing.sm + verticalPadBump,
            ),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        if (item.icon != null) {
            // Icon ~15dp — iOS `.font(.subheadline.weight(.semibold))` renders
            // at approximately 15pt. Drawn via androidx.compose.foundation.Image
            // (no Material3 Icon composable — house rule).
            Image(
                imageVector        = item.icon,
                contentDescription = null,
                colorFilter        = ColorFilter.tint(AMTheme.Text.primary.resolve(isDark)),
                modifier           = Modifier.size(15.dp),
            )
        }

        // Text: secondary scale, single line with ellipsis.
        // iOS: `.font(.subheadline.weight(.medium))`.
        BasicText(
            text     = item.text,
            style    = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark, pill visible over placeholder rows
// ---------------------------------------------------------------------------

@Composable
private fun ToastPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    val item = AMToastItem(
        text     = "Copied to clipboard",
        icon     = Icons.Filled.CheckCircle,
        duration = Long.MAX_VALUE,   // prevent auto-dismiss in preview
    )

    AMToast(
        item      = item,
        onDismiss = {},
        modifier  = Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        // Placeholder rows — show the pill overlaying real content.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(AMTheme.Surface.page.resolve(isDark)),
        )
    }
}

@Preview(
    name            = "AMToast · Light",
    group           = "AMToast",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode          = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMToastPreviewLight() {
    AMThemeRoot {
        ToastPreviewContent()
    }
}

@Preview(
    name            = "AMToast · Dark",
    group           = "AMToast",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMToastPreviewDark() {
    AMThemeRoot {
        ToastPreviewContent()
    }
}
