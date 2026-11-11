package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMMenuPicker
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val MenuPickerEntry = ComponentEntry(
    id = "menuPicker",
    category = CatalogCategory.Selection,
    title = "Menu picker",
    summary = "A row that opens its options in the spotlight",
    icon = R.drawable.ic_arrow_drop_down_circle,
    keywords = listOf(
        "AMMenuPicker", "picker", "menu", "dropdown", "options", "radio button", "RadioButton", "spotlight", "sort",
        "row",
    ),
) { MenuPickerPage() }

@Composable
private fun MenuPickerPage() {
    ComponentPage(
        MenuPickerEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { SortRow() },
    ) {
        PlaybackSettings()
    }
}

private val SortOrders = listOf("Newest", "Oldest", "A–Z")

/** One picker row on a surface strip. */
@Composable
private fun SortRow() {
    var sort by rememberSaveable { mutableStateOf("Newest") }
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        AMMenuPicker("Sort by", SortOrders, label = { it }, selected = sort, onSelectedChange = { sort = it })
    }
}

private val Qualities = listOf("Low", "Standard", "High")

private val SleepTimers = listOf("Off", "15 minutes", "30 minutes", "1 hour")

private val DownloadQualities = listOf("Standard", "High")

/** A card of playback settings: a picker with a glyph, a plain one and one that's locked. */
@Composable
private fun PlaybackSettings() {
    var quality by rememberSaveable { mutableStateOf("Standard") }
    var sleepTimer by rememberSaveable { mutableStateOf("Off") }
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMMenuPicker(
            "Streaming quality",
            Qualities,
            label = { it },
            selected = quality,
            onSelectedChange = { quality = it },
            icon = painterResource(R.drawable.ic_cell_tower),
        )
        AMDivider(inset = inset)
        AMMenuPicker("Sleep timer", SleepTimers, label = { it }, selected = sleepTimer, onSelectedChange = { sleepTimer = it })
        AMDivider(inset = inset)
        AMMenuPicker(
            "Download quality",
            DownloadQualities,
            label = { it },
            selected = "High",
            onSelectedChange = {},
            enabled = false,
        )
    }
}
