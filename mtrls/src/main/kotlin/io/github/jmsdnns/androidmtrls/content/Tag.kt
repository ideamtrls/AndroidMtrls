package io.github.jmsdnns.androidmtrls.content

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMSemantic
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.AMTokens
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens

// ---------------------------------------------------------------------------
// AMTagTint — semantic tint axis
// ---------------------------------------------------------------------------

/**
 * Semantic tint axis for [AMTag].
 *
 * Top-level rather than nested inside [AMTag] — matching the iOS design
 * where `IMTagTint` is top-level so that call sites can annotate with just
 * `AMTagTint` without repeating type arguments.
 *
 * - [NEUTRAL] — primary text on the neutral fill; categories with no semantic charge.
 * - [INFO]    — informational blue wash; neutral status worth noticing.
 * - [SUCCESS] — success green wash; completed, active, healthy.
 * - [WARNING] — warning yellow wash; needs attention, degraded.
 *              Yellow uses a higher fill opacity (0.18) than blue/red (0.12) because
 *              yellow is less perceptually intense — the extra wash compensates.
 * - [DESTRUCTIVE] — destructive red wash; errors, blocked, removal.
 *
 * The four charged cases delegate their base color to [AMSemantic] — the one
 * canonical role→color axis — while [NEUTRAL] and the per-hue fill-opacity
 * table stay local. Color resolution happens on the composable (reads
 * [LocalAMTokens]) so [AMTagTint] itself stays a pure-case enum — no token
 * dependency.
 */
@Immutable
enum class AMTagTint {
    NEUTRAL,
    INFO,
    SUCCESS,
    WARNING,
    DESTRUCTIVE,
}

// ---------------------------------------------------------------------------
// AMTag — custom-content slot overload (designated)
// ---------------------------------------------------------------------------

/**
 * Compact rounded-pill label with a fully custom content slot.
 *
 * The capsule shape, tint fill, and foreground color are applied by [AMTag];
 * the [content] lambda supplies only the inner layout (text, icons, or any
 * composable).
 *
 * For the standard icon + text layout, prefer the [AMTag] String overload:
 * ```kotlin
 * AMTag(text = "Active", icon = Icons.Default.Check, tint = AMTagTint.SUCCESS)
 * ```
 *
 * For fully custom content:
 * ```kotlin
 * AMTag(tint = AMTagTint.INFO) {
 *     Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp))
 *     BasicText("Custom", style = captionSemibold)
 * }
 * ```
 *
 * iOS equivalent: `IMTag(tint:label:)` — the generic `@ViewBuilder label` init.
 *
 * @param tint    Semantic tint that drives the capsule fill and foreground color.
 *                Defaults to [AMTagTint.NEUTRAL].
 * @param content Content rendered inside the capsule. Receives [RowScope] so
 *                callers can use `Modifier.weight`, `Arrangement`, etc.
 */
@Composable
fun AMTag(
    tint: AMTagTint = AMTagTint.NEUTRAL,
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    val fill = amTagFillColor(tint = tint, tokens = tokens, isDark = isDark)
    val shape = RoundedCornerShape(50)   // Capsule = 50% radius

    Row(
        modifier = Modifier
            .clip(shape)
            .background(fill)
            .padding(horizontal = tokens.spacing.sm, vertical = tokens.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

// ---------------------------------------------------------------------------
// AMTag — String + optional icon overload (primary convenience)
// ---------------------------------------------------------------------------

/**
 * Compact rounded-pill label rendered with a standard icon + text layout.
 *
 * The [icon] (if non-null) precedes the [text] label at caption-semibold weight,
 * matching the `IMTagStringLabel` layout in iOS.
 *
 * ```kotlin
 * AMTag(text = "Active", icon = Icons.Default.Check, tint = AMTagTint.SUCCESS)
 * AMTag(text = "Neutral")
 * AMTag(text = "Info", tint = AMTagTint.INFO)
 * ```
 *
 * iOS equivalent: `IMTag(_ text:systemImage:tint:)` — the convenience init that
 * builds `IMTagStringLabel` internally.
 *
 * @param text Text label rendered inside the capsule.
 * @param icon Optional leading icon rendered before [text] at 12dp.
 *             Pass `null` to show text only.
 * @param tint Semantic tint that drives the capsule fill and foreground color.
 *             Defaults to [AMTagTint.NEUTRAL].
 */
@Composable
fun AMTag(
    text: String,
    icon: ImageVector? = null,
    tint: AMTagTint = AMTagTint.NEUTRAL,
) {
    val tokens = LocalAMTokens.current
    val isDark = LocalAMIsDarkTheme.current

    val foreground = amTagForegroundColor(tint = tint, tokens = tokens, isDark = isDark)

    AMTag(tint = tint) {
        if (icon != null) {
            // Icon import deferred to caller's choice of icon set; we only
            // use androidx.compose.ui.graphics.vector.ImageVector here.
            // Render via a BasicText-free approach: Compose's Icon composable
            // is in material3/material-icons, which :mtrls avoids. Use the
            // foundation-level androidx.compose.foundation.Image approach instead.
            // Callers who want icons should pass Icons.* from their own module.
            //
            // Implementation: delegate to AMTagIcon — a private helper that draws
            // the ImageVector using Compose's built-in vector painter, which lives
            // in androidx.compose.ui (no material dependency).
            AMTagIcon(icon = icon, tint = foreground)
        }
        BasicText(
            text = text,
            // iOS `IMTagStringLabel` = `.caption.weight(.semibold)` (12pt) → the
            // house map of `.caption` is Type.caption (12sp); emphasis is Medium,
            // never SemiBold.
            style = AMTheme.Type.caption
                .toTextStyle(foreground)
                .copy(fontWeight = FontWeight.Medium),
        )
    }
}

// ---------------------------------------------------------------------------
// AMTagIcon — internal icon renderer (no Material3 dependency)
// ---------------------------------------------------------------------------

/**
 * Renders an [ImageVector] glyph at the standard tag-icon size (12dp × 12dp).
 *
 * Internal helper for the [AMTag] String overload. Uses only `androidx.compose.ui`
 * primitives — no Material3 `Icon` composable — so `:mtrls` stays Material3-free.
 *
 * @param icon Glyph to render.
 * @param tint Color applied to the glyph.
 */
@Composable
private fun AMTagIcon(
    icon: ImageVector,
    tint: Color,
) {
    androidx.compose.foundation.Image(
        imageVector = icon,
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = Modifier.size(12.dp),
    )
}

// ---------------------------------------------------------------------------
// Color resolution helpers (package-private; could be shared with other
// Content components that use the same tint axis in future)
// ---------------------------------------------------------------------------

/**
 * Resolves the **foreground** color for a given [AMTagTint].
 *
 * Charged tints route their base color through [AMSemantic] (resolved against
 * [LocalAMTokens], so overrides flow through); [AMTagTint.NEUTRAL] reads the
 * locked [AMTheme.Text.primary], matching the iOS recipe where
 * `IMTheme.Text.primary` anchors the neutral axis.
 */
@Composable
internal fun amTagForegroundColor(
    tint: AMTagTint,
    tokens: AMTokens = LocalAMTokens.current,
    isDark: Boolean = LocalAMIsDarkTheme.current,
): Color = when (tint) {
    AMTagTint.NEUTRAL     -> AMTheme.Text.primary.resolve(isDark)
    AMTagTint.INFO        -> AMSemantic.Info.color(tokens.semantic, isDark)
    AMTagTint.SUCCESS     -> AMSemantic.Success.color(tokens.semantic, isDark)
    AMTagTint.WARNING     -> AMSemantic.Warning.color(tokens.semantic, isDark)
    AMTagTint.DESTRUCTIVE -> AMSemantic.Destructive.color(tokens.semantic, isDark)
}

/**
 * Resolves the **background fill** color for a given [AMTagTint].
 *
 * Wash opacities are intentionally per-hue tuned to produce equal perceived
 * strength across tints. Yellow ([AMTagTint.WARNING]) uses 0.18 rather than
 * 0.12 because yellow registers less intensely at equal opacity — **do not
 * normalise these values**.
 *
 * | Tint        | Fill opacity |
 * |-------------|-------------|
 * | NEUTRAL     | 0.08 ([AMTheme.neutralFill]) |
 * | INFO        | 0.12 |
 * | SUCCESS     | 0.15 |
 * | WARNING     | 0.18 |
 * | DESTRUCTIVE | 0.12 |
 */
@Composable
internal fun amTagFillColor(
    tint: AMTagTint,
    tokens: AMTokens = LocalAMTokens.current,
    isDark: Boolean = LocalAMIsDarkTheme.current,
): Color = when (tint) {
    AMTagTint.NEUTRAL     -> AMTheme.neutralFill()
    AMTagTint.INFO        -> AMSemantic.Info.color(tokens.semantic, isDark).copy(alpha = 0.12f)
    AMTagTint.SUCCESS     -> AMSemantic.Success.color(tokens.semantic, isDark).copy(alpha = 0.15f)
    AMTagTint.WARNING     -> AMSemantic.Warning.color(tokens.semantic, isDark).copy(alpha = 0.18f)
    AMTagTint.DESTRUCTIVE -> AMSemantic.Destructive.color(tokens.semantic, isDark).copy(alpha = 0.12f)
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun TagPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    val tokens = LocalAMTokens.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(tokens.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.md),
    ) {
        AMTag(text = "Neutral")
        AMTag(text = "Info", tint = AMTagTint.INFO)
        AMTag(text = "Success", tint = AMTagTint.SUCCESS)
        AMTag(text = "Warning", tint = AMTagTint.WARNING)
        AMTag(text = "Destructive", tint = AMTagTint.DESTRUCTIVE)
    }
}

@Preview(
    name = "AMTag · Light",
    group = "AMTag",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,  // AMTheme.Surface.page light
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMTagPreviewLight() {
    AMThemeRoot {
        TagPreviewContent()
    }
}

@Preview(
    name = "AMTag · Dark",
    group = "AMTag",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,  // AMTheme.Surface.page dark
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMTagPreviewDark() {
    AMThemeRoot {
        TagPreviewContent()
    }
}
