package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrostedSampleRegionTest {

    @Test
    fun surfaceWellInsideTheArtSamplesItsRectPlusTheMarginOnEverySide() {
        val region = frostedSampleRegion(x = 100f, y = 200f, width = 300f, height = 80f, margin = 50f, artWidth = 1000f, artHeight = 2000f)
        assertEquals(SampleRegion(left = 50, top = 150, width = 400, height = 180), region)
    }

    @Test
    fun theMarginStopsAtTheArtsOwnEdges() {
        val region = frostedSampleRegion(x = 16f, y = 10f, width = 300f, height = 80f, margin = 50f, artWidth = 332f, artHeight = 2000f)
        assertEquals(SampleRegion(left = 0, top = 0, width = 332, height = 140), region)
    }

    @Test
    fun aSurfacePartlyScrolledPastTheArtSamplesOnlyTheOverlap() {
        val region = frostedSampleRegion(x = 16f, y = -60f, width = 300f, height = 80f, margin = 10f, artWidth = 400f, artHeight = 500f)
        assertEquals(SampleRegion(left = 6, top = 0, width = 320, height = 30), region)
    }

    @Test
    fun fractionalPositionsWidenToWholePixels() {
        val region = frostedSampleRegion(x = 10.4f, y = 20.6f, width = 100.2f, height = 50.1f, margin = 0f, artWidth = 500f, artHeight = 500f)
        // left 10.4 -> 10, right 110.6 -> 111 wide from 10; top 20.6 -> 20, bottom 70.7 -> 71.
        assertEquals(SampleRegion(left = 10, top = 20, width = 101, height = 51), region)
    }

    @Test
    fun aSurfaceOffTheArtSamplesNothing() {
        assertNull(frostedSampleRegion(x = 0f, y = 600f, width = 300f, height = 80f, margin = 10f, artWidth = 400f, artHeight = 500f))
        assertNull(frostedSampleRegion(x = -400f, y = 0f, width = 300f, height = 80f, margin = 50f, artWidth = 400f, artHeight = 500f))
    }

    @Test
    fun aSurfaceTouchingTheArtOnlyThroughItsMarginStillSamplesTheMargin() {
        val region = frostedSampleRegion(x = 0f, y = 520f, width = 300f, height = 80f, margin = 40f, artWidth = 400f, artHeight = 500f)
        assertEquals(SampleRegion(left = 0, top = 480, width = 340, height = 20), region)
    }
}
