package io.github.jmsdnns.androidmtrls.lists

import android.content.res.Configuration
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.AMThemeRoot
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMReduceMotion
import io.github.jmsdnns.androidmtrls.tokens.LocalAMTokens
import io.github.jmsdnns.androidmtrls.tokens.amSquircle
import io.github.jmsdnns.androidmtrls.tokens.resolved
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// ---------------------------------------------------------------------------
// AMReorderableCardList — reorderable lazy list for card-shaped content
// ---------------------------------------------------------------------------

/**
 * Reorderable vertical list for card-shaped content.
 *
 * Wraps a transparent [LazyColumn] with the `sh.calvin.reorderable` 2.x engine
 * so cards can be rearranged by long-press drag. Each reorder event calls
 * [onReorder] with the full updated list — the caller owns the state and writes
 * it back.
 *
 * **Pairing rule:** Use [AMListCard] as the content slot. This list is the card
 * counterpart of [AMReorderableRowList]. The separation exists because UIKit's
 * `List` edit-mode causes rows to shift horizontally to accommodate trailing
 * chrome (drag handles, delete buttons) — that horizontal shift looks wrong on
 * rounded card-shaped content. The iOS `IMReorderableCardList` solves this with
 * a `UICollectionView`-backed implementation where the lifted card sits directly
 * over its original position without any horizontal offset. This Compose port
 * achieves the same visual result: cards do not show a trailing drag handle;
 * instead the entire card surface is a long-press drag target.
 *
 * **Drag surface:** No visible handle. The entire cell body is a long-press drag
 * surface via `Modifier.longPressDraggableHandle`. This matches the iOS UIKit
 * path where `collectionView.dragInteractionEnabled = true` + long-press
 * initiates the drag from anywhere on the cell.
 *
 * **Drag lift:** while a card is being dragged, `isDragging` is `true` inside
 * the [ReorderableItem] scope. A `graphicsLayer { shadowElevation = … }` bump
 * (0dp → 8dp) gives the illusion of the card lifting off the surface — the same
 * "ghost lift" UIKit's collection view performs. This elevation is distinct from
 * the two-layer card shadow that [AMListCard] itself draws.
 *
 * **Cell margins (matching iOS `UIHostingConfiguration` margins):**
 * iOS applies `.margins(.horizontal, tokens.spacing.lg)` and
 * `.margins(.vertical, tokens.spacing.xs)` in the cell registration. Each
 * `ReorderableItem` here wraps its content in
 * `Modifier.padding(horizontal = 16.dp, vertical = 4.dp)` — `spacing.lg`
 * horizontal, `spacing.xs` vertical.
 *
 * **Haptics:** drag start fires [HapticFeedbackType.LongPress]; drag end fires
 * [HapticFeedbackType.GestureEnd] (available in Compose UI from BOM 2024.01+).
 *
 * **Structural vs field diffing:** provide a stable [key] so Compose can diff
 * structural changes (add/remove/reorder) from field-only mutations. When only a
 * field changes, Compose recomposes the cell in-place rather than reinserting it
 * — equivalent to iOS `reconfigureItems` in the diffable data source.
 *
 * **Caller-owned state:** this composable is stateless. Hold `items` in a
 * `MutableState`, pass the current snapshot in, and write the updated list back
 * in [onReorder]:
 *
 * ```kotlin
 * var tasks by remember { mutableStateOf(initialTasks) }
 *
 * AMReorderableCardList(
 *     items = tasks,
 *     key = { it.id },
 *     onReorder = { tasks = it },
 * ) { task ->
 *     AMListCard(modifier = Modifier.fillMaxWidth()) {
 *         BasicText(task.title, style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)))
 *     }
 * }
 * ```
 *
 * @param items     Ordered snapshot of items to display. Not mutated.
 * @param key       Stable unique key for each item. Must be consistent across
 *                  recompositions. Used as the [LazyColumn] item key.
 * @param onReorder Called on every drag-over event with the fully reordered list.
 *                  The caller must write the result back to their state for live
 *                  move-as-you-drag to work correctly.
 * @param modifier  Layout modifier applied to the root [LazyColumn].
 * @param content   Card composable slot. Receives the item. Typically wraps
 *                  content in [AMListCard].
 */
@Composable
fun <T : Any> AMReorderableCardList(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val reduceMotion = LocalAMReduceMotion.current
    // Lift shadow uses the default card radius (tokens.radius.lg = 16dp) so the
    // shadow contour matches the rounded card corners while dragging.
    val tokens = LocalAMTokens.current
    val liftShape = amSquircle(tokens.radius.lg)

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
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 8.dp else 0.dp,
                    // Route the drag-lift through the house Reduce-Motion policy.
                    // The effective default here is spring<Dp>(); wrapping it in
                    // tokens.motion.resolved leaves that spring untouched under
                    // normal motion and collapses it to the 180ms overlay-fade
                    // tween under Reduce Motion, so the lift settles without a
                    // spatial spring when the user has disabled animations. Every
                    // other animating primitive routes the same way.
                    animationSpec = tokens.motion.resolved(spring<Dp>(), reduceMotion),
                    label = "AMReorderableCardList.dragElevation",
                )
                // Cell margin: spacing.lg horizontal (16.dp), spacing.xs vertical (4.dp)
                // mirrors iOS UIHostingConfiguration.margins(.horizontal, spacing.lg) /
                // .margins(.vertical, spacing.xs).
                val cellModifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .graphicsLayer {
                        shadowElevation = elevation.toPx()
                        shape = liftShape
                        clip = false
                    }
                    .longPressDraggableHandle(
                        onDragStarted = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragStopped = {
                            haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        },
                    )
                // The content slot receives the cell modifier so the drag surface,
                // margins, and lift shadow are applied to whatever the caller renders.
                // Typically the caller passes an AMListCard which already fills width.
                Box(modifier = cellModifier) {
                    content(item)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews — light + dark (required by conventions)
// ---------------------------------------------------------------------------

private data class PreviewTask(val id: Int, val title: String, val detail: String)

private val previewTasks = listOf(
    PreviewTask(1, "Write the release notes", "Due Friday · Docs"),
    PreviewTask(2, "Review the design tokens", "Due Monday · Design"),
    PreviewTask(3, "Profile the launch path", "Due Tuesday · Perf"),
)

@Composable
private fun ReorderableCardListPreviewContent() {
    val isDark = LocalAMIsDarkTheme.current
    var items by remember { mutableStateOf(previewTasks) }

    AMReorderableCardList(
        items = items,
        key = { it.id },
        onReorder = { items = it },
    ) { task ->
        AMListCard(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                text = task.title,
                style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            )
            BasicText(
                text = task.detail,
                style = AMTheme.Type.caption.toTextStyle(AMTheme.Text.secondary.resolve(isDark)),
            )
        }
    }
}

@Preview(
    name = "AMReorderableCardList · Light",
    group = "AMReorderableCardList",
    showBackground = true,
    backgroundColor = 0xFFF2F2F7,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Composable
private fun AMReorderableCardListPreviewLight() {
    AMThemeRoot {
        ReorderableCardListPreviewContent()
    }
}

@Preview(
    name = "AMReorderableCardList · Dark",
    group = "AMReorderableCardList",
    showBackground = true,
    backgroundColor = 0xFF0C0C0D,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AMReorderableCardListPreviewDark() {
    AMThemeRoot {
        ReorderableCardListPreviewContent()
    }
}
