package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMListCard
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMRemoteImageFallback
import com.ideamtrls.androidmtrls.AMReorderableCardList
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbarButton

/** "1 album" / "n albums": the Albums count line. */
private fun photosAlbumCountLabel(count: Int): String = if (count == 1) "1 album" else "$count albums"

/**
 * The Albums root: the header and the album count over every album as a frosted card on the
 * latest photo's backdrop. A tap opens the album's panel; a press and hold (or a drag on a card's
 * handle) moves it, and the new order is the model's. The card list is as tall as its cards and
 * scrolls with the page, which leaves the floating action button its room. There's no select mode
 * here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotosAlbums(model: PhotosModel) {
    val spacing = AMTheme.tokens.spacing
    val albums = model.albums
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    PhotosPageBackdrop(model.latestPhoto) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            // With a scroll behavior it's the Library's two-row header, so the titles sit alike.
            topBar = { AMPageHeader("Albums", scrollBehavior = scrollBehavior) },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(top = spacing.sm, bottom = PhotosFabClearance),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                if (albums.isEmpty()) {
                    AMEmptyState(
                        painterResource(R.drawable.ic_photo_album),
                        "No albums",
                        Modifier.padding(start = spacing.lg, top = spacing.lg, end = spacing.lg),
                    )
                } else {
                    Text(
                        photosAlbumCountLabel(albums.size),
                        Modifier.padding(horizontal = spacing.lg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // The cards carry the page gutter as their own margins, so the list runs edge
                    // to edge; in the scrolling page it's as tall as its cards and scrolls with
                    // the page.
                    AMReorderableCardList(
                        albums,
                        key = { it.id },
                        onMove = model::moveAlbum,
                        modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
                    ) { album ->
                        PhotosAlbumCard(model, album)
                    }
                }
            }
        }
        if (model.isOnTop(PhotosView.Albums, PhotosRoute.Root)) PhotosSheets(model)
    }
}

/**
 * One album as a frosted card: its cover beside its name over its photo count, read as "Trips
 * 2026, 18 photos". The list offers TalkBack's Move up and Move down on it.
 */
@Composable
private fun PhotosAlbumCard(model: PhotosModel, album: PhotosAlbum) {
    AMListCard(
        leadingContent = { PhotosAlbumCover(model.cover(album), 56.dp, MaterialTheme.shapes.small) },
        supportingContent = { Text(photosCountLabel(album.photoIds.size), color = AMTheme.Frosted.secondaryText) },
        fill = AMSurfaceFill.Frosted,
        onClick = { model.openAlbum(album.id) },
    ) {
        Text(album.name)
    }
}

/**
 * An album's cover: its first photo, or the album glyph when it's empty. The album cards and the
 * Add to album sheet share it, so an album reads the same on both.
 */
@Composable
internal fun PhotosAlbumCover(photo: PhotosItem?, size: Dp, shape: Shape) {
    AMRemoteImage(
        photo?.thumbUrl,
        contentDescription = null,
        modifier = Modifier.size(size).clip(shape),
        fallback = { AMRemoteImageFallback(icon = painterResource(R.drawable.ic_photo_album)) },
    )
}

/** What a view showed last, kept to draw while it leaves after what it showed has gone. */
internal class PhotosLastSeen<T>(var value: T)

/** An album panel's selection, held to draw while Back pops the panel mid-selection. */
@Immutable
private data class PhotosHeldSelection(val title: String, val ids: Set<Int>)

/**
 * An album's panel, over a backdrop of its first photo: the photo count over the album's grid,
 * or "No photos" once it's empty. Its bar shows the album's name, or in select mode the
 * selection's title with Done, which ends it; back ends select mode before it pops the panel.
 *
 * It holds on to what it last showed while it pops: deleting the album pops the panel, which keeps
 * drawing the album (its photos stay in the library) instead of a blank title over "No photos";
 * popping it mid-selection ends the selection, and the panel keeps its title and checks until it's
 * gone.
 */
@Composable
internal fun PhotosAlbumPanel(model: PhotosModel, root: PhotosView, route: PhotosRoute.Album) {
    val spacing = AMTheme.tokens.spacing
    val live = model.album(route.id)
    val lastAlbum = remember { PhotosLastSeen(live) }
    val album = live ?: lastAlbum.value
    val photos = album?.let(model::photosIn).orEmpty()
    val pushed = route in model.stack(root)

    val liveSelection = if (model.selecting == PhotosSelectScope.Album(route.id)) {
        PhotosHeldSelection(model.selectionTitle, model.selectedIds)
    } else {
        null
    }
    val lastSelection = remember { PhotosLastSeen<PhotosHeldSelection?>(null) }
    val selection = liveSelection ?: if (pushed) null else lastSelection.value
    SideEffect {
        if (live != null) lastAlbum.value = live
        // Done clears it while the panel stays; a pop keeps it to draw on the way out.
        if (liveSelection != null || pushed) lastSelection.value = liveSelection
    }

    PhotosSelectBackHandler(liveSelection != null, model::endSelecting)

    val done: (@Composable RowScope.() -> Unit)? = if (selection != null) {
        { AMToolbarButton(painterResource(R.drawable.ic_check), "Done", onClick = model::endSelecting) }
    } else {
        null
    }

    PhotosPageBackdrop(photos.firstOrNull()) {
        // One bar in both modes: select mode changes its title and its end, never the panel.
        AMPanel(
            selection?.title ?: album?.name.orEmpty(),
            onBack = { model.pop(root) },
            actions = done,
            containerColor = Color.Transparent,
        ) { padding ->
            if (photos.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    AMEmptyState(
                        painterResource(R.drawable.ic_photo_album),
                        "No photos",
                        Modifier.padding(horizontal = spacing.lg),
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(PhotosGridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = photosGridPadding(padding),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                        PhotosCountLine(photos.size, Modifier.padding(bottom = spacing.md - spacing.sm))
                    }
                    photosTiles(
                        photos,
                        loading = false,
                        selecting = selection != null,
                        selectedIds = selection?.ids.orEmpty(),
                        onTap = { photo ->
                            if (liveSelection != null) model.toggleSelected(photo.id) else model.openDetail(photo.id, photos)
                        },
                    )
                }
            }
        }
        if (model.isOnTop(root, route)) PhotosSheets(model)
    }
}
