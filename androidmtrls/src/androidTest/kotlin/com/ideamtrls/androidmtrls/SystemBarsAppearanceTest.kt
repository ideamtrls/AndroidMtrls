package com.ideamtrls.androidmtrls

import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The window's bar icons under a pinned appearance: the outermost [AMTheme] sets them for its
 * flag and again when it changes, a nested theme leaves them alone, and art of the other tone
 * under the status bar turns its icons over: a claim, or an art backdrop reaching the top.
 */
class SystemBarsAppearanceTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    /** The window's light-icon bits: status and navigation. */
    private fun lightBars(): Int {
        var bits = 0
        rule.runOnUiThread {
            bits = rule.activity.window.insetsController!!.systemBarsAppearance and Both
        }
        return bits
    }

    @Test
    fun theOutermostThemeTintsTheBarsForItsAppearance() {
        var dark by mutableStateOf(true)
        rule.setContent { AMTheme(darkTheme = dark) { Text("Screen") } }
        rule.waitForIdle()
        assertEquals("dark: light icons on both bars", 0, lightBars())

        dark = false
        rule.waitForIdle()
        assertEquals("light: dark icons on both bars", Both, lightBars())

        dark = true
        rule.waitForIdle()
        assertEquals(0, lightBars())
    }

    @Test
    fun aNestedThemeLeavesTheBarsToTheOuterOne() {
        rule.setContent {
            AMTheme(darkTheme = false) {
                AMTheme(darkTheme = true) { Text("A dark card in a light app") }
            }
        }
        rule.waitForIdle()
        assertEquals(Both, lightBars())
    }

    @Test
    fun aClaimOverArtHoldsTheStatusBarAcrossAChange() {
        var dark by mutableStateOf(false)
        rule.setContent {
            AMTheme(darkTheme = dark) {
                // A photo shown as it is: dark, whatever the mode.
                AMStatusBarIcons(AMArtTone.Dark)
                Text("Photo")
            }
        }
        rule.waitForIdle()
        assertEquals("light mode, dark art: light icons", APPEARANCE_LIGHT_NAVIGATION_BARS, lightBars())
        dark = true
        rule.waitForIdle()
        assertEquals("dark mode, dark art: the mode's own light icons", 0, lightBars())
    }

    @Test
    fun lightArtInDarkModeTurnsTheIconsDark() {
        rule.setContent {
            AMTheme(darkTheme = true) {
                AMStatusBarIcons(AMArtTone.Light)
                Text("Snow")
            }
        }
        rule.waitForIdle()
        assertEquals(APPEARANCE_LIGHT_STATUS_BARS, lightBars())
    }

    @Test
    fun aBackdropReachingTheTopTurnsTheIconsForItsTone() {
        var tone by mutableStateOf<AMArtTone?>(AMArtTone.Dark)
        rule.setContent {
            AMTheme(darkTheme = false) {
                AMBackdrop(art = {}, Modifier.fillMaxSize(), tone = tone) { Text("Art") }
            }
        }
        rule.waitForIdle()
        assertEquals("dark art at the top in light mode", APPEARANCE_LIGHT_NAVIGATION_BARS, lightBars())
        tone = null
        rule.waitForIdle()
        assertEquals("the mode's tone: the mode's icons", Both, lightBars())
    }

    @Test
    fun aBackdropBelowTheTopLeavesTheIcons() {
        rule.setContent {
            AMTheme(darkTheme = false) {
                Column(Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(200.dp))
                    AMBackdrop(art = {}, Modifier.fillMaxWidth().height(200.dp), tone = AMArtTone.Dark) { Text("Card") }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(Both, lightBars())
    }

    private companion object {
        const val Both = APPEARANCE_LIGHT_STATUS_BARS or APPEARANCE_LIGHT_NAVIGATION_BARS
    }
}
