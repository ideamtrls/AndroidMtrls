package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// The one engine behind both reorderable lists: items that lift on a press and hold (or at once
// from their drag handle), follow the finger, trade places with their neighbors as they pass
// them, scroll the list near its edges, and settle into their slot on release. TalkBack gets
// "Move up" and "Move down" on every item instead. The items lie in a LazyColumn that scrolls
// itself, or (the card list in a scrolling page) in a plain column that scrolls with the page.

/** The drag handle's glyph size. */
internal val ReorderHandleSize: Dp = 24.dp

/** How close to the list's top or bottom edge a lifted item starts the list scrolling. */
private val ReorderEdgeZone: Dp = 64.dp

/** The fastest the list scrolls under a lifted item, per second, at the very edge. */
private val ReorderMaxScrollVelocity: Dp = 800.dp

/** Material's alpha for disabled content. */
private const val DisabledAlpha = 0.38f

/** Whether the item around this content is lifted: a card lifts its shadow while it's dragged. */
internal val LocalAMReorderLifted = compositionLocalOf { false }

/** The room a card leaves at its trailing edge for the list's drag handle. 0 outside a list. */
internal val LocalAMReorderHandleInset = compositionLocalOf { 0.dp }

/**
 * The drag handle every item in a reorderable list carries: Material's `drag_handle` glyph in
 * `onSurfaceVariant`. It shows where a drag starts at once; a press and hold anywhere on the item
 * lifts it too. TalkBack skips it: the item carries the move actions itself.
 */
@Composable
internal fun ReorderHandle(enabled: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Icon(
        painterResource(R.drawable.am_ic_drag_handle),
        contentDescription = null,
        modifier = modifier,
        tint = if (enabled) scheme.onSurfaceVariant else scheme.onSurface.copy(alpha = DisabledAlpha),
    )
}

/** Builds an item's cell: the item, whether it's lifted, and the move actions TalkBack offers on it. */
internal typealias ReorderCell<T> = @Composable (item: T, lifted: Boolean, moveActions: List<CustomAccessibilityAction>) -> Unit

/**
 * The engine's state for a list on [layout], with its settings written after every composition:
 * the trailing [handleZone] of each item starts a drag as soon as the finger moves.
 */
@Composable
private fun rememberReorderState(
    layout: ReorderLayout,
    onMove: (from: Int, to: Int) -> Unit,
    enabled: Boolean,
    handleZone: Dp,
): ReorderState {
    val scope = rememberCoroutineScope()
    val state = remember(layout, scope) { ReorderState(layout, scope) }
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val settleSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    SideEffect {
        state.onMove = onMove
        state.enabled = enabled
        state.rtl = rtl
        state.haptics = haptics
        state.settleSpec = settleSpec
        with(density) {
            state.handleZonePx = handleZone.toPx()
            state.edgeZonePx = ReorderEdgeZone.toPx()
            state.maxScrollVelocityPx = ReorderMaxScrollVelocity.toPx()
        }
    }
    return state
}

/** The placement of the lifted and the settling item: above the rest, offset from their slot. */
private fun Modifier.reorderPlacement(state: ReorderState, key: Any): Modifier? = when (key) {
    state.liftedKey -> zIndex(2f).graphicsLayer { translationY = state.liftedOffset() }
    state.settlingKey -> zIndex(1f).graphicsLayer { translationY = state.settleOffset }
    else -> null
}

/**
 * The shared list: [items] in a `LazyColumn` on [listState], each built by [cell]. The trailing
 * [handleZone] of each item, where the cell draws its handle, starts a drag as soon as the finger
 * moves; anywhere else a press and hold does.
 */
@Composable
internal fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier,
    listState: LazyListState,
    enabled: Boolean,
    contentPadding: PaddingValues,
    handleZone: Dp,
    cell: ReorderCell<T>,
) {
    val layout = remember(listState) { LazyReorderLayout(listState) }
    val state = rememberReorderState(layout, onMove, enabled, handleZone)
    val currentOnMove by rememberUpdatedState(onMove)
    val motion = MaterialTheme.motionScheme
    val placementSpec = motion.defaultSpatialSpec<IntOffset>()
    val fadeSpec = motion.defaultEffectsSpec<Float>()
    LazyColumn(
        modifier = modifier.reorderGestures(state),
        state = listState,
        contentPadding = contentPadding,
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            val itemKey = key(item)
            val placement = Modifier.reorderPlacement(state, itemKey)
                ?: Modifier.animateItem(fadeInSpec = fadeSpec, placementSpec = placementSpec, fadeOutSpec = fadeSpec)
            val moveActions = reorderAccessibilityActions(index, items.lastIndex, enabled) { from, to ->
                currentOnMove(from, to)
            }
            Box(placement) {
                cell(item, state.isLifted(itemKey), moveActions)
            }
        }
    }
}

/**
 * The list in a scrolling page: [items] in a plain column, as tall as its items, each built by
 * [cell]. It scrolls with the page that holds it; a lifted item scrolls the page near the edges of
 * what's visible of the list, through nested scrolling, so whatever scrolls the page (a scrolling
 * column, a lazy list, a collapsing top app bar above them) takes part. Neighbors slide out of the
 * lifted item's way with Material's motion.
 */
@Composable
internal fun <T> ReorderablePageColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    handleZone: Dp,
    cell: ReorderCell<T>,
) {
    val dispatcher = remember { NestedScrollDispatcher() }
    val scope = rememberCoroutineScope()
    val pageLayout = remember(dispatcher, scope) { PageReorderLayout(dispatcher, scope) }
    val state = rememberReorderState(pageLayout, onMove, enabled, handleZone)
    val currentOnMove by rememberUpdatedState(onMove)
    val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    SideEffect { pageLayout.slideSpec = slideSpec }
    val keys = items.map(key)
    Layout(
        content = {
            items.forEachIndexed { index, item ->
                val itemKey = keys[index]
                androidx.compose.runtime.key(itemKey) {
                    val moveActions = reorderAccessibilityActions(index, items.lastIndex, enabled) { from, to ->
                        currentOnMove(from, to)
                    }
                    Box(Modifier.reorderPlacement(state, itemKey) ?: Modifier) {
                        cell(item, state.isLifted(itemKey), moveActions)
                    }
                }
            }
        },
        modifier = modifier
            .nestedScroll(PageNestedScrollConnection, dispatcher)
            .onPlaced { pageLayout.coordinates = it }
            .reorderGestures(state),
    ) { measurables, constraints ->
        val itemConstraints = Constraints(minWidth = constraints.minWidth, maxWidth = constraints.maxWidth)
        val placeables = measurables.map { it.measure(itemConstraints) }
        var top = 0
        val placed = placeables.mapIndexed { index, placeable ->
            ReorderItem(keys[index], index, top, placeable.height).also { top += placeable.height }
        }
        pageLayout.place(placed, state)
        val width = constraints.constrainWidth(placeables.maxOfOrNull { it.width } ?: 0)
        layout(width, constraints.constrainHeight(top)) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(0, placed[index].offset + pageLayout.slideOffset(keys[index]).roundToInt())
            }
        }
    }
}

/** A nested-scroll connection that takes nothing: the page column only dispatches to its parents. */
private val PageNestedScrollConnection = object : NestedScrollConnection {}

/** "Move up" and "Move down" for the item at [index], where they lead anywhere. None while disabled. */
internal fun reorderAccessibilityActions(
    index: Int,
    lastIndex: Int,
    enabled: Boolean,
    onMove: (from: Int, to: Int) -> Unit,
): List<CustomAccessibilityAction> {
    if (!enabled) return emptyList()
    val actions = ArrayList<CustomAccessibilityAction>(2)
    if (index > 0) {
        actions += CustomAccessibilityAction("Move up") {
            onMove(index, index - 1)
            true
        }
    }
    if (index < lastIndex) {
        actions += CustomAccessibilityAction("Move down") {
            onMove(index, index + 1)
            true
        }
    }
    return actions
}

/** One item as the engine sees it: its key, its place in the list, and its span along the list's frame. */
internal data class ReorderItem(val key: Any, val index: Int, val offset: Int, val size: Int)

/**
 * What the engine drives: the items' places along the list, in the list's frame (the frame its
 * pointer positions come in, after [itemAt]'s conversion), the window they're seen through, and
 * how the list scrolls.
 */
internal interface ReorderLayout {
    /** The laid-out item with [key], or null (a lazy list's item scrolled out of view). Reads the layout. */
    fun item(key: Any): ReorderItem?

    /** The laid-out item at [index], or null. */
    fun itemAt(index: Int): ReorderItem?

    /** The item under a touch at [y], in the touch's local coordinates, or null. */
    fun itemUnder(y: Float): ReorderItem?

    /** The part of the list a lifted item stays inside: the visible window, in the list's frame. */
    fun window(): ReorderSpan

    /** Whether the list can scroll in [velocity]'s direction (positive toward the end). */
    fun canScroll(velocity: Float): Boolean

    /**
     * Scrolls by [delta] pixels (positive toward the end) and returns how far that moves the list's
     * own frame toward the start under a still finger: 0 where the items move inside the frame (a
     * lazy list), the scroll consumed where the whole list moves (a column in a page).
     */
    suspend fun scrollBy(delta: Float): Float

    /**
     * How far the list's frame moved toward the start under a still finger since the last call,
     * beyond what [scrollBy] reported. 0 for a list whose frame stays put.
     */
    fun drift(): Float

    /** A touch event has just reported the finger's true place in the list's frame. */
    fun pointerSynced()

    /** About to move the item at [from] to [to]: keep the scroll position steady. */
    fun beforeMove(from: Int, to: Int)
}

/** A `LazyColumn`'s items: positions are the list's own, from the start of its content area. */
private class LazyReorderLayout(private val listState: LazyListState) : ReorderLayout {
    override fun item(key: Any): ReorderItem? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.toItem()

    override fun itemAt(index: Int): ReorderItem? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.toItem()

    override fun itemUnder(y: Float): ReorderItem? {
        val info = listState.layoutInfo
        val position = y + info.viewportStartOffset
        return info.visibleItemsInfo.firstOrNull { position >= it.offset && position < it.offset + it.size }?.toItem()
    }

    override fun window(): ReorderSpan {
        val info = listState.layoutInfo
        return ReorderSpan(info.viewportStartOffset.toFloat(), info.viewportEndOffset.toFloat())
    }

    override fun canScroll(velocity: Float): Boolean = true

    override suspend fun scrollBy(delta: Float): Float {
        listState.scrollBy(delta)
        return 0f
    }

    override fun drift(): Float = 0f

    override fun pointerSynced() = Unit

    override fun beforeMove(from: Int, to: Int) {
        // The list keeps its first visible item in place by key; when that item takes part in the
        // move, pin the position by index instead so the list doesn't jump.
        val first = listState.firstVisibleItemIndex
        if (from == first || to == first) {
            listState.requestScrollToItem(first, listState.firstVisibleItemScrollOffset)
        }
    }

    private fun LazyListItemInfo.toItem(): ReorderItem = ReorderItem(key, index, offset, size)
}

/**
 * A plain column's items in a scrolling page: positions are the column's own, from its top; the
 * window is what's visible of the column (inside every clipping parent, the page's viewport
 * among them); scrolling goes to the page through [dispatcher], and moves the whole column.
 */
private class PageReorderLayout(
    private val dispatcher: NestedScrollDispatcher,
    private val scope: CoroutineScope,
) : ReorderLayout {
    var coordinates: LayoutCoordinates? = null
    var slideSpec: FiniteAnimationSpec<Float> = spring()

    /** The items as last laid out, and a counter that tells readers they changed. */
    private var placed: List<ReorderItem> = emptyList()
    private var version by mutableIntStateOf(0)
    private var versions = 0

    /** The column's top in the root, as expected after the scrolls dispatched so far. */
    private var expectedTop = Float.NaN

    private val slides = HashMap<Any, Slide>()

    /**
     * Records the items' new places and slides each item that moved (but the lifted or settling
     * one, which follows the finger or its own settle) from where it was to its new place.
     */
    fun place(items: List<ReorderItem>, state: ReorderState) {
        if (items == placed) return
        val before = placed.associate { it.key to it.offset }
        placed = items
        versions += 1
        version = versions
        val keys = HashSet<Any>(items.size)
        items.forEach { item ->
            keys += item.key
            val was = before[item.key]
            when {
                state.isLifted(item.key) -> slides.remove(item.key)?.job?.cancel()
                was != null && was != item.offset -> slide(item.key, (was - item.offset).toFloat())
            }
        }
        (slides.keys - keys).forEach { slides.remove(it)?.job?.cancel() }
    }

    /** How far [key]'s item is from its slot while it slides into it. Reads the slide. */
    fun slideOffset(key: Any): Float = slides[key]?.offset ?: 0f

    private fun slide(key: Any, by: Float) {
        val slide = slides.getOrPut(key) { Slide() }
        val from = slide.current + by
        slide.current = from
        slide.offset = from
        slide.job?.cancel()
        val spec = slideSpec
        slide.job = scope.launch {
            slide.animatable.snapTo(from)
            slide.animatable.animateTo(0f, spec) {
                slide.current = value
                slide.offset = value
            }
        }
    }

    private fun items(): List<ReorderItem> {
        // Read so a layer or a gesture that reads the items follows the layout.
        version
        return placed
    }

    override fun item(key: Any): ReorderItem? = items().firstOrNull { it.key == key }

    override fun itemAt(index: Int): ReorderItem? = items().getOrNull(index)

    override fun itemUnder(y: Float): ReorderItem? = items().firstOrNull { y >= it.offset && y < it.offset + it.size }

    override fun window(): ReorderSpan {
        val column = coordinates?.takeIf { it.isAttached } ?: return ReorderSpan(0f, 0f)
        val height = column.size.height.toFloat()
        val root = column.findRootCoordinates()
        val visible = root.localBoundingBoxOf(column, clipBounds = true)
        if (visible.height <= 0f) return ReorderSpan(0f, 0f)
        val top = column.localPositionOf(root, Offset(visible.left, visible.top)).y
        val bottom = column.localPositionOf(root, Offset(visible.left, visible.bottom)).y
        return ReorderSpan(top.coerceIn(0f, height), bottom.coerceIn(0f, height))
    }

    override fun canScroll(velocity: Float): Boolean {
        val column = coordinates?.takeIf { it.isAttached } ?: return false
        val window = window()
        // Only toward an edge where more of the list is hidden.
        return if (velocity < 0f) window.start > 0.5f else window.end < column.size.height - 0.5f
    }

    override suspend fun scrollBy(delta: Float): Float {
        // Nested scrolling speaks in finger movement: content scrolling toward the end is a finger
        // moving up.
        val available = Offset(0f, -delta)
        val pre = dispatcher.dispatchPreScroll(available, NestedScrollSource.UserInput)
        val post = dispatcher.dispatchPostScroll(Offset.Zero, available - pre, NestedScrollSource.UserInput)
        val moved = -(pre.y + post.y)
        if (!expectedTop.isNaN()) expectedTop -= moved
        return moved
    }

    override fun drift(): Float {
        val top = columnTop()
        val expected = expectedTop
        expectedTop = top
        return if (top.isNaN() || expected.isNaN()) 0f else expected - top
    }

    override fun pointerSynced() {
        expectedTop = columnTop()
    }

    override fun beforeMove(from: Int, to: Int) = Unit

    private fun columnTop(): Float = coordinates?.takeIf { it.isAttached }?.positionInRoot()?.y ?: Float.NaN

    /** An item sliding into its slot: its offset from it, as state for placement and as a plain value. */
    private class Slide {
        var offset by mutableFloatStateOf(0f)
        var current = 0f
        val animatable = Animatable(0f)
        var job: Job? = null
    }
}

/**
 * The drag in progress: which item is lifted, where the finger is, and the settle after a drop.
 * Positions are the list's own frame ([ReorderLayout]), so a scroll moves the items (a lazy list)
 * or the frame under the finger (a column in a page), never the item's place under the finger.
 */
@Stable
internal class ReorderState(
    private val layout: ReorderLayout,
    private val scope: CoroutineScope,
) {
    // Set by the list after every composition.
    var onMove: (from: Int, to: Int) -> Unit = { _, _ -> }
    var enabled: Boolean = true
    var rtl: Boolean = false
    var haptics: HapticFeedback? = null
    var settleSpec: FiniteAnimationSpec<Float> = spring()
    var handleZonePx: Float = 0f
    var edgeZonePx: Float = 0f
    var maxScrollVelocityPx: Float = 0f

    /** The lifted item's key, or null. */
    var liftedKey: Any? by mutableStateOf(null)
        private set

    /** The key of the item settling into its slot after a drop, or null. */
    var settlingKey: Any? by mutableStateOf(null)
        private set

    private var pointer by mutableFloatStateOf(0f)
    private var pointerStart = 0f
    private var liftedStart = 0f
    private var lastOffset = 0f
    private var expectedIndex = -1
    private val settle = Animatable(0f)
    private var autoScroll: Job? = null
    private var settleJob: Job? = null

    /** The settling item's offset from its slot. */
    val settleOffset: Float get() = settle.value

    /** Whether [key]'s item is lifted or still settling. */
    fun isLifted(key: Any): Boolean = key == liftedKey || key == settlingKey

    /**
     * The lifted item's offset from its slot: under the finger, kept inside the list's window.
     * Reads the list's layout, so a layer that reads it redraws as the item and the list move.
     */
    fun liftedOffset(): Float {
        val key = liftedKey ?: return 0f
        val item = layout.item(key) ?: return lastOffset
        val window = layout.window()
        val top = clampLiftedTop(
            top = liftedStart + pointer - pointerStart,
            size = item.size.toFloat(),
            viewportStart = window.start,
            viewportEnd = window.end,
        )
        lastOffset = top - item.offset
        return lastOffset
    }

    /** The item under [y], in the list's local coordinates, or null. */
    fun itemAt(y: Float): ReorderItem? = layout.itemUnder(y)

    /** Whether [x] falls in the handle's zone at the trailing edge of a list [width] wide. */
    fun isOnHandle(x: Float, width: Float): Boolean = if (rtl) x <= handleZonePx else x >= width - handleZonePx

    /** Lifts [key]'s item under a finger at [pointerY]. */
    fun lift(key: Any, pointerY: Float, viaHandle: Boolean) {
        val item = layout.item(key) ?: return
        var top = item.offset.toFloat()
        if (key == settlingKey) {
            top += settle.value
            settleJob?.cancel()
            settlingKey = null
        }
        liftedKey = key
        expectedIndex = item.index
        pointerStart = pointerY
        pointer = pointerY
        layout.pointerSynced()
        liftedStart = top
        lastOffset = top - item.offset
        haptics?.performHapticFeedback(
            if (viaHandle) HapticFeedbackType.GestureThresholdActivate else HapticFeedbackType.LongPress,
        )
        autoScroll?.cancel()
        autoScroll = scope.launch { autoScrollWhileLifted() }
    }

    /** Moves the lifted item with the finger, now at [pointerY]. */
    fun drag(pointerY: Float) {
        if (liftedKey == null) return
        pointer = pointerY
        layout.pointerSynced()
        moveIfPassed()
    }

    /** Lets go: the item settles into its slot. */
    fun drop() {
        val key = liftedKey ?: return
        val from = liftedOffset()
        autoScroll?.cancel()
        autoScroll = null
        liftedKey = null
        settleJob?.cancel()
        settlingKey = key
        // Undispatched, so the settle starts from where the item was in this same frame.
        settleJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                settle.snapTo(from)
                settle.animateTo(0f, settleSpec)
            } finally {
                if (settlingKey == key) settlingKey = null
            }
        }
        haptics?.performHapticFeedback(HapticFeedbackType.GestureEnd)
    }

    /** Trades places with the neighbor the lifted item has passed, if any. */
    private fun moveIfPassed() {
        val key = liftedKey ?: return
        val lifted = layout.item(key) ?: return
        // The last move hasn't reached the layout yet.
        if (lifted.index != expectedIndex) return
        val top = lifted.offset + liftedOffset()
        val step = reorderStep(
            liftedTop = top,
            liftedBottom = top + lifted.size,
            above = layout.itemAt(lifted.index - 1)?.span(),
            below = layout.itemAt(lifted.index + 1)?.span(),
        )
        if (step == 0) return
        val target = lifted.index + step
        layout.beforeMove(lifted.index, target)
        expectedIndex = target
        onMove(lifted.index, target)
        haptics?.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    /** Scrolls the list each frame while the lifted item is pressed into its top or bottom edge. */
    private suspend fun autoScrollWhileLifted() {
        var previous = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - previous) / 1_000_000_000f
            previous = now
            // Where the list's frame moved under the still finger, the finger is that much further along it.
            pointer += layout.drift()
            val velocity = autoScrollVelocity()
            if (velocity != 0f && layout.canScroll(velocity)) {
                pointer += layout.scrollBy(velocity * seconds)
                moveIfPassed()
            }
        }
    }

    private fun autoScrollVelocity(): Float {
        val key = liftedKey ?: return 0f
        val item = layout.item(key) ?: return 0f
        val travel = pointer - pointerStart
        val top = liftedStart + travel
        val window = layout.window()
        return reorderAutoScrollVelocity(
            liftedTop = top,
            liftedBottom = top + item.size,
            viewportStart = window.start,
            viewportEnd = window.end,
            edge = minOf(edgeZonePx, (window.end - window.start) / 4f),
            maxVelocity = maxScrollVelocityPx,
            travel = travel,
        )
    }

    private fun ReorderItem.span(): ReorderSpan = ReorderSpan(offset.toFloat(), (offset + size).toFloat())
}

/**
 * The list's drag gestures, on the list itself so positions stay in its frame while items move.
 * It watches each touch in the initial pass, ahead of the items' own clicks and swipes and the
 * scroll (the list's or the page's), and takes nothing until a drag starts: from the handle's zone
 * once the touch moves past the touch slop, anywhere else once it's held still for the long-press
 * timeout. From then on it takes the whole touch.
 */
private fun Modifier.reorderGestures(state: ReorderState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (!state.enabled) return@awaitEachGesture
        val item = state.itemAt(down.position.y) ?: return@awaitEachGesture
        val viaHandle = state.isOnHandle(down.position.x, size.width.toFloat())
        val lifts = if (viaHandle) {
            awaitSlop(down, viewConfiguration.touchSlop)
        } else {
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                awaitLetGo(down, viewConfiguration.touchSlop)
            } == null
        }
        if (!lifts) return@awaitEachGesture
        state.lift(item.key, down.position.y, viaHandle)
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                event.changes.forEach { it.consume() }
                if (change == null || !change.pressed) break
                state.drag(change.position.y)
            }
        } finally {
            state.drop()
        }
    }
}

/**
 * Waits for the touch that went [down] to move past [touchSlop], and takes that move. False if it
 * lifts first or something else takes it.
 */
private suspend fun AwaitPointerEventScope.awaitSlop(down: PointerInputChange, touchSlop: Float): Boolean {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return false
        if (change.changedToUpIgnoreConsumed() || change.isConsumed) return false
        if ((change.position - down.position).getDistance() > touchSlop) {
            change.consume()
            return true
        }
    }
}

/**
 * Waits until the touch that went [down] lets go of the item: it lifts, moves past [touchSlop], or
 * something else (a scroll, a swipe) takes it. A hold is whatever outlasts this.
 */
private suspend fun AwaitPointerEventScope.awaitLetGo(down: PointerInputChange, touchSlop: Float): Boolean {
    while (true) {
        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: return true
        if (change.changedToUpIgnoreConsumed() || change.isConsumed) return true
        if ((change.position - down.position).getDistance() > touchSlop) return true
    }
}

/** One item's span along the list, start to end, in pixels. */
internal class ReorderSpan(val start: Float, val end: Float) {
    val middle: Float get() = (start + end) / 2f
}

/**
 * The step the lifted item takes: +1 once its bottom passes the middle of the item [below], -1
 * once its top passes the middle of the item [above], else 0. Measuring against each neighbor's
 * own middle keeps a short item and a tall one from trading places back and forth: after a
 * trade, the way back needs the item to retreat past the same point.
 */
internal fun reorderStep(liftedTop: Float, liftedBottom: Float, above: ReorderSpan?, below: ReorderSpan?): Int = when {
    below != null && liftedBottom > below.middle -> 1
    above != null && liftedTop < above.middle -> -1
    else -> 0
}

/**
 * Keeps a lifted item of [size] inside the list's window: its top no higher than [viewportStart]
 * and its bottom no lower than [viewportEnd]. An item taller than the window isn't held.
 */
internal fun clampLiftedTop(top: Float, size: Float, viewportStart: Float, viewportEnd: Float): Float {
    val highest = viewportEnd - size
    return if (highest < viewportStart) top else top.coerceIn(viewportStart, highest)
}

/**
 * How fast the list scrolls under a lifted item, in pixels per second: positive toward the end.
 * It scrolls only toward the edge the item is being dragged to ([travel] since the lift), once the
 * item reaches within [edge] of it, faster the deeper it goes, up to [maxVelocity].
 */
internal fun reorderAutoScrollVelocity(
    liftedTop: Float,
    liftedBottom: Float,
    viewportStart: Float,
    viewportEnd: Float,
    edge: Float,
    maxVelocity: Float,
    travel: Float,
): Float {
    if (edge <= 0f) return 0f
    if (travel > 0f) {
        val depth = liftedBottom - (viewportEnd - edge)
        if (depth > 0f) return maxVelocity * (depth / edge).coerceAtMost(1f)
    }
    if (travel < 0f) {
        val depth = (viewportStart + edge) - liftedTop
        if (depth > 0f) return -maxVelocity * (depth / edge).coerceAtMost(1f)
    }
    return 0f
}
