package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// ---------------------------------------------------------------------------
// Geometry — the two square ± buttons
// ---------------------------------------------------------------------------

/** Edge length of each square ± button in dp. */
private val stepperButtonSize = 40.dp

/** Minimum width of the center readout — keeps the row stable as digits change. */
private val stepperReadoutMinWidth = 40.dp

/** Glyph size for the ± buttons, proportioned for the 40dp square. */
private val stepperGlyphSize = 18.dp

// ---------------------------------------------------------------------------
// Press-and-hold auto-repeat timing (mirrors an accelerating stepper repeat)
// ---------------------------------------------------------------------------

/** Delay before the first auto-repeat after the initial press, in ms. */
private const val STEPPER_INITIAL_DELAY_MS = 500L

/** Interval between auto-repeats before acceleration, in ms. */
private const val STEPPER_REPEAT_INTERVAL_MS = 100L

/** After this much total hold time the repeat interval accelerates, in ms. */
private const val STEPPER_ACCELERATE_AFTER_MS = 2000L

/** Accelerated repeat interval after a long hold, in ms. */
private const val STEPPER_FAST_INTERVAL_MS = 50L

// ---------------------------------------------------------------------------
// AMStepperButton — internal ± square button
// ---------------------------------------------------------------------------

/**
 * Internal square button used by [AMStepper] for the minus and plus affordances.
 *
 * A [stepperButtonSize] square, clipped to a small-radius (circular-arc)
 * [RoundedCornerShape] and filled with the house neutral grey
 * ([AMTheme.neutralFill]) — a squared-off tonal icon button. A dark-mode-only
 * hairline ([amCardHairline]) defines its edge against the near-black page.
 * Press feedback is the monochrome `amRipple()` state layer, bounded to the
 * clipped shape.
 *
 * **Press-and-hold auto-repeat**: a tap fires once; a hold fires once
 * immediately, waits [STEPPER_INITIAL_DELAY_MS], then repeats every
 * [STEPPER_REPEAT_INTERVAL_MS], accelerating to [STEPPER_FAST_INTERVAL_MS] after
 * [STEPPER_ACCELERATE_AFTER_MS] of holding. The timer and the up/cancel wait are
 * interleaved in a single pointer coroutine via [withTimeoutOrNull] so no repeat
 * job can leak. [rememberUpdatedState] keeps the loop calling the latest
 * [onClick] as `value` changes, and the whole gesture is keyed on [enabled] so
 * reaching a bound stops the repeat.
 *
 * When disabled the button keeps its fill at **full alpha** with its glyph
 * tinted [AMTheme.Icon.disabled] — a state tint, deliberately distinct from the
 * ripple press feedback — drops its gesture entirely, and marks itself
 * `disabled()` for TalkBack.
 *
 * @param icon               Glyph to render ([Icons.Default.Add] for the plus button). Pass `null`
 *                           to draw a 12×2dp rounded horizontal bar (the minus glyph), avoiding a
 *                           dependency on the extended-icons artifact.
 * @param enabled            Whether the button is interactive. Disabled buttons are not clickable.
 * @param contentDescription Accessibility label.
 * @param onClick            Invoked on tap and on each auto-repeat while held.
 */
@Composable
private fun AMStepperButton(
    icon: ImageVector?,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    // Keep the hold loop calling the latest onClick — the passed closure captures
    // the current `value`, so a stale capture would freeze the repeat's target.
    val currentOnClick by rememberUpdatedState(onClick)

    // Square, mildly-rounded (circular-arc) tonal button — the house neutral fill.
    val buttonShape = RoundedCornerShape(tokens.radius.sm)
    val buttonFill = AMTheme.neutralFill()

    // Disabled: tint glyph with Icon.disabled (a state tint) — the button keeps
    // its fill at full alpha, visually distinct from the ripple press feedback.
    // Enabled: tint with Text.primary at full alpha.
    val iconTint = if (!enabled) {
        AMTheme.Icon.disabled.resolve(isDark)
    } else {
        AMTheme.Text.primary.resolve(isDark)
    }

    Box(
        modifier = Modifier
            .size(stepperButtonSize)
            .clip(buttonShape)
            .background(buttonFill)
            .amCardHairline(buttonShape)
            // Manual ripple: the hold loop emits Press/Release into the source so
            // the amRipple() sweep still plays (clickable is replaced by the
            // pointerInput hold loop). Bounded to the clipped button shape.
            .indication(interactionSource, amRipple())
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
                if (enabled) {
                    // Accessibility activation fires a single step (services can't
                    // press-and-hold); touch users get the repeat via pointerInput.
                    onClick { currentOnClick(); true }
                } else {
                    disabled()
                }
            }
            // Press-and-hold auto-repeat loop. Keyed on `enabled` so reaching a
            // bound cancels an in-flight hold.
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val press = PressInteraction.Press(down.position)
                        scope.launch { interactionSource.emit(press) }
                        try {
                            // Fire immediately, then interleave the accelerating
                            // timer with the up/cancel wait in this one coroutine.
                            currentOnClick()
                            var held = 0L
                            var interval = STEPPER_INITIAL_DELAY_MS
                            while (true) {
                                val ended = withTimeoutOrNull(interval) {
                                    waitForUpOrCancellation()
                                    true
                                } ?: false
                                if (ended) break
                                currentOnClick()
                                held += interval
                                interval = if (held >= STEPPER_ACCELERATE_AFTER_MS) {
                                    STEPPER_FAST_INTERVAL_MS
                                } else {
                                    STEPPER_REPEAT_INTERVAL_MS
                                }
                            }
                        } finally {
                            // Always release the ripple — even if reaching a bound
                            // re-keys pointerInput and cancels this coroutine while
                            // the finger is still down. `scope` (rememberCoroutineScope)
                            // outlives the cancelled gesture coroutine.
                            scope.launch { interactionSource.emit(PressInteraction.Release(press)) }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                imageVector = icon,
                contentDescription = null,   // accessibility on outer Box
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(stepperGlyphSize),
            )
        } else {
            // Minus glyph: 12dp × 2dp rounded horizontal bar, no extra dependency.
            Canvas(modifier = Modifier.size(stepperGlyphSize)) {
                val barWidth = 12.dp.toPx()
                val barHeight = 2.dp.toPx()
                val cornerRadius = barHeight / 2f
                drawRoundRect(
                    color = iconTint,
                    topLeft = Offset(
                        x = (size.width - barWidth) / 2f,
                        y = (size.height - barHeight) / 2f,
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMStepper — composable slot overload
// ---------------------------------------------------------------------------

/**
 * Flat row stepper widget: leading label slot, trailing `−` button / value / `+`
 * button quantity cluster.
 *
 * A flat row widget — the caller's card or section provides the row surface.
 * Accepts [Int] values only. For fractional values use `AMSlider`.
 *
 * The ± affordances are **two separate square tonal buttons** ([AMStepperButton],
 * [stepperButtonSize] each) — a minus button and a plus button flanking the live
 * value — filled with [AMTheme.neutralFill] and rounded on the small
 * (circular-arc) radius, with a dark-mode-only hairline. There is no joined
 * capsule and no interior divider. Press feedback is the monochrome `amRipple()`
 * state layer, bounded to each button. Holding a ± button auto-repeats with
 * acceleration. The minus button is disabled when `value == valueRange.first`;
 * the plus button when `value == valueRange.last` — a disabled button keeps its
 * fill at full alpha with its glyph tinted [AMTheme.Icon.disabled], stops
 * responding to taps, and marks itself `disabled()` for TalkBack.
 *
 * The value readout sits **between** the two buttons in `AMTheme.Type.body`
 * (16sp) with [FontFamily.Monospace] and `fontFeatureSettings = "tnum"`
 * (monospaced digits), centered inside a `widthIn(min = 40.dp)` cell so the row
 * does not shift as the digit count changes.
 *
 * **Visual only** — this composable only fires [onValueChange]; range clamping
 * and state hosting are the caller's responsibility.
 *
 * Usage:
 * ```kotlin
 * var qty by rememberSaveable { mutableStateOf(1) }
 * AMStepper(
 *     value = qty,
 *     onValueChange = { qty = it },
 *     valueRange = 1..20,
 *     label = "Quantity",
 * )
 * ```
 *
 * @param value         The current integer value. Displayed in the center readout.
 * @param onValueChange Callback with the new clamped value when the user taps ± .
 * @param valueRange    The closed integer range the value is confined to.
 * @param step          Amount to increment/decrement per tap. Defaults to `1`.
 * @param modifier      Modifier applied to the outer [Row].
 * @param label         Composable slot rendered as the leading label (headline font,
 *                      primary foreground, max width, leading alignment).
 */
@Composable
fun AMStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int = 1,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    Row(
        modifier = modifier
            .padding(horizontal = tokens.spacing.lg)
            .padding(vertical = tokens.spacing.md)
            .heightIn(min = AMTheme.rowMinHeight.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // Leading label — takes all available width; caller is responsible for
        // styling (same as iOS @ViewBuilder label slot in IMStepper).
        Box(modifier = Modifier.weight(1f)) {
            label()
        }

        // Trailing: two square tonal buttons flanking the visible value.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            AMStepperButton(
                icon = null,   // drawn as 12×2dp rounded bar — no extended-icons dep needed
                enabled = value > valueRange.first,
                contentDescription = "Decrease",
                onClick = { onValueChange((value - step).coerceIn(valueRange)) },
            )

            BasicText(
                text = value.toString(),
                style = AMTheme.Type.body
                    .toTextStyle(AMTheme.Text.primary.resolve(isDark))
                    .copy(
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,       // monospaced digits — same as iOS .monospacedDigit()
                        fontFeatureSettings = "tnum",
                    ),
                modifier = Modifier.widthIn(min = stepperReadoutMinWidth),
            )

            AMStepperButton(
                icon = Icons.Default.Add,
                enabled = value < valueRange.last,
                contentDescription = "Increase",
                onClick = { onValueChange((value + step).coerceIn(valueRange)) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AMStepper — String convenience overload
// ---------------------------------------------------------------------------

/**
 * Flat row stepper widget with a plain-string leading label.
 *
 * Convenience overload preserving the original simple-call API. Renders
 * [label] as a headline-weight [BasicText] in [AMTheme.Text.primary].
 *
 * @param value         The current integer value.
 * @param onValueChange Callback with the new clamped value when the user taps ± .
 * @param valueRange    The closed integer range the value is confined to.
 * @param step          Amount to increment/decrement per tap. Defaults to `1`.
 * @param label         Plain string rendered as a headline-weight leading label.
 * @param modifier      Modifier applied to the outer [Row].
 */
@Composable
fun AMStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    step: Int = 1,
    label: String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current

    AMStepper(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        step = step,
        modifier = modifier,
    ) {
        BasicText(
            text = label,
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun StepperPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Flat demo — two steppers on a bare Surface.primary strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AMTheme.Surface.primary.resolve(isDark)),
        ) {
            Column {
                AMStepper(
                    value = 2,
                    onValueChange = {},
                    valueRange = 1..12,
                    label = "Guests",
                )
                AMStepper(
                    value = 20,
                    onValueChange = {},
                    valueRange = 0..100,
                    step = 5,
                    label = "Copies",
                )
            }
        }

        // Card demo — same steppers wrapped in AMCard (shows elevated treatment)
        AMCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                AMStepper(
                    value = 1,
                    onValueChange = {},
                    valueRange = 1..20,
                    label = "Quantity",
                )
                AMStepper(
                    value = 1,
                    onValueChange = {},
                    valueRange = 1..1,   // at-min and at-max: both buttons dimmed
                    label = "At bounds",
                )
            }
        }
    }
}

@Preview(
    name = "AMStepper · Light",
    group = "AMStepper",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMStepperPreviewLight() {
    AMThemeRoot {
        StepperPreviewContent()
    }
}

@Preview(
    name = "AMStepper · Dark",
    group = "AMStepper",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMStepperPreviewDark() {
    AMThemeRoot {
        StepperPreviewContent()
    }
}
