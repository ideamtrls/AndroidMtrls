package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMEmptyState
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSearchField
import com.ideamtrls.androidmtrls.AMTextFieldAutofocus
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/**
 * The Search sheet's content, under the sheet's "Search" title: the search field, focused as the
 * sheet opens, over the matching products, every product while the query is blank, so every row
 * works without typing. A row closes the sheet and shows its product. The results' card is as tall
 * as its rows up to the room left, and scrolls inside past that, so it always ends in its own
 * corners.
 */
@Composable
internal fun ColumnScope.ShopSearchSheet(model: ShopModel) {
    val spacing = AMTheme.tokens.spacing
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            AMSearchField(model.searchQuery, placeholder = "Search coffee and gear", autofocus = AMTextFieldAutofocus.CaretAtEnd)
        }
        val results = model.searchResults
        if (results.isEmpty()) {
            AMEmptyState(
                painterResource(R.drawable.ic_search),
                "No matches",
                description = "Nothing matches “${model.searchQuery.text.trim()}”. Try an origin or a tasting note.",
            )
        } else {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .amCard()
                    .verticalScroll(rememberScrollState()),
            ) {
                results.forEach { product ->
                    AMListRow(
                        product.name,
                        subtitle = product.searchLine,
                        leadingContent = { ShopLabelArt(product, ShopLabelStyle.Thumb) },
                        onClick = { model.openSearchResult(product.id) },
                    )
                }
            }
        }
    }
}
