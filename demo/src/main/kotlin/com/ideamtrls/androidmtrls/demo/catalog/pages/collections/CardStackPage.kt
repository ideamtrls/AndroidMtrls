package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMCardStack
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amArtText
import com.ideamtrls.androidmtrls.amCard

internal val CardStackEntry = ComponentEntry(
    id = "cardStack",
    category = CatalogCategory.DataDisplay,
    title = "Card stack",
    summary = "Swipeable deck with peeking cards",
    icon = R.drawable.ic_auto_awesome_motion,
    keywords = listOf(
        "AMCardStack", "AMCardStackSwipeDirection", "swipe", "deck", "onSwipe", "currentIndex",
        "onCurrentIndexChange", "wraps", "visibleDepth", "photo",
    ),
) { CardStackPage() }

@Composable
private fun CardStackPage() {
    ComponentPage(CardStackEntry) {
        AlbumDeck()
    }
}

private class CollectionsAlbum(val id: Int, val title: String, val detail: String, val seed: String)

private val Albums = listOf(
    CollectionsAlbum(1, "Morning walk", "12 photos", "ramen"),
    CollectionsAlbum(2, "Long weekend", "48 photos", "tart"),
    CollectionsAlbum(3, "Field trip", "23 photos", "curry"),
    CollectionsAlbum(4, "Low tide", "9 photos", "focaccia"),
    CollectionsAlbum(5, "Night out", "31 photos", "gazpacho"),
)

/** The deck's height; every card fills it. */
private val DeckHeight = 280.dp

/**
 * The deck alone, with no buttons or dots: the swipe is the whole interaction. It opens on the
 * second album, so it steps both ways.
 */
@Composable
private fun AlbumDeck() {
    var index by rememberSaveable { mutableIntStateOf(1) }
    AMCardStack(
        Albums,
        currentIndex = index,
        onCurrentIndexChange = { index = it },
        modifier = Modifier.fillMaxWidth().height(DeckHeight),
        key = { it.id },
        visibleDepth = 4,
    ) { album ->
        AlbumCard(album)
    }
}

/** An album as a photo card: the picture fills the card, its title in art text on a dark fade at the bottom. */
@Composable
private fun AlbumCard(album: CollectionsAlbum) {
    val scrim = MaterialTheme.colorScheme.scrim
    Box(Modifier.fillMaxSize().amCard()) {
        AMRemoteImage(catalogPhoto(album.seed, 800, 680), contentDescription = null, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(scrim.copy(alpha = 0f), scrim.copy(alpha = 0.55f))))
                .padding(AMTheme.tokens.spacing.lg)
                .amArtText(),
        ) {
            Text(album.title, style = MaterialTheme.typography.titleLarge)
            Text(album.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
