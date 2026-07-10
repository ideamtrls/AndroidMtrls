package io.github.jmsdnns.androidmtrls.demo.scenes.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.scenes.common.LocalSceneExit
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabLift
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMFloatingActionsHost
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheet
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToast
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastItem
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import java.time.LocalDate
import java.time.YearMonth

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [CalendarScene] demo (Screens section). id
 * `scene-calendar`. Renders full-screen with no `ComponentPage` scaffold — the
 * scene owns its chrome and its Exit affordance ([LocalSceneExit]). Mirrors iOS
 * `CalendarScene` registration.
 */
val calendarSceneEntry: ComponentEntry = ComponentEntry(
    id = "scene-calendar",
    section = ComponentSection.Screens,
    title = "Calendar",
    subtitle = "A month calendar with a day's events",
    icon = Icons.Filled.CalendarMonth,
    content = { CalendarScene() },
)

// ---------------------------------------------------------------------------
// Navigation model
// ---------------------------------------------------------------------------

/** The scene's two root views, swapped instantly by plain state writes. Mirrors iOS `SceneView`. */
enum class CalendarView { Month, MonthsPicker }

/**
 * Event-sheet context: a `null` [event] presents the sheet in create mode,
 * pre-filled to [defaultDate] (the selected day); a non-null [event] opens it in
 * edit mode. A plain class (identity, not value, equality) so each presentation
 * re-seeds the sheet's draft — reopening create on the same day starts blank
 * rather than reviving a cancelled draft. Mirrors iOS `EventSheetContext`.
 */
class EventSheetContext(val event: EventData?, val defaultDate: LocalDate)

// ---------------------------------------------------------------------------
// CalendarState — the plain @Stable state holder (no ViewModel)
// ---------------------------------------------------------------------------

/**
 * The single state holder driving the whole Calendar scene — the month landing
 * view, the year-sectioned months picker, the dual create/edit event sheet, and
 * the confirmation toast. Remembered once at the scene root (no `ViewModel`),
 * mirroring the iOS `@State`-driven `CalendarScene`. Nothing is persisted.
 */
@Stable
class CalendarState {
    /** The events; seeded from [calendarSampleEvents] and mutated in place. */
    val events = mutableStateListOf<EventData>().apply { addAll(calendarSampleEvents()) }

    /** The month the landing view shows — seeded to the current month. */
    var displayedMonth by mutableStateOf(YearMonth.now())

    /** The selected day — highlighted in the grid and headlined over the events list. */
    var selectedDate by mutableStateOf(LocalDate.now())

    /** The current root view (Month landing / months picker). */
    var currentView by mutableStateOf(CalendarView.Month)

    /** The event sheet's context, or `null` while the sheet is closed. */
    var eventSheet by mutableStateOf<EventSheetContext?>(null)

    /** The confirmation toast, or `null`. */
    var toast by mutableStateOf<AMToastItem?>(null)

    // ── Derived state ─────────────────────────────────────────────────────────

    /** Events-per-day, feeding the month grid's dots. */
    val eventCounts: Map<LocalDate, Int>
        get() = events.groupingBy { it.date.toLocalDate() }.eachCount()

    /** The selected day's events, sorted by time. */
    val eventsOnSelectedDate: List<EventData>
        get() = events.filter { it.date.toLocalDate() == selectedDate }.sortedBy { it.date }

    /**
     * True while the landing view already shows the current month *and* today is
     * the selected day — greys out the FAB's Today tile. Mirrors iOS
     * `isViewingToday`.
     */
    val isViewingToday: Boolean
        get() = displayedMonth == YearMonth.now() && selectedDate == LocalDate.now()

    // ── Sheet / navigation ────────────────────────────────────────────────────

    /** Opens the event sheet in create mode, pre-filled to the selected day. */
    fun openCreateSheet() {
        eventSheet = EventSheetContext(event = null, defaultDate = selectedDate)
    }

    /** Opens the event sheet in edit mode over [event]. */
    fun openEditSheet(event: EventData) {
        eventSheet = EventSheetContext(event = event, defaultDate = selectedDate)
    }

    /** Snaps the displayed month and selected day back to today, on the month view. */
    fun goToToday() {
        displayedMonth = YearMonth.now()
        selectedDate = LocalDate.now()
        currentView = CalendarView.Month
    }

    /** Swaps in the year-sectioned months picker. */
    fun showMonthsPicker() {
        currentView = CalendarView.MonthsPicker
    }

    /**
     * Picks a month from the picker: sets the displayed month, moves the
     * selection to its 1st (so the day section reflects the month just picked),
     * and swaps back to the month view. Mirrors iOS `onPickMonth`.
     */
    fun pickMonth(month: YearMonth) {
        displayedMonth = month
        selectedDate = month.atDay(1)
        currentView = CalendarView.Month
    }

    // ── Save / delete ─────────────────────────────────────────────────────────

    /** Edit mode writes back over the matching id; create mode appends. */
    fun saveEvent(event: EventData) {
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) {
            events[index] = event
            toast = AMToastItem(text = "Event saved", icon = Icons.Filled.CheckCircle)
        } else {
            events.add(event)
            toast = AMToastItem(text = "Event added", icon = Icons.Filled.EditCalendar)
        }
        eventSheet = null
    }

    fun deleteEvent(event: EventData) {
        events.removeAll { it.id == event.id }
        eventSheet = null
        toast = AMToastItem(text = "Event deleted", icon = Icons.Filled.Delete)
    }
}

// ---------------------------------------------------------------------------
// CalendarScene — the month-calendar scene
// ---------------------------------------------------------------------------

/**
 * Calendar — a month calendar whose day taps reveal that day's events inline
 * beneath the grid, plus a year-sectioned months picker the FAB's Months tile
 * swaps in. One remembered [CalendarState] drives both root views, the dual
 * create/edit event sheet, and the toast. Ports iOS `CalendarScene`.
 *
 * **Overlay host nesting** (outermost → innermost):
 * `AMToast → AMSheet(eventSheet) → AMFloatingActionsHost`. The toast and sheet
 * hosts wrap the scene body and OR their state into `LocalAMOverlayActive`, so
 * the hub's FAB (innermost) fades out of the way under the toast, the event
 * sheet, or its own menu.
 *
 * The two root views swap instantly (plain state writes, the iOS `imInstantSwitch`
 * twin); nothing pushes, so the scene owns no panel host.
 */
@Composable
fun CalendarScene() {
    val state = remember { CalendarState() }
    val sceneExit = LocalSceneExit.current

    var showActions by remember { mutableStateOf(false) }
    val fabOffset = remember { mutableStateOf(Offset.Zero) }

    // Retain the last sheet context so the card keeps its content while sliding
    // out after the sheet clears; the context's identity keys the draft, so a
    // fresh present always re-seeds it.
    val activeContext = state.eventSheet
    val retainedContext = remember { mutableStateOf<EventSheetContext?>(null) }
    if (activeContext != null) retainedContext.value = activeContext

    // ── Host stack: toast → sheet → FAB → body ──
    AMToast(
        item = state.toast,
        onDismiss = { state.toast = null },
    ) {
        AMSheet(
            isPresented = activeContext != null,
            onDismiss = { state.eventSheet = null },
            sheetContent = {
                retainedContext.value?.let { context ->
                    EventSheetView(
                        context = context,
                        onSave = { state.saveEvent(it) },
                        onDelete = { state.deleteEvent(it) },
                    )
                }
            },
        ) {
            AMFloatingActionsHost(
                isPresented = showActions,
                onPresentChange = { showActions = it },
                bottomInset = sceneFabLift(),
                offset = fabOffset,
                accessibilityLabel = "Actions",
                menu = { FabMenu(state, sceneExit) { showActions = it } },
            ) {
                SceneBody(state)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scene body — the current root view over the shared page fill
// ---------------------------------------------------------------------------

@Composable
private fun SceneBody(state: CalendarState) {
    val isDark = LocalAMIsDarkTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AMTheme.Surface.page.resolve(isDark)),
    ) {
        when (state.currentView) {
            CalendarView.Month -> CalendarLandingView(
                month = state.displayedMonth,
                selectedDate = state.selectedDate,
                events = state.eventsOnSelectedDate,
                eventCounts = state.eventCounts,
                onSelectDay = { state.selectedDate = it },
                onSelectEvent = { state.openEditSheet(it) },
            )

            CalendarView.MonthsPicker -> CalendarMonthsPicker(
                displayedMonth = state.displayedMonth,
                onPickMonth = { state.pickMonth(it) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// FAB menu — [New Event, Today] over [Months, Exit]
// ---------------------------------------------------------------------------

/**
 * The FAB's actions grid: a `[New Event, Today]` actions row over a
 * `[Months, Exit]` nav row. Today greys out while the landing view already shows
 * today (current month + today selected); it doubles as the way back from the
 * months picker to the month view, alongside picking a month. Ports iOS
 * `CalendarScene.fabMenu`.
 */
@Composable
private fun FabMenu(
    state: CalendarState,
    sceneExit: () -> Unit,
    onPresentChange: (Boolean) -> Unit,
) {
    AMActionGrid {
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.EditCalendar,
                    title = "Add Event",
                    onClick = {
                        onPresentChange(false)
                        state.openCreateSheet()
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Filled.Today,
                    title = "Today",
                    enabled = !state.isViewingToday,
                    onClick = {
                        onPresentChange(false)
                        state.goToToday()
                    },
                )
            },
        )
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Current",
                    onClick = {
                        onPresentChange(false)
                        state.currentView = CalendarView.Month
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Filled.DateRange,
                    title = "Years",
                    onClick = {
                        onPresentChange(false)
                        state.showMonthsPicker()
                    },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Exit",
                    onClick = { sceneExit() },
                )
            },
        )
    }
}
