package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMChipGroup
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val ChipGroupEntry = ComponentEntry(
    id = "chipGroup",
    category = CatalogCategory.Selection,
    title = "Chip group",
    summary = "Wrapping chips for one or many choices",
    icon = R.drawable.ic_label,
    keywords = listOf(
        "AMChipGroup", "AMFlowLayout", "FilterChip", "FlowRow", "chip", "filter", "tags", "multi-select",
        "single choice", "flow layout", "wrap",
    ),
) { ChipGroupPage() }

@Composable
private fun ChipGroupPage() {
    ComponentPage(
        ChipGroupEntry,
        preview = { GenreChips() },
    ) {
        PlaceFilters()
    }
}

private val Genres = listOf("Ambient", "Jazz", "Post-rock", "Electronic", "Folk", "Classical", "Hip-hop", "Soundtrack")

/** Pick any number of genres. */
@Composable
private fun GenreChips() {
    var picked by rememberSaveable { mutableStateOf(setOf("Jazz", "Electronic")) }
    AMChipGroup(Genres, label = { it }, selection = picked, onSelectionChange = { picked = it })
}

private val Cities = listOf("All", "Brooklyn", "Kyoto", "Lisbon", "Reykjavík")

private val Moods = listOf("Quiet", "Outdoors", "Late night", "Good coffee", "Live music")

private val MoodIcons = mapOf(
    "Quiet" to R.drawable.ic_eco,
    "Outdoors" to R.drawable.ic_light_mode,
    "Late night" to R.drawable.ic_dark_mode,
    "Good coffee" to R.drawable.ic_coffee,
    "Live music" to R.drawable.ic_music_note,
)

/** A place finder's filters: one city, then any moods, each with its glyph. */
@Composable
private fun PlaceFilters() {
    var city by rememberSaveable { mutableStateOf("All") }
    var moods by rememberSaveable { mutableStateOf(setOf("Quiet", "Good coffee")) }
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMChipGroup(Cities, label = { it }, selected = city, onSelectedChange = { city = it })
        AMDivider()
        AMChipGroup(
            Moods,
            label = { it },
            selection = moods,
            onSelectionChange = { moods = it },
            icon = { painterResource(MoodIcons.getValue(it)) },
        )
    }
}
