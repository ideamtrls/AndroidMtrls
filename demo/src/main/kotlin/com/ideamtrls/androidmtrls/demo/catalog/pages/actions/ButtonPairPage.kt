package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonPair
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.ProvideAMOnSurface
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val ButtonPairEntry = ComponentEntry(
    id = "buttonPair",
    category = CatalogCategory.Actions,
    title = "Button pair",
    summary = "Two buttons that split a row, or stack",
    icon = R.drawable.ic_view_column_2,
    keywords = listOf("AMButtonPair", "pair", "split", "stack", "side by side", "equal width", "font scale", "wrap"),
) { ButtonPairPage() }

@Composable
private fun ButtonPairPage() {
    ComponentPage(
        ButtonPairEntry,
        preview = {
            AMButtonPair(
                { AMButton("Play", onClick = ::play, icon = painterResource(R.drawable.ic_play_arrow_filled)) },
                {
                    AMButton(
                        "Shuffle",
                        onClick = ::shuffle,
                        icon = painterResource(R.drawable.ic_shuffle),
                        style = AMButtonStyle.Secondary(),
                    )
                },
            )
        },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
            CatalogFrostedStage {
                PlayAndQueue(Modifier.padding(AMTheme.tokens.spacing.lg))
            }
            DownloadedEpisode()
        }
    }
}

// Stand-ins for your own actions.
private fun play() {}

private fun shuffle() {}

private fun queue() {}

private fun share() {}

private fun removeDownload() {}

/** Play and Queue, which fit side by side. */
@Composable
private fun PlayAndQueue(modifier: Modifier = Modifier) {
    AMButtonPair(
        { AMButton("Play", onClick = ::play, icon = painterResource(R.drawable.ic_play_arrow_filled)) },
        {
            AMButton(
                "Queue",
                onClick = ::queue,
                icon = painterResource(R.drawable.ic_playlist_add),
                style = AMButtonStyle.Secondary(),
            )
        },
        modifier,
    )
}

/**
 * A downloaded episode in a card, then its actions: Play and Queue side by side, and Share with
 * Remove download, whose label won't fit on one line at half a phone's width, so those two stack.
 */
@Composable
private fun DownloadedEpisode() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Column(Modifier.fillMaxWidth().amCard()) {
            ProvideAMOnSurface {
                Column(Modifier.amRowPadding()) {
                    Text("The Long Way Round", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Episode 42 · 38 min · Downloaded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AMTheme.secondaryText,
                    )
                }
            }
        }
        PlayAndQueue()
        AMButtonPair(
            {
                AMButton(
                    "Share",
                    onClick = ::share,
                    icon = painterResource(R.drawable.ic_share),
                    style = AMButtonStyle.Secondary(),
                )
            },
            {
                AMButton(
                    "Remove download",
                    onClick = ::removeDownload,
                    icon = painterResource(R.drawable.ic_delete),
                    style = AMButtonStyle.Secondary(),
                    destructive = true,
                )
            },
        )
    }
}
