package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AMSemantic] — the canonical role→color + role→glyph axis.
 *
 * Pure JVM tests: [AMSemantic.color] resolves against a plain [AMTokensSemantics]
 * data class and [AMSemantic.icon] builds a `material-icons-core` [ImageVector],
 * neither of which needs the Android framework or a Compose runtime.
 */
class SemanticTest {

    private val semantics = AMTokens.standard.semantic

    // -------------------------------------------------------------------------
    // color(semantics, isDark) — each role resolves its adaptive pair per mode
    // -------------------------------------------------------------------------

    @Test
    fun `color resolves Info to the semantic info token`() {
        assertEquals(semantics.info.resolve(false), AMSemantic.Info.color(semantics, isDark = false))
        assertEquals(Color(0xFF007AFF), AMSemantic.Info.color(semantics, isDark = false)) // iOS system blue (light)
        assertEquals(Color(0xFF0A84FF), AMSemantic.Info.color(semantics, isDark = true))  // brighter dark variant
    }

    @Test
    fun `color resolves Success to the semantic success token`() {
        assertEquals(semantics.success.resolve(false), AMSemantic.Success.color(semantics, isDark = false))
        assertEquals(Color(0xFF34C759), AMSemantic.Success.color(semantics, isDark = false)) // iOS system green (light)
        assertEquals(Color(0xFF30D158), AMSemantic.Success.color(semantics, isDark = true))  // dark variant
    }

    @Test
    fun `color resolves Warning to the semantic warning token`() {
        assertEquals(semantics.warning.resolve(false), AMSemantic.Warning.color(semantics, isDark = false))
        assertEquals(Color(0xFFFFCC00), AMSemantic.Warning.color(semantics, isDark = false)) // iOS system yellow (light)
        assertEquals(Color(0xFFFFD60A), AMSemantic.Warning.color(semantics, isDark = true))  // dark variant
    }

    @Test
    fun `color resolves Destructive to the semantic destructive token`() {
        assertEquals(semantics.destructive.resolve(false), AMSemantic.Destructive.color(semantics, isDark = false))
        assertEquals(Color(0xFFFF3B30), AMSemantic.Destructive.color(semantics, isDark = false)) // iOS system red (light)
        assertEquals(Color(0xFFFF453A), AMSemantic.Destructive.color(semantics, isDark = true))  // dark variant
    }

    @Test
    fun `color reflects an overridden semantic token set`() {
        // color() reads the supplied token set, so an AMTokensProvider override
        // flows through rather than resolving against the locked AMTheme.Semantic.
        val custom = AMTokensSemantics(info = AMAdaptiveColor(light = Color(0xFF00FF00), dark = Color(0xFF00AA00)))
        assertEquals(Color(0xFF00FF00), AMSemantic.Info.color(custom, isDark = false))
        assertEquals(Color(0xFF00AA00), AMSemantic.Info.color(custom, isDark = true))
    }

    // -------------------------------------------------------------------------
    // icon — each role maps to its material-icons-core glyph
    // -------------------------------------------------------------------------

    @Test
    fun `icon maps each role to its core glyph`() {
        assertEquals(Icons.Filled.Info,        AMSemantic.Info.icon)
        assertEquals(Icons.Filled.CheckCircle, AMSemantic.Success.icon)
        assertEquals(Icons.Filled.Warning,     AMSemantic.Warning.icon)
        // Destructive substitutes Close for the absent xmark.octagon.
        assertEquals(Icons.Filled.Close,       AMSemantic.Destructive.icon)
    }
}
