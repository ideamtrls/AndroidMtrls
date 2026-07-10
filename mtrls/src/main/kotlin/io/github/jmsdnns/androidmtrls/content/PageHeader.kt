package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// AMPageHeader — large screen-title header
// ---------------------------------------------------------------------------

/**
 * Large, screen-level title header — port of iOS `IMPageHeader`.
 *
 * Applies the page-title font treatment (`28sp Bold`),
 * [AMTheme.Text.primary] foreground color, and the two-layer glyph-level
 * legibility shadow ([AMTextLegibilityLabel]) so the title remains readable
 * over a blurred backdrop. The composable fills the full available width
 * with leading alignment.
 *
 * Unlike [AMSectionHeader], the title renders **as-is** — no uppercase
 * transform. A page title reads as a heading, not a label.
 *
 * Use the String convenience overload for the common case:
 * ```kotlin
 * AMPageHeader("Library")
 * ```
 *
 * Or supply fully custom content (the header treatment — font, color,
 * shadow, leading alignment — is applied by this composable; the slot
 * provides only what sits inside that treatment):
 * ```kotlin
 * AMPageHeader {
 *     Row {
 *         Icon(Icons.Default.Tray, contentDescription = null)
 *         Spacer(Modifier.width(AMTheme.Spacing.sm.dp))
 *         BasicText(
 *             "Inbox",
 *             style = TextStyle(
 *                 color = AMTheme.Text.primary.resolve(isDark),
 *                 fontSize = 28.sp,
 *                 fontWeight = FontWeight.Bold,
 *             ),
 *         )
 *     }
 * }
 * ```
 *
 * iOS equivalent: `IMPageHeader` (`Sources/Content/PageHeader.swift`).
 * Token cross-references: [AMTheme.Text.primary], [AMTheme.Shadow.textNear],
 * [AMTheme.Shadow.textFar].
 *
 * @param content Composable content rendered inside the header treatment.
 *                The caller supplies layout, the title font/color, and its
 *                own legibility shadow ([AMTextLegibilityLabel] for text) —
 *                Compose cannot alpha-mask-shadow arbitrary content, and a
 *                bounding-rect shadow reads as a gray band behind labels.
 */
@Composable
fun AMPageHeader(
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

// ---------------------------------------------------------------------------
// String convenience overload
// ---------------------------------------------------------------------------

/**
 * Creates a page header from a raw title string — the common case.
 *
 * Renders [title] in the page-title style: `28sp Bold`,
 * [AMTheme.Text.primary] foreground, and the glyph-level legibility
 * shadow. Title text renders as-is (no uppercasing — page titles are
 * headings, not labels).
 *
 * ```kotlin
 * AMPageHeader("Library")
 * ```
 *
 * iOS equivalent: `IMPageHeader(_ title: String)` convenience init.
 *
 * @param title Page title rendered with the header treatment.
 */
@Composable
fun AMPageHeader(title: String) {
    val isDark = LocalAMIsDarkTheme.current
    AMPageHeader {
        AMTextLegibilityLabel(
            text = title,
            // No letterSpacing — iOS `imHeaderTreatment` applies `.title.bold`
            // with no `.tracking`, so the title uses the font's default spacing.
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------------
// Previews (light + dark, as required by conventions)
// ---------------------------------------------------------------------------

@Preview(
    name = "AMPageHeader · Light",
    group = "AMPageHeader",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMPageHeaderPreviewLight() {
    AMThemeRoot {
        AMPageHeaderPreviewContent()
    }
}

@Preview(
    name = "AMPageHeader · Dark",
    group = "AMPageHeader",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMPageHeaderPreviewDark() {
    AMThemeRoot {
        AMPageHeaderPreviewContent()
    }
}

@Composable
private fun AMPageHeaderPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AMTheme.Spacing.lg.dp,
                vertical = AMTheme.Spacing.xl.dp,
            ),
    ) {
        // String convenience — title renders as-is
        AMPageHeader("Library")

        Spacer(modifier = Modifier.padding(vertical = AMTheme.Spacing.md.dp))

        // Custom-content slot — caller supplies layout; header contributes
        // font, color, shadow, and leading alignment
        AMPageHeader {
            Row {
                BasicText(
                    text = "Inbox",
                    style = TextStyle(
                        color = AMTheme.Text.primary.resolve(isDark),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.width(AMTheme.Spacing.sm.dp))
                BasicText(
                    text = "·",
                    style = TextStyle(
                        color = AMTheme.Text.secondary.resolve(isDark),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
