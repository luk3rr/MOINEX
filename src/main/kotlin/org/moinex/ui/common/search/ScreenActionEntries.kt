package org.moinex.ui.common.search

import org.moinex.common.constant.TranslationKeys

/**
 * An action of a screen's "+" button, offered by the global search. [index] is its position in the list that the
 * screen (or the [tab] of it) returns from screenActions(). [labelKey] and [contextKeys] are translation keys.
 */
data class ScreenActionEntry(
    val screen: SearchScreen,
    val tab: Int?,
    val index: Int,
    val labelKey: String,
    val contextKeys: List<String>,
)

object ScreenActionEntries {
    private const val SAVINGS_STOCKS_FUNDS_TAB = 1
    private const val SAVINGS_BONDS_TAB = 2
    private const val GOALS_TAB = 0
    private const val WISHLIST_TAB = 2

    val all: List<ScreenActionEntry> =
        listOf(
            ScreenActionEntry(
                SearchScreen.WALLETS,
                null,
                0,
                TranslationKeys.WALLET_BUTTON_ADD_WALLET,
                listOf(TranslationKeys.MAIN_WALLET),
            ),
            ScreenActionEntry(
                SearchScreen.WALLETS,
                null,
                1,
                TranslationKeys.WALLET_BUTTON_ADD_TRANSFER,
                listOf(TranslationKeys.MAIN_WALLET),
            ),
            ScreenActionEntry(
                SearchScreen.WALLETS,
                null,
                2,
                TranslationKeys.WALLET_BUTTON_TRANSFERS,
                listOf(TranslationKeys.MAIN_WALLET),
            ),
            ScreenActionEntry(
                SearchScreen.WALLETS,
                null,
                3,
                TranslationKeys.WALLET_BUTTON_WALLET_ARCHIVE,
                listOf(TranslationKeys.MAIN_WALLET),
            ),
            ScreenActionEntry(
                SearchScreen.TRANSACTIONS,
                null,
                0,
                TranslationKeys.TRANSACTION_BUTTON_ADD_INCOME,
                listOf(TranslationKeys.MAIN_TRANSACTION),
            ),
            ScreenActionEntry(
                SearchScreen.TRANSACTIONS,
                null,
                1,
                TranslationKeys.TRANSACTION_BUTTON_ADD_EXPENSE,
                listOf(TranslationKeys.MAIN_TRANSACTION),
            ),
            ScreenActionEntry(
                SearchScreen.TRANSACTIONS,
                null,
                2,
                TranslationKeys.TRANSACTION_BUTTON_PERIODIC_TRANSACTION,
                listOf(TranslationKeys.MAIN_TRANSACTION),
            ),
            ScreenActionEntry(
                SearchScreen.TRANSACTIONS,
                null,
                3,
                TranslationKeys.TRANSACTION_BUTTON_MANAGE_CATEGORIES,
                listOf(TranslationKeys.MAIN_TRANSACTION),
            ),
            ScreenActionEntry(
                SearchScreen.CREDIT_CARDS,
                null,
                0,
                TranslationKeys.CREDIT_CARD_BUTTON_ADD_DEBT,
                listOf(TranslationKeys.MAIN_CREDIT_CARD),
            ),
            ScreenActionEntry(
                SearchScreen.CREDIT_CARDS,
                null,
                1,
                TranslationKeys.CREDIT_CARD_BUTTON_ADD_CREDIT_CARD,
                listOf(TranslationKeys.MAIN_CREDIT_CARD),
            ),
            ScreenActionEntry(
                SearchScreen.CREDIT_CARDS,
                null,
                2,
                TranslationKeys.CREDIT_CARD_BUTTON_RECURRING_DEBTS,
                listOf(TranslationKeys.MAIN_CREDIT_CARD),
            ),
            ScreenActionEntry(
                SearchScreen.CREDIT_CARDS,
                null,
                3,
                TranslationKeys.CREDIT_CARD_BUTTON_CREDIT_CARD_ARCHIVE,
                listOf(TranslationKeys.MAIN_CREDIT_CARD),
            ),
            ScreenActionEntry(
                SearchScreen.GOALS_PLANS,
                GOALS_TAB,
                0,
                TranslationKeys.GOAL_BUTTON_ADD_GOAL,
                listOf(TranslationKeys.MAIN_GOALS_AND_PLANS, TranslationKeys.GOALS_PLANS_TAB_GOALS),
            ),
            ScreenActionEntry(
                SearchScreen.GOALS_PLANS,
                WISHLIST_TAB,
                0,
                TranslationKeys.WISHLIST_DIALOG_ADD_TITLE,
                listOf(TranslationKeys.MAIN_GOALS_AND_PLANS, TranslationKeys.GOALS_PLANS_TAB_WISHLIST),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_STOCKS_FUNDS_TAB,
                0,
                TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_REGISTER_TICKER,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_STOCKS_FUNDS),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_STOCKS_FUNDS_TAB,
                1,
                TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_TICKER_ARCHIVE,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_STOCKS_FUNDS),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_STOCKS_FUNDS_TAB,
                2,
                TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_EXCHANGE_CRYPTOS,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_STOCKS_FUNDS),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_STOCKS_FUNDS_TAB,
                3,
                TranslationKeys.SAVINGS_STOCKS_FUNDS_BUTTON_SHOW_TRANSACTIONS,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_STOCKS_FUNDS),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_BONDS_TAB,
                0,
                TranslationKeys.SAVINGS_BONDS_BUTTON_REGISTER_BOND,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_BONDS),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_BONDS_TAB,
                1,
                TranslationKeys.SAVINGS_BONDS_BUTTON_BOND_ARCHIVE,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_BONDS),
            ),
            ScreenActionEntry(
                SearchScreen.SAVINGS,
                SAVINGS_BONDS_TAB,
                2,
                TranslationKeys.SAVINGS_BONDS_BUTTON_SHOW_TRANSACTIONS,
                listOf(TranslationKeys.MAIN_SAVINGS, TranslationKeys.SAVINGS_TAB_BONDS),
            ),
        )
}
