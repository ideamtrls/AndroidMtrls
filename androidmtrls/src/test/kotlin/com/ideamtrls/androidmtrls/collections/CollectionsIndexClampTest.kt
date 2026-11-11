package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Test

/** The collections' shared index clamp. */
class CollectionsIndexClampTest {

    @Test
    fun anIndexInsideTheCollectionStays() {
        assertEquals(0, clampIndex(0, count = 4))
        assertEquals(2, clampIndex(2, count = 4))
        assertEquals(3, clampIndex(3, count = 4))
    }

    @Test
    fun anIndexOutsideTheCollectionPinsToItsEnds() {
        assertEquals(0, clampIndex(-1, count = 4))
        assertEquals(0, clampIndex(Int.MIN_VALUE, count = 4))
        assertEquals(3, clampIndex(4, count = 4))
        assertEquals(3, clampIndex(Int.MAX_VALUE, count = 4))
        assertEquals(0, clampIndex(7, count = 1))
    }

    @Test
    fun anEmptyCollectionCollapsesToZero() {
        assertEquals(0, clampIndex(0, count = 0))
        assertEquals(0, clampIndex(5, count = 0))
        assertEquals(0, clampIndex(-5, count = 0))
        assertEquals(0, clampIndex(2, count = -1))
    }
}
