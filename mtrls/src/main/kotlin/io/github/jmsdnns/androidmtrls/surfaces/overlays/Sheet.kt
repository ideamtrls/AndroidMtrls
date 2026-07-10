package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// modalSpringIntOffset — typed mirror of AMTheme.Motion.modalSpring
//
// AMTheme.Motion.modalSpring is AnimationSpec<Float>. The AnimatedVisibility
// slide APIs accept FiniteAnimationSpec<IntOffset>; a typed spring must be
// constructed at the call site. Values mirror the token exactly:
//   dampingRatio = 0.86, stiffness = 224 (response = 0.42s).
// The spec's §8 stiffness of 894 is a 4× drafting error — AMENDED ruling 4
// confirms the house Phase 1 token wins: 224/0.86.
// House convention: file-level val with a lockstep comment naming the token
// (precedent: Banner.kt presentSpringIntOffset, PageIndicator.kt snapSpringDp).
// ---------------------------------------------------------------------------

/**
 * Mirror of [AMTheme.Motion.modalSpring] typed as `FiniteAnimationSpec<IntOffset>`.
 *
 * [slideInVertically] / [slideOutVertically] require `FiniteAnimationSpec<IntOffset>`.
 * The motion token is `AnimationSpec<Float>`, so this file-level spring mirrors
 * it with identical values and `IntOffset.VisibilityThreshold` (Compose convention
 * for spring-driven discrete offsets). dampingRatio = 0.86, stiffness = 224.
 */
private val modalSpringIntOffset = spring<IntOffset>(
    dampingRatio        = 0.86f,
    stiffness           = 224f,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

/**
 * Mirror of [AMTheme.Motion.modalSpring] typed as `FiniteAnimationSpec<Float>`.
 *
 * [fadeIn] / [fadeOut] require `FiniteAnimationSpec<Float>`, and the motion token is
 * typed as the wider `AnimationSpec<Float>`. The tap-catcher fade rides this — the
 * *same* modalSpring as the card slide (not the 180ms overlayFade tween) — so the
 * catcher and card read as one motion, mirroring iOS where the `Color.clear` catcher's
 * opacity transition rides the sheet's `modalSpring` (`Sheet.swift:98-101,131`). Under
 * Reduce Motion the card cross-dissolves via this same spec, softened by resolvedFinite
 * to the gentle overlay fade. dampingRatio = 0.86, stiffness = 224.
 */
private val modalSpringFloat = spring<Float>(dampingRatio = 0.86f, stiffness = 224f)

// ---------------------------------------------------------------------------
// AMSheetHeight — how tall the sheet card grows
// ---------------------------------------------------------------------------

/**
 * How tall an [AMSheet] card grows.
 *
 * iOS equivalent: `IMSheetHeight` in `Sources/Surfaces/Overlays/Sheet.swift`.
 */
@Immutable
enum class AMSheetHeight {
    /**
     * Sizes to its content and rides the bottom edge, capped at the host's
     * available height so it can never exceed the screen. Taller content is
     * clipped at the bottom — host anything that can overflow in its own
     * scroll container.
     */
    Content,

    /**
     * Fills the height, stopping just below the top status-bar inset so the
     * grabber clears the status bar. The content region is bounded to the height
     * remaining below the drag handle, so a scroll container in `sheetContent`
     * measures against a finite height and scrolls; content is top-aligned and
     * non-scrolling overflow is clipped. The default — matching iOS
     * `IMSheetHeight.full`.
     */
    Full,
}

// ---------------------------------------------------------------------------
// AMSheet — host composable
// ---------------------------------------------------------------------------

/**
 * Bottom sheet host that wraps [hostContent].
 *
 * Presents a bottom-anchored sheet card while [isPresented] is `true`. The card
 * slides up from the screen bottom on a `modalSpring` (224/0.86) animation and
 * slides back down on dismiss. Under Reduce Motion (`LocalAMReduceMotion`) the
 * full-screen slide collapses to a cross-dissolve in place — the slide/fade
 * specs route through the house Reduce-Motion policy in `tokens/Motion.kt`.
 *
 * **Attach at the scene root** so the sheet card spans the full screen width and
 * the tap-catcher covers the entire surface behind it. Attaching to a sub-composable
 * clips both the card and the tap-catcher to that node's measured bounds.
 *
 * **Height ([height]):**
 * - [AMSheetHeight.Full] (**default**) fills the height, stopping just below the
 *   top status-bar inset ([statusBarsPadding]) so the grabber clears the status
 *   bar; the content region is bounded to the remaining height (`weight(1f)`) so
 *   a scroll container in [sheetContent] measures finite and scrolls, with
 *   content top-aligned and non-scrolling overflow clipped.
 * - [AMSheetHeight.Content] sizes the card to its content, capped at the host's
 *   available height and clipped.
 *
 * The default is [AMSheetHeight.Full] to match iOS `IMSheetHeight.full`; this is
 * a deliberate behavior change from the previous always-content sizing. Pass
 * `height = AMSheetHeight.Content` for the old wrap-to-content behavior.
 *
 * **No blur** — unlike [AMActionsOverlay], the sheet does not blur the content
 * underneath. It simply floats on top, keeping the world visible beneath it.
 * An invisible tap-catcher sits behind the card so taps outside dismiss the sheet
 * rather than landing on the underlying content.
 *
 * **Dismissal modes:**
 * - **Tap outside:** an invisible [amTapCatcher] behind the card calls [onDismiss].
 * - **Drag down:** dragging the card downward past `AMTheme.Motion.dismissThreshold`
 *   (60dp) calls [onDismiss] via [amDragDismiss].
 * - **System back:** [BackHandler] calls [onDismiss] while the sheet is presented.
 *
 * **Safe area & keyboard:** the card background extends to the physical screen
 * edge (the card fill never shrinks), while the content Column is padded with
 * `navigationBarsPadding().imePadding()` so text fields ride above the keyboard
 * when it is up and clear the navigation-bar gesture zone when it is down. Inset
 * consumption makes the two padding modifiers resolve to `max(navBar, ime)`, so
 * the card fill still spans under the keyboard's rounded corners.
 *
 * iOS equivalent: `View.imSheet(isPresented:height:content:)` and `IMSheetModifier`
 * in `Sources/Surfaces/Overlays/Sheet.swift`.
 *
 * Usage:
 * ```kotlin
 * AMSheet(
 *     isPresented  = showSheet,
 *     onDismiss    = { showSheet = false },
 *     height       = AMSheetHeight.Content,
 *     sheetContent = {
 *         BasicText("Sheet content here")
 *     },
 * ) {
 *     MainScreenContent()   // always visible behind the sheet
 * }
 * ```
 *
 * @param isPresented  When `true` the sheet is visible; `false` hides it.
 * @param onDismiss    Called when the user taps outside or drags down past the
 *                     dismiss threshold. Callers must flip [isPresented] to `false`
 *                     in response.
 * @param height       How tall the card grows — [AMSheetHeight.Full] (default) or
 *                     [AMSheetHeight.Content].
 * @param modifier     Modifier applied to the host root [Box].
 * @param sheetContent Composable content rendered inside the sheet card body,
 *                     below the drag handle.
 * @param hostContent  The background composable that is always present behind the
 *                     sheet overlay. Trailing lambda.
 */
@Composable
fun AMSheet(
    isPresented: Boolean,
    onDismiss: () -> Unit,
    height: AMSheetHeight = AMSheetHeight.Full,
    modifier: Modifier = Modifier,
    sheetContent: @Composable () -> Unit,
    hostContent: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Slide + fade specs routed through the house Reduce-Motion policy
    // (tokens/Motion.kt). Under normal motion the card slides on modalSpring;
    // under Reduce Motion the slide collapses (resolvedFiniteOrNull → null) and
    // the card cross-dissolves in place via fadeSpec instead of travelling the
    // full screen height — the Compose twin of iOS
    // `imPresentationEdge(.bottom, reduceMotion:)`.
    val slideSpec = tokens.motion.resolvedFiniteOrNull(modalSpringIntOffset, reduceMotion)
    // Catcher fade (and the card's cross-dissolve under Reduce Motion) ride the same
    // modalSpring as the card slide, so the two read as one motion (iOS fades the
    // catcher on modalSpring). resolvedFinite softens it to the 180ms overlay fade
    // under Reduce Motion.
    val fadeSpec  = tokens.motion.resolvedFinite(modalSpringFloat, reduceMotion)

    // System back dismisses the sheet while it is presented, matching the
    // tap-outside / drag-down affordances.
    BackHandler(enabled = isPresented) { onDismiss() }

    // dragOffset is caller-owned MutableState: hot gesture state that amDragDismiss
    // writes per-frame, and the .offset modifier reads. Keyed on isPresented so
    // the offset resets to 0f whenever the sheet re-presents (mirrors the iOS
    // @State var dragOffset: CGFloat = 0 which resets on each presentation).
    // House rule: gesture-coupled hot state uses MutableState as a bridge
    // (FAB, Banner precedents in conventions.md §Gesture rules).
    val dragOffset: MutableState<Float> = remember(isPresented) { mutableStateOf(0f) }

    // Sheet top-corners-only shape — bottom is anchored at the screen edge, so
    // square bottom corners look correct. Continuous (squircle) corners match
    // iOS UnevenRoundedRectangle(..., style: .continuous):
    //   topLeading = tokens.radius.lg, topTrailing = tokens.radius.lg, bottomLeading = 0, bottomTrailing = 0
    val sheetShape = amSquircle(
        topStart    = tokens.radius.lg,
        topEnd      = tokens.radius.lg,
        bottomEnd   = 0.dp,
        bottomStart = 0.dp,
    )

    // Inherited overlay-active signal, read BEFORE providing it below so a nested
    // host ORs its own state with every ancestor's (see LocalAMOverlayActive).
    val inheritedOverlayActive = LocalAMOverlayActive.current

    Box(modifier = modifier.fillMaxSize()) {
        // ── Background host content — always composed ─────────────────────
        // Reports "an overlay is up" (this sheet OR any ancestor host) down to
        // hostContent so a wrapped FAB hub fades out of the way.
        CompositionLocalProvider(
            LocalAMOverlayActive provides (isPresented || inheritedOverlayActive),
        ) {
            hostContent()
        }

        // ── Tap-outside catcher ───────────────────────────────────────────
        // Fades in/out with fadeSpec (the Reduce-Motion-resolved overlayFade) so
        // it appears alongside the sheet rather than popping. Sits BEFORE the
        // sheet card in declaration order
        // so it is drawn behind the card. The card sinks taps on its own chrome
        // (its detectTapGestures consumer below consumes the down), so only taps
        // on the empty background area outside the card reach this catcher.
        //
        // NO visual scrim or blur — world stays fully visible beneath the sheet.
        // iOS equivalent: Color.clear.imTapCatcher { isPresented = false } with
        //   .transition(.opacity) inside the `if isPresented` block.
        AnimatedVisibility(
            visible = isPresented,
            enter   = fadeIn(animationSpec  = fadeSpec),
            exit    = fadeOut(animationSpec = fadeSpec),
        ) {
            Box(modifier = Modifier.amTapCatcher { onDismiss() })
        }

        // ── Sheet card ────────────────────────────────────────────────────
        // Slides up from the screen bottom on modalSpring (224/0.86 — longer
        // response for large-distance bottom-edge travel). Aligned to
        // BottomCenter so the card rides the screen bottom edge.
        //
        // Chrome layering order (mirrors Card.kt):
        //   1. amElevation (shadow draws behind fill — bleeds onto page substrate)
        //   2. background fill (Surface.primary clipped to sheetShape)
        //   3. amHairline (unconditional — drawn on top of fill, inside bounds)
        // Then content inside: drag handle → sheetContent → bottom spacer.
        //
        // Height ([height]): Full adds statusBarsPadding + fillMaxHeight to the
        // card container so it fills below the status bar; Content leaves the card
        // wrapping its content. navigationBarsPadding + imePadding sit on the inner
        // CONTENT column (not the card container) so the background fill extends to
        // the physical screen edge under the nav bar AND the keyboard, while text
        // and controls clear the gesture zone and ride above the keyboard.
        AnimatedVisibility(
            visible  = isPresented,
            // Normal motion: slide up from the bottom on modalSpring. Reduce
            // Motion (slideSpec == null): cross-dissolve in place, no full-screen
            // travel — mirrors iOS `imPresentationEdge(.bottom, reduceMotion:)`.
            enter    = if (slideSpec != null) {
                slideInVertically(
                    animationSpec  = slideSpec,
                    initialOffsetY = { fullHeight -> fullHeight },
                )
            } else {
                fadeIn(animationSpec = fadeSpec)
            },
            exit     = if (slideSpec != null) {
                slideOutVertically(
                    animationSpec = slideSpec,
                    targetOffsetY = { fullHeight -> fullHeight },
                )
            } else {
                fadeOut(animationSpec = fadeSpec)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Live drag offset: written by amDragDismiss, read here to
                    // give the user tactile follow-during-drag feedback.
                    // offset {} avoids layout recomposition on every frame.
                    // coerceAtLeast(0) clamps the card at rest (iOS Sheet.swift:115
                    // `.offset(y: max(dragOffset, 0))`) so the under-damped snap-back
                    // overshoot can't lift the card above its top edge; the Down
                    // direction gating already prevents an upward drag from writing
                    // a negative offset in the first place.
                    .offset { IntOffset(0, dragOffset.value.roundToInt().coerceAtLeast(0)) }
                    // Full: push the whole card (fill + chrome) below the top
                    // status-bar inset so the grabber clears the status bar, then
                    // fill the remaining height. Content stays top-aligned (Column
                    // default) and anything taller is clipped by clip(sheetShape).
                    // Content: no top inset, no fill — the card wraps its content,
                    // capped at the host Box's max height and clipped.
                    .then(
                        if (height == AMSheetHeight.Full) {
                            Modifier.statusBarsPadding().fillMaxHeight()
                        } else {
                            Modifier
                        }
                    )
                    // Gesture: downward drag past dismissThreshold (60dp) calls onDismiss.
                    .amDragDismiss(
                        direction  = AMDragDismissDirection.Down,
                        threshold  = tokens.motion.dismissThreshold,
                        dragOffset = dragOffset,
                        onDismiss  = onDismiss,
                    )
                    // Tap sink over the whole card. amDragDismiss's vertical-drag
                    // detector does NOT consume a simple tap, so a tap on the card's
                    // own chrome would otherwise fall through to the amTapCatcher
                    // sibling behind and dismiss the sheet. detectTapGestures consumes
                    // the down so chrome taps are swallowed here; drags still work (the
                    // drag detector accepts a consumed down).
                    .pointerInput(Unit) {
                        detectTapGestures { /* consume: chrome taps are not outside-taps */ }
                    }
                    // Explicit dismiss semantics so TalkBack exposes a discrete
                    // "Dismiss" action for the invisible tap-outside affordance,
                    // mirroring iOS's `.accessibilityAction(.escape)` on the catcher
                    // (Sheet.swift:100). System-back already routes here via BackHandler.
                    .semantics { dismiss("Dismiss") { onDismiss(); true } }
                    // Chrome layer 1: elevation shadow — draws BEHIND the fill (drawBehind),
                    // bleeds onto the page background and remains visible.
                    .amElevation(level = AMElevation.Medium, shape = sheetShape)
                    // Chrome layer 2: background fill clipped to top-only-rounded shape.
                    .background(
                        color = AMTheme.Surface.primary.resolve(isDark),
                        shape = sheetShape,
                    )
                    // Chrome layer 3: unconditional hairline inside bounds.
                    // Uses amHairline (not amCardHairline) because the sheet uses an
                    // unconditional outline in both light and dark — iOS imSheet uses
                    // .imHairline (unconditional), not .imCardHairline (dark-only).
                    .amHairline(sheetShape)
                    // Clip after chrome so content respects the top-only corners.
                    .clip(sheetShape),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ── Drag handle ───────────────────────────────────────────
                // 36×5dp capsule, Icon.muted fill, padding-top sm, centered — sized
                // to the iOS sheet grabber (Sheet.swift:69-70 dragHandleWidth 36,
                // dragHandleHeight 5) so the affordance reads as the system grabber.
                Box(
                    modifier = Modifier.padding(top = tokens.spacing.sm),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 5.dp)
                            .background(
                                color = AMTheme.Icon.muted.resolve(isDark),
                                shape = RoundedCornerShape(percent = 50),  // capsule
                            ),
                    )
                }

                // ── Sheet body content ────────────────────────────────────
                // navigationBarsPadding + imePadding on the content Column so the
                // content clears the nav-bar gesture zone and rides above the
                // keyboard, while the background fill (set on the outer Column
                // modifier) still extends to the physical screen edge under both.
                // Chained navBar-then-ime, inset consumption makes the pair
                // resolve to max(navBar, ime): when the keyboard is up the ime
                // padding subtracts the already-consumed nav-bar strip, so the
                // total is the keyboard height (not keyboard + nav bar); when it
                // is down the ime insets are zero and only the nav bar pads. The
                // card fill never shrinks — only the content is pushed up.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Full: bound the content region to the height remaining
                        // below the drag handle via weight(1f). Content mode keeps
                        // wrap sizing — the card itself wraps its content, so there
                        // is no unbounded height to hand down.
                        .then(
                            if (height == AMSheetHeight.Full) Modifier.weight(1f)
                            else Modifier
                        )
                        .navigationBarsPadding()
                        .imePadding(),
                ) {
                    // Full: sheetContent goes inside a weighted BOX, not directly
                    // in this Column — a plain Column hands its non-weighted
                    // children an INFINITE main-axis max, which crashes any scroll
                    // container placed at the top level of sheetContent
                    // ("Vertically scrollable component was measured with an
                    // infinity maximum height"). A Box passes its own bounded
                    // constraints through, so a verticalScroll/LazyColumn in
                    // sheetContent measures against a FINITE max height and
                    // scrolls. Same precedent as AMPanel's weighted body Box
                    // (Panel.kt). The bottom spacer stays a sibling BELOW the
                    // weighted region so it is never scrolled away.
                    if (height == AMSheetHeight.Full) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            sheetContent()
                        }
                    } else {
                        sheetContent()
                    }

                    // Bottom spacer — gives content room above the home indicator
                    // in the same way iOS .padding(.bottom, tokens.spacing.xxl) does.
                    Spacer(modifier = Modifier.height(tokens.spacing.xxl))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark, sheet presented over placeholder rows
// ---------------------------------------------------------------------------

@Composable
private fun SheetPreviewContent(height: AMSheetHeight) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    AMSheet(
        isPresented  = true,
        onDismiss    = {},
        height       = height,
        modifier     = Modifier
            .fillMaxWidth()
            .height(480.dp),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(tokens.spacing.lg),
            ) {
                BasicText(
                    text  = "Sheet title",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                Spacer(modifier = Modifier.height(tokens.spacing.sm))
                BasicText(
                    text  = "Drag down or tap outside to dismiss.",
                    style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                )
            }
        },
    ) {
        // Placeholder host content visible beneath the sheet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMTheme.Surface.page.resolve(isDark))
                .padding(tokens.spacing.lg),
        ) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AMTheme.rowMinHeight.dp)
                        .background(
                            color = AMTheme.Surface.primary.resolve(isDark),
                            shape = amSquircle(AMTheme.Radius.md.dp),
                        )
                        .padding(horizontal = tokens.spacing.lg),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicText(
                        text  = "Placeholder row ${i + 1}",
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
                if (i < 4) Spacer(modifier = Modifier.height(tokens.spacing.sm))
            }
        }
    }
}

// Full · Light — fills the frame (the preview renderer reports a zero status-bar
// inset, so statusBarsPadding contributes nothing and the card fills 480dp).
@Preview(
    name            = "AMSheet · Full · Light",
    group           = "AMSheet",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode          = Configuration.UI_MODE_NIGHT_NO,
    widthDp         = 390,
    heightDp        = 480,
)
@Composable
private fun AMSheetFullPreviewLight() {
    AMThemeRoot {
        SheetPreviewContent(height = AMSheetHeight.Full)
    }
}

// Content · Dark — the card wraps its content and rides the bottom edge, so the
// placeholder host rows stay visible above it (no scrim, no blur).
@Preview(
    name            = "AMSheet · Content · Dark",
    group           = "AMSheet",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
    widthDp         = 390,
    heightDp        = 480,
)
@Composable
private fun AMSheetContentPreviewDark() {
    AMThemeRoot {
        SheetPreviewContent(height = AMSheetHeight.Content)
    }
}
