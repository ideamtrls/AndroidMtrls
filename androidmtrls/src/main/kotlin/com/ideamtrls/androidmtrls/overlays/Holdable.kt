package com.ideamtrls.androidmtrls

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onLongClick

/**
 * Press and hold: calls [onHold] once this item has been held for the system's long-press
 * timeout, with the long-press haptic as the hold registers. The trigger for an item's
 * [AMSpotlight].
 *
 * ```kotlin
 * ArticleRow(
 *     article,
 *     Modifier
 *         .clickable { open(article) }
 *         .amHoldable(
 *             onHold = { held = article },
 *             customActions = listOf(
 *                 CustomAccessibilityAction("Share") { share(article); true },
 *                 CustomAccessibilityAction("Delete") { delete(article); true },
 *             ),
 *         ),
 * )
 * ```
 *
 * It works alongside the item's own input, wherever it sits in the chain:
 *
 * - **A tap still does the item's main thing.** A touch that lifts before the timeout is left to
 *   the item's own click. A hold consumes the rest of its touch, so the item's click doesn't also
 *   fire; the ripple shows while the finger is down, as for any long press.
 * - **A drag that starts on the item still scrolls.** Once the touch moves past the touch slop, or
 *   something else (a scrolling list) takes it, the hold is off.
 * - **TalkBack**: double-tap and hold opens the hold, and [customActions] put the actions the
 *   spotlight offers on the item itself, so they're one step away.
 *
 * @param onHold called when the hold registers, while the finger is still down.
 * @param enabled whether the item can be held. False leaves it tap-only and drops the actions.
 * @param customActions the actions the hold offers (the spotlight's tiles), for TalkBack users,
 *   who get them on the item directly.
 */
public fun Modifier.amHoldable(
    onHold: () -> Unit,
    enabled: Boolean = true,
    customActions: List<CustomAccessibilityAction> = emptyList(),
): Modifier = this then HoldableElement(onHold, enabled, customActions)

private data class HoldableElement(
    val onHold: () -> Unit,
    val enabled: Boolean,
    val customActions: List<CustomAccessibilityAction>,
) : ModifierNodeElement<HoldableNode>() {
    override fun create() = HoldableNode(onHold, enabled, customActions)

    override fun update(node: HoldableNode) {
        node.onHold = onHold
        if (node.enabled != enabled || node.customActions != customActions) {
            node.enabled = enabled
            node.customActions = customActions
            node.invalidateSemantics()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amHoldable"
        properties["onHold"] = onHold
        properties["enabled"] = enabled
        properties["customActions"] = customActions
    }
}

private class HoldableNode(
    var onHold: () -> Unit,
    var enabled: Boolean,
    var customActions: List<CustomAccessibilityAction>,
) : DelegatingNode(), SemanticsModifierNode, CompositionLocalConsumerModifierNode {

    init {
        delegate(
            SuspendingPointerInputModifierNode {
                // The initial pass sees the touch before the item's own click and before a
                // scrolling parent's drag, whatever the modifier order, without taking it from them.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (!enabled) return@awaitEachGesture
                    val letGo = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        awaitLetGo(down, viewConfiguration.touchSlop)
                    }
                    if (letGo != null) return@awaitEachGesture

                    hold()
                    // The rest of this touch belongs to the hold: the item's click never sees it.
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
        )
    }

    private fun hold() {
        currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.LongPress)
        onHold()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (!enabled) return
        onLongClick {
            onHold()
            true
        }
        if (this@HoldableNode.customActions.isNotEmpty()) customActions = this@HoldableNode.customActions
    }
}

/**
 * Waits until the touch that went [down] lets go of the item: it lifts, moves past [touchSlop], or
 * something else (a scrolling list) consumes it. Returns true then; a hold is whatever outlasts
 * this.
 */
private suspend fun AwaitPointerEventScope.awaitLetGo(down: PointerInputChange, touchSlop: Float): Boolean {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return true
        if (change.changedToUpIgnoreConsumed() || change.isConsumed) return true
        if ((change.position - down.position).getDistance() > touchSlop) return true
    }
}
