package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSectionHeader
import com.ideamtrls.androidmtrls.AMTheme

internal val SectionHeaderEntry = ComponentEntry(
    id = "sectionHeader",
    category = CatalogCategory.Foundations,
    title = "Section header",
    summary = "A sentence-case label over a group",
    icon = R.drawable.ic_title,
    keywords = listOf("AMSectionHeader", "subheader", "list subheader", "group title", "label", "titleSmall", "heading"),
) { SectionHeaderPage() }

@Composable
private fun SectionHeaderPage() {
    ComponentPage(
        SectionHeaderEntry,
        previewLayout = StageLayout.FullBleed,
        loadedLayout = StageLayout.FullBleed,
        preview = {
            AMSectionHeader("Recents", Modifier.background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg))
        },
    ) {
        LibrarySettings()
    }
}

// Stand-in for your own action.
private fun openPlaylist() {}

/**
 * A flat settings strip: a header with a glyph and a count over two rows, then a header over a
 * switch. Each header sits right on its rows, which bring their own space above their text.
 */
@Composable
private fun LibrarySettings() {
    val spacing = AMTheme.tokens.spacing
    var allowsAlerts by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(vertical = spacing.sm)) {
        AMSectionHeader(Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg)) {
            Icon(painterResource(R.drawable.ic_queue_music), contentDescription = null, Modifier.size(18.dp))
            Text("Playlists", Modifier.weight(1f))
            Text("12")
        }
        ListItem(
            onClick = ::openPlaylist,
            supportingContent = { Text("18 songs") },
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Morning light")
        }
        AMDivider(inset = spacing.lg)
        ListItem(
            onClick = ::openPlaylist,
            supportingContent = { Text("9 songs") },
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Harbor walk")
        }
        AMSectionHeader(
            "Notifications",
            Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.lg),
        )
        ListItem(
            checked = allowsAlerts,
            onCheckedChange = { allowsAlerts = it },
            trailingContent = { Switch(checked = allowsAlerts, onCheckedChange = null) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Allow alerts")
        }
    }
}
