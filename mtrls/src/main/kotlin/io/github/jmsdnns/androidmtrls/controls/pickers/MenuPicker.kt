package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.jmsdnns.androidmtrls.tokens.AMElevation
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amElevation
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// AMMenuPicker — row widget that opens an anchored popup menu
// ---------------------------------------------------------------------------

/**
 * Row widget that opens an anchored popup menu for picking one of a set of options.
 *
 * A flat row widget — the caller's card/section provides the surface. The row
 * shows a leading [label] slot, a trailing cluster of the currently-selected
 * value display string and a chevron-down glyph. Tapping anywhere on the row
 * opens an anchored [Popup] styled as an elevated surface (same recipe as
 * `AMCard`: `Surface.primary` fill, `radius.lg` corners, `AMElevation.Low`
 * shadow, dark-mode hairline).
 *
 * Selecting an option writes state up via [onSelectionChange] and closes the
 * menu. The selected item shows a leading [Icons.Default.Check] icon, matching
 * the iOS SwiftUI `Label(display(option), systemImage: "checkmark")` recipe.
 *
 * **No card wrap inside the composable.** Wrap with `AMCard` at the call site.
 *
 * Press feedback is the monochrome `amRipple()` state layer on both the trigger
 * row and each menu item — never opacity-dim, per conventions.
 *
 * iOS equivalent: `IMMenuPicker` in `Sources/Controls/Pickers/MenuPicker.swift`.
 *
 * Usage:
 * ```kotlin
 * var sort by rememberSaveable { mutableStateOf("Newest") }
 * AMMenuPicker(
 *     selection = sort,
 *     onSelectionChange = { sort = it },
 *     options = listOf("Newest", "Oldest", "Most replied"),
 *     display = { it },
 * ) {
 *     BasicText("Sort by", style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)))
 * }
 * ```
 *
 * @param selection         The currently selected value.
 * @param onSelectionChange Called when the user picks an option; write the new
 *   value back to caller state to update the row's trailing readout.
 * @param options           The full ordered list of options to present in the menu.
 * @param display           Transforms a [Value] into the display string used for
 *   both the trailing readout and each menu item label.
 * @param modifier          Modifier applied to the trigger row's root layout node.
 * @param label             Composable slot for the leading label area. Receives no
 *   forced styling — apply headline weight and primary color inside the slot.
 */
@Composable
fun <Value : Any> AMMenuPicker(
    selection: Value,
    onSelectionChange: (Value) -> Unit,
    options: List<Value>,
    display: (Value) -> String,
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Measured dimensions of the trigger row — used to anchor and size the popup.
    var rowWidthPx by remember { mutableIntStateOf(0) }
    var rowHeightPx by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        // Trigger row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AMTheme.rowMinHeight.dp)
                .onSizeChanged { size ->
                    rowWidthPx = size.width
                    rowHeightPx = size.height
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = amRipple(),
                    role = Role.Button,
                ) { expanded = !expanded }
                .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
        ) {
            // Leading label — fills remaining width; caller applies headline style
            Box(modifier = Modifier.weight(1f)) {
                label()
            }

            // Trailing cluster: current value text + chevron-down
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
            ) {
                BasicText(
                    text = display(selection),
                    style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
                )
                Image(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    colorFilter = ColorFilter.tint(AMTheme.Icon.muted.resolve(isDark)),
                )
            }
        }

        // Anchored popup menu — positioned immediately below the trigger row,
        // with width matching the trigger row width.
        // Guard: skip until rowWidthPx is known (first-frame measure not yet complete).
        if (expanded && rowWidthPx > 0) {
            Popup(
                offset = IntOffset(0, rowHeightPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                AMMenuPickerDropdown(
                    options = options,
                    selection = selection,
                    display = display,
                    menuWidth = with(density) { rowWidthPx.toDp() },
                    onOptionSelected = { option ->
                        onSelectionChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// String convenience overload
// ---------------------------------------------------------------------------

/**
 * Creates an [AMMenuPicker] with a plain string leading label.
 *
 * Preserves the original public API shape of the iOS `IMMenuPicker(label:…)`
 * convenience init — callers can pass a `String` without writing a composable
 * label slot.
 *
 * @param label             Plain text for the leading label; rendered with
 *   [AMTheme.Type.title] and [AMTheme.Text.primary] color.
 * @param selection         The currently selected value.
 * @param onSelectionChange Called when the user picks a new option.
 * @param options           The full ordered list of options.
 * @param display           Transforms a [Value] into its display string.
 * @param modifier          Modifier applied to the trigger row.
 */
@Composable
fun <Value : Any> AMMenuPicker(
    label: String,
    selection: Value,
    onSelectionChange: (Value) -> Unit,
    options: List<Value>,
    display: (Value) -> String,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    AMMenuPicker(
        selection = selection,
        onSelectionChange = onSelectionChange,
        options = options,
        display = display,
        modifier = modifier,
        label = {
            BasicText(
                text = label,
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
        },
    )
}

// ---------------------------------------------------------------------------
// Private: dropdown menu surface
// ---------------------------------------------------------------------------

/**
 * Elevated popup menu surface for [AMMenuPicker].
 *
 * Styled as an elevated card: `Surface.primary` fill, `radius.lg` corner
 * radius, `AMElevation.Low` two-layer shadow, dark-mode hairline. Rendered
 * inside a [Popup] so it floats above other composables.
 *
 * Each option row shows:
 * - A leading [Icons.Default.Check] icon placeholder — visible only when
 *   [option == selection], matching the iOS checkmark recipe.
 * - The display string for the option.
 *
 * Pressing an item calls [onOptionSelected] (which also closes the popup).
 */
@Composable
private fun <Value : Any> AMMenuPickerDropdown(
    options: List<Value>,
    selection: Value,
    display: (Value) -> String,
    menuWidth: androidx.compose.ui.unit.Dp,
    onOptionSelected: (Value) -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current
    val shape = amSquircle(tokens.radius.lg)

    Column(
        modifier = Modifier
            .width(menuWidth)
            .amElevation(level = AMElevation.Low, shape = shape)
            .background(
                color = AMTheme.Surface.primary.resolve(isDark),
                shape = shape,
            )
            .amCardHairline(shape)
            .padding(vertical = tokens.spacing.xs)
            // Exclusive group so TalkBack announces position-in-group.
            .selectableGroup(),
    ) {
        options.forEach { option ->
            // key(option) ties each row's remembered interaction source to its
            // option identity, so per-row pressed state follows the row rather than
            // its list position if `options` changes length while the popup is open.
            key(option) {
                val isSelected = option == selection
                val itemInteraction = remember { MutableInteractionSource() }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Selectable so TalkBack announces which option is currently
                        // selected (the visual check alone is invisible to TalkBack).
                        .selectable(
                            selected = isSelected,
                            interactionSource = itemInteraction,
                            indication = amRipple(),
                            role = Role.Button,
                        ) { onOptionSelected(option) }
                        .padding(horizontal = tokens.spacing.lg, vertical = tokens.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.sm),
                ) {
                    // Leading check icon — reserves a fixed slot so text aligns
                    Box(modifier = Modifier.size(20.dp)) {
                        if (isSelected) {
                            Image(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                colorFilter = ColorFilter.tint(AMTheme.Text.primary.resolve(isDark)),
                            )
                        }
                    }
                    BasicText(
                        text = display(option),
                        style = AMTheme.Type.body
                            .toTextStyle(AMTheme.Text.primary.resolve(isDark))
                            .copy(fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// The Popup overlay itself cannot render in a static @Preview; these previews
// show the trigger row in its default state (closed) inside a card surface so
// the full visual recipe — including elevation and hairline — is visible.
// ---------------------------------------------------------------------------

@Composable
private fun MenuPickerPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val shape = amSquircle(tokens.radius.lg)

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // String-label convenience overload (the typical usage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .amElevation(level = AMElevation.Low, shape = shape)
                .background(AMTheme.Surface.primary.resolve(isDark), shape)
                .amCardHairline(shape),
        ) {
            AMMenuPicker(
                label = "Sort by",
                selection = "Newest",
                onSelectionChange = {},
                options = listOf("Newest", "Oldest", "Most replied"),
                display = { it },
            )
        }

        // Composable-label overload (generic slot usage)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .amElevation(level = AMElevation.Low, shape = shape)
                .background(AMTheme.Surface.primary.resolve(isDark), shape)
                .amCardHairline(shape),
        ) {
            AMMenuPicker(
                selection = "Oldest",
                onSelectionChange = {},
                options = listOf("Newest", "Oldest", "Most replied"),
                display = { it },
                label = {
                    BasicText(
                        text = "Order",
                        style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
                    )
                },
            )
        }
    }
}

@Preview(
    name = "AMMenuPicker · Light",
    group = "AMMenuPicker",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMMenuPickerPreviewLight() {
    AMThemeRoot {
        MenuPickerPreviewContent()
    }
}

@Preview(
    name = "AMMenuPicker · Dark",
    group = "AMMenuPicker",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMMenuPickerPreviewDark() {
    AMThemeRoot {
        MenuPickerPreviewContent()
    }
}
