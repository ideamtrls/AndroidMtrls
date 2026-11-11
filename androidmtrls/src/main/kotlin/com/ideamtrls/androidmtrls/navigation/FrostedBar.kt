package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import kotlin.math.max

// How a top app bar over art fills once content scrolls under it: the frosted fill, never a solid
// page band. AMArtPanelChrome always wears it; AMPanelChrome, AMToolbar and AMPageHeader wear it
// inside an AMBackdrop and take the page color everywhere else.

/**
 * [bar] with the frosted fill behind it, faded in by [fill] (0 at rest, 1 once content is under
 * it). [fill] is read while drawing, so a moving fraction redraws the fill without recomposing.
 */
@Composable
internal fun FrostedBar(fill: () -> Float, modifier: Modifier = Modifier, bar: @Composable () -> Unit) {
    Box(modifier) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = fill() }.frostedBarFill())
        bar()
    }
}

/**
 * Whether content has scrolled under a one-row top app bar: Material's own test, the overlap past
 * [BarScrolledFraction]. False without a scroll behavior.
 */
@Composable
internal fun barScrolledState(scrollBehavior: TopAppBarScrollBehavior?): State<Boolean> =
    remember(scrollBehavior) {
        derivedStateOf { (scrollBehavior?.state?.overlappedFraction ?: 0f) > BarScrolledFraction }
    }

/**
 * A one-row bar's frosted fill: in once content scrolls under it, fading with Material's effects
 * motion, as Material fades a one-row bar's scrolled color.
 */
@Composable
internal fun oneRowBarFill(scrollBehavior: TopAppBarScrollBehavior?): State<Float> {
    val scrolled by barScrolledState(scrollBehavior)
    return animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
}

/**
 * A two-row (large) bar's frosted fill: in at the rate the bar collapses, eased as Material eases
 * a two-row bar's scrolled color, so it's full once the bar is collapsed.
 */
internal fun twoRowBarFill(scrollBehavior: TopAppBarScrollBehavior?): Float =
    FastOutLinearInEasing.transform(scrollBehavior?.state?.collapsedFraction ?: 0f)

/** The overlap past which Material's top app bar counts content as under it. */
internal const val BarScrolledFraction: Float = 0.01f

/**
 * A bar's frosted fill: the nearest [AMBackdrop]'s art under the bar, blurred, under the frosted
 * fill, with the frosted edge along the bottom. With no art to sample (no backdrop, or before the
 * art and the bar are placed) it draws the solid surface, with the dark-mode hairline along the
 * bottom, never unblurred translucency.
 */
internal fun Modifier.frostedBarFill(): Modifier = this then FrostedBarFillElement

private data object FrostedBarFillElement : ModifierNodeElement<FrostedBarFillNode>() {
    override fun create() = FrostedBarFillNode()

    override fun update(node: FrostedBarFillNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "frostedBarFill"
    }
}

private class FrostedBarFillNode :
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

    // A bar that moves over its art samples a different slice, so it redraws.
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
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        val backdrop = currentValueOf(LocalAMBackdrop)
        val bounds = Path().apply { addRect(size.toRect()) }
        val frosted = backdrop != null &&
            slice.draw(this, bounds, backdrop, coordinates, Style.Frosted.BlurRadius.toPx())
        when {
            frosted -> drawRect(if (dark) Style.Frosted.FillDark else Style.Frosted.FillLight)
            else -> drawRect(if (dark) Style.Surface.Dark else Style.Surface.Light)
        }
        drawContent()
        val edge = when {
            frosted -> if (dark) Style.Frosted.EdgeDark else Style.Frosted.EdgeLight
            dark -> Style.SelectionRing.Dark
            else -> return
        }
        val stroke = max(Style.Hairline.Width.toPx(), 1f)
        drawRect(edge, topLeft = Offset(0f, size.height - stroke), size = Size(size.width, stroke))
    }
}
