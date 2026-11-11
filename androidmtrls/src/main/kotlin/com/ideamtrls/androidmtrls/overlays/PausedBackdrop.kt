package com.ideamtrls.androidmtrls

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp

/** What [Modifier.amPausedBackdrop][amPausedBackdrop] lays over the blurred picture. */
public enum class AMPausedScrim {
    /**
     * The modal backdrop: the blur alone in light mode, and a black dim of
     * [AMTheme.Overlay.darkDimAlpha] over it in dark mode, the look behind the remote and the
     * spotlight.
     */
    Modal,

    /**
     * The page wash: [AMTheme.Surface.page] at 0.55 over the blur in both modes, so the picture's
     * color bleeds through while the page still reads as itself. A photo's detail view behind the
     * photo.
     */
    Page,
}

/**
 * Stills this picture into a backdrop: the content blurs (a pure Gaussian blur of the content
 * itself, its edges clamped so they never fade to transparency) and [scrim] lies over it.
 *
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     AMRemoteImage(photo.url, contentDescription = null, Modifier.matchParentSize().amPausedBackdrop(AMPausedScrim.Page))
 *     PhotoDetail(photo)
 * }
 * ```
 *
 * Put it on the picture (a photo, an album's art) and stack the screen's content above it. It's
 * decoration: screen readers skip it and it takes no input, so nothing in it reacts while it's
 * paused. Not a blocking layer of its own; for a layer that pauses the whole screen and waits for
 * a choice, use [AMActionsOverlay] or [AMSpotlight].
 *
 * To swap the picture under a steady blur (a gallery moving to the next photo), add
 * [Modifier.amCrossfade][amCrossfade] after it, keyed by the picture:
 * `Modifier.amPausedBackdrop(AMPausedScrim.Page).amCrossfade(photo.id)`. The new picture fades in
 * under the blur and the scrim, which stay put.
 *
 * @param scrim what lies over the blur: [AMPausedScrim.Modal] (the default) or
 *   [AMPausedScrim.Page].
 * @param blurRadius the blur, as Android's blur radius: [AMTheme.Overlay.blurRadius] by default.
 */
public fun Modifier.amPausedBackdrop(
    scrim: AMPausedScrim = AMPausedScrim.Modal,
    blurRadius: Dp = AMTheme.Overlay.blurRadius,
): Modifier =
    this
        .then(PausedScrimElement(scrim))
        .clearAndSetSemantics {}
        // Seen first, outermost, and kept: nothing inside reacts.
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
        .blur(blurRadius, BlurredEdgeTreatment.Rectangle)

private data class PausedScrimElement(val scrim: AMPausedScrim) : ModifierNodeElement<PausedScrimNode>() {
    override fun create() = PausedScrimNode(scrim)

    override fun update(node: PausedScrimNode) {
        node.scrim = scrim
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "amPausedBackdrop"
        properties["scrim"] = scrim
    }
}

/** Draws the scrim over the blurred content, following the theme's appearance. */
private class PausedScrimNode(var scrim: AMPausedScrim) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {

    override fun ContentDrawScope.draw() {
        drawContent()
        val dark = currentValueOf(LocalAMDarkTheme) ?: false
        when (scrim) {
            AMPausedScrim.Modal -> if (dark) drawRect(Color.Black.copy(alpha = Style.ModalOverlay.DarkDimAlpha))
            AMPausedScrim.Page -> drawRect((if (dark) Style.Page.Dark else Style.Page.Light).copy(alpha = Style.ModalOverlay.PageWashAlpha))
        }
    }
}
