package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Modifier.amBackdropBlur — tint-free Gaussian blur of a wrapped content layer
// ---------------------------------------------------------------------------

/**
 * Applies a tint-free Gaussian blur at [radius] dp to the composable this
 * modifier is attached to, using [BlurEffect] via [graphicsLayer].
 *
 * **Why a Modifier, not a sibling composable:**
 * Pure Compose cannot sample what is behind a sibling node — `BlurEffect`
 * blurs the rendered output of the node it is applied to, not arbitrary
 * background pixels. The canonical pattern is therefore to have the overlay
 * *host* apply this modifier to its [hostContent] layer (the scene content
 * the overlay sits above), not to a sibling placed over it. This is the
 * approach mandated in the Overlays AMENDED ruling 2 (2026-06-11).
 *
 * **Animation-friendly:** [radius] is a plain [Dp]. Pass an animated value
 * produced by `animateDpAsState` — the `graphicsLayer` lambda re-executes on
 * every frame when the captured state changes, producing smooth blur ramps on
 * overlay present/dismiss.
 *
 * **Guard:** when [radius] ≤ 0dp no [BlurEffect] is set. [BlurEffect] requires
 * a strictly positive radius; passing 0 or negative to it crashes at runtime.
 *
 * **Deliberately unclipped:** the layer keeps the `graphicsLayer` default
 * `clip = false`, so the blur paints past the layer bounds (Skia expands the
 * output by ~3σ ≈ 1.7 × radius). When the host wraps a sub-region of the
 * window — e.g. the area below an app top bar — adjacent chrome is softly
 * washed over while the overlay is up, reading as if the whole screen sits
 * under the blur (owner ruling 2026-06-12; do not set `clip = true` here).
 * Note the washed-over chrome is only painted over, not disabled — it keeps
 * receiving taps, and the host's scrim/tap-catcher do not extend over it.
 *
 * **Minimum API:** minSdk 31 (Android 12). [BlurEffect] / [graphicsLayer] render
 * effects are always available in this project and require no version guard.
 *
 * **No haze dependency:** This implementation follows the AMENDED ruling — the
 * `haze` library is not used. If a future gate run reveals that `graphicsLayer
 * { renderEffect = BlurEffect(…) }` does not produce an actual backdrop blur on
 * a target device, see the ruling for the escalation path.
 *
 * iOS equivalent: the Gaussian-blur application inside `PureBlurView.applyPureBlur()` /
 * `installGaussianFilter(on:)` in `Sources/Surfaces/Overlays/BackdropBlur.swift`.
 * The iOS implementation uses a private `CAFilter(gaussianBlur)` to avoid the
 * tint lift that `UIVisualEffectView` standard materials produce over the very
 * dark `IMTheme.Surface.page` (12,12,13). On Android, [BlurEffect] with
 * [TileMode.Clamp] is the direct analogue — no system tint is applied.
 *
 * **Tooling limitation:** Android Studio's interactive preview renderer does
 * not execute `RenderEffect` chains; this modifier renders as a no-op in
 * preview. Validate on a physical device or emulator (API 31+).
 *
 * @param radius Gaussian blur radius in dp. Values ≤ 0 are no-ops.
 */
internal fun Modifier.amBackdropBlur(radius: Dp): Modifier =
    this.graphicsLayer {
        val px = radius.toPx()
        if (px > 0f) {
            renderEffect = BlurEffect(
                radiusX    = px,
                radiusY    = px,
                edgeTreatment = TileMode.Clamp,
            )
            // No clip = true: the blur intentionally bleeds past the layer
            // bounds so chrome adjacent to the host is washed under the blur
            // (owner ruling 2026-06-12).
        }
    }

// ---------------------------------------------------------------------------
// AMBackdropBlur — public, self-contained tint-free Gaussian blur
// ---------------------------------------------------------------------------

/**
 * A tint-free Gaussian blur of the [content] it wraps — the public,
 * self-contained backdrop-blur primitive. The substrate reads as a pure frosted
 * softening: no vibrancy tint and **no scrim in either light or dark mode**, so
 * the content's own color and brightness come through untouched.
 *
 * **Divergence from iOS (documented, platform-mandated):** iOS `IMBackdropBlur`
 * is an overlay `View` you place *over* a sibling substrate — it samples and
 * blurs whatever is rendered *behind* it. Pure Compose cannot sample behind a
 * sibling node ([BlurEffect] blurs the rendered output of the node it is applied
 * to, not arbitrary background pixels; Overlays AMENDED ruling 2, 2026-06-11), so
 * `AMBackdropBlur` blurs the [content] it **wraps** instead of a sibling behind
 * it. Wrap the substrate you want softened:
 *
 * ```kotlin
 * AMBackdropBlur(modifier = Modifier.size(300.dp).clip(shape)) {
 *     PhotoView()          // rendered blurred
 * }
 * ```
 *
 * For the paused-world treatment (blur **plus** a scrim, optionally with a
 * substrate crossfade) reach for [Modifier.amPausedBackdrop] / [AMPausedSubstrate];
 * for a blocking, dismissable layer use [AMActionsOverlay]. `AMBackdropBlur` is
 * the raw blur alone.
 *
 * **Animation-friendly:** [radius] is a plain [Dp]. Pass an animated value from
 * `animateDpAsState` and the blur ramps smoothly (the underlying `graphicsLayer`
 * lambda re-executes per frame).
 *
 * **Guard:** [radius] ≤ 0dp renders [content] crisp — [BlurEffect] requires a
 * strictly positive radius.
 *
 * **Tooling limitation:** Android Studio's interactive preview renderer does not
 * execute `RenderEffect` chains, so the blur is a no-op in `@Preview`; validate
 * on a physical device or emulator (API 31+, always available at this project's
 * minSdk).
 *
 * iOS equivalent: `IMBackdropBlur` in
 * `Sources/Surfaces/Overlays/BackdropBlur.swift`.
 *
 * @param radius   Gaussian blur radius in dp. Defaults to
 *   [AMTheme.Overlay.blurRadius] (20dp) — roughly the system thin material.
 *   Values ≤ 0 render the content crisp.
 * @param modifier Modifier applied to the wrapping [Box]. Clip it to a shape to
 *   contain the blur's edge bleed (the iOS page clips the blur to a rounded rect).
 * @param content  The substrate to blur.
 */
@Composable
fun AMBackdropBlur(
    radius: Dp = AMTheme.Overlay.blurRadius.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.amBackdropBlur(radius)) {
        content()
    }
}

// ---------------------------------------------------------------------------
// AMDarkScrim — dark-mode-only full-screen black scrim
// ---------------------------------------------------------------------------

/**
 * Full-screen [Color.Black] scrim rendered only in dark mode.
 *
 * Pairs with [amBackdropBlur] on modal overlays. The pure-blur backdrop in
 * dark mode shows `AMTheme.Surface.page` (RGB 12,12,13) verbatim — readable
 * but not "paused enough." Layering pure `Color.Black` at [opacity] over the
 * blur pulls the apparent brightness below the substrate without introducing a
 * new tint color.
 *
 * Light mode (242,242,247) reads as a demoted backdrop on its own once
 * foreground content lands — no scrim needed.
 *
 * **Dark-mode detection:** reads [LocalAMIsDarkTheme] (the house rule — NOT
 * `isSystemInDarkTheme()`, per AMENDED ruling 3). This ensures that the demo
 * app's manual light/dark toggle is respected.
 *
 * **Non-interactive:** this composable carries no gesture modifier. Pointer
 * events pass through because no `clickable` or `pointerInput` modifier is
 * attached. Overlay hosts must place their own tap-catcher on top when
 * dismissal on outside-tap is needed.
 *
 * iOS equivalent: `IMDarkScrim` in `Sources/Surfaces/Overlays/BackdropBlur.swift`.
 *
 * @param modifier Modifier to apply to the scrim [Box]. Defaults to
 *   [Modifier.fillMaxSize] applied inside the composable; callers can
 *   override sizing here when a non-full-size scrim is needed.
 * @param opacity  Black scrim alpha. Defaults to [AMTheme.Overlay.scrimOpacity] (0.4f).
 *   Applied only in dark mode; ignored in light mode.
 */
@Composable
internal fun AMDarkScrim(
    modifier: Modifier = Modifier,
    opacity: Float = AMTheme.Overlay.scrimOpacity,
) {
    val isDark = LocalAMIsDarkTheme.current
    if (isDark) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = opacity)),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------
// Note: Android Studio's preview renderer does not execute RenderEffect chains.
// Modifier.amBackdropBlur renders as a no-op in preview — validate on a real
// device or emulator (API 31+). AMDarkScrim previews below show the dark-mode
// scrim overlay in isolation over colorful content.

@Composable
private fun DarkScrimPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page.resolve(isDark)),
    ) {
        // Colorful backing strip to make the scrim effect visible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF5E5CE6)), // indigo stand-in
        )
        AMDarkScrim()
    }
}

@Preview(
    name = "AMDarkScrim · Light",
    group = "AMDarkScrim",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 200,
)
@Composable
private fun AMDarkScrimPreviewLight() {
    AMThemeRoot {
        DarkScrimPreviewContent()
    }
}

@Preview(
    name = "AMDarkScrim · Dark",
    group = "AMDarkScrim",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMDarkScrimPreviewDark() {
    AMThemeRoot {
        DarkScrimPreviewContent()
    }
}

// AMBackdropBlur previews. RenderEffect is a no-op in the preview renderer, so
// the wrapped content shows crisp here — these document the wrap-content shape;
// validate the actual blur on a device/emulator (API 31+).

@Composable
private fun BackdropBlurPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page.resolve(isDark)),
    ) {
        AMBackdropBlur(modifier = Modifier.fillMaxSize()) {
            // Colorful substrate so the tint-free softening is easy to read.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF5E5CE6)), // indigo stand-in
            )
        }
    }
}

@Preview(
    name = "AMBackdropBlur · Light",
    group = "AMBackdropBlur",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 200,
)
@Composable
private fun AMBackdropBlurPreviewLight() {
    AMThemeRoot {
        BackdropBlurPreviewContent()
    }
}

@Preview(
    name = "AMBackdropBlur · Dark",
    group = "AMBackdropBlur",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMBackdropBlurPreviewDark() {
    AMThemeRoot {
        BackdropBlurPreviewContent()
    }
}
