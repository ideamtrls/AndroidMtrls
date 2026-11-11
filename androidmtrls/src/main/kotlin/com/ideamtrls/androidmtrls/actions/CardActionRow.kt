package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The action row at the bottom of a card, the one arrangement every card's actions share: the
 * counted actions ([counts]: reply 12, repost 4, like 48) at the start, the glyph-only ones
 * ([actions]: share, then ⋮ last) at the end, with the free space between them.
 *
 * ```kotlin
 * val spacing = AMTheme.tokens.spacing
 * Column(Modifier.fillMaxWidth().amCard()) {
 *     PostText(Modifier.padding(start = spacing.lg, top = spacing.lg, end = spacing.lg))
 *     AMCardActionRow(
 *         modifier = Modifier.padding(start = spacing.xs, end = spacing.xs, bottom = spacing.xs),
 *         counts = {
 *             AMCardButton(painterResource(R.drawable.ic_chat_bubble), "Reply", onClick = ::reply, count = "12")
 *             AMCardButton(painterResource(R.drawable.ic_favorite), "Like", onClick = ::like, count = "48")
 *         },
 *     ) {
 *         AMCardButton(painterResource(R.drawable.ic_share), "Share", onClick = ::share)
 *         AMCardButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::more)
 *     }
 * }
 * ```
 *
 * The rule holds everywhere: every action of a card sits on this one row, below the content, never
 * split across two rows or placed beside the title; within [actions], share comes before ⋮ and ⋮
 * is always last. A card with no counts passes [actions] alone.
 *
 * The buttons are Material's and carry their own padding, which already spaces them apart. Inset
 * the row `spacing.xs` from the card's edges (not the card's `spacing.lg`), so the first glyph
 * lines up with the text above and ⋮ with its end.
 *
 * @param modifier the row's placement. The row fills the width it's given.
 * @param counts the counted actions, at the start, usually [AMCardButton]s with a count. Null for a
 *   card with none.
 * @param actions the glyph-only actions, at the end: share, then ⋮.
 */
@Composable
public fun AMCardActionRow(
    modifier: Modifier = Modifier,
    counts: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        counts?.invoke(this)
        Spacer(Modifier.weight(1f))
        actions()
    }
}
