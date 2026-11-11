package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * How an [AMButton] looks. Sealed, so only these three exist and a meaningless combination can't be
 * written:
 *
 * - [Primary]: the main action, a floating surface at Material's medium button size, its label at
 *   that size's own medium weight.
 * - [Secondary]: the same floating surface with a regular-weight label, for an action beside the
 *   main one, or one that isn't critical but still wants the button's weight.
 * - [Ghost]: Material's text button, with no surface, for an action that should recede (Cancel in
 *   a dialog or a panel).
 *
 * A button takes its style from [LocalAMButtonStyle] unless it passes one, so a container styles
 * the buttons in its slots ([AMFormActions] makes its commit button [Primary] and its destructive
 * one [Secondary]).
 *
 * ```kotlin
 * AMButton("Save", onClick = ::save, style = AMButtonStyle.Primary())
 * AMButton("Play", onClick = ::play, style = AMButtonStyle.Primary(fill = AMSurfaceFill.Frosted))
 * AMButton("Cancel", onClick = ::cancel, style = AMButtonStyle.Ghost)
 * ```
 */
@Immutable
public sealed interface AMButtonStyle {

    /**
     * The main action: Material's filled button at its medium size (56 dp tall, with that size's
     * padding and label type, and Material's ripple), a rounded rectangle in the theme's medium
     * shape, wearing the floating surface, white in light mode and black in dark mode, with the
     * shape's two-layer shadow, and its label at the medium size's own medium weight.
     *
     * @property elevation the surface's shadow: [AMElevation.Low] by default, [AMElevation.None]
     *   for a button that keeps its fill and casts nothing.
     * @property fill solid (the default) or frosted over the art of the nearest [AMBackdrop].
     */
    @Immutable
    public class Primary(
        public val elevation: AMElevation = AMElevation.Low,
        public val fill: AMSurfaceFill = AMSurfaceFill.Solid,
    ) : AMButtonStyle {
        override fun equals(other: Any?): Boolean =
            other is Primary && other.elevation == elevation && other.fill == fill

        override fun hashCode(): Int = 31 * elevation.hashCode() + fill.hashCode()

        override fun toString(): String = "Primary(elevation=$elevation, fill=$fill)"
    }

    /**
     * The same floating surface as [Primary], with a regular-weight label: an action beside the
     * main one.
     *
     * @property elevation the surface's shadow: [AMElevation.Low] by default, [AMElevation.None]
     *   for a button that keeps its fill and casts nothing.
     * @property fill solid (the default) or frosted over the art of the nearest [AMBackdrop].
     */
    @Immutable
    public class Secondary(
        public val elevation: AMElevation = AMElevation.Low,
        public val fill: AMSurfaceFill = AMSurfaceFill.Solid,
    ) : AMButtonStyle {
        override fun equals(other: Any?): Boolean =
            other is Secondary && other.elevation == elevation && other.fill == fill

        override fun hashCode(): Int = 31 * elevation.hashCode() + fill.hashCode() + 1

        override fun toString(): String = "Secondary(elevation=$elevation, fill=$fill)"
    }

    /**
     * Material's text button: no surface, no shadow, its label in the color scheme's primary, its
     * ripple in the theme's medium shape like the filled styles. For actions that recede, in
     * dialogs and panels or beside a filled button.
     */
    @Immutable
    public data object Ghost : AMButtonStyle
}

/**
 * The style an [AMButton] takes when it doesn't pass one: [AMButtonStyle.Primary] at the root. A
 * container provides it to style the buttons in its slots, as [AMFormActions] does.
 *
 * ```kotlin
 * CompositionLocalProvider(LocalAMButtonStyle provides AMButtonStyle.Secondary()) { Actions() }
 * ```
 */
public val LocalAMButtonStyle: ProvidableCompositionLocal<AMButtonStyle> =
    staticCompositionLocalOf { AMButtonStyle.Primary() }

/**
 * A button: a label, optionally led by a glyph, in one of the three [AMButtonStyle]s.
 *
 * ```kotlin
 * AMButton("Continue", onClick = ::next, modifier = Modifier.fillMaxWidth())
 * AMButton("Shuffle", onClick = ::shuffle, icon = painterResource(R.drawable.ic_shuffle), style = AMButtonStyle.Secondary())
 * AMButton("Delete playlist", onClick = ::delete, style = AMButtonStyle.Secondary(), destructive = true)
 * ```
 *
 * Every style is a rounded rectangle in the theme's medium shape (`MaterialTheme.shapes.medium`,
 * 12 dp by default), its surface, clip and ripple alike: never a pill.
 *
 * The filled styles are Material's `Button` at its medium size: 56 dp tall, with that size's
 * padding, glyph size and label type (`ButtonDefaults.MediumContainerHeight`, `contentPaddingFor`,
 * `iconSizeFor`, `textStyleFor`) and its ripple, so a filled button stands as tall as the rows
 * around it. They wear the floating surface: white in light mode and black in dark mode, the
 * shape's two-layer shadow, and in dark mode a hairline (the selection ring on the page, the
 * brighter [AMTheme.controlOutline] inside a card or on a black sheet), or frosted glass over art
 * with [AMSurfaceFill.Frosted]. Their label is `onSurface`. [AMButtonStyle.Ghost] is Material's
 * `TextButton` in the same shape. A button wraps its label like Material's; give it
 * `Modifier.fillMaxWidth()` or a weight to span a row. It takes taps across at least 48 dp.
 *
 * Disabled (its own [enabled], or a disabling container around it), the label greys to Material's
 * disabled color and a filled button drops its shadow, keeping its place, size and fill.
 *
 * @param text the label.
 * @param onClick called when the button is tapped.
 * @param modifier the button's placement and size.
 * @param icon a glyph before the label, at the style's Material glyph size. Null shows none.
 * @param style the look: [LocalAMButtonStyle] unless given, so a container can style its buttons.
 * @param destructive whether the action removes something: the label takes [AMSemantic.Destructive].
 * @param enabled whether the button takes taps.
 */
@Composable
public fun AMButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    style: AMButtonStyle = LocalAMButtonStyle.current,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val filled = style != AMButtonStyle.Ghost
    ButtonBody(onClick, modifier, style, destructive, enabled, leadingIcon = icon != null) {
        if (icon != null) {
            val glyph = if (filled) ButtonDefaults.iconSizeFor(FilledHeight) else ButtonDefaults.IconSize
            val gap = if (filled) ButtonDefaults.iconSpacingFor(FilledHeight) else ButtonDefaults.IconSpacing
            Icon(icon, contentDescription = null, modifier = Modifier.size(glyph))
            Spacer(Modifier.size(gap))
        }
        Text(text)
    }
}

/** The filled styles' height: Material's medium button. */
private val FilledHeight = ButtonDefaults.MediumContainerHeight

/**
 * A button with custom content: [content] laid out in a row, taking the style's label type and
 * color, so a custom label (a glyph after the text, two weights) stays on-system. Otherwise the same
 * button as the string form.
 *
 * @param onClick called when the button is tapped.
 * @param modifier the button's placement and size.
 * @param style the look: [LocalAMButtonStyle] unless given, so a container can style its buttons.
 * @param destructive whether the action removes something: the content takes
 *   [AMSemantic.Destructive].
 * @param enabled whether the button takes taps.
 * @param content the button's label, in a row.
 */
@Composable
public fun AMButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AMButtonStyle = LocalAMButtonStyle.current,
    destructive: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    ButtonBody(onClick, modifier, style, destructive, enabled, leadingIcon = false, content = content)
}

/** Both forms of [AMButton]: picks the Material button and size for [style], and its padding. */
@Composable
private fun ButtonBody(
    onClick: () -> Unit,
    modifier: Modifier,
    style: AMButtonStyle,
    destructive: Boolean,
    enabled: Boolean,
    leadingIcon: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    when (style) {
        is AMButtonStyle.Primary ->
            SurfaceButton(onClick, modifier, style.elevation, style.fill, null, destructive, isEnabled, leadingIcon, content)
        is AMButtonStyle.Secondary ->
            SurfaceButton(onClick, modifier, style.elevation, style.fill, FontWeight.Normal, destructive, isEnabled, leadingIcon, content)
        AMButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = isEnabled,
            shape = MaterialTheme.shapes.medium,
            colors = if (destructive) {
                ButtonDefaults.textButtonColors(contentColor = AMSemantic.Destructive.color)
            } else {
                ButtonDefaults.textButtonColors()
            },
            contentPadding = if (leadingIcon) {
                ButtonDefaults.TextButtonWithIconContentPadding
            } else {
                ButtonDefaults.TextButtonContentPadding
            },
            content = content,
        )
    }
}

/**
 * Material's `Button` at its medium size, wearing the floating surface. The surface is drawn around
 * the button's own bounds, never around a touch target: the 48 dp target is reserved outside the
 * surface, and the button inside lays out at its own height.
 *
 * @param labelWeight the label's weight, or null for the medium size's own (medium) weight.
 */
@Composable
private fun SurfaceButton(
    onClick: () -> Unit,
    modifier: Modifier,
    elevation: AMElevation,
    fill: AMSurfaceFill,
    labelWeight: FontWeight?,
    destructive: Boolean,
    enabled: Boolean,
    leadingIcon: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = MaterialTheme.shapes.medium
    val minimumSize = LocalMinimumInteractiveComponentSize.current
    val labelStyle = ButtonDefaults.textStyleFor(FilledHeight).let { medium ->
        if (labelWeight != null) medium.copy(fontWeight = labelWeight) else medium
    }
    Box(
        modifier
            .minimumInteractiveComponentSize()
            .controlSurface(shape, if (enabled) elevation else AMElevation.None, fill),
        propagateMinConstraints = true,
    ) {
        // The target is reserved above, outside the surface; the button itself lays out at its own
        // height, so the surface hugs it.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Button(
                onClick = onClick,
                modifier = Modifier.heightIn(min = FilledHeight),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = if (destructive) AMSemantic.Destructive.color else onSurface,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = onSurface.copy(alpha = DisabledContentAlpha),
                ),
                elevation = null,
                contentPadding = ButtonDefaults.contentPaddingFor(FilledHeight, hasStartIcon = leadingIcon),
            ) {
                val row = this
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides minimumSize) {
                    ProvideTextStyle(labelStyle) { row.content() }
                }
            }
        }
    }
}

/** Material's alpha for disabled content. */
private const val DisabledContentAlpha = 0.38f

/**
 * The floating surface a solid control wears: the elevated surface ([amElevatedSurface]'s recipe,
 * through the shared [ElevatedSurfaceDrawer]), whose dark-mode hairline is the brighter
 * [AMTheme.controlOutline] when the control sits inside a card or on a black sheet
 * ([sitsOnPrimarySurface]), where the ring would vanish against the black around it, and the
 * selection ring on the page.
 */
private fun Modifier.controlSurface(shape: Shape, elevation: AMElevation, fill: AMSurfaceFill): Modifier =
    this then ControlSurfaceElement(shape, elevation, fill)

private data class ControlSurfaceElement(
    val shape: Shape,
    val elevation: AMElevation,
    val fill: AMSurfaceFill,
) : ModifierNodeElement<ControlSurfaceNode>() {
    override fun create() = ControlSurfaceNode(shape, elevation, fill)

    override fun update(node: ControlSurfaceNode) {
        node.shape = shape
        node.elevation = elevation
        node.fill = fill
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "controlSurface"
        properties["shape"] = shape
        properties["elevation"] = elevation
        properties["fill"] = fill
    }
}

private class ControlSurfaceNode(
    var shape: Shape,
    var elevation: AMElevation,
    var fill: AMSurfaceFill,
) : Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode {

    private val surface = ElevatedSurfaceDrawer()

    override fun onAttach() = surface.attach(requireGraphicsContext())

    override fun onDetach() = surface.detach(requireGraphicsContext())

    // A frosted control that moves over its art (a scroll) samples a different slice, so it redraws.
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (surface.position(coordinates, backdrop())) invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        // Inside a card (flat or not) or on a black sheet, the outline steps up.
        val onPrimary = sitsOnPrimarySurface()
        with(surface) {
            drawSurface(
                context = requireGraphicsContext(),
                shape = shape,
                elevation = elevation,
                fill = fill,
                hairline = if (onPrimary) Style.ControlOutline.Dark else Color.Unspecified,
                dark = dark,
                backdrop = backdrop(),
                clipContent = false,
            )
        }
    }

    private fun backdrop(): AMBackdropSource? =
        if (fill == AMSurfaceFill.Frosted) currentValueOf(LocalAMBackdrop) else null
}
