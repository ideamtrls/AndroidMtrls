package io.github.jmsdnns.androidmtrls.controls.buttons

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amRipple
import io.github.jmsdnns.androidmtrls.tokens.amSquircle

// ---------------------------------------------------------------------------
// AMCardButton — compact icon-first action button for card action rows
// ---------------------------------------------------------------------------

/**
 * Compact icon-first action button for use inside the bottom action row of
 * social-feed-style cards.
 *
 * Renders at caption size in [AMTheme.Icon.muted] color. Provides no fill,
 * shadow, or surface of its own — the surrounding [AMCard] supplies those.
 * Press feedback is the monochrome `amRipple()` state layer, bounded to a small
 * rounded rect clipped around the icon-and-count row.
 *
 * The [content] slot receives a [RowScope] so callers can compose icon +
 * optional count text with horizontal row spacing. The fixed visual treatment
 * (caption font, muted foreground) is applied over whatever the slot produces.
 *
 * **Hard layout rule:** Card action rows must always use a single [Row] with
 * `horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp)`.
 * Count-labeled buttons cluster LEFT; icon-only buttons cluster RIGHT;
 * a `Spacer(Modifier.weight(1f))` separates the two clusters. Share always
 * precedes ellipsis; ellipsis is always last.
 *
 * **Active state.** The `AMCardButton(icon, label, …)` convenience overload
 * exposes `active`/`activeTint` to render a toggled-on action (like, repost) in
 * a semantic tint instead of the muted default — see that overload. This slot
 * overload deliberately omits those parameters: slot content is caller-styled
 * and `:mtrls` has no content-color CompositionLocal, so the component cannot
 * recolor arbitrary slot children (the same rule that keeps `role` off slot
 * overloads — see conventions). To signal an active state from the slot, color
 * your own content with the desired tint (e.g. [AMTheme.Semantic.destructive]
 * for a filled heart, [AMTheme.Semantic.success] for a repost).
 *
 * iOS equivalent: `IMCardButton<Label>` in
 * `Sources/Controls/Buttons/CardButton.swift`.
 *
 * @param accessibilityLabel Semantic label for this button (spoken by TalkBack).
 * @param onClick            Invoked when the button is tapped.
 * @param content            Composable slot rendered inside a [Row]. The caller
 *   is responsible for placing icon and optional label text and for choosing
 *   their color and size; the slot only applies `xs` spacing between children.
 *   The convenience overload below applies the standard muted (or, when
 *   `active`, tinted) treatment automatically.
 */
@Composable
fun AMCardButton(
    accessibilityLabel: String,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Compact icon-first affordance with no fill; clip to a small rounded rect so
    // the bounded ripple stays snug around the icon-and-count row.
    val shape = amSquircle(AMTheme.Radius.sm.dp)

    Row(
        modifier = Modifier
            .clip(shape)
            .semantics { contentDescription = accessibilityLabel }
            .clickable(
                interactionSource = interactionSource,
                indication = amRipple(),
                // Announce as a button to TalkBack — iOS `IMCardButton` is a
                // SwiftUI `Button` and carries the `isButton` trait.
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Convenience overload — icon + optional count/label string
// ---------------------------------------------------------------------------

/**
 * Convenience overload of [AMCardButton] for the common pattern of an icon with
 * an optional trailing count or short text label.
 *
 * The icon is rendered at 16dp (caption-icon scale). When [label] is provided it
 * is rendered in the default caption font with `fontFeatureSettings = "tnum"`
 * (tabular digits only, iOS `.monospacedDigit()`) so numeric counts align without
 * forcing the label into a monospace typeface.
 *
 * **Active state** — defaults to `false`, the quiet muted look. Pass
 * `active = true` to render the WHOLE label (icon **and** count text) in
 * [activeTint] instead of [AMTheme.Icon.muted], highlighting a toggled-on action
 * (like, repost) as "on" without hand-rolling a colored label. [activeTint]
 * defaults to `null`, which resolves to `LocalAMTokens.current.semantic.info`
 * (so per-subtree token overrides flow through); pass a semantic color such as
 * [AMTheme.Semantic.destructive] (filled heart) or [AMTheme.Semantic.success]
 * (repost). Foreground precedence mirrors iOS `IMCardButton`: `active` wins over
 * the muted default. There is no disabled state (no `enabled` param), and press
 * feedback is the orthogonal `amRipple()` sweep — neither participates in the
 * foreground color.
 *
 * iOS equivalent:
 * `IMCardButton.init(systemImage:label:active:activeTint:accessibilityLabel:action:)`.
 *
 * @param icon               The icon to render. Tinted [AMTheme.Icon.muted], or
 *                           [activeTint] when [active].
 * @param label              Optional trailing count or short verb (e.g. `"12"`, `"Reply"`).
 * @param active             When `true` the icon and label render in [activeTint]
 *                           instead of the muted default. Defaults to `false`.
 * @param activeTint         Tint applied to the whole label when [active]. `null`
 *                           (default) resolves to `LocalAMTokens.current.semantic.info`.
 * @param accessibilityLabel Semantic label spoken by TalkBack.
 * @param onClick            Invoked when the button is tapped.
 */
@Composable
fun AMCardButton(
    icon: ImageVector,
    label: String? = null,
    active: Boolean = false,
    activeTint: Color? = null,
    accessibilityLabel: String,
    onClick: () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current

    // Foreground precedence mirrors iOS `IMCardButton`: `active` wins, else
    // muted. No disabled state, and the `amRipple()` press sweep is orthogonal,
    // so foreground depends only on `active`. A null `activeTint` resolves to
    // the live `info` semantic so token overrides flow through.
    val foreground = if (active) {
        activeTint ?: LocalAMTokens.current.semantic.info.resolve(isDark)
    } else {
        AMTheme.Icon.muted.resolve(isDark)
    }

    AMCardButton(
        accessibilityLabel = accessibilityLabel,
        onClick = onClick,
    ) {
        Image(
            imageVector = icon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(foreground),
            modifier = Modifier.size(16.dp),
        )
        if (label != null) {
            BasicText(
                text = label,
                // Tabular digits ONLY (iOS `.monospacedDigit()`): keep the default
                // font and enable `tnum` so counts align, without forcing the whole
                // label into a monospace/code typeface.
                style = AMTheme.Type.caption.toTextStyle(foreground).copy(
                    fontFeatureSettings = "tnum",
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

@Composable
private fun CardButtonPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current

    Column(
        modifier = Modifier
            .background(AMTheme.Surface.page.resolve(isDark))
            .padding(AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        // Social-feed action row inside a card — the intended usage context.
        // Reply is the quiet muted default; repost and like are `active` toggles
        // tinted success (green) and destructive (red) so both branches of the
        // foreground precedence render in each color scheme.
        AMCard(
            modifier = Modifier.fillMaxWidth(),
            padding = AMPadding.block,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Count-labeled buttons cluster LEFT
                AMCardButton(
                    icon = Icons.Default.Email,
                    label = "12",
                    accessibilityLabel = "Reply",
                    onClick = {},
                )
                // Active repost — success (green) tint
                AMCardButton(
                    icon = Icons.Default.Refresh,
                    label = "4",
                    active = true,
                    activeTint = AMTheme.Semantic.success.resolve(isDark),
                    accessibilityLabel = "Reposted",
                    onClick = {},
                )
                // Active like — destructive (red) tint
                AMCardButton(
                    icon = Icons.Default.Favorite,
                    label = "48",
                    active = true,
                    activeTint = AMTheme.Semantic.destructive.resolve(isDark),
                    accessibilityLabel = "Liked",
                    onClick = {},
                )
                Spacer(Modifier.weight(1f))
                // Icon-only buttons cluster RIGHT (share before ellipsis)
                AMCardButton(
                    icon = Icons.Default.Share,
                    accessibilityLabel = "Share",
                    onClick = {},
                )
                AMCardButton(
                    icon = Icons.Default.MoreVert,
                    accessibilityLabel = "More",
                    onClick = {},
                )
            }
        }
    }
}

@Preview(
    name = "AMCardButton · Light",
    group = "AMCardButton",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
)
@Composable
private fun AMCardButtonPreviewLight() {
    AMThemeRoot {
        CardButtonPreviewContent()
    }
}

@Preview(
    name = "AMCardButton · Dark",
    group = "AMCardButton",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMCardButtonPreviewDark() {
    AMThemeRoot {
        CardButtonPreviewContent()
    }
}
