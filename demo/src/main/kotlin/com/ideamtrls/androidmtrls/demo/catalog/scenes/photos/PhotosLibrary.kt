package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMChipGroup
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMSkeleton
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbarButton
import com.ideamtrls.androidmtrls.amPageBackdrop
import com.ideamtrls.androidmtrls.amSelectionCheck
import com.ideamtrls.androidmtrls.amShimmer

/**
 * The Library, the scene's landing view: its header (the light/dark chip at its end, or in select
 * mode the selection's title with Done), the photo count, the filter chips, then the filtered
 * photos in month sections, over a backdrop of the latest photo. A tap opens the detail paging the
 * filtered library, or in select mode checks the photo; back ends select mode first.
 *
 * While the library loads, everything under the header is that same layout under the skeleton's
 * shimmer: the count and month titles as bars, the photos as blocks, the chips inert, so nothing
 * moves when it lands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotosLibrary(model: PhotosModel) {
    val spacing = AMTheme.tokens.spacing
    val selecting = model.selecting == PhotosSelectScope.Library
    val loading = model.isLoading
    val photos = model.libraryPhotos
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    PhotosSelectBackHandler(selecting, model::endSelecting)

    PhotosPageBackdrop(model.latestPhoto) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AMPageHeader(
                    if (selecting) model.selectionTitle else "Library",
                    actions = {
                        // Select mode has no toolbar: Done holds the light/dark chip's place.
                        if (selecting) {
                            AMToolbarButton(painterResource(R.drawable.ic_check), "Done", onClick = model::endSelecting)
                        } else {
                            SceneAppearanceButton()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(PhotosGridColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = photosGridPadding(padding),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                item(key = "count", span = { GridItemSpan(maxLineSpan) }) {
                    PhotosCountLine(photos.size, loading = loading)
                }
                item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                    AMChipGroup(
                        model.filters,
                        label = { it.title },
                        selected = model.filter,
                        onSelectedChange = { model.filter = it },
                        modifier = Modifier
                            .padding(top = spacing.lg - spacing.sm)
                            .then(if (loading) Modifier.amShimmer() else Modifier),
                        enabled = !loading,
                    )
                }
                if (photos.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        val favorites = model.filter == PhotosFilter.Favorites
                        AMEmptyState(
                            painterResource(if (favorites) R.drawable.ic_favorite else R.drawable.ic_photo_library),
                            if (favorites) "No favorites" else "No photos",
                            Modifier.padding(top = spacing.lg),
                            description = if (favorites) {
                                "Photos you mark with the heart show up here."
                            } else {
                                "Photos you take show up here."
                            },
                        )
                    }
                } else {
                    model.libraryMonths.forEach { month ->
                        item(key = month.month.toString(), span = { GridItemSpan(maxLineSpan) }) {
                            AMSectionHeader(Modifier.padding(top = spacing.lg - spacing.sm)) {
                                if (loading) AMSkeleton.Text(month.title) else Text(month.title)
                            }
                        }
                        photosTiles(
                            month.photos,
                            loading = loading,
                            selecting = selecting,
                            selectedIds = model.selectedIds,
                            onTap = { photo ->
                                if (selecting) model.toggleSelected(photo.id) else model.openDetail(photo.id, model.libraryPhotos)
                            },
                        )
                    }
                }
            }
        }
        if (model.isOnTop(PhotosView.Library, PhotosRoute.Root)) PhotosSheets(model)
    }
}

/** The photo grid's columns. */
internal const val PhotosGridColumns = 3

/**
 * A photo grid's padding: the scaffold's (the header and the system bars) with the page gutter at
 * the sides, `spacing.sm` above the first line, and room for the floating action button below.
 */
@Composable
internal fun photosGridPadding(scaffold: PaddingValues): PaddingValues {
    val spacing = AMTheme.tokens.spacing
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = scaffold.calculateStartPadding(direction) + spacing.lg,
        top = scaffold.calculateTopPadding() + spacing.sm,
        end = scaffold.calculateEndPadding(direction) + spacing.lg,
        bottom = scaffold.calculateBottomPadding() + PhotosFabClearance,
    )
}

/**
 * "30 photos" under a grid's header, in the supporting color, or while [loading] its skeleton at
 * the same size. It's always laid out, so the chips under it hold still when a filter comes up
 * empty: at zero it keeps its height, shows nothing and says nothing to TalkBack.
 */
@Composable
internal fun PhotosCountLine(count: Int, modifier: Modifier = Modifier, loading: Boolean = false) {
    val style = MaterialTheme.typography.bodyMedium
    Box(modifier.fillMaxWidth()) {
        when {
            count == 0 -> Text(" ", Modifier.clearAndSetSemantics {}, style = style)
            loading -> AMSkeleton.Text(photosCountLabel(count), style = style)
            else -> Text(photosCountLabel(count), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The square tiles of a photo grid, three to a line. Each is the photo in the medium corners,
 * read by its place and date: a tap opens it, or in select mode checks it, and a checked tile
 * carries the selection mark. While loading they're shimmering blocks.
 */
internal fun LazyGridScope.photosTiles(
    photos: List<PhotosItem>,
    loading: Boolean,
    selecting: Boolean,
    selectedIds: Set<Int>,
    onTap: (PhotosItem) -> Unit,
) {
    items(photos, key = { it.id }) { photo ->
        PhotosTile(photo, loading, selecting, checked = selecting && photo.id in selectedIds, onTap = { onTap(photo) })
    }
}

@Composable
private fun PhotosTile(photo: PhotosItem, loading: Boolean, selecting: Boolean, checked: Boolean, onTap: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    if (loading) {
        AMSkeleton.Rect(Modifier.aspectRatio(1f), shape = shape)
        return
    }
    val name = photo.accessibilityName
    val action = if (selecting) {
        Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onTap() })
    } else {
        Modifier.clickable(onClick = onTap)
    }
    AMRemoteImage(
        photo.thumbUrl,
        contentDescription = null,
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(action)
            .semantics { contentDescription = name }
            .amSelectionCheck(checked),
    )
}

/**
 * The scene's background: a photo as a page backdrop, the art its frosted cards and sheets blur,
 * or the plain page with no photo.
 */
@Composable
internal fun PhotosPageBackdrop(photo: PhotosItem?, content: @Composable BoxScope.() -> Unit) {
    val url = photo?.thumbUrl
    AMBackdrop(art = { Box(Modifier.fillMaxSize().amPageBackdrop(url)) }, modifier = Modifier.fillMaxSize(), content = content)
}

/** While [selecting], back ends select mode before anything else on the view. */
@Composable
internal fun PhotosSelectBackHandler(selecting: Boolean, onEnd: () -> Unit) {
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = selecting,
        onBackCompleted = onEnd,
    )
}
