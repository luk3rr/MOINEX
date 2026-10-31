package org.moinex.ui.common.search

import org.moinex.common.ui.Icons

/**
 * What the search needs from the main window to run its actions.
 */
interface SearchHost {
    fun runScreenAction(
        screen: SearchScreen,
        tabIndex: Int?,
        index: Int,
    )

    fun showScreen(
        screen: SearchScreen,
        tabIndex: Int? = null,
    )

    fun openEntry(entry: SearchEntry)

    fun toggleTheme()

    fun openCalendar()

    fun openCalculator()

    fun toggleNotifications()
}

enum class SearchScreen {
    HOME,
    WALLETS,
    CREDIT_CARDS,
    TRANSACTIONS,
    GOALS_PLANS,
    SAVINGS,
    ANNUAL_SUMMARY,
    AI_CHAT,
    SETTINGS,
}

enum class SearchEntry { EXPENSE, INCOME, TRANSFER }

/**
 * The 23 actions of the global search: screens, tabs, new entries and app commands.
 */
class SearchActionCatalog(
    private val host: SearchHost,
) {
    fun actions(): List<SearchAction> =
        listOf(
            screen("screen.home", Icons.SIDEBAR_HOME, SearchScreen.HOME),
            screen("screen.wallets", Icons.SIDEBAR_WALLET, SearchScreen.WALLETS),
            screen("screen.creditCards", Icons.SIDEBAR_CREDIT_CARD, SearchScreen.CREDIT_CARDS),
            screen("screen.transactions", Icons.SIDEBAR_TRANSACTION, SearchScreen.TRANSACTIONS),
            screen("screen.goalsPlans", Icons.SIDEBAR_GOAL, SearchScreen.GOALS_PLANS),
            screen("screen.savings", Icons.SIDEBAR_SAVINGS, SearchScreen.SAVINGS),
            screen("screen.annualSummary", Icons.SIDEBAR_SUMMARY, SearchScreen.ANNUAL_SUMMARY),
            screen("screen.aiChat", Icons.SIDEBAR_AI_CHAT, SearchScreen.AI_CHAT),
            screen("screen.settings", Icons.SIDEBAR_SETTINGS, SearchScreen.SETTINGS),
            tab("tab.goals", Icons.GOAL, SearchScreen.GOALS_PLANS, GOALS_TAB),
            tab("tab.plans", Icons.LIST, SearchScreen.GOALS_PLANS, PLANS_TAB),
            tab("tab.fire", Icons.PROFIT, SearchScreen.GOALS_PLANS, FIRE_TAB),
            tab("tab.wishlist", Icons.PURCHASED, SearchScreen.GOALS_PLANS, WISHLIST_TAB),
            tab("tab.savingsOverview", Icons.SIDEBAR_SAVINGS, SearchScreen.SAVINGS, OVERVIEW_TAB),
            tab("tab.stocksFunds", Icons.WALLET_TYPE_BROKER, SearchScreen.SAVINGS, STOCKS_FUNDS_TAB),
            tab("tab.bonds", Icons.COIN, SearchScreen.SAVINGS, BONDS_TAB),
            entry("entry.expense", Icons.EXPENSE, SearchEntry.EXPENSE),
            entry("entry.income", Icons.INCOME, SearchEntry.INCOME),
            entry("entry.transfer", Icons.TRANSFER, SearchEntry.TRANSFER),
            SearchAction("app.toggleTheme", SearchGroup.APP, Icons.SHOW) { host.toggleTheme() },
            SearchAction("app.calendar", SearchGroup.APP, Icons.CALENDAR) { host.openCalendar() },
            SearchAction("app.calculator", SearchGroup.APP, Icons.CALCULATOR) { host.openCalculator() },
            SearchAction("app.notifications", SearchGroup.APP, Icons.NOTIFICATION) { host.toggleNotifications() },
        )

    fun screenActions(translate: (String) -> String): List<SearchAction> =
        ScreenActionEntries.all.map { entry ->
            SearchAction(
                id = "screen.${entry.screen}.${entry.tab}.${entry.index}",
                group = SearchGroup.SCREEN_ACTIONS,
                icon = Icons.ADD,
                label = translate(entry.labelKey),
                context = entry.contextKeys.joinToString(" · ") { translate(it) },
                run = { host.runScreenAction(entry.screen, entry.tab, entry.index) },
            )
        }

    private fun screen(
        id: String,
        icon: String,
        screen: SearchScreen,
    ) = SearchAction(id, SearchGroup.SCREENS, icon) { host.showScreen(screen) }

    private fun tab(
        id: String,
        icon: String,
        screen: SearchScreen,
        tabIndex: Int,
    ) = SearchAction(id, SearchGroup.TABS, icon) { host.showScreen(screen, tabIndex) }

    private fun entry(
        id: String,
        icon: String,
        entry: SearchEntry,
    ) = SearchAction(id, SearchGroup.ENTRIES, icon) { host.openEntry(entry) }

    companion object {
        const val GOALS_TAB = 0
        const val PLANS_TAB = 1
        const val WISHLIST_TAB = 2
        const val FIRE_TAB = 3
        const val OVERVIEW_TAB = 0
        const val STOCKS_FUNDS_TAB = 1
        const val BONDS_TAB = 2
    }
}
