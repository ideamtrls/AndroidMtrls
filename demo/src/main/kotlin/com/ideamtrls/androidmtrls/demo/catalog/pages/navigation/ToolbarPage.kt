package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
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
        AMToolbar(
            "Now playing",
            Modifier.background(AMTheme.Surface.primary),
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
}

// Stand-ins for your own actions.
private fun openMenu() {}

private fun compose() {}

private fun goBack() {}

private fun search() {}
