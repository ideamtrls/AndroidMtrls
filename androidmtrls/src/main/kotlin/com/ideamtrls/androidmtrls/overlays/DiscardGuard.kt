package com.ideamtrls.androidmtrls

import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.WindowInsets as PlatformWindowInsets

/** The discard prompt's words. */
internal const val DiscardTitle: String = "Discard changes?"
internal const val DiscardMessage: String = "Your changes won't be saved."
internal const val DiscardLabel: String = "Discard"
internal const val KeepEditingLabel: String = "Keep editing"

/**
 * Asks before the sheet this sits in closes with unsaved edits: while [hasChanges] is true, a drag
 * down, a tap outside the sheet and back leave the sheet up and raise "Discard changes?" (Keep
 * editing, or Discard, which closes the sheet). Closing the sheet from code never asks.
 *
 * ```kotlin
 * AMSheet(editing, onDismissRequest = { editing = false }, title = "Edit profile") {
 *     val name = rememberTextFieldState(profile.name)
 *     Column(Modifier.amDiscardGuard(hasChanges = name.text.toString() != profile.name)) {
 *         OutlinedTextField(name, label = { Text("Display name") })
 *         AMButton("Save", onClick = { save(name.text.toString()); editing = false })
 *     }
 * }
 * ```
 *
 * Put it on any element of an [AMSheet]'s content. It reports to the nearest sheet only: a sheet
 * inside another's content guards itself, and outside a sheet it does nothing. Several guards in
 * one sheet ask while any of them has changes.
 *
 * With the keyboard up, the first back only hides the keyboard; the next one asks. A dirty sheet
 * doesn't shrink under a predictive back gesture; the prompt comes when the gesture completes.
 *
 * @param hasChanges whether the content holds edits that closing would lose.
 */
public fun Modifier.amDiscardGuard(hasChanges: Boolean): Modifier = this then DiscardGuardElement(hasChanges)

/**
 * A sheet's discard guard, the nearest-owner registry its content reports edits to. Each
 * [Modifier.amDiscardGuard][amDiscardGuard] reports under its own key, and the sheet asks while
 * any of them has changes.
 */
@Stable
internal class AMDiscardGuardState {
    private val reports = mutableStateMapOf<Any, Boolean>()

    /** Whether any guard in the sheet reports edits. Snapshot-observable. */
    val hasChanges: Boolean get() = reports.values.any { it }

    /** Whether "Discard changes?" is up. */
    var prompting by mutableStateOf(false)

    /** Set while the sheet closes from code or after Discard, when hiding never asks. */
    var closing: Boolean = false

    fun report(key: Any, hasChanges: Boolean) {
        if (reports[key] != hasChanges) reports[key] = hasChanges
    }

    fun remove(key: Any) {
        reports.remove(key)
    }

    /**
     * The sheet state's `confirmValueChange`, which Material consults before a drag or a tap
     * outside hides the sheet: while there are changes it refuses, so the sheet settles back, and
     * raises the prompt.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    fun confirm(value: SheetValue): Boolean {
        if (value != SheetValue.Hidden || closing || !hasChanges) return true
        prompting = true
        return false
    }
}

/** The discard guard of the sheet whose content this is, or null outside a sheet. */
internal val LocalAMDiscardGuard = staticCompositionLocalOf<AMDiscardGuardState?> { null }

/**
 * Back while the sheet has changes: raises the prompt instead of closing. Composed inside the
 * sheet's window, so it registers with that window's back dispatcher, after the sheet's own
 * handling (which the sheet turns off while there are changes).
 *
 * While the keyboard is up it stands aside, so the keyboard's own back handling hides the keyboard
 * first; if a back still reaches it with the keyboard up, it hides the keyboard and doesn't ask.
 */
@Composable
internal fun DiscardBackHandler(guard: AMDiscardGuardState) {
    val view = LocalView.current
    val keyboardUp = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val active = guard.hasChanges && !keyboardUp
    DisposableEffect(view, guard, active) {
        val dispatcher = if (active) view.findOnBackInvokedDispatcher() else null
        val callback = OnBackInvokedCallback {
            val ime = PlatformWindowInsets.Type.ime()
            if (view.rootWindowInsets?.isVisible(ime) == true) {
                view.windowInsetsController?.hide(ime)
            } else {
                guard.prompting = true
            }
        }
        dispatcher?.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
        onDispose { dispatcher?.unregisterOnBackInvokedCallback(callback) }
    }
}

private data class DiscardGuardElement(val hasChanges: Boolean) : ModifierNodeElement<DiscardGuardNode>() {
    override fun create() = DiscardGuardNode(hasChanges)

    override fun update(node: DiscardGuardNode) = node.update(hasChanges)

    override fun InspectorInfo.inspectableProperties() {
        name = "amDiscardGuard"
        properties["hasChanges"] = hasChanges
    }
}

/** Reports to the nearest sheet's guard under its own identity while it's attached. */
private class DiscardGuardNode(private var hasChanges: Boolean) : Modifier.Node(), CompositionLocalConsumerModifierNode {
    private var guard: AMDiscardGuardState? = null

    override fun onAttach() {
        guard = currentValueOf(LocalAMDiscardGuard)
        guard?.report(this, hasChanges)
    }

    override fun onDetach() {
        guard?.remove(this)
        guard = null
    }

    fun update(hasChanges: Boolean) {
        this.hasChanges = hasChanges
        guard?.report(this, hasChanges)
    }
}
