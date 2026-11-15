package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How a floating surface draws: the app's setting decides it, and a piece embedded in a surface
 * (inside a card, tabs or a section, or on a sheet) draws the tint in a frosted app, so nothing
 * samples frost inside frost.
 */
class SurfaceLookTest {

    @Test
    fun theSettingDecidesTheLook() {
        assertEquals(SurfaceLook.Solid, surfaceLook(AMSurfaceFill.Solid, embedded = false))
        assertEquals(SurfaceLook.Solid, surfaceLook(AMSurfaceFill.Solid, embedded = true))
        assertEquals(SurfaceLook.Frosted, surfaceLook(AMSurfaceFill.Frosted, embedded = false))
        assertEquals(SurfaceLook.Tinted, surfaceLook(AMSurfaceFill.Frosted, embedded = true))
    }

    @Test
    fun fieldsTakeTheTintEmbeddedInAFrostedApp() {
        assertEquals(FieldContainer.Tint, fieldContainer(AMSurfaceFill.Frosted, LiesOn.Surface))
        assertEquals(FieldContainer.Tint, fieldContainer(AMSurfaceFill.Frosted, LiesOn.Sheet))
        assertEquals(FieldContainer.Clear, fieldContainer(AMSurfaceFill.Frosted, LiesOn.Page))
        assertEquals(FieldContainer.Solid, fieldContainer(AMSurfaceFill.Solid, LiesOn.Sheet))
        assertEquals(FieldContainer.Clear, fieldContainer(AMSurfaceFill.Solid, LiesOn.Surface))
        assertEquals(FieldContainer.Clear, fieldContainer(AMSurfaceFill.Solid, LiesOn.Page))
    }
}
