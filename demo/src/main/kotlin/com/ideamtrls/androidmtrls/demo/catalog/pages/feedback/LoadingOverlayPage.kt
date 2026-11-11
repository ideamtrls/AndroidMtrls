package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMLoadingOverlay
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal val LoadingOverlayEntry = ComponentEntry(
    id = "loadingOverlay",
    category = CatalogCategory.Feedback,
    title = "Loading overlay",
    summary = "A blocking progress card while work finishes",
    icon = R.drawable.ic_hourglass_empty,
    keywords = listOf(
        "AMLoadingOverlay", "spinner", "progress", "CircularProgressIndicator", "blocking", "saving",
        "uploading", "modal", "dialog", "wait",
    ),
) { LoadingOverlayPage() }

@Composable
private fun LoadingOverlayPage() {
    ComponentPage(
        LoadingOverlayEntry,
        preview = { ShowLoading() },
    ) {
        NewPost()
    }
}

// Stand-ins for your own async work.
private suspend fun work() = delay(2.seconds)

private suspend fun uploadPhoto() = delay(2.seconds)

/** A button that holds the loading overlay up while some work runs. */
@Composable
private fun ShowLoading() {
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    AMButton(
        "Show loading overlay",
        onClick = {
            scope.launch {
                working = true
                work()
                working = false
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    AMLoadingOverlay(working, message = "Loading…")
}

/** A photo post about to be shared: the upload holds the loading overlay up. */
@Composable
private fun NewPost() {
    val spacing = AMTheme.tokens.spacing
    val scope = rememberCoroutineScope()
    val caption = rememberTextFieldState("Morning light")
    var uploading by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        AMRemoteImage(
            CatalogArt.Forest.url(AMTheme.isDark),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large),
        )
        Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            AMTextField(caption, label = "Caption")
        }
        AMButton(
            "Share post",
            onClick = {
                scope.launch {
                    uploading = true
                    uploadPhoto()
                    uploading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AMLoadingOverlay(uploading, message = "Uploading photo…")
}
