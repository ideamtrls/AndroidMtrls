package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTheme

internal val PageHeaderEntry = ComponentEntry(
    id = "pageHeader",
    category = CatalogCategory.Foundations,
    title = "Page header",
    summary = "A large title that collapses as you scroll",
    icon = R.drawable.ic_web_asset,
    keywords = listOf(
        "AMPageHeader", "LargeTopAppBar", "top app bar", "title", "heading", "screen title",
        "scroll behavior", "collapsing", "navigation icon", "actions", "status bar", "scroll edge",
        "amStatusBarEdge",
    ),
) { PageHeaderPage() }

@Composable
private fun PageHeaderPage() {
    ComponentPage(
        PageHeaderEntry,
        previewLayout = StageLayout.FullBleed,
        loadedLayout = StageLayout.Fill,
        preview = {
            AMPageHeader("Library", Modifier.background(AMTheme.Surface.primary), windowInsets = WindowInsets(0))
        },
    ) {
        ReadingList()
    }
}

// Stand-in for your own action.
private fun openArticle() {}

/** The top of a reading list screen: the title with the list's count, over its first rows. */
@Composable
private fun ReadingList() {
    Column(Modifier.fillMaxWidth()) {
        AMPageHeader(windowInsets = WindowInsets(0)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Reading list", Modifier.weight(1f))
                Text(
                    "12",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        (1..4).forEach { number ->
            if (number > 1) AMDivider(inset = AMTheme.tokens.spacing.lg)
            ListItem(
                onClick = ::openArticle,
                supportingContent = { Text("${number + 3} min") },
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
            ) {
                Text("Article $number")
            }
        }
    }
}
