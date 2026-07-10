package io.github.jmsdnns.androidmtrls.lists

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// ---------------------------------------------------------------------------
// AMReorderableRowList — reorderable lazy list for flat row content
// ---------------------------------------------------------------------------

/**
 * Reorderable vertical list for flat row-style content.
 *
 * Wraps a transparent [LazyColumn] with the `sh.calvin.reorderable` 2.x engine
 * so items can be rearranged by drag. Each reorder event calls [onReorder] with
 * the full updated list — the caller owns the state and writes it back.
 *
 * **Pairing rule:** Do NOT use [AMListCard] here — card content (padded,
 * rounded, elevated) looks wrong beside the trailing drag handle. Use
 * [AMReorderableCardList] for card-shaped content. Pair this composable with
 * [AMListRow].
 *
 * **Edit mode duality:**
 * - `isEditing = false` (default): no visible handle. The entire row body is
 *   a long-press drag surface via `Modifier.longPressDraggableHandle`. Matches
 *   the iOS internal-edit-mode path where long-press anywhere initiates reorder.
 * - `isEditing = true`: a drawn drag handle (three short rounded horizontal bars)
 *   appears on the trailing edge of each row. Only the handle itself is the drag
 *   surface via `Modifier.draggableHandle`. Matches the iOS edit-mode visual where
 *   UIKit shows the standard trailing three-line grab handle.
 *
 * **Handle glyph note:** `Icons.Rounded.DragHandle` is not in `material-icons-core`.
 * The handle is drawn with [Canvas] as three 14dp × 2dp rounded horizontal bars
 * spaced 4dp apart (the iOS three-line grip), tinted [AMTheme.Icon.muted]. This
 * precedent mirrors the minus bar in `AMStepper`.
 *
 * **Haptics:** drag start fires [HapticFeedbackType.LongPress]; drag end fires
 * [HapticFeedbackType.GestureEnd] (available in Compose UI from BOM 2024.01+).
 *
 * **Attach-state note:** this composable is stateless — the caller owns
 * `items: List<T>` in a `MutableState`. Pass the current list in; receive the
 * updated list via [onReorder] and write it back. This is idiomatic Compose
 * (avoids mutable state passed as a reference).
 *
 * ```kotlin
 * var queue by remember { mutableStateOf(tracks) }
 *
 * AMReorderableRowList(
 *     items = queue,
 *     key = { it.id },
 *     onReorder = { queue = it },
 *     isEditing = editMode,
 * ) { track, isDragging ->
 *     AMListRow(title = track.title, subtitle = track.artist)
 * }
 * ```
 *
 * @param items       Ordered snapshot of items to display. Not mutated.
 * @param key         Stable unique key for each item. Must be consistent across
 *                    recompositions. Used as the [LazyColumn] item key.
 * @param onReorder   Called on every drag-over event with the fully reordered list.
 *                    The caller must write the result back to their state for live
 *                    move-as-you-drag to work correctly.
 * @param isEditing   When `true`, shows a trailing drawn drag handle that is the
 *                    drag surface. When `false`, the entire row body is a long-press
 *                    drag surface (no visible handle).
 * @param rowPadding  Padding applied to each item's outer wrapper.
 *                    Defaults to [PaddingValues] of zero so [AMListRow] cells touch
 *                    the list edges (matching iOS `rowInsets: EdgeInsets = EdgeInsets()`).
 * @param modifier    Layout modifier applied to the root [LazyColumn].
 * @param content     Row composable slot. Receives the item and [isDragging] — a
 *                    boolean that is `true` while this particular item is being
 *                    dragged. Use [isDragging] for visual lift feedback
 *                    (e.g. `animateDpAsState` shadow elevation).
 */
@Composable
fun <T : Any> AMReorderableRowList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    isEditing: Boolean = false,
    rowPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
    content: @Composable (T, isDragging: Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalAMIsDarkTheme.current
    val handleTint = AMTheme.Icon.muted.resolve(isDark)

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(amMoveItem(items, from.index, to.index))
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
    ) {
        items(items, key = { key(it) }) { item ->
            ReorderableItem(reorderableState, key = key(item)) { isDragging ->
                if (isEditing) {
                    // Edit mode: trailing drawn handle is the drag surface.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f).padding(rowPadding)) {
                            content(item, isDragging)
                        }
                        // Drawn drag handle — three short rounded horizontal bars.
                        // DragHandle glyph is absent from material-icons-core; Canvas
                        // drawn per AMENDED ruling 5 (precedent: AMStepper minus bar).
                        // The 44dp Box is the touch/semantics target (minimum recommended
                        // touch target size); the glyph Canvas is 24dp and centered inside.
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    },
                                )
                                .semantics { contentDescription = "Reorder" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val barWidth = 14.dp.toPx()
                                val barHeight = 2.dp.toPx()
                                val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                                val centerX = (size.width - barWidth) / 2f
                                val gap = 4.dp.toPx()
                                // Three stacked bars = iOS's three-line reorder grip.
                                val barCount = 3
                                val totalHeight = barHeight * barCount + gap * (barCount - 1)
                                val startY = (size.height - totalHeight) / 2f

                                repeat(barCount) { i ->
                                    drawRoundRect(
                                        color = handleTint,
                                        topLeft = Offset(centerX, startY + i * (barHeight + gap)),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = cornerRadius,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Default mode: no handle — whole row body is a long-press drag surface.
                    Box(
                        modifier = Modifier
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                },
                            ),
                    ) {
                        Box(modifier = Modifier.padding(rowPadding)) {
                            content(item, isDragging)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

private data class PreviewTrack(val id: Int, val title: String, val artist: String)

private val previewTracks = listOf(
    PreviewTrack(1, "Open Water", "Harbor Lights"),
    PreviewTrack(2, "Celestial Drive", "Nova Fade"),
    PreviewTrack(3, "Morning Static", "Low Signal"),
    PreviewTrack(4, "Drift Code", "Pale Current"),
)

@Composable
private fun ReorderableRowListPreviewContent(isEditing: Boolean) {
    var items by remember { mutableStateOf(previewTracks) }
    AMReorderableRowList(
        items = items,
        key = { it.id },
        onReorder = { items = it },
        isEditing = isEditing,
    ) { track, _ ->
        AMListRow(title = track.title, subtitle = track.artist)
    }
}

@Preview(
    name = "AMReorderableRowList · Light · Default",
    group = "AMReorderableRowList",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMReorderableRowListPreviewLight() {
    AMThemeRoot {
        ReorderableRowListPreviewContent(isEditing = false)
    }
}

@Preview(
    name = "AMReorderableRowList · Dark · Editing",
    group = "AMReorderableRowList",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMReorderableRowListPreviewDark() {
    AMThemeRoot {
        ReorderableRowListPreviewContent(isEditing = true)
    }
}
