/*
 * Filename: Icons.kt
 * Created on: October 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import org.kordamp.ikonli.javafx.FontIcon

/**
 * Ikonli literals (Material Design 2 pack) of the interface icons, one per former PNG.
 */
object Icons {
    const val DEFAULT_SIZE: Int = 16
    const val ICON_STYLE_CLASS: String = "app-icon"
    const val SIZE_STYLE_CLASS_PREFIX: String = "icon-size-"

    // Common
    const val ADD: String = "mdal-add"
    const val ARCHIVE: String = "mdal-archive"
    const val ARROW_LEFT: String = "mdal-arrow_back"
    const val ARROW_RIGHT: String = "mdal-arrow_forward"
    const val CALCULATOR: String = "mdal-calculate"
    const val CALENDAR: String = "mdal-calendar_today"
    const val CATEGORY: String = "mdal-category"
    const val CHECK: String = "mdal-check"
    const val COIN: String = "mdmz-monetization_on"
    const val CREDIT_CARD: String = "mdal-credit_card"
    const val CRYPTO_EXCHANGE: String = "mdmz-swap_horiz"
    const val CRYPTOCURRENCY: String = "mdmz-toll"
    const val CSV_IMPORT: String = "mdal-file_upload"
    const val DEFAULT: String = "mdal-help_outline"
    const val DESCRIPTION: String = "mdal-description"
    const val DETAILS: String = "mdal-info"
    const val DIGITS: String = "mdal-dialpad"
    const val EDIT: String = "mdal-edit"
    const val EXPENSE: String = "mdal-arrow_downward"
    const val EXPENSE_BLACK: String = "mdal-arrow_downward"
    const val FIND: String = "mdmz-search"
    const val GOAL: String = "mdal-flag"
    const val HIDE: String = "mdmz-visibility_off"
    const val INCOME: String = "mdal-arrow_upward"
    const val INCOME_BLACK: String = "mdal-arrow_upward"
    const val LIST: String = "mdal-list"
    const val NOTIFICATION: String = "mdmz-notifications"
    const val OPTIONS: String = "mdmz-more_vert"
    const val PENCIL: String = "mdal-edit"
    const val PENDING: String = "mdal-hourglass_empty"
    const val PERCENT: String = "mdmz-percentage"
    const val PROFIT: String = "mdmz-trending_up"
    const val PURCHASED: String = "mdmz-shopping_cart"
    const val QUANTITY: String = "mdal-format_list_numbered"
    const val QUESTION: String = "mdal-help_outline"
    const val REFUND: String = "mdmz-undo"
    const val RELOAD: String = "mdmz-refresh"
    const val REMOVE: String = "mdal-close"
    const val SHOW: String = "mdmz-visibility"
    const val SIGN_IN: String = "mdal-login"
    const val SIGN_OUT: String = "mdal-exit_to_app"
    const val SUCCESS: String = "mdal-check_circle"
    const val SYNCHRONIZE: String = "mdmz-sync"
    const val TRADE: String = "mdmz-swap_horiz"
    const val TRANSACTION: String = "mdmz-receipt"
    const val TRANSFER: String = "mdal-compare_arrows"
    const val TRANSFER_MONEY: String = "mdmz-send"
    const val TRASH: String = "mdal-delete"
    const val TROPHY: String = "mdal-emoji_events"

    // Sidebar
    const val SIDEBAR_AI_CHAT: String = "mdal-chat"
    const val SIDEBAR_CATEGORY: String = "mdal-category"
    const val SIDEBAR_CREDIT_CARD: String = "mdal-credit_card"
    const val SIDEBAR_GOAL: String = "mdal-flag"
    const val SIDEBAR_HOME: String = "mdal-home"
    const val SIDEBAR_IMPORT: String = "mdal-file_upload"
    const val SIDEBAR_MENU: String = "mdmz-menu"
    const val SIDEBAR_SAVINGS: String = "mdmz-money"
    const val SIDEBAR_SETTINGS: String = "mdmz-settings"
    const val SIDEBAR_SUMMARY: String = "mdal-bar_chart"
    const val SIDEBAR_TRANSACTION: String = "mdmz-receipt"
    const val SIDEBAR_WALLET: String = "mdal-account_balance_wallet"

    // Wallet types
    const val WALLET_TYPE_BROKER: String = "mdmz-show_chart"
    const val WALLET_TYPE_CHECKING: String = "mdal-account_balance"
    const val WALLET_TYPE_CRYPTOCURRENCY: String = "mdmz-toll"
    const val WALLET_TYPE_FOOD_VOUCHER: String = "mdmz-restaurant"
    const val WALLET_TYPE_GOAL: String = "mdal-flag"
    const val WALLET_TYPE_MEAL_VOUCHER: String = "mdal-fastfood"
    const val WALLET_TYPE_OTHERS: String = "mdmz-more_horiz"
    const val WALLET_TYPE_SAVINGS: String = "mdmz-money"
    const val WALLET_TYPE_WALLET: String = "mdal-account_balance_wallet"
}

/**
 * Builds a [FontIcon] for [literal] with the shared `app-icon` style class, so its color comes from the theme tokens,
 * and an `icon-size-N` class from base.css, because a size set from code is overridden when the theme changes the font.
 */
fun icon(
    literal: String,
    size: Int = Icons.DEFAULT_SIZE,
): FontIcon =
    FontIcon(literal).apply {
        styleClass.addAll(Icons.ICON_STYLE_CLASS, "${Icons.SIZE_STYLE_CLASS_PREFIX}$size")
    }
