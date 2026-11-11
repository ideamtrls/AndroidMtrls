package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import com.ideamtrls.androidmtrls.AMCardActionRow
import com.ideamtrls.androidmtrls.AMCardButton
import com.ideamtrls.androidmtrls.AMListCard
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme

internal val ListCardEntry = ComponentEntry(
    id = "listCard",
    category = CatalogCategory.Lists,
    title = "List card",
    summary = "A list item in its own card, at the row rhythm",
    icon = R.drawable.ic_view_day,
    keywords = listOf("AMListCard", "ListItem", "card", "amCard", "row", "cell", "task", "item"),
) { ListCardPage() }

@Composable
private fun ListCardPage() {
    ComponentPage(
        ListCardEntry,
        preview = { AMListCard("Write the release notes", subtitle = "Due Friday · Docs") },
    ) {
        Tasks()
    }
}

// Stand-ins for your own actions.
private fun markDone() {}

private fun showMore() {}

/** A board of tasks: one with its actions, one with its labels, one with a note. */
@Composable
private fun Tasks() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AMListCard(
            supportingContent = {
                // The buttons carry their own padding; reach out so their glyphs line up with the text.
                AMCardActionRow(
                    modifier = Modifier.padding(top = spacing.xs).widenBy(spacing.lg - spacing.xs),
                    counts = {
                        AMCardButton(painterResource(R.drawable.ic_radio_button_unchecked), "Mark done", onClick = ::markDone)
                        AMTag("Mon", icon = painterResource(R.drawable.ic_calendar_today), tint = AMTagTint.Warning)
                    },
                ) {
                    AMCardButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::showMore)
                }
            },
        ) {
            Text("Recompute Mercury trajectory")
        }
        AMListCard(
            supportingContent = {
                Row(Modifier.padding(top = spacing.sm), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    AMTag("Draft")
                    AMTag("Infra", tint = AMTagTint.Info)
                    AMTag("P2", tint = AMTagTint.Warning)
                }
            },
        ) {
            Text("Cache-warm policies")
        }
        AMListCard(
            "Field survey wrapped early",
            subtitle = "The rain held off until evening, which gave the crew a clean window to finish the eastern fence line.",
        )
    }
}

/** Lays the content out [amount] wider on each side than it's given, reaching past its parent's inset. */
private fun Modifier.widenBy(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * extra))
    layout(placeable.width - 2 * extra, placeable.height) { placeable.placeRelative(-extra, 0) }
}
