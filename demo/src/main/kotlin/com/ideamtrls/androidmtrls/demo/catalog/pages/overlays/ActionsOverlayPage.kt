package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMActionsOverlay
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme

internal val ActionsOverlayEntry = ComponentEntry(
    id = "actionsOverlay",
    category = CatalogCategory.Overlays,
    title = "Actions overlay",
    summary = "A grid of actions over the blurred screen",
    icon = R.drawable.ic_apps,
    keywords = listOf(
        "AMActionsOverlay", "AMActionGrid", "AMActionGridRow", "AMActionGridButton", "remote", "share", "blur",
        "blur-behind", "window", "menu", "tiles",
    ),
) { ActionsOverlayPage() }

@Composable
private fun ActionsOverlayPage() {
    ComponentPage(
        ActionsOverlayEntry,
        preview = { ShareActions() },
    ) {
        PhotoActions()
    }
}

/** Ways to send something on, two rows of three. */
@Composable
private fun ShareActions() {
    var showsActions by rememberSaveable { mutableStateOf(false) }
    val close = { showsActions = false }
    AMButton("Show actions", onClick = { showsActions = true }, modifier = Modifier.fillMaxWidth())
    AMActionsOverlay(showsActions, onDismissRequest = close) {
        AMActionGrid {
            AMActionGridRow(
                { Tile(R.drawable.ic_chat_bubble, "Message", close) },
                { Tile(R.drawable.ic_mail, "Mail", close) },
                { Tile(R.drawable.ic_link, "Copy link", close) },
            )
            AMActionGridRow(
                { Tile(R.drawable.ic_cast, "Cast", close) },
                { Tile(R.drawable.ic_download, "Save", close) },
                { Tile(R.drawable.ic_print, "Print", close) },
            )
        }
    }
}

/** A photo over its button; its actions come up over the blur, Favorite already on. */
@Composable
private fun PhotoActions() {
    var showsActions by rememberSaveable { mutableStateOf(false) }
    val close = { showsActions = false }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.lg)) {
        AMRemoteImage(
            catalogPhoto("orchard", 800, 692),
            contentDescription = "An orchard at dusk",
            modifier = Modifier.fillMaxWidth().height(320.dp).clip(MaterialTheme.shapes.large),
        )
        AMButton("Photo actions", onClick = { showsActions = true })
    }
    AMActionsOverlay(showsActions, onDismissRequest = close) {
        AMActionGrid {
            AMActionGridRow(
                { Tile(R.drawable.ic_favorite, "Favorite", close, active = true) },
                { Tile(R.drawable.ic_library_add, "Add to album", close) },
            )
            AMActionGridRow(
                { Tile(R.drawable.ic_crop, "Crop", close) },
                { Tile(R.drawable.ic_tune, "Adjust", close) },
                { Tile(R.drawable.ic_delete, "Delete", close, destructive = true) },
            )
        }
    }
}

/** A tile that does its stand-in action by closing the overlay. */
@Composable
private fun Tile(
    @DrawableRes icon: Int,
    title: String,
    onClick: () -> Unit,
    active: Boolean = false,
    destructive: Boolean = false,
) {
    AMActionGridButton(painterResource(icon), title, onClick = onClick, active = active, destructive = destructive)
}
