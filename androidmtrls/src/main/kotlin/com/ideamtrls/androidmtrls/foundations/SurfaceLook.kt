package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
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
     * under the frosted fill, the frosted edge. With no art to sample, frost over the page color;
     * in a blurred window, frost over the window's own blur, and solid while the system has
     * cross-window blur off ([frostPath]).
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

/** What a [SurfaceLook.Frosted] surface draws under its frosted fill ([frostPath]). */
internal enum class FrostPath {
    /** The art of the nearest [AMBackdrop] under the surface, blurred. */
    Sampled,

    /** The page color: frost over the page, opaque, while there's no art to sample. */
    OverPage,

    /**
     * Nothing: in a blurred window (the remote, the spotlight) the window's own blur-behind under
     * its wash is the blur, so the fill lies straight over it and samples nothing.
     */
    OverWindowBlur,

    /**
     * No frost at all: in a blurred window while the system has cross-window blur off, there's no
     * blur to frost over, so the surface draws solid, never unblurred translucency.
     */
    Solid,
}

/**
 * The path a frosted surface takes: in a blurred window ([windowBlur] not null), frost over the
 * window's blur while cross-window blur is on and solid while it's off; elsewhere ([windowBlur]
 * null) the art sampled once it's placed ([artPlaced]), frost over the page until then.
 */
internal fun frostPath(artPlaced: Boolean, windowBlur: Boolean?): FrostPath = when (windowBlur) {
    true -> FrostPath.OverWindowBlur
    false -> FrostPath.Solid
    null -> if (artPlaced) FrostPath.Sampled else FrostPath.OverPage
}

/**
 * Whether the blurred window this content is in (the remote's, the spotlight's) has its
 * blur-behind: true while the system allows cross-window blur, false while it doesn't (battery
 * saver), null outside a blurred window. [AMOverlayWindow] provides it; every other window resets
 * it to null ([ProvideOverlayWindowLocals]). Dynamic, so a node that reads it while drawing redraws
 * when the system's setting flips.
 */
internal val LocalAMWindowBlur = compositionLocalOf<Boolean?> { null }

/**
 * The art a surface of [look] samples: the nearest [AMBackdrop]'s when it's frosted, none
 * otherwise (solid and tinted surfaces never sample).
 */
internal fun CompositionLocalConsumerModifierNode.backdropFor(look: SurfaceLook): AMBackdropSource? =
    if (look == SurfaceLook.Frosted) currentValueOf(LocalAMBackdrop) else null

/** What a text field's container draws ([fieldContainer]). */
internal enum class FieldContainer {
    /** The field's own Material default: see-through on the page, or whatever the field uses. */
    Clear,

    /** The opaque surface color, so a field on a sheet's frost is never see-through. */
    Solid,

    /** The tint ([AMTheme.Frosted.embeddedFill]) over the surface the field sits on. */
    Tint,
}

/**
 * The container of a text field under the app's [setting] where it [liesOn]: the tint in a frosted
 * app off the page; solid on a sheet in a solid app, since a sheet is frost in either setting;
 * otherwise the field's own.
 */
internal fun fieldContainer(setting: AMSurfaceFill, liesOn: LiesOn): FieldContainer = when {
    setting == AMSurfaceFill.Frosted && liesOn != LiesOn.Page -> FieldContainer.Tint
    setting == AMSurfaceFill.Solid && liesOn == LiesOn.Sheet -> FieldContainer.Solid
    else -> FieldContainer.Clear
}

/** The [fieldContainer] at this point of the composition. */
@Composable
@ReadOnlyComposable
internal fun fieldContainerKind(): FieldContainer = fieldContainer(AMTheme.surfaceFill, LocalAMLiesOn.current)

/**
 * The container color of a text field here: [AMTheme.Frosted.embeddedFill] for a tint,
 * [AMTheme.Surface.primary] for solid, and [clear] (the field's own Material default) otherwise.
 */
@Composable
@ReadOnlyComposable
internal fun fieldContainerColor(clear: Color): Color = when (fieldContainerKind()) {
    FieldContainer.Tint -> AMTheme.Frosted.embeddedFill
    FieldContainer.Solid -> AMTheme.Surface.primary
    FieldContainer.Clear -> clear
}
