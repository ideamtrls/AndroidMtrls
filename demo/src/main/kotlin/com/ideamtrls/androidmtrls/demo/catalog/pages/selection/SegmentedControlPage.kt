package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSegmentedControl
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val SegmentedControlEntry = ComponentEntry(
    id = "segmentedControl",
    category = CatalogCategory.Selection,
    title = "Segmented control",
    summary = "Pick one of a few short options",
    icon = R.drawable.ic_view_week,
    keywords = listOf(
        "AMSegmentedControl", "SegmentedButton", "segmented button", "single choice", "filter", "range", "toggle",
    ),
) { SegmentedControlPage() }

@Composable
private fun SegmentedControlPage() {
    ComponentPage(
        SegmentedControlEntry,
        preview = { RangePicker() },
    ) {
        DocumentsFilter()
    }
}

// Stand-in for your own action.
private fun openDocument() {}

private val Ranges = listOf("Day", "Week", "Month")

/** Day, week or month. */
@Composable
private fun RangePicker() {
    var range by rememberSaveable { mutableStateOf("Week") }
    AMSegmentedControl(
        Ranges,
        label = { it },
        selected = range,
        onSelectedChange = { range = it },
        modifier = Modifier.fillMaxWidth(),
    )
}

private enum class DocumentScope(val title: String) {
    Mine("Mine"),
    Shared("Shared"),
    Archived("Archived"),
}

@Immutable
private class SampleDocument(val title: String, val detail: String, val scope: DocumentScope)

private val Documents = listOf(
    SampleDocument("Trip to Lisbon", "Edited today", DocumentScope.Mine),
    SampleDocument("Reading list", "Edited yesterday", DocumentScope.Mine),
    SampleDocument("Garden plan", "Edited Monday", DocumentScope.Mine),
    SampleDocument("Team offsite", "Shared by Ada Lovelace", DocumentScope.Shared),
    SampleDocument("Launch checklist", "Shared by Grace Hopper", DocumentScope.Shared),
    SampleDocument("2025 taxes", "Archived in March", DocumentScope.Archived),
)

/** A documents list filtered by whose they are. */
@Composable
private fun DocumentsFilter() {
    var scope by rememberSaveable { mutableStateOf(DocumentScope.Mine) }
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        AMSegmentedControl(
            DocumentScope.entries,
            label = { it.title },
            selected = scope,
            onSelectedChange = { scope = it },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(Modifier.fillMaxWidth().amCard()) {
            Documents.filter { it.scope == scope }.forEachIndexed { index, document ->
                if (index > 0) AMDivider(inset = spacing.lg)
                ListItem(
                    onClick = ::openDocument,
                    supportingContent = { Text(document.detail) },
                    colors = AMRowDefaults.colors(),
                    shapes = AMRowDefaults.shapes(),
                    contentPadding = AMRowDefaults.contentPadding,
                ) {
                    Text(document.title)
                }
            }
        }
    }
}
