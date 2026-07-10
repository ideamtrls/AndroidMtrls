package io.github.jmsdnns.androidmtrls.collections

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amCardHairline
import io.github.jmsdnns.androidmtrls.tokens.amHairlineWidth
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolved
import io.github.jmsdnns.androidmtrls.tokens.resolvedFinite
import io.github.jmsdnns.androidmtrls.tokens.resolvedOrNull

// ---------------------------------------------------------------------------
// File-level style constant
// ---------------------------------------------------------------------------

/** Tab label: the house [AMTheme.Type.label] scale (14sp Medium, 0.1 tracking). */
private val AMTabLabelStyle = TextStyle(
    fontSize = AMTheme.Type.label.fontSize.sp,
    fontWeight = AMTheme.Type.label.fontWeight,
    letterSpacing = AMTheme.Type.label.letterSpacing.sp,
)

/**
 * Dp-typed spring equivalent to [AMTheme.Motion.snapSpring].
 *
 * [AMTheme.Motion.snapSpring] is declared as `spring<Float>` (the house
 * token type). [animateDpAsState] requires `AnimationSpec<Dp>` — a different
 * type parameter that cannot be assigned from the Float token directly.
 * This private val reproduces the identical spring parameters
 * (dampingRatio = 0.86, stiffness = 384) as a `spring<Dp>` so the animation
 * feel is identical to the house token. If the token values are ever tuned,
 * this val must be updated in lockstep.
 */
private val snapSpringDp = spring<Dp>(dampingRatio = 0.86f, stiffness = 384f)

// ---------------------------------------------------------------------------
// AMTab — tab descriptor
// ---------------------------------------------------------------------------

/**
 * One tab entry in an [AMTabs] component.
 *
 * Identity is carried by [id], matched against [AMTabs]'s `selection` to
 * determine which tab is active and where the strip's underline sits. Two
 * label paths mirror the iOS `IMTab` init paths:
 *
 * - **String path:** set [label] to a non-empty string; [labelContent] stays
 *   `null`. The strip applies [AMTheme.Type.label] automatically.
 * - **Custom path:** set [labelContent] to a `@Composable` lambda; the strip
 *   wraps it in the same equal-width cell with active/inactive foreground
 *   treatment. Font styling is the caller's responsibility.
 *
 * Content is stored as a `@Composable () -> Unit` lambda so that [AMTabs] can
 * restrict rendering to the selected tab's content — only the active tab's
 * composable runs inside [AnimatedContent], matching the iOS "lazy closure"
 * trick. **Do not call [content] outside of [AMTabs].**
 *
 * Annotated `@Immutable` following house convention: config/data carriers
 * crossing recomposition carry `@Immutable` even when they contain lambdas
 * (consistent with `AMSegmentedControl.Option` and every other house data
 * carrier). Compose's own `@Stable`/`@Immutable` analysis treats lambdas
 * captured at composition time as stable.
 *
 * @param id           Selection value this tab represents.
 * @param label        Display text for the string-label path. Empty string
 *                     signals custom-label path; use [labelContent] then.
 * @param labelContent Custom composable label. When non-null, rendered instead
 *                     of [label]. Font and layout are caller-controlled.
 * @param content      Composable rendered below the strip when this tab is
 *                     selected. Only the active tab's [content] is invoked.
 */
@Immutable
data class AMTab<T>(
    val id: T,
    val label: String = "",
    val labelContent: (@Composable () -> Unit)? = null,
    val content: @Composable () -> Unit,
)

// ---------------------------------------------------------------------------
// AMTabs — in-page content switcher
// ---------------------------------------------------------------------------

/**
 * In-page content switcher: a tab strip header plus a content area that
 * cross-fades between tab views as the selection changes.
 *
 * **Not** a screen-level tab bar. Intended for content switching within a
 * surface — the same role as iOS `IMTabs`.
 *
 * ## Layout
 *
 * The whole component (strip + content area) sits on one continuous flat
 * rounded panel: clipped to `amSquircle(`[AMTheme.Radius].lg`)` (16dp)
 * and filled with [AMTheme.Surface].primary, with a dark-mode-only
 * [amCardHairline] defining the panel edge — the Compose port of iOS's
 * `RoundedRectangle(radius.lg).fill(Surface.primary)` + `.clipShape`. No
 * elevation: the panel reads as flat, not a lifted card. The clip keeps the
 * strip's bottom hairline from running past the rounded corners.
 *
 * The strip is a [Row] of equal-weight ([Modifier.weight(1f)]) tappable cells,
 * each vertically padded by `spacing.md` (12dp). Labels use [AMTheme.Type.label];
 * the label color crossfades [AMTheme.Text.primary] (selected) ↔
 * [AMTheme.Text.secondary] (unselected) over 150ms. A density-clamped hairline
 * ([amHairlineWidth]) in [AMTheme.separator] runs the strip's bottom edge,
 * separating it from the content area.
 *
 * The indicator is a **full-cell-width, 1dp-tall** capsule bar in
 * [AMTheme.Text.primary], sitting at the strip's bottom edge on top of the
 * hairline. It spans the entire equal-weight cell (cell width = `maxWidth /
 * tabs.size`, measured in [BoxWithConstraints]) and slides by
 * `cellWidth × selectedIndex` — the iOS full-width 1pt underline, not a
 * Material label-width tab indicator. The x-offset animates with `snapSpring`
 * semantics (stiffness=384, dampingRatio=0.86, the `snapSpringDp` Dp mirror).
 * Empty `tabs` or a selection not found in the list are handled gracefully
 * (nothing renders for empty tabs; selectedIndex falls back to 0).
 *
 * ## Content area
 *
 * [AnimatedContent] keyed on `selection` performs a **stationary crossfade** —
 * `fadeIn` togetherWith `fadeOut`, no directional travel, over a 280ms
 * [FastOutSlowInEasing] `tween` (the port of iOS `contentSwapSpring`,
 * response=0.28; the swap "settles just behind the underline, not travel like
 * a surface"). Only the selected tab's [AMTab.content] composable runs —
 * [AnimatedContent]'s exit-phase content is not recomposed after it leaves the
 * screen.
 *
 * All motion is reduce-motion-aware via `tokens.motion.resolved` /
 * `resolvedOrNull` (`tokens/Motion.kt`): under Reduce Motion the underline
 * jumps to the selected tab (the slide collapses to a snap) and the content
 * swap softens to the gentle overlay fade (still a crossfade).
 *
 * ## Press feedback
 *
 * Each cell uses a [MutableInteractionSource] wired to the monochrome
 * [amRipple] state layer, clipped to the cell's rectangle. The
 * [MutableInteractionSource] list is remembered keyed on `tabs.size` so the
 * `remember` call count stays stable when tab count changes.
 *
 * ## Adaptive colors
 *
 * All colors resolved via [LocalAMIsDarkTheme] / [AMAdaptiveColor.resolve] —
 * never [isSystemInDarkTheme] directly (house rule).
 *
 * ## Usage
 * ```kotlin
 * var current by rememberSaveable { mutableStateOf("posts") }
 * AMTabs(
 *     tabs = listOf(
 *         AMTab(id = "posts",   label = "Posts")   { PostsContent() },
 *         AMTab(id = "replies", label = "Replies") { RepliesContent() },
 *     ),
 *     selection = current,
 *     onSelectionChange = { current = it },
 *     modifier = Modifier.fillMaxWidth(),
 * )
 * ```
 *
 * @param tabs              Ordered list of [AMTab] entries. Empty list renders nothing.
 * @param selection         Currently selected tab id. Compared against each [AMTab.id].
 * @param onSelectionChange Called when the user taps a tab, passing the new selection value.
 * @param modifier          Modifier applied to the outer [Column].
 */
@Composable
fun <T : Any> AMTabs(
    tabs: List<AMTab<T>>,
    selection: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return

    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current
    val reduceMotion = LocalAMReduceMotion.current
    val separatorColor = AMTheme.separator.resolve(isDark)
    val textPrimaryColor = AMTheme.Text.primary.resolve(isDark)
    val textSecondaryColor = AMTheme.Text.secondary.resolve(isDark)
    val surfacePrimaryColor = AMTheme.Surface.primary.resolve(isDark)
    val hairlineHeight = amHairlineWidth()

    // Resolve selected index — fallback to 0 when selection not found.
    val selectedIndex = tabs.indexOfFirst { it.id == selection }.coerceAtLeast(0)

    // Hoist interaction sources outside the forEachIndexed loop, keyed on tabs.size,
    // so the remember count stays stable if tab count changes.
    val interactionSources = remember(tabs.size) {
        List(tabs.size) { MutableInteractionSource() }
    }

    // One continuous flat rounded panel for the strip + content area: clipped to
    // radius.lg, filled Surface.primary, with a dark-mode-only hairline defining
    // the edge (iOS RoundedRectangle(radius.lg).fill(Surface.primary) + clip; no
    // elevation → reads as flat, not a lifted card).
    val panelShape = amSquircle(tokens.radius.lg)

    Column(
        modifier = modifier
            .clip(panelShape)
            .background(surfacePrimaryColor)
            .amCardHairline(panelShape),
    ) {
        // ── Tab strip ────────────────────────────────────────────────────────
        // BoxWithConstraints gives the measured strip width so the full-cell-width
        // indicator can be positioned inside each equal-weight cell
        // (cellWidth = maxWidth / tabs.size).
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val cellWidth = maxWidth / tabs.size

            // ── Cell row ─────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex
                    val interactionSource = interactionSources[index]

                    // Animate label color between active/inactive.
                    // selectionFade = 150ms tween, EaseInOut (house motion token,
                    // AMTheme.Motion.selectionFade), routed through the Reduce-Motion
                    // policy — a colour cross-fade (no movement) so it SOFTENS under
                    // RM (resolved), never snaps.
                    val labelColor by animateColorAsState(
                        targetValue = if (isSelected) textPrimaryColor else textSecondaryColor,
                        animationSpec = tokens.motion.resolved(
                            tween<Color>(durationMillis = 150, easing = EaseInOut),
                            reduceMotion,
                        ),
                        label = "AMTabs.labelColor[$index]",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RectangleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = amRipple(),
                            ) { onSelectionChange(tab.id) }
                            .padding(vertical = AMTheme.Spacing.md.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (tab.labelContent != null) {
                            // Custom label path — foreground tint is caller's
                            // responsibility; the ripple state layer provides
                            // press feedback.
                            tab.labelContent.invoke()
                        } else {
                            // String label path — Type.label scale, animated color.
                            BasicText(
                                text = tab.label,
                                style = AMTabLabelStyle.copy(
                                    color = labelColor,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                }
            }

            // ── Bottom hairline ───────────────────────────────────────────────
            // Density-clamped hairline: max(0.5dp, 1px), separator color.
            // Drawn as an overlay aligned to BottomStart along the row's bottom edge.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hairlineHeight)
                    .background(separatorColor)
                    .align(Alignment.BottomStart),
            )

            // ── Full-cell-width underline ─────────────────────────────────────
            // A 1dp capsule bar spanning the whole selected cell, slid by
            // cellWidth × selectedIndex — the iOS full-width 1pt underline, not a
            // Material label-width indicator. The SLIDE is the canonical
            // resolvedOrNull case named in tokens/Motion.kt: under Reduce Motion
            // the travel collapses to a snap() so the bar jumps to the selected
            // tab instead of gliding. Width is fixed (cellWidth), so only the
            // offset animates; animateDpAsState initialises at the target on the
            // first frame, so it never grows in from zero.
            val underlineSpec = tokens.motion.resolvedOrNull(snapSpringDp, reduceMotion)
            val indicatorOffsetX by animateDpAsState(
                targetValue = cellWidth * selectedIndex,
                animationSpec = underlineSpec ?: snap(),
                label = "AMTabs.indicatorOffset",
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = indicatorOffsetX)
                    .width(cellWidth)
                    .height(1.dp)
                    .background(
                        color = textPrimaryColor,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }

        // ── Content area ─────────────────────────────────────────────────────
        // AnimatedContent keyed on `selection` performs a STATIONARY crossfade —
        // fadeIn togetherWith fadeOut, no directional travel — over a 280ms
        // FastOutSlowInEasing tween (the port of iOS contentSwapSpring,
        // response=0.28; the swap "settles just behind the underline, not travel
        // like a surface"). Only the selected tab's composable runs — inactive
        // content trees are not recomposed after the exit phase. Under Reduce
        // Motion the fade softens to the overlay-fade tween (still a crossfade).
        AnimatedContent(
            targetState = selection,
            transitionSpec = {
                val fadeSpec = tokens.motion.resolvedFinite(
                    tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing),
                    reduceMotion,
                )
                fadeIn(fadeSpec) togetherWith fadeOut(fadeSpec)
            },
            label = "AMTabs.content",
        ) { currentSelection ->
            tabs.firstOrNull { it.id == currentSelection }?.content?.invoke()
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun TabsPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
    ) {
        AMTabs(
            tabs = listOf(
                AMTab(id = "posts", label = "Posts") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AMTheme.Spacing.lg.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "12 posts",
                            style = AMTheme.Type.secondary.toTextStyle(
                                AMTheme.Text.secondary.resolve(isDark),
                            ),
                        )
                    }
                },
                AMTab(id = "replies", label = "Replies") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AMTheme.Spacing.lg.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "4 replies",
                            style = AMTheme.Type.secondary.toTextStyle(
                                AMTheme.Text.secondary.resolve(isDark),
                            ),
                        )
                    }
                },
                AMTab(id = "likes", label = "Likes") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AMTheme.Spacing.lg.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        BasicText(
                            text = "87 likes",
                            style = AMTheme.Type.secondary.toTextStyle(
                                AMTheme.Text.secondary.resolve(isDark),
                            ),
                        )
                    }
                },
            ),
            selection = "posts",
            onSelectionChange = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(
    name = "AMTabs · Light",
    group = "AMTabs",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMTabsPreviewLight() {
    AMThemeRoot {
        TabsPreviewContent()
    }
}

@Preview(
    name = "AMTabs · Dark",
    group = "AMTabs",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMTabsPreviewDark() {
    AMThemeRoot {
        TabsPreviewContent()
    }
}
