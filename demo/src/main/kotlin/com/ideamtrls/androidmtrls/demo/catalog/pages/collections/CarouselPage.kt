package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMCarousel
import com.ideamtrls.androidmtrls.AMPageIndicator
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val CarouselEntry = ComponentEntry(
    id = "carousel",
    category = CatalogCategory.DataDisplay,
    title = "Carousel",
    summary = "Paging row with peeking neighbors",
    icon = R.drawable.ic_view_carousel,
    keywords = listOf(
        "AMCarousel", "AMPageIndicator", "pager", "paging", "currentIndex", "onCurrentIndexChange",
        "sideInset", "swipe", "HorizontalPager",
    ),
) { CarouselPage() }

@Composable
private fun CarouselPage() {
    ComponentPage(
        CarouselEntry,
        previewLayout = StageLayout.Wide,
        loadedLayout = StageLayout.Wide,
        preview = { PagedStories() },
    ) {
        SteppedStories()
    }
}

private class CollectionsStory(val title: String, val kicker: String, val photo: String)

/** Snowy mountains and forests by day. */
private val DayStories = listOf(
    CollectionsStory("Above the treeline", "Photo essay", "Cheops Mountain seen the Sir Donald Trail.jpg"),
    CollectionsStory("Evening in the forest", "Field notes", "Evening sunlight through the trees, Epping Forest - geograph.org.uk - 7865603.jpg"),
    CollectionsStory("Snow over the valley", "Travel", "Snowy Landscape - Ftan, Scuol.jpg"),
    CollectionsStory("First snow", "Field notes", "Sunlight in the snowy forest 1.jpg"),
)

/** New York and its jazz clubs by night. As many stories as by day, so the page holds across a switch. */
private val NightStories = listOf(
    CollectionsStory("Midtown after dark", "Photo essay", "New-York-City-night-skyline-September-2014.jpg"),
    CollectionsStory("Across the Hudson", "City walks", "Manhattan from Weehawken, NJ.jpg"),
    CollectionsStory("Late set at the Blue Note", "Live music", "Vladimir Cetkar prforming at Blue Note Jazz Club, NYC.jpg"),
    CollectionsStory("Coltrane at the Blue Note", "Live music", "Ravi Coltrane at the Blue Note, March 7, 2023-L1002389.jpg"),
)

/** The stories for the appearance showing. */
@Composable
private fun stories(): List<CollectionsStory> = if (AMTheme.isDark) NightStories else DayStories

/** A story card's photo, edge to edge across its top. */
private val StoryPhotoHeight = 120.dp

/** A story card's height: the photo over the kicker and the title in their padding, at the current text size. */
@Composable
private fun storyHeight(): Dp {
    val type = MaterialTheme.typography
    val text = with(LocalDensity.current) { type.bodySmall.lineHeight.toDp() + type.titleMedium.lineHeight.toDp() }
    return StoryPhotoHeight + AMTheme.tokens.spacing.md * 2 + text
}

/**
 * The carousel at its default spacing, over its dots. It runs edge to edge, so its side inset adds
 * the page gutter to the default one, and the next card peeks in from the screen's edge.
 */
@Composable
private fun PagedStories() {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val stories = stories()
    StoriesOverDots(stories.size, page) {
        AMCarousel(
            stories,
            currentIndex = page,
            onCurrentIndexChange = { page = it },
            modifier = Modifier.fillMaxWidth().height(storyHeight()),
            sideInset = StageLayout.PageGutter + AMTheme.tokens.spacing.lg,
        ) { story ->
            StoryCard(story)
        }
    }
}

/** The carousel with roomier insets and spacing, over its dots, edge to edge like the preview. */
@Composable
private fun SteppedStories() {
    val spacing = AMTheme.tokens.spacing
    var page by rememberSaveable { mutableIntStateOf(0) }
    val stories = stories()
    StoriesOverDots(stories.size, page) {
        AMCarousel(
            stories,
            currentIndex = page,
            onCurrentIndexChange = { page = it },
            modifier = Modifier.fillMaxWidth().height(storyHeight()),
            sideInset = StageLayout.PageGutter + spacing.xl,
            spacing = spacing.lg,
        ) { story ->
            StoryCard(story)
        }
    }
}

/** A carousel with its page dots under it. No card: the dots lie on the page. */
@Composable
private fun StoriesOverDots(count: Int, page: Int, carousel: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        carousel()
        AMPageIndicator(count = count, currentIndex = page)
    }
}

/** A story card: its photo across the top, its kicker over its title. */
@Composable
private fun StoryCard(story: CollectionsStory) {
    Column(Modifier.fillMaxSize().amCard().semantics(mergeDescendants = true) { }) {
        AMRemoteImage(commonsPhoto(story.photo), contentDescription = null, modifier = Modifier.fillMaxWidth().height(StoryPhotoHeight))
        Column(Modifier.padding(AMTheme.tokens.spacing.md)) {
            Text(
                story.kicker,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(story.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
