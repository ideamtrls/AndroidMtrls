package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The nearest owner's record of which overlays are up. [AMScene] provides one so it can fade its
 * floating action button while any overlay (a sheet, an alert, the actions overlay, the spotlight,
 * the loading overlay) is showing. Overlays report through [ReportOverlayUp]; with no owner above
 * them, reporting does nothing.
 */
internal class AMOverlayRegistry {
    private val keys = mutableSetOf<Any>()
    private var count by mutableIntStateOf(0)

    /** True while at least one overlay is up. Snapshot-observable. */
    val anyUp: Boolean get() = count > 0

    fun register(key: Any) {
        if (keys.add(key)) count = keys.size
    }

    fun unregister(key: Any) {
        if (keys.remove(key)) count = keys.size
    }
}

/** The nearest [AMOverlayRegistry], or null when no scene owns one. */
internal val LocalAMOverlayRegistry = staticCompositionLocalOf<AMOverlayRegistry?> { null }

/**
 * Reports to the nearest [AMOverlayRegistry] that an overlay is up while [up] is true, and
 * withdraws the report when [up] turns false or this leaves the composition.
 */
@Composable
internal fun ReportOverlayUp(up: Boolean) {
    val registry = LocalAMOverlayRegistry.current
    val key = remember { Any() }
    DisposableEffect(registry, up) {
        if (up) registry?.register(key)
        onDispose { registry?.unregister(key) }
    }
}
