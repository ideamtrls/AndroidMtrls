package io.github.jmsdnns.androidmtrls.surfaces.frames

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.blockInsets

// ---------------------------------------------------------------------------
// AMSectionStyle
// ---------------------------------------------------------------------------

/**
 * Drives the content-block rendering treatment inside [AMSection].
 *
 * - [Card] — content is wrapped in an [AMCard] with `tokens.blockInsets` (16dp
 *   all sides), rounded corners, elevation, and (dark mode only) a hairline outline.
 *   Equivalent to iOS `IMSection.Style.card`.
 * - [Flat] — content sits on a full-bleed [AMTheme.Surface.primary] fill with no
 *   rounded corners and no elevation shadow. Matches the inline reading-block
 *   treatment `IMListRow` uses. Equivalent to iOS `IMSection.Style.flat`.
 */
@Immutable
enum class AMSectionStyle {
    /** Content wrapped in a floating card surface (default). */
    Card,

    /** Content on a full-bleed primary-surface fill; no corners, no elevation. */
    Flat,
}

// ---------------------------------------------------------------------------
// AMSection
// ---------------------------------------------------------------------------

/**
 * Titled group surface — the Android port of `IMSection<Content: View>`.
 *
 * Bundles the common pattern of an [AMSectionHeader] above a surface-backed
 * content block, with an optional footer caption below:
 *
 * ```
 * Column(spacing = tokens.spacing.md) {
 *     AMSectionHeader(title)          // inset by tokens.spacing.lg when inset=true
 *     contentBlock                    // card or flat, see AMSectionStyle
 *     Text(footer)                    // footnote / Text.secondary when non-null
 * }
 * ```
 *
 * **Style = Card (default):** content is placed inside an [AMCard] padded with
 * `tokens.blockInsets` (16dp all sides). The card is inset from the screen edges
 * by `tokens.spacing.lg` (16dp) when [inset] is `true`.
 *
 * **Style = Flat:** content is placed on a full-bleed [AMTheme.Surface.primary]
 * fill. No rounded corners, no elevation. The fill always extends to the screen
 * edges regardless of [inset].
 *
 * **inset = false:** removes the horizontal padding from the header, footer, and
 * (Card style) the card, enabling full-bleed hosting contexts.
 *
 * iOS equivalent: `IMSection` (`Sources/Surfaces/Frames/Section.swift`).
 *
 * Usage:
 * ```kotlin
 * // Card style (default)
 * AMSection("Account", footer = "Changes take effect at next sign-in.") {
 *     BasicText("Display name")
 * }
 *
 * // Flat style — full-bleed reading block
 * AMSection("About", style = AMSectionStyle.Flat) {
 *     BasicText("Body copy riding the primary surface.")
 * }
 * ```
 *
 * @param title    The section header text. Rendered via [AMSectionHeader] — all-caps,
 *   12sp / Medium / 0.5sp letter-spacing / [AMTheme.Text.primary].
 * @param modifier Modifier applied to the outer [Column] root.
 * @param footer   Optional footnote caption rendered below the content block with
 *   [AMTheme.Type.caption] in [AMTheme.Text.secondary]. Pass `null` to omit.
 * @param style    Content-block treatment — [AMSectionStyle.Card] (default) or
 *   [AMSectionStyle.Flat]. See [AMSectionStyle] for details.
 * @param inset    When `true` (default), the header, footer, and Card-style card
 *   are padded `tokens.spacing.lg` (16dp) horizontally, matching the standard
 *   screen-edge inset. Pass `false` for full-bleed hosting contexts.
 * @param content  The composable content to host inside the section block.
 */
@Composable
fun AMSection(
    title: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    style: AMSectionStyle = AMSectionStyle.Card,
    inset: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    // Read overridable spacing/insets from LocalAMTokens (like AMCard reads
    // tokens.radius) so an AMTokensProvider transform re-tunes this section's
    // rhythm — mirroring the iOS frame reading tokens.spacing / tokens.blockInsets.
    // Values are identical to AMTheme.Spacing.* at the defaults.
    val tokens = LocalAMTokens.current
    val horizontalInset = if (inset) tokens.spacing.lg else 0.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // ── Section header ────────────────────────────────────────────────────
        // Wrap in a Box so horizontal inset padding is applied outside
        // AMSectionHeader (which has no modifier parameter).
        Box(modifier = Modifier.padding(horizontal = horizontalInset)) {
            AMSectionHeader(title = title)
        }

        // ── Content block ─────────────────────────────────────────────────────
        // Both branches stretch the content slot to full width, leading-aligned,
        // before the card/fill — mirroring iOS `.frame(maxWidth: .infinity,
        // alignment: .leading)` (Section.swift:72-73,79-80) so content with
        // trailing elements, internal Spacers, or a background fills the block.
        when (style) {
            AMSectionStyle.Card -> {
                // Card with block padding, inset from screen edges.
                AMCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset),
                    padding = tokens.blockInsets,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        content()
                    }
                }
            }

            AMSectionStyle.Flat -> {
                // Full-bleed surface fill — always extends to screen edges;
                // the spec does not inset the flat block even when inset=true.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AMTheme.Surface.primary.resolve(isDark))
                        .padding(all = tokens.spacing.lg),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        content()
                    }
                }
            }
        }

        // ── Footer caption ────────────────────────────────────────────────────
        if (footer != null) {
            BasicText(
                text = footer,
                // .footnote in iOS — the caption scale entry is the Compose analog.
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalInset),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Composable
private fun SectionPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(vertical = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Card style (default) with footer — mirrors iOS preview
        AMSection(
            title = "Account",
            footer = "Changes take effect at next sign-in.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                BasicText(
                    text = "Display name",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                BasicText(
                    text = "Jane Appleseed",
                    style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                )
            }
        }

        // Flat style — full-bleed reading block, mirrors iOS preview
        AMSection(
            title = "About",
            style = AMSectionStyle.Flat,
        ) {
            BasicText(
                text = "AMSection bundles AMSectionHeader + card + optional footer into one call.",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

@Preview(
    name = "AMSection · Light",
    group = "AMSection",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7, // AMTheme.Surface.page light
)
@Composable
private fun AMSectionPreviewLight() {
    AMThemeRoot {
        SectionPreviewContent()
    }
}

@Preview(
    name = "AMSection · Dark",
    group = "AMSection",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D, // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSectionPreviewDark() {
    AMThemeRoot {
        SectionPreviewContent()
    }
}
