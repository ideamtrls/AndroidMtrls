package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMTextEditor
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val TextEditorEntry = ComponentEntry(
    id = "textEditor",
    category = CatalogCategory.Inputs,
    title = "Text editor",
    summary = "Multiline input that grows with its text",
    icon = R.drawable.ic_notes,
    keywords = listOf("AMTextEditor", "multiline", "notes", "compose", "message", "auto-grow", "lines"),
) { TextEditorPage() }

@Composable
private fun TextEditorPage() {
    ComponentPage(
        TextEditorEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { NoteEditor() },
    ) {
        WeeklyUpdate()
    }
}

private const val Summary =
    "Shipped the new onboarding flow, fixed the keyboard covering the form on small phones, and " +
        "reviewed three pull requests. Next week: an accessibility pass on the settings screens, then " +
        "start on the share sheet."

/** An empty note editor on a surface strip. */
@Composable
private fun NoteEditor() {
    val note = rememberTextFieldState()
    Box(Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(AMTheme.tokens.spacing.lg)) {
        AMTextEditor(note, label = "Note", placeholder = "Write a note…")
    }
}

/** A weekly update: a filled summary that has grown with its text, and an empty blockers editor. */
@Composable
private fun WeeklyUpdate() {
    val spacing = AMTheme.tokens.spacing
    val summary = rememberTextFieldState(Summary)
    val blockers = rememberTextFieldState()
    Column(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMTextEditor(summary, label = "This week", minLines = 2)
        AMTextEditor(blockers, label = "Blockers", placeholder = "Anything the team should know")
    }
}
