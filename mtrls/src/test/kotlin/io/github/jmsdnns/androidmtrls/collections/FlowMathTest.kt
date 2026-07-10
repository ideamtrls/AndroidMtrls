package io.github.jmsdnns.androidmtrls.collections

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [amComputeFlowLayout].
 *
 * All cases exercise the pure greedy wrapping algorithm with plain Int math —
 * no Compose runtime needed. Cases are derived from the greedy algorithm spec
 * in the ChipGroup port spec §3.4:
 *  - empty list
 *  - single item
 *  - items exactly filling one row
 *  - overflow wraps to a new row
 *  - item wider than maxWidth gets its own row (overflow-wide)
 *  - total height = sum of row heights + spacing * (rows - 1)
 *  - total width = max row width
 *  - positions advance x by width+spacing, y by row height+spacing
 *  - row height = max item height in that row
 */
class FlowMathTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun size(w: Int, h: Int) = AMFlowItemSize(w, h)

    // -------------------------------------------------------------------------
    // Empty list
    // -------------------------------------------------------------------------

    @Test
    fun `empty list returns zero total size and no rows`() {
        val result = amComputeFlowLayout(
            itemSizes = emptyList(),
            maxWidth = 400,
            spacingPx = 8,
        )
        assertEquals(0, result.totalSize.width)
        assertEquals(0, result.totalSize.height)
        assertEquals(0, result.rows.size)
        assertEquals(0, result.positions.size)
    }

    // -------------------------------------------------------------------------
    // Single item
    // -------------------------------------------------------------------------

    @Test
    fun `single item placed at origin with its own dimensions`() {
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(60, 32)),
            maxWidth = 400,
            spacingPx = 8,
        )
        assertEquals(1, result.rows.size)
        assertEquals(listOf(0), result.rows[0])
        assertEquals(1, result.positions.size)
        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowItemSize(width = 60, height = 32), result.totalSize)
    }

    // -------------------------------------------------------------------------
    // Two items exactly fill one row (no overflow)
    // -------------------------------------------------------------------------

    @Test
    fun `two items that exactly fill maxWidth stay on one row`() {
        // item0=60, spacing=8, item1=332 → total=400 == maxWidth
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(60, 32), size(332, 32)),
            maxWidth = 400,
            spacingPx = 8,
        )
        assertEquals(1, result.rows.size)
        assertEquals(listOf(0, 1), result.rows[0])
        // item0 at (0, 0), item1 at (60+8=68, 0)
        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowPosition(x = 68, y = 0), result.positions[1])
        // total: width = 400, height = 32
        assertEquals(AMFlowItemSize(width = 400, height = 32), result.totalSize)
    }

    // -------------------------------------------------------------------------
    // Overflow wraps to a new row
    // -------------------------------------------------------------------------

    @Test
    fun `third item that overflows wraps to second row`() {
        // Three items of width 150, spacing 8, maxWidth 400
        // row0: [0,1] = 150+8+150=308 ≤ 400; adding item2: 308+8+150=466 > 400 → wrap
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(150, 30), size(150, 30), size(150, 30)),
            maxWidth = 400,
            spacingPx = 8,
        )
        assertEquals(2, result.rows.size)
        assertEquals(listOf(0, 1), result.rows[0])
        assertEquals(listOf(2), result.rows[1])

        // Positions
        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowPosition(x = 158, y = 0), result.positions[1])    // 150+8
        assertEquals(AMFlowPosition(x = 0, y = 38), result.positions[2])     // y = row0.height(30) + spacing(8)

        // Total width = max(308, 150) = 308
        // Total height = 30 + 8 + 30 = 68
        assertEquals(AMFlowItemSize(width = 308, height = 68), result.totalSize)
    }

    // -------------------------------------------------------------------------
    // Item wider than maxWidth gets its own row (not skipped)
    // -------------------------------------------------------------------------

    @Test
    fun `item wider than maxWidth gets its own row`() {
        // item0=100, item1=500 (>400), item2=80
        // row0: [0] (100); item1 projected: 100+8+500=608>400 but row not empty → wrap
        //   item1 row: [1] (500>400 but row IS empty when item1 starts → place alone)
        // row2: [2] (80); projected after row1: 500+8+80=588>400 → wrap; [2] alone
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(100, 30), size(500, 30), size(80, 30)),
            maxWidth = 400,
            spacingPx = 8,
        )
        assertEquals(3, result.rows.size)
        assertEquals(listOf(0), result.rows[0])
        assertEquals(listOf(1), result.rows[1])
        assertEquals(listOf(2), result.rows[2])

        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowPosition(x = 0, y = 38), result.positions[1])   // 30+8
        assertEquals(AMFlowPosition(x = 0, y = 76), result.positions[2])   // 30+8+30+8

        // total width = max(100, 500, 80) = 500
        // total height = 30+8+30+8+30 = 106
        assertEquals(AMFlowItemSize(width = 500, height = 106), result.totalSize)
    }

    // -------------------------------------------------------------------------
    // Row height = max item height in that row
    // -------------------------------------------------------------------------

    @Test
    fun `row height is maximum item height in the row`() {
        // Two items on same row, different heights
        // item0 = 100x20, item1 = 80x40; both fit in maxWidth=300
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(100, 20), size(80, 40)),
            maxWidth = 300,
            spacingPx = 8,
        )
        assertEquals(1, result.rows.size)
        assertEquals(AMFlowItemSize(width = 188, height = 40), result.totalSize)  // width=100+8+80=188, height=max(20,40)=40
    }

    // -------------------------------------------------------------------------
    // Two rows, different heights — y advances by row height + spacing
    // -------------------------------------------------------------------------

    @Test
    fun `y position advances by row height plus spacing`() {
        // row0: item0 (200x50), item1 (180x30) — row height=50; item1 x=208
        // row2: item2 (120x25) — y = 50+8=58
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(200, 50), size(180, 30), size(120, 25)),
            maxWidth = 400,
            spacingPx = 8,
        )
        assertEquals(2, result.rows.size)

        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowPosition(x = 208, y = 0), result.positions[1])  // 200+8
        assertEquals(AMFlowPosition(x = 0, y = 58), result.positions[2])   // row0.height(50)+spacing(8)

        // total width = max(200+8+180=388, 120) = 388
        // total height = 50 + 8 + 25 = 83
        assertEquals(AMFlowItemSize(width = 388, height = 83), result.totalSize)
    }

    // -------------------------------------------------------------------------
    // Three rows — total height = sum of row heights + spacing * (rows-1)
    // -------------------------------------------------------------------------

    @Test
    fun `three rows total height equals sum of row heights plus inter-row spacing`() {
        // All items width=200, height=30, spacing=10, maxWidth=250
        // Each row holds only one item (200+10+200=410>250 from row2 onward)
        // Actually two items would give 200+10+200=410>250 → each alone
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(200, 30), size(200, 30), size(200, 30)),
            maxWidth = 250,
            spacingPx = 10,
        )
        assertEquals(3, result.rows.size)
        // total height = 30+10+30+10+30 = 110; total width = 200
        assertEquals(AMFlowItemSize(width = 200, height = 110), result.totalSize)
        // Positions
        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowPosition(x = 0, y = 40), result.positions[1])   // 30+10
        assertEquals(AMFlowPosition(x = 0, y = 80), result.positions[2])   // 30+10+30+10
    }

    // -------------------------------------------------------------------------
    // Total width = max row width
    // -------------------------------------------------------------------------

    @Test
    fun `total width equals max row width across all rows`() {
        // row0: two items fitting 200+8+150=358; row1: one item 300; max=358
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(200, 30), size(150, 30), size(300, 30)),
            maxWidth = 400,
            spacingPx = 8,
        )
        // item0+item1 = 358 ≤ 400; item0+item1+item2 = 358+8+300=666>400 → item2 wraps
        assertEquals(2, result.rows.size)
        assertEquals(358, result.totalSize.width)
    }

    // -------------------------------------------------------------------------
    // Zero spacing
    // -------------------------------------------------------------------------

    @Test
    fun `zero spacing items placed adjacently`() {
        val result = amComputeFlowLayout(
            itemSizes = listOf(size(100, 20), size(100, 20), size(100, 20)),
            maxWidth = 300,
            spacingPx = 0,
        )
        // 100+0+100+0+100=300 == maxWidth → all on one row
        assertEquals(1, result.rows.size)
        assertEquals(AMFlowPosition(x = 0, y = 0), result.positions[0])
        assertEquals(AMFlowPosition(x = 100, y = 0), result.positions[1])
        assertEquals(AMFlowPosition(x = 200, y = 0), result.positions[2])
        assertEquals(AMFlowItemSize(width = 300, height = 20), result.totalSize)
    }

    // -------------------------------------------------------------------------
    // Many items — regression: row indices list is correct
    // -------------------------------------------------------------------------

    @Test
    fun `row indices reference correct item positions`() {
        // 5 items width=90, spacing=8, maxWidth=300
        // row0: 0,1,2 → 90+8+90+8+90=286 ≤ 300; +8+90=384>300 → wrap at 3
        // row1: 3,4 → 90+8+90=188 ≤ 300
        val result = amComputeFlowLayout(
            itemSizes = List(5) { size(90, 24) },
            maxWidth = 300,
            spacingPx = 8,
        )
        assertEquals(2, result.rows.size)
        assertEquals(listOf(0, 1, 2), result.rows[0])
        assertEquals(listOf(3, 4), result.rows[1])
    }
}
