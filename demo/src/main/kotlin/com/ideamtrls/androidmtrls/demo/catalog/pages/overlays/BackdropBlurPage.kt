package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMBackdropBlur
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtText

internal val BackdropBlurEntry = ComponentEntry(
    id = "backdropBlur",
    category = CatalogCategory.Overlays,
    title = "Backdrop blur",
    summary = "A pure blur of the art behind, with no tint",
    icon = R.drawable.ic_blur_on,
    keywords = listOf("AMBackdropBlur", "AMBackdrop", "blur", "gaussian", "frosted", "radius", "now playing"),
) { BackdropBlurPage() }

@Composable
private fun BackdropBlurPage() {
    ComponentPage(
        BackdropBlurEntry,
        preview = { CrispAndBlurred() },
    ) {
        NowPlaying()
    }
}

/** The picture, then the same picture blurred, side by side. */
@Composable
private fun CrispAndBlurred() {
    val art = CatalogArt.Winter.url(AMTheme.isDark)
    val shape = MaterialTheme.shapes.small
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        AMRemoteImage(art, contentDescription = null, modifier = Modifier.size(FrameSize).clip(shape))
        AMBackdrop(
            art = { AMRemoteImage(art, contentDescription = null) },
            modifier = Modifier.weight(1f).height(FrameSize).clip(shape),
        ) {
            AMBackdropBlur(Modifier.matchParentSize())
        }
    }
}

/** A track's cover and title over the same art, blurred. */
@Composable
private fun NowPlaying() {
    val art = CatalogArt.Winter.url(AMTheme.isDark)
    val spacing = AMTheme.tokens.spacing
    AMBackdrop(
        art = { AMRemoteImage(art, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().height(280.dp).clip(MaterialTheme.shapes.large),
    ) {
        AMBackdropBlur(Modifier.matchParentSize())
        Column(
            Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AMRemoteImage(
                art,
                contentDescription = "Harbor lights cover",
                modifier = Modifier.size(FrameSize).clip(MaterialTheme.shapes.medium),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Harbor lights", Modifier.amArtText(), style = MaterialTheme.typography.titleMedium)
                Text("The lanterns", Modifier.amArtText(), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val FrameSize = 140.dp
