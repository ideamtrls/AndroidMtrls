package io.github.jmsdnns.androidmtrls.controls.inputs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedOrNull

// ---------------------------------------------------------------------------
// Filled text-field anatomy — shared building blocks
//
// The single-line and multiline AndroidMtrls inputs ([AMTextField],
// [AMSecureField], [AMTextEditor]) share the Android filled-field anatomy on
// the mtrls surface language: a `Surface.primary` container (a floating white/
// black surface, with the dark-mode-only card hairline) in a top-rounded
// (bottom-square) silhouette, and a bottom indicator line that thickens and
// tints to the accent on focus. The fields draw NO label of their own — in the
// Mtrls UI the label is external, a standing [AMFieldLabel] above the field.
// [AMSearchField] reuses the fill and tap-to-focus wiring but takes a pill
// silhouette with no indicator.
// ---------------------------------------------------------------------------

/** Minimum height of a filled single-line field (Android field metric). */
private val FIELD_MIN_HEIGHT = 56.dp

/** Bottom indicator thickness when the field is unfocused. */
private val FIELD_INDICATOR_UNFOCUSED = 1.dp

/** Bottom indicator thickness when the field is focused. */
private val FIELD_INDICATOR_FOCUSED = 2.dp

/**
 * Silhouette of the filled single-line fields ([AMTextField], [AMSecureField]):
 * 4dp circular-arc top corners, square bottom corners — the bottom-heavy Android
 * filled-field shape (deliberately a plain [RoundedCornerShape], not `amSquircle`).
 */
internal val amFilledFieldShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 4.dp,
    bottomEnd = 0.dp,
    bottomStart = 0.dp,
)

/**
 * Silhouette of the filled multiline editor ([AMTextEditor]): all four corners
 * rounded 4dp, since a text block reads better without a bottom-heavy shape.
 */
internal val amFilledEditorShape = RoundedCornerShape(4.dp)

/** Silhouette of the search field ([AMSearchField]): a full pill / capsule. */
internal val amSearchFieldShape = RoundedCornerShape(percent = 50)

// Color- and Dp-typed lockstep mirrors of AMTheme.Motion.selectionFade
// (tween<Float>(150, EaseInOut)). animateColorAsState / animateDpAsState need a
// typed spec that the AnimationSpec<Float> motion token cannot supply directly —
// retune these together with selectionFade.
private val fieldIndicatorColorSpec = tween<Color>(durationMillis = 150, easing = EaseInOut)
private val fieldIndicatorThicknessSpec = tween<Dp>(durationMillis = 150, easing = EaseInOut)

// ---------------------------------------------------------------------------
// AMFilledField — the shared filled-container scaffold
// ---------------------------------------------------------------------------

/**
 * Draws the shared filled-field chrome — the `Surface.primary` fill, [shape]
 * silhouette, and the animated bottom indicator, plus the [placeholder]
 * overlay — around a caller-supplied [field] (a bare `BasicTextField` that owns
 * its own keyboard/IME wiring) and an optional [trailing] affordance.
 *
 * The container is a floating mtrls surface: filled with [AMTheme.Surface]
 * `.primary` (white in light, black in dark), clipped to [shape], with the
 * dark-mode-only card hairline for edge definition on the near-black page.
 * Tapping anywhere lands focus via [onTap]; nested [trailing] affordances
 * consume their own taps.
 *
 * **Bottom indicator** (drawn when [showIndicator]): a full-width line, 1dp in the
 * [AMTheme.separator] hairline color when unfocused, thickening to 2dp in the live
 * `semantic.accent` when [isFocused]. Thickness snaps under Reduce Motion; the
 * color cross-fades.
 *
 * **No label.** The fields draw no label of their own — the Mtrls UI labels
 * inputs externally with a standing [AMFieldLabel] above the field. The
 * [placeholder] shows whenever [text] is empty.
 *
 * @param placeholder       Hint shown while [text] is empty.
 * @param text              Current field text — gates the placeholder.
 * @param isFocused         Whether the hosted field currently holds focus.
 * @param shape             Container silhouette (top-rounded, all-rounded, or pill).
 * @param interactionSource The tap-to-focus interaction source owned by the caller.
 * @param onTap             Requests focus on the hosted field.
 * @param modifier          Modifier applied to the container.
 * @param alwaysHairline    Draws the hairline outline in BOTH themes (the search
 *   pill, which has no indicator to delineate it on white surfaces); false uses
 *   the standard dark-mode-only card hairline.
 * @param showIndicator     Whether to draw the animated bottom indicator line.
 * @param fieldAlignTop     Top-aligns the field (multiline editor); otherwise centers.
 * @param minHeight         Minimum container height. Defaults to 56dp.
 * @param maxHeight         Maximum container height ([Dp.Unspecified] = unbounded).
 * @param fieldVerticalPadding Extra top/bottom breathing around a top-aligned
 *   field (the multiline editor); 0dp for the vertically-centered single-line fields.
 * @param leading           Optional leading affordance (search magnifier).
 * @param trailing          Optional trailing affordance (clear / reveal button).
 * @param field             The hosted `BasicTextField`, without its own decoration.
 */
@Composable
internal fun AMFilledField(
    placeholder: String,
    text: String,
    isFocused: Boolean,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    alwaysHairline: Boolean = false,
    showIndicator: Boolean = true,
    fieldAlignTop: Boolean = false,
    minHeight: Dp = FIELD_MIN_HEIGHT,
    maxHeight: Dp = Dp.Unspecified,
    fieldVerticalPadding: Dp = 0.dp,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    field: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    val fill = AMTheme.Surface.primary.resolve(isDark)
    val separatorColor = AMTheme.separator.resolve(isDark)
    val accentColor = tokens.semantic.accent.resolve(isDark)
    val mutedColor = AMTheme.Icon.muted.resolve(isDark)

    // Bottom indicator: 1dp separator when unfocused → 2dp accent when focused.
    val indicatorColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else separatorColor,
        animationSpec = tokens.motion.resolved(fieldIndicatorColorSpec, reduceMotion),
        label = "AMFieldIndicatorColor",
    )
    val indicatorThickness by animateDpAsState(
        targetValue = if (isFocused) FIELD_INDICATOR_FOCUSED else FIELD_INDICATOR_UNFOCUSED,
        animationSpec = tokens.motion.resolvedOrNull(fieldIndicatorThicknessSpec, reduceMotion) ?: snap(),
        label = "AMFieldIndicatorThickness",
    )

    val heightModifier = if (maxHeight != Dp.Unspecified) {
        Modifier.heightIn(min = minHeight, max = maxHeight)
    } else {
        Modifier.heightIn(min = minHeight)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(heightModifier)
            .clip(shape)
            .background(fill)
            .then(
                if (alwaysHairline) Modifier.amHairline(shape) else Modifier.amCardHairline(shape),
            )
            .then(
                if (showIndicator) {
                    Modifier.drawBehind {
                        val thicknessPx = indicatorThickness.toPx()
                        val y = size.height - thicknessPx / 2f
                        drawLine(
                            color = indicatorColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = thicknessPx,
                        )
                    }
                } else {
                    Modifier
                },
            )
            .amInputRowClickable(interactionSource = interactionSource, onTap = onTap)
            .padding(horizontal = AMTheme.Input.inset.dp),
        contentAlignment = if (fieldAlignTop) Alignment.TopStart else Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = if (fieldAlignTop) Alignment.Top else Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            leading?.invoke()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = fieldVerticalPadding),
            ) {
                if (text.isEmpty() && placeholder.isNotEmpty()) {
                    BasicText(
                        text = placeholder,
                        style = AMTheme.Type.body.toTextStyle(mutedColor),
                    )
                }
                field()
            }

            trailing?.invoke()
        }
    }
}

// ---------------------------------------------------------------------------
// amInputRowClickable — no-indication tap-to-focus wiring for the input row
// ---------------------------------------------------------------------------

/**
 * Wires a no-ripple, full-container tap-to-focus callback.
 *
 * Applied by the filled fields so a tap anywhere in the container — including the
 * inset padding outside the `BasicTextField` hit area — lands focus on the field.
 * Nested affordances (the trailing clear × / reveal button) are themselves
 * clickable, so their taps are consumed before this fires and never re-focus while
 * acting.
 *
 * Uses `indication = null` deliberately: a text field's tap-to-focus surface is
 * not a button affordance, so it must NOT show the `amRipple()` state layer that
 * AndroidMtrls buttons use.
 *
 * @param interactionSource The [MutableInteractionSource] owned by the caller.
 * @param onTap             Called when the container is tapped; the caller should
 *                          call `focusRequester.requestFocus()` here.
 */
internal fun Modifier.amInputRowClickable(
    interactionSource: MutableInteractionSource,
    onTap: () -> Unit,
): Modifier = this
    .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onTap,
    )
