package com.ideamtrls.androidmtrls

import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The window's bar icons under a pinned appearance: the outermost [AMTheme] sets them for its
 * flag and again when it changes, a nested theme leaves them alone, and a light-icon claim over
 * art still holds the status bar across a change.
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
        var dark by mutableStateOf(true)
        rule.setContent {
            AMTheme(darkTheme = dark) {
                AMLightStatusBarIcons()
                Text("Player")
            }
        }
        rule.waitForIdle()
        dark = false
        rule.waitForIdle()
        assertEquals("the claim keeps the status bar's icons light", APPEARANCE_LIGHT_NAVIGATION_BARS, lightBars())
    }

    private companion object {
        const val Both = APPEARANCE_LIGHT_STATUS_BARS or APPEARANCE_LIGHT_NAVIGATION_BARS
    }
}
