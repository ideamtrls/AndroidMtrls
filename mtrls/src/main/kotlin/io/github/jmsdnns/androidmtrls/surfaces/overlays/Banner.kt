package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMSemantic
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.AMTokens
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// AMBannerStyle — the semantic axis for banners
// ---------------------------------------------------------------------------

/**
 * Semantic style axis for [AMBannerItem] — a `typealias` for [AMSemantic], the
 * one canonical role→color + role→glyph axis. A banner style *is* a semantic
 * role: `AMBannerStyle.Info` / `Success` / `Warning` / `Destructive` are the
 * [AMSemantic] cases, and the leading glyph and tint come from [AMSemantic.icon]
 * and [AMSemantic.color]. Mirrors the iOS `public typealias Style = IMSemantic`.
 *
 * The Destructive glyph is [AMSemantic]'s `material-icons-core` substitution
 * (an x-glyph for the absent `xmark.octagon`) — see [AMSemantic.icon] for the
 * full iOS ↔ core substitution table.
 */
typealias AMBannerStyle = AMSemantic

// ---------------------------------------------------------------------------
// AMBannerItem — data carrier
// ---------------------------------------------------------------------------

/**
 * Data carrier for a single banner presentation.
 *
 * Identity is stable across recomposition via [id] (UUID). Pass a new instance
 * with a fresh UUID to replace a running banner; reusing the same [id] leaves
 * the auto-dismiss timer unchanged.
 *
 * @param id       Stable identity. Defaults to a random UUID. Reset the timer
 *                 by replacing the item with a new UUID.
 * @param title    Primary single-line message. Rendered with [AMTheme.Type.title].
 * @param message  Optional supporting line below the title. Rendered with [AMTheme.Type.caption].
 * @param style    Semantic tint axis — drives icon and tint color.
 * @param duration Auto-dismiss delay in milliseconds. iOS default: 4000 ms.
 */
@Immutable
data class AMBannerItem(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String? = null,
    val style: AMBannerStyle = AMBannerStyle.Info,
    val duration: Long = 4000L,
)

// ---------------------------------------------------------------------------
// presentSpringIntOffset — typed mirror of AMTheme.Motion.presentSpring
//
// AMTheme.Motion.presentSpring is AnimationSpec<Float>. The AnimatedVisibility
// slide APIs accept FiniteAnimationSpec<IntOffset>; a typed spring must be
// constructed at the call site. Values mirror the token exactly:
//   dampingRatio = 0.85, stiffness = 321 (response = 0.35s).
// House convention: file-level val with a lockstep comment naming the token
// (precedent: PageIndicator.kt, Tabs.kt snapSpringDp, LoadingOverlay.kt).
// ---------------------------------------------------------------------------

/**
 * Mirror of [AMTheme.Motion.presentSpring] typed as `FiniteAnimationSpec<IntOffset>`.
 *
 * [slideInVertically] / [slideOutVertically] require `FiniteAnimationSpec<IntOffset>`.
 * The motion token is `AnimationSpec<Float>`, so this file-level spring mirrors
 * it with identical values and `IntOffset.VisibilityThreshold` (Compose convention
 * for spring-driven discrete offsets). dampingRatio = 0.85, stiffness = 321.
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
 * typed as the wider `AnimationSpec<Float>`. Driving the scrim + card fade on the
 * *same* present spring as the card slide (instead of the 180ms overlayFade tween)
 * makes present/dismiss read as one motion — iOS animates the whole present, opacity
 * included, on `presentSpring` (`Banner.swift:110`). dampingRatio = 0.85, stiffness = 321.
 */
private val presentSpringFloat = spring<Float>(dampingRatio = 0.85f, stiffness = 321f)

// ---------------------------------------------------------------------------
// AMBanner — host composable
// ---------------------------------------------------------------------------

/**
 * Top-anchored transient banner host that wraps [hostContent].
 *
 * Presents a floating banner card at the top of the screen while [item] is
 * non-null. The banner auto-dismisses after [AMBannerItem.duration] milliseconds,
 * or the user can dismiss it by tapping or swiping upward.
 *
 * **Attach at the scene root** so the banner overlays the full screen. Wrapping
 * a sub-composable clips the overlay to that node's measured size — same
 * constraint as iOS `.imBanner($item)`.
 *
 * **Dismissal modes:**
 * - **Auto:** `LaunchedEffect(item?.id)` fires [onDismiss] after `item.duration`.
 *   Cancels automatically when [item] becomes null or the host leaves composition.
 * - **Tap:** single tap on the banner card calls [onDismiss] immediately.
 * - **Swipe up:** drag upward past `tokens.motion.dismissThresholdCompact` (default
 *   30dp, overridable via AMTokensProvider) triggers [onDismiss] via [amDragDismiss].
 *
 * **Style semantics:** [AMBannerStyle] drives the icon glyph and tint color.
 * The card background, scrim, and text are always adaptive (surface/text
 * tokens), independent of style.
 *
 * @param item        Currently-visible banner data, or `null` to show no banner.
 * @param onDismiss   Called when any dismissal path fires. Caller must set
 *                    [item] to `null` in response.
 * @param modifier    Modifier applied to the host root [Box].
 * @param hostContent Scene content wrapped by this host. Rendered behind the
 *                    banner and its top scrim.
 */
@Composable
fun AMBanner(
    item: AMBannerItem?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hostContent: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Keep the last non-null item in memory during the exit animation so the
    // banner card has content to render while it slides out after item → null.
    var lastItem by remember { mutableStateOf(item) }
    if (item != null) lastItem = item
    val visibleItem = lastItem

    // dragOffset is caller-owned MutableState — hot gesture state that the
    // drag modifier writes per-frame and the offset modifier reads. Keyed on
    // item?.id so it resets to 0f whenever a new banner appears (house rule:
    // FAB precedent in conventions.md §Gesture rules).
    val dragOffset: MutableState<Float> = remember(item?.id) { mutableStateOf(0f) }

    // Auto-dismiss timer. LaunchedEffect cancels and restarts on item?.id change
    // (new banner → fresh countdown) and cancels when item becomes null or the
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
        // Scene content — rendered behind the banner and scrim. Reports "an overlay
        // is up" (this banner OR any ancestor host) down to hostContent so a wrapped
        // FAB hub fades out of the way.
        CompositionLocalProvider(
            LocalAMOverlayActive provides (item != null || inheritedOverlayActive),
        ) {
            hostContent()
        }

        // ── Surface-tinted top scrim ──────────────────────────────────────
        // Fades on the SAME presentSpring as the banner card (not the overlayFade
        // tween) so scrim and card read as one motion — iOS animates the whole
        // present, opacity included, on presentSpring. Non-interactive: the Box
        // carries no click handler so touches fall through to hostContent. Softens
        // to the gentle overlay fade under Reduce Motion via resolvedFinite.
        AnimatedVisibility(
            visible  = item != null,
            enter    = fadeIn(animationSpec  = tokens.motion.resolvedFinite(presentSpringFloat, reduceMotion)),
            exit     = fadeOut(animationSpec = tokens.motion.resolvedFinite(presentSpringFloat, reduceMotion)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)         // scrimHeight — tall enough to cover banner + slide travel
                    .background(
                        brush = Brush.verticalGradient(
                            // scrimFadeStart = 0.2: fully opaque 0%→20%, fades to clear at 100%
                            0.0f to AMTheme.Surface.primary.resolve(isDark),
                            0.2f to AMTheme.Surface.primary.resolve(isDark),
                            1.0f to AMTheme.Surface.primary.resolve(isDark).copy(alpha = 0f),
                        )
                    ),
            )
        }

        // ── Banner card ───────────────────────────────────────────────────
        // Slides in from above combined with a fade, both spring-driven at
        // presentSpring so the slide and opacity move in lockstep (iOS drives the
        // whole present on presentSpring). The card is offset live by dragOffset
        // during swipe-up gesture tracking. Under Reduce Motion the slide drops to a
        // snap (resolvedFiniteOrNull → null) so the card appears in place, while the
        // fade softens via resolvedFinite.
        AnimatedVisibility(
            visible  = item != null,
            enter    = slideInVertically(
                animationSpec  = tokens.motion.resolvedFiniteOrNull(presentSpringIntOffset, reduceMotion) ?: snap(),
                initialOffsetY = { fullHeight -> -fullHeight },
            ) + fadeIn(animationSpec = tokens.motion.resolvedFinite(presentSpringFloat, reduceMotion)),
            exit     = slideOutVertically(
                animationSpec  = tokens.motion.resolvedFiniteOrNull(presentSpringIntOffset, reduceMotion) ?: snap(),
                targetOffsetY  = { fullHeight -> -fullHeight },
            ) + fadeOut(animationSpec = tokens.motion.resolvedFinite(presentSpringFloat, reduceMotion)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            if (visibleItem != null) {
                BannerCard(
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
// BannerCard — internal card composable
// ---------------------------------------------------------------------------

/**
 * The visible banner card: icon + title + optional message inside [AMCard],
 * with swipe-up and tap-to-dismiss wired, and an unconditional hairline border.
 *
 * Structure follows the iOS `BannerView` exactly:
 * - Safe area: `statusBarsPadding()` so the card lands below the status bar on an
 *   edge-to-edge host (iOS keeps the card inside the top safe area; only the scrim
 *   bleeds under the status bar).
 * - Outer padding: `spacing.lg` horizontal, `spacing.sm` vertical.
 * - Live drag offset applied via `Modifier.offset {}`.
 * - Accessibility: `liveRegion = Polite` so TalkBack announces the banner when it
 *   appears mid-screen (DECISIONS D8 — a transient status surface should announce).
 * - Hairline: unconditional `.amHairline(shape)` — iOS uses `.imHairline(…)`
 *   (not `.imCardHairline`, which is dark-only). Banner floating surfaces always
 *   show the outline ring so the card reads over any substrate color.
 * - Row: icon 20dp + Column(spacedBy 2dp): title [AMTheme.Type.title], message [AMTheme.Type.caption].
 */
@Composable
private fun BannerCard(
    item: AMBannerItem,
    dragOffset: MutableState<Float>,
    onDismiss: () -> Unit,
    tokens: AMTokens,
    isDark: Boolean,
) {
    val bannerShape = amSquircle(tokens.radius.lg)

    AMCard(
        modifier = Modifier
            // Clear the status bar on an edge-to-edge host — the scrim behind still
            // bleeds full-height into the status area (it carries no such inset).
            .statusBarsPadding()
            .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.sm)
            .offset { IntOffset(0, dragOffset.value.roundToInt()) }
            .amDragDismiss(
                direction  = AMDragDismissDirection.Up,
                threshold  = tokens.motion.dismissThresholdCompact,
                dragOffset = dragOffset,
                onDismiss  = onDismiss,
            )
            .pointerInput(onDismiss) {
                // Tap-to-dismiss: detectTapGestures so indication = null is implicit
                // (no ripple — house opacity-based press feedback rule).
                detectTapGestures(onTap = { onDismiss() })
            }
            // Announce the banner to TalkBack when it appears (polite: waits for the
            // current utterance to finish rather than interrupting).
            .semantics { liveRegion = LiveRegionMode.Polite }
            // Unconditional hairline — iOS BannerView uses `.imHairline(…)` not
            // `.imCardHairline` (dark-only). Banner cards always show the ring.
            .amHairline(bannerShape),
        padding = null,
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            verticalAlignment     = Alignment.Top,
        ) {
            // Style icon ~20dp — iOS `.font(.title3)` renders at approximately 20pt.
            // Drawn via androidx.compose.foundation.Image (no Material3 Icon composable).
            Image(
                imageVector        = item.style.icon,
                contentDescription = null,
                colorFilter        = ColorFilter.tint(item.style.color(tokens.semantic, isDark)),
                modifier           = Modifier.size(20.dp),
            )

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),  // textSpacing = 2dp (tighter than xs=4dp)
            ) {
                // Title: iOS `.font(.headline)` analog
                BasicText(
                    text  = item.title,
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                // Optional message: iOS `.font(.caption)` analog
                if (item.message != null) {
                    BasicText(
                        text  = item.message,
                        style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark, banner visible over placeholder rows
// ---------------------------------------------------------------------------

@Composable
private fun BannerPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    val item = AMBannerItem(
        title    = "Synced successfully",
        message  = "Your library is up to date.",
        style    = AMBannerStyle.Success,
        duration = Long.MAX_VALUE,  // prevent auto-dismiss in preview
    )

    AMBanner(
        item      = item,
        onDismiss = {},
        modifier  = Modifier
            .fillMaxWidth()
            .height(320.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            color = AMTheme.Surface.primary.resolve(isDark),
                            shape = amSquircle(AMTheme.Radius.md.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text  = "Placeholder row ${i + 1}",
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
            }
        }
    }
}

@Preview(
    name            = "AMBanner · Light",
    group           = "AMBanner",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode          = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMBannerPreviewLight() {
    AMThemeRoot {
        BannerPreviewContent()
    }
}

@Preview(
    name            = "AMBanner · Dark",
    group           = "AMBanner",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMBannerPreviewDark() {
    AMThemeRoot {
        BannerPreviewContent()
    }
}
