package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMProgressView
import com.ideamtrls.androidmtrls.AMProgressViewStyle
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val ProgressEntry = ComponentEntry(
    id = "progress",
    category = CatalogCategory.Feedback,
    title = "Progress",
    summary = "A spinner or a linear bar, with its label",
    icon = R.drawable.ic_progress_activity,
    keywords = listOf(
        "AMProgressView", "AMProgressViewStyle", "Spinner", "Bar", "CircularProgressIndicator",
        "LinearProgressIndicator", "loading", "determinate", "indeterminate", "percent", "upload",
    ),
) { ProgressPage() }

@Composable
private fun ProgressPage() {
    ComponentPage(
        ProgressEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { MailProgress() },
    ) {
        SyncProgress()
    }
}

/** A spinner and a bar on a surface strip. */
@Composable
private fun MailProgress() {
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        AMProgressView(
            "Checking for new mail",
            Modifier.fillMaxWidth().padding(AMTheme.tokens.spacing.lg),
            style = AMProgressViewStyle.Spinner(card = false),
        )
        AMProgressView("Uploading", progress = { 0.6f }, valueLabel = "60%", style = AMProgressViewStyle.Bar)
    }
}

/** A sync card: three bars done, part way and waiting, then a spinner for the next check. */
@Composable
private fun SyncProgress() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth().amCard()) {
        AMProgressView("Photos", progress = { 1f }, valueLabel = "Done", style = AMProgressViewStyle.Bar)
        AMDivider(inset = spacing.lg)
        AMProgressView("Videos", progress = { 0.35f }, valueLabel = "35%", style = AMProgressViewStyle.Bar)
        AMDivider(inset = spacing.lg)
        AMProgressView("Documents", progress = { 0f }, valueLabel = "Waiting", style = AMProgressViewStyle.Bar)
        AMDivider(inset = spacing.lg)
        AMProgressView(
            "Checking for new files",
            Modifier.fillMaxWidth().padding(spacing.lg),
            style = AMProgressViewStyle.Spinner(card = false),
        )
    }
}
