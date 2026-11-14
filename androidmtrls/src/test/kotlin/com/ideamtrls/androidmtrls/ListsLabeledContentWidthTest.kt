package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Test

class ListsLabeledContentWidthTest {

    @Test
    fun bothKeepTheirLineWhenTheyFit() {
        // "Plan" beside "Pro".
        assertEquals(40, labeledContentLabelWidth(labelNatural = 40, labelLongestWord = 40, valueNatural = 30, valueLongestWord = 30, room = 300))
    }

    @Test
    fun aShortLabelKeepsItsLineBesideALongValue() {
        // "Address" beside "214 Van Brunt St, Brooklyn, NY 11231": the value wraps, the label doesn't.
        assertEquals(70, labeledContentLabelWidth(labelNatural = 70, labelLongestWord = 70, valueNatural = 400, valueLongestWord = 90, room = 300))
    }

    @Test
    fun aShortValueKeepsItsLineBesideALongLabel() {
        // A long title beside "2 hours ago": the title wraps, the value stays whole instead of a word a line.
        assertEquals(200, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 100, valueNatural = 100, valueLongestWord = 50, room = 300))
    }

    @Test
    fun aValueOfExactlyHalfTheRoomStaysWhole() {
        assertEquals(150, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 80, valueNatural = 150, valueLongestWord = 60, room = 300))
    }

    @Test
    fun twoLongTextsWrapInHalfTheRowEach() {
        assertEquals(150, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 80, valueNatural = 350, valueLongestWord = 70, room = 300))
    }

    @Test
    fun theLabelLeavesTheValuesLongestWordWhole() {
        // A short label beside a value whose one word is wider than the rest of the row.
        assertEquals(60, labeledContentLabelWidth(labelNatural = 100, labelLongestWord = 50, valueNatural = 240, valueLongestWord = 240, room = 300))
    }

    @Test
    fun theLabelNeverGoesBelowItsLongestWord() {
        assertEquals(150, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 150, valueNatural = 400, valueLongestWord = 250, room = 300))
    }

    @Test
    fun theLabelNeverOutgrowsTheRoom() {
        assertEquals(120, labeledContentLabelWidth(labelNatural = 400, labelLongestWord = 200, valueNatural = 50, valueLongestWord = 50, room = 120))
        assertEquals(0, labeledContentLabelWidth(labelNatural = 40, labelLongestWord = 40, valueNatural = 10, valueLongestWord = 10, room = 0))
    }
}
