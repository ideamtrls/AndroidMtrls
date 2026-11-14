package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtCard
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtBackdrop
import com.ideamtrls.androidmtrls.amArtText

internal val ArtCardEntry = ComponentEntry(
    id = "artCard",
    category = CatalogCategory.DataDisplay,
    title = "Art card",
    summary = "Cover art as a floating card",
    icon = R.drawable.ic_photo_album,
    keywords = listOf("AMArtCard", "cover", "album art", "artwork", "hero", "card", "shadow", "bitmap"),
) { ArtCardPage() }

@Composable
private fun ArtCardPage() {
    ComponentPage(
        ArtCardEntry,
        preview = { AMArtCard(catalogPhoto("mountain", 400), contentDescription = null, Modifier.size(160.dp)) },
    ) {
        AlbumHeader()
    }
}

/** The top of an album's page: its cover as an art card over its own art, its title and details under it. */
@Composable
private fun AlbumHeader() {
    val spacing = AMTheme.tokens.spacing
    val cover = catalogPhoto("orchard", 600)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .amArtBackdrop(cover),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            AMArtCard(cover, contentDescription = null, Modifier.size(200.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text("Orchard", Modifier.amArtText(), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    "Juniper Row · 9 songs · 38 min",
                    Modifier.amArtText(secondary = true),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
