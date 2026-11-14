package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow

/**
 * The art behind a region, as frosted surfaces sample it: the layer the art records itself into
 * each time it draws, and where the art sits, so a surface can find its own slice.
 */
@Stable
internal class AMBackdropSource(val layer: GraphicsLayer) {
    /** The art's placement, once it has one. */
    var coordinates: LayoutCoordinates? = null

    /**
     * How many bars on this art have their frosted fill in under the status bar: while any does,
     * the status bar lies on the fill, not on the art. Snapshot state.
     */
    var topCovers: Int by mutableIntStateOf(0)
}

/**
 * An art surface's say over the status bar: while its region reaches the top of the window and no
 * bar on it has its fill in, art of [tone] lies under the status bar ([AMStatusBarIcons]). It
 * returns the modifier that tracks whether the region reaches the top, for the region's box.
 */
@Composable
internal fun artStatusBar(source: AMBackdropSource, tone: AMArtTone): Modifier {
    var atTop by remember(source) { mutableStateOf(false) }
    AMStatusBarIcons(tone, enabled = atTop && source.topCovers == 0)
    return Modifier.onGloballyPositioned { coordinates ->
        val top = coordinates.positionInWindow().y
        atTop = top <= 0f && top + coordinates.size.height > 0f
    }
}

/** Counts this bar against [source] while its frosted fill [covers] the status bar. */
@Composable
internal fun ReportTopCover(source: AMBackdropSource?, covers: Boolean) {
    DisposableEffect(source, covers) {
        if (source == null || !covers) return@DisposableEffect onDispose {}
        source.topCovers += 1
        onDispose { source.topCovers -= 1 }
    }
}

/**
 * The nearest [AMBackdrop]'s art, or null where there's none. The nearest wins, so a row's own
 * art beats the page's. Dynamic, so a surface reading it while drawing redraws when it changes.
 */
internal val LocalAMBackdrop = compositionLocalOf<AMBackdropSource?> { null }

/**
 * Records what this draws into [source]'s layer, then draws the layer, so the art shows as usual
 * and frosted surfaces can sample it.
 */
internal fun Modifier.backdropSource(source: AMBackdropSource): Modifier =
    this
        .onPlaced { source.coordinates = it }
        .drawWithContent {
            source.layer.record { this@drawWithContent.drawContent() }
            drawLayer(source.layer)
        }
