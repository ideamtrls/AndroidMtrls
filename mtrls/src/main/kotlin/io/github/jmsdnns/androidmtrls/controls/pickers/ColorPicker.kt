package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// Private — discrete palette for the scoped-palette dialog
// ---------------------------------------------------------------------------

/**
 * The 18-color discrete palette shown inside the [AMColorPicker] dialog.
 *
 * Colors are the Material 700 row (one 700-level tone per Material hue), giving
 * a saturated, evenly-stepped set that reads well against the house surfaces.
 * Opacity (alpha) is locked at 1.0 when [AMColorPicker.supportsOpacity] is
 * false; every color in this list is fully opaque so the restriction is a no-op
 * for the current implementation.
 */
private val amColorPalette: List<Color> = listOf(
    // Warm hues — red through indigo
    Color(0xFFD32F2F),  // Red 700
    Color(0xFFC2185B),  // Pink 700
    Color(0xFF7B1FA2),  // Purple 700
    Color(0xFF512DA8),  // Deep Purple 700
    Color(0xFF303F9F),  // Indigo 700
    Color(0xFF1976D2),  // Blue 700

    // Cool hues — light blue through light green
    Color(0xFF0288D1),  // Light Blue 700
    Color(0xFF0097A7),  // Cyan 700
    Color(0xFF00796B),  // Teal 700
    Color(0xFF388E3C),  // Green 700
    Color(0xFF689F38),  // Light Green 700
    Color(0xFFAFB42B),  // Lime 700

    // Warm accents + neutrals — yellow through grey
    Color(0xFFFBC02D),  // Yellow 700
    Color(0xFFFFA000),  // Amber 700
    Color(0xFFF57C00),  // Orange 700
    Color(0xFFE64A19),  // Deep Orange 700
    Color(0xFF5D4037),  // Brown 700
    Color(0xFF616161),  // Grey 700
)

/**
 * Human-readable names for [amColorPalette] entries, in the same order, used as
 * the accessibility `contentDescription` for each swatch so TalkBack announces
 * the color (e.g. "Indigo") rather than an unlabeled swatch.
 */
private val amColorNames: List<String> = listOf(
    "Red", "Pink", "Purple", "Deep Purple", "Indigo", "Blue",
    "Light Blue", "Cyan", "Teal", "Green", "Light Green", "Lime",
    "Yellow", "Amber", "Orange", "Deep Orange", "Brown", "Grey",
)

/** Number of columns in the palette grid. */
private const val PALETTE_COLUMNS = 6

/** Diameter of each color circle in the palette grid in dp. */
private val SWATCH_SIZE = 36.dp

/** Diameter of the trailing swatch chip in the picker row in dp. Spec §3.3. */
private val ROW_SWATCH_SIZE = 28.dp

/** Padding inside the palette dialog in dp. */
private val DIALOG_PADDING = 20.dp

/** Gap between swatches in the grid in dp. */
private val SWATCH_GAP = 8.dp

// ---------------------------------------------------------------------------
// AMColorPicker — public composable
// ---------------------------------------------------------------------------

/**
 * Flat row widget: [label] text on the leading side, a filled circle swatch
 * on the trailing side. Tapping the swatch (or anywhere on the row) opens a
 * discrete-palette [Dialog] where the user picks a color from a fixed grid
 * of Material-palette swatches.
 *
 * **Android approach:** Android has no system color picker equivalent to
 * SwiftUI's `ColorPicker`. This composable implements a scoped-palette dialog
 * (a grid of preset color circles) as recommended by the controls port spec
 * §3.3. A full HSV wheel or third-party library is explicitly out of scope for
 * the design-system primitive.
 *
 * A flat widget — the caller's card or section provides the surface:
 * ```kotlin
 * AMCard {
 *     AMColorPicker(
 *         label             = "Accent",
 *         selection         = accentColor,
 *         onSelectionChange = { accentColor = it },
 *     )
 * }
 * ```
 *
 * Press feedback is the monochrome `amRipple()` state layer — never opacity-dim.
 *
 * @param label             Leading text label shown in the row.
 * @param selection         Currently selected [Color].
 * @param onSelectionChange Called when the user selects a new color from the
 *                          palette dialog. Receives the chosen [Color].
 * @param supportsOpacity   iOS pass-through flag. When `false`, the dialog
 *                          header notes that opacity is locked; palette colors
 *                          are always fully opaque so this has no visible
 *                          effect in the current palette implementation.
 * @param modifier          Modifier applied to the row's root layout node.
 */
@Composable
fun AMColorPicker(
    label: String,
    selection: Color,
    onSelectionChange: (Color) -> Unit,
    supportsOpacity: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    var dialogOpen by rememberSaveable { mutableStateOf(false) }

    // Press feedback — the monochrome amRipple() state layer, never opacity-dim.
    val interactionSource = remember { MutableInteractionSource() }

    // Human name for the current color (when it is one of the palette entries),
    // so the row announces "$label, Indigo" to TalkBack.
    val currentColorName = amColorPalette.indexOf(selection).let { i ->
        if (i >= 0) amColorNames[i] else null
    }
    val rowDescription = currentColorName?.let { "$label, $it" } ?: label

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AMTheme.rowMinHeight.dp)
            .clickable(
                interactionSource = interactionSource,
                indication        = amRipple(),
                onClick           = { dialogOpen = true },
            )
            // Accessibility: the whole row is the affordance that opens the
            // color picker — name it and mark it a button (the swatch is decorative).
            .semantics {
                role = Role.Button
                contentDescription = rowDescription
            }
            .padding(
                horizontal = tokens.spacing.lg,
                vertical   = tokens.spacing.md,
            ),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // Leading label — headline font, fills remaining width, leading alignment.
        BasicText(
            text     = label,
            modifier = Modifier.weight(1f),
            style    = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
        )

        // Trailing swatch: a plain filled circle of the current color with a
        // hairline ring for edge definition on light colors. Decorative — the
        // row carries the accessibility label. 28.dp.
        Canvas(modifier = Modifier.size(ROW_SWATCH_SIZE)) {
            val d = size.minDimension
            val center = Offset(d / 2f, d / 2f)
            val ringStroke = AMTheme.hairlineWidth.dp.toPx()

            // Filled circle of the current color.
            drawCircle(color = selection, radius = d / 2f, center = center)

            // Hairline ring, inset by half the stroke so it sits crisply on the
            // edge and never bleeds outside the swatch bounds.
            drawCircle(
                color  = AMTheme.separator.resolve(isDark),
                radius = d / 2f - ringStroke / 2f,
                center = center,
                style  = Stroke(width = ringStroke),
            )
        }
    }

    // Scoped-palette dialog.
    if (dialogOpen) {
        AMColorPickerDialog(
            selection         = selection,
            supportsOpacity   = supportsOpacity,
            onSelectionChange = { color ->
                onSelectionChange(color)
                dialogOpen = false
            },
            onDismiss = { dialogOpen = false },
        )
    }
}

// ---------------------------------------------------------------------------
// AMColorPickerDialog — private palette dialog
// ---------------------------------------------------------------------------

/**
 * Discrete-palette dialog for [AMColorPicker].
 *
 * Displays [amColorPalette] as a grid of tappable swatch circles.
 * The currently selected color is indicated by a [AMTheme.selectionRing]
 * border so the user can see which swatch is active.
 *
 * @param selection         Currently selected [Color]; highlighted in the grid.
 * @param supportsOpacity   When `false`, a subtitle note indicates opacity is
 *                          locked. Palette swatches are always fully opaque,
 *                          so this flag has no functional effect in the current
 *                          implementation.
 * @param onSelectionChange Called when the user taps a swatch; receives the
 *                          chosen [Color]. Callers should dismiss the dialog.
 * @param onDismiss         Called when the user dismisses the dialog without
 *                          selecting a color (back press / outside tap).
 */
@Composable
private fun AMColorPickerDialog(
    selection: Color,
    supportsOpacity: Boolean,
    onSelectionChange: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Dialog(onDismissRequest = onDismiss) {
        // Dialog surface — elevated card recipe: fill + shadow + radius.
        val dialogShape = amSquircle(AMTheme.Radius.lg.dp)
        Box(
            modifier = Modifier
                .amElevation(level = AMElevation.Medium, shape = dialogShape)
                .clip(dialogShape)
                .background(AMTheme.Surface.primary.resolve(isDark))
                .padding(DIALOG_PADDING),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.md)) {

                // ── Dialog header ─────────────────────────────────────────────
                BasicText(
                    text  = "Color",
                    style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                )

                if (!supportsOpacity) {
                    BasicText(
                        text  = "Opacity locked",
                        style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                    )
                }

                // ── Palette grid ──────────────────────────────────────────────
                // Lay out rows of PALETTE_COLUMNS swatches. selectableGroup so
                // TalkBack announces the swatches as one exclusive group.
                Column(
                    modifier = Modifier.selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(SWATCH_GAP),
                ) {
                    val rows = amColorPalette.indices.chunked(PALETTE_COLUMNS)
                    rows.forEach { rowIndices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP)) {
                            rowIndices.forEach { i ->
                                val color = amColorPalette[i]
                                AMPaletteSwatch(
                                    color      = color,
                                    name       = amColorNames[i],
                                    isSelected = color == selection,
                                    isDark     = isDark,
                                    onClick    = { onSelectionChange(color) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMPaletteSwatch — private single swatch cell
// ---------------------------------------------------------------------------

/**
 * One tappable color swatch in the palette grid.
 *
 * Selected state is shown by a [AMTheme.selectionRing] border. Press feedback
 * is the monochrome `amRipple()` state layer, clipped to the swatch circle.
 *
 * Accessibility: a [Modifier.selectable] with `role = Role.Button` plus the
 * color [name] as `contentDescription`, so TalkBack announces the color and
 * whether it is currently selected.
 *
 * @param color      The swatch color.
 * @param name       Human-readable color name for the accessibility label.
 * @param isSelected Whether this swatch is the currently selected color.
 * @param isDark     Current dark-theme state for color resolution.
 * @param onClick    Invoked when the swatch is tapped.
 */
@Composable
private fun AMPaletteSwatch(
    color: Color,
    name: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(SWATCH_SIZE)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = AMTheme.selectionRing.resolve(isDark),
                        shape = CircleShape,
                    )
                } else {
                    Modifier.border(
                        width = AMTheme.hairlineWidth.dp,
                        color = AMTheme.separator.resolve(isDark),
                        shape = CircleShape,
                    )
                }
            )
            .selectable(
                selected          = isSelected,
                interactionSource = interactionSource,
                indication        = amRipple(),
                role              = Role.Button,
                onClick           = onClick,
            )
            .semantics { contentDescription = name },
    )
}

// ---------------------------------------------------------------------------
// Color Saver — for rememberSaveable at call sites
// ---------------------------------------------------------------------------

/**
 * A [Saver] that persists a [Color] across process death by encoding the color
 * as its [ULong] bit pattern.
 *
 * Usage:
 * ```kotlin
 * var accent by rememberSaveable(stateSaver = AMColorSaver) {
 *     mutableStateOf(Color(0xFF1976D2))
 * }
 * ```
 */
val AMColorSaver: Saver<Color, Long> = Saver(
    save    = { color -> color.value.toLong() },
    restore = { raw   -> Color(raw.toULong()) },
)

// ---------------------------------------------------------------------------
// Previews — light + dark (row states, not dialog — per spec)
// ---------------------------------------------------------------------------

@Composable
private fun ColorPickerPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Row 1 — with opacity support (default).
        AMColorPicker(
            label             = "Accent",
            selection         = Color(0xFF303F9F),   // Indigo 700
            onSelectionChange = {},
        )

        // Row 2 — opacity locked.
        AMColorPicker(
            label             = "Fill",
            selection         = Color(0xFFFFA000),   // Amber 700
            onSelectionChange = {},
            supportsOpacity   = false,
        )
    }
}

@Preview(
    name            = "AMColorPicker · Light",
    group           = "AMColorPicker",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMColorPickerPreviewLight() {
    AMThemeRoot {
        ColorPickerPreviewContent()
    }
}

@Preview(
    name            = "AMColorPicker · Dark",
    group           = "AMColorPicker",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMColorPickerPreviewDark() {
    AMThemeRoot {
        ColorPickerPreviewContent()
    }
}
