package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * A flat list row: Material's `ListItem` at the row recipe's rhythm, lying flat on whatever holds
 * it. [title] in `bodyLarge`, an optional [subtitle] under it in `bodyMedium` and
 * `onSurfaceVariant`, and optional leading and trailing content.
 *
 * ```kotlin
 * Column(Modifier.fillMaxWidth().amCard()) {
 *     AMListRow("Ada Lovelace", subtitle = "ada@example.com", onClick = ::openAccount, leadingContent = { Icon(person, null) })
 *     AMDivider(inset = AMTheme.tokens.spacing.lg)
 *     AMListRow("About", onClick = ::openAbout)
 * }
 * ```
 *
 * - **Its surface.** Standing on the page, the row fills itself with [AMTheme.Surface.primary],
 *   edge to edge with no corners or shadow; inside a card it draws no fill, so the card's
 *   rounded surface shows. Stack rows with `AMDivider(inset = spacing.lg)` between them.
 * - **Rhythm.** 28 dp above and below the content and 16 dp at the sides ([AMRowDefaults]): one
 *   line makes an 80 dp row, a title and subtitle a 100 dp row. Text wraps; the row grows.
 * - **Taps.** Without [onClick] the row only draws. With it, the row is a button with Material's
 *   ripple. For actions on a press and hold, add `Modifier.amHoldable`; for swipe actions, wrap it
 *   in [AMSwipeableRow].
 * - **Chosen.** [selected] draws the selection ring ([AMTheme.selectionRingWidth] in
 *   [AMTheme.selectionRing]) inside the row's edges and tells a screen reader it's selected. The
 *   row itself never tints.
 *
 * @param title the row's heading.
 * @param modifier the row's placement.
 * @param subtitle a supporting line under the title. Null or empty shows none.
 * @param leadingContent a glyph, avatar or picture before the text. Null shows none.
 * @param trailingContent an accessory after the text: a value, a tag, a switch. Null shows none.
 * @param selected whether the row is the chosen one, ringed.
 * @param onClick makes the row a button. Null leaves it a plain row.
 * @param enabled whether the row takes taps. A disabled row keeps its place and dims.
 */
@Composable
public fun AMListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    AMListRow(
        modifier = modifier,
        leadingContent = leadingContent,
        supportingContent = if (subtitle.isNullOrEmpty()) null else ({ Text(subtitle) }),
        trailingContent = trailingContent,
        selected = selected,
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(title)
    }
}

/**
 * A flat list row with custom content: [content] in the headline's place (`bodyLarge`,
 * `onSurface`) and [supportingContent] under it (`bodyMedium`, `onSurfaceVariant`). Otherwise the
 * same row as the string form.
 *
 * @param modifier the row's placement.
 * @param leadingContent a glyph, avatar or picture before the text. Null shows none.
 * @param supportingContent what sits under the headline. Null shows none.
 * @param trailingContent an accessory after the text: a value, a tag, a switch. Null shows none.
 * @param selected whether the row is the chosen one, ringed.
 * @param onClick makes the row a button. Null leaves it a plain row.
 * @param enabled whether the row takes taps. A disabled row keeps its place and dims.
 * @param content the row's headline.
 */
@Composable
public fun AMListRow(
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isEnabled = resolveEnabled(enabled)
    val rowModifier = modifier
        .fillMaxWidth()
        .amFlatSurface()
        .then(claimRowActions())
        .then(
            if (selected) {
                Modifier
                    .amHairline(RectangleShape, width = AMTheme.selectionRingWidth)
                    .semantics { this.selected = true }
            } else {
                Modifier
            },
        )
    CompositionLocalProvider(LocalAMRowActions provides null) {
        if (onClick == null) {
            ListItem(
                modifier = rowModifier,
                enabled = isEnabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
                content = content,
            )
        } else {
            ListItem(
                onClick = onClick,
                modifier = rowModifier,
                enabled = isEnabled,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
                content = content,
            )
        }
    }
}
