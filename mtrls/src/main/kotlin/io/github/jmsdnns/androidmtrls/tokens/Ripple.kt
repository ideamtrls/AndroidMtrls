package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.createRippleModifierNode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.unit.Dp

// ---------------------------------------------------------------------------
// State-layer alphas
// ---------------------------------------------------------------------------

// The single monochrome state-layer opacity set shared by every AM ripple.
// Material state-layer metrics: pressed/focused stronger, hovered subtle,
// dragged strongest. The color is monochrome (Black/White per theme) so these
// alphas read the same as the press-overlay recipe on inverted surfaces.
private val AMRippleAlpha = RippleAlpha(
    draggedAlpha = 0.16f,
    focusedAlpha = 0.12f,
    hoveredAlpha = 0.08f,
    pressedAlpha = 0.12f,
)

// ---------------------------------------------------------------------------
// amRipple — the AndroidMtrls press idiom
// ---------------------------------------------------------------------------

/**
 * The monochrome ripple that is the AndroidMtrls press idiom, replacing the
 * older opacity-dim feedback.
 *
 * A state-layer [androidx.compose.foundation.Indication] wrapping the low-level
 * [createRippleModifierNode] (the platform ripple node every design system
 * builds on — NOT Material3). The layer is pure monochrome: `Color.Black` on
 * light surfaces, `Color.White` on dark, keyed off [LocalAMIsDarkTheme] so the
 * demo app's manual light/dark toggle is respected (never
 * `isSystemInDarkTheme()`). Alphas are [AMRippleAlpha] — pressed/focused 0.12,
 * dragged 0.16, hovered 0.08.
 *
 * Wire it as the `indication` of a `clickable`/`toggleable`/`selectable` (or
 * `Modifier.indication`) alongside a hoisted `MutableInteractionSource`:
 *
 * ```kotlin
 * val interactionSource = remember { MutableInteractionSource() }
 * Box(
 *     Modifier
 *         .clip(shape) // bounded ripples clip to the caller's shape
 *         .clickable(interactionSource, indication = amRipple()) { onClick() }
 * )
 * ```
 *
 * @param bounded when `true` (default) the ripple is clipped to the composable
 *   bounds — the caller MUST `Modifier.clip(shape)` the target first, since the
 *   node only bounds to a rectangle. `false` gives an unbounded circular ripple
 *   sized by [radius], for icon-sized targets.
 * @param radius explicit ripple radius; [Dp.Unspecified] (default) lets the
 *   node derive it from the target size.
 */
@Composable
fun amRipple(
    bounded: Boolean = true,
    radius: Dp = Dp.Unspecified,
): IndicationNodeFactory {
    val color = if (LocalAMIsDarkTheme.current) Color.White else Color.Black
    return remember(bounded, radius, color) { AMRippleNodeFactory(bounded, radius, color) }
}

/**
 * The [IndicationNodeFactory] returned by [amRipple]. `@Stable` with value
 * [equals]/[hashCode] so recomposition reuses the same indication when the
 * bounded/radius/color triple is unchanged.
 */
@Stable
private class AMRippleNodeFactory(
    private val bounded: Boolean,
    private val radius: Dp,
    private val color: Color,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        createRippleModifierNode(
            interactionSource = interactionSource,
            bounded = bounded,
            radius = radius,
            color = ColorProducer { color },
            rippleAlpha = { AMRippleAlpha },
        )

    override fun equals(other: Any?): Boolean =
        other is AMRippleNodeFactory &&
            other.bounded == bounded &&
            other.radius == radius &&
            other.color == color

    override fun hashCode(): Int =
        (bounded.hashCode() * 31 + radius.hashCode()) * 31 + color.hashCode()
}
