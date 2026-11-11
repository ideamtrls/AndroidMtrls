package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTag
import com.ideamtrls.androidmtrls.AMTagTint
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val ListRowEntry = ComponentEntry(
    id = "listRow",
    category = CatalogCategory.Lists,
    title = "List row",
    summary = "Flat full-width row with leading and trailing slots",
    icon = R.drawable.ic_table_rows,
    keywords = listOf(
        "AMListRow", "ListItem", "row", "cell", "title", "subtitle", "selected", "selection ring", "leading",
        "trailing", "onClick",
    ),
) { ListRowPage() }

@Composable
private fun ListRowPage() {
    ComponentPage(
        ListRowEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { DeviceSettings() },
    ) {
        AccountSettings()
    }
}

// Stand-ins for your own actions.
private fun openNotifications() {}

private fun openStorage() {}

private fun openAccount() {}

private fun openAbout() {}

/** Two settings rows on a surface strip. */
@Composable
private fun DeviceSettings() {
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        AMListRow(
            "Notifications",
            subtitle = "All sounds on",
            leadingContent = { RowGlyph(R.drawable.ic_notifications, AMSemantic.Destructive) },
            onClick = ::openNotifications,
        )
        AMDivider(inset = AMTheme.tokens.spacing.lg)
        AMListRow(
            "Storage",
            subtitle = "Downloads and cache",
            leadingContent = { RowGlyph(R.drawable.ic_hard_drive, AMSemantic.Info) },
            onClick = ::openStorage,
        )
    }
}

private enum class ListsPlan(val title: String, val detail: String) {
    Standard("Standard", "Free for everyone"),
    Plus("Plus", "Unlimited projects"),
    Team("Team", "Shared workspaces"),
}

/** An account's settings card, then its plans, the chosen one ringed. */
@Composable
private fun AccountSettings() {
    val spacing = AMTheme.tokens.spacing
    var plan by rememberSaveable { mutableStateOf(ListsPlan.Plus) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(Modifier.fillMaxWidth().amCard()) {
            AMListRow(
                "Ada Lovelace",
                subtitle = "ada@example.com",
                leadingContent = { RowGlyph(R.drawable.ic_account_circle, AMSemantic.Info) },
                onClick = ::openAccount,
            )
            AMDivider(inset = spacing.lg)
            AMListRow(
                "Storage",
                subtitle = "Downloads and cache",
                leadingContent = { RowGlyph(R.drawable.ic_hard_drive, AMSemantic.Warning) },
                trailingContent = { AMTag("82% full", tint = AMTagTint.Warning) },
                onClick = ::openStorage,
            )
            AMDivider(inset = spacing.lg)
            AMListRow("About", onClick = ::openAbout)
        }
        Column(Modifier.fillMaxWidth().amCard()) {
            ListsPlan.entries.forEach { option ->
                AMListRow(
                    option.title,
                    subtitle = option.detail,
                    selected = option == plan,
                    onClick = { plan = option },
                )
                if (option != ListsPlan.entries.last()) AMDivider(inset = spacing.lg)
            }
        }
    }
}

/** A row's leading glyph in a status color. */
@Composable
private fun RowGlyph(@DrawableRes icon: Int, tint: AMSemantic) {
    Icon(painterResource(icon), contentDescription = null, tint = tint.color)
}
