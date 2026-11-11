package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val DividerEntry = ComponentEntry(
    id = "divider",
    category = CatalogCategory.Layout,
    title = "Divider",
    summary = "The line between rows, with an optional inset",
    icon = R.drawable.ic_horizontal_rule,
    keywords = listOf("AMDivider", "HorizontalDivider", "separator", "hairline", "inset", "rule", "line"),
) { DividerPage() }

@Composable
private fun DividerPage() {
    ComponentPage(
        DividerEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { Mailboxes() },
    ) {
        OrderTotal()
    }
}

/** Two flat rows on a surface strip, a full-width line between them. */
@Composable
private fun Mailboxes() {
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        Text("Inbox", Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
        AMDivider()
        Text("Archive", Modifier.amRowPadding(), style = MaterialTheme.typography.bodyLarge)
    }
}

/** An order's total: its title over a full-width line, then the amounts with inset lines between. */
@Composable
private fun OrderTotal() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        Column(Modifier.amRowPadding()) {
            Text("Order total", style = MaterialTheme.typography.bodyLarge)
            Text(
                "2 items · ships Friday",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AMDivider()
        AmountRow("Subtotal", "$44.00")
        AMDivider(inset = inset)
        AmountRow("Shipping", "$4.00")
        AMDivider(inset = inset)
        AmountRow("Total", "$48.00")
    }
}

/** A label with its amount at the end. */
@Composable
private fun AmountRow(label: String, amount: String) {
    ListItem(
        trailingContent = {
            Text(amount, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(label)
    }
}
