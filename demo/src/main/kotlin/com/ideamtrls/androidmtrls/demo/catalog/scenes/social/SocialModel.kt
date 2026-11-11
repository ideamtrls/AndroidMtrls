package com.ideamtrls.androidmtrls.demo

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import com.ideamtrls.androidmtrls.AMBannerItem
import com.ideamtrls.androidmtrls.AMSemantic
import com.ideamtrls.androidmtrls.AMToastItem
import com.ideamtrls.androidmtrls.amInstantSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** The two root views, which the remote swaps instantly. */
internal enum class SocialView { Feed, Alerts }

/** The Alerts view's segments. */
internal enum class SocialAlertFilter(val title: String) {
    All("All"),
    Mentions("Mentions"),
}

/**
 * A place on one root view's back stack: the root view itself, then the panels pushed over it.
 * Each panel carries a [serial] so the same post or profile can be pushed twice on one stack and
 * still be its own entry.
 */
internal sealed interface SocialRoute {
    /** The root view (the Feed or the Alerts), always first on its stack. */
    data object Root : SocialRoute

    /** A post's panel. */
    data class Post(val postId: String, val serial: Int) : SocialRoute

    /** A person's profile panel. */
    data class Profile(val userId: String, val serial: Int) : SocialRoute

    /** The alerts as a panel, pushed by the feed's bell, so back returns to the feed. */
    data class Alerts(val serial: Int) : SocialRoute
}

/** What the compose sheet writes: a new post, or a reply to a post. */
internal sealed interface SocialComposeMode {
    data object Post : SocialComposeMode

    data class Reply(val postId: String) : SocialComposeMode
}

/**
 * Owns the Social scene: the posts, people and alerts, each root view's back stack, and every
 * overlay. A plain class holding Compose state, remembered at the scene root and saved with
 * [saver]: what you've liked, posted, read, followed and muted, where each root view stands, and
 * which overlay is up all come back after recreation. A refresh in flight isn't kept.
 *
 * The scene's requests are stand-ins: the first load and pulling the feed down are short waits.
 * A refresh runs as a coroutine in [scope], the scene's own, so leaving the scene cancels it, and
 * [cancelPendingWork] cancels it whenever you move to another screen, so its toast never lands on
 * a screen it wasn't asked from.
 */
@Stable
internal class SocialModel(private val scope: CoroutineScope) {
    // Data
    val users: List<SocialUser> = SocialSamples.users

    /** Newest first. */
    val posts: SnapshotStateList<SocialPost> = SocialSamples.posts.toMutableStateList()
    val replies: SnapshotStateList<SocialReply> = SocialSamples.replies.toMutableStateList()
    val notifications: List<SocialNotification> = SocialSamples.notifications

    var liked by mutableStateOf(emptySet<String>())
        private set
    var reposted by mutableStateOf(emptySet<String>())
        private set
    var followed by mutableStateOf(emptySet<String>())
        private set
    var muted by mutableStateOf(emptySet<String>())
        private set
    var readAlerts by mutableStateOf(SocialSamples.notifications.filter { it.isRead }.map { it.id }.toSet())
        private set

    /** True until the first load lands; the feed shows its loading look meanwhile. */
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    // Navigation
    var currentView by mutableStateOf(SocialView.Feed)
    val feedStack: SnapshotStateList<SocialRoute> = mutableStateListOf(SocialRoute.Root)
    val alertsStack: SnapshotStateList<SocialRoute> = mutableStateListOf(SocialRoute.Root)
    var alertFilter by mutableStateOf(SocialAlertFilter.All)

    // Overlays
    /** The post whose menu is up in the spotlight. */
    var postMenuId by mutableStateOf<String?>(null)
    var composing by mutableStateOf(false)

    /** Set as the sheet opens and never cleared, so its title holds while it slides away. */
    var composeMode by mutableStateOf<SocialComposeMode>(SocialComposeMode.Post)
        private set
    var searching by mutableStateOf(false)
    val searchQuery = TextFieldState()
    var searchTopics by mutableStateOf(emptySet<SocialTopic>())

    /** The post the "Delete post?" alert asks about. */
    var confirmDeleteId by mutableStateOf<String?>(null)

    // Feedback
    var banner by mutableStateOf<AMBannerItem?>(null)
    var toast by mutableStateOf<AMToastItem?>(null)

    private var nextSerial by mutableIntStateOf(1)
    private var composedCount by mutableIntStateOf(0)

    /** The last person muted, for the banner's undo. */
    private var mutedUserId: String? = null

    /** A pull to refresh in flight. */
    private var refresh: Job? = null

    // Lookups

    val selfUser: SocialUser
        get() = users.first { it.isSelf }

    fun user(id: String): SocialUser = users.firstOrNull { it.id == id } ?: UnknownUser

    fun post(id: String): SocialPost? = posts.firstOrNull { it.id == id }

    fun isLiked(post: SocialPost): Boolean = post.id in liked

    fun isReposted(post: SocialPost): Boolean = post.id in reposted

    /** The post's likes, yours included. */
    fun likes(post: SocialPost): Int = post.likes + if (isLiked(post)) 1 else 0

    /** The post's reposts, yours included. */
    fun reposts(post: SocialPost): Int = post.reposts + if (isReposted(post)) 1 else 0

    fun isFollowed(userId: String): Boolean = userId in followed

    /** The follower count, counting you while you follow them. */
    fun followerCount(user: SocialUser): Int = user.followers + if (isFollowed(user.id)) 1 else 0

    fun isRead(notification: SocialNotification): Boolean = notification.id in readAlerts

    /** Posts by people you haven't muted, newest first. */
    val feed: List<SocialPost>
        get() = posts.filter { it.authorId !in muted }

    /** Unread alerts from people you haven't muted: the bell's count. */
    val unreadCount: Int
        get() = liveNotifications.count { !isRead(it) }

    /** The alerts the Alerts view lists under its filter. */
    val shownNotifications: List<SocialNotification>
        get() = liveNotifications.filter { alertFilter == SocialAlertFilter.All || it.kind.isMention }

    fun replies(postId: String): List<SocialReply> = replies.filter { it.postId == postId }

    fun replyCount(postId: String): Int = replies.count { it.postId == postId }

    /** Everything [userId] has posted. */
    fun posts(userId: String): List<SocialPost> = posts.filter { it.authorId == userId }

    /** No query and no topics: nothing to search for yet. */
    val isSearchIdle: Boolean
        get() = trimmedQuery.isEmpty() && searchTopics.isEmpty()

    /**
     * Posts by people you haven't muted whose text, author name or handle holds the trimmed query,
     * in a chosen topic when any are chosen. Topics alone are a search. Empty while idle.
     */
    val searchResults: List<SocialPost>
        get() {
            if (isSearchIdle) return emptyList()
            val query = trimmedQuery
            return posts.filter { post ->
                val author = user(post.authorId)
                author.id !in muted &&
                    (searchTopics.isEmpty() || post.topic in searchTopics) &&
                    (
                        query.isEmpty() ||
                            post.text.contains(query, ignoreCase = true) ||
                            author.name.contains(query, ignoreCase = true) ||
                            author.handle.contains(query, ignoreCase = true)
                        )
            }
        }

    val trimmedQuery: String
        get() = searchQuery.text.trim().toString()

    val composeTitle: String
        get() = if (composeMode is SocialComposeMode.Reply) "Reply" else "New post"

    /** The post being replied to while the compose sheet writes a reply. */
    val replyTarget: SocialPost?
        get() = (composeMode as? SocialComposeMode.Reply)?.let { post(it.postId) }

    /** The post the spotlight's menu is up for, while it still exists. */
    val postMenuPost: SocialPost?
        get() = postMenuId?.let(::post)

    // Lifecycle

    /** The first load: the feed shows its loading look, then the posts. */
    suspend fun load() {
        if (!isLoading) return
        delay(1200.milliseconds)
        isLoading = false
    }

    /** Pulling the feed down: adds the next pool post to the top, once each, then says there's nothing new. */
    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        refresh = scope.launch {
            val job = coroutineContext.job
            try {
                delay(1.seconds)
                val next = SocialSamples.refreshPool.firstOrNull { pooled -> posts.none { it.id == pooled.id } }
                if (next == null) {
                    toast = AMToastItem("No new posts")
                } else {
                    posts.add(0, next)
                    toast = AMToastItem("Feed updated")
                }
            } finally {
                // A cancelled refresh may finish after a newer one has started; only the latest clears the flag.
                if (refresh === job) {
                    isRefreshing = false
                    refresh = null
                }
            }
        }
    }

    /** Abandons a refresh in flight. Runs whenever you move to another screen, and as the scene leaves. */
    fun cancelPendingWork() {
        refresh?.cancel()
        refresh = null
        isRefreshing = false
    }

    // Navigation

    /** The back stack of [view], its root first. */
    fun stack(view: SocialView): SnapshotStateList<SocialRoute> =
        if (view == SocialView.Feed) feedStack else alertsStack

    /** Swaps the root view instantly; each keeps its own panels. */
    fun switchTo(view: SocialView) {
        cancelPendingWork()
        amInstantSwitch { currentView = view }
    }

    /** Pops the top panel of [view]'s stack. */
    fun pop(view: SocialView) {
        val stack = stack(view)
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    fun openPost(postId: String) = push(SocialRoute.Post(postId, takeSerial()))

    fun openProfile(userId: String) = push(SocialRoute.Profile(userId, takeSerial()))

    /**
     * The feed's bell: pushes the alerts as a panel over the feed, so back returns to where it was
     * pressed. The remote's Alerts tile swaps the root instead.
     */
    fun openAlertsPanel() = push(SocialRoute.Alerts(takeSerial()))

    /** Pushes your own profile over whichever root shows, replacing any open panel. */
    fun openOwnProfile() {
        val stack = stack(currentView)
        val showing = stack.size == 2 && (stack[1] as? SocialRoute.Profile)?.userId == selfUser.id
        if (!showing) replacePanels(SocialRoute.Profile(selfUser.id, takeSerial()))
    }

    /** Marks the alert read and opens what it's about: the profile for a follow, otherwise its post. */
    fun open(notification: SocialNotification) {
        readAlerts = readAlerts + notification.id
        if (notification.kind == SocialNotificationKind.Follow) {
            openProfile(notification.actorId)
        } else {
            notification.postId?.takeIf { post(it) != null }?.let(::openPost)
        }
    }

    fun toggleRead(notificationId: String) {
        readAlerts = readAlerts.toggled(notificationId)
    }

    // Posts and people

    fun toggleLike(postId: String) {
        liked = liked.toggled(postId)
    }

    fun toggleRepost(postId: String) {
        reposted = reposted.toggled(postId)
    }

    fun showPostMenu(postId: String) {
        postMenuId = postId
    }

    /** Hides the person's posts, alerts and search results; the banner's tap undoes it. */
    fun mute(userId: String) {
        postMenuId = null
        muted = muted + userId
        mutedUserId = userId
        banner = AMBannerItem(
            "Muted ${user(userId).handle}",
            message = "Their posts and alerts are hidden. Tap to undo.",
            style = AMSemantic.Info,
        )
    }

    fun undoMute() {
        val userId = mutedUserId ?: return
        mutedUserId = null
        muted = muted - userId
        toast = AMToastItem("Unmuted ${user(userId).handle}")
    }

    /** Asks before deleting your own post. */
    fun confirmDelete(postId: String) {
        postMenuId = null
        confirmDeleteId = postId
    }

    /** Removes the post, its replies (and so its alerts) and any panel showing it. */
    fun delete(postId: String) {
        posts.removeAll { it.id == postId }
        replies.removeAll { it.postId == postId }
        listOf(feedStack, alertsStack).forEach { stack -> stack.removeAll { (it as? SocialRoute.Post)?.postId == postId } }
        toast = AMToastItem("Post deleted")
    }

    fun toggleFollow(userId: String) {
        followed = followed.toggled(userId)
        val handle = user(userId).handle
        toast = AMToastItem(if (isFollowed(userId)) "Following $handle" else "Unfollowed $handle")
    }

    // Compose

    fun beginPost() {
        composeMode = SocialComposeMode.Post
        composing = true
    }

    fun beginReply(postId: String) {
        composeMode = SocialComposeMode.Reply(postId)
        composing = true
    }

    /** Posts or replies with the trimmed text and closes the sheet. */
    fun send(text: String) {
        val trimmed = text.trim()
        composedCount += 1
        when (val mode = composeMode) {
            SocialComposeMode.Post -> {
                posts.add(0, composedPost("mine-$composedCount", trimmed))
                toast = AMToastItem("Posted")
            }
            is SocialComposeMode.Reply -> {
                replies.add(SocialReply("reply-mine-$composedCount", mode.postId, selfUser.id, trimmed, "now"))
                toast = AMToastItem("Reply sent")
            }
        }
        composing = false
    }

    // Search

    /** Opens the search sheet fresh. */
    fun beginSearch() {
        searchQuery.clearText()
        searchTopics = emptySet()
        searching = true
    }

    /** Closes the search sheet and opens the post over the root. */
    fun openSearchResult(postId: String) {
        searching = false
        replacePanels(SocialRoute.Post(postId, takeSerial()))
    }

    // Private

    /** Alerts from people you haven't muted, about posts that still exist. */
    private val liveNotifications: List<SocialNotification>
        get() = notifications.filter { notification ->
            notification.actorId !in muted && (notification.postId == null || post(notification.postId) != null)
        }

    /**
     * Pushes [route] on the showing root's stack, unless that same panel is already on top: a double
     * tap would push it twice, its serial alone making the two differ.
     */
    private fun push(route: SocialRoute) {
        val stack = stack(currentView)
        if (stack.lastOrNull()?.isSamePanel(route) == true) return
        cancelPendingWork()
        stack.add(route)
    }

    /** Whether this and [other] show the same thing, whatever their serials. */
    private fun SocialRoute.isSamePanel(other: SocialRoute): Boolean = when (this) {
        SocialRoute.Root -> other == SocialRoute.Root
        is SocialRoute.Post -> other is SocialRoute.Post && other.postId == postId
        is SocialRoute.Profile -> other is SocialRoute.Profile && other.userId == userId
        is SocialRoute.Alerts -> other is SocialRoute.Alerts
    }

    /** Leaves the showing root with only [route] over it. */
    private fun replacePanels(route: SocialRoute) {
        cancelPendingWork()
        val stack = stack(currentView)
        stack.removeRange(1, stack.size)
        stack.add(route)
    }

    private fun takeSerial(): Int = nextSerial.also { nextSerial += 1 }

    private fun composedPost(id: String, text: String): SocialPost =
        SocialPost(id, selfUser.id, text, topic = null, timestamp = "now", likes = 0, reposts = 0)

    companion object {
        private val UnknownUser = SocialUser(
            "unknown", "Unknown", "@unknown", null, bio = "", joined = "", location = "", website = "",
            followers = 0, following = 0,
        )

        private fun <T> Set<T>.toggled(value: T): Set<T> = if (value in this) this - value else this + value

        private fun encode(route: SocialRoute): String = when (route) {
            SocialRoute.Root -> "root"
            is SocialRoute.Post -> "post|${route.postId}|${route.serial}"
            is SocialRoute.Profile -> "profile|${route.userId}|${route.serial}"
            is SocialRoute.Alerts -> "alerts||${route.serial}"
        }

        private fun decode(code: String): SocialRoute? {
            val parts = code.split('|')
            val serial = parts.getOrNull(2)?.toIntOrNull()
            return when (parts[0]) {
                "root" -> SocialRoute.Root
                "post" -> serial?.let { SocialRoute.Post(parts[1], it) }
                "profile" -> serial?.let { SocialRoute.Profile(parts[1], it) }
                "alerts" -> serial?.let { SocialRoute.Alerts(it) }
                else -> null
            }
        }

        private fun SnapshotStateList<SocialRoute>.restore(codes: List<*>) {
            val routes = codes.filterIsInstance<String>().mapNotNull(::decode).filter { it != SocialRoute.Root }
            removeRange(1, size)
            addAll(routes)
        }

        private fun strings(value: Any?): List<String> = (value as? List<*>).orEmpty().filterIsInstance<String>()

        /**
         * Saves the data you've changed, both back stacks, the current view and which overlay is up;
         * restores them into a model on the restored scene's [scope]. The posts are saved as their
         * ids in feed order, with the text of those you composed; a refresh in flight isn't kept.
         */
        fun saver(scope: CoroutineScope): Saver<SocialModel, Any> = mapSaver(
            save = { model ->
                val composed = model.posts.filter { it.id.startsWith("mine-") }.flatMap { listOf(it.id, it.text) }
                val myReplies = model.replies.filter { it.id.startsWith("reply-mine-") }
                    .flatMap { listOf(it.id, it.postId, it.text) }
                mapOf(
                    "postIds" to ArrayList(model.posts.map { it.id }),
                    "composed" to ArrayList(composed),
                    "replyIds" to ArrayList(model.replies.map { it.id }),
                    "myReplies" to ArrayList(myReplies),
                    "liked" to ArrayList(model.liked),
                    "reposted" to ArrayList(model.reposted),
                    "followed" to ArrayList(model.followed),
                    "muted" to ArrayList(model.muted),
                    "read" to ArrayList(model.readAlerts),
                    "loading" to model.isLoading,
                    "view" to model.currentView.name,
                    "feedStack" to ArrayList(model.feedStack.map(::encode)),
                    "alertsStack" to ArrayList(model.alertsStack.map(::encode)),
                    "filter" to model.alertFilter.name,
                    "serial" to model.nextSerial,
                    "composedCount" to model.composedCount,
                    "postMenu" to model.postMenuId,
                    "composing" to model.composing,
                    "replyTo" to (model.composeMode as? SocialComposeMode.Reply)?.postId,
                    "searching" to model.searching,
                    "query" to model.searchQuery.text.toString(),
                    "topics" to ArrayList(model.searchTopics.map { it.name }),
                    "confirmDelete" to model.confirmDeleteId,
                )
            },
            restore = { saved ->
                SocialModel(scope).apply {
                    val composed = strings(saved["composed"]).chunked(2).associate { (id, text) -> id to text }
                    val known = (SocialSamples.posts + SocialSamples.refreshPool).associateBy { it.id }
                    val restoredPosts = strings(saved["postIds"]).mapNotNull { id ->
                        known[id] ?: composed[id]?.let { composedPost(id, it) }
                    }
                    posts.clear()
                    posts.addAll(restoredPosts)

                    val mine = strings(saved["myReplies"]).chunked(3).associate { (id, postId, text) ->
                        id to SocialReply(id, postId, selfUser.id, text, "now")
                    }
                    val knownReplies = SocialSamples.replies.associateBy { it.id }
                    val restoredReplies = strings(saved["replyIds"]).mapNotNull { knownReplies[it] ?: mine[it] }
                    replies.clear()
                    replies.addAll(restoredReplies)

                    liked = strings(saved["liked"]).toSet()
                    reposted = strings(saved["reposted"]).toSet()
                    followed = strings(saved["followed"]).toSet()
                    muted = strings(saved["muted"]).toSet()
                    readAlerts = strings(saved["read"]).toSet()
                    isLoading = saved["loading"] as Boolean
                    currentView = SocialView.valueOf(saved["view"] as String)
                    feedStack.restore(strings(saved["feedStack"]))
                    alertsStack.restore(strings(saved["alertsStack"]))
                    alertFilter = SocialAlertFilter.valueOf(saved["filter"] as String)
                    nextSerial = saved["serial"] as Int
                    composedCount = saved["composedCount"] as Int
                    postMenuId = saved["postMenu"] as String?
                    composing = saved["composing"] as Boolean
                    composeMode = (saved["replyTo"] as String?)?.let { SocialComposeMode.Reply(it) } ?: SocialComposeMode.Post
                    searching = saved["searching"] as Boolean
                    searchQuery.setTextAndPlaceCursorAtEnd(saved["query"] as String)
                    searchTopics = strings(saved["topics"]).map { SocialTopic.valueOf(it) }.toSet()
                    confirmDeleteId = saved["confirmDelete"] as String?
                }
            },
        )
    }
}
