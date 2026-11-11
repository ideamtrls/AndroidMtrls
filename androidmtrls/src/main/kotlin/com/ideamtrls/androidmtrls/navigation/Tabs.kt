package com.ideamtrls.androidmtrls

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

/**
 * Tabs inside a page: Material's secondary tab row over a content area that shows the selected
 * tab's content, crossfading to another tab's when the selection changes. They switch what part
 * of a page shows, not the app's views; the remote does that.
 *
 * ```kotlin
 * var tab by rememberSaveable { mutableStateOf("posts") }
 * AMTabs(selection = tab, onSelectionChange = { tab = it }) {
 *     tab("posts", "Posts") { PostsList() }
 *     tab("replies", "Replies") { RepliesList() }
 *     if (showsDrafts) tab("drafts", "Drafts") { DraftsList() }
 * }
 * ```
 *
 * - **Tabs** are declared in [tabs], in order: a label (and optionally a glyph before it) and the
 *   content it shows. `if` and loops work there. Tapping a tab calls [onSelectionChange] with its
 *   id; the selection is the caller's, so a change made elsewhere switches the tabs too.
 * - **The strip** is Material's: equal-width tabs, the selected label in `onSurface` and the others
 *   in `onSurfaceVariant`, the indicator sliding under the selected tab, a divider under the row,
 *   ripple, and each tab read as a selected or unselected tab.
 * - **The content** of the selected tab alone is composed; it crossfades with Material's motion
 *   scheme when the selection changes. A [selection] matching no tab shows no content and no
 *   indicator. The area fills the height the tabs are given, under the strip; in a scrolling
 *   column it takes its content's height.
 * - **The surface** under the strip and the content is one flat rounded shape (`shapes.large`)
 *   with no shadow: the floating surface's fill for [AMSurfaceFill.Solid], or glass over the
 *   nearest [AMBackdrop]'s art for [AMSurfaceFill.Frosted], matching the app's cards.
 *
 * @param selection the id of the tab showing.
 * @param onSelectionChange called with a tab's id when it's tapped.
 * @param modifier the tabs' placement and size.
 * @param fill solid (the default) or frosted over art, like the app's cards.
 * @param enabled whether the tabs take taps, here and in any disabling container around them.
 * @param tabs declares the tabs, in order, through [AMTabsScope.tab].
 */
@Composable
public fun <T> AMTabs(
    selection: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: AMSurfaceFill = AMSurfaceFill.Solid,
    enabled: Boolean = true,
    tabs: AMTabsScope<T>.() -> Unit,
) {
    val entries = AMTabsScope<T>().apply(tabs).entries
    val selectedIndex = entries.indexOfFirst { it.id == selection }
    val isEnabled = resolveEnabled(enabled)
    val shape = MaterialTheme.shapes.large
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = when (fill) {
        AMSurfaceFill.Solid -> Modifier.background(AMTheme.Surface.primary, shape)
        AMSurfaceFill.Frosted -> Modifier.amElevatedSurface(shape, AMElevation.None, AMSurfaceFill.Frosted)
    }
    Column(modifier.then(surface).clip(shape)) {
        SecondaryTabRow(
            selectedTabIndex = selectedIndex.coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = selectedColor,
            indicator = {
                if (selectedIndex >= 0) {
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = false))
                }
            },
        ) {
            entries.forEachIndexed { index, entry ->
                val onClick = { onSelectionChange(entry.id) }
                if (entry.icon == null) {
                    Tab(
                        selected = index == selectedIndex,
                        onClick = onClick,
                        enabled = isEnabled,
                        text = entry.label,
                        selectedContentColor = selectedColor,
                        unselectedContentColor = unselectedColor,
                    )
                } else {
                    LeadingIconTab(
                        selected = index == selectedIndex,
                        onClick = onClick,
                        text = entry.label,
                        icon = entry.icon,
                        enabled = isEnabled,
                        selectedContentColor = selectedColor,
                        unselectedContentColor = unselectedColor,
                    )
                }
            }
        }
        Crossfade(
            targetState = selection,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "AMTabs",
        ) { id ->
            entries.firstOrNull { it.id == id }?.content?.invoke()
        }
    }
}

/**
 * Where an [AMTabs]'s tabs are declared, in order. Each [tab] adds one: its id, its label and the
 * content it shows. Plain Kotlin runs here, so `if` and loops add tabs conditionally or from data.
 */
public class AMTabsScope<T> internal constructor() {
    internal val entries: MutableList<TabEntry<T>> = mutableListOf()

    /**
     * Adds a tab labeled [label].
     *
     * @param id the tab's id: the selection when it's showing.
     * @param label the tab's label, in the tab's own type; up to two lines.
     * @param icon a glyph before the label, usually an `Icon` with a null description. Null
     *   shows none.
     * @param content what the tab shows while it's selected.
     */
    public fun tab(
        id: T,
        label: String,
        icon: (@Composable () -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        tab(id, label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) }, icon = icon, content = content)
    }

    /**
     * Adds a tab with custom label content. The tab sets the label's type and color around what
     * [label] emits.
     *
     * @param id the tab's id: the selection when it's showing.
     * @param label the tab's label content.
     * @param icon a glyph before the label. Null shows none.
     * @param content what the tab shows while it's selected.
     */
    public fun tab(
        id: T,
        label: @Composable () -> Unit,
        icon: (@Composable () -> Unit)? = null,
        content: @Composable () -> Unit,
    ) {
        entries += TabEntry(id, label, icon, content)
    }
}

/** One declared tab. */
internal class TabEntry<T>(
    val id: T,
    val label: @Composable () -> Unit,
    val icon: (@Composable () -> Unit)?,
    val content: @Composable () -> Unit,
)
