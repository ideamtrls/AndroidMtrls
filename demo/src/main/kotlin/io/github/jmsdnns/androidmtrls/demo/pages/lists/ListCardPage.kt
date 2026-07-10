package io.github.jmsdnns.androidmtrls.demo.pages.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.content.AMTag
import io.github.jmsdnns.androidmtrls.content.AMTagTint
import io.github.jmsdnns.androidmtrls.controls.buttons.AMCardButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.lists.AMListCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// ListCard catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMListCard].
 *
 * id: "lists-listcard", section: [ComponentSection.Lists].
 */
val listCardEntry: ComponentEntry = ComponentEntry(
    id = "lists-listcard",
    section = ComponentSection.Lists,
    title = "List Card",
    tileTitle = "Card",
    subtitle = "Card-shaped list row",
    intro = "Card-shaped row primitive — sibling to AMListRow. Wraps content in " +
        ".amCard() with .amBlock padding and a greedy width, so each card spans " +
        "its container.",
    icon = Icons.Default.Article,
    content = { ListCardPage() },
)

// ---------------------------------------------------------------------------
// ListCardPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMListCard].
 *
 * Two groups per demo spec §3.7:
 *
 * - **Variants** — note, article, and tag-style cards stacked with 12dp spacing.
 * - **With action row** — a single card ("Recompute Mercury trajectory") with a
 *   calendar tag and [AMCardButton] action strip, mirroring iOS exactly.
 */
@Composable
fun ListCardPage() {

    // ── Variants ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Variants") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            // Note style — title + short caption
            NoteCard()

            // Article style — title + body paragraph
            ArticleCard()

            // Tag style — title + metadata tags
            TagCard()
        }
    }

    // ── With action row ───────────────────────────────────────────────────────
    catalogGroup(title = "With action row") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            ActionRowCard()
        }
    }

}

// ---------------------------------------------------------------------------
// Private demo helpers
// ---------------------------------------------------------------------------

/** Note-style card: "Reasoning about reasoning" + short caption. */
@Composable
private fun NoteCard() {
    val isDark = LocalAMIsDarkTheme.current
    AMListCard(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = "Reasoning about reasoning",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = "Notes on second-order metacognition.",
            style = TextStyle(
                color = AMTheme.Text.secondary.resolve(isDark),
                fontSize = 12.sp,
            ),
        )
    }
}

/** Article-style card: "Field survey wrapped early" + body paragraph. */
@Composable
private fun ArticleCard() {
    val isDark = LocalAMIsDarkTheme.current
    AMListCard(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = "Field survey wrapped early",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = "The rain held off until evening, which gave the crew a clean window " +
                "to finish the eastern fence line before the first drops landed.",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 15.sp,
            ),
        )
    }
}

/** Tag-style card: "Cache-warm policies" + Draft / Infra / P2 tags. */
@Composable
private fun TagCard() {
    val isDark = LocalAMIsDarkTheme.current
    AMListCard(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = "Cache-warm policies",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            AMTag(text = "Draft")
            AMTag(text = "Infra", tint = AMTagTint.INFO)
            AMTag(text = "P2", tint = AMTagTint.WARNING)
        }
    }
}

/**
 * Single card with action row — "Recompute Mercury trajectory" with a calendar
 * tag and [AMCardButton] actions. Mirrors iOS IMListCardPage "With action row"
 * (one card only).
 */
@Composable
private fun ActionRowCard() {
    val isDark = LocalAMIsDarkTheme.current
    AMListCard(modifier = Modifier.fillMaxWidth()) {
        BasicText(
            text = "Recompute Mercury trajectory",
            style = TextStyle(
                color = AMTheme.Text.primary.resolve(isDark),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            AMCardButton(
                icon = Icons.Default.RadioButtonUnchecked,
                accessibilityLabel = "Mark done",
                onClick = {},
            )
            AMTag(
                text = "Mon",
                icon = Icons.Default.CalendarToday,
                tint = AMTagTint.WARNING,
            )
            Spacer(modifier = Modifier.weight(1f))
            AMCardButton(
                icon = Icons.Default.MoreHoriz,
                accessibilityLabel = "More",
                onClick = {},
            )
        }
    }
}
