package io.github.jmsdnns.androidmtrls.demo.scenes.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMDivider
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMIconButtonSize
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull

// ---------------------------------------------------------------------------
// Panel transition specs
// ---------------------------------------------------------------------------

/**
 * Duration for the scene panel slide/fade, in ms. Matches `AMPanelHost`'s
 * 250ms so a scene's in-app panels read the same as the library primitive
 * (the panel travels the full screen width).
 */
private const val scenePanelDurationMs = 250

/** Horizontal-offset slide spec for the panel present/dismiss. */
private val panelSlideSpec = tween<IntOffset>(durationMillis = scenePanelDurationMs)

/** Opacity spec paired with [panelSlideSpec] so slide and fade share a duration. */
private val panelFadeSpec = tween<Float>(durationMillis = scenePanelDurationMs)

// ---------------------------------------------------------------------------
// ScenePanelHost — state-driven panel stack over a scene root
// ---------------------------------------------------------------------------

/**
 * Renders a scene's [root] with a **state-driven panel stack** layered over it.
 *
 * The scene owns a `path: List<R>` of pushed routes in its state holder. [root]
 * is always composed and sits behind; whenever [path] is non-empty the **top**
 * route (`path.last()`) renders over it in a full-size [AMTheme.Surface.page]
 * box that slides in from the trailing edge (and back out when it pops). Pushing
 * another route slides the new top over the old; popping reverses it. This is
 * the Compose-idiomatic replacement for a nested `NavHost` — there is no second
 * navigation graph, just a list the scene mutates.
 *
 * A [BackHandler] enabled while [path] is non-empty routes system / predictive
 * back to [onPop], so back peels one panel at a time before the scene itself
 * exits (which the scene's own `LocalSceneExit` handles at path depth 0).
 *
 * The slide/fade honor Reduce Motion via the `:mtrls` motion resolvers
 * (`tokens/Motion.kt`): under Reduce Motion the horizontal slide collapses to a
 * `snap()` (panel appears in place) while the fade softens to the gentle 180ms
 * overlay fade — mirroring `AMPanelHost`.
 *
 * Pair the panel body with [ScenePanelChrome] for the shared Android-native app
 * bar (leading back arrow, left-aligned title, optional trailing action).
 *
 * Usage:
 * ```kotlin
 * ScenePanelHost(
 *     path = state.path,
 *     onPop = { state.pop() },
 *     root = { LibraryView() },
 * ) { route ->
 *     when (route) {
 *         is Route.Album  -> AlbumPanel(route.name)
 *         is Route.Detail -> DetailPanel(route.ids)
 *     }
 * }
 * ```
 *
 * **Instant view-switch** — set [snapNext] `true` on the same recomposition that
 * mutates [path] to make the *next* panel transition land settled with no slide
 * or fade (the Compose twin of iOS `imInstantSwitch`). A scene's FAB nav tiles set
 * it while swapping the root view so a stale panel vanishes instantly rather than
 * sliding out first; ordinary push/pop leave it `false` and keep the slide. The
 * flag is one-shot — the scene clears it right after (a `LaunchedEffect` on the
 * flag), and the settled transition sticks because [path] no longer changes.
 *
 * @param R        The scene's route type (a sealed type in the scene's state holder).
 * @param path     The current panel stack. Empty → only [root] shows.
 * @param onPop    Called to pop the top route (back button / predictive back).
 * @param snapNext When `true`, the next panel transition is instant (no slide/fade).
 * @param modifier Modifier applied to the host's root [Box].
 * @param root     The always-present scene root, behind every panel.
 * @param panel    Renders the panel body for a given top route.
 */
@Composable
fun <R> ScenePanelHost(
    path: List<R>,
    onPop: () -> Unit,
    snapNext: Boolean = false,
    modifier: Modifier = Modifier,
    root: @Composable () -> Unit,
    panel: @Composable (R) -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // System / predictive back pops the top panel before the scene can exit.
    BackHandler(enabled = path.isNotEmpty()) { onPop() }

    Box(modifier = modifier.fillMaxSize()) {
        // Root is always composed and sits behind any panel.
        root()

        // Only the TOP route renders over the root. AnimatedContent is keyed on the
        // top route PAIRED WITH the stack depth (`path.size`) so the transition spec
        // can tell a push from a pop: the route value alone can't, since a mid-stack
        // pop (B→A) and a push (A→B) both just swap which route is on top. Pairing in
        // the depth also drives the transition when only the depth changes, and keeps
        // null↔route sliding — present (depth 0→1) reads as a push, dismiss (1→0) as
        // a pop.
        val top: R? = path.lastOrNull()
        AnimatedContent(
            targetState = top to path.size,
            transitionSpec = {
                // Push (depth grew) reads left-to-right: the incoming panel enters
                // from the trailing edge while the outgoing exits toward the leading
                // edge. Pop (depth shrank) reverses: the revealed panel enters from
                // the leading edge while the popped panel exits toward the trailing
                // edge. Equal depth (a rare same-level replace) is treated as a push.
                // null↔route falls out of this for free: present (0→1) enters from
                // trailing, dismiss (1→0) exits toward trailing — unchanged from before.
                val isPush = targetState.second >= initialState.second
                // snapNext collapses this one transition to nothing (iOS
                // imInstantSwitch): the FAB nav tiles swap the root view and want a
                // stale panel gone instantly, not sliding out first.
                val enter = slideInHorizontally(
                    animationSpec = if (snapNext) snap()
                    else tokens.motion.resolvedFiniteOrNull(panelSlideSpec, reduceMotion) ?: snap(),
                    initialOffsetX = { fullWidth -> if (isPush) fullWidth else -fullWidth },
                ) + fadeIn(
                    animationSpec = if (snapNext) snap()
                    else tokens.motion.resolvedFinite(panelFadeSpec, reduceMotion),
                )
                val exit = slideOutHorizontally(
                    animationSpec = if (snapNext) snap()
                    else tokens.motion.resolvedFiniteOrNull(panelSlideSpec, reduceMotion) ?: snap(),
                    targetOffsetX = { fullWidth -> if (isPush) -fullWidth else fullWidth },
                ) + fadeOut(
                    animationSpec = if (snapNext) snap()
                    else tokens.motion.resolvedFinite(panelFadeSpec, reduceMotion),
                )
                enter togetherWith exit
            },
            label = "ScenePanelHost",
        ) { (route, _) ->
            if (route != null) {
                // Full-size panel surface over the root — opaque page fill so the
                // root does not bleed through during and after the slide. The tap
                // sink consumes taps that land on blank panel areas so they do NOT
                // fall through to the interactive scene root beneath (which mounts a
                // FAB). An empty `pointerInput(Unit) {}` consumes NOTHING — only a
                // gesture detector claims the down — so this uses the same
                // `detectTapGestures {}` sink the library overlays rely on (see the
                // chrome tap-sink in AMActionsOverlay and AMSheet). The panel's own
                // children still receive events first.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AMTheme.Surface.page.resolve(isDark))
                        .pointerInput(Unit) { detectTapGestures {} },
                ) {
                    panel(route)
                }
            } else {
                // Stack empty — transparent full-size box so the root shows through.
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ScenePanelChrome — shared Android-native panel app bar
// ---------------------------------------------------------------------------

/**
 * The shared **Android-native top app bar** for a scene panel — the same anatomy
 * as the library primitive `AMPanel`, so a scene's in-app panels read identically
 * to the standalone panel:
 * - **Leading back arrow** — a borderless [AMIconButton] ([AMIconButtonSize.Medium],
 *   `Icons.AutoMirrored.Filled.ArrowBack`, no surface chip / fill / shadow) that
 *   calls [onBack]. Press feedback is the monochrome `amRipple()` bounded to its
 *   circular touch target, with `Role.Button` + `contentDescription = "Back"`.
 * - **Left-aligned title** — [AMTheme.Type.title] in `Text.primary`, next to the
 *   back arrow with `weight(1f)`, single line, ellipsized. Not centered — Android
 *   bars lead with the title.
 * - **Trailing slot** — optional caller-supplied action at the end of the bar.
 *   Pass `null` (default) for none.
 * - **Hairline under the bar** — a full-bleed [AMDivider] (the "attached app bar"
 *   cue), gated by [divider]. Turn it **off** for chrome that floats transparently
 *   over full-bleed content (a photo, a washed backdrop), where an attached hairline
 *   would draw across the imagery.
 *
 * The bar carries no fill, so the panel's own substrate — an opaque `Surface.page`
 * or a full-bleed backdrop — shows behind it. Unlike `AMPanel`, the chrome does
 * **not** apply `statusBarsPadding()`: it is one bar the scene stacks inside a
 * Column it owns, so the scene call site owns the status-bar inset. Ports the
 * Android re-skin of iOS `imPanelChrome`.
 *
 * @param title    Bar title, rendered left-aligned next to the back arrow.
 * @param onBack   Called when the back arrow is tapped.
 * @param modifier Modifier applied to the chrome's root [Column].
 * @param trailing Optional trailing action (typically a borderless [AMIconButton]).
 *                 Pass `null` (default) for none.
 * @param divider  When `true` (default) a hairline is drawn under the bar. Pass
 *                 `false` for transparent chrome floating over full-bleed content.
 */
@Composable
fun ScenePanelChrome(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
) {
    val isDark = LocalAMIsDarkTheme.current

    Column(modifier = modifier) {
        // ── App bar ─────────────────────────────────────────────────────────
        // Native-height (64dp) top app bar: leading back arrow, left-aligned
        // title, optional trailing action — the same anatomy as AMPanel. No
        // statusBarsPadding() here: the scene call site owns the top inset (the
        // chrome is one bar inside a scene-owned Column, not a full-screen host).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                // 4dp gutter lands the back glyph ~16dp from the edge (the Material
                // navigation-icon inset), matching AMPanel.
                .padding(horizontal = AMTheme.Spacing.xs.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Borderless back arrow — the Android navigation-icon idiom, amRipple
            // bounded to its disc. No surface chip.
            AMIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                size = AMIconButtonSize.Medium,
                contentDescription = "Back",
                onClick = onBack,
            )

            Spacer(modifier = Modifier.width(AMTheme.Spacing.xs.dp))

            // Left-aligned title — weight(1f) claims the slack and pushes any
            // trailing action to the end. Ellipsized single line.
            BasicText(
                text = title,
                modifier = Modifier.weight(1f),
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Trailing action slot — caller-sized. No balancing spacer needed: the
            // title is left-aligned, so nothing has to be kept optically centered.
            if (trailing != null) {
                Spacer(modifier = Modifier.width(AMTheme.Spacing.sm.dp))
                trailing()
            }
        }

        // ── Hairline under the bar ──────────────────────────────────────────
        // The Android "attached app bar" cue, matching AMPanel. Suppressed for
        // chrome that floats transparently over full-bleed content.
        if (divider) {
            AMDivider()
        }
    }
}
