package com.ideamtrls.androidmtrls.demo

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import java.text.Normalizer
import java.util.Locale

/**
 * One catalog entry: what the list shows, what search matches, and how to build the entry's page.
 * Declared as `internal val <Name>Entry` in the page's own file and listed in its category's
 * `<Category>Pages.entries`.
 *
 * Identity is [id] alone, so an entry works as a navigation key.
 */
@Immutable
internal class ComponentEntry(
    /**
     * Stable id: the lowerCamelCase of [title] ("Floating action button" →
     * `floatingActionButton`). Showcase scenes add a `Scene` suffix so they never collide with a
     * component ("Calendar" → `calendarScene`).
     */
    val id: String,
    /** The category whose `<Category>Pages.entries` lists this entry. */
    val category: CatalogCategory,
    /** Full component name in sentence case, no abbreviations. Also the page's top bar title. */
    val title: String,
    /** One line, what the component is for: under the title in the list and on the page. */
    val summary: String,
    /** The glyph on the list row. */
    @param:DrawableRes val icon: Int,
    /** Extra search terms: API spellings (`AMButton`, `amCard`) and synonyms. */
    val keywords: List<String> = emptyList(),
    /** Builds the entry's page: a [ComponentPage], or a scene for Showcase. */
    val page: @Composable () -> Unit,
) {
    /** Case- and accent-insensitive match over title, summary, category title and keywords. */
    fun matches(query: String): Boolean {
        val wanted = query.trim().folded()
        if (wanted.isEmpty()) return true
        return (listOf(title, summary, category.title) + keywords).any { it.folded().contains(wanted) }
    }

    override fun equals(other: Any?): Boolean = other is ComponentEntry && other.id == id

    override fun hashCode(): Int = id.hashCode()
}

private val CombiningMarks = Regex("\\p{Mn}+")

private fun String.folded(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(CombiningMarks, "").lowercase(Locale.ROOT)

/**
 * Every registered entry, in list order: each category's own `<Category>Pages.entries`,
 * concatenated in [CatalogCategory] order. Nothing here changes when a page is added; the
 * category file owns it. Debug builds fail fast on an entry listed under the wrong category and
 * on a duplicate id.
 */
internal object CatalogRegistry {
    val all: List<ComponentEntry> by lazy {
        CatalogCategory.entries.flatMap { category ->
            category.componentEntries.onEach { entry ->
                if (BuildConfig.DEBUG) {
                    check(entry.category == category) {
                        "${entry.id} declares category ${entry.category} but is listed in ${category.title}'s entries"
                    }
                }
            }
        }
    }

    private val byId: Map<String, ComponentEntry> by lazy {
        val byId = LinkedHashMap<String, ComponentEntry>()
        for (entry in all) {
            val duplicate = byId.putIfAbsent(entry.id, entry)
            if (BuildConfig.DEBUG) check(duplicate == null) { "Duplicate catalog entry id '${entry.id}'" }
        }
        byId
    }

    /** The registered entry with this id, if any. */
    fun entry(id: String): ComponentEntry? = byId[id]
}
