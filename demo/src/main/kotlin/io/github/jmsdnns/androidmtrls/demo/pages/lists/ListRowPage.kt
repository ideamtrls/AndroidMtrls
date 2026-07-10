package io.github.jmsdnns.androidmtrls.demo.pages.lists

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// ListRow catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMListRow].
 *
 * id: "lists-listrow", section: [ComponentSection.Lists].
 */
val listRowEntry: ComponentEntry = ComponentEntry(
    id = "lists-listrow",
    section = ComponentSection.Lists,
    title = "List Row",
    tileTitle = "Row",
    subtitle = "Flat list row",
    intro = "Flat row for vertical lists. Fills the available width with Surface.primary, " +
        "no rounded corners, no shadow — meant to sit flush against neighboring rows. " +
        "Composes a leading icon, a title (and optional subtitle), and a trailing accessory.",
    icon = Icons.Default.ViewList,
    content = { ListRowPage() },
)

// ---------------------------------------------------------------------------
// ListRowPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMListRow].
 *
 * Two groups per demo spec §3.7:
 *
 * - **Variants** — core row states with leading icons and tints:
 *   title-only, title + subtitle, selected, after-selected.
 * - **With chevron** — navigation-style rows with leading icons, subtitles,
 *   and the drill-down chevron, mirroring iOS navRows sample data.
 */
@Composable
fun ListRowPage() {

    // Leading-icon tints drawn from the library's semantic palette so the
    // specimens track the tokens rather than hardcoded hexes.
    val accentBlue = AMTheme.Semantic.info.resolve(LocalAMIsDarkTheme.current)
    val yellowTint = AMTheme.warning.resolve(LocalAMIsDarkTheme.current)
    val greenTint = AMTheme.success.resolve(LocalAMIsDarkTheme.current)
    val redTint = AMTheme.destructive.resolve(LocalAMIsDarkTheme.current)
    val indigoTint = Color(0xFF5856D6)
    val secondaryTint = Color(0xFF8E8E93)

    // ── Variants ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Variants") {
        catalogFlatDemo(padding = 0.dp) {
            Column {
                AMListRow(
                    title = "Title only",
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(accentBlue),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                AMListRow(
                    title = "Title with subtitle",
                    subtitle = "Longer secondary text",
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(yellowTint),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                AMListRow(
                    title = "Selected row",
                    subtitle = "isSelected = true",
                    isSelected = true,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(greenTint),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                AMListRow(
                    title = "Row after selected",
                    subtitle = "Neighbor without the fill",
                    isSelected = false,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(accentBlue),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }

    // ── With chevron ──────────────────────────────────────────────────────────
    catalogGroup(title = "With chevron") {
        catalogFlatDemo(padding = 0.dp) {
            Column {
                AMListRow(
                    title = "Account",
                    subtitle = "jdennis@example.com",
                    showsChevron = true,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(accentBlue),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                AMListRow(
                    title = "Notifications",
                    subtitle = "All sounds on",
                    showsChevron = true,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(redTint),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                AMListRow(
                    title = "Storage",
                    subtitle = "Downloads & cache",
                    showsChevron = true,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(indigoTint),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
                AMListRow(
                    title = "About",
                    showsChevron = true,
                    modifier = Modifier.fillMaxWidth(),
                    leading = {
                        Image(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(secondaryTint),
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )
            }
        }
    }

}
