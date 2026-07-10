package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ---------------------------------------------------------------------------
// AMSemantic — the canonical role → color + role → glyph axis
// ---------------------------------------------------------------------------

/**
 * The one canonical semantic axis shared by every status-bearing primitive —
 * `AMTag` and `AMBanner` — so `info`/`success`/`warning`/`destructive` resolve
 * to a single color and a single glyph everywhere, instead of each primitive
 * re-deciding.
 *
 * This is the Kotlin port of iOS `IMSemantic` (`Sources/Tokens/Semantic.swift`).
 * Two members define the axis:
 *
 * - [color] — the role's tint, resolved against a live [AMTokensSemantics] set
 *   so per-subtree `AMTokensProvider` re-tints flow through. This is the main
 *   tint a tag foreground or a banner accent reads; a faint fill wash is that
 *   same color at a per-hue opacity, applied by the caller.
 * - [icon] — the filled glyph that reads as this role at a glance (the leading
 *   glyph on an `AMBanner`).
 *
 * **Consumers.** `AMBannerStyle` is a `typealias` for this enum, so a banner
 * style *is* a semantic role. `AMTagTint` delegates its four charged cases here
 * for their base color while keeping its uncharged `NEUTRAL` case — and its
 * per-hue fill-opacity table — local.
 *
 * The case set is the four *charged* roles only. A primitive with an extra
 * uncharged case (`AMTagTint.NEUTRAL`) keeps that case on its own type and
 * delegates only its semantic cases here.
 */
@Immutable
enum class AMSemantic {

    /** Informational — neutral status worth noticing (default: iOS system blue). */
    Info,

    /** Success / confirmation — completed, active, healthy (default: iOS system green). */
    Success,

    /** Warning / attention — needs a look, degraded (default: iOS system yellow). */
    Warning,

    /** Destructive / error — failed, blocked, removal (default: iOS system red). */
    Destructive;

    /**
     * The tint color for this role, resolved against the supplied
     * [AMTokensSemantics] set (so per-subtree `AMTokensProvider` re-tints flow
     * through) for the given [isDark] mode. Each role's field is an
     * [AMAdaptiveColor] pair, so the light/dark hue is picked here — matching how
     * `accent` is consumed. Pure and draw-scope safe: pass
     * `LocalAMTokens.current.semantic` and `LocalAMIsDarkTheme.current` (captured
     * at the composable boundary) at the call site.
     *
     * @param semantics The live semantic token set to resolve against.
     * @param isDark    Current dark-mode flag; selects the pair's light/dark hue.
     */
    fun color(semantics: AMTokensSemantics, isDark: Boolean): Color = when (this) {
        Info        -> semantics.info.resolve(isDark)
        Success     -> semantics.success.resolve(isDark)
        Warning     -> semantics.warning.resolve(isDark)
        Destructive -> semantics.destructive.resolve(isDark)
    }

    /**
     * Filled glyph that reads as this role at a glance, sourced from
     * `material-icons-core` only (`:mtrls` has no extended-icons dependency).
     *
     * The iOS originals and their core substitutions:
     *
     * | Role        | iOS SF Symbol                 | Core glyph                | Note             |
     * |-------------|-------------------------------|---------------------------|------------------|
     * | Info        | info.circle.fill              | Icons.Filled.Info         | direct match     |
     * | Success     | checkmark.circle.fill         | Icons.Filled.CheckCircle  | direct match     |
     * | Warning     | exclamationmark.triangle.fill | Icons.Filled.Warning      | direct match     |
     * | Destructive | xmark.octagon.fill            | Icons.Filled.Close        | xmark-octagon sub |
     *
     * **Destructive substitution:** `icons-core` has no `xmark.octagon`
     * (stop-sign shape). [Icons.Filled.Close] (x glyph) is the closest core
     * glyph that conveys a hard stop / error semantic without pulling in the
     * extended icon set — the established Banner precedent.
     */
    val icon: ImageVector get() = when (this) {
        Info        -> Icons.Filled.Info
        Success     -> Icons.Filled.CheckCircle
        Warning     -> Icons.Filled.Warning
        Destructive -> Icons.Filled.Close
    }
}
