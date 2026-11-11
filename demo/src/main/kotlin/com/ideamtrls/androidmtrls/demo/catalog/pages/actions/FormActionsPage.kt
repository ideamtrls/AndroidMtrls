package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMFormActions
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val FormActionsEntry = ComponentEntry(
    id = "formActions",
    category = CatalogCategory.Actions,
    title = "Form actions",
    summary = "The Save and Delete row that closes a form",
    icon = R.drawable.ic_fact_check,
    keywords = listOf("AMFormActions", "save", "delete", "destructive", "commit", "sheet", "footer", "edit", "form"),
) { FormActionsPage() }

@Composable
private fun FormActionsPage() {
    ComponentPage(
        FormActionsEntry,
        preview = {
            AMFormActions(destructive = { AMButton("Delete", onClick = ::delete, destructive = true) }) {
                AMButton("Save", onClick = ::save)
            }
        },
    ) {
        EditTask()
    }
}

// Stand-ins for your own actions.
private fun save() {}

private fun delete() {}

/** The end of a task's edit form: its title, then Delete and Save, which waits for a title. */
@Composable
private fun EditTask() {
    val spacing = AMTheme.tokens.spacing
    val title = rememberTextFieldState("Write the quarterly update")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            AMTextField(title, label = "Title")
        }
        AMFormActions(destructive = { AMButton("Delete", onClick = ::delete, destructive = true) }) {
            AMButton("Save", onClick = ::save, enabled = title.text.isNotEmpty())
        }
    }
}
