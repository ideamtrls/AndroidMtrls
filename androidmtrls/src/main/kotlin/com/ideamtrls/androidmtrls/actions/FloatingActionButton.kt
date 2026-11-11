package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Where an [AMFloatingActionButton] has been moved to, kept by whoever outlives the button (the
 * screen, or the hub that shows it), so the button comes back where it was left after it leaves the
 * screen for a while, a configuration change or process death.
 *
 * ```kotlin
 * val fab = rememberAMFloatingActionButtonState()
 * AMFloatingActionButton("Show actions", onClick = { showsActions = true }, state = fab)
 * ```
 *
 * @param initialOffset where the button starts, from its resting place.
 */
@Stable
public class AMFloatingActionButtonState(initialOffset: DpOffset = DpOffset.Zero) {

    /**
     * How far the button sits from its resting place (bottom center, on the navigation bar's
     * inset): positive x to the right on screen, whatever the layout direction, positive y down.
     * Zero puts it back.
     */
    public var offset: DpOffset by mutableStateOf(initialOffset)

    public companion object {
        /** Saves and restores the [offset], for `rememberSaveable`. */
        public val Saver: Saver<AMFloatingActionButtonState, *> = listSaver(
            save = { listOf(it.offset.x.value, it.offset.y.value) },
            restore = { AMFloatingActionButtonState(DpOffset(it[0].dp, it[1].dp)) },
        )
    }
}

/**
 * An [AMFloatingActionButtonState] at its resting place, saved and restored with the screen around
 * it.
 */
@Composable
public fun rememberAMFloatingActionButtonState(): AMFloatingActionButtonState =
    rememberSaveable(saver = AMFloatingActionButtonState.Saver) { AMFloatingActionButtonState() }

/**
 * The button that opens the remote, the app's main navigation: a 56 dp circle with a +, the same
 * on every screen.
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     Screen()
 *     AMFloatingActionButton("Show actions", onClick = { showsActions = true }, state = fab)
 * }
 * ```
 *
 * It's a layer: it fills the space it's given and rests at the bottom center, `spacing.lg` above
 * the navigation bar's inset, letting touches anywhere else through to what's under it. Put it over
 * the whole screen.
 *
 * **Visual and gestures only.** A scene shows it through [AMFloatingActions], its slot in
 * [AMScene], which opens the remote from it and fades it out of the way while any overlay is up;
 * a button placed by hand gets none of that.
 *
 * It wears the floating surface (white in light mode, black in dark mode), the circle's two-layer
 * shadow and a hairline in both modes, and it's always solid: the + stays obvious over any
 * backdrop. A tap ripples and calls [onClick].
 *
 * **It always moves.** A long press (the system's long-press timeout) lifts it, with a haptic: it
 * grows a little and casts a deeper shadow. While lifted it follows the finger, and where it's let
 * go it stays ([state]). Lifting it never calls [onClick]. Near the screen's edges its area is
 * kept from the system's back gesture, so a drag that starts on it moves it instead of going back.
 * Under Remove animations the lift is instant; the drag always tracks the finger.
 *
 * A screen reader hears [contentDescription] and can activate it; moving it is a touch gesture
 * only. Disabled (its own [enabled], or a disabling container around it), the + greys to Material's
 * disabled color and the button takes no taps or drags.
 *
 * @param contentDescription what a screen reader says for the button ("Show actions").
 * @param onClick called when the button is tapped.
 * @param modifier the layer's placement and size: usually the whole screen.
 * @param state where the button has been moved to. Hoist it where it outlives the button.
 * @param enabled whether the button takes taps and drags.
 */
@Composable
public fun AMFloatingActionButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    state: AMFloatingActionButtonState = rememberAMFloatingActionButtonState(),
    enabled: Boolean = true,
) {
    val isEnabled = resolveEnabled(enabled)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val interactions = remember { MutableInteractionSource() }
    var lifted by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val scale by animateFloatAsState(
        targetValue = if (lifted) LiftScale else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "lift",
    )
    Box(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = AMTheme.tokens.spacing.lg),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                // Absolute: the drag is in screen terms, so the button follows the finger in right
                // to left layouts too.
                .absoluteOffset {
                    IntOffset(
                        (state.offset.x.toPx() + drag.x).roundToInt(),
                        (state.offset.y.toPx() + drag.y).roundToInt(),
                    )
                }
                .systemGestureExclusion()
                .size(FloatingActionButtonDiameter)
                // The tap: Material's ripple, the button role, focus and keys. The lift below takes
                // the touch from it, so a lifted button never counts as a tap.
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    enabled = isEnabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .pointerInput(isEnabled, state) {
                    if (!isEnabled) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val hold = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                        lifted = true
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        var released = false
                        try {
                            while (!released) {
                                val change = awaitPointerEvent().changes.firstOrNull { it.id == hold.id } ?: break
                                drag += change.positionChangeIgnoreConsumed()
                                change.consume()
                                released = !change.pressed
                            }
                        } finally {
                            // A release leaves the button where it is; a cancelled touch puts it back.
                            if (released) {
                                state.offset = DpOffset(state.offset.x + drag.x.toDp(), state.offset.y + drag.y.toDp())
                            }
                            drag = Offset.Zero
                            lifted = false
                        }
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                // The hairline below is drawn in both modes, so the surface's own dark-mode one is off.
                .amElevatedSurface(
                    CircleShape,
                    if (lifted) AMElevation.Medium else AMElevation.Low,
                    hairline = Color.Transparent,
                )
                .amHairline(CircleShape)
                .clip(CircleShape)
                .indication(interactions, ripple()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.am_ic_add),
                contentDescription = contentDescription,
                tint = if (isEnabled) onSurface else onSurface.copy(alpha = DisabledContentAlpha),
            )
        }
    }
}

/** The button's diameter. A scene reads it to keep its toasts clear of the button. */
internal val FloatingActionButtonDiameter: Dp = 56.dp

/** How much a lifted button grows, so it reads picked up. */
private const val LiftScale = 1.15f

/** Material's alpha for disabled content. */
private const val DisabledContentAlpha = 0.38f
