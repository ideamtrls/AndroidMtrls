package com.ideamtrls.androidmtrls.demo

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.ideamtrls.androidmtrls.AMButton
import com.ideamtrls.androidmtrls.AMButtonPair
import com.ideamtrls.androidmtrls.AMButtonStyle
import com.ideamtrls.androidmtrls.AMCarousel
import com.ideamtrls.androidmtrls.AMDivider
import com.ideamtrls.androidmtrls.AMPageIndicator
import com.ideamtrls.androidmtrls.AMRowDefaults
import com.ideamtrls.androidmtrls.AMTheme
import com.ideamtrls.androidmtrls.AMToast
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amCard
import com.ideamtrls.androidmtrls.amCrossfade
import java.util.Locale

internal val MotionEntry = ComponentEntry(
    id = "motion",
    category = CatalogCategory.Foundations,
    title = "Motion",
    summary = "Material's standard springs, and instant swaps",
    icon = R.drawable.ic_animation,
    keywords = listOf(
        "motionScheme", "MotionScheme", "standard", "spring", "spatial", "effects", "amCrossfade",
        "amInstantSwitch", "crossfade", "animation", "Remove animations", "animator duration scale",
    ),
) { MotionPage() }

@Composable
private fun MotionPage() {
    ComponentPage(
        MotionEntry,
        previewLayout = StageLayout.FullBleed,
        preview = { MotionSpecs() },
    ) {
        MotionPanel()
    }
}

private class MotionRole(val name: String, val use: String, val spec: (MotionScheme) -> FiniteAnimationSpec<Float>)

private val MotionRoles = listOf(
    MotionRole("Default spatial", "Moves and resizes") { it.defaultSpatialSpec() },
    MotionRole("Fast spatial", "Small moves: a switch, a tab indicator") { it.fastSpatialSpec() },
    MotionRole("Slow spatial", "Large moves across the screen") { it.slowSpatialSpec() },
    MotionRole("Default effects", "Color and fades, the crossfade") { it.defaultEffectsSpec() },
    MotionRole("Fast effects", "Quick feedback on small parts") { it.fastEffectsSpec() },
    MotionRole("Slow effects", "Slow fades over large areas") { it.slowEffectsSpec() },
)

/** Each spring of the motion scheme with its values and what it moves, then whether motion plays. */
@Composable
private fun MotionSpecs() {
    val scheme = MaterialTheme.motionScheme
    val context = LocalContext.current
    val animationsRemoved = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    Column(Modifier.fillMaxWidth().background(AMTheme.Surface.primary)) {
        MotionRoles.forEach { role ->
            ListItem(
                supportingContent = { Text("${springText(role.spec(scheme))}\n${role.use}") },
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
            ) {
                Text(role.name)
            }
            AMDivider(inset = AMTheme.tokens.spacing.lg)
        }
        ListItem(
            leadingContent = { Icon(painterResource(R.drawable.ic_animation), contentDescription = null) },
            colors = AMRowDefaults.colors(),
            shapes = AMRowDefaults.shapes(),
            contentPadding = AMRowDefaults.contentPadding,
        ) {
            Text(if (animationsRemoved) "Remove animations is on: motion is instant" else "Remove animations is off: motion plays")
        }
    }
}

private fun springText(spec: FiniteAnimationSpec<Float>): String =
    if (spec is SpringSpec<Float>) {
        String.format(Locale.ROOT, "Spring, damping %.1f, stiffness %.0f", spec.dampingRatio, spec.stiffness)
    } else {
        "Tween"
    }

private val Genres = listOf("Jazz", "Folk", "Ambient", "Soul")

private val MotionTabs = listOf("Posts", "Replies")

private val MotionStories = listOf("Morning light", "Harbor walk", "Night market")

// Stand-in for your own action.
private fun save() {}

/**
 * Library and Material motion, each group with its Replay: on one card the tab indicator, a
 * crossfading sign-in row and chip selection; then a carousel paging under its dots; last a press
 * and a toast presenting itself.
 */
@Composable
private fun MotionPanel() {
    val spacing = AMTheme.tokens.spacing
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var signedIn by rememberSaveable { mutableStateOf(false) }
    var genres by remember { mutableStateOf(setOf("Jazz")) }
    var story by rememberSaveable { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<AMToastItem?>(null) }
    val copy = painterResource(R.drawable.ic_content_copy)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Column(Modifier.fillMaxWidth().amCard()) {
            SecondaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                MotionTabs.forEachIndexed { index, title ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
                Text(MotionTabs[tab], style = MaterialTheme.typography.bodyLarge)
            }
            AMDivider(inset = spacing.lg)
            ListItem(
                modifier = Modifier.amCrossfade(signedIn),
                leadingContent = { Icon(painterResource(R.drawable.ic_account_circle), contentDescription = null) },
                colors = AMRowDefaults.colors(),
                shapes = AMRowDefaults.shapes(),
                contentPadding = AMRowDefaults.contentPadding,
            ) {
                Text(if (signedIn) "Welcome back, Ada" else "Sign in to continue", style = MaterialTheme.typography.titleMedium)
            }
            AMDivider(inset = spacing.lg)
            FlowRow(
                Modifier.fillMaxWidth().padding(spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Genres.forEach { genre ->
                    FilterChip(
                        selected = genre in genres,
                        onClick = { genres = if (genre in genres) genres - genre else genres + genre },
                        label = { Text(genre) },
                    )
                }
            }
        }
        AMButton(
            "Replay",
            onClick = {
                tab = 1 - tab
                signedIn = !signedIn
                genres = if ("Jazz" in genres) setOf("Folk", "Soul") else setOf("Jazz")
            },
            modifier = Modifier.fillMaxWidth(),
            style = AMButtonStyle.Secondary(),
        )
        AMCarousel(
            MotionStories,
            currentIndex = story,
            onCurrentIndexChange = { story = it },
            // Edge to edge, so the cards' shadows aren't cut at the gutters; the cards stand where they would.
            modifier = Modifier.widenBy(StageLayout.PageGutter).fillMaxWidth().height(120.dp),
            sideInset = StageLayout.PageGutter + spacing.lg,
        ) { title ->
            Box(
                Modifier.fillMaxWidth().heightIn(min = 88.dp).amCard(),
                contentAlignment = Alignment.Center,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
        }
        AMPageIndicator(count = MotionStories.size, currentIndex = story, modifier = Modifier.align(Alignment.CenterHorizontally))
        AMButton(
            "Replay",
            onClick = { story = (story + 1) % MotionStories.size },
            modifier = Modifier.fillMaxWidth(),
            style = AMButtonStyle.Secondary(),
        )
        AMButtonPair(
            { AMButton("Press and hold", onClick = ::save, style = AMButtonStyle.Primary()) },
            {
                AMButton(
                    "Replay",
                    onClick = { toast = AMToastItem("Copied to clipboard", icon = copy) },
                    style = AMButtonStyle.Secondary(),
                )
            },
        )
    }
    AMToast(toast, onDismissRequest = { toast = null })
}

/** Lays the content out [amount] wider on each side than it's given, reaching past its parent's inset. */
private fun Modifier.widenBy(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * extra))
    layout(placeable.width - 2 * extra, placeable.height) { placeable.placeRelative(-extra, 0) }
}
