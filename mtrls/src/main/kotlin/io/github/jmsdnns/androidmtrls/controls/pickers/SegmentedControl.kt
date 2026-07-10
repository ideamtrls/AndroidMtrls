package io.github.jmsdnns.androidmtrls.controls.pickers

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.amHairlineWidth
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedFiniteOrNull

// ---------------------------------------------------------------------------
// Motion — Color-/Float-/IntSize-typed lockstep mirrors of the selection fade
// ---------------------------------------------------------------------------

/**
 * Color-typed mirror of [AMTheme.Motion.selectionFade] (150ms, [EaseInOut]) for
 * the selected segment's tonal fill.
 *
 * The house motion token is declared `spring`/`tween<Float>`, while
 * [animateColorAsState] needs `AnimationSpec<Color>`, so the identical
 * parameters are reproduced here (precedent: Toggle.kt `selectionColorFade`). If
 * the token values are ever tuned, this val must be updated in lockstep.
 */
private val selectionColorFade = tween<Color>(durationMillis = 150, easing = EaseInOut)

/**
 * Float-typed mirror of [AMTheme.Motion.selectionFade] for the check icon's
 * fade-in / fade-out. Lockstep with [selectionColorFade].
 */
private val selectionFadeFloat = tween<Float>(durationMillis = 150, easing = EaseInOut)

/**
 * IntSize-typed mirror of [AMTheme.Motion.selectionFade] for the check icon's
 * expand / shrink (the label shifts as the check reserves its slot). Lockstep
 * with [selectionColorFade].
 */
private val selectionExpand = tween<IntSize>(durationMillis = 150, easing = EaseInOut)

/** Height of the segmented group in dp — the M3 segmented-button height. */
private val trackHeight = 40.dp

/** Size of the leading check glyph on the selected segment in dp. */
private val checkIconSize = 18.dp

// ---------------------------------------------------------------------------
// AMSegmentedControl — generic segmented picker (Android segmented-button look)
// ---------------------------------------------------------------------------

/**
 * A generic segmented control that lets users pick one value from a flat list of
 * labeled options — the Android segmented-button anatomy: an **outlined**
 * connected button group of equal-width segments, in the mtrls monochrome
 * palette.
 *
 * The control is **self-framed**: it draws its own outline and interior
 * dividers over the caller's surface — do **not** wrap it in
 * [io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard]. The track itself is
 * transparent (the caller's surface shows through). Apply horizontal
 * screen-edge padding at the call site.
 *
 * **Chrome:** a [trackHeight]-tall row with a stadium (fully rounded) outer
 * outline in [AMTheme.separator] and full-height hairline dividers between
 * segments; the outer ends are rounded, interior segment corners are square.
 * The selected segment carries a tonal grey fill ([AMTheme.neutralFill]) plus a
 * leading [Icons.Default.Check] glyph beside its label — there is no travelling
 * thumb. The fill cross-fades and the check fades/expands in on
 * [AMTheme.Motion.selectionFade]; under Reduce Motion the fill softens and the
 * check's expand snaps.
 *
 * Every segment's label renders in [AMTheme.Type.label] weight and
 * [AMTheme.Text.primary] (selected AND unselected — unselected is never dimmed
 * to invisibility); the fill and check, not a weight change, mark selection.
 *
 * Accessibility: the group is a [Modifier.selectableGroup] and each segment is a
 * [Modifier.selectable] with `role = Role.RadioButton`, so TalkBack announces
 * each segment's selected state and its position within the exclusive group.
 *
 * Usage:
 * ```kotlin
 * var filter by rememberSaveable { mutableStateOf("all") }
 * AMSegmentedControl(
 *     selection = filter,
 *     onSelectionChange = { filter = it },
 *     options = listOf(
 *         AMSegmentedControl.Option("All", "all"),
 *         AMSegmentedControl.Option("Mine", "mine"),
 *         AMSegmentedControl.Option("Archived", "archived"),
 *     ),
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .padding(horizontal = AMTheme.Spacing.lg.dp),
 * )
 * ```
 *
 * @param selection         The currently selected value. Must implement [equals]
 *   for segment-match comparison; use a `data class` or primitive.
 * @param onSelectionChange Called when the user taps a segment, passing the new value.
 * @param options           Ordered list of [AMSegmentedControl.Option] entries.
 *   Each carries a display [AMSegmentedControl.Option.label] and the
 *   [AMSegmentedControl.Option.value] it represents. Values must be unique within
 *   the list.
 * @param modifier          Modifier applied to the outer group container.
 */
@Composable
fun <Value : Any> AMSegmentedControl(
    selection: Value,
    onSelectionChange: (Value) -> Unit,
    options: List<AMSegmentedControl.Option<Value>>,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return

    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current

    // Outlined group — fully rounded ends (stadium), transparent track; an
    // unconditional separator-toned outline defines the group in both themes.
    val groupShape = RoundedCornerShape(percent = 50)
    val selectedFill = AMTheme.neutralFill()
    val outlineColor = AMTheme.separator.resolve(isDark)
    val labelColor = AMTheme.Text.primary.resolve(isDark)
    val dividerWidth = amHairlineWidth()

    // Index of the currently selected option (clamp to 0 if value not found).
    val selectedIndex = options.indexOfFirst { it.value == selection }.coerceAtLeast(0)

    // Selection motion: the tonal fill and check fade soften under Reduce Motion
    // (resolved / resolvedFinite); the check's horizontal expand is a spatial
    // shift, so it collapses to a snap under Reduce Motion (resolvedFiniteOrNull).
    val fillSpec = tokens.motion.resolved(selectionColorFade, reduceMotion)
    val fadeSpec = tokens.motion.resolvedFinite(selectionFadeFloat, reduceMotion)
    val expandSpec = tokens.motion.resolvedFiniteOrNull(selectionExpand, reduceMotion) ?: snap()

    // Per-segment interaction sources hoisted outside the loop, keyed on
    // options.size, so the remember count stays stable if option count changes.
    val interactionSources = remember(options.size) {
        List(options.size) { MutableInteractionSource() }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(groupShape)
            .amHairline(groupShape, color = outlineColor)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            // Full-height hairline divider between adjacent segments.
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(dividerWidth)
                        .fillMaxHeight()
                        .background(outlineColor),
                )
            }

            val isSelected = index == selectedIndex
            val interactionSource = interactionSources[index]

            // Tonal fill cross-fades in when the segment becomes selected.
            val segmentFill by animateColorAsState(
                targetValue = if (isSelected) selectedFill else Color.Transparent,
                animationSpec = fillSpec,
                label = "AMSegmentedControl.fill",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Fill draws behind the ripple (background before selectable);
                    // the group's rounded clip trims the end segments' corners, so
                    // no per-segment clip is needed.
                    .background(segmentFill)
                    .selectable(
                        selected = isSelected,
                        interactionSource = interactionSource,
                        indication = amRipple(),
                        role = Role.RadioButton,
                    ) { onSelectionChange(option.value) }
                    .padding(horizontal = tokens.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Leading check on the selected segment — fades and expands in
                    // beside the label (no travelling thumb marks selection).
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = fadeIn(fadeSpec) + expandHorizontally(expandSpec, clip = false),
                        exit = fadeOut(fadeSpec) + shrinkHorizontally(expandSpec, clip = false),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,   // segment selectable carries a11y
                                modifier = Modifier.size(checkIconSize),
                                colorFilter = ColorFilter.tint(labelColor),
                            )
                            Spacer(modifier = Modifier.width(tokens.spacing.xs))
                        }
                    }

                    BasicText(
                        text = option.label,
                        maxLines = 1,
                        softWrap = false,
                        // Type.label is Medium; selected and unselected share it
                        // (fill + check mark selection, not a weight change). Both
                        // stay Text.primary — unselected is never dimmed away.
                        style = AMTheme.Type.label.toTextStyle(labelColor),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMSegmentedControl object — Option nested class
// ---------------------------------------------------------------------------

/**
 * Namespace for [AMSegmentedControl] supporting types.
 *
 * The composable function `AMSegmentedControl` and this object share the same
 * name — the object provides the [Option] data class as a nested type so
 * callers write `AMSegmentedControl.Option(...)`.
 */
object AMSegmentedControl {

    /**
     * One segment in an [AMSegmentedControl].
     *
     * @param label The display text rendered inside the segment button.
     * @param value The domain value this segment selects. Must be unique within
     *   the `options` list and implement [equals] for selection comparison.
     */
    @Immutable
    data class Option<Value>(
        val label: String,
        val value: Value,
    )
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------

@Composable
private fun SegmentedControlPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Three-option example — first option selected
        AMSegmentedControl(
            selection = "all",
            onSelectionChange = {},
            options = listOf(
                AMSegmentedControl.Option("All", "all"),
                AMSegmentedControl.Option("Mine", "mine"),
                AMSegmentedControl.Option("Archived", "archived"),
            ),
        )

        // Three-option example — middle option selected
        AMSegmentedControl(
            selection = "mine",
            onSelectionChange = {},
            options = listOf(
                AMSegmentedControl.Option("All", "all"),
                AMSegmentedControl.Option("Mine", "mine"),
                AMSegmentedControl.Option("Archived", "archived"),
            ),
        )

        // Two-option example — second option selected
        AMSegmentedControl(
            selection = "week",
            onSelectionChange = {},
            options = listOf(
                AMSegmentedControl.Option("Day", "day"),
                AMSegmentedControl.Option("Week", "week"),
            ),
        )
    }
}

@Preview(
    name = "AMSegmentedControl · Light",
    group = "AMSegmentedControl",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMSegmentedControlPreviewLight() {
    AMThemeRoot {
        SegmentedControlPreviewContent()
    }
}

@Preview(
    name = "AMSegmentedControl · Dark",
    group = "AMSegmentedControl",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMSegmentedControlPreviewDark() {
    AMThemeRoot {
        SegmentedControlPreviewContent()
    }
}
