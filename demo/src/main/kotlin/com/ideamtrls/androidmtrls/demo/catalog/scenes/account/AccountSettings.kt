package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMLabeledContent
import com.ideamtrls.androidmtrls.AMMenuPicker
import com.ideamtrls.androidmtrls.AMProgressView
import com.ideamtrls.androidmtrls.AMProgressViewStyle
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMToggle

/**
 * Margin's settings: the account, notifications, reading, privacy, storage and version, each a
 * section header over a card of bare rows, then "Sign out".
 */
@Composable
internal fun AccountSettings(model: AccountModel) {
    AccountPage("Settings") {
        AMSection("Account", style = AMSectionStyle.Rows, inset = false) {
            AMLabeledContent("Email", model.email.text.toString())
            AMLabeledContent("Plan", "Free")
        }
        AMSection(
            "Notifications",
            footer = "Mentions only keeps alerts to replies and mentions of you.",
            style = AMSectionStyle.Rows,
            inset = false,
        ) {
            AMToggle("Push alerts", model.pushAlerts, onCheckedChange = { model.pushAlerts = it })
            AMToggle("Email digest", model.emailDigest, onCheckedChange = { model.emailDigest = it })
            AMToggle("Mentions only", model.mentionsOnly, onCheckedChange = { model.mentionsOnly = it })
        }
        AMSection("Reading", style = AMSectionStyle.Rows, inset = false) {
            AMMenuPicker(
                "Reader font",
                options = AccountReaderFont.entries,
                label = { it.title },
                selected = model.readerFont,
                onSelectedChange = { model.readerFont = it },
            )
        }
        AMSection(
            "Privacy",
            footer = "A private account shares your highlights only with friends you approve.",
            style = AMSectionStyle.Rows,
            inset = false,
        ) {
            AMToggle("Private account", model.privateAccount, onCheckedChange = { model.privateAccount = it })
            AMToggle("Show activity", model.showActivity, onCheckedChange = { model.showActivity = it })
        }
        // The bar's label and value are the storage row: what's used, of how much.
        AMSection("Storage", style = AMSectionStyle.Rows, inset = false) {
            AMProgressView(
                "Used",
                progress = { 0.42f },
                valueLabel = "2.1 GB of 5 GB",
                style = AMProgressViewStyle.Bar,
            )
        }
        AMSection("About", style = AMSectionStyle.Rows, inset = false) {
            AMLabeledContent("Version", "1.0 (42)")
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AMButton(
                "Sign out",
                onClick = model::confirmSignOut,
                style = AMButtonStyle.Ghost,
                destructive = true,
            )
        }
    }
}
