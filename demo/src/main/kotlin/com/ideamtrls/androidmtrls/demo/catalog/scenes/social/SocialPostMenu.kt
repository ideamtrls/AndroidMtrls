package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ideamtrls.androidmtrls.AMActionGridButton
import com.ideamtrls.androidmtrls.AMActionGridRow
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMSpotlight
import com.ideamtrls.androidmtrls.AMTheme

/**
 * The post menu's actions: Copy, then Delete on your own post or Mute on anyone else's. The
 * spotlight shows them as tiles, and the feed card offers them to TalkBack directly. [copy] puts
 * the post's text on the clipboard; the system confirms the copy itself.
 */
@Stable
internal class SocialPostActions(
    private val model: SocialModel,
    val copy: (SocialPost) -> Unit,
) {
    /** The menu's actions as custom actions on the card. */
    fun accessibilityActions(post: SocialPost): List<CustomAccessibilityAction> {
        val author = model.user(post.authorId)
        val second = if (author.isSelf) {
            CustomAccessibilityAction("Delete") { model.confirmDelete(post.id); true }
        } else {
            CustomAccessibilityAction("Mute") { model.mute(author.id); true }
        }
        return listOf(CustomAccessibilityAction("Copy") { copy(post); true }, second)
    }
}

/**
 * The post menu: a held post (or one whose ⋮ was tapped) up close in the spotlight, a still copy of
 * its card over the tiles of what to do with it. Up while the model names a post.
 */
@Composable
internal fun SocialPostMenu(model: SocialModel, actions: SocialPostActions) {
    AMSpotlight(
        item = model.postMenuPost,
        onDismissRequest = { model.postMenuId = null },
        actions = { post -> SocialPostTiles(post, model, actions) },
    ) { post -> SocialPostPreview(post, model) }
}

/** The feed card without its action row or live controls: the avatar, the identity, the text and photo. */
@Composable
private fun SocialPostPreview(post: SocialPost, model: SocialModel) {
    val spacing = AMTheme.tokens.spacing
    val author = model.user(post.authorId)
    Row(
        Modifier.fillMaxWidth().padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        AMAvatar(author.avatarUrl, author.name, Modifier.clearAndSetSemantics {})
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SocialIdentityHeader(author, post.timestamp)
            SocialPostBody(post)
        }
    }
}

/** Copy and Delete on your own post; Copy and Mute on anyone else's. Each closes the menu first. */
@Composable
private fun SocialPostTiles(post: SocialPost, model: SocialModel, actions: SocialPostActions) {
    val author = model.user(post.authorId)
    AMActionGridRow(
        {
            AMActionGridButton(painterResource(R.drawable.ic_content_copy), "Copy", onClick = {
                model.postMenuId = null
                actions.copy(post)
            })
        },
        {
            if (author.isSelf) {
                AMActionGridButton(
                    painterResource(R.drawable.ic_delete),
                    "Delete",
                    onClick = { model.confirmDelete(post.id) },
                    destructive = true,
                )
            } else {
                AMActionGridButton(painterResource(R.drawable.ic_volume_off), "Mute", onClick = { model.mute(author.id) })
            }
        },
    )
}
