package io.github.jmsdnns.androidmtrls.demo.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amElevation

// ---------------------------------------------------------------------------
// catalogCategoryHeader — page-level title
// ---------------------------------------------------------------------------

/**
 * Large page header matching iOS `catalogCategoryHeader(_:)`.
 *
 * Delegates to [AMPageHeader] (which owns font, color, legibility shadow, and
 * leading alignment) and adds catalog page-edge padding: top [AMTheme.Spacing.md]
 * and horizontal [AMTheme.Spacing.lg] — matching iOS `catalogCategoryHeader`.
 *
 * Called by [ComponentPage] when [ComponentPage.showsTitle] is true, and can
 * also be used directly on pages that manage their own scroll root.
 *
 * @param title The header text.
 */
@Composable
fun catalogCategoryHeader(
    title: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = AMTheme.Spacing.md.dp,
                start = AMTheme.Spacing.lg.dp,
                end = AMTheme.Spacing.lg.dp,
            ),
    ) {
        AMPageHeader(title = title)
    }
}

// ---------------------------------------------------------------------------
// catalogGroup — section header + flush content slot
// ---------------------------------------------------------------------------

/**
 * A labeled group block matching iOS `catalogGroup(title:content:)`.
 *
 * Renders an [AMSectionHeader] (which owns casing, size, tracking, and color)
 * padded at horizontal [AMTheme.Spacing.lg], above a flush content slot. The
 * Column uses [AMTheme.Spacing.md] spacing between label and content, matching
 * iOS `VStack(spacing: IMTheme.Spacing.md)`.
 *
 * Content runs full-bleed so rows and cards can span edge-to-edge.
 *
 * @param title   Section label text (transformed to uppercase by [AMSectionHeader]).
 * @param content The group body.
 */
@Composable
fun catalogGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            AMSectionHeader(title = title)
        }
        content()
    }
}

// ---------------------------------------------------------------------------
// catalogFlatDemo — full-bleed primary-surface strip
// ---------------------------------------------------------------------------

/**
 * Full-bleed [AMTheme.Surface.primary] strip for flat-by-nature widgets.
 *
 * Matches iOS `catalogFlatDemo(padding:content:)`. The strip fills edge-to-edge;
 * [padding] is applied as horizontal-only inset (leading + trailing), matching
 * iOS default `EdgeInsets(top: 0, leading: lg, bottom: 0, trailing: lg)`.
 * Pass `0.dp` for widgets that self-pad (text inputs, sliders).
 *
 * @param padding Horizontal interior padding applied to the surface strip.
 *                Defaults to [AMTheme.Spacing.lg].
 * @param content The widget to host inside the flat surface.
 */
@Composable
fun catalogFlatDemo(
    padding: Dp = AMTheme.Spacing.lg.dp,
    content: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.primary.resolve(isDark))
            .padding(horizontal = padding),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// catalogSwatch — 44×32dp color/text swatch chip
// ---------------------------------------------------------------------------

/**
 * Color swatch chip matching iOS `catalogSwatch(_:_:textColor:)`.
 *
 * Renders a 44×32dp pill with [color] fill and a monospaced [name] label.
 * When [showAsText] is true the chip renders as a text specimen ("Aa" in
 * [textColor] or [color]) on a [AMTheme.Surface.primary] background.
 * The "Aa" specimen uses `.title3.bold()` (≈20sp Bold), matching iOS.
 *
 * @param name        Token name label displayed in monospace.
 * @param color       The fill color to display.
 * @param textColor   Label color override (uses [color] when null and [showAsText] is true).
 * @param showAsText  If true, the swatch body renders "Aa" in [color] (text specimen mode).
 */
@Composable
fun catalogSwatch(
    name: String,
    color: Color,
    textColor: Color? = null,
    showAsText: Boolean = false,
) {
    val isDark = LocalAMIsDarkTheme.current
    val labelColor = AMTheme.Text.primary.resolve(isDark)
    val shape = RoundedCornerShape(AMTheme.Radius.sm.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(32.dp)
                .clip(shape)
                .background(if (showAsText) AMTheme.Surface.primary.resolve(isDark) else color)
                .border(width = AMTheme.hairlineWidth.dp, color = AMTheme.selectionRing.resolve(isDark), shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            if (showAsText) {
                BasicText(
                    text = "Aa",
                    style = TextStyle(
                        color = textColor ?: color,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        BasicText(
            text = name,
            style = TextStyle(
                color = labelColor,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------
// catalogSpacingChip — tint-filled bar chip of a given width
// ---------------------------------------------------------------------------

/**
 * Spacing swatch chip matching iOS `catalogSpacingChip(_:_:)`.
 *
 * Renders a tint-filled horizontal bar whose width equals [value] dp, with a
 * monospaced name label and numeric value below. Vertical spacing between
 * elements is 4dp (matching iOS `VStack(spacing: 4)`).
 *
 * @param name  Token name label (e.g. "lg").
 * @param value Token value in dp (the bar width).
 */
@Composable
fun catalogSpacingChip(
    name: String,
    value: Float,
) {
    val isDark = LocalAMIsDarkTheme.current
    val labelColor = AMTheme.Text.primary.resolve(isDark)
    val secondaryColor = AMTheme.Text.secondary.resolve(isDark)
    // iOS fills the bar with the full system-blue `Semantic.info` (no alpha) —
    // matches `catalogSpacingChip(_:_:)` in `Catalog/Helpers.swift`.
    val chipColor = AMTheme.Semantic.info.resolve(isDark)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(value.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(chipColor),
        )
        BasicText(
            text = name,
            style = TextStyle(
                color = labelColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            ),
        )
        BasicText(
            text = "${value.toInt()}",
            style = TextStyle(
                color = secondaryColor,
                fontSize = 11.sp,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// catalogRadiusChip — 56×56dp rounded-rect chip with shadow
// ---------------------------------------------------------------------------

/**
 * Radius swatch chip matching iOS `catalogRadiusChip(_:_:)`.
 *
 * Renders a 56×56dp square with corner radius [value] and a Low elevation shadow.
 * Vertical spacing between elements is 4dp (matching iOS `VStack(spacing: 4)`).
 *
 * @param name  Token name label (e.g. "lg").
 * @param value Radius value in dp.
 */
@Composable
fun catalogRadiusChip(
    name: String,
    value: Float,
) {
    val isDark = LocalAMIsDarkTheme.current
    val labelColor = AMTheme.Text.primary.resolve(isDark)
    val secondaryColor = AMTheme.Text.secondary.resolve(isDark)
    val shape = RoundedCornerShape(value.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .amElevation(level = AMElevation.Low, shape = shape)
                .background(color = AMTheme.Surface.primary.resolve(isDark), shape = shape),
        )
        BasicText(
            text = name,
            style = TextStyle(
                color = labelColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            ),
        )
        BasicText(
            text = "${value.toInt()}",
            style = TextStyle(
                color = secondaryColor,
                fontSize = 11.sp,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// catalogElevationChip — 80×56dp surface rect at a given elevation level
// ---------------------------------------------------------------------------

/**
 * Elevation swatch chip matching iOS `catalogElevationChip(_:_:)`.
 *
 * Renders an 80×56dp surface rectangle at [level] elevation. Vertical spacing
 * between the chip and label is 6dp (matching iOS `VStack(spacing: 6)`).
 *
 * @param name  Token name label (e.g. "Low").
 * @param level The elevation level to demonstrate.
 */
@Composable
fun catalogElevationChip(
    name: String,
    level: AMElevation,
) {
    val isDark = LocalAMIsDarkTheme.current
    val labelColor = AMTheme.Text.secondary.resolve(isDark)
    val shape = RoundedCornerShape(AMTheme.Radius.md.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(56.dp)
                .amElevation(level = level, shape = shape)
                .background(color = AMTheme.Surface.primary.resolve(isDark), shape = shape),
        )
        BasicText(
            text = name,
            style = TextStyle(
                color = labelColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// catalogTypeRow — two-column type specimen row
// ---------------------------------------------------------------------------

/**
 * Type specimen row matching iOS `catalogTypeRow(_:_:_:)`.
 *
 * Renders a two-column row: the style name on the left (90dp fixed width,
 * caption2 monospaced) and a live specimen ("The quick brown fox") in the
 * given [style] on the right. A metadata line below the specimen shows
 * `"Roboto · NNsp"` (equivalent to iOS `"FontName · NNpt"`), matching the
 * resolved point-size annotation iOS uses.
 *
 * @param name  Style name label (e.g. "Title", "Body").
 * @param style The [TextStyle] to specimen.
 */
@Composable
fun catalogTypeRow(
    name: String,
    style: TextStyle,
) {
    val isDark = LocalAMIsDarkTheme.current
    val labelColor = AMTheme.Text.secondary.resolve(isDark)
    val primaryColor = AMTheme.Text.primary.resolve(isDark)
    val metaColor = AMTheme.Text.secondary.resolve(isDark)

    val fontSizeSp = style.fontSize.value.toInt()
    val fontMeta = "Roboto · ${fontSizeSp}sp"
    val labelColumnWidth = 90.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp, vertical = AMTheme.Spacing.sm.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        BasicText(
            text = name,
            style = TextStyle(
                color = labelColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            ),
            modifier = Modifier.width(labelColumnWidth),
        )
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = "The quick brown fox",
                style = style.copy(color = primaryColor),
            )
            BasicText(
                text = fontMeta,
                style = TextStyle(
                    color = metaColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                ),
            )
        }
    }
}
