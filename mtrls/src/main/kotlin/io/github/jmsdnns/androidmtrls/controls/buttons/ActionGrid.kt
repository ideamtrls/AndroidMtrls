package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// AMActionGrid — the bottom-anchored menu layout for a screen's actions
// ---------------------------------------------------------------------------

/**
 * The FAB-menu layout you fill an [AMActionsOverlay] (or a scene's
 * floating-actions menu) with: a stack of tile rows inset from the page edges.
 * Drop a few [AMActionGridRow]s inside.
 *
 * ```kotlin
 * AMActionsOverlay(
 *     isPresented = showMenu,
 *     onDismiss   = { showMenu = false },
 *     overlayContent = {
 *         AMActionGrid {
 *             AMActionGridRow(
 *                 tileA = { AMActionGridButton(Icons.Default.Edit, "Compose") { … } },
 *                 tileB = { AMActionGridButton(Icons.Default.Search, "Search") { … } },
 *             )
 *             AMActionGridRow(
 *                 tileA = { AMActionGridButton(Icons.Default.Notifications, "Alerts") { … } },
 *                 tileB = { AMActionGridButton(Icons.Default.Star, "Feed") { … } },
 *                 tileC = { AMActionGridButton(Icons.AutoMirrored.Filled.ExitToApp, "Exit") { … } },
 *             )
 *         }
 *     },
 * ) { … }
 * ```
 *
 * The outer recipe is fixed: rows are spaced by `spacing.md`, inset by
 * `spacing.lg` horizontally, and lifted `spacing.xxl` off the bottom edge so the
 * last row clears the gesture-nav zone. Tiles inside are normally
 * [AMActionGridButton]s at their **default `elevation = AMElevation.Low`** — each
 * tile self-cards (its own fill + hairline + shadow) and floats free over the
 * blurred paused world, exactly like iOS.
 *
 * **Bottom-anchoring is built in** — mirroring iOS `IMActionGrid`. A leading
 * weighted [Spacer] pushes the row stack to the bottom of the grid's bounds, so
 * the grid **fills its parent** (`fillMaxSize` by default) and the rows sit at
 * the bottom edge. The canonical parent is [AMActionsOverlay]'s full-screen,
 * free-floating content Box, which hands the grid a bounded, screen-filling
 * height. **Contract:** give this grid a *bounded* parent (the overlay does).
 * Composing it inside an unbounded parent — e.g. a `verticalScroll` column —
 * would feed the weighted Spacer an infinite height and crash on measure, the
 * same constraint iOS's full-screen overlay satisfies for its grid.
 *
 * iOS equivalent: `IMActionGrid` in `ActionGrid.swift`.
 *
 * @param modifier Applied to the outer [Column] after the built-in `fillMaxSize`,
 *                 so a caller can further constrain it if composing outside an
 *                 overlay.
 * @param content  The tile rows — one or more [AMActionGridRow]s, top to bottom,
 *                 scoped to [ColumnScope].
 */
@Composable
fun AMActionGrid(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    // fillMaxSize + a leading weighted Spacer bottom-anchor the rows within the
    // overlay's full-screen content Box (iOS: outer VStack { Spacer(minLength: 0);
    // rows }). The `then(modifier)` lets a caller outside an overlay re-constrain.
    Column(modifier = Modifier.fillMaxSize().then(modifier)) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.lg)
                .padding(bottom = tokens.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
            content = content,
        )
    }
}

// ---------------------------------------------------------------------------
// AMActionGridRow — a single row of 2, 3, or 4 equal-width action tiles
// ---------------------------------------------------------------------------

/**
 * A single row of action tiles, holding **exactly 2, 3, or 4** tiles laid out
 * edge-to-edge with equal treatment (`spacing.md` between them). Each tile is
 * wrapped in a `Modifier.weight(1f)` [Box], so N tiles split the row into N equal
 * columns.
 *
 * The 2/3/4-tile arity is enforced *structurally*: there are exactly three
 * overloads, taking two, three, or four tile lambdas — there is no one-tile
 * overload and no list/vararg path, so a row with a lone tile (or five) does not
 * compile. This encodes the scene design rule ("rows hold 2, 3, or 4 tiles, never
 * a lone tile") in the type system, mirroring iOS `IMActionGridRow`'s exact-arity
 * initializers.
 *
 * Tiles are normally [AMActionGridButton]s at their default elevation — each
 * self-cards and floats free over the overlay's blurred backdrop.
 *
 * iOS equivalent: `IMActionGridRow` in `ActionGrid.swift`.
 *
 * @param tileA    Leading tile.
 * @param tileB    Trailing tile of a 2-tile row / middle tile otherwise.
 * @param modifier Applied to the [Row]. `Modifier.fillMaxWidth()` is applied
 *                 after it, so a caller-supplied `padding` insets the whole row.
 */
@Composable
fun AMActionGridRow(
    tileA: @Composable () -> Unit,
    tileB: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        Box(modifier = Modifier.weight(1f)) { tileA() }
        Box(modifier = Modifier.weight(1f)) { tileB() }
    }
}

/**
 * A row of exactly three action tiles. See the 2-tile [AMActionGridRow] for the
 * arity rule and layout.
 *
 * @param tileA    Leading tile.
 * @param tileB    Middle tile.
 * @param tileC    Trailing tile.
 * @param modifier Applied to the [Row] before `Modifier.fillMaxWidth()`.
 */
@Composable
fun AMActionGridRow(
    tileA: @Composable () -> Unit,
    tileB: @Composable () -> Unit,
    tileC: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        Box(modifier = Modifier.weight(1f)) { tileA() }
        Box(modifier = Modifier.weight(1f)) { tileB() }
        Box(modifier = Modifier.weight(1f)) { tileC() }
    }
}

/**
 * A row of exactly four action tiles. See the 2-tile [AMActionGridRow] for the
 * arity rule and layout.
 *
 * @param tileA    Leading tile.
 * @param tileB    Second tile.
 * @param tileC    Third tile.
 * @param tileD    Trailing tile.
 * @param modifier Applied to the [Row] before `Modifier.fillMaxWidth()`.
 */
@Composable
fun AMActionGridRow(
    tileA: @Composable () -> Unit,
    tileB: @Composable () -> Unit,
    tileC: @Composable () -> Unit,
    tileD: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        Box(modifier = Modifier.weight(1f)) { tileA() }
        Box(modifier = Modifier.weight(1f)) { tileB() }
        Box(modifier = Modifier.weight(1f)) { tileC() }
        Box(modifier = Modifier.weight(1f)) { tileD() }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ActionGridPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    // A bounded-height frame stands in for the full-screen overlay content Box.
    // The grid fills it and bottom-anchors its rows via the built-in weighted
    // Spacer; each tile self-cards at its default AMElevation.Low.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(AMTheme.Surface.page.resolve(isDark)),
    ) {
        AMActionGrid {
            // 2-tile row
            AMActionGridRow(
                tileA = {
                    AMActionGridButton(
                        icon = Icons.Default.Edit,
                        title = "Compose",
                        onClick = {},
                    )
                },
                tileB = {
                    AMActionGridButton(
                        icon = Icons.Default.Search,
                        title = "Search",
                        onClick = {},
                    )
                },
            )
            // 3-tile row
            AMActionGridRow(
                tileA = {
                    AMActionGridButton(
                        icon = Icons.Default.Notifications,
                        title = "Alerts",
                        onClick = {},
                    )
                },
                tileB = {
                    AMActionGridButton(
                        icon = Icons.Default.Star,
                        title = "Feed",
                        onClick = {},
                    )
                },
                tileC = {
                    AMActionGridButton(
                        icon = Icons.Default.Share,
                        title = "Share",
                        onClick = {},
                    )
                },
            )
            // 4-tile row
            AMActionGridRow(
                tileA = {
                    AMActionGridButton(
                        icon = Icons.Default.Edit,
                        title = "Compose",
                        onClick = {},
                    )
                },
                tileB = {
                    AMActionGridButton(
                        icon = Icons.Default.Search,
                        title = "Search",
                        onClick = {},
                    )
                },
                tileC = {
                    AMActionGridButton(
                        icon = Icons.Default.Star,
                        title = "Feed",
                        onClick = {},
                    )
                },
                tileD = {
                    AMActionGridButton(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Exit",
                        onClick = {},
                    )
                },
            )
        }
    }
}

@Preview(
    name = "AMActionGrid · Light",
    group = "AMActionGrid",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMActionGridPreviewLight() {
    AMThemeRoot {
        ActionGridPreviewContent()
    }
}

@Preview(
    name = "AMActionGrid · Dark",
    group = "AMActionGrid",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMActionGridPreviewDark() {
    AMThemeRoot {
        ActionGridPreviewContent()
    }
}
