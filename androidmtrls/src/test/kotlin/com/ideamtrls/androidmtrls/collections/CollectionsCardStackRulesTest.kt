package com.ideamtrls.androidmtrls

import com.ideamtrls.androidmtrls.AMCardStackSwipeDirection.EndToStart
import com.ideamtrls.androidmtrls.AMCardStackSwipeDirection.StartToEnd
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The card stack's rules: the release rule, the window and its steps, the identities a deck draws
 * its cards with, and the bookkeeping that carries them between compositions.
 */
class CollectionsCardStackRulesTest {

    // Release rule. Cards are 370 wide: a release commits past 92.5 of travel or 222 predicted,
    // measured toward the end side.

    private fun side(dx: Float, predicted: Float) = CardStackCommit.side(dx, predictedDx = predicted, width = 370f)

    @Test
    fun aPlainSwipeGoesItsWay() {
        assertEquals(EndToStart, side(dx = -120f, predicted = -300f))
        assertEquals(EndToStart, side(dx = -100f, predicted = -100f))
        assertEquals(StartToEnd, side(dx = 120f, predicted = 300f))
        assertEquals(StartToEnd, side(dx = 100f, predicted = 100f))
    }

    @Test
    fun aShortDragSpringsHome() {
        assertNull(side(dx = 80f, predicted = 120f))
        assertNull(side(dx = -80f, predicted = -120f))
        assertNull(side(dx = 0f, predicted = 0f))
    }

    /** Dragged to +100, then let go easing back: the side is the start, where nothing was cleared. */
    @Test
    fun aReversalShortOfItsNewSideSpringsHome() {
        assertNull(side(dx = 100f, predicted = -50f))
    }

    /** Dragged to +100, then thrown back hard: past the start side's 222. */
    @Test
    fun aReversalPastItsNewSideCommits() {
        assertEquals(EndToStart, side(dx = 100f, predicted = -600f))
    }

    /** A quick flick with next to no travel goes the way its velocity says, once it coasts far enough. */
    @Test
    fun aFlickGoesWithItsVelocity() {
        assertEquals(EndToStart, side(dx = 0f, predicted = -300f))
        assertEquals(StartToEnd, side(dx = 0f, predicted = 300f))
        assertNull(side(dx = 0f, predicted = 200f))
    }

    @Test
    fun aDeckWithNoWidthNeverCommits() {
        assertNull(CardStackCommit.side(dx = 10f, predictedDx = 10f, width = 0f))
    }

    // Window and steps.

    @Test
    fun theWindowStartsAtTheIndex() {
        assertEquals(listOf(1, 2, 3), CardStackWindow.indices(count = 4, currentIndex = 1, visibleDepth = 3, wraps = false))
        assertEquals(listOf(3), CardStackWindow.indices(count = 4, currentIndex = 3, visibleDepth = 3, wraps = false))
        assertEquals(listOf(3), CardStackWindow.indices(count = 4, currentIndex = 9, visibleDepth = 3, wraps = false))
        assertEquals(listOf(0), CardStackWindow.indices(count = 4, currentIndex = -2, visibleDepth = 0, wraps = false))
        assertEquals(emptyList<Int>(), CardStackWindow.indices(count = 0, currentIndex = 0, visibleDepth = 3, wraps = true))
    }

    @Test
    fun aWrappingWindowNeverRepeatsACard() {
        assertEquals(listOf(3, 0, 1), CardStackWindow.indices(count = 4, currentIndex = 3, visibleDepth = 3, wraps = true))
        assertEquals(listOf(1, 0), CardStackWindow.indices(count = 2, currentIndex = 1, visibleDepth = 3, wraps = true))
        assertEquals(listOf(0), CardStackWindow.indices(count = 1, currentIndex = 0, visibleDepth = 3, wraps = true))
    }

    @Test
    fun stepsStopAtTheEndsUnlessWrapping() {
        assertEquals(2, CardStackWindow.next(1, count = 4, wraps = false))
        assertNull(CardStackWindow.next(3, count = 4, wraps = false))
        assertEquals(0, CardStackWindow.next(3, count = 4, wraps = true))
        assertNull(CardStackWindow.next(0, count = 1, wraps = true))
        assertNull(CardStackWindow.previous(0, count = 4, wraps = false))
        assertEquals(3, CardStackWindow.previous(0, count = 4, wraps = true))
        assertNull(CardStackWindow.previous(0, count = 1, wraps = true))
        // An index out of range steps from where it draws, clamped.
        assertEquals(2, CardStackWindow.previous(9, count = 4, wraps = false))
    }

    @Test
    fun aStepReadsTheWindows() {
        assertEquals(CardStackStep.Forward, CardStackStep.between(listOf("a", "b", "c"), listOf("b", "c", "d"), preferBack = false))
        assertEquals(CardStackStep.Back, CardStackStep.between(listOf("b", "c", "d"), listOf("a", "b", "c"), preferBack = false))
        assertEquals(CardStackStep.Forward, CardStackStep.between(listOf("c", "d"), listOf("d"), preferBack = false))
        assertEquals(CardStackStep.Back, CardStackStep.between(listOf("d"), listOf("c", "d"), preferBack = false))
        assertEquals(CardStackStep.Other, CardStackStep.between(listOf("a", "b", "c"), listOf("a", "c", "b"), preferBack = false))
        assertEquals(CardStackStep.Back, CardStackStep.between(listOf("a", "b"), listOf("b", "a"), preferBack = true))
        assertEquals(CardStackStep.Forward, CardStackStep.between(listOf("a", "b"), listOf("b", "a"), preferBack = false))
    }

    // Identities.

    private fun card(id: String, lap: Int) = CardStackCard(id, lap)

    private fun pass(from: List<String>, to: List<String>, step: CardStackStep?, laps: Map<String, Int>, nextLap: Int) =
        CardStackPass(from, to, step, laps, nextLap)

    @Test
    fun aDeepDeckKeepsTheCardsThatStay() {
        val forward = pass(listOf("a", "b", "c"), listOf("b", "c", "d"), CardStackStep.Forward, mapOf("a" to 0, "b" to 1, "c" to 2), 3)
        assertEquals(listOf(card("b", 1), card("c", 2), card("d", 3)), forward.cards)
        assertEquals(card("a", 0), forward.leaving)
        assertEquals(EndToStart, forward.leavesToward)
        assertEquals(mapOf("b" to 1, "c" to 2, "d" to 3), forward.laps)
        assertEquals(4, forward.nextLap)

        val back = pass(listOf("b", "c", "d"), listOf("a", "b", "c"), CardStackStep.Back, forward.laps, forward.nextLap)
        assertEquals(listOf(card("a", 4), card("b", 1), card("c", 2)), back.cards)
        assertNull(back.leaving)
    }

    @Test
    fun theFirstWindowTakesALapPerCard() {
        val first = pass(emptyList(), listOf("a", "b", "c"), null, emptyMap(), 0)
        assertEquals(listOf(card("a", 0), card("b", 1), card("c", 2)), first.cards)
        assertNull(first.leaving)
        assertEquals(3, first.nextLap)
    }

    @Test
    fun aCardComingBackNeverReusesItsOldIdentity() {
        val next = pass(listOf("b", "c", "d"), listOf("c", "d", "e"), CardStackStep.Forward, mapOf("b" to 1, "c" to 2, "d" to 3), 4)
        assertEquals(card("b", 1), next.leaving)
        val previous = pass(listOf("c", "d", "e"), listOf("b", "c", "d"), CardStackStep.Back, next.laps, next.nextLap)
        assertEquals(listOf(card("b", 5), card("c", 2), card("d", 3)), previous.cards)

        // One deep: the old top leaves toward the end, and comes back fresh.
        val oneBack = pass(listOf("b"), listOf("a"), CardStackStep.Back, mapOf("b" to 7), 8)
        assertEquals(card("b", 7), oneBack.leaving)
        assertEquals(StartToEnd, oneBack.leavesToward)
        val oneNext = pass(listOf("a"), listOf("b"), CardStackStep.Forward, oneBack.laps, oneBack.nextLap)
        assertEquals(listOf(card("b", 9)), oneNext.cards)
        assertEquals(card("a", 8), oneNext.leaving)
        assertEquals(EndToStart, oneNext.leavesToward)
    }

    @Test
    fun aWrappingCardGoingRoundTakesAFreshIdentity() {
        val forward = pass(listOf("a", "b", "c"), listOf("b", "c", "a"), CardStackStep.Forward, mapOf("a" to 0, "b" to 1, "c" to 2), 3)
        assertEquals(listOf(card("b", 1), card("c", 2), card("a", 3)), forward.cards)
        assertEquals(card("a", 0), forward.leaving)

        val back = pass(listOf("b", "c", "a"), listOf("a", "b", "c"), CardStackStep.Back, forward.laps, forward.nextLap)
        assertEquals(listOf(card("a", 4), card("b", 1), card("c", 2)), back.cards)
        assertNull(back.leaving)
        assertEquals(mapOf("a" to 4, "b" to 1, "c" to 2), back.laps)
    }

    @Test
    fun aTwoCardWrappingDeckGoesRoundBothWays() {
        val forward = pass(listOf("a", "b"), listOf("b", "a"), CardStackStep.Forward, mapOf("a" to 0, "b" to 1), 2)
        assertEquals(listOf(card("b", 1), card("a", 2)), forward.cards)
        assertEquals(card("a", 0), forward.leaving)
        val back = pass(listOf("a", "b"), listOf("b", "a"), CardStackStep.Back, mapOf("a" to 0, "b" to 1), 2)
        assertEquals(listOf(card("b", 2), card("a", 0)), back.cards)
        assertNull(back.leaving)
    }

    @Test
    fun aJumpSendsTheTopOffAndAStillChangesNothing() {
        val jump = pass(listOf("a", "b", "c"), listOf("d", "e", "f"), CardStackStep.Other, mapOf("a" to 5, "b" to 6, "c" to 7), 8)
        assertEquals(listOf(card("d", 8), card("e", 9), card("f", 10)), jump.cards)
        assertEquals(card("a", 5), jump.leaving)
        assertEquals(EndToStart, jump.leavesToward)

        val still = pass(listOf("b", "c", "a"), listOf("b", "c", "a"), null, mapOf("a" to 3, "b" to 1, "c" to 2), 4)
        assertEquals(listOf(card("b", 1), card("c", 2), card("a", 3)), still.cards)
        assertNull(still.leaving)
        assertEquals(4, still.nextLap)
    }

    // Bookkeeping.

    @Test
    fun aBoundDeckReadsItsStepsAgainstWhatItLastDrew() {
        val book = CardStackBook<String>()
        val first = book.read(listOf("b", "c", "d", "e"), visibleDepth = 3, bound = true)
        assertNull(first.step)
        assertFalse(book.hasSettled)
        book.commit(first)
        assertTrue(book.hasSettled)

        // Unchanged, it reads no step and nothing leaves.
        val still = book.read(listOf("b", "c", "d", "e"), visibleDepth = 3, bound = true)
        assertNull(still.step)
        assertEquals(first.pass.cards, still.pass.cards)
        assertTrue(still.leaving.isEmpty())

        // Back: the new top comes in from the start; the card pushed past the window fades.
        val back = book.read(listOf("a", "b", "c", "d"), visibleDepth = 3, bound = true)
        assertEquals(CardStackStep.Back, back.step)
        assertTrue(back.enters)
        assertEquals(listOf(card("d", 2)), back.leaving)
        assertEquals(2, back.depthBefore(card("d", 2)))
        assertTrue(back.isNew(back.pass.cards[0]))
        assertFalse(back.isNew(card("b", 0)))
        book.commit(back)

        // Forward: the old top leaves toward the start.
        val forward = book.read(listOf("b", "c", "d", "e"), visibleDepth = 3, bound = true)
        assertEquals(CardStackStep.Forward, forward.step)
        assertFalse(forward.enters)
        assertEquals(back.pass.cards[0], forward.pass.leaving)
        assertEquals(EndToStart, forward.pass.leavesToward)
        assertEquals(listOf(back.pass.cards[0]), forward.leaving)
    }

    @Test
    fun aOneDeepDeckStepsBackByLeavingTowardTheEnd() {
        val book = CardStackBook<String>()
        book.commit(book.read(listOf("b", "c"), visibleDepth = 1, bound = true))
        val back = book.read(listOf("a", "b"), visibleDepth = 1, bound = true)
        assertEquals(CardStackStep.Back, back.step)
        assertEquals(card("b", 0), back.pass.leaving)
        assertEquals(StartToEnd, back.pass.leavesToward)
        // The old top isn't drawn any more, so the new one fades in rather than sliding over it.
        assertFalse(back.enters)
    }

    @Test
    fun aTwoCardWrappingDeckStepsBackAfterItsOwnSwipeBack() {
        val book = CardStackBook<String>()
        book.commit(book.read(listOf("a", "b"), visibleDepth = 3, bound = true))
        book.swipedBackFrom = "a"
        val back = book.read(listOf("b", "a"), visibleDepth = 3, bound = true)
        assertEquals(CardStackStep.Back, back.step)
        book.commit(back)
        assertNull(book.swipedBackFrom)
        val forward = book.read(listOf("a", "b"), visibleDepth = 3, bound = true)
        assertEquals(CardStackStep.Forward, forward.step)
    }

    @Test
    fun anEventDeckNeverStepsAndLetsRemovedCardsGo() {
        val book = CardStackBook<String>()
        val first = book.read(listOf("a", "b", "c"), visibleDepth = 3, bound = false)
        assertEquals(listOf(card("a", 0), card("b", 1), card("c", 2)), first.pass.cards)
        book.commit(first)

        val swiped = book.read(listOf("b", "c", "d"), visibleDepth = 3, bound = false)
        assertNull(swiped.step)
        assertFalse(swiped.enters)
        assertEquals(listOf(card("b", 1), card("c", 2), card("d", 3)), swiped.pass.cards)
        assertEquals(card("a", 0), swiped.pass.leaving)
        assertEquals(listOf(card("a", 0)), swiped.leaving)
        assertTrue(swiped.isNew(card("d", 3)))
        assertFalse(swiped.isNew(card("b", 1)))
        book.commit(swiped)

        // The same item coming back takes a new identity.
        val returned = book.read(listOf("a", "b", "c"), visibleDepth = 3, bound = false)
        assertEquals(card("a", 4), returned.pass.cards[0])
    }

    /**
     * Walks bound decks of eight shapes (two to five cards, one to three deep, wrapping or not)
     * through a long, fixed run of steps (next, previous, jumps) with the deck's own bookkeeping,
     * and checks the identity rule on every composition: no identity is drawn twice, so a card
     * that comes back never meets an instance of itself still on its way out, and a card that
     * stays drawn keeps its identity unless it goes round.
     */
    @Test
    fun noIdentityIsEverDrawnTwice() {
        val shapes = listOf(
            Triple(3, 3, true), Triple(3, 3, false), Triple(2, 3, true), Triple(4, 3, true),
            Triple(5, 3, false), Triple(5, 1, false), Triple(3, 1, true), Triple(5, 2, true),
        )
        for ((count, depth, wraps) in shapes) {
            val book = CardStackBook<String>()
            val retired = mutableSetOf<CardStackCard<String>>()
            var drawn = emptyList<CardStackCard<String>>()
            var index = 0
            var seed = -0x61c8864680b583ebL
            fun draw(swipedBack: Boolean): Boolean {
                if (swipedBack) book.swipedBackFrom = drawn.firstOrNull()?.id
                val window = CardStackWindow.indices(count, index, maxOf(2, depth), wraps).map { "c$it" }
                val read = book.read(window, depth, bound = true)
                val cards = read.pass.cards
                var holds = cards.toSet().size == cards.size && cards.none { it in retired }
                val newIds = window.take(depth)
                for (card in drawn) {
                    if (card.id in newIds && card !in cards) {
                        // Only a card going round may change identity while it stays.
                        val wentRound = (read.step == CardStackStep.Forward && card == drawn.first()) ||
                            (read.step == CardStackStep.Back && card.id == newIds.first())
                        holds = holds && wentRound
                    }
                }
                holds = holds && read.leaving.all { it in drawn && it !in cards }
                retired += read.leaving
                drawn = cards
                book.commit(read)
                return holds
            }
            assertTrue(draw(swipedBack = false))
            repeat(300) {
                seed = seed * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
                var swipedBack = false
                when ((seed ushr 61).toInt()) {
                    0, 1, 2 -> index = CardStackWindow.next(index, count, wraps) ?: index
                    3, 4, 5 -> {
                        index = CardStackWindow.previous(index, count, wraps) ?: index
                        swipedBack = true
                    }
                    else -> index = ((seed ushr 32) % count).toInt()
                }
                assertTrue("deck of $count, $depth deep, wraps $wraps, at $index", draw(swipedBack))
            }
        }
    }
}
