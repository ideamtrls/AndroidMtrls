package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// AMTextStyle — a single locked type-scale entry
// ---------------------------------------------------------------------------

/**
 * One entry in the locked AndroidMtrls type scale (see [AMTheme.Type]).
 *
 * Typography is part of the library's signature voice, so these values are
 * locked and never routed through `AMTokens`. Following the `AMTheme`
 * dimension-token convention, [fontSize] and [letterSpacing] are `Float`
 * constants meaning **sp**; convert to a Compose [TextStyle] (which applies
 * `.sp`) with [toTextStyle].
 *
 * @param fontSize      Font size in sp.
 * @param fontWeight    Compose [FontWeight] for the entry.
 * @param letterSpacing Tracking in sp (Material label/body metric).
 */
@Immutable
data class AMTextStyle(
    val fontSize: Float,
    val fontWeight: FontWeight,
    val letterSpacing: Float,
) {

    /**
     * Materialise this locked scale entry as a Compose [TextStyle] with the
     * given [color], applying `.sp` to the [fontSize] and [letterSpacing]
     * Floats.
     *
     * @param color Text color to apply (typically an `AMTheme.Text.*` value
     *   resolved for the current mode).
     */
    fun toTextStyle(color: Color): TextStyle = TextStyle(
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing.sp,
    )
}
