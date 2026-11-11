package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints

/**
 * Softens scrolled content as it passes under the status bar, on a screen with no top bar. A top
 * app bar takes content scrolling under it into its own fill; a screen without one (a view whose
 * header scrolls away with its content, a list that starts with its first section) would run its
 * rows straight under the clock. With this, the content fades out over the status bar's height,
 * whole at the bar's bottom edge and gone at the top of the screen, so what lies behind it (the
 * page, a backdrop's art) shows through under the icons.
 *
 * ```kotlin
 * LazyColumn(
 *     Modifier.fillMaxSize().amStatusBarEdge(),
 *     contentPadding = WindowInsets.systemBars.asPaddingValues(),
 * ) { … }
 * ```
 *
 * - **Put it on the scrolling container**, the one that fills the screen from its top edge and
 *   stays put while its content scrolls. It fades what the container draws, never what's behind
 *   it: an alpha mask on the content, not a band of color, so art behind the screen shows through.
 * - **It adds no inset and moves nothing.** The container still pads the status bar, so its
 *   content rests below it and nothing changes until content scrolls up under the bar.
 * - **It follows the inset**: the fade is the status bar's height, as it changes and as it
 *   animates. With no status bar over the container (the bar hidden, or a container that doesn't
 *   reach the top of the screen) it does nothing.
 * - The fade follows Material's fast-out-linear-in curve, so the content is mostly gone behind the
 *   icons and only comes back in the bar's last few pixels.
 *
 * A screen with a top bar doesn't need it: [AMPageHeader], [AMToolbar] and [AMPanelChrome] take
 * the page color (or the frosted fill over art) once content scrolls under them, status bar
 * included.
 */
public fun Modifier.amStatusBarEdge(): Modifier = this then StatusBarEdgeElement

private object StatusBarEdgeElement : ModifierNodeElement<StatusBarEdgeNode>() {
    override fun create(): StatusBarEdgeNode = StatusBarEdgeNode()

    override fun update(node: StatusBarEdgeNode) = Unit

    override fun equals(other: Any?): Boolean = other === this

    override fun hashCode(): Int = javaClass.hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "amStatusBarEdge"
    }
}

/**
 * Reads where the status bar ends, in its own coordinates, each time it's placed (the inset's
 * ruler, so a change to the inset places it again), and draws the content through a mask that
 * fades it out over that height.
 */
private class StatusBarEdgeNode : Modifier.Node(), LayoutModifierNode, DrawModifierNode {
    /** The height, from the content's top, that lies under the status bar; 0 when none does. */
    private var edge = 0f

    /** The mask for [edge], built when it's first drawn at that height. */
    private var mask: Brush? = null

    private val layerPaint = Paint()

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // The status bar's bottom edge, relative to this content's top: at or above it, no bar
            // lies over the content.
            val statusBarBottom = WindowInsetsRulers.StatusBars.current.top.current(0f)
            val newEdge = statusBarBottom.coerceIn(0f, placeable.height.toFloat())
            if (newEdge != edge) {
                edge = newEdge
                mask = null
                invalidateDraw()
            }
            placeable.place(0, 0)
        }
    }

    override fun ContentDrawScope.draw() {
        val edge = edge
        if (edge <= 0f) {
            drawContent()
            return
        }
        val brush = mask ?: Brush.verticalGradient(*EdgeMaskStops, startY = 0f, endY = edge).also { mask = it }
        // Room at the sides for what the content draws past them (its cards' shadows); the layer
        // never grows past what the canvas can show.
        val side = size.width
        val canvas = drawContext.canvas
        canvas.saveLayer(Rect(-side, 0f, size.width + side, size.height), layerPaint)
        drawContent()
        drawRect(brush, topLeft = Offset(-side, 0f), size = Size(size.width + 2 * side, edge), blendMode = BlendMode.DstIn)
        canvas.restore()
    }
}

/**
 * The mask's opacity down the status bar, top to bottom: Material's fast-out-linear-in curve from
 * nothing to whole, sampled finely enough that the gradient between samples can't be told from it.
 */
private val EdgeMaskStops: Array<Pair<Float, Color>> = Array(EdgeMaskSamples + 1) { index ->
    val fraction = index / EdgeMaskSamples.toFloat()
    fraction to Color.Black.copy(alpha = FastOutLinearInEasing.transform(fraction))
}

private const val EdgeMaskSamples = 12
