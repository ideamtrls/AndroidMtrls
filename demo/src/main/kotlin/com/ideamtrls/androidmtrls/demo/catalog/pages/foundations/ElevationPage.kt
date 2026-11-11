package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amElevation

internal val ElevationEntry = ComponentEntry(
    id = "elevation",
    category = CatalogCategory.Foundations,
    title = "Elevation",
    summary = "Two-layer shadows cast by a shape",
    icon = R.drawable.ic_shadow,
    keywords = listOf("AMElevation", "amElevation", "amTextShadow", "shadow", "drop shadow", "dropShadow", "low", "medium", "none", "two-layer"),
) { ElevationPage() }

@Composable
private fun ElevationPage() {
    ComponentPage(
        ElevationEntry,
        preview = { ElevationLevels() },
    ) {
        Albums()
    }
}

// Stand-in for your own action.
private fun pause() {}

/** The surface levels side by side, highest first. */
@Composable
private fun ElevationLevels() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl),
    ) {
        LevelChip("Medium", AMElevation.Medium)
        LevelChip("Low", AMElevation.Low)
        LevelChip("None", AMElevation.None)
    }
}

@Composable
private fun LevelChip(name: String, level: AMElevation) {
    val shape = MaterialTheme.shapes.medium
    Box(
        Modifier
            .defaultMinSize(96.dp, 64.dp)
            .amElevation(level, shape)
            .background(AMTheme.Surface.primary, shape)
            .padding(AMTheme.tokens.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = MaterialTheme.typography.labelLarge)
    }
}

/** Two album tiles at the low level, then the playing track's card higher, at medium. */
@Composable
private fun Albums() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            AlbumTile("Morning light", "Iris Vale", Modifier.weight(1f))
            AlbumTile("Harbor walk", "The Quays", Modifier.weight(1f))
        }
        ListItem(
            modifier = Modifier.fillMaxWidth().amCard(elevation = AMElevation.Medium),
            leadingContent = { Icon(painterResource(R.drawable.ic_music_note), contentDescription = null) },
            trailingContent = {
                IconButton(onClick = ::pause) {
                    Icon(painterResource(R.drawable.ic_pause), contentDescription = "Pause")
                }
            },
            supportingContent = { Text("Now playing") },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Night market")
        }
    }
}

/** A tile whose shadow comes from its shape, never from its text. */
@Composable
private fun AlbumTile(title: String, artist: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .heightIn(min = 96.dp)
            .amElevation(AMElevation.Low, shape)
            .background(AMTheme.Surface.primary, shape)
            .padding(AMTheme.tokens.spacing.lg),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
