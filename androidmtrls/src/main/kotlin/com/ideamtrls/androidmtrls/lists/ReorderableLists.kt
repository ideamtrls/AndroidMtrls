package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * A list of rows the user puts in order: [items], each row built by [content] with the list's drag
 * handle at its trailing edge. Pair it with [AMListRow]; cards go in [AMReorderableCardList].
 *
 * ```kotlin
 * val queue = remember { mutableStateListOf(*tracks) }
 * AMReorderableRowList(
 *     queue,
 *     key = { it.id },
 *     onMove = { from, to -> queue.add(to, queue.removeAt(from)) },
 *     endAction = { track -> AMSwipeAction("Remove", deleteIcon, AMSemantic.Destructive, removesRow = true) { queue.remove(track) } },
 * ) { track ->
 *     AMListRow(track.title, subtitle = track.show)
 * }
 * ```
 *
 * - **Dragging.** A drag on a row's handle lifts it at once; a press and hold anywhere on the row
 *   lifts it too, with the long-press haptic, unless [holdToLift] is false. The lifted row comes
 *   up as a floating surface, in `shapes.large` with a higher shadow, and follows the finger; its
 *   neighbors slide out of its way with Material's motion, a light tick marking each place it
 *   passes. Each step shows at once and [onMove] reports it. Near the top or bottom edge of what's
 *   visible of the list, the list (or the page holding it) scrolls under it. On release it
 *   settles into its slot. The order is never hidden
 *   behind an edit mode: the handle is always there.
 * - **The order is the caller's.** [onMove] asks to move the item at `from` to `to`, one place at
 *   a time, each step from the order the step before left; make the same moves in [items], in
 *   order, at once or later. The list keeps its own order until [items] has taken the last step
 *   (or changes to an order none of the steps left), so the steps can go to a database and come
 *   back through its updates without holding up the drag. [key] must be stable across moves and
 *   saveable in a `Bundle`, as for any `LazyColumn`.
 * - **Swipes.** [startAction] and [endAction] give a row an action per swipe direction, as
 *   [AMSwipeableRow] does; the handle moves with the swiping row. An action that removes its item
 *   from [items] fades the row out as the rows below close up.
 * - **Surface.** Rows lie flat on what holds the list: a card (`Modifier.amCard()`, frosted over
 *   art too, its rows carrying `Modifier.amArtRow`) or a strip of the surface color.
 * - **Scrolling on its own.** Given a bounded height (a screen's, a weight, `heightIn(max = …)`),
 *   it's a `LazyColumn` on [state] that takes its rows' height up to that and scrolls inside it.
 * - **In a scrolling page.** Measured without a height limit (inside `Modifier.verticalScroll`, or
 *   as an item of a lazy list), it's a plain column as tall as its rows that scrolls with the
 *   page, so the card holding it is as long as its rows. Everything else stays: the lift, the
 *   neighbors sliding aside, the settle, the swipes. A lifted row scrolls the page near the edges
 *   of what's visible of the list, through nested scrolling, so a collapsing top app bar follows
 *   along. Every row is composed, so keep this for lists of a page's length; [state] isn't used.
 * - **Holding a row.** A hold that lifts a row and a hold on the row itself (`Modifier.amHoldable`
 *   opening its spotlight) answer the same long-press timeout. With [holdToLift] false only the
 *   handle lifts a row, and a hold on the rest of it is the row's own.
 * - **TalkBack** gets "Move up" and "Move down" on each row, and the swipe actions, as custom
 *   actions on the row itself; the handle is skipped.
 *
 * ```kotlin
 * // In an AMArtRowsBackdrop, so the frosted card blurs the rows' colors.
 * Column(Modifier.verticalScroll(rememberScrollState()).amArtRows()) {
 *     AMSectionHeader("Up next", Modifier.padding(horizontal = 16.dp))
 *     AMReorderableRowList(queue, key = { it.id }, onMove = ::moveTrack, Modifier.padding(horizontal = 16.dp).amCard(AMSurfaceFill.Frosted)) { track ->
 *         AMListRow(track.title, Modifier.amArtRow(track.coverUrl), subtitle = track.album)
 *     }
 * }
 * ```
 *
 * @param items the rows' items, in order.
 * @param key a stable, unique key per item.
 * @param onMove called as the user moves the item at `from` to `to`, a place in the order the last
 *   step left.
 * @param modifier the list's placement and size.
 * @param state the list's scroll state while it scrolls on its own.
 * @param startAction the action a swipe toward the end performs on an item's row, or null for
 *   none (the default).
 * @param endAction the action a swipe toward the start performs on an item's row, or null for none
 *   (the default).
 * @param holdToLift whether a press and hold anywhere on a row lifts it (the default). False lifts
 *   a row from its handle only, leaving holds to the row.
 * @param enabled whether rows can be moved and swiped. A disabled list keeps its rows and dims its
 *   handles.
 * @param content builds an item's row.
 */
@Composable
public fun <T> AMReorderableRowList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    startAction: (T) -> AMSwipeAction? = { null },
    endAction: (T) -> AMSwipeAction? = { null },
    holdToLift: Boolean = true,
    enabled: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    val side = AMTheme.tokens.spacing.lg
    val liftShape = MaterialTheme.shapes.large
    ReorderableList(
        items = items,
        key = key,
        onMove = onMove,
        modifier = modifier,
        listState = state,
        enabled = isEnabled,
        contentPadding = PaddingValues(),
        handleZone = side + ReorderHandleSize + side,
        holdToLift = holdToLift,
    ) { item, lifted, moveActions ->
        val start = startAction(item)
        val end = endAction(item)
        RowActionsContainer(moveActions + swipeAccessibilityActions(start, end, isEnabled)) {
            SwipeBox(
                startAction = start,
                endAction = end,
                enabled = isEnabled && !lifted,
                modifier = if (lifted) Modifier.amElevatedSurface(liftShape, AMElevation.Medium).clip(liftShape) else Modifier,
            ) {
                Row(Modifier.fillMaxWidth().amFlatSurface(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { content(item) }
                    ReorderHandle(isEnabled, Modifier.padding(end = side))
                }
            }
        }
    }
}

/**
 * A list of cards the user puts in order, each card built by [content] with the list's drag handle
 * inside its trailing edge. Pair it with [AMListCard]; rows go in [AMReorderableRowList].
 *
 * ```kotlin
 * AMReorderableCardList(tasks, key = { it.id }, onMove = { from, to -> tasks.add(to, tasks.removeAt(from)) }) { task ->
 *     AMListCard(task.title, subtitle = task.due)
 * }
 * ```
 *
 * - **Dragging** works as in [AMReorderableRowList]: the handle lifts a card at once, a press
 *   and hold anywhere on it lifts it too (unless [holdToLift] is false, leaving holds to the
 *   card, `Modifier.amHoldable` among them), each step shows at once and [onMove] reports it (the
 *   order is the caller's, taken in order, at once or later), the list scrolls near its edges, and
 *   the card settles into its slot on release. A lifted [AMListCard] casts a higher
 *   shadow; any card follows the finger above its neighbors.
 * - **Layout.** Each card carries the page gutter (`spacing.lg`) at its sides and `spacing.xs`
 *   above and below, so the list runs edge to edge of its container and its cards land on the
 *   gutters. An [AMListCard] keeps its text clear of the handle; custom card content leaves its
 *   trailing edge free.
 * - **Scrolling on its own.** Given a bounded height (a screen's, a weight, `heightIn(max = …)`),
 *   it's a `LazyColumn` on [state] that scrolls inside that height, with `spacing.md` above the
 *   first card and below the last for their shadows.
 * - **In a scrolling page.** Measured without a height limit (inside `Modifier.verticalScroll`, or
 *   as an item of a lazy list), it's a plain column as tall as its cards that scrolls with the
 *   page, the way a page of cards under a header reads. Everything else stays: the lift, the
 *   neighbors sliding aside, the settle. A lifted card scrolls the page near the edges of what's
 *   visible of the list, through nested scrolling, so a collapsing top app bar follows along. Every
 *   card is composed, so keep this for lists of a page's length; [state] isn't used.
 * - **TalkBack** gets "Move up" and "Move down" on each card, on an [AMListCard]'s own node.
 *
 * ```kotlin
 * Column(Modifier.verticalScroll(rememberScrollState())) {
 *     Text("12 albums", Modifier.padding(horizontal = 16.dp))
 *     AMReorderableCardList(albums, key = { it.id }, onMove = ::moveAlbum, Modifier.fillMaxWidth()) { album -> AlbumCard(album) }
 * }
 * ```
 *
 * @param items the cards' items, in order.
 * @param key a stable, unique key per item, saveable in a `Bundle`.
 * @param onMove called as the user moves the item at `from` to `to`, a place in the order the last
 *   step left.
 * @param modifier the list's placement and size.
 * @param state the list's scroll state while it scrolls on its own.
 * @param holdToLift whether a press and hold anywhere on a card lifts it (the default). False lifts
 *   a card from its handle only, leaving holds to the card.
 * @param enabled whether cards can be moved. A disabled list keeps its cards and dims its handles.
 * @param content builds an item's card.
 */
@Composable
public fun <T> AMReorderableCardList(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    holdToLift: Boolean = true,
    enabled: Boolean = true,
    content: @Composable (T) -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    val spacing = AMTheme.tokens.spacing
    val handleZone = spacing.lg + spacing.lg + ReorderHandleSize + spacing.lg
    val cell: ReorderCell<T> = { item, lifted, moveActions ->
        RowActionsContainer(moveActions, Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs)) {
            CompositionLocalProvider(
                LocalAMReorderLifted provides lifted,
                LocalAMReorderHandleInset provides ReorderHandleSize + spacing.md,
            ) {
                Box {
                    content(item)
                    ReorderHandle(isEnabled, Modifier.align(Alignment.CenterEnd).padding(end = spacing.lg))
                }
            }
        }
    }
    ReorderableList(
        items = items,
        key = key,
        onMove = onMove,
        modifier = modifier,
        listState = state,
        enabled = isEnabled,
        contentPadding = PaddingValues(vertical = spacing.md),
        handleZone = handleZone,
        holdToLift = holdToLift,
        cell = cell,
    )
}
