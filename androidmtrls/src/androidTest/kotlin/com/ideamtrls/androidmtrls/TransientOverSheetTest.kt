package com.ideamtrls.androidmtrls

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration

/**
 * Toasts and banners while a sheet is up: they show over the sheet, the window the user sees,
 * whether posted from the screen under it or from inside it, and go back to the screen's hosts
 * when the sheet goes.
 */
class TransientOverSheetTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var sheetUp by mutableStateOf(true)
    private var screenToast by mutableStateOf<AMToastItem?>(null)
    private var screenBanner by mutableStateOf<AMBannerItem?>(null)
    private var sheetBanner by mutableStateOf<AMBannerItem?>(null)

    private fun showScene() {
        rule.setContent {
            AMTheme {
                AMScene {
                    Text("Screen")
                    AMSheet(sheetUp, onDismissRequest = { sheetUp = false }) {
                        Text("Sheet")
                        AMBanner(sheetBanner, onDismissRequest = { sheetBanner = null })
                    }
                    AMToast(screenToast, onDismissRequest = { screenToast = null })
                    AMBanner(screenBanner, onDismissRequest = { screenBanner = null })
                }
            }
        }
        rule.waitForIdle()
    }

    /** The window a text lies in, by its root's coordinates. */
    private fun windowOf(text: String): LayoutCoordinates {
        var coordinates = rule.onNodeWithText(text).fetchSemanticsNode().layoutInfo.coordinates
        while (coordinates.parentCoordinates != null) coordinates = coordinates.parentCoordinates!!
        return coordinates
    }

    @Test
    fun aScreensToastShowsInTheSheetThenBackOnTheScreen() {
        showScene()
        rule.mainClock.autoAdvance = false
        screenToast = AMToastItem("Saved", duration = Duration.INFINITE)
        rule.mainClock.advanceTimeBy(500)
        assertSame("over the sheet", windowOf("Sheet"), windowOf("Saved"))

        sheetUp = false
        rule.mainClock.advanceTimeBy(2_000)
        assertSame("back on the screen", windowOf("Screen"), windowOf("Saved"))
    }

    @Test
    fun aScreensBannerComesDownOverTheSheet() {
        showScene()
        rule.mainClock.autoAdvance = false
        screenBanner = AMBannerItem("Couldn't follow", duration = Duration.INFINITE)
        rule.mainClock.advanceTimeBy(500)
        rule.onNode(hasText("Couldn't follow") and hasAnyAncestor(isPopup())).assertExists()
        assertNotSame(windowOf("Screen"), windowOf("Couldn't follow"))
        assertNotSame(windowOf("Sheet"), windowOf("Couldn't follow"))
    }

    @Test
    fun aSheetsOwnBannerComesDownOverIt() {
        showScene()
        rule.mainClock.autoAdvance = false
        sheetBanner = AMBannerItem("Downloaded", duration = Duration.INFINITE)
        rule.mainClock.advanceTimeBy(500)
        rule.onNode(hasText("Downloaded") and hasAnyAncestor(isPopup())).assertExists()
    }

    @Test
    fun theNewestMessageShowsWhereverItWasPosted() {
        showScene()
        rule.mainClock.autoAdvance = false
        sheetBanner = AMBannerItem("Downloaded", duration = Duration.INFINITE)
        rule.mainClock.advanceTimeBy(500)
        screenBanner = AMBannerItem("Couldn't follow", duration = Duration.INFINITE)
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNode(hasText("Couldn't follow") and hasAnyAncestor(isPopup())).assertExists()
        assertEquals(0, rule.onAllNodes(hasText("Downloaded")).fetchSemanticsNodes().size)
    }
}
