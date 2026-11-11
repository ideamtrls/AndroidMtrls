package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme

internal val ButtonStylesEntry = ComponentEntry(
    id = "buttonStyles",
    category = CatalogCategory.Actions,
    title = "Button styles",
    summary = "Primary, secondary and ghost buttons",
    icon = R.drawable.ic_smart_button,
    keywords = listOf(
        "AMButton", "AMButtonStyle", "Primary", "Secondary", "Ghost", "LocalAMButtonStyle", "Button",
        "TextButton", "call to action", "destructive", "disabled", "frosted", "glass", "AMSurfaceFill",
    ),
) { ButtonStylesPage() }

@Composable
private fun ButtonStylesPage() {
    ComponentPage(
        ButtonStylesEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewButtons() },
    ) {
        PlaylistActions()
    }
}

// Stand-ins for your own actions.
private fun next() {}

private fun saveDraft() {}

private fun cancel() {}

private fun play() {}

private fun shuffle() {}

private fun download() {}

private fun share() {}

private fun deletePlaylist() {}

/** The three styles on a surface strip, as a form's last step stacks them. */
@Composable
private fun PreviewButtons() {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AMButton("Continue", onClick = ::next, modifier = Modifier.fillMaxWidth())
        AMButton("Save draft", onClick = ::saveDraft, modifier = Modifier.fillMaxWidth(), style = AMButtonStyle.Secondary())
        AMButton("Cancel", onClick = ::cancel, style = AMButtonStyle.Ghost)
    }
}

/**
 * A playlist's actions: Play and Shuffle frosted over its picture, then Download (not available
 * offline yet) and Share, then Delete playlist.
 */
@Composable
private fun PlaylistActions() {
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AMBackdrop(
            art = { AMRemoteImage(CatalogArt.Mountain.url(AMTheme.isDark), contentDescription = null) },
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large),
        ) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                AMButton(
                    "Play",
                    onClick = ::play,
                    modifier = Modifier.weight(1f),
                    icon = painterResource(R.drawable.ic_play_arrow_filled),
                    style = AMButtonStyle.Primary(fill = AMSurfaceFill.Frosted),
                )
                AMButton(
                    "Shuffle",
                    onClick = ::shuffle,
                    modifier = Modifier.weight(1f),
                    icon = painterResource(R.drawable.ic_shuffle),
                    style = AMButtonStyle.Secondary(fill = AMSurfaceFill.Frosted),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            AMButton(
                "Download",
                onClick = ::download,
                modifier = Modifier.weight(1f),
                style = AMButtonStyle.Secondary(),
                enabled = false,
            )
            AMButton("Share", onClick = ::share, modifier = Modifier.weight(1f), style = AMButtonStyle.Secondary())
        }
        AMButton(
            "Delete playlist",
            onClick = ::deletePlaylist,
            modifier = Modifier.fillMaxWidth(),
            style = AMButtonStyle.Secondary(),
            destructive = true,
        )
    }
}
