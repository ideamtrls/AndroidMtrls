package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amSection

internal val SectionEntry = ComponentEntry(
    id = "section",
    category = CatalogCategory.Layout,
    title = "Section",
    summary = "A titled group: header, card or flat block, footer",
    icon = R.drawable.ic_view_agenda,
    keywords = listOf(
        "AMSection", "AMSectionStyle", "amSection", "group", "header", "footer", "card", "flat", "rows", "inset",
        "settings", "lazy", "LazyColumn", "long list", "episodes",
    ),
) { SectionPage() }

@Composable
private fun SectionPage() {
    ComponentPage(
        SectionEntry,
        previewLayout = StageLayout.FullBleed,
        preview = {
            AMSection(
                "Account",
                Modifier.background(AMTheme.Surface.primary).padding(vertical = AMTheme.tokens.spacing.md),
                footer = "Changes take effect at next sign-in.",
                style = AMSectionStyle.Flat,
            ) {
                AccountName()
            }
        },
        loadedLayout = StageLayout.Wide,
    ) {
        WorkspaceSettings()
    }
}

/** The display name as a block: its label over its value. */
@Composable
private fun AccountName() {
    Column(verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs)) {
        Text(
            "Display name",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Ada Lovelace", style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A workspace's settings: a card of switches with a footer, a card of members, an about block,
 * then a podcast's episodes as a lazy section in a list of its own height.
 */
@Composable
private fun WorkspaceSettings() {
    val spacing = AMTheme.tokens.spacing
    var pushAlerts by rememberSaveable { mutableStateOf(true) }
    var emailDigest by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
        CatalogFrostedStage(Modifier.padding(horizontal = StageLayout.PageGutter)) {
            AMSection("Notifications", Modifier.padding(spacing.lg).padding(top = spacing.sm), style = AMSectionStyle.Rows) {
                SwitchRow("Push alerts", pushAlerts) { pushAlerts = it }
                AMDivider(inset = spacing.lg)
                SwitchRow("Email digest", emailDigest) { emailDigest = it }
            }
        }
        AMSection(
            "Notifications",
            footer = "Email digest sends one summary a day.",
            style = AMSectionStyle.Rows,
        ) {
            SwitchRow("Push alerts", pushAlerts) { pushAlerts = it }
            AMDivider(inset = spacing.lg)
            SwitchRow("Email digest", emailDigest) { emailDigest = it }
        }
        AMSection("Team", style = AMSectionStyle.Rows) {
            MemberRow("Ada Lovelace", "Owner")
            AMDivider(inset = spacing.lg)
            MemberRow("Grace Hopper", "Editor")
        }
        AMSection("About", elevation = AMElevation.None) {
            Text(
                "Atlas keeps every project, task and deadline in one place so nothing slips between meetings.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Episodes()
    }
}

/**
 * A podcast's episodes, too many to compose at once: one lazy section in a list of its own height,
 * which composes only the rows on screen.
 */
@Composable
private fun Episodes() {
    val episodes = remember { podcastEpisodes(EpisodeCount) }
    LazyColumn(Modifier.fillMaxWidth().height(EpisodesHeight)) {
        amSection(
            episodes,
            key = { it.number },
            title = "Episodes",
            footer = "$EpisodeCount episodes, newest first.",
        ) { episode ->
            AMListRow(episode.title, subtitle = "Episode ${episode.number} · ${episode.minutes} min")
        }
    }
}

private class PodcastEpisode(val number: Int, val title: String, val minutes: Int)

/** [count] episodes, newest first, their titles cycling through a season's topics. */
private fun podcastEpisodes(count: Int): List<PodcastEpisode> = (count downTo 1).map { number ->
    PodcastEpisode(number, EpisodeTopics[number % EpisodeTopics.size], 28 + (number * 7) % 33)
}

private val EpisodeTopics = listOf(
    "Mapping the harbor floor",
    "A week without meetings",
    "The night shift at the observatory",
    "Small teams, long roadmaps",
    "Field notes from the valley",
    "What the tide tables know",
    "Writing for the second reader",
    "Building the quiet office",
    "Letters from the archive",
    "Rain, roofs and gutters",
    "The weekly review, revisited",
    "Ferries and timetables",
)

private const val EpisodeCount = 240
private val EpisodesHeight = 560.dp

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(title)
    }
}

@Composable
private fun MemberRow(name: String, role: String) {
    ListItem(
        supportingContent = { Text(role) },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(name)
    }
}
