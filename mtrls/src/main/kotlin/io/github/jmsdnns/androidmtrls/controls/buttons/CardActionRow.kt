package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// AMCardActionRow — the hard-rule container for card action rows
// ---------------------------------------------------------------------------

/**
 * Top inset that lifts the action row off the card content above it.
 *
 * DELIBERATELY a file-level private constant, NOT a spacing token: the "hard
 * design rule" fixes the row's relationship to the content above it, and a
 * theme spacing override must not be able to distort that fixed 2dp lift. The
 * inter-button `xl` spacing, by contrast, flows through the live theme token.
 * iOS parity — `IMCardActionRow.topInset` is a private static constant, not
 * `tokens.spacing`.
 */
private val CARD_ACTION_ROW_TOP_INSET = 2.dp

/**
 * The action row at the bottom of a card — the container that encodes the
 * system's hard design rule for card actions so call sites can't drift from it.
 *
 * Lays its content out in a single [Row] with
 * `horizontalArrangement = Arrangement.spacedBy(spacing.xl)` (the live theme
 * token) and [CARD_ACTION_ROW_TOP_INSET] of top padding that lifts the row off
 * the content above. The `content` slot receives a [RowScope] so callers place
 * the mandatory `Spacer(Modifier.weight(1f))` separator themselves.
 *
 * **The arrangement (identical everywhere):** all card actions live on this
 * single bottom row. Count-labeled [AMCardButton]s (`reply 12`, `like 48`)
 * cluster on the **leading** side; a `Spacer(Modifier.weight(1f))` separates
 * them from the **trailing** cluster of icon-only buttons (share, overflow).
 * Within the trailing cluster, **share comes before the overflow/ellipsis, and
 * the overflow button is always last.** When there are no count-labeled buttons
 * the row is just `Spacer(Modifier.weight(1f))` + the trailing cluster. Never
 * split actions across two rows.
 *
 * ```kotlin
 * AMCard {
 *     Column {
 *         // …card content…
 *         AMCardActionRow {
 *             AMCardButton(icon = ..., label = "12", accessibilityLabel = "Reply") { }
 *             AMCardButton(icon = ..., label = "48", accessibilityLabel = "Like") { }
 *             Spacer(Modifier.weight(1f))
 *             AMCardButton(icon = ..., accessibilityLabel = "Share") { }
 *             AMCardButton(icon = ..., accessibilityLabel = "More") { }
 *         }
 *     }
 * }
 * ```
 *
 * iOS equivalent: `IMCardActionRow<Content>` in
 * `Sources/Controls/Buttons/CardActionRow.swift`.
 *
 * @param modifier Modifier applied to the row's root layout node (the fixed top
 *   inset is added after it).
 * @param content  The row's buttons and separating `Spacer(Modifier.weight(1f))`,
 *   in leading-to-trailing order, composed in a [RowScope].
 */
@Composable
fun AMCardActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current

    Row(
        modifier = modifier.padding(top = CARD_ACTION_ROW_TOP_INSET),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun CardActionRowPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Counts leading, icon-only actions trailing, hosted inside a card the
        // way the container is designed to be used. Like is the one `active`
        // toggle (destructive/red tint); reply and repost stay muted.
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            ) {
                BasicText(
                    text = "A post with a couple of interactions on it.",
                    style = AMTheme.Type.body.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                AMCardActionRow {
                    // Count-labeled buttons cluster LEFT.
                    AMCardButton(
                        icon = Icons.Default.Email,
                        label = "12",
                        accessibilityLabel = "Reply",
                        onClick = {},
                    )
                    AMCardButton(
                        icon = Icons.Default.Refresh,
                        label = "4",
                        accessibilityLabel = "Repost",
                        onClick = {},
                    )
                    // Active like — destructive (red) tint.
                    AMCardButton(
                        icon = Icons.Default.Favorite,
                        label = "48",
                        active = true,
                        activeTint = AMTheme.Semantic.destructive.resolve(isDark),
                        accessibilityLabel = "Liked",
                        onClick = {},
                    )
                    // Separator — pushes the trailing cluster to the edge.
                    Spacer(Modifier.weight(1f))
                    // Icon-only buttons cluster RIGHT (share before overflow).
                    AMCardButton(
                        icon = Icons.Default.Share,
                        accessibilityLabel = "Share",
                        onClick = {},
                    )
                    AMCardButton(
                        icon = Icons.Default.MoreVert,
                        accessibilityLabel = "More",
                        onClick = {},
                    )
                }
            }
        }
    }
}

@Preview(
    name = "AMCardActionRow · Light",
    group = "AMCardActionRow",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,   // AMTheme.Surface.page light
)
@Composable
private fun AMCardActionRowPreviewLight() {
    AMThemeRoot {
        CardActionRowPreviewContent()
    }
}

@Preview(
    name = "AMCardActionRow · Dark",
    group = "AMCardActionRow",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,   // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMCardActionRowPreviewDark() {
    AMThemeRoot {
        CardActionRowPreviewContent()
    }
}
