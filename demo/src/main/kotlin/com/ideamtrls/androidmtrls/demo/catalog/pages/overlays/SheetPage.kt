package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
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
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMSheetSurface
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amDiscardGuard

internal val SheetEntry = ComponentEntry(
    id = "sheet",
    category = CatalogCategory.Overlays,
    title = "Sheet",
    summary = "A bottom sheet over an undimmed screen",
    icon = R.drawable.ic_bottom_sheets,
    keywords = listOf(
        "AMSheet", "AMSheetHeight", "AMSheetSurface", "amDiscardGuard", "bottom sheet", "modal", "drag handle",
        "title", "page", "frosted", "discard changes", "no dimming",
    ),
) { SheetPage() }

@Composable
private fun SheetPage() {
    ComponentPage(
        SheetEntry,
        preview = { EditProfile() },
    ) {
        NotificationSettings()
    }
}

/** A full-height sheet editing the display name, which asks before it drops an edit. */
@Composable
private fun EditProfile() {
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    AMButton("Show sheet", onClick = { editing = true }, modifier = Modifier.fillMaxWidth())
    AMSheet(editing, onDismissRequest = { editing = false }, title = "Edit profile") {
        ProfileForm(
            saved = name,
            onSave = {
                name = it
                editing = false
            },
        )
    }
}

/** The name in a card at the top, Save at the bottom of the sheet. */
@Composable
private fun ColumnScope.ProfileForm(saved: String, onSave: (String) -> Unit) {
    val spacing = AMTheme.tokens.spacing
    val draft = rememberTextFieldState(saved)
    Column(
        Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg)
            .amDiscardGuard(hasChanges = draft.text.toString() != saved),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            AMTextField(draft, label = "Display name")
        }
        Spacer(Modifier.weight(1f))
        AMButton("Save", onClick = { onSave(draft.text.toString()) }, modifier = Modifier.fillMaxWidth())
    }
}

/** Notification settings in a page-colored sheet: switches, then the delivery details. */
@Composable
private fun NotificationSettings() {
    var showsSettings by rememberSaveable { mutableStateOf(false) }
    var mentions by rememberSaveable { mutableStateOf(true) }
    var replies by rememberSaveable { mutableStateOf(true) }
    var followers by rememberSaveable { mutableStateOf(false) }
    val spacing = AMTheme.tokens.spacing

    AMButton("Notification settings", onClick = { showsSettings = true })
    AMSheet(
        showsSettings,
        onDismissRequest = { showsSettings = false },
        title = "Notifications",
        surface = AMSheetSurface.Page,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(Modifier.fillMaxWidth().amCard(elevation = AMElevation.None)) {
                SwitchRow("Mentions", mentions) { mentions = it }
                AMDivider(inset = spacing.lg)
                SwitchRow("Replies", replies) { replies = it }
                AMDivider(inset = spacing.lg)
                SwitchRow("New followers", followers) { followers = it }
            }
            Column(Modifier.fillMaxWidth().amCard(elevation = AMElevation.None)) {
                ValueRow("Quiet hours", "10 PM – 7 AM")
                AMDivider(inset = spacing.lg)
                ValueRow("Sound", "Chime")
                AMDivider(inset = spacing.lg)
                ValueRow("Badge", "Unread count")
            }
        }
    }
}

/** A setting with its switch; the whole row toggles. */
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

/** A setting with its current value at the end. */
@Composable
private fun ValueRow(label: String, value: String) {
    ListItem(
        trailingContent = {
            Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        colors = AMRowDefaults.colors(),
        shapes = AMRowDefaults.shapes(),
        contentPadding = AMRowDefaults.contentPadding,
    ) {
        Text(label)
    }
}
