package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp

/**
 * A pure Gaussian blur of the art behind this area, with no tint: the nearest [AMBackdrop]'s art,
 * the part under this area, blurred and drawn over it, so the picture's own colors and brightness
 * come through untouched.
 *
 * ```kotlin
 * AMBackdrop(art = { AMRemoteImage(cover, contentDescription = null) }, Modifier.height(280.dp)) {
 *     AMBackdropBlur(Modifier.matchParentSize())
 *     NowPlaying(Modifier.align(Alignment.Center))
 * }
 * ```
 *
 * For a region that blurs part of a picture: a now-playing background, a strip over art. Its size
 * comes from [modifier]; clip it for rounded corners. Over the whole art it blurs everything; over
 * a part, only that part. It samples a little past its edges, so the blur at its edge mixes in what
 * is really there, and at the art's own edge it repeats real pixels, so it never darkens toward
 * transparency. It follows the art as it scrolls and as a photo arrives.
 *
 * It blurs only the art of an [AMBackdrop] it sits inside; elsewhere it draws nothing. For a
 * whole stilled picture behind other content, [Modifier.amPausedBackdrop][amPausedBackdrop] blurs
 * the picture itself; a modal layer's blur comes with [AMActionsOverlay] and [AMSpotlight].
 *
 * @param modifier the blurred area's size and placement.
 * @param radius the blur, as Android's blur radius: [AMTheme.Overlay.blurRadius] by default.
 */
@Composable
public fun AMBackdropBlur(modifier: Modifier = Modifier, radius: Dp = AMTheme.Overlay.blurRadius) {
    Spacer(modifier.then(BackdropBlurElement(radius)))
}

private data class BackdropBlurElement(val radius: Dp) : ModifierNodeElement<BackdropBlurNode>() {
    override fun create() = BackdropBlurNode(radius)

    override fun update(node: BackdropBlurNode) {
        node.radius = radius
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "AMBackdropBlur"
        properties["radius"] = radius
    }
}

/** Draws its slice of the nearest backdrop's art, blurred, and redraws as it moves over the art. */
private class BackdropBlurNode(var radius: Dp) :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {

    private val slice = FrostedSlice()
    private var coordinates: LayoutCoordinates? = null
    private var lastOffset = Offset.Unspecified

    override fun onAttach() {
        slice.layer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        slice.layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        slice.layer = null
        coordinates = null
        lastOffset = Offset.Unspecified
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        val art = currentValueOf(LocalAMBackdrop)?.coordinates?.takeIf { it.isAttached } ?: return
        if (!coordinates.isAttached) return
        val offset = art.localPositionOf(coordinates, Offset.Zero)
        if (offset != lastOffset) {
            lastOffset = offset
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val backdrop = currentValueOf(LocalAMBackdrop)
        if (backdrop != null) {
            val area = Path().apply { addRect(size.toRect()) }
            slice.draw(this, area, backdrop, coordinates, radius.toPx())
        }
        drawContent()
    }
}
