package org.moinex.ui.common.search

import java.text.Normalizer

object SearchMatcher {
    private val diacritics = Regex("\\p{InCombiningDiacriticalMarks}+")

    fun normalize(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .lowercase()
            .trim()

    /**
     * Actions matching [query] ignoring case and accents: title prefix first, then title contains,
     * then synonym contains. An empty query returns every action in group order.
     */
    fun rank(
        query: String,
        actions: List<SearchAction>,
        translate: (String) -> String,
    ): List<SearchAction> {
        val q = normalize(query)
        if (q.isEmpty()) return actions.sortedBy { it.group.ordinal }

        return actions
            .mapNotNull { action ->
                val title = normalize(action.label ?: translate(action.titleKey))
                val keywords = if (action.label == null) normalize(translate(action.keywordsKey)) else ""
                val score =
                    when {
                        title.startsWith(q) -> 0
                        title.contains(q) -> 1
                        keywords.split(",").any { it.trim().contains(q) } -> 2
                        else -> null
                    }
                score?.let { action to it }
            }.sortedWith(compareBy({ it.second }, { it.first.group.ordinal }))
            .map { it.first }
    }
}
