package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow

/**
 * A region with art behind it, as what's over it samples it: [layer], the art alone, which the
 * frosted surfaces on it blur; [screen], the region whole (the art and everything on it), which a
 * sheet over it blurs from its own window. Each records itself each time it draws. And where the
 * region sits, so a surface can find its own slice.
 */
@Stable
internal class AMBackdropSource(val layer: GraphicsLayer, val screen: GraphicsLayer) {
    /** The art's placement, once it has one: the region's, which the art fills. */
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
 * The page backdrops a scene's root views draw, for what the scene presents over them: a root's
 * outermost [AMBackdrop] or [AMArtRowsBackdrop] reports itself here while it's composed. The last
 * one composed is the showing one: the showing root's, or, while Navigation 3 moves between panels,
 * the one it brings to the top (only the visible entries are composed). Snapshot state, so a
 * reader recomposes when it changes.
 */
@Stable
internal class AMSceneBackdrops {
    private val sources = mutableStateListOf<AMBackdropSource>()

    /** The backdrop the showing root, or the panel on top of it, draws; null while it draws none. */
    val showing: AMBackdropSource?
        get() = sources.lastOrNull()

    fun add(source: AMBackdropSource) {
        sources += source
    }

    fun remove(source: AMBackdropSource) {
        sources -= source
    }
}

/** The scene's page backdrops, around its root views; null elsewhere, and inside a backdrop. */
internal val LocalAMSceneBackdrops = staticCompositionLocalOf<AMSceneBackdrops?> { null }

/**
 * Reports [source] to the scene around it while composed, when it's a root view's outermost
 * backdrop, and returns what to provide to its content, so the backdrops inside it (a row's own
 * art) don't report.
 */
@Composable
internal fun sceneBackdrop(source: AMBackdropSource): ProvidedValue<AMSceneBackdrops?> {
    val scene = LocalAMSceneBackdrops.current
    DisposableEffect(scene, source) {
        if (scene == null) return@DisposableEffect onDispose {}
        scene.add(source)
        onDispose { scene.remove(source) }
    }
    return LocalAMSceneBackdrops provides null
}

/**
 * Records what this draws, the art, into [source]'s layer, then draws the layer, so the art shows
 * as usual and frosted surfaces can sample it.
 */
internal fun Modifier.backdropSource(source: AMBackdropSource): Modifier =
    this
        .onPlaced { source.coordinates = it }
        .drawWithContent {
            source.layer.record { this@drawWithContent.drawContent() }
            drawLayer(source.layer)
        }

/**
 * Records what this draws, the region whole with its art, into [source]'s screen layer, then
 * draws the layer, so the region shows as usual and a sheet over it can blur it from its own
 * window. It goes on the region's box, which the art fills, so both layers share the art's place.
 */
internal fun Modifier.backdropScreen(source: AMBackdropSource): Modifier =
    drawWithContent {
        source.screen.record { this@drawWithContent.drawContent() }
        drawLayer(source.screen)
    }
