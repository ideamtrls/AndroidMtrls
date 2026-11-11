package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.ideamtrls.androidmtrls.AMAlert
import com.ideamtrls.androidmtrls.AMAlertAction
import com.ideamtrls.androidmtrls.AMAlertItem
import com.ideamtrls.androidmtrls.AMFloatingActions
import com.ideamtrls.androidmtrls.AMScene
import com.ideamtrls.androidmtrls.AMToast

internal val PhotosSceneEntry = ComponentEntry(
    id = "photosScene",
    category = CatalogCategory.Showcase,
    title = "Photos",
    summary = "Browse, favorite, and organize photos",
    icon = R.drawable.ic_photo_library,
    keywords = listOf(
        "AMScene", "AMKeepsPlace", "AMPanel", "AMCarousel", "AMChipGroup", "amSelectionCheck", "AMReorderableCardList",
        "library", "albums", "grid", "select", "favorites", "page backdrop", "frosted", "sheet", "scene",
    ),
) { PhotosScene() }

/**
 * Room every Photos view leaves under its last row, so it scrolls clear of the floating action
 * button: the button's 56 dp, with `spacing.lg` under it and the same over it.
 */
internal val PhotosFabClearance = 88.dp

/**
 * Photos: a photo library. One [PhotosModel] owns the photos, the albums, navigation, select mode
 * and every overlay. The Library and Albums are the roots of the [AMScene], which the remote swaps
 * instantly; each root pushes its album and detail panels onto its own back stack of
 * [PhotosRoute]s, shown by a Navigation 3 `NavDisplay`.
 *
 * Back goes to the delete alert, then the remote, then a sheet, then select mode, then the root's
 * panels, then from Albums to the Library, then out of the scene.
 */
@Composable
internal fun PhotosScene() {
    val scope = rememberCoroutineScope()
    val saver = remember(scope) { PhotosModel.saver(scope) }
    val model = rememberSaveable(saver = saver) { PhotosModel(scope) }
    val navigator = LocalCatalogNavigator.current

    // The library lands a moment after the scene opens; leaving abandons the read.
    LaunchedEffect(model) { model.finishLoading() }
    DisposableEffect(model) { onDispose { model.cancelPendingWork() } }

    AMScene(
        current = model.rootView,
        landing = PhotosView.Library,
        onCurrentChange = { model.rootView = it },
        floatingActions = {
            AMFloatingActions(
                model.showsMenu,
                onExpandedChange = { if (it) model.openMenu() else model.closeMenu() },
                contentDescription = "Actions",
            ) {
                PhotosMenu(
                    model,
                    onExit = {
                        model.closeMenu()
                        model.cancelPendingWork()
                        navigator.back()
                    },
                )
            }
        },
        backStack = { root -> model.stack(root) },
    ) { root ->
        PhotosRoot(model, root)
        PhotosAlerts(model)
        AMToast(model.toast, onDismissRequest = { model.toast = null })
    }
}

/** A root view and the panels it pushes, on the root's own back stack. */
@Composable
private fun PhotosRoot(model: PhotosModel, root: PhotosView) {
    NavDisplay(
        backStack = model.stack(root),
        onBack = { model.pop(root) },
        entryProvider = { route ->
            when (route) {
                PhotosRoute.Root -> NavEntry(route) {
                    when (root) {
                        PhotosView.Library -> PhotosLibrary(model)
                        PhotosView.Albums -> PhotosAlbums(model)
                    }
                }
                is PhotosRoute.Album -> NavEntry(route) { PhotosAlbumPanel(model, root, route) }
                is PhotosRoute.Detail -> NavEntry(route) { PhotosDetail(model, root, route) }
            }
        },
    )
}

/**
 * The question asked before anything is deleted: "Delete photo?" / "Delete 3 photos?" or "Delete
 * album?", with Cancel and a destructive Delete. It covers whatever sheet is up.
 */
@Composable
private fun PhotosAlerts(model: PhotosModel) {
    val alert = model.alert
    val item = remember(alert) {
        when (alert) {
            is PhotosAlert.DeletePhotos -> {
                val single = alert.ids.size == 1
                AMAlertItem(
                    if (single) "Delete photo?" else "Delete ${alert.ids.size} photos?",
                    confirm = AMAlertAction("Delete", destructive = true) { model.delete(alert.ids) },
                    message = if (single) {
                        "It's removed from your library and every album."
                    } else {
                        "They're removed from your library and every album."
                    },
                    cancel = AMAlertAction("Cancel"),
                )
            }
            is PhotosAlert.DeleteAlbum -> AMAlertItem(
                "Delete album?",
                confirm = AMAlertAction("Delete", destructive = true) { model.deleteAlbum(alert.id) },
                message = "“${model.album(alert.id)?.name.orEmpty()}” is removed. Its photos stay in your library.",
                cancel = AMAlertAction("Cancel"),
            )
            null -> null
        }
    }
    AMAlert(item, onDismissRequest = { model.alert = null })
}
