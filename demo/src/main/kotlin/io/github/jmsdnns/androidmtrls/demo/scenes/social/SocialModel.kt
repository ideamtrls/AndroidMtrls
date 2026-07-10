package io.github.jmsdnns.androidmtrls.demo.scenes.social

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jmsdnns.androidmtrls.surfaces.frames.AMButtonRole
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlertAction
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMAlertItem
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBannerItem
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMBannerStyle
import io.github.jmsdnns.androidmtrls.surfaces.overlays.AMToastItem
import kotlinx.coroutines.delay
import java.util.UUID

// ---------------------------------------------------------------------------
// Data types — ports of the iOS SocialModel structs/enums
// ---------------------------------------------------------------------------

/**
 * A social account. [id] is the handle without the leading `@` (e.g. `"countess"`),
 * used as the stable key referenced by posts, replies, and notifications.
 *
 * Immutable value type — mutations (follow, mute) replace the element in
 * [SocialState.users]. Mirrors iOS `SocialUser`.
 */
@Stable
data class SocialUser(
    val id: String,
    val name: String,
    val handle: String,
    val initials: String,
    val avatarUrl: String?,
    val bio: String,
    val joined: String,
    val location: String,
    val link: String,
    val followers: Int,
    val following: Int,
    val isFollowed: Boolean = false,
    val isMuted: Boolean = false,
    val isSelf: Boolean = false,
)

/**
 * A feed post. [id] is a fresh [UUID] generated at seed time; replies and
 * notifications reference it, so posts must be built before them. Mirrors iOS
 * `SocialPost`.
 */
@Stable
data class SocialPost(
    val id: UUID = UUID.randomUUID(),
    val authorId: String,
    val text: String,
    val imageUrl: String? = null,
    val timestamp: String,
    val likes: Int,
    val reposts: Int,
    val likedByMe: Boolean = false,
    val repostedByMe: Boolean = false,
)

/** A reply keyed to a post by [postId]. Mirrors iOS `SocialReply`. */
@Stable
data class SocialReply(
    val id: UUID = UUID.randomUUID(),
    val postId: UUID,
    val authorId: String,
    val text: String,
    val timestamp: String,
)

/** The four notification kinds, each carrying its user-visible verb. Mirrors iOS `SocialNotificationKind`. */
enum class SocialNotificationKind(val verb: String) {
    Like("liked your post"),
    Repost("reposted your post"),
    Reply("replied to your post"),
    Follow("followed you"),
}

/** An activity notification. Mirrors iOS `SocialNotification`. */
@Stable
data class SocialNotification(
    val id: UUID = UUID.randomUUID(),
    val kind: SocialNotificationKind,
    val actorId: String,
    val postId: UUID? = null,
    val timestamp: String,
    val isRead: Boolean,
)

/** The two swappable scene roots. Mirrors iOS `SocialView`. */
enum class SocialView { Feed, Alerts }

/**
 * A pushed panel in the Social scene's state-driven navigation stack. Drives
 * [SocialState.path] through `ScenePanelHost`. Mirrors iOS `SocialRoute`.
 */
sealed interface SocialRoute {
    /** The post-detail panel, keyed by the post's id. */
    data class PostDetail(val postId: UUID) : SocialRoute

    /** A user's profile panel, keyed by the user's id (handle without `@`). */
    data class Profile(val userId: String) : SocialRoute
}

// ---------------------------------------------------------------------------
// SocialState — the plain @Stable state holder (no ViewModel)
// ---------------------------------------------------------------------------

/**
 * The single state holder driving the whole Social scene — feed, post-detail and
 * profile panels, alerts, compose/search sheets, and every feedback overlay.
 * Remembered once at the scene root (no `ViewModel`), mirroring the iOS
 * `@Observable SocialModel`.
 *
 * All collections are [mutableStateListOf] and all scalars [mutableStateOf], so
 * reads inside composables subscribe and mutations recompose. Value types
 * ([SocialUser]/[SocialPost]) are replaced in place via [withUser]/[withPost].
 */
@Stable
class SocialState {
    // Data
    val users = mutableStateListOf<SocialUser>().apply { addAll(seedUsers) }
    val posts = mutableStateListOf<SocialPost>()
    val replies = mutableStateListOf<SocialReply>()
    val notifications = mutableStateListOf<SocialNotification>()

    // Navigation
    var currentView by mutableStateOf(SocialView.Feed)

    /** State-driven stack of pushed panels (post detail, profile), owned here. */
    val path = mutableStateListOf<SocialRoute>()

    /**
     * One-shot flag: `true` for the recomposition after a FAB nav switch so the
     * panel stack collapses instantly (no slide) rather than sliding a stale panel
     * out first — the iOS `imInstantSwitch` twin. The scene clears it right after.
     */
    var snapNextPanel by mutableStateOf(false)

    var postMenuId by mutableStateOf<UUID?>(null)
    var composing by mutableStateOf(false)

    /** When non-null, the compose sheet is in reply mode and targets this post. */
    var replyTargetId by mutableStateOf<UUID?>(null)
    var searching by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    // Lifecycle
    var isLoading by mutableStateOf(true)
    private var hasLoaded = false
    private var refreshIndex = 0

    // Feedback overlays
    var toast by mutableStateOf<AMToastItem?>(null)
    var banner by mutableStateOf<AMBannerItem?>(null)
    var alert by mutableStateOf<AMAlertItem?>(null)

    init {
        // Replies / notifications reference posts[i].id, so posts are seeded first.
        val seeded = seedPosts()
        posts.addAll(seeded)
        replies.addAll(seedReplies(seeded))
        notifications.addAll(seedNotifications(seeded))
    }

    // ── Lookups ─────────────────────────────────────────────────────────────

    fun user(id: String): SocialUser = users.firstOrNull { it.id == id } ?: unknownUser

    val selfUser: SocialUser
        get() = users.firstOrNull { it.isSelf } ?: unknownUser

    val visibleFeed: List<SocialPost>
        get() = posts.filter { !user(it.authorId).isMuted }

    val unreadCount: Int
        get() = notifications.count { !it.isRead }

    val postMenuPost: SocialPost?
        get() = postMenuId?.let { id -> posts.firstOrNull { it.id == id } }

    fun repliesFor(postId: UUID): List<SocialReply> = replies.filter { it.postId == postId }

    fun replyCount(postId: UUID): Int = repliesFor(postId).size

    fun postsBy(userId: String): List<SocialPost> = posts.filter { it.authorId == userId }

    fun post(postId: UUID): SocialPost? = posts.firstOrNull { it.id == postId }

    /**
     * Posts whose text, author name, or handle contains the trimmed query
     * (case-insensitive). An empty query returns nothing. Mirrors iOS `searchResults`.
     */
    val searchResults: List<SocialPost>
        get() {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) return emptyList()
            return posts.filter { post ->
                val author = user(post.authorId)
                post.text.lowercase().contains(q) ||
                    author.name.lowercase().contains(q) ||
                    author.handle.lowercase().contains(q)
            }
        }

    // ── Private mutation helpers ──────────────────────────────────────────────

    private fun withPost(id: UUID, transform: (SocialPost) -> SocialPost) {
        val i = posts.indexOfFirst { it.id == id }
        if (i >= 0) posts[i] = transform(posts[i])
    }

    private fun withUser(id: String, transform: (SocialUser) -> SocialUser) {
        val i = users.indexOfFirst { it.id == id }
        if (i >= 0) users[i] = transform(users[i])
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    fun openPost(postId: UUID) {
        path.add(SocialRoute.PostDetail(postId))
    }

    fun openProfile(userId: String) {
        path.add(SocialRoute.Profile(userId))
    }

    fun pop() {
        if (path.isNotEmpty()) path.removeAt(path.lastIndex)
    }

    /** Switches to [SocialView.Feed] and pops any open panel, instantly (no slide). */
    fun showFeed() {
        snapNextPanel = true
        currentView = SocialView.Feed
        path.clear()
    }

    /** Switches to [SocialView.Alerts] and pops any open panel, instantly (no slide). */
    fun showAlerts() {
        snapNextPanel = true
        currentView = SocialView.Alerts
        path.clear()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    suspend fun loadIfNeeded() {
        if (hasLoaded) return
        hasLoaded = true
        delay(1200)
        isLoading = false
    }

    suspend fun refresh() {
        delay(1000)
        if (refreshPool.isEmpty()) return
        val entry = refreshPool[refreshIndex % refreshPool.size]
        refreshIndex += 1
        posts.add(
            0,
            SocialPost(authorId = entry.first, text = entry.second, timestamp = "now", likes = 0, reposts = 0),
        )
        toast = AMToastItem(text = "Feed updated", icon = Icons.Filled.Refresh)
    }

    // ── Post interactions ─────────────────────────────────────────────────────

    fun toggleLike(postId: UUID) {
        withPost(postId) { post ->
            val liked = !post.likedByMe
            post.copy(likedByMe = liked, likes = post.likes + if (liked) 1 else -1)
        }
    }

    fun toggleRepost(postId: UUID) {
        withPost(postId) { post ->
            val reposted = !post.repostedByMe
            post.copy(repostedByMe = reposted, reposts = post.reposts + if (reposted) 1 else -1)
        }
    }

    /**
     * Records the "Copied" toast and closes the post menu. The actual clipboard
     * write is performed by the caller (which owns `LocalClipboardManager`), since
     * a plain state holder cannot read a CompositionLocal.
     */
    fun copyText() {
        postMenuId = null
        toast = AMToastItem(text = "Copied", icon = Icons.Filled.ContentCopy)
    }

    fun muteAuthor(postId: UUID) {
        val post = posts.firstOrNull { it.id == postId } ?: return
        val author = user(post.authorId)
        val hidden = postsBy(author.id).size
        withUser(author.id) { it.copy(isMuted = true) }
        postMenuId = null
        banner = AMBannerItem(
            title = "Muted ${author.handle}",
            message = "$hidden post${if (hidden == 1) "" else "s"} hidden from your feed.",
            style = AMBannerStyle.Info,
        )
    }

    fun confirmDelete(postId: UUID) {
        postMenuId = null
        alert = AMAlertItem(
            title = "Delete post?",
            message = "This removes the post from the feed.",
            confirm = AMAlertAction(label = "Delete", role = AMButtonRole.Destructive) {
                deletePost(postId)
            },
            cancel = AMAlertAction(label = "Cancel"),
        )
    }

    private fun deletePost(postId: UUID) {
        posts.removeAll { it.id == postId }
        replies.removeAll { it.postId == postId }
        notifications.removeAll { it.postId == postId }
        // Pop the detail panel if it's showing the post being deleted.
        path.removeAll { it == SocialRoute.PostDetail(postId) }
        toast = AMToastItem(text = "Post deleted", icon = Icons.Filled.Delete)
    }

    // ── Compose & reply ───────────────────────────────────────────────────────

    fun addPost(text: String) {
        posts.add(
            0,
            SocialPost(authorId = selfUser.id, text = text, timestamp = "now", likes = 0, reposts = 0),
        )
        composing = false
        toast = AMToastItem(text = "Posted", icon = Icons.Filled.CheckCircle)
    }

    /** Opens the compose sheet in reply mode, targeting [postId]. */
    fun beginReply(postId: UUID) {
        replyTargetId = postId
        composing = true
    }

    fun addReply(text: String) {
        val postId = replyTargetId ?: return
        replies.add(SocialReply(postId = postId, authorId = selfUser.id, text = text, timestamp = "now"))
        composing = false
        replyTargetId = null
        toast = AMToastItem(text = "Reply added", icon = Icons.AutoMirrored.Filled.Comment)
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /** Closes the search sheet and opens the chosen post's detail. */
    fun openSearchResult(postId: UUID) {
        searching = false
        searchQuery = ""
        path.clear()
        path.add(SocialRoute.PostDetail(postId))
    }

    // ── Profile & notifications ───────────────────────────────────────────────

    fun toggleFollow(userId: String) {
        withUser(userId) { it.copy(isFollowed = !it.isFollowed) }
        val u = user(userId)
        toast = AMToastItem(
            text = if (u.isFollowed) "Following ${u.handle}" else "Unfollowed ${u.handle}",
            icon = if (u.isFollowed) Icons.Filled.PersonAdd else Icons.Filled.PersonRemove,
        )
    }

    fun open(notification: SocialNotification) {
        val i = notifications.indexOfFirst { it.id == notification.id }
        if (i >= 0) notifications[i] = notifications[i].copy(isRead = true)
        when (notification.kind) {
            SocialNotificationKind.Follow -> path.add(SocialRoute.Profile(notification.actorId))
            SocialNotificationKind.Like,
            SocialNotificationKind.Repost,
            SocialNotificationKind.Reply,
            -> {
                val postId = notification.postId ?: return
                if (posts.any { it.id == postId }) path.add(SocialRoute.PostDetail(postId))
            }
        }
    }

    // ── Seed data (VERBATIM from iOS SocialModel) ─────────────────────────────

    companion object {
        private val unknownUser = SocialUser(
            id = "unknown", name = "Unknown", handle = "@unknown", initials = "?",
            avatarUrl = null, bio = "", joined = "", location = "", link = "",
            followers = 0, following = 0,
        )

        /**
         * Wikimedia Commons portrait by file name. The file name is already
         * percent-encoded; `Special:FilePath` resolves to the current file and
         * `width` thumbnails it. Mirrors iOS `commons(_:)`.
         */
        private fun commons(file: String): String =
            "https://commons.wikimedia.org/wiki/Special:FilePath/$file?width=320"

        private fun picsum(seed: String): String = "https://picsum.photos/seed/$seed/600/600"

        val seedUsers: List<SocialUser> = listOf(
            SocialUser(
                id = "countess", name = "Ada Lovelace", handle = "@countess", initials = "AL",
                avatarUrl = commons("Ada_Lovelace_daguerreotype_by_Antoine_Claudet_1843_-_cropped.png"),
                bio = "Notes on analytical engines. Poetry on weekends.",
                joined = "March 1836", location = "London", link = "analyticalengine.io",
                followers = 842, following = 121, isSelf = true,
            ),
            SocialUser(
                id = "amazinggrace", name = "Grace Hopper", handle = "@amazinggrace", initials = "GH",
                avatarUrl = commons("Commodore_Grace_M._Hopper%2C_USN_%28covered%29_head_and_shoulders_crop.jpg"),
                bio = "Compilers, nanoseconds, and the occasional moth.",
                joined = "December 1943", location = "Arlington, VA", link = "cobol.dev",
                followers = 1893, following = 77,
            ),
            SocialUser(
                id = "cshannon", name = "Claude Shannon", handle = "@cshannon", initials = "CS",
                avatarUrl = commons("C.E._Shannon._Tekniska_museet_43069_%282x3_crop%29.jpg"),
                bio = "Information, juggling, unicycles. Mostly entropy.",
                joined = "July 1948", location = "Murray Hill, NJ", link = "bell-labs.example",
                followers = 2048, following = 64,
            ),
            SocialUser(
                id = "aturing", name = "Alan Turing", handle = "@aturing", initials = "AT",
                avatarUrl = commons("Alan_turing_header.jpg"),
                bio = "Machines, morphogenesis, marathon running.",
                joined = "June 1936", location = "Bletchley", link = "halting.example",
                followers = 3141, following = 89,
            ),
            SocialUser(
                id = "mhamilton", name = "Margaret Hamilton", handle = "@mhamilton", initials = "MH",
                avatarUrl = commons("Margaret_Hamilton_1995.jpg"),
                bio = "Software engineering — I named it.",
                joined = "August 1961", location = "Cambridge, MA", link = "apollo11.example",
                followers = 1969, following = 102,
            ),
            SocialUser(
                id = "kjohnson", name = "Katherine Johnson", handle = "@kjohnson", initials = "KJ",
                avatarUrl = commons("Katherine_Johnson_1983.jpg"),
                bio = "Orbital mechanics by hand. Check my numbers.",
                joined = "June 1953", location = "Hampton, VA", link = "friendship7.example",
                followers = 1620, following = 58,
            ),
            SocialUser(
                id = "jvn", name = "John von Neumann", handle = "@jvn", initials = "JN",
                avatarUrl = commons("JohnvonNeumann-LosAlamos.gif"),
                bio = "Architecture sketches on napkins.",
                joined = "June 1945", location = "Princeton, NJ", link = "edvac.example",
                followers = 2718, following = 33,
            ),
            SocialUser(
                id = "bliskov", name = "Barbara Liskov", handle = "@bliskov", initials = "BL",
                avatarUrl = commons("Barbara_Liskov_MIT_computer_scientist_2010.jpg"),
                bio = "Substitution without surprises.",
                joined = "September 1968", location = "Cambridge, MA", link = "clu.example",
                followers = 1407, following = 95,
            ),
            SocialUser(
                id = "dknuth", name = "Donald Knuth", handle = "@dknuth", initials = "DK",
                avatarUrl = commons("Donald_Ervin_Knuth_%28cropped%29.jpg"),
                bio = "TAOCP volume 4 is coming along nicely.",
                joined = "January 1962", location = "Stanford, CA", link = "tex.example",
                followers = 4096, following = 1,
            ),
            SocialUser(
                id = "dmr", name = "Dennis Ritchie", handle = "@dmr", initials = "DR",
                avatarUrl = commons("Dennis_Ritchie_2011.jpg"),
                bio = "hello, world",
                joined = "1969", location = "Murray Hill, NJ", link = "unix.example",
                followers = 2501, following = 42,
            ),
        )

        /** Seed order is load-bearing: index 0 is Ada's post — notifications reference it. */
        fun seedPosts(): List<SocialPost> = listOf(
            SocialPost(
                authorId = "countess",
                text = "Just shipped a draft of the analytical engine notes. The bit about chess feels right; the bit about poetry needs a rewrite before tomorrow.",
                timestamp = "2h", likes = 48, reposts = 3,
            ),
            SocialPost(
                authorId = "amazinggrace",
                text = "Found a moth in relay 70 today. The actual bug. Taped it into the logbook so the night shift wouldn't lose its mind looking for the cause.",
                imageUrl = picsum("iosmtrls-social-moth"),
                timestamp = "5h", likes = 92, reposts = 18,
            ),
            SocialPost(
                authorId = "cshannon",
                text = "Juggling on the unicycle again in the hallway. Useful for thinking about information loss — every dropped pin is a bit you can't get back.",
                timestamp = "8h", likes = 167, reposts = 14,
            ),
            SocialPost(
                authorId = "aturing",
                text = "The question \"can machines think?\" is meaningless without first defining what we mean by thinking. Spent the afternoon on a more tractable replacement.",
                timestamp = "1d", likes = 134, reposts = 22,
            ),
            SocialPost(
                authorId = "mhamilton",
                text = "Apollo software stack printed and stacked next to me, taller than my four-year-old. Reviewing the abort routines tonight.",
                imageUrl = picsum("iosmtrls-social-apollo"),
                timestamp = "2d", likes = 31, reposts = 4,
            ),
            SocialPost(
                authorId = "kjohnson",
                text = "Recomputed the Mercury trajectory by hand to double-check the IBM. Numbers agreed. Glenn says he won't fly unless I confirm.",
                timestamp = "3d", likes = 88, reposts = 27,
            ),
            SocialPost(
                authorId = "jvn",
                text = "Stored-program architecture sketch on the back of a napkin at lunch. The whole thing fits on one page if you don't draw the bus.",
                imageUrl = picsum("iosmtrls-social-napkin"),
                timestamp = "4d", likes = 210, reposts = 41,
            ),
            SocialPost(
                authorId = "bliskov",
                text = "If S is a subtype of T, anywhere a T is expected an S should work without surprises. Re-derived the principle on the whiteboard with the seminar group today.",
                timestamp = "5d", likes = 76, reposts = 9,
            ),
            SocialPost(
                authorId = "dknuth",
                text = "Premature optimization is still the root of all evil — but mature optimization, after profiling, is the only path to a fast inner loop.",
                timestamp = "6d", likes = 245, reposts = 36,
            ),
            SocialPost(
                authorId = "dmr",
                text = "Spent the morning convincing a colleague that void* is not an admission of defeat. It's a contract that says: I'll know the type at the boundary.",
                timestamp = "1w", likes = 64, reposts = 7,
            ),
        )

        fun seedReplies(posts: List<SocialPost>): List<SocialReply> = listOf(
            // Ada's post (index 0) — 3 replies
            SocialReply(postId = posts[0].id, authorId = "amazinggrace",
                text = "Ship the chess bit. The poetry will follow — it always does.", timestamp = "1h"),
            SocialReply(postId = posts[0].id, authorId = "aturing",
                text = "Keen to read the chess section. Machines playing games is the right wedge.", timestamp = "1h"),
            SocialReply(postId = posts[0].id, authorId = "mhamilton",
                text = "Draft now, rewrite at altitude. Works for software too.", timestamp = "30m"),
            // Grace's moth post (index 1) — 2 replies
            SocialReply(postId = posts[1].id, authorId = "countess",
                text = "First actual bug. The logbook entry will outlive us all.", timestamp = "4h"),
            SocialReply(postId = posts[1].id, authorId = "dknuth",
                text = "Filed under: literal debugging.", timestamp = "3h"),
            // Shannon's post (index 2) — 2 replies
            SocialReply(postId = posts[2].id, authorId = "jvn",
                text = "Channel capacity of a hallway juggler — there's a paper in that.", timestamp = "7h"),
            SocialReply(postId = posts[2].id, authorId = "kjohnson",
                text = "The dropped pins are recoverable if you model the bounce.", timestamp = "6h"),
            // Turing's post (index 3) — 2 replies
            SocialReply(postId = posts[3].id, authorId = "cshannon",
                text = "Define the game, then play it. Imitation seems tractable.", timestamp = "20h"),
            SocialReply(postId = posts[3].id, authorId = "bliskov",
                text = "Behavioral definitions beat introspective ones every time.", timestamp = "18h"),
            // von Neumann's post (index 6) — 1 reply
            SocialReply(postId = posts[6].id, authorId = "dmr",
                text = "One page, no bus. Sounds like a spec I'd actually read.", timestamp = "3d"),
            // Knuth's post (index 8) — 1 reply
            SocialReply(postId = posts[8].id, authorId = "amazinggrace",
                text = "Profile first. A nanosecond saved in the wrong loop is still wasted.", timestamp = "5d"),
        )

        fun seedNotifications(posts: List<SocialPost>): List<SocialNotification> {
            val adaPost = posts[0].id
            return listOf(
                SocialNotification(kind = SocialNotificationKind.Like, actorId = "amazinggrace",
                    postId = adaPost, timestamp = "1h", isRead = false),
                SocialNotification(kind = SocialNotificationKind.Reply, actorId = "aturing",
                    postId = adaPost, timestamp = "1h", isRead = false),
                SocialNotification(kind = SocialNotificationKind.Follow, actorId = "cshannon",
                    timestamp = "2h", isRead = false),
                SocialNotification(kind = SocialNotificationKind.Repost, actorId = "kjohnson",
                    postId = adaPost, timestamp = "3h", isRead = true),
                SocialNotification(kind = SocialNotificationKind.Like, actorId = "dknuth",
                    postId = adaPost, timestamp = "5h", isRead = true),
                SocialNotification(kind = SocialNotificationKind.Follow, actorId = "bliskov",
                    timestamp = "1d", isRead = true),
                SocialNotification(kind = SocialNotificationKind.Like, actorId = "mhamilton",
                    postId = adaPost, timestamp = "1d", isRead = true),
                SocialNotification(kind = SocialNotificationKind.Reply, actorId = "jvn",
                    postId = adaPost, timestamp = "2d", isRead = true),
            )
        }

        /** Pull-to-refresh draws from this pool, cycling via `refreshIndex`. Mirrors iOS `refreshPool`. */
        val refreshPool: List<Pair<String, String>> = listOf(
            "amazinggrace" to "A compiler should let you say it in English and argue about the semicolons later.",
            "dknuth" to "Programs are meant to be read by humans and only incidentally for computers to execute. Drafting chapter notes.",
            "cshannon" to "Built a mechanical mouse that learns the maze. It cheats — it remembers.",
        )
    }
}
