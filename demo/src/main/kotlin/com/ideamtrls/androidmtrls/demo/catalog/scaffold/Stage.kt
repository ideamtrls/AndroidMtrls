package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMTheme

/**
 * Where a demo (the Preview, the Loaded composition) sits on the page. There is never a box around
 * it: a demo either floats on the page within the page gutters, or runs edge to edge as a flat
 * region.
 */
internal enum class StageLayout {
    /**
     * Directly on the page, within the page gutters, start-aligned, with nothing around it.
     * Surfaces (cards, filled buttons, chips) float the way an app places them; flat content
     * brings its own card.
     */
    Floating,

    /**
     * Out past the page gutters, edge to edge of the screen: a flat strip whose content carries
     * the surface color (a bare field row).
     */
    FullBleed,

    /**
     * Out past the page gutters like [FullBleed], for a component that carries its own side
     * insets and floats its own surfaces (an action grid, whose tiles land on the gutters).
     */
    Wide,

    /**
     * Edge to edge on a surface-colored band the scaffold paints, clipped to it: content that is
     * itself a small screen (a floating action hub's scene), so the band is the screen.
     */
    Fill;

    companion object {
        /** The page's side inset. [PageSection] insets its content by it; wide stages reach back out past it. */
        val PageGutter = 16.dp
    }
}

/**
 * The canvas a component renders on: the page itself. Components sit exactly where an app would
 * put them (surfaces float on the page, widgets inside the container they bring), placed per
 * [layout]. Only [StageLayout.Fill] clips, so shadows fall onto the page.
 */
@Composable
internal fun Stage(layout: StageLayout = StageLayout.Floating, content: @Composable () -> Unit) {
    val surface = AMTheme.Surface.primary
    val modifier = when (layout) {
        StageLayout.Floating -> Modifier.fillMaxWidth()
        StageLayout.FullBleed, StageLayout.Wide -> Modifier.bleed(StageLayout.PageGutter).fillMaxWidth()
        StageLayout.Fill -> Modifier.bleed(StageLayout.PageGutter).fillMaxWidth().background(surface).clipToBounds()
    }
    val alignment = if (layout == StageLayout.Floating) Alignment.Start else Alignment.CenterHorizontally
    Column(modifier, horizontalAlignment = alignment) { content() }
}

/**
 * One of the page's two demo sections, inset by the page gutter. It shows no header: its title only
 * names it, as the `scrollTo` and `present` target of the demos inside it (`preview`, `loaded`).
 */
@Composable
internal fun PageSection(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = StageLayout.PageGutter)) {
        CompositionLocalProvider(LocalCatalogTarget provides title.lowercase(), content = content)
    }
}

/**
 * What the `present` extra matches for the demos in this subtree: the enclosing section's
 * lowercased title (`preview`, `loaded`).
 */
internal val LocalCatalogTarget = staticCompositionLocalOf { "" }

/**
 * Widens the content by [gutter] on each side, so it runs edge to edge past the page gutters.
 * Done in layout: the parent still sees its own width, and nothing moves around it.
 */
private fun Modifier.bleed(gutter: Dp): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val inset = gutter.roundToPx()
    val width = constraints.maxWidth + inset * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-inset, 0) }
}
