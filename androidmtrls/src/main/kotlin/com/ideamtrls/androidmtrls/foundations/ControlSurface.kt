package com.ideamtrls.androidmtrls

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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

/**
 * The surface a control with its own shape wears (a filled button, a chip, a segmented control's
 * backing): the elevated surface ([amElevatedSurface]'s recipe, through the shared
 * [ElevatedSurfaceDrawer]), in the app's surface setting, read while it draws. Solid in a solid
 * app; in a frosted app frosted on the backdrop and, embedded inside a card, tabs or a section, or
 * on a sheet ([isEmbedded]), the tint. Embedded and solid, its dark-mode hairline is the brighter
 * [AMTheme.controlOutline], where the ring would vanish against the black around it; on the page
 * it keeps the selection ring.
 *
 * Put it after the control's `minimumInteractiveComponentSize()`, so the 48 dp target is reserved
 * outside it and the surface hugs the control's face.
 *
 * @param shape the control's own shape.
 * @param elevation the shadow the shape casts.
 * @param solidFill the solid look's fill. Unspecified (the default) is the surface color,
 *   [AMTheme.Surface.primary]; frost and the tint ignore it.
 * @param outline whether it draws the solid look's dark-mode hairline and the frosted edge. False
 *   draws neither, for a control whose Material outline stays (an unselected chip, the segmented
 *   buttons).
 */
internal fun Modifier.amControlSurface(
    shape: Shape,
    elevation: AMElevation,
    solidFill: Color = Color.Unspecified,
    outline: Boolean = true,
): Modifier = this then ControlSurfaceElement(shape, elevation, solidFill, outline)

private data class ControlSurfaceElement(
    val shape: Shape,
    val elevation: AMElevation,
    val solidFill: Color,
    val outline: Boolean,
) : ModifierNodeElement<ControlSurfaceNode>() {
    override fun create() = ControlSurfaceNode(shape, elevation, solidFill, outline)

    override fun update(node: ControlSurfaceNode) {
        node.shape = shape
        node.elevation = elevation
        node.solidFill = solidFill
        node.outline = outline
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amControlSurface"
        properties["shape"] = shape
        properties["elevation"] = elevation
        properties["solidFill"] = solidFill
        properties["outline"] = outline
    }
}

private class ControlSurfaceNode(
    var shape: Shape,
    var elevation: AMElevation,
    var solidFill: Color,
    var outline: Boolean,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {

    private val surface = ElevatedSurfaceDrawer()

    override fun onAttach() = surface.attach(requireGraphicsContext())

    override fun onDetach() = surface.detach(requireGraphicsContext())

    // A frosted control that moves over its art (a scroll) samples a different slice, so it
    // redraws. A solid one never walks the tree to find where it stands.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (surface.position(coordinates, backdropFor(lookOf(currentValueOf(LocalAMSurfaceFill))))) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        // Inside a card (flat or not), tabs or a section, or on a sheet: tinted when the app is
        // frosted, and when it's solid, the outline steps up.
        val embedded = isEmbedded()
        val look = surfaceLook(currentValueOf(LocalAMSurfaceFill), embedded)
        with(surface) {
            drawSurface(
                context = requireGraphicsContext(),
                shape = shape,
                shadow = elevation.shadowLevel,
                look = look,
                hairline = if (embedded) Style.ControlOutline.Dark else Color.Unspecified,
                dark = dark,
                backdrop = backdropFor(look),
                clipContent = false,
                outline = outline,
                solidFill = solidFill,
            )
        }
    }
}
