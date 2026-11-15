package com.ideamtrls.androidmtrls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** A filled button style is the same style exactly when it's the same kind at the same elevation. */
class ButtonStyleTest {

    @Test
    fun theSameKindAtTheSameElevationIsTheSameStyle() {
        assertEquals(AMButtonStyle.Primary(), AMButtonStyle.Primary())
        assertEquals(AMButtonStyle.Primary().hashCode(), AMButtonStyle.Primary().hashCode())
        assertEquals(AMButtonStyle.Secondary(AMElevation.None), AMButtonStyle.Secondary(AMElevation.None))
        assertEquals(
            AMButtonStyle.Secondary(AMElevation.None).hashCode(),
            AMButtonStyle.Secondary(AMElevation.None).hashCode(),
        )
    }

    @Test
    fun anotherElevationIsAnotherStyle() {
        assertNotEquals(AMButtonStyle.Primary(AMElevation.None), AMButtonStyle.Primary())
        assertNotEquals(AMButtonStyle.Secondary(AMElevation.None), AMButtonStyle.Secondary())
    }

    @Test
    fun primaryAndSecondaryAreNeverTheSameStyle() {
        assertNotEquals(AMButtonStyle.Primary(), AMButtonStyle.Secondary())
        assertNotEquals(AMButtonStyle.Secondary(), AMButtonStyle.Primary())
        assertNotEquals(AMButtonStyle.Primary().hashCode(), AMButtonStyle.Secondary().hashCode())
    }

    @Test
    fun aStyleNamesOnlyItsElevation() {
        assertEquals("Primary(elevation=Low)", AMButtonStyle.Primary().toString())
        assertEquals("Secondary(elevation=None)", AMButtonStyle.Secondary(AMElevation.None).toString())
    }
}
