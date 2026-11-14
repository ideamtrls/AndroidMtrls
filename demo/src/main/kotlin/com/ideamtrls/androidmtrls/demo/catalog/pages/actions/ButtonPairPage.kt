package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonPair
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMTheme

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
        DownloadedAlbum()
    }
}

// Stand-ins for your own actions.
private fun play() {}

private fun shuffle() {}

private fun removeDownload() {}

/**
 * A downloaded album's title and actions: Play, and Remove download, whose label won't fit on one
 * line at half a phone's width, so the two stack.
 */
@Composable
private fun DownloadedAlbum() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column {
            Text("Field Recordings", style = MaterialTheme.typography.titleLarge)
            Text(
                "12 songs · Downloaded, 412 MB",
                style = MaterialTheme.typography.bodyMedium,
                color = AMTheme.secondaryText,
            )
        }
        AMButtonPair(
            { AMButton("Play", onClick = ::play, icon = painterResource(R.drawable.ic_play_arrow_filled)) },
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
