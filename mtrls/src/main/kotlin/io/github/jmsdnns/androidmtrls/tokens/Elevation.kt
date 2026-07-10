package io.github.jmsdnns.androidmtrls.tokens

import android.content.res.Configuration
import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// AMElevation — elevation levels for the two-layer drop-shadow recipe
// ---------------------------------------------------------------------------

/**
 * Elevation levels for AndroidMtrls floating surfaces.
 *
 * Each level except [None] renders as two stacked blurred shadows — a tight
 * **near** layer close to the surface and a softer, wider **far** layer
 * further out — matching the iOSMtrls `IMElevation` recipe exactly.
 *
 * [None] draws with zero radius and transparent colors rather than branching
 * in the modifier, so view identity stays stable across elevation transitions
 * and animated level changes remain smooth.
 *
 * Apply via [Modifier.amElevation].
 *
 * iOS equivalent: `IMElevation`.
 */
@Immutable
enum class AMElevation {
    /**
     * No shadow. For tile grids and card bands where per-item shadows would
     * stack into visual noise. The modifier still runs its draw pass so
     * elevation transitions animate without composition changes.
     *
     * iOS equivalent: `IMElevation.none`.
     */
    None,

    /**
     * Default surface elevation for row cards, action grid buttons, and
     * text fields.
     *
     * Near: radius 4dp, y-offset 2dp, [AMTheme.Shadow.near].
     * Far:  radius 16dp, y-offset 10dp, [AMTheme.Shadow.far].
     *
     * iOS equivalent: `IMElevation.low`.
     */
    Low,

    /**
     * Deeper lift for bottom sheets and prominent overlays — sits higher
     * off the page than a [Low] row card.
     *
     * Near: radius 6dp, y-offset 4dp, [AMTheme.Shadow.near].
     * Far:  radius 28dp, y-offset 18dp, [AMTheme.Shadow.far].
     *
     * iOS equivalent: `IMElevation.medium`.
     */
    Medium,

    /**
     * Text-legibility shadow for floating labels (e.g., section headers)
     * that must remain readable over blur substrates or busy backgrounds.
     * Tuned tighter than surface shadows; uses fixed (non-adaptive) colors.
     *
     * Near: radius 3dp, y-offset 1dp, [AMTheme.Shadow.textNear] (fixed 12% black).
     * Far:  radius 10dp, y-offset 4dp, [AMTheme.Shadow.textFar] (fixed 10% black).
     *
     * iOS equivalent: `IMElevation.text`.
     */
    Text,
}

// ---------------------------------------------------------------------------
// Internal per-layer descriptor
// ---------------------------------------------------------------------------

/**
 * A single resolved shadow layer ready for canvas drawing.
 *
 * All measurements are in **pixels** — converted from dp inside the draw
 * scope where [Density] is available.
 *
 * @param radiusPx Gaussian blur radius in pixels.
 * @param yOffsetPx Downward shadow offset in pixels (positive = down).
 * @param color Fully resolved [Color] including alpha channel.
 */
private data class ElevationLayer(
    val radiusPx: Float,
    val yOffsetPx: Float,
    val color: Color,
)

/** Resolve the near shadow layer for this level given the current dark-mode flag. */
private fun AMElevation.nearLayer(isDark: Boolean, density: Density): ElevationLayer =
    with(density) {
        when (this@nearLayer) {
            AMElevation.None   -> ElevationLayer(0f, 0f, Color.Transparent)
            AMElevation.Low    -> ElevationLayer(4.dp.toPx(),  2.dp.toPx(),  AMTheme.Shadow.near.resolve(isDark))
            AMElevation.Medium -> ElevationLayer(6.dp.toPx(),  4.dp.toPx(),  AMTheme.Shadow.near.resolve(isDark))
            AMElevation.Text   -> ElevationLayer(3.dp.toPx(),  1.dp.toPx(),  AMTheme.Shadow.textNear)
        }
    }

/** Resolve the far shadow layer for this level given the current dark-mode flag. */
private fun AMElevation.farLayer(isDark: Boolean, density: Density): ElevationLayer =
    with(density) {
        when (this@farLayer) {
            AMElevation.None   -> ElevationLayer(0f,  0f,  Color.Transparent)
            AMElevation.Low    -> ElevationLayer(16.dp.toPx(), 10.dp.toPx(), AMTheme.Shadow.far.resolve(isDark))
            AMElevation.Medium -> ElevationLayer(28.dp.toPx(), 18.dp.toPx(), AMTheme.Shadow.far.resolve(isDark))
            AMElevation.Text   -> ElevationLayer(10.dp.toPx(),  4.dp.toPx(), AMTheme.Shadow.textFar)
        }
    }

// ---------------------------------------------------------------------------
// Modifier.amElevation
// ---------------------------------------------------------------------------

/**
 * Apply an AndroidMtrls two-layer drop shadow behind the composable.
 *
 * Draws two blurred rounded-rect shadows — far layer first (underneath),
 * then the near layer on top — using [BlurMaskFilter] inside
 * [Modifier.drawBehind]. This directly maps the iOSMtrls `(radius, y, color)`
 * recipe and supports custom, alpha-bearing shadow colors including the
 * adaptive dark-mode pairs from [AMTheme.Shadow].
 *
 * Dark-mode adaptation is handled by capturing [LocalAMIsDarkTheme] inside
 * [Modifier.composed], which gives composable scope for the CompositionLocal
 * read. The resolved [Boolean] is then passed into the draw lambda, which
 * runs outside composable scope.
 *
 * **Elevation table (dp values, verbatim from `IMElevation`):**
 *
 * | Level  | Near radius / y-offset | Far radius / y-offset |
 * |--------|------------------------|-----------------------|
 * | None   | 0dp / 0dp (transparent)| 0dp / 0dp (transparent)|
 * | Low    | 4dp / 2dp              | 16dp / 10dp            |
 * | Medium | 6dp / 4dp              | 28dp / 18dp            |
 * | Text   | 3dp / 1dp              | 10dp / 4dp             |
 *
 * **Shadow colors:**
 * - `Low` / `Medium` near layer → [AMTheme.Shadow.near]:
 *   light alpha 0.10, dark alpha 0.55.
 * - `Low` / `Medium` far layer → [AMTheme.Shadow.far]:
 *   light alpha 0.08, dark alpha 0.45.
 * - `Text` near layer → [AMTheme.Shadow.textNear]: fixed 0.12 (non-adaptive).
 * - `Text` far layer → [AMTheme.Shadow.textFar]: fixed 0.10 (non-adaptive).
 *
 * This modifier does **not** use `Modifier.shadow`: Android elevation semantics
 * do not map cleanly to the iOS `(radius, y)` recipe, and `Modifier.shadow`
 * does not accept custom colors. See foundations spec §3.2 for the full
 * option analysis.
 *
 * @param level Elevation tier to apply. Defaults to [AMElevation.Low].
 * @param shape Shape of the surface; shadows follow this silhouette.
 *   Defaults to [AMTheme.Radius.md] rounded corners — what most row cards use.
 *   Explicit cards should pass `amSquircle(AMTheme.Radius.lg.dp)`.
 */
fun Modifier.amElevation(
    level: AMElevation = AMElevation.Low,
    shape: Shape = amSquircle(AMTheme.Radius.md.dp),
): Modifier = composed {
    // Capture the dark-mode flag here in composable scope (Modifier.composed
    // runs during composition). The draw lambda below is NOT composable scope
    // and cannot read CompositionLocals directly.
    val isDark by rememberUpdatedState(LocalAMIsDarkTheme.current)

    drawBehind {
        val nearLayer = level.nearLayer(isDark, this)
        val farLayer  = level.farLayer(isDark, this)

        drawIntoCanvas { canvas ->
            // Draw far layer first (under near) so the tighter near shadow
            // sits visually on top of the wider far shadow — matching the
            // two-layer stacking in iOSMtrls.
            for (layer in listOf(farLayer, nearLayer)) {
                // Skip zero layers (None level) to avoid allocating a paint
                // with a zero-radius BlurMaskFilter.
                if (layer.radiusPx == 0f) continue

                val paint = Paint()
                @Suppress("DEPRECATION")
                paint.asFrameworkPaint().apply {
                    isAntiAlias = true
                    // Color must be set on the framework paint so the full ARGB
                    // value (including alpha) reaches BlurMaskFilter. The Compose
                    // Paint color is not consulted when a maskFilter is active.
                    // asFrameworkPaint() is the only stable API for attaching a
                    // BlurMaskFilter to a Compose Paint; the deprecation is noted
                    // but no alternative exists yet for this specific use case.
                    color = layer.color.toArgb()
                    maskFilter = BlurMaskFilter(layer.radiusPx, BlurMaskFilter.Blur.NORMAL)
                }

                // Translate downward by the y-offset so the blur kernel is
                // centred at the shadow's target position, then draw the shape.
                canvas.save()
                canvas.translate(0f, layer.yOffsetPx)

                when (val outline = shape.createOutline(size, layoutDirection, this)) {
                    is Outline.Rectangle -> canvas.drawRect(outline.rect, paint)
                    is Outline.Rounded   -> {
                        val rr = outline.roundRect
                        canvas.drawRoundRect(
                            left    = rr.left,
                            top     = rr.top,
                            right   = rr.right,
                            bottom  = rr.bottom,
                            radiusX = rr.topLeftCornerRadius.x,
                            radiusY = rr.topLeftCornerRadius.y,
                            paint   = paint,
                        )
                    }
                    is Outline.Generic   -> canvas.drawPath(outline.path, paint)
                }

                canvas.restore()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark, all elevation levels on Surface.page background
// ---------------------------------------------------------------------------

@Composable
private fun ElevationPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val pageColor    = AMTheme.Surface.page.resolve(isDark)
    val surfaceColor = AMTheme.Surface.primary.resolve(isDark)

    Column(
        modifier = Modifier
            .background(pageColor)
            .padding(AMTheme.Spacing.xl.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
    ) {
        for ((elev, _) in listOf(
            AMElevation.Medium to "Medium",
            AMElevation.Low    to "Low",
            AMElevation.Text   to "Text",
            AMElevation.None   to "None",
        )) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AMTheme.rowMinHeight.dp)
                    // Shadow drawn behind the filled surface so the blur
                    // bleeds onto the page background and is visible.
                    .amElevation(level = elev, shape = amSquircle(AMTheme.Radius.lg.dp))
                    .background(
                        color = surfaceColor,
                        shape = amSquircle(AMTheme.Radius.lg.dp),
                    ),
            )
        }
    }
}

@Preview(
    name = "AMElevation · Light",
    group = "AMElevation",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7, // AMTheme.Surface.page light
)
@Composable
private fun ElevationPreviewLight() {
    AMThemeRoot {
        ElevationPreviewContent()
    }
}

@Preview(
    name = "AMElevation · Dark",
    group = "AMElevation",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D, // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ElevationPreviewDark() {
    AMThemeRoot {
        ElevationPreviewContent()
    }
}
