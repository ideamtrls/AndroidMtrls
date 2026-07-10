package io.github.jmsdnns.androidmtrls.surfaces.frames

import android.content.res.Configuration
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMDivider
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.surfaces.overlays.LocalAMOverlayActive
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Panel presentation animation
// ---------------------------------------------------------------------------

/**
 * Duration for the panel slide-in / slide-out animation, in milliseconds.
 *
 * Longer than [AMTheme.Motion.overlayFade] (180ms) because the panel travels
 * the full screen width. iOS equivalent: `panelPresentationFade = .easeInOut(duration:0.25)`.
 */
private const val panelPresentationDurationMs = 250

/**
 * Panel present/dismiss slide spec — a [panelPresentationDurationMs]ms tween typed
 * for the horizontal-offset transition. [slideInHorizontally] / [slideOutHorizontally]
 * require `FiniteAnimationSpec<IntOffset>`; a typed tween is constructed here so the
 * spec can route through the house Reduce-Motion policy.
 */
private val panelSlideSpec = tween<IntOffset>(durationMillis = panelPresentationDurationMs)

/**
 * Panel present/dismiss fade spec — a [panelPresentationDurationMs]ms tween typed for
 * opacity, paired with [panelSlideSpec] so slide and fade share one duration.
 */
private val panelFadeSpec = tween<Float>(durationMillis = panelPresentationDurationMs)

/**
 * Spring that mirrors [AMTheme.Motion.snapSpring] for Float targets — drives the
 * predictive-back peek snap-back when the system gesture is cancelled. Duplicated
 * from the token by design (the token is `AnimationSpec<Float>`; this file needs a
 * `spring<Float>` instance): dampingRatio = 0.86, stiffness = 384. Keep in lockstep
 * with `AMTheme.Motion.snapSpring`.
 */
private val snapSpringFloat = spring<Float>(dampingRatio = 0.86f, stiffness = 384f)

// ---------------------------------------------------------------------------
// AMPanel — standalone slide-in panel composable
// ---------------------------------------------------------------------------

/**
 * Shared visual chrome for slide-in detail panels — an **Android-native top app
 * bar** over an edge-to-edge surface.
 *
 * The bar is a standard Material-class app bar (64dp tall below the status bar),
 * dressed in the mtrls skin:
 * - **Leading back arrow** — a borderless [AMIconButton] (Material
 *   `Icons.AutoMirrored.Filled.ArrowBack` glyph, no surface chip / fill / shadow)
 *   that calls [onBack]. Press feedback is the monochrome `amRipple()` state layer
 *   bounded to a circular touch target, exactly like a native `IconButton`.
 * - **Left-aligned title** — [AMTheme.Type.title] `Text.primary`, next to the back
 *   arrow, single line with an ellipsis. Not centered — Android bars lead with the
 *   title, and [content] with `Modifier.weight(1f)` pushes any trailing action to
 *   the end.
 * - **Trailing slot** — optional caller-supplied action rendered at the end of the
 *   bar (typically another borderless [AMIconButton]). Pass `null` (default) for none.
 * - **Hairline under the bar** — a full-bleed [AMDivider] separates the bar from
 *   the content beneath (always on; the library carries no scroll-aware bar
 *   machinery). This is the Android "attached app bar" cue, replacing the iOS
 *   chrome-less floating title.
 *
 * The bar carries `statusBarsPadding()` so the back arrow and title clear the
 * status bar / display cutout — the Compose analog of iOS hosting the bar in a top
 * `safeAreaInset`. Below the bar and its divider, [content] fills the remaining
 * space edge-to-edge.
 *
 * Dismissal is by tapping the back arrow. When presented through [AMPanelHost],
 * the Android system predictive-back gesture also dismisses the panel and drives
 * a live peek-away preview — the system owns the interactive back affordance.
 *
 * **Background:** `AMPanel` deliberately carries **no** background. The
 * [AMPanelHost] presenter applies `AMTheme.Surface.page` behind the panel so
 * callers who use `AMPanel` directly may supply their own substrate.
 *
 * **Divergence from iOS (deliberate re-Androidification):** iOS
 * `IMPanel<Content, Trailing>` (`Sources/Surfaces/Frames/Panel.swift`) floats a
 * *centered* title over a leading circular back chip with no bar chrome. This
 * Android port keeps the mtrls signature (type ramp, hairline, motion) but adopts
 * native top-app-bar anatomy — leading back arrow, left title, standard bar
 * height, divider under the bar — so it reads as an Android screen, not an iOS one.
 *
 * Usage (standalone, no trailing):
 * ```kotlin
 * AMPanel(title = "Settings", onBack = { navController.popBackStack() }) {
 *     SettingsContent()
 * }
 * ```
 *
 * Usage (with trailing action):
 * ```kotlin
 * AMPanel(
 *     title = "Settings",
 *     onBack = onBack,
 *     trailing = { AMIconButton(Icons.Default.MoreVert, contentDescription = "More", onClick = {}) },
 * ) {
 *     SettingsContent()
 * }
 * ```
 *
 * @param title    Title text rendered left-aligned in the app bar, next to the back arrow.
 * @param onBack   Called when the back arrow is tapped.
 * @param modifier Modifier applied to the panel's root layout node.
 * @param trailing Optional composable rendered as the bar's trailing action (sizes
 *                 itself — typically a borderless [AMIconButton]). Pass `null`
 *                 (default) for no trailing action.
 * @param content  The composable content hosted below the app bar and its divider.
 */
@Composable
fun AMPanel(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── App bar ────────────────────────────────────────────────────
            // A native-height (64dp) top app bar: leading back arrow, left-aligned
            // title, optional trailing action. statusBarsPadding() lands the bar
            // below the status bar / cutout (mirroring iOS hosting the title bar in
            // a top safeAreaInset, Panel.swift:84) so the arrow and title never
            // collide with the system status bar; height(64.dp) then fixes the bar
            // to the Android app-bar class. Applied here (not the body) so the body
            // sits flush under the bar with no double top inset.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    // 4dp gutter: the back arrow's 44dp tap target lands its glyph
                    // ~16dp from the edge, the Material navigation-icon inset.
                    .padding(horizontal = AMTheme.Spacing.xs.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Leading back arrow — a borderless icon button (no surface chip),
                // the Android navigation-icon idiom. amRipple bounded to its disc.
                AMIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    size = AMIconButtonSize.Medium,
                    contentDescription = "Back",
                    onClick = onBack,
                )

                Spacer(modifier = Modifier.width(AMTheme.Spacing.xs.dp))

                // Left-aligned title — weight(1f) claims the remaining width and
                // pushes any trailing action to the end. Ellipsized single line.
                BasicText(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Trailing action slot — caller-sized (typically a borderless
                // AMIconButton). No balancing spacer needed: the title is
                // left-aligned, so nothing has to be kept optically centered.
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(AMTheme.Spacing.sm.dp))
                    trailing()
                }
            }

            // ── Hairline under the bar ─────────────────────────────────────
            // Full-bleed separator — the Android "attached app bar" cue that
            // replaces the iOS chrome-less floating title. Always on: the library
            // has no scroll-aware/collapsing bar machinery to gate it on.
            AMDivider()

            // ── Panel body content ────────────────────────────────────────
            // Bounded to the remaining height below the title bar via weight(1f)
            // so it behaves like a Scaffold body: content that scrolls
            // (verticalScroll / LazyColumn) measures against a finite height. A
            // plain Column hands its non-weighted children an INFINITE main-axis
            // max, which crashes any scroll container placed directly here
            // ("Vertically scrollable component was measured with an infinity
            // maximum height"). The weight gives content a finite max instead.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMPanelHost — composable presenter that wraps a host composable
// ---------------------------------------------------------------------------

/**
 * Presents an [AMPanel] as an animated slide-in overlay over [hostContent].
 *
 * When [isPresented] is `true`, the panel slides in from the trailing (right)
 * edge with a 250ms ease-in-out animation, backed by a full-screen
 * `AMTheme.Surface.page` fill. When `false`, the panel slides back out.
 *
 * **Predictive back:** while presented, a [PredictiveBackHandler] tracks the
 * Android system back gesture. As the user drags, the panel peeks off toward the
 * trailing edge (`translationX` up to 0.3× width) and fades slightly (`alpha`
 * down to 0.9); committing the gesture calls [onDismiss] and the 250ms slide-out
 * carries the panel away, while cancelling springs the peek back to rest on
 * [AMTheme.Motion.snapSpring]. The system owns the back affordance.
 *
 * **Attach at the scene root** so the overlay covers the full screen. Attaching
 * to a sub-composable clips the panel to that component's bounds.
 *
 * This is the Compose-idiomatic translation of `View.imPanel(isPresented:...)` — a
 * `ViewModifier` in iOS that wraps its receiver in a `ZStack` with an overlay.
 * Because Compose `Modifier` extensions cannot introduce composable layout nodes
 * (they cannot call `AnimatedVisibility`), this is a standalone `@Composable`
 * function with separate [hostContent] and [panelContent] slots.
 *
 * iOS equivalent: `View.imPanel(isPresented:title:content:trailing:)` and
 * `IMPanelModifier` in `Sources/Surfaces/Frames/Panel.swift`.
 *
 * Usage:
 * ```kotlin
 * AMPanelHost(
 *     isPresented = showPanel,
 *     title = "Settings",
 *     onDismiss = { showPanel = false },
 *     panelContent = { SettingsContent() },
 * ) {
 *     MainScreenContent()   // always visible behind the panel
 * }
 * ```
 *
 * @param isPresented  When `true` the panel is visible; `false` hides it.
 * @param title        Title text for the panel's title bar.
 * @param onDismiss    Called when the back button is tapped or the system
 *                     predictive-back gesture commits. Callers should flip
 *                     [isPresented] to `false` here.
 * @param trailing     Optional composable for the trailing title-bar slot.
 * @param panelContent The composable content hosted inside the panel body.
 * @param hostContent  The background composable that is always present.
 */
@Composable
fun AMPanelHost(
    isPresented: Boolean,
    title: String,
    onDismiss: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    panelContent: @Composable () -> Unit,
    hostContent: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Snap-back settle for the cancelled predictive-back peek, routed through the
    // house Reduce-Motion policy (softens to the gentle overlay fade under Reduce
    // Motion). The live peek tracking itself (backProgress ← event.progress) stays
    // 1:1 and unrouted.
    val settleSpec = remember(reduceMotion) {
        tokens.motion.resolved(snapSpringFloat, reduceMotion)
    }

    // Live predictive-back progress in [0f, 1f]: 0f at rest, growing as the user
    // drags the system back gesture. Read by the panel's graphicsLayer below.
    var backProgress by remember { mutableFloatStateOf(0f) }

    // Scope for the cancel snap-back. It must NOT be the PredictiveBackHandler's
    // own gesture coroutine: on cancellation the handler cancels that job, which
    // would immediately kill any suspending animation launched inside the catch.
    val backScope = rememberCoroutineScope()

    // System predictive back. Called unconditionally (the `enabled` flag toggles
    // it) per the PredictiveBackHandler contract — never inside an `if`.
    PredictiveBackHandler(enabled = isPresented) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { event ->
                backProgress = event.progress
            }
            // Gesture committed — snap the peek away so the slide-out starts from
            // rest, then dismiss. The 250ms AnimatedVisibility exit carries the
            // panel off-screen.
            backProgress = 0f
            onDismiss()
        } catch (cancellation: CancellationException) {
            // Gesture cancelled — spring the peek back to rest. Launched on
            // backScope because this gesture coroutine is being cancelled.
            val anim = Animatable(backProgress)
            backScope.launch {
                anim.animateTo(targetValue = 0f, animationSpec = settleSpec) {
                    backProgress = value
                }
            }
            // Re-throw per the PredictiveBackHandler contract so the gesture
            // coroutine finishes as cancelled.
            throw cancellation
        }
    }

    // Inherited overlay-active signal, read BEFORE providing it below so a nested
    // host ORs its own state with every ancestor's (see LocalAMOverlayActive). A
    // FAB hub ABOVE this panel host does not see this — panels are navigation and
    // keep the hub on top; only a FAB in the background scene (below the panel)
    // fades, which is correct since the panel covers it.
    val inheritedOverlayActive = LocalAMOverlayActive.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Background host content — always composed. Reports "an overlay is up"
        // (this panel OR any ancestor host) down to hostContent.
        CompositionLocalProvider(
            LocalAMOverlayActive provides (isPresented || inheritedOverlayActive),
        ) {
            hostContent()
        }

        // Panel overlay — animated slide-in/out from the trailing (right) edge.
        // The full-screen-width slide drops to a snap under Reduce Motion
        // (resolvedFiniteOrNull → null) so the panel appears in place, while the fade
        // softens via resolvedFinite to the gentle 180ms fade.
        AnimatedVisibility(
            visible = isPresented,
            enter = slideInHorizontally(
                animationSpec = tokens.motion.resolvedFiniteOrNull(panelSlideSpec, reduceMotion) ?: snap(),
                initialOffsetX = { fullWidth -> fullWidth },
            ) + fadeIn(
                animationSpec = tokens.motion.resolvedFinite(panelFadeSpec, reduceMotion),
            ),
            exit = slideOutHorizontally(
                animationSpec = tokens.motion.resolvedFiniteOrNull(panelSlideSpec, reduceMotion) ?: snap(),
                targetOffsetX = { fullWidth -> fullWidth },
            ) + fadeOut(
                animationSpec = tokens.motion.resolvedFinite(panelFadeSpec, reduceMotion),
            ),
        ) {
            AMPanel(
                title = title,
                onBack = onDismiss,
                modifier = Modifier
                    .fillMaxSize()
                    // Predictive-back peek: translate toward the trailing edge and
                    // fade slightly as the gesture progresses. At rest (0f) this is
                    // a no-op, so the 250ms slide owns programmatic present/dismiss.
                    .graphicsLayer {
                        translationX = size.width * 0.3f * backProgress
                        alpha = 1f - 0.1f * backProgress
                    }
                    .background(AMTheme.Surface.page.resolve(isDark)),
                trailing = trailing,
                content = panelContent,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun PanelPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    AMPanel(
        title = "Settings",
        onBack = {},
        modifier = Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page.resolve(isDark)),
        trailing = {
            AMIconButton(
                icon = Icons.Default.MoreVert,
                contentDescription = "More options",
                onClick = {},
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tokens.spacing.lg),
        ) {
            BasicText(
                text = "Panel body content goes here. The app bar sits above with a " +
                    "hairline under it, the back arrow leads on the start side, and an " +
                    "optional trailing action occupies the end of the bar.",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

@Preview(
    name = "AMPanel · Light",
    group = "AMPanel",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
    widthDp = 390,
    heightDp = 600,
)
@Composable
private fun AMPanelPreviewLight() {
    AMThemeRoot {
        PanelPreviewContent()
    }
}

@Preview(
    name = "AMPanel · Dark",
    group = "AMPanel",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    widthDp = 390,
    heightDp = 600,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMPanelPreviewDark() {
    AMThemeRoot {
        PanelPreviewContent()
    }
}
