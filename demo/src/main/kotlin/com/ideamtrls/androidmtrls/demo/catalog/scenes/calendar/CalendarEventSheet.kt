package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMDatePicker
import com.ideamtrls.androidmtrls.AMDatePickerComponents
import com.ideamtrls.androidmtrls.AMElevation
import com.ideamtrls.androidmtrls.AMFormActions
import com.ideamtrls.androidmtrls.AMMenuPicker
import com.ideamtrls.androidmtrls.AMSection
import com.ideamtrls.androidmtrls.AMSectionStyle
import com.ideamtrls.androidmtrls.AMSheet
import com.ideamtrls.androidmtrls.AMSheetSurface
import com.ideamtrls.androidmtrls.AMTextEditor
import com.ideamtrls.androidmtrls.AMTextField
import com.ideamtrls.androidmtrls.AMTextFieldAutofocus
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToggle
import java.time.LocalDate

/**
 * The event form, in a frosted sheet like the scene's cards: "New event" (Save alone) or "Edit
 * event" (Delete and Save). It closes without asking; Save and Delete come from the model.
 */
@Composable
internal fun CalendarEventSheet(model: CalendarModel) {
    val draft = model.draft
    AMSheet(
        model.showsEventSheet,
        onDismissRequest = model::closeEventSheet,
        title = if (draft == null || draft.isNew) "New event" else "Edit event",
        surface = AMSheetSurface.Frosted,
    ) {
        if (draft != null) {
            CalendarEventForm(
                draft,
                onSave = model::save,
                onDelete = model::confirmDelete,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The form: the scene's showcase of inputs, every one in its natural place. Five flat cards under
 * their headers: Details (the title, focused on a new event, and the location), Time (All-day,
 * Starts, Ends), Repeat and alert, Calendar, Notes. The form scrolls; Save, with Delete beside it
 * on an event being edited, stays under it in reach. Save waits for a title.
 */
@Composable
private fun CalendarEventForm(
    draft: CalendarDraft,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AMTheme.tokens.spacing
    val components = if (draft.isAllDay) AMDatePickerComponents.Date else AMDatePickerComponents.DateAndTime
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            // A card of fields: each outlined field carries its own outline, so no dividers.
            AMSection("Details", style = AMSectionStyle.Card, elevation = AMElevation.None, inset = false) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    AMTextField(
                        draft.title,
                        label = "Title",
                        autofocus = if (draft.isNew) AMTextFieldAutofocus.CaretAtEnd else AMTextFieldAutofocus.Off,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    AMTextField(
                        draft.location,
                        label = "Location",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }
            }
            // All-day turns the pickers to dates alone; Ends never offers a day before Starts.
            AMSection("Time", style = AMSectionStyle.Rows, elevation = AMElevation.None, inset = false) {
                AMToggle("All-day", draft.isAllDay, onCheckedChange = { draft.isAllDay = it })
                AMDatePicker("Starts", draft.start, onValueChange = { draft.start = it }, components = components)
                AMDatePicker(
                    "Ends",
                    draft.end,
                    onValueChange = { draft.end = it },
                    components = components,
                    dateRange = draft.start.toLocalDate()..LocalDate.MAX,
                )
            }
            AMSection("Repeat and alert", style = AMSectionStyle.Rows, elevation = AMElevation.None, inset = false) {
                AMMenuPicker(
                    "Repeat",
                    options = CalendarEventRepeat.entries,
                    label = { it.title },
                    selected = draft.repeatRule,
                    onSelectedChange = { draft.repeatRule = it },
                )
                AMMenuPicker(
                    "Alert",
                    options = CalendarEventAlert.entries,
                    label = { it.title },
                    selected = draft.alert,
                    onSelectedChange = { draft.alert = it },
                )
            }
            // The event's dot takes its calendar's color.
            AMSection("Calendar", style = AMSectionStyle.Rows, elevation = AMElevation.None, inset = false) {
                AMMenuPicker(
                    "Calendar",
                    options = CalendarEventCalendar.entries,
                    label = { it.title },
                    selected = draft.calendar,
                    onSelectedChange = { draft.calendar = it },
                )
            }
            AMSection("Notes", style = AMSectionStyle.Card, elevation = AMElevation.None, inset = false) {
                AMTextEditor(draft.notes, label = "Notes", placeholder = "Directions, links, what to bring")
            }
        }
        AMFormActions(
            Modifier.padding(start = spacing.lg, end = spacing.lg, top = spacing.sm, bottom = spacing.lg),
            destructive = if (draft.isNew) {
                null
            } else {
                { AMButton("Delete", onClick = onDelete, destructive = true) }
            },
            elevation = AMElevation.None,
        ) {
            AMButton("Save", onClick = onSave, enabled = draft.canSave)
        }
    }
}
