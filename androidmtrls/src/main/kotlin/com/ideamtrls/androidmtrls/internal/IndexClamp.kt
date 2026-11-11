package com.ideamtrls.androidmtrls

/**
 * Pins a page or card [index] into a collection of [count] items: `0` when the collection is
 * empty (no index is valid), otherwise into `0 until count`. The index-driven collections (the
 * carousel, the card stack) draw a briefly out-of-range index clamped through it, so they never
 * show an empty strip, and never write the clamp back.
 */
internal fun clampIndex(index: Int, count: Int): Int =
    if (count <= 0) 0 else index.coerceIn(0, count - 1)
