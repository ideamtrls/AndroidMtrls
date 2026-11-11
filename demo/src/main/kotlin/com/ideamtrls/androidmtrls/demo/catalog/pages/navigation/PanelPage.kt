package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMArtPanelChrome
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMPanel
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbarButton
import com.ideamtrls.androidmtrls.amArtBackdrop
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amElevation
import com.ideamtrls.androidmtrls.amRowPadding

internal val PanelEntry = ComponentEntry(
    id = "panel",
    category = CatalogCategory.Navigation,
    title = "Panel",
    summary = "A pushed screen under its own top app bar",
    icon = R.drawable.ic_right_panel_open,
    keywords = listOf(
        "AMPanel", "AMPanelChrome", "AMArtPanelChrome", "Navigation 3", "NavDisplay", "NavEntry", "back stack",
        "push", "detail", "back", "predictive back", "top app bar", "TopAppBar", "art", "album", "frosted",
    ),
) { PanelPage() }

/** Each project row pushes its project's panel; the album row pushes the album over its art. */
@Composable
private fun PanelPage() {
    CatalogPanels(
        page = {
            ComponentPage(PanelEntry, preview = { ProjectList() }) {
                AlbumLink()
            }
        },
    ) { route ->
        when (route) {
            AlbumRoute -> AlbumPanel()
            else -> Projects.firstOrNull { it.id == route }?.let { ProjectPanel(it) }
        }
    }
}

private class Project(val id: String, val name: String, val summary: String, val tasks: List<String>)

private val Projects = listOf(
    Project("atlas", "Atlas", "3 tasks open, 1 due this week", listOf("Draft the launch plan", "Review the beta cutlist", "Book the venue")),
    Project("beacon", "Beacon", "2 tasks open, none due", listOf("Sketch the onboarding", "Pick a typeface")),
    Project("cinder", "Cinder", "1 task open, due today", listOf("Send the final invoice")),
)

/** Projects as rows in a card; a row pushes its project's panel onto the catalog's back stack. */
@Composable
private fun ProjectList() {
    val navigator = LocalCatalogNavigator.current
    Column(Modifier.fillMaxWidth().amCard()) {
        Projects.forEachIndexed { index, project ->
            if (index > 0) AMDivider(inset = AMTheme.tokens.spacing.lg)
            ListItem(
                onClick = { navigator.openPanel(PanelEntry, project.id) },
                supportingContent = { Text(project.summary) },
                trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
            ) {
                Text(project.name)
            }
        }
    }
}

/** A project's panel: its open tasks, with a share action in the bar. */
@Composable
private fun ProjectPanel(project: Project) {
    val navigator = LocalCatalogNavigator.current
    val spacing = AMTheme.tokens.spacing
    AMPanel(
        title = project.name,
        onBack = navigator::back,
        actions = {
            AMToolbarButton(painterResource(R.drawable.ic_share), contentDescription = "Share", onClick = ::share)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(vertical = spacing.lg),
        ) {
            AMSection("Open tasks", footer = project.summary, style = AMSectionStyle.Rows) {
                project.tasks.forEachIndexed { index, task ->
                    if (index > 0) AMDivider(inset = spacing.lg)
                    Text(task, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private const val AlbumRoute = "album"

private val AlbumCover: String
    get() = catalogPhoto("cobalt", 600)

private class Track(val title: String, val length: String)

private val AlbumTracks = listOf(
    Track("Low Tide", "4:12"), Track("Cobalt", "3:48"), Track("Salt Roads", "5:01"),
    Track("Paper Moon", "3:26"), Track("Northbound", "4:40"), Track("Glasswork", "3:55"),
    Track("Small Hours", "4:18"), Track("Undertow", "5:22"), Track("Field Notes", "3:59"),
)

/** The album as a row in a card; it pushes the album's page over its art. */
@Composable
private fun AlbumLink() {
    val navigator = LocalCatalogNavigator.current
    Column(Modifier.fillMaxWidth().amCard()) {
        ListItem(
            onClick = { navigator.openPanel(PanelEntry, AlbumRoute) },
            leadingContent = {
                AMRemoteImage(AlbumCover, contentDescription = null, Modifier.size(40.dp).clip(MaterialTheme.shapes.small))
            },
            supportingContent = { Text("Field Notes · 9 songs") },
            trailingContent = { Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Cobalt")
        }
    }
}

/**
 * The album's page over its art: the cover blurred and dimmed behind everything, the cover itself,
 * then its tracks in a card, under the art panel bar.
 */
@Composable
private fun AlbumPanel() {
    val navigator = LocalCatalogNavigator.current
    val spacing = AMTheme.tokens.spacing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    AMBackdrop(art = { Box(Modifier.fillMaxSize().amArtBackdrop(AlbumCover)) }, Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AMArtPanelChrome(
                    onBack = navigator::back,
                    title = "Cobalt",
                    subtitle = "Field Notes · 9 songs · 38 min",
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(start = spacing.lg, end = spacing.lg, top = spacing.md, bottom = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                AMRemoteImage(
                    AlbumCover,
                    contentDescription = null,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .amElevation(AMElevation.Low, MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large),
                )
                Column(Modifier.fillMaxWidth().amCard()) {
                    AlbumTracks.forEachIndexed { index, track ->
                        if (index > 0) AMDivider(inset = spacing.lg)
                        ListItem(
                            trailingContent = { Text(track.length) },
                            colors = AMRowDefaults.colors(),
                            shapes = AMRowDefaults.shapes(),
                            contentPadding = AMRowDefaults.contentPadding,
                        ) {
                            Text(track.title)
                        }
                    }
                }
            }
        }
    }
}

private fun share() {}
