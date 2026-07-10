package io.github.jmsdnns.androidmtrls.demo.pages.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.controls.buttons.AMSecondaryButton
import io.github.jmsdnns.androidmtrls.demo.LocalDemoImmersive
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentPage
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.lists.AMListRow
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMPanelHost
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMSection
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMSectionStyle
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Panel catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMPanelHost] and [io.github.jmsdnns.androidmtrls.surfaces.frames.AMPanel].
 *
 * id: "frames-panel", section: Frames.
 *
 * **Registration:** add this entry to [CatalogRegistry.all] in the Frames slot.
 * This file intentionally does not touch any shared registry — registration is
 * the task owner's responsibility per the task brief.
 */
val panelEntry: ComponentEntry = ComponentEntry(
    id = "frames-panel",
    section = ComponentSection.Frames,
    title = "Panel",
    subtitle = "Trailing-edge detail panel",
    intro = "Modal detail panel that slides in from the trailing edge behind a native app bar — " +
        "back arrow, left-aligned title, and a hairline under the bar. Use the system back " +
        "gesture to dismiss. The flatter cousin to AMSheet.",
    icon = Icons.Default.ViewSidebar,
    // AMPanelHost is a full-coverage host: its panel slides in over the whole
    // viewport. Like ActionsOverlay it must wrap ComponentPage (not attach inside
    // the scaffold's scroll column), so the entry owns its scaffold. Attaching
    // inside the scroll column feeds the panel infinite height and crashes.
    ownsScaffold = true,
    content = { PanelPage() },
)

// ---------------------------------------------------------------------------
// PanelPage
// ---------------------------------------------------------------------------

private data class PanelRow(val number: Int, val title: String, val subtitle: String)

private val panelRows = listOf(
    PanelRow(1, "Item one",   "AMPanel hosts arbitrary content"),
    PanelRow(2, "Item two",   "Same surface treatment as the catalog"),
    PanelRow(3, "Item three", "Scrollable when the content overflows"),
)

/**
 * Demo page for [io.github.jmsdnns.androidmtrls.surfaces.frames.AMPanel] and [AMPanelHost].
 *
 * One group per the iOS demo spec §3.5:
 * - **Default** — an "Open panel" secondary button that presents a panel slide-in
 *   via [AMPanelHost]. The panel body contains a descriptive text section and
 *   [AMListRow] list rows mirroring iOS content.
 *
 * The entry sets `ownsScaffold = true`, so this composable — not `DemoNavHost` —
 * calls [ComponentPage], and [AMPanelHost] wraps the scaffold. That gives the host
 * the full page viewport (a host attached inside the scaffold's unbounded scroll
 * column would feed the panel infinite height and crash). Mirrors the
 * ActionsOverlay page.
 */
@Composable
fun PanelPage() {
    var showPanel by rememberSaveable { mutableStateOf(false) }

    // Drive the shell into immersive mode while the panel is up, so the catalog
    // top bar hides and the panel presents with a single title bar (matching iOS,
    // where a pushed panel covers the nav bar). AMPanelHost only fills the NavHost
    // area — the shell top bar is a sibling above it, so it must be hidden here
    // rather than covered. Reset on dismiss, and on leaving the back stack.
    val setImmersive = LocalDemoImmersive.current
    LaunchedEffect(showPanel) { setImmersive(showPanel) }
    DisposableEffect(Unit) { onDispose { setImmersive(false) } }

    // AMPanelHost wraps ComponentPage (the whole page viewport) so the panel
    // overlay covers the full page bounds when it slides in.
    AMPanelHost(
        isPresented = showPanel,
        title = "Panel",
        onDismiss = { showPanel = false },
        panelContent = { PanelDemoContent() },
    ) {
        ComponentPage(entry = panelEntry) {
            // ── Page body ─────────────────────────────────────────────────
            catalogGroup(title = "Default") {
                AMSecondaryButton(
                    label = "Open panel",
                    onClick = { showPanel = true },
                    modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PanelDemoContent — content hosted inside the demo panel
// ---------------------------------------------------------------------------

/**
 * Content body rendered inside the demo [AMPanelHost] panel.
 *
 * Mirrors iOS IMPanelPage panel content: a flat descriptive text section
 * ("Text") followed by a "List" header with [AMListRow] rows (icon leading +
 * subtitle), matching the iOS title/subtitle/icon pattern.
 */
@Composable
private fun PanelDemoContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(modifier = Modifier.height(AMTheme.Spacing.lg.dp))

        AMSection("Text", style = AMSectionStyle.Flat) {
            BasicText(
                text = "Tapping \"Open panel\" pushed this view in from the trailing edge. " +
                    "Tap the back arrow in the app bar, or use the system back " +
                    "gesture, to dismiss it.",
                style = TextStyle(
                    color = AMTheme.Text.primary.resolve(isDark),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(AMTheme.Spacing.xl.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp)) {
                AMSectionHeader("List")
            }

            Spacer(modifier = Modifier.height(AMTheme.Spacing.md.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                panelRows.forEach { item ->
                    AMListRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        leading = {
                            // Solid info-blue circle with a white numeral — mirrors
                            // iOS `Image(systemName: "\(id).circle.fill")` tinted to
                            // the accent, not the hollow rounded-square Looks glyphs.
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(AMTheme.Semantic.info.resolve(isDark), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                BasicText(
                                    text = item.number.toString(),
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AMTheme.Spacing.lg.dp))
    }
}
