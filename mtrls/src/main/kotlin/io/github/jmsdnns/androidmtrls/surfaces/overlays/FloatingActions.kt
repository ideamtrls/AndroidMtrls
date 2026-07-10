package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFABAlignment
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFloatingActionButton
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolved

// ---------------------------------------------------------------------------
// LocalAMOverlayActive — the "an overlay is up" signal every host reports down
// ---------------------------------------------------------------------------

/**
 * `true` while any AndroidMtrls overlay host **wrapping the reader** is presented
 * — a sheet, banner, toast, alert, actions overlay, loading overlay, or panel.
 * [AMFloatingActionsHost] consumes it to fade its floating button out of the way
 * so a FAB never sits on top of an overlay; a custom floating accessory can read
 * it the same way.
 *
 * **The Compose translation of iOS `IMOverlayActivePreferenceKey`.** On iOS the
 * signal flows **child → parent** as a SwiftUI preference: overlay modifiers
 * `transformPreference` an OR up the tree and `.imFloatingActions` reads the
 * accumulated value with `overlayPreferenceValue`. Compose has no equivalent
 * bottom-up channel for this because AndroidMtrls overlay presenters are **hosts
 * that WRAP their scene content** (they cannot report up from a sibling modifier
 * the way SwiftUI does). So the direction inverts: each host reads the value it
 * *inherited* from its ancestors, ORs in its own presented state, and provides
 * the result **top-down** into its `hostContent`. Nested hosts therefore OR
 * correctly — the innermost content (where a FAB hub lives) sees `true` if *any*
 * ancestor host is presented.
 *
 * **Not `staticCompositionLocalOf`.** The value changes at runtime as overlays
 * present and dismiss; a `compositionLocalOf` recomposes only the readers when it
 * flips, whereas a static local would force the whole provided subtree to
 * recompose on every change.
 *
 * Defaults to `false` so unhosted content (previews, tests, a scene with no
 * overlay above it) reads "no overlay up".
 */
val LocalAMOverlayActive = compositionLocalOf { false }

// ---------------------------------------------------------------------------
// Disabled dim
// ---------------------------------------------------------------------------

/**
 * Whole-button opacity applied to a **disabled** ([AMFloatingActionsHost]
 * `enabled = false`) FAB. iOS greys a disabled `imFloatingActions` FAB with
 * SwiftUI's `.disabled()`, which dims the *entire* view (glyph + fill + shadow)
 * and blocks hits — this mirrors that whole-view dim rather than
 * `AMToolbarButton`'s glyph-only greying, because the public
 * [AMFloatingActionButton] exposes no disabled glyph-tint hook and this host must
 * not reach inside it. The click is gated separately (see the host body).
 */
private const val DISABLED_FAB_ALPHA = 0.4f

// ---------------------------------------------------------------------------
// AMFloatingActionsHost — the scene's floating action hub
// ---------------------------------------------------------------------------

/**
 * The scene's floating action hub: renders [hostContent], floats an
 * [AMFloatingActionButton] above it, and presents an actions [menu] above the
 * button when tapped. The Compose port of iOS `imFloatingActions`.
 *
 * **Scene-root pattern.** Wrap a scene's whole content **once** in this host and
 * put the scene's actions in [menu] as [AMActionGrid] rows — including an "Exit"
 * tile when the scene needs one. The host presents [menu] through an internal
 * [AMActionsOverlay], so the menu rides the signature blurred, scrimmed
 * free-floating layer; fill it with [AMActionGridButton]s at their **default**
 * elevation — each tile self-cards and floats over the blur, like iOS.
 *
 * **Z-order — content < FAB < menu overlay.** The FAB draws above [hostContent],
 * and the [AMActionsOverlay] chrome (blur + scrim + floating tiles) draws above
 * the FAB, so the menu always beats the button.
 *
 * **Overlays always beat the FAB.** The button fades out — and once fully faded is
 * removed from the composition entirely, so it stops responding and drops out of
 * the TalkBack focus order instead of lingering as an inert alpha-0 node that still
 * eats taps — whenever an overlay is up: its own [menu] (via [isPresented]) *or*
 * any overlay host wrapping this one (a sheet, banner, toast, alert, loading
 * overlay, …), read through [LocalAMOverlayActive]. Attach order can't break it:
 * overlays take the user's attention and a floating button never sits on top of
 * one. The fade uses [AMTheme.Motion.overlayFade] routed through the house
 * Reduce-Motion policy (instant under [LocalAMReduceMotion]).
 *
 * **Disabled.** `enabled = false` dims the whole button to [DISABLED_FAB_ALPHA],
 * gates its click — a disabled hub can't open its menu — and marks the button
 * `disabled()` in semantics so TalkBack announces it as disabled rather than an
 * actionable button that does nothing. This mirrors iOS's `.disabled()` whole-view
 * dim (see [DISABLED_FAB_ALPHA]).
 *
 * **Placement.** The FAB rests at the bottom of the scene at [alignment]
 * (default [AMFABAlignment.Center] — the iOS scene-hub default; note a *bare*
 * [AMFloatingActionButton] keeps its own `Trailing` default), lifted by
 * [bottomInset] (e.g. above a batch-selection toolbar). [offset] is the
 * caller-owned drag position (see [AMFloatingActionButton]) — `null` keeps the
 * button static.
 *
 * @param isPresented        `true` while the actions [menu] is showing.
 * @param onPresentChange    Toggles [menu] visibility — the FAB calls
 *                           `onPresentChange(true)`, an overlay dismissal calls
 *                           `onPresentChange(false)`.
 * @param alignment          Horizontal anchor for the resting FAB.
 * @param bottomInset        Extra bottom lift for the resting FAB.
 * @param enabled            `false` dims the FAB and blocks its click.
 * @param offset             `null` (default) → static FAB; a caller-owned state →
 *                           draggable FAB (hoist it above overlay transitions).
 * @param accessibilityLabel Label announced for the FAB.
 * @param modifier           Applied to the host root.
 * @param menu               The actions menu — typically [AMActionGrid] rows.
 * @param hostContent        The scene content this host wraps. Trailing lambda.
 *
 * iOS equivalent: `View.imFloatingActions(isPresented:alignment:bottomInset:disabled:offset:accessibilityLabel:menu:)`
 * in `Sources/Controls/Buttons/FloatingActionButton.swift`.
 */
@Composable
fun AMFloatingActionsHost(
    isPresented: Boolean,
    onPresentChange: (Boolean) -> Unit,
    alignment: AMFABAlignment = AMFABAlignment.Center,
    bottomInset: Dp = 0.dp,
    enabled: Boolean = true,
    offset: MutableState<Offset>? = null,
    accessibilityLabel: String = "Show actions",
    modifier: Modifier = Modifier,
    menu: @Composable () -> Unit,
    hostContent: @Composable () -> Unit,
) {
    // The menu presents through AMActionsOverlay, whose hostContent is [scene +
    // FAB]. AMActionsOverlay also provides LocalAMOverlayActive = (isPresented ||
    // inherited) into that hostContent (see LocalAMOverlayActive's KDoc), so the
    // FAB reads its own menu state AND any ancestor overlay in a single local.
    AMActionsOverlay(
        isPresented    = isPresented,
        onDismiss      = { onPresentChange(false) },
        modifier       = modifier,
        overlayContent = menu,
    ) {
        val tokens = LocalAMTokens.current
        val reduceMotion = LocalAMReduceMotion.current

        // Read the overlay-active signal at THIS scope — inside AMActionsOverlay's
        // hostContent, so LocalAMOverlayActive.current already ORs in this host's
        // own isPresented plus every ancestor host. `|| isPresented` is kept for an
        // explicit, self-evident condition (it is redundant given the provider).
        val overlayActive = LocalAMOverlayActive.current || isPresented

        // The FAB is interactive only when enabled AND no overlay is up.
        val interactive = enabled && !overlayActive

        // Fade the FAB to 0 while any overlay is up so it never sits on an overlay;
        // dim (not hide) an idle disabled FAB; else full opacity. Routed through the
        // overlayFade token via the house Reduce-Motion resolver (instant under RM).
        val targetAlpha = when {
            overlayActive -> 0f
            !enabled      -> DISABLED_FAB_ALPHA
            else          -> 1f
        }
        val fabAlpha by animateFloatAsState(
            targetValue   = targetAlpha,
            animationSpec = tokens.motion.resolved(AMTheme.Motion.overlayFade, reduceMotion),
            label         = "AMFloatingActionsHost.fabAlpha",
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // Scene content — beneath the FAB (z-order: content < FAB).
            hostContent()

            // FAB layer — anchored to the scene bottom and lifted by bottomInset.
            // The FAB's own fillMaxWidth row owns horizontal alignment and its
            // horizontal inset; it carries no built-in bottom inset (iOS parity),
            // so this wrapper's bottomInset is the lift mechanism — it pins the FAB
            // to the bottom edge, adds bottomInset, and carries the
            // visibility/disabled alpha.
            //
            // Composed only while the fade leaves it visible (fabAlpha > 0). The
            // fade-out still renders frame-by-frame, but once the alpha settles at
            // exactly 0 (an overlay is up) the FAB leaves the tree entirely — an
            // alpha-0 graphicsLayer node would otherwise keep its 56dp footprint,
            // stay a focusable "Show actions" TalkBack node, and silently swallow
            // taps. Mirrors iOS `fabLayer`'s `if !hidden { … }`, which drops the
            // button from the view tree when an overlay is up.
            if (fabAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = bottomInset)
                        .graphicsLayer { alpha = fabAlpha }
                        // Expose the disabled state to accessibility alongside the
                        // dim + click-gate so a disabled hub reads as disabled to
                        // TalkBack instead of an actionable-but-inert button.
                        .then(if (!enabled) Modifier.semantics { disabled() } else Modifier),
                ) {
                    AMFloatingActionButton(
                        alignment          = alignment,
                        offset             = offset,
                        accessibilityLabel = accessibilityLabel,
                        // Click gated on visibility + enabled: a disabled FAB still
                        // consumes its own tap but does nothing (a hidden FAB is no
                        // longer composed — see the fabAlpha gate above), so it can
                        // never open the menu while an overlay is up or when disabled.
                        onClick            = { if (interactive) onPresentChange(true) },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
//
// Note: AMFloatingActionsHost presents its menu through AMActionsOverlay, whose
// Modifier.amBackdropBlur is a no-op in the Android Studio preview renderer
// (RenderEffect is not executed). With isPresented = false the previews simply
// show the resting FAB over placeholder scene rows; validate the blur/menu on a
// physical device or emulator (API 31+).
// ---------------------------------------------------------------------------

@Composable
private fun FloatingActionsPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val fabOffset = remember { mutableStateOf(Offset.Zero) }

    // Bounded height so the internal fillMaxSize scene Box (and the bottom-anchored
    // FAB) measure against a finite viewport in the preview.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp),
    ) {
        AMFloatingActionsHost(
            isPresented     = false,
            onPresentChange = {},
            offset          = fabOffset,
            menu = {
                AMActionGrid {
                    AMActionGridRow(
                        tileA = {
                            AMActionGridButton(
                                icon = Icons.Default.Edit,
                                title = "Compose",
                                onClick = {},
                            )
                        },
                        tileB = {
                            AMActionGridButton(
                                icon = Icons.Default.Search,
                                title = "Search",
                                onClick = {},
                            )
                        },
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AMTheme.Surface.page.resolve(isDark))
                    .padding(AMTheme.Spacing.lg.dp),
            ) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = AMTheme.Spacing.sm.dp)
                            .background(
                                color = AMTheme.Surface.primary.resolve(isDark),
                                shape = amSquircle(AMTheme.Radius.lg.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text  = "Placeholder row ${i + 1}",
                            style = AMTheme.Type.secondary
                                .toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "AMFloatingActionsHost · Light",
    group = "AMFloatingActionsHost",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 420,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMFloatingActionsHostPreviewLight() {
    AMThemeRoot {
        FloatingActionsPreviewContent()
    }
}

@Preview(
    name = "AMFloatingActionsHost · Dark",
    group = "AMFloatingActionsHost",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 420,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMFloatingActionsHostPreviewDark() {
    AMThemeRoot {
        FloatingActionsPreviewContent()
    }
}
