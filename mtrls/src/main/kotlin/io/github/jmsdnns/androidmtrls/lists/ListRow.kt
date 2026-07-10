package io.github.jmsdnns.androidmtrls.lists

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amHairline
import io.github.jmsdnns.androidmtrls.tokens.listRowInsets

// ---------------------------------------------------------------------------
// File-level style constants — hoisted to avoid per-recomposition allocation.
// Colors depend on the theme and are applied per-call via style.copy(color = …).
// ---------------------------------------------------------------------------

private val AMListRowTitleStyle = AMTheme.Type.title.toTextStyle(Color.Unspecified)
private val AMListRowSubtitleStyle = AMTheme.Type.caption.toTextStyle(Color.Unspecified)

// ---------------------------------------------------------------------------
// AMListRow — slot overload (designated)
// ---------------------------------------------------------------------------

/**
 * Full-bleed list row for vertical lists.
 *
 * Fills the available width with [AMTheme.Surface.primary], no rounded corners,
 * no shadow — designed to sit flush against neighbouring rows or a container's
 * surface. Horizontal `leading / text-area / trailing / chevron` layout, with
 * internal padding (`tokens.listRowInsets`) for breathing room.
 *
 * **Visual only** — this composable carries no `onClick`. Callers add
 * `Modifier.clickable(…)` or `Modifier.combinedClickable(…)` at the call site
 * to wire up tap/long-press. This matches the iOS design intent: "taps stay at
 * the list level" (see `IMListRow` source).
 *
 * The row height is pinned to a **two-line minimum** via an invisible
 * `alpha(0f)` placeholder column (title-line + subtitle-line spaced 2dp), so
 * single-line rows share the same height as two-line rows. Real content is
 * centred vertically by the surrounding [Box].
 *
 * Pass `isSelected = true` to draw a 1.5dp selection-ring outline
 * ([AMTheme.selectionRing] at [AMTheme.selectionRingWidth]) inside the row
 * bounds — a `strokeBorder`-equivalent stroke over the untouched
 * [AMTheme.Surface.primary] interior, mirroring iOS. Selecting never changes
 * the row's metrics.
 *
 * Chevrons are **opt-in**: Android list rows don't decorate navigation by
 * default (`showsChevron = false`). Pass `showsChevron = true` on a row that
 * pushes a detail view to render a trailing `▶` drill-down glyph, to the right
 * of any caller-supplied [trailing] content.
 *
 * **Chevron glyph note:** iOS uses `chevron.right` (SF Symbols). The nearest
 * `material-icons-core` glyph is
 * [Icons.AutoMirrored.Filled.KeyboardArrowRight], rendered at caption-equivalent
 * size (16dp) and tinted [AMTheme.Icon.muted]. The auto-mirrored variant
 * ensures correct RTL layout.
 *
 * **Pairs with** [AMReorderableRowList] — the reorderable list uses [AMListRow]
 * as its cell content.
 *
 * @param isSelected    When `true`, draws the 1.5dp [AMTheme.selectionRing]
 *                      outline inside the row bounds (interior untouched).
 * @param showsChevron  Opt-in drill-down affordance, `false` by default. When
 *                      `true`, appends a trailing `KeyboardArrowRight` glyph.
 *                      Decorative — no `contentDescription`.
 * @param leading       Optional composable slot rendered before the text area.
 *                      Pass `null` (default) to omit. Typically an avatar or icon.
 * @param trailing      Optional composable slot rendered after the text area and
 *                      before the chevron (when present). Pass `null` to omit.
 *                      Typically a status tag or value label.
 * @param modifier      Layout modifier applied to the root [Row]. Place
 *                      `Modifier.clickable(…)` here at the call site.
 * @param content       Text-area composable slot. For the standard title/subtitle
 *                      layout use the [AMListRow] string overload instead.
 */
@Composable
fun AMListRow(
    isSelected: Boolean = false,
    showsChevron: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    val surfaceColor = AMTheme.Surface.primary.resolve(isDark)
    val iconMutedColor = AMTheme.Icon.muted.resolve(isDark)

    // Selection draws a 1.5dp selection-ring OUTLINE inside the full row bounds
    // (strokeBorder-equivalent via amHairline) over the untouched Surface.primary
    // interior — mirrors iOS ListRow's
    // `Rectangle().strokeBorder(selectionRing, lineWidth: selectionRingWidth)`.
    // Applied before the insets padding so the ring frames the whole row; the
    // interior is never tinted and selecting never changes the row's metrics.
    val selectionModifier = if (isSelected) {
        Modifier.amHairline(
            shape = RectangleShape,
            color = AMTheme.selectionRing.resolve(isDark),
            width = AMTheme.selectionRingWidth.dp,
        )
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .then(selectionModifier)
            .padding(tokens.listRowInsets),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        // Leading slot — omitted when null
        if (leading != null) {
            leading()
        }

        // Text-area — height-pinned to two-line minimum via alpha(0f) placeholder
        Box(contentAlignment = Alignment.CenterStart) {
            // Invisible sizer: pins the row to two-line height regardless of
            // whether real content is one or two lines. Mirrors the iOS ZStack
            // hidden-placeholder trick (two Text(" ") views stacked in a VStack).
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.alpha(0f),
            ) {
                BasicText(text = " ", style = AMListRowTitleStyle)
                BasicText(text = " ", style = AMListRowSubtitleStyle)
            }
            // Real content — centred vertically by the Box alignment
            content()
        }

        // Flexible spacer: greedily expands to pin trailing content (and the
        // chevron) to the row's end edge. weight(1f) (fill = true) claims all
        // remaining width; defaultMinSize(8dp) keeps at least an 8dp gap when
        // long content leaves no slack (mirrors iOS `Spacer(minLength: sm)`).
        Spacer(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minWidth = 8.dp),
        )

        // Trailing slot — omitted when null
        if (trailing != null) {
            trailing()
        }

        // Chevron glyph — decorative navigation affordance
        // iOS: `Image(systemName: "chevron.right").font(.caption.weight(.semibold))`
        // Android substitute: Icons.AutoMirrored.Filled.KeyboardArrowRight at
        // 16dp (caption-equivalent size), tinted AMTheme.Icon.muted,
        // contentDescription = null (decorative).
        if (showsChevron) {
            Image(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconMutedColor),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AMListRow — String convenience overload
// ---------------------------------------------------------------------------

/**
 * Full-bleed list row with standard title/subtitle text rendering.
 *
 * Convenience overload of [AMListRow] that fills the text-area slot with the
 * canonical headline/caption [Column]: `title` with [AMTheme.Type.title]
 * ([AMTheme.Text.primary]) and an optional `subtitle` with [AMTheme.Type.caption]
 * ([AMTheme.Text.secondary]) spaced 2dp apart. Both lines are single-line
 * (`maxLines = 1`, ellipsis truncation).
 *
 * The 2dp title/subtitle gap is deliberately hardcoded — tighter than
 * `tokens.spacing.xs = 4dp` so the pair reads as one text block, not two
 * stacked rows. (Mirrors `IMListRow.titleSubtitleSpacing = 2` in iOSMtrls.)
 *
 * **Visual only** — no `onClick`. Callers add `Modifier.clickable(…)` at the
 * call site. This row **pairs with** [AMReorderableRowList].
 *
 * @param title         Primary text line. [AMTheme.Type.title], [AMTheme.Text.primary].
 * @param subtitle      Optional secondary text line. [AMTheme.Type.caption],
 *                      [AMTheme.Text.secondary]. Omitted when `null`.
 * @param isSelected    When `true`, draws the 1.5dp [AMTheme.selectionRing]
 *                      outline inside the row bounds (interior untouched).
 * @param showsChevron  Opt-in drill-down chevron, `false` by default. When
 *                      `true`, appends a trailing navigation chevron.
 * @param leading       Optional leading composable slot (avatar, icon, artwork).
 *                      Pass `null` (default) to omit.
 * @param trailing      Optional trailing composable slot (status tag, value label).
 *                      Pass `null` (default) to omit.
 * @param modifier      Layout modifier applied to the root [Row].
 */
@Composable
fun AMListRow(
    title: String,
    subtitle: String? = null,
    isSelected: Boolean = false,
    showsChevron: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalAMIsDarkTheme.current
    val primaryColor = AMTheme.Text.primary.resolve(isDark)
    val secondaryColor = AMTheme.Text.secondary.resolve(isDark)

    AMListRow(
        isSelected = isSelected,
        showsChevron = showsChevron,
        leading = leading,
        trailing = trailing,
        modifier = modifier,
    ) {
        // Standard title/subtitle VStack — mirrors iOS `AnyView(VStack { … })`
        // in the string convenience init. The 2dp gap is hardcoded per spec.
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(
                text = title,
                style = AMListRowTitleStyle.copy(color = primaryColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                BasicText(
                    text = subtitle,
                    style = AMListRowSubtitleStyle.copy(color = secondaryColor),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun ListRowPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AMListRow(title = "Title only")
        AMListRow(title = "Title and subtitle", subtitle = "Supporting detail")
        AMListRow(
            title = "Drill-down row",
            subtitle = "Navigates on tap",
            showsChevron = true,
        )
        AMListRow(
            title = "Selected row",
            subtitle = "Carries the selection ring",
            isSelected = true,
        )
    }
}

@Preview(
    name = "AMListRow · Light",
    group = "AMListRow",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMListRowPreviewLight() {
    AMThemeRoot {
        ListRowPreviewContent()
    }
}

@Preview(
    name = "AMListRow · Dark",
    group = "AMListRow",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMListRowPreviewDark() {
    AMThemeRoot {
        ListRowPreviewContent()
    }
}
