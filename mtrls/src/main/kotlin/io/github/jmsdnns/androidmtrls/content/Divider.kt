package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amHairlineWidth

// ---------------------------------------------------------------------------
// AMDivider
// ---------------------------------------------------------------------------

/**
 * A horizontal hairline divider, port of iOS `IMDivider`.
 *
 * Renders a full-width 0.5dp line in [AMTheme.separator] color, optionally
 * indented by [inset] from the leading edge. The default of `0.dp` produces a
 * full-bleed edge-to-edge rule; pass `AMTheme.Input.inset.dp` (14dp) to align
 * the leading end with [AMListRow] title text.
 *
 * **Does not shadow `HorizontalDivider` from `androidx.compose.material3`.**
 *
 * Height is [AMTheme.hairlineWidth] (0.5dp) clamped to at least 1 physical
 * pixel per density (the same `max(0.5dp, 1px)` rule as [Modifier.amHairline]),
 * so the rule never vanishes on low-density screens.
 * Color is `AMTheme.separator`: `rgba(60,60,67,0.29)` in light mode and
 * `rgba(84,84,88,0.65)` in dark mode, matching `UIColor.separator`.
 *
 * Token cross-references: [AMTheme.separator], [AMTheme.hairlineWidth],
 * [AMTheme.Input.inset].
 *
 * @param inset Leading padding in dp. Default `0.dp` = full-width. Pass
 *              `AMTheme.Input.inset.dp` to align with list row text.
 */
@Composable
fun AMDivider(
    inset: Dp = 0.dp,
) {
    val isDark = LocalAMIsDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(amHairlineWidth())
            .background(AMTheme.separator.resolve(isDark))
    )
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMDivider · Light",
    group = "AMDivider",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMDividerPreviewLight() {
    AMThemeRoot {
        AMDividerPreviewContent()
    }
}

@Preview(
    name = "AMDivider · Dark",
    group = "AMDivider",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMDividerPreviewDark() {
    AMThemeRoot {
        AMDividerPreviewContent()
    }
}

@Composable
private fun AMDividerPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AMTheme.Spacing.lg.dp),
    ) {
        // Full-width (default inset = 0.dp)
        AMDivider()
        // Inset aligned with AMListRow text (14dp)
        Box(modifier = Modifier.height(AMTheme.Spacing.lg.dp))
        AMDivider(inset = AMTheme.Input.inset.dp)
    }
}
