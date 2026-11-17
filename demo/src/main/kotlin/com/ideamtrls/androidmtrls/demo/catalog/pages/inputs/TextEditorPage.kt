package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMPlainTextEditor
import com.ideamtrls.androidmtrls.AMTextEditor
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.ProvideAMOnSurface
import com.ideamtrls.androidmtrls.amCard

internal val TextEditorEntry = ComponentEntry(
    id = "textEditor",
    category = CatalogCategory.Inputs,
    title = "Text editor",
    summary = "Multiline input that grows with its text",
    icon = R.drawable.ic_notes,
    keywords = listOf(
        "AMTextEditor",
        "AMPlainTextEditor",
        "multiline",
        "notes",
        "compose",
        "message",
        "auto-grow",
        "lines",
        "plain",
        "frameless",
    ),
) { TextEditorPage() }

@Composable
private fun TextEditorPage() {
    ComponentPage(
        TextEditorEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { NoteEditor() },
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AMTheme.tokens.spacing.xl)) {
            FrostedNoteCard()
            WeeklyUpdate()
            PlainReply()
        }
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

/** A note editor in a frosted card over a photo. */
@Composable
private fun FrostedNoteCard() {
    val spacing = AMTheme.tokens.spacing
    val note = rememberTextFieldState("Pick up the keys before noon.")
    CatalogFrostedStage {
        Column(Modifier.padding(spacing.lg).fillMaxWidth().amCard(padding = PaddingValues(spacing.lg))) {
            ProvideAMOnSurface {
                AMTextEditor(note, label = "Note", minLines = 2)
            }
        }
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

/** A reply written straight on its card: your avatar beside a plain editor holding a line. */
@Composable
private fun PlainReply() {
    val spacing = AMTheme.tokens.spacing
    val reply = rememberTextFieldState("Count me in for Thursday. I'll bring the slides.")
    Row(
        Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)).height(PlainReplyHeight),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMAvatar(
            null,
            "Ada Lovelace",
            Modifier.padding(top = spacing.xs).clearAndSetSemantics {},
            size = AMAvatarSize.Small,
        )
        AMPlainTextEditor(reply, "Write a reply", Modifier.weight(1f).fillMaxHeight())
    }
}

/** How tall the reply's card is: room to write under the line it holds. */
private val PlainReplyHeight = 160.dp
