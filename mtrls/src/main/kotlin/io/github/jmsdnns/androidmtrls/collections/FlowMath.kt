package io.github.jmsdnns.androidmtrls.collections

// ---------------------------------------------------------------------------
// AMFlowItemSize — plain Int width/height carrier (avoids androidx.compose.ui.unit
// import friction in pure JVM unit tests while matching the algorithm's needs)
// ---------------------------------------------------------------------------

/**
 * Plain width×height carrier used by [amComputeFlowLayout].
 *
 * Defined as a plain `data class` rather than `androidx.compose.ui.unit.IntSize`
 * so the algorithm remains testable in a pure JVM unit-test environment with no
 * Compose runtime on the classpath. The [ChipGroup.kt] composable converts
 * measured [androidx.compose.ui.layout.Placeable] dimensions into this type
 * before calling [amComputeFlowLayout].
 *
 * @param width Width in pixels (integer).
 * @param height Height in pixels (integer).
 */
internal data class AMFlowItemSize(val width: Int, val height: Int)

// ---------------------------------------------------------------------------
// AMFlowPosition — x/y placement in pixels
// ---------------------------------------------------------------------------

/**
 * Top-leading placement coordinate produced by [amComputeFlowLayout].
 *
 * @param x Horizontal offset from the layout's left edge, in pixels.
 * @param y Vertical offset from the layout's top edge, in pixels.
 */
internal data class AMFlowPosition(val x: Int, val y: Int)

// ---------------------------------------------------------------------------
// AMFlowLayoutResult — full result of one layout pass
// ---------------------------------------------------------------------------

/**
 * Result of [amComputeFlowLayout].
 *
 * @param rows      Each element is the ordered list of item indices that share
 *                  a row. Indices match the original [amComputeFlowLayout]
 *                  `itemSizes` list.
 * @param positions Per-item top-leading placement in pixels, indexed to match
 *                  the original `itemSizes` list.
 * @param totalSize Bounding box of the entire flow in pixels:
 *                  width = max row width across all rows;
 *                  height = sum of row heights + spacing × (rowCount − 1).
 */
internal data class AMFlowLayoutResult(
    val rows: List<List<Int>>,
    val positions: List<AMFlowPosition>,
    val totalSize: AMFlowItemSize,
)

// ---------------------------------------------------------------------------
// amComputeFlowLayout — greedy left-to-right wrapping algorithm
// ---------------------------------------------------------------------------

/**
 * Greedy left-to-right flow-layout algorithm ported from `FlowLayout.layoutRows`
 * in iOSMtrls (`Sources/Collections/ChipGroup.swift`).
 *
 * Items are placed left-to-right in a row until the next item would overflow
 * [maxWidth]. When overflow is detected the current row is committed and a
 * new row begins. An item that is individually wider than [maxWidth] still
 * receives its own row (it is never skipped).
 *
 * The function is **pure** — no Compose runtime required — so it can be called
 * from plain JVM unit tests without an Android device or emulator.
 *
 * ### Algorithm (mirrors iOS `layoutRows`)
 * 1. For each item in order:
 *    - `projectedWidth` = if row empty → item width,
 *                         else → currentRowWidth + spacing + item width.
 *    - If `projectedWidth > maxWidth` AND current row is **not** empty →
 *      commit current row; start a new row.
 *    - Append item to the current row; advance `currentRowWidth`;
 *      update `currentRowHeight = max(currentRowHeight, item.height)`.
 * 2. After all items, commit the final row if non-empty.
 * 3. `totalHeight` = sum of committed row heights + [spacingPx] × (rowCount − 1).
 * 4. `totalWidth`  = maximum committed row width.
 * 5. `positions`: x advances by item width + [spacingPx] within each row;
 *    y advances by row height + [spacingPx] between rows.
 *
 * @param itemSizes  Measured sizes of each child in order, in pixels.
 * @param maxWidth   Available width in pixels (the layout's measured width).
 * @param spacingPx  Gap between items horizontally and between rows vertically,
 *                   in pixels. Mirrors the single `spacing` parameter of the
 *                   iOS `FlowLayout`.
 * @return [AMFlowLayoutResult] with rows, per-item positions, and total size.
 */
internal fun amComputeFlowLayout(
    itemSizes: List<AMFlowItemSize>,
    maxWidth: Int,
    spacingPx: Int,
): AMFlowLayoutResult {
    if (itemSizes.isEmpty()) {
        return AMFlowLayoutResult(
            rows = emptyList(),
            positions = emptyList(),
            totalSize = AMFlowItemSize(width = 0, height = 0),
        )
    }

    // Mutable row accumulator: each row holds (itemIndex, itemSize) pairs.
    data class MutableRow(
        val items: MutableList<Pair<Int, AMFlowItemSize>> = mutableListOf(),
        var width: Int = 0,
        var height: Int = 0,
    )

    val committedRows = mutableListOf<MutableRow>()
    var current = MutableRow()

    for ((index, size) in itemSizes.withIndex()) {
        val projectedWidth = if (current.items.isEmpty()) {
            size.width
        } else {
            current.width + spacingPx + size.width
        }

        // Overflow: commit current row and start fresh — but only if current row
        // is non-empty (mirrors iOS `!current.items.isEmpty` guard, ensuring a
        // single item wider than maxWidth still gets placed).
        if (projectedWidth > maxWidth && current.items.isNotEmpty()) {
            committedRows.add(current)
            current = MutableRow()
        }

        // Add item to current row
        if (current.items.isEmpty()) {
            current.width = size.width
        } else {
            current.width += spacingPx + size.width
        }
        current.items.add(index to size)
        current.height = maxOf(current.height, size.height)
    }

    // Commit trailing row
    if (current.items.isNotEmpty()) {
        committedRows.add(current)
    }

    // Build result arrays
    val rowIndices = committedRows.map { row -> row.items.map { it.first } }
    val positions = Array(itemSizes.size) { AMFlowPosition(0, 0) }

    var y = 0
    for ((rowIdx, row) in committedRows.withIndex()) {
        var x = 0
        for ((itemIndex, size) in row.items) {
            positions[itemIndex] = AMFlowPosition(x = x, y = y)
            x += size.width + spacingPx
        }
        y += row.height
        if (rowIdx < committedRows.lastIndex) {
            y += spacingPx
        }
    }

    val totalWidth = committedRows.maxOf { it.width }
    // y now equals the sum of all row heights + spacing between them
    val totalHeight = y

    return AMFlowLayoutResult(
        rows = rowIndices,
        positions = positions.toList(),
        totalSize = AMFlowItemSize(width = totalWidth, height = totalHeight),
    )
}
