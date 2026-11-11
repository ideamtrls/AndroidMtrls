package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMSwipeAction
import com.ideamtrls.androidmtrls.AMSwipeableRow
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val SwipeableRowEntry = ComponentEntry(
    id = "swipeableRow",
    category = CatalogCategory.Lists,
    title = "Swipeable row",
    summary = "A full swipe acts, one action each way",
    icon = R.drawable.ic_swipe_left,
    keywords = listOf(
        "AMSwipeableRow", "AMSwipeAction", "SwipeToDismissBox", "swipe", "dismiss", "delete", "archive", "flag",
        "full swipe",
    ),
) { SwipeableRowPage() }

@Composable
private fun SwipeableRowPage() {
    ComponentPage(
        SwipeableRowEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { Digest() },
    ) {
        Inbox()
    }
}

// Stand-ins for your own actions.
private fun pin() {}

private fun markRead() {}

private fun flag() {}

/** One message on its own: swipe right to pin it, left to mark it read. */
@Composable
private fun Digest() {
    AMSwipeableRow(
        startAction = AMSwipeAction("Pin", painterResource(R.drawable.ic_push_pin), onClick = ::pin),
        endAction = AMSwipeAction("Mark as read", painterResource(R.drawable.ic_mark_email_read), AMSemantic.Success, onClick = ::markRead),
    ) {
        AMListRow(
            "Weekly digest",
            subtitle = "3 new highlights",
            leadingContent = { RowGlyph(R.drawable.ic_mail, AMSemantic.Info) },
        )
    }
}

/** Which swipes a message offers. */
private enum class ListsMessageSwipes { ArchiveOrDelete, Flag, Delete }

@Immutable
private class ListsMessage(
    val id: Int,
    val title: String,
    val detail: String,
    @param:DrawableRes val icon: Int,
    val tint: AMSemantic,
    val actions: ListsMessageSwipes,
)

private val Messages = listOf(
    ListsMessage(1, "Grace commented", "On “Cache policies”", R.drawable.ic_chat_bubble, AMSemantic.Info, ListsMessageSwipes.ArchiveOrDelete),
    ListsMessage(2, "Build succeeded", "AndroidMtrls · 2m ago", R.drawable.ic_verified, AMSemantic.Success, ListsMessageSwipes.Flag),
    ListsMessage(3, "Storage almost full", "1.2 GB remaining", R.drawable.ic_hard_drive, AMSemantic.Warning, ListsMessageSwipes.Delete),
)

/** An inbox card: archive and delete take a message away, a flag leaves it in place. */
@Composable
private fun Inbox() {
    val messages = rememberListsItems(Messages) { it.id }
    val archive = painterResource(R.drawable.ic_archive)
    val delete = painterResource(R.drawable.ic_delete)
    val flagged = painterResource(R.drawable.ic_flag)
    Column(Modifier.fillMaxWidth().amCard()) {
        messages.forEachIndexed { index, message ->
            key(message.id) {
                val remove = AMSwipeAction("Delete", delete, AMSemantic.Destructive, removesRow = true) { messages.remove(message) }
                AMSwipeableRow(
                    startAction = when (message.actions) {
                        ListsMessageSwipes.ArchiveOrDelete -> AMSwipeAction("Archive", archive, removesRow = true) { messages.remove(message) }
                        else -> null
                    },
                    endAction = when (message.actions) {
                        ListsMessageSwipes.Flag -> AMSwipeAction("Flag", flagged, AMSemantic.Warning, onClick = ::flag)
                        else -> remove
                    },
                ) {
                    AMListRow(
                        message.title,
                        subtitle = message.detail,
                        leadingContent = { RowGlyph(message.icon, message.tint) },
                    )
                }
                if (index < messages.lastIndex) AMDivider(inset = AMTheme.tokens.spacing.lg)
            }
        }
    }
}

/** A row's leading glyph in a status color. */
@Composable
private fun RowGlyph(@DrawableRes icon: Int, tint: AMSemantic) {
    Icon(painterResource(icon), contentDescription = null, tint = tint.color)
}
