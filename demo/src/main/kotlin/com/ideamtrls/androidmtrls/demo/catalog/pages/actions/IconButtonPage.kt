package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMIconButton
import com.ideamtrls.androidmtrls.AMIconButtonSize
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val IconButtonEntry = ComponentEntry(
    id = "iconButton",
    category = CatalogCategory.Actions,
    title = "Icon button",
    summary = "A bare glyph with a 48 dp touch target",
    icon = R.drawable.ic_play_circle,
    keywords = listOf(
        "AMIconButton", "AMIconButtonSize", "IconButton", "IconToggleButton", "toggle", "checked",
        "glyph", "transport", "media", "badge", "BadgedBox", "tooltip", "TooltipBox", "long press",
    ),
) { IconButtonPage() }

@Composable
private fun IconButtonPage() {
    ComponentPage(
        IconButtonEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewControls() },
    ) {
        NowPlaying()
    }
}

// Stand-ins for your own actions.
private fun previous() {}

private fun play() {}

private fun pause() {}

private fun next() {}

private fun repeatOne() {}

private fun cast() {}

/** Transport controls on a surface strip. */
@Composable
private fun PreviewControls() {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMIconButton(painterResource(R.drawable.ic_skip_previous_filled), "Previous", onClick = ::previous, size = AMIconButtonSize.Large)
        AMIconButton(painterResource(R.drawable.ic_play_arrow_filled), "Play", onClick = ::play, size = AMIconButtonSize.Large)
        AMIconButton(painterResource(R.drawable.ic_skip_next_filled), "Next", onClick = ::next, size = AMIconButtonSize.Large)
    }
}

/**
 * A now-playing card: the track over its artist with casting unavailable, then shuffle (toggling),
 * the transport and repeat-one marked with a badge.
 */
@Composable
private fun NowPlaying() {
    val spacing = AMTheme.tokens.spacing
    var shuffles by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Harbor lights", style = MaterialTheme.typography.titleMedium)
                Text(
                    "The Tides",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AMIconButton(painterResource(R.drawable.ic_cast), "Cast unavailable", onClick = ::cast, enabled = false)
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AMIconButton(
                painterResource(if (shuffles) R.drawable.ic_shuffle_on else R.drawable.ic_shuffle),
                "Shuffle",
                checked = shuffles,
                onCheckedChange = { shuffles = it },
                size = AMIconButtonSize.Small,
            )
            AMIconButton(painterResource(R.drawable.ic_skip_previous_filled), "Previous", onClick = ::previous, size = AMIconButtonSize.Large)
            AMIconButton(painterResource(R.drawable.ic_pause_filled), "Pause", onClick = ::pause, size = AMIconButtonSize.Large)
            AMIconButton(painterResource(R.drawable.ic_skip_next_filled), "Next", onClick = ::next, size = AMIconButtonSize.Large)
            AMIconButton(
                painterResource(R.drawable.ic_repeat),
                "Repeat one",
                onClick = ::repeatOne,
                size = AMIconButtonSize.Small,
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = AMTheme.Surface.primary,
                    ) { Text("1") }
                },
            )
        }
    }
}
