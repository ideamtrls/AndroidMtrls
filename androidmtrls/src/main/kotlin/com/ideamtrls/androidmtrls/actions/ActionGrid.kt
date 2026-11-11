package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The remote's grid: rows of tiles stacked at the bottom of the space it's given, where a thumb
 * reaches them, inset from the screen's edges. Fill it with [AMActionGridRow]s, top to bottom.
 *
 * ```kotlin
 * AMActionGrid {
 *     AMActionGridRow(
 *         { AMActionGridButton(painterResource(R.drawable.ic_edit_square), "Compose", onClick = ::compose) },
 *         { AMActionGridButton(painterResource(R.drawable.ic_search), "Search", onClick = ::search) },
 *     )
 *     AMActionGridRow(
 *         { AMActionGridButton(painterResource(R.drawable.ic_feed), "Feed", onClick = ::openFeed, active = true) },
 *         { AMActionGridButton(painterResource(R.drawable.ic_notifications), "Alerts", onClick = ::openAlerts) },
 *         { AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = ::signOut) },
 *     )
 * }
 * ```
 *
 * The recipe is fixed: rows `spacing.md` apart, `spacing.lg` from the sides, and the last row
 * `spacing.xxl` above the navigation bar. The grid fills the space it's given and pushes its rows
 * to the bottom; with no height bound (in a scrolling column) it's as tall as its rows.
 *
 * @param modifier the grid's placement.
 * @param windowInsets the system bars the grid keeps clear of: the navigation bar by default. Pass
 *   `WindowInsets(0)` where the grid doesn't reach the bottom of the screen.
 * @param content the rows, top to bottom.
 */
@Composable
public fun AMActionGrid(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = AMTheme.tokens.spacing
    Column(
        modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets)
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.Bottom),
        content = content,
    )
}

/**
 * A row of exactly two tiles, side by side in equal columns `spacing.md` apart.
 *
 * A row holds two, three or four tiles, never one and never more: there's an overload for each and
 * no list form, so a row of any other size doesn't compile. Each slot is stretched to its column's
 * full width, so an [AMActionGridButton] fills its column and every tile in a row is as wide as
 * the others.
 *
 * @param first the first tile.
 * @param second the second tile.
 * @param modifier the row's placement. The row fills the width it's given.
 */
@Composable
public fun AMActionGridRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(modifier, listOf(first, second))
}

/**
 * A row of exactly three tiles, side by side in equal columns `spacing.md` apart. See the two-tile
 * [AMActionGridRow] for the rule.
 *
 * @param first the first tile.
 * @param second the second tile.
 * @param third the third tile.
 * @param modifier the row's placement. The row fills the width it's given.
 */
@Composable
public fun AMActionGridRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(modifier, listOf(first, second, third))
}

/**
 * A row of exactly four tiles, side by side in equal columns `spacing.md` apart. See the two-tile
 * [AMActionGridRow] for the rule.
 *
 * @param first the first tile.
 * @param second the second tile.
 * @param third the third tile.
 * @param fourth the fourth tile.
 * @param modifier the row's placement. The row fills the width it's given.
 */
@Composable
public fun AMActionGridRow(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    third: @Composable () -> Unit,
    fourth: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    TileRow(modifier, listOf(first, second, third, fourth))
}

/** Every row's layout: equal columns, each slot stretched to its column. */
@Composable
private fun TileRow(modifier: Modifier, tiles: List<@Composable () -> Unit>) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        tiles.forEach { tile ->
            Box(Modifier.weight(1f), propagateMinConstraints = true) { tile() }
        }
    }
}
