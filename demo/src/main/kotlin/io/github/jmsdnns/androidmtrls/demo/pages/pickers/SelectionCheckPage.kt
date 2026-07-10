package io.github.jmsdnns.androidmtrls.demo.pages.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMSelectionCheckBox
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Selection Check catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for `AMSelectionMark` / [AMSelectionCheckBox].
 *
 * id: "pickers-select", section: [ComponentSection.Pickers]. Mirrors iOS
 * `IMSelectionCheckPage` (`Demo/Sources/Catalog/Pages/Pickers.swift`), tile
 * "Select".
 */
val selectionCheckEntry: ComponentEntry = ComponentEntry(
    id       = "pickers-select",
    section  = ComponentSection.Pickers,
    title    = "Selection Check",
    tileTitle = "Select",
    subtitle = "Checkmark for selectable tiles",
    intro    = "The selection checkmark for a selectable photo or album tile — a white " +
        "check-circle with a glyph-legibility shadow so it stays readable over any image. " +
        "Overlay AMSelectionCheckBox on a tile to gate the mark on the top-trailing corner; " +
        "nothing draws while unselected.",
    icon     = Icons.Filled.CheckCircle,
    content  = { SelectionCheckPage() },
)

// ---------------------------------------------------------------------------
// Tile substrates — light, dark, and mid-tone "photo" stand-ins
// ---------------------------------------------------------------------------

// Light, dark, and mid-tone stand-in "photos" so the checkmark's legibility
// shadow reads over each. Colors ported from iOS `IMSelectionCheckPage.tiles`.
private val tileColors: List<Color> = listOf(
    Color(0xFFD9DEE6), // light   — iOS (0.85, 0.87, 0.90)
    Color(0xFF333847), // dark    — iOS (0.20, 0.22, 0.28)
    Color(0xFF9EC7B8), // mid-tone — iOS (0.62, 0.78, 0.72)
)

// ---------------------------------------------------------------------------
// SelectionCheckPage
// ---------------------------------------------------------------------------

/**
 * Demo page for `AMSelectionMark` / [AMSelectionCheckBox].
 *
 * One "Tiles" group of three tappable "photo" tiles — light, dark, and mid-tone
 * — so the mark's two-layer glyph shadow can be read against each. Tapping a
 * tile toggles its selection and the mark appears/disappears through the live
 * `isSelected` binding (no conditional at the call site). The last tile starts
 * unselected, mirroring iOS `IMSelectionCheckPage`.
 */
@Composable
fun SelectionCheckPage() {
    catalogGroup(title = "Tiles") {
        // First two tiles start selected; the last starts unselected (iOS parity).
        val selected = remember { mutableStateListOf(true, true, false) }
        // Per-tile interaction sources, keyed on the (fixed) tile count so the
        // ripple never shares a source across tiles.
        val interactionSources = remember(tileColors.size) {
            List(tileColors.size) { MutableInteractionSource() }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AMTheme.Spacing.lg.dp),
            horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            tileColors.forEachIndexed { i, color ->
                SelectionTile(
                    color = color,
                    isSelected = selected[i],
                    interactionSource = interactionSources[i],
                    onToggle = { selected[i] = !selected[i] },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * One tappable "photo" tile that toggles its selection mark. Applies the tile
 * face (clip + fill) and the `toggleable` affordance to the
 * [AMSelectionCheckBox] wrapper so the mark overlays them.
 */
@Composable
private fun SelectionTile(
    color: Color,
    isSelected: Boolean,
    interactionSource: MutableInteractionSource,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = amSquircle(AMTheme.Radius.md.dp)
    AMSelectionCheckBox(
        isSelected = isSelected,
        modifier = modifier
            .height(96.dp)
            .clip(shape)
            .background(color)
            .toggleable(
                value = isSelected,
                interactionSource = interactionSource,
                indication = amRipple(),
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
    ) {}
}
