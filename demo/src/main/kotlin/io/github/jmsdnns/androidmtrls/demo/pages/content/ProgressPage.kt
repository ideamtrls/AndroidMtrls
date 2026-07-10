package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMProgressBar
import io.github.jmsdnns.androidmtrls.content.AMSpinner
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Progress catalog entry
// ---------------------------------------------------------------------------

val progressEntry: ComponentEntry = ComponentEntry(
    id = "content-progress",
    section = ComponentSection.Content,
    title = "Progress",
    subtitle = "Spinner & bar styles",
    intro = "Two progress styles: AMSpinner (a card-framed centered spinner) and " +
        "AMProgressBar (a flat linear progress bar with a value readout).",
    icon = Icons.Default.Timelapse,
    content = { ProgressPage() },
)

// ---------------------------------------------------------------------------
// ProgressPage — demo composable
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMSpinner] and [AMProgressBar].
 *
 * Two groups matching iOS `ProgressViewStylesPage`:
 * - **Spinner** — full (with label) + compact side-by-side in one HStack.
 * - **Bar** — animated: loops 0→1 over 3 seconds via rememberInfiniteTransition.
 */
@Composable
fun ProgressPage() {
    // ── Spinner ───────────────────────────────────────────────────────────────
    catalogGroup(title = "Spinner") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            // Full spinner with label — takes equal space
            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                AMSpinner(label = "Loading")
            }
            // Compact spinner — takes equal space
            androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
                AMSpinner(compact = true)
            }
        }
    }

    // ── Bar ───────────────────────────────────────────────────────────────────
    catalogGroup(title = "Bar") {
        AnimatedProgressBar()
    }
}

// ---------------------------------------------------------------------------
// AnimatedProgressBar — loops 0→1 over 3 s
// ---------------------------------------------------------------------------

/**
 * Recreates the iOS `AnimatedUploadBar` — drives progress from 0f to 1f
 * linearly over 3 000 ms via [rememberInfiniteTransition], then restarts.
 */
@Composable
private fun AnimatedProgressBar() {
    val transition = rememberInfiniteTransition(label = "progress-bar")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
        ),
        label = "progress",
    )

    AMProgressBar(
        progress = progress,
        label = "Uploading",
        valueLabel = "${(progress * 100).toInt()}%",
    )
}
