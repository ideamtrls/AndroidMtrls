package io.github.jmsdnns.androidmtrls.collections

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.resolved

// ---------------------------------------------------------------------------
// Motion — Color-typed lockstep mirror of the selection-commit fade
// ---------------------------------------------------------------------------

/**
 * Colour cross-fade for a chip's fill (neutral ↔ accent) and its label
 * (primary ↔ white).
 *
 * Lockstep mirror of [AMTheme.Motion.selectionFade] (150ms, [EaseInOut]) —
 * the house selection-commit curve, matching iOS `IMChipGroup`'s
 * `.animation(selectionFade, value: isSelected)`. The token is declared as
 * `AnimationSpec<Float>` and [animateColorAsState] needs `AnimationSpec<Color>`,
 * so the identical parameters are reproduced here as a `tween<Color>`. If the
 * token values are ever tuned, this val must be updated in lockstep.
 */
private val selectionColorFade = tween<Color>(durationMillis = 150, easing = EaseInOut)

// ---------------------------------------------------------------------------
// AMChipGroup — String-label overload (convenience path)
// ---------------------------------------------------------------------------

/**
 * Wrapping flow of selectable chip buttons. Each chip shows a [String] label
 * derived from [label]; tap a chip to toggle membership in [selection].
 *
 * Chips wrap to the next line when a row overflows the container width using
 * a greedy left-to-right algorithm ([amComputeFlowLayout]).
 *
 * Visual treatment is fixed for consistency (iOS capsule filter-chip):
 * - [AMTheme.Type.label] label text (14sp Medium)
 * - Chip height 32dp; horizontal padding 12dp
 * - `RoundedCornerShape(50)` — a full capsule (pill)
 * - **Unselected:** [AMTheme.neutralFill] fill (monochrome 8% grey), **no** outline
 * - **Selected:** solid `tokens.semantic.accent` fill with **white** label text
 *   (primary-on-accent), **no** outline, **no** check glyph
 * - Fill (neutral ↔ accent) and label (primary ↔ white) crossfade on selection
 *   commit via [AMTheme.Motion.selectionFade]
 * - Press feedback: monochrome [amRipple] state layer clipped to the capsule
 *
 * iOS equivalent: `IMChipGroup(_ items:id:label:selection:spacing:)` — selected
 * chip is `Color.accentColor` fill with white text.
 *
 * @param items            Ordered list of data items to render as chips.
 * @param key              Stable unique identity for each item, used as the
 *                         toggle key in [selection]. Mirrors Swift's `KeyPath<Item, ID>`.
 * @param label            Extracts the display string from an item.
 * @param selection        Current set of selected keys.
 * @param onSelectionChange Called when the user taps a chip; receives the
 *                          updated [Set] with the key toggled.
 * @param spacing          Gap between chips horizontally and between rows
 *                         vertically. Defaults to `tokens.spacing.sm` (8dp).
 * @param modifier         Layout modifier applied to the flow container.
 */
@Composable
fun <T> AMChipGroup(
    items: List<T>,
    key: (T) -> Any,
    label: (T) -> String,
    selection: Set<Any>,
    onSelectionChange: (Set<Any>) -> Unit,
    spacing: Dp? = null,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current
    // Label color crossfades primary → white as a chip becomes selected
    // (white-on-accent), on the same selection-commit curve as the fill and
    // routed through the house Reduce-Motion policy.
    val labelColorSpec = tokens.motion.resolved(selectionColorFade, reduceMotion)
    AMChipGroup(
        items = items,
        key = key,
        selection = selection,
        onSelectionChange = onSelectionChange,
        spacing = spacing,
        modifier = modifier,
        chipLabel = { item ->
            val isSelected = selection.contains(key(item))
            val labelColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else AMTheme.Text.primary.resolve(),
                animationSpec = labelColorSpec,
                label = "chipLabelColor",
            )
            BasicText(
                text = label(item),
                style = AMTheme.Type.label.toTextStyle(labelColor),
            )
        },
    )
}

// ---------------------------------------------------------------------------
// AMChipGroup — composable-slot overload (designated / flexible path)
// ---------------------------------------------------------------------------

/**
 * Wrapping flow of selectable chip buttons with a fully custom chip label slot.
 *
 * The [chipLabel] slot is called once per item; the chip's chrome (capsule fill,
 * height, shape, press feedback) is applied around it identically to the
 * [String]-convenience overload. Because [AMChipGroup] has no content-color
 * CompositionLocal, callers style the label themselves — use white
 * ([Color.White]) for a **selected** chip (white-on-accent) and
 * [AMTheme.Text.primary] otherwise. Selection is conveyed by the fill
 * (neutral → accent) plus this label-color swap; there is no check glyph.
 * Callers can read the same [selection] set to decide the color.
 *
 * Preferred for items with icons or compound labels.
 *
 * iOS equivalent: `IMChipGroup(_ items:id:selection:spacing:chipLabel:)`.
 *
 * @param items            Ordered list of data items to render as chips.
 * @param key              Stable unique identity for each item.
 * @param selection        Current set of selected keys.
 * @param onSelectionChange Called when the user taps a chip; receives the
 *                          updated [Set] with the key toggled.
 * @param spacing          Gap between chips. Defaults to `tokens.spacing.sm` (8dp).
 * @param modifier         Layout modifier applied to the flow container.
 * @param chipLabel        Content rendered inside the chip for each item.
 *                         Receives the item; must apply its own text color
 *                         (white when selected, [AMTheme.Text.primary] otherwise)
 *                         because [AMChipGroup] has no content-color CompositionLocal.
 */
@Composable
fun <T> AMChipGroup(
    items: List<T>,
    key: (T) -> Any,
    selection: Set<Any>,
    onSelectionChange: (Set<Any>) -> Unit,
    spacing: Dp? = null,
    modifier: Modifier = Modifier,
    chipLabel: @Composable (T) -> Unit,
) {
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current
    val effectiveSpacing = spacing ?: tokens.spacing.sm

    // Hoist interaction sources — one per chip, keyed on items.size per
    // conventions (never inside a loop with variable trip count).
    val interactionSources = remember(items.size) {
        List(items.size) { MutableInteractionSource() }
    }

    // Resolve adaptive colors once at the composable boundary (read CompositionLocals).
    val isDark = LocalAMIsDarkTheme.current
    val unselectedFill = AMTheme.neutralFill()
    val selectedFill = tokens.semantic.accent.resolve(isDark)

    // Fill cross-fade routes through the house Reduce-Motion policy
    // (tokens/Motion.kt): under RM the selection fade softens to the overlay-fade
    // tween. A fade (no spatial indicator), so resolved (soften), never
    // resolvedOrNull. Read once; the spec is chip-index-independent. Mirrors
    // AMSegmentedControl.
    val colorSpec = tokens.motion.resolved(selectionColorFade, reduceMotion)

    // Full capsule (pill) — matches the iOS `Capsule` chip and the system's
    // capsule vocabulary (AMPageIndicator, tags).
    val chipShape = RoundedCornerShape(50)

    AMFlowLayout(
        spacing = effectiveSpacing,
        modifier = modifier,
    ) {
        items.forEachIndexed { index, item ->
            val k = key(item)
            val isSelected = selection.contains(k)
            val interactionSource = interactionSources[index]

            // Fill crossfades neutral grey → accent on selection commit.
            val fillColor by animateColorAsState(
                targetValue = if (isSelected) selectedFill else unselectedFill,
                animationSpec = colorSpec,
                label = "chipFillColor",
            )

            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(chipShape)
                    .background(fillColor, chipShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = amRipple(),
                        onClick = {
                            val updated = if (selection.contains(k)) {
                                selection - k
                            } else {
                                selection + k
                            }
                            onSelectionChange(updated)
                        },
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                chipLabel(item)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMFlowLayout — custom Layout using amComputeFlowLayout
// ---------------------------------------------------------------------------

/**
 * Internal custom [Layout] that implements greedy left-to-right wrapping via
 * [amComputeFlowLayout]. Measures all children unconstrained in width
 * (mirroring `subview.sizeThatFits(.unspecified)` in the iOS `FlowLayout`),
 * then places them using the computed pixel positions.
 *
 * @param spacing  Gap between chips and between rows, in dp.
 * @param modifier Layout modifier.
 * @param content  Child composables (chips).
 */
@Composable
private fun AMFlowLayout(
    spacing: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        // Measure each child unconstrained in width and free of any inherited
        // minHeight floor (mirrors iOS sizeThatFits(.unspecified)); a tight
        // incoming minHeight would otherwise stretch every chip to a full-height
        // slab. maxHeight stays inherited.
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = Int.MAX_VALUE)) }

        val spacingPx = spacing.roundToPx()
        val maxWidthPx = constraints.maxWidth

        val itemSizes = placeables.map { AMFlowItemSize(width = it.width, height = it.height) }
        val flowResult = amComputeFlowLayout(
            itemSizes = itemSizes,
            maxWidth = maxWidthPx,
            spacingPx = spacingPx,
        )

        val totalWidth = if (placeables.isEmpty()) 0 else flowResult.totalSize.width
        val totalHeight = if (placeables.isEmpty()) 0 else flowResult.totalSize.height

        layout(width = totalWidth.coerceAtLeast(constraints.minWidth), height = totalHeight.coerceAtLeast(constraints.minHeight)) {
            placeables.forEachIndexed { index, placeable ->
                val pos = flowResult.positions[index]
                placeable.placeRelative(x = pos.x, y = pos.y)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

private data class PreviewGenre(val id: Int, val name: String)

private val previewGenres = listOf(
    PreviewGenre(1, "Tech"),
    PreviewGenre(2, "Long-form"),
    PreviewGenre(3, "Interview"),
    PreviewGenre(4, "Daily"),
    PreviewGenre(5, "Science"),
    PreviewGenre(6, "Comedy"),
    PreviewGenre(7, "History"),
    PreviewGenre(8, "Design"),
    PreviewGenre(9, "Engineering"),
)

@Composable
private fun ChipGroupPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    // Mixed selection (2, 4) so previews show selected + unselected chips side by side.
    var selection by remember { mutableStateOf<Set<Any>>(setOf(2, 4)) }

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AMChipGroup(
            items = previewGenres,
            key = { it.id },
            label = { it.name },
            selection = selection,
            onSelectionChange = { selection = it },
        )
    }
}

@Preview(
    name = "AMChipGroup · Light",
    group = "AMChipGroup",
    widthDp = 320,
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMChipGroupPreviewLight() {
    AMThemeRoot {
        ChipGroupPreviewContent()
    }
}

@Preview(
    name = "AMChipGroup · Dark",
    group = "AMChipGroup",
    widthDp = 320,
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMChipGroupPreviewDark() {
    AMThemeRoot {
        ChipGroupPreviewContent()
    }
}
