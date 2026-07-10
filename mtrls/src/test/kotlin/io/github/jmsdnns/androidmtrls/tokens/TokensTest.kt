package io.github.jmsdnns.androidmtrls.tokens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for [AMTokens], [LocalAMTokens], and the token-derived padding extensions.
 *
 * Pure JVM tests — no Android framework or Compose runtime required.
 * All tests exercise data-class semantics and value fidelity to [AMTheme].
 */
class TokensTest {

    // -------------------------------------------------------------------------
    // AMTokens.standard — mirrors AMTheme values exactly
    // -------------------------------------------------------------------------

    @Test
    fun `standard semantic info mirrors AMTheme`() {
        assertEquals(AMTheme.Semantic.info, AMTokens.standard.semantic.info)
    }

    @Test
    fun `standard semantic success mirrors AMTheme`() {
        assertEquals(AMTheme.Semantic.success, AMTokens.standard.semantic.success)
    }

    @Test
    fun `standard semantic warning mirrors AMTheme`() {
        assertEquals(AMTheme.Semantic.warning, AMTokens.standard.semantic.warning)
    }

    @Test
    fun `standard semantic destructive mirrors AMTheme`() {
        assertEquals(AMTheme.Semantic.destructive, AMTokens.standard.semantic.destructive)
    }

    @Test
    fun `standard semantic accent mirrors AMTheme`() {
        assertEquals(AMTheme.accent, AMTokens.standard.semantic.accent)
    }

    @Test
    fun `standard spacing xs mirrors AMTheme`() {
        assertEquals(AMTheme.Spacing.xs.dp, AMTokens.standard.spacing.xs)
    }

    @Test
    fun `standard spacing sm mirrors AMTheme`() {
        assertEquals(AMTheme.Spacing.sm.dp, AMTokens.standard.spacing.sm)
    }

    @Test
    fun `standard spacing md mirrors AMTheme`() {
        assertEquals(AMTheme.Spacing.md.dp, AMTokens.standard.spacing.md)
    }

    @Test
    fun `standard spacing lg mirrors AMTheme`() {
        assertEquals(AMTheme.Spacing.lg.dp, AMTokens.standard.spacing.lg)
    }

    @Test
    fun `standard spacing xl mirrors AMTheme`() {
        assertEquals(AMTheme.Spacing.xl.dp, AMTokens.standard.spacing.xl)
    }

    @Test
    fun `standard spacing xxl mirrors AMTheme`() {
        assertEquals(AMTheme.Spacing.xxl.dp, AMTokens.standard.spacing.xxl)
    }

    @Test
    fun `standard radius sm mirrors AMTheme`() {
        assertEquals(AMTheme.Radius.sm.dp, AMTokens.standard.radius.sm)
    }

    @Test
    fun `standard radius md mirrors AMTheme`() {
        assertEquals(AMTheme.Radius.md.dp, AMTokens.standard.radius.md)
    }

    @Test
    fun `standard radius lg mirrors AMTheme`() {
        assertEquals(AMTheme.Radius.lg.dp, AMTokens.standard.radius.lg)
    }

    @Test
    fun `standard input inset mirrors AMTheme`() {
        assertEquals(AMTheme.Input.inset.dp, AMTokens.standard.input.inset)
    }

    @Test
    fun `standard input radius mirrors AMTheme`() {
        assertEquals(AMTheme.Input.radius.dp, AMTokens.standard.input.radius)
    }

    @Test
    fun `standard motion dismissThreshold mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.dismissThreshold.dp, AMTokens.standard.motion.dismissThreshold)
    }

    @Test
    fun `standard motion dismissThresholdCompact mirrors AMTheme`() {
        assertEquals(
            AMTheme.Motion.dismissThresholdCompact.dp,
            AMTokens.standard.motion.dismissThresholdCompact
        )
    }

    @Test
    fun `standard motion snapSpring mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.snapSpring, AMTokens.standard.motion.snapSpring)
    }

    @Test
    fun `standard motion presentSpring mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.presentSpring, AMTokens.standard.motion.presentSpring)
    }

    @Test
    fun `standard motion modalSpring mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.modalSpring, AMTokens.standard.motion.modalSpring)
    }

    @Test
    fun `standard motion overlayFade mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.overlayFade, AMTokens.standard.motion.overlayFade)
    }

    @Test
    fun `standard motion pressFade mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.pressFade, AMTokens.standard.motion.pressFade)
    }

    @Test
    fun `standard motion selectionFade mirrors AMTheme`() {
        assertEquals(AMTheme.Motion.selectionFade, AMTokens.standard.motion.selectionFade)
    }

    // -------------------------------------------------------------------------
    // AMTokens default construction == standard
    // -------------------------------------------------------------------------

    @Test
    fun `AMTokens default construction equals standard`() {
        assertEquals(AMTokens.standard, AMTokens())
    }

    // -------------------------------------------------------------------------
    // AMTokens data-class copy semantics — transform API
    // -------------------------------------------------------------------------

    @Test
    fun `copy preserves unchanged fields`() {
        val modified = AMTokens.standard.copy(
            spacing = AMTokens.standard.spacing.copy(lg = 20.dp)
        )

        // Modified field
        assertEquals(20.dp, modified.spacing.lg)

        // Unchanged fields are preserved
        assertEquals(AMTheme.Spacing.md.dp, modified.spacing.md)
        assertEquals(AMTheme.Spacing.xl.dp, modified.spacing.xl)
        assertEquals(AMTheme.Radius.lg.dp,  modified.radius.lg)
        assertEquals(AMTheme.Input.inset.dp, modified.input.inset)
    }

    @Test
    fun `copy with single semantic override preserves all other semantics`() {
        val brandRed = AMAdaptiveColor(light = Color(0xFFFF0000), dark = Color(0xFFCC0000))
        val modified = AMTokens.standard.copy(
            semantic = AMTokens.standard.semantic.copy(destructive = brandRed)
        )

        assertEquals(brandRed, modified.semantic.destructive)
        assertEquals(AMTheme.Semantic.info,    modified.semantic.info)
        assertEquals(AMTheme.Semantic.success, modified.semantic.success)
        assertEquals(AMTheme.Semantic.warning, modified.semantic.warning)
        assertEquals(AMTheme.accent,           modified.semantic.accent)
    }

    @Test
    fun `copy with accent override preserves all other semantics`() {
        val brandAccent = AMAdaptiveColor(light = Color(0xFF6200EE), dark = Color(0xFFBB86FC))
        val modified = AMTokens.standard.copy(
            semantic = AMTokens.standard.semantic.copy(accent = brandAccent)
        )

        assertEquals(brandAccent, modified.semantic.accent)
        assertEquals(AMTheme.Semantic.info,        modified.semantic.info)
        assertEquals(AMTheme.Semantic.success,     modified.semantic.success)
        assertEquals(AMTheme.Semantic.warning,     modified.semantic.warning)
        assertEquals(AMTheme.Semantic.destructive, modified.semantic.destructive)
    }

    @Test
    fun `copy with no changes equals original`() {
        val copy = AMTokens.standard.copy()
        assertEquals(AMTokens.standard, copy)
    }

    @Test
    fun `transform lambda receives inherited values and returns modified copy`() {
        // Simulate what AMTokensProvider(transform) does internally:
        // transform receives the inherited AMTokens and returns a copy.
        val inherited = AMTokens.standard
        val transform: (AMTokens) -> AMTokens = { it.copy(
            radius = it.radius.copy(lg = 24.dp)
        )}

        val result = transform(inherited)

        assertEquals(24.dp, result.radius.lg)
        // Other radius values unchanged
        assertEquals(AMTheme.Radius.sm.dp, result.radius.sm)
        assertEquals(AMTheme.Radius.md.dp, result.radius.md)
    }

    @Test
    fun `transform compose — two sequential transforms accumulate correctly`() {
        // First transform: change spacing lg
        val transform1: (AMTokens) -> AMTokens = { it.copy(
            spacing = it.spacing.copy(lg = 20.dp)
        )}
        // Second transform: change radius lg (simulates a nested subtree override)
        val transform2: (AMTokens) -> AMTokens = { it.copy(
            radius = it.radius.copy(lg = 24.dp)
        )}

        val after1 = transform1(AMTokens.standard)
        val after2 = transform2(after1)

        assertEquals(20.dp, after2.spacing.lg)  // from transform1
        assertEquals(24.dp, after2.radius.lg)   // from transform2
        // Both transforms leave other fields at standard values
        assertEquals(AMTheme.Spacing.md.dp, after2.spacing.md)
        assertEquals(AMTheme.Radius.sm.dp,  after2.radius.sm)
    }

    @Test
    fun `modified tokens are not equal to standard`() {
        val modified = AMTokens.standard.copy(
            spacing = AMTokens.standard.spacing.copy(lg = 20.dp)
        )
        assertNotEquals(AMTokens.standard, modified)
    }

    // -------------------------------------------------------------------------
    // Sub data-class equality and copy
    // -------------------------------------------------------------------------

    @Test
    fun `AMTokensSemantics default construction equals itself`() {
        assertEquals(AMTokensSemantics(), AMTokensSemantics())
    }

    @Test
    fun `AMTokensSpacing default construction equals itself`() {
        assertEquals(AMTokensSpacing(), AMTokensSpacing())
    }

    @Test
    fun `AMTokensRadius default construction equals itself`() {
        assertEquals(AMTokensRadius(), AMTokensRadius())
    }

    @Test
    fun `AMTokensInput default construction equals itself`() {
        assertEquals(AMTokensInput(), AMTokensInput())
    }

    @Test
    fun `AMTokensMotion default construction equals itself`() {
        assertEquals(AMTokensMotion(), AMTokensMotion())
    }

    // -------------------------------------------------------------------------
    // Token-derived padding extension properties
    // -------------------------------------------------------------------------

    @Test
    fun `rowInsets uses spacing lg horizontal and spacing md vertical`() {
        val insets = AMTokens.standard.rowInsets
        val expected = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        assertEquals(
            expected.calculateLeftPadding(LayoutDirection.Ltr),
            insets.calculateLeftPadding(LayoutDirection.Ltr)
        )
        assertEquals(
            expected.calculateRightPadding(LayoutDirection.Ltr),
            insets.calculateRightPadding(LayoutDirection.Ltr)
        )
        assertEquals(expected.calculateTopPadding(),    insets.calculateTopPadding())
        assertEquals(expected.calculateBottomPadding(), insets.calculateBottomPadding())
    }

    @Test
    fun `blockInsets uses spacing lg for all edges`() {
        val insets = AMTokens.standard.blockInsets
        val expected = PaddingValues(all = 16.dp)
        assertEquals(
            expected.calculateLeftPadding(LayoutDirection.Ltr),
            insets.calculateLeftPadding(LayoutDirection.Ltr)
        )
        assertEquals(
            expected.calculateRightPadding(LayoutDirection.Ltr),
            insets.calculateRightPadding(LayoutDirection.Ltr)
        )
        assertEquals(expected.calculateTopPadding(),    insets.calculateTopPadding())
        assertEquals(expected.calculateBottomPadding(), insets.calculateBottomPadding())
    }

    @Test
    fun `listRowInsets uses input inset horizontal and spacing lg vertical`() {
        val insets = AMTokens.standard.listRowInsets
        val expected = PaddingValues(horizontal = 14.dp, vertical = 16.dp)
        assertEquals(
            expected.calculateLeftPadding(LayoutDirection.Ltr),
            insets.calculateLeftPadding(LayoutDirection.Ltr)
        )
        assertEquals(
            expected.calculateRightPadding(LayoutDirection.Ltr),
            insets.calculateRightPadding(LayoutDirection.Ltr)
        )
        assertEquals(expected.calculateTopPadding(),    insets.calculateTopPadding())
        assertEquals(expected.calculateBottomPadding(), insets.calculateBottomPadding())
    }

    @Test
    fun `titleBarInsets uses input inset horizontal and 10dp vertical`() {
        val insets = AMTokens.standard.titleBarInsets
        val expected = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        assertEquals(
            expected.calculateLeftPadding(LayoutDirection.Ltr),
            insets.calculateLeftPadding(LayoutDirection.Ltr)
        )
        assertEquals(
            expected.calculateRightPadding(LayoutDirection.Ltr),
            insets.calculateRightPadding(LayoutDirection.Ltr)
        )
        assertEquals(expected.calculateTopPadding(),    insets.calculateTopPadding())
        assertEquals(expected.calculateBottomPadding(), insets.calculateBottomPadding())
    }

    @Test
    fun `token-derived rowInsets with custom spacing propagate custom values`() {
        // When spacing tokens are overridden, the derived padding must reflect them.
        // We can't introspect PaddingValues directly, but we CAN verify different
        // token sets produce different (not-equal) PaddingValues objects.
        val standard  = AMTokens.standard.rowInsets
        val customLg  = AMTokens.standard.copy(
            spacing = AMTokens.standard.spacing.copy(lg = 20.dp)
        ).rowInsets
        // Different lg → different PaddingValues; reference inequality is the signal.
        assertNotEquals(standard, customLg)
    }
}
