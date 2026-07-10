package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMSectionHeader
// ---------------------------------------------------------------------------

/**
 * A floating uppercase section header, port of iOS `IMSectionHeader`.
 *
 * Carries the two-layer text-legibility shadow at the GLYPH level (see
 * [AMTextLegibilityLabel]) so the label remains readable over blur substrates
 * or busy backgrounds — matching iOS, where `.imElevation(.text)` shadows the
 * rendered glyph alpha mask, never the label's bounding rectangle.
 *
 * Two overloads follow the composable-slot + String-convenience pattern:
 *
 * - **String overload** ([AMSectionHeader] `(title: String)`) — most common;
 *   renders [title] uppercased at 12sp Medium with
 *   0.5sp letter-spacing, [AMTheme.Text.primary] color, and the glyph-level
 *   legibility shadow.
 * - **Slot overload** ([AMSectionHeader] `(content: @Composable () -> Unit)`) —
 *   caller supplies arbitrary composable content and its own shadow treatment.
 *
 * **Typography** (the section-header uppercase treatment):
 * - Font size: 12sp
 * - Weight: [FontWeight.Medium]
 * - Letter spacing: 0.5sp (iOS `tracking = 0.5`)
 * - Text transform: [String.uppercase] (Compose equivalent of iOS `.textCase(nil)` override)
 * - Color: [AMTheme.Text.primary]
 *
 * **Legibility shadow** (the iOS `.text` elevation recipe, on glyphs):
 * - Near layer: radius 3dp, y-offset 1dp, fixed black α=0.12
 * - Far layer: radius 10dp, y-offset 4dp, fixed black α=0.10
 *
 * Token cross-references: [AMTheme.Text.primary], [AMTheme.Shadow.textNear],
 * [AMTheme.Shadow.textFar].
 *
 * iOS equivalent: `IMSectionHeader` (`Sources/Content/SectionHeader.swift`).
 *
 * @param title The section label text. Rendered uppercased.
 */
@Composable
fun AMSectionHeader(title: String) {
    val isDark = LocalAMIsDarkTheme.current
    AMSectionHeader {
        AMTextLegibilityLabel(
            text = title.uppercase(),
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

/**
 * Custom-content slot overload of [AMSectionHeader].
 *
 * Wraps [content] in a full-width, leading-aligned [Box]. The [content]
 * composable is responsible for its own typographic styling AND its own
 * legibility shadow — Compose has no equivalent of SwiftUI's alpha-mask
 * `.shadow` for arbitrary content, and a bounding-rect shadow reads as a
 * gray band behind transparent labels. Text content can reuse
 * [AMTextLegibilityLabel] for the standard two-layer glyph shadow.
 *
 * iOS equivalent: `IMSectionHeader` generic `@ViewBuilder label` init.
 *
 * @param content Composable content to render inside the header frame.
 */
@Composable
fun AMSectionHeader(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

// ---------------------------------------------------------------------------
// AMTextLegibilityLabel (internal)
// ---------------------------------------------------------------------------

/**
 * Renders [text] with the two-layer iOS `.text` legibility shadow applied to
 * the GLYPHS.
 *
 * [TextStyle.shadow] holds exactly one [Shadow], so the label is drawn twice
 * in a stack: the bottom copy carries the far layer (10dp blur, 4dp y, fixed
 * black α=0.10), the top copy the near layer (3dp blur, 1dp y, fixed black
 * α=0.12). The copies share text and style, so they lay out identically and
 * the top copy's fill exactly covers the bottom one — the composite is
 * glyphs + near shadow over far shadow, the iOS stacking. Shared by
 * [AMSectionHeader] and `AMPageHeader`.
 *
 * @param text  Final display string (casing already applied by the caller).
 * @param style Typography for the label; its `shadow` field is overwritten.
 */
@Composable
internal fun AMTextLegibilityLabel(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val (nearShadow, farShadow) = with(density) {
        Shadow(
            color = AMTheme.Shadow.textNear,
            offset = Offset(0f, 1.dp.toPx()),
            blurRadius = 3.dp.toPx(),
        ) to Shadow(
            color = AMTheme.Shadow.textFar,
            offset = Offset(0f, 4.dp.toPx()),
            blurRadius = 10.dp.toPx(),
        )
    }

    Box {
        BasicText(text = text, style = style.copy(shadow = farShadow), modifier = modifier)
        BasicText(text = text, style = style.copy(shadow = nearShadow), modifier = modifier)
    }
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMSectionHeader · Light",
    group = "AMSectionHeader",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7, // AMTheme.Surface.page light
)
@Composable
private fun AMSectionHeaderPreviewLight() {
    AMThemeRoot {
        AMSectionHeaderPreviewContent()
    }
}

@Preview(
    name = "AMSectionHeader · Dark",
    group = "AMSectionHeader",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D, // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSectionHeaderPreviewDark() {
    AMThemeRoot {
        AMSectionHeaderPreviewContent()
    }
}

@Composable
private fun AMSectionHeaderPreviewContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AMTheme.Spacing.lg.dp,
                vertical = AMTheme.Spacing.xl.dp,
            ),
    ) {
        // String overload — most common usage
        AMSectionHeader(title = "Upcoming Events")

        Spacer(modifier = Modifier.height(AMTheme.Spacing.xl.dp))

        // Slot overload — custom content
        AMSectionHeader {
            val isDark = LocalAMIsDarkTheme.current
            BasicText(
                text = "CUSTOM SLOT HEADER",
                style = TextStyle(
                    color = AMTheme.Text.secondary.resolve(isDark),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                ),
            )
        }
    }
}
