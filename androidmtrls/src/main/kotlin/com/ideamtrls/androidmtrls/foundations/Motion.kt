package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

// The app-facing motion helpers. Components own their own motion; these two are for an app that
// swaps a view's content itself. Both take Material's standard motion scheme, and the system's
// Remove animations setting (the animator duration scale at 0) makes them instant, as it does all
// Compose animation.

/**
 * Swaps content instantly, the way a tab switch lands: the changes in [changes] apply together, in
 * one frame, and nothing in the library animates them. A [Modifier.amCrossfade][amCrossfade] whose
 * value changes here snaps instead of fading.
 *
 * Wrap the state change that picks the new view, plus any resets that should land already settled
 * with it. Keep the dismissal of whatever triggered the switch outside, so it still animates
 * away over the new view:
 *
 * ```kotlin
 * amInstantSwitch { current = View.Alerts }
 * menuOpen = false
 * ```
 *
 * @param changes the state changes to apply together.
 */
public fun amInstantSwitch(changes: () -> Unit) {
    Snapshot.withMutableSnapshot {
        InstantSwitch.generation++
        changes()
    }
}

/**
 * Fades this content through whenever [value] changes: for a view that swaps its own content and
 * wants motion doing it (a sign-in step, a mode change). The old content leaves at once and the
 * new fades in over what's behind it, with the default effects spring of Material's standard
 * motion scheme.
 *
 * ```kotlin
 * Row(Modifier.amCrossfade(signedIn)) { if (signedIn) Welcome() else SignInPrompt() }
 * ```
 *
 * Pass whatever picks the content: the step, the mode. A change made inside [amInstantSwitch]
 * lands without the fade, and so does every change while the system's Remove animations is on.
 * Call it while composing, where the content is.
 *
 * @param value what picks the content; a change to it fades the new content in.
 */
public fun Modifier.amCrossfade(value: Any?): Modifier =
    this then CrossfadeElement(value, InstantSwitch.generation)

/**
 * Counts [amInstantSwitch] calls. A change applied in the same snapshot as a new count was made
 * inside one, so the library's own motion lands it instantly.
 */
internal object InstantSwitch {
    var generation by mutableIntStateOf(0)
}

private class CrossfadeElement(val value: Any?, val generation: Int) : ModifierNodeElement<CrossfadeNode>() {
    override fun create() = CrossfadeNode(value, generation)

    override fun update(node: CrossfadeNode) = node.update(value, generation)

    override fun equals(other: Any?): Boolean =
        other is CrossfadeElement && other.value == value && other.generation == generation

    override fun hashCode(): Int = 31 * (value?.hashCode() ?: 0) + generation

    override fun InspectorInfo.inspectableProperties() {
        name = "amCrossfade"
        properties["value"] = value
    }
}

private class CrossfadeNode(private var value: Any?, private var generation: Int) :
    Modifier.Node(), LayoutModifierNode, CompositionLocalConsumerModifierNode {

    private val alpha = Animatable(1f)

    fun update(value: Any?, generation: Int) {
        val changed = value != this.value
        val instant = generation != this.generation
        this.value = value
        this.generation = generation
        if (!changed || !isAttached) return
        if (instant) {
            coroutineScope.launch { alpha.snapTo(1f) }
            return
        }
        val spec = currentValueOf(MaterialTheme.LocalMaterialTheme).motionScheme.defaultEffectsSpec<Float>()
        coroutineScope.launch {
            alpha.snapTo(0f)
            alpha.animateTo(1f, spec)
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) { alpha = this@CrossfadeNode.alpha.value }
        }
    }
}
