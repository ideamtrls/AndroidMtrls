package io.github.jmsdnns.androidmtrls.surfaces.frames

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMDivider
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite

// ---------------------------------------------------------------------------
// Private animation spec
// ---------------------------------------------------------------------------

/**
 * Bespoke expand/collapse spring for disclosure toggle — sits between
 * `AMTheme.Motion.snapSpring` (response≈0.32s, stiffness≈384) and
 * `AMTheme.Motion.presentSpring` (response≈0.35s, stiffness≈321) so the
 * content reveal breathes without lagging the chevron.
 *
 * iOS equivalent: `disclosureToggleSpring = .spring(response: 0.35, dampingFraction: 0.86)`.
 * Kotlin conversion: stiffness = (2π / 0.35)² ≈ 321; dampingRatio = 0.86.
 */
// IMPORTANT: disclosureToggleSpring and disclosureSlideSpring must keep identical
// dampingRatio and stiffness so the chevron-rotation fade and the content
// slide-in/out animations finish together. Changing one without the other
// causes a visible split — the content slides while the opacity is still
// animating, or vice versa. Both route through the shared Reduce-Motion policy,
// which collapses each to the same tween(180) under Reduce Motion, so the
// lockstep holds there too — never route one via resolvedOrNull (→ snap) while
// the other softens, or they desync.
private val disclosureToggleSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness    = 321f,
)

/** Same spring as [disclosureToggleSpring], typed for the offset-based slide. */
private val disclosureSlideSpring = spring<IntOffset>(
    dampingRatio = 0.86f,
    stiffness    = 321f,
)

// ---------------------------------------------------------------------------
// AMDisclosure — hoisted (external state) overload
// ---------------------------------------------------------------------------

/**
 * Surface-card disclosure group — hoisted variant with external state control.
 *
 * Renders a card row with the caller's [label] leading and a rotating chevron
 * trailing. Tapping the header row toggles [expanded] via [onExpandedChange].
 * When [expanded] is `true` the content slot animates in below a hairline
 * divider inside the same card surface.
 *
 * The card chrome is provided by [AMCard], which participates in the
 * [LocalAMSurfaceStyle] nesting contract: at the root of a screen the card
 * renders as `Elevated` (fill + shadow + dark-mode hairline); when placed
 * inside another card it auto-demotes to `Flat` (no fill, no shadow, no
 * hairline), preventing shadow stacking.
 *
 * Header press feedback is the monochrome `amRipple()` state layer, rect-bounded
 * inside the clipped [AMCard] — no opacity dim.
 *
 * iOS equivalent: `IMCardDisclosureGroupStyle` via `.disclosureGroupStyle(.imCard)`.
 *
 * ```kotlin
 * var expanded by remember { mutableStateOf(false) }
 * AMDisclosure(
 *     label = { BasicText("Advanced options") },
 *     expanded = expanded,
 *     onExpandedChange = { expanded = it },
 * ) {
 *     BasicText("Option A")
 *     BasicText("Option B")
 * }
 * ```
 *
 * @param label            Composable label rendered in the header row, leading side.
 *                         Use [BasicText] with your preferred style; the String
 *                         convenience overloads apply headline style automatically.
 * @param expanded         Current expanded state. `true` = content visible.
 * @param onExpandedChange Called when the user taps the header row. Update [expanded] here.
 * @param modifier         Modifier applied to the card's root layout node.
 * @param content          The composable content slot revealed when [expanded] is `true`.
 *                         Content is padded to 16dp horizontal / 12dp vertical, matching
 *                         other surface-card row primitives.
 */
@Composable
fun AMDisclosure(
    label: @Composable () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val reduceMotion = LocalAMReduceMotion.current
    val tokens = LocalAMTokens.current

    // Chevron: 0° when expanded (pointing down), -90° when collapsed (pointing right).
    // Routed through the Reduce-Motion policy in lockstep with the content
    // expand/shrink below — both collapse to the same tween(180) under Reduce Motion.
    val chevronRotation by animateFloatAsState(
        targetValue   = if (expanded) 0f else -90f,
        animationSpec = tokens.motion.resolved(disclosureToggleSpring, reduceMotion),
        label         = "AMDisclosure.chevronRotation",
    )

    // Press feedback — monochrome amRipple() state layer, no opacity dim.
    val interactionSource = remember { MutableInteractionSource() }

    AMCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header row ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AMTheme.rowMinHeight.dp)
                    .toggleable(
                        value             = expanded,
                        interactionSource = interactionSource,
                        indication        = amRipple(),
                        // Role.Button (not Switch): a disclosure is an
                        // expand/collapse affordance, not a boolean on/off switch.
                        // stateDescription conveys the open state to TalkBack,
                        // mirroring the iOS disclosure Button trait.
                        role              = Role.Button,
                        onValueChange     = onExpandedChange,
                    )
                    .semantics {
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    }
                    .padding(
                        horizontal = tokens.spacing.lg,
                        vertical   = tokens.spacing.md,
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
                ) {
                    // Label — fills remaining width, leading alignment.
                    Box(
                        modifier         = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        label()
                    }

                    // Rotating chevron — animated via disclosureToggleSpring.
                    Image(
                        imageVector        = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        colorFilter        = ColorFilter.tint(AMTheme.Icon.muted.resolve(isDark)),
                        modifier           = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = chevronRotation },
                    )
                }
            }

            // ── Animated content ─────────────────────────────────────────────
            // Slide-from-top + fade, matching iOS `.opacity.combined(with:
            // .move(edge: .top))` (Disclosure.swift:91): the content block
            // translates in from the top edge (behind the header) rather than a
            // clip-grow reveal. slideInVertically(initialOffsetY = { -it }) starts
            // the block shifted up by its full height and slides it down to rest;
            // the exit reverses toward the top. The slide rides
            // disclosureSlideSpring, the fade disclosureToggleSpring — both stay
            // value-identical (lockstep) and both soften to tween(180) under RM.
            AnimatedVisibility(
                visible = expanded,
                enter   = fadeIn(animationSpec = tokens.motion.resolvedFinite(disclosureToggleSpring, reduceMotion))
                    + slideInVertically(
                        animationSpec  = tokens.motion.resolvedFinite(disclosureSlideSpring, reduceMotion),
                        initialOffsetY = { fullHeight -> -fullHeight },
                    ),
                exit    = fadeOut(animationSpec = tokens.motion.resolvedFinite(disclosureToggleSpring, reduceMotion))
                    + slideOutVertically(
                        animationSpec = tokens.motion.resolvedFinite(disclosureSlideSpring, reduceMotion),
                        targetOffsetY = { fullHeight -> -fullHeight },
                    ),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AMDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = tokens.spacing.lg,
                                vertical   = tokens.spacing.md,
                            ),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AMDisclosure — self-managed (internal state) overload
// ---------------------------------------------------------------------------

/**
 * Surface-card disclosure group — self-managed variant.
 *
 * Wraps [AMDisclosure] (hoisted) with internal
 * `remember { mutableStateOf(initiallyExpanded) }` state. Use this overload
 * when the parent does not need to observe or control the expanded state.
 *
 * For external state control (programmatic expand/collapse, state persistence,
 * animated orchestration), use the `expanded`/`onExpandedChange` overload.
 *
 * ```kotlin
 * AMDisclosure(
 *     label = { BasicText("Advanced options") },
 *     initiallyExpanded = true,
 * ) {
 *     BasicText("Option A")
 * }
 * ```
 *
 * @param label            Composable label rendered in the header row, leading side.
 * @param modifier         Modifier applied to the card's root layout node.
 * @param initiallyExpanded Whether the disclosure starts in the expanded state.
 *                         Defaults to `false` (collapsed). Only read once at
 *                         first composition.
 * @param content          The composable content slot revealed when expanded.
 */
@Composable
fun AMDisclosure(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    AMDisclosure(
        label            = label,
        expanded         = expanded,
        onExpandedChange = { expanded = it },
        modifier         = modifier,
        content          = content,
    )
}

// ---------------------------------------------------------------------------
// AMDisclosure — String label convenience, hoisted variant
// ---------------------------------------------------------------------------

/**
 * Surface-card disclosure group — String label convenience, hoisted variant.
 *
 * Applies the standard disclosure headline style ([AMTheme.Type.title],
 * [AMTheme.Text.primary]) to [label]. Equivalent to the composable-slot
 * hoisted overload with a pre-styled [BasicText] label.
 *
 * @param label            The disclosure header text.
 * @param expanded         Current expanded state.
 * @param onExpandedChange Called when the user taps the header row.
 * @param modifier         Modifier applied to the card's root layout node.
 * @param content          The composable content slot revealed when expanded.
 */
@Composable
fun AMDisclosure(
    label: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AMDisclosure(
        label            = { AMDisclosureLabel(label) },
        expanded         = expanded,
        onExpandedChange = onExpandedChange,
        modifier         = modifier,
        content          = content,
    )
}

// ---------------------------------------------------------------------------
// AMDisclosure — String label convenience, self-managed variant
// ---------------------------------------------------------------------------

/**
 * Surface-card disclosure group — String label convenience, self-managed variant.
 *
 * Applies the standard disclosure headline style ([AMTheme.Type.title],
 * [AMTheme.Text.primary]) to [label] automatically.
 *
 * @param label            The disclosure header text.
 * @param modifier         Modifier applied to the card's root layout node.
 * @param initiallyExpanded Whether the disclosure starts in the expanded state.
 * @param content          The composable content slot revealed when expanded.
 */
@Composable
fun AMDisclosure(
    label: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    AMDisclosure(
        label             = { AMDisclosureLabel(label) },
        modifier          = modifier,
        initiallyExpanded = initiallyExpanded,
        content           = content,
    )
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

/**
 * Standard headline-style label rendered by the String convenience overloads.
 *
 * Renders the label with [AMTheme.Type.title] in [AMTheme.Text.primary].
 */
@Composable
private fun AMDisclosureLabel(text: String) {
    val isDark = LocalAMIsDarkTheme.current
    BasicText(
        text  = text,
        style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
    )
}

// ---------------------------------------------------------------------------
// Previews — light + dark
// ---------------------------------------------------------------------------

@Composable
private fun DisclosurePreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Collapsed disclosure
        AMDisclosure(label = "Collapsed group") {
            BasicText(
                text  = "Hidden until tapped.",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }

        // Expanded disclosure — starts open via initiallyExpanded
        AMDisclosure(
            label             = "Advanced options",
            initiallyExpanded = true,
        ) {
            BasicText(
                text  = "Option A",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text  = "Option B",
                style = AMTheme.Type.secondary.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

@Preview(
    name            = "AMDisclosure · Light",
    group           = "AMDisclosure",
    showBackground  = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
)
@Composable
private fun AMDisclosurePreviewLight() {
    AMThemeRoot {
        DisclosurePreviewContent()
    }
}

@Preview(
    name            = "AMDisclosure · Dark",
    group           = "AMDisclosure",
    showBackground  = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode          = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMDisclosurePreviewDark() {
    AMThemeRoot {
        DisclosurePreviewContent()
    }
}
