package com.ideamtrls.androidmtrls.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMActionGrid
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow

/**
 * The remote's menu, for where the scene stood when it opened ([PhotosModel.menuState] is frozen
 * then, so the rows hold still while the menu fades out under the action it ran):
 *
 * - the Library: Select, Library, Albums, Exit;
 * - select mode: Add to album, Favorite / Select all (or Deselect all), Delete / the views row;
 * - the open detail: Info, Favorite / Add to album, Delete, for the photo it shows / the views row;
 * - an open album: Select, Delete album / the views row;
 * - Albums: the views row alone.
 *
 * The views row is Library, Albums and Exit, the view showing ringed.
 */
@Composable
internal fun PhotosMenu(model: PhotosModel, onExit: () -> Unit) {
    val state = model.menuState
    AMActionGrid {
        when (val context = state.context) {
            is PhotosMenuContext.Detail -> {
                AMActionGridRow(
                    { AMActionGridButton(painterResource(R.drawable.ic_info), "Info", onClick = { model.showInfo(context.photoId) }) },
                    { PhotosFavoriteTile(state, onClick = { model.toggleFavorite(listOf(context.photoId)) }) },
                )
                AMActionGridRow(
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_library_add),
                            "Add to album",
                            onClick = { model.presentAddToAlbum(listOf(context.photoId)) },
                            enabled = state.hasAlbums,
                        )
                    },
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_delete),
                            "Delete",
                            onClick = { model.confirmDelete(listOf(context.photoId)) },
                            destructive = true,
                        )
                    },
                )
                PhotosViewsRow(model, onExit)
            }
            PhotosMenuContext.Selecting -> {
                AMActionGridRow(
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_library_add),
                            "Add to album",
                            onClick = { model.presentAddToAlbum(model.orderedSelection) },
                            enabled = state.hasSelection && state.hasAlbums,
                        )
                    },
                    {
                        PhotosFavoriteTile(
                            state,
                            onClick = { model.toggleFavorite(model.orderedSelection) },
                            enabled = state.hasSelection,
                        )
                    },
                )
                AMActionGridRow(
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_check_circle_filled),
                            if (state.allSelected) "Deselect all" else "Select all",
                            onClick = model::toggleSelectAll,
                            enabled = state.hasShownPhotos,
                        )
                    },
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_delete),
                            "Delete",
                            onClick = { model.confirmDelete(model.orderedSelection) },
                            destructive = true,
                            enabled = state.hasSelection,
                        )
                    },
                )
                PhotosViewsRow(model, onExit)
            }
            PhotosMenuContext.Library -> AMActionGridRow(
                {
                    AMActionGridButton(
                        painterResource(R.drawable.ic_check_circle),
                        "Select",
                        onClick = { model.beginSelecting(PhotosSelectScope.Library) },
                        enabled = state.hasShownPhotos,
                    )
                },
                { PhotosViewTile(model, PhotosView.Library) },
                { PhotosViewTile(model, PhotosView.Albums) },
                { PhotosExitTile(onExit) },
            )
            is PhotosMenuContext.Album -> {
                AMActionGridRow(
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_check_circle),
                            "Select",
                            onClick = { model.beginSelecting(PhotosSelectScope.Album(context.id)) },
                            enabled = state.hasShownPhotos,
                        )
                    },
                    {
                        AMActionGridButton(
                            painterResource(R.drawable.ic_delete),
                            "Delete album",
                            onClick = { model.confirmDeleteAlbum(context.id) },
                            destructive = true,
                        )
                    },
                )
                PhotosViewsRow(model, onExit)
            }
            PhotosMenuContext.Albums -> PhotosViewsRow(model, onExit)
        }
    }
}

/** Library, Albums and Exit: the row every menu but the Library's ends in. */
@Composable
private fun PhotosViewsRow(model: PhotosModel, onExit: () -> Unit) {
    AMActionGridRow(
        { PhotosViewTile(model, PhotosView.Library) },
        { PhotosViewTile(model, PhotosView.Albums) },
        { PhotosExitTile(onExit) },
    )
}

/** A root view's tile, ringed while it shows: a tap closes the menu and swaps to it. */
@Composable
private fun PhotosViewTile(model: PhotosModel, view: PhotosView) {
    AMActionGridButton(
        painterResource(if (view == PhotosView.Library) R.drawable.ic_photo_library else R.drawable.ic_photo_album),
        if (view == PhotosView.Library) "Library" else "Albums",
        onClick = { model.switchTo(view) },
        active = model.rootView == view,
    )
}

/** Favorite, filled and ringed when every photo it acts on already is one. */
@Composable
private fun PhotosFavoriteTile(state: PhotosMenuState, onClick: () -> Unit, enabled: Boolean = true) {
    AMActionGridButton(
        painterResource(if (state.allFavorites) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
        "Favorite",
        onClick = onClick,
        active = state.allFavorites,
        enabled = enabled,
    )
}

@Composable
private fun PhotosExitTile(onExit: () -> Unit) {
    AMActionGridButton(painterResource(R.drawable.ic_logout), "Exit", onClick = onExit)
}
