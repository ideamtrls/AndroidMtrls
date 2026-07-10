package io.github.jmsdnns.androidmtrls.demo.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

/**
 * Shared scaffold for every component demo page (non-Scene entries).
 *
 * Mirrors iOS `ComponentPage<Content>` from `Catalog/ComponentPage.swift`:
 * - Page-background fill ([AMTheme.Surface.page])
 * - Optional page header (rendered via the catalog header helper)
 * - Vertically scrollable content slot
 *
 * **Light/dark toggle:** The toggle rides the shell top bar's trailing slot and
 * is hoisted to `MainActivity` above the `NavHost`. [ComponentPage] does NOT manage its own
 * dark-override; it inherits whatever [LocalAMIsDarkTheme] is current.
 * (iOS `ComponentPage` used a per-page `schemeOverride`; the Android port
 * intentionally deviates — global toggle is simpler and equally useful for a
 * demo catalog.)
 *
 * @param entry      The catalog entry whose [ComponentEntry.title] is displayed
 *                   in the page header when [showsTitle] is true.
 * @param showsTitle If false, the header is suppressed (Typography page uses this
 *                   because every header IS a type specimen).
 * @param content    The page body; composed below the optional header inside a
 *                   vertically scrollable column.
 */
@Composable
fun ComponentPage(
    entry: ComponentEntry,
    showsTitle: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val pageColor = AMTheme.Surface.page.resolve(isDark)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = AMTheme.Spacing.lg.dp),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xl.dp),
        ) {
            if (showsTitle) {
                catalogCategoryHeader(title = entry.title)
            }
            content()
        }
    }
}
