package io.github.jmsdnns.androidmtrls.demo.pages.content

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.content.AMBadge
import io.github.jmsdnns.androidmtrls.content.AMBadgeContent
import io.github.jmsdnns.androidmtrls.content.AMBadgedBox
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// badgeEntry — ComponentEntry exposed to CatalogRegistry (do NOT register here)
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the AMBadge component page.
 *
 * Exposes `id = "content-badge"`, `section = ComponentSection.Content`.
 * Register this in CatalogRegistry when the Content section is wired up.
 *
 * Mirrors `IMBadgePage.entry` from iOSMtrls Demo/Sources/Catalog/Pages/Content.swift.
 */
val badgeEntry: ComponentEntry = ComponentEntry(
    id = "content-badge",
    section = ComponentSection.Content,
    title = "Badge",
    subtitle = "Dot or count indicator",
    intro = "Micro-indicator that attaches to another element — dot for unread state or count " +
        "for notifications. Sits at the top-trailing corner with a thin surface ring so it " +
        "stays legible against any background.",
    icon = Icons.Default.Notifications,
    content = { BadgePage() },
)

// ---------------------------------------------------------------------------
// BadgePage
// ---------------------------------------------------------------------------

/**
 * Component page for `AMBadge` / `AMBadgedBox`.
 *
 * Three groups mirror the iOS `IMBadgePage`:
 * - **Dots** — three AMAvatar md circles badged with dot variants (default, info, success tint).
 * - **Counts** — three notification icons showing count badges (3, 12, 142 → "99+").
 * - **Zero count** — side-by-side `count: 0` (hidden) vs `count: 5` (visible) with monospaced labels.
 */
@Composable
fun BadgePage() {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val textPrimary = AMTheme.Text.primary.resolve(isDark)
    val textSecondary = AMTheme.Text.secondary.resolve(isDark)

    // ── Dots group ───────────────────────────────────────────────────────────
    catalogGroup(title = "Dots") {
        // Card-framed strip — matches iOS `.imCard().padding(.horizontal)` wrapping
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AMTheme.Spacing.lg.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    AMTheme.Spacing.xl.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Default (destructive red) — JD
                AMBadgedBox(badge = { AMBadge(AMBadgeContent.Dot) }) {
                    AMAvatar(url = null, initials = "JD", size = AMAvatarSize.MD)
                }
                // Info tint — AR
                AMBadgedBox(
                    badge = { AMBadge(AMBadgeContent.Dot, tint = tokens.semantic.info.resolve(isDark)) },
                ) {
                    AMAvatar(url = null, initials = "AR", size = AMAvatarSize.MD)
                }
                // Success tint — TS
                AMBadgedBox(
                    badge = { AMBadge(AMBadgeContent.Dot, tint = tokens.semantic.success.resolve(isDark)) },
                ) {
                    AMAvatar(url = null, initials = "TS", size = AMAvatarSize.MD)
                }
            }
        }
    }

    // ── Counts group ─────────────────────────────────────────────────────────
    catalogGroup(title = "Counts") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AMTheme.Spacing.lg.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    AMTheme.Spacing.xl.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 3 — bell (Notifications)
                AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(3)) }) {
                    Image(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp),
                    )
                }
                // 12 — envelope (Mail)
                AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(12)) }) {
                    Image(
                        imageVector = Icons.Default.Mail,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp),
                    )
                }
                // 142 — tray (overflows to "99+")
                AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(142)) }) {
                    Image(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }

    // ── Zero count group ─────────────────────────────────────────────────────
    catalogGroup(title = "Zero count") {
        AMCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AMTheme.Spacing.lg.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    AMTheme.Spacing.xl.dp,
                    Alignment.CenterHorizontally,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // count: 0 — badge hidden
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(0)) }) {
                        Image(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(textPrimary),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    BasicText(
                        text = "count: 0",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }

                // count: 5 — badge visible
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    AMBadgedBox(badge = { AMBadge(AMBadgeContent.Count(5)) }) {
                        Image(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(textPrimary),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    BasicText(
                        text = "count: 5",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        ),
                    )
                }
            }
        }
    }
}
