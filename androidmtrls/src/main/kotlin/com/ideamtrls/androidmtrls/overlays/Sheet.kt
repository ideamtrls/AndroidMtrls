package com.ideamtrls.androidmtrls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** How tall an [AMSheet] grows. */
public enum class AMSheetHeight {
    /**
     * As tall as the sheet can be. Content that should take the room (a form with its button at the
     * bottom, a list that scrolls itself) gives itself a weight or `fillMaxHeight`.
     */
    Full,

    /** As tall as its content, up to the full height; taller content should scroll itself. */
    Content,
}

/** What an [AMSheet]'s surface is made of. */
public enum class AMSheetSurface {
    /** The floating surface, [AMTheme.Surface.primary]: white in light mode, black in dark mode. */
    Primary,

    /**
     * The page color, [AMTheme.Surface.page], for a sheet that holds cards (a grouped form), so
     * they stand on it as they do on a page.
     */
    Page,

    /**
     * Glass, for an app whose pages use frosted cards: the art of the [AMBackdrop] the sheet is
     * presented from, blurred under the frosted fill, with the frosted edge. What the sheet holds
     * stays solid. With no backdrop to sample, it draws [Primary].
     */
    Frosted,
}

/**
 * A sheet: Material's modal bottom sheet wearing the floating surface, over a screen that stays as
 * it is, with no dimming behind it.
 *
 * ```kotlin
 * var editing by rememberSaveable { mutableStateOf(false) }
 * AMButton("Edit profile", onClick = { editing = true })
 * AMSheet(editing, onDismissRequest = { editing = false }, title = "Edit profile") {
 *     ProfileForm(onSave = { editing = false })
 * }
 * ```
 *
 * - **Native behavior**: Material's sheet with its drag handle, shape, motion, drag to dismiss,
 *   tap outside to dismiss and predictive back. It slides in when [visible] turns true; setting
 *   [visible] false from code slides it away and never asks.
 * - **The style**: [surface] fills it with no tonal tint, the `Medium` two-layer shadow falls
 *   from its top edge, and a hairline outlines it in both modes. The screen behind isn't dimmed:
 *   the scrim is transparent, and still takes a tap outside.
 * - **The discard guard**: while [Modifier.amDiscardGuard][amDiscardGuard] inside reports changes,
 *   a drag down, a tap outside and back raise "Discard changes?" instead of closing.
 * - **A fresh context**: what the sheet holds is solid and enabled whatever surrounds the call;
 *   cards in it float as they do on a page, and on a black sheet a filled button takes the
 *   brighter [AMTheme.controlOutline] in dark mode, as inside a card.
 * - **Its own toasts**: the sheet carries an [AMToastHost], so an [AMToast] posted inside shows at
 *   the sheet's bottom, over it.
 * - **A window**: it covers the screen from wherever it's called and takes back before the screen
 *   under it. An alert, the spotlight or the remote opened while it's up sit above it.
 *
 * The content comes under the drag handle and [title], padded clear of the navigation bar and the
 * keyboard; give it its own side padding (the page gutter) or rows on the row recipe. While it's
 * up the sheet reports to the nearest scene, so the floating action button fades out of its way.
 *
 * @param visible whether the sheet is up.
 * @param onDismissRequest called once the user has closed the sheet (a drag, a tap outside, back,
 *   or Discard); set [visible] to false.
 * @param modifier the sheet's placement.
 * @param title the sheet's heading under the drag handle, in `titleLarge`, read as a heading. Null
 *   shows none. Keep it null or not for the whole time the sheet is up; its text may change.
 * @param height [AMSheetHeight.Full] (the default) or [AMSheetHeight.Content].
 * @param surface the sheet's fill: [AMSheetSurface.Primary] (the default), [AMSheetSurface.Page]
 *   or, in an app with frosted cards, [AMSheetSurface.Frosted].
 * @param content what the sheet holds, in a column under the title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AMSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    height: AMSheetHeight = AMSheetHeight.Full,
    surface: AMSheetSurface = AMSheetSurface.Primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val guard = remember { AMDiscardGuardState() }
    val confirm = remember(guard) { { value: SheetValue -> guard.confirm(value) } }
    val sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded), confirm)
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismissRequest)
    // Composed from the moment it shows until it has slid away.
    var composed by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            guard.closing = false
            // Entering, Material's sheet slides itself in; reopened while sliding away, it's ours.
            if (composed) sheetState.show() else composed = true
        } else if (composed) {
            guard.closing = true
            guard.prompting = false
            sheetState.hide()
            composed = false
        }
    }

    ReportOverlayUp(visible && composed)
    if (!composed) return

    val discard = remember(guard, sheetState, scope) {
        AMAlertItem(
            title = DiscardTitle,
            confirm = AMAlertAction(DiscardLabel, destructive = true) {
                guard.closing = true
                scope.launch {
                    sheetState.hide()
                    composed = false
                    dismiss()
                }
            },
            message = DiscardMessage,
            cancel = AMAlertAction(KeepEditingLabel),
        )
    }
    val shape = BottomSheetDefaults.ExpandedShape
    val frosted = surface == AMSheetSurface.Frosted

    ModalBottomSheet(
        onDismissRequest = {
            composed = false
            dismiss()
        },
        modifier = modifier.sheetChrome(sheetState, shape, frosted, LocalAMBackdrop.current),
        sheetState = sheetState,
        shape = shape,
        containerColor = when (surface) {
            AMSheetSurface.Primary -> AMTheme.Surface.primary
            AMSheetSurface.Page -> AMTheme.Surface.page
            // The glass is the chrome's.
            AMSheetSurface.Frosted -> Color.Transparent
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = Color.Transparent,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        // While there are changes, back reaches the guard instead.
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !guard.hasChanges),
    ) {
        val size = Modifier
            .fillMaxWidth()
            .then(if (height == AMSheetHeight.Full) Modifier.fillMaxHeight() else Modifier)
        CompositionLocalProvider(LocalAMDiscardGuard provides guard) {
            ProvideOverlayWindowLocals {
                // A black sheet lifts the controls on it the way a card does (Primary, and Frosted,
                // which is solid without art); a page-colored one leaves them as on a page.
                CompositionLocalProvider(LocalAMOnPrimarySurface provides (surface != AMSheetSurface.Page)) {
                    // The sheet is a window, so its toasts show in it, over its content.
                    AMToastHost(size) {
                        Column(size) {
                            if (title != null) SheetTitle(title)
                            content()
                        }
                    }
                }
            }
        }
        DiscardBackHandler(guard)
        AMAlert(if (guard.prompting) discard else null, onDismissRequest = { guard.prompting = false })
    }
}

/** The sheet's heading, under the drag handle at the row inset. */
@Composable
private fun SheetTitle(title: String) {
    val spacing = AMTheme.tokens.spacing
    Text(
        title,
        Modifier
            .fillMaxWidth()
            .padding(start = spacing.lg, end = spacing.lg, bottom = spacing.md)
            .semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
}
