package com.ideamtrls.androidmtrls

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor

/**
 * How a floating surface draws, from the app's [AMSurfaceFill] and where the surface stands
 * ([surfaceLook]).
 */
internal enum class SurfaceLook {
    /** The opaque floating surface: its shadow, the surface color, the dark-mode hairline. */
    Solid,

    /**
     * The frosted recipe on the backdrop: the shadow outside the shape, the art behind blurred
     * under the frosted fill, the frosted edge. With no art to sample, frost over the page color.
     */
    Frosted,

    /**
     * A piece embedded in a frosted surface: the shadow outside the shape, the tint
     * ([AMTheme.Frosted.embeddedFill]) over the frost it sits on with no blur of its own, the
     * frosted edge. It never samples.
     */
    Tinted,
}

/** What content lies on. */
internal enum class LiesOn {
    /** The backdrop: the page, or art. A screen's root and an overlay window's start here. */
    Page,

    /** A floating surface: a card, a section, a disclosure group, a list card, tabs ([ProvideAMOnSurface]). */
    Surface,

    /** A sheet, which is a window of its own, so a piece on it can't find it in the layout tree. */
    Sheet,
}

/**
 * What the content here lies on: [LiesOn.Page] at the root. Dynamic, so a modifier node that reads
 * it while drawing redraws on a change.
 */
internal val LocalAMLiesOn = compositionLocalOf { LiesOn.Page }

/**
 * The look of a surface under the app's [setting]: solid in a solid app; in a frosted app, frosted
 * on the backdrop and tinted where it's [embedded], so it never samples frost through frost.
 */
internal fun surfaceLook(setting: AMSurfaceFill, embedded: Boolean): SurfaceLook = when {
    setting == AMSurfaceFill.Solid -> SurfaceLook.Solid
    embedded -> SurfaceLook.Tinted
    else -> SurfaceLook.Frosted
}

/**
 * Whether a surface at this node is embedded in another surface: inside a card (any
 * [NestingSurfaceKey] node above it in the layout tree, flat or not), or where the composition says
 * its content lies on a surface or a sheet ([LocalAMLiesOn]), which a sheet, being a window, and
 * tabs, being no card, can only say that way. A solid control there steps its dark-mode outline up
 * to [AMTheme.controlOutline], since the selection ring would vanish against the black around it.
 *
 * Call it only while the node is attached (in measure, layout or draw).
 */
internal fun <T> T.isEmbedded(): Boolean where T : DelegatableNode, T : CompositionLocalConsumerModifierNode =
    currentValueOf(LocalAMLiesOn) != LiesOn.Page || findNearestAncestor(NestingSurfaceKey) != null

/**
 * The look of a surface of [fill] at this node ([surfaceLook]). It asks where the surface stands
 * ([isEmbedded], a walk up the tree) only for a frosted fill, since a solid surface is solid
 * wherever it stands, so a solid one repositioned every scroll frame never walks.
 *
 * Call it only while the node is attached (in measure, layout, draw or positioning).
 */
internal fun <T> T.lookOf(fill: AMSurfaceFill): SurfaceLook where T : DelegatableNode, T : CompositionLocalConsumerModifierNode =
    surfaceLook(fill, embedded = fill == AMSurfaceFill.Frosted && isEmbedded())

/**
 * The art a surface of [look] samples: the nearest [AMBackdrop]'s when it's frosted, none
 * otherwise (solid and tinted surfaces never sample).
 */
internal fun CompositionLocalConsumerModifierNode.backdropFor(look: SurfaceLook): AMBackdropSource? =
    if (look == SurfaceLook.Frosted) currentValueOf(LocalAMBackdrop) else null
