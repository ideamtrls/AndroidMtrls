package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** How an [AMSection] holds its content: one style per kind of content. */
public enum class AMSectionStyle {
    /**
     * A group of rows: stacked bare in a card that adds no padding, since every row carries the
     * row recipe ([Modifier.amRowPadding][amRowPadding], [AMRowDefaults]). Put an [AMDivider] with
     * the `lg` inset between rows that want a line.
     */
    Rows,

    /** Block content (prose, a composite block): in a card padded by `spacing.lg` all round. */
    Card,

    /**
     * A reading block: padded by `spacing.lg` on a full-width strip of [AMTheme.Surface.primary],
     * with no corners or shadow. Inside a card the strip draws no fill and lies on the card.
     */
    Flat,
}

/**
 * A titled group: a section header, the content in a card or on a flat strip, and an optional
 * footer under it.
 *
 * ```kotlin
 * AMSection("Notifications", footer = "Email digest sends one summary a day.", style = AMSectionStyle.Rows) {
 *     PushAlertsRow()
 *     AMDivider(inset = AMTheme.tokens.spacing.lg)
 *     EmailDigestRow()
 * }
 * ```
 *
 * Rows go in [AMSectionStyle.Rows], not [AMSectionStyle.Card]: the card style pads its content, so
 * a row in it would sit inset twice.
 *
 * On art (inside an [AMBackdrop] or [AMArtRowsBackdrop]) the header and the footer lie on the art
 * as art text for its tone ([amArtText]): the header in `onSurface`, the footer in
 * `onSurfaceVariant`, which the art's wash holds them over.
 *
 * @param title the header, in the section header's look ([AMSectionHeader]).
 * @param modifier the section's placement.
 * @param footer a caption under the content, in `bodySmall` and `onSurfaceVariant` (on art, art
 *   text in the secondary ink). Null shows none.
 * @param style how the content sits: a card of rows, a padded card block, or a flat strip.
 * @param fill the card's fill, solid or frosted over art. The flat style ignores it.
 * @param elevation the card's shadow; [AMElevation.None] keeps the fill and corners but casts
 *   nothing. The flat style ignores it.
 * @param inset whether the header, footer and card stand in from the section's edges by
 *   `spacing.lg`. Pass false where the column around the section is already inset (a page gutter).
 *   The flat strip always runs the full width.
 * @param content the section's content, in a column.
 */
@Composable
public fun AMSection(
    title: String,
    modifier: Modifier = Modifier,
    footer: String? = null,
    style: AMSectionStyle = AMSectionStyle.Card,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    inset: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AMSection(
        header = { Text(title) },
        modifier = modifier,
        footer = footer?.let { text -> { Text(text) } },
        style = style,
        fill = fill,
        elevation = elevation,
        inset = inset,
        content = content,
    )
}

/**
 * A titled group with a custom header and footer: the section header's look applied to [header]
 * (a glyph, a title, a count), the content in a card or on a flat strip, and [footer] under it in
 * the caption look.
 *
 * ```kotlin
 * AMSection(
 *     header = {
 *         Text("Team", Modifier.weight(1f))
 *         Text("2")
 *     },
 *     style = AMSectionStyle.Rows,
 * ) { MemberRows() }
 * ```
 *
 * On art (inside an [AMBackdrop] or [AMArtRowsBackdrop]) the header and the footer are art text
 * for the art's tone, as in the string form.
 *
 * @param header the header's parts, in a row, in `titleSmall` and `primary`.
 * @param modifier the section's placement.
 * @param footer the caption under the content, given `bodySmall` and `onSurfaceVariant` (on art,
 *   art text in the secondary ink). Null shows none.
 * @param style how the content sits: a card of rows, a padded card block, or a flat strip.
 * @param fill the card's fill, solid or frosted over art. The flat style ignores it.
 * @param elevation the card's shadow. The flat style ignores it.
 * @param inset whether the header, footer and card stand in from the section's edges by
 *   `spacing.lg`.
 * @param content the section's content, in a column.
 */
@Composable
public fun AMSection(
    header: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    footer: (@Composable () -> Unit)? = null,
    style: AMSectionStyle = AMSectionStyle.Card,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    inset: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionLayout(header, modifier, footer, style, fill, elevation, inset, content)
}

/**
 * A group with no header: the content in a card or on a flat strip, and an optional footer under
 * it, for a group that follows a header of its own (a page's first group under its title) or needs
 * none. Otherwise the same section as the titled forms: the footer in the caption look, art text
 * on art.
 *
 * ```kotlin
 * AMSection(footer = "Downloads play offline.", style = AMSectionStyle.Rows) { DownloadRows() }
 * ```
 *
 * @param modifier the section's placement.
 * @param footer a caption under the content, in `bodySmall` and `onSurfaceVariant` (on art, art
 *   text in the secondary ink). Null shows none.
 * @param style how the content sits: a card of rows, a padded card block, or a flat strip.
 * @param fill the card's fill, solid or frosted over art. The flat style ignores it.
 * @param elevation the card's shadow. The flat style ignores it.
 * @param inset whether the footer and card stand in from the section's edges by `spacing.lg`.
 * @param content the section's content, in a column.
 */
@Composable
public fun AMSection(
    modifier: Modifier = Modifier,
    footer: String? = null,
    style: AMSectionStyle = AMSectionStyle.Card,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    inset: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SectionLayout(null, modifier, footer?.let { text -> { Text(text) } }, style, fill, elevation, inset, content)
}

/** Every form of [AMSection]: the header when there is one, the content in its style, the footer. */
@Composable
private fun SectionLayout(
    header: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier,
    footer: (@Composable () -> Unit)?,
    style: AMSectionStyle,
    fill: AMSurfaceFill,
    elevation: AMElevation,
    inset: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    val side = if (inset) spacing.lg else 0.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (header != null) AMSectionHeader(Modifier.padding(horizontal = side), content = header)
        when (style) {
            AMSectionStyle.Rows -> SectionCard(
                Modifier.padding(horizontal = side).fillMaxWidth().amCard(fill, elevation),
                fill,
                content,
            )
            AMSectionStyle.Card -> SectionCard(
                Modifier.padding(horizontal = side).fillMaxWidth()
                    .amCard(fill, elevation, padding = PaddingValues(spacing.lg)),
                fill,
                content,
            )
            AMSectionStyle.Flat -> Column(
                Modifier.fillMaxWidth().amFlatSurface().padding(spacing.lg),
                content = content,
            )
        }
        if (footer != null) {
            // On art the footer is art text in the secondary ink.
            val onArt = LocalAMArtTone.current != null
            val color = if (onArt) artInk(secondary = true) else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                Modifier
                    .padding(horizontal = side)
                    .then(if (onArt) Modifier.amArtText(secondary = true) else Modifier),
            ) {
                CompositionLocalProvider(LocalContentColor provides color) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall, footer)
                }
            }
        }
    }
}

/**
 * The section's card: its content flat inside it, so a card in the content draws nothing of its
 * own, and lying on the card's [fill], so its secondary text resolves from it.
 */
@Composable
private fun SectionCard(modifier: Modifier, fill: AMSurfaceFill, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        val column = this
        ProvideAMSurfaceFill(fill) {
            ProvideAMSurfaceStyle(AMSurfaceStyle.Flat) { column.content() }
        }
    }
}
