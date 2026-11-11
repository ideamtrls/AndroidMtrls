package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMEmptyStateAction

internal val EmptyStateEntry = ComponentEntry(
    id = "emptyState",
    category = CatalogCategory.Feedback,
    title = "Empty state",
    summary = "Glyph, title and optional actions for no content",
    icon = R.drawable.ic_inbox,
    keywords = listOf(
        "AMEmptyState", "AMEmptyStateAction", "no results", "nothing here", "blank slate", "placeholder",
        "zero state",
    ),
) { EmptyStatePage() }

@Composable
private fun EmptyStatePage() {
    ComponentPage(
        EmptyStateEntry,
        preview = {
            AMEmptyState(
                painterResource(R.drawable.ic_inbox),
                "No messages",
                description = "New messages will appear here.",
                action = AMEmptyStateAction("Compose", painterResource(R.drawable.ic_edit_square), ::compose),
            )
        },
    ) {
        AMEmptyState(
            painterResource(R.drawable.ic_group),
            "No contacts",
            description = "Add people to start a conversation.",
        ) {
            AMButton("Add contact", onClick = ::addContact, style = AMButtonStyle.Primary())
            AMButton("Import contacts", onClick = ::importContacts, style = AMButtonStyle.Ghost)
        }
    }
}

// Stand-ins for your own actions.
private fun compose() {}
private fun addContact() {}
private fun importContacts() {}
