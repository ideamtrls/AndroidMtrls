package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToggle
import com.ideamtrls.androidmtrls.amCard

internal val ToggleEntry = ComponentEntry(
    id = "toggle",
    category = CatalogCategory.Inputs,
    title = "Toggle",
    summary = "Settings row with a switch at its end",
    icon = R.drawable.ic_toggle_on,
    keywords = listOf("AMToggle", "Switch", "ListItem", "switch", "settings", "preference", "on", "off"),
) { TogglePage() }

@Composable
private fun TogglePage() {
    ComponentPage(
        ToggleEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { NotificationsToggle() },
    ) {
        NotificationSettings()
    }
}

/** One switch row on a surface strip. */
@Composable
private fun NotificationsToggle() {
    var on by rememberSaveable { mutableStateOf(true) }
    AMToggle("Notifications", on, onCheckedChange = { on = it }, modifier = Modifier.background(AMTheme.Surface.primary))
}

/** Notification settings: two switches, one with a supporting line, and one the plan locks on. */
@Composable
private fun NotificationSettings() {
    var push by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf(false) }
    var lowData by rememberSaveable { mutableStateOf(false) }
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMToggle("Push notifications", push, onCheckedChange = { push = it })
        AMDivider(inset = inset)
        AMToggle("Email digest", email, onCheckedChange = { email = it })
        AMDivider(inset = inset)
        AMToggle(
            "Low data mode",
            lowData,
            onCheckedChange = { lowData = it },
            supportingText = "Pause automatic updates and downloads",
        )
        AMDivider(inset = inset)
        AMToggle("Sync over cellular", checked = true, onCheckedChange = {}, enabled = false)
    }
}
