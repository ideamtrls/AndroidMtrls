package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
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
 * a row in it would sit inset twice. Rows too many to compose at once go in a `LazyColumn` through
 * [amSection], the same card drawn a row at a time.
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
        if (footer != null) SectionFooter(footer, Modifier.padding(horizontal = side))
    }
}

/** A section's footer: the caption look, or on art, art text in the secondary ink. */
@Composable
private fun SectionFooter(footer: @Composable () -> Unit, modifier: Modifier) {
    val onArt = LocalAMArtTone.current != null
    val color = if (onArt) artInk(secondary = true) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier.then(if (onArt) Modifier.amArtText(secondary = true) else Modifier)) {
        CompositionLocalProvider(LocalContentColor provides color) {
            ProvideTextStyle(MaterialTheme.typography.bodySmall, footer)
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

/**
 * A section of rows in a lazy list: an optional header, the rows as one card, an optional footer,
 * with each row composed only while it's on screen, for a group of rows too long to compose at
 * once (a podcast's episodes).
 *
 * ```kotlin
 * LazyColumn(Modifier.fillMaxSize()) {
 *     amSection(episodes, key = { it.id }, title = "Episodes", footer = "${episodes.size} episodes") { episode ->
 *         AMListRow(episode.title, subtitle = episode.date)
 *     }
 * }
 * ```
 *
 * - **One card.** It's [AMSection]'s [AMSectionStyle.Rows] card, drawn a row at a time: each row
 *   draws its slice of the card, the first row the top corners, the last the bottom ones, and the
 *   shadow, fill and edge run unbroken between them. Rows lie bare on it as in any card of rows,
 *   each bringing the row recipe; the card clips them to its corners, ripples included.
 * - **Dividers.** An [AMDivider] at the `lg` inset sits between each row and the next, unless
 *   [dividers] is false.
 * - **The list stacks its items edge to edge.** Give the `LazyColumn` no spacing between items, or
 *   the card breaks at each row; set sections apart with items of their own or with the list's
 *   content padding. The header and the footer stand `spacing.sm` off the card, as in [AMSection].
 * - **Keys.** [key] gives each row a key that is stable, unique across the whole list (other
 *   sections included) and saveable in a `Bundle`, as for any `LazyColumn`; the header and the
 *   footer key themselves.
 * - **Over art.** A frosted [fill] blurs the art of the nearest [AMBackdrop] or [AMArtRowsBackdrop]
 *   under each row; on an [amArtRows] list, rows marked with [amArtRow] give their own colors. The
 *   header and the footer are art text there, as in [AMSection].
 *
 * @param items the rows' items, in order.
 * @param key a stable, unique key per item, saveable in a `Bundle`.
 * @param title the header, in the section header's look ([AMSectionHeader]). Null shows none.
 * @param footer a caption under the card, in `bodySmall` and `onSurfaceVariant` (on art, art text
 *   in the secondary ink). Null shows none.
 * @param fill the card's fill, solid or frosted over art.
 * @param elevation the card's shadow; [AMElevation.None] keeps the fill and corners but casts
 *   nothing.
 * @param inset whether the header, footer and card stand in from the list's edges by `spacing.lg`.
 *   Pass false where the list is already inset (content padding at the page gutter).
 * @param dividers whether an inset [AMDivider] separates each row from the next.
 * @param content builds an item's row.
 */
public fun <T> LazyListScope.amSection(
    items: List<T>,
    key: (item: T) -> Any,
    title: String? = null,
    footer: String? = null,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    inset: Boolean = true,
    dividers: Boolean = true,
    content: @Composable (item: T) -> Unit,
) {
    lazySection(
        items = items,
        key = key,
        header = title?.let { text -> { Text(text) } },
        footer = footer?.let { text -> { Text(text) } },
        fill = fill,
        elevation = elevation,
        inset = inset,
        dividers = dividers,
        content = content,
    )
}

/**
 * A section of rows in a lazy list with a custom header and footer: the section header's look
 * applied to [header] (a glyph, a title, a count), the rows as one card composed only while on
 * screen, and [footer] under it in the caption look. Otherwise the same as the string form.
 *
 * ```kotlin
 * amSection(
 *     episodes,
 *     key = { it.id },
 *     header = {
 *         Text("Episodes", Modifier.weight(1f))
 *         Text("${episodes.size}")
 *     },
 * ) { episode -> AMListRow(episode.title) }
 * ```
 *
 * @param items the rows' items, in order.
 * @param key a stable, unique key per item, saveable in a `Bundle`.
 * @param header the header's parts, in a row, in `titleSmall` and `primary`.
 * @param footer the caption under the card, given `bodySmall` and `onSurfaceVariant` (on art, art
 *   text in the secondary ink). Null shows none.
 * @param fill the card's fill, solid or frosted over art.
 * @param elevation the card's shadow.
 * @param inset whether the header, footer and card stand in from the list's edges by `spacing.lg`.
 * @param dividers whether an inset [AMDivider] separates each row from the next.
 * @param content builds an item's row.
 */
public fun <T> LazyListScope.amSection(
    items: List<T>,
    key: (item: T) -> Any,
    header: @Composable RowScope.() -> Unit,
    footer: (@Composable () -> Unit)? = null,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    elevation: AMElevation = AMElevation.Low,
    inset: Boolean = true,
    dividers: Boolean = true,
    content: @Composable (item: T) -> Unit,
) {
    lazySection(items, key, header, footer, fill, elevation, inset, dividers, content)
}

/** Every form of [amSection]: the header item when there is one, a slice item per row, the footer item. */
private fun <T> LazyListScope.lazySection(
    items: List<T>,
    key: (item: T) -> Any,
    header: (@Composable RowScope.() -> Unit)?,
    footer: (@Composable () -> Unit)?,
    fill: AMSurfaceFill,
    elevation: AMElevation,
    inset: Boolean,
    dividers: Boolean,
    content: @Composable (item: T) -> Unit,
) {
    if (header != null) {
        item(contentType = SectionHeaderItem) {
            val spacing = AMTheme.tokens.spacing
            AMSectionHeader(
                Modifier.padding(horizontal = if (inset) spacing.lg else 0.dp).padding(bottom = spacing.sm),
                content = header,
            )
        }
    }
    items(count = items.size, key = { index -> key(items[index]) }, contentType = { SectionRowItem }) { index ->
        val spacing = AMTheme.tokens.spacing
        Column(
            Modifier
                .padding(horizontal = if (inset) spacing.lg else 0.dp)
                .fillMaxWidth()
                .amCardSlice(fill, elevation, MaterialTheme.shapes.large, first = index == 0, last = index == items.lastIndex),
        ) {
            ProvideAMSurfaceFill(fill) {
                ProvideAMSurfaceStyle(AMSurfaceStyle.Flat) {
                    if (dividers && index > 0) AMDivider(inset = spacing.lg)
                    content(items[index])
                }
            }
        }
    }
    if (footer != null) {
        item(contentType = SectionFooterItem) {
            val spacing = AMTheme.tokens.spacing
            SectionFooter(footer, Modifier.padding(top = spacing.sm).padding(horizontal = if (inset) spacing.lg else 0.dp))
        }
    }
}

// The lazy section's content types, so lists reuse each kind of item for its own kind.
private data object SectionHeaderItem
private data object SectionRowItem
private data object SectionFooterItem
