package com.ideamtrls.androidmtrls

import androidx.compose.ui.unit.Dp

/**
 * The library's two blurs of what lies behind: pure Gaussian blurs with no tint, named by how much
 * of the scene they keep.
 *
 * ```kotlin
 * AMBackdrop(art = { AMRemoteImage(cover, contentDescription = null) }) {
 *     AMBackdropBlur(Modifier.matchParentSize(), blur = AMBlur.Frost)
 * }
 * ```
 *
 * - [Frost] is what frosted surfaces blur the art behind them by: cards, sections, bars, banners
 *   and sheets in a frosted app.
 * - [Mist] is lighter: the screen behind the remote ([AMActionsOverlay]) and the spotlight
 *   ([AMSpotlight]), a picture stilled in place ([Modifier.amPausedBackdrop][amPausedBackdrop]),
 *   and [AMBackdropBlur] by default.
 *
 * The art behind a scene ([Modifier.amArtBackdrop][amArtBackdrop]) has blurs of its own, set by
 * how much of the picture it keeps.
 *
 * @property radius the blur, as Android's blur radius (a Gaussian sigma of about
 *   `0.577 × radius`).
 */
public enum class AMBlur(public val radius: Dp) {
    /** The frosted surfaces' blur: 40 dp, a Gaussian sigma of about 24. */
    Frost(Style.Frosted.BlurRadius),

    /** The lighter blur behind overlays and of a stilled picture: 34 dp, a sigma of about 20. */
    Mist(Style.ModalOverlay.BlurRadius),
}
