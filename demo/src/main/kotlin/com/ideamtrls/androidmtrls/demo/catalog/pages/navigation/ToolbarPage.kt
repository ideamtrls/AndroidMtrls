package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbar
import com.ideamtrls.androidmtrls.AMToolbarButton

internal val ToolbarEntry = ComponentEntry(
    id = "toolbar",
    category = CatalogCategory.Navigation,
    title = "Toolbar",
    summary = "A top bar of actions around a centered title",
    icon = R.drawable.ic_toolbar,
    keywords = listOf(
        "AMToolbar", "AMToolbarButton", "top app bar", "CenterAlignedTopAppBar", "TopAppBar", "header",
        "title bar", "navigation icon", "actions", "subtitle",
    ),
) { ToolbarPage() }

@Composable
private fun ToolbarPage() {
    ComponentPage(
        ToolbarEntry,
        previewLayout = StageLayout.FullBleed,
        loadedLayout = StageLayout.FullBleed,
        preview = {
            AMToolbar(
                "Inbox",
                Modifier.background(AMTheme.Surface.primary),
                navigationIcon = {
                    AMToolbarButton(painterResource(R.drawable.ic_menu), "Menu", onClick = ::openMenu)
                },
                actions = {
                    AMToolbarButton(painterResource(R.drawable.ic_edit_square), "Compose", onClick = ::compose)
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
            CatalogFrostedStage(Modifier.padding(horizontal = StageLayout.PageGutter)) {
                NowPlayingToolbar(Modifier.padding(vertical = AMTheme.tokens.spacing.xl))
            }
            NowPlayingToolbar(Modifier.background(AMTheme.Surface.primary))
        }
    }
}

/** A player's toolbar: back, the title over what's playing from, then search and more. */
@Composable
private fun NowPlayingToolbar(modifier: Modifier = Modifier) {
    AMToolbar(
        "Now playing",
        modifier,
        subtitle = "Recently added",
        navigationIcon = {
            AMToolbarButton(painterResource(R.drawable.ic_arrow_back), "Back", onClick = ::goBack)
        },
        actions = {
            AMToolbarButton(painterResource(R.drawable.ic_search), "Search", onClick = ::search)
            AMToolbarButton(painterResource(R.drawable.ic_more_vert), "More options", onClick = ::openMenu)
        },
        windowInsets = WindowInsets(0),
    )
}

// Stand-ins for your own actions.
private fun openMenu() {}

private fun compose() {}

private fun goBack() {}

private fun search() {}
