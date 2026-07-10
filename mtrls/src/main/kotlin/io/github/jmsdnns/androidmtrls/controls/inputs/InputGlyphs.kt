package io.github.jmsdnns.androidmtrls.controls.inputs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Input glyphs — hand-authored ImageVectors for the secure-field reveal toggle
//
// `:mtrls` depends only on `material-icons-core`, which lacks the `Visibility` /
// `VisibilityOff` glyphs, so [AMSecureField]'s reveal toggle — `eye.fill` /
// `eye.slash.fill` — is drawn here rather than pulled from
// `material-icons-extended`. Same zero-dependency precedent as the Canvas-drawn
// minus bar in `AMStepper`. (The trailing clear button uses core's
// `Icons.Filled.Close`, so it needs no drawn glyph.)
//
// Each vector fills solid black; the consuming `AMIconButton` recolors it with
// `ColorFilter.tint(Icon.muted)` (via `active = false`), matching the muted tone.
// The glyphs use `PathFillType.EvenOdd` so their interior detail (the ring +
// pupil of the eye) reads as a knockout — transparent negative space through
// which the host surface shows.
// ---------------------------------------------------------------------------

/** Builds a 24×24 solid-black [ImageVector] from an SVG-style path string. */
private fun inputGlyph(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.Black),
    ).build()

/**
 * Reveal glyph, concealed state — a filled eye (ring + pupil knockout), the
 * drawn twin of iOS `eye.fill`. Shown by [AMSecureField] while the password is
 * masked ("Show password").
 */
internal val AMInputEyeGlyph: ImageVector by lazy {
    inputGlyph(
        name = "AMInputEye",
        pathData = "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73" +
            "-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0" +
            "-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z",
    )
}

/**
 * Reveal glyph, revealed state — a slashed eye, the drawn twin of iOS
 * `eye.slash.fill`. Shown by [AMSecureField] while the password is visible
 * ("Hide password").
 */
internal val AMInputEyeSlashGlyph: ImageVector by lazy {
    inputGlyph(
        name = "AMInputEyeSlash",
        pathData = "M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 " +
            "3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 " +
            "7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 " +
            "0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05" +
            ".21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53" +
            "-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66" +
            "-1.34-3-3-3l-.17.01z",
    )
}
