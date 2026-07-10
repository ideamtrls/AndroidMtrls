package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.resolved

// ---------------------------------------------------------------------------
// Private switch track/thumb metrics (M3 switch morphology, monochrome palette)
// ---------------------------------------------------------------------------

/** Width of the switch track in dp. */
private val trackWidth = 52.dp

/** Height of the switch track (also the capsule diameter) in dp. */
private val trackHeight = 32.dp

/** Off-state thumb diameter in dp — the small dot that reads as "off". */
private val thumbDiameterOff = 16f

/** On-state thumb diameter in dp — grows to fill the track when checked. */
private val thumbDiameterOn = 24f

/** Pressed thumb diameter in dp — swells while held, in either state. */
private val thumbDiameterPressed = 28f

/** Diameter of the check glyph drawn on the thumb when ON, in dp. */
private val thumbIconSize = 16f

/**
 * Rest inset of the thumb CENTRE from the near track end in dp.
 *
 * Both rest centres sit 16dp in from their respective track ends of the
 * 52×32 track: off centre = 16dp from the leading edge (16dp thumb → its
 * edge is 8dp from that end); on centre = 16dp from the trailing edge (24dp
 * thumb → its edge is 4dp from that end).
 */
private val thumbRestInset = 16f

/** Off-state track border width in dp. */
private val trackBorderWidth = 1f

/**
 * Spring for the thumb position + diameter — the crisp "snap to rest" feel.
 *
 * Lockstep mirror of [AMTheme.Motion.snapSpring] (dampingRatio = 0.86,
 * stiffness = 384). The house motion token is declared as `spring<Float>` and
 * this animates `Float` state, so the parameters are reproduced here as a
 * file-level constant shared by both thumb animations. If the token values are
 * ever tuned, this val must be updated in lockstep. The colours animate on a
 * separate, softer curve ([AMTheme.Motion.selectionFade]).
 */
private val thumbSnapSpring = spring<Float>(dampingRatio = 0.86f, stiffness = 384f)

/**
 * Colour cross-fade for the track fill, border, and thumb.
 *
 * Lockstep mirror of [AMTheme.Motion.selectionFade] (150ms, EaseInOut) —
 * the house selection-commit curve. The token is declared as `AnimationSpec<Float>`
 * and [animateColorAsState] needs `AnimationSpec<Color>`, so the identical
 * parameters are reproduced here as a `tween<Color>`. If the token values are
 * ever tuned, this val must be updated in lockstep.
 */
private val selectionColorFade = tween<Color>(durationMillis = 150, easing = EaseInOut)

// ---------------------------------------------------------------------------
// Private draw helper — pure Canvas, no Material3
// ---------------------------------------------------------------------------

/**
 * Draws the M3 switch track (capsule + optional hairline border), its thumb, and
 * the on-state check glyph inside a [DrawScope]. No drop shadow — the signature
 * look is flat.
 *
 * @param trackFill      Resolved capsule fill colour.
 * @param borderColor    Resolved off-state border colour (animates to transparent when on).
 * @param thumbColor     Resolved thumb colour.
 * @param positionFraction Fractional offset of the thumb centre in [0f, 1f];
 *                         0 = off (leading rest), 1 = on (trailing rest).
 * @param thumbDiameterDp Current thumb diameter in dp (animates across off/on/pressed).
 * @param checkPainter   Painter for the Material check glyph shown on the thumb when ON.
 * @param checkColor     Resolved tint for the check glyph — reads against the thumb.
 * @param iconAlpha      Opacity of the check glyph in [0f, 1f]; 0 = OFF (glyph hidden),
 *                       1 = ON. Fades in on the same curve as the thumb travel.
 */
private fun DrawScope.drawSwitchTrack(
    trackFill: Color,
    borderColor: Color,
    thumbColor: Color,
    positionFraction: Float,
    thumbDiameterDp: Float,
    checkPainter: Painter,
    checkColor: Color,
    iconAlpha: Float,
) {
    val trackW = trackWidth.toPx()
    val trackH = trackHeight.toPx()

    // Centre the track within the draw area.
    val left = (size.width - trackW) / 2f
    val top = (size.height - trackH) / 2f
    val radius = trackH / 2f

    // ── Track fill capsule ──────────────────────────────────────────────────
    drawRoundRect(
        color = trackFill,
        topLeft = Offset(left, top),
        size = Size(trackW, trackH),
        cornerRadius = CornerRadius(radius),
    )

    // ── Off-state border (animates to transparent when on) ─────────────────────
    // Inset by half the stroke so the outline stays crisp inside the capsule.
    val stroke = trackBorderWidth.dp.toPx()
    drawRoundRect(
        color = borderColor,
        topLeft = Offset(left + stroke / 2f, top + stroke / 2f),
        size = Size(trackW - stroke, trackH - stroke),
        cornerRadius = CornerRadius(radius - stroke / 2f),
        style = Stroke(width = stroke),
    )

    // ── Thumb ───────────────────────────────────────────────────────────────
    val inset = thumbRestInset.dp.toPx()
    val thumbCenterMin = left + inset            // off rest: 16dp from leading edge
    val thumbCenterMax = left + trackW - inset   // on  rest: 16dp from trailing edge
    val thumbCX = thumbCenterMin + (thumbCenterMax - thumbCenterMin) * positionFraction
    val thumbCY = top + trackH / 2f

    drawCircle(
        color = thumbColor,
        radius = thumbDiameterDp.dp.toPx() / 2f,
        center = Offset(thumbCX, thumbCY),
    )

    // ── Thumb check glyph (ON only; fades in with the thumb travel) ────────────
    if (iconAlpha > 0f) {
        val iconPx = thumbIconSize.dp.toPx()
        translate(left = thumbCX - iconPx / 2f, top = thumbCY - iconPx / 2f) {
            with(checkPainter) {
                draw(
                    size        = Size(iconPx, iconPx),
                    alpha       = iconAlpha,
                    colorFilter = ColorFilter.tint(checkColor),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMToggleRow — composable-slot primary overload
// ---------------------------------------------------------------------------

/**
 * Flat row widget: [label] on the leading side, an M3-morphology switch
 * (capsule track + travelling thumb, Canvas-drawn) on the trailing side.
 *
 * A flat widget — the caller's card or section provides the surface:
 * ```kotlin
 * AMCard {
 *     AMToggleRow(
 *         checked          = isOn,
 *         onCheckedChange  = { isOn = it },
 *     ) {
 *         BasicText("Notifications", style = headlineStyle)
 *     }
 * }
 * ```
 *
 * The entire row is tappable (mirrors iOS row toggle behaviour). Press
 * feedback is the monochrome `amRipple()` state layer — never opacity-dim.
 *
 * @param checked          Current toggle state.
 * @param onCheckedChange  Called when the user taps the row or the thumb.
 * @param modifier         Modifier applied to the row's root layout node.
 * @param label            Composable label rendered in the leading [Column]. Use a
 *                         [Column] with multiple texts for the multi-line variant;
 *                         [ColumnScope] matches iOS's `VStack` in the `Toggle { }` closure.
 */
@Composable
fun AMToggleRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val reduceMotion = LocalAMReduceMotion.current

    // Press feedback — the monochrome amRipple() state layer, never opacity-dim.
    val interactionSource = remember { MutableInteractionSource() }

    // Pressed state drives the thumb GEOMETRY (it swells to 28dp), NOT an alpha
    // dim — a legitimate, deliberate non-press-feedback read of the same
    // interaction source; press feedback itself stays the amRipple() sweep.
    val pressed by interactionSource.collectIsPressedAsState()

    // Thumb geometry (position + diameter) and the colour cross-fades route
    // through the house Reduce-Motion policy (tokens/Motion.kt). Under RM the
    // snapSpring and the selection fade both soften to the overlay-fade tween:
    // the thumb still settles to its on/off/pressed state and the colours still
    // cross-fade, without the spring. Position + diameter share thumbSpec so the
    // two thumb animations stay in lockstep.
    val thumbSpec = tokens.motion.resolved(thumbSnapSpring, reduceMotion)
    val colorSpec = tokens.motion.resolved(selectionColorFade, reduceMotion)

    // Thumb travel: 0f = off (leading rest), 1f = on (trailing rest).
    // Position + diameter snap on AMTheme.Motion.snapSpring (mirrored above).
    val positionFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = thumbSpec,
        label = "AMToggleRow.position",
    )
    val thumbDiameter by animateFloatAsState(
        targetValue = when {
            pressed -> thumbDiameterPressed
            checked -> thumbDiameterOn
            else -> thumbDiameterOff
        },
        animationSpec = thumbSpec,
        label = "AMToggleRow.thumbDiameter",
    )

    // Colours cross-fade on the selection-commit curve (selectionColorFade,
    // the Color-typed mirror of AMTheme.Motion.selectionFade).
    // On track = the live, overridable accent tint (follows the app/subtree
    // accent decision, not a fixed hue); off track = the neutralFill recipe
    // (Black/White @ 0.08) computed from isDark directly (a draw scope can't
    // read the @Composable neutralFill()).
    val offTrackFill = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val trackFill by animateColorAsState(
        targetValue = if (checked) tokens.semantic.accent.resolve(isDark) else offTrackFill,
        animationSpec = colorSpec,
        label = "AMToggleRow.trackFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Color.Transparent else AMTheme.separator.resolve(isDark),
        animationSpec = colorSpec,
        label = "AMToggleRow.trackBorder",
    )
    // On thumb = the primary surface (white light / black dark), so the knob
    // adapts like every other mtrls surface; off thumb = the muted-icon dot.
    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            AMTheme.Surface.primary.resolve(isDark)
        } else {
            AMTheme.Icon.muted.resolve(isDark)
        },
        animationSpec = colorSpec,
        label = "AMToggleRow.thumb",
    )

    // Material-style check glyph on the thumb, shown only when ON. It fades in on
    // the same thumbSpec as the thumb travel and is tinted to the accent (the ON
    // track colour) so it reads against the primary-surface thumb.
    val checkPainter = rememberVectorPainter(Icons.Default.Check)
    val checkColor = tokens.semantic.accent.resolve(isDark)
    val iconAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = thumbSpec,
        label = "AMToggleRow.iconAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AMTheme.rowMinHeight.dp)
            .toggleable(
                value             = checked,
                interactionSource = interactionSource,
                indication        = amRipple(),
                role              = Role.Switch,
                onValueChange     = onCheckedChange,
            )
            .padding(
                horizontal = tokens.spacing.lg,
                vertical   = tokens.spacing.md,
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // Leading label — fills remaining width, leading alignment.
        Column(modifier = Modifier.weight(1f)) {
            label()
        }

        // Trailing M3 switch — capsule track + travelling thumb.
        Box(
            modifier = Modifier
                .size(width = trackWidth, height = trackHeight)
                .drawBehind {
                    drawSwitchTrack(
                        trackFill        = trackFill,
                        borderColor      = borderColor,
                        thumbColor       = thumbColor,
                        positionFraction = positionFraction,
                        thumbDiameterDp  = thumbDiameter,
                        checkPainter     = checkPainter,
                        checkColor       = checkColor,
                        iconAlpha        = iconAlpha,
                    )
                },
        )
    }
}

// ---------------------------------------------------------------------------
// AMToggleRow — String convenience overload
// ---------------------------------------------------------------------------

/**
 * Flat row widget — String convenience overload.
 *
 * Applies the standard headline style ([AMTheme.Type.title], [AMTheme.Text.primary])
 * to [label]. Equivalent to the composable-slot overload with a pre-styled
 * [BasicText] label.
 *
 * ```kotlin
 * AMToggleRow(
 *     label            = "Notifications",
 *     checked          = isOn,
 *     onCheckedChange  = { isOn = it },
 * )
 * ```
 *
 * @param label           The toggle label text.
 * @param checked         Current toggle state.
 * @param onCheckedChange Called when the user taps the row.
 * @param modifier        Modifier applied to the row's root layout node.
 */
@Composable
fun AMToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AMToggleRow(
        checked         = checked,
        onCheckedChange = onCheckedChange,
        modifier        = modifier,
    ) {
        AMToggleLabel(label)
    }
}

// ---------------------------------------------------------------------------
// Private helper — standard headline label
// ---------------------------------------------------------------------------

/**
 * Standard headline-style label for [AMToggleRow] String overload.
 *
 * Renders the label with [AMTheme.Type.title] in [AMTheme.Text.primary].
 */
@Composable
private fun AMToggleLabel(text: String) {
    val isDark = LocalAMIsDarkTheme.current
    BasicText(
        text  = text,
        style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
    )
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------

@Composable
private fun TogglePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // String convenience: on state.
        AMToggleRow(
            label           = "Notifications",
            checked         = true,
            onCheckedChange = {},
        )

        // String convenience: off state.
        AMToggleRow(
            label           = "Autoplay",
            checked         = false,
            onCheckedChange = {},
        )

        // Composable-slot overload: multi-line label.
        AMToggleRow(
            checked         = true,
            onCheckedChange = {},
        ) {
            BasicText(
                text  = "Background Refresh",
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text  = "Update content when on Wi-Fi",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

@Preview(
    name            = "AMToggleRow · Light",
    group           = "AMToggleRow",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMToggleRowPreviewLight() {
    AMThemeRoot {
        TogglePreviewContent()
    }
}

@Preview(
    name            = "AMToggleRow · Dark",
    group           = "AMToggleRow",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMToggleRowPreviewDark() {
    AMThemeRoot {
        TogglePreviewContent()
    }
}
