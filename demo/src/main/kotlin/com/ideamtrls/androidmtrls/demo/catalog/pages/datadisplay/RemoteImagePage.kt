package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMRemoteImageFallback
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val RemoteImageEntry = ComponentEntry(
    id = "remoteImage",
    category = CatalogCategory.DataDisplay,
    title = "Remote image",
    summary = "Cached web picture with a fallback",
    icon = R.drawable.ic_image,
    keywords = listOf(
        "AMRemoteImage", "AMRemoteImageFallback", "url", "photo", "picture", "thumbnail", "placeholder", "cache",
        "image pipeline",
    ),
) { RemoteImagePage() }

@Composable
private fun RemoteImagePage() {
    ComponentPage(
        RemoteImageEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewRemoteImage() },
    ) {
        TripAlbums()
    }
}

/** One picture with rounded corners, on a surface strip. */
@Composable
private fun PreviewRemoteImage() {
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMRemoteImage(catalogPhoto("mountain", 400), contentDescription = null, Modifier.size(160.dp).clip(MaterialTheme.shapes.medium))
    }
}

/**
 * A trip's albums: its cover, three photos (one not taken yet), then an album row with a
 * thumbnail and an empty album's row with its own fallback.
 */
@Composable
private fun TripAlbums() {
    val spacing = AMTheme.tokens.spacing
    val corners = MaterialTheme.shapes.medium
    Column(Modifier.fillMaxWidth().amCard()) {
        Column(Modifier.padding(spacing.lg), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            AMRemoteImage(
                CatalogArt.Peaks.url(AMTheme.isDark),
                contentDescription = "The trip's cover photo",
                Modifier.fillMaxWidth().height(160.dp).clip(corners),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                val tile = Modifier.weight(1f).aspectRatio(3f / 4f).clip(corners)
                AMRemoteImage(catalogPhoto("orchard", 300, 400), contentDescription = null, tile)
                AMRemoteImage(catalogPhoto("mountain", 300, 400), contentDescription = null, tile)
                AMRemoteImage(
                    null,
                    contentDescription = null,
                    tile,
                    fallback = { AMRemoteImageFallback(icon = painterResource(R.drawable.ic_landscape)) },
                )
            }
        }
        AMDivider(inset = spacing.lg)
        AMListRow(
            "Coast",
            subtitle = "24 photos",
            leadingContent = {
                AMRemoteImage(catalogPhoto("salt", 200, 200), contentDescription = null, Modifier.size(40.dp).clip(MaterialTheme.shapes.small))
            },
        )
        AMDivider(inset = spacing.lg)
        AMListRow(
            "Untitled album",
            subtitle = "No photos yet",
            leadingContent = {
                AMRemoteImage(
                    null,
                    contentDescription = null,
                    Modifier.size(40.dp).clip(MaterialTheme.shapes.small),
                    fallback = { AMRemoteImageFallback(icon = painterResource(R.drawable.ic_stacks)) },
                )
            },
        )
    }
}
