package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ideamtrls.androidmtrls.AMPageHeader
import com.ideamtrls.androidmtrls.AMTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** The default Preview: none. A page that passes no Preview shows its Loaded demo alone. */
private val NoPreview: @Composable () -> Unit = {}

/**
 * The scaffold every component page uses, top to bottom:
 *
 * 1. the page header ([AMPageHeader]), titled with the component's name, and the one-line summary;
 * 2. **Preview**: the component once, in its simplest default setup, live;
 * 3. **Loaded**: one fuller composition that shows most of what the component does at a glance.
 *
 * A page is the widget and nothing else: the two demos carry no headers, captions or code. A page
 * whose Preview would only repeat its Loaded demo passes [loaded] alone and shows no Preview.
 *
 * [previewLayout] and [loadedLayout] set where each demo sits ([StageLayout]); most pages keep the
 * floating default. A component that lives either flat or in a card previews flat, edge to edge
 * ([StageLayout.FullBleed]), and shows the card placement in its Loaded composition.
 *
 * The page is the screen's root ([CatalogScreenHost]): a banner or a toast a demo posts shows over
 * the whole page, top app bar included.
 *
 * The page holds no demo state; the demo composables own theirs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComponentPage(
    entry: ComponentEntry,
    previewLayout: StageLayout = StageLayout.Floating,
    loadedLayout: StageLayout = StageLayout.Floating,
    preview: @Composable () -> Unit = NoPreview,
    loaded: @Composable () -> Unit = {},
) {
    val navigator = LocalCatalogNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val sectionTops = remember { mutableMapOf<String, Int>() }
    var scrollRoom by remember { mutableIntStateOf(0) }

    // The `scrollTo` extra: once the page has laid out, collapse the bar and bring the section
    // to just under it. A page that scrolls gets the viewport's height again as room at its end,
    // so the target can always reach the top instead of the scroll stopping at the page's end.
    // A page that doesn't scroll gets none and still doesn't.
    LaunchedEffect(Unit) {
        val target = LaunchOptions.scrollTarget ?: return@LaunchedEffect
        snapshotFlow { scrollState.viewportSize }.first { it > 0 }
        if (scrollState.maxValue <= 0) return@LaunchedEffect
        scrollRoom = scrollState.viewportSize
        delay(400)
        val top = sectionTops[target] ?: return@LaunchedEffect
        scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
        scrollState.scrollTo(top)
    }

    // The screen root: banners and toasts the demos post show over the whole page.
    CatalogScreenHost {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AMPageHeader(
                    title = entry.title,
                    navigationIcon = {
                        IconButton(onClick = navigator::back) {
                            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
                        }
                    },
                    actions = { InspectionMenu() },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = AMTheme.Surface.page,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(padding)
                    .padding(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Text(
                    text = entry.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = StageLayout.PageGutter),
                )
                if (preview !== NoPreview) {
                    PageSection("Preview", Modifier.recordTop("preview", sectionTops)) {
                        Stage(previewLayout, preview)
                    }
                }
                PageSection("Loaded", Modifier.recordTop("loaded", sectionTops)) {
                    Stage(loadedLayout, loaded)
                }
                if (scrollRoom > 0) {
                    Spacer(Modifier.height(with(LocalDensity.current) { scrollRoom.toDp() }))
                }
            }
        }
    }
}

/** Records where a section sits in the page's scrolling column, for the `scrollTo` extra. */
private fun Modifier.recordTop(id: String, tops: MutableMap<String, Int>): Modifier =
    onPlaced { coordinates -> tops[id] = coordinates.positionInParent().y.roundToInt() }
