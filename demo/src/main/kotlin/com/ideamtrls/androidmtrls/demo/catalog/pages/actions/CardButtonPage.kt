package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMCardActionRow
import com.ideamtrls.androidmtrls.AMCardButton
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTheme

internal val CardButtonEntry = ComponentEntry(
    id = "cardButton",
    category = CatalogCategory.Actions,
    title = "Card button",
    summary = "A quiet glyph and count for a card's actions",
    icon = R.drawable.ic_favorite,
    keywords = listOf(
        "AMCardButton", "like", "reply", "repost", "comment", "count", "social", "feed", "active",
        "activeTint", "TextButton", "IconButton",
    ),
) { CardButtonPage() }

@Composable
private fun CardButtonPage() {
    ComponentPage(
        CardButtonEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewActions() },
    ) {
        Post()
    }
}

// Stand-ins for your own actions.
private fun reply() {}

private fun like() {}

private fun repost() {}

private fun share() {}

private fun more() {}

/** Reply, like and share on a surface strip. */
@Composable
private fun PreviewActions() {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.primary)
            .padding(horizontal = spacing.xs, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMCardButton(painterResource(R.drawable.ic_chat_bubble), "Reply", onClick = ::reply, count = "12")
        AMCardButton(painterResource(R.drawable.ic_favorite), "Like", onClick = ::like, count = "48")
        AMCardButton(painterResource(R.drawable.ic_share), "Share", onClick = ::share)
    }
}

/** A post you've reposted and liked; tapping the heart takes the like back. */
@Composable
private fun Post() {
    var liked by rememberSaveable { mutableStateOf(true) }
    var likes by rememberSaveable { mutableIntStateOf(49) }
    ActionsPostCard(
        author = "Ada Lovelace",
        text = "The engine weaves algebraic patterns the way the loom weaves flowers and leaves.",
    ) {
        AMCardActionRow(
            counts = {
                AMCardButton(painterResource(R.drawable.ic_forum), "Comments", onClick = ::reply, count = "1.2K")
                AMCardButton(
                    painterResource(R.drawable.ic_repeat),
                    "Reposted",
                    onClick = ::repost,
                    count = "4",
                    active = true,
                )
                AMCardButton(
                    painterResource(if (liked) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
                    if (liked) "Liked" else "Like",
                    onClick = {
                        liked = !liked
                        likes += if (liked) 1 else -1
                    },
                    count = "$likes",
                    active = liked,
                    activeTint = AMSemantic.Destructive,
                )
            },
        ) {
            AMCardButton(painterResource(R.drawable.ic_share), "Share", onClick = ::share)
            AMCardButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::more)
        }
    }
}
