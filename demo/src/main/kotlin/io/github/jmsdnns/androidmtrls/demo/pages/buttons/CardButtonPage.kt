package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardActionRow
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Card Button catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMCardButton].
 *
 * id: "imCardButton", section: Buttons.
 */
val cardButtonEntry: ComponentEntry = ComponentEntry(
    id = "imCardButton",
    section = ComponentSection.Buttons,
    title = "Card Button",
    tileTitle = "Card",
    subtitle = "Compact button for card rows",
    intro = "Icon-first compact button that lives inside a card's action row. Deliberately " +
        "flat — the surrounding AMCard already supplies fill and elevation. AMCardActionRow " +
        "encodes the canonical arrangement: count-labeled buttons cluster leading, a Spacer " +
        "separates them, and icon-only buttons cluster trailing with share before ellipsis.",
    icon = Icons.Filled.CheckCircle,
    content = { CardButtonPage() },
)

// ---------------------------------------------------------------------------
// CardButtonPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMCardButton], mirroring the iOS demo (Card Button):
 * - **Default** — heart and bubble icon-only buttons LEFT, Spacer, then share and
 *   ellipsis icon-only buttons RIGHT, all in the quiet muted treatment.
 * - **Action Row** — a post title line above the row, then count-labeled reply LEFT
 *   and an active like (destructive/red tint) demonstrating `active`/`activeTint`,
 *   Spacer, then share and ellipsis RIGHT.
 *
 * Both rows live inside a padded [AMCard]; the surrounding card supplies the fill
 * and elevation the buttons deliberately lack.
 */
@Composable
fun CardButtonPage() {
    val isDark = LocalAMIsDarkTheme.current
    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon-only buttons — cluster LEFT
                AMCardButton(
                    icon = Icons.Default.FavoriteBorder,
                    accessibilityLabel = "Like",
                    onClick = {},
                )
                AMCardButton(
                    icon = Icons.Default.ChatBubbleOutline,
                    accessibilityLabel = "Reply",
                    onClick = {},
                )

                // Separator — pushes right cluster to the trailing edge
                Spacer(Modifier.weight(1f))

                // Icon-only buttons — cluster RIGHT (share before ellipsis)
                AMCardButton(
                    icon = Icons.Default.Share,
                    accessibilityLabel = "Share",
                    onClick = {},
                )
                AMCardButton(
                    icon = Icons.Default.MoreHoriz,
                    accessibilityLabel = "More",
                    onClick = {},
                )
            }
        }
    }

    // ── Action Row (active toggles) ──────────────────────────────────────────
    catalogGroup(title = "Action Row") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            padding = AMPadding.block,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                // Post title line above the action row.
                BasicText(
                    text = "A post with a couple of interactions on it.",
                    style = AMTheme.Type.body.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )
                // AMCardActionRow is the hard-rule container: it fixes the xl button
                // spacing, the 2dp top inset, and CenterVertically so the call site
                // can't drift. Count-labeled buttons cluster LEFT, a weighted Spacer
                // separates, icon-only buttons cluster RIGHT (share before overflow).
                AMCardActionRow {
                    // Count-labeled buttons — cluster LEFT
                    AMCardButton(
                        icon = Icons.Default.ChatBubbleOutline,
                        label = "12",
                        accessibilityLabel = "Reply",
                        onClick = {},
                    )
                    // Active like — destructive (red) tint
                    AMCardButton(
                        icon = Icons.Default.Favorite,
                        label = "48",
                        active = true,
                        activeTint = AMTheme.Semantic.destructive.resolve(isDark),
                        accessibilityLabel = "Liked",
                        onClick = {},
                    )

                    // Separator — pushes right cluster to the trailing edge
                    Spacer(Modifier.weight(1f))

                    // Icon-only buttons — cluster RIGHT (share before ellipsis)
                    AMCardButton(
                        icon = Icons.Default.Share,
                        accessibilityLabel = "Share",
                        onClick = {},
                    )
                    AMCardButton(
                        icon = Icons.Default.MoreHoriz,
                        accessibilityLabel = "More",
                        onClick = {},
                    )
                }
            }
        }
    }
}
