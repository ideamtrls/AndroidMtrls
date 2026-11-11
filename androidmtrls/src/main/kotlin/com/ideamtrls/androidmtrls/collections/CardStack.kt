package com.ideamtrls.androidmtrls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * The side an [AMCardStack]'s top card was swiped toward. What each means is the caller's.
 * Directions follow the layout, the way Material's swipe-to-dismiss names them.
 */
public enum class AMCardStackSwipeDirection {
    /** Toward the end: to the right in a left-to-right layout, to the left in right to left. */
    StartToEnd,

    /** Toward the start: to the left in a left-to-right layout, to the right in right to left. */
    EndToStart,
}

/**
 * A swipeable deck that hands each swipe to the caller: the top card, with the cards after it
 * peeking out below, each a step smaller and fainter. The top card follows a horizontal drag,
 * tilting toward it. Let go past a quarter of its width, or flung, it flies off that way and the
 * deck calls [onSwipe]; short of that it springs home.
 *
 * ```kotlin
 * AMCardStack(
 *     suggestions,
 *     onSwipe = { song, direction ->
 *         if (direction == AMCardStackSwipeDirection.StartToEnd) save(song)
 *         suggestions = suggestions - song
 *     },
 *     modifier = Modifier.fillMaxWidth().height(440.dp),
 *     key = { it.id },
 *     startToEndActionLabel = "Save",
 *     endToStartActionLabel = "Skip",
 * ) { song -> SongCard(song) }
 * ```
 *
 * - **What a swipe means is yours.** The first item is the top card. Remove the item in [onSwipe]
 *   and the next card comes up; keep it and the card comes back.
 * - **Cards.** Every card fills the deck, so give the deck a size; that uniformity is what makes
 *   the peeks read as a stack. A card is the caller's, surface included (`Modifier.amCard()`).
 *   Each keeps its identity, and its state, as it moves up. [key] names an item's identity; by
 *   default the item itself, so items must be distinct.
 * - **Only the top card** takes touches, and only a horizontal drag moves it, so a vertical drag
 *   that starts on the deck scrolls the screen. The drag tracks the finger; the fling, the spring
 *   home and the cards moving up use Material's motion scheme.
 * - **TalkBack** reads the top card alone, as one element, and offers the two swipes as custom
 *   actions on it, named by [startToEndActionLabel] and [endToStartActionLabel] ("Swipe right" and
 *   "Swipe left" in a left-to-right layout by default). Say what they mean.
 *
 * @param items the deck, top card first.
 * @param onSwipe called with the top card's item, and which way it went, once it has flown off.
 * @param modifier the deck's placement and size.
 * @param key an item's identity. Null uses the item itself.
 * @param visibleDepth how many cards draw at once, the top included; at least 1.
 * @param startToEndActionLabel TalkBack's name for a swipe toward the end. Null names the swipe.
 * @param endToStartActionLabel TalkBack's name for a swipe toward the start. Null names the swipe.
 * @param content one card.
 */
@Composable
public fun <T> AMCardStack(
    items: List<T>,
    onSwipe: (item: T, direction: AMCardStackSwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    visibleDepth: Int = 3,
    startToEndActionLabel: String? = null,
    endToStartActionLabel: String? = null,
    content: @Composable (item: T) -> Unit,
) {
    CardDeck(
        items = items,
        key = key,
        visibleDepth = visibleDepth,
        mode = DeckMode.Event(onSwipe, startToEndActionLabel, endToStartActionLabel),
        modifier = modifier,
        content = content,
    )
}

/**
 * A swipeable deck bound to an index: the card at [currentIndex] is on top and the deck stays
 * whole. A swipe toward the start flies the top card off, then moves to the next card; a swipe
 * toward the end springs it home and moves to the previous card at once, which comes in from the
 * start side, over it. A direction with no card to move to springs back.
 *
 * ```kotlin
 * var index by rememberSaveable { mutableIntStateOf(0) }
 * AMCardStack(tracks, currentIndex = index, onCurrentIndexChange = { index = it }, Modifier.fillMaxWidth().height(320.dp), key = { it.id }) { track ->
 *     CoverCard(track)
 * }
 * ```
 *
 * - **Your own changes move it the same way**, so buttons beside the deck step it too: a step
 *   forward sends the top card off toward the start, a step back brings the previous card in from
 *   the start side. Any other change moves the cards that stay to their new places and fades the
 *   rest in or out; a top card that leaves goes off toward the start. With [visibleDepth] 1, a step
 *   back sends the top card off toward the end while the previous one fades in.
 * - **The index.** The deck only reports indices inside the deck, and draws an index out of range
 *   clamped. [wraps] steps from the last card to the first and back, drawing the start of the deck
 *   behind the last card (never one card twice); a single card never steps.
 * - Cards, touch and motion are those of the deck that hands each swipe to the caller.
 * - **TalkBack** reads the top card alone and offers "Next" and "Previous" on it, each only when
 *   there's a card to move to.
 *
 * @param items the deck, in order.
 * @param currentIndex the top card's index.
 * @param onCurrentIndexChange called with the index a swipe moves to.
 * @param modifier the deck's placement and size.
 * @param key an item's identity. Null uses the item itself.
 * @param visibleDepth how many cards draw at once, the top included; at least 1.
 * @param wraps whether the deck steps round from the last card to the first and back.
 * @param content one card.
 */
@Composable
public fun <T> AMCardStack(
    items: List<T>,
    currentIndex: Int,
    onCurrentIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((item: T) -> Any)? = null,
    visibleDepth: Int = 3,
    wraps: Boolean = false,
    content: @Composable (item: T) -> Unit,
) {
    CardDeck(
        items = items,
        key = key,
        visibleDepth = visibleDepth,
        mode = DeckMode.Bound(currentIndex, onCurrentIndexChange, wraps),
        modifier = modifier,
        content = content,
    )
}

// Per-fling tuning: the deck's physicality is one piece (the peek, the tilt, the commit
// thresholds in `CardStackCommit` and the fling lean on each other), so none of it is a token.

/** Each card behind the top is this much smaller per step back. */
private const val DepthScaleFactor = 0.96f

/** Each card behind the top drops this far per step back: what makes the peeks show below. */
private val DepthYOffset = 8.dp

/** Each card behind the top is this much fainter per step back. */
private const val DepthOpacityFactor = 0.72f

/** The dragged card's tilt, at most: enough to feel physical, restrained enough to stay quiet. */
private const val MaxTiltDegrees = 12f

/** The fraction of the card's width a drag travels before the tilt is full. */
private const val TiltFullAtWidthFraction = 0.5f

/** How far a flung card travels, as a fraction of its width: well past the edge, corners and all. */
private const val FlingDistanceFraction = 1.6f

/** How much of the fling's predicted vertical travel carries into its exit, so it follows the finger's diagonal. */
private const val FlingVerticalCarry = 0.3f

/** What the deck does with a swipe. */
private sealed interface DeckMode<T> {
    /** Hands each swipe to the caller, and names the two swipes for TalkBack. */
    class Event<T>(
        val onSwipe: (T, AMCardStackSwipeDirection) -> Unit,
        val startToEndLabel: String?,
        val endToStartLabel: String?,
    ) : DeckMode<T>

    /** Steps through the deck by reporting indices. */
    class Bound<T>(val currentIndex: Int, val onCurrentIndexChange: (Int) -> Unit, val wraps: Boolean) : DeckMode<T>
}

/** What releasing the top card one way does. */
private enum class CardRelease {
    /** Fly off, then report. */
    Fling,

    /** Spring home and report at once: a bound step back. */
    Settle,

    /** Spring home; there's nowhere to go. */
    Reject,
}

/** What a release does each way. */
private class CardReleases(val startToEnd: CardRelease, val endToStart: CardRelease) {
    operator fun get(direction: AMCardStackSwipeDirection): CardRelease =
        if (direction == AMCardStackSwipeDirection.StartToEnd) startToEnd else endToStart
}

/** A TalkBack action on the top card: its name, and the swipe it performs. */
private class CardAction(val label: String, val direction: AMCardStackSwipeDirection)

/** How a card that has left the drawn window goes. */
private sealed interface CardLeaving {
    /** The top card leaving a bound deck: it flies off toward [toward]. */
    data class Fly(val toward: AMCardStackSwipeDirection) : CardLeaving

    /** Any other card: it fades where it was. */
    data object Fade : CardLeaving
}

/**
 * One card the deck draws: its identity, its item, how deep it sits, and, for a card that has left
 * the window, how it goes. [enters] and [fadesIn] say how a card new to the deck appears.
 */
private class DrawnCard<T>(
    val card: CardStackCard<Any>,
    val item: T,
    val depth: Int,
    val zIndex: Float,
    val leaving: CardLeaving? = null,
    val enters: Boolean = false,
    val fadesIn: Boolean = false,
) {
    val isTop: Boolean get() = depth == 0 && leaving == null
}

/** The top card, and where a step from it goes in a bound deck. */
private class LiveTop(val card: CardStackCard<Any>?, val next: Int?, val previous: Int?)

/** A deck's state across compositions: its bookkeeping, the cards it drew, the ones still leaving, and its size. */
@Stable
private class Deck<T> {
    val book = CardStackBook<Any>()
    var settled: List<DrawnCard<T>> = emptyList()
    val departures = mutableStateListOf<DrawnCard<T>>()
    var size by mutableStateOf(IntSize.Zero)
}

/** Stands in for a null item as its own identity. */
private object NullItemKey

@Composable
private fun <T> CardDeck(
    items: List<T>,
    key: ((item: T) -> Any)?,
    visibleDepth: Int,
    mode: DeckMode<T>,
    modifier: Modifier,
    content: @Composable (item: T) -> Unit,
) {
    val depth = visibleDepth.coerceAtLeast(1)
    val count = items.size
    val bound = mode as? DeckMode.Bound<T>
    val index = if (bound != null) clampIndex(bound.currentIndex, count) else 0
    // A bound deck reads at least two cards, so a one-deep deck can still tell a step back from a
    // step forward; only `depth` of them draw.
    val window = if (bound != null) {
        CardStackWindow.indices(count, index, maxOf(2, depth), bound.wraps)
    } else {
        CardStackWindow.indices(count, 0, depth, wraps = false)
    }
    val identity: (T) -> Any = { item -> if (key != null) key(item) else item ?: NullItemKey }

    val deck = remember { Deck<T>() }
    val read = deck.book.read(window.map { identity(items[it]) }, depth, bound = bound != null)
    val firstDraw = !deck.book.hasSettled
    val live = read.pass.cards.mapIndexed { at, card ->
        val enters = at == 0 && read.enters
        DrawnCard(
            card = card,
            item = items[window[at]],
            depth = at,
            zIndex = -at.toFloat(),
            enters = enters,
            fadesIn = !firstDraw && !enters && read.isNew(card),
        )
    }
    // The cards leaving in this composition stay drawn, under the same identity, until they've gone.
    val leavingNow = read.leaving.mapNotNull { card ->
        val shown = deck.settled.firstOrNull { it.card == card } ?: return@mapNotNull null
        val wasTop = card == read.pass.leaving
        val before = read.depthBefore(card)
        DrawnCard(
            card = card,
            item = shown.item,
            depth = before,
            zIndex = if (wasTop) 1f else -before - 0.5f,
            leaving = if (wasTop && bound != null) CardLeaving.Fly(read.pass.leavesToward) else CardLeaving.Fade,
        )
    }

    val next = bound?.let { CardStackWindow.next(index, count, it.wraps) }
    val previous = bound?.let { CardStackWindow.previous(index, count, it.wraps) }
    val releases = if (bound == null) {
        CardReleases(CardRelease.Fling, CardRelease.Fling)
    } else {
        CardReleases(
            startToEnd = if (previous == null) CardRelease.Reject else CardRelease.Settle,
            endToStart = if (next == null) CardRelease.Reject else CardRelease.Fling,
        )
    }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val actions = when (mode) {
        is DeckMode.Event -> listOf(
            CardAction(mode.startToEndLabel ?: swipeName(AMCardStackSwipeDirection.StartToEnd, rtl), AMCardStackSwipeDirection.StartToEnd),
            CardAction(mode.endToStartLabel ?: swipeName(AMCardStackSwipeDirection.EndToStart, rtl), AMCardStackSwipeDirection.EndToStart),
        )
        is DeckMode.Bound -> listOfNotNull(
            CardAction("Next", AMCardStackSwipeDirection.EndToStart).takeIf { next != null },
            CardAction("Previous", AMCardStackSwipeDirection.StartToEnd).takeIf { previous != null },
        )
    }

    // Read when a release reports, which can be well after the composition that started it.
    val liveTop = rememberUpdatedState(LiveTop(read.pass.cards.firstOrNull(), next, previous))
    val liveMode = rememberUpdatedState(mode)
    // A release is reported only while its card is still the top, so the caller is never handed a
    // card that has gone, and a bound step always moves from the card on top.
    val report: (CardStackCard<Any>, T, AMCardStackSwipeDirection) -> Unit = report@{ card, item, direction ->
        val top = liveTop.value
        if (top.card != card) return@report
        when (val current = liveMode.value) {
            is DeckMode.Event -> current.onSwipe(item, direction)
            is DeckMode.Bound -> when (direction) {
                AMCardStackSwipeDirection.EndToStart -> top.next?.let {
                    deck.book.swipedBackFrom = null
                    current.onCurrentIndexChange(it)
                }
                AMCardStackSwipeDirection.StartToEnd -> top.previous?.let {
                    deck.book.swipedBackFrom = card.id
                    current.onCurrentIndexChange(it)
                }
            }
        }
    }

    Box(modifier.onSizeChanged { deck.size = it }) {
        // One keyed list, so a card keeps its state as it moves up, and as it leaves.
        for (drawn in live + deck.departures + leavingNow) {
            key(drawn.card) {
                DeckCard(
                    drawn = drawn,
                    deck = deck,
                    releases = releases,
                    actions = if (drawn.isTop) actions else emptyList(),
                    onReport = { direction -> report(drawn.card, drawn.item, direction) },
                    onGone = { deck.departures.removeAll { it.card == drawn.card } },
                    content = content,
                )
            }
        }
    }

    SideEffect {
        deck.book.commit(read)
        deck.settled = live
        if (leavingNow.isNotEmpty()) deck.departures.addAll(leavingNow)
    }
}

/** A card's motion, owned by the card so a drag frame redraws that card alone. */
@Stable
private class CardMotion(depth: Int, enters: Boolean, fadesIn: Boolean) {
    /** The finger's offset, and where a release's spring or fling has carried the card. */
    var drag by mutableStateOf(Offset.Zero)

    /** The depth the card is drawn at, moving to its new one when the window moves. */
    val depth = Animatable(depth.toFloat())

    /** 1 → 0 as a card stepping back comes in from the start side. */
    val enter = Animatable(if (enters) 1f else 0f)

    /** 0 → 1 as a top card that left flies off. */
    val exit = Animatable(0f)

    /** The card's own opacity: fading in as it arrives, out as it goes. */
    val fade = Animatable(if (fadesIn) 0f else 1f)

    /** The release animation running, which a new touch interrupts. */
    var release: Job? = null

    /** Set while a committed release flies the card off: it takes no new touch or action. */
    var committed = false
}

@Composable
private fun <T> DeckCard(
    drawn: DrawnCard<T>,
    deck: Deck<T>,
    releases: CardReleases,
    actions: List<CardAction>,
    onReport: (AMCardStackSwipeDirection) -> Unit,
    onGone: () -> Unit,
    content: @Composable (item: T) -> Unit,
) {
    val motion = remember { CardMotion(drawn.depth, drawn.enters, drawn.fadesIn) }
    val scheme = MaterialTheme.motionScheme
    val scope = rememberCoroutineScope()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isTop = drawn.isTop
    val leaving = drawn.leaving
    val isLiveTop = rememberUpdatedState(isTop)
    val currentOnGone by rememberUpdatedState(onGone)

    LaunchedEffect(motion) {
        if (motion.enter.value != 0f) launch { motion.enter.animateTo(0f, scheme.defaultSpatialSpec()) }
        if (motion.fade.value != 1f) launch { motion.fade.animateTo(1f, scheme.defaultEffectsSpec()) }
    }
    LaunchedEffect(drawn.depth) {
        motion.depth.animateTo(drawn.depth.toFloat(), scheme.defaultSpatialSpec())
    }
    LaunchedEffect(leaving) {
        when (leaving) {
            null -> Unit
            is CardLeaving.Fly -> {
                motion.exit.animateTo(1f, scheme.defaultSpatialSpec())
                currentOnGone()
            }
            CardLeaving.Fade -> {
                motion.fade.animateTo(0f, scheme.defaultEffectsSpec())
                currentOnGone()
            }
        }
    }

    // What a release does, current as of the latest composition: the drag and TalkBack both call it.
    val release by rememberUpdatedState { direction: AMCardStackSwipeDirection, velocity: Velocity, predictedY: Float ->
        if (!motion.committed) {
            when (releases[direction]) {
                CardRelease.Fling -> {
                    motion.committed = true
                    motion.release?.cancel()
                    motion.release = scope.launch {
                        motion.flingOff(direction, velocity, predictedY, deck.size.width.toFloat(), rtl, scheme)
                        onReport(direction)
                        // A step that took makes the card a departure, which carries it on from
                        // off-screen. Otherwise (the caller kept it, or the step didn't take) it
                        // comes back once the composition has caught up.
                        withFrameNanos { }
                        withFrameNanos { }
                        if (isLiveTop.value) {
                            motion.committed = false
                            motion.springHome(Velocity.Zero, scheme)
                        }
                    }
                }
                CardRelease.Settle -> {
                    motion.release?.cancel()
                    motion.release = scope.launch { motion.springHome(velocity, scheme) }
                    onReport(direction)
                }
                CardRelease.Reject -> {
                    motion.release?.cancel()
                    motion.release = scope.launch { motion.springHome(velocity, scheme) }
                }
            }
        }
    }

    // Outside the card's transforms, so the drag reads the finger in the deck's own coordinates.
    val gesture = if (isTop) {
        Modifier.pointerInput(motion, rtl) {
            val decay = splineBasedDecay<Float>(this)
            val toEnd = if (rtl) -1f else 1f
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (motion.committed) return@awaitEachGesture
                val tracker = VelocityTracker()
                tracker.addPointerInputChange(down)
                var overSlop = 0f
                // Only a move that leans horizontal takes the card; a vertical one goes to the
                // scrolling screen around the deck.
                val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                    change.consume()
                    overSlop = over
                } ?: return@awaitEachGesture
                if (motion.committed) return@awaitEachGesture
                motion.release?.cancel()
                tracker.addPointerInputChange(start)
                motion.drag += Offset(overSlop, 0f)
                val lifted = drag(start.id) { change ->
                    tracker.addPointerInputChange(change)
                    motion.drag += change.positionChange()
                    change.consume()
                }
                val velocity = if (lifted) tracker.calculateVelocity() else Velocity.Zero
                // The side comes from where the fling would coast to, the threshold from the travel.
                val dx = motion.drag.x * toEnd
                val predicted = dx + decay.calculateTargetValue(0f, velocity.x * toEnd)
                val side = CardStackCommit.side(dx, predicted, size.width.toFloat())
                if (lifted && side != null) {
                    release(side, velocity, decay.calculateTargetValue(0f, velocity.y))
                } else {
                    motion.release = scope.launch { motion.springHome(velocity, scheme) }
                }
            }
        }
    } else {
        Modifier
    }

    // The top card is one element carrying the swipes; every other card is hidden from TalkBack.
    val semantics = if (isTop) {
        Modifier.semantics(mergeDescendants = true) {
            customActions = actions.map { action ->
                CustomAccessibilityAction(action.label) {
                    release(action.direction, Velocity.Zero, 0f)
                    true
                }
            }
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }

    val exitSign = (leaving as? CardLeaving.Fly)?.toward?.physicalSign(rtl) ?: 0f
    Box(
        Modifier
            .zIndex(drawn.zIndex)
            .fillMaxSize()
            .then(gesture)
            .graphicsLayer {
                val width = deck.size.width.toFloat()
                val toStart = if (rtl) 1f else -1f
                val drag = motion.drag
                val tilt = if (width > 0f) (drag.x / (width * TiltFullAtWidthFraction)).coerceIn(-1f, 1f) else 0f
                // The entry and the exit are one pose each: tilted toward a side about the card's
                // center, and carried past it.
                val pose = toStart * motion.enter.value + exitSign * motion.exit.value
                val at = motion.depth.value
                rotationZ = (tilt + pose) * MaxTiltDegrees
                translationX = drag.x + pose * width * FlingDistanceFraction
                translationY = at * DepthYOffset.toPx() + drag.y
                val scale = DepthScaleFactor.pow(at)
                scaleX = scale
                scaleY = scale
                alpha = DepthOpacityFactor.pow(at) * motion.fade.value
            }
            .then(semantics),
    ) {
        content(drawn.item)
    }
}

/** Flies the card off toward [direction] from where the release left it, carrying its velocity. */
private suspend fun CardMotion.flingOff(
    direction: AMCardStackSwipeDirection,
    velocity: Velocity,
    predictedY: Float,
    width: Float,
    rtl: Boolean,
    scheme: MotionScheme,
) {
    val target = Offset(
        x = direction.physicalSign(rtl) * width * FlingDistanceFraction,
        y = drag.y + predictedY * FlingVerticalCarry,
    )
    animate(Offset.VectorConverter, drag, target, Offset(velocity.x, velocity.y), scheme.fastSpatialSpec()) { value, _ ->
        drag = value
    }
}

/** Springs the card back to rest, carrying the release's velocity. */
private suspend fun CardMotion.springHome(velocity: Velocity, scheme: MotionScheme) {
    animate(Offset.VectorConverter, drag, Offset.Zero, Offset(velocity.x, velocity.y), scheme.defaultSpatialSpec()) { value, _ ->
        drag = value
    }
}

/** +1 when [this] direction points right on screen, -1 when it points left. */
private fun AMCardStackSwipeDirection.physicalSign(rtl: Boolean): Float {
    val toEnd = if (rtl) -1f else 1f
    return if (this == AMCardStackSwipeDirection.StartToEnd) toEnd else -toEnd
}

/** TalkBack's default name for a swipe: the way it goes on screen. */
private fun swipeName(direction: AMCardStackSwipeDirection, rtl: Boolean): String =
    if (direction.physicalSign(rtl) > 0f) "Swipe right" else "Swipe left"
