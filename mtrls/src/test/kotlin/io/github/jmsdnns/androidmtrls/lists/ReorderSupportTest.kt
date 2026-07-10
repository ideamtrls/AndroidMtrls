package io.github.jmsdnns.androidmtrls.lists

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for [amMoveItem] in [ReorderSupport.kt].
 *
 * All cases are pure list operations — no Compose runtime needed.
 *
 * Cases covered:
 *  - move forward (fromIndex < toIndex)
 *  - move backward (fromIndex > toIndex)
 *  - adjacent swap (distance == 1)
 *  - no-op (fromIndex == toIndex, or clamped indices equal): returns the SAME input instance
 *  - first → last
 *  - last → first
 *  - single-element list (degenerate; no change)
 *  - input list is NOT mutated (defensive copy)
 *  - a real move returns a new list instance (never the same reference as input)
 */
class ReorderSupportTest {

    // -------------------------------------------------------------------------
    // Move forward (fromIndex < toIndex)
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem moves element forward in list`() {
        // [A, B, C, D] — move index 0 → index 2 → [B, C, A, D]
        val result = amMoveItem(listOf("A", "B", "C", "D"), fromIndex = 0, toIndex = 2)
        assertEquals(listOf("B", "C", "A", "D"), result)
    }

    @Test
    fun `moveItem moves element from second to last position`() {
        // [1, 2, 3, 4] — move index 1 → index 3 → [1, 3, 4, 2]
        val result = amMoveItem(listOf(1, 2, 3, 4), fromIndex = 1, toIndex = 3)
        assertEquals(listOf(1, 3, 4, 2), result)
    }

    // -------------------------------------------------------------------------
    // Move backward (fromIndex > toIndex)
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem moves element backward in list`() {
        // [A, B, C, D] — move index 3 → index 1 → [A, D, B, C]
        val result = amMoveItem(listOf("A", "B", "C", "D"), fromIndex = 3, toIndex = 1)
        assertEquals(listOf("A", "D", "B", "C"), result)
    }

    @Test
    fun `moveItem moves element from last to second position`() {
        // [1, 2, 3, 4] — move index 3 → index 1 → [1, 4, 2, 3]
        val result = amMoveItem(listOf(1, 2, 3, 4), fromIndex = 3, toIndex = 1)
        assertEquals(listOf(1, 4, 2, 3), result)
    }

    // -------------------------------------------------------------------------
    // Adjacent swap (distance == 1)
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem adjacent swap forward`() {
        // [A, B, C] — move index 0 → index 1 → [B, A, C]
        val result = amMoveItem(listOf("A", "B", "C"), fromIndex = 0, toIndex = 1)
        assertEquals(listOf("B", "A", "C"), result)
    }

    @Test
    fun `moveItem adjacent swap backward`() {
        // [A, B, C] — move index 2 → index 1 → [A, C, B]
        val result = amMoveItem(listOf("A", "B", "C"), fromIndex = 2, toIndex = 1)
        assertEquals(listOf("A", "C", "B"), result)
    }

    // -------------------------------------------------------------------------
    // No-op: fromIndex == toIndex
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem fromIndex equals toIndex returns list with same elements`() {
        val input = listOf("X", "Y", "Z")
        val result = amMoveItem(input, fromIndex = 1, toIndex = 1)
        assertEquals(input, result)
    }

    // -------------------------------------------------------------------------
    // First ↔ last
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem first element to last`() {
        // [A, B, C, D] — move index 0 → index 3 → [B, C, D, A]
        val result = amMoveItem(listOf("A", "B", "C", "D"), fromIndex = 0, toIndex = 3)
        assertEquals(listOf("B", "C", "D", "A"), result)
    }

    @Test
    fun `moveItem last element to first`() {
        // [A, B, C, D] — move index 3 → index 0 → [D, A, B, C]
        val result = amMoveItem(listOf("A", "B", "C", "D"), fromIndex = 3, toIndex = 0)
        assertEquals(listOf("D", "A", "B", "C"), result)
    }

    // -------------------------------------------------------------------------
    // Single-element list
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem single element list returns equal list`() {
        val input = listOf("Solo")
        val result = amMoveItem(input, fromIndex = 0, toIndex = 0)
        assertEquals(input, result)
    }

    // -------------------------------------------------------------------------
    // Defensive copy: input list must NOT be mutated
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem does not mutate the input list`() {
        val input = listOf("A", "B", "C", "D")
        val snapshot = input.toList()
        amMoveItem(input, fromIndex = 0, toIndex = 3)
        assertEquals("input list must be unchanged after amMoveItem", snapshot, input)
    }

    // -------------------------------------------------------------------------
    // New list instance
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem returns a new list instance`() {
        val input = listOf("A", "B", "C")
        val result = amMoveItem(input, fromIndex = 0, toIndex = 2)
        assertNotSame("amMoveItem must return a new list, not the same reference", input, result)
    }

    @Test
    fun `moveItem no-op returns the same instance`() {
        val input = listOf("A", "B", "C")
        val result = amMoveItem(input, fromIndex = 1, toIndex = 1)
        assertSame(
            "amMoveItem no-op path intentionally skips the defensive copy and returns the input itself",
            input,
            result,
        )
    }

    // -------------------------------------------------------------------------
    // Two-element list — exhaustive coverage of the simplest reorderable case
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem swaps two-element list`() {
        val result = amMoveItem(listOf("first", "second"), fromIndex = 0, toIndex = 1)
        assertEquals(listOf("second", "first"), result)
    }

    // -------------------------------------------------------------------------
    // Out-of-range indices: clamping (never throws)
    // -------------------------------------------------------------------------

    @Test
    fun `moveItem clamps negative fromIndex to zero`() {
        // fromIndex = -1 clamps to 0; move first element to index 2 → [B, C, A, D]
        val result = amMoveItem(listOf("A", "B", "C", "D"), fromIndex = -1, toIndex = 2)
        assertEquals(listOf("B", "C", "A", "D"), result)
    }

    @Test
    fun `moveItem clamps out-of-range toIndex to lastIndex`() {
        // toIndex = size+3 = 7 clamps to lastIndex (3); move index 0 to end → [B, C, D, A]
        val result = amMoveItem(listOf("A", "B", "C", "D"), fromIndex = 0, toIndex = 7)
        assertEquals(listOf("B", "C", "D", "A"), result)
    }
}
