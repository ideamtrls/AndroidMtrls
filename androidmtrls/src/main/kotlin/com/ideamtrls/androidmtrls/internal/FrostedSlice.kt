package com.ideamtrls.androidmtrls

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A frosted surface's view of the art behind it: the backdrop's slice under the surface, blurred,
 * drawn clipped to the surface's shape. It owns one layer, which records a reference to the
 * backdrop's layer, so new art (a photo arriving) shows without re-recording.
 */
internal class FrostedSlice {
    var layer: GraphicsLayer? = null
    private var effect: BlurEffect? = null
    private var effectRadius = -1f

    /**
     * Draws [source]'s art under the surface at [surface], blurred by [radius] px and clipped to
     * [shape]. Returns false, drawing nothing, while there's nothing to sample yet (the art or
     * the surface isn't placed, or they don't overlap); the caller then draws the solid surface.
     */
    fun draw(scope: DrawScope, shape: Path, source: AMBackdropSource, surface: LayoutCoordinates?, radius: Float): Boolean {
        val layer = layer ?: return false
        val art = source.coordinates?.takeIf { it.isAttached } ?: return false
        val placed = surface?.takeIf { it.isAttached } ?: return false
        val offset = art.localPositionOf(placed, Offset.Zero)

        // The slice reaches past the surface, so the blur near its edge mixes in what's really
        // there; at the art's own edge, clamping repeats real pixels instead of transparency.
        val region = frostedSampleRegion(
            x = offset.x,
            y = offset.y,
            width = scope.size.width,
            height = scope.size.height,
            margin = radius * 2f,
            artWidth = art.size.width.toFloat(),
            artHeight = art.size.height.toFloat(),
        ) ?: return false

        if (radius != effectRadius) {
            effectRadius = radius
            effect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Clamp) else null
        }
        layer.renderEffect = effect
        layer.topLeft = IntOffset((region.left - offset.x).roundToInt(), (region.top - offset.y).roundToInt())
        with(scope) {
            layer.record(IntSize(region.width, region.height)) {
                translate(-region.left.toFloat(), -region.top.toFloat()) { drawLayer(source.layer) }
            }
            clipPath(shape) { drawLayer(layer) }
        }
        return true
    }
}

/** A rectangle of the art in whole pixels, in the art's own coordinates. */
internal class SampleRegion(val left: Int, val top: Int, val width: Int, val height: Int) {
    override fun equals(other: Any?): Boolean =
        other is SampleRegion && other.left == left && other.top == top && other.width == width && other.height == height

    override fun hashCode(): Int = ((left * 31 + top) * 31 + width) * 31 + height

    override fun toString(): String = "SampleRegion(left=$left, top=$top, width=$width, height=$height)"
}

/**
 * The part of the art a frosted surface samples: the surface's rectangle at ([x], [y]) in the
 * art's coordinates, grown by [margin] on every side, cut to the art's own bounds, and widened to
 * whole pixels. Null when the surface and the art don't overlap.
 */
internal fun frostedSampleRegion(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    margin: Float,
    artWidth: Float,
    artHeight: Float,
): SampleRegion? {
    val left = max(x - margin, 0f)
    val top = max(y - margin, 0f)
    val right = min(x + width + margin, artWidth)
    val bottom = min(y + height + margin, artHeight)
    if (right <= left || bottom <= top) return null
    val wholeLeft = floor(left)
    val wholeTop = floor(top)
    return SampleRegion(
        left = wholeLeft.toInt(),
        top = wholeTop.toInt(),
        width = ceil(right - wholeLeft).toInt(),
        height = ceil(bottom - wholeTop).toInt(),
    )
}
