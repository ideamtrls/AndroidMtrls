package io.github.jmsdnns.androidmtrls.demo.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.collections.AMCarousel
import io.github.jmsdnns.androidmtrls.collections.AMPageIndicator
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.demo.catalog.CatalogRegistry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amHairline

// ---------------------------------------------------------------------------
// CatalogHomeScreen
// ---------------------------------------------------------------------------

/**
 * The catalog home screen — a grouped tile grid of all component sections and
 * their entries.
 *
 * Mirrors the iOS `CatalogRoot` view. Sections appear in [ComponentSection.allCases]
 * order; only sections that have at least one registered entry are rendered.
 * The Screens section leads as a scene carousel band (see [SectionBand]) once its
 * entries register; while empty it is skipped like any other empty section.
 *
 * @param onEntryClick Called when the user taps a catalog tile. The caller
 *                     (MainActivity via DemoNavHost) navigates to the entry page.
 */
@Composable
fun CatalogHomeScreen(onEntryClick: (ComponentEntry) -> Unit) {
    val isDark = LocalAMIsDarkTheme.current
    val pageColor = AMTheme.Surface.page.resolve(isDark)

    // Group all registered entries by section, preserving curated order
    val bySection: Map<ComponentSection, List<ComponentEntry>> =
        CatalogRegistry.all.groupBy { it.section }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // In-body large title — stands in for iOS large nav title
            item {
                AppTitleHeader()
            }

            // One band per section that has entries; skip empty sections
            ComponentSection.allCases.forEach { section ->
                val entries = bySection[section]
                if (entries.isNullOrEmpty()) return@forEach

                item(key = section.name) {
                    SectionBand(
                        section = section,
                        entries = entries,
                        onEntryClick = onEntryClick,
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(AMTheme.Spacing.lg.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AppTitleHeader — large in-body title (iOS large nav title equivalent)
// ---------------------------------------------------------------------------

@Composable
private fun AppTitleHeader() {
    val isDark = LocalAMIsDarkTheme.current
    val textPrimary = AMTheme.Text.primary.resolve(isDark)

    BasicText(
        text = "AndroidMtrls Components",
        style = TextStyle(
            color = textPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = AMTheme.Spacing.lg.dp,
                end = AMTheme.Spacing.lg.dp,
                top = AMTheme.Spacing.lg.dp,
                bottom = AMTheme.Spacing.lg.dp,
            ),
    )
}

// ---------------------------------------------------------------------------
// SectionBand — one AMSectionHeader + tile grid
// ---------------------------------------------------------------------------

@Composable
private fun SectionBand(
    section: ComponentSection,
    entries: List<ComponentEntry>,
    onEntryClick: (ComponentEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Section header — AMSectionHeader owns casing/tracking/color
        Box(modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp)) {
            AMSectionHeader(title = section.displayName)
        }

        if (section == ComponentSection.Screens) {
            // Scenes lead the catalog as a one-at-a-time carousel of wide cards,
            // not a tile grid (iOS SceneCarouselBand). With scenesEntries empty
            // this branch never runs — the empty-section skip in CatalogHomeScreen
            // covers it — so the band only appears once scenes register.
            SceneCarouselBand(
                entries = entries,
                onEntryClick = onEntryClick,
            )
        } else {
            // Lists section uses 2 columns (iOS comment: "deliberate 2×2"); others use 3
            val columns = if (section == ComponentSection.Lists) 2 else 3
            EntryTileGrid(
                entries = entries,
                columns = columns,
                onEntryClick = onEntryClick,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SceneCarouselBand — one-at-a-time carousel of wide scene cards + indicator
// ---------------------------------------------------------------------------

/**
 * The Scenes band: an [AMCarousel] of 140dp-tall scene cards with an
 * [AMPageIndicator] beneath, mirroring the iOS `SceneCarouselBand`.
 *
 * The pager clips to its bounds, so overflow can no longer bleed past the
 * carousel to the screen edge. To keep the resting card flush with the page
 * margin *and* let the next card peek all the way out, the band measures its
 * available width `W` ([BoxWithConstraints]), widens the carousel to
 * `W + 2·(xxl − lg)`, and shifts it left by `2·(xxl − lg)` — half from
 * [Alignment.Center] placing the oversized pager at `−(xxl − lg)`, half from the
 * explicit [offset]. A wider-than-default `sideInset` (`2·xxl − lg`) holds each
 * card at its original size. The net geometry lands the resting card's leading
 * edge on the `lg` page margin while the pager's right bound sits on the screen
 * edge, so the right-edge peek paints to the edge. Tapping a card routes through
 * the same [onEntryClick] as the tile grid.
 */
@Composable
private fun SceneCarouselBand(
    entries: List<ComponentEntry>,
    onEntryClick: (ComponentEntry) -> Unit,
) {
    val tokens = LocalAMTokens.current
    var index by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
    ) {
        // maxWidth is the band's available width W; the carousel is widened past
        // it and centered, so the oversized pager starts at −δ before the offset.
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // δ = xxl − lg — the spare inset the old offset trick reclaimed.
            val delta = tokens.spacing.xxl - tokens.spacing.lg
            AMCarousel(
                items = entries,
                key = { it.id },
                currentIndex = index,
                onIndexChange = { index = it },
                // s = 2·xxl − lg keeps page width = Wp − 2s = W − 2·xxl (card unchanged).
                sideInset = tokens.spacing.xxl + delta,
                modifier = Modifier
                    // Extra −δ shift; with the Center placement it lands the pager
                    // at −2δ..W, right bound on the screen edge, card left at lg.
                    .offset(x = -delta)
                    // Wp = W + 2δ, wide enough that the peek paints to the edge.
                    .requiredWidth(maxWidth + delta * 2)
                    .height(140.dp),
            ) { entry ->
                SceneCard(entry = entry, onClick = { onEntryClick(entry) })
            }
        }

        AMPageIndicator(count = entries.size, currentIndex = index)
    }
}

// ---------------------------------------------------------------------------
// SceneCard — wide carousel card: icon top-start, title + one-line pitch
// ---------------------------------------------------------------------------

/**
 * Wide scene card for [SceneCarouselBand]: icon anchored top-start, then the
 * title and one-line subtitle pushed to the bottom. Fills the carousel slot's
 * width and the band's 140dp height.
 *
 * Shadowless [AMCard] (`elevation = None`) with an explicit unconditional
 * [amHairline] for definition in both schemes (iOS `imCard(elevation: .none)`
 * + `imHairline`).
 */
@Composable
private fun SceneCard(
    entry: ComponentEntry,
    onClick: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val shape = RoundedCornerShape(tokens.radius.lg)
    val interactionSource = remember { MutableInteractionSource() }

    AMCard(
        modifier = Modifier
            .fillMaxSize()
            // Monochrome ripple press feedback — house rule: every affordance
            // carries `indication = amRipple()`; the clip bounds the sweep to
            // the card's corner radius. The explicit hairline draws over the
            // fill.
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = amRipple(),
                onClick = onClick,
            )
            .amHairline(shape),
        padding = AMPadding.block,
        elevation = AMElevation.None,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
        ) {
            Image(
                imageVector = entry.icon ?: Icons.Filled.AutoAwesome,
                contentDescription = null,
                colorFilter = ColorFilter.tint(AMTheme.Text.primary.resolve(isDark)),
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            BasicText(
                text = entry.title,
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text = entry.subtitle ?: "",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// EntryTileGrid — manual row chunking (no nested LazyVerticalGrid)
// ---------------------------------------------------------------------------

/**
 * Three-column (or two-column for Lists) grid of compact action tiles.
 *
 * Built from [entries].chunked([columns]) rows — each row is a [Row] with
 * [Arrangement.spacedBy] and each cell has [Modifier.weight(1f)]. The last
 * row is padded with [Spacer] cells to keep tiles equal-width.
 *
 * Avoids nesting a [LazyVerticalGrid] inside the parent [LazyColumn].
 */
@Composable
private fun EntryTileGrid(
    entries: List<ComponentEntry>,
    columns: Int,
    onEntryClick: (ComponentEntry) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        entries.chunked(columns).forEach { rowEntries ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
            ) {
                rowEntries.forEach { entry ->
                    Box(modifier = Modifier.weight(1f)) {
                        AMActionGridButton(
                            icon = entry.icon ?: Icons.Filled.AutoAwesome,
                            title = entry.effectiveTileTitle,
                            elevation = AMElevation.None,
                            onClick = { onEntryClick(entry) },
                        )
                    }
                }
                // Pad incomplete last row so tiles stay equal width
                repeat(columns - rowEntries.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
