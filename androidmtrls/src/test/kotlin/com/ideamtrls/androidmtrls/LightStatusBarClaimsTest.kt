package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LightStatusBarClaimsTest {

    // The window's light-status-bar appearance bit: set means dark icons (a light appearance).
    private val darkIcons = 8
    private val lightIcons = 0

    @Test
    fun theOnlyClaimPutsBackWhatItFound() {
        val claims = LightStatusBarClaims()
        claims.claim(darkIcons)
        assertEquals(darkIcons, claims.release())
        assertEquals(0, claims.held)
    }

    @Test
    fun overlappingClaimsPutBackWhatTheFirstFoundWhenTheLastEnds() {
        val claims = LightStatusBarClaims()
        claims.claim(darkIcons)                  // a view over art
        claims.claim(lightIcons)                 // an art panel pushed over it finds the icons light
        assertNull(claims.release())             // the covered view leaves first
        assertEquals(darkIcons, claims.release())
    }

    @Test
    fun claimsEndingOutOfOrderNeverUndoTheIconsEarly() {
        val claims = LightStatusBarClaims()
        claims.claim(darkIcons)
        claims.claim(lightIcons)
        claims.claim(lightIcons)
        assertNull(claims.release())
        assertNull(claims.release())
        assertEquals(1, claims.held)
        assertEquals(darkIcons, claims.release())
    }

    @Test
    fun aClaimAfterTheLastEndedFindsTheWindowAfresh() {
        val claims = LightStatusBarClaims()
        claims.claim(darkIcons)
        assertEquals(darkIcons, claims.release())
        claims.claim(lightIcons)                 // the appearance turned dark in between
        assertEquals(lightIcons, claims.release())
    }

    @Test(expected = IllegalStateException::class)
    fun endingAClaimNobodyHoldsFails() {
        LightStatusBarClaims().release()
    }
}
