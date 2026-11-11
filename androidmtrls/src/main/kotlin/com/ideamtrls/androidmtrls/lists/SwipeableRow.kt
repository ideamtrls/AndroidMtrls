package com.ideamtrls.androidmtrls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * What a full swipe on an [AMSwipeableRow] does: the action runs, and the row slides away over a
 * band of [tint]'s color showing [icon].
 *
 * ```kotlin
 * AMSwipeAction("Delete", painterResource(R.drawable.ic_delete), AMSemantic.Destructive, removesRow = true) {
 *     messages.remove(message)
 * }
 * ```
 *
 * Actions carry a callback, so two actions are never equal unless they're the same action.
 *
 * @property label what the action does, in sentence case. TalkBack offers it as a custom action
 *   on the row.
 * @property icon the glyph shown on the band as the row slides away.
 * @property tint the band's color: the role's color, its glyph in the role's on-color;
 *   [AMSemantic.Info] by default, [AMSemantic.Destructive] for a delete. Null for a neutral action
 *   that is neither a status nor a removal (mute, snooze): a gray band, the scheme's
 *   `surfaceContainerHighest` with the glyph in `onSurface`, the pair a neutral [AMTag] wears.
 * @property removesRow whether the action takes the row away (delete, archive). Such a row stays
 *   swiped off, showing the band, until the caller removes it from its list. False (the default)
 *   slides the row back once the action has run.
 * @property onClick what the action does, run on the main thread after the swipe completes, or
 *   from the row's custom action.
 */
@Immutable
public class AMSwipeAction(
    public val label: String,
    public val icon: Painter,
    public val tint: AMSemantic? = AMSemantic.Info,
    public val removesRow: Boolean = false,
    public val onClick: () -> Unit,
)

/**
 * A row with an action on each swipe direction, the Android way: Material's `SwipeToDismissBox`.
 * A full swipe (released past half the row, or flung) performs the action as the row slides away
 * over a band of the action's color with its glyph; a short swipe springs back and does nothing.
 *
 * ```kotlin
 * AMSwipeableRow(
 *     startAction = AMSwipeAction("Archive", archiveIcon, removesRow = true) { archive(message) },
 *     endAction = AMSwipeAction("Delete", deleteIcon, AMSemantic.Destructive, removesRow = true) { delete(message) },
 * ) {
 *     AMListRow(message.sender, subtitle = message.subject)
 * }
 * ```
 *
 * - **One action per direction.** [startAction] is revealed at the row's start by a swipe toward
 *   its end (to the right, left to right); [endAction] at its end by a swipe toward its start.
 *   Either may be null, and a row with neither doesn't swipe. Anything more goes in a ⋮ menu or a
 *   press and hold.
 * - **After the swipe.** An action that [AMSwipeAction.removesRow] leaves the row swiped off until
 *   the caller drops it from the list; any other slides the row back once it has run. The band
 *   shows only in the strip the row has uncovered, so a row lying flat in a card shows nothing
 *   at rest, and inside a card the band keeps to the card's corners.
 * - **Feel.** Crossing the point where a release would act gives a light haptic. The drag tracks
 *   the finger; the settle and the slide-away use Material's motion. A vertical drag that starts
 *   on the row still scrolls.
 * - **TalkBack** gets each action as a custom action on the row (on a library row's own node
 *   when the content is [AMListRow], [AMListCard] or [AMLabeledContent]), so no swipe is needed.
 *
 * @param modifier the row's placement.
 * @param startAction the action a swipe toward the end performs. Null for none.
 * @param endAction the action a swipe toward the start performs. Null for none.
 * @param enabled whether the row swipes and offers its actions.
 * @param content the row.
 */
@Composable
public fun AMSwipeableRow(
    modifier: Modifier = Modifier,
    startAction: AMSwipeAction? = null,
    endAction: AMSwipeAction? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    RowActionsContainer(swipeAccessibilityActions(startAction, endAction, isEnabled), modifier) {
        SwipeBox(startAction, endAction, isEnabled, Modifier, content)
    }
}

/** The actions' custom accessibility actions, start first. None while disabled. */
internal fun swipeAccessibilityActions(
    startAction: AMSwipeAction?,
    endAction: AMSwipeAction?,
    enabled: Boolean,
): List<CustomAccessibilityAction> {
    if (!enabled) return emptyList()
    return listOfNotNull(startAction, endAction).map { action ->
        CustomAccessibilityAction(action.label) {
            action.onClick()
            true
        }
    }
}

/**
 * The swipe itself, shared by [AMSwipeableRow] and the reorderable row list: `SwipeToDismissBox`
 * with the masked band behind [content], the threshold haptic, and the settle back for actions
 * that keep the row.
 */
@Composable
internal fun SwipeBox(
    startAction: AMSwipeAction?,
    endAction: AMSwipeAction?,
    enabled: Boolean,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // A full swipe: past half the row (or a fling), not Material's default fixed distance.
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { distance -> distance * SwipeCommitFraction })
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentStart by rememberUpdatedState(startAction)
    val currentEnd by rememberUpdatedState(endAction)

    // The light tick as the swipe crosses the point where letting go would act.
    LaunchedEffect(state, haptics) {
        snapshotFlow { state.targetValue }
            .distinctUntilChanged()
            .drop(1)
            .collect { target ->
                if (target != SwipeToDismissBoxValue.Settled) {
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
            }
    }

    SwipeToDismissBox(
        state = state,
        backgroundContent = { SwipeBand(state, startAction, endAction) },
        modifier = modifier,
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        gesturesEnabled = enabled && (startAction != null || endAction != null),
        onDismiss = { direction ->
            val action = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> currentStart
                SwipeToDismissBoxValue.EndToStart -> currentEnd
                SwipeToDismissBoxValue.Settled -> null
            }
            action?.onClick?.invoke()
            if (action?.removesRow != true) scope.launch { state.reset() }
        },
    ) {
        Box(Modifier.fillMaxWidth()) { content() }
    }
}

/** How far across the row a release must be to act, short of a fling. */
private const val SwipeCommitFraction = 0.5f

/**
 * The band behind a swiping row: the revealed action's color with its glyph at the edge the
 * swipe started from, drawn only in the strip the row has uncovered.
 */
@Composable
private fun SwipeBand(state: SwipeToDismissBoxState, startAction: AMSwipeAction?, endAction: AMSwipeAction?) {
    // Recomposes when the side changes, not on every frame of the drag; the strip is drawn.
    val direction by remember(state) { derivedStateOf { state.dismissDirection } }
    val action = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> startAction
        SwipeToDismissBoxValue.EndToStart -> endAction
        SwipeToDismissBoxValue.Settled -> null
    } ?: return
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // A neutral action (no role) takes the neutral tag's pair, which reads gray in both modes.
    val bandColor = action.tint?.color ?: MaterialTheme.colorScheme.surfaceContainerHighest
    val glyphColor = action.tint?.onColor ?: MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .fillMaxSize()
            .drawWithContent {
                val offset = if (state.dismissDirection == SwipeToDismissBoxValue.Settled) 0f else state.requireOffset()
                val strip = swipeRevealSpan(offset, size.width, rtl)
                clipRect(left = strip.start, right = strip.endInclusive) {
                    this@drawWithContent.drawContent()
                }
            }
            .background(bandColor)
            .padding(horizontal = AMTheme.tokens.spacing.lg),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(action.icon, contentDescription = null, tint = glyphColor)
    }
}

/**
 * The strip, in physical pixels from the left, that a row of [width] uncovers when its content
 * sits [offset] along the swipe: positive toward the row's end, which is the right in left to
 * right and the left in right to left.
 */
internal fun swipeRevealSpan(offset: Float, width: Float, rtl: Boolean): ClosedFloatingPointRange<Float> {
    val shift = (if (rtl) -offset else offset).coerceIn(-width, width)
    return if (shift >= 0f) 0f..shift else (width + shift)..width
}
