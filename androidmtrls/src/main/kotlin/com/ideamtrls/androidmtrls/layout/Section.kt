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
 * Over a picture (inside an [AMBackdrop]) the header and the footer both take `onSurface` and the
 * text legibility shadow, so they hold over the art.
 *
 * @param title the header, in the section header's look ([AMSectionHeader]).
 * @param modifier the section's placement.
 * @param footer a caption under the content, in `bodySmall` and `onSurfaceVariant` (over a
 *   picture, `onSurface` with the text shadow). Null shows none.
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
 * Over a picture (inside an [AMBackdrop]) the header and the footer both take `onSurface` and the
 * text legibility shadow.
 *
 * @param header the header's parts, in a row, in `titleSmall` and `primary`.
 * @param modifier the section's placement.
 * @param footer the caption under the content, given `bodySmall` and `onSurfaceVariant` (over a
 *   picture, `onSurface` with the text shadow). Null shows none.
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
    val spacing = AMTheme.tokens.spacing
    val side = if (inset) spacing.lg else 0.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        AMSectionHeader(Modifier.padding(horizontal = side), content = header)
        when (style) {
            AMSectionStyle.Rows -> SectionCard(
                Modifier.padding(horizontal = side).fillMaxWidth().amCard(fill, elevation),
                content,
            )
            AMSectionStyle.Card -> SectionCard(
                Modifier.padding(horizontal = side).fillMaxWidth()
                    .amCard(fill, elevation, padding = PaddingValues(spacing.lg)),
                content,
            )
            AMSectionStyle.Flat -> Column(
                Modifier.fillMaxWidth().amFlatSurface().padding(spacing.lg),
                content = content,
            )
        }
        if (footer != null) {
            // Over a picture the footer reads as the header does: onSurface with the text shadow.
            val overArt = LocalAMBackdrop.current != null
            val color = if (overArt) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                Modifier
                    .padding(horizontal = side)
                    .then(if (overArt) Modifier.glyphShadow(Style.Shadow.Text) else Modifier),
            ) {
                CompositionLocalProvider(LocalContentColor provides color) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall, footer)
                }
            }
        }
    }
}

/** The section's card: its content flat inside it, so a card in the content draws nothing of its own. */
@Composable
private fun SectionCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier) {
        val column = this
        ProvideAMSurfaceStyle(AMSurfaceStyle.Flat) { column.content() }
    }
}
