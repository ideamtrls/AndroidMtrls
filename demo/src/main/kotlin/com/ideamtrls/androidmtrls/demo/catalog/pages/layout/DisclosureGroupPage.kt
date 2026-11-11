package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMDisclosureGroup
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTheme

internal val DisclosureGroupEntry = ComponentEntry(
    id = "disclosureGroup",
    category = CatalogCategory.Layout,
    title = "Disclosure group",
    summary = "A card that folds open in place",
    icon = R.drawable.ic_expand_circle_down,
    keywords = listOf("AMDisclosureGroup", "disclosure", "folding", "expand", "collapse", "accordion", "expandable"),
) { DisclosureGroupPage() }

@Composable
private fun DisclosureGroupPage() {
    ComponentPage(
        DisclosureGroupEntry,
        preview = { AdvancedOptions() },
    ) {
        OrderDetails()
    }
}

/** A closed card of advanced options: a strict-mode switch inside. */
@Composable
private fun AdvancedOptions() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var strict by rememberSaveable { mutableStateOf(true) }
    AMDisclosureGroup("Advanced", expanded, onExpandedChange = { expanded = it }) {
        ListItem(
            checked = strict,
            onCheckedChange = { strict = it },
            trailingContent = { Switch(checked = strict, onCheckedChange = null) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text("Strict mode")
        }
    }
}

/** An order's details folded into cards: the summary open, billing and shipping closed. */
@Composable
private fun OrderDetails() {
    var showsSummary by rememberSaveable { mutableStateOf(true) }
    var showsBilling by rememberSaveable { mutableStateOf(false) }
    var showsShipping by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.md)) {
        AMDisclosureGroup("Order summary", showsSummary, onExpandedChange = { showsSummary = it }) {
            Detail("2 items, $48.00")
        }
        AMDisclosureGroup(
            "Billing",
            showsBilling,
            onExpandedChange = { showsBilling = it },
            icon = painterResource(R.drawable.ic_credit_card),
        ) {
            Detail("Your plan renews on the 1st of each month.")
        }
        AMDisclosureGroup("Shipping details", showsShipping, onExpandedChange = { showsShipping = it }) {
            Detail("Standard delivery takes three to five business days.")
        }
    }
}

@Composable
private fun Detail(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
