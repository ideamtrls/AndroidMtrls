package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMAlert
import com.ideamtrls.androidmtrls.AMAlertAction
import com.ideamtrls.androidmtrls.AMAlertItem
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amRowPadding

internal val AlertEntry = ComponentEntry(
    id = "alert",
    category = CatalogCategory.Overlays,
    title = "Alert",
    summary = "A dialog that asks before something happens",
    icon = R.drawable.ic_feedback,
    keywords = listOf(
        "AMAlert", "AMAlertItem", "AMAlertAction", "AlertDialog", "dialog", "confirm", "cancel", "destructive",
    ),
) { AlertPage() }

@Composable
private fun AlertPage() {
    ComponentPage(
        AlertEntry,
        preview = { PublishDraft() },
    ) {
        DeleteProject()
    }
}

// A stand-in for your own action.
private fun publish() {}

/** The plain question: publish, or not now. */
@Composable
private fun PublishDraft() {
    var alert by remember { mutableStateOf<AMAlertItem?>(null) }
    AMButton(
        "Show alert",
        onClick = {
            alert = AMAlertItem(
                "Publish draft?",
                confirm = AMAlertAction("Publish", onClick = ::publish),
                message = "Everyone with the link will be able to read it.",
                cancel = AMAlertAction("Not now"),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    AMAlert(alert, onDismissRequest = { alert = null })
}

/** A project over its Delete button; confirming marks the project deleted. */
@Composable
private fun DeleteProject() {
    var alert by remember { mutableStateOf<AMAlertItem?>(null) }
    var status by rememberSaveable { mutableStateOf("12 tasks open") }
    val spacing = AMTheme.tokens.spacing
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(Modifier.fillMaxWidth().amCard()) {
            Column(Modifier.amRowPadding()) {
                Text("Harbor survey", style = MaterialTheme.typography.bodyLarge)
                Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AMButton(
            "Delete project",
            onClick = {
                alert = AMAlertItem(
                    "Delete project?",
                    confirm = AMAlertAction("Delete", destructive = true) { status = "Deleted" },
                    message = "This can't be undone.",
                    cancel = AMAlertAction("Keep"),
                )
            },
            destructive = true,
        )
    }
    AMAlert(alert, onDismissRequest = { alert = null })
}
