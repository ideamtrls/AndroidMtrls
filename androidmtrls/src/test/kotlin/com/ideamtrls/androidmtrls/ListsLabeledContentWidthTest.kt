package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Test

class ListsLabeledContentWidthTest {

    @Test
    fun theLabelKeepsItsOwnWidthBesideALongValue() {
        // "Address" beside "214 Van Brunt St, Brooklyn, NY 11231": the value wraps, the label doesn't.
        assertEquals(70, labeledContentLabelWidth(labelNatural = 70, labelLongestWord = 70, valueLongestWord = 90, room = 300))
    }

    @Test
    fun aLongLabelGivesUpOnlyWhatTheValuesLongestWordNeeds() {
        assertEquals(220, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 100, valueLongestWord = 80, room = 300))
    }

    @Test
    fun theLabelNeverGoesBelowItsLongestWord() {
        assertEquals(150, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 150, valueLongestWord = 250, room = 300))
    }

    @Test
    fun theLabelNeverOutgrowsTheRoom() {
        assertEquals(120, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 200, valueLongestWord = 50, room = 120))
        assertEquals(0, labeledContentLabelWidth(labelNatural = 40, labelLongestWord = 40, valueLongestWord = 10, room = 0))
    }
}
