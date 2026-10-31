package org.moinex.ui.common.search

enum class SearchGroup(
    val titleKey: String,
) {
    SCREENS("search.group.screens"),
    TABS("search.group.tabs"),
    ENTRIES("search.group.entries"),
    APP("search.group.app"),
    SCREEN_ACTIONS("search.group.screenActions"),
}

/**
 * An action the global search can run. Its title and synonyms come from `search.action.<id>` and
 * `search.action.<id>.keywords` in the i18n bundles.
 */
data class SearchAction(
    val id: String,
    val group: SearchGroup,
    val icon: String,
    val label: String? = null,
    val run: () -> Unit,
) {
    val titleKey: String get() = "search.action.$id"
    val keywordsKey: String get() = "search.action.$id.keywords"
}
