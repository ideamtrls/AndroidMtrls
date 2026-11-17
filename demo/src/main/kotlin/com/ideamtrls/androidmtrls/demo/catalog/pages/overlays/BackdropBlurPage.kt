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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtTone
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMBackdropBlur
import com.ideamtrls.androidmtrls.AMBlur
import com.ideamtrls.androidmtrls.AMSegmentedControl
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
        var blur by rememberSaveable { mutableStateOf(AMBlur.Mist) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
            BlurChoice(blur, onBlurChange = { blur = it })
            NowPlaying(blur)
        }
    }
}

/** The blurs a page's demo offers, the components' default first. */
private val BlurOptions = listOf(AMBlur.Mist, AMBlur.Frost)

/** Mist or Frost, for a page's blur demo. */
@Composable
internal fun BlurChoice(blur: AMBlur, onBlurChange: (AMBlur) -> Unit) {
    AMSegmentedControl(
        BlurOptions,
        label = { it.name },
        selected = blur,
        onSelectedChange = onBlurChange,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The picture, then the same picture blurred, side by side. */
@Composable
private fun CrispAndBlurred() {
    val art = CatalogArt.Garden.url(AMTheme.isDark)
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

/** A track's cover and title over the same art, blurred by [blur]. */
@Composable
private fun NowPlaying(blur: AMBlur) {
    val art = CatalogArt.Garden.url(AMTheme.isDark)
    val spacing = AMTheme.tokens.spacing
    AMBackdrop(
        art = { AMRemoteImage(art, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().height(280.dp).clip(MaterialTheme.shapes.large),
        // The photo is shown as it is, unwashed: its art text is white over it.
        tone = AMArtTone.Dark,
    ) {
        AMBackdropBlur(Modifier.matchParentSize(), blur)
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
                Text("The lanterns", Modifier.amArtText(secondary = true), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private val FrameSize = 140.dp
