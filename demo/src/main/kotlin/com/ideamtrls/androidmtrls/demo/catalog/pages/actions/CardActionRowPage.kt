package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMCardActionRow
import com.ideamtrls.androidmtrls.AMCardButton
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTheme

internal val CardActionRowEntry = ComponentEntry(
    id = "cardActionRow",
    category = CatalogCategory.Actions,
    title = "Card action row",
    summary = "The fixed bottom row of a card's actions",
    icon = R.drawable.ic_call_to_action,
    keywords = listOf("AMCardActionRow", "AMCardButton", "feed", "post", "share", "more", "overflow", "layout rule"),
) { CardActionRowPage() }

@Composable
private fun CardActionRowPage() {
    ComponentPage(
        CardActionRowEntry,
        preview = {
            ActionsPostCard(text = "Shipped the new catalog today. Every component has a page now.") {
                AMCardActionRow(
                    counts = {
                        AMCardButton(painterResource(R.drawable.ic_chat_bubble), "Reply", onClick = ::reply, count = "12")
                        AMCardButton(painterResource(R.drawable.ic_favorite), "Like", onClick = ::like, count = "48")
                    },
                ) {
                    ShareAndMore()
                }
            }
        },
    ) {
        Feed()
    }
}

// Stand-ins for your own actions.
private fun reply() {}

private fun repost() {}

private fun like() {}

private fun share() {}

private fun more() {}

/** The trailing pair every card ends with: share, then ⋮ last. */
@Composable
private fun ShareAndMore() {
    AMCardButton(painterResource(R.drawable.ic_share), "Share", onClick = ::share)
    AMCardButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::more)
}

/** Two posts in a feed: one with counts, reposted and liked, and an announcement with none. */
@Composable
private fun Feed() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        ActionsPostCard(
            author = "Ada Lovelace",
            text = "Notes on the engine are up. The appendix runs longer than the paper.",
        ) {
            AMCardActionRow(
                counts = {
                    AMCardButton(painterResource(R.drawable.ic_chat_bubble), "Reply", onClick = ::reply, count = "12")
                    AMCardButton(painterResource(R.drawable.ic_repeat), "Reposted", onClick = ::repost, count = "4", active = true)
                    AMCardButton(
                        painterResource(R.drawable.ic_favorite_filled),
                        "Liked",
                        onClick = ::like,
                        count = "49",
                        active = true,
                        activeTint = AMSemantic.Destructive,
                    )
                },
            ) {
                ShareAndMore()
            }
        }
        ActionsPostCard(author = "Catalog", text = "Version 2 is out, with a page for every component.") {
            AMCardActionRow {
                ShareAndMore()
            }
        }
    }
}
