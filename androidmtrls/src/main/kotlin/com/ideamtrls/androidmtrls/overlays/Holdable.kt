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
 * [AMSpotlight]; [actions] are its tiles, which TalkBack users get on the item itself.
 *
 * ```kotlin
 * val actions = articleActions(article)   // the spotlight's tiles, as AMHoldActions
 * ArticleRow(article, Modifier.clickable { open(article) }.amHoldable(onHold = { held = article }, actions = actions))
 * ```
 *
 * With an [AMSpotlightState] the hold and the spotlight share one definition of the actions:
 * `Modifier.amHoldable(spotlight, article)`.
 *
 * It works alongside the item's own input, wherever it sits in the chain:
 *
 * - **A tap still does the item's main thing.** A touch that lifts before the timeout is left to
 *   the item's own click. A hold consumes the rest of its touch, so the item's click doesn't also
 *   fire; the ripple shows while the finger is down, as for any long press.
 * - **A drag that starts on the item still scrolls.** Once the touch moves past the touch slop, or
 *   something else (a scrolling list) takes it, the hold is off.
 * - **TalkBack**: double-tap and hold opens the hold, and [actions] are custom actions on the item,
 *   in tile order, named by each action's spoken name, so they're one step away. On a row that also
 *   swipes or reorders ([AMSwipeableRow], the reorderable lists) they join the row's own actions in
 *   one list: the hold's first, then the moves, then the swipes, so nothing is lost.
 *
 * @param onHold called when the hold registers, while the finger is still down.
 * @param actions what the hold offers (the spotlight's tiles), in tile order, for TalkBack users,
 *   who get them on the item directly. Each runs its action.
 * @param enabled whether the item can be held. False leaves it tap-only and drops the actions.
 */
public fun Modifier.amHoldable(
    onHold: () -> Unit,
    actions: List<AMHoldAction> = emptyList(),
    enabled: Boolean = true,
): Modifier = this then HoldableElement(onHold, actions, enabled)

/**
 * Press and hold that opens [state]'s spotlight on [item]: the hold calls `state.show(item)`, and
 * TalkBack users get the spotlight's tiles for [item] (the state's actions) on the item itself,
 * with no second list to keep in step.
 *
 * ```kotlin
 * val spotlight = rememberAMSpotlightState<Article> { article -> articleActions(article) }
 * ArticleRow(article, Modifier.amHoldable(spotlight, article))
 * AMSpotlight(spotlight) { article -> ArticleRow(article) }
 * ```
 *
 * Everything else is the other form's.
 *
 * @param state the spotlight the hold opens, and where the item's actions come from.
 * @param item what's held.
 * @param enabled whether the item can be held. False leaves it tap-only and drops the actions.
 */
public fun <T : Any> Modifier.amHoldable(
    state: AMSpotlightState<T>,
    item: T,
    enabled: Boolean = true,
): Modifier = amHoldable(onHold = { state.show(item) }, actions = state.actionsFor(item), enabled = enabled)

private data class HoldableElement(
    val onHold: () -> Unit,
    val actions: List<AMHoldAction>,
    val enabled: Boolean,
) : ModifierNodeElement<HoldableNode>() {
    override fun create() = HoldableNode(onHold, actions, enabled)

    override fun update(node: HoldableNode) = node.update(onHold, actions, enabled)

    override fun InspectorInfo.inspectableProperties() {
        name = "amHoldable"
        properties["onHold"] = onHold
        properties["actions"] = actions.map { it.spokenName }
        properties["enabled"] = enabled
    }
}

private class HoldableNode(
    var onHold: () -> Unit,
    private var actions: List<AMHoldAction>,
    private var enabled: Boolean,
) : DelegatingNode(), SemanticsModifierNode, CompositionLocalConsumerModifierNode {

    /** The row container the hold adds its actions to, or null to carry them itself. */
    private var row: RowActions? = null

    /** The actions as TalkBack sees them, rebuilt only when their names change. */
    private var spoken: List<CustomAccessibilityAction> = emptyList()

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

    fun update(onHold: () -> Unit, actions: List<AMHoldAction>, enabled: Boolean) {
        this.onHold = onHold
        this.actions = actions
        if (this.enabled != enabled) {
            this.enabled = enabled
            invalidateSemantics()
        }
        publish()
    }

    override fun onAttach() {
        row = currentValueOf(LocalAMRowHold)
        publish()
    }

    override fun onDetach() {
        row?.let { if (it.held === spoken) it.held = emptyList() }
        row = null
        spoken = emptyList()
    }

    /**
     * Builds the TalkBack actions when their names change (each runs the current action at its
     * place, so new lambdas need no rebuild) and puts them on the row, or on this item.
     */
    private fun publish() {
        val names = if (enabled) actions.map { it.spokenName } else emptyList()
        if (names == spoken.map { it.label }) return
        spoken = names.mapIndexed { index, name ->
            CustomAccessibilityAction(name) {
                actions.getOrNull(index)?.onClick?.invoke()
                true
            }
        }
        val row = row
        if (row != null) row.held = spoken else if (isAttached) invalidateSemantics()
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
        // In a row container the row carries them, merged with its own.
        if (row == null && spoken.isNotEmpty()) customActions = spoken
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
