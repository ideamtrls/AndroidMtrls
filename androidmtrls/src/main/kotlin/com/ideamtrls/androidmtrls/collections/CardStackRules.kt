package com.ideamtrls.androidmtrls

// The card stack's rules, apart from the composable so they're unit-tested: whether a release
// commits, which cards a deck draws, how its drawn window moved, which instance of each card it
// draws, and the bookkeeping that carries all of that from one composition to the next.

/**
 * Whether a released drag commits, and toward which side. Travel is measured along the card's
 * start-to-end axis: positive toward the end side, whichever physical side that is.
 *
 * The predicted end (the travel plus the fling's coast) picks the side, so a quick flick that
 * ends with little travel goes the way its velocity says. The release then commits only past a
 * threshold on that side: travel past [DistanceFraction] of the width, or a predicted end past
 * [VelocityFraction] of it. A drag that reverses before it's let go commits only if it clears its
 * new side, and otherwise springs home.
 */
internal object CardStackCommit {
    /** Travel, as a fraction of the card's width, past which a release commits. */
    const val DistanceFraction: Float = 0.25f

    /** Predicted travel, as a fraction of the width, past which a quick flick commits. */
    const val VelocityFraction: Float = 0.6f

    /**
     * The side a release with travel [dx] and predicted end [predictedDx] (both toward the end)
     * on a card [width] wide commits to, or null to spring home.
     */
    fun side(dx: Float, predictedDx: Float, width: Float): AMCardStackSwipeDirection? {
        if (width <= 0f) return null
        val side = if (predictedDx >= 0f) AMCardStackSwipeDirection.StartToEnd else AMCardStackSwipeDirection.EndToStart
        val sign = if (side == AMCardStackSwipeDirection.StartToEnd) 1f else -1f
        val beyondDistance = dx * sign > width * DistanceFraction
        val beyondVelocity = predictedDx * sign > width * VelocityFraction
        return if (beyondDistance || beyondVelocity) side else null
    }
}

/** A deck's index rules: the cards it draws, and where a step from the top goes. */
internal object CardStackWindow {
    /**
     * The indices a deck draws, top first: from the clamped [currentIndex], up to [visibleDepth]
     * cards (at least 1), continuing from the start of the deck when [wraps], and never one card
     * twice. Empty for an empty deck.
     */
    fun indices(count: Int, currentIndex: Int, visibleDepth: Int, wraps: Boolean): List<Int> {
        if (count <= 0) return emptyList()
        val start = clampIndex(currentIndex, count)
        val available = if (wraps) count else count - start
        return List(minOf(visibleDepth.coerceAtLeast(1), available)) { (start + it) % count }
    }

    /** The card after [index], round to the first when [wraps]; null with nowhere to go. A single card never steps. */
    fun next(index: Int, count: Int, wraps: Boolean): Int? {
        if (count <= 1) return null
        val at = clampIndex(index, count)
        return if (at < count - 1) at + 1 else if (wraps) 0 else null
    }

    /** The card before [index], round to the last when [wraps]; null with nowhere to go. A single card never steps. */
    fun previous(index: Int, count: Int, wraps: Boolean): Int? {
        if (count <= 1) return null
        val at = clampIndex(index, count)
        return if (at > 0) at - 1 else if (wraps) count - 1 else null
    }
}

/** How a bound deck's window moved, read from its ids. */
internal enum class CardStackStep {
    /** The new top was the old second card. */
    Forward,

    /** The new second card is the old top: the new top stepped back in. */
    Back,

    /** Anything else: a jump, a reorder, a new deck. */
    Other;

    companion object {
        /**
         * The move from [old] to [new]. When both a step back and a step forward fit (a two-card
         * wrapping deck), [preferBack] decides; the deck sets it only right after its own swipe
         * back.
         */
        fun <ID> between(old: List<ID>, new: List<ID>, preferBack: Boolean): CardStackStep {
            val isBack = new.size > 1 && old.isNotEmpty() && old[0] == new[1]
            val isForward = old.size > 1 && new.isNotEmpty() && new[0] == old[1]
            return when {
                isBack && isForward -> if (preferBack) Back else Forward
                isBack -> Back
                isForward -> Forward
                else -> Other
            }
        }
    }
}

/** A drawn card's identity: its item's id and the lap it's on. */
internal data class CardStackCard<ID>(val id: ID, val lap: Int)

/**
 * Which instance of each card a deck draws after [step], and whether the old top leaves.
 *
 * A card keeps its identity while it stays in the drawn window. Each time a card enters the window
 * it starts a new lap, taken from a counter that only goes up, so no identity is ever drawn twice:
 * a card coming back never meets an instance of itself that is still on its way out. A card going
 * round between the top and the back of a deck no deeper than the window starts a new lap too,
 * without leaving: stepping forward its old instance leaves the top the way any top card does
 * while the new one fades in at the back; stepping back the old instance fades from the back while
 * the new one comes in on top.
 *
 * @param from the drawn window before, top first.
 * @param to the drawn window now, top first.
 * @param step how a bound deck's drawn cards moved from [from] to [to], or null when they didn't.
 * @param laps the lap each card in [from] is on (0 when it has none).
 * @param nextLap the next unused lap.
 */
internal class CardStackPass<ID>(
    from: List<ID>,
    to: List<ID>,
    step: CardStackStep?,
    laps: Map<ID, Int>,
    nextLap: Int,
) {
    /** The drawn window's identities, top first. */
    val cards: List<CardStackCard<ID>>

    /** The old top, when it isn't among [cards] any more. */
    val leaving: CardStackCard<ID>?

    /** Toward the end on a step back (only a one-deep deck's old top leaves then), toward the start otherwise. */
    val leavesToward: AMCardStackSwipeDirection =
        if (step == CardStackStep.Back) AMCardStackSwipeDirection.StartToEnd else AMCardStackSwipeDirection.EndToStart

    /** The laps to carry into the next composition, for the drawn cards only. */
    val laps: Map<ID, Int>

    /** The lap the next card to enter (or go round) starts. */
    val nextLap: Int

    init {
        val wentRound: ID? = when (step) {
            CardStackStep.Forward -> from.firstOrNull()?.takeIf { it in to.drop(1) }
            CardStackStep.Back -> to.firstOrNull()?.takeIf { it in from.drop(1) }
            else -> null
        }
        val carried = LinkedHashMap<ID, Int>()
        var next = nextLap
        for (id in to) {
            if (id in from && id != wentRound) {
                carried[id] = laps[id] ?: 0
            } else {
                carried[id] = next
                next += 1
            }
        }
        val drawn = to.map { CardStackCard(it, carried.getValue(it)) }
        val oldTop = if (from.isEmpty()) null else CardStackCard(from[0], laps[from[0]] ?: 0)
        cards = drawn
        leaving = oldTop?.takeIf { it !in drawn }
        this.laps = carried
        this.nextLap = next
    }
}

/**
 * What one composition of a deck draws, read against the composition it last settled on.
 *
 * @property window the ids the deck reads, top first (a bound deck reads at least two).
 * @property step how a bound deck's drawn cards moved; null when they didn't, and always in event mode.
 * @property pass the identities of the cards drawn now.
 * @property leaving every card drawn before that isn't drawn now, top first: the old top flies
 *   off or fades, the rest fade where they were.
 * @property enters whether the new top comes in from the start side: a step back whose old top
 *   stays drawn (one deep, it leaves instead, and the new top fades in).
 * @param before the cards drawn in the composition it's read against, top first, which [isNew]
 *   and [depthBefore] look in.
 */
internal class CardStackRead<ID>(
    val window: List<ID>,
    val step: CardStackStep?,
    val pass: CardStackPass<ID>,
    val leaving: List<CardStackCard<ID>>,
    val enters: Boolean,
    private val before: List<CardStackCard<ID>>,
) {
    /** Whether [card] wasn't drawn before: it fades in, unless it's the top coming in or the deck's first. */
    fun isNew(card: CardStackCard<ID>): Boolean = card !in before

    /** Where [card] sat before, top first, or -1 when it wasn't drawn. */
    fun depthBefore(card: CardStackCard<ID>): Int = before.indexOf(card)
}

/**
 * A deck's bookkeeping between compositions: the window it last drew, the identities it drew it
 * with, and the next unused lap. A composition [read]s against it; once that composition is
 * applied the deck [commit]s the read, so a step holds for exactly the composition that drew it.
 */
internal class CardStackBook<ID : Any> {
    private var settledWindow: List<ID> = emptyList()
    private var drawn: List<CardStackCard<ID>> = emptyList()
    private var laps: Map<ID, Int> = emptyMap()
    private var nextLap = 0

    /** Whether the deck has drawn before: its very first cards appear at once, with no fade. */
    var hasSettled: Boolean = false
        private set

    /**
     * The card the deck's own swipe back stepped back from, so a two-card wrapping deck, whose new
     * window reads both ways, steps back. The next commit clears it.
     */
    var swipedBackFrom: ID? = null

    /**
     * Reads [window] (top first), drawing its first [visibleDepth] cards. A [bound] deck reads
     * steps; an event deck never steps, its cards simply staying, arriving or leaving.
     */
    fun read(window: List<ID>, visibleDepth: Int, bound: Boolean): CardStackRead<ID> {
        val newIds = window.take(visibleDepth.coerceAtLeast(1))
        val oldIds = drawn.map { it.id }
        val step = if (bound && settledWindow.isNotEmpty() && oldIds != newIds) {
            CardStackStep.between(settledWindow, window, preferBack = settledWindow.first() == swipedBackFrom)
        } else {
            null
        }
        val pass = CardStackPass(oldIds, newIds, step, laps, nextLap)
        return CardStackRead(
            window = window,
            step = step,
            pass = pass,
            leaving = drawn.filter { it !in pass.cards },
            enters = step == CardStackStep.Back && pass.leaving == null,
            before = drawn,
        )
    }

    /** Settles on [read], once the composition that drew it is applied. */
    fun commit(read: CardStackRead<ID>) {
        settledWindow = read.window
        drawn = read.pass.cards
        laps = read.pass.laps
        nextLap = read.pass.nextLap
        hasSettled = true
        swipedBackFrom = null
    }
}
