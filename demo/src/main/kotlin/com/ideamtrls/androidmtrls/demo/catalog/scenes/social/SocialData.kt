package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

/** What a post is about. Search filters by it, and the post panel shows it as a tag. Composed posts have none. */
internal enum class SocialTopic(val title: String) {
    Engines("Engines"),
    Bugs("Bugs"),
    Information("Information"),
    Chess("Chess"),
    Space("Space"),
    Languages("Languages"),
}

/** A person in the scene. [id] is the handle without "@". */
@Immutable
internal data class SocialUser(
    val id: String,
    val name: String,
    val handle: String,
    val avatarUrl: String?,
    val bio: String,
    val joined: String,
    val location: String,
    val website: String,
    val followers: Int,
    val following: Int,
    val isSelf: Boolean = false,
) {
    /** The first word of the name, for "When Grace posts, it shows up here." */
    val firstName: String
        get() = name.substringBefore(' ')
}

/**
 * A post. [likes] and [reposts] are the counts before you act on it; the model adds your own like
 * and repost on top. [imageAspectRatio] is the photo's width over its height.
 */
@Immutable
internal data class SocialPost(
    val id: String,
    val authorId: String,
    val text: String,
    val topic: SocialTopic?,
    val timestamp: String,
    val likes: Int,
    val reposts: Int,
    val imageUrl: String? = null,
    val imageDescription: String? = null,
    val imageAspectRatio: Float = 4f / 3f,
)

/** A reply under a post. */
@Immutable
internal data class SocialReply(
    val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val timestamp: String,
)

/** What an alert says someone did, and the glyph at its end. Mentions lists mentions and replies. */
internal enum class SocialNotificationKind(val verb: String, @param:DrawableRes val icon: Int, val isMention: Boolean) {
    Like("liked your post", R.drawable.ic_favorite_filled, false),
    Repost("reposted your post", R.drawable.ic_repeat, false),
    Reply("replied to your post", R.drawable.ic_chat_bubble_filled, true),
    Mention("mentioned you", R.drawable.ic_alternate_email, true),
    Follow("followed you", R.drawable.ic_person_add, false),
}

/**
 * An alert: who did what, about which post (none for a follow), the words that mention or reply to
 * you, when, and whether it starts read.
 */
@Immutable
internal data class SocialNotification(
    val id: String,
    val kind: SocialNotificationKind,
    val actorId: String,
    val timestamp: String,
    val isRead: Boolean,
    val postId: String? = null,
    val snippet: String? = null,
)

/**
 * Everything the scene starts with: computing's pioneers posting as if they had phones. You are
 * Ada Lovelace. Posts list newest first; [refreshPool] is what pulling the feed down adds, one post
 * per pull, in order.
 */
internal object SocialSamples {
    val users: List<SocialUser> = listOf(
        SocialUser(
            "countess", "Ada Lovelace", "@countess",
            commonsPhoto("Ada_Lovelace_daguerreotype_by_Antoine_Claudet_1843_-_cropped.png"),
            bio = "Notes on analytical engines. Poetry on weekends.",
            joined = "March 1836", location = "London", website = "analyticalengine.io",
            followers = 842, following = 121, isSelf = true,
        ),
        SocialUser(
            "amazinggrace", "Grace Hopper", "@amazinggrace",
            commonsPhoto("Commodore_Grace_M._Hopper,_USN_(covered)_head_and_shoulders_crop.jpg"),
            bio = "Compilers, nanoseconds, and the occasional moth.",
            joined = "December 1943", location = "Arlington, VA", website = "cobol.dev",
            followers = 1893, following = 77,
        ),
        SocialUser(
            "cshannon", "Claude Shannon", "@cshannon",
            commonsPhoto("C.E._Shannon._Tekniska_museet_43069_(2x3_crop).jpg"),
            bio = "Information, juggling, unicycles. Mostly entropy.",
            joined = "July 1948", location = "Murray Hill, NJ", website = "bell-labs.example",
            followers = 2048, following = 64,
        ),
        SocialUser(
            "aturing", "Alan Turing", "@aturing",
            commonsPhoto("Alan_turing_header.jpg"),
            bio = "Machines, morphogenesis, marathon running.",
            joined = "June 1936", location = "Bletchley", website = "halting.example",
            followers = 3141, following = 89,
        ),
        SocialUser(
            "mhamilton", "Margaret Hamilton", "@mhamilton",
            commonsPhoto("Margaret_Hamilton_1995.jpg"),
            bio = "Software engineering — I named it.",
            joined = "August 1961", location = "Cambridge, MA", website = "apollo11.example",
            followers = 1969, following = 102,
        ),
        SocialUser(
            "kjohnson", "Katherine Johnson", "@kjohnson",
            commonsPhoto("Katherine_Johnson_1983.jpg"),
            bio = "Orbital mechanics by hand. Check my numbers.",
            joined = "June 1953", location = "Hampton, VA", website = "friendship7.example",
            followers = 1620, following = 58,
        ),
        SocialUser(
            "jvn", "John von Neumann", "@jvn",
            commonsPhoto("JohnvonNeumann-LosAlamos.gif"),
            bio = "Architecture sketches on napkins.",
            joined = "June 1945", location = "Princeton, NJ", website = "edvac.example",
            followers = 2718, following = 33,
        ),
        SocialUser(
            "bliskov", "Barbara Liskov", "@bliskov",
            commonsPhoto("Barbara_Liskov_MIT_computer_scientist_2010.jpg"),
            bio = "Substitution without surprises.",
            joined = "September 1968", location = "Cambridge, MA", website = "clu.example",
            followers = 1407, following = 95,
        ),
        SocialUser(
            "dknuth", "Donald Knuth", "@dknuth",
            commonsPhoto("Donald_Ervin_Knuth_(cropped).jpg"),
            bio = "TAOCP volume 4 is coming along nicely.",
            joined = "January 1962", location = "Stanford, CA", website = "tex.example",
            followers = 4096, following = 1,
        ),
        SocialUser(
            "dmr", "Dennis Ritchie", "@dmr",
            commonsPhoto("Dennis_Ritchie_2011.jpg"),
            bio = "hello, world",
            joined = "1969", location = "Murray Hill, NJ", website = "unix.example",
            followers = 2501, following = 42,
        ),
    )

    private val shannonChess = SocialPost(
        "shannon-chess", "cshannon",
        "Draft of the computer chess paper is out. There are about 10¹²⁰ possible games, so a machine can't list " +
            "them all. It has to choose, the way a player does. @countess saw this coming.",
        SocialTopic.Chess, timestamp = "41m", likes = 74, reposts = 19,
    )
    private val johnsonOrbit = SocialPost(
        "johnson-orbit", "kjohnson",
        "Glenn asked for the numbers again before he'll fly. Running the whole orbit by hand one more time, launch " +
            "to splashdown.",
        SocialTopic.Space, timestamp = "27m", likes = 112, reposts = 31,
    )
    private val liskovClu = SocialPost(
        "liskov-clu", "bliskov",
        "CLU has iterators now. Wrote one this morning, and the loop that uses it reads exactly like the loop I meant.",
        SocialTopic.Languages, timestamp = "15m", likes = 39, reposts = 6,
    )
    private val adaNotes = SocialPost(
        "ada-notes", "countess",
        "Just shipped a draft of the analytical engine notes. The bit about chess feels right; the bit about poetry " +
            "needs a rewrite before tomorrow.",
        SocialTopic.Engines, timestamp = "2h", likes = 48, reposts = 3,
    )
    private val hopperMoth = SocialPost(
        "hopper-moth", "amazinggrace",
        "Found a moth in relay 70 today. The actual bug. Taped it into the logbook so the night shift wouldn't lose " +
            "its mind looking for the cause. @countess, one for your notes.",
        SocialTopic.Bugs, timestamp = "3h", likes = 92, reposts = 18,
        imageUrl = commonsPhoto("First_Computer_Bug,_1945.jpg"),
        imageDescription = "The logbook page with the moth taped in",
    )
    private val hamiltonApollo = SocialPost(
        "hamilton-apollo", "mhamilton",
        "Apollo software stack printed and stacked next to me, taller than my four-year-old. Reviewing the abort " +
            "routines tonight.",
        SocialTopic.Space, timestamp = "5h", likes = 31, reposts = 4,
        imageUrl = commonsPhoto("Margaret_Hamilton_-_restoration.jpg"),
        imageDescription = "Margaret Hamilton beside the printed Apollo code",
        imageAspectRatio = 4f / 5f,
    )
    private val turingThink = SocialPost(
        "turing-think", "aturing",
        "The question \"can machines think?\" is meaningless without first defining what we mean by thinking. Spent " +
            "the afternoon on a more tractable replacement.",
        SocialTopic.Information, timestamp = "12h", likes = 134, reposts = 22,
    )
    private val jvnNapkin = SocialPost(
        "jvn-napkin", "jvn",
        "Stored-program architecture sketch on the back of a napkin at lunch. The whole thing fits on one page if you " +
            "don't draw the bus.",
        SocialTopic.Engines, timestamp = "1d", likes = 210, reposts = 41,
    )
    private val adaNoteG = SocialPost(
        "ada-note-g", "countess",
        "Note G is finished: the full table of operations for the Engine to compute the Bernoulli numbers. Mr. " +
            "Babbage thinks my notes run longer than the paper they annotate. They do.",
        SocialTopic.Engines, timestamp = "4d", likes = 156, reposts = 21,
    )

    /** Newest first. */
    val posts: List<SocialPost> = listOf(
        liskovClu, johnsonOrbit, shannonChess, adaNotes, hopperMoth, hamiltonApollo, turingThink, jvnNapkin, adaNoteG,
    )

    private val turingOnAda = SocialReply(
        "r-turing-notes", adaNotes.id, "aturing",
        "Keen to read the chess section. Machines playing games is the right wedge.", "1h",
    )
    private val jvnOnNoteG = SocialReply(
        "r-jvn-note-g", adaNoteG.id, "jvn",
        "A program is exactly as long as it is. Keep the table; it's the proof.", "2d",
    )

    val replies: List<SocialReply> = listOf(
        SocialReply(
            "r-turing-chess", shannonChess.id, "aturing",
            "Mine only looks two moves ahead and still loses. Choosing is the hard part.", "30m",
        ),
        SocialReply(
            "r-ada-chess", shannonChess.id, "countess",
            "I only said the Engine might play chess. I never promised it would play well.", "22m",
        ),
        SocialReply(
            "r-hamilton-orbit", johnsonOrbit.id, "mhamilton",
            "Numbers you'd sign your name to. That's the only kind worth flying on.", "12m",
        ),
        SocialReply(
            "r-hopper-notes", adaNotes.id, "amazinggrace",
            "Ship the chess bit. The poetry will follow — it always does.", "1h",
        ),
        turingOnAda,
        SocialReply(
            "r-hamilton-notes", adaNotes.id, "mhamilton",
            "Draft now, rewrite at altitude. Works for software too.", "30m",
        ),
        SocialReply(
            "r-ada-moth", hopperMoth.id, "countess",
            "First actual bug. The logbook entry will outlive us all.", "2h",
        ),
        SocialReply("r-knuth-moth", hopperMoth.id, "dknuth", "Filed under: literal debugging.", "1h"),
        SocialReply(
            "r-shannon-think", turingThink.id, "cshannon",
            "Define the game, then play it. Imitation seems tractable.", "10h",
        ),
        SocialReply(
            "r-liskov-think", turingThink.id, "bliskov",
            "Behavioral definitions beat introspective ones every time.", "9h",
        ),
        SocialReply(
            "r-dmr-napkin", jvnNapkin.id, "dmr",
            "One page, no bus. Sounds like a spec I'd actually read.", "20h",
        ),
        jvnOnNoteG,
    )

    /** Newest first; the four newest start unread. */
    val notifications: List<SocialNotification> = listOf(
        SocialNotification("n-hopper-like", SocialNotificationKind.Like, "amazinggrace", "12m", false, adaNotes.id),
        SocialNotification(
            "n-shannon-mention", SocialNotificationKind.Mention, "cshannon", "41m", false, shannonChess.id, shannonChess.text,
        ),
        SocialNotification(
            "n-turing-reply", SocialNotificationKind.Reply, "aturing", "1h", false, adaNotes.id, turingOnAda.text,
        ),
        SocialNotification("n-johnson-follow", SocialNotificationKind.Follow, "kjohnson", "2h", false),
        SocialNotification("n-hamilton-repost", SocialNotificationKind.Repost, "mhamilton", "3h", true, adaNoteG.id),
        SocialNotification(
            "n-hopper-mention", SocialNotificationKind.Mention, "amazinggrace", "3h", true, hopperMoth.id, hopperMoth.text,
        ),
        SocialNotification("n-knuth-like", SocialNotificationKind.Like, "dknuth", "1d", true, adaNoteG.id),
        SocialNotification("n-liskov-follow", SocialNotificationKind.Follow, "bliskov", "1d", true),
        SocialNotification(
            "n-jvn-reply", SocialNotificationKind.Reply, "jvn", "2d", true, adaNoteG.id, jvnOnNoteG.text,
        ),
        SocialNotification("n-dmr-like", SocialNotificationKind.Like, "dmr", "3d", true, adaNoteG.id),
    )

    val refreshPool: List<SocialPost> = listOf(
        SocialPost(
            "hopper-compiler", "amazinggrace",
            "A compiler should let you say it in English and argue about the semicolons later.",
            SocialTopic.Languages, timestamp = "now", likes = 0, reposts = 0,
        ),
        SocialPost(
            "knuth-literate", "dknuth",
            "Programs are meant to be read by humans and only incidentally for computers to execute. Drafting chapter " +
                "notes.",
            SocialTopic.Languages, timestamp = "now", likes = 0, reposts = 0,
        ),
        SocialPost(
            "shannon-mouse", "cshannon",
            "Built a mechanical mouse that learns the maze. It cheats — it remembers.",
            SocialTopic.Information, timestamp = "now", likes = 0, reposts = 0,
        ),
    )
}
