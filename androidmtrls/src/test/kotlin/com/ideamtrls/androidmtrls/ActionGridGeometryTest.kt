package com.ideamtrls.androidmtrls

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Where the remote's grid puts its rows, as content lining up with them reads it. */
class ActionGridGeometryTest {

    @Test
    fun theBottomRowSitsOnTheGridsBottomPadding() {
        assertEquals(32.dp, actionGridRowBottom(0, tileHeight = 76.dp, bottomPadding = 32.dp, rowGap = 12.dp))
    }

    @Test
    fun eachRowAboveIsATileAndAGapHigher() {
        // Just above one row: the bottom padding, a tile, the gap.
        assertEquals(120.dp, actionGridRowBottom(1, tileHeight = 76.dp, bottomPadding = 32.dp, rowGap = 12.dp))
        assertEquals(208.dp, actionGridRowBottom(2, tileHeight = 76.dp, bottomPadding = 32.dp, rowGap = 12.dp))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rowsCountFromZero() {
        actionGridRowBottom(-1, tileHeight = 76.dp, bottomPadding = 32.dp, rowGap = 12.dp)
    }
}
