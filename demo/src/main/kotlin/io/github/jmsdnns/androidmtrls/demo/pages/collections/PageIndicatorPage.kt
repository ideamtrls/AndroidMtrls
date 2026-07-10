package io.github.jmsdnns.androidmtrls.demo.pages.collections

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMPageIndicator
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogFlatDemo
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMPageIndicator].
 *
 * id: "collections-pageindicator", section: [ComponentSection.Collections].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the
 * Collections slot. This file intentionally does not touch any shared
 * registry — registration is the task owner's responsibility per the task brief.
 */
val pageIndicatorEntry: ComponentEntry = ComponentEntry(
    id = "collections-pageindicator",
    section = ComponentSection.Collections,
    title = "Page Indicator",
    tileTitle = "Indicator",
    subtitle = "Dot strip for paging",
    intro = "Dot strip that reads the current page out of a total. Pairs with AMCarousel — " +
        "the active dot elongates to a capsule in primary text color while inactive dots " +
        "stay small and muted.",
    icon = Icons.Default.MoreHoriz,
    content = { PageIndicatorPage() },
)

// ---------------------------------------------------------------------------
// PageIndicatorPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMPageIndicator].
 *
 * Two groups per demo spec §3.9:
 *
 * - **Default** — interactive 5-page demo with Prev / Next buttons that step
 *   a remembered index; live indicator reflects the current page.
 * - **Counts** — three static rows showing 3-dot (mid), 5-dot (mid), and
 *   8-dot (mid) indicators at a glance.
 */
@Composable
fun PageIndicatorPage() {

    // ── Default (interactive) ────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        catalogFlatDemo {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
            ) {
                val pageCount = 5
                var currentIndex by remember { mutableIntStateOf(1) }

                AMPageIndicator(
                    count = pageCount,
                    currentIndex = currentIndex,
                )

                val isDark = LocalAMIsDarkTheme.current
                val labelColor = AMTheme.Text.primary.resolve(isDark)
                val labelStyle = TextStyle(
                    color = labelColor,
                    fontSize = 17.sp,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                ) {
                    // Prev: chevron.left icon before label — mirrors iOS Label("Prev", systemImage: "chevron.left")
                    AMSecondaryButton(
                        onClick = { currentIndex = (currentIndex - 1).coerceIn(0, pageCount - 1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Image(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(labelColor),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        BasicText(text = "Prev", style = labelStyle)
                    }
                    // Next: label before chevron.right icon — mirrors iOS Label("Next", systemImage: "chevron.right")
                    AMSecondaryButton(
                        onClick = { currentIndex = (currentIndex + 1).coerceIn(0, pageCount - 1) },
                        modifier = Modifier.weight(1f),
                    ) {
                        BasicText(text = "Next", style = labelStyle)
                        Spacer(modifier = Modifier.width(4.dp))
                        Image(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(labelColor),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }

    // ── Counts ───────────────────────────────────────────────────────────────
    catalogGroup(title = "Counts") {
        catalogFlatDemo {
            val isDark = LocalAMIsDarkTheme.current
            val labelStyle = TextStyle(
                color = AMTheme.Text.secondary.resolve(isDark),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AMTheme.Spacing.lg.dp),
                verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
            ) {
                listOf(3, 5, 8).forEach { count ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
                    ) {
                        BasicText(
                            text = "$count pages",
                            style = labelStyle,
                            modifier = Modifier.width(70.dp),
                        )
                        AMPageIndicator(count = count, currentIndex = count / 2)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

}
