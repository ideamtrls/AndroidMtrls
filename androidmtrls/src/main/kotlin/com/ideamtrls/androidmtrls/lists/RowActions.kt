package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics

// A row container (a swipeable row, a reorderable list's cell) offers its actions to TalkBack as
// custom actions. They belong on the row's own focusable node, the one that already reads the
// row's text, so they're one step away and the row stays one stop. The container can't reach
// that node, so it hands the actions down, and the library's rows put them on their list item
// and claim them. Content that doesn't claim them gets them on the container instead, merged. A
// hold on the row (amHoldable) adds its spotlight's actions to the container, ahead of the rest.

/**
 * The custom actions the nearest row container offers, how many rows have claimed them, and the
 * actions of a hold on its row ([held], a held item's spotlight tiles), which come first: TalkBack
 * finds [all] of them in one list on the row, the hold's, then the container's own (moves, then
 * swipes).
 */
@Stable
internal class RowActions(actions: List<CustomAccessibilityAction>, val claims: MutableIntState) {
    /** The container's own actions, the outer container's ahead of its own. Snapshot state. */
    var actions: List<CustomAccessibilityAction> by mutableStateOf(actions)

    /** A hold's actions on the row, in tile order. Snapshot state. */
    var held: List<CustomAccessibilityAction> by mutableStateOf(emptyList())

    /** Every action on the row, in order: the hold's, then the container's. */
    val all: List<CustomAccessibilityAction> get() = held + actions
}

/** The nearest row container's actions. Null outside one, and inside a row's own slots. */
internal val LocalAMRowActions = compositionLocalOf<RowActions?> { null }

/**
 * The nearest row container's actions, for a hold on its row to add to ([RowActions.held]). Rows
 * don't clear it for their slots, so a hold on a row's own modifier finds it.
 */
internal val LocalAMRowHold = compositionLocalOf<RowActions?> { null }

/**
 * A row container offering [actions] to the row in [content]. A container inside another takes
 * the outer one's actions too, ahead of its own, and claims them. Until a row inside claims the
 * combined list, the container itself carries it, with a hold's actions ahead, merged with its
 * content into one node.
 */
@Composable
internal fun RowActionsContainer(
    actions: List<CustomAccessibilityAction>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val outer = LocalAMRowActions.current
    if (outer != null) ClaimEffect(outer)
    val combined = if (outer == null) actions else outer.actions + actions
    val claims = remember { mutableIntStateOf(0) }
    val rowActions = remember(claims) { RowActions(combined, claims) }
    SideEffect { rowActions.actions = combined }
    val held by remember(rowActions) { derivedStateOf { rowActions.held.isNotEmpty() } }
    val fallback = if (claims.intValue == 0 && (combined.isNotEmpty() || held)) {
        Modifier.semantics(mergeDescendants = true) { customActions = rowActions.all }
    } else {
        Modifier
    }
    Box(modifier.then(fallback), propagateMinConstraints = true) {
        CompositionLocalProvider(
            LocalAMRowActions provides rowActions,
            LocalAMRowHold provides rowActions,
            content = content,
        )
    }
}

/**
 * For a library row: claims the nearest container's actions and returns the modifier that puts
 * them on the row's own node, a hold's ahead. Nothing outside a container. A row then provides null
 * for its own slots, so nothing inside it claims them again.
 */
@Composable
internal fun claimRowActions(): Modifier {
    val slot = LocalAMRowActions.current ?: return Modifier
    ClaimEffect(slot)
    return Modifier.semantics {
        val all = slot.all
        if (all.isNotEmpty()) customActions = all
    }
}

/** Counts this composition as a claim on [slot] while it's composed. */
@Composable
private fun ClaimEffect(slot: RowActions) {
    DisposableEffect(slot.claims) {
        slot.claims.intValue += 1
        onDispose { slot.claims.intValue -= 1 }
    }
}
