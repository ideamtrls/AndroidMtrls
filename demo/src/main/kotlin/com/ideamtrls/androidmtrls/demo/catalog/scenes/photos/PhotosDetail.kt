package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMCarousel
import com.ideamtrls.androidmtrls.AMIconButton
import com.ideamtrls.androidmtrls.AMPanelChrome
import com.ideamtrls.androidmtrls.AMPausedScrim
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSkeleton
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbarButton
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amCrossfade
import com.ideamtrls.androidmtrls.amPausedBackdrop

/** The pages a detail last resolved to anything, and the page it showed, drawn while it pops. */
private class PhotosHeldPages(var pages: List<PhotosItem>, var index: Int)

/**
 * A photo's detail, pushed as a panel: a square carousel of the photos it opened with, edge to
 * edge, over a blurred copy of the photo showing, which fades to the next as you swipe. The bar is
 * titled by the photo's place, with Info at its end; a frosted caption card under the photo holds
 * its date and time and the favorite heart. Info, Favorite, Add to album and Delete are also on the
 * remote.
 *
 * The pages resolve against the live library, so a deleted photo drops out and the page lands on
 * its neighbor; the model pops the panel when the last one goes, and the detail keeps drawing the
 * pages it last resolved while it leaves, never blank.
 */
@Composable
internal fun PhotosDetail(model: PhotosModel, root: PhotosView, route: PhotosRoute.Detail) {
    val spacing = AMTheme.tokens.spacing
    val resolved = route.ids.mapNotNull(model::photo)
    val held = remember { PhotosHeldPages(resolved, model.detailIndex(root)) }
    val pages = resolved.ifEmpty { held.pages }
    val index = if (pages.isEmpty()) 0 else (if (resolved.isEmpty()) held.index else model.detailIndex(root)).coerceIn(0, pages.lastIndex)
    val current = pages.getOrNull(index)
    SideEffect {
        if (resolved.isNotEmpty()) {
            held.pages = resolved
            held.index = index
        }
    }

    AMBackdrop(
        art = {
            if (current != null) PhotosDetailBackdrop(current) else Box(Modifier.fillMaxSize().background(AMTheme.Surface.page))
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                AMPanelChrome(
                    current?.place?.title.orEmpty(),
                    onBack = { model.pop(root) },
                    actions = {
                        if (current != null) {
                            AMToolbarButton(painterResource(R.drawable.ic_info), "Info", onClick = { model.showInfo(current.id) })
                        }
                    },
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(top = spacing.md)) {
                AMCarousel(
                    pages,
                    currentIndex = index,
                    onCurrentIndexChange = { model.setDetailIndex(root, it) },
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    sideInset = 0.dp,
                ) { photo ->
                    AMRemoteImage(
                        photo.fullUrl,
                        contentDescription = photo.accessibilityName,
                        modifier = Modifier.fillMaxSize(),
                        fallback = { AMSkeleton.Rect(Modifier.fillMaxSize(), shape = RectangleShape) },
                    )
                }
                if (current != null) PhotosCaption(current, onFavorite = { model.toggleFavorite(listOf(current.id)) })
            }
        }
        if (model.isOnTop(root, route)) PhotosSheets(model)
    }
}

/**
 * The detail's backdrop: the photo showing, blurred and washed toward the page so it reads as the
 * page tinted by the photo. Swiping fades the next photo in under the steady blur.
 */
@Composable
private fun PhotosDetailBackdrop(photo: PhotosItem) {
    AMRemoteImage(
        photo.fullUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxSize().amPausedBackdrop(AMPausedScrim.Page).amCrossfade(photo.id),
        fallback = { Box(Modifier.fillMaxSize().background(AMTheme.Surface.page)) },
    )
}

/** The photo's date and time, with the heart at the end, on a frosted card under the photo. */
@Composable
private fun PhotosCaption(photo: PhotosItem, onFavorite: () -> Unit) {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier
            .padding(start = spacing.lg, top = spacing.md, end = spacing.lg)
            .fillMaxWidth()
            .amCard(
                fill = AMSurfaceFill.Frosted,
                padding = PaddingValues(start = spacing.lg, top = spacing.xs, end = spacing.xs, bottom = spacing.xs),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            photo.captionDate,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = AMTheme.Frosted.secondaryText,
        )
        AMIconButton(
            painterResource(if (photo.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite),
            contentDescription = if (photo.isFavorite) "Remove from Favorites" else "Add to Favorites",
            onClick = onFavorite,
        )
    }
}
