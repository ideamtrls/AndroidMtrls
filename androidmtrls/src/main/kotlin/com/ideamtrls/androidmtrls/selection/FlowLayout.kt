package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Lays its children out from the start edge and starts a new line when the next one doesn't fit:
 * Compose's `FlowRow` at the library's spacing, for a run of tags, chips or other pills that
 * should wrap to the width they're given.
 *
 * ```kotlin
 * AMFlowLayout {
 *     topics.forEach { topic -> AMTag(topic) }
 * }
 * ```
 *
 * Each line fills until the next child won't fit, then breaks; a line is as tall as its tallest
 * child, and children sit at its top. A child wider than the layout is offered the layout's width,
 * so a label that can wrap does. The layout is as wide as its widest line; give it
 * `Modifier.fillMaxWidth()` to take the whole width (its children stay at the start).
 * [AMChipGroup] wraps its chips with it.
 *
 * @param modifier the layout's placement and size.
 * @param spacing the gap between neighbors on a line: `spacing.sm` by default.
 * @param lineSpacing the gap between lines: [spacing] by default.
 * @param content the children, in reading order.
 */
@Composable
public fun AMFlowLayout(
    modifier: Modifier = Modifier,
    spacing: Dp = AMTheme.tokens.spacing.sm,
    lineSpacing: Dp = spacing,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(lineSpacing),
        content = content,
    )
}
