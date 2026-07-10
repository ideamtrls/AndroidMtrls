package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Pure track-math helpers (internal — unit-testable without Compose runtime)
// ---------------------------------------------------------------------------

/**
 * Normalizes [value] within [[start], [end]] to a fraction in [0, 1].
 *
 * Values outside the range are clamped so the fraction never exceeds [0, 1].
 * Drives the handle-centre geometry in the draw pass.
 *
 * @param value The raw slider value.
 * @param start The range's lower bound.
 * @param end   The range's upper bound.
 * @return Normalized fraction in [0, 1].
 */
internal fun amSliderValueToFraction(value: Float, start: Float, end: Float): Float {
    if (end <= start) return 0f
    return ((value - start) / (end - start)).coerceIn(0f, 1f)
}

/**
 * Converts a normalized [fraction] in [0, 1] to a value within [[start], [end]].
 *
 * @param fraction Normalized position in [0, 1].
 * @param start    The range's lower bound.
 * @param end      The range's upper bound.
 * @return The corresponding value in the range.
 */
internal fun amSliderFractionToValue(fraction: Float, start: Float, end: Float): Float {
    return start + fraction * (end - start)
}

/**
 * Clamps [value] to the closed interval [[start], [end]].
 *
 * @param value The value to clamp.
 * @param start The lower bound.
 * @param end   The upper bound.
 * @return The value clamped to [start, end].
 */
internal fun amSliderClampValue(value: Float, start: Float, end: Float): Float {
    return value.coerceIn(start, end)
}

/**
 * Converts a horizontal drag delta in pixels to a value delta within [[start], [end]].
 *
 * The mapping is linear: [trackWidthPx] pixels of drag spans the full range.
 *
 * @param dragDeltaPx  The horizontal drag delta in pixels (positive = rightward).
 * @param trackWidthPx The full usable width of the track in pixels.
 * @param start        The range's lower bound.
 * @param end          The range's upper bound.
 * @return The value delta corresponding to the drag.
 */
internal fun amSliderDragDeltaToValueDelta(
    dragDeltaPx: Float,
    trackWidthPx: Float,
    start: Float,
    end: Float,
): Float {
    if (trackWidthPx <= 0f) return 0f
    val fraction = dragDeltaPx / trackWidthPx
    return fraction * (end - start)
}

/**
 * Maps an ABSOLUTE horizontal finger position to a slider value.
 *
 * Both tap-to-jump and absolute drag positioning use this: the value is
 * `positionPx / trackWidthPx` (clamped to `[0, 1]`) mapped across
 * [valueRange]. Absolute mapping is safe for the slider because the Canvas
 * node itself never moves (unlike the draggable FAB), so the read position is
 * always in stable node-local coordinates.
 *
 * @param positionPx   The finger's x position in track-local pixels.
 * @param trackWidthPx The usable track width in pixels. `<= 0` yields
 *                     [valueRange]`.start` (the degenerate / unmeasured guard).
 * @param valueRange   The closed range of valid values.
 * @return The value at [positionPx], clamped into [valueRange].
 */
internal fun amSliderPositionToValue(
    positionPx: Float,
    trackWidthPx: Float,
    valueRange: ClosedFloatingPointRange<Float>,
): Float {
    if (trackWidthPx <= 0f) return valueRange.start
    val fraction = (positionPx / trackWidthPx).coerceIn(0f, 1f)
    return valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
}

// ---------------------------------------------------------------------------
// Track & handle metrics (file-private — Material 3 slider anatomy)
// ---------------------------------------------------------------------------

/** Height of the thick rounded track in dp (the Material 3 slider track). */
private val trackHeight = 16.dp

/** Track corner radius — fully rounds the 16dp track height into capsule caps. */
private val trackRadius = 8.dp

/** Width of the narrow vertical handle in dp (the Material 3 handle). */
private val handleWidth = 4.dp

/** Height of the narrow vertical handle in dp — a tall capsule crossing the track. */
private val handleHeight = 44.dp

/** Handle corner radius — fully rounds the 4dp width into a capsule. */
private val handleRadius = 2.dp

/** Clear gap between each side of the handle and the adjacent track segment. */
private val trackGap = 6.dp

/** Radius of the stop-indicator dot in dp (4dp diameter). */
private val stopDotRadius = 2.dp

/** Canvas / touch-target height in dp — sized to the tall handle. */
private val sliderHeight = 44.dp

// ---------------------------------------------------------------------------
// AMSliderTrack — internal Canvas-based split track + narrow handle
// ---------------------------------------------------------------------------

/**
 * Custom slider track drawn via [Canvas] + [pointerInput], by hand with
 * foundation primitives (no Material3).
 *
 * Material 3 slider anatomy: a thick ([trackHeight]) fully-rounded track split
 * into two capsule segments — an active portion (leading edge → the handle
 * notch) filled in the live semantic accent (`tokens.semantic.accent`, the
 * overridable primary tint) and an inactive portion (past the notch → trailing
 * edge) in the neutral control-fill grey. A narrow vertical accent handle
 * ([handleWidth]×[handleHeight]) sits in the [trackGap] notch between the two
 * segments (no drop shadow), and a stop-indicator dot marks the trailing end —
 * accent-colored on the inactive grey, flipping to the on-accent contrast tone
 * when the value reaches the extreme and the handle passes it.
 *
 * Positioning is ABSOLUTE: a tap jumps the handle to the finger, and a horizontal
 * drag positions the handle at the finger on every change. This is safe because
 * the Canvas node never moves — no delta-accumulator is needed (contrast the
 * draggable FAB, which must use relative `positionChange`).
 *
 * Accessibility: the track carries [ProgressBarRangeInfo] plus a `setProgress`
 * action so TalkBack reads the value ("50%") and can adjust it, and (when a
 * headline [label] exists) names the control via `contentDescription` — the
 * accessibility SwiftUI's native `Slider` provides for free on iOS.
 *
 * The value↔position mapping uses the pure helpers [amSliderValueToFraction]
 * (draw pass) and [amSliderPositionToValue] (gestures), both testable without a
 * Compose runtime. [trackWidthPx] is written in the draw pass and read in the
 * gesture coroutines (the house draw→gesture bridge).
 *
 * @param value       The current slider value.
 * @param onValueChange  Callback invoked when the user taps or drags.
 * @param valueRange  The closed range of valid values.
 * @param modifier    Modifier applied to the track canvas.
 * @param label       Optional headline label — associated as the track's
 *                    accessibility name so TalkBack announces it.
 * @param isDark      Current dark-theme state for color resolution.
 */
@Composable
private fun AMSliderTrack(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    label: String? = null,
    isDark: Boolean = LocalAMIsDarkTheme.current,
) {
    val tokens = LocalAMTokens.current
    val rangeStart = valueRange.start
    val rangeEnd   = valueRange.endInclusive

    // Colors resolved once at composition boundary — safe for use inside the
    // Canvas draw lambda (not composable scope). The active fill and handle are
    // the live, overridable semantic accent (the primary tint), so they flow
    // with the demo's dynamic-accent toggle.
    val accentColor   = tokens.semantic.accent.resolve(isDark)
    // Inactive track: the neutral control-fill grey shared by the segmented /
    // stepper / toggle fills — Black/White @ 8%, computed straight from isDark
    // for the draw scope (the neutralFill recipe).
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    // On-accent contrast tone for the stop dot when it sits on the accent-filled
    // segment (value at the extreme) — reads against the accent like M3's
    // on-primary stop indicator.
    val onAccentColor = AMTheme.Surface.primary.resolve(isDark)

    // Usable track width captured during the Canvas draw pass for use in the
    // gesture handlers (the draw→gesture bridge). Written inside the draw lambda
    // (single draw thread), read inside the pointerInput coroutines — a plain
    // mutableFloatStateOf suffices since writes trigger no recomposition.
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    // Keep the gesture blocks calling the latest onValueChange — the pointerInput
    // loops are keyed on (rangeStart, rangeEnd) only (geometry math depends on
    // them), so they would otherwise capture a stale onValueChange when the caller
    // passes a new lambda without changing the range.
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    Canvas(
        modifier = modifier
            .heightIn(min = sliderHeight)
            // Accessibility: expose value + adjust action (progressBarRangeInfo +
            // setProgress) so TalkBack can read and change the slider, and name
            // the control with the headline label when present.
            .semantics(mergeDescendants = true) {
                if (label != null) contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    onValueChange(target.coerceIn(rangeStart, rangeEnd))
                    true
                }
            }
            // Tap-to-jump: absolute position → value.
            .pointerInput(rangeStart, rangeEnd) {
                detectTapGestures { offset ->
                    currentOnValueChange(amSliderPositionToValue(offset.x, trackWidthPx, valueRange))
                }
            }
            // Absolute drag positioning: the handle chases the finger. No
            // delta-accumulator — the node is stationary so position→value is
            // read directly on every change.
            .pointerInput(rangeStart, rangeEnd) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentOnValueChange(amSliderPositionToValue(change.position.x, trackWidthPx, valueRange))
                }
            },
    ) {
        val width      = size.width
        val handleWPx  = handleWidth.toPx()
        val handleHalf = handleWPx / 2f

        // Handle centre travels between handleHalf and width − handleHalf, so the
        // usable span excludes the handle's own footprint.
        val usableWidth = (width - handleWPx).coerceAtLeast(0f)
        trackWidthPx = usableWidth

        val fraction = amSliderValueToFraction(value, rangeStart, rangeEnd)
        val centerX  = handleHalf + fraction * usableWidth
        val centerY  = size.height / 2f

        drawSliderTrack(
            centerX         = centerX,
            width           = width,
            centerY         = centerY,
            handleHalfPx    = handleHalf,
            gapPx           = trackGap.toPx(),
            trackHeightPx   = trackHeight.toPx(),
            trackRadiusPx   = trackRadius.toPx(),
            stopDotRadiusPx = stopDotRadius.toPx(),
            activeColor     = accentColor,
            inactiveColor   = inactiveColor,
            onActiveColor   = onAccentColor,
        )

        drawSliderHandle(
            centerX        = centerX,
            centerY        = centerY,
            handleWidthPx  = handleWPx,
            handleHeightPx = handleHeight.toPx(),
            cornerRadiusPx = handleRadius.toPx(),
            color          = accentColor,
        )
    }
}

/**
 * Draws the split track and its trailing stop indicator.
 *
 * The active segment (accent) runs from the leading edge to just before the
 * handle notch; the inactive segment (neutral grey) runs from past the notch to
 * the trailing edge. Both are fully-rounded capsules, so the [gapPx] notch on
 * each side of the handle reads as a clean break — the handle sits in the gap,
 * not on a continuous rail.
 *
 * The stop-indicator dot marks the trailing end. It normally reads on the
 * inactive grey ([activeColor]); when the value reaches the extreme and the
 * handle passes it, the trailing end is the accent-filled segment, so the dot
 * flips to the [onActiveColor] contrast tone (M3 style).
 */
private fun DrawScope.drawSliderTrack(
    centerX: Float,
    width: Float,
    centerY: Float,
    handleHalfPx: Float,
    gapPx: Float,
    trackHeightPx: Float,
    trackRadiusPx: Float,
    stopDotRadiusPx: Float,
    activeColor: Color,
    inactiveColor: Color,
    onActiveColor: Color,
) {
    val top    = centerY - trackHeightPx / 2f
    val corner = CornerRadius(trackRadiusPx)

    // Active segment: leading edge → just before the handle notch.
    val activeEnd = (centerX - handleHalfPx - gapPx).coerceAtLeast(0f)
    if (activeEnd > 0f) {
        drawRoundRect(
            color        = activeColor,
            topLeft      = Offset(0f, top),
            size         = Size(activeEnd, trackHeightPx),
            cornerRadius = corner,
        )
    }

    // Inactive segment: just after the handle notch → trailing edge.
    val inactiveStart = (centerX + handleHalfPx + gapPx).coerceAtMost(width)
    if (inactiveStart < width) {
        drawRoundRect(
            color        = inactiveColor,
            topLeft      = Offset(inactiveStart, top),
            size         = Size(width - inactiveStart, trackHeightPx),
            cornerRadius = corner,
        )
    }

    // Stop indicator: a dot centred on the trailing rounded cap. Accent-colored
    // while it sits on the inactive grey; on-accent once the handle has passed
    // it (value at the extreme, so the trailing end is the active segment).
    val stopDotCx  = width - trackHeightPx / 2f
    val onInactive = stopDotCx >= inactiveStart
    drawCircle(
        color  = if (onInactive) activeColor else onActiveColor,
        radius = stopDotRadiusPx,
        center = Offset(stopDotCx, centerY),
    )
}

/**
 * Draws the narrow vertical handle — a tall accent capsule ([handleWidthPx] ×
 * [handleHeightPx]) centred at [centerX], sitting in the track notch. No drop
 * shadow, no white pill; the handle carries the accent tint like the active
 * track fill.
 */
private fun DrawScope.drawSliderHandle(
    centerX: Float,
    centerY: Float,
    handleWidthPx: Float,
    handleHeightPx: Float,
    cornerRadiusPx: Float,
    color: Color,
) {
    val left = centerX - handleWidthPx / 2f
    val top  = centerY - handleHeightPx / 2f
    drawRoundRect(
        color        = color,
        topLeft      = Offset(left, top),
        size         = Size(handleWidthPx, handleHeightPx),
        cornerRadius = CornerRadius(cornerRadiusPx),
    )
}

// ---------------------------------------------------------------------------
// AMSliderDecoration — one decoration per side (icon or text, never both)
// ---------------------------------------------------------------------------

/**
 * A single decoration flanking a slider track — an icon or a short text readout.
 *
 * Modeling each side as one value (rather than separate icon/text params) makes
 * the ambiguous "both an icon and text on one side" state unrepresentable: a
 * side is an [Icon] or a [Text], never both. This mirrors the iOS
 * `IMSliderDecoration` enum's type-level guarantee.
 *
 * @see AMSlider
 */
@Immutable
sealed interface AMSliderDecoration {

    /** An icon, rendered 20dp in the muted icon color ([AMTheme.Icon.muted]). */
    @Immutable
    data class Icon(val icon: ImageVector) : AMSliderDecoration

    /**
     * A short text readout, rendered caption-sized with monospaced digits in the
     * secondary text color ([AMTheme.Text.secondary]). Mirrors iOS
     * `.caption.monospacedDigit()`.
     */
    @Immutable
    data class Text(val text: String) : AMSliderDecoration
}

/**
 * Internal renderer for an [AMSliderDecoration] with the fixed slider styling,
 * keeping the icon/text rendering out of the generic slider body. Mirrors iOS
 * `IMSliderDecorationLabel`.
 *
 * @param decoration The icon or text decoration to render.
 */
@Composable
private fun AMSliderDecorationLabel(decoration: AMSliderDecoration) {
    val isDark = LocalAMIsDarkTheme.current
    when (decoration) {
        is AMSliderDecoration.Text -> {
            // Caption with tabular (monospaced) digits — mirrors iOS
            // `.caption.monospacedDigit()`: digits stay column-aligned while
            // non-digits (e.g. the "m:ss" colon) keep their natural proportional
            // width. A fully monospaced font would instead pad the colon into its
            // own wide cell, reading as "0 : 00".
            BasicText(
                text  = decoration.text,
                style = AMTheme.Type.caption
                    .toTextStyle(AMTheme.Text.secondary.resolve(isDark))
                    .copy(fontFeatureSettings = "tnum"),
            )
        }
        is AMSliderDecoration.Icon -> {
            // Icon at muted tint — uses `androidx.compose.foundation.Image` with
            // a tint ColorFilter, mirroring the house pattern in Tag.kt (AMTagIcon).
            // No Material3 `Icon` composable; `:mtrls` is Material3-free.
            Image(
                imageVector        = decoration.icon,
                contentDescription = null,
                colorFilter        = ColorFilter.tint(AMTheme.Icon.muted.resolve(isDark)),
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AMSlider — primary composable overload (custom leading/trailing slots)
// ---------------------------------------------------------------------------

/**
 * Flat row slider widget with optional leading/trailing decoration slots and
 * an optional headline label above the track.
 *
 * A flat row widget — the caller's card/section provides the surface.
 * No card wrap inside the composable; wrap with `AMCard` at the call site.
 *
 * The track is drawn with a custom [Canvas] + [pointerInput] implementation
 * (Material 3 slider anatomy — a thick split track, a narrow vertical accent
 * handle, and a trailing stop indicator — drawn by hand with foundation
 * primitives, no Material3). Positioning is absolute: a tap jumps the handle,
 * and a horizontal drag positions it at the finger. Value↔position mapping uses
 * the pure helpers [amSliderValueToFraction] and [amSliderPositionToValue], both
 * testable without a Compose runtime.
 *
 * iOS equivalent: `IMSlider<Leading, Trailing>` in
 * `Sources/Controls/Pickers/Slider.swift`.
 *
 * Usage (no decorations):
 * ```kotlin
 * var volume by rememberSaveable { mutableFloatStateOf(0.6f) }
 * AMSlider(
 *     value        = volume,
 *     onValueChange = { volume = it },
 *     valueRange   = 0f..1f,
 *     label        = "Volume",
 * )
 * ```
 *
 * Usage (with custom leading/trailing):
 * ```kotlin
 * AMSlider(
 *     value        = volume,
 *     onValueChange = { volume = it },
 *     valueRange   = 0f..1f,
 *     leading      = { Icon(Icons.Default.VolumeDown, contentDescription = null) },
 *     trailing     = { Icon(Icons.Default.VolumeUp,   contentDescription = null) },
 * )
 * ```
 *
 * @param value         The current slider value. Must be within [valueRange].
 * @param onValueChange Callback invoked when the user taps or drags the track.
 *                      Only writes state up — no internal state is kept.
 * @param valueRange    The closed range of valid values (inclusive on both ends).
 * @param label         Optional headline string rendered above the track row.
 *                      When non-null the label renders in body weight at
 *                      [AMTheme.Text.primary] and names the track for TalkBack.
 * @param leading       Composable rendered to the left of the track. Pass `{}`
 *                      for no decoration. Required (no default) so this slot
 *                      overload stays distinct from the [AMSliderDecoration]
 *                      convenience overload — a bare call with no slots resolves
 *                      to the convenience overload (no decorations).
 * @param trailing      Composable rendered to the right of the track. Pass `{}`
 *                      for no decoration.
 * @param modifier      Modifier applied to the outermost layout node.
 */
@Composable
fun AMSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark  = LocalAMIsDarkTheme.current
    val tokens  = LocalAMTokens.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.lg)
            .padding(vertical = tokens.spacing.md)
            .heightIn(min = AMTheme.rowMinHeight.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (label != null) {
            BasicText(
                text  = label,
                style = AMTheme.Type.body.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                modifier = Modifier.padding(bottom = tokens.spacing.sm),
            )
        }

        Row(
            verticalAlignment      = Alignment.CenterVertically,
            horizontalArrangement  = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            leading()
            AMSliderTrack(
                value         = value,
                onValueChange = onValueChange,
                valueRange    = valueRange,
                modifier      = Modifier.weight(1f),
                label         = label,
                isDark        = isDark,
            )
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------
// AMSlider — convenience overload (icon/text decorations)
// ---------------------------------------------------------------------------

/**
 * Flat row slider widget using one [AMSliderDecoration] per side for the
 * leading/trailing decorations.
 *
 * Each side is a single optional [AMSliderDecoration] ([AMSliderDecoration.Icon]
 * or [AMSliderDecoration.Text]), so a side can never carry both an icon and text
 * at once — the ambiguity the old flat-param API allowed is now unrepresentable,
 * matching iOS's `IMSliderDecoration` enum. Omit a side for no decoration.
 *
 * iOS equivalent: `IMSlider` convenience `init(leading:trailing:)` taking one
 * `IMSliderDecoration?` per side in `Sources/Controls/Pickers/Slider.swift`.
 *
 * Usage:
 * ```kotlin
 * AMSlider(
 *     value         = volume,
 *     onValueChange  = { volume = it },
 *     valueRange    = 0f..1f,
 *     leading       = AMSliderDecoration.Icon(Icons.Default.VolumeDown),
 *     trailing      = AMSliderDecoration.Icon(Icons.Default.VolumeUp),
 * )
 * ```
 *
 * @param value         The current slider value. Must be within [valueRange].
 * @param onValueChange Callback invoked when the user taps or drags the track.
 * @param valueRange    The closed range of valid values.
 * @param label         Optional headline string above the track.
 * @param leading       Optional [AMSliderDecoration] rendered left of the track.
 * @param trailing      Optional [AMSliderDecoration] rendered right of the track.
 * @param modifier      Modifier applied to the outermost layout node.
 */
@Composable
fun AMSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String? = null,
    leading: AMSliderDecoration? = null,
    trailing: AMSliderDecoration? = null,
    modifier: Modifier = Modifier,
) {
    AMSlider(
        value         = value,
        onValueChange  = onValueChange,
        valueRange    = valueRange,
        label         = label,
        leading       = { leading?.let { AMSliderDecorationLabel(it) } },
        trailing      = { trailing?.let { AMSliderDecorationLabel(it) } },
        modifier      = modifier,
    )
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun SliderPreviewContent() {
    val tokens = LocalAMTokens.current

    // Standalone preview values — not remembered across Android recompositions
    // but sufficient for static preview rendering.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Mid-value with label + text decorations.
        AMSlider(
            value         = 0.5f,
            onValueChange  = {},
            valueRange    = 0f..1f,
            label         = "Volume",
            leading       = AMSliderDecoration.Text("0"),
            trailing      = AMSliderDecoration.Text("1"),
        )
        // Near-max value.
        AMSlider(
            value         = 99f,
            onValueChange  = {},
            valueRange    = 0f..100f,
            label         = "Brightness",
            leading       = AMSliderDecoration.Text("0"),
            trailing      = AMSliderDecoration.Text("100"),
        )
        // Bare track, no label or decorations, low value.
        AMSlider(
            value         = 0.25f,
            onValueChange  = {},
            valueRange    = 0f..1f,
        )
    }
}

@Preview(
    name            = "AMSlider · Light",
    group           = "AMSlider",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
    widthDp         = 390,
)
@Composable
private fun AMSliderPreviewLight() {
    AMThemeRoot {
        SliderPreviewContent()
    }
}

@Preview(
    name            = "AMSlider · Dark",
    group           = "AMSlider",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    widthDp         = 390,
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSliderPreviewDark() {
    AMThemeRoot {
        SliderPreviewContent()
    }
}
