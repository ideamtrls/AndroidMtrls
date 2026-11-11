package com.ideamtrls.androidmtrls.demo

import android.content.Intent
import java.time.LocalDateTime
import java.time.LocalTime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay

/**
 * Debug-only launch extras for driving the catalog from the command line (screenshots, page
 * checks). Every extra is a string (`--es <name> <value>`):
 *
 * - `entry <id>`: opens that entry on launch, over the catalog list.
 * - `scrollTo preview|loaded`: scrolls the opened page to that section.
 * - `present preview|loaded`: once the page appears, the [CatalogAutoPresent] demo in that
 *   section shows what it opens.
 * - `appearance light|dark|system`, `fontScale <float>`, `rtl true|false`: pin that inspection
 *   setting for this launch. The stored setting is left alone, and the inspection menu can't
 *   change a pinned one until a launch without the extra.
 * - `artwork night`: every catalog photo switches to its second set ([catalogPhoto]).
 * - `demoTime <H:MM>`: the clock-driven samples (the Date picker page's starting value, the Calendar
 *   scene's now marker) show that time of day, 24-hour, to agree with a pinned status bar clock
 *   ([clock]).
 * - `shopFillsCheckout yes`: the Shop scene's checkout opens with its form filled in.
 *
 * The entry id matches exactly; the other values match case-insensitively. Release builds ignore
 * every extra.
 *
 * ```
 * adb shell am start -S -n com.ideamtrls.androidmtrls.demo/.MainActivity --es entry card --es appearance dark
 * ```
 */
internal object LaunchOptions {
    var entry: String? = null
        private set

    /** The `scrollTo` section, lowercased like the ids [PageSection] carries. */
    var scrollTarget: String? = null
        private set

    var appearance: InspectionAppearance? = null
        private set

    var fontScale: Float? = null
        private set

    var rtl: Boolean? = null
        private set

    var artwork: String? = null
        private set

    /** The `shopFillsCheckout` extra: the Shop scene's checkout starts filled in. */
    var shopFillsCheckout: Boolean = false
        private set

    /** The `demoTime` time of day, or null when the extra is absent or not `H:MM`. */
    private var demoTime: LocalTime? = null

    private var present: String? = null
    private var didPresent = false

    /** Reads the extras from the intent that started the activity. */
    fun read(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        fun extra(name: String) = intent.getStringExtra(name)?.trim()?.takeIf { it.isNotEmpty() }

        entry = extra("entry")
        scrollTarget = extra("scrollTo")?.lowercase()
        present = extra("present")?.lowercase()
        appearance = extra("appearance")?.let { value ->
            InspectionAppearance.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
        }
        fontScale = extra("fontScale")?.toFloatOrNull()?.takeIf { it > 0f }
        rtl = extra("rtl")?.lowercase()?.toBooleanStrictOrNull()
        artwork = extra("artwork")?.lowercase()
        shopFillsCheckout = extra("shopFillsCheckout")?.lowercase() in setOf("yes", "true", "1")
        demoTime = extra("demoTime")?.split(":")?.mapNotNull { it.toIntOrNull() }
            ?.takeIf { it.size == 2 && it[0] in 0..23 && it[1] in 0..59 }
            ?.let { (hour, minute) -> LocalTime.of(hour, minute) }
    }

    /**
     * [now] at the `demoTime` time of day (the same day), or [now] itself without the extra. The
     * clock-driven samples read the time through this, so screenshots agree with a pinned status
     * bar clock.
     */
    fun clock(now: LocalDateTime = LocalDateTime.now()): LocalDateTime =
        demoTime?.let { now.toLocalDate().atTime(it) } ?: now

    /**
     * Whether the demo in section [target] (`preview` or `loaded`) should present itself now.
     * True at most once per process, so the first match wins and a demo doesn't present again
     * when its page comes back.
     */
    fun claimPresentation(target: String): Boolean {
        if (didPresent || present == null || present != target.lowercase()) return false
        didPresent = true
        return true
    }
}

/**
 * Debug hook for screenshots: calls [onPresent] once, shortly after it enters the composition,
 * when the `present` extra names this demo's section ([LocalCatalogTarget]). At most one demo
 * per launch.
 *
 * For a demo that shows something inline on its own (a hub opening its menu). Overlay demos
 * open from their own button instead.
 */
@Composable
internal fun CatalogAutoPresent(onPresent: () -> Unit) {
    val target = LocalCatalogTarget.current
    val present by rememberUpdatedState(onPresent)
    LaunchedEffect(target) {
        if (!LaunchOptions.claimPresentation(target)) return@LaunchedEffect
        // Let the push and any scrollTo jump settle first.
        delay(800)
        present()
    }
}
