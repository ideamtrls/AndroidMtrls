package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The line between rows: Material's horizontal divider, in its own thickness and color, with an
 * optional leading [inset].
 *
 * ```kotlin
 * Column(Modifier.fillMaxWidth().amCard()) {
 *     Text("Subtotal", Modifier.amRowPadding())
 *     AMDivider(inset = AMTheme.tokens.spacing.lg)
 *     Text("Shipping", Modifier.amRowPadding())
 * }
 * ```
 *
 * @param modifier its placement.
 * @param inset the space before the line at its start. Pass the spacing scale's `lg` between rows
 *   of any kind, so the line starts where their text does (every row carries the row recipe's
 *   side inset); leave it at 0 for a line across the full width.
 */
@Composable
public fun AMDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    HorizontalDivider(modifier.padding(start = inset))
}
