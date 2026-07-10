package io.github.jmsdnns.androidmtrls.demo.pages.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.Style
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMCardStack
import io.github.jmsdnns.androidmtrls.content.AMEmptyState
import io.github.jmsdnns.androidmtrls.content.AMEmptyStateAction
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMCardStack].
 *
 * id: "collections-cardstack", section: [ComponentSection.Collections].
 */
val cardStackEntry: ComponentEntry = ComponentEntry(
    id = "collections-cardstack",
    section = ComponentSection.Collections,
    title = "Card Stack",
    tileTitle = "Stack",
    subtitle = "Swipeable card deck",
    intro = "Tinder-style swipeable stack of cards. Drag horizontally; release past the " +
        "threshold to fling the top card off and reveal the next.",
    icon = Icons.Outlined.Style,
    content = { CardStackPage() },
)

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private data class Episode(
    val id: Int,
    val title: String,
    val subtitle: String,
    val tint: Color,
)

private fun initialDeck(): List<Episode> = listOf(
    Episode(1, "Episode 314", "Reasoning about reasoning",      Color(0xFF5E5CE6)), // indigo
    Episode(2, "Episode 313", "Cache-warm policies in practice", Color(0xFFFF2D55)), // pink
    Episode(3, "Episode 312", "What \"agentic\" actually means", Color(0xFF30B0C7)), // teal
    Episode(4, "Episode 311", "Long context vs. retrieval",      Color(0xFFFF9500)), // orange
    Episode(5, "Episode 310", "Speculative decoding, plain",     Color(0xFFBF5AF2)), // purple
)

// ---------------------------------------------------------------------------
// CardStackPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMCardStack].
 *
 * Group "Default": 5-card swipeable deck of episode cards with gradient
 * backgrounds. Swipe the top card left or right to dismiss it; when the
 * deck empties an empty state appears with a "Refill" action. A swipe-hint
 * caption sits below the stack, matching iOS placement.
 */
@Composable
fun CardStackPage() {

    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        val tokens = LocalAMTokens.current
        val isDark = LocalAMIsDarkTheme.current
        var deck by remember { mutableStateOf(initialDeck()) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.lg)
                .padding(vertical = tokens.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // ── Card stack or empty state — 340dp tall, matching iOS ZStack.frame(height: 340)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (deck.isEmpty()) {
                    // Empty state — shown once all cards are swiped away
                    AMEmptyState(
                        icon = Icons.Default.Inbox,
                        title = "Stack empty",
                        action = AMEmptyStateAction(
                            title = "Refill",
                            onClick = { deck = initialDeck() },
                        ),
                    )
                } else {
                    AMCardStack(
                        items = deck,
                        key = { it.id },
                        visibleDepth = 3,
                        onSwipe = { swiped, _ ->
                            deck = deck.filterNot { it.id == swiped.id }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                    ) { card ->
                        EpisodeCard(episode = card)
                    }
                }
            }

            // ── Swipe hint caption — unconditional sibling after the stack,
            // matching iOS placement (Text after ZStack, always visible).
            BasicText(
                text = "Drag a card horizontally; release past the threshold to fling.",
                style = TextStyle(
                    color = AMTheme.Text.secondary.resolve(isDark),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

}

// ---------------------------------------------------------------------------
// Private: EpisodeCard — gradient-backed card matching iOS EpisodeCard
// ---------------------------------------------------------------------------

@Composable
private fun EpisodeCard(episode: Episode) {
    val tokens = LocalAMTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(tokens.radius.lg))
            .background(
                Brush.linearGradient(
                    colors = listOf(episode.tint, episode.tint.copy(alpha = 0.55f)),
                ),
            )
            .padding(tokens.spacing.xl),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
        ) {
            BasicText(
                text = "#${episode.id}",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            BasicText(
                text = episode.title,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            BasicText(
                text = episode.subtitle,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 15.sp,
                ),
            )
        }
    }
}
