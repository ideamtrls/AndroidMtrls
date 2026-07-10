package io.github.jmsdnns.androidmtrls.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.content.AMPageHeader
import io.github.jmsdnns.androidmtrls.content.AMSectionHeader
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogElevationChip
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogRadiusChip
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogSpacingChip
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogSwatch
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogTypeRow
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Foundations section entries
// ---------------------------------------------------------------------------

/**
 * All Foundations section [ComponentEntry] objects.
 *
 * Registered in [CatalogRegistry.all] in the Foundations slot.
 */
val foundationsEntries: List<ComponentEntry> = listOf(
    ComponentEntry(
        id = "foundations-theme",
        section = ComponentSection.Foundations,
        title = "Theme",
        subtitle = "Colors · spacing · radius · elevation",
        intro = "The design vocabulary every other primitive composes on — surface and text colors, semantic tints, the spacing and radius scales, and the elevation shadow recipes.",
        icon = Icons.Default.Palette,
        content = { FoundationsThemePage() },
    ),
    ComponentEntry(
        id = "foundations-typography",
        section = ComponentSection.Foundations,
        title = "Typography",
        subtitle = "Header components & the working type ramp",
        intro = "The two header components — AMPageHeader for screen titles, AMSectionHeader for uppercase group labels, both carrying the amElevation(.text) legibility shadow — shown composed as on a real screen, plus the system type ramp from title down (largeTitle and callout omitted; nothing in the kit uses them).",
        icon = Icons.Default.TextFields,
        showsTitle = false,
        content = { FoundationsTypographyPage() },
    ),
)

// ---------------------------------------------------------------------------
// Theme page
// ---------------------------------------------------------------------------

/**
 * Theme page — surface colors, text/icon colors, decoration tokens, semantic
 * tints, spacing scale, radius scale, and elevation levels.
 *
 * Mirrors iOS `IMThemePage` in `Pages/Foundations.swift` section-for-section.
 */
@Composable
fun FoundationsThemePage() {
    val isDark = LocalAMIsDarkTheme.current

    // ── Surface ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Surface") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            catalogSwatch(
                name = "Surface.primary",
                color = AMTheme.Surface.primary.resolve(isDark),
            )
            catalogSwatch(
                name = "Surface.page",
                color = AMTheme.Surface.page.resolve(isDark),
            )
        }
    }

    // ── Text & Icon ─────────────────────────────────────────────────────────
    catalogGroup(title = "Text & Icon") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            catalogSwatch(
                name = "Text.primary",
                color = AMTheme.Text.primary.resolve(isDark),
                textColor = AMTheme.Text.primary.resolve(isDark),
                showAsText = true,
            )
            catalogSwatch(
                name = "Text.secondary",
                color = AMTheme.Text.secondary.resolve(isDark),
                textColor = AMTheme.Text.secondary.resolve(isDark),
                showAsText = true,
            )
            catalogSwatch(
                name = "Icon.muted",
                color = AMTheme.Icon.muted.resolve(isDark),
                textColor = AMTheme.Icon.muted.resolve(isDark),
                showAsText = true,
            )
            catalogSwatch(
                name = "Icon.disabled",
                color = AMTheme.Icon.disabled.resolve(isDark),
                textColor = AMTheme.Icon.disabled.resolve(isDark),
                showAsText = true,
            )
        }
    }

    // ── Decoration ──────────────────────────────────────────────────────────
    catalogGroup(title = "Decoration") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            catalogSwatch(
                name = "selectionRing",
                color = AMTheme.selectionRing.resolve(isDark),
            )
            catalogSwatch(
                name = "separator",
                color = AMTheme.separator.resolve(isDark),
            )
        }
    }

    // ── Semantic ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Semantic") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            catalogSwatch(name = "destructive", color = AMTheme.destructive.resolve(LocalAMIsDarkTheme.current))
            catalogSwatch(name = "success", color = AMTheme.success.resolve(LocalAMIsDarkTheme.current))
            catalogSwatch(name = "warning", color = AMTheme.warning.resolve(LocalAMIsDarkTheme.current))
        }
    }

    // ── Spacing ──────────────────────────────────────────────────────────────
    catalogGroup(title = "Spacing") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            catalogSpacingChip("xxl", AMTheme.Spacing.xxl)
            catalogSpacingChip("xl", AMTheme.Spacing.xl)
            catalogSpacingChip("lg", AMTheme.Spacing.lg)
            catalogSpacingChip("md", AMTheme.Spacing.md)
            catalogSpacingChip("sm", AMTheme.Spacing.sm)
            catalogSpacingChip("xs", AMTheme.Spacing.xs)
        }
    }

    // ── Radius ────────────────────────────────────────────────────────────────
    catalogGroup(title = "Radius") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            catalogRadiusChip("lg", AMTheme.Radius.lg)
            catalogRadiusChip("md", AMTheme.Radius.md)
            catalogRadiusChip("sm", AMTheme.Radius.sm)
        }
    }

    // ── Elevation ─────────────────────────────────────────────────────────────
    catalogGroup(title = "Elevation") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
        ) {
            catalogElevationChip("medium", AMElevation.Medium)
            catalogElevationChip("low", AMElevation.Low)
            catalogElevationChip("none", AMElevation.None)
        }
    }
}

// ---------------------------------------------------------------------------
// Typography page
// ---------------------------------------------------------------------------

/**
 * Typography page — page header + section header specimens, then the full
 * type ramp from title down to caption2 (largeTitle and callout omitted;
 * nothing in the kit uses them).
 *
 * `showsTitle = false` is handled by [ComponentPage] at the call site in
 * [CatalogRegistry]: the entry's [ComponentEntry.content] is composed inside
 * `ComponentPage(entry, showsTitle = false)`. The first visible content on
 * this page IS the specimen headers — every header is a specimen.
 *
 * Mirrors iOS `IMTypographyPage` in `Pages/Foundations.swift`.
 */
@Composable
fun FoundationsTypographyPage() {
    val isDark = LocalAMIsDarkTheme.current
    val textPrimary = AMTheme.Text.primary.resolve(isDark)

    // ── Header specimens — live components shown at the top of the page ──────
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
    ) {
        AMPageHeader("Page Header")
        AMSectionHeader("Section Header")
    }

    // ── Type ramp — largest to smallest ──────────────────────────────────────
    // largeTitle (34sp) and callout (16sp) are omitted — nothing in the kit
    // uses them, matching the iOS page comment verbatim.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        catalogTypeRow(
            name = "title",
            style = TextStyle(
                color = textPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "title2",
            style = TextStyle(
                color = textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "title3",
            style = TextStyle(
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "headline",
            style = TextStyle(
                color = textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        catalogTypeRow(
            name = "body",
            style = TextStyle(
                color = textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "subheadline",
            style = TextStyle(
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "footnote",
            style = TextStyle(
                color = textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "caption",
            style = TextStyle(
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
        catalogTypeRow(
            name = "caption2",
            style = TextStyle(
                color = textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}
