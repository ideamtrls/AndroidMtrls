package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * A post in a card, as a feed shows one: an optional author, its text, then its action row at the
 * row's own inset, so the row's first glyph lines up with the text.
 */
@Composable
internal fun ActionsPostCard(
    text: String,
    modifier: Modifier = Modifier,
    author: String? = null,
    actionRow: @Composable () -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    Column(modifier.fillMaxWidth().amCard()) {
        Column(
            Modifier.fillMaxWidth().padding(start = spacing.lg, top = spacing.lg, end = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            if (author != null) Text(author, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
        Box(Modifier.fillMaxWidth().padding(start = spacing.xs, end = spacing.xs, bottom = spacing.xs)) {
            actionRow()
        }
    }
}
