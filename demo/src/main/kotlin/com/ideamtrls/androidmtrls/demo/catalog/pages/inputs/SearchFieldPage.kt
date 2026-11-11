package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMSearchField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val SearchFieldEntry = ComponentEntry(
    id = "searchField",
    category = CatalogCategory.Inputs,
    title = "Search field",
    summary = "Search input that filters as you type",
    icon = R.drawable.ic_search,
    keywords = listOf(
        "AMSearchField", "SearchBarDefaults", "InputField", "search", "filter", "query", "clear", "back",
    ),
) { SearchFieldPage() }

@Composable
private fun SearchFieldPage() {
    ComponentPage(
        SearchFieldEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { SearchStrip() },
    ) {
        CitySearch()
    }
}

private val Cities = listOf("Amsterdam", "Berlin", "Copenhagen", "Lisbon", "Oslo", "Prague")

/** An empty search field on a surface strip. */
@Composable
private fun SearchStrip() {
    val query = rememberTextFieldState()
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMSearchField(query)
    }
}

/** A card of cities under a search field that filters them as you type. */
@Composable
private fun CitySearch() {
    val spacing = AMTheme.tokens.spacing
    val query = rememberTextFieldState()
    val text = query.text.toString().trim()
    Column(Modifier.fillMaxWidth().amCard()) {
        AMSearchField(query, Modifier.padding(spacing.lg), placeholder = "Search cities")
        Cities.filter { it.contains(text, ignoreCase = true) }.forEach { city ->
            AMDivider(inset = spacing.lg)
            Text(city, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
