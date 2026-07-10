package io.github.jmsdnns.androidmtrls.tokens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath

// ---------------------------------------------------------------------------
// Squircle — the continuous-corner rounded rectangle
// ---------------------------------------------------------------------------

/**
 * A continuous-corner (squircle) rounded rectangle [Shape] — the Compose
 * counterpart of iOS `RoundedRectangle(cornerRadius: radius, style: .continuous)`.
 *
 * iOSMtrls draws every floating surface (cards, pills, inputs, sheets) with the
 * `.continuous` corner style: a superellipse whose curvature eases into the
 * straight edges rather than meeting them at the abrupt tangent of a circular
 * arc. This factory reproduces that curve on Android by rounding a
 * [RoundedPolygon.rectangle] with a [CornerRounding] whose `smoothing` factor
 * extends the rounded region into the flat edges — the visual signature that a
 * plain [androidx.compose.foundation.shape.RoundedCornerShape] (circular arcs)
 * cannot produce.
 *
 * The returned shape:
 * - **scales to the composable's size** — the outline is rebuilt from the actual
 *   pixel [Size] passed to [Shape.createOutline], so one shape instance fits any
 *   box it clips, backgrounds, or borders;
 * - **is correct for any aspect ratio** — the corner radius is applied in pixel
 *   space (not by non-uniform scaling of a normalized unit square), so a wide or
 *   tall rectangle keeps perfectly circular-radius corners instead of stretched
 *   ellipses. The radius is clamped to half the shorter side so it never
 *   over-rounds a small box;
 * - **caches its path per size** — the generated [Path] is memoized against the
 *   last `(size, density)` and only rebuilt when either changes, so repeated
 *   layout/draw passes at a stable size do no extra polygon work;
 * - **works as a `clip`, `background`, and `border` shape** anywhere a Compose
 *   [Shape] is accepted, and casts a matching squircle shadow through
 *   `Modifier.amElevation` (which draws `shape.createOutline(...)` as a generic
 *   path).
 *
 * Instances with equal `(radius, smoothing)` compare equal, so passing
 * `amSquircle(radius)` inline still lets Compose skip re-clipping across
 * recompositions.
 *
 * ```kotlin
 * Box(
 *     Modifier
 *         .size(120.dp, 64.dp)
 *         .clip(amSquircle(AMTheme.Radius.lg.dp))
 *         .background(AMTheme.Surface.primary.resolve(isDark))
 * )
 * ```
 *
 * @param radius    Corner radius in [Dp]. Clamped per size to half the shorter
 *                  side.
 * @param smoothing Continuous-corner smoothing, `0f` (a plain circular-arc
 *                  corner) to `1f` (maximally extended). The default `0.6f`
 *                  approximates Apple's `.continuous` curve.
 */
fun amSquircle(radius: Dp, smoothing: Float = 0.6f): Shape =
    AMSquircleShape(radius = radius, smoothing = smoothing)

/**
 * A continuous-corner (squircle) rounded rectangle [Shape] with an independent
 * radius per corner — the Compose counterpart of iOS
 * `UnevenRoundedRectangle(cornerRadii:, style: .continuous)`.
 *
 * Used where a surface rounds only some corners: `AMSheet`, for instance, rounds
 * its two top corners and squares the two bottom corners so it seats flush
 * against the screen edge. Each corner carries the same [smoothing] as the
 * uniform [amSquircle] so the eased curve matches the rest of the system.
 *
 * Radii are named in the same start/end, clockwise-from-top order as
 * [androidx.compose.foundation.shape.RoundedCornerShape]'s per-corner
 * constructor, and are **layout-direction aware**: `topStart`/`bottomStart`
 * resolve to the left edge in LTR and the right edge in RTL. A radius of `0.dp`
 * produces a sharp corner. Each radius is clamped per size to half the shorter
 * side.
 *
 * Backed by [RoundedPolygon.rectangle]'s `perVertexRounding`, so — like the
 * uniform factory — the shape scales to the composable's size, stays uniform at
 * any aspect ratio, memoizes its path, and casts a matching squircle shadow
 * through `Modifier.amElevation` (which draws `shape.createOutline(...)` as a
 * generic path, exact even for the mixed sharp/rounded silhouette).
 *
 * Instances with equal radii and [smoothing] compare equal, so passing the shape
 * inline still lets Compose skip re-clipping across recompositions.
 *
 * @param topStart    Top leading corner radius (left in LTR, right in RTL).
 * @param topEnd      Top trailing corner radius (right in LTR, left in RTL).
 * @param bottomEnd   Bottom trailing corner radius (right in LTR, left in RTL).
 * @param bottomStart Bottom leading corner radius (left in LTR, right in RTL).
 * @param smoothing   Continuous-corner smoothing; default `0.6f` matches
 *                    [amSquircle].
 */
fun amSquircle(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp,
    smoothing: Float = 0.6f,
): Shape = AMSquirclePerCornerShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart,
    smoothing = smoothing,
)

/**
 * Builds the size-space [RoundedPolygon] backing [amSquircle] — a rectangle
 * spanning `[0, width] × [0, height]` with continuous ([smoothing]-eased)
 * corners of [radiusPx], clamped to half the shorter side.
 *
 * Extracted from [AMSquircleShape.createOutline] so the size/aspect-ratio
 * behaviour is exercisable in a pure-JVM unit test (the polygon and its
 * `calculateBounds()` are framework-free; only the later `toPath()` bridge needs
 * Android). Works in pixel space so corners stay uniform at any aspect ratio.
 */
internal fun amSquircleRectanglePolygon(
    width: Float,
    height: Float,
    radiusPx: Float,
    smoothing: Float
): RoundedPolygon {
    val clampedRadius = radiusPx.coerceIn(0f, minOf(width, height) / 2f)
    return RoundedPolygon.rectangle(
        width = width,
        height = height,
        rounding = CornerRounding(radius = clampedRadius, smoothing = smoothing),
        centerX = width / 2f,
        centerY = height / 2f
    )
}

/**
 * Builds the size-space [RoundedPolygon] backing the per-corner [amSquircle] — a
 * rectangle spanning `[0, width] × [0, height]` whose four corners round to their
 * own **already-resolved visual** radius (left/right, not start/end) with
 * continuous ([smoothing]-eased) corners, each clamped to half the shorter side.
 *
 * The layout-direction → left/right resolution happens in the [Shape] before
 * calling this, so the helper stays framework-free and pure-JVM testable (the
 * polygon and its `calculateBounds()` need no Android; only the later `toPath()`
 * bridge does). Works in pixel space so corners stay uniform at any aspect ratio.
 *
 * Corner-to-vertex mapping follows [RoundedPolygon.rectangle]'s vertex order
 * `[bottomRight, bottomLeft, topLeft, topRight]`, with the visual top at `y = 0`.
 */
internal fun amSquircleRectanglePolygonPerCorner(
    width: Float,
    height: Float,
    topLeftPx: Float,
    topRightPx: Float,
    bottomRightPx: Float,
    bottomLeftPx: Float,
    smoothing: Float
): RoundedPolygon {
    val maxRadius = minOf(width, height) / 2f
    fun rounding(px: Float) =
        CornerRounding(radius = px.coerceIn(0f, maxRadius), smoothing = smoothing)
    return RoundedPolygon.rectangle(
        width = width,
        height = height,
        perVertexRounding = listOf(
            rounding(bottomRightPx),
            rounding(bottomLeftPx),
            rounding(topLeftPx),
            rounding(topRightPx)
        ),
        centerX = width / 2f,
        centerY = height / 2f
    )
}

/**
 * The [Shape] implementation returned by [amSquircle].
 *
 * Value is `(radius, smoothing)`; equality is defined over those two fields so
 * the shape is a stable Compose parameter. The cached path is transparent
 * memoization — not part of the value — and a fresh [Path] is built on each
 * rebuild so an already-returned [Outline] never sees its path mutated.
 */
private class AMSquircleShape(
    private val radius: Dp,
    private val smoothing: Float
) : Shape {

    // Per-size memo: the last built outline path plus the (size, density) it was
    // built for. Rebuilt only when either input changes.
    private var cachedPath: Path? = null
    private var cachedSize: Size = Size.Unspecified
    private var cachedDensity: Float = Float.NaN

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val densityValue = density.density
        val memo = cachedPath
        if (memo != null && cachedSize == size && cachedDensity == densityValue) {
            return Outline.Generic(memo)
        }

        val radiusPx = with(density) { radius.toPx() }
        val path = amSquircleRectanglePolygon(
            width = size.width,
            height = size.height,
            radiusPx = radiusPx,
            smoothing = smoothing
        ).toPath().asComposePath()

        cachedPath = path
        cachedSize = size
        cachedDensity = densityValue
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AMSquircleShape) return false
        return radius == other.radius && smoothing == other.smoothing
    }

    override fun hashCode(): Int = 31 * radius.hashCode() + smoothing.hashCode()

    override fun toString(): String = "AMSquircleShape(radius=$radius, smoothing=$smoothing)"
}

/**
 * The [Shape] implementation returned by the per-corner [amSquircle].
 *
 * Value is the four start/end radii plus `smoothing`; equality is defined over
 * those five fields so the shape is a stable Compose parameter. Because the
 * outline depends on layout direction (start/end map to different edges under
 * RTL), the per-size path memo also keys on `layoutDirection`.
 */
private class AMSquirclePerCornerShape(
    private val topStart: Dp,
    private val topEnd: Dp,
    private val bottomEnd: Dp,
    private val bottomStart: Dp,
    private val smoothing: Float
) : Shape {

    private var cachedPath: Path? = null
    private var cachedSize: Size = Size.Unspecified
    private var cachedDensity: Float = Float.NaN
    private var cachedLayoutDirection: LayoutDirection? = null

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val densityValue = density.density
        val memo = cachedPath
        if (memo != null &&
            cachedSize == size &&
            cachedDensity == densityValue &&
            cachedLayoutDirection == layoutDirection
        ) {
            return Outline.Generic(memo)
        }

        // Resolve start/end to visual left/right for the current layout direction.
        val ltr = layoutDirection == LayoutDirection.Ltr
        val topLeft     = if (ltr) topStart else topEnd
        val topRight    = if (ltr) topEnd else topStart
        val bottomLeft  = if (ltr) bottomStart else bottomEnd
        val bottomRight = if (ltr) bottomEnd else bottomStart

        val path = with(density) {
            amSquircleRectanglePolygonPerCorner(
                width = size.width,
                height = size.height,
                topLeftPx = topLeft.toPx(),
                topRightPx = topRight.toPx(),
                bottomRightPx = bottomRight.toPx(),
                bottomLeftPx = bottomLeft.toPx(),
                smoothing = smoothing
            )
        }.toPath().asComposePath()

        cachedPath = path
        cachedSize = size
        cachedDensity = densityValue
        cachedLayoutDirection = layoutDirection
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AMSquirclePerCornerShape) return false
        return topStart == other.topStart &&
            topEnd == other.topEnd &&
            bottomEnd == other.bottomEnd &&
            bottomStart == other.bottomStart &&
            smoothing == other.smoothing
    }

    override fun hashCode(): Int {
        var result = topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + smoothing.hashCode()
        return result
    }

    override fun toString(): String =
        "AMSquirclePerCornerShape(topStart=$topStart, topEnd=$topEnd, " +
            "bottomEnd=$bottomEnd, bottomStart=$bottomStart, smoothing=$smoothing)"
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "amSquircle · Light",
    group = "amSquircle",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7  // Surface.page light
)
@Composable
private fun AMSquirclePreviewLight() {
    AMThemeRoot { AMSquirclePreviewContent() }
}

@Preview(
    name = "amSquircle · Dark",
    group = "amSquircle",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AMSquirclePreviewDark() {
    AMThemeRoot { AMSquirclePreviewContent() }
}

@Composable
private fun AMSquirclePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val fill = AMTheme.Surface.primary.resolve(isDark)

    Column(
        modifier = Modifier.padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)
    ) {
        // Square specimens across the radius scale.
        Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
            listOf(
                AMTheme.Radius.sm.dp,
                AMTheme.Radius.md.dp,
                AMTheme.Radius.lg.dp
            ).forEach { r ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(amSquircle(r))
                        .background(fill)
                )
            }
        }

        // Wide aspect ratio — corners must stay uniform, not stretched.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(220.dp)
                .height(56.dp)
                .clip(amSquircle(AMTheme.Radius.lg.dp))
                .background(fill)
        )
    }
}
