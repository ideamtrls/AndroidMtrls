package io.github.jmsdnns.androidmtrls.demo.pages.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBanner
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBannerItem
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBannerStyle
import io.github.jmsdnns.androidmtrls.tokens.AMTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [AMBanner] host composable.
 *
 * id: "overlays-banner", section: [ComponentSection.Overlays].
 */
val bannerEntry: ComponentEntry = ComponentEntry(
    id      = "overlays-banner",
    section = ComponentSection.Overlays,
    title   = "Banner",
    subtitle = "Top-anchored transient notice",
    intro   = "AMBanner wraps scene content and presents a floating card at the top " +
        "of the screen. Banners auto-dismiss after their duration (default 4 s), " +
        "or the user can dismiss by tapping or swiping up. Attach at the scene root " +
        "so the banner and its surface scrim cover the full screen.",
    icon    = Icons.Filled.NotificationsActive,
    content = { BannerDemoPage() },
)

// ---------------------------------------------------------------------------
// BannerDemoPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMBanner].
 *
 * Wraps the page body in [AMBanner] so the banner overlays the full content
 * area — the correct attachment pattern (attach at the scene root; comment
 * in this file mirrors the KDoc advice in Banner.kt).
 *
 * The "Styles" group shows four [AMSecondaryButton]s arranged in a 2×2 grid:
 * one per [AMBannerStyle]. Each button sets the [AMBannerItem] state, which
 * the surrounding [AMBanner] host displays. Tapping the banner, swiping up,
 * or waiting 4 s dismisses it.
 */
@Composable
fun BannerDemoPage() {
    // Banner state: null = no banner showing.
    var bannerItem by remember { mutableStateOf<AMBannerItem?>(null) }

    // AMBanner wraps hostContent — attach here (page level) so the overlay
    // covers the entire demo page content, matching the scene-root pattern.
    AMBanner(
        item      = bannerItem,
        onDismiss = { bannerItem = null },
    ) {
        // ── Page body (hostContent) ───────────────────────────────────────
        // iOS insets the 2×2 trigger grid by the lg page margin (Overlays.swift).
        catalogGroup(title = "Styles") {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AMTheme.Spacing.lg.dp),
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    AMSecondaryButton(
                        label   = "Info",
                        onClick = {
                            bannerItem = AMBannerItem(
                                title   = "Heads up",
                                message = "Just letting you know.",
                                style   = AMBannerStyle.Info,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    AMSecondaryButton(
                        label   = "Success",
                        onClick = {
                            bannerItem = AMBannerItem(
                                title = "Saved",
                                style = AMBannerStyle.Success,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                ) {
                    AMSecondaryButton(
                        label   = "Warning",
                        onClick = {
                            bannerItem = AMBannerItem(
                                title   = "Battery low",
                                message = "Connect to a power source.",
                                style   = AMBannerStyle.Warning,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    AMSecondaryButton(
                        label   = "Error",
                        onClick = {
                            bannerItem = AMBannerItem(
                                title = "Upload failed",
                                style = AMBannerStyle.Destructive,
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
