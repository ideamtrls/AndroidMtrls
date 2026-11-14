package com.ideamtrls.androidmtrls

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutCardSliceTest {

    private val row = Size(300f, 80f)
    private val reach = 100f

    @Test
    fun aSingleRowIsTheWholeCardWithItsShadowFreeOnEverySide() {
        val slice = cardSlice(row, first = true, last = true, reach = reach)
        assertEquals(Rect(0f, 0f, 300f, 80f), slice.card)
        assertEquals(Rect(-100f, -100f, 400f, 180f), slice.band)
    }

    @Test
    fun theFirstRowHasTheTopCornersAndRunsOnBelow() {
        val slice = cardSlice(row, first = true, last = false, reach = reach)
        assertEquals(Rect(0f, 0f, 300f, 180f), slice.card)
        assertEquals(Rect(-100f, -100f, 400f, 80f), slice.band)
    }

    @Test
    fun aMiddleRowShowsOnlyTheCardsSidesWithinItsOwnHeight() {
        val slice = cardSlice(row, first = false, last = false, reach = reach)
        assertEquals(Rect(0f, -100f, 300f, 180f), slice.card)
        assertEquals(Rect(-100f, 0f, 400f, 80f), slice.band)
    }

    @Test
    fun theLastRowHasTheBottomCornersAndRunsOnAbove() {
        val slice = cardSlice(row, first = false, last = true, reach = reach)
        assertEquals(Rect(0f, -100f, 300f, 80f), slice.card)
        assertEquals(Rect(-100f, 0f, 400f, 180f), slice.band)
    }

    @Test
    fun neighborsBandsMeetWithNoGapOrOverlap() {
        // Rows of 80, 100 and 60 stacked from y = 0; each band in page coordinates.
        val heights = listOf(80f, 100f, 60f)
        var top = 0f
        val bands = heights.mapIndexed { index, height ->
            val band = cardSlice(Size(300f, height), index == 0, index == heights.lastIndex, reach).band
            band.translate(0f, top).also { top += height }
        }
        assertEquals(bands[0].bottom, bands[1].top)
        assertEquals(bands[1].bottom, bands[2].top)
    }

    @Test
    fun theReachClearsEveryElevationsShadowAndACardsCorners() {
        for (level in listOf(Style.Shadow.Low, Style.Shadow.Medium)) {
            val shadow = maxOf(level.near.blur * 2 + level.near.offsetY, level.far.blur * 2 + level.far.offsetY)
            assertTrue(cardSliceReach(level) >= shadow + 16.dp)
        }
        assertEquals(64.dp + 48.dp, cardSliceReach(Style.Shadow.Low))
    }
}
