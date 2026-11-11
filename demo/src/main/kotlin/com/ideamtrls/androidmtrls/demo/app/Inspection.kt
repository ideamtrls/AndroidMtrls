package com.ideamtrls.androidmtrls.demo

import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToolbarButton
import kotlin.math.roundToInt

// App-wide inspection settings: appearance, text size, layout direction. Persisted in
// SharedPreferences, edited from `InspectionMenu` (in the list's and every page's top app bar) and
// the scenes' `SceneAppearanceButton`, and applied once at the root by `InspectionRoot`. Only those,
// and the activity for its bar icons, read them, so a change re-renders those and whatever depends
// on the theme, the density or the layout direction, never the pages themselves.

internal enum class InspectionAppearance(val title: String, @param:DrawableRes val icon: Int) {
    System("System", R.drawable.ic_contrast),
    Light("Light", R.drawable.ic_light_mode),
    Dark("Dark", R.drawable.ic_dark_mode),
}

/** System, or one of the font scales the system's own font size setting offers. */
internal enum class InspectionTextSize(val title: String, val fontScale: Float?) {
    System("System", null),
    Scale85("85%", 0.85f),
    Scale100("100% (default)", 1f),
    Scale115("115%", 1.15f),
    Scale130("130%", 1.3f),
    Scale150("150%", 1.5f),
    Scale180("180%", 1.8f),
    Scale200("200%", 2f),
}

internal enum class InspectionLayoutDirection(val title: String, val layoutDirection: LayoutDirection) {
    Ltr("Left to right", LayoutDirection.Ltr),
    Rtl("Right to left", LayoutDirection.Rtl),
}

/**
 * The inspection settings as Compose state, stored in [prefs] as they change.
 *
 * A launch extra ([LaunchOptions]) pins its setting for that launch: the `applied` value stays
 * pinned while the stored one still takes what the menu writes.
 */
@Stable
internal class Inspection(private val prefs: SharedPreferences) {
    private var storedAppearance by mutableStateOf(load(KeyAppearance, InspectionAppearance.System))
    private var storedTextSize by mutableStateOf(load(KeyTextSize, InspectionTextSize.System))
    private var storedLayoutDirection by mutableStateOf(load(KeyLayoutDirection, InspectionLayoutDirection.Ltr))

    var appearance: InspectionAppearance
        get() = storedAppearance
        set(value) {
            storedAppearance = value
            prefs.edit { putString(KeyAppearance, value.name) }
        }

    var textSize: InspectionTextSize
        get() = storedTextSize
        set(value) {
            storedTextSize = value
            prefs.edit { putString(KeyTextSize, value.name) }
        }

    var layoutDirection: InspectionLayoutDirection
        get() = storedLayoutDirection
        set(value) {
            storedLayoutDirection = value
            prefs.edit { putString(KeyLayoutDirection, value.name) }
        }

    val appliedAppearance: InspectionAppearance
        get() = LaunchOptions.appearance ?: appearance

    /** The font scale to apply, or null to keep the system's. */
    val appliedFontScale: Float?
        get() = LaunchOptions.fontScale ?: textSize.fontScale

    val appliedLayoutDirection: InspectionLayoutDirection
        get() = when (LaunchOptions.rtl) {
            true -> InspectionLayoutDirection.Rtl
            false -> InspectionLayoutDirection.Ltr
            null -> layoutDirection
        }

    val isDefault: Boolean
        get() = appliedAppearance == InspectionAppearance.System &&
            appliedFontScale == null &&
            appliedLayoutDirection == InspectionLayoutDirection.Ltr

    fun reset() {
        appearance = InspectionAppearance.System
        textSize = InspectionTextSize.System
        layoutDirection = InspectionLayoutDirection.Ltr
    }

    private inline fun <reified T : Enum<T>> load(key: String, default: T): T =
        prefs.getString(key, null)?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        const val KeyAppearance = "appearance"
        const val KeyTextSize = "textSize"
        const val KeyLayoutDirection = "layoutDirection"
    }
}

/** Whether the applied appearance is dark: the system's for System. */
@Composable
internal fun Inspection.isDark(): Boolean = when (appliedAppearance) {
    InspectionAppearance.System -> isSystemInDarkTheme()
    InspectionAppearance.Light -> false
    InspectionAppearance.Dark -> true
}

/** The [Inspection] that [InspectionRoot] installs, for [InspectionMenu]. */
internal val LocalInspection = staticCompositionLocalOf<Inspection> { error("InspectionRoot provides the inspection settings") }

/**
 * Applies the inspection settings to everything below it: the appearance through [AMTheme]'s dark
 * flag (which every style color follows), the text size through [LocalDensity]'s font scale (with the platform's nonlinear scaling),
 * and the layout direction through [LocalLayoutDirection]. Attach it once, at the root. The tree
 * below keeps its shape whatever the settings, so changing one never resets the screen's state.
 */
@Composable
internal fun InspectionRoot(inspection: Inspection, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val systemDensity = LocalDensity.current
    val fontScale = inspection.appliedFontScale
    val density = remember(fontScale, configuration, systemDensity) {
        if (fontScale == null) {
            systemDensity
        } else {
            val scaled = Configuration(configuration).apply { this.fontScale = fontScale }
            Density(context.createConfigurationContext(scaled))
        }
    }
    AMTheme(darkTheme = inspection.isDark()) {
        CompositionLocalProvider(
            LocalInspection provides inspection,
            LocalDensity provides density,
            LocalLayoutDirection provides inspection.appliedLayoutDirection.layoutDirection,
            content = content,
        )
    }
}

private enum class InspectionMenuPage { Main, TextSize, LayoutDirection }

/**
 * The top app bar action that edits the inspection settings: an eye, filled while anything is off
 * its default. Appearance picks inline; text size and layout direction open their own lists in
 * the same menu. The list's top app bar and every page's carry it ([ComponentPage] does this for
 * you); the scenes carry [SceneAppearanceButton] instead.
 */
@Composable
internal fun InspectionMenu() {
    val inspection = LocalInspection.current
    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf(InspectionMenuPage.Main) }
    val close = { expanded = false }

    Box {
        IconButton(onClick = { page = InspectionMenuPage.Main; expanded = true }) {
            Icon(
                painter = painterResource(if (inspection.isDefault) R.drawable.ic_visibility else R.drawable.ic_visibility_filled),
                contentDescription = "Inspection settings",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = close) {
            when (page) {
                InspectionMenuPage.Main -> {
                    MenuLabel("Appearance")
                    InspectionAppearance.entries.forEach { option ->
                        OptionItem(
                            title = option.title,
                            icon = option.icon,
                            selected = option == inspection.appliedAppearance,
                            onClick = { inspection.appearance = option; close() },
                        )
                    }
                    HorizontalDivider()
                    SubmenuItem(
                        title = "Text size",
                        icon = R.drawable.ic_format_size,
                        value = textSizeTitle(inspection.appliedFontScale),
                        onClick = { page = InspectionMenuPage.TextSize },
                    )
                    SubmenuItem(
                        title = "Layout direction",
                        icon = R.drawable.ic_swap_horiz,
                        value = inspection.appliedLayoutDirection.title,
                        onClick = { page = InspectionMenuPage.LayoutDirection },
                    )
                    if (!inspection.isDefault) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Reset") },
                            leadingIcon = { Icon(painterResource(R.drawable.ic_restart_alt), contentDescription = null) },
                            onClick = { inspection.reset(); close() },
                        )
                    }
                }
                InspectionMenuPage.TextSize -> {
                    BackItem("Text size") { page = InspectionMenuPage.Main }
                    InspectionTextSize.entries.forEach { option ->
                        OptionItem(
                            title = option.title,
                            selected = option.fontScale == inspection.appliedFontScale,
                            onClick = { inspection.textSize = option; close() },
                        )
                    }
                }
                InspectionMenuPage.LayoutDirection -> {
                    BackItem("Layout direction") { page = InspectionMenuPage.Main }
                    InspectionLayoutDirection.entries.forEach { option ->
                        OptionItem(
                            title = option.title,
                            selected = option == inspection.appliedLayoutDirection,
                            onClick = { inspection.layoutDirection = option; close() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * The light/dark chip a Showcase scene's landing view carries at the trailing end of its header:
 * a toolbar button that flips the shared appearance setting away from what is on screen. From
 * System too it writes Light or Dark, so the flip sticks. Its glyph is the appearance it switches
 * to (`light_mode` or `dark_mode`), and it's read as "Switch to light mode" or "Switch to dark
 * mode". It writes the same stored setting as [InspectionMenu], so a launch-pinned appearance keeps
 * the screen as it is, as it does for the menu.
 */
@Composable
internal fun SceneAppearanceButton() {
    val inspection = LocalInspection.current
    val target = if (inspection.isDark()) InspectionAppearance.Light else InspectionAppearance.Dark
    AMToolbarButton(
        painterResource(target.icon),
        contentDescription = if (target == InspectionAppearance.Light) "Switch to light mode" else "Switch to dark mode",
        onClick = { inspection.appearance = target },
    )
}

private fun textSizeTitle(fontScale: Float?): String =
    InspectionTextSize.entries.firstOrNull { it.fontScale == fontScale }?.title
        ?: "${((fontScale ?: 1f) * 100).roundToInt()}%"

@Composable
private fun MenuLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun OptionItem(title: String, selected: Boolean, onClick: () -> Unit, @DrawableRes icon: Int? = null) {
    DropdownMenuItem(
        text = { Text(title) },
        onClick = onClick,
        leadingIcon = icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        trailingIcon = if (selected) {
            { Icon(painterResource(R.drawable.ic_check), contentDescription = "Selected") }
        } else {
            null
        },
    )
}

@Composable
private fun SubmenuItem(title: String, @DrawableRes icon: Int, value: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(title) },
        onClick = onClick,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null)
            }
        },
    )
}

@Composable
private fun BackItem(title: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(title, style = MaterialTheme.typography.labelLarge) },
        onClick = onClick,
        leadingIcon = { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back") },
    )
    HorizontalDivider()
}
