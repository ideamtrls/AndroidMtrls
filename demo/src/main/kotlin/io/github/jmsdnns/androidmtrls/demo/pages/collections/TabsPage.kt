package io.github.jmsdnns.androidmtrls.demo.pages.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMTab
import io.github.jmsdnns.androidmtrls.collections.AMTabs
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMTabs] / [AMTab].
 *
 * id: "collections-tabs", section: [ComponentSection.Collections].
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Collections
 * slot. This file intentionally does not touch any shared registry —
 * registration is the task owner's responsibility per the task brief.
 */
val tabsEntry: ComponentEntry = ComponentEntry(
    id = "collections-tabs",
    section = ComponentSection.Collections,
    title = "Tabs",
    subtitle = "In-page content switcher",
    intro = "In-page content switcher with a horizontal tab strip. Segments content " +
        "within a screen — not a bottom tab bar (AndroidMtrls intentionally has no " +
        "TabBar primitive).",
    icon = Icons.Default.Tab,
    content = { TabsPage() },
)

// ---------------------------------------------------------------------------
// TabsPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMTabs] / [AMTab].
 *
 * One group per demo spec §3.9:
 *
 * - **Default** — Posts/Replies/Media tabs with short body-text content in
 *   each pane. The [AMTabs] component is constrained to 140dp height.
 *   Selection is held in [rememberSaveable].
 */
@Composable
fun TabsPage() {

    // ── Default ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        // AMTabs draws its own inset rounded panel — host it on the page
        // background with the lg page margin (iOS), not a full-bleed strip.
        Box(modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp)) {
            val isDark = LocalAMIsDarkTheme.current
            val bodyStyle = TextStyle(
                fontSize = 15.sp,
                color = AMTheme.Text.primary.resolve(isDark),
            )

            var currentTab by rememberSaveable { mutableStateOf("posts") }

            AMTabs(
                tabs = listOf(
                    AMTab(id = "posts", label = "Posts") {
                        BasicText(
                            text = "Posts feed lives here — long-form, link-outs, quote-replies.",
                            style = bodyStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AMTheme.Spacing.lg.dp),
                        )
                    },
                    AMTab(id = "replies", label = "Replies") {
                        BasicText(
                            text = "Replies — your responses to other accounts in chronological order.",
                            style = bodyStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AMTheme.Spacing.lg.dp),
                        )
                    },
                    AMTab(id = "media", label = "Media") {
                        BasicText(
                            text = "Media — just the posts that include an image or video attachment.",
                            style = bodyStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AMTheme.Spacing.lg.dp),
                        )
                    },
                ),
                selection = currentTab,
                onSelectionChange = { currentTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
        }
    }

}
