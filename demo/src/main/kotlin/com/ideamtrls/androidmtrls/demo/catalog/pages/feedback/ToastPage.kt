package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToast
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding
import kotlin.time.Duration.Companion.seconds

internal val ToastEntry = ComponentEntry(
    id = "toast",
    category = CatalogCategory.Feedback,
    title = "Toast",
    summary = "A brief note at the bottom for quick confirmations",
    icon = R.drawable.ic_bottom_panel_open,
    keywords = listOf(
        "AMToast", "AMToastItem", "AMToastHost", "snackbar", "Snackbar", "confirmation", "copied", "saved",
        "transient",
    ),
) { ToastPage() }

@Composable
private fun ToastPage() {
    ComponentPage(
        ToastEntry,
        preview = { ShowToast() },
    ) {
        QueueCard()
    }
}

/** A button that shows a toast. */
@Composable
private fun ShowToast() {
    var toast by remember { mutableStateOf<AMToastItem?>(null) }
    AMButton("Show toast", onClick = { toast = AMToastItem("Link copied") }, modifier = Modifier.fillMaxWidth())
    AMToast(toast, onDismissRequest = { toast = null })
}

/** Two tracks, and a button that adds them to the queue and confirms it with a toast. */
@Composable
private fun QueueCard() {
    val spacing = AMTheme.tokens.spacing
    var toast by remember { mutableStateOf<AMToastItem?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(Modifier.fillMaxWidth().amCard()) {
            TrackRow("Harbor Lights", "The Lanterns")
            AMDivider(inset = spacing.lg)
            TrackRow("Low Tide", "Marlow Fields")
        }
        AMButton(
            "Add to queue",
            onClick = { toast = AMToastItem("Added to queue", duration = 4.seconds) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AMToast(toast, onDismissRequest = { toast = null })
}

/** A track on the row recipe: its title over its artist. */
@Composable
private fun TrackRow(title: String, artist: String) {
    Column(Modifier.amRowPadding()) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
