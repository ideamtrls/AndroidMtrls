package com.ideamtrls.androidmtrls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.updateTransition
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
 * Shows [item] as a banner at the top of the window while it's non-null: a floating surface in the
 * app's surface setting with the two-layer shadow (solid with a hairline in both modes, or frosted
 * with the frosted edge) just under the status bar, over a fade of the surface color that keeps
 * the status bar legible.
 *
 * It draws nothing where it's called: the nearest [AMBannerHost], at the root of the window, shows
 * it, so a banner posted from deep inside a screen still spans the screen. While a sheet is up, it
 * shows over the sheet, whether it was posted from the sheet or from the screen under it. Without a
 * host above it, it fails at once.
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
    PostTransient(LocalAMBannerHost.current ?: error(NoBannerHost), item, onDismissRequest, onClick)
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
 * Every [AMSheet] carries one too, which takes over while the sheet is up: a banner posted to the
 * screen's host, or from inside the sheet, comes down over the sheet, in a window of its own at
 * the top of the screen, and goes back to the screen's host if it's still up when the sheet goes.
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
    BannerHost(modifier, overScreen = false, content)
}

/**
 * [AMBannerHost], or with [overScreen] the host of a window over a screen (a sheet's): while it's
 * composed it shows its screen's banners, and its own, in a window of its own at the top, over its
 * window, and the screen's host shows none.
 */
@Composable
internal fun BannerHost(
    modifier: Modifier,
    overScreen: Boolean,
    content: @Composable () -> Unit,
) {
    val below = if (overScreen) LocalAMBannerHost.current else null
    val host = remember(below) { TransientHost<AMBannerItem>(below) }
    if (overScreen) OverScreen(host)
    val posting = host.current
    val accessibility = LocalAccessibilityManager.current

    LaunchedEffect(posting) {
        val shown = posting ?: return@LaunchedEffect
        delay(accessibility.transientTimeout(shown.item.duration, containsIcons = true, shown.caller.actionable))
        shown.caller.onDismissRequest()
    }

    val transition = updateTransition(posting, label = "AMBanner")
    Box(modifier, propagateMinConstraints = true) {
        CompositionLocalProvider(LocalAMBannerHost provides host, content = content)
        if (!overScreen) {
            BannerLayer(transition, WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal), Modifier.matchParentSize())
        } else if (transition.currentState != null || transition.targetState != null) {
            // Showing, or sliding away: in a window over this one.
            BannerWindow(transition)
        }
    }
}

/** The nearest banner host, or null with none above. */
internal val LocalAMBannerHost = staticCompositionLocalOf<TransientHost<AMBannerItem>?> { null }

private const val NoBannerHost = "AMBanner shows in the nearest AMBannerHost: put one at the root of the window"

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
 * The scrim and the banner over a host: the scrim fades, the card slides from the top, clear of
 * [insets]. The card changes in place when the same caller posts a new item, and slides out and in
 * between callers.
 *
 * In a host's own window the scrim is [ScrimHeight] tall over the content. In a window of its own
 * ([fitted]) the window is only as tall as the card's band, so the scrim fills the band instead:
 * solid behind the status bar, clear at the card's bottom.
 */
@Composable
private fun BannerLayer(
    transition: Transition<TransientPosting<AMBannerItem>?>,
    insets: WindowInsets,
    modifier: Modifier,
    fitted: Boolean = false,
) {
    val motion = MaterialTheme.motionScheme
    val surface = AMTheme.Surface.primary
    val spacing = AMTheme.tokens.spacing
    val solidTop = insets.asPaddingValues().calculateTopPadding()
    Box(modifier) {
        transition.AnimatedVisibility(
            visible = { it != null },
            modifier = if (fitted) Modifier.matchParentSize() else Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            enter = fadeIn(motion.defaultEffectsSpec()),
            exit = fadeOut(motion.fastEffectsSpec()),
        ) {
            val scrim = if (fitted) {
                Modifier.fillMaxSize().drawBehind {
                    val solid = (solidTop.toPx() / size.height).coerceIn(0f, 1f)
                    drawRect(Brush.verticalGradient(0f to surface, solid to surface, 1f to surface.copy(alpha = 0f)))
                }
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(ScrimHeight)
                    .background(
                        Brush.verticalGradient(
                            0f to surface,
                            ScrimFadeStart to surface,
                            1f to surface.copy(alpha = 0f),
                        ),
                    )
            }
            Box(scrim)
        }
        transition.AnimatedContent(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            transitionSpec = {
                val enter = slideInVertically(motion.defaultSpatialSpec()) { -it } +
                    fadeIn(motion.defaultEffectsSpec())
                val exit = slideOutVertically(motion.fastSpatialSpec()) { -it } +
                    fadeOut(motion.fastEffectsSpec())
                (enter togetherWith exit).using(null)
            },
            contentAlignment = Alignment.TopCenter,
            contentKey = { it?.caller },
        ) { shown ->
            if (shown != null) {
                BannerCard(
                    shown,
                    Modifier
                        .windowInsetsPadding(insets)
                        .padding(top = spacing.sm, start = spacing.lg, end = spacing.lg),
                )
            }
        }
    }
}

/**
 * The banner of a host over a screen (a sheet's), in a window of its own at the top of the screen,
 * over the host's window: a popup that never takes focus or back and lets touches outside it
 * through. The window spans the card's band only, so the sheet under it keeps its touches. The
 * insets are the host window's, read here, since the popup lies over the status bar.
 */
@Composable
private fun BannerWindow(transition: Transition<TransientPosting<AMBannerItem>?>) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val safe = WindowInsets.safeDrawing
    val insets = WindowInsets(
        left = safe.getLeft(density, direction),
        top = safe.getTop(density),
        right = safe.getRight(density, direction),
        bottom = 0,
    )
    Popup(
        popupPositionProvider = WindowTop,
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        // A window of its own at the top of the screen, not on the sheet: the card starts from the
        // window's fresh context, lying on the page with no art to sample, so in a frosted app it's
        // frost over the page color, opaque, never the sheet's tint over a scrim fading to clear.
        ProvideOverlayWindowLocals {
            BannerLayer(transition, insets, Modifier.fillMaxWidth(), fitted = true)
        }
    }
}

/** Places a popup at the top start of its window, which for a sheet is the top of the screen. */
private object WindowTop : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

/**
 * The banner itself: the role's glyph, then the title over the message, on the floating surface in
 * the app's surface setting, outlined in both modes. It follows a drag up 1:1 and dismisses past
 * [SwipeThreshold] (or on a quick fling up); a shorter drag settles back.
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
            // In the app's surface setting, outlined in both modes when it's solid.
            .amElevatedSurface(shape, alwaysOutlined = true)
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
 *
 * A screen's host is a root. The host of a window over that screen (a sheet's) is made with the
 * screen's host [below] it and shares its root; while that window is up ([open] to [close]) it's
 * the topmost of the root's windows, and the topmost host shows the most recent posting made to
 * any host of the root, so a message reaches the window the user sees. The others show none.
 */
@Stable
internal class TransientHost<T : Any>(below: TransientHost<T>? = null) {
    private val root: TransientHost<T> = below?.root ?: this
    private val postings = mutableStateListOf<TransientPosting<T>>()

    /** On a root: the hosts of the windows up over its screen, in the order they came up. */
    private val windows = mutableStateListOf<TransientHost<T>>()

    /** On a root: the order the next posting to any of its hosts takes. */
    private var nextOrder = 0L

    /**
     * The posting to show here: the most recent still up among the root's hosts, while this host's
     * window is the topmost of them; otherwise none. Snapshot-observable.
     */
    val current: TransientPosting<T>?
        get() {
            val top = root.windows.lastOrNull() ?: root
            if (top !== this) return null
            var latest = root.postings.lastOrNull()
            root.windows.forEach { window ->
                val own = window.postings.lastOrNull()
                if (own != null && (latest == null || own.order > latest.order)) latest = own
            }
            return latest
        }

    /** Replaces [caller]'s posting with [item], made the most recent; null withdraws it. */
    fun post(caller: TransientCaller, item: T?) {
        postings.removeAll { it.caller === caller }
        if (item != null) postings.add(TransientPosting(caller, item, root.nextOrder++))
    }

    /** This host's window has come up over its screen: it shows the root's messages now. */
    fun open() {
        if (root !== this && this !in root.windows) root.windows.add(this)
    }

    /** This host's window has gone: the one under it shows the root's messages again. */
    fun close() {
        root.windows.remove(this)
    }
}

/** Holds [host] up over its screen while this is composed: from the window's content coming in to its leaving. */
@Composable
internal fun <T : Any> OverScreen(host: TransientHost<T>) {
    DisposableEffect(host) {
        host.open()
        onDispose { host.close() }
    }
}

/** One call site's latest callbacks, read when the host needs them. */
internal class TransientCaller {
    var onDismissRequest: () -> Unit = {}
    var onClick: (() -> Unit)? = null

    /** Whether a tap runs an action (and the message reads as a button). Snapshot-observable. */
    var actionable by mutableStateOf(false)
}

/**
 * One showing of [item] for [caller], [order] placing it among its root's postings. Compared by
 * identity: each post is a new showing.
 */
internal class TransientPosting<T : Any>(val caller: TransientCaller, val item: T, val order: Long)

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
