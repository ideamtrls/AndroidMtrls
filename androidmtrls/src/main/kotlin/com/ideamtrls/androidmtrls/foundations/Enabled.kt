package com.ideamtrls.androidmtrls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

/**
 * Whether the containers around a control let it be enabled. True at the root; a container that
 * disables everything inside it ([AMFormActions] with `enabled = false`) sets it to false through
 * [ProvideAMEnabled], and every AM control reads it through [resolveEnabled].
 *
 * Internal: an app disables a control through its own `enabled`, or a library container through
 * the container's.
 */
internal val LocalAMEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

/**
 * Whether a control is enabled: its own [enabled] and every container's around it. Every AM control
 * passes its `enabled` parameter through this, so a disabled container greys and stops its children
 * without the caller touching each one.
 */
@Composable
@ReadOnlyComposable
internal fun resolveEnabled(enabled: Boolean): Boolean = enabled && LocalAMEnabled.current

/**
 * Disables the AM controls in [content] when [enabled] is false. Containers combine: inside a
 * disabled container, an inner container can't enable its children again.
 */
@Composable
internal fun ProvideAMEnabled(enabled: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAMEnabled provides resolveEnabled(enabled), content = content)
}
