package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMPageIndicator
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import kotlinx.coroutines.launch

internal val PageIndicatorEntry = ComponentEntry(
    id = "pageIndicator",
    category = CatalogCategory.Navigation,
    title = "Page indicator",
    summary = "Dots showing the current page of paged content",
    icon = R.drawable.ic_more_horiz,
    keywords = listOf(
        "AMPageIndicator", "dots", "pager", "HorizontalPager", "pagination", "carousel", "onboarding",
    ),
) { PageIndicatorPage() }

@Composable
private fun PageIndicatorPage() {
    ComponentPage(
        PageIndicatorEntry,
        previewLayout = StageLayout.FullBleed,
        preview = {
            Box(
                Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(vertical = AMTheme.tokens.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                AMPageIndicator(count = 5, currentIndex = 2)
            }
        },
    ) {
        OnboardingPager()
    }
}

// Stand-in for your own action.
private fun finish() {}

private class OnboardingSlide(@DrawableRes val icon: Int, val title: String, val detail: String)

private val Slides = listOf(
    OnboardingSlide(R.drawable.ic_waving_hand, "Welcome", "Everything for the week, in one place."),
    OnboardingSlide(R.drawable.ic_calendar_today, "Plan your week", "Drop tasks onto the days they belong to."),
    OnboardingSlide(R.drawable.ic_notifications, "Stay on track", "A nudge the morning something is due."),
    OnboardingSlide(R.drawable.ic_group, "Share the load", "Invite others to a project and split it up."),
)

/**
 * An onboarding card: slides you swipe through, the dots under them, and Next, which turns into
 * Get started on the last slide.
 */
@Composable
private fun OnboardingPager() {
    val spacing = AMTheme.tokens.spacing
    val pagerState = rememberPagerState { Slides.size }
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == Slides.lastIndex
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(vertical = spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Every slide stays composed, so the pager is as tall as the tallest and never jumps.
        HorizontalPager(pagerState, Modifier.fillMaxWidth(), beyondViewportPageCount = Slides.lastIndex) { page ->
            SlideContent(Slides[page])
        }
        AMPageIndicator(count = Slides.size, currentIndex = pagerState.currentPage)
        AMButton(
            if (last) "Get started" else "Next",
            onClick = {
                if (last) finish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
        )
    }
}

@Composable
private fun SlideContent(slide: OnboardingSlide) {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(slide.icon),
            contentDescription = null,
            modifier = Modifier.padding(bottom = spacing.sm).size(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(slide.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            slide.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
