package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMTheme

internal val ActionGridButtonEntry = ComponentEntry(
    id = "actionGridButton",
    category = CatalogCategory.Actions,
    title = "Action grid button",
    summary = "A big glyph-over-title tile for the remote",
    icon = R.drawable.ic_grid_view_filled,
    keywords = listOf("AMActionGridButton", "tile", "remote", "menu", "filter", "active", "destructive", "elevation"),
) { ActionGridButtonPage() }

@Composable
private fun ActionGridButtonPage() {
    ComponentPage(
        ActionGridButtonEntry,
        preview = {
            AMActionGridRow(
                { AMActionGridButton(painterResource(R.drawable.ic_share), "Share", onClick = ::share) },
                { AMActionGridButton(painterResource(R.drawable.ic_content_copy), "Copy", onClick = ::copy) },
                { AMActionGridButton(painterResource(R.drawable.ic_push_pin), "Pin", onClick = ::pin) },
            )
        },
    ) {
        MailboxTiles()
    }
}

// Stand-ins for your own actions.
private fun share() {}

private fun copy() {}

private fun pin() {}

private fun openStorage() {}

private fun move() {}

private fun delete() {}

private enum class MailboxFilter { All, Starred, Archived }

/**
 * A mailbox's remote: the filters, the chosen one ringed, over storage, Move (nothing selected to
 * move) and Delete.
 */
@Composable
private fun MailboxTiles() {
    var filter by rememberSaveable { mutableStateOf(MailboxFilter.All) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        AMActionGridRow(
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_filter_list),
                    "All",
                    onClick = { filter = MailboxFilter.All },
                    active = filter == MailboxFilter.All,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_star),
                    "Starred",
                    onClick = { filter = MailboxFilter.Starred },
                    active = filter == MailboxFilter.Starred,
                )
            },
            {
                AMActionGridButton(
                    painterResource(R.drawable.ic_archive),
                    "Archived",
                    onClick = { filter = MailboxFilter.Archived },
                    active = filter == MailboxFilter.Archived,
                )
            },
        )
        AMActionGridRow(
            {
                AMActionGridButton(onClick = ::openStorage) {
                    Text("80%", style = MaterialTheme.typography.titleMedium)
                    Text("Storage")
                }
            },
            { AMActionGridButton(painterResource(R.drawable.ic_drive_file_move), "Move", onClick = ::move, enabled = false) },
            { AMActionGridButton(painterResource(R.drawable.ic_delete), "Delete", onClick = ::delete, destructive = true) },
        )
    }
}
