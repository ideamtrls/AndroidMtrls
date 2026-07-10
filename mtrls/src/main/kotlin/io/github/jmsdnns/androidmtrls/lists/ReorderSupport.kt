package io.github.jmsdnns.androidmtrls.lists

/**
 * Pure list-reorder helper shared by [AMReorderableRowList] and
 * [AMReorderableCardList].
 *
 * Moves the element at [fromIndex] to [toIndex] and returns a new list.
 * The input list is never mutated — a defensive copy (`toMutableList`) is
 * made before any structural change.
 *
 * **Defensive clamping:** if [items] is empty, the original list is returned
 * immediately. Both [fromIndex] and [toIndex] are clamped to
 * `0..items.lastIndex` via [coerceIn] before the move, so out-of-range
 * indices never throw; if the clamped indices are equal, the original list
 * is returned unchanged (still a defensive copy is avoided for this path).
 *
 * This function is the single source of truth for the "live move-as-you-drag"
 * write-back step: both reorderable list composables call it from their
 * `rememberReorderableLazyListState { from, to -> onReorder(amMoveItem(items, from.index, to.index)) }`
 * lambda.
 *
 * @param items     The current ordered list. Not mutated.
 * @param fromIndex Zero-based source index of the element to move. Clamped to
 *                  valid range before use.
 * @param toIndex   Zero-based destination index where the element is inserted.
 *                  Clamped to valid range before use.
 * @return          A new [List] with the element relocated, or [items] itself
 *                  when [items] is empty or clamped indices are equal.
 */
internal fun <T> amMoveItem(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
    if (items.isEmpty()) return items
    val clampedFrom = fromIndex.coerceIn(0, items.lastIndex)
    val clampedTo = toIndex.coerceIn(0, items.lastIndex)
    if (clampedFrom == clampedTo) return items
    val mutable = items.toMutableList()
    val element = mutable.removeAt(clampedFrom)
    mutable.add(clampedTo, element)
    return mutable
}
