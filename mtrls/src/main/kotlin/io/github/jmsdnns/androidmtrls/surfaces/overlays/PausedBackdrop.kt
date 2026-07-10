package io.github.jmsdnns.androidmtrls.surfaces.overlays

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMCrossfade
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// The "paused world" backdrop — blur + scrim behind foreground content.
//
// The Compose translation of iOS `imPausedBackdrop` / `IMPausedScrim`
// (Sources/Surfaces/Overlays/PausedBackdrop.swift). It bundles the internal
// `amBackdropBlur` and a scrim into one decorative treatment so call sites stop
// hand-rolling the stack (and the crossfade that goes with it) when they want a
// substrate to read as *stilled* while foreground content sits above it.
//
// This is the DECORATIVE backdrop only: it is full-bleed and non-interactive
// and adds no tap-catcher, input blocker, or `isPresented` gate. It is distinct
// from the modal overlay hosts (`AMAlert`/`AMLoadingOverlay`/`AMActionsOverlay`)
// which own dismissal and modality — reach for those when you need a blocking,
// dismissable layer.
// ---------------------------------------------------------------------------

/**
 * Opacity of the [AMPausedScrim.Page] recipe's `Surface.page` wash — enough to
 * demote the blurred substrate to a backdrop while letting its color bleed
 * through.
 *
 * Deliberately a private constant, **not** an `AMTheme.Overlay` token: it
 * mirrors iOS's private `pausedPageWashOpacity` so a theme override cannot
 * distort the photo-detail wash.
 */
private const val PAUSED_PAGE_WASH_OPACITY = 0.55f

/**
 * Which scrim recipe [Modifier.amPausedBackdrop] / [AMPausedSubstrate] lay over
 * the blur.
 *
 * iOS equivalent: `IMPausedScrim`.
 */
@Immutable
enum class AMPausedScrim {
    /**
     * Canonical modal backdrop: blur + a **dark-mode-only** black scrim at
     * [AMTheme.Overlay.scrimOpacity]. Light mode shows the blur alone, exactly
     * matching what [AMAlert] / [AMLoadingOverlay] / [AMActionsOverlay] render
     * under their content (the [AMDarkScrim] recipe).
     */
    Modal,

    /**
     * Photo-detail wash: blur + a translucent [AMTheme.Surface.page] tint at
     * [PAUSED_PAGE_WASH_OPACITY], so the substrate's color bleeds through while
     * the page still reads as itself.
     */
    Page,
}

/**
 * Resolves the flat scrim color for [scrim] in the current theme. Reused by the
 * static [Modifier.amPausedBackdrop] draw and (for [AMPausedScrim.Page]) the
 * crossfading [AMPausedSubstrate] wash. [AMPausedScrim.Modal] returns a
 * fully-transparent color in light mode (the scrim composes nothing), matching
 * the [AMDarkScrim] recipe.
 */
private fun pausedScrimColor(scrim: AMPausedScrim, isDark: Boolean): Color =
    when (scrim) {
        AMPausedScrim.Modal ->
            if (isDark) Color.Black.copy(alpha = AMTheme.Overlay.scrimOpacity)
            else Color.Transparent
        AMPausedScrim.Page ->
            AMTheme.Surface.page.resolve(isDark).copy(alpha = PAUSED_PAGE_WASH_OPACITY)
    }

// ---------------------------------------------------------------------------
// Modifier.amPausedBackdrop — static paused backdrop
// ---------------------------------------------------------------------------

/**
 * Turn the node this modifier is attached to into a paused-world backdrop: blur
 * its content and lay a scrim over it, non-interactive.
 *
 * The node's **content** (its children, or drawing chained *after* this
 * modifier) is the substrate — it is blurred via the internal [amBackdropBlur],
 * then the [scrim] is drawn crisply on top. Pure Compose cannot sample behind a
 * sibling, so — like every blurring host in this package — the blur attaches to
 * the layer it decorates rather than floating over it (Overlays AMENDED ruling
 * 2). Attach it to a scene-filling substrate:
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize().amPausedBackdrop(AMPausedScrim.Page)) {
 *     PhotoView()          // blurred + washed
 * }
 * DetailPanel(...)         // stack crisp, interactive foreground above it
 * ```
 *
 * This modifier adds **no** pointer input — it is purely decorative, and taps
 * pass through to whatever the caller stacks above the backdrop. It is distinct
 * from the modal overlay hosts ([AMAlert] / [AMLoadingOverlay] /
 * [AMActionsOverlay]): there is no tap-catcher, no input blocker, and no
 * modality. When the substrate changes identity behind the blur (a gallery
 * swapping the photo), reach for [AMPausedSubstrate] instead — it owns the
 * cross-dissolve.
 *
 * **Blur note:** [amBackdropBlur] uses [androidx.compose.ui.graphics.BlurEffect]
 * via `graphicsLayer`, which Android Studio's preview renderer does not execute
 * — the blur renders as a no-op in `@Preview`; the scrim still draws. Validate
 * the blur on a physical device or emulator (API 31+).
 *
 * iOS equivalent: `View.imPausedBackdrop(_:blurRadius:)`.
 *
 * @param scrim      Which scrim recipe to layer over the blur. Defaults to
 *                   [AMPausedScrim.Modal].
 * @param blurRadius Gaussian blur radius. Defaults to
 *                   [AMTheme.Overlay.blurRadius] (20dp).
 */
fun Modifier.amPausedBackdrop(
    scrim: AMPausedScrim = AMPausedScrim.Modal,
    blurRadius: Dp = AMTheme.Overlay.blurRadius.dp,
): Modifier = composed {
    val isDark = LocalAMIsDarkTheme.current
    val scrimColor = pausedScrimColor(scrim, isDark)
    // Order matters: `drawWithContent` sits OUTSIDE `amBackdropBlur`'s
    // graphicsLayer, so `drawContent()` paints the already-blurred substrate and
    // the scrim rect draws crisply on top (not swept into the blur). The scrim
    // reproduces the AMDarkScrim / page-wash recipe here because a Modifier
    // cannot compose the AMDarkScrim/Box the crossfading form uses.
    this
        .drawWithContent {
            drawContent()
            if (scrimColor.alpha > 0f) drawRect(scrimColor)
        }
        .amBackdropBlur(blurRadius)
}

// ---------------------------------------------------------------------------
// AMPausedSubstrate — crossfading paused backdrop
// ---------------------------------------------------------------------------

/**
 * A paused-world backdrop that also owns the substrate cross-dissolve — the
 * photo-detail form. When [key] changes, [content] crossfades to its new value
 * behind a **stable** blur + scrim, so a gallery swapping the photo under a
 * detail view writes no animation code:
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     AMPausedSubstrate(key = currentPhoto.id, scrim = AMPausedScrim.Page) { id ->
 *         PhotoView(id)     // cross-dissolves on id change, blurred + washed
 *     }
 *     DetailPanel(...)      // crisp, interactive foreground above the backdrop
 * }
 * ```
 *
 * The crossfade is the house [AMCrossfade] curve — 250ms `EaseInOut`, and
 * **instant under Reduce Motion** ([io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion]).
 * The timing is fixed, not a theme-overridable token, so it cannot be distorted
 * (mirrors iOS's private `pausedSubstrateCrossfade`).
 *
 * **Why the blur does not re-ramp on [key] change:** on Android the blur must
 * attach to the layer it decorates (it cannot sample behind a sibling). It is
 * applied here to a fixed inner [Box] that wraps the crossfade and is never
 * keyed on [key], so its `graphicsLayer` keeps stable identity and re-samples
 * the cross-dissolving content live rather than tearing down and re-building
 * (which would flash the blur on every swap). The scrim likewise stays a stable
 * sibling above the blurred substrate.
 *
 * Non-interactive and decorative, like [Modifier.amPausedBackdrop]. Pass a
 * **bounded**, scene-filling [modifier] (e.g. `Modifier.fillMaxSize()`) — the
 * substrate and scrim both fill the node, so an unbounded modifier collapses
 * the backdrop.
 *
 * **Blur note:** the blur is a no-op in Android Studio `@Preview`; validate on a
 * device or emulator (API 31+).
 *
 * iOS equivalent: `View.imPausedBackdrop(_:blurRadius:crossfading:)`.
 *
 * @param key        Identity of the current substrate; a change crossfades
 *                   [content] to its new value.
 * @param scrim      Which scrim recipe to layer over the blur. Defaults to
 *                   [AMPausedScrim.Modal].
 * @param blurRadius Gaussian blur radius. Defaults to
 *                   [AMTheme.Overlay.blurRadius] (20dp).
 * @param modifier   Modifier applied to the backdrop container. Pass a bounded,
 *                   scene-filling value.
 * @param content    Renders the substrate for a given [key] value; should fill
 *                   the backdrop (e.g. `Modifier.fillMaxSize()`).
 */
@Composable
fun <T> AMPausedSubstrate(
    key: T,
    scrim: AMPausedScrim = AMPausedScrim.Modal,
    blurRadius: Dp = AMTheme.Overlay.blurRadius.dp,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier) {
        // The substrate crossfades on `key` inside a stable blur layer. Placing
        // amBackdropBlur on this fixed inner Box (never keyed on `key`) keeps the
        // blur's graphicsLayer identity stable, so it re-samples the
        // cross-dissolving content live without re-ramping or flashing on swap.
        Box(modifier = Modifier.fillMaxSize().amBackdropBlur(blurRadius)) {
            AMCrossfade(targetState = key) { current ->
                content(current)
            }
        }
        // Scrim stays stable above the blurred substrate — not blurred, not keyed.
        PausedScrimLayer(scrim)
    }
}

/**
 * Renders the scrim for [scrim] as a real composable layer (the crossfading
 * form can compose, unlike the static [Modifier.amPausedBackdrop] draw).
 * [AMPausedScrim.Modal] reuses the internal [AMDarkScrim]; [AMPausedScrim.Page]
 * is a translucent [AMTheme.Surface.page] wash. Fills the parent node — the
 * caller supplies bounded constraints.
 */
@Composable
private fun PausedScrimLayer(scrim: AMPausedScrim) {
    when (scrim) {
        AMPausedScrim.Modal -> AMDarkScrim()
        AMPausedScrim.Page -> {
            val isDark = LocalAMIsDarkTheme.current
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pausedScrimColor(AMPausedScrim.Page, isDark)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------
// Note: Android Studio's preview renderer does not execute RenderEffect chains,
// so the blur is a no-op here — the scrim still draws. The Modal recipe shows
// the scrim only in dark mode (matching AMDarkScrim); Page shows the wash in
// both. Validate the blur on a device/emulator (API 31+).

/** Colorful substrate so the tint-free blur and the two scrim recipes read apart. */
private val previewSubstrateBrush: Brush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF5E5CE6), // indigo
        0.35f to Color(0xFFAF52DE), // purple
        0.7f to Color(0xFFFF2D55), // pink
        1.0f to Color(0xFFFF9500), // orange
    ),
)

@Composable
private fun PausedBackdropPreviewContent(scrim: AMPausedScrim) {
    val isDark = LocalAMIsDarkTheme.current
    Box(modifier = Modifier.fillMaxSize()) {
        // Paused backdrop: substrate child is blurred + scrimmed.
        Box(modifier = Modifier.fillMaxSize().amPausedBackdrop(scrim)) {
            Box(modifier = Modifier.fillMaxSize().background(previewSubstrateBrush))
        }

        // Crisp, interactive-looking foreground above the backdrop.
        AMCard(
            padding = AMPadding.block,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 280.dp)
                .padding(AMTheme.Spacing.xl.dp),
        ) {
            Column {
                BasicText(
                    text = if (scrim == AMPausedScrim.Modal) "Modal" else "Page",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                BasicText(
                    text = "Foreground stays crisp over the paused backdrop.",
                    style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    modifier = Modifier.padding(top = AMTheme.Spacing.sm.dp),
                )
            }
        }
    }
}

@Preview(
    name = "AMPausedBackdrop · Modal · Light",
    group = "AMPausedBackdrop",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 280,
)
@Composable
private fun AMPausedBackdropPreviewModalLight() {
    AMThemeRoot {
        Box(modifier = Modifier.fillMaxSize().clip(amSquircle(AMTheme.Radius.lg.dp))) {
            PausedBackdropPreviewContent(scrim = AMPausedScrim.Modal)
        }
    }
}

@Preview(
    name = "AMPausedBackdrop · Page · Light",
    group = "AMPausedBackdrop",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    widthDp = 360,
    heightDp = 280,
)
@Composable
private fun AMPausedBackdropPreviewPageLight() {
    AMThemeRoot {
        Box(modifier = Modifier.fillMaxSize().clip(amSquircle(AMTheme.Radius.lg.dp))) {
            PausedBackdropPreviewContent(scrim = AMPausedScrim.Page)
        }
    }
}

@Preview(
    name = "AMPausedBackdrop · Modal · Dark",
    group = "AMPausedBackdrop",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 280,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMPausedBackdropPreviewModalDark() {
    AMThemeRoot {
        Box(modifier = Modifier.fillMaxSize().clip(amSquircle(AMTheme.Radius.lg.dp))) {
            PausedBackdropPreviewContent(scrim = AMPausedScrim.Modal)
        }
    }
}

@Preview(
    name = "AMPausedBackdrop · Page · Dark",
    group = "AMPausedBackdrop",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    widthDp = 360,
    heightDp = 280,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMPausedBackdropPreviewPageDark() {
    AMThemeRoot {
        Box(modifier = Modifier.fillMaxSize().clip(amSquircle(AMTheme.Radius.lg.dp))) {
            PausedBackdropPreviewContent(scrim = AMPausedScrim.Page)
        }
    }
}
