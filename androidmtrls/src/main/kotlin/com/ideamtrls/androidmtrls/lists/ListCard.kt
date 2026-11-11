package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A list item that is a card: Material's `ListItem` at the row recipe's rhythm inside a floating
 * card ([amCard]), for lists whose items stand apart rather than sharing a card.
 *
 * ```kotlin
 * Column(verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
 *     tasks.forEach { task -> AMListCard(task.title, subtitle = task.due, onClick = { open(task) }) }
 * }
 * ```
 *
 * The card spans its container's width, in `shapes.large` with the card's shadow; its text sits
 * at the same inset and height as any other row's (28 dp above and below, 16 dp at the sides).
 * With [onClick] the whole card is a button, its ripple inside the card's corners. Stack cards
 * `spacing.md` apart, or hand them to [AMReorderableCardList], which keeps their text clear of
 * its drag handle and lifts a dragged card higher.
 *
 * @param title the card's heading.
 * @param modifier the card's placement.
 * @param subtitle a supporting line under the title. Null or empty shows none.
 * @param leadingContent a glyph, avatar or picture before the text. Null shows none.
 * @param trailingContent an accessory after the text. Null shows none.
 * @param fill the card's fill, solid or frosted over art.
 * @param onClick makes the card a button. Null leaves it a plain card.
 * @param enabled whether the card takes taps. A disabled card keeps its place and dims.
 */
@Composable
public fun AMListCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    AMListCard(
        modifier = modifier,
        leadingContent = leadingContent,
        supportingContent = if (subtitle.isNullOrEmpty()) null else ({ Text(subtitle) }),
        trailingContent = trailingContent,
        fill = fill,
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(title)
    }
}

/**
 * A list item card with custom content: [content] in the headline's place (`bodyLarge`,
 * `onSurface`) and [supportingContent] under it (`bodyMedium`, `onSurfaceVariant`), such as a
 * row of tags or the card's actions. Otherwise the same card as the string form.
 *
 * @param modifier the card's placement.
 * @param leadingContent a glyph, avatar or picture before the text. Null shows none.
 * @param supportingContent what sits under the headline. Null shows none.
 * @param trailingContent an accessory after the text. Null shows none.
 * @param fill the card's fill, solid or frosted over art.
 * @param onClick makes the card a button. Null leaves it a plain card.
 * @param enabled whether the card takes taps. A disabled card keeps its place and dims.
 * @param content the card's headline.
 */
@Composable
public fun AMListCard(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    val side = AMTheme.tokens.spacing.lg
    val vertical = AMTheme.rowVerticalPadding
    // In a reorderable card list the list draws its handle at the card's trailing edge.
    val padding = PaddingValues(start = side, top = vertical, end = side + LocalAMReorderHandleInset.current, bottom = vertical)
    val elevation = if (LocalAMReorderLifted.current) AMElevation.Medium else AMElevation.Low
    val cardModifier = modifier
        .fillMaxWidth()
        .amCard(fill = fill, elevation = elevation)
        .then(claimRowActions())
    CompositionLocalProvider(
        LocalAMRowActions provides null,
        LocalAMReorderHandleInset provides 0.dp,
        LocalAMReorderLifted provides false,
    ) {
        if (onClick == null) {
            ListItem(
                modifier = cardModifier,
                enabled = isEnabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = padding,
                content = content,
            )
        } else {
            ListItem(
                onClick = onClick,
                modifier = cardModifier,
                enabled = isEnabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = padding,
                content = content,
            )
        }
    }
}
