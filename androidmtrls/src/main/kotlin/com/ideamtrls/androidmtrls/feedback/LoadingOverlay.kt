package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * A blocking progress overlay for work the user has to wait for: a save, an upload, an account
 * step. While [visible], a dialog window covers the screen with the platform's dim and holds a
 * card, centered, with Material's circular progress indicator over [message].
 *
 * ```kotlin
 * var saving by remember { mutableStateOf(false) }
 * AMButton("Save", onClick = { scope.launch { saving = true; save(); saving = false } })
 * AMLoadingOverlay(saving, message = "Saving…")
 * ```
 *
 * Nothing dismisses it but the caller: a tap outside does nothing and back is consumed, so the
 * user waits until [visible] turns false when the work is done. It's a modal window, so a screen
 * reader stays inside it and reads the card as one element. While it's up it counts as an overlay,
 * so the scene's floating action button fades out of its way.
 *
 * @param visible whether the overlay is up.
 * @param message what's happening, under the indicator ("Uploading photo…"). Null shows
 *   "Loading", so the indicator always has a caption.
 */
@Composable
public fun AMLoadingOverlay(
    visible: Boolean,
    message: String? = null,
) {
    ReportOverlayUp(visible)
    if (!visible) return
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        val spacing = AMTheme.tokens.spacing
        // The window fills the screen, so the card's shadow has room on every side.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.xl)
                    .amCard(padding = PaddingValues(spacing.lg))
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(
                    message ?: "Loading",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
