package io.github.jmsdnns.androidmtrls.demo.scenes.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.content.AMRemoteImage
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.content.AMSelectionCheckBox
import io.github.jmsdnns.androidmtrls.content.AMSkeleton
import io.github.jmsdnns.androidmtrls.content.AMSpinner
import io.github.jmsdnns.androidmtrls.content.amShimmer
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabClearance
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Constants — thumbnail / cover metrics, matching iOS.
// ---------------------------------------------------------------------------

/** Number of shimmering tiles the loading twin shows (iOS `gallerySkeletonTileCount`). */
private const val GALLERY_SKELETON_TILE_COUNT = 12

// ---------------------------------------------------------------------------
// GalleryPhotoGrid — shared 3-column thumbnail grid
// ---------------------------------------------------------------------------

/**
 * Shared 3-column thumbnail grid. In select mode taps toggle selection and
 * selected thumbnails carry an [AMSelectionCheckBox] mark; otherwise taps open
 * detail. Built from `chunked(3)` rows (not a `LazyVGrid`) so it composes inside a
 * `verticalScroll` column without an infinite-height crash. Each tile fills its
 * weighted cell as a square; the [AMRemoteImage] loads the picsum thumb and shows
 * an [AMSkeleton] rect while loading. Ports iOS `GalleryPhotoGrid`.
 */
@Composable
internal fun GalleryPhotoGrid(
    photos: List<GalleryPhoto>,
    isSelecting: Boolean = false,
    selectedIDs: Set<Int> = emptySet(),
    onTap: (GalleryPhoto) -> Unit,
) {
    val shape = RoundedCornerShape(AMTheme.Radius.md.dp)
    // One interaction source per cell, keyed on count so the list is stable across
    // recompositions (house rule: never `remember { … }` inside a variable-trip loop).
    val interactions = remember(photos.size) { List(photos.size) { MutableInteractionSource() } }

    Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
        photos.chunked(3).forEachIndexed { rowIndex, rowPhotos ->
            Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                rowPhotos.forEachIndexed { colIndex, photo ->
                    val interaction = interactions[rowIndex * 3 + colIndex]
                    BoxWithConstraints(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                        val side = maxWidth
                        AMSelectionCheckBox(
                            isSelected = isSelecting && selectedIDs.contains(photo.id),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape)
                                .clickable(
                                    interactionSource = interaction,
                                    indication = amRipple(),
                                    onClick = { onTap(photo) },
                                ),
                        ) {
                            AMRemoteImage(
                                url = photo.thumbUrl,
                                size = side,
                                cornerRadius = AMTheme.Radius.md.dp,
                                contentDescription = photo.title,
                                fallback = {
                                    AMSkeleton.rect(
                                        width = side,
                                        height = side,
                                        cornerRadius = AMTheme.Radius.md.dp,
                                    )
                                },
                            )
                        }
                    }
                }
                // Pad an incomplete last row so cells keep their square width.
                repeat(3 - rowPhotos.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// GalleryLibraryView — the month-grouped library root
// ---------------------------------------------------------------------------

/**
 * The Library root: page header + photo count, month-grouped 3-column grid, the
 * shimmering loading twin, an empty state when the library empties, and
 * pull-to-refresh (a `NestedScrollConnection` overscroll drag, mirroring the
 * Social feed) that reshuffles the photos. Ports iOS `GalleryLibraryView`.
 *
 * Month grouping iterates [galleryMonths], so a refresh shuffle reorders photos
 * within a month but never reorders the months themselves.
 */
@Composable
internal fun GalleryLibraryView(state: GalleryState) {
    val isDark = LocalAMIsDarkTheme.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val thresholdPx = with(density) { 72.dp.toPx() }

    var refreshing by remember { mutableStateOf(false) }
    var pullPx by remember { mutableFloatStateOf(0f) }

    // Overscroll-drag pull-to-refresh (spec-accepted stand-in for iOS `.refreshable`).
    val nestedConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0f && pullPx > 0f) {
                    val newPull = (pullPx + available.y).coerceAtLeast(0f)
                    val consumed = newPull - pullPx
                    pullPx = newPull
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!refreshing && source == NestedScrollSource.UserInput && available.y > 0f) {
                    pullPx += available.y * 0.5f
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!refreshing && pullPx > thresholdPx) {
                    refreshing = true
                    scope.launch {
                        state.refresh()
                        refreshing = false
                    }
                }
                pullPx = 0f
                return Velocity.Zero
            }
        }
    }

    val contentShift = if (refreshing) thresholdPx else pullPx

    Box(modifier = Modifier.fillMaxSize().nestedScroll(nestedConnection)) {
        if (refreshing || pullPx > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = AMTheme.Spacing.md.dp),
                contentAlignment = Alignment.Center,
            ) {
                AMSpinner(compact = true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, contentShift.roundToInt()) }
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(AMTheme.Spacing.lg.dp)
                .padding(bottom = sceneFabClearance()),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
        ) {
            // Header + count.
            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
                AMPageHeader("Library")
                if (!state.isLoading && state.photos.isNotEmpty()) {
                    BasicText(
                        text = galleryPhotoCount(state.photos.size),
                        style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }
            }

            when {
                state.isLoading -> LoadingTwin(state)

                state.photos.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = AMTheme.Spacing.xxl.dp)) {
                        AMEmptyState(
                            icon = Icons.Filled.PhotoLibrary,
                            title = "No photos",
                            description = "Photos you add appear here.",
                        )
                    }
                }

                else -> {
                    // Grouping ITERATES galleryMonths — months never reorder.
                    galleryMonths.forEach { month ->
                        val monthPhotos = state.photos.filter { it.month == month }
                        if (monthPhotos.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                                AMSectionHeader(month)
                                GalleryPhotoGrid(
                                    photos = monthPhotos,
                                    isSelecting = state.isSelecting,
                                    selectedIDs = state.selectedIDs,
                                    onTap = { state.handleLibraryTap(it) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loading twin: a dedicated neutral skeleton — an [AMSectionHeader] for the first
 * month over [GALLERY_SKELETON_TILE_COUNT] separator-wash tiles laid out in
 * [GalleryPhotoGrid]'s 3-column rhythm, all under one [amShimmer] sweep. The tiles
 * are plain filled shapes that never resolve a real image, so the placeholder
 * reads redacted even when Coil's thumbnail cache is already warm — the Android
 * twin of iOS's `.redacted(.placeholder)`. Alongside it a [LaunchedEffect] warms
 * Coil for the first month's thumbnails (without composing any image) so they are
 * ready the instant the real grid replaces this skeleton. Ports iOS
 * `GalleryLibraryView.loadingTwin`.
 */
@Composable
private fun LoadingTwin(state: GalleryState) {
    val isDark = LocalAMIsDarkTheme.current
    val context = LocalContext.current
    val wash = AMTheme.separator.resolve(isDark)
    val tileShape = RoundedCornerShape(AMTheme.Radius.md.dp)

    // Warm Coil for the first month's thumbnails without composing any image, so
    // they are cached by the time the real grid replaces this skeleton.
    LaunchedEffect(Unit) {
        val loader = SingletonImageLoader.get(context)
        state.photos.take(GALLERY_SKELETON_TILE_COUNT).forEach { photo ->
            loader.enqueue(ImageRequest.Builder(context).data(photo.thumbUrl).build())
        }
    }

    Box(modifier = Modifier.amShimmer()) {
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
            AMSectionHeader(galleryMonths[0])
            // Neutral tiles in the grid's 3-column rhythm — chunked into rows like
            // GalleryPhotoGrid so the placeholder keeps the same square cadence.
            List(GALLERY_SKELETON_TILE_COUNT) { it }.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp)) {
                    row.forEach { _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(wash, tileShape),
                        )
                    }
                    // Pad an incomplete last row so tiles keep their square width.
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// GalleryAlbumsView — the album-cards root
// ---------------------------------------------------------------------------

/**
 * The Albums root: a 2-column grid of album cards — Favorites (derived) first,
 * then the four manual albums. Empty albums show a muted glyph cover. In select
 * mode a manual card's tap toggles its membership in the selection (a
 * top-trailing mark shows the chosen ones); the derived Favorites card can't be
 * deleted, so it dims and ignores taps while selecting. Ports iOS
 * `GalleryAlbumsView`.
 */
@Composable
internal fun GalleryAlbumsView(state: GalleryState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(AMTheme.Spacing.lg.dp)
            .padding(bottom = sceneFabClearance()),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        AMPageHeader("Albums")

        // Favorites first, then the manual albums — chunked into 2-up rows.
        val favorites = state.favoritePhotos
        val cards = buildList {
            add(AlbumCardModel("Favorites", favorites.size, favorites.firstOrNull(), Icons.Filled.Favorite, isFavorites = true))
            state.albums.forEach { album ->
                add(AlbumCardModel(album.name, album.photoIDs.size, state.coverPhoto(album), Icons.Filled.Collections, isFavorites = false))
            }
        }

        cards.chunked(2).forEach { rowCards ->
            Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp)) {
                rowCards.forEach { card ->
                    AlbumCard(card = card, gallery = state, modifier = Modifier.weight(1f))
                }
                repeat(2 - rowCards.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/** The per-card display data. */
private data class AlbumCardModel(
    val name: String,
    val count: Int,
    val cover: GalleryPhoto?,
    val glyph: ImageVector,
    val isFavorites: Boolean,
)

@Composable
private fun AlbumCard(
    card: AlbumCardModel,
    gallery: GalleryState,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    // Favorites is derived and cannot be deleted, so while selecting it dims and
    // its taps do nothing; manual cards toggle selection while selecting and open
    // otherwise.
    val dimmed = card.isFavorites && gallery.isSelecting
    val showCheck = gallery.isSelecting && !card.isFavorites && gallery.selectedAlbumNames.contains(card.name)
    val interaction = remember(card.name) { MutableInteractionSource() }
    val cardShape = RoundedCornerShape(AMTheme.Radius.lg.dp)
    val coverShape = RoundedCornerShape(AMTheme.Radius.md.dp)

    AMCard(
        modifier = modifier.then(if (dimmed) Modifier.alpha(0.4f) else Modifier),
        padding = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clickable(
                    enabled = !dimmed,
                    interactionSource = interaction,
                    indication = amRipple(),
                    onClick = {
                        if (gallery.isSelecting) gallery.toggleAlbumSelection(card.name)
                        else gallery.openAlbum(card.name)
                    },
                )
                .padding(AMTheme.Spacing.md.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                val side = maxWidth
                AMSelectionCheckBox(
                    isSelected = showCheck,
                    modifier = Modifier.fillMaxSize().clip(coverShape),
                ) {
                    AMRemoteImage(
                        url = card.cover?.thumbUrl,
                        size = side,
                        cornerRadius = AMTheme.Radius.md.dp,
                        contentDescription = null,
                        fallbackIcon = card.glyph,
                    )
                }
            }
            BasicText(
                text = card.name,
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text = galleryPhotoCount(card.count),
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// GalleryAlbumGridView — the grid inside a pushed album panel
// ---------------------------------------------------------------------------

/**
 * The grid inside an opened album panel, with a per-album empty state
 * ("No favorites yet" for Favorites). Ports iOS `GalleryAlbumGridView`.
 */
@Composable
internal fun GalleryAlbumGridView(state: GalleryState, name: String) {
    val albumPhotos = if (name == "Favorites") state.favoritePhotos else state.photosInAlbum(name)

    if (albumPhotos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AMEmptyState(
                icon = if (name == "Favorites") Icons.Filled.FavoriteBorder else Icons.Filled.Collections,
                title = if (name == "Favorites") "No favorites yet" else "No photos",
                description = if (name == "Favorites") "Tap the heart on a photo." else null,
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AMTheme.Spacing.lg.dp)
                .padding(bottom = sceneFabClearance()),
        ) {
            GalleryPhotoGrid(
                photos = albumPhotos,
                onTap = { state.openDetail(albumPhotos, it.id) },
            )
        }
    }
}
