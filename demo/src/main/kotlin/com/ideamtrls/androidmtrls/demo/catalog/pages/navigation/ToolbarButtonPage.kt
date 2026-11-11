package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbar
import com.ideamtrls.androidmtrls.AMToolbarButton

internal val ToolbarButtonEntry = ComponentEntry(
    id = "toolbarButton",
    category = CatalogCategory.Navigation,
    title = "Toolbar button",
    summary = "A toolbar action, solid over a picture",
    icon = R.drawable.ic_radio_button_checked,
    keywords = listOf(
        "AMToolbarButton", "IconButton", "glyph", "toolbar", "top app bar", "action", "solid", "art",
        "backdrop", "destructive", "tooltip",
    ),
) { ToolbarButtonPage() }

@Composable
private fun ToolbarButtonPage() {
    ComponentPage(
        ToolbarButtonEntry,
        previewLayout = StageLayout.FullBleed,
        loadedLayout = StageLayout.FullBleed,
        preview = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(AMTheme.Surface.primary)
                    .padding(horizontal = AMTheme.tokens.spacing.xs, vertical = AMTheme.tokens.spacing.sm),
            ) {
                AMToolbarButton(painterResource(R.drawable.ic_edit_square), "Compose", onClick = ::compose)
            }
        },
    ) {
        PhotoDetail()
    }
}

// Stand-ins for your own actions.
private fun compose() {}

private fun goBack() {}

private fun share() {}

private fun delete() {}

private val Photo: String
    get() = catalogPhoto("brooklyn", 800, 600)

/**
 * The top of a photo's screen: a toolbar over the photo's blurred, washed colors, its buttons solid
 * (sharing unavailable, delete in the destructive color), over the photo itself.
 */
@Composable
private fun PhotoDetail() {
    AMBackdrop(art = { PhotoArt() }, Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            AMToolbar(
                "Brooklyn",
                navigationIcon = {
                    AMToolbarButton(painterResource(R.drawable.ic_arrow_back), "Back", onClick = ::goBack)
                },
                actions = {
                    AMToolbarButton(painterResource(R.drawable.ic_share), "Share", onClick = ::share, enabled = false)
                    AMToolbarButton(painterResource(R.drawable.ic_delete), "Delete photo", onClick = ::delete, destructive = true)
                },
                windowInsets = WindowInsets(0),
            )
            AMRemoteImage(Photo, contentDescription = null, Modifier.fillMaxWidth().aspectRatio(4f / 3f))
        }
    }
}

/** The photo behind the toolbar: heavily blurred under the frosted wash, so the title reads on it. */
@Composable
private fun PhotoArt() {
    AMRemoteImage(Photo, contentDescription = null, Modifier.fillMaxSize().blur(AMTheme.Frosted.blurRadius))
    Box(Modifier.fillMaxSize().background(AMTheme.Frosted.fill))
}
