package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amSelectionCheck

internal val SelectionCheckEntry = ComponentEntry(
    id = "selectionCheck",
    category = CatalogCategory.Selection,
    title = "Selection check",
    summary = "Corner check mark for selectable tiles",
    icon = R.drawable.ic_check_circle,
    keywords = listOf(
        "amSelectionCheck", "AMSelectionMark", "check mark", "checkmark", "photo", "grid", "multi-select", "picker",
    ),
) { SelectionCheckPage() }

@Composable
private fun SelectionCheckPage() {
    ComponentPage(
        SelectionCheckEntry,
        preview = { MarkedTiles() },
    ) {
        PhotoPicker()
    }
}

/** Three photos, the first and last marked. */
@Composable
private fun MarkedTiles() {
    val shape = MaterialTheme.shapes.medium
    Row(horizontalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        listOf("selection-tile-1" to true, "selection-tile-2" to false, "selection-tile-3" to true).forEach { (seed, selected) ->
            AMRemoteImage(
                catalogPhoto(seed, 300),
                contentDescription = null,
                modifier = Modifier.size(TileSize).clip(shape).amSelectionCheck(selected),
            )
        }
    }
}

private val TileSize = 96.dp

private const val PhotoCount = 9

private const val PhotoColumns = 3

/** A photo picker: tap a photo to add it to or take it out of the selection. */
@Composable
private fun PhotoPicker() {
    var picked by rememberSaveable { mutableStateOf(setOf(1, 3, 4, 8)) }
    val gap = AMTheme.tokens.spacing.sm
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        (0 until PhotoCount).chunked(PhotoColumns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { index ->
                    val selected = index in picked
                    AMRemoteImage(
                        catalogPhoto("selection-picker-$index", 400),
                        contentDescription = "Photo ${index + 1}",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .toggleable(value = selected, role = Role.Checkbox) {
                                picked = if (selected) picked - index else picked + index
                            }
                            .amSelectionCheck(selected),
                    )
                }
            }
        }
    }
}
