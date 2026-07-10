package io.github.jmsdnns.androidmtrls.demo.scenes.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.controls.buttons.AMPrimaryButton
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextEditor
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

/** The compose character limit. Mirrors iOS `SocialComposeSheet.limit`. */
private const val COMPOSE_LIMIT = 280

/**
 * Content for the compose sheet: an editor with a live character counter and a
 * primary action that prepends a new post to the feed — or, in reply mode
 * ([SocialState.replyTargetId] non-null), appends a reply to the targeted post.
 * Ports iOS `SocialComposeSheet`.
 *
 * Sits inside an [io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheet]
 * (`AMSheetHeight.Full`), so the editor grows to fill the sheet. The editor
 * autofocuses, so the keyboard rises as the sheet appears (iOS `autofocus: true`).
 */
@Composable
fun SocialComposeSheet(state: SocialState) {
    val isDark = LocalAMIsDarkTheme.current
    val primary = AMTheme.Text.primary.resolve(isDark)
    val secondary = AMTheme.Text.secondary.resolve(isDark)

    var text by remember { mutableStateOf("") }

    val isReply = state.replyTargetId != null
    val trimmed = text.trim()
    val overLimit = text.length > COMPOSE_LIMIT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        BasicText(
            text = if (isReply) "Reply" else "New Post",
            style = AMTheme.Type.title.toTextStyle(primary)
                .copy(fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AMTheme.Spacing.sm.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AMAvatar(
                url = state.selfUser.avatarUrl,
                initials = state.selfUser.initials,
                size = AMAvatarSize.SM,
            )
            AMTextEditor(
                value = text,
                onValueChange = { text = it },
                placeholder = if (isReply) "Post your reply…" else "What's happening?",
                minHeight = 120.dp,
                maxHeight = 1_000.dp,
                autofocus = true,
                modifier = Modifier.weight(1f),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            BasicText(
                text = "${text.length}/$COMPOSE_LIMIT",
                style = AMTheme.Type.caption.toTextStyle(
                    if (overLimit) AMTheme.Semantic.destructive.resolve(isDark) else secondary,
                ).copy(fontFeatureSettings = "tnum"),
            )
        }

        AMPrimaryButton(
            label = if (isReply) "Reply" else "Post",
            onClick = {
                val draft = trimmed
                text = ""
                if (isReply) state.addReply(draft) else state.addPost(draft)
            },
            enabled = trimmed.isNotEmpty() && !overLimit,
        )
    }
}
