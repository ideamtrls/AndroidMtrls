package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMLightStatusBarIcons
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCrossfade

/**
 * The Player root view, named by its cover and controls rather than a header. The current album's
 * cover, as art, fills the screen behind two cards: the controls card, placed with
 * [musicControlsPlacement] (the spot the remote shows the same card in), and the cover as a square
 * art card, centered in the room between the status bar and the controls and shrinking to fit on
 * short screens. Until the cover is read both show the album's project photo. The art and the card
 * dissolve when the album changes; songs on one album share them. The status bar's icons are light
 * over the art in both appearances ([AMLightStatusBarIcons]). The Queue sheet opens inside this art,
 * so its glass samples it.
 */
@Composable
internal fun MusicPlayerView(model: MusicModel) {
    val album = model.currentAlbum
    val cover = model.cover(album?.id)
    AMLightStatusBarIcons()
    AMBackdrop(art = { Box(Modifier.fillMaxSize().musicArtBackdrop(cover, album?.coverUrl)) }, Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().musicControlsPlacement()) {
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = AMTheme.tokens.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                MusicArtCard(
                    album?.coverUrl,
                    cover,
                    Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true).amCrossfade(album?.id),
                )
            }
            MusicControlsCard(model)
        }
        MusicQueueSheet(model)
    }
}
