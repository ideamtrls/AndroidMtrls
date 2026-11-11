package com.ideamtrls.androidmtrls

import android.view.View
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

/**
 * Turns the status bar's icons light while [enabled], for whatever lies on dark art under the
 * status bar: a view whose header rests on its art rows, a full-screen player over its cover. Put
 * it in the composable that knows the art is there, and it holds the icons light for as long as
 * it's composed with [enabled] true.
 *
 * ```kotlin
 * @Composable
 * fun Player(track: Track) {
 *     AMLightStatusBarIcons()                                    // art from top to bottom
 *     …
 * }
 *
 * AMLightStatusBarIcons(enabled = headerOverArt)                 // until the header collapses
 * ```
 *
 * - **Claims count together.** Every caller in a window holds one claim on its icons, and
 *   [AMArtPanelChrome]'s bar at rest holds one too. The icons turn light with the first claim,
 *   stay light while any is held, and go back to what the window had before the first only when
 *   the last one ends, so a view leaving never undoes the icons of the one that replaced it.
 *   Navigation 3 brings the incoming entry in before the covered one leaves, and predictive back
 *   brings the one below in and may take it out again, so claims overlap and end out of order;
 *   counting them keeps the screen on top in charge.
 * - **The app's appearance comes back** once nothing claims: dark icons in light mode, light ones
 *   in dark mode, as the app set them. A claim follows the theme's appearance, so an app re-tinting
 *   its bars when the appearance changes doesn't win over the art.
 * - In dark mode the icons are already light, so a claim changes nothing you can see.
 *
 * Edge to edge, the status bar lies over the content, so the content draws the art behind it
 * itself. Main thread only, like the window it changes.
 *
 * @param enabled whether this claim is held: true while the art lies under the status bar, false
 *   once something solid covers it (a collapsed header, a scrolled bar's fill).
 */
@Composable
public fun AMLightStatusBarIcons(enabled: Boolean = true) {
    val view = LocalView.current
    val dark = AMTheme.isDark
    DisposableEffect(view, enabled, dark) {
        val controller = view.windowInsetsController
        if (!enabled || controller == null) return@DisposableEffect onDispose {}
        val claims = lightStatusBarClaims(view)
        claims.claim(controller.systemBarsAppearance and LightStatusBars)
        controller.setSystemBarsAppearance(0, LightStatusBars)
        onDispose {
            claims.release()?.let { found -> controller.setSystemBarsAppearance(found, LightStatusBars) }
        }
    }
}

/**
 * The claims holding one window's status bar icons light, and what the window's icons were before
 * the first of them. Claims start and end in any order; the icons go back only when the last ends.
 */
internal class LightStatusBarClaims {
    /** How many claims are held. */
    var held: Int = 0
        private set

    /** The window's light-status-bar appearance bit as the first claim found it. */
    private var before = 0

    /**
     * Takes a claim. [found] is the window's light-status-bar appearance bit right now, kept only
     * when this is the first claim, since later ones find the icons the first claim set.
     */
    fun claim(found: Int) {
        if (held++ == 0) before = found
    }

    /**
     * Ends a claim. Returns the appearance bit to put back when it was the last one held, or null
     * while other claims still hold the icons light.
     */
    fun release(): Int? {
        check(held > 0) { "No light status bar claim is held." }
        return if (--held == 0) before else null
    }
}

/** Each window's claims, keyed on the window's root view, so any view in it finds them. */
private val windowClaims = WeakHashMap<View, LightStatusBarClaims>()

/** The claims on the status bar icons of [view]'s window. */
private fun lightStatusBarClaims(view: View): LightStatusBarClaims =
    windowClaims.getOrPut(view.rootView) { LightStatusBarClaims() }

private const val LightStatusBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
