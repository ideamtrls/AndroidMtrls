package io.github.jmsdnns.androidmtrls.demo.pages.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.github.jmsdnns.androidmtrls.collections.AMCarousel
import io.github.jmsdnns.androidmtrls.collections.AMPageIndicator
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
 * Catalog entry for [AMCarousel].
 *
 * id: "collections-carousel", section: [ComponentSection.Collections].
 */
val carouselEntry: ComponentEntry = ComponentEntry(
    id = "collections-carousel",
    section = ComponentSection.Collections,
    title = "Carousel",
    subtitle = "Index-driven horizontal pager",
    intro = "Horizontal pager driven by a current-index binding. Swipe between pages; " +
        "the current index updates as the user pages through.",
    icon = Icons.Default.ViewCarousel,
    content = { CarouselPage() },
)

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

private data class Story(
    val id: Int,
    val headline: String,
    val body: String,
    val tint: Color,
)

private val demoStories = listOf(
    Story(1, "Spring shipment",  "Twelve new colorways landed this morning.",          Color(0xFFFF2D55)), // pink
    Story(2, "Studio diary",     "Behind the prototypes we didn't keep.",              Color(0xFF5E5CE6)), // indigo
    Story(3, "Field notes",      "What testers said about the leather grain.",         Color(0xFF30B0C7)), // teal
    Story(4, "Atelier visit",    "Bench-side with the pattern team in Porto.",         Color(0xFFFF9500)), // orange
)

// ---------------------------------------------------------------------------
// CarouselPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMCarousel].
 *
 * One group per demo spec §3.9:
 *
 * - **Default** — 4 story cards at 180dp height with gradient backgrounds,
 *   an [AMPageIndicator] centered below, and a swipe-hint caption.
 *   The page index is remembered with [rememberSaveable].
 */
@Composable
fun CarouselPage() {

    // ── Default (interactive carousel) ──────────────────────────────────────
    catalogGroup(title = "Default") {
        val tokens = LocalAMTokens.current
        val isDark = LocalAMIsDarkTheme.current

        var currentIndex by rememberSaveable { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = tokens.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            // Carousel — 4 story cards at 180dp height matching iOS .frame(height: 180)
            AMCarousel(
                items = demoStories,
                key = { it.id },
                currentIndex = currentIndex,
                onIndexChange = { currentIndex = it },
                modifier = Modifier.height(180.dp),
            ) { story ->
                StoryCard(story = story)
            }

            // Page indicator — shares the same currentIndex
            AMPageIndicator(
                count = demoStories.size,
                currentIndex = currentIndex,
                modifier = Modifier.fillMaxWidth(),
            )

            // Swipe-hint caption
            BasicText(
                text = "Swipe horizontally; flick or drag past the threshold to page.",
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
// Private: StoryCard — gradient-backed card matching iOS StoryCard
// ---------------------------------------------------------------------------

@Composable
private fun StoryCard(story: Story) {
    val tokens = LocalAMTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(tokens.radius.lg))
            .background(
                Brush.linearGradient(
                    colors = listOf(story.tint, story.tint.copy(alpha = 0.55f)),
                ),
            )
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
    ) {
        BasicText(
            text = story.headline,
            style = TextStyle(
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        BasicText(
            text = story.body,
            style = TextStyle(
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 15.sp,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}
