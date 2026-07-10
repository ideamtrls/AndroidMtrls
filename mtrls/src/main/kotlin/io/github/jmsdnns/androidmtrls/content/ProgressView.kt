package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.listRowInsets

// ---------------------------------------------------------------------------
// Spinner geometry + motion constants (Material indeterminate feel — tuned by eye)
// ---------------------------------------------------------------------------

/**
 * Diameter of the [AMSpinner] arc track, in dp.
 *
 * Approximates the Material circular indicator footprint (~28dp) when rendered
 * inside an `AMCard` block.
 */
private val SPINNER_DIAMETER_DP = 28.dp

/**
 * Stroke width of the spinner arc, in dp (round-capped).
 */
private val SPINNER_STROKE_DP = 3.dp

/**
 * Shortest arc sweep, in degrees — the collapsed end of the breathe.
 */
private const val SPINNER_MIN_SWEEP_DEG = 30f

/**
 * Longest arc sweep, in degrees — the expanded peak of the breathe.
 */
private const val SPINNER_MAX_SWEEP_DEG = 270f

/**
 * One full container revolution, in milliseconds (linear, uniform speed).
 */
private const val SPINNER_ROTATION_MS = 1568

/**
 * One grow-then-shrink breathe of the arc sweep, in milliseconds
 * ([FastOutSlowInEasing] on each leg).
 */
private const val SPINNER_BREATHE_MS = 1400

/**
 * Degrees the arc's start angle (tail) advances per breathe cycle — one full
 * revolution, so the tail races a lap to catch the head as the sweep collapses.
 *
 * Must stay a multiple of 360°: the advance snaps back to 0 at each cycle seam
 * ([RepeatMode.Restart]), and only a full revolution is continuous mod 360
 * there — any other value teleports the arc at the seam, exactly when the
 * sweep is at its 30° minimum and the jump is most visible.
 */
private const val SPINNER_START_ADVANCE_DEG = 360f

// ---------------------------------------------------------------------------
// AMSpinner — indeterminate centered spinner
// ---------------------------------------------------------------------------

/**
 * Indeterminate activity spinner, ported from `IMSpinnerProgressViewStyle`
 * (SwiftUI `ProgressViewStyle` conformance).
 *
 * In iOS the style was injected via `.progressViewStyle(.imSpinner)`. In Compose
 * there is no style-injection mechanism, so this is a standalone composable —
 * the canonical call site is `AMSpinner()` rather than
 * `ProgressView().progressViewStyle(.imSpinner)`.
 *
 * **Layout modes:**
 * - `compact = false` (default): card-framed block via [AMCard], filling the
 *   available width. Centered column containing the 28dp spinning arc, with an
 *   optional [label] below in `caption / secondary` style. Matches the iOS
 *   full-screen loader block.
 * - `compact = true`: inline, no card wrapper — same arc and optional label
 *   Column without any surface chrome. Use when embedding the spinner inside an
 *   existing card or alongside other content.
 *
 * Animation: a single [rememberInfiniteTransition] drives the recognizable
 * Material indeterminate "breathe" — a linear 0→360° container rotation, a
 * [FastOutSlowInEasing] sweep pulsing 30°→270°→30°, and a per-cycle start-angle
 * advance so the arc's head chases its tail. These are tuned for feel, not
 * sourced iOS values; see [AMSpinnerArc]. (Static `@Preview` frames only show a
 * single instant — verify the motion in the demo.)
 *
 * The arc color adapts to dark/light via [AMTheme.Text.secondary]; it draws on a
 * transparent track (no track ring).
 *
 * @param label    Optional descriptive text shown below the arc. Defaults to null (no label).
 * @param compact  When true, suppresses the [AMCard] wrapper. Defaults to false.
 */
@Composable
fun AMSpinner(
    label: String? = null,
    compact: Boolean = false,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    val spinnerContent: @Composable () -> Unit = {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.xl, vertical = tokens.spacing.lg),
        ) {
            AMSpinnerArc(isDark = isDark)
            if (label != null) {
                Spacer(modifier = Modifier.height(tokens.spacing.md))
                BasicText(
                    text = label,
                    style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                )
            }
        }
    }

    if (compact) {
        spinnerContent()
    } else {
        AMCard {
            spinnerContent()
        }
    }
}

// ---------------------------------------------------------------------------
// AMSpinnerArc — private spinning arc canvas primitive
// ---------------------------------------------------------------------------

/**
 * The animated arc for [AMSpinner].
 *
 * Draws a single stroked arc on a transparent track (no visible track ring). A
 * single [rememberInfiniteTransition] drives three values that together read as
 * the Material indeterminate spinner (all stop when the composable leaves the
 * tree):
 * - **rotation** — 0→360° linear over [SPINNER_ROTATION_MS], the overall spin.
 * - **sweep** — [SPINNER_MIN_SWEEP_DEG] → [SPINNER_MAX_SWEEP_DEG] →
 *   [SPINNER_MIN_SWEEP_DEG] over [SPINNER_BREATHE_MS] with [FastOutSlowInEasing]
 *   on each leg, the grow-then-collapse breathe.
 * - **start advance** — the arc's start (tail) holds through the grow leg then
 *   advances one full revolution ([SPINNER_START_ADVANCE_DEG] = 360°) through
 *   the shrink leg — the tail races a lap to catch the head as the arc
 *   collapses. A full revolution keeps the arc continuous mod 360 across the
 *   cycle seam, where the advance snaps back to 0.
 *
 * @param isDark Current dark-mode state, for color resolution.
 */
@Composable
private fun AMSpinnerArc(isDark: Boolean) {
    val arcColor = AMTheme.Text.secondary.resolve(isDark)

    val transition = rememberInfiniteTransition(label = "AMSpinner")

    // Overall spin — continuous and linear.
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPINNER_ROTATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "AMSpinner rotation",
    )

    // Sweep breathe — grows to the peak, then collapses back.
    val sweep by transition.animateFloat(
        initialValue = SPINNER_MIN_SWEEP_DEG,
        targetValue = SPINNER_MIN_SWEEP_DEG,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = SPINNER_BREATHE_MS
                SPINNER_MIN_SWEEP_DEG at 0 using FastOutSlowInEasing
                SPINNER_MAX_SWEEP_DEG at SPINNER_BREATHE_MS / 2 using FastOutSlowInEasing
                SPINNER_MIN_SWEEP_DEG at SPINNER_BREATHE_MS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "AMSpinner sweep",
    )

    // Tail advance — holds through the grow leg, then races forward through the
    // shrink leg so the head chases the tail.
    val startAdvance by transition.animateFloat(
        initialValue = 0f,
        targetValue = SPINNER_START_ADVANCE_DEG,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = SPINNER_BREATHE_MS
                0f at SPINNER_BREATHE_MS / 2 using FastOutSlowInEasing
                SPINNER_START_ADVANCE_DEG at SPINNER_BREATHE_MS
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "AMSpinner tail advance",
    )

    Canvas(modifier = Modifier.size(SPINNER_DIAMETER_DP)) {
        val strokePx = SPINNER_STROKE_DP.toPx()
        val inset = strokePx / 2f
        drawArc(
            color = arcColor,
            startAngle = rotation + startAdvance,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokePx, size.height - strokePx),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
    }
}

// ---------------------------------------------------------------------------
// AMProgressBar — determinate linear progress bar
// ---------------------------------------------------------------------------

/**
 * Determinate linear progress bar, ported from `IMBarProgressViewStyle`
 * (SwiftUI `ProgressViewStyle` conformance).
 *
 * In iOS the style was injected via `.progressViewStyle(.imBar)`. In Compose
 * there is no style-injection mechanism, so this is a standalone composable —
 * the canonical call site is `AMProgressBar(progress = 0.6f)`.
 *
 * **Layout:** full-bleed [AMTheme.Surface.primary] row (not card-framed),
 * inset by [AMTokens.listRowInsets] (horizontal 14dp, vertical 16dp). Matches
 * `IMBarProgressViewStyle` body layout which renders inside a `.listRowInsets`
 * strip.
 *
 * **Track + fill:**
 * - Track height: **6dp** (iOS `IMBarProgressViewStyle.barHeight` — slimmer than
 *   any spacing token so it reads as an indicator, not a layout band).
 * - Track color: [AMTheme.separator] — the same quiet monochrome channel the
 *   divider, skeleton, and timeline rail use (iOS fills the track with
 *   `IMTheme.separator`).
 * - Fill color: the accent token (`tokens.semantic.accent`, monochrome by
 *   default — black light / white dark, overridable per-app; the port-wide
 *   translation of iOS `Color.accentColor`).
 * - Corner radius: 3dp (half the track height — fully rounded capsule).
 *
 * [progress] is clamped to `0f..1f`. iOS uses `configuration.fractionCompleted ?? 0`
 * which maps `nil` to 0 — Kotlin's non-nullable `Float` plus `.coerceIn` is the
 * direct equivalent.
 *
 * @param progress   Fraction complete: `0f` = empty, `1f` = full. Clamped to `0f..1f`.
 * @param label      Optional leading label, [AMTheme.Type.title]. Defaults to null.
 * @param valueLabel Optional trailing value label (e.g. "45%" or "9/20") in
 *                   [AMTheme.Type.secondary]. Defaults to null.
 */
@Composable
fun AMProgressBar(
    progress: Float,
    label: String? = null,
    valueLabel: String? = null,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    val clampedProgress = progress.coerceIn(0f, 1f)
    val trackColor = AMTheme.separator.resolve(isDark)
    val fillColor = tokens.semantic.accent.resolve(isDark)
    val surfaceColor = AMTheme.Surface.primary.resolve(isDark)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .padding(tokens.listRowInsets),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        // ── Header row: label (leading) + valueLabel (trailing) ───────────────
        if (label != null || valueLabel != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (label != null) {
                    BasicText(
                        text = label,
                        style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (valueLabel != null) {
                    BasicText(
                        text = valueLabel,
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
            }
        }

        // ── Track + fill ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
        ) {
            // Track (background)
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = trackColor,
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
            // Fill (foreground)
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = fillColor,
                    size = Size(size.width * clampedProgress, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ProgressViewPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.lg),
    ) {
        // Spinner — card-framed with label
        AMSpinner(label = "Loading…")

        // Spinner — compact (no card)
        AMSpinner(compact = true)

        // Progress bar — labeled, 60% complete
        AMProgressBar(
            progress = 0.6f,
            label = "Uploading",
            valueLabel = "60%",
        )

        // Progress bar — no labels, 30% complete
        AMProgressBar(progress = 0.3f)

        // Progress bar — full (edge case: 1.0)
        AMProgressBar(progress = 1.0f, label = "Complete")
    }
}

@Preview(
    name = "AMSpinner + AMProgressBar · Light",
    group = "AMProgressView",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMProgressViewPreviewLight() {
    AMThemeRoot {
        ProgressViewPreviewContent()
    }
}

@Preview(
    name = "AMSpinner + AMProgressBar · Dark",
    group = "AMProgressView",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMProgressViewPreviewDark() {
    AMThemeRoot {
        ProgressViewPreviewContent()
    }
}
