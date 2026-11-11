package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMActionsOverlay
import com.ideamtrls.androidmtrls.AMButton

internal val ActionGridEntry = ComponentEntry(
    id = "actionGrid",
    category = CatalogCategory.Actions,
    title = "Action grid",
    summary = "Bottom-anchored rows of two to four tiles",
    icon = R.drawable.ic_grid_view,
    keywords = listOf(
        "AMActionGrid", "AMActionGridRow", "AMActionGridButton", "remote", "menu", "tiles", "navigation",
        "floating action button", "arity",
    ),
) { ActionGridPage() }

@Composable
private fun ActionGridPage() {
    // The grid carries its own side insets and floats its own tiles, so it runs edge to edge.
    ComponentPage(
        ActionGridEntry,
        previewLayout = StageLayout.Wide,
        preview = { SceneMenu() },
    ) {
        ShowMenu()
    }
}

// Stand-ins for your own actions.
private fun compose() {}

private fun search() {}

private fun openFeed() {}

private fun openAlerts() {}

private fun signOut() {}

private fun pin() {}

private fun duplicate() {}

private fun share() {}

private fun archive() {}

/** A small app's remote: two actions, then its views and Exit. */
@Composable
private fun SceneMenu() {
    // On the page, the grid sits above the navigation bar already.
    AMActionGrid(windowInsets = WindowInsets(0)) {
        AMActionGridRow(
            { Tile(R.drawable.ic_edit_square, "Compose", ::compose) },
            { Tile(R.drawable.ic_search, "Search", ::search) },
        )
        AMActionGridRow(
            { Tile(R.drawable.ic_feed, "Feed", ::openFeed) },
            { Tile(R.drawable.ic_notifications, "Alerts", ::openAlerts) },
            { Tile(R.drawable.ic_logout, "Exit", ::signOut) },
        )
    }
}

/**
 * A full remote over the blurred screen: actions on the item in four, then Compose, Search and
 * Alerts, then Feed and Exit. Each tile closes the menu, then does its thing.
 */
@Composable
private fun ShowMenu() {
    var showsMenu by rememberSaveable { mutableStateOf(false) }
    fun closeThen(action: () -> Unit): () -> Unit = {
        showsMenu = false
        action()
    }
    AMButton("Show menu", onClick = { showsMenu = true }, modifier = Modifier.fillMaxWidth())
    AMActionsOverlay(showsMenu, onDismissRequest = { showsMenu = false }) {
        AMActionGrid {
            AMActionGridRow(
                { Tile(R.drawable.ic_push_pin, "Pin", closeThen(::pin)) },
                { Tile(R.drawable.ic_library_add, "Duplicate", closeThen(::duplicate)) },
                { Tile(R.drawable.ic_share, "Share", closeThen(::share)) },
                { Tile(R.drawable.ic_archive, "Archive", closeThen(::archive)) },
            )
            AMActionGridRow(
                { Tile(R.drawable.ic_edit_square, "Compose", closeThen(::compose)) },
                { Tile(R.drawable.ic_search, "Search", closeThen(::search)) },
                { Tile(R.drawable.ic_notifications, "Alerts", closeThen(::openAlerts)) },
            )
            AMActionGridRow(
                { Tile(R.drawable.ic_feed, "Feed", closeThen(::openFeed)) },
                { Tile(R.drawable.ic_logout, "Exit", closeThen(::signOut)) },
            )
        }
    }
}

@Composable
private fun Tile(@DrawableRes icon: Int, title: String, onClick: () -> Unit) {
    AMActionGridButton(painterResource(icon), title, onClick = onClick)
}
