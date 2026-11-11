package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTabs
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val TabsEntry = ComponentEntry(
    id = "tabs",
    category = CatalogCategory.Navigation,
    title = "Tabs",
    summary = "In-page tabs with crossfading content",
    icon = R.drawable.ic_tab,
    keywords = listOf(
        "AMTabs", "AMTabsScope", "SecondaryTabRow", "Tab", "LeadingIconTab", "tab row", "indicator",
        "switcher", "segments", "crossfade", "frosted",
    ),
) { TabsPage() }

@Composable
private fun TabsPage() {
    ComponentPage(
        TabsEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewTabs() },
    ) {
        LibraryTabs()
    }
}

/** A project's three tabs on a surface strip, each a line of text. */
@Composable
private fun PreviewTabs() {
    var tab by rememberSaveable { mutableStateOf("overview") }
    AMTabs(tab, onSelectionChange = { tab = it }, Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        tab("overview", "Overview") { PaneText("12 tasks open, 3 due this week.") }
        tab("activity", "Activity") { PaneText("Ada Lovelace closed 4 tasks today.") }
        tab("files", "Files") { PaneText("8 files, 24 MB.") }
    }
}

@Composable
private fun PaneText(text: String) {
    Text(text, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
}

private class LibraryItem(val title: String, val detail: String)

private val Photos = listOf(
    LibraryItem("Today", "36 photos"),
    LibraryItem("Yesterday", "112 photos"),
    LibraryItem("Last week", "100 photos"),
)

private val Albums = listOf(
    LibraryItem("Lisbon", "84 photos"),
    LibraryItem("Garden", "41 photos"),
    LibraryItem("Favorites", "17 photos"),
)

private val Shared = listOf(
    LibraryItem("Family", "Ada Lovelace and 3 others"),
    LibraryItem("Team offsite", "Grace Hopper"),
    LibraryItem("Book club", "Katherine Johnson and 5 others"),
)

// Stand-in for your own action.
private fun openItem() {}

/** A photo library in a card: Photos, Albums and Shared, each with its glyph and a list of rows. */
@Composable
private fun LibraryTabs() {
    var tab by rememberSaveable { mutableStateOf("albums") }
    AMTabs(tab, onSelectionChange = { tab = it }, Modifier.fillMaxWidth().amCard()) {
        tab("photos", "Photos", icon = { TabGlyph(R.drawable.ic_image) }) { LibraryRows(Photos) }
        tab("albums", "Albums", icon = { TabGlyph(R.drawable.ic_photo_album) }) { LibraryRows(Albums) }
        tab("shared", "Shared", icon = { TabGlyph(R.drawable.ic_group) }) { LibraryRows(Shared) }
    }
}

@Composable
private fun TabGlyph(@DrawableRes icon: Int) {
    Icon(painterResource(icon), contentDescription = null)
}

/** A tab's pane: a title and detail per row, a chevron at the end, dividers between. */
@Composable
private fun LibraryRows(items: List<LibraryItem>) {
    Column(Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            if (index > 0) AMDivider(inset = AMTheme.tokens.spacing.lg)
            ListItem(
                onClick = ::openItem,
                supportingContent = { Text(item.detail) },
                trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
            ) {
                Text(item.title)
            }
        }
    }
}
