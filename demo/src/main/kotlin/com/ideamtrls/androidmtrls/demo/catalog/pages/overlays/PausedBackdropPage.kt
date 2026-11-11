package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMPausedScrim
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amPausedBackdrop

internal val PausedBackdropEntry = ComponentEntry(
    id = "pausedBackdrop",
    category = CatalogCategory.Overlays,
    title = "Paused backdrop",
    summary = "Blur and scrim that still a picture",
    icon = R.drawable.ic_pause_presentation,
    keywords = listOf("amPausedBackdrop", "AMPausedScrim", "modal", "page", "blur", "scrim", "wash", "photo detail"),
) { PausedBackdropPage() }

@Composable
private fun PausedBackdropPage() {
    ComponentPage(
        PausedBackdropEntry,
        preview = { Paused() },
    ) {
        PhotoDetail()
    }
}

/** The photo both demos still, picked when the page shows (it follows the artwork extra). */
private fun photo(): String = catalogPhoto("orchard", 600)

/** A photo stilled with the modal recipe, a word over it. */
@Composable
private fun Paused() {
    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        AMRemoteImage(photo(), contentDescription = null, modifier = Modifier.matchParentSize().amPausedBackdrop())
        Text("Paused", Modifier.amArtText(), style = MaterialTheme.typography.titleLarge)
    }
}

/** A photo's detail: the photo itself over its own page wash. */
@Composable
private fun PhotoDetail() {
    val spacing = AMTheme.tokens.spacing
    Box(
        Modifier.fillMaxWidth().height(280.dp).clip(MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        AMRemoteImage(
            photo(),
            contentDescription = null,
            modifier = Modifier.matchParentSize().amPausedBackdrop(AMPausedScrim.Page),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AMRemoteImage(
                photo(),
                contentDescription = "Orchard at dusk",
                modifier = Modifier.size(160.dp).clip(MaterialTheme.shapes.medium),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Orchard at dusk", style = MaterialTheme.typography.titleMedium)
                Text(
                    "October 12 · Hudson Valley",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
