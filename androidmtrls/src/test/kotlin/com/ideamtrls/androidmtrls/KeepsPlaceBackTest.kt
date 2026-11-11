package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeepsPlaceBackTest {

    private enum class View { Feed, Alerts }

    @Test
    fun backOnAnotherRootWithNoPanelSwapsToTheLanding() {
        assertEquals(View.Feed, keepsPlaceBackTarget(View.Alerts, View.Feed, depth = 1))
    }

    @Test
    fun aRootWithNoBackStackCountsAsShowingNoPanel() {
        assertEquals(View.Feed, keepsPlaceBackTarget(View.Alerts, View.Feed, depth = 0))
    }

    @Test
    fun aPanelOnAnotherRootPopsBeforeTheSwap() {
        assertNull(keepsPlaceBackTarget(View.Alerts, View.Feed, depth = 2))
    }

    @Test
    fun backOnTheLandingRootFallsThroughToLeaveTheApp() {
        assertNull(keepsPlaceBackTarget(View.Feed, View.Feed, depth = 1))
        assertNull(keepsPlaceBackTarget(View.Feed, View.Feed, depth = 0))
    }

    @Test
    fun aPanelOnTheLandingRootIsNeverThisContainersBack() {
        assertNull(keepsPlaceBackTarget(View.Feed, View.Feed, depth = 3))
    }
}
