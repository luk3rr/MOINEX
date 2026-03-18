package org.moinex.constants

import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.moinex.model.enums.AssetType
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Constants {
    @JvmStatic
    val INSTANCE = this
    const val APP_NAME: String = "Moinex"

    const val SCRIPT_PATH: String = "/scripts/"

    @JvmField
    val PYTHON_INTERPRETER: String = pythonInterpreter

    const val GET_STOCK_PRICE_SCRIPT: String = "get_stock_price.py"

    const val GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT: String = "get_brazilian_market_indicators.py"

    const val GET_FUNDAMENTAL_DATA_SCRIPT: String = "get_fundamental_data.py"

    const val GET_STOCK_LOGO_SCRIPT: String = "get_stock_logo.py"

    const val GET_MARKET_INDICATOR_HISTORY_SCRIPT: String = "get_market_indicator_history.py"

    const val YAHOO_LOOKUP_URL: String = "https://finance.yahoo.com/research-hub/screener/most_actives"

    // Data directories
    @JvmField
    val MOINEX_DATA_DIR: String = System.getProperty("user.home") + "/.moinex"

    @JvmField
    val LOGOS_DIR: String = MOINEX_DATA_DIR + "/logos"

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
    const val UI_DIALOG_GOAL_PATH: String = UI_DIALOG_PATH + "goal/"

    // UI goal package
    const val EDIT_GOAL_FXML: String = UI_DIALOG_GOAL_PATH + ("edit_goal." + "fxml")
    const val ADD_GOAL_FXML: String = UI_DIALOG_GOAL_PATH + "add_goal.fxml"
    const val UI_DIALOG_INVESTMENT_PATH: String = UI_DIALOG_PATH + "investment/"

    // UI investment package
    const val BUY_TICKER_FXML: String = UI_DIALOG_INVESTMENT_PATH + "buy_ticker.fxml"
    const val SALE_TICKER_FXML: String = UI_DIALOG_INVESTMENT_PATH + "sale_ticker.fxml"
    const val ADD_TICKER_FXML: String = UI_DIALOG_INVESTMENT_PATH + "add_ticker.fxml"
    const val ADD_BOND_FXML: String = UI_DIALOG_INVESTMENT_PATH + "add_bond.fxml"
    const val EDIT_BOND_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_bond.fxml"
    const val BUY_BOND_FXML: String = UI_DIALOG_INVESTMENT_PATH + "buy_bond.fxml"
    const val SALE_BOND_FXML: String = UI_DIALOG_INVESTMENT_PATH + "sale_bond.fxml"
    const val BOND_TRANSACTIONS_FXML: String = UI_DIALOG_INVESTMENT_PATH + "bond_transactions.fxml"
    const val BOND_INTEREST_HISTORY_FXML: String = UI_DIALOG_INVESTMENT_PATH + "bond_interest_history.fxml"
    const val ADD_DIVIDEND_FXML: String = UI_DIALOG_INVESTMENT_PATH + ("add_dividend." + "fxml")
    const val ADD_CRYPTO_EXCHANGE_FXML: String = UI_DIALOG_INVESTMENT_PATH + "add_crypto_exchange.fxml"
    const val ARCHIVED_TICKERS_FXML: String = UI_DIALOG_INVESTMENT_PATH + "archived_tickers.fxml"
    const val ARCHIVED_BONDS_FXML: String = UI_DIALOG_INVESTMENT_PATH + "archived_bonds.fxml"
    const val EDIT_TICKER_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_ticker.fxml"
    const val EDIT_TICKER_PURCHASE_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_ticker_purchase.fxml"
    const val EDIT_TICKER_SALE_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_ticker_sale.fxml"
    const val EDIT_BOND_PURCHASE_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_bond_purchase.fxml"
    const val EDIT_BOND_SALE_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_bond_sale.fxml"
    const val EDIT_DIVIDEND_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_dividend.fxml"
    const val EDIT_CRYPTO_EXCHANGE_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_crypto_exchange.fxml"
    const val INVESTMENT_TRANSACTIONS_FXML: String = UI_DIALOG_INVESTMENT_PATH + "investment_transactions.fxml"
    const val EDIT_INVESTMENT_TARGET_FXML: String = UI_DIALOG_INVESTMENT_PATH + "edit_investment_target.fxml"
    const val FUNDAMENTAL_ANALYSIS_FXML: String = UI_DIALOG_INVESTMENT_PATH + "fundamental_analysis.fxml"
    const val UI_DIALOG_WALLETTRANSACTION_PATH: String = UI_DIALOG_PATH + "wallettransaction/"

    // UI wallettransaction package
    const val ADD_WALLET_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "add_wallet.fxml"
    const val ADD_INCOME_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "add_income.fxml"
    const val ADD_TRANSFER_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + ("add_transfer." + "fxml")
    const val EDIT_TRANSFER_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + ("edit_transfer." + "fxml")
    const val ADD_EXPENSE_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "add_expense.fxml"
    const val ARCHIVED_WALLETS_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "archived_wallets.fxml"
    const val TRANSFERS_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "transfers.fxml"
    const val EDIT_TRANSACTION_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "edit_transaction.fxml"
    const val REMOVE_TRANSACTION_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "remove_transaction.fxml"
    const val CHANGE_WALLET_TYPE_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "change_wallet_type.fxml"
    const val CHANGE_WALLET_BALANCE_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "change_wallet_balance.fxml"
    const val RENAME_WALLET_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "rename_wallet.fxml"
    const val RECURRING_TRANSACTIONS_FXML: String = UI_DIALOG_WALLETTRANSACTION_PATH + "recurring_transaction.fxml"
    const val ADD_RECURRING_TRANSACTION_FXML: String =
        UI_DIALOG_WALLETTRANSACTION_PATH + "add_recurring_transaction.fxml"
    const val EDIT_RECURRING_TRANSACTION_FXML: String =
        UI_DIALOG_WALLETTRANSACTION_PATH + "edit_recurring_transaction.fxml"
    const val UI_DIALOG_FINANCIALPLANNING_PATH: String = UI_DIALOG_PATH + "financialplanning/"

    // UI financial planning package
    const val ADD_BUDGET_GROUP_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "add_budget_group.fxml"
    const val EDIT_BUDGET_GROUP_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "edit_budget_group.fxml"
    const val ADD_PLAN_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "add_plan.fxml"
    const val EDIT_PLAN_FXML: String = UI_DIALOG_FINANCIALPLANNING_PATH + "edit_plan.fxml"
    const val UI_COMMON_PATH: String = "/ui/common/"
    const val CSS_SCENE_PATH: String = "/css/scene/"
    const val CSS_COMMON_PATH: String = "/css/common/"
    const val CSS_COMPONENT_PATH: String = "/css/component/"

    // UI main package
    const val MAIN_FXML: String = UI_MAIN_PATH + "main.fxml"
    const val HOME_FXML: String = UI_MAIN_PATH + "home.fxml"
    const val WALLET_FXML: String = UI_MAIN_PATH + "wallet.fxml"
    const val CREDIT_CARD_FXML: String = UI_MAIN_PATH + "credit_card.fxml"
    const val TRANSACTION_FXML: String = UI_MAIN_PATH + "transaction.fxml"
    const val GOALS_FXML: String = UI_MAIN_PATH + "goals.fxml"
    const val PLANS_FXML: String = UI_MAIN_PATH + "plans.fxml"
    const val GOALS_AND_PLANS_FXML: String = UI_MAIN_PATH + "goals_and_plans.fxml"
    const val SAVINGS_FXML: String = UI_MAIN_PATH + "savings.fxml"
    const val SAVINGS_OVERVIEW_FXML: String = UI_MAIN_PATH + "savings_overview.fxml"
    const val SAVINGS_STOCKS_FUNDS_FXML: String = UI_MAIN_PATH + "savings_stocks_funds.fxml"
    const val SAVINGS_BONDS_FXML: String = UI_MAIN_PATH + "savings_bonds.fxml"
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
    const val WALLET_FULL_PANE_FXML: String = UI_COMMON_PATH + "wallet_full_pane.fxml"
    const val GOAL_FULL_PANE_FXML: String = UI_COMMON_PATH + "goal_full_pane.fxml"
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

    // CSS
    const val MAIN_STYLE_SHEET: String = CSS_SCENE_PATH + "main.css"
    const val HOME_STYLE_SHEET: String = CSS_SCENE_PATH + "home.css"
    const val WALLET_STYLE_SHEET: String = CSS_SCENE_PATH + "wallet.css"
    const val CREDIT_CARD_STYLE_SHEET: String = CSS_SCENE_PATH + "credit-card.css"
    const val TRANSACTION_STYLE_SHEET: String = CSS_SCENE_PATH + "transaction.css"
    const val GOALS_STYLE_SHEET: String = CSS_SCENE_PATH + "goals.css"
    const val PLANS_STYLE_SHEET: String = CSS_SCENE_PATH + "plans.css"
    const val GOALS_AND_PLANS_STYLE_SHEET: String = CSS_SCENE_PATH + "goals_and_plans.css"
    const val SAVINGS_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val SAVINGS_OVERVIEW_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val SAVINGS_STOCKS_FUNDS_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val SAVINGS_BONDS_STYLE_SHEET: String = CSS_SCENE_PATH + "savings.css"
    const val CSV_IMPORT_STYLE_SHEET: String = CSS_SCENE_PATH + "csv_import.css"
    const val SETTINGS_STYLE_SHEET: String = CSS_SCENE_PATH + "settings.css"
    const val CHARTS_COLORS_STYLE_SHEET: String = CSS_COMPONENT_PATH + "charts.css"

    // Component styles
    const val CHARTS_COLORS_COUNT: Int = 20
    const val CHARTS_COLORS_PREFIX: String = "chart-color-"
    const val CHARTS_LEGEND_RECT_STYLE: String = "legend-color"

    const val PROGRESS_BAR_RED_COLOR_STYLE: String = "progress-bar-red"
    const val PROGRESS_BAR_YELLOW_COLOR_STYLE: String = "progress-bar-yellow"
    const val PROGRESS_BAR_GREEN_COLOR_STYLE: String = "progress-bar-green"

    const val COMMON_STYLE_SHEET: String = CSS_COMMON_PATH + "common-styles.css"
    const val TIMELINE_CHART_STYLE_SHEET: String = CSS_COMPONENT_PATH + "timeline-chart.css"

    // Info styles from common-styles.css
    const val INFO_LABEL_RED_STYLE: String = "info-label-red"
    const val INFO_LABEL_GREEN_STYLE: String = "info-label-green"
    const val INFO_LABEL_YELLOW_STYLE: String = "info-label-yellow"
    const val INFO_LABEL_NEUTRAL_STYLE: String = "info-label-neutral"

    // Main pane styles
    const val SIDEBAR_SELECTED_BUTTON_STYLE: String = "sidebar-button-selected"
    const val NEGATIVE_BALANCE_STYLE: String = "negative-balance"
    const val POSITIVE_BALANCE_STYLE: String = "positive-balance"
    const val NEUTRAL_BALANCE_STYLE: String = "neutral-balance"

    // Home pane styles
    const val HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE: String = "income-item"
    const val HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE: String = "expense-item"

    const val HOME_CREDIT_CARD_ITEM_STYLE: String = "credit-card-item"

    // Top Performers Table Styles
    const val TOP_PERFORMERS_ASSET_COLUMN_WIDTH: Double = 90.0
    const val TOP_PERFORMERS_RETURN_COLUMN_WIDTH: Double = 90.0
    const val TOP_PERFORMERS_VALUE_COLUMN_WIDTH: Double = 90.0
    const val CUSTOM_TABLE_TITLE_STYLE: String = "custom-table-title"
    const val CUSTOM_TABLE_HEADER_STYLE: String = "custom-table-header"
    const val CUSTOM_TABLE_CELL_STYLE: String = "custom-table-cell"

    // Allocation Panel Styles
    const val ALLOCATION_TYPE_LABEL_STYLE: String = "allocation-type-label"
    const val ALLOCATION_PROGRESS_BAR_STYLE: String = "allocation-progress-bar"
    const val ALLOCATION_FILLED_BAR_CRITICAL_LOW_STYLE: String = "allocation-filled-bar-critical-low"
    const val ALLOCATION_FILLED_BAR_WARNING_LOW_STYLE: String = "allocation-filled-bar-warning-low"
    const val ALLOCATION_FILLED_BAR_ON_TARGET_STYLE: String = "allocation-filled-bar-on-target"
    const val ALLOCATION_FILLED_BAR_WARNING_HIGH_STYLE: String = "allocation-filled-bar-warning-high"
    const val ALLOCATION_FILLED_BAR_CRITICAL_HIGH_STYLE: String = "allocation-filled-bar-critical-high"
    const val ALLOCATION_INFO_LABEL_STYLE: String = "allocation-info-label"
    const val ALLOCATION_DIFF_LABEL_STYLE: String = "allocation-diff-label"
    const val ALLOCATION_DIFF_CRITICAL_LOW_STYLE: String = "allocation-diff-critical-low"
    const val ALLOCATION_DIFF_WARNING_LOW_STYLE: String = "allocation-diff-warning-low"
    const val ALLOCATION_DIFF_ON_TARGET_STYLE: String = "allocation-diff-on-target"
    const val ALLOCATION_DIFF_WARNING_HIGH_STYLE: String = "allocation-diff-warning-high"
    const val ALLOCATION_DIFF_CRITICAL_HIGH_STYLE: String = "allocation-diff-critical-high"

    // Allocation Panel Thresholds
    const val ALLOCATION_CRITICAL_LOW_THRESHOLD: Double = 0.0
    const val ALLOCATION_WARNING_LOW_THRESHOLD: Double = 80.0
    const val ALLOCATION_ON_TARGET_LOW_THRESHOLD: Double = 95.0
    const val ALLOCATION_ON_TARGET_HIGH_THRESHOLD: Double = 105.0
    const val ALLOCATION_WARNING_HIGH_THRESHOLD: Double = 120.0

    // Credit Card Item Styles
    const val HOME_CREDIT_CARD_ITEM_NAME_STYLE: String = "credit-card-item-name"
    const val HOME_CREDIT_CARD_ITEM_BALANCE_STYLE: String = "credit-card-item-balance"
    const val HOME_CREDIT_CARD_ITEM_DIGITS_STYLE: String = "credit-card-item-digits"
    const val HOME_CREDIT_CARD_ITEM_OPERATOR_STYLE: String = "credit-card-item-operator"

    const val HOME_WALLET_ITEM_STYLE: String = "wallet-item"
    const val HOME_WALLET_ITEM_NAME_STYLE: String = "wallet-item-name"
    const val HOME_WALLET_ITEM_BALANCE_STYLE: String = "wallet-item-balance"
    const val HOME_WALLET_TYPE_STYLE: String = "wallet-item-type"
    const val HOME_VIRTUAL_WALLET_INFO_STYLE: String = "virtual-wallet-info"

    const val TOOLTIP_STYLE: String = "tooltip"

    const val TOTAL_BALANCE_VALUE_LABEL_STYLE: String = "total-balance-value-label"
    const val TOTAL_BALANCE_FORESEEN_LABEL_STYLE: String = "total-balance-foreseen-label"

    // Wallet pane styles
    const val WALLET_TOTAL_BALANCE_WALLETS_LABEL_STYLE: String = "total-balance-wallets-label"
    const val WALLET_CHECK_BOX_STYLE: String = "check-box"

    // Icons sizes
    const val WALLET_TYPE_ICONS_SIZE: Int = 42 // 42x42 px
    const val CRC_OPERATOR_ICONS_SIZE: Int = 42 // 42x42 px
    const val HOME_LAST_TRANSACTIONS_ICON_SIZE: Int = 32 // 32x32 px

    // Home scene constants
    const val HOME_LAST_TRANSACTIONS_SIZE: Int = 15
    const val HOME_LAST_TRANSACTIONS_DESCRIPTION_LABEL_WIDTH: Int = 290
    const val HOME_LAST_TRANSACTIONS_VALUE_LABEL_WIDTH: Int = 70
    const val HOME_LAST_TRANSACTIONS_DATE_LABEL_WIDTH: Int = 80
    const val HOME_LAST_TRANSACTIONS_WALLET_LABEL_WIDTH: Int = 100
    const val HOME_LAST_TRANSACTIONS_CATEGORY_LABEL_WIDTH: Int = 100
    const val HOME_LAST_TRANSACTIONS_STATUS_LABEL_WIDTH: Int = 90
    const val HOME_PANES_ITEMS_PER_PAGE: Int = 2

    const val EPSILON: Double = 1e-6
    const val ONE_SECOND_IN_NS: Double = 1000000000.0

    const val NEGATIVE_PERCENTAGE_THRESHOLD: Double = -1000.0

    // Credit card
    const val MAX_BILLING_DUE_DAY: Int = 28
    const val INSTALLMENTS_FIELD_MAX_DIGITS: Int = 3
    const val MAX_INSTALLMENTS: Int = 999

    // Animation constants
    const val MENU_COLLAPSED_WIDTH: Double = 80.0
    const val MENU_EXPANDED_WIDTH: Double = 220.0

    const val XYBAR_CHART_MONTHS: Int = 12
    const val XYBAR_CHART_FUTURE_MONTHS: Int = 6
    const val PL_CHART_MONTHS: Int = 12 * 5
    const val PL_CHART_FUTURE_MONTHS: Int = 3
    const val MONTH_RESUME_FUTURE_MONTHS: Int = 6

    const val CRC_XYBAR_CHART_MAX_MONTHS: Int = 25
    const val XYBAR_CHART_TICKS: Int = 6

    const val FADE_IN_ANIMATION_DURATION: Double = 1.0 // s
    const val FADE_OUT_ANIMATION_DURATION: Double = 1.0 // s
    const val SLIDE_ANIMATION_DURATION: Double = 1.0 // s

    const val MENU_ANIMATION_DURATION: Double = 200.0 // ms
    const val XYBAR_CHART_ANIMATION_FRAMES: Int = 30
    const val XYBAR_CHART_ANIMATION_DURATION: Double = 0.3 // s
    const val TOOLTIP_ANIMATION_DURATION: Double = 0.5 // s
    const val TOOLTIP_ANIMATION_DELAY: Double = 0.5 // s

    const val HOME_ITEM_NODE_NAME_MAX_LENGTH: Int = 100

    // Calendar config
    const val YEAR_RESUME_FUTURE_YEARS: Int = 2
    const val NON_LEAP_YEAR_FEBRUARY_DAYS: Int = 28
    const val WEEK_DAYS: Int = 7

    @JvmField
    val CALENDAR_WEEKDAY_FONT_CONFIG: Font? = Font.font("Arial", FontWeight.BOLD, 14.0)

    @JvmField
    val CALENDAR_DATE_FONT_CONFIG: Font? = Font.font("Arial", FontWeight.BOLD, 14.0)

    const val CALENDAR_CELL_BORDER_WIDTH: Double = 0.5
    const val CALENDAR_CELL_EXTERNAL_BORDER_WIDTH: Double = 2.0

    // Circular progress bar on the goal pane
    const val GOAL_PANE_PROGRESS_BAR_RADIUS: Double = 80.0
    const val GOAL_PANE_PROGRESS_BAR_WIDTH: Double = 8.0

    const val SUGGESTIONS_MAX_ITEMS: Int = 5

    // WARNING: Do not change this value. If you do, update too on the database
    const val GOAL_DEFAULT_WALLET_TYPE_NAME: String = "Goal"

    // Enough time for you to become poor :)
    // Or rich, who knows?
    // WARNING: Do not change this value. If you do, update too on the database
    @JvmField
    val RECURRING_TRANSACTION_DEFAULT_END_DATE: LocalDate = LocalDate.of(2100, 12, 31)

    @JvmField
    val RECURRING_TRANSACTION_DEFAULT_TIME: LocalTime? = LocalTime.of(23, 59, 59, 0)

    @JvmField
    val RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME: LocalTime? = LocalTime.of(0, 0, 0, 0)

    const val NA_DATA: String = "N/A"

    // Date formats
    const val DB_DATE_FORMAT: String = "yyyy-MM-dd'T'HH:mm:ss"
    const val DATE_FORMAT_NO_TIME: String = "yyyy-MM-dd"
    const val SHORT_DATE_FORMAT_NO_TIME: String = "yy-MM-dd"
    const val DATE_FORMAT_WITH_TIME: String = "yyyy-MM-dd HH:mm:ss"
    const val DB_MONTH_YEAR_FORMAT: String = "yyyy-MM"
    const val BACEN_DATE_FORMAT: String = "dd/MM/yyyy"

    @JvmField
    val DB_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DB_DATE_FORMAT)

    @JvmField
    val DATE_FORMATTER_NO_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_NO_TIME)

    @JvmField
    val SHORT_DATE_FORMATTER_NO_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern(SHORT_DATE_FORMAT_NO_TIME)

    @JvmField
    val DATE_FORMATTER_WITH_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_WITH_TIME)

    @JvmField
    val DB_MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DB_MONTH_YEAR_FORMAT)

    @JvmField
    val BACEN_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(BACEN_DATE_FORMAT)

    // Define the pattern for positive and negative currency values
    const val CURRENCY_FORMAT: String = "R$ #,##0.00;- R$ #,##0.00"

    // Percentage with two decimal places
    const val PERCENTAGE_FORMAT: String = "0.00"

    const val CREDIT_CARD_NUMBER_FORMAT: String = "**** **** **** ####"

    // Regex
    const val DIGITS_ONLY_REGEX: String = "\\d*"
    const val MONETARY_VALUE_REGEX: String = "\\d*\\.?\\d{0,2}"
    const val SIGNED_MONETARY_VALUE_REGEX: String = "-?\\d*\\.?\\d{0,2}"
    const val PERCENTAGE_REGEX: String = "\\d{0,2}(\\.\\d{0,2})?|100(\\.00)?"
    const val INTEREST_RATE_REGEX: String = "\\d{0,3}(\\.\\d{0,4})?"

    const val INVESTMENT_CALCULATION_PRECISION: Int = 8

    @JvmField
    val INVESTMENT_VALUE_REGEX: String = "\\d*\\.?\\d{0," + INVESTMENT_CALCULATION_PRECISION + "}"

    // Yahoo Finance API constants
    const val IBOVESPA_TICKER: String = "^BVSP"
    const val DOLLAR_TICKER: String = "USDBRL=X"
    const val EURO_TICKER: String = "EURBRL=X"

    const val GOLD_TICKER: String = "GC=F"
    const val SOYBEAN_TICKER: String = "ZS=F"
    const val COFFEE_ARABICA_TICKER: String = "KC=F"
    const val WHEAT_TICKER: String = "ZW=F"
    const val OIL_BRENT_TICKER: String = "BZ=F"

    const val BITCOIN_TICKER: String = "BTC-USD"
    const val ETHEREUM_TICKER: String = "ETH-USD"

    /**
     * Get a regex that matches digits up to n
     *
     * @param n The maximum number of digits
     * @return The regex
     * @throws IllegalArgumentException If n is negative
     */
    @JvmStatic
    fun getDigitsRegexUpTo(n: Int): String {
        require(n >= 0) { "n must be non-negative" }

        return "\\d{0," + n + "}"
    }

    /**
     * Calculate the number of months until the target date
     *
     * @param beginDate The start date
     * @param targetDate The target date
     * @return The number of months until the target date
     */
    @JvmStatic
    fun calculateMonthsUntilTarget(
        beginDate: LocalDate,
        targetDate: LocalDate?,
    ): Int = Math.toIntExact(ChronoUnit.MONTHS.between(beginDate, targetDate))

    /**
     * Calculate the number of days until the target date
     *
     * @param beginDate The begin date
     * @param targetDate The target date
     * @return The number of days until the target date
     */
    @JvmStatic
    fun calculateDaysUntilTarget(
        beginDate: LocalDate,
        targetDate: LocalDate?,
    ): Int = Math.toIntExact(ChronoUnit.DAYS.between(beginDate, targetDate))

    /**
     * Round price according to the ticker type
     *
     * @param price The price to be rounded
     * @param tickerType The ticker type
     */
    @JvmStatic
    fun roundPrice(
        price: BigDecimal,
        assetType: AssetType,
    ): BigDecimal {
        // Stocks and funds have two decimal places
        // Cryptocurrencies have MAX allowed by settings
        if (assetType == AssetType.STOCK || assetType == AssetType.FUND) {
            return price.setScale(2, RoundingMode.HALF_UP)
        } else {
            return price.setScale(INVESTMENT_CALCULATION_PRECISION, RoundingMode.HALF_UP)
        }
    }

    val pythonInterpreter: String
        /**
         * Get the Python interpreter path based on the operating system For Windows with embedded
         * Python, use the bundled interpreter For Linux/Mac, use the system Python
         */
        get() {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())

            if (os.contains("win")) {
                // Check if running from jpackage installation (embedded Python)
                val appPath = System.getProperty("jpackage.app-path")

                if (appPath != null) {
                    // Running from jpackage installation
                    val appDir = File(appPath).parentFile

                    if (appDir != null) {
                        val embeddedPython =
                            File(appDir, "python-embedded\\python.exe")
                                .absolutePath

                        if (File(embeddedPython).exists()) {
                            return embeddedPython
                        }
                    }
                }

                // Fallback to system Python on Windows
                return "python"
            } else {
                // Linux/Mac
                return "/usr/bin/python3"
            }
        }
}
