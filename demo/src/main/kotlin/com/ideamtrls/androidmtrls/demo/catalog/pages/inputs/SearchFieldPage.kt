package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.ideamtrls.androidmtrls.ProvideAMOnSurface
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val SearchFieldEntry = ComponentEntry(
    id = "searchField",
    category = CatalogCategory.Inputs,
    title = "Search field",
    summary = "Search input that filters as you type",
    icon = R.drawable.ic_search,
    keywords = listOf(
        "AMSearchField", "TextField", "card", "search", "filter", "query", "clear", "back",
    ),
) { SearchFieldPage() }

@Composable
private fun SearchFieldPage() {
    ComponentPage(
        SearchFieldEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { SearchStrip() },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.lg)) {
            FrostedCitySearch()
            CitySearch()
        }
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

/** A search field over a photo, the first few cities that match in a card under it. */
@Composable
private fun FrostedCitySearch() {
    val spacing = AMTheme.tokens.spacing
    val query = rememberTextFieldState()
    val text = query.text.toString().trim()
    CatalogFrostedStage {
        Column(Modifier.padding(spacing.lg).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
            AMSearchField(query, placeholder = "Search cities")
            CityCard(Cities.filter { it.contains(text, ignoreCase = true) }.take(3))
        }
    }
}

/** A search field over a card of cities that it filters as you type. */
@Composable
private fun CitySearch() {
    val query = rememberTextFieldState()
    val text = query.text.toString().trim()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        AMSearchField(query, placeholder = "Search cities")
        CityCard(Cities.filter { it.contains(text, ignoreCase = true) })
    }
}

/** The cities that match, one row each in a card; nothing while none do. */
@Composable
private fun CityCard(cities: List<String>) {
    if (cities.isEmpty()) return
    Column(Modifier.fillMaxWidth().amCard()) {
        ProvideAMOnSurface {
            cities.forEachIndexed { index, city ->
                if (index > 0) AMDivider(inset = AMTheme.tokens.spacing.lg)
                Text(city, Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
