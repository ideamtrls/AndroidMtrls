package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced

/**
 * The art behind a region, as frosted surfaces sample it: the layer the art records itself into
 * each time it draws, and where the art sits, so a surface can find its own slice.
 */
@Stable
internal class AMBackdropSource(val layer: GraphicsLayer) {
    /** The art's placement, once it has one. */
    var coordinates: LayoutCoordinates? = null
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
