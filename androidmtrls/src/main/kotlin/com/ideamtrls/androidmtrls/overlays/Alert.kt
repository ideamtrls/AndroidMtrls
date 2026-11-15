package com.ideamtrls.androidmtrls

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse

/**
 * One button of an [AMAlertItem]: its label, whether it removes something, and what it does.
 *
 * ```kotlin
 * AMAlertAction("Delete", destructive = true) { deleteProject() }
 * AMAlertAction("Keep")
 * ```
 *
 * @property label the button's text, in sentence case.
 * @property destructive whether the action removes something: its label takes
 *   [AMSemantic.Destructive]. Dismissing an alert never runs a destructive action.
 * @property onClick what the button does, run on the main thread from the alert's own button,
 *   before the alert asks to close. The default does nothing, for a cancel that only closes.
 */
@Immutable
public class AMAlertAction(
    public val label: String,
    public val destructive: Boolean = false,
    public val onClick: () -> Unit = {},
)

/**
 * What an [AMAlert] asks: a title, an optional message, the confirming action and an optional
 * cancel. Two buttons ask a question; [confirm] alone acknowledges something.
 *
 * ```kotlin
 * AMAlertItem(
 *     "Delete project?",
 *     confirm = AMAlertAction("Delete", destructive = true) { status = "Deleted" },
 *     message = "This can't be undone.",
 *     cancel = AMAlertAction("Keep"),
 * )
 * ```
 *
 * Items carry actions, so two items are never equal unless they're the same item.
 *
 * @property title the question or statement, in sentence case.
 * @property confirm the action the alert is about, at the end of the button row.
 * @property message supporting text under the title, or null for none.
 * @property cancel the way out, before [confirm]; back and a tap outside run it too. Null for an
 *   alert that only acknowledges.
 */
@Immutable
public class AMAlertItem(
    public val title: String,
    public val confirm: AMAlertAction,
    public val message: String? = null,
    public val cancel: AMAlertAction? = null,
)

/**
 * An alert: Material's `AlertDialog` asking [item]'s question, up while [item] isn't null.
 *
 * ```kotlin
 * var alert by remember { mutableStateOf<AMAlertItem?>(null) }
 * AMButton("Delete project", onClick = {
 *     alert = AMAlertItem(
 *         "Delete project?",
 *         confirm = AMAlertAction("Delete", destructive = true) { deleteProject() },
 *         message = "This can't be undone.",
 *         cancel = AMAlertAction("Keep"),
 *     )
 * }, destructive = true)
 * AMAlert(alert, onDismissRequest = { alert = null })
 * ```
 *
 * It keeps Android's shape, motion and dim: Material's dialog, its title and message in their own
 * colors, and text buttons with the cancel before the confirm; a destructive action's label takes
 * [AMSemantic.Destructive]. Its container follows the app's surface setting
 * ([AMTheme.surfaceFill]): Material's own in a solid app; in a frosted one frost over the page
 * color, opaque, since the dialog is a window of its own with no art to sample, wherever it's
 * opened from, outlined by the frosted edge ([AMTheme.Frosted.edge]) as other frosted surfaces
 * are, so its shape holds against the dimmed screen in dark mode.
 *
 * - **A button** runs its action, then calls [onDismissRequest].
 * - **Back and a tap outside** dismiss it: they run the cancel action when there is one, then call
 *   [onDismissRequest]. Dismissing never runs [AMAlertItem.confirm], so it never runs a
 *   destructive action.
 * - **It's a window**, the topmost while it's up, so it takes back before a spotlight, the remote
 *   or a sheet below it.
 *
 * Clear the item in [onDismissRequest]. While an alert is up it reports to the nearest scene, so
 * the floating action button fades out of its way.
 *
 * @param item what to ask, or null for no alert.
 * @param onDismissRequest called after a button's action, and on back or a tap outside; set
 *   [item] to null.
 * @param modifier the dialog's placement.
 */
@Composable
public fun AMAlert(item: AMAlertItem?, onDismissRequest: () -> Unit, modifier: Modifier = Modifier) {
    ReportOverlayUp(item != null)
    if (item == null) return
    // The dialog is a window of its own: it starts from a window's fresh context wherever it's
    // opened from (a sheet, the remote, the spotlight), lying on the page with no art to sample.
    ProvideOverlayWindowLocals {
        val container = frostedContainerColor()
        val shape = AlertDialogDefaults.shape
        AlertDialog(
            onDismissRequest = {
                item.cancel?.onClick?.invoke()
                onDismissRequest()
            },
            confirmButton = { AlertButton(item.confirm, onDismissRequest) },
            // Frosted, it's outlined by the frosted edge; solid, it's Material's dialog as it is.
            modifier = if (container.isSpecified) modifier.amHairline(shape, AMTheme.Frosted.edge) else modifier,
            dismissButton = item.cancel?.let { cancel -> { AlertButton(cancel, onDismissRequest) } },
            title = { Text(item.title) },
            text = item.message?.let { message -> { Text(message) } },
            shape = shape,
            containerColor = container.takeOrElse { AlertDialogDefaults.containerColor },
        )
    }
}

/** One of the alert's text buttons: runs [action], then asks the alert to close. */
@Composable
private fun AlertButton(action: AMAlertAction, onDismissRequest: () -> Unit) {
    TextButton(
        onClick = {
            action.onClick()
            onDismissRequest()
        },
        colors = if (action.destructive) {
            ButtonDefaults.textButtonColors(contentColor = AMSemantic.Destructive.color)
        } else {
            ButtonDefaults.textButtonColors()
        },
    ) {
        Text(action.label)
    }
}
