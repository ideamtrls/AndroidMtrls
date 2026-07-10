package io.github.jmsdnns.androidmtrls.surfaces.frames

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMSurfaceStyle
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMSurfaceStyle
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// AMCard — surface-card composable with nesting demotion contract
// ---------------------------------------------------------------------------

/**
 * Wraps content in the standard AndroidMtrls floating-surface card.
 *
 * Honors the inherited [LocalAMSurfaceStyle] environment value:
 *
 * - **Elevated** (default at the root of a screen): applies a
 *   [AMTheme.Surface.primary] fill, `tokens.radius.lg` corner radius, a
 *   two-layer drop shadow via `amElevation(AMElevation.Low)`, and (dark mode
 *   only) a [AMTheme.selectionRing] hairline via `amCardHairline`. Content is
 *   inset by [padding] when provided.
 * - **Flat** (inherited when nested inside another card): renders nothing —
 *   no fill, no rounded clip, no shadow, no hairline, and [padding] is
 *   skipped. Content rides directly on the parent surface; only the parent's
 *   interior padding applies.
 *
 * The card **always** provides [AMSurfaceStyle.Flat] into [LocalAMSurfaceStyle]
 * for its content slot, regardless of its own current style. This is the
 * nesting contract that prevents double-padding and shadow-on-shadow stacking
 * when card surfaces are composed inside each other.
 *
 * iOS equivalent: `View.imCard(padding:radius:elevation:)` in
 * `Sources/Surfaces/Frames/Card.swift` — translated to a `@Composable`
 * function because Compose `Modifier` cannot call `CompositionLocalProvider`
 * (see foundations spec §5.2).
 *
 * Usage:
 * ```kotlin
 * AMCard(padding = AMPadding.block) {
 *     BasicText("Card content")
 * }
 * ```
 *
 * @param modifier   Modifier applied to the card's root layout node.
 * @param padding    Interior padding inset applied when the card renders as
 *   [AMSurfaceStyle.Elevated]. Pass `null` (or omit) if the caller is already
 *   managing content padding. Typical values: [AMPadding.block] (16dp all
 *   sides) or [AMPadding.row] (16dp horizontal / 12dp vertical).
 * @param radius     Corner radius override. Defaults to
 *   `LocalAMTokens.current.radius.lg` (16dp). Pass an explicit value to
 *   deviate from the token radius for a single call site.
 * @param elevation  Shadow depth for the Elevated render. Defaults to
 *   [AMElevation.Low] — the standard card lift. Pass [AMElevation.None] to
 *   suppress shadows while keeping the fill and hairline.
 * @param content    The composable content to host. Receives
 *   [AMSurfaceStyle.Flat] via [LocalAMSurfaceStyle].
 */
@Composable
fun AMCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues? = null,
    radius: Dp? = null,
    elevation: AMElevation = AMElevation.Low,
    content: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val surfaceStyle = LocalAMSurfaceStyle.current

    // Card shape: large radius from the current token set (overridable).
    val shape = amSquircle(radius ?: tokens.radius.lg)

    // The content slot always receives Flat so nested cards demote in turn.
    // CompositionLocalProvider here is the reason AMCard must be a @Composable
    // function rather than a Modifier extension.
    CompositionLocalProvider(LocalAMSurfaceStyle provides AMSurfaceStyle.Flat) {
        when (surfaceStyle) {
            AMSurfaceStyle.Elevated -> {
                // Build up the elevated card modifier chain, drawn outermost → innermost:
                //   1. amElevation — two-layer drop shadow, drawn BEHIND the fill so it
                //      bleeds onto the page substrate and stays visible.
                //   2. background — Surface.primary fill in the squircle shape.
                //   3. amCardHairline — dark-mode-only hairline ring, drawn on top of
                //      the fill inside bounds.
                //   4. clip — clips the content slot to the squircle so full-bleed
                //      children (AMDisclosure's rect-bounded ripple, AMToggleRow /
                //      AMListRow fills) honor the rounded corners instead of painting
                //      square corners over them.
                // clip is LAST so it bounds only the children: the shadow, fill, and
                // hairline drawn earlier in the chain are untouched (the shadow still
                // bleeds onto the page). Matches the iOS layering fill → hairline
                // overlay → .imElevation shadow, plus a content clip.
                val cardModifier = modifier
                    .amElevation(level = elevation, shape = shape)
                    .background(
                        color = AMTheme.Surface.primary.resolve(isDark),
                        shape = shape,
                    )
                    .amCardHairline(shape)
                    .clip(shape)

                if (padding != null) {
                    Box(modifier = cardModifier.padding(padding)) {
                        content()
                    }
                } else {
                    Box(modifier = cardModifier) {
                        content()
                    }
                }
            }

            AMSurfaceStyle.Flat -> {
                // Flat: no fill, no shape, no shadow, no hairline, no padding.
                // The modifier is passed through so callers can still attach
                // layout constraints (fillMaxWidth, etc.) without visual chrome.
                Box(modifier = modifier) {
                    content()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark, each showing a card on Surface.page plus a nested
// card demonstrating the Flat demotion (no double shadow).
// ---------------------------------------------------------------------------

@Composable
private fun CardPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Standard elevated card with block padding
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                BasicText(
                    text = "Card title",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                BasicText(
                    text = "Supporting body copy inside an AMCard(padding = AMPadding.block) block.",
                    style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                )
            }
        }

        // Nested card — inner AMCard auto-demotes to Flat (no double shadow)
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                BasicText(
                    text = "Outer card",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )

                // This nested AMCard reads Flat from LocalAMSurfaceStyle and
                // renders nothing — no fill, no shadow, no hairline.
                AMCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = null,
                ) {
                    BasicText(
                        text = "Nested primitive — Flat demotion, no double shadow",
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
            }
        }
    }
}

@Preview(
    name = "AMCard · Light",
    group = "AMCard",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMCardPreviewLight() {
    AMThemeRoot {
        CardPreviewContent()
    }
}

@Preview(
    name = "AMCard · Dark",
    group = "AMCard",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMCardPreviewDark() {
    AMThemeRoot {
        CardPreviewContent()
    }
}
