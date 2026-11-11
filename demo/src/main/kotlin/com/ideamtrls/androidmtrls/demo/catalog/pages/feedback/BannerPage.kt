package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMBanner
import com.ideamtrls.androidmtrls.AMBannerItem
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import kotlin.time.Duration.Companion.seconds

internal val BannerEntry = ComponentEntry(
    id = "banner",
    category = CatalogCategory.Feedback,
    title = "Banner",
    summary = "A status message at the top that leaves on its own",
    icon = R.drawable.ic_top_panel_open,
    keywords = listOf(
        "AMBanner", "AMBannerItem", "AMBannerHost", "notification", "status", "info", "success", "warning",
        "error", "transient", "swipe", "AMSemantic",
    ),
) { BannerPage() }

@Composable
private fun BannerPage() {
    ComponentPage(
        BannerEntry,
        preview = { ShowBanner() },
    ) {
        SuggestName()
    }
}

/** A button that shows a success banner. */
@Composable
private fun ShowBanner() {
    var banner by remember { mutableStateOf<AMBannerItem?>(null) }
    AMButton(
        "Show banner",
        onClick = {
            banner = AMBannerItem(
                "Changes saved",
                message = "Your profile is up to date.",
                style = AMSemantic.Success,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    AMBanner(banner, onDismissRequest = { banner = null })
}

/** A name field whose suggestion arrives as a banner; tapping the banner fills the field. */
@Composable
private fun SuggestName() {
    val spacing = AMTheme.tokens.spacing
    val name = rememberTextFieldState()
    var banner by remember { mutableStateOf<AMBannerItem?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            AMTextField(name, label = "Project name", placeholder = "Untitled")
        }
        AMButton(
            "Suggest a name",
            onClick = {
                banner = AMBannerItem(
                    "Try “Harbor Lights”",
                    message = "Tap to use this name.",
                    duration = 8.seconds,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AMBanner(
        banner,
        onDismissRequest = { banner = null },
        onClick = { name.setTextAndPlaceCursorAtEnd("Harbor Lights") },
    )
}
