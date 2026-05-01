package org.moinex.common.constant

object Files {
    // Data directories
    val MOINEX_DATA_DIR: String = System.getProperty("user.home") + "/.moinex"

    val LOGOS_DIR: String = "$MOINEX_DATA_DIR/logos"

    const val SCRIPT_PATH: String = "/scripts/"

    // Paths
    const val WALLET_TYPE_ICONS_PATH: String = "/icon/wallet_type/"
    const val SIDEBAR_ICONS_PATH: String = "/icon/sidebar/"
    const val CRC_OPERATOR_ICONS_PATH: String = "/icon/crc_operator/"
    const val COMMON_ICONS_PATH: String = "/icon/common/"
    const val GIF_PATH: String = "/icon/gif/"

    const val UI_MAIN_PATH: String = "/ui/main/"
    const val UI_DIALOG_PATH: String = "/ui/dialog/"
    const val UI_DIALOG_CREDITCARD_PATH: String = UI_DIALOG_PATH + "creditcard/"

    // UI creditcard package
    const val ADD_CREDIT_CARD_DEBT_FXML: String = UI_DIALOG_CREDITCARD_PATH + "add_credit_card_debt.fxml"
    const val ADD_CREDIT_CARD_CREDIT_FXML: String = UI_DIALOG_CREDITCARD_PATH + "add_credit_card_credit.fxml"
    const val CREDIT_CARD_CREDITS_FXML: String = UI_DIALOG_CREDITCARD_PATH + "credit_card_credits.fxml"
    const val ADD_CREDIT_CARD_FXML: String = UI_DIALOG_CREDITCARD_PATH + "add_credit_card.fxml"
    const val ARCHIVED_CREDIT_CARDS_FXML: String = UI_DIALOG_CREDITCARD_PATH + "archived_credit_cards.fxml"
    const val EDIT_CREDIT_CARD_FXML: String = UI_DIALOG_CREDITCARD_PATH + "edit_credit_card.fxml"
    const val EDIT_CREDIT_CARD_DEBT_FXML: String = UI_DIALOG_CREDITCARD_PATH + "edit_credit_card_debt.fxml"
    const val CREDIT_CARD_INVOICE_PAYMENT_FXML: String =
        UI_DIALOG_CREDITCARD_PATH + "credit_card_invoice_payment.fxml"
    const val ADD_RECURRING_CREDIT_CARD_DEBT_FXML: String =
        UI_DIALOG_CREDITCARD_PATH + "add_recurring_credit_card_debt.fxml"
    const val EDIT_RECURRING_CREDIT_CARD_DEBT_FXML: String =
        UI_DIALOG_CREDITCARD_PATH + "edit_recurring_credit_card_debt.fxml"
    const val RECURRING_CREDIT_CARD_DEBTS_FXML: String =
        UI_DIALOG_CREDITCARD_PATH + "recurring_credit_card_debts.fxml"
    const val UI_DIALOG_GOAL_PATH: String = UI_DIALOG_PATH + "goal/"

    // UI goal package
    const val EDIT_GOAL_FXML: String = UI_DIALOG_GOAL_PATH + "edit_goal.fxml"
    const val ADD_GOAL_FXML: String = UI_DIALOG_GOAL_PATH + "add_goal.fxml"

    // UI investment package
    const val UI_DIALOG_INVESTMENT_PATH: String = UI_DIALOG_PATH + "investment/"
    const val UI_DIALOG_INVESTMENT_CREATE_PATH: String = UI_DIALOG_INVESTMENT_PATH + "create/"
    const val UI_DIALOG_INVESTMENT_UPDATE_PATH: String = UI_DIALOG_INVESTMENT_PATH + "update/"
    const val UI_DIALOG_INVESTMENT_VIEW_PATH: String = UI_DIALOG_INVESTMENT_PATH + "view/"

    // UI investment package - Create
    const val BUY_TICKER_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "buy_ticker.fxml"
    const val SALE_TICKER_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "sale_ticker.fxml"
    const val ADD_TICKER_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "add_ticker.fxml"
    const val ADD_BOND_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "add_bond.fxml"
    const val BUY_BOND_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "buy_bond.fxml"
    const val SALE_BOND_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "sale_bond.fxml"
    const val ADD_DIVIDEND_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "add_dividend.fxml"
    const val ADD_CRYPTO_EXCHANGE_FXML: String = UI_DIALOG_INVESTMENT_CREATE_PATH + "add_crypto_exchange.fxml"

    // UI investment package - Update
    const val EDIT_BOND_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_bond.fxml"
    const val EDIT_TICKER_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_ticker.fxml"
    const val EDIT_TICKER_PURCHASE_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_ticker_purchase.fxml"
    const val EDIT_TICKER_SALE_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_ticker_sale.fxml"
    const val EDIT_BOND_PURCHASE_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_bond_purchase.fxml"
    const val EDIT_BOND_SALE_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_bond_sale.fxml"
    const val EDIT_DIVIDEND_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_dividend.fxml"
    const val EDIT_CRYPTO_EXCHANGE_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_crypto_exchange.fxml"
    const val EDIT_INVESTMENT_TARGET_FXML: String = UI_DIALOG_INVESTMENT_UPDATE_PATH + "edit_investment_target.fxml"

    // UI investment package - View
    const val BOND_TRANSACTIONS_FXML: String = UI_DIALOG_INVESTMENT_VIEW_PATH + "bond_transactions.fxml"
    const val BOND_INTEREST_HISTORY_FXML: String = UI_DIALOG_INVESTMENT_VIEW_PATH + "bond_interest_history.fxml"
    const val ARCHIVED_TICKERS_FXML: String = UI_DIALOG_INVESTMENT_VIEW_PATH + "archived_tickers.fxml"
    const val ARCHIVED_BONDS_FXML: String = UI_DIALOG_INVESTMENT_VIEW_PATH + "archived_bonds.fxml"
    const val INVESTMENT_TRANSACTIONS_FXML: String = UI_DIALOG_INVESTMENT_VIEW_PATH + "investment_transactions.fxml"
    const val FUNDAMENTAL_ANALYSIS_FXML: String = UI_DIALOG_INVESTMENT_VIEW_PATH + "fundamental_analysis.fxml"

    // UI wallettransaction package
    const val UI_DIALOG_WALLETTRANSACTION_PATH: String = UI_DIALOG_PATH + "wallettransaction/"
    const val UI_DIALOG_WALLETTRANSACTION_CREATE_PATH: String = UI_DIALOG_WALLETTRANSACTION_PATH + "create/"
    const val UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH: String = UI_DIALOG_WALLETTRANSACTION_PATH + "update/"
    const val UI_DIALOG_WALLETTRANSACTION_VIEW_PATH: String = UI_DIALOG_WALLETTRANSACTION_PATH + "view/"

    // UI wallettransaction package - Create
    const val ADD_WALLET_FXML: String = UI_DIALOG_WALLETTRANSACTION_CREATE_PATH + "add_wallet.fxml"
    const val ADD_INCOME_FXML: String = UI_DIALOG_WALLETTRANSACTION_CREATE_PATH + "add_income.fxml"
    const val ADD_TRANSFER_FXML: String = UI_DIALOG_WALLETTRANSACTION_CREATE_PATH + "add_transfer.fxml"
    const val ADD_EXPENSE_FXML: String = UI_DIALOG_WALLETTRANSACTION_CREATE_PATH + "add_expense.fxml"
    const val ADD_RECURRING_TRANSACTION_FXML: String =
        UI_DIALOG_WALLETTRANSACTION_CREATE_PATH + "add_recurring_transaction.fxml"

    // UI wallettransaction package - Update
    const val EDIT_TRANSFER_FXML: String = UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH + "edit_transfer.fxml"
    const val EDIT_TRANSACTION_FXML: String = UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH + "edit_transaction.fxml"
    const val EDIT_RECURRING_TRANSACTION_FXML: String =
        UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH + "edit_recurring_transaction.fxml"
    const val CHANGE_WALLET_TYPE_FXML: String = UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH + "change_wallet_type.fxml"
    const val CHANGE_WALLET_BALANCE_FXML: String =
        UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH + "change_wallet_balance.fxml"
    const val RENAME_WALLET_FXML: String = UI_DIALOG_WALLETTRANSACTION_UPDATE_PATH + "rename_wallet.fxml"

    // UI wallettransaction package - View
    const val ARCHIVED_WALLETS_FXML: String = UI_DIALOG_WALLETTRANSACTION_VIEW_PATH + "archived_wallets.fxml"
    const val TRANSFERS_FXML: String = UI_DIALOG_WALLETTRANSACTION_VIEW_PATH + "transfers.fxml"
    const val RECURRING_TRANSACTIONS_FXML: String = UI_DIALOG_WALLETTRANSACTION_VIEW_PATH + "recurring_transaction.fxml"

    // UI financial planning package
    const val UI_DIALOG_FINANCIALPLANNING_PATH: String = UI_DIALOG_PATH + "financialplanning/"
    const val ADD_BUDGET_GROUP_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "add_budget_group.fxml"
    const val EDIT_BUDGET_GROUP_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "edit_budget_group.fxml"
    const val ADD_PLAN_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "add_plan.fxml"
    const val EDIT_PLAN_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "edit_plan.fxml"

    // UI wishlist package
    const val UI_DIALOG_WISHLIST_PATH: String = UI_DIALOG_PATH + "wishlist/"
    const val ADD_WISHLIST_ITEM_FXML: String = UI_DIALOG_WISHLIST_PATH + "add_wishlist_item.fxml"
    const val EDIT_WISHLIST_ITEM_FXML: String = UI_DIALOG_WISHLIST_PATH + "edit_wishlist_item.fxml"
    const val MARK_AS_PURCHASED_FXML: String = UI_DIALOG_WISHLIST_PATH + "mark_as_purchased.fxml"

    const val UI_COMMON_PATH: String = "/ui/common/"
    const val CSS_SCENE_PATH: String = "/css/scene/"
    const val CSS_COMMON_PATH: String = "/css/common/"
    const val CSS_COMPONENT_PATH: String = "/css/component/"
    const val CSS_THEME_PATH: String = "/css/theme/"

    // UI main package
    const val MAIN_FXML: String = UI_MAIN_PATH + "main.fxml"
    const val HOME_FXML: String = UI_MAIN_PATH + "home.fxml"
    const val WALLET_FXML: String = UI_MAIN_PATH + "wallet.fxml"
    const val CREDIT_CARD_FXML: String = UI_MAIN_PATH + "credit_card.fxml"
    const val TRANSACTION_FXML: String = UI_MAIN_PATH + "transaction.fxml"
    const val GOALS_FXML: String = UI_MAIN_PATH + "goals.fxml"
    const val PLANS_FXML: String = UI_MAIN_PATH + "plans.fxml"
    const val GOALS_AND_PLANS_FXML: String = UI_MAIN_PATH + "goals_and_plans.fxml"
    const val FIRE_CALCULATOR_FXML: String = UI_MAIN_PATH + "fire_calculator.fxml"
    const val SAVINGS_FXML: String = UI_MAIN_PATH + "savings.fxml"
    const val SAVINGS_OVERVIEW_FXML: String = UI_MAIN_PATH + "savings_overview.fxml"
    const val SAVINGS_STOCKS_FUNDS_FXML: String = UI_MAIN_PATH + "savings_stocks_funds.fxml"
    const val SAVINGS_BONDS_FXML: String = UI_MAIN_PATH + "savings_bonds.fxml"
    const val WISHLIST_FXML: String = UI_MAIN_PATH + "wishlist.fxml"
    const val CSV_IMPORT_FXML: String = UI_MAIN_PATH + "csv_import.fxml"
    const val SETTINGS_FXML: String = UI_MAIN_PATH + "settings.fxml"
    const val SPLASH_SCREEN_FXML: String = UI_MAIN_PATH + ("splash_screen." + "fxml")

    // UI dialog package
    const val ADD_CATEGORY_FXML: String = UI_DIALOG_PATH + ("add_category." + "fxml")
    const val REMOVE_CATEGORY_FXML: String = UI_DIALOG_PATH + "remove_category.fxml"
    const val MANAGE_CATEGORY_FXML: String = UI_DIALOG_PATH + "manage_category.fxml"
    const val EDIT_CATEGORY_FXML: String = UI_DIALOG_PATH + "edit_category.fxml"
    const val ADD_CALENDAR_EVENT_FXML: String = UI_DIALOG_PATH + "add_calendar_event.fxml"

    // UI common package
    const val WALLET_FULL_PANE_FXML: String = UI_COMMON_PATH + "wallet_pane.fxml"
    const val GOAL_FULL_PANE_FXML: String = UI_COMMON_PATH + "goal_pane.fxml"
    const val BUDGET_GROUP_PANE_FXML: String = UI_COMMON_PATH + "budget_group_pane.fxml"
    const val BUDGET_GROUP_PREVIEW_PANE_FXML: String = UI_COMMON_PATH + "budget_group_preview_pane.fxml"

    const val RESUME_PANE_FXML: String = UI_COMMON_PATH + "resume_pane.fxml"
    const val CRC_PANE_FXML: String = UI_COMMON_PATH + ("credit_card_pane." + "fxml")
    const val FUNDAMENTAL_METRIC_PANE_FXML: String = UI_COMMON_PATH + "fundamental_metric_pane.fxml"

    const val CALCULATOR_FXML: String = UI_COMMON_PATH + "calculator.fxml"
    const val CALENDAR_FXML: String = UI_COMMON_PATH + "calendar.fxml"

    // Icons
    const val HOME_EXPENSE_ICON: String = COMMON_ICONS_PATH + "expense.png"
    const val HOME_INCOME_ICON: String = COMMON_ICONS_PATH + "income.png"
    const val SUCCESS_ICON: String = COMMON_ICONS_PATH + "success.png"
    const val DEFAULT_ICON: String = COMMON_ICONS_PATH + "default.png"
    const val TROPHY_ICON: String = COMMON_ICONS_PATH + "trophy.png"
    const val HIDE_ICON: String = COMMON_ICONS_PATH + "hide.png"
    const val SHOW_ICON: String = COMMON_ICONS_PATH + "show.png"

    // GIFs
    const val LOADING_GIF: String = GIF_PATH + "loading.gif"
    const val SAVINGS_SCREEN_SYNC_PRICES_BUTTON_DEFAULT_ICON: String = COMMON_ICONS_PATH + "synchronize.png"
    const val RELOAD_ICON: String = COMMON_ICONS_PATH + "reload.png"

    // CSS – theme tokens
    const val LIGHT_TOKENS_CSS: String = CSS_THEME_PATH + "tokens.light.css"
    const val DARK_TOKENS_CSS: String = CSS_THEME_PATH + "tokens.dark.css"

    // CSS
    const val MAIN_STYLE_SHEET: String = CSS_SCENE_PATH + "main.css"
    const val HOME_STYLE_SHEET: String = CSS_SCENE_PATH + "home.css"
    const val WALLET_STYLE_SHEET: String = CSS_SCENE_PATH + "wallet.css"
    const val CREDIT_CARD_STYLE_SHEET: String = CSS_SCENE_PATH + "credit-card.css"
    const val TRANSACTION_STYLE_SHEET: String = CSS_SCENE_PATH + "transaction.css"
    const val GOALS_STYLE_SHEET: String = CSS_SCENE_PATH + "goals.css"
    const val PLANS_STYLE_SHEET: String = CSS_SCENE_PATH + "plans.css"
    const val GOALS_AND_PLANS_STYLE_SHEET: String = CSS_SCENE_PATH + "goals_and_plans.css"
    const val FIRE_CALCULATOR_STYLE_SHEET: String = CSS_SCENE_PATH + "fire_calculator.css"
    const val SAVINGS_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val SAVINGS_OVERVIEW_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val SAVINGS_STOCKS_FUNDS_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val SAVINGS_BONDS_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val WISHLIST_STYLE_SHEET: String = CSS_SCENE_PATH + "wishlist.css"
    const val CSV_IMPORT_STYLE_SHEET: String = CSS_SCENE_PATH + "csv_import.css"
    const val SETTINGS_STYLE_SHEET: String = CSS_SCENE_PATH + "settings.css"
    const val CHARTS_COLORS_STYLE_SHEET: String = CSS_COMPONENT_PATH + "charts.css"

    const val COMMON_STYLE_SHEET: String = CSS_COMMON_PATH + "common-styles.css"
    const val TIMELINE_CHART_STYLE_SHEET: String = CSS_COMPONENT_PATH + "timeline-chart.css"

    // Notifications
    const val NOTIFICATION_CENTER_FXML: String = UI_MAIN_PATH + "notification_center.fxml"
    const val NOTIFICATION_CSS: String = CSS_COMPONENT_PATH + "notification.css"
    const val BELL_ICON: String = COMMON_ICONS_PATH + "bell.png"
    const val BELL_GIF: String = COMMON_ICONS_PATH + "bell.gif"
}
