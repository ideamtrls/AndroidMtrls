package io.github.jmsdnns.androidmtrls.demo.scenes.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.controls.buttons.AMFormActions
import io.github.jmsdnns.androidmtrls.controls.inputs.AMFieldLabel
import io.github.jmsdnns.androidmtrls.controls.inputs.AMTextField
import io.github.jmsdnns.androidmtrls.controls.pickers.AMDateComponents
import io.github.jmsdnns.androidmtrls.controls.pickers.AMDatePickerRow
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import java.time.Instant
import java.time.ZoneId

// ---------------------------------------------------------------------------
// EventSheetView — dual create/edit event editor
// ---------------------------------------------------------------------------

/**
 * Dual-mode event editor: create (empty draft pre-filled to 9:00 on the selected
 * day, Save appends, no Delete) and edit (Save writes back, destructive Delete
 * removes). The draft is seeded from [context] once per presentation — the
 * context's identity keys the [remember], so a fresh present starts a fresh
 * draft.
 *
 * Non-self-labeling anatomy: an [AMFieldLabel] micro-label sits above each
 * [AMTextField] (title, location) — the Mtrls labeling pattern; fields carry
 * no label of their own. The [AMDatePickerRow] labels itself ("When"). Ports
 * iOS `EventSheetView`.
 */
@Composable
fun EventSheetView(
    context: EventSheetContext,
    onSave: (EventData) -> Unit,
    onDelete: (EventData) -> Unit,
) {
    val isDark = LocalAMIsDarkTheme.current
    val isNew = context.event == null

    // Seed the draft once per presentation (context identity is the key). Create
    // mode pre-fills 9:00 on the selected day.
    var draft by remember(context) {
        mutableStateOf(
            context.event ?: EventData(
                title = "",
                date = context.defaultDate.atTime(9, 0),
                location = "",
            ),
        )
    }

    val canSave = draft.title.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AMTheme.Spacing.lg.dp),
        verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.lg.dp),
    ) {
        BasicText(
            text = if (isNew) "New Event" else "Edit Event",
            style = AMTheme.Type.title.toTextStyle(AMTheme.Text.primary.resolve(isDark)),
            modifier = Modifier.padding(top = AMTheme.Spacing.sm.dp),
        )

        // Title — micro-label over an unlabeled field.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Title")
            AMTextField(
                value = draft.title,
                onValueChange = { draft = draft.copy(title = it) },
                placeholder = "Title",
            )
        }

        // When — the date picker labels itself.
        AMDatePickerRow(
            label = "When",
            selection = draft.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            onSelectionChange = { millis ->
                // Instant → zoned → LocalDateTime is API 26-safe; LocalDateTime.ofInstant
                // is API 34+ and would crash on minSdk 31 (no core-library desugaring).
                draft = draft.copy(
                    date = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
                )
            },
            displayedComponents = AMDateComponents.DateAndTime,
        )

        // Location — micro-label over an unlabeled field.
        Column(verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.xs.dp)) {
            AMFieldLabel("Location")
            AMTextField(
                value = draft.location,
                onValueChange = { draft = draft.copy(location = it) },
                placeholder = "Location",
            )
        }

        // Create mode is Save-only (gated on a non-blank title); edit mode adds an
        // equal-width destructive Delete beside it.
        if (isNew) {
            AMFormActions(
                primaryLabel = "Save",
                primaryEnabled = canSave,
                onPrimary = { onSave(draft) },
            )
        } else {
            AMFormActions(
                destructiveLabel = "Delete",
                primaryLabel = "Save",
                primaryEnabled = canSave,
                onDestructive = { onDelete(draft) },
                onPrimary = { onSave(draft) },
            )
        }
    }
}
