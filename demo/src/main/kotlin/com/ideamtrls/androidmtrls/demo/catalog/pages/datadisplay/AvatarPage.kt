package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ideamtrls.androidmtrls.AMAvatar
import com.ideamtrls.androidmtrls.AMAvatarSize
import com.ideamtrls.androidmtrls.AMBadgedBox
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMListRow
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.amCard

internal val AvatarEntry = ComponentEntry(
    id = "avatar",
    category = CatalogCategory.DataDisplay,
    title = "Avatar",
    summary = "Profile picture with an initials fallback",
    icon = R.drawable.ic_account_circle,
    keywords = listOf("AMAvatar", "AMAvatarSize", "profile", "initials", "user", "person", "photo", "circle", "square"),
) { AvatarPage() }

@Composable
private fun AvatarPage() {
    ComponentPage(
        AvatarEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { PreviewAvatars() },
    ) {
        WorkspaceMembers()
    }
}

private val AdaPhoto: String
    get() = catalogPhoto("ada", 200)

/** A photo, then two people with no photo, on a surface strip. */
@Composable
private fun PreviewAvatars() {
    val spacing = AMTheme.tokens.spacing
    Row(
        Modifier.fillMaxWidth().background(AMTheme.Surface.primary).padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AMAvatar(AdaPhoto, name = "Ada Lovelace", size = AMAvatarSize.Large)
        AMAvatar(null, name = "Grace Hopper", size = AMAvatarSize.Large)
        AMAvatar(null, name = "Alan Turing", size = AMAvatarSize.Large)
    }
}

/** A workspace's members: the workspace's initials in a rounded square, its people, one with news, an open invite. */
@Composable
private fun WorkspaceMembers() {
    val inset = AMTheme.tokens.spacing.lg
    Column(Modifier.fillMaxWidth().amCard()) {
        AMListRow(
            "Apollo Guidance",
            subtitle = "Workspace · 4 members",
            leadingContent = {
                AMAvatar(null, name = "Apollo Guidance", size = AMAvatarSize.Large, shape = MaterialTheme.shapes.medium)
            },
        )
        AMDivider(inset = inset)
        AMListRow("Ada Lovelace", subtitle = "Owner", leadingContent = { AMAvatar(AdaPhoto, name = "Ada Lovelace") })
        AMDivider(inset = inset)
        AMListRow(
            "Katherine Johnson",
            subtitle = "Flight dynamics",
            leadingContent = { AMBadgedBox(count = 2) { AMAvatar(null, name = "Katherine Johnson") } },
        )
        AMDivider(inset = inset)
        AMListRow(
            "Margaret Heafield Hamilton",
            subtitle = "Software lead",
            leadingContent = { AMAvatar(null, name = "Margaret Heafield Hamilton") },
        )
        AMDivider(inset = inset)
        AMListRow("Invite pending", subtitle = "Sent yesterday", leadingContent = { AMAvatar(null, name = null) })
    }
}
