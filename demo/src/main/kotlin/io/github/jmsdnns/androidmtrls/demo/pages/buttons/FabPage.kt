package io.github.jmsdnns.androidmtrls.demo.pages.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFloatingActionButton
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.catalog.catalogGroup
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for [AMFloatingActionButton].
 *
 * id: "imFloatingActionButton", section: [ComponentSection.Buttons].
 */
val fabEntry: ComponentEntry = ComponentEntry(
    id = "imFloatingActionButton",
    section = ComponentSection.Buttons,
    title = "Floating Action Button",
    tileTitle = "FAB",
    subtitle = "Moveable primary-action button",
    intro = "Persistent affordance that triggers the screen's primary action. Always moveable — " +
        "quick tap fires the action, long-press lifts the button and drags it. No static mode.",
    icon = Icons.Default.AddCircle,
    content = { FabPage() },
)

// ---------------------------------------------------------------------------
// Offset saver — decomposes Offset into a List<Float> for rememberSaveable
// ---------------------------------------------------------------------------

/**
 * [listSaver] that persists an [Offset] as `[x, y]` across process death and
 * configuration changes. Used by [FabPage]'s draggable group so the drag offset
 * survives within the demo app.
 */
private val OffsetStateSaver = listSaver<MutableState<Offset>, Float>(
    save    = { listOf(it.value.x, it.value.y) },
    restore = { (x, y) -> mutableStateOf(Offset(x, y)) },
)

// ---------------------------------------------------------------------------
// FabPage
// ---------------------------------------------------------------------------

/**
 * Demo page for [AMFloatingActionButton].
 *
 * Two groups mirroring the component's two modes:
 * - **Default** — a static FAB (`offset = null`): a quick tap fires the action.
 * - **Draggable** — the opt-in offset path: a `rememberSaveable`-hoisted offset
 *   is handed to the FAB so long-press lift and drag are enabled and the dropped
 *   position survives configuration changes.
 */
@Composable
fun FabPage() {
    // ── Default (static) ───────────────────────────────────────────────────────
    catalogGroup(title = "Default") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            FabDemoZone(offset = null)
            FabCaption("Tap to fire the action.")
        }
    }

    // ── Draggable ──────────────────────────────────────────────────────────────
    // Caller-owned offset persisted across config changes and hoisted to the page
    // root so the position is not reset if the FAB is remounted.
    val offset = rememberSaveable(saver = OffsetStateSaver) {
        mutableStateOf(Offset.Zero)
    }
    catalogGroup(title = "Draggable") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.md.dp),
        ) {
            FabDemoZone(offset = offset)
            FabCaption("Tap to fire the action; long-press to lift and drag.")
        }
    }
}

// ---------------------------------------------------------------------------
// FabCaption — 12sp secondary note under a demo zone
// ---------------------------------------------------------------------------

@Composable
private fun FabCaption(text: String) {
    val isDark = LocalAMIsDarkTheme.current
    // Matches iOS .font(.caption).foregroundStyle(secondary).
    BasicText(
        text = text,
        style = TextStyle(
            color = AMTheme.Text.secondary.resolve(isDark),
            fontSize = 12.sp,
        ),
        modifier = Modifier.padding(horizontal = AMTheme.Spacing.lg.dp),
    )
}

// ---------------------------------------------------------------------------
// FabDemoZone — 160dp demo zone with FAB
// ---------------------------------------------------------------------------

/**
 * A 160dp-tall demo zone that hosts [AMFloatingActionButton] against a
 * semi-transparent rounded-rect surface, mirroring the iOS ZStack approach
 * (RoundedRectangle.fill(systemBackground.opacity(0.4))).
 *
 * @param offset `null` → static FAB; a caller-owned state → draggable FAB whose
 *   dropped position persists.
 */
@Composable
private fun FabDemoZone(offset: MutableState<Offset>?) {
    val isDark = LocalAMIsDarkTheme.current
    val surfaceColor = AMTheme.Surface.primary.resolve(isDark).copy(alpha = 0.4f)
    val shape = RoundedCornerShape(AMTheme.Radius.md.dp)

    Box(
        modifier = Modifier
            // The FAB must draw above everything else on the page (content <
            // FAB < overlay). Compose draws later siblings on top, and the
            // caption follows this zone in the column — zIndex lifts the zone
            // (and the FAB inside it) above it so a dragged FAB never slides
            // under page content.
            .zIndex(1f)
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp)
            .height(160.dp)
            .background(color = surfaceColor, shape = shape),
    ) {
        // FAB anchored to the bottom of the demo zone; it defaults to Trailing
        // placement inside its own fillMaxWidth container.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            AMFloatingActionButton(
                offset = offset,
                accessibilityLabel = "Demo",
                onClick = { /* tap registered — no-op in the demo */ },
            )
        }
    }
}
