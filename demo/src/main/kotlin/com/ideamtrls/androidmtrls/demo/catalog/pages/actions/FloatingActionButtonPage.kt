package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amInstantSwitch
import com.ideamtrls.androidmtrls.amRowPadding

internal val FloatingActionButtonEntry = ComponentEntry(
    id = "floatingActionButton",
    category = CatalogCategory.Actions,
    title = "Floating action button",
    summary = "A movable plus button that opens a scene's remote",
    icon = R.drawable.ic_add_circle,
    keywords = listOf(
        "AMFloatingActionButton", "AMFloatingActions", "AMScene", "AMKeepsPlace", "FAB", "hub", "remote", "plus",
        "movable", "drag", "lift", "AMActionGrid", "actions menu", "views", "landing view", "navigation",
    ),
) { FloatingActionButtonPage() }

@Composable
private fun FloatingActionButtonPage() {
    // Each demo is a small scene, since the button and its menu belong to a whole screen: an
    // edge-to-edge band that is the scene's screen.
    ComponentPage(
        FloatingActionButtonEntry,
        previewLayout = StageLayout.Fill,
        loadedLayout = StageLayout.Fill,
        preview = { NotesScene() },
    ) {
        MailScene()
    }
}

// Stand-ins for your own actions.
private fun compose() {}

private fun search() {}

/** The band's height: the scene's screen. */
private val SceneHeight = 380.dp

/**
 * Room at the end of a list so its last row can scroll clear of the button: the button's 56 dp, the
 * `spacing.lg` under it and the same again over it.
 */
private val ButtonClearance = 88.dp

/**
 * A scene on the band: its height, and no system bars of its own, so the button rests just above
 * the band's bottom edge rather than above the screen's navigation bar.
 */
private fun Modifier.sceneFrame(systemBars: WindowInsets): Modifier =
    fillMaxWidth().height(SceneHeight).consumeWindowInsets(systemBars)

/**
 * A notes app's scene: its button opens Compose and Search, and the line under the title says which
 * was picked.
 */
@Composable
private fun NotesScene() {
    var showsMenu by rememberSaveable { mutableStateOf(false) }
    var lastAction by rememberSaveable { mutableStateOf("None yet") }
    fun pick(action: String): () -> Unit = {
        showsMenu = false
        lastAction = action
    }
    AMScene(
        Modifier.sceneFrame(WindowInsets.safeDrawing),
        floatingActions = {
            AMFloatingActions(showsMenu, onExpandedChange = { showsMenu = it }) {
                AMActionGrid {
                    AMActionGridRow(
                        { Tile(R.drawable.ic_edit_square, "Compose", pick("Compose")) },
                        { Tile(R.drawable.ic_search, "Search", pick("Search")) },
                    )
                }
            }
        },
        containerColor = Color.Transparent,
    ) {
        Column(Modifier.fillMaxSize()) {
            AMPageHeader("Notes")
            Text(
                "Last action: $lastAction",
                Modifier.padding(horizontal = AMTheme.tokens.spacing.lg),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    CatalogAutoPresent { showsMenu = true }
}

/** The mail app's two root views, which its remote switches between. */
private enum class Mailbox { Inbox, Archive }

/**
 * A mail app's scene with two views: the remote holds Compose and Search, then Inbox and Archive,
 * the current one ringed. Each view keeps its place (its scroll) while the other shows, and back
 * from Archive returns to the Inbox.
 */
@Composable
private fun MailScene() {
    var mailbox by rememberSaveable { mutableStateOf(Mailbox.Inbox) }
    var showsMenu by rememberSaveable { mutableStateOf(false) }
    fun closeThen(action: () -> Unit): () -> Unit = {
        showsMenu = false
        action()
    }
    fun show(view: Mailbox): () -> Unit = {
        showsMenu = false
        amInstantSwitch { mailbox = view }
    }
    AMScene(
        current = mailbox,
        landing = Mailbox.Inbox,
        onCurrentChange = { mailbox = it },
        modifier = Modifier.sceneFrame(WindowInsets.safeDrawing),
        floatingActions = {
            AMFloatingActions(showsMenu, onExpandedChange = { showsMenu = it }) {
                AMActionGrid {
                    AMActionGridRow(
                        { Tile(R.drawable.ic_edit_square, "Compose", closeThen(::compose)) },
                        { Tile(R.drawable.ic_search, "Search", closeThen(::search)) },
                    )
                    AMActionGridRow(
                        { Tile(R.drawable.ic_inbox, "Inbox", show(Mailbox.Inbox), mailbox == Mailbox.Inbox) },
                        { Tile(R.drawable.ic_archive, "Archive", show(Mailbox.Archive), mailbox == Mailbox.Archive) },
                    )
                }
            }
        },
        containerColor = Color.Transparent,
    ) { root ->
        when (root) {
            Mailbox.Inbox -> MailboxView("Inbox", InboxMessages)
            Mailbox.Archive -> MailboxView("Archive", ArchivedMessages)
        }
    }
    CatalogAutoPresent { showsMenu = true }
}

/** One mailbox: its title over its messages, which scroll under the button. */
@Composable
private fun MailboxView(title: String, messages: List<Message>) {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxSize()) {
        AMPageHeader(title)
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(bottom = ButtonClearance)) {
            itemsIndexed(messages) { index, message ->
                if (index > 0) AMDivider(inset = spacing.lg)
                Column(Modifier.amRowPadding()) {
                    Text(message.sender, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        message.subject,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(@DrawableRes icon: Int, title: String, onClick: () -> Unit, active: Boolean = false) {
    AMActionGridButton(painterResource(icon), title, onClick = onClick, active = active)
}

private class Message(val sender: String, val subject: String)

private val InboxMessages = listOf(
    Message("Maya Chen", "Harbor survey notes"),
    Message("Tom Okafor", "Lunch on Thursday?"),
    Message("Priya Raman", "Draft for your review"),
    Message("Leo Park", "Tickets for Saturday"),
    Message("Ana Silva", "Photos from the trip"),
    Message("Sam Reed", "Quarterly report"),
)

private val ArchivedMessages = listOf(
    Message("Nora Hale", "Welcome to the team"),
    Message("Ben Ito", "Your order has shipped"),
    Message("Maya Chen", "Kickoff agenda"),
)
