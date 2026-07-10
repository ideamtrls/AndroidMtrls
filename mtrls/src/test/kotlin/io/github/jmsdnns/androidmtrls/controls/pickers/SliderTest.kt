package io.github.jmsdnns.androidmtrls.controls.pickers

import io.github.jmsdnns.androidmtrls.controls.pickers.amSliderClampValue
import io.github.jmsdnns.androidmtrls.controls.pickers.amSliderDragDeltaToValueDelta
import io.github.jmsdnns.androidmtrls.controls.pickers.amSliderFractionToValue
import io.github.jmsdnns.androidmtrls.controls.pickers.amSliderPositionToValue
import io.github.jmsdnns.androidmtrls.controls.pickers.amSliderValueToFraction
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the pure track-math functions in [AMSlider].
 *
 * All tested functions are extracted as internal top-level functions in
 * `Slider.kt` so they are testable without Compose runtime.
 *
 * Math under test:
 *  - [amSliderValueToFraction] — value → normalized [0,1] fraction
 *  - [amSliderFractionToValue] — [0,1] fraction → value in range
 *  - [amSliderClampValue]      — value clamped to [start, end]
 *  - [amSliderDragDeltaToValueDelta] — px drag delta → value delta
 *  - [amSliderPositionToValue] — absolute finger position → value in range
 */
class SliderTest {

    // -------------------------------------------------------------------------
    // amSliderValueToFraction
    // -------------------------------------------------------------------------

    @Test
    fun `valueToFraction returns 0 at range start`() {
        assertEquals(0f, amSliderValueToFraction(0f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `valueToFraction returns 1 at range end`() {
        assertEquals(1f, amSliderValueToFraction(1f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `valueToFraction returns 0_5 at midpoint`() {
        assertEquals(0.5f, amSliderValueToFraction(0.5f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `valueToFraction works with non-zero start`() {
        // range 10..30, value 20 → fraction 0.5
        assertEquals(0.5f, amSliderValueToFraction(20f, 10f, 30f), 0.0001f)
    }

    @Test
    fun `valueToFraction returns 0_25 at quarter point`() {
        // range 0..100, value 25 → fraction 0.25
        assertEquals(0.25f, amSliderValueToFraction(25f, 0f, 100f), 0.0001f)
    }

    @Test
    fun `valueToFraction clamps value below start to 0`() {
        // value -10 for range 0..1 → fraction should clamp to 0
        assertEquals(0f, amSliderValueToFraction(-10f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `valueToFraction clamps value above end to 1`() {
        // value 2 for range 0..1 → fraction should clamp to 1
        assertEquals(1f, amSliderValueToFraction(2f, 0f, 1f), 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amSliderFractionToValue
    // -------------------------------------------------------------------------

    @Test
    fun `fractionToValue returns start when fraction is 0`() {
        assertEquals(0f, amSliderFractionToValue(0f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `fractionToValue returns end when fraction is 1`() {
        assertEquals(1f, amSliderFractionToValue(1f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `fractionToValue returns midpoint when fraction is 0_5`() {
        assertEquals(0.5f, amSliderFractionToValue(0.5f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `fractionToValue works with non-zero start`() {
        // range 10..30, fraction 0.5 → value 20
        assertEquals(20f, amSliderFractionToValue(0.5f, 10f, 30f), 0.0001f)
    }

    @Test
    fun `fractionToValue returns start plus 75 percent of range`() {
        // range 0..100, fraction 0.75 → value 75
        assertEquals(75f, amSliderFractionToValue(0.75f, 0f, 100f), 0.0001f)
    }

    @Test
    fun `fractionToValue and valueToFraction are inverse`() {
        val start = 5f
        val end   = 95f
        val value = 37f
        val roundtrip = amSliderFractionToValue(
            amSliderValueToFraction(value, start, end),
            start, end
        )
        assertEquals(value, roundtrip, 0.001f)
    }

    // -------------------------------------------------------------------------
    // amSliderClampValue
    // -------------------------------------------------------------------------

    @Test
    fun `clampValue returns value unchanged when within range`() {
        assertEquals(0.5f, amSliderClampValue(0.5f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `clampValue clamps to start when value is below range`() {
        assertEquals(0f, amSliderClampValue(-1f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `clampValue clamps to end when value is above range`() {
        assertEquals(1f, amSliderClampValue(2f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `clampValue clamps to range start on wide range`() {
        assertEquals(10f, amSliderClampValue(-5f, 10f, 90f), 0.0001f)
    }

    @Test
    fun `clampValue clamps to range end on wide range`() {
        assertEquals(90f, amSliderClampValue(100f, 10f, 90f), 0.0001f)
    }

    @Test
    fun `clampValue returns boundary start exactly`() {
        assertEquals(0f, amSliderClampValue(0f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `clampValue returns boundary end exactly`() {
        assertEquals(1f, amSliderClampValue(1f, 0f, 1f), 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amSliderDragDeltaToValueDelta
    // -------------------------------------------------------------------------

    @Test
    fun `dragDeltaToValueDelta returns positive delta for rightward drag on 0-1 range`() {
        // track 100px, range 0..1 — 10px right → +0.1 value change
        assertEquals(0.1f, amSliderDragDeltaToValueDelta(10f, 100f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `dragDeltaToValueDelta returns negative delta for leftward drag`() {
        // track 100px, range 0..1 — -10px left → -0.1 value change
        assertEquals(-0.1f, amSliderDragDeltaToValueDelta(-10f, 100f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `dragDeltaToValueDelta scales with range`() {
        // track 200px, range 0..100 — 10px right → 5.0 value change
        assertEquals(5f, amSliderDragDeltaToValueDelta(10f, 200f, 0f, 100f), 0.0001f)
    }

    @Test
    fun `dragDeltaToValueDelta returns zero for zero drag`() {
        assertEquals(0f, amSliderDragDeltaToValueDelta(0f, 200f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `dragDeltaToValueDelta half-track drag equals half the range`() {
        // track 200px, range 0..1 — 100px → 0.5 value change
        assertEquals(0.5f, amSliderDragDeltaToValueDelta(100f, 200f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `dragDeltaToValueDelta full-track drag equals the entire range`() {
        // track 200px, range 0..1 — 200px → 1.0 value change
        assertEquals(1f, amSliderDragDeltaToValueDelta(200f, 200f, 0f, 1f), 0.0001f)
    }

    @Test
    fun `dragDeltaToValueDelta returns zero when track width is zero`() {
        // zero track width guard — must not crash and must return 0
        assertEquals(0f, amSliderDragDeltaToValueDelta(10f, 0f, 0f, 1f), 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amSliderValueToFraction — degenerate range guard
    // -------------------------------------------------------------------------

    @Test
    fun `valueToFraction returns zero when start equals end`() {
        // degenerate range: start == end → fraction defaults to 0
        assertEquals(0f, amSliderValueToFraction(5f, 5f, 5f), 0.0001f)
    }

    // -------------------------------------------------------------------------
    // amSliderPositionToValue — absolute finger-position → value mapping
    // -------------------------------------------------------------------------

    @Test
    fun `positionToValue returns range start at position 0`() {
        // finger at the left edge → start of the range
        assertEquals(0f, amSliderPositionToValue(0f, 100f, 0f..1f), 0.0001f)
    }

    @Test
    fun `positionToValue returns range end at full track width`() {
        // finger at the right edge → end of the range
        assertEquals(1f, amSliderPositionToValue(100f, 100f, 0f..1f), 0.0001f)
    }

    @Test
    fun `positionToValue returns midpoint at half track width`() {
        assertEquals(0.5f, amSliderPositionToValue(50f, 100f, 0f..1f), 0.0001f)
    }

    @Test
    fun `positionToValue maps into a non-zero-start range`() {
        // range 10..30, half width → value 20
        assertEquals(20f, amSliderPositionToValue(50f, 100f, 10f..30f), 0.0001f)
    }

    @Test
    fun `positionToValue clamps a negative position to range start`() {
        assertEquals(0f, amSliderPositionToValue(-25f, 100f, 0f..1f), 0.0001f)
    }

    @Test
    fun `positionToValue clamps an overshooting position to range end`() {
        assertEquals(1f, amSliderPositionToValue(150f, 100f, 0f..1f), 0.0001f)
    }

    @Test
    fun `positionToValue returns range start when track width is zero`() {
        // degenerate zero-width guard — must not divide by zero
        assertEquals(0f, amSliderPositionToValue(50f, 0f, 0f..1f), 0.0001f)
    }

    @Test
    fun `positionToValue returns range start when track width is negative`() {
        // negative width guard resolves to the range start (here 10)
        assertEquals(10f, amSliderPositionToValue(50f, -100f, 10f..30f), 0.0001f)
    }
}
