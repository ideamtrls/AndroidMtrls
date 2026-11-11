package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMBackdrop
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMRemoteImage
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSurfaceFill
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val CardEntry = ComponentEntry(
    id = "card",
    category = CatalogCategory.Layout,
    title = "Card",
    summary = "The floating surface every container builds on",
    icon = R.drawable.ic_rectangle,
    keywords = listOf(
        "amCard", "amRowPadding", "AMRowDefaults", "surface", "container", "elevation", "padding", "row",
        "shadow", "AMSurfaceStyle", "nested", "fill", "frosted", "glass", "AMSurfaceFill",
    ),
) { CardPage() }

@Composable
private fun CardPage() {
    ComponentPage(
        CardEntry,
        preview = { ProjectSummary() },
    ) {
        ProjectDetail()
    }
}

/** A project's title over its status, one row in a card. */
@Composable
private fun ProjectSummary() {
    Column(Modifier.fillMaxWidth().amCard()) {
        TitledRow("Project Atlas", "12 tasks open, 3 due this week")
    }
}

/**
 * A project's page: a frosted card over the project's photo, its description in a card, and a
 * card of rows, a report's schedule over its switch.
 */
@Composable
private fun ProjectDetail() {
    val spacing = AMTheme.tokens.spacing
    var sendsReport by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        AMBackdrop(
            art = { AMRemoteImage(CatalogArt.Valley.url(AMTheme.isDark), contentDescription = null) },
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large),
        ) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(spacing.lg)
                    .fillMaxWidth()
                    .amCard(fill = AMSurfaceFill.Frosted),
            ) {
                TitledRow("Project Atlas", "Harbor survey · 12 tasks open", AMTheme.Frosted.secondaryText)
            }
        }
        Text(
            "Atlas keeps every survey, task and deadline in one place so nothing slips between site visits.",
            Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
            style = MaterialTheme.typography.bodyLarge,
        )
        Column(Modifier.fillMaxWidth().amCard()) {
            TitledRow("Weekly report", "Sent every Monday at 9:00")
            AMDivider(inset = spacing.lg)
            ListItem(
                checked = sendsReport,
                onCheckedChange = { sendsReport = it },
                trailingContent = { Switch(checked = sendsReport, onCheckedChange = null) },
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
            ) {
                Text("Send weekly report")
            }
        }
    }
}

/** A hand-built row on the row recipe: a title over a supporting line. */
@Composable
private fun TitledRow(title: String, supporting: String, supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Column(Modifier.amRowPadding()) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(supporting, style = MaterialTheme.typography.bodyMedium, color = supportingColor)
    }
}
