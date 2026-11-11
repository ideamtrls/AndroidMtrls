package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The overridable tier of the style: only what Material's theme lacks, the spacing scale and the
 * success and warning tints. Type, color, shape and motion are overridden through `MaterialTheme`.
 *
 * [AMTheme] provides them, [AMTheme.tokens] reads them, and a subtree re-tunes them with a nested
 * theme:
 *
 * ```kotlin
 * AMTheme(tokens = AMTheme.tokens.copy(spacing = AMTheme.tokens.spacing.copy(lg = 20.dp))) { Feed() }
 * ```
 *
 * The style tier (surfaces, shadows, hairlines, the frosted recipe) isn't here: it's the look
 * itself, and stays fixed.
 *
 * @property spacing the spacing scale.
 * @property semantic the success and warning tints that [AMSemantic] reads.
 */
@Immutable
public class AMTokens(
    public val spacing: AMSpacing = AMSpacing(),
    public val semantic: AMSemanticTints = AMSemanticTints(),
) {
    /**
     * A copy with the given values replaced.
     *
     * @param spacing the spacing scale: this one's by default.
     * @param semantic the success and warning tints: this one's by default.
     */
    public fun copy(spacing: AMSpacing = this.spacing, semantic: AMSemanticTints = this.semantic): AMTokens =
        AMTokens(spacing, semantic)

    override fun equals(other: Any?): Boolean =
        other is AMTokens && other.spacing == spacing && other.semantic == semantic

    override fun hashCode(): Int = 31 * spacing.hashCode() + semantic.hashCode()

    override fun toString(): String = "AMTokens(spacing=$spacing, semantic=$semantic)"

    public companion object {
        /** The stock tokens: the 4 dp spacing scale and Material's green and amber tints. */
        public val Default: AMTokens = AMTokens()
    }
}

/**
 * The spacing scale, on a 4 dp grid. Use these between and around elements instead of raw sizes,
 * so a token override moves them together.
 *
 * @property xs 4 dp: tight gaps between related glyphs or inline chips.
 * @property sm 8 dp: compact spacing between elements in a row.
 * @property md 12 dp: standard spacing inside a card.
 * @property lg 16 dp: a card's inner padding, the page gutter, and spacing between sections.
 * @property xl 24 dp: generous room between distinct sections.
 * @property xxl 32 dp: page-level margins and large gaps.
 */
@Immutable
public class AMSpacing(
    public val xs: Dp = 4.dp,
    public val sm: Dp = 8.dp,
    public val md: Dp = 12.dp,
    public val lg: Dp = 16.dp,
    public val xl: Dp = 24.dp,
    public val xxl: Dp = 32.dp,
) {
    /**
     * A copy with the given steps replaced. Each step is this scale's by default.
     *
     * @param xs the 4 dp step.
     * @param sm the 8 dp step.
     * @param md the 12 dp step.
     * @param lg the 16 dp step.
     * @param xl the 24 dp step.
     * @param xxl the 32 dp step.
     */
    public fun copy(
        xs: Dp = this.xs,
        sm: Dp = this.sm,
        md: Dp = this.md,
        lg: Dp = this.lg,
        xl: Dp = this.xl,
        xxl: Dp = this.xxl,
    ): AMSpacing = AMSpacing(xs, sm, md, lg, xl, xxl)

    override fun equals(other: Any?): Boolean =
        other is AMSpacing && other.xs == xs && other.sm == sm && other.md == md &&
            other.lg == lg && other.xl == xl && other.xxl == xxl

    override fun hashCode(): Int = listOf(xs, sm, md, lg, xl, xxl).hashCode()

    override fun toString(): String = "AMSpacing(xs=$xs, sm=$sm, md=$md, lg=$lg, xl=$xl, xxl=$xxl)"
}

/**
 * The status tints Material's color scheme lacks, success and warning, each for light and dark
 * mode. Info and destructive come from the color scheme itself (`primary` and `error`), so an app
 * re-tints those through its scheme. [AMSemantic] resolves all four.
 *
 * The defaults are built the way Material builds `error`, from Material's own tonal palettes:
 * success from the Material green palette (seeded by Green 500, `#4CAF50`), warning from the amber
 * palette (Amber 500, `#FFC107`). See [AMSemanticColors] for the tones.
 *
 * @property successLight the success roles in light mode.
 * @property successDark the success roles in dark mode.
 * @property warningLight the warning roles in light mode.
 * @property warningDark the warning roles in dark mode.
 */
@Immutable
public class AMSemanticTints(
    public val successLight: AMSemanticColors = AMSemanticColors(
        color = Color(0xFF006E1C),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFF94F990),
        onContainer = Color(0xFF002204),
    ),
    public val successDark: AMSemanticColors = AMSemanticColors(
        color = Color(0xFF78DC77),
        onColor = Color(0xFF00390A),
        container = Color(0xFF005313),
        onContainer = Color(0xFF94F990),
    ),
    public val warningLight: AMSemanticColors = AMSemanticColors(
        color = Color(0xFF795900),
        onColor = Color(0xFFFFFFFF),
        container = Color(0xFFFFDF9D),
        onContainer = Color(0xFF261A00),
    ),
    public val warningDark: AMSemanticColors = AMSemanticColors(
        color = Color(0xFFFABD00),
        onColor = Color(0xFF3F2E00),
        container = Color(0xFF5B4300),
        onContainer = Color(0xFFFFDF9D),
    ),
) {
    /**
     * A copy with the given tints replaced. Each is this one's by default.
     *
     * @param successLight the success roles in light mode.
     * @param successDark the success roles in dark mode.
     * @param warningLight the warning roles in light mode.
     * @param warningDark the warning roles in dark mode.
     */
    public fun copy(
        successLight: AMSemanticColors = this.successLight,
        successDark: AMSemanticColors = this.successDark,
        warningLight: AMSemanticColors = this.warningLight,
        warningDark: AMSemanticColors = this.warningDark,
    ): AMSemanticTints = AMSemanticTints(successLight, successDark, warningLight, warningDark)

    override fun equals(other: Any?): Boolean =
        other is AMSemanticTints && other.successLight == successLight && other.successDark == successDark &&
            other.warningLight == warningLight && other.warningDark == warningDark

    override fun hashCode(): Int = listOf(successLight, successDark, warningLight, warningDark).hashCode()

    override fun toString(): String =
        "AMSemanticTints(successLight=$successLight, successDark=$successDark, " +
            "warningLight=$warningLight, warningDark=$warningDark)"
}

/**
 * One status role's colors in one appearance, the four roles Material gives `error`: the role
 * color, the color on it, its container and the color on the container.
 *
 * Built like Material's error roles: in light mode the color is tone 40, the color on it tone 100,
 * the container tone 90 and the color on that tone 10; in dark mode they are tones 80, 20, 30
 * and 90.
 *
 * @property color the role color: icons, text and fills that carry the status (tone 40 light, 80 dark).
 * @property onColor content drawn on [color] (tone 100 light, 20 dark).
 * @property container a quieter fill for the status, such as a tag or a band (tone 90 light, 30 dark).
 * @property onContainer content drawn on [container] (tone 10 light, 90 dark).
 */
@Immutable
public class AMSemanticColors(
    public val color: Color,
    public val onColor: Color,
    public val container: Color,
    public val onContainer: Color,
) {
    override fun equals(other: Any?): Boolean =
        other is AMSemanticColors && other.color == color && other.onColor == onColor &&
            other.container == container && other.onContainer == onContainer

    override fun hashCode(): Int = listOf(color, onColor, container, onContainer).hashCode()

    override fun toString(): String =
        "AMSemanticColors(color=$color, onColor=$onColor, container=$container, onContainer=$onContainer)"
}
