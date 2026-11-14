package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a button pair splits its row and when it stacks. */
class ButtonPairTest {

    // A 411 dp phone less two 16 dp gutters, at density 1, with the 12 dp gap.
    private val width = 379
    private val gap = 12

    @Test
    fun eachButtonTakesHalfTheRowLessTheGap() {
        assertEquals(184, buttonPairHalf(width + 1, gap))
        // An odd remainder rounds down, so the pair never outgrows its row.
        assertEquals(183, buttonPairHalf(width, gap))
    }

    @Test
    fun twoShortLabelsSplit() {
        assertTrue(buttonPairSplits(firstWidth = 120, secondWidth = 140, width = width, gap = gap))
    }

    @Test
    fun aLabelThatFitsExactlySplits() {
        assertTrue(buttonPairSplits(firstWidth = 183, secondWidth = 183, width = width, gap = gap))
    }

    @Test
    fun eitherLabelTooWideStacks() {
        assertFalse(buttonPairSplits(firstWidth = 120, secondWidth = 184, width = width, gap = gap))
        assertFalse(buttonPairSplits(firstWidth = 184, secondWidth = 120, width = width, gap = gap))
    }
}
