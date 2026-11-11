package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMTextEditor
import com.ideamtrls.androidmtrls.AMTextFieldAutofocus
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

/** How long a post or a reply may be. */
private const val SocialPostLimit = 280

/**
 * The compose sheet's content, under its "New post" / "Reply" title: who you're replying to, your
 * avatar beside the editor (focused as the sheet opens), a character count (red past the limit),
 * and Post or Reply at the bottom, disabled while the text is blank or too long. The draft lives in
 * the sheet, so any close but sending drops it, without asking.
 */
@Composable
internal fun ColumnScope.SocialComposeSheet(model: SocialModel) {
    val spacing = AMTheme.tokens.spacing
    val draft = rememberTextFieldState()
    val isReply = model.composeMode is SocialComposeMode.Reply
    val text = draft.text.toString()
    val count = text.codePointCount(0, text.length)
    val sendable = text.isNotBlank() && count <= SocialPostLimit
    Column(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        model.replyTarget?.let { target ->
            Text(
                "Replying to ${model.user(target.authorId).handle}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            Modifier.fillMaxWidth().amCard(padding = PaddingValues(spacing.lg)),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            val self = model.selfUser
            AMAvatar(
                self.avatarUrl,
                self.name,
                Modifier.padding(top = spacing.sm).clearAndSetSemantics {},
                size = AMAvatarSize.Small,
            )
            AMTextEditor(
                draft,
                label = if (isReply) "Write a reply" else "Write a post",
                modifier = Modifier.weight(1f),
                minLines = 5,
                maxLines = 12,
                autofocus = AMTextFieldAutofocus.CaretAtEnd,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            "$count/$SocialPostLimit",
            Modifier.fillMaxWidth(),
            color = if (count > SocialPostLimit) AMSemantic.Destructive.color else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.End,
        )
        AMButton(
            if (isReply) "Reply" else "Post",
            onClick = { model.send(text) },
            modifier = Modifier.fillMaxWidth(),
            style = AMButtonStyle.Primary(),
            enabled = sendable,
        )
    }
}
