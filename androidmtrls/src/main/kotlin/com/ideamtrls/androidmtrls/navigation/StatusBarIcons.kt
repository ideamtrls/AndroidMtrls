package com.ideamtrls.androidmtrls

import android.view.View
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import java.util.WeakHashMap

/**
 * Says what lies under the status bar here: art of [tone]. The status bar's icons follow the mode
 * (the outermost [AMTheme] sets them), so art in the mode's tone changes nothing; art of the other
 * tone turns them over while this is composed with [enabled] true: light icons over dark art in
 * light mode, dark icons over light art in dark mode.
 *
 * ```kotlin
 * @Composable
 * fun PhotoViewer(photo: Photo) {
 *     AMStatusBarIcons(AMArtTone.Dark)                             // a raw photo, top to bottom
 *     …
 * }
 * ```
 *
 * The library's own art says so by itself: an [AMBackdrop] or [AMArtRowsBackdrop] given a tone
 * other than the mode's, reaching the top of the window, holds one of these while no bar's frosted
 * fill covers the status bar. The library's art is washed toward the mode, so by default nothing
 * turns the icons over. Call it for what the library doesn't draw: a photo shown as it is, video.
 *
 * - **Claims count together.** Every caller in a window holds one claim on its icons, and an art
 *   surface holds one too. The icons turn over with the first claim, stay over while any is held,
 *   and go back to what the window had before the first only when the last one ends, so a view
 *   leaving never undoes the icons of the one that replaced it. Navigation 3 brings the incoming
 *   entry in before the covered one leaves, and predictive back brings the one below in and may
 *   take it out again, so claims overlap and end out of order; counting them keeps the screen on
 *   top in charge. In one mode every claim wants the same icons.
 * - **The mode's icons come back** once nothing claims. A claim follows the theme's appearance, so
 *   the theme re-tinting the bars when the appearance changes doesn't win over the art.
 *
 * Edge to edge, the status bar lies over the content, so the content draws the art behind it
 * itself. Main thread only, like the window it changes.
 *
 * @param tone how light what lies under the status bar is.
 * @param enabled whether this claim is held: true while that lies under the status bar, false once
 *   something solid covers it (a scrolled bar's fill).
 */
@Composable
public fun AMStatusBarIcons(tone: AMArtTone, enabled: Boolean = true) {
    val view = LocalView.current
    val dark = AMTheme.isDark
    // Art in the mode's tone wants the mode's icons, which the theme already set.
    val contrary = tone != artToneOf(dark)
    DisposableEffect(view, enabled && contrary, dark) {
        val controller = view.windowInsetsController
        if (!enabled || !contrary || controller == null) return@DisposableEffect onDispose {}
        val claims = statusBarClaims(view)
        claims.claim(controller.systemBarsAppearance and LightStatusBars)
        // Over dark art (light mode) the icons turn light; over light art (dark mode), dark.
        controller.setSystemBarsAppearance(if (dark) LightStatusBars else 0, LightStatusBars)
        onDispose {
            claims.release()?.let { found -> controller.setSystemBarsAppearance(found, LightStatusBars) }
        }
    }
}

/**
 * The claims holding one window's status bar icons over to the other tone, and what the window's
 * icons were before the first of them. Claims start and end in any order; the icons go back only
 * when the last ends.
 */
internal class StatusBarClaims {
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
     * while other claims still hold the icons.
     */
    fun release(): Int? {
        check(held > 0) { "No status bar claim is held." }
        return if (--held == 0) before else null
    }
}

/** Each window's claims, keyed on the window's root view, so any view in it finds them. */
private val windowClaims = WeakHashMap<View, StatusBarClaims>()

/** The claims on the status bar icons of [view]'s window. */
private fun statusBarClaims(view: View): StatusBarClaims =
    windowClaims.getOrPut(view.rootView) { StatusBarClaims() }

private const val LightStatusBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
