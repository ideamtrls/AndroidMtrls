package com.ideamtrls.androidmtrls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * One banner: a short status message at the top of its window, led by its role's glyph.
 *
 * Each item is its own showing. Items compare by identity, so posting a new item, even one with the
 * same text, shows it afresh and starts its time again.
 *
 * ```kotlin
 * AMBannerItem("Changes saved", message = "Your profile is up to date.", style = AMSemantic.Success)
 * ```
 *
 * @property title what happened, in sentence case: the first line, in `bodyLarge`.
 * @property message an optional second line, in `bodyMedium` and `onSurfaceVariant`.
 * @property style the status role. Its glyph leads the banner in the role's color
 *   ([AMSemantic.icon], [AMSemantic.color]), and a screen reader speaks the role before the title
 *   ("Success", "Warning", "Error"); an [AMSemantic.Info] banner reads its text alone.
 * @property duration how long the banner stays up once it shows: 4 seconds by default, longer when
 *   the user has asked Android for more time to act (Accessibility, Time to take action).
 */
@Immutable
public class AMBannerItem(
    public val title: String,
    public val message: String? = null,
    public val style: AMSemantic = AMSemantic.Info,
    public val duration: Duration = 4.seconds,
)

/**
 * Shows [item] as a banner at the top of the window while it's non-null: a floating surface (white
 * in light mode, black in dark mode, the two-layer shadow and a hairline) just under the status
 * bar, over a fade of the surface color that keeps the status bar legible.
 *
 * It draws nothing where it's called: the nearest [AMBannerHost], at the root of the window, shows
 * it, so a banner posted from deep inside a screen still spans the screen. Without a host above it,
 * it fails at once.
 *
 * ```kotlin
 * var banner by remember { mutableStateOf<AMBannerItem?>(null) }
 * AMButton("Save", onClick = { save(); banner = AMBannerItem("Changes saved", style = AMSemantic.Success) })
 * AMBanner(banner, onDismissRequest = { banner = null })
 * ```
 *
 * The state is the caller's. The banner asks to go through [onDismissRequest] when its time is up
 * (the item's `duration`, stretched to the user's accessibility timeout), on a swipe up, on a tap,
 * or on a screen reader's dismiss action; set the item back to null there.
 *
 * It slides down from the top with Material's motion and goes back up the same way. A screen
 * reader hears it once, politely, as one element: the role, the title, then the message. It isn't
 * a modal window: what's behind it stays reachable.
 *
 * A host shows one banner at a time: the most recently posted one.
 *
 * @param item the banner to show, or null for none.
 * @param onDismissRequest called when the banner asks to go.
 * @param onClick makes the banner a button. A tap asks to dismiss first, then calls it, so the
 *   action can post a follow-up banner. A swipe and the timer never call it. Without it, a tap only
 *   dismisses.
 */
@Composable
public fun AMBanner(
    item: AMBannerItem?,
    onDismissRequest: () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    PostTransient(LocalAMBannerHost.current, item, onDismissRequest, onClick)
}

/**
 * A window's banner layer: draws [content], and over its top edge the banner any [AMBanner] inside
 * it posts, below the status bar and inside the display cutout's safe area.
 *
 * Put one at the root of each window that shows banners (a screen, a window of the app's own),
 * filling it, so its banners reach the top of that window. The content is laid out as the host
 * itself is; the banner takes no space from it and lets touches outside the card through.
 * [AMScene] carries one already.
 *
 * ```kotlin
 * AMBannerHost(Modifier.fillMaxSize()) { Screen() }
 * ```
 *
 * @param modifier the host's size and placement: usually the whole window.
 * @param content the window's content.
 */
@Composable
public fun AMBannerHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val host = remember { TransientHost<AMBannerItem>() }
    val posting = host.current
    val accessibility = LocalAccessibilityManager.current

    LaunchedEffect(posting) {
        val shown = posting ?: return@LaunchedEffect
        delay(accessibility.transientTimeout(shown.item.duration, containsIcons = true, shown.caller.actionable))
        shown.caller.onDismissRequest()
    }

    Box(modifier, propagateMinConstraints = true) {
        CompositionLocalProvider(LocalAMBannerHost provides host, content = content)
        BannerLayer(posting, Modifier.matchParentSize())
    }
}

/** The nearest [AMBannerHost]. Reading it with no host above fails. */
internal val LocalAMBannerHost = staticCompositionLocalOf<TransientHost<AMBannerItem>> {
    error("AMBanner shows in the nearest AMBannerHost: put one at the root of the window")
}

/** The fade of the surface color behind a banner, from the window's top edge. */
private val ScrimHeight = 220.dp

/** Where the scrim starts fading from the solid surface color toward clear. */
private const val ScrimFadeStart = 0.2f

/** The gap between the title and the message, so the two lines read as one statement. */
private val TextSpacing = 2.dp

/** How far up a drag has to carry the banner to dismiss it. */
private val SwipeThreshold = 30.dp

/** An upward fling at least this fast dismisses the banner, however short the drag. */
private val FlingVelocity = 125.dp

/**
 * The scrim and the banner over a host: the scrim fades, the card slides from the top. The card
 * changes in place when the same caller posts a new item, and slides out and in between callers.
 */
@Composable
private fun BannerLayer(posting: TransientPosting<AMBannerItem>?, modifier: Modifier) {
    val motion = MaterialTheme.motionScheme
    val surface = AMTheme.Surface.primary
    val spacing = AMTheme.tokens.spacing
    Box(modifier) {
        AnimatedVisibility(
            visible = posting != null,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            enter = fadeIn(motion.defaultEffectsSpec()),
            exit = fadeOut(motion.fastEffectsSpec()),
            label = "AMBanner scrim",
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(ScrimHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to surface,
                            ScrimFadeStart to surface,
                            1f to surface.copy(alpha = 0f),
                        ),
                    ),
            )
        }
        AnimatedContent(
            targetState = posting,
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            transitionSpec = {
                val enter = slideInVertically(motion.defaultSpatialSpec()) { -it } +
                    fadeIn(motion.defaultEffectsSpec())
                val exit = slideOutVertically(motion.fastSpatialSpec()) { -it } +
                    fadeOut(motion.fastEffectsSpec())
                (enter togetherWith exit).using(null)
            },
            contentAlignment = Alignment.TopCenter,
            label = "AMBanner",
            contentKey = { it?.caller },
        ) { shown ->
            if (shown != null) {
                BannerCard(
                    shown,
                    Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(top = spacing.sm, start = spacing.lg, end = spacing.lg),
                )
            }
        }
    }
}

/**
 * The banner itself: the role's glyph, then the title over the message, on the floating surface
 * with a hairline in both modes. It follows a drag up 1:1 and dismisses past [SwipeThreshold] (or
 * on a quick fling up); a shorter drag settles back.
 */
@Composable
private fun BannerCard(posting: TransientPosting<AMBannerItem>, modifier: Modifier) {
    val item = posting.item
    val caller = posting.caller
    val spacing = AMTheme.tokens.spacing
    val shape = MaterialTheme.shapes.large
    val motion = MaterialTheme.motionScheme
    val density = LocalDensity.current
    val threshold = with(density) { SwipeThreshold.toPx() }
    val fling = with(density) { FlingVelocity.toPx() }
    var drag by remember(posting) { mutableFloatStateOf(0f) }
    val close = { caller.onDismissRequest() }
    val tap = {
        caller.onDismissRequest()
        caller.onClick?.invoke()
        Unit
    }

    Row(
        modifier
            .fillMaxWidth()
            .offset { IntOffset(0, drag.roundToInt()) }
            .draggable(
                state = rememberDraggableState { delta -> drag = (drag + delta).coerceAtMost(0f) },
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (drag < -threshold || velocity < -fling) {
                        // The removal slides on from where the finger left the card.
                        close()
                    } else {
                        animate(drag, 0f, velocity, motion.fastSpatialSpec()) { value, _ -> drag = value }
                    }
                },
            )
            // The hairline below is drawn in both modes, so the surface's own dark-mode one is off.
            .amElevatedSurface(shape, hairline = Color.Transparent)
            .amHairline(shape)
            .clip(shape)
            .then(
                if (caller.actionable) {
                    Modifier.clickable(role = Role.Button, onClick = tap)
                } else {
                    Modifier.pointerInput(caller) { detectTapGestures(onTap = { close() }) }
                },
            )
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                dismiss {
                    close()
                    true
                }
            }
            .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(item.style.icon, contentDescription = item.style.severityLabel, tint = item.style.color)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TextSpacing)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (item.message != null) {
                Text(item.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// The plumbing banners and toasts share: callers post their current item to the nearest host of
// their kind, and the host shows the latest one and runs its time.

/**
 * A window's record of the transient messages (banners, or toasts) its callers want shown. Each
 * caller has at most one posting; the host shows the most recent.
 */
@Stable
internal class TransientHost<T : Any> {
    private val postings = mutableStateListOf<TransientPosting<T>>()

    /** The posting to show: the most recent still up. Snapshot-observable. */
    val current: TransientPosting<T>? get() = postings.lastOrNull()

    /** Replaces [caller]'s posting with [item], made the most recent; null withdraws it. */
    fun post(caller: TransientCaller, item: T?) {
        postings.removeAll { it.caller === caller }
        if (item != null) postings.add(TransientPosting(caller, item))
    }
}

/** One call site's latest callbacks, read when the host needs them. */
internal class TransientCaller {
    var onDismissRequest: () -> Unit = {}
    var onClick: (() -> Unit)? = null

    /** Whether a tap runs an action (and the message reads as a button). Snapshot-observable. */
    var actionable by mutableStateOf(false)
}

/** One showing of [item] for [caller]. Compared by identity: each post is a new showing. */
internal class TransientPosting<T : Any>(val caller: TransientCaller, val item: T)

/** Posts [item] for this call site to [host] while it's non-null and the call site is composed. */
@Composable
internal fun <T : Any> PostTransient(
    host: TransientHost<T>,
    item: T?,
    onDismissRequest: () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val caller = remember { TransientCaller() }
    SideEffect {
        caller.onDismissRequest = onDismissRequest
        caller.onClick = onClick
        caller.actionable = onClick != null
    }
    DisposableEffect(host, caller, item) {
        host.post(caller, item)
        onDispose { host.post(caller, null) }
    }
}

/**
 * How long a transient message stays up: [duration], stretched to the timeout the user asked
 * Android for (Accessibility, Time to take action) when that's longer.
 */
internal fun AccessibilityManager?.transientTimeout(
    duration: Duration,
    containsIcons: Boolean,
    containsControls: Boolean,
): Long {
    val millis = duration.inWholeMilliseconds
    return this?.calculateRecommendedTimeoutMillis(
        millis,
        containsIcons = containsIcons,
        containsText = true,
        containsControls = containsControls,
    ) ?: millis
}
