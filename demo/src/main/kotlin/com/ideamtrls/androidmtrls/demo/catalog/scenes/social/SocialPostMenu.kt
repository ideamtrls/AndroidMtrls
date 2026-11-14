package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMHoldAction
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
    val icons = rememberSocialPostIcons()
    AMSpotlight(
        item = model.postMenuPost,
        onDismissRequest = { model.postMenuId = null },
        actions = { post -> socialPostActions(post, model, actions, icons) },
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

/** The post menu's glyphs. */
@Immutable
internal class SocialPostIcons(val copy: Painter, val delete: Painter, val mute: Painter)

/** The post menu's glyphs. */
@Composable
internal fun rememberSocialPostIcons(): SocialPostIcons = SocialPostIcons(
    copy = painterResource(R.drawable.ic_content_copy),
    delete = painterResource(R.drawable.ic_delete),
    mute = painterResource(R.drawable.ic_volume_off),
)

/** Copy and Delete on your own post; Copy and Mute on anyone else's. A tile closes the menu first. */
internal fun socialPostActions(post: SocialPost, model: SocialModel, actions: SocialPostActions, icons: SocialPostIcons): List<AMHoldAction> {
    val author = model.user(post.authorId)
    return listOf(
        AMHoldAction("Copy", icons.copy) { actions.copy(post) },
        if (author.isSelf) {
            AMHoldAction("Delete", icons.delete, destructive = true) { model.confirmDelete(post.id) }
        } else {
            AMHoldAction("Mute", icons.mute) { model.mute(author.id) }
        },
    )
}
