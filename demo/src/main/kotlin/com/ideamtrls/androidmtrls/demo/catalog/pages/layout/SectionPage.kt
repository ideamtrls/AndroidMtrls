package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMTheme

internal val SectionEntry = ComponentEntry(
    id = "section",
    category = CatalogCategory.Layout,
    title = "Section",
    summary = "A titled group: header, card or flat block, footer",
    icon = R.drawable.ic_view_agenda,
    keywords = listOf("AMSection", "AMSectionStyle", "group", "header", "footer", "card", "flat", "rows", "inset", "settings"),
) { SectionPage() }

@Composable
private fun SectionPage() {
    ComponentPage(
        SectionEntry,
        previewLayout = StageLayout.FullBleed,
        preview = {
            AMSection(
                "Account",
                Modifier.background(AMTheme.Surface.primary).padding(vertical = AMTheme.tokens.spacing.md),
                footer = "Changes take effect at next sign-in.",
                style = AMSectionStyle.Flat,
            ) {
                AccountName()
            }
        },
    ) {
        WorkspaceSettings()
    }
}

/** The display name as a block: its label over its value. */
@Composable
private fun AccountName() {
    Column(verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xs)) {
        Text(
            "Display name",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Ada Lovelace", style = MaterialTheme.typography.bodyLarge)
    }
}

/** A workspace's settings: a card of switches with a footer, a card of members, an about block. */
@Composable
private fun WorkspaceSettings() {
    val spacing = AMTheme.tokens.spacing
    var pushAlerts by rememberSaveable { mutableStateOf(true) }
    var emailDigest by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xl)) {
        AMSection(
            "Notifications",
            footer = "Email digest sends one summary a day.",
            style = AMSectionStyle.Rows,
            inset = false,
        ) {
            SwitchRow("Push alerts", pushAlerts) { pushAlerts = it }
            AMDivider(inset = spacing.lg)
            SwitchRow("Email digest", emailDigest) { emailDigest = it }
        }
        AMSection("Team", style = AMSectionStyle.Rows, inset = false) {
            MemberRow("Ada Lovelace", "Owner")
            AMDivider(inset = spacing.lg)
            MemberRow("Grace Hopper", "Editor")
        }
        AMSection("About", elevation = AMElevation.None, inset = false) {
            Text(
                "Atlas keeps every project, task and deadline in one place so nothing slips between meetings.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(title)
    }
}

@Composable
private fun MemberRow(name: String, role: String) {
    ListItem(
        supportingContent = { Text(role) },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(name)
    }
}
