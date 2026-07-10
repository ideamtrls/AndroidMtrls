package io.github.jmsdnns.androidmtrls.demo.scenes.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.jmsdnns.androidmtrls.content.AMAvatar
import io.github.jmsdnns.androidmtrls.content.AMAvatarSize
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGrid
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridButton
import io.github.jmsdnns.androidmtrls.controls.buttons.AMActionGridRow
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentEntry
import io.github.jmsdnns.androidmtrls.demo.catalog.ComponentSection
import io.github.jmsdnns.androidmtrls.demo.scenes.common.LocalSceneExit
import io.github.jmsdnns.androidmtrls.demo.scenes.common.SceneGradient
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelChrome
import io.github.jmsdnns.androidmtrls.demo.scenes.common.ScenePanelHost
import io.github.jmsdnns.androidmtrls.demo.scenes.common.brush
import io.github.jmsdnns.androidmtrls.demo.scenes.common.sceneFabLift
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMCard
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMActionsOverlay
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlert
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBanner
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMFloatingActionsHost
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheet
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMSheetHeight
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToast
import io.github.jmsdnns.androidmtrls.tokens.AMPadding
import io.github.jmsdnns.androidmtrls.tokens.AMTheme
import io.github.jmsdnns.androidmtrls.tokens.LocalAMIsDarkTheme
import kotlinx.coroutines.flow.first

// ---------------------------------------------------------------------------
// Catalog entry
// ---------------------------------------------------------------------------

/**
 * Catalog entry for the [SocialScene] demo (Screens section). id `scene-social`.
 * Renders full-screen with no `ComponentPage` scaffold — the scene owns its
 * chrome and its Exit affordance ([LocalSceneExit]). Mirrors iOS `SocialScene`
 * registration.
 */
val socialSceneEntry: ComponentEntry = ComponentEntry(
    id = "scene-social",
    section = ComponentSection.Screens,
    title = "Social",
    subtitle = "A feed of cards with action rows",
    icon = Icons.Filled.People,
    content = { SocialScene() },
)

// ---------------------------------------------------------------------------
// SocialScene — the flagship social-feed scene
// ---------------------------------------------------------------------------

/**
 * Social — a realistic, fully interactive social-feed scene composed from
 * AndroidMtrls primitives. One remembered [SocialState] drives the feed, the
 * post-detail and profile panels, the alerts view, the compose and search
 * sheets, and all feedback overlays. Post cards sit on a colored gradient
 * substrate ([SocialBackdrop]) that follows the light/dark scheme. Ports iOS
 * `SocialScene`.
 *
 * **Overlay host nesting** (outermost → innermost):
 * `AMToast → AMBanner → AMAlert → AMActionsOverlay → AMSheet(compose) →
 * AMSheet(search) → AMFloatingActionsHost → ScenePanelHost`. Each host wraps the
 * scene body and ORs its presented state into `LocalAMOverlayActive`, so the
 * hub's FAB (innermost) fades under any feedback/modal/sheet. Sheets and modals
 * are ancestors of the panel host, so they cover pushed panels too — mirroring
 * iOS attaching them to the navigation-stack container.
 */
@Composable
fun SocialScene() {
    val state = remember { SocialState() }
    val sceneExit = LocalSceneExit.current
    val clipboard = LocalClipboardManager.current

    var showActions by remember { mutableStateOf(false) }
    val fabOffset = remember { mutableStateOf(Offset.Zero) }

    // Clear the one-shot instant-switch flag right after the frame that consumed
    // it (the settled transition sticks because `path` no longer changes).
    LaunchedEffect(state.snapNextPanel) {
        if (state.snapNextPanel) state.snapNextPanel = false
    }

    // ── Host stack: toast → banner → alert → actions → sheets → FAB → body ──
    AMToast(
        item = state.toast,
        onDismiss = { state.toast = null },
    ) {
        AMBanner(
            item = state.banner,
            onDismiss = { state.banner = null },
        ) {
            AMAlert(
                item = state.alert,
                onDismiss = { state.alert = null },
            ) {
                AMActionsOverlay(
                    isPresented = state.postMenuId != null,
                    onDismiss = { state.postMenuId = null },
                    overlayContent = { PostActionMenu(state, clipboard) },
                ) {
                    AMSheet(
                        isPresented = state.composing,
                        onDismiss = {
                            state.composing = false
                            state.replyTargetId = null
                        },
                        height = AMSheetHeight.Full,
                        sheetContent = { SocialComposeSheet(state) },
                    ) {
                        AMSheet(
                            isPresented = state.searching,
                            onDismiss = {
                                state.searching = false
                                state.searchQuery = ""
                            },
                            height = AMSheetHeight.Full,
                            sheetContent = { SocialSearchSheet(state) },
                        ) {
                            AMFloatingActionsHost(
                                isPresented = showActions,
                                onPresentChange = { showActions = it },
                                bottomInset = sceneFabLift(),
                                offset = fabOffset,
                                accessibilityLabel = "Actions",
                                menu = { FabMenu(state, sceneExit) { showActions = it } },
                            ) {
                                SceneBody(state)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Scene body — panel stack over the feed/alerts root
// ---------------------------------------------------------------------------

@Composable
private fun SceneBody(state: SocialState) {
    ScenePanelHost(
        path = state.path,
        onPop = { state.pop() },
        snapNext = state.snapNextPanel,
        root = { RootContent(state) },
        panel = { route ->
            when (route) {
                is SocialRoute.PostDetail -> {
                    // Same pastel substrate as the feed runs full-bleed behind the
                    // transparent chrome and status bar.
                    Box(modifier = Modifier.fillMaxSize()) {
                        SocialBackdrop()
                        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                            // No hairline: the chrome floats over the full-bleed
                            // pastel backdrop, not an attached Surface.page bar.
                            ScenePanelChrome(
                                title = "Post",
                                onBack = { state.pop() },
                                divider = false,
                            )
                            SocialPostDetailView(state, route.postId)
                        }
                    }
                }

                is SocialRoute.Profile -> {
                    // The panel supplies its own Surface.page fill (via ScenePanelHost).
                    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                        ScenePanelChrome(title = state.user(route.userId).name, onBack = { state.pop() })
                        SocialProfileView(state, route.userId)
                    }
                }
            }
        },
    )
}

@Composable
private fun RootContent(state: SocialState) {
    Box(modifier = Modifier.fillMaxSize()) {
        SocialBackdrop()
        when (state.currentView) {
            SocialView.Feed -> SocialFeedView(state)
            SocialView.Alerts -> SocialNotificationsView(state)
        }
    }
}

// ---------------------------------------------------------------------------
// FAB menu — Compose / Search over Feed / Alerts / Exit
// ---------------------------------------------------------------------------

/**
 * Global FAB menu — a 2-up row (Compose, Search) over a 3-up row (Feed, Alerts,
 * Exit), per the action-grid 2/3/4-per-row rule. Ports iOS `SocialScene.fabMenu`.
 */
@Composable
private fun FabMenu(
    state: SocialState,
    sceneExit: () -> Unit,
    onPresentChange: (Boolean) -> Unit,
) {
    AMActionGrid {
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.Filled.Edit,
                    title = "Compose",
                    onClick = {
                        onPresentChange(false)
                        state.replyTargetId = null
                        state.composing = true
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Filled.Search,
                    title = "Search",
                    onClick = {
                        onPresentChange(false)
                        state.searching = true
                    },
                )
            },
        )
        AMActionGridRow(
            tileA = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.ViewList,
                    title = "Feed",
                    onClick = {
                        // Pop any open panel too — the FAB is reachable from inside
                        // panels, and a stale panel would cover the feed.
                        state.showFeed()
                        onPresentChange(false)
                    },
                )
            },
            tileB = {
                AMActionGridButton(
                    icon = Icons.Filled.Notifications,
                    title = "Alerts",
                    onClick = {
                        state.showAlerts()
                        onPresentChange(false)
                    },
                )
            },
            tileC = {
                AMActionGridButton(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Exit",
                    onClick = { sceneExit() },
                )
            },
        )
    }
}

// ---------------------------------------------------------------------------
// PostActionMenu — the per-post action sheet inside the actions overlay
// ---------------------------------------------------------------------------

/**
 * Fixed height of the action row's [AMActionGridButton] tiles (the mtrls
 * `actionGridTileHeight`). The pinned Copy/Mute/Delete row is exactly this tall,
 * so the card's scroll region reserves it without measuring the row live.
 */
private val postMenuActionRowHeight = 76.dp

/**
 * Per-post action menu shown inside the blurred paused-world overlay — a measured,
 * pinned card preview mirroring iOS `SocialScene.PostActionMenu`.
 *
 * The tapped post rises from the bottom as the **full, non-interactive feed card**
 * (avatar image, identity, body text, and the post image when present — the shared
 * [postBodyContent]) with the Copy / Mute / Delete row **pinned beneath it**. An
 * empty blurred strip stays above the card so a tap or drag there dismisses the
 * overlay (the catcher shows through the untouched region).
 *
 * **Measured cap (iOS parity).** [BoxWithConstraints] hands the overlay height;
 * the card sits in a `verticalScroll` region capped at `available` — the height
 * left after an empty top strip (`xxl` + status bar), the `md` card→row gap, the
 * fixed row height, and the bottom pad (`xl` + nav bar). A short card shrink-wraps
 * (leaving more blurred dismiss room above); a tall card is squeezed to `available`
 * and scrolls, **anchored to the bottom** so the card's bottom — nearest the
 * buttons — shows first (iOS `defaultScrollAnchor(.bottom)`). The buttons never
 * move. There is no sheet card and no drag handle anywhere in this menu.
 *
 * Mute is disabled for the self user; Delete is destructive.
 */
@Composable
private fun PostActionMenu(
    state: SocialState,
    clipboard: ClipboardManager,
) {
    val post = state.postMenuPost ?: return
    val author = state.user(post.authorId)
    val primary = AMTheme.Text.primary.resolve(LocalAMIsDarkTheme.current)

    // Bases + safe-area insets so the card clears the status bar / cutout and the
    // buttons clear the gesture-nav bar, whichever way the overlay is inset.
    val topStrip = AMTheme.Spacing.xxl.dp +
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gap = AMTheme.Spacing.md.dp
    val bottomPad = AMTheme.Spacing.xl.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val scrollState = rememberScrollState()
    // Anchor the scroll to the bottom once the card overflows (iOS
    // defaultScrollAnchor(.bottom)): a short card never overflows and this
    // suspends harmlessly; a tall card jumps to its bottom edge, beside the row.
    LaunchedEffect(post.id) {
        val max = snapshotFlow { scrollState.maxValue }
            .first { it > 0 && it != Int.MAX_VALUE }
        scrollState.scrollTo(max)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Room the card may occupy: total minus the empty top strip, the gap, the
        // pinned row, and the bottom pad. The card shrink-wraps below this and
        // scrolls at it.
        val available = (maxHeight - topStrip - gap - postMenuActionRowHeight - bottomPad)
            .coerceAtLeast(0.dp)

        Column(modifier = Modifier.fillMaxSize()) {
            // Empty blurred dismiss strip — grows to bottom-anchor the card + row,
            // and is exactly `topStrip` tall when the card fills `available`.
            Spacer(modifier = Modifier.weight(1f))

            // Non-interactive snapshot of the feed card, scrollable + capped.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = available)
                    .verticalScroll(scrollState)
                    .padding(horizontal = AMTheme.Spacing.lg.dp),
            ) {
                AMCard(modifier = Modifier.fillMaxWidth(), padding = AMPadding.block) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        AMAvatar(url = author.avatarUrl, initials = author.initials, size = AMAvatarSize.MD)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(AMTheme.Spacing.sm.dp),
                        ) {
                            PostIdentityHeader(author = author, timestamp = post.timestamp)
                            postBodyContent(
                                post = post,
                                textStyle = AMTheme.Type.secondary.toTextStyle(primary),
                                imageSize = 240.dp,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(gap))

            // Pinned Copy / Mute / Delete row — always visible beneath the card.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AMTheme.Spacing.lg.dp)
                    .padding(bottom = bottomPad),
            ) {
                AMActionGridRow(
                    tileA = {
                        AMActionGridButton(
                            icon = Icons.Filled.ContentCopy,
                            title = "Copy",
                            onClick = {
                                clipboard.setText(AnnotatedString(post.text))
                                state.copyText()
                            },
                        )
                    },
                    tileB = {
                        AMActionGridButton(
                            icon = Icons.Filled.VolumeOff,
                            title = "Mute",
                            enabled = !author.isSelf,
                            onClick = { state.muteAuthor(post.id) },
                        )
                    },
                    tileC = {
                        AMActionGridButton(
                            icon = Icons.Filled.Delete,
                            title = "Delete",
                            role = AMButtonRole.Destructive,
                            onClick = { state.confirmDelete(post.id) },
                        )
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SocialBackdrop — the colored gradient + translucent circles substrate
// ---------------------------------------------------------------------------

/** The two-stop scene gradient carrying Social's exact light/dark stops. */
private val socialGradient = SceneGradient(
    lightTop = Color(red = 0.84f, green = 0.94f, blue = 0.89f, alpha = 1f),
    lightBottom = Color(red = 0.88f, green = 0.85f, blue = 0.96f, alpha = 1f),
    darkTop = Color(red = 0.03f, green = 0.03f, blue = 0.10f, alpha = 1f),
    darkBottom = Color(red = 0.10f, green = 0.04f, blue = 0.16f, alpha = 1f),
)

/**
 * The Social scene's image substrate: the two-stop [socialGradient] wash plus
 * three offset translucent circles, in cool pastel (light) or deep cool-purple
 * (dark). Decorative — never captures taps. Colors and offsets ported verbatim
 * from iOS `SocialBackdrop` (SocialScene.swift:250-298). Offsets are relative to
 * the substrate center (iOS ZStack centering); iOS points map to dp.
 */
@Composable
internal fun SocialBackdrop() {
    val isDark = LocalAMIsDarkTheme.current
    val brush = socialGradient.brush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(brush = brush)
                drawBackdropCircles(isDark)
            },
    )
}

private fun DrawScope.drawBackdropCircles(isDark: Boolean) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    fun circle(color: Color, diameter: Float, dx: Float, dy: Float) {
        drawCircle(
            color = color,
            radius = (diameter / 2f).dp.toPx(),
            center = Offset(cx + dx.dp.toPx(), cy + dy.dp.toPx()),
        )
    }

    if (isDark) {
        circle(Color(red = 0.18f, green = 0.55f, blue = 0.95f, alpha = 0.30f), 320f, -140f, -260f)
        circle(Color(red = 0.70f, green = 0.25f, blue = 0.90f, alpha = 0.30f), 240f, 160f, 80f)
        circle(Color(red = 0.95f, green = 0.30f, blue = 0.55f, alpha = 0.25f), 200f, 40f, 320f)
    } else {
        circle(Color(red = 0.56f, green = 0.74f, blue = 0.92f, alpha = 0.55f), 320f, -140f, -260f)
        circle(Color(red = 0.71f, green = 0.62f, blue = 0.92f, alpha = 0.55f), 240f, 160f, 80f)
        circle(Color(red = 0.56f, green = 0.87f, blue = 0.76f, alpha = 0.55f), 200f, 40f, 320f)
    }
}
