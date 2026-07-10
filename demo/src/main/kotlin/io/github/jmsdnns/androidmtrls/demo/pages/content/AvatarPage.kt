package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarShape
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

val avatarEntry: ComponentEntry = ComponentEntry(
    id = "content-avatar",
    section = ComponentSection.Content,
    title = "Avatar",
    subtitle = "Portrait with initials fallback",
    intro = "Portrait with an optional initials fallback, in a circle or rounded-square " +
        "silhouette. Three sizes — sm, md, lg — sized to typical row and header contexts.",
    icon = Icons.Default.AccountCircle,
    content = { AvatarPage() },
)

// ---------------------------------------------------------------------------
// AvatarPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMAvatar].
 *
 * Four groups — one per shape — each showing lg → md → sm.
 * Mirrors iOS `IMAvatarPage` from `Demo/Sources/Catalog/Pages/Content.swift`.
 */
@Composable
fun AvatarPage() {
    // ── Circle ────────────────────────────────────────────────────────────────
    catalogGroup(title = "Circle") {
        ShapeRow(
            shape = AMAvatarShape.Circle,
            initials = "Jane Doe",
        )
    }

    // ── Square · lg ───────────────────────────────────────────────────────────
    catalogGroup(title = "Square · lg") {
        ShapeRow(
            shape = AMAvatarShape.Square(AMTheme.Radius.lg.dp),
            initials = "Alex Rivera",
        )
    }

    // ── Square · md ───────────────────────────────────────────────────────────
    catalogGroup(title = "Square · md") {
        ShapeRow(
            shape = AMAvatarShape.Square(AMTheme.Radius.md.dp),
            initials = "Taylor Smith",
        )
    }

    // ── Square · sm ───────────────────────────────────────────────────────────
    catalogGroup(title = "Square · sm") {
        ShapeRow(
            shape = AMAvatarShape.Square(AMTheme.Radius.sm.dp),
            initials = "Christopher Knight",
        )
    }
}

// ---------------------------------------------------------------------------
// Private helper
// ---------------------------------------------------------------------------

@Composable
private fun ShapeRow(shape: AMAvatarShape, initials: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AMAvatar(url = null, initials = initials, size = AMAvatarSize.LG, shape = shape)
        AMAvatar(url = null, initials = initials, size = AMAvatarSize.MD, shape = shape)
        AMAvatar(url = null, initials = initials, size = AMAvatarSize.SM, shape = shape)
    }
}
