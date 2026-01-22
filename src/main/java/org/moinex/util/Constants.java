/*
 * Filename: Constants.java
 * Created on: August 28, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.moinex.model.enums.TickerType;

/**
 * Constants used in the application
 */
public final class Constants {
    public static final String APP_NAME = "Moinex";

    public static final String SCRIPT_PATH = "/scripts/";

    public static final String PYTHON_INTERPRETER = getPythonInterpreter();

    public static final String GET_STOCK_PRICE_SCRIPT = "get_stock_price.py";

    public static final String GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT =
            "get_brazilian_market_indicators.py";

    public static final String GET_FUNDAMENTAL_DATA_SCRIPT = "get_fundamental_data.py";

    public static final String YAHOO_LOOKUP_URL =
            "https://finance.yahoo.com/research-hub/screener/most_actives";

    // Paths
    public static final String WALLET_TYPE_ICONS_PATH = "/icon/wallet_type/";
    public static final String SIDEBAR_ICONS_PATH = "/icon/sidebar/";
    public static final String CRC_OPERATOR_ICONS_PATH = "/icon/crc_operator/";
    public static final String COMMON_ICONS_PATH = "/icon/common/";
    public static final String GIF_PATH = "/icon/gif/";

    public static final String UI_MAIN_PATH = "/ui/main/";
    public static final String UI_DIALOG_PATH = "/ui/dialog/";
    public static final String UI_DIALOG_CREDITCARD_PATH = UI_DIALOG_PATH + "creditcard/";
    // UI creditcard package
    public static final String ADD_CREDIT_CARD_DEBT_FXML =
            UI_DIALOG_CREDITCARD_PATH + "add_credit_card_debt.fxml";
    public static final String ADD_CREDIT_CARD_CREDIT_FXML =
            UI_DIALOG_CREDITCARD_PATH + "add_credit_card_credit.fxml";
    public static final String CREDIT_CARD_CREDITS_FXML =
            UI_DIALOG_CREDITCARD_PATH + "credit_card_credits.fxml";
    public static final String ADD_CREDIT_CARD_FXML =
            UI_DIALOG_CREDITCARD_PATH + "add_credit_card.fxml";
    public static final String ARCHIVED_CREDIT_CARDS_FXML =
            UI_DIALOG_CREDITCARD_PATH + "archived_credit_cards.fxml";
    public static final String EDIT_CREDIT_CARD_FXML =
            UI_DIALOG_CREDITCARD_PATH + "edit_credit_card.fxml";
    public static final String EDIT_CREDIT_CARD_DEBT_FXML =
            UI_DIALOG_CREDITCARD_PATH + "edit_credit_card_debt.fxml";
    public static final String CREDIT_CARD_INVOICE_PAYMENT_FXML =
            UI_DIALOG_CREDITCARD_PATH + "credit_card_invoice_payment.fxml";
    public static final String UI_DIALOG_GOAL_PATH = UI_DIALOG_PATH + "goal/";
    // UI goal package
    public static final String EDIT_GOAL_FXML = UI_DIALOG_GOAL_PATH + ("edit_goal." + "fxml");
    public static final String ADD_GOAL_FXML = UI_DIALOG_GOAL_PATH + "add_goal.fxml";
    public static final String UI_DIALOG_INVESTMENT_PATH = UI_DIALOG_PATH + "investment/";
    // UI investment package
    public static final String BUY_TICKER_FXML = UI_DIALOG_INVESTMENT_PATH + "buy_ticker.fxml";
    public static final String SALE_TICKER_FXML = UI_DIALOG_INVESTMENT_PATH + "sale_ticker.fxml";
    public static final String ADD_TICKER_FXML = UI_DIALOG_INVESTMENT_PATH + "add_ticker.fxml";
    public static final String ADD_BOND_FXML = UI_DIALOG_INVESTMENT_PATH + "add_bond.fxml";
    public static final String EDIT_BOND_FXML = UI_DIALOG_INVESTMENT_PATH + "edit_bond.fxml";
    public static final String BUY_BOND_FXML = UI_DIALOG_INVESTMENT_PATH + "buy_bond.fxml";
    public static final String SALE_BOND_FXML = UI_DIALOG_INVESTMENT_PATH + "sale_bond.fxml";
    public static final String BOND_TRANSACTIONS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "bond_transactions.fxml";
    public static final String ADD_DIVIDEND_FXML =
            UI_DIALOG_INVESTMENT_PATH + ("add_dividend." + "fxml");
    public static final String ADD_CRYPTO_EXCHANGE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "add_crypto_exchange.fxml";
    public static final String ARCHIVED_TICKERS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "archived_tickers.fxml";
    public static final String ARCHIVED_BONDS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "archived_bonds.fxml";
    public static final String EDIT_TICKER_FXML = UI_DIALOG_INVESTMENT_PATH + "edit_ticker.fxml";
    public static final String EDIT_TICKER_PURCHASE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_ticker_purchase.fxml";
    public static final String EDIT_TICKER_SALE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_ticker_sale.fxml";
    public static final String EDIT_BOND_PURCHASE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_bond_purchase.fxml";
    public static final String EDIT_BOND_SALE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_bond_sale.fxml";
    public static final String EDIT_DIVIDEND_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_dividend.fxml";
    public static final String EDIT_CRYPTO_EXCHANGE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_crypto_exchange.fxml";
    public static final String INVESTMENT_TRANSACTIONS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "investment_transactions.fxml";
    public static final String EDIT_INVESTMENT_TARGET_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_investment_target.fxml";
    public static final String FUNDAMENTAL_ANALYSIS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "fundamental_analysis.fxml";
    public static final String UI_DIALOG_WALLETTRANSACTION_PATH =
            UI_DIALOG_PATH + "wallettransaction/";
    // UI wallettransaction package
    public static final String ADD_WALLET_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "add_wallet.fxml";
    public static final String ADD_INCOME_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "add_income.fxml";
    public static final String ADD_TRANSFER_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + ("add_transfer." + "fxml");
    public static final String EDIT_TRANSFER_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + ("edit_transfer." + "fxml");
    public static final String ADD_EXPENSE_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "add_expense.fxml";
    public static final String ARCHIVED_WALLETS_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "archived_wallets.fxml";
    public static final String TRANSFERS_FXML = UI_DIALOG_WALLETTRANSACTION_PATH + "transfers.fxml";
    public static final String EDIT_TRANSACTION_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "edit_transaction.fxml";
    public static final String REMOVE_TRANSACTION_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "remove_transaction.fxml";
    public static final String CHANGE_WALLET_TYPE_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "change_wallet_type.fxml";
    public static final String CHANGE_WALLET_BALANCE_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "change_wallet_balance.fxml";
    public static final String RENAME_WALLET_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "rename_wallet.fxml";
    public static final String RECURRING_TRANSACTIONS_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "recurring_transaction.fxml";
    public static final String ADD_RECURRING_TRANSACTION_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "add_recurring_transaction.fxml";
    public static final String EDIT_RECURRING_TRANSACTION_FXML =
            UI_DIALOG_WALLETTRANSACTION_PATH + "edit_recurring_transaction.fxml";
    public static final String UI_DIALOG_FINANCIALPLANNING_PATH =
            UI_DIALOG_PATH + "financialplanning/";
    // UI financial planning package
    public static final String ADD_BUDGET_GROUP_FXML =
            UI_DIALOG_FINANCIALPLANNING_PATH + "add_budget_group.fxml";
    public static final String EDIT_BUDGET_GROUP_FXML =
            UI_DIALOG_FINANCIALPLANNING_PATH + "edit_budget_group.fxml";
    public static final String ADD_PLAN_FXML = UI_DIALOG_FINANCIALPLANNING_PATH + "add_plan.fxml";
    public static final String EDIT_PLAN_FXML = UI_DIALOG_FINANCIALPLANNING_PATH + "edit_plan.fxml";
    public static final String UI_COMMON_PATH = "/ui/common/";
    public static final String CSS_SCENE_PATH = "/css/scene/";
    public static final String CSS_COMMON_PATH = "/css/common/";
    public static final String CSS_COMPONENT_PATH = "/css/component/";
    // UI main package
    public static final String MAIN_FXML = UI_MAIN_PATH + "main.fxml";
    public static final String HOME_FXML = UI_MAIN_PATH + "home.fxml";
    public static final String WALLET_FXML = UI_MAIN_PATH + "wallet.fxml";
    public static final String CREDIT_CARD_FXML = UI_MAIN_PATH + "credit_card.fxml";
    public static final String TRANSACTION_FXML = UI_MAIN_PATH + "transaction.fxml";
    public static final String GOALS_FXML = UI_MAIN_PATH + "goals.fxml";
    public static final String PLANS_FXML = UI_MAIN_PATH + "plans.fxml";
    public static final String GOALS_AND_PLANS_FXML = UI_MAIN_PATH + "goals_and_plans.fxml";
    public static final String SAVINGS_FXML = UI_MAIN_PATH + "savings.fxml";
    public static final String CSV_IMPORT_FXML = UI_MAIN_PATH + "csv_import.fxml";
    public static final String SETTINGS_FXML = UI_MAIN_PATH + "settings.fxml";
    public static final String SPLASH_SCREEN_FXML = UI_MAIN_PATH + ("splash_screen." + "fxml");
    // UI dialog package
    public static final String ADD_CATEGORY_FXML = UI_DIALOG_PATH + ("add_category." + "fxml");
    public static final String REMOVE_CATEGORY_FXML = UI_DIALOG_PATH + "remove_category.fxml";
    public static final String MANAGE_CATEGORY_FXML = UI_DIALOG_PATH + "manage_category.fxml";
    public static final String EDIT_CATEGORY_FXML = UI_DIALOG_PATH + "edit_category.fxml";
    public static final String ADD_CALENDAR_EVENT_FXML = UI_DIALOG_PATH + "add_calendar_event.fxml";

    // UI common package
    public static final String WALLET_FULL_PANE_FXML = UI_COMMON_PATH + "wallet_full_pane.fxml";
    public static final String GOAL_FULL_PANE_FXML = UI_COMMON_PATH + "goal_full_pane.fxml";
    public static final String BUDGET_GROUP_PANE_FXML = UI_COMMON_PATH + "budget_group_pane.fxml";
    public static final String BUDGET_GROUP_PREVIEW_PANE_FXML =
            UI_COMMON_PATH + "budget_group_preview_pane.fxml";

    public static final String RESUME_PANE_FXML = UI_COMMON_PATH + "resume_pane.fxml";
    public static final String CRC_PANE_FXML = UI_COMMON_PATH + ("credit_card_pane." + "fxml");
    public static final String FUNDAMENTAL_METRIC_PANE_FXML =
            UI_COMMON_PATH + "fundamental_metric_pane.fxml";

    public static final String CALCULATOR_FXML = UI_COMMON_PATH + "calculator.fxml";
    public static final String CALENDAR_FXML = UI_COMMON_PATH + "calendar.fxml";

    // Icons
    public static final String HOME_EXPENSE_ICON = COMMON_ICONS_PATH + "expense.png";
    public static final String HOME_INCOME_ICON = COMMON_ICONS_PATH + "income.png";
    public static final String SUCCESS_ICON = COMMON_ICONS_PATH + "success.png";
    public static final String DEFAULT_ICON = COMMON_ICONS_PATH + "default.png";
    public static final String TROPHY_ICON = COMMON_ICONS_PATH + "trophy.png";
    public static final String HIDE_ICON = COMMON_ICONS_PATH + "hide.png";
    public static final String SHOW_ICON = COMMON_ICONS_PATH + "show.png";

    // GIFs
    public static final String LOADING_GIF = GIF_PATH + "loading.gif";
    public static final String SAVINGS_SCREEN_SYNC_PRICES_BUTTON_DEFAULT_ICON =
            COMMON_ICONS_PATH + "synchronize.png";

    // CSS
    public static final String MAIN_STYLE_SHEET = CSS_SCENE_PATH + "main.css";
    public static final String HOME_STYLE_SHEET = CSS_SCENE_PATH + "home.css";
    public static final String WALLET_STYLE_SHEET = CSS_SCENE_PATH + "wallet.css";
    public static final String CREDIT_CARD_STYLE_SHEET = CSS_SCENE_PATH + "credit-card.css";
    public static final String TRANSACTION_STYLE_SHEET = CSS_SCENE_PATH + "transaction.css";
    public static final String GOALS_STYLE_SHEET = CSS_SCENE_PATH + "goals.css";
    public static final String PLANS_STYLE_SHEET = CSS_SCENE_PATH + "plans.css";
    public static final String GOALS_AND_PLANS_STYLE_SHEET = CSS_SCENE_PATH + "goals_and_plans.css";
    public static final String SAVINGS_STYLE_SHEET = CSS_SCENE_PATH + "savings.css";
    public static final String CSV_IMPORT_STYLE_SHEET = CSS_SCENE_PATH + "csv_import.css";
    public static final String SETTINGS_STYLE_SHEET = CSS_SCENE_PATH + "settings.css";
    public static final String CHARTS_COLORS_STYLE_SHEET = CSS_COMPONENT_PATH + "charts.css";

    // Component styles
    public static final Integer CHARTS_COLORS_COUNT = 20;
    public static final String CHARTS_COLORS_PREFIX = "chart-color-";
    public static final String CHARTS_LEGEND_RECT_STYLE = "legend-color";

    public static final String PROGRESS_BAR_RED_COLOR_STYLE = "progress-bar-red";
    public static final String PROGRESS_BAR_YELLOW_COLOR_STYLE = "progress-bar-yellow";
    public static final String PROGRESS_BAR_GREEN_COLOR_STYLE = "progress-bar-green";

    public static final String COMMON_STYLE_SHEET = CSS_COMMON_PATH + "common-styles.css";
    public static final String TIMELINE_CHART_STYLE_SHEET =
            CSS_COMPONENT_PATH + "timeline-chart.css";

    // Info styles from common-styles.css
    public static final String INFO_LABEL_RED_STYLE = "info-label-red";
    public static final String INFO_LABEL_GREEN_STYLE = "info-label-green";
    public static final String INFO_LABEL_YELLOW_STYLE = "info-label-yellow";
    public static final String INFO_LABEL_NEUTRAL_STYLE = "info-label-neutral";

    // Main pane styles
    public static final String SIDEBAR_SELECTED_BUTTON_STYLE = "sidebar-button-selected";
    public static final String NEGATIVE_BALANCE_STYLE = "negative-balance";
    public static final String POSITIVE_BALANCE_STYLE = "positive-balance";
    public static final String NEUTRAL_BALANCE_STYLE = "neutral-balance";

    // Home pane styles
    public static final String HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE = "income-item";
    public static final String HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE = "expense-item";

    public static final String HOME_CREDIT_CARD_ITEM_STYLE = "credit-card-item";

    // Top Performers Table Styles
    public static final Double TOP_PERFORMERS_ASSET_COLUMN_WIDTH = 90.0;
    public static final Double TOP_PERFORMERS_RETURN_COLUMN_WIDTH = 90.0;
    public static final Double TOP_PERFORMERS_VALUE_COLUMN_WIDTH = 90.0;
    public static final String CUSTOM_TABLE_TITLE_STYLE = "custom-table-title";
    public static final String CUSTOM_TABLE_HEADER_STYLE = "custom-table-header";
    public static final String CUSTOM_TABLE_CELL_STYLE = "custom-table-cell";

    // Allocation Panel Styles
    public static final String ALLOCATION_TYPE_LABEL_STYLE = "allocation-type-label";
    public static final String ALLOCATION_PROGRESS_BAR_STYLE = "allocation-progress-bar";
    public static final String ALLOCATION_FILLED_BAR_CRITICAL_LOW_STYLE =
            "allocation-filled-bar-critical-low";
    public static final String ALLOCATION_FILLED_BAR_WARNING_LOW_STYLE =
            "allocation-filled-bar-warning-low";
    public static final String ALLOCATION_FILLED_BAR_ON_TARGET_STYLE =
            "allocation-filled-bar-on-target";
    public static final String ALLOCATION_FILLED_BAR_WARNING_HIGH_STYLE =
            "allocation-filled-bar-warning-high";
    public static final String ALLOCATION_FILLED_BAR_CRITICAL_HIGH_STYLE =
            "allocation-filled-bar-critical-high";
    public static final String ALLOCATION_INFO_LABEL_STYLE = "allocation-info-label";
    public static final String ALLOCATION_DIFF_LABEL_STYLE = "allocation-diff-label";
    public static final String ALLOCATION_DIFF_CRITICAL_LOW_STYLE = "allocation-diff-critical-low";
    public static final String ALLOCATION_DIFF_WARNING_LOW_STYLE = "allocation-diff-warning-low";
    public static final String ALLOCATION_DIFF_ON_TARGET_STYLE = "allocation-diff-on-target";
    public static final String ALLOCATION_DIFF_WARNING_HIGH_STYLE = "allocation-diff-warning-high";
    public static final String ALLOCATION_DIFF_CRITICAL_HIGH_STYLE =
            "allocation-diff-critical-high";

    // Allocation Panel Thresholds
    public static final Double ALLOCATION_CRITICAL_LOW_THRESHOLD = 0.0;
    public static final Double ALLOCATION_WARNING_LOW_THRESHOLD = 80.0;
    public static final Double ALLOCATION_ON_TARGET_LOW_THRESHOLD = 95.0;
    public static final Double ALLOCATION_ON_TARGET_HIGH_THRESHOLD = 105.0;
    public static final Double ALLOCATION_WARNING_HIGH_THRESHOLD = 120.0;

    // Credit Card Item Styles
    public static final String HOME_CREDIT_CARD_ITEM_NAME_STYLE = "credit-card-item-name";
    public static final String HOME_CREDIT_CARD_ITEM_BALANCE_STYLE = "credit-card-item-balance";
    public static final String HOME_CREDIT_CARD_ITEM_DIGITS_STYLE = "credit-card-item-digits";
    public static final String HOME_CREDIT_CARD_ITEM_OPERATOR_STYLE = "credit-card-item-operator";

    public static final String HOME_WALLET_ITEM_STYLE = "wallet-item";
    public static final String HOME_WALLET_ITEM_NAME_STYLE = "wallet-item-name";
    public static final String HOME_WALLET_ITEM_BALANCE_STYLE = "wallet-item-balance";
    public static final String HOME_WALLET_TYPE_STYLE = "wallet-item-type";
    public static final String HOME_VIRTUAL_WALLET_INFO_STYLE = "virtual-wallet-info";

    public static final String TOOLTIP_STYLE = "tooltip";

    public static final String TOTAL_BALANCE_VALUE_LABEL_STYLE = "total-balance-value-label";
    public static final String TOTAL_BALANCE_FORESEEN_LABEL_STYLE = "total-balance-foreseen-label";

    // Wallet pane styles
    public static final String WALLET_TOTAL_BALANCE_WALLETS_LABEL_STYLE =
            "total-balance-wallets-label";
    public static final String WALLET_CHECK_BOX_STYLE = "check-box";

    // Icons sizes
    public static final Integer WALLET_TYPE_ICONS_SIZE = 42; // 42x42 px
    public static final Integer CRC_OPERATOR_ICONS_SIZE = 42; // 42x42 px
    public static final Integer HOME_LAST_TRANSACTIONS_ICON_SIZE = 32; // 32x32 px

    // Home scene constants
    public static final Integer HOME_LAST_TRANSACTIONS_SIZE = 15;
    public static final Integer HOME_LAST_TRANSACTIONS_DESCRIPTION_LABEL_WIDTH = 290;
    public static final Integer HOME_LAST_TRANSACTIONS_VALUE_LABEL_WIDTH = 70;
    public static final Integer HOME_LAST_TRANSACTIONS_DATE_LABEL_WIDTH = 80;
    public static final Integer HOME_LAST_TRANSACTIONS_WALLET_LABEL_WIDTH = 100;
    public static final Integer HOME_LAST_TRANSACTIONS_CATEGORY_LABEL_WIDTH = 100;
    public static final Integer HOME_LAST_TRANSACTIONS_STATUS_LABEL_WIDTH = 90;
    public static final Integer HOME_PANES_ITEMS_PER_PAGE = 2;

    public static final Double EPSILON = 1e-6;
    public static final Double ONE_SECOND_IN_NS = 1_000_000_000.0;

    public static final Double NEGATIVE_PERCENTAGE_THRESHOLD = -1000.0;

    // Credit card
    public static final Integer MAX_BILLING_DUE_DAY = 28;
    public static final Integer INSTALLMENTS_FIELD_MAX_DIGITS = 3;
    public static final Short MAX_INSTALLMENTS = 999;

    // Animation constants
    public static final Double MENU_COLLAPSED_WIDTH = 80.0;
    public static final Double MENU_EXPANDED_WIDTH = 220.0;

    public static final Integer XYBAR_CHART_MONTHS = 12;
    public static final Integer XYBAR_CHART_FUTURE_MONTHS = 6;
    public static final Integer PL_CHART_MONTHS = 18;
    public static final Integer PL_CHART_FUTURE_MONTHS = 3;
    public static final Integer MONTH_RESUME_FUTURE_MONTHS = 6;

    public static final Integer CRC_XYBAR_CHART_MAX_MONTHS = 25;
    public static final Integer XYBAR_CHART_TICKS = 6;

    public static final Double FADE_IN_ANIMATION_DURATION = 1.0; // s
    public static final Double FADE_OUT_ANIMATION_DURATION = 1.0; // s
    public static final Double SLIDE_ANIMATION_DURATION = 1.0; // s

    public static final Double MENU_ANIMATION_DURATION = 200.0; // ms
    public static final Integer XYBAR_CHART_ANIMATION_FRAMES = 30;
    public static final Double XYBAR_CHART_ANIMATION_DURATION = 0.3; // s
    public static final Double TOOLTIP_ANIMATION_DURATION = 0.5; // s
    public static final Double TOOLTIP_ANIMATION_DELAY = 0.5; // s

    public static final Integer HOME_ITEM_NODE_NAME_MAX_LENGTH = 100;

    // Calendar config
    public static final Integer YEAR_RESUME_FUTURE_YEARS = 2;
    public static final Integer NON_LEAP_YEAR_FEBRUARY_DAYS = 28;
    public static final Integer WEEK_DAYS = 7;

    public static final Font CALENDAR_WEEKDAY_FONT_CONFIG = Font.font("Arial", FontWeight.BOLD, 14);

    public static final Font CALENDAR_DATE_FONT_CONFIG = Font.font("Arial", FontWeight.BOLD, 14);

    public static final Double CALENDAR_CELL_BORDER_WIDTH = 0.5;
    public static final Double CALENDAR_CELL_EXTERNAL_BORDER_WIDTH = 2.0;

    // Circular progress bar on the goal pane
    public static final Double GOAL_PANE_PROGRESS_BAR_RADIUS = 80.0;
    public static final Double GOAL_PANE_PROGRESS_BAR_WIDTH = 8.0;

    public static final Integer SUGGESTIONS_MAX_ITEMS = 5;

    // WARNING: Do not change this value. If you do, update too on the database
    public static final String GOAL_DEFAULT_WALLET_TYPE_NAME = "Goal";

    // Enough time for you to become poor :)
    // Or rich, who knows?
    // WARNING: Do not change this value. If you do, update too on the database
    public static final LocalDate RECURRING_TRANSACTION_DEFAULT_END_DATE =
            LocalDate.of(2100, 12, 31);

    public static final LocalTime RECURRING_TRANSACTION_DEFAULT_TIME = LocalTime.of(23, 59, 59, 0);

    public static final LocalTime RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME =
            LocalTime.of(0, 0, 0, 0);

    public static final String NA_DATA = "N/A";

    // Date formats
    public static final String DB_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_FORMAT_NO_TIME = "yyyy-MM-dd";
    public static final String SHORT_DATE_FORMAT_NO_TIME = "yy-MM-dd";
    public static final String DATE_FORMAT_WITH_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final String DB_MONTH_YEAR_FORMAT = "yyyy-MM";

    public static final DateTimeFormatter DB_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DB_DATE_FORMAT);

    public static final DateTimeFormatter DATE_FORMATTER_NO_TIME =
            DateTimeFormatter.ofPattern(DATE_FORMAT_NO_TIME);

    public static final DateTimeFormatter SHORT_DATE_FORMATTER_NO_TIME =
            DateTimeFormatter.ofPattern(SHORT_DATE_FORMAT_NO_TIME);

    public static final DateTimeFormatter DATE_FORMATTER_WITH_TIME =
            DateTimeFormatter.ofPattern(DATE_FORMAT_WITH_TIME);

    public static final DateTimeFormatter DB_MONTH_YEAR_FORMATTER =
            DateTimeFormatter.ofPattern(DB_MONTH_YEAR_FORMAT);

    // Define the pattern for positive and negative currency values
    public static final String CURRENCY_FORMAT = "R$ #,##0.00;- R$ #,##0.00";

    // Percentage with two decimal places
    public static final String PERCENTAGE_FORMAT = "0.00";

    public static final String CREDIT_CARD_NUMBER_FORMAT = "**** **** **** ####";

    // Regex
    public static final String DIGITS_ONLY_REGEX = "\\d*";
    public static final String MONETARY_VALUE_REGEX = "\\d*\\.?\\d{0,2}";
    public static final String SIGNED_MONETARY_VALUE_REGEX = "-?\\d*\\.?\\d{0,2}";
    public static final String PERCENTAGE_REGEX = "\\d{0,2}(\\.\\d{0,2})?|100(\\.00)?";
    public static final String INTEREST_RATE_REGEX = "\\d{0,3}(\\.\\d{0,4})?";

    public static final Integer INVESTMENT_CALCULATION_PRECISION = 8;
    public static final String INVESTMENT_VALUE_REGEX =
            "\\d*\\.?\\d{0," + INVESTMENT_CALCULATION_PRECISION + "}";

    // Yahoo Finance API constants
    public static final String IBOVESPA_TICKER = "^BVSP";
    public static final String DOLLAR_TICKER = "USDBRL=X";
    public static final String EURO_TICKER = "EURBRL=X";

    public static final String GOLD_TICKER = "GC=F";
    public static final String SOYBEAN_TICKER = "ZS=F";
    public static final String COFFEE_ARABICA_TICKER = "KC=F";
    public static final String WHEAT_TICKER = "ZW=F";
    public static final String OIL_BRENT_TICKER = "BZ=F";

    public static final String BITCOIN_TICKER = "BTC-USD";
    public static final String ETHEREUM_TICKER = "ETH-USD";

    public static final class TranslationKeys {
        private TranslationKeys() {
            // Prevent instantiation
        }

        // App
        public static final String APP_TITLE = "app.title";

        // Main menu
        public static final String MAIN_MENU = "main.menu";
        public static final String MAIN_HOME = "main.home";
        public static final String MAIN_WALLET = "main.wallet";
        public static final String MAIN_CREDIT_CARD = "main.creditCard";
        public static final String MAIN_TRANSACTION = "main.transaction";
        public static final String MAIN_GOALS_AND_PLANS = "main.goalsAndPlans";
        public static final String MAIN_SAVINGS = "main.savings";
        public static final String MAIN_SETTINGS = "main.settings";
        public static final String MAIN_CALENDAR = "main.calendar";
        public static final String MAIN_CALCULATOR = "main.calculator";

        public static final String UIUTILS_FORMAT_PERCENTAGE_TOO_MUCH_NEGATIVE =
                "uiutils.formatPercentage.tooMuchNegative";

        // Dialogs
        public static final String DIALOG_CONFIRMATION_TITLE = "dialog.confirmation.title";
        public static final String DIALOG_INFO_TITLE = "dialog.info.title";
        public static final String DIALOG_ERROR_TITLE = "dialog.error.title";
        public static final String DIALOG_SUCCESS_TITLE = "dialog.success.title";
        public static final String DIALOG_BUTTON_YES = "dialog.button.yes";
        public static final String DIALOG_BUTTON_NO = "dialog.button.no";
        public static final String DIALOG_BUTTON_OK = "dialog.button.ok";
        public static final String DIALOG_BUTTON_CANCEL = "dialog.button.cancel";
        public static final String DIALOG_BUTTON_SAVE = "dialog.button.save";
        public static final String DIALOG_BUTTON_ADD = "dialog.button.add";
        public static final String DIALOG_BUTTON_EDIT = "dialog.button.edit";
        public static final String DIALOG_BUTTON_DELETE = "dialog.button.delete";
        public static final String DIALOG_BUTTON_CREATE = "dialog.button.create";
        public static final String DIALOG_BUTTON_CONFIRM = "dialog.button.confirm";
        public static final String DIALOG_BUTTON_CLOSE = "dialog.button.close";
        public static final String DIALOG_BUTTON_UNARCHIVE = "dialog.button.unarchive";
        public static final String DIALOG_BUTTON_ARCHIVE = "dialog.button.archive";

        // Settings
        public static final String SETTINGS_LANGUAGE = "settings.language";

        // Home
        public static final String HOME_WALLET_TITLE = "home.wallet.title";
        public static final String HOME_CREDIT_CARD_TITLE = "home.creditCard.title";
        public static final String HOME_MONEY_FLOW_TITLE = "home.moneyFlow.title";
        public static final String HOME_RESUME_TITLE = "home.resume.title";
        public static final String HOME_TRANSACTIONS_TITLE = "home.transactions.title";
        public static final String HOME_TRANSACTIONS_TABLE_TITLE = "home.transactions.table.title";
        public static final String HOME_WALLET_VIRTUAL_WALLET = "home.wallet.virtualWallet";
        public static final String HOME_WALLET_TOOLTIP_WALLET_NAME =
                "home.wallet.tooltip.walletName";
        public static final String HOME_WALLET_TOOLTIP_WALLET_TYPE =
                "home.wallet.tooltip.walletType";
        public static final String HOME_WALLET_TOOLTIP_WALLET_BALANCE =
                "home.wallet.tooltip.walletBalance";
        public static final String HOME_WALLET_TOOLTIP_IS_VIRTUAL_WALLET =
                "home.wallet.tooltip.isVirtualWallet";
        public static final String HOME_WALLET_TOOLTIP_NOT_VIRTUAL_WALLET =
                "home.wallet.tooltip.notVirtualWallet";
        public static final String HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_NAME =
                "home.creditCard.tooltip.creditCardName";
        public static final String HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_OPERATOR =
                "home.creditCard.tooltip.creditCardOperator";
        public static final String HOME_CREDIT_CARD_TOOLTIP_AVAILABLE_CREDIT =
                "home.creditCard.tooltip.availableCredit";
        public static final String HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_NUMBER =
                "home.creditCard.tooltip.creditCardNumber";
        public static final String HOME_NET_WORTH_TITLE = "home.netWorth.title";
        public static final String HOME_NET_WORTH_ASSETS = "home.netWorth.assets";
        public static final String HOME_NET_WORTH_LIABILITIES = "home.netWorth.liabilities";
        public static final String HOME_NET_WORTH_NET_WORTH = "home.netWorth.netWorth";

        // Wallet
        public static final String WALLET_ALL_WALLETS = "wallet.allWallets";
        public static final String WALLET_DIALOG_ADD_TRANSFER_TITLE =
                "wallet.dialog.addTransfer.title";
        public static final String WALLET_DIALOG_ADD_WALLET_TITLE = "wallet.dialog.addWallet.title";
        public static final String WALLET_DIALOG_ARCHIVED_WALLETS_TITLE =
                "wallet.dialog.archivedWallets.title";
        public static final String WALLET_DIALOG_VIEW_TRANSFERS_TITLE =
                "wallet.dialog.viewTransfers.title";
        public static final String WALLET_TOTAL_BALANCE_FORESEEN = "wallet.totalBalance.foreseen";
        public static final String WALLET_TOTAL_BALANCE_CORRESPONDS_TO =
                "wallet.totalBalance.correspondsTo";
        public static final String WALLET_TOTAL_BALANCE_TITLE = "wallet.totalBalance.title";
        public static final String WALLET_BUTTON_ADD_TRANSFER = "wallet.button.addTransfer";
        public static final String WALLET_BUTTON_ADD_WALLET = "wallet.button.addWallet";
        public static final String WALLET_BUTTON_WALLET_ARCHIVE = "wallet.button.walletArchive";
        public static final String WALLET_BUTTON_TRANSFERS = "wallet.button.transfers";
        public static final String WALLET_WALLETS_TITLE = "wallet.wallets.title";
        public static final String WALLET_MONEY_FLOW_TITLE = "wallet.moneyFlow.title";
        public static final String WALLET_BALANCE_BY_TYPE_TITLE = "wallet.balanceByType.title";

        // Credit Card
        public static final String CREDIT_CARD_TOTAL_DEBTS_TITLE = "creditCard.totalDebts.title";
        public static final String CREDIT_CARD_TOTAL_DEBTS_PENDING_PAYMENTS =
                "creditCard.totalDebts.pendingPayments";
        public static final String CREDIT_CARD_BUTTON_ADD_DEBT = "creditCard.button.addDebt";
        public static final String CREDIT_CARD_BUTTON_ADD_CREDIT_CARD =
                "creditCard.button.addCreditCard";
        public static final String CREDIT_CARD_BUTTON_CREDIT_CARD_ARCHIVE =
                "creditCard.button.creditCardArchive";
        public static final String CREDIT_CARD_DEBTS_FLOW_TITLE = "creditCard.debtsFlow.title";
        public static final String CREDIT_CARD_DEBTS_LIST_TITLE = "creditCard.debtsList.title";
        public static final String CREDIT_CARD_DEBTS_LIST_BUTTON_EDIT =
                "creditCard.debtsList.button.edit";
        public static final String CREDIT_CARD_DEBTS_LIST_BUTTON_DELETE =
                "creditCard.debtsList.button.delete";
        public static final String CREDIT_CARD_DEBTS_LIST_SEARCH_PLACEHOLDER =
                "creditCard.debtsList.searchPlaceholder";
        public static final String CREDIT_CARD_CREDIT_CARDS_TITLE = "creditCard.creditCards.title";
        public static final String CREDIT_CARD_DIALOG_ADD_CREDIT_CARD_TITLE =
                "creditCard.dialog.addCreditCard.title";
        public static final String CREDIT_CARD_DIALOG_ADD_DEBT_TITLE =
                "creditCard.dialog.addDebt.title";
        public static final String CREDIT_CARD_DIALOG_EDIT_DEBT_TITLE =
                "creditCard.dialog.editDebt.title";
        public static final String CREDIT_CARD_DIALOG_CREDIT_CARD_ARCHIVE_TITLE =
                "creditCard.dialog.creditCardArchive.title";
        public static final String CREDIT_CARD_DIALOG_NO_SELECTION_TITLE =
                "creditCard.dialog.noSelection.title";
        public static final String CREDIT_CARD_DIALOG_NO_SELECTION_EDIT_MESSAGE =
                "creditCard.dialog.noSelection.edit.message";
        public static final String CREDIT_CARD_DIALOG_NO_SELECTION_DELETE_MESSAGE =
                "creditCard.dialog.noSelection.delete.message";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_TITLE =
                "creditCard.dialog.confirmationDelete.title";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_DESCRIPTION =
                "creditCard.dialog.confirmationDelete.description";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_AMOUNT =
                "creditCard.dialog.confirmationDelete.amount";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REGISTER_DATE =
                "creditCard.dialog.confirmationDelete.registerDate";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS =
                "creditCard.dialog.confirmationDelete.installments";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS_PAID =
                "creditCard.dialog.confirmationDelete.installmentsPaid";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CATEGORY =
                "creditCard.dialog.confirmationDelete.category";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CREDIT_CARD =
                "creditCard.dialog.confirmationDelete.creditCard";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REFUND_AMOUNT =
                "creditCard.dialog.confirmationDelete.refundAmount";
        public static final String CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_NO_REFUND_AMOUNT =
                "creditCard.dialog.confirmationDelete.noRefundAmount";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_DEBT_ID =
                "creditCard.debtsList.header.debtId";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_DESCRIPTION =
                "creditCard.debtsList.header.description";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_AMOUNT =
                "creditCard.debtsList.header.amount";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_INSTALLMENT =
                "creditCard.debtsList.header.installment";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_CREDIT_CARD =
                "creditCard.debtsList.header.creditCard";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_CATEGORY =
                "creditCard.debtsList.header.category";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_INVOICE_DATE =
                "creditCard.debtsList.header.invoiceDate";
        public static final String CREDIT_CARD_DEBTS_LIST_HEADER_STATUS =
                "creditCard.debtsList.header.status";
        public static final String CREDIT_CARD_DEBTS_LIST_STATUS_PENDING =
                "creditCard.debtsList.status.pending";
        public static final String CREDIT_CARD_DEBTS_LIST_STATUS_PAID =
                "creditCard.debtsList.status.paid";

        // Transactions
        public static final String TRANSACTION_MONTHLY_RESUME_TITLE =
                "transaction.monthlyResume.title";
        public static final String TRANSACTION_YEARLY_RESUME_TITLE =
                "transaction.yearlyResume.title";
        public static final String TRANSACTION_MONEY_FLOW_BY_CATEGORY_TITLE =
                "transaction.moneyFlowByCategory.title";
        public static final String TRANSACTION_BUTTON_ADD_INCOME = "transaction.button.addIncome";
        public static final String TRANSACTION_BUTTON_ADD_EXPENSE = "transaction.button.addExpense";
        public static final String TRANSACTION_BUTTON_PERIODIC_TRANSACTION =
                "transaction.button.periodicTransaction";
        public static final String TRANSACTION_BUTTON_MANAGE_CATEGORIES =
                "transaction.button.manageCategories";
        public static final String TRANSACTION_DIALOG_ADD_INCOME_TITLE =
                "transaction.dialog.addIncome.title";
        public static final String TRANSACTION_DIALOG_ADD_EXPENSE_TITLE =
                "transaction.dialog.addExpense.title";
        public static final String TRANSACTION_DIALOG_EDIT_TRANSACTION_TITLE =
                "transaction.dialog.editTransaction.title";
        public static final String TRANSACTION_DIALOG_PERIODIC_TRANSACTION_TITLE =
                "transaction.dialog.periodicTransaction.title";
        public static final String TRANSACTION_DIALOG_MANAGE_CATEGORIES_TITLE =
                "transaction.dialog.manageCategories.title";
        public static final String TRANSACTION_DIALOG_NO_SELECTION_TITLE =
                "transaction.dialog.noSelection.title";
        public static final String TRANSACTION_DIALOG_NO_SELECTION_EDIT_MESSAGE =
                "transaction.dialog.noSelection.edit.message";
        public static final String TRANSACTION_DIALOG_NO_SELECTION_DELETE_MESSAGE =
                "transaction.dialog.noSelection.delete.message";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_TITLE =
                "transaction.dialog.confirmationDelete.title";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_DESCRIPTION =
                "transaction.dialog.confirmationDelete.description";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_AMOUNT =
                "transaction.dialog.confirmationDelete.amount";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_REGISTER_DATE =
                "transaction.dialog.confirmationDelete.registerDate";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_STATUS =
                "transaction.dialog.confirmationDelete.status";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_WALLET =
                "transaction.dialog.confirmationDelete.wallet";
        public static final String TRANSACTION_DIALOG_CONFIRMATION_DELETE_WALLET_BALANCE =
                "transaction.dialog.confirmationDelete.walletBalance";
        public static final String
                TRANSACTION_DIALOG_CONFIRMATION_DELETE_WALLET_BALANCE_AFTER_TRANSACTION =
                        "transaction.dialog.confirmationDelete.walletBalanceAfterTransaction";
        public static final String TRANSACTION_TRANSACTION_LIST_TITLE =
                "transaction.transactionList.title";
        public static final String TRANSACTION_TRANSACTION_LIST_BUTTON_EDIT =
                "transaction.transactionList.button.edit";
        public static final String TRANSACTION_TRANSACTION_LIST_BUTTON_DELETE =
                "transaction.transactionList.button.delete";
        public static final String TRANSACTION_TRANSACTION_LIST_TYPE =
                "transaction.transactionList.type";
        public static final String TRANSACTION_TRANSACTION_LIST_START_DATE =
                "transaction.transactionList.startDate";
        public static final String TRANSACTION_TRANSACTION_LIST_END_DATE =
                "transaction.transactionList.endDate";
        public static final String TRANSACTION_TRANSACTION_LIST_SEARCH_PLACEHOLDER =
                "transaction.transactionList.searchPlaceholder";

        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_ID =
                "transaction.transactionList.header.id";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_DESCRIPTION =
                "transaction.transactionList.header.description";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_AMOUNT =
                "transaction.transactionList.header.amount";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_WALLET =
                "transaction.transactionList.header.wallet";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_DATE =
                "transaction.transactionList.header.date";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_TYPE =
                "transaction.transactionList.header.type";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_CATEGORY =
                "transaction.transactionList.header.category";
        public static final String TRANSACTION_TRANSACTION_LIST_HEADER_STATUS =
                "transaction.transactionList.header.status";
        public static final String TRANSACTION_FILTER_ALL = "transaction.filter.all";

        // Wallet types
        public static final String WALLET_TYPE_CHECKING = "wallet.type.checking";
        public static final String WALLET_TYPE_SAVINGS = "wallet.type.savings";
        public static final String WALLET_TYPE_BROKER = "wallet.type.broker";
        public static final String WALLET_TYPE_CRIPTOCURRENCY = "wallet.type.criptocurrency";
        public static final String WALLET_TYPE_FOOD_VOUCHER = "wallet.type.foodVoucher";
        public static final String WALLET_TYPE_MEAL_VOUCHER = "wallet.type.mealVoucher";
        public static final String WALLET_TYPE_GOAL = "wallet.type.goal";
        public static final String WALLET_TYPE_WALLET = "wallet.type.wallet";
        public static final String WALLET_TYPE_OTHERS = "wallet.type.others";

        // Transaction types
        public static final String TRANSACTION_TYPE_EXPENSES = "transaction.type.expenses";
        public static final String TRANSACTION_TYPE_INCOMES = "transaction.type.incomes";
        public static final String TRANSACTION_TYPE_INCOME = "transaction.type.income";
        public static final String TRANSACTION_TYPE_EXPENSE = "transaction.type.expense";

        // Ticker types
        public static final String TICKER_TYPE_STOCK = "ticker.type.stock";
        public static final String TICKER_TYPE_FUND = "ticker.type.fund";
        public static final String TICKER_TYPE_CRYPTO = "ticker.type.crypto";
        public static final String TICKER_TYPE_REIT = "ticker.type.reit";
        public static final String TICKER_TYPE_ETF = "ticker.type.etf";

        // Asset types
        public static final String ASSET_TYPE_STOCK = "ticker.type.stock";
        public static final String ASSET_TYPE_FUND = "ticker.type.fund";
        public static final String ASSET_TYPE_CRYPTO = "ticker.type.crypto";
        public static final String ASSET_TYPE_REIT = "ticker.type.reit";
        public static final String ASSET_TYPE_ETF = "ticker.type.etf";
        public static final String ASSET_TYPE_BOND = "bond.type.bond";

        // Bond types
        public static final String BOND_TYPE_CDB = "bond.type.cdb";
        public static final String BOND_TYPE_LCI = "bond.type.lci";
        public static final String BOND_TYPE_LCA = "bond.type.lca";
        public static final String BOND_TYPE_TREASURY_PREFIXED = "bond.type.treasuryPrefixed";
        public static final String BOND_TYPE_TREASURY_POSTFIXED = "bond.type.treasuryPostfixed";
        public static final String BOND_TYPE_INTERNATIONAL = "bond.type.international";
        public static final String BOND_TYPE_OTHER = "bond.type.other";

        // Interest types
        public static final String INTEREST_TYPE_FIXED = "interest.type.fixed";
        public static final String INTEREST_TYPE_FLOATING = "interest.type.floating";
        public static final String INTEREST_TYPE_ZERO_COUPON = "interest.type.zeroCoupon";

        // Interest indices
        public static final String INTEREST_INDEX_CDI = "interest.index.cdi";
        public static final String INTEREST_INDEX_SELIC = "interest.index.selic";
        public static final String INTEREST_INDEX_IPCA = "interest.index.ipca";
        public static final String INTEREST_INDEX_LIBOR = "interest.index.libor";
        public static final String INTEREST_INDEX_SOFR = "interest.index.sofr";
        public static final String INTEREST_INDEX_OTHER = "interest.index.other";

        // Credit Card Credit types
        public static final String CREDIT_CARD_CREDIT_TYPE_CASHBACK =
                "creditCard.credit.type.cashback";
        public static final String CREDIT_CARD_CREDIT_TYPE_REFUND = "creditCard.credit.type.refund";
        public static final String CREDIT_CARD_CREDIT_TYPE_REWARD = "creditCard.credit.type.reward";

        // Operation types
        public static final String OPERATION_TYPE_BUY = "operation.type.buy";
        public static final String OPERATION_TYPE_SELL = "operation.type.sell";

        // Transaction status
        public static final String TRANSACTION_STATUS_PENDING = "transaction.status.pending";
        public static final String TRANSACTION_STATUS_CONFIRMED = "transaction.status.confirmed";

        // Recurring Transaction status
        public static final String RECURRING_TRANSACTION_STATUS_ACTIVE =
                "recurringtransaction.status.active";
        public static final String RECURRING_TRANSACTION_STATUS_INACTIVE =
                "recurringtransaction.status.inactive";

        // Recurring Transaction frequency
        public static final String RECURRING_TRANSACTION_FREQUENCY_DAILY =
                "recurringtransaction.frequency.daily";
        public static final String RECURRING_TRANSACTION_FREQUENCY_WEEKLY =
                "recurringtransaction.frequency.weekly";
        public static final String RECURRING_TRANSACTION_FREQUENCY_MONTHLY =
                "recurringtransaction.frequency.monthly";
        public static final String RECURRING_TRANSACTION_FREQUENCY_YEARLY =
                "recurringtransaction.frequency.yearly";

        // Goals and Plans
        public static final String GOALS_PLANS_TAB_GOALS = "goalsPlans.tab.goals";
        public static final String GOALS_PLANS_TAB_PLANS = "goalsPlans.tab.plans";

        // Goals
        public static final String GOAL_IN_PROGRESS_TITLE = "goal.inProgress.title";
        public static final String GOAL_ACCOMPLISHED_TITLE = "goal.accomplished.title";
        public static final String GOAL_TITLE = "goal.title";
        public static final String GOAL_BUTTON_ADD_GOAL = "goal.button.addGoal";
        public static final String GOAL_BUTTON_ADD_DEPOSIT = "goal.button.addDeposit";
        public static final String GOAL_BUTTON_EDIT_GOAL = "goal.button.editGoal";
        public static final String GOAL_BUTTON_DELETE_GOAL = "goal.button.deleteGoal";
        public static final String GOAL_FILTER_STATUS = "goal.filter.status";
        public static final String GOAL_FILTER_ALL = "goal.filter.all";
        public static final String GOAL_FILTER_ACTIVE = "goal.filter.active";
        public static final String GOAL_FILTER_COMPLETED = "goal.filter.completed";
        public static final String GOAL_FILTER_ARCHIVED = "goal.filter.archived";
        public static final String GOAL_SEARCH_PLACEHOLDER = "goal.searchPlaceholder";
        public static final String GOAL_DIALOG_ADD_GOAL_TITLE = "goal.dialog.addGoal.title";
        public static final String GOAL_DIALOG_EDIT_GOAL_TITLE = "goal.dialog.editGoal.title";
        public static final String GOAL_DIALOG_ADD_TRANSFER_TITLE = "goal.dialog.addTransfer.title";
        public static final String GOAL_DIALOG_NO_SELECTION_TITLE = "goal.dialog.noSelection.title";
        public static final String GOAL_DIALOG_NO_SELECTION_EDIT_MESSAGE =
                "goal.dialog.noSelection.edit.message";
        public static final String GOAL_DIALOG_NO_SELECTION_DELETE_MESSAGE =
                "goal.dialog.noSelection.delete.message";
        public static final String GOAL_DIALOG_NO_SELECTION_ADD_DEPOSIT_MESSAGE =
                "goal.dialog.noSelection.addDeposit.message";
        public static final String GOAL_DIALOG_ARCHIVED_TITLE = "goal.dialog.archived.title";
        public static final String GOAL_DIALOG_ARCHIVED_MESSAGE = "goal.dialog.archived.message";
        public static final String GOAL_DIALOG_HAS_TRANSACTIONS_TITLE =
                "goal.dialog.hasTransactions.title";
        public static final String GOAL_DIALOG_HAS_TRANSACTIONS_MESSAGE =
                "goal.dialog.hasTransactions.message";
        public static final String GOAL_DIALOG_CONFIRMATION_DELETE_TITLE =
                "goal.dialog.confirmationDelete.title";
        public static final String GOAL_DIALOG_CONFIRMATION_DELETE_NAME =
                "goal.dialog.confirmationDelete.name";
        public static final String GOAL_DIALOG_CONFIRMATION_DELETE_INITIAL_AMOUNT =
                "goal.dialog.confirmationDelete.initialAmount";
        public static final String GOAL_DIALOG_CONFIRMATION_DELETE_CURRENT_AMOUNT =
                "goal.dialog.confirmationDelete.currentAmount";
        public static final String GOAL_DIALOG_CONFIRMATION_DELETE_TARGET_AMOUNT =
                "goal.dialog.confirmationDelete.targetAmount";
        public static final String GOAL_DIALOG_CONFIRMATION_DELETE_TARGET_DATE =
                "goal.dialog.confirmationDelete.targetDate";
        public static final String GOAL_TABLE_HEADER_ID = "goal.table.header.id";
        public static final String GOAL_TABLE_HEADER_NAME = "goal.table.header.name";
        public static final String GOAL_TABLE_HEADER_INITIAL_AMOUNT =
                "goal.table.header.initialAmount";
        public static final String GOAL_TABLE_HEADER_CURRENT_AMOUNT =
                "goal.table.header.currentAmount";
        public static final String GOAL_TABLE_HEADER_TARGET_AMOUNT =
                "goal.table.header.targetAmount";
        public static final String GOAL_TABLE_HEADER_PROGRESS = "goal.table.header.progress";
        public static final String GOAL_TABLE_HEADER_TARGET_DATE = "goal.table.header.targetDate";
        public static final String GOAL_TABLE_HEADER_COMPLETION_DATE =
                "goal.table.header.completionDate";
        public static final String GOAL_TABLE_HEADER_STATUS = "goal.table.header.status";
        public static final String GOAL_TABLE_HEADER_MONTHS_UNTIL_TARGET =
                "goal.table.header.monthsUntilTarget";
        public static final String GOAL_TABLE_HEADER_RECOMMENDED_MONTHLY_DEPOSIT =
                "goal.table.header.recommendedMonthlyDeposit";
        public static final String GOAL_TABLE_TOOLTIP_MOTIVATION = "goal.table.tooltip.motivation";
        public static final String GOAL_STATUS_ACTIVE = "goal.status.active";
        public static final String GOAL_STATUS_COMPLETED = "goal.status.completed";
        public static final String GOAL_STATUS_ARCHIVED = "goal.status.archived";

        // Plans
        public static final String PLAN_TITLE = "plan.title";
        public static final String PLAN_BASE_MONTHLY_INCOME = "plan.baseMonthlyIncome";
        public static final String PLAN_BUTTON_NEW_PLAN = "plan.button.newPlan";
        public static final String PLAN_BUTTON_EDIT_PLAN = "plan.button.editPlan";
        public static final String PLAN_DISTRIBUTION_TITLE = "plan.planDistribution.title";
        public static final String PLAN_BUDGET_GROUPS_TITLE = "plan.budgetGroups.title";
        public static final String PLAN_DIALOG_ADD_PLAN_TITLE = "plan.dialog.addPlan.title";
        public static final String PLAN_DIALOG_EDIT_PLAN_TITLE = "plan.dialog.editPlan.title";
        public static final String PLAN_DIALOG_NO_ACTIVE_PLAN_TITLE =
                "plan.dialog.noActivePlan.title";
        public static final String PLAN_DIALOG_NO_ACTIVE_PLAN_MESSAGE =
                "plan.dialog.noActivePlan.message";

        // Savings
        public static final String SAVINGS_TAB_OVERVIEW = "savings.tab.overview";
        public static final String SAVINGS_TAB_STOCKS_FUNDS = "savings.tab.stocksFunds";
        public static final String SAVINGS_TAB_BONDS = "savings.tab.bonds";

        // Savings Overview
        public static final String SAVINGS_OVERVIEW_TOTAL_INVESTED =
                "savings.overview.totalInvested";
        public static final String SAVINGS_OVERVIEW_GAINS_WITH_INTEREST =
                "savings.overview.gainsWithInterest";
        public static final String SAVINGS_OVERVIEW_LOSSES_WITH_DEPRECIATION =
                "savings.overview.lossesWithDepreciation";
        public static final String SAVINGS_OVERVIEW_TOTAL_VALUE = "savings.overview.totalValue";
        public static final String SAVINGS_OVERVIEW_PORTFOLIO = "savings.overview.portfolio";
        public static final String SAVINGS_OVERVIEW_BRAZILIAN_MARKET_INDICATORS =
                "savings.overview.brazilianMarketIndicators";
        public static final String SAVINGS_OVERVIEW_MARKET_QUOTES = "savings.overview.marketQuotes";
        public static final String SAVINGS_OVERVIEW_COMMODITIES = "savings.overview.commodities";
        public static final String SAVINGS_OVERVIEW_LAST_UPDATE = "savings.overview.lastUpdate";
        public static final String SAVINGS_OVERVIEW_IPCA_12_MONTHS =
                "savings.overview.ipca12Months";

        // Savings Stocks & Funds
        public static final String SAVINGS_STOCKS_FUNDS_NET_CAPITAL_INVESTED =
                "savings.stocksFunds.netCapitalInvested";
        public static final String SAVINGS_STOCKS_FUNDS_CURRENT_VALUE =
                "savings.stocksFunds.currentValue";
        public static final String SAVINGS_STOCKS_FUNDS_PROFIT_LOSS =
                "savings.stocksFunds.profitLoss";
        public static final String SAVINGS_STOCKS_FUNDS_DIVIDENDS_RECEIVED =
                "savings.stocksFunds.dividendsReceived";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_REGISTER_TICKER =
                "savings.stocksFunds.button.registerTicker";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_TICKER_ARCHIVE =
                "savings.stocksFunds.button.tickerArchive";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_SHOW_TRANSACTIONS =
                "savings.stocksFunds.button.showTransactions";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_UPDATE_PRICES =
                "savings.stocksFunds.button.updatePrices";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_UPDATING =
                "savings.stocksFunds.button.updating";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_EDIT =
                "savings.stocksFunds.button.edit";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_DELETE =
                "savings.stocksFunds.button.delete";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_BUY_TICKER =
                "savings.stocksFunds.button.buyTicker";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_SELL_TICKER =
                "savings.stocksFunds.button.sellTicker";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_ADD_DIVIDEND =
                "savings.stocksFunds.button.addDividend";
        public static final String SAVINGS_STOCKS_FUNDS_BUTTON_EXCHANGE_CRYPTOS =
                "savings.stocksFunds.button.exchangeCryptos";
        public static final String SAVINGS_STOCKS_FUNDS_FILTER_TYPE =
                "savings.stocksFunds.filter.type";
        public static final String SAVINGS_STOCKS_FUNDS_FILTER_ALL =
                "savings.stocksFunds.filter.all";
        public static final String SAVINGS_STOCKS_FUNDS_SEARCH_PLACEHOLDER =
                "savings.stocksFunds.searchPlaceholder";
        public static final String SAVINGS_STOCKS_FUNDS_PORTFOLIO = "savings.stocksFunds.portfolio";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_ADD_TICKER_TITLE =
                "savings.stocksFunds.dialog.addTicker.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_BUY_TICKER_TITLE =
                "savings.stocksFunds.dialog.buyTicker.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_SELL_TICKER_TITLE =
                "savings.stocksFunds.dialog.sellTicker.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_ADD_DIVIDEND_TITLE =
                "savings.stocksFunds.dialog.addDividend.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_ADD_CRYPTO_EXCHANGE_TITLE =
                "savings.stocksFunds.dialog.addCryptoExchange.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_TICKER_ARCHIVE_TITLE =
                "savings.stocksFunds.dialog.tickerArchive.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_INVESTMENT_TRANSACTIONS_TITLE =
                "savings.stocksFunds.dialog.investmentTransactions.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_EDIT_TICKER_TITLE =
                "savings.stocksFunds.dialog.editTicker.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_TITLE =
                "savings.stocksFunds.dialog.noSelection.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_BUY_MESSAGE =
                "savings.stocksFunds.dialog.noSelection.buy.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_SELL_MESSAGE =
                "savings.stocksFunds.dialog.noSelection.sell.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_ADD_DIVIDEND_MESSAGE =
                "savings.stocksFunds.dialog.noSelection.addDividend.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_EDIT_MESSAGE =
                "savings.stocksFunds.dialog.noSelection.edit.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_DELETE_MESSAGE =
                "savings.stocksFunds.dialog.noSelection.delete.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_NO_SELECTION_MESSAGE =
                "savings.stocksFunds.dialog.noSelection.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_TITLE =
                "savings.stocksFunds.dialog.hasTransactions.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_HAS_TRANSACTIONS_MESSAGE =
                "savings.stocksFunds.dialog.hasTransactions.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_CONFIRMATION_DELETE_TITLE =
                "savings.stocksFunds.dialog.confirmationDelete.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_TITLE =
                "savings.stocksFunds.dialog.updatePrices.success.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_SUCCESS_MESSAGE =
                "savings.stocksFunds.dialog.updatePrices.success.message";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_TITLE =
                "savings.stocksFunds.dialog.updatePrices.error.title";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_ALL_FAILED =
                "savings.stocksFunds.dialog.updatePrices.error.allFailed";
        public static final String SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_ERROR_SOME_FAILED =
                "savings.stocksFunds.dialog.updatePrices.error.someFailed";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_ID =
                "savings.stocksFunds.table.header.id";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_NAME =
                "savings.stocksFunds.table.header.name";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_SYMBOL =
                "savings.stocksFunds.table.header.symbol";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TYPE =
                "savings.stocksFunds.table.header.type";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_QUANTITY_OWNED =
                "savings.stocksFunds.table.header.quantityOwned";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_UNIT_PRICE =
                "savings.stocksFunds.table.header.unitPrice";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_TOTAL_VALUE =
                "savings.stocksFunds.table.header.totalValue";
        public static final String SAVINGS_STOCKS_FUNDS_TABLE_HEADER_AVERAGE_UNIT_PRICE =
                "savings.stocksFunds.table.header.averageUnitPrice";

        // Savings - Brazilian Market Indicators
        public static final String SAVINGS_INDICATORS_SELIC = "savings.indicators.selic";
        public static final String SAVINGS_INDICATORS_IPCA_12_MONTHS =
                "savings.indicators.ipca12Months";
        public static final String SAVINGS_INDICATORS_LAST_UPDATE = "savings.indicators.lastUpdate";

        // Savings - Market Quotes
        public static final String SAVINGS_QUOTES_DOLLAR = "savings.quotes.dollar";
        public static final String SAVINGS_QUOTES_EURO = "savings.quotes.euro";
        public static final String SAVINGS_QUOTES_IBOVESPA = "savings.quotes.ibovespa";
        public static final String SAVINGS_QUOTES_BITCOIN = "savings.quotes.bitcoin";
        public static final String SAVINGS_QUOTES_ETHEREUM = "savings.quotes.ethereum";

        // Savings - Commodities
        public static final String SAVINGS_COMMODITIES_TITLE = "savings.commodities.title";
        public static final String SAVINGS_COMMODITIES_GOLD = "savings.commodities.gold";
        public static final String SAVINGS_COMMODITIES_SOYBEAN = "savings.commodities.soybean";
        public static final String SAVINGS_COMMODITIES_COFFEE = "savings.commodities.coffee";
        public static final String SAVINGS_COMMODITIES_WHEAT = "savings.commodities.wheat";
        public static final String SAVINGS_COMMODITIES_OIL_BRENT = "savings.commodities.oilBrent";

        // Savings - Metrics
        public static final String SAVINGS_METRICS_TOTAL_RETURN = "savings.metrics.totalReturn";
        public static final String SAVINGS_METRICS_DIVIDEND_YIELD = "savings.metrics.dividendYield";
        public static final String SAVINGS_METRICS_TOTAL_INVESTED = "savings.metrics.totalInvested";
        public static final String SAVINGS_METRICS_CURRENT_VALUE = "savings.metrics.currentValue";
        public static final String SAVINGS_METRICS_PROFIT_LOSS = "savings.metrics.profitLoss";
        public static final String SAVINGS_METRICS_TOTAL_DIVIDENDS =
                "savings.metrics.totalDividends";

        // Savings - Allocation
        public static final String SAVINGS_ALLOCATION_TARGET = "savings.allocation.target";
        public static final String SAVINGS_ALLOCATION_STATUS_CRITICAL_LOW =
                "savings.allocation.status.criticalLow";
        public static final String SAVINGS_ALLOCATION_STATUS_WARNING_LOW =
                "savings.allocation.status.warningLow";
        public static final String SAVINGS_ALLOCATION_STATUS_ON_TARGET =
                "savings.allocation.status.onTarget";
        public static final String SAVINGS_ALLOCATION_STATUS_WARNING_HIGH =
                "savings.allocation.status.warningHigh";
        public static final String SAVINGS_ALLOCATION_STATUS_CRITICAL_HIGH =
                "savings.allocation.status.criticalHigh";

        // Savings - Top Performers
        public static final String SAVINGS_TOP_PERFORMERS_BEST = "savings.topPerformers.best";
        public static final String SAVINGS_TOP_PERFORMERS_WORST = "savings.topPerformers.worst";
        public static final String SAVINGS_TOP_PERFORMERS_HEADER_ASSET =
                "savings.topPerformers.header.asset";
        public static final String SAVINGS_TOP_PERFORMERS_HEADER_RETURN =
                "savings.topPerformers.header.return";
        public static final String SAVINGS_TOP_PERFORMERS_HEADER_VALUE =
                "savings.topPerformers.header.value";

        // Investment Target
        public static final String INVESTMENT_DIALOG_EDIT_TARGET_TITLE =
                "investment.dialog.editTarget.title";
        public static final String INVESTMENT_DIALOG_TARGET_UPDATED_TITLE =
                "investment.dialog.targetUpdated.title";
        public static final String INVESTMENT_DIALOG_TARGET_UPDATED_MESSAGE =
                "investment.dialog.targetUpdated.message";
        public static final String INVESTMENT_DIALOG_ERROR_UPDATING_TARGET_TITLE =
                "investment.dialog.errorUpdatingTarget.title";
        public static final String INVESTMENT_DIALOG_INVALID_PERCENTAGE_TITLE =
                "investment.dialog.invalidPercentage.title";
        public static final String INVESTMENT_DIALOG_INVALID_PERCENTAGE_MESSAGE =
                "investment.dialog.invalidPercentage.message";
        public static final String INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION_TITLE =
                "investment.dialog.totalPercentageValidation.title";
        public static final String INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION =
                "investment.dialog.totalPercentageValidation.message";

        // Savings - Bonds
        public static final String SAVINGS_BONDS_DIALOG_ADD_BOND_TITLE =
                "savings.bonds.dialog.addBond.title";
        public static final String SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE =
                "savings.bonds.dialog.noSelection.title";
        public static final String SAVINGS_BONDS_DIALOG_NO_SELECTION_EDIT_MESSAGE =
                "savings.bonds.dialog.noSelection.edit.message";
        public static final String SAVINGS_BONDS_DIALOG_NO_SELECTION_DELETE_MESSAGE =
                "savings.bonds.dialog.noSelection.delete.message";
        public static final String SAVINGS_BONDS_DIALOG_EDIT_IN_DEVELOPMENT_TITLE =
                "savings.bonds.dialog.editInDevelopment.title";
        public static final String SAVINGS_BONDS_DIALOG_EDIT_IN_DEVELOPMENT_MESSAGE =
                "savings.bonds.dialog.editInDevelopment.message";
        public static final String SAVINGS_BONDS_DIALOG_ARCHIVE_IN_DEVELOPMENT_TITLE =
                "savings.bonds.dialog.archiveInDevelopment.title";
        public static final String SAVINGS_BONDS_DIALOG_ARCHIVE_IN_DEVELOPMENT_MESSAGE =
                "savings.bonds.dialog.archiveInDevelopment.message";
        public static final String SAVINGS_BONDS_DIALOG_EDIT_BOND_TITLE =
                "savings.bonds.dialog.editBond.title";
        public static final String SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_TITLE =
                "savings.bonds.dialog.confirmDelete.title";
        public static final String SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_MESSAGE =
                "savings.bonds.dialog.confirmDelete.message";
        public static final String SAVINGS_BONDS_DIALOG_BOND_DELETED_TITLE =
                "savings.bonds.dialog.bondDeleted.title";
        public static final String SAVINGS_BONDS_DIALOG_BOND_DELETED_MESSAGE =
                "savings.bonds.dialog.bondDeleted.message";
        public static final String SAVINGS_BONDS_DIALOG_ERROR_DELETING_BOND_TITLE =
                "savings.bonds.dialog.errorDeletingBond.title";
        public static final String SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_TITLE =
                "savings.bonds.dialog.hasOperations.title";
        public static final String SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_MESSAGE =
                "savings.bonds.dialog.hasOperations.message";
        public static final String SAVINGS_BONDS_DIALOG_BOND_ARCHIVED_TITLE =
                "savings.bonds.dialog.bondArchived.title";
        public static final String SAVINGS_BONDS_DIALOG_BOND_ARCHIVED_MESSAGE =
                "savings.bonds.dialog.bondArchived.message";
        public static final String SAVINGS_BONDS_DIALOG_BOND_ARCHIVE_TITLE =
                "savings.bonds.dialog.bondArchive.title";
        public static final String SAVINGS_BONDS_TABLE_HEADER_NAME =
                "savings.bonds.table.header.name";
        public static final String SAVINGS_BONDS_TABLE_HEADER_SYMBOL =
                "savings.bonds.table.header.symbol";
        public static final String SAVINGS_BONDS_TABLE_HEADER_TYPE =
                "savings.bonds.table.header.type";
        public static final String SAVINGS_BONDS_TABLE_HEADER_CURRENT_VALUE =
                "savings.bonds.table.header.currentValue";
        public static final String SAVINGS_BONDS_TABLE_HEADER_INVESTED_VALUE =
                "savings.bonds.table.header.investedValue";
        public static final String SAVINGS_BONDS_TABLE_HEADER_PROFIT_LOSS =
                "savings.bonds.table.header.profitLoss";
        public static final String SAVINGS_BONDS_DIALOG_BUY_BOND_TITLE =
                "savings.bonds.dialog.buyBond.title";
        public static final String SAVINGS_BONDS_DIALOG_SELL_BOND_TITLE =
                "savings.bonds.dialog.sellBond.title";
        public static final String SAVINGS_BONDS_DIALOG_NO_SELECTION_BUY_MESSAGE =
                "savings.bonds.dialog.noSelection.buy.message";
        public static final String SAVINGS_BONDS_DIALOG_NO_SELECTION_SELL_MESSAGE =
                "savings.bonds.dialog.noSelection.sell.message";

        // Bond Labels
        public static final String BOND_LABEL_BOND = "bond.label.bond";
        public static final String BOND_LABEL_PROFIT = "bond.label.profit";

        // Bond Dialog Messages
        public static final String BOND_DIALOG_PURCHASE_ADDED_TITLE =
                "bond.dialog.purchaseAdded.title";
        public static final String BOND_DIALOG_PURCHASE_ADDED_MESSAGE =
                "bond.dialog.purchaseAdded.message";
        public static final String BOND_DIALOG_SALE_ADDED_TITLE = "bond.dialog.saleAdded.title";
        public static final String BOND_DIALOG_SALE_ADDED_MESSAGE = "bond.dialog.saleAdded.message";
        public static final String BOND_DIALOG_ERROR_BUYING_TITLE = "bond.dialog.errorBuying.title";
        public static final String BOND_DIALOG_ERROR_SELLING_TITLE =
                "bond.dialog.errorSelling.title";

        // Bond Table Headers
        public static final String BOND_TABLE_ID = "bond.table.id";
        public static final String BOND_TABLE_NAME = "bond.table.name";
        public static final String BOND_TABLE_SYMBOL = "bond.table.symbol";
        public static final String BOND_TABLE_ISSUER = "bond.table.issuer";
        public static final String BOND_TABLE_INVESTED_VALUE = "bond.table.investedValue";
        public static final String BOND_TABLE_OPERATION_TYPE = "bond.table.operationType";
        public static final String BOND_TABLE_BOND = "bond.table.bond";
        public static final String BOND_TABLE_TYPE = "bond.table.type";
        public static final String BOND_TABLE_DATE = "bond.table.date";
        public static final String BOND_TABLE_QUANTITY = "bond.table.quantity";
        public static final String BOND_TABLE_UNIT_PRICE = "bond.table.unitPrice";
        public static final String BOND_TABLE_FEES = "bond.table.fees";
        public static final String BOND_TABLE_TAXES = "bond.table.taxes";
        public static final String BOND_TABLE_PROFIT_LOSS = "bond.table.profitLoss";
        public static final String BOND_TABLE_TOTAL_AMOUNT = "bond.table.totalAmount";
        public static final String BOND_TABLE_WALLET = "bond.table.wallet";
        public static final String BOND_TABLE_STATUS = "bond.table.status";
        public static final String BOND_MATURITY_DATE = "bond.maturityDate";

        // Fundamental Analysis
        public static final String FUNDAMENTAL_ANALYSIS_DIALOG_TITLE =
                "fundamentalAnalysis.dialog.title";
        public static final String FUNDAMENTAL_ANALYSIS_COMPANY_NAME =
                "fundamentalAnalysis.companyName";
        public static final String FUNDAMENTAL_ANALYSIS_SECTOR = "fundamentalAnalysis.sector";
        public static final String FUNDAMENTAL_ANALYSIS_INDUSTRY = "fundamentalAnalysis.industry";
        public static final String FUNDAMENTAL_ANALYSIS_CURRENCY = "fundamentalAnalysis.currency";
        public static final String FUNDAMENTAL_ANALYSIS_LAST_UPDATE =
                "fundamentalAnalysis.lastUpdate";
        public static final String FUNDAMENTAL_ANALYSIS_PERIOD = "fundamentalAnalysis.period";
        public static final String FUNDAMENTAL_ANALYSIS_BUTTON_REFRESH =
                "fundamentalAnalysis.button.refresh";
        public static final String FUNDAMENTAL_ANALYSIS_BUTTON_CLOSE =
                "fundamentalAnalysis.button.close";
        public static final String FUNDAMENTAL_ANALYSIS_LOADING = "fundamentalAnalysis.loading";
        public static final String FUNDAMENTAL_ANALYSIS_ERROR_TITLE =
                "fundamentalAnalysis.error.title";
        public static final String FUNDAMENTAL_ANALYSIS_ERROR_CONNECTION_TITLE =
                "fundamentalAnalysis.error.connection.title";
        public static final String FUNDAMENTAL_ANALYSIS_ERROR_CONNECTION_MESSAGE =
                "fundamentalAnalysis.error.connection.message";

        // Fundamental Analysis - Tabs
        public static final String FUNDAMENTAL_ANALYSIS_TAB_PROFITABILITY =
                "fundamentalAnalysis.tab.profitability";
        public static final String FUNDAMENTAL_ANALYSIS_TAB_VALUATION =
                "fundamentalAnalysis.tab.valuation";
        public static final String FUNDAMENTAL_ANALYSIS_TAB_GROWTH =
                "fundamentalAnalysis.tab.growth";
        public static final String FUNDAMENTAL_ANALYSIS_TAB_DEBT = "fundamentalAnalysis.tab.debt";
        public static final String FUNDAMENTAL_ANALYSIS_TAB_EFFICIENCY =
                "fundamentalAnalysis.tab.efficiency";
        public static final String FUNDAMENTAL_ANALYSIS_TAB_CASH_GENERATION =
                "fundamentalAnalysis.tab.cashGeneration";
        public static final String FUNDAMENTAL_ANALYSIS_TAB_PRICE_PERFORMANCE =
                "fundamentalAnalysis.tab.pricePerformance";

        // Fundamental Analysis - Metrics
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ROE =
                "fundamentalAnalysis.metric.roe";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ROIC =
                "fundamentalAnalysis.metric.roic";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_NET_MARGIN =
                "fundamentalAnalysis.metric.netMargin";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_MARGIN =
                "fundamentalAnalysis.metric.ebitdaMargin";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE =
                "fundamentalAnalysis.metric.currentPrice";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP =
                "fundamentalAnalysis.metric.marketCap";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ENTERPRISE_VALUE =
                "fundamentalAnalysis.metric.enterpriseValue";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EPS =
                "fundamentalAnalysis.metric.eps";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_PE_RATIO =
                "fundamentalAnalysis.metric.peRatio";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_PEG_RATIO =
                "fundamentalAnalysis.metric.pegRatio";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EV_EBITDA =
                "fundamentalAnalysis.metric.evEbitda";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EARNINGS_YIELD =
                "fundamentalAnalysis.metric.earningsYield";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_FCF_YIELD =
                "fundamentalAnalysis.metric.fcfYield";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD =
                "fundamentalAnalysis.metric.dividendYield";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_RATE =
                "fundamentalAnalysis.metric.dividendRate";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_PAYOUT_RATIO =
                "fundamentalAnalysis.metric.payoutRatio";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_NUMBER =
                "fundamentalAnalysis.metric.grahamNumber";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE =
                "fundamentalAnalysis.metric.grahamFairValue";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY =
                "fundamentalAnalysis.metric.marginOfSafety";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YOY =
                "fundamentalAnalysis.metric.revenueGrowthYoy";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_CAGR =
                "fundamentalAnalysis.metric.revenueGrowthCagr";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YEARS =
                "fundamentalAnalysis.metric.revenueGrowthYears";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_TOTAL_DEBT =
                "fundamentalAnalysis.metric.totalDebt";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT =
                "fundamentalAnalysis.metric.netDebt";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_EBITDA =
                "fundamentalAnalysis.metric.netDebtEbitda";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_RATIO =
                "fundamentalAnalysis.metric.currentRatio";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ASSET_TURNOVER =
                "fundamentalAnalysis.metric.assetTurnover";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EBITDA =
                "fundamentalAnalysis.metric.ebitda";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_FREE_CASH_FLOW =
                "fundamentalAnalysis.metric.freeCashFlow";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_OPERATING_CASH_FLOW =
                "fundamentalAnalysis.metric.operatingCashFlow";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CAPEX =
                "fundamentalAnalysis.metric.capex";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_FCF_NET_INCOME =
                "fundamentalAnalysis.metric.fcfNetIncome";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DAY_HIGH =
                "fundamentalAnalysis.metric.dayHigh";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DAY_LOW =
                "fundamentalAnalysis.metric.dayLow";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1D =
                "fundamentalAnalysis.metric.change1d";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_5D =
                "fundamentalAnalysis.metric.change5d";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1M =
                "fundamentalAnalysis.metric.change1m";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_3M =
                "fundamentalAnalysis.metric.change3m";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_6M =
                "fundamentalAnalysis.metric.change6m";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_YTD =
                "fundamentalAnalysis.metric.changeYtd";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1Y =
                "fundamentalAnalysis.metric.change1y";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_52W_HIGH =
                "fundamentalAnalysis.metric.52wHigh";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_52W_LOW =
                "fundamentalAnalysis.metric.52wLow";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_HIGH =
                "fundamentalAnalysis.metric.distance52wHigh";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_LOW =
                "fundamentalAnalysis.metric.distance52wLow";

        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ROE_TOOLTIP =
                "fundamentalAnalysis.metric.roe.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ROIC_TOOLTIP =
                "fundamentalAnalysis.metric.roic.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_NET_MARGIN_TOOLTIP =
                "fundamentalAnalysis.metric.netMargin.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_MARGIN_TOOLTIP =
                "fundamentalAnalysis.metric.ebitdaMargin.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE_TOOLTIP =
                "fundamentalAnalysis.metric.currentPrice.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_MARKET_CAP_TOOLTIP =
                "fundamentalAnalysis.metric.marketCap.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ENTERPRISE_VALUE_TOOLTIP =
                "fundamentalAnalysis.metric.enterpriseValue.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EPS_TOOLTIP =
                "fundamentalAnalysis.metric.eps.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_PE_RATIO_TOOLTIP =
                "fundamentalAnalysis.metric.peRatio.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_PEG_RATIO_TOOLTIP =
                "fundamentalAnalysis.metric.pegRatio.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EV_EBITDA_TOOLTIP =
                "fundamentalAnalysis.metric.evEbitda.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EARNINGS_YIELD_TOOLTIP =
                "fundamentalAnalysis.metric.earningsYield.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_FCF_YIELD_TOOLTIP =
                "fundamentalAnalysis.metric.fcfYield.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_YIELD_TOOLTIP =
                "fundamentalAnalysis.metric.dividendYield.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DIVIDEND_RATE_TOOLTIP =
                "fundamentalAnalysis.metric.dividendRate.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_PAYOUT_RATIO_TOOLTIP =
                "fundamentalAnalysis.metric.payoutRatio.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_NUMBER_TOOLTIP =
                "fundamentalAnalysis.metric.grahamNumber.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE_TOOLTIP =
                "fundamentalAnalysis.metric.grahamFairValue.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY_TOOLTIP =
                "fundamentalAnalysis.metric.marginOfSafety.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YOY_TOOLTIP =
                "fundamentalAnalysis.metric.revenueGrowthYoy.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_CAGR_TOOLTIP =
                "fundamentalAnalysis.metric.revenueGrowthCagr.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_REVENUE_GROWTH_YEARS_TOOLTIP =
                "fundamentalAnalysis.metric.revenueGrowthYears.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_TOTAL_DEBT_TOOLTIP =
                "fundamentalAnalysis.metric.totalDebt.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_TOOLTIP =
                "fundamentalAnalysis.metric.netDebt.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_NET_DEBT_EBITDA_TOOLTIP =
                "fundamentalAnalysis.metric.netDebtEbitda.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_RATIO_TOOLTIP =
                "fundamentalAnalysis.metric.currentRatio.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_ASSET_TURNOVER_TOOLTIP =
                "fundamentalAnalysis.metric.assetTurnover.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_EBITDA_TOOLTIP =
                "fundamentalAnalysis.metric.ebitda.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_FREE_CASH_FLOW_TOOLTIP =
                "fundamentalAnalysis.metric.freeCashFlow.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_OPERATING_CASH_FLOW_TOOLTIP =
                "fundamentalAnalysis.metric.operatingCashFlow.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CAPEX_TOOLTIP =
                "fundamentalAnalysis.metric.capex.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_FCF_NET_INCOME_TOOLTIP =
                "fundamentalAnalysis.metric.fcfNetIncome.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DAY_HIGH_TOOLTIP =
                "fundamentalAnalysis.metric.dayHigh.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DAY_LOW_TOOLTIP =
                "fundamentalAnalysis.metric.dayLow.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1D_TOOLTIP =
                "fundamentalAnalysis.metric.change1d.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_5D_TOOLTIP =
                "fundamentalAnalysis.metric.change5d.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1M_TOOLTIP =
                "fundamentalAnalysis.metric.change1m.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_3M_TOOLTIP =
                "fundamentalAnalysis.metric.change3m.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_6M_TOOLTIP =
                "fundamentalAnalysis.metric.change6m.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_YTD_TOOLTIP =
                "fundamentalAnalysis.metric.changeYtd.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_CHANGE_1Y_TOOLTIP =
                "fundamentalAnalysis.metric.change1y.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_52W_HIGH_TOOLTIP =
                "fundamentalAnalysis.metric.52wHigh.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_52W_LOW_TOOLTIP =
                "fundamentalAnalysis.metric.52wLow.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_HIGH_TOOLTIP =
                "fundamentalAnalysis.metric.distance52wHigh.tooltip";
        public static final String FUNDAMENTAL_ANALYSIS_METRIC_DISTANCE_52W_LOW_TOOLTIP =
                "fundamentalAnalysis.metric.distance52wLow.tooltip";

        // Period Types
        public static final String PERIOD_TYPE_ANNUAL = "periodType.annual";
        public static final String PERIOD_TYPE_QUARTERLY = "periodType.quarterly";

        // Fundamental Analysis - Metadata
        public static final String FUNDAMENTAL_ANALYSIS_REFERENCE_DATE =
                "fundamentalAnalysis.referenceDate";

        // Fundamental Analysis - Errors
        public static final String FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_TITLE =
                "fundamentalAnalysis.error.invalidTickerType.title";
        public static final String FUNDAMENTAL_ANALYSIS_ERROR_INVALID_TICKER_TYPE_MESSAGE =
                "fundamentalAnalysis.error.invalidTickerType.message";

        public static final String BOND_INTEREST_RATE = "bond.interestRate";

        // Bond Tabs
        public static final String BOND_TAB_PURCHASE = "bond.tab.purchase";
        public static final String BOND_TAB_SALE = "bond.tab.sale";

        public static final String BOND_LABEL_SELECT_TRANSACTION = "bond.label.selectTransaction";

        // Bond Transactions Dialog
        public static final String BOND_DIALOG_TRANSACTIONS_TITLE =
                "bond.dialog.transactions.title";

        // Common Components - Budget Group Pane
        public static final String COMMON_BUDGET_GROUP_TARGET = "common.budgetGroup.target";
        public static final String COMMON_BUDGET_GROUP_OVERSPENT = "common.budgetGroup.overspent";
        public static final String COMMON_BUDGET_GROUP_REMAINING = "common.budgetGroup.remaining";
        public static final String COMMON_BUDGET_GROUP_ON_TRACK = "common.budgetGroup.onTrack";

        // Common Components - Calculator
        public static final String COMMON_CALCULATOR_INSERT_RESULT =
                "common.calculator.insertResult";

        // Common Components - Calendar
        public static final String COMMON_CALENDAR_ADD_EVENT = "common.calendar.addEvent";

        // Common Components - Credit Card Pane
        public static final String COMMON_CREDIT_CARD_ADD_DEBT = "common.creditCard.addDebt";
        public static final String COMMON_CREDIT_CARD_ADD_CREDIT = "common.creditCard.addCredit";
        public static final String COMMON_CREDIT_CARD_EDIT = "common.creditCard.edit";
        public static final String COMMON_CREDIT_CARD_ARCHIVE = "common.creditCard.archive";
        public static final String COMMON_CREDIT_CARD_DELETE = "common.creditCard.delete";
        public static final String COMMON_CREDIT_CARD_SHOW_REBATES =
                "common.creditCard.showRebates";
        public static final String COMMON_CREDIT_CARD_LIMIT = "common.creditCard.limit";
        public static final String COMMON_CREDIT_CARD_PENDING = "common.creditCard.pending";
        public static final String COMMON_CREDIT_CARD_FREE = "common.creditCard.free";
        public static final String COMMON_CREDIT_CARD_REBATE = "common.creditCard.rebate";
        public static final String COMMON_CREDIT_CARD_CLOSURE = "common.creditCard.closure";
        public static final String COMMON_CREDIT_CARD_NEXT_INVOICE =
                "common.creditCard.nextInvoice";
        public static final String COMMON_CREDIT_CARD_DUE_DATE = "common.creditCard.dueDate";
        public static final String COMMON_CREDIT_CARD_INVOICE = "common.creditCard.invoice";
        public static final String COMMON_CREDIT_CARD_OPEN = "common.creditCard.open";
        public static final String COMMON_CREDIT_CARD_CLOSED = "common.creditCard.closed";
        public static final String COMMON_CREDIT_CARD_REGISTER_PAYMENT =
                "common.creditCard.registerPayment";

        // Common Components - Goal Full Pane
        public static final String COMMON_GOAL_ADD_INCOME = "common.goal.addIncome";
        public static final String COMMON_GOAL_ADD_EXPENSE = "common.goal.addExpense";
        public static final String COMMON_GOAL_ADD_TRANSFER = "common.goal.addTransfer";
        public static final String COMMON_GOAL_EDIT = "common.goal.edit";
        public static final String COMMON_GOAL_COMPLETE_GOAL = "common.goal.completeGoal";
        public static final String COMMON_GOAL_UNCOMPLETE_GOAL = "common.goal.uncompleteGoal";
        public static final String COMMON_GOAL_ARCHIVE_GOAL = "common.goal.archiveGoal";
        public static final String COMMON_GOAL_UNARCHIVE_GOAL = "common.goal.unarchiveGoal";
        public static final String COMMON_GOAL_DELETE = "common.goal.delete";
        public static final String COMMON_GOAL_GOAL = "common.goal.goal";
        public static final String COMMON_GOAL_CURRENT = "common.goal.current";
        public static final String COMMON_GOAL_EXPECTATION_DATE = "common.goal.expectationDate";
        public static final String COMMON_GOAL_COMPLETION_DATE = "common.goal.completionDate";
        public static final String COMMON_GOAL_MISSING_DAYS = "common.goal.missingDays";
        public static final String COMMON_GOAL_IDEAL_PER_MONTH = "common.goal.idealPerMonth";

        // Common Components - Resume Pane
        public static final String COMMON_RESUME_INCOMES = "common.resume.incomes";
        public static final String COMMON_RESUME_EXPENSES = "common.resume.expenses";
        public static final String COMMON_RESUME_BALANCE = "common.resume.balance";
        public static final String COMMON_RESUME_SAVINGS = "common.resume.savings";
        public static final String COMMON_RESUME_NO_SAVINGS = "common.resume.noSavings";
        public static final String COMMON_RESUME_CREDIT_CARDS = "common.resume.creditCards";
        public static final String COMMON_RESUME_FORESEEN = "common.resume.foreseen";
        public static final String COMMON_RESUME_INVOICES_TO_PAY = "common.resume.invoicesToPay";

        // Common Components - Wallet Full Pane
        public static final String COMMON_WALLET_ADD_INCOME = "common.wallet.addIncome";
        public static final String COMMON_WALLET_ADD_EXPENSE = "common.wallet.addExpense";
        public static final String COMMON_WALLET_ADD_TRANSFER = "common.wallet.addTransfer";
        public static final String COMMON_WALLET_RENAME = "common.wallet.rename";
        public static final String COMMON_WALLET_CHANGE_TYPE = "common.wallet.changeType";
        public static final String COMMON_WALLET_ADJUST_BALANCE = "common.wallet.adjustBalance";
        public static final String COMMON_WALLET_ARCHIVE = "common.wallet.archive";
        public static final String COMMON_WALLET_DELETE = "common.wallet.delete";
        public static final String COMMON_WALLET_OPENING_BALANCE = "common.wallet.openingBalance";
        public static final String COMMON_WALLET_INCOMES = "common.wallet.incomes";
        public static final String COMMON_WALLET_EXPENSES = "common.wallet.expenses";
        public static final String COMMON_WALLET_CREDITED_TRANSFERS =
                "common.wallet.creditedTransfers";
        public static final String COMMON_WALLET_DEBITED_TRANSFERS =
                "common.wallet.debitedTransfers";
        public static final String COMMON_WALLET_CURRENT_BALANCE = "common.wallet.currentBalance";
        public static final String COMMON_WALLET_FORESEEN_BALANCE = "common.wallet.foreseenBalance";

        // Common Components - Dialogs and Messages
        // Goal Full Pane Dialogs
        public static final String COMMON_GOAL_DIALOG_ARCHIVED_TITLE =
                "common.goal.dialog.archived.title";
        public static final String COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_INCOME =
                "common.goal.dialog.archived.cannotAddIncome";
        public static final String COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_EXPENSE =
                "common.goal.dialog.archived.cannotAddExpense";
        public static final String COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_TRANSFER =
                "common.goal.dialog.archived.cannotAddTransfer";
        public static final String COMMON_GOAL_DIALOG_REOPEN_TITLE =
                "common.goal.dialog.reopen.title";
        public static final String COMMON_GOAL_DIALOG_REOPEN_MESSAGE =
                "common.goal.dialog.reopen.message";
        public static final String COMMON_GOAL_DIALOG_COMPLETE_TITLE =
                "common.goal.dialog.complete.title";
        public static final String COMMON_GOAL_DIALOG_COMPLETE_MESSAGE =
                "common.goal.dialog.complete.message";
        public static final String COMMON_GOAL_DIALOG_COMPLETE_ERROR =
                "common.goal.dialog.complete.error";
        public static final String COMMON_GOAL_DIALOG_UNARCHIVE_TITLE =
                "common.goal.dialog.unarchive.title";
        public static final String COMMON_GOAL_DIALOG_UNARCHIVE_MESSAGE =
                "common.goal.dialog.unarchive.message";
        public static final String COMMON_GOAL_DIALOG_ARCHIVE_TITLE =
                "common.goal.dialog.archive.title";
        public static final String COMMON_GOAL_DIALOG_ARCHIVE_MESSAGE =
                "common.goal.dialog.archive.message";
        public static final String COMMON_GOAL_DIALOG_DELETE_HAS_TRANSACTIONS_TITLE =
                "common.goal.dialog.delete.hasTransactions.title";
        public static final String COMMON_GOAL_DIALOG_DELETE_HAS_TRANSACTIONS_MESSAGE =
                "common.goal.dialog.delete.hasTransactions.message";
        public static final String COMMON_GOAL_DIALOG_DELETE_TITLE =
                "common.goal.dialog.delete.title";
        public static final String COMMON_GOAL_DIALOG_DELETE_NAME =
                "common.goal.dialog.delete.name";
        public static final String COMMON_GOAL_DIALOG_DELETE_INITIAL_AMOUNT =
                "common.goal.dialog.delete.initialAmount";
        public static final String COMMON_GOAL_DIALOG_DELETE_CURRENT_AMOUNT =
                "common.goal.dialog.delete.currentAmount";
        public static final String COMMON_GOAL_DIALOG_DELETE_TARGET_AMOUNT =
                "common.goal.dialog.delete.targetAmount";
        public static final String COMMON_GOAL_DIALOG_DELETE_TARGET_DATE =
                "common.goal.dialog.delete.targetDate";
        public static final String COMMON_GOAL_DIALOG_DELETE_VIRTUAL_WALLETS =
                "common.goal.dialog.delete.virtualWallets";
        public static final String COMMON_GOAL_DIALOG_DELETE_ERROR =
                "common.goal.dialog.delete.error";

        // Wallet Full Pane Dialogs
        public static final String COMMON_WALLET_DIALOG_ARCHIVE_TITLE =
                "common.wallet.dialog.archive.title";
        public static final String COMMON_WALLET_DIALOG_ARCHIVE_MESSAGE =
                "common.wallet.dialog.archive.message";
        public static final String COMMON_WALLET_DIALOG_DELETE_HAS_TRANSACTIONS_TITLE =
                "common.wallet.dialog.delete.hasTransactions.title";
        public static final String COMMON_WALLET_DIALOG_DELETE_HAS_TRANSACTIONS_MESSAGE =
                "common.wallet.dialog.delete.hasTransactions.message";
        public static final String COMMON_WALLET_DIALOG_DELETE_TITLE =
                "common.wallet.dialog.delete.title";
        public static final String COMMON_WALLET_DIALOG_DELETE_MESSAGE =
                "common.wallet.dialog.delete.message";
        public static final String COMMON_WALLET_DIALOG_DELETE_VIRTUAL_WALLETS =
                "common.wallet.dialog.delete.virtualWallets";
        public static final String COMMON_WALLET_DIALOG_DELETE_SUCCESS_TITLE =
                "common.wallet.dialog.delete.success.title";
        public static final String COMMON_WALLET_DIALOG_DELETE_SUCCESS_MESSAGE =
                "common.wallet.dialog.delete.success.message";
        public static final String COMMON_WALLET_DIALOG_DELETE_ERROR =
                "common.wallet.dialog.delete.error";

        // Credit Card Pane Dialogs
        public static final String COMMON_CREDIT_CARD_DIALOG_ARCHIVE_TITLE =
                "common.creditCard.dialog.archive.title";
        public static final String COMMON_CREDIT_CARD_DIALOG_ARCHIVE_MESSAGE =
                "common.creditCard.dialog.archive.message";
        public static final String COMMON_CREDIT_CARD_DIALOG_ARCHIVE_SUCCESS_TITLE =
                "common.creditCard.dialog.archive.success.title";
        public static final String COMMON_CREDIT_CARD_DIALOG_ARCHIVE_SUCCESS_MESSAGE =
                "common.creditCard.dialog.archive.success.message";
        public static final String COMMON_CREDIT_CARD_DIALOG_ARCHIVE_ERROR =
                "common.creditCard.dialog.archive.error";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_HAS_DEBTS_TITLE =
                "common.creditCard.dialog.delete.hasDebts.title";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_HAS_DEBTS_MESSAGE =
                "common.creditCard.dialog.delete.hasDebts.message";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_TITLE =
                "common.creditCard.dialog.delete.title";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_MESSAGE =
                "common.creditCard.dialog.delete.message";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_SUCCESS_TITLE =
                "common.creditCard.dialog.delete.success.title";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_SUCCESS_MESSAGE =
                "common.creditCard.dialog.delete.success.message";
        public static final String COMMON_CREDIT_CARD_DIALOG_DELETE_ERROR =
                "common.creditCard.dialog.delete.error";

        // Calculator Dialog
        public static final String COMMON_CALCULATOR_DIALOG_ERROR_TITLE =
                "common.calculator.dialog.error.title";

        // Common Components - Modal Window Titles
        // Goal Full Pane Modal Titles
        public static final String COMMON_GOAL_MODAL_ADD_INCOME = "common.goal.modal.addIncome";
        public static final String COMMON_GOAL_MODAL_ADD_EXPENSE = "common.goal.modal.addExpense";
        public static final String COMMON_GOAL_MODAL_ADD_TRANSFER = "common.goal.modal.addTransfer";
        public static final String COMMON_GOAL_MODAL_EDIT_GOAL = "common.goal.modal.editGoal";

        // Wallet Full Pane Modal Titles
        public static final String COMMON_WALLET_MODAL_ADD_INCOME = "common.wallet.modal.addIncome";
        public static final String COMMON_WALLET_MODAL_ADD_EXPENSE =
                "common.wallet.modal.addExpense";
        public static final String COMMON_WALLET_MODAL_ADD_TRANSFER =
                "common.wallet.modal.addTransfer";
        public static final String COMMON_WALLET_MODAL_RENAME = "common.wallet.modal.rename";
        public static final String COMMON_WALLET_MODAL_CHANGE_TYPE =
                "common.wallet.modal.changeType";
        public static final String COMMON_WALLET_MODAL_CHANGE_BALANCE =
                "common.wallet.modal.changeBalance";

        // Credit Card Pane Modal Titles
        public static final String COMMON_CREDIT_CARD_MODAL_ADD_DEBT =
                "common.creditCard.modal.addDebt";
        public static final String COMMON_CREDIT_CARD_MODAL_ADD_CREDIT =
                "common.creditCard.modal.addCredit";
        public static final String COMMON_CREDIT_CARD_MODAL_EDIT = "common.creditCard.modal.edit";
        public static final String COMMON_CREDIT_CARD_MODAL_SHOW_CREDITS =
                "common.creditCard.modal.showCredits";
        public static final String COMMON_CREDIT_CARD_MODAL_REGISTER_PAYMENT =
                "common.creditCard.modal.registerPayment";

        // Calendar Modal Titles
        public static final String COMMON_CALENDAR_MODAL_ADD_EVENT =
                "common.calendar.modal.addEvent";

        // Common Dialog Elements

        // Dialog Labels
        public static final String DIALOG_LABEL_NAME = "dialog.label.name";
        public static final String DIALOG_LABEL_DESCRIPTION = "dialog.label.description";
        public static final String DIALOG_LABEL_AMOUNT = "dialog.label.amount";
        public static final String DIALOG_LABEL_DATE = "dialog.label.date";
        public static final String DIALOG_LABEL_CATEGORY = "dialog.label.category";
        public static final String DIALOG_LABEL_WALLET = "dialog.label.wallet";
        public static final String DIALOG_LABEL_TYPE = "dialog.label.type";
        public static final String DIALOG_LABEL_STATUS = "dialog.label.status";
        public static final String DIALOG_LABEL_VALUE = "dialog.label.value";
        public static final String DIALOG_LABEL_QUANTITY = "dialog.label.quantity";
        public static final String DIALOG_LABEL_NOTES = "dialog.label.notes";
        public static final String DIALOG_LABEL_ARCHIVED = "dialog.label.archived";
        public static final String DIALOG_LABEL_INSTALLMENTS = "dialog.label.installments";
        public static final String DIALOG_LABEL_FREQUENCY = "dialog.label.frequency";
        public static final String DIALOG_LABEL_START_DATE = "dialog.label.startDate";
        public static final String DIALOG_LABEL_END_DATE = "dialog.label.endDate";
        public static final String DIALOG_LABEL_DUE_DATE = "dialog.label.dueDate";
        public static final String DIALOG_LABEL_PAYMENT_DATE = "dialog.label.paymentDate";

        // Credit Card Dialog Messages
        public static final String CREDITCARD_DIALOG_EMPTY_FIELDS_TITLE =
                "creditcard.dialog.emptyFields.title";
        public static final String CREDITCARD_DIALOG_EMPTY_FIELDS_MESSAGE =
                "creditcard.dialog.emptyFields.message";
        public static final String CREDITCARD_DIALOG_CREATED_TITLE =
                "creditcard.dialog.created.title";
        public static final String CREDITCARD_DIALOG_CREATED_MESSAGE =
                "creditcard.dialog.created.message";
        public static final String CREDITCARD_DIALOG_UPDATED_TITLE =
                "creditcard.dialog.updated.title";
        public static final String CREDITCARD_DIALOG_UPDATED_MESSAGE =
                "creditcard.dialog.updated.message";
        public static final String CREDITCARD_DIALOG_INVALID_LIMIT_TITLE =
                "creditcard.dialog.invalidLimit.title";
        public static final String CREDITCARD_DIALOG_INVALID_LIMIT_MESSAGE =
                "creditcard.dialog.invalidLimit.message";
        public static final String CREDITCARD_DIALOG_ERROR_CREATING_TITLE =
                "creditcard.dialog.errorCreating.title";
        public static final String CREDITCARD_DIALOG_NO_CHANGES_TITLE =
                "creditcard.dialog.noChanges.title";
        public static final String CREDITCARD_DIALOG_NO_CHANGES_MESSAGE =
                "creditcard.dialog.noChanges.message";
        public static final String CREDITCARD_DIALOG_DEBT_CREATED_TITLE =
                "creditcard.dialog.debtCreated.title";
        public static final String CREDITCARD_DIALOG_DEBT_CREATED_MESSAGE =
                "creditcard.dialog.debtCreated.message";
        public static final String CREDITCARD_DIALOG_INVALID_VALUE_TITLE =
                "creditcard.dialog.invalidValue.title";
        public static final String CREDITCARD_DIALOG_INVALID_VALUE_MESSAGE =
                "creditcard.dialog.invalidValue.message";
        public static final String CREDITCARD_DIALOG_ERROR_CREATING_DEBT_TITLE =
                "creditcard.dialog.errorCreatingDebt.title";
        public static final String CREDITCARD_DIALOG_TRANSACTION_UPDATED_TITLE =
                "creditcard.dialog.transactionUpdated.title";
        public static final String CREDITCARD_DIALOG_TRANSACTION_UPDATED_MESSAGE =
                "creditcard.dialog.transactionUpdated.message";
        public static final String CREDITCARD_DIALOG_NO_CHANGES_DEBT_TITLE =
                "creditcard.dialog.noChangesDebt.title";
        public static final String CREDITCARD_DIALOG_NO_CHANGES_DEBT_MESSAGE =
                "creditcard.dialog.noChangesDebt.message";
        public static final String CREDITCARD_DIALOG_CREDIT_CREATED_TITLE =
                "creditcard.dialog.creditCreated.title";
        public static final String CREDITCARD_DIALOG_CREDIT_CREATED_MESSAGE =
                "creditcard.dialog.creditCreated.message";
        public static final String CREDITCARD_DIALOG_INVALID_CREDIT_VALUE_TITLE =
                "creditcard.dialog.invalidCreditValue.title";
        public static final String CREDITCARD_DIALOG_INVALID_CREDIT_VALUE_MESSAGE =
                "creditcard.dialog.invalidCreditValue.message";
        public static final String CREDITCARD_DIALOG_NO_SELECTION_TITLE =
                "creditcard.dialog.noSelection.title";
        public static final String CREDITCARD_DIALOG_NO_SELECTION_UNARCHIVE =
                "creditcard.dialog.noSelection.unarchive";
        public static final String CREDITCARD_DIALOG_NO_SELECTION_DELETE =
                "creditcard.dialog.noSelection.delete";
        public static final String CREDITCARD_DIALOG_UNARCHIVE_TITLE =
                "creditcard.dialog.unarchive.title";
        public static final String CREDITCARD_DIALOG_UNARCHIVE_MESSAGE =
                "creditcard.dialog.unarchive.message";
        public static final String CREDITCARD_DIALOG_UNARCHIVED_TITLE =
                "creditcard.dialog.unarchived.title";
        public static final String CREDITCARD_DIALOG_UNARCHIVED_MESSAGE =
                "creditcard.dialog.unarchived.message";
        public static final String CREDITCARD_DIALOG_ERROR_UNARCHIVING_TITLE =
                "creditcard.dialog.errorUnarchiving.title";
        public static final String CREDITCARD_DIALOG_HAS_DEBTS_TITLE =
                "creditcard.dialog.hasDebts.title";
        public static final String CREDITCARD_DIALOG_HAS_DEBTS_MESSAGE =
                "creditcard.dialog.hasDebts.message";
        public static final String CREDITCARD_DIALOG_DELETE_TITLE =
                "creditcard.dialog.delete.title";
        public static final String CREDITCARD_DIALOG_DELETE_MESSAGE =
                "creditcard.dialog.delete.message";
        public static final String CREDITCARD_DIALOG_DELETED_TITLE =
                "creditcard.dialog.deleted.title";
        public static final String CREDITCARD_DIALOG_DELETED_MESSAGE =
                "creditcard.dialog.deleted.message";
        public static final String CREDITCARD_DIALOG_ERROR_DELETING_TITLE =
                "creditcard.dialog.errorDeleting.title";
        public static final String CREDITCARD_DIALOG_WALLET_NOT_SELECTED_TITLE =
                "creditcard.dialog.walletNotSelected.title";
        public static final String CREDITCARD_DIALOG_WALLET_NOT_SELECTED_MESSAGE =
                "creditcard.dialog.walletNotSelected.message";
        public static final String CREDITCARD_DIALOG_INVOICE_ALREADY_PAID_TITLE =
                "creditcard.dialog.invoiceAlreadyPaid.title";
        public static final String CREDITCARD_DIALOG_INVOICE_ALREADY_PAID_MESSAGE =
                "creditcard.dialog.invoiceAlreadyPaid.message";
        public static final String CREDITCARD_DIALOG_INVOICE_PAID_TITLE =
                "creditcard.dialog.invoicePaid.title";
        public static final String CREDITCARD_DIALOG_INVOICE_PAID_MESSAGE =
                "creditcard.dialog.invoicePaid.message";
        public static final String CREDITCARD_DIALOG_ERROR_PAYING_INVOICE_TITLE =
                "creditcard.dialog.errorPayingInvoice.title";

        // Credit Card Table Columns
        public static final String CREDITCARD_TABLE_ID = "creditcard.table.id";
        public static final String CREDITCARD_TABLE_CREDIT_CARD = "creditcard.table.creditCard";
        public static final String CREDITCARD_TABLE_OPERATOR = "creditcard.table.operator";
        public static final String CREDITCARD_TABLE_ASSOCIATED_DEBTS =
                "creditcard.table.associatedDebts";
        public static final String CREDITCARD_TABLE_DESCRIPTION = "creditcard.table.description";
        public static final String CREDITCARD_TABLE_AMOUNT = "creditcard.table.amount";
        public static final String CREDITCARD_TABLE_TYPE = "creditcard.table.type";
        public static final String CREDITCARD_TABLE_DATE = "creditcard.table.date";

        // Credit Card Debt Management Messages
        public static final String CREDITCARD_DEBT_INVALID_INSTALLMENTS =
                "creditcard.debt.invalidInstallments";
        public static final String CREDITCARD_DEBT_REPEAT_MONTHS = "creditcard.debt.repeatMonths";
        public static final String CREDITCARD_DEBT_REPEAT_MONTHS_UNEVEN =
                "creditcard.debt.repeatMonthsUneven";
        public static final String CREDITCARD_DEBT_INVALID_VALUE = "creditcard.debt.invalidValue";

        // Credit Card Credits Dialog
        public static final String CREDITCARD_CREDITS_ADD_TITLE = "creditcard.credits.addTitle";

        // Credit Card FXML Labels
        public static final String CREDITCARD_LABEL_CREDIT_CARD = "creditcard.label.creditCard";
        public static final String CREDITCARD_LABEL_LIMIT = "creditcard.label.limit";
        public static final String CREDITCARD_LABEL_LIMIT_AVAILABLE =
                "creditcard.label.limitAvailable";
        public static final String CREDITCARD_LABEL_LIMIT_AVAILABLE_AFTER_DEBT =
                "creditcard.label.limitAvailableAfterDebt";
        public static final String CREDITCARD_LABEL_TOTAL = "creditcard.label.total";
        public static final String CREDITCARD_LABEL_INSTALLMENTS = "creditcard.label.installments";
        public static final String CREDITCARD_LABEL_INVOICE = "creditcard.label.invoice";
        public static final String CREDITCARD_LABEL_CREDIT_CARD_NAME =
                "creditcard.label.creditCardName";
        public static final String CREDITCARD_LABEL_LAST_FOUR_DIGITS =
                "creditcard.label.lastFourDigits";
        public static final String CREDITCARD_LABEL_OPERATOR = "creditcard.label.operator";
        public static final String CREDITCARD_LABEL_DEFAULT_BILLING_WALLET =
                "creditcard.label.defaultBillingWallet";
        public static final String CREDITCARD_LABEL_CLOSING_DAY = "creditcard.label.closingDay";
        public static final String CREDITCARD_LABEL_DUE_DAY = "creditcard.label.dueDay";
        public static final String CREDITCARD_LABEL_CURRENT_BALANCE =
                "creditcard.label.currentBalance";
        public static final String CREDITCARD_LABEL_BALANCE_AFTER_PAYMENT =
                "creditcard.label.balanceAfterPayment";
        public static final String CREDITCARD_LABEL_INVOICE_DUE = "creditcard.label.invoiceDue";
        public static final String CREDITCARD_LABEL_INVOICE_MONTH = "creditcard.label.invoiceMonth";
        public static final String CREDITCARD_LABEL_AVAILABLE_REBATE =
                "creditcard.label.availableRebate";
        public static final String CREDITCARD_LABEL_USE_REBATE = "creditcard.label.useRebate";
        public static final String CREDITCARD_LABEL_TOTAL_TO_PAY = "creditcard.label.totalToPay";
        public static final String CREDITCARD_LABEL_SELECT_CREDIT_CARD =
                "creditcard.label.selectCreditCard";
        public static final String CREDITCARD_LABEL_SELECT_CREDIT_CARD_CREDIT =
                "creditcard.label.selectCreditCardCredit";
        public static final String CREDITCARD_PROMPT_TEXT_SEARCH_ID_OR_DESCRIPTION =
                "creditcard.promptText.searchIdOrDescription";

        // Financial Planning Dialog Messages
        public static final String FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_TITLE =
                "financialplanning.dialog.emptyFields.title";
        public static final String FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_MESSAGE =
                "financialplanning.dialog.emptyFields.message";
        public static final String FINANCIALPLANNING_DIALOG_PLAN_CREATED_TITLE =
                "financialplanning.dialog.planCreated.title";
        public static final String FINANCIALPLANNING_DIALOG_PLAN_CREATED_MESSAGE =
                "financialplanning.dialog.planCreated.message";
        public static final String FINANCIALPLANNING_DIALOG_PLAN_UPDATED_TITLE =
                "financialplanning.dialog.planUpdated.title";
        public static final String FINANCIALPLANNING_DIALOG_PLAN_UPDATED_MESSAGE =
                "financialplanning.dialog.planUpdated.message";
        public static final String FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_TITLE =
                "financialplanning.dialog.invalidBaseIncome.title";
        public static final String FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_MESSAGE =
                "financialplanning.dialog.invalidBaseIncome.message";
        public static final String FINANCIALPLANNING_DIALOG_ERROR_CREATING_PLAN_TITLE =
                "financialplanning.dialog.errorCreatingPlan.title";
        public static final String FINANCIALPLANNING_DIALOG_ERROR_UPDATING_PLAN_TITLE =
                "financialplanning.dialog.errorUpdatingPlan.title";
        public static final String FINANCIALPLANNING_DIALOG_NO_CHANGES_TITLE =
                "financialplanning.dialog.noChanges.title";
        public static final String FINANCIALPLANNING_DIALOG_NO_CHANGES_MESSAGE =
                "financialplanning.dialog.noChanges.message";
        public static final String FINANCIALPLANNING_DIALOG_REQUIRED_FIELDS_TITLE =
                "financialplanning.dialog.requiredFields.title";
        public static final String FINANCIALPLANNING_DIALOG_REQUIRED_FIELDS_MESSAGE =
                "financialplanning.dialog.requiredFields.message";
        public static final String FINANCIALPLANNING_DIALOG_INVALID_INPUT_TITLE =
                "financialplanning.dialog.invalidInput.title";
        public static final String FINANCIALPLANNING_DIALOG_INVALID_INPUT_MESSAGE =
                "financialplanning.dialog.invalidInput.message";
        public static final String FINANCIALPLANNING_DIALOG_INSUFFICIENT_GROUPS_TITLE =
                "financialplanning.dialog.insufficientGroups.title";
        public static final String FINANCIALPLANNING_DIALOG_INSUFFICIENT_GROUPS_MESSAGE =
                "financialplanning.dialog.insufficientGroups.message";
        public static final String FINANCIALPLANNING_DIALOG_INVALID_PERCENTAGES_TITLE =
                "financialplanning.dialog.invalidPercentages.title";
        public static final String FINANCIALPLANNING_DIALOG_INVALID_PERCENTAGES_MESSAGE =
                "financialplanning.dialog.invalidPercentages.message";
        public static final String FINANCIALPLANNING_DIALOG_EMPTY_GROUPS_TITLE =
                "financialplanning.dialog.emptyGroups.title";
        public static final String FINANCIALPLANNING_DIALOG_EMPTY_GROUPS_MESSAGE =
                "financialplanning.dialog.emptyGroups.message";
        public static final String FINANCIALPLANNING_DIALOG_ADD_BUDGET_GROUP_TITLE =
                "financialplanning.dialog.addBudgetGroup.title";
        public static final String FINANCIALPLANNING_DIALOG_EDIT_BUDGET_GROUP_TITLE =
                "financialplanning.dialog.editBudgetGroup.title";

        // Financial Planning FXML Labels
        public static final String FINANCIALPLANNING_LABEL_GROUP_NAME =
                "financialplanning.label.groupName";
        public static final String FINANCIALPLANNING_LABEL_TARGET_PERCENTAGE =
                "financialplanning.label.targetPercentage";
        public static final String FINANCIALPLANNING_LABEL_ASSOCIATED_CATEGORIES =
                "financialplanning.label.associatedCategories";
        public static final String FINANCIALPLANNING_LABEL_AVAILABLE_CATEGORIES =
                "financialplanning.label.availableCategories";
        public static final String FINANCIALPLANNING_LABEL_CATEGORIES_IN_GROUP =
                "financialplanning.label.categoriesInGroup";
        public static final String FINANCIALPLANNING_LABEL_PLAN_NAME =
                "financialplanning.label.planName";
        public static final String FINANCIALPLANNING_LABEL_BASE_MONTHLY_INCOME =
                "financialplanning.label.baseMonthlyIncome";
        public static final String FINANCIALPLANNING_LABEL_BUDGET_GROUPS =
                "financialplanning.label.budgetGroups";
        public static final String FINANCIALPLANNING_LABEL_INFORMATION =
                "financialplanning.label.information";
        public static final String FINANCIALPLANNING_LABEL_ADD_NEW_GROUP =
                "financialplanning.label.addNewGroup";
        public static final String FINANCIALPLANNING_LABEL_BUDGET_TEMPLATE =
                "financialplanning.label.budgetTemplate";
        public static final String FINANCIALPLANNING_PROMPT_TEXT_GROUP_NAME =
                "financialplanning.promptText.groupName";
        public static final String FINANCIALPLANNING_PROMPT_TEXT_TARGET_PERCENTAGE =
                "financialplanning.promptText.targetPercentage";
        public static final String FINANCIALPLANNING_PROMPT_TEXT_PLAN_NAME =
                "financialplanning.promptText.planName";

        // Financial Planning Budget Templates
        public static final String FINANCIALPLANNING_TEMPLATE_50_30_20_NAME =
                "financialplanning.template.50-30-20.name";
        public static final String FINANCIALPLANNING_TEMPLATE_50_30_20_DESCRIPTION =
                "financialplanning.template.50-30-20.description";
        public static final String FINANCIALPLANNING_TEMPLATE_30_30_40_NAME =
                "financialplanning.template.30-30-40.name";
        public static final String FINANCIALPLANNING_TEMPLATE_30_30_40_DESCRIPTION =
                "financialplanning.template.30-30-40.description";
        public static final String FINANCIALPLANNING_TEMPLATE_CUSTOM_NAME =
                "financialplanning.template.custom.name";
        public static final String FINANCIALPLANNING_TEMPLATE_CUSTOM_DESCRIPTION =
                "financialplanning.template.custom.description";
        public static final String FINANCIALPLANNING_TEMPLATE_ESSENTIALS =
                "financialplanning.template.essentials";
        public static final String FINANCIALPLANNING_TEMPLATE_WANTS =
                "financialplanning.template.wants";
        public static final String FINANCIALPLANNING_TEMPLATE_INVESTMENTS =
                "financialplanning.template.investments";

        // Financial Planning Context Menu
        public static final String FINANCIALPLANNING_CONTEXT_MENU_EDIT =
                "financialplanning.contextMenu.edit";
        public static final String FINANCIALPLANNING_CONTEXT_MENU_DELETE =
                "financialplanning.contextMenu.delete";

        // Financial Planning Info Messages
        public static final String FINANCIALPLANNING_INFO_PERCENTAGE_EXCEEDS =
                "financialplanning.info.percentageExceeds";
        public static final String FINANCIALPLANNING_INFO_EMPTY_GROUPS =
                "financialplanning.info.emptyGroups";
        public static final String FINANCIALPLANNING_INFO_PERCENTAGE_BELOW =
                "financialplanning.info.percentageBelow";
        public static final String FINANCIALPLANNING_INFO_CORRECTLY_CONFIGURED =
                "financialplanning.info.correctlyConfigured";

        // Financial Planning Transaction Type Filter
        public static final String FINANCIALPLANNING_FILTER_INCOME =
                "financialplanning.filter.income";
        public static final String FINANCIALPLANNING_FILTER_EXPENSE =
                "financialplanning.filter.expense";
        public static final String FINANCIALPLANNING_FILTER_BOTH = "financialplanning.filter.both";

        // Financial Planning Timeline Chart
        public static final String PLAN_TIMELINE_TITLE = "plan.timeline.title";
        public static final String PLAN_TIMELINE_X_AXIS = "plan.timeline.xAxis";
        public static final String PLAN_TIMELINE_Y_AXIS = "plan.timeline.yAxis";
        public static final String PLAN_TIMELINE_ACTUAL = "plan.timeline.actual";
        public static final String PLAN_TIMELINE_TARGET = "plan.timeline.target";

        // Goal Dialog Messages
        public static final String GOAL_DIALOG_EMPTY_FIELDS_TITLE = "goal.dialog.emptyFields.title";
        public static final String GOAL_DIALOG_EMPTY_FIELDS_MESSAGE =
                "goal.dialog.emptyFields.message";
        public static final String GOAL_DIALOG_STRATEGY_REQUIRED_TITLE =
                "goal.dialog.strategyRequired.title";
        public static final String GOAL_DIALOG_STRATEGY_REQUIRED_MESSAGE =
                "goal.dialog.strategyRequired.message";
        public static final String GOAL_DIALOG_GOAL_CREATED_TITLE = "goal.dialog.goalCreated.title";
        public static final String GOAL_DIALOG_GOAL_CREATED_MESSAGE =
                "goal.dialog.goalCreated.message";
        public static final String GOAL_DIALOG_GOAL_UPDATED_TITLE = "goal.dialog.goalUpdated.title";
        public static final String GOAL_DIALOG_GOAL_UPDATED_MESSAGE =
                "goal.dialog.goalUpdated.message";
        public static final String GOAL_DIALOG_INVALID_BALANCE_TITLE =
                "goal.dialog.invalidBalance.title";
        public static final String GOAL_DIALOG_INVALID_BALANCE_MESSAGE =
                "goal.dialog.invalidBalance.message";
        public static final String GOAL_DIALOG_ERROR_CREATING_GOAL_TITLE =
                "goal.dialog.errorCreatingGoal.title";
        public static final String GOAL_DIALOG_ERROR_UPDATING_GOAL_TITLE =
                "goal.dialog.errorUpdatingGoal.title";
        public static final String GOAL_DIALOG_NO_CHANGES_TITLE = "goal.dialog.noChanges.title";
        public static final String GOAL_DIALOG_NO_CHANGES_MESSAGE = "goal.dialog.noChanges.message";

        // Goal FXML Labels
        public static final String GOAL_LABEL_GOAL_NAME = "goal.label.goalName";
        public static final String GOAL_LABEL_INITIAL_BALANCE = "goal.label.initialBalance";
        public static final String GOAL_LABEL_CURRENT_BALANCE = "goal.label.currentBalance";
        public static final String GOAL_LABEL_TARGET_BALANCE = "goal.label.targetBalance";
        public static final String GOAL_LABEL_TARGET_DATE = "goal.label.targetDate";
        public static final String GOAL_LABEL_MOTIVATION = "goal.label.motivation";
        public static final String GOAL_LABEL_MASTER_WALLET = "goal.label.masterWallet";
        public static final String GOAL_LABEL_GOAL_FUNDING_STRATEGY =
                "goal.label.goalFundingStrategy";
        public static final String GOAL_LABEL_ADDS_TO_MASTER_WALLET =
                "goal.label.addsToMasterWallet";
        public static final String GOAL_LABEL_ALLOCATES_FROM_MASTER_WALLET =
                "goal.label.allocatesFromMasterWallet";
        public static final String GOAL_LABEL_COMPLETED = "goal.label.completed";

        // Investment Dialog Messages
        public static final String INVESTMENT_DIALOG_EMPTY_FIELDS_TITLE =
                "investment.dialog.emptyFields.title";
        public static final String INVESTMENT_DIALOG_EMPTY_FIELDS_MESSAGE =
                "investment.dialog.emptyFields.message";
        public static final String INVESTMENT_DIALOG_INVALID_FIELDS_TITLE =
                "investment.dialog.invalidFields.title";
        public static final String INVESTMENT_DIALOG_INVALID_FIELDS_MESSAGE =
                "investment.dialog.invalidFields.message";
        public static final String INVESTMENT_DIALOG_TICKER_ADDED_TITLE =
                "investment.dialog.tickerAdded.title";
        public static final String INVESTMENT_DIALOG_TICKER_ADDED_MESSAGE =
                "investment.dialog.tickerAdded.message";
        public static final String INVESTMENT_DIALOG_TICKER_UPDATED_TITLE =
                "investment.dialog.tickerUpdated.title";
        public static final String INVESTMENT_DIALOG_TICKER_UPDATED_MESSAGE =
                "investment.dialog.tickerUpdated.message";
        public static final String INVESTMENT_DIALOG_INVALID_NUMBER_TITLE =
                "investment.dialog.invalidNumber.title";
        public static final String INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE =
                "investment.dialog.invalidNumber.message";
        public static final String INVESTMENT_DIALOG_ERROR_ADDING_TICKER_TITLE =
                "investment.dialog.errorAddingTicker.title";
        public static final String INVESTMENT_DIALOG_ERROR_UPDATING_TICKER_TITLE =
                "investment.dialog.errorUpdatingTicker.title";
        public static final String INVESTMENT_DIALOG_PURCHASE_ADDED_TITLE =
                "investment.dialog.purchaseAdded.title";
        public static final String INVESTMENT_DIALOG_PURCHASE_ADDED_MESSAGE =
                "investment.dialog.purchaseAdded.message";
        public static final String INVESTMENT_DIALOG_PURCHASE_UPDATED_TITLE =
                "investment.dialog.purchaseUpdated.title";
        public static final String INVESTMENT_DIALOG_PURCHASE_UPDATED_MESSAGE =
                "investment.dialog.purchaseUpdated.message";
        public static final String INVESTMENT_DIALOG_ERROR_BUYING_TICKER_TITLE =
                "investment.dialog.errorBuyingTicker.title";
        public static final String INVESTMENT_DIALOG_ERROR_UPDATING_PURCHASE_TITLE =
                "investment.dialog.errorUpdatingPurchase.title";
        public static final String INVESTMENT_DIALOG_SALE_ADDED_TITLE =
                "investment.dialog.saleAdded.title";
        public static final String INVESTMENT_DIALOG_SALE_ADDED_MESSAGE =
                "investment.dialog.saleAdded.message";
        public static final String INVESTMENT_DIALOG_SALE_UPDATED_TITLE =
                "investment.dialog.saleUpdated.title";
        public static final String INVESTMENT_DIALOG_SALE_UPDATED_MESSAGE =
                "investment.dialog.saleUpdated.message";
        public static final String INVESTMENT_DIALOG_ERROR_SELLING_TICKER_TITLE =
                "investment.dialog.errorSellingTicker.title";
        public static final String INVESTMENT_DIALOG_ERROR_UPDATING_SALE_TITLE =
                "investment.dialog.errorUpdatingSale.title";
        public static final String INVESTMENT_DIALOG_DIVIDEND_CREATED_TITLE =
                "investment.dialog.dividendCreated.title";
        public static final String INVESTMENT_DIALOG_DIVIDEND_CREATED_MESSAGE =
                "investment.dialog.dividendCreated.message";
        public static final String INVESTMENT_DIALOG_DIVIDEND_UPDATED_TITLE =
                "investment.dialog.dividendUpdated.title";
        public static final String INVESTMENT_DIALOG_DIVIDEND_UPDATED_MESSAGE =
                "investment.dialog.dividendUpdated.message";
        public static final String INVESTMENT_DIALOG_INVALID_DIVIDEND_VALUE_TITLE =
                "investment.dialog.invalidDividendValue.title";
        public static final String INVESTMENT_DIALOG_INVALID_DIVIDEND_VALUE_MESSAGE =
                "investment.dialog.invalidDividendValue.message";
        public static final String INVESTMENT_DIALOG_ERROR_CREATING_DIVIDEND_TITLE =
                "investment.dialog.errorCreatingDividend.title";
        public static final String INVESTMENT_DIALOG_ERROR_UPDATING_DIVIDEND_TITLE =
                "investment.dialog.errorUpdatingDividend.title";
        public static final String INVESTMENT_DIALOG_EXCHANGE_CREATED_TITLE =
                "investment.dialog.exchangeCreated.title";
        public static final String INVESTMENT_DIALOG_EXCHANGE_CREATED_MESSAGE =
                "investment.dialog.exchangeCreated.message";
        public static final String INVESTMENT_DIALOG_EXCHANGE_UPDATED_TITLE =
                "investment.dialog.exchangeUpdated.title";
        public static final String INVESTMENT_DIALOG_EXCHANGE_UPDATED_MESSAGE =
                "investment.dialog.exchangeUpdated.message";
        public static final String INVESTMENT_DIALOG_INVALID_EXCHANGE_QUANTITY_TITLE =
                "investment.dialog.invalidExchangeQuantity.title";
        public static final String INVESTMENT_DIALOG_INVALID_EXCHANGE_QUANTITY_MESSAGE =
                "investment.dialog.invalidExchangeQuantity.message";
        public static final String INVESTMENT_DIALOG_ERROR_CREATING_EXCHANGE_TITLE =
                "investment.dialog.errorCreatingExchange.title";
        public static final String INVESTMENT_DIALOG_ERROR_UPDATING_EXCHANGE_TITLE =
                "investment.dialog.errorUpdatingExchange.title";
        public static final String INVESTMENT_DIALOG_NO_CHANGES_TITLE =
                "investment.dialog.noChanges.title";
        public static final String INVESTMENT_DIALOG_NO_CHANGES_PURCHASE_MESSAGE =
                "investment.dialog.noChangesPurchase.message";
        public static final String INVESTMENT_DIALOG_NO_CHANGES_SALE_MESSAGE =
                "investment.dialog.noChangesSale.message";
        public static final String INVESTMENT_DIALOG_NO_CHANGES_DIVIDEND_MESSAGE =
                "investment.dialog.noChangesDividend.message";
        public static final String INVESTMENT_DIALOG_NO_CHANGES_EXCHANGE_MESSAGE =
                "investment.dialog.noChangesExchange.message";
        public static final String INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_TITLE =
                "investment.dialog.noPurchaseSelected.title";
        public static final String INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_MESSAGE =
                "investment.dialog.noPurchaseSelected.message";
        public static final String INVESTMENT_DIALOG_NO_PURCHASE_SELECTED_DELETE_MESSAGE =
                "investment.dialog.noPurchaseSelectedDelete.message";
        public static final String INVESTMENT_DIALOG_NO_SALE_SELECTED_TITLE =
                "investment.dialog.noSaleSelected.title";
        public static final String INVESTMENT_DIALOG_NO_SALE_SELECTED_MESSAGE =
                "investment.dialog.noSaleSelected.message";
        public static final String INVESTMENT_DIALOG_NO_SALE_SELECTED_DELETE_MESSAGE =
                "investment.dialog.noSaleSelectedDelete.message";
        public static final String INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_TITLE =
                "investment.dialog.noDividendSelected.title";
        public static final String INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_MESSAGE =
                "investment.dialog.noDividendSelected.message";

        // Bond Dialog Messages
        public static final String BOND_DIALOG_EMPTY_FIELDS_TITLE = "bond.dialog.emptyFields.title";
        public static final String BOND_DIALOG_EMPTY_FIELDS_MESSAGE =
                "bond.dialog.emptyFields.message";
        public static final String BOND_DIALOG_ADDED_TITLE = "bond.dialog.added.title";
        public static final String BOND_DIALOG_ADDED_MESSAGE = "bond.dialog.added.message";
        public static final String BOND_DIALOG_INVALID_NUMBER_TITLE =
                "bond.dialog.invalidNumber.title";
        public static final String BOND_DIALOG_INVALID_NUMBER_MESSAGE =
                "bond.dialog.invalidNumber.message";
        public static final String BOND_DIALOG_ALREADY_EXISTS_TITLE =
                "bond.dialog.alreadyExists.title";
        public static final String BOND_DIALOG_ALREADY_EXISTS_MESSAGE =
                "bond.dialog.alreadyExists.message";
        public static final String BOND_DIALOG_UPDATED_TITLE = "bond.dialog.updated.title";
        public static final String BOND_DIALOG_UPDATED_MESSAGE = "bond.dialog.updated.message";
        public static final String BOND_DIALOG_NO_CHANGES_MESSAGE = "bond.dialog.noChanges.message";
        public static final String BOND_DIALOG_ERROR_UPDATING_TITLE =
                "bond.dialog.errorUpdating.title";
        public static final String BOND_DIALOG_NO_BOND_SELECTED_TITLE =
                "bond.dialog.noBondSelected.title";
        public static final String BOND_DIALOG_NO_BOND_SELECTED_UNARCHIVE =
                "bond.dialog.noBondSelected.unarchive";
        public static final String BOND_DIALOG_NO_BOND_SELECTED_DELETE =
                "bond.dialog.noBondSelected.delete";
        public static final String BOND_DIALOG_CONFIRM_UNARCHIVE_TITLE =
                "bond.dialog.confirmUnarchive.title";
        public static final String BOND_DIALOG_CONFIRM_UNARCHIVE_MESSAGE =
                "bond.dialog.confirmUnarchive.message";
        public static final String BOND_DIALOG_BOND_UNARCHIVED_TITLE =
                "bond.dialog.bondUnarchived.title";
        public static final String BOND_DIALOG_BOND_UNARCHIVED_MESSAGE =
                "bond.dialog.bondUnarchived.message";
        public static final String BOND_DIALOG_ERROR_UNARCHIVING_TITLE =
                "bond.dialog.errorUnarchiving.title";
        public static final String BOND_DIALOG_CONFIRM_DELETE_TITLE =
                "bond.dialog.confirmDelete.title";
        public static final String BOND_DIALOG_CONFIRM_DELETE_MESSAGE =
                "bond.dialog.confirmDelete.message";
        public static final String BOND_DIALOG_BOND_DELETED_TITLE = "bond.dialog.bondDeleted.title";
        public static final String BOND_DIALOG_BOND_DELETED_MESSAGE =
                "bond.dialog.bondDeleted.message";
        public static final String BOND_DIALOG_ERROR_DELETING_TITLE =
                "bond.dialog.errorDeleting.title";
        public static final String BOND_DIALOG_HAS_OPERATIONS_TITLE =
                "bond.dialog.hasOperations.title";
        public static final String BOND_DIALOG_HAS_OPERATIONS_MESSAGE =
                "bond.dialog.hasOperations.message";
        public static final String BOND_LABEL_ARCHIVED = "bond.label.archived";
        public static final String BOND_LABEL_SELECT_BOND = "bond.label.selectBond";
        public static final String BOND_PROMPT_TEXT_SEARCH_ID_OR_DESCRIPTION =
                "bond.promptText.searchIdOrDescription";

        // Bond Operation Edit/Delete
        public static final String BOND_DIALOG_NO_OPERATION_SELECTED_TITLE =
                "bond.dialog.noOperationSelected.title";
        public static final String BOND_DIALOG_NO_OPERATION_SELECTED_EDIT_MESSAGE =
                "bond.dialog.noOperationSelected.edit";
        public static final String BOND_DIALOG_NO_OPERATION_SELECTED_DELETE_MESSAGE =
                "bond.dialog.noOperationSelected.delete";
        public static final String BOND_DIALOG_EDIT_PURCHASE_TITLE =
                "bond.dialog.editPurchase.title";
        public static final String BOND_DIALOG_EDIT_SALE_TITLE = "bond.dialog.editSale.title";
        public static final String BOND_DIALOG_PURCHASE_UPDATED_TITLE =
                "bond.dialog.purchaseUpdated.title";
        public static final String BOND_DIALOG_PURCHASE_UPDATED_MESSAGE =
                "bond.dialog.purchaseUpdated.message";
        public static final String BOND_DIALOG_SALE_UPDATED_TITLE = "bond.dialog.saleUpdated.title";
        public static final String BOND_DIALOG_SALE_UPDATED_MESSAGE =
                "bond.dialog.saleUpdated.message";
        public static final String BOND_DIALOG_NO_CHANGES_TITLE = "bond.dialog.noChanges.title";
        public static final String BOND_DIALOG_NO_CHANGES_PURCHASE_MESSAGE =
                "bond.dialog.noChanges.purchase";
        public static final String BOND_DIALOG_NO_CHANGES_SALE_MESSAGE =
                "bond.dialog.noChanges.sale";
        public static final String BOND_DIALOG_ERROR_UPDATING_PURCHASE_TITLE =
                "bond.dialog.errorUpdatingPurchase.title";
        public static final String BOND_DIALOG_ERROR_UPDATING_SALE_TITLE =
                "bond.dialog.errorUpdatingSale.title";
        public static final String BOND_DIALOG_CONFIRM_DELETE_OPERATION_TITLE =
                "bond.dialog.confirmDeleteOperation.title";
        public static final String BOND_DIALOG_CONFIRM_DELETE_OPERATION_MESSAGE =
                "bond.dialog.confirmDeleteOperation.message";
        public static final String BOND_DIALOG_OPERATION_DELETED_TITLE =
                "bond.dialog.operationDeleted.title";
        public static final String BOND_DIALOG_OPERATION_DELETED_MESSAGE =
                "bond.dialog.operationDeleted.message";
        public static final String INVESTMENT_DIALOG_NO_DIVIDEND_SELECTED_DELETE_MESSAGE =
                "investment.dialog.noDividendSelectedDelete.message";
        public static final String INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_TITLE =
                "investment.dialog.noExchangeSelected.title";
        public static final String INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_MESSAGE =
                "investment.dialog.noExchangeSelected.message";
        public static final String INVESTMENT_DIALOG_NO_EXCHANGE_SELECTED_DELETE_MESSAGE =
                "investment.dialog.noExchangeSelectedDelete.message";
        public static final String INVESTMENT_DIALOG_CONFIRM_DELETE_PURCHASE_TITLE =
                "investment.dialog.confirmDeletePurchase.title";
        public static final String INVESTMENT_DIALOG_CONFIRM_DELETE_SALE_TITLE =
                "investment.dialog.confirmDeleteSale.title";
        public static final String INVESTMENT_DIALOG_CONFIRM_DELETE_DIVIDEND_TITLE =
                "investment.dialog.confirmDeleteDividend.title";
        public static final String INVESTMENT_DIALOG_CONFIRM_DELETE_EXCHANGE_TITLE =
                "investment.dialog.confirmDeleteExchange.title";
        public static final String INVESTMENT_DIALOG_NO_TICKER_SELECTED_TITLE =
                "investment.dialog.noTickerSelected.title";
        public static final String INVESTMENT_DIALOG_NO_TICKER_SELECTED_UNARCHIVE =
                "investment.dialog.noTickerSelected.unarchive";
        public static final String INVESTMENT_DIALOG_NO_TICKER_SELECTED_DELETE =
                "investment.dialog.noTickerSelected.delete";
        public static final String INVESTMENT_DIALOG_CONFIRM_UNARCHIVE_TICKER_TITLE =
                "investment.dialog.confirmUnarchiveTicker.title";
        public static final String INVESTMENT_DIALOG_CONFIRM_UNARCHIVE_TICKER_MESSAGE =
                "investment.dialog.confirmUnarchiveTicker.message";
        public static final String INVESTMENT_DIALOG_TICKER_UNARCHIVED_TITLE =
                "investment.dialog.tickerUnarchived.title";
        public static final String INVESTMENT_DIALOG_TICKER_UNARCHIVED_MESSAGE =
                "investment.dialog.tickerUnarchived.message";
        public static final String INVESTMENT_DIALOG_ERROR_UNARCHIVING_TICKER_TITLE =
                "investment.dialog.errorUnarchivingTicker.title";
        public static final String INVESTMENT_DIALOG_CONFIRM_DELETE_TICKER_TITLE =
                "investment.dialog.confirmDeleteTicker.title";
        public static final String INVESTMENT_DIALOG_CONFIRM_DELETE_TICKER_MESSAGE =
                "investment.dialog.confirmDeleteTicker.message";
        public static final String INVESTMENT_DIALOG_TICKER_DELETED_TITLE =
                "investment.dialog.tickerDeleted.title";
        public static final String INVESTMENT_DIALOG_TICKER_DELETED_MESSAGE =
                "investment.dialog.tickerDeleted.message";
        public static final String INVESTMENT_DIALOG_ERROR_DELETING_TICKER_TITLE =
                "investment.dialog.errorDeletingTicker.title";
        public static final String INVESTMENT_DIALOG_TICKER_HAS_TRANSACTIONS_TITLE =
                "investment.dialog.tickerHasTransactions.title";
        public static final String INVESTMENT_DIALOG_TICKER_HAS_TRANSACTIONS_MESSAGE =
                "investment.dialog.tickerHasTransactions.message";

        // Investment FXML Labels
        public static final String INVESTMENT_BUTTON_YAHOO_LOOKUP_TOOLTIP =
                "investment.button.yahooLookup.tooltip";
        public static final String INVESTMENT_LABEL_NAME = "investment.label.name";
        public static final String INVESTMENT_LABEL_SYMBOL = "investment.label.symbol";
        public static final String INVESTMENT_LABEL_TYPE = "investment.label.type";
        public static final String INVESTMENT_LABEL_CURRENT_PRICE = "investment.label.currentPrice";
        public static final String INVESTMENT_LABEL_QUANTITY_IN_PORTFOLIO =
                "investment.label.quantityInPortfolio";
        public static final String INVESTMENT_LABEL_AVG_UNIT_PRICE =
                "investment.label.avgUnitPrice";
        public static final String INVESTMENT_LABEL_TICKER = "investment.label.ticker";
        public static final String INVESTMENT_LABEL_CURRENT_BALANCE =
                "investment.label.currentBalance";
        public static final String INVESTMENT_LABEL_BALANCE_AFTER_DIVIDEND =
                "investment.label.balanceAfterDividend";
        public static final String INVESTMENT_LABEL_BALANCE_AFTER_SALE =
                "investment.label.balanceAfterSale";
        public static final String INVESTMENT_LABEL_BALANCE_AFTER_PURCHASE =
                "investment.label.balanceAfterPurchase";
        public static final String INVESTMENT_LABEL_UNIT_PRICE = "investment.label.unitPrice";
        public static final String INVESTMENT_LABEL_QUANTITY = "investment.label.quantity";
        public static final String INVESTMENT_LABEL_TOTAL = "investment.label.total";
        public static final String INVESTMENT_LABEL_CRYPTO_SOLD = "investment.label.cryptoSold";
        public static final String INVESTMENT_LABEL_CRYPTO_RECEIVED =
                "investment.label.cryptoReceived";
        public static final String INVESTMENT_LABEL_CURRENT_QUANTITY =
                "investment.label.currentQuantity";
        public static final String INVESTMENT_LABEL_QUANTITY_AFTER_EXCHANGE =
                "investment.label.quantityAfterExchange";
        public static final String INVESTMENT_LABEL_QUANTITY_SOLD = "investment.label.quantitySold";
        public static final String INVESTMENT_LABEL_QUANTITY_RECEIVED =
                "investment.label.quantityReceived";
        public static final String INVESTMENT_LABEL_SELECT_TICKER = "investment.label.selectTicker";
        public static final String INVESTMENT_PROMPT_TEXT_SEARCH_ID_OR_DESCRIPTION =
                "investment.promptText.searchIdOrDescription";
        public static final String INVESTMENT_PROMPT_TEXT_SYMBOL = "investment.promptText.symbol";

        // Additional Investment Labels and Messages
        public static final String INVESTMENT_LABEL_CALCULATOR = "investment.label.calculator";
        public static final String INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_TITLE =
                "investment.dialog.invalidNumberCalculation.title";
        public static final String INVESTMENT_DIALOG_INVALID_NUMBER_CALCULATION_MESSAGE =
                "investment.dialog.invalidNumberCalculation.message";
        public static final String INVESTMENT_TOOLTIP_CATEGORY_REQUIRED =
                "investment.tooltip.categoryRequired";
        public static final String INVESTMENT_DIALOG_NO_CHANGES_TICKER_MESSAGE =
                "investment.dialog.noChangesTickerMessage";

        // Table Column Headers
        public static final String INVESTMENT_TABLE_ID = "investment.table.id";
        public static final String INVESTMENT_TABLE_NAME = "investment.table.name";
        public static final String INVESTMENT_TABLE_SYMBOL = "investment.table.symbol";
        public static final String INVESTMENT_TABLE_TYPE = "investment.table.type";
        public static final String INVESTMENT_TABLE_QUANTITY_OWNED =
                "investment.table.quantityOwned";
        public static final String INVESTMENT_TABLE_UNIT_PRICE = "investment.table.unitPrice";
        public static final String INVESTMENT_TABLE_TOTAL_VALUE = "investment.table.totalValue";
        public static final String INVESTMENT_TABLE_AVERAGE_UNIT_PRICE =
                "investment.table.averageUnitPrice";
        public static final String INVESTMENT_TABLE_TICKER = "investment.table.ticker";
        public static final String INVESTMENT_TABLE_DATE = "investment.table.date";
        public static final String INVESTMENT_TABLE_QUANTITY = "investment.table.quantity";
        public static final String INVESTMENT_TABLE_TOTAL_AMOUNT = "investment.table.totalAmount";
        public static final String INVESTMENT_TABLE_WALLET = "investment.table.wallet";
        public static final String INVESTMENT_TABLE_STATUS = "investment.table.status";
        public static final String INVESTMENT_TABLE_DESCRIPTION = "investment.table.description";
        public static final String INVESTMENT_TABLE_CATEGORY = "investment.table.category";
        public static final String INVESTMENT_TABLE_CRYPTO_SOLD = "investment.table.cryptoSold";
        public static final String INVESTMENT_TABLE_CRYPTO_RECEIVED =
                "investment.table.cryptoReceived";
        public static final String INVESTMENT_TABLE_QUANTITY_SOLD = "investment.table.quantitySold";
        public static final String INVESTMENT_TABLE_QUANTITY_RECEIVED =
                "investment.table.quantityReceived";
        public static final String INVESTMENT_TABLE_DIVIDEND_VALUE =
                "investment.table.dividendValue";

        // Investment Transaction Dialog Titles
        public static final String INVESTMENT_DIALOG_EDIT_TICKER_PURCHASE =
                "investment.dialog.editTickerPurchase";
        public static final String INVESTMENT_DIALOG_EDIT_TICKER_SALE =
                "investment.dialog.editTickerSale";
        public static final String INVESTMENT_DIALOG_EDIT_DIVIDEND =
                "investment.dialog.editDividend";
        public static final String INVESTMENT_DIALOG_EDIT_CRYPTO_EXCHANGE =
                "investment.dialog.editCryptoExchange";

        // Investment Delete Message Labels
        public static final String INVESTMENT_DELETE_DESCRIPTION = "investment.delete.description";
        public static final String INVESTMENT_DELETE_AMOUNT = "investment.delete.amount";
        public static final String INVESTMENT_DELETE_DATE = "investment.delete.date";
        public static final String INVESTMENT_DELETE_STATUS = "investment.delete.status";
        public static final String INVESTMENT_DELETE_WALLET = "investment.delete.wallet";
        public static final String INVESTMENT_DELETE_WALLET_BALANCE =
                "investment.delete.walletBalance";
        public static final String INVESTMENT_DELETE_WALLET_BALANCE_AFTER_DELETION =
                "investment.delete.walletBalanceAfterDeletion";
        public static final String INVESTMENT_DELETE_SOURCE_CRYPTO =
                "investment.delete.sourceCrypto";
        public static final String INVESTMENT_DELETE_TARGET_CRYPTO =
                "investment.delete.targetCrypto";
        public static final String INVESTMENT_DELETE_SOURCE_QUANTITY =
                "investment.delete.sourceQuantity";
        public static final String INVESTMENT_DELETE_SOURCE_QUANTITY_AFTER_DELETION =
                "investment.delete.sourceQuantityAfterDeletion";
        public static final String INVESTMENT_DELETE_TARGET_QUANTITY =
                "investment.delete.targetQuantity";
        public static final String INVESTMENT_DELETE_TARGET_QUANTITY_AFTER_DELETION =
                "investment.delete.targetQuantityAfterDeletion";

        // Wallet Transaction Labels
        public static final String WALLETTRANSACTION_TOOLTIP_NEED_CATEGORY =
                "wallettransaction.tooltip.needCategory";
        public static final String WALLETTRANSACTION_TOOLTIP_NEED_CATEGORY_RECURRING =
                "wallettransaction.tooltip.needCategoryRecurring";
        public static final String WALLETTRANSACTION_LABEL_CALCULATOR =
                "wallettransaction.label.calculator";
        public static final String WALLETTRANSACTION_SUGGESTION_FROM =
                "wallettransaction.suggestion.from";
        public static final String WALLETTRANSACTION_SUGGESTION_TO =
                "wallettransaction.suggestion.to";
        public static final String WALLETTRANSACTION_SUGGESTION_NO_CATEGORY =
                "wallettransaction.suggestion.noCategory";
        public static final String WALLETTRANSACTION_INFO_STARTS_ON =
                "wallettransaction.info.startsOn";
        public static final String WALLETTRANSACTION_INFO_ENDS_ON = "wallettransaction.info.endsOn";
        public static final String WALLETTRANSACTION_INFO_FREQUENCY =
                "wallettransaction.info.frequency";
        public static final String WALLETTRANSACTION_INFO_LAST_TRANSACTION =
                "wallettransaction.info.lastTransaction";

        // Wallet Transaction Dialog Messages
        public static final String WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE =
                "wallettransaction.dialog.emptyFields.title";
        public static final String WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE =
                "wallettransaction.dialog.emptyFields.message";
        public static final String WALLETTRANSACTION_DIALOG_EXPENSE_CREATED_TITLE =
                "wallettransaction.dialog.expenseCreated.title";
        public static final String WALLETTRANSACTION_DIALOG_EXPENSE_CREATED_MESSAGE =
                "wallettransaction.dialog.expenseCreated.message";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_EXPENSE_VALUE_TITLE =
                "wallettransaction.dialog.invalidExpenseValue.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_EXPENSE_VALUE_MESSAGE =
                "wallettransaction.dialog.invalidExpenseValue.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_CREATING_EXPENSE_TITLE =
                "wallettransaction.dialog.errorCreatingExpense.title";
        public static final String WALLETTRANSACTION_DIALOG_INCOME_CREATED_TITLE =
                "wallettransaction.dialog.incomeCreated.title";
        public static final String WALLETTRANSACTION_DIALOG_INCOME_CREATED_MESSAGE =
                "wallettransaction.dialog.incomeCreated.message";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_INCOME_VALUE_TITLE =
                "wallettransaction.dialog.invalidIncomeValue.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_INCOME_VALUE_MESSAGE =
                "wallettransaction.dialog.invalidIncomeValue.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_CREATING_INCOME_TITLE =
                "wallettransaction.dialog.errorCreatingIncome.title";
        public static final String WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_TITLE =
                "wallettransaction.dialog.transferCreated.title";
        public static final String WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_MESSAGE =
                "wallettransaction.dialog.transferCreated.message";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_VALUE_TITLE =
                "wallettransaction.dialog.invalidTransferValue.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_VALUE_MESSAGE =
                "wallettransaction.dialog.invalidTransferValue.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_CREATING_TRANSFER_TITLE =
                "wallettransaction.dialog.errorCreatingTransfer.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_TITLE =
                "wallettransaction.dialog.invalidTransfer.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_MESSAGE =
                "wallettransaction.dialog.invalidTransfer.message";
        public static final String WALLETTRANSACTION_DIALOG_TRANSACTION_UPDATED_TITLE =
                "wallettransaction.dialog.transactionUpdated.title";
        public static final String WALLETTRANSACTION_DIALOG_TRANSACTION_UPDATED_MESSAGE =
                "wallettransaction.dialog.transactionUpdated.message";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_TRANSACTION_VALUE_TITLE =
                "wallettransaction.dialog.invalidTransactionValue.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_TRANSACTION_VALUE_MESSAGE =
                "wallettransaction.dialog.invalidTransactionValue.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_UPDATING_TRANSACTION_TITLE =
                "wallettransaction.dialog.errorUpdatingTransaction.title";
        public static final String WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TITLE =
                "wallettransaction.dialog.noChangesMade.title";
        public static final String WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_MESSAGE =
                "wallettransaction.dialog.noChangesMade.message";
        public static final String WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TRANSFER_MESSAGE =
                "wallettransaction.dialog.noChangesMadeTransfer.message";
        public static final String WALLETTRANSACTION_DIALOG_TRANSFER_UPDATED_TITLE =
                "wallettransaction.dialog.transferUpdated.title";
        public static final String WALLETTRANSACTION_DIALOG_TRANSFER_UPDATED_MESSAGE =
                "wallettransaction.dialog.transferUpdated.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_UPDATING_TRANSFER_TITLE =
                "wallettransaction.dialog.errorUpdatingTransfer.title";
        public static final String WALLETTRANSACTION_DIALOG_RECURRING_TRANSACTION_CREATED_TITLE =
                "wallettransaction.dialog.recurringTransactionCreated.title";
        public static final String WALLETTRANSACTION_DIALOG_RECURRING_TRANSACTION_CREATED_MESSAGE =
                "wallettransaction.dialog.recurringTransactionCreated.message";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_RECURRING_VALUE_TITLE =
                "wallettransaction.dialog.invalidRecurringValue.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_RECURRING_VALUE_MESSAGE =
                "wallettransaction.dialog.invalidRecurringValue.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_CREATING_RECURRING_TITLE =
                "wallettransaction.dialog.errorCreatingRecurring.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_CREATED_TITLE =
                "wallettransaction.dialog.walletCreated.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_CREATED_MESSAGE =
                "wallettransaction.dialog.walletCreated.message";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_BALANCE_TITLE =
                "wallettransaction.dialog.invalidBalance.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_BALANCE_MESSAGE =
                "wallettransaction.dialog.invalidBalance.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_CREATING_WALLET_TITLE =
                "wallettransaction.dialog.errorCreatingWallet.title";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_RENAMING_WALLET_TITLE =
                "wallettransaction.dialog.errorRenamingWallet.title";
        public static final String WALLETTRANSACTION_DIALOG_INVALID_INPUT_TITLE =
                "wallettransaction.dialog.invalidInput.title";
        public static final String WALLETTRANSACTION_DIALOG_NO_CHANGES_BALANCE_MESSAGE =
                "wallettransaction.dialog.noChangesBalance.message";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_UPDATED_TITLE =
                "wallettransaction.dialog.walletUpdated.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_BALANCE_UPDATED_MESSAGE =
                "wallettransaction.dialog.walletBalanceUpdated.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_UPDATING_BALANCE_TITLE =
                "wallettransaction.dialog.errorUpdatingBalance.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_TYPE_CHANGED_TITLE =
                "wallettransaction.dialog.walletTypeChanged.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_TYPE_CHANGED_MESSAGE =
                "wallettransaction.dialog.walletTypeChanged.message";
        public static final String WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_TITLE =
                "wallettransaction.dialog.noWalletSelected.title";
        public static final String WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_UNARCHIVE_MESSAGE =
                "wallettransaction.dialog.noWalletSelectedUnarchive.message";
        public static final String WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_DELETE_MESSAGE =
                "wallettransaction.dialog.noWalletSelectedDelete.message";
        public static final String WALLETTRANSACTION_DIALOG_UNARCHIVE_WALLET_TITLE =
                "wallettransaction.dialog.unarchiveWallet.title";
        public static final String WALLETTRANSACTION_DIALOG_UNARCHIVE_WALLET_MESSAGE =
                "wallettransaction.dialog.unarchiveWallet.message";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_UNARCHIVED_TITLE =
                "wallettransaction.dialog.walletUnarchived.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_UNARCHIVED_MESSAGE =
                "wallettransaction.dialog.walletUnarchived.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_UNARCHIVING_WALLET_TITLE =
                "wallettransaction.dialog.errorUnarchivingWallet.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_HAS_TRANSACTIONS_TITLE =
                "wallettransaction.dialog.walletHasTransactions.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_HAS_TRANSACTIONS_MESSAGE =
                "wallettransaction.dialog.walletHasTransactions.message";
        public static final String WALLETTRANSACTION_DIALOG_REMOVE_WALLET_TITLE =
                "wallettransaction.dialog.removeWallet.title";
        public static final String WALLETTRANSACTION_DIALOG_REMOVE_WALLET_MESSAGE =
                "wallettransaction.dialog.removeWallet.message";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_DELETED_TITLE =
                "wallettransaction.dialog.walletDeleted.title";
        public static final String WALLETTRANSACTION_DIALOG_WALLET_DELETED_MESSAGE =
                "wallettransaction.dialog.walletDeleted.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_REMOVING_WALLET_TITLE =
                "wallettransaction.dialog.errorRemovingWallet.title";
        public static final String WALLETTRANSACTION_DIALOG_CONFIRM_DELETE =
                "wallettransaction.dialog.confirmDelete";
        public static final String WALLETTRANSACTION_LABEL_DESCRIPTION =
                "wallettransaction.label.description";
        public static final String WALLETTRANSACTION_LABEL_AMOUNT =
                "wallettransaction.label.amount";
        public static final String WALLETTRANSACTION_LABEL_DATE = "wallettransaction.label.date";
        public static final String WALLETTRANSACTION_LABEL_STATUS =
                "wallettransaction.label.status";
        public static final String WALLETTRANSACTION_LABEL_WALLET =
                "wallettransaction.label.wallet";
        public static final String WALLETTRANSACTION_LABEL_WALLET_BALANCE =
                "wallettransaction.label.walletBalance";
        public static final String WALLETTRANSACTION_LABEL_WALLET_BALANCE_AFTER_DELETION =
                "wallettransaction.label.walletBalanceAfterDeletion";
        public static final String WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_TITLE =
                "wallettransaction.dialog.noTransferSelected.title";
        public static final String WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_EDIT_MESSAGE =
                "wallettransaction.dialog.noTransferSelectedEdit.message";
        public static final String WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_DELETE_MESSAGE =
                "wallettransaction.dialog.noTransferSelectedDelete.message";
        public static final String WALLETTRANSACTION_DIALOG_ADD_NEW_TRANSFER_TITLE =
                "wallettransaction.dialog.addNewTransfer.title";
        public static final String WALLETTRANSACTION_DIALOG_EDIT_TRANSFER_TITLE =
                "wallettransaction.dialog.editTransfer.title";
        public static final String WALLETTRANSACTION_DIALOG_DELETE_TRANSFER_TITLE =
                "wallettransaction.dialog.deleteTransfer.title";
        public static final String WALLETTRANSACTION_DIALOG_DELETE_TRANSFER_MESSAGE =
                "wallettransaction.dialog.deleteTransfer.message";
        public static final String WALLETTRANSACTION_DIALOG_SUCCESS_TITLE =
                "wallettransaction.dialog.success.title";
        public static final String WALLETTRANSACTION_DIALOG_TRANSFER_DELETED_MESSAGE =
                "wallettransaction.dialog.transferDeleted.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_TITLE =
                "wallettransaction.dialog.error.title";
        public static final String WALLETTRANSACTION_TABLE_ID = "wallettransaction.table.id";
        public static final String WALLETTRANSACTION_TABLE_DATE = "wallettransaction.table.date";
        public static final String WALLETTRANSACTION_TABLE_DESCRIPTION =
                "wallettransaction.table.description";
        public static final String WALLETTRANSACTION_TABLE_AMOUNT =
                "wallettransaction.table.amount";
        public static final String WALLETTRANSACTION_TABLE_SENDER =
                "wallettransaction.table.sender";
        public static final String WALLETTRANSACTION_TABLE_RECEIVER =
                "wallettransaction.table.receiver";
        public static final String WALLETTRANSACTION_TABLE_CATEGORY =
                "wallettransaction.table.category";
        public static final String WALLETTRANSACTION_TABLE_WALLET =
                "wallettransaction.table.wallet";
        public static final String WALLETTRANSACTION_TABLE_TYPE = "wallettransaction.table.type";
        public static final String WALLETTRANSACTION_TABLE_STATUS =
                "wallettransaction.table.status";
        public static final String WALLETTRANSACTION_TABLE_FREQUENCY =
                "wallettransaction.table.frequency";
        public static final String WALLETTRANSACTION_TABLE_START_DATE =
                "wallettransaction.table.startDate";
        public static final String WALLETTRANSACTION_TABLE_END_DATE =
                "wallettransaction.table.endDate";
        public static final String WALLETTRANSACTION_TABLE_NEXT_DUE_DATE =
                "wallettransaction.table.nextDueDate";
        public static final String WALLETTRANSACTION_TABLE_EXPECTED_REMAINING_AMOUNT =
                "wallettransaction.table.expectedRemainingAmount";
        public static final String WALLETTRANSACTION_TABLE_INDEFINITE =
                "wallettransaction.table.indefinite";
        public static final String WALLETTRANSACTION_COMBOBOX_ALL =
                "wallettransaction.combobox.all";
        public static final String WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_TITLE =
                "wallettransaction.dialog.noRecurringSelected.title";
        public static final String WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_EDIT_MESSAGE =
                "wallettransaction.dialog.noRecurringSelectedEdit.message";
        public static final String WALLETTRANSACTION_DIALOG_NO_RECURRING_SELECTED_DELETE_MESSAGE =
                "wallettransaction.dialog.noRecurringSelectedDelete.message";
        public static final String WALLETTRANSACTION_DIALOG_CREATE_RECURRING_TRANSACTION_TITLE =
                "wallettransaction.dialog.createRecurringTransaction.title";
        public static final String WALLETTRANSACTION_DIALOG_EDIT_RECURRING_TRANSACTION_TITLE =
                "wallettransaction.dialog.editRecurringTransaction.title";
        public static final String WALLETTRANSACTION_DIALOG_REMOVE_RECURRING_TRANSACTION_TITLE =
                "wallettransaction.dialog.removeRecurringTransaction.title";
        public static final String WALLETTRANSACTION_DIALOG_REMOVE_RECURRING_TRANSACTION_MESSAGE =
                "wallettransaction.dialog.removeRecurringTransaction.message";
        public static final String WALLETTRANSACTION_DIALOG_RECURRING_TRANSACTION_UPDATED_TITLE =
                "wallettransaction.dialog.recurringTransactionUpdated.title";
        public static final String WALLETTRANSACTION_DIALOG_RECURRING_TRANSACTION_UPDATED_MESSAGE =
                "wallettransaction.dialog.recurringTransactionUpdated.message";
        public static final String WALLETTRANSACTION_DIALOG_ERROR_EDITING_RECURRING_TITLE =
                "wallettransaction.dialog.errorEditingRecurring.title";
        public static final String WALLETTRANSACTION_INFO_RECURRING_INACTIVE =
                "wallettransaction.info.recurringInactive";

        // Calendar Event Dialog Messages
        public static final String CALENDAR_DIALOG_EVENT_CREATED_TITLE =
                "calendar.dialog.eventCreated.title";
        public static final String CALENDAR_DIALOG_EVENT_CREATED_MESSAGE =
                "calendar.dialog.eventCreated.message";
        public static final String CALENDAR_DIALOG_ERROR_CREATING_EVENT_TITLE =
                "calendar.dialog.errorCreatingEvent.title";

        // Calendar Event Types
        public static final String CALENDAR_EVENTTYPE_CREDIT_CARD_STATEMENT_CLOSING =
                "calendar.eventtype.creditCardStatementClosing";
        public static final String CALENDAR_EVENTTYPE_CREDIT_CARD_DUE_DATE =
                "calendar.eventtype.creditCardDueDate";
        public static final String CALENDAR_EVENTTYPE_DEBT_PAYMENT_DUE_DATE =
                "calendar.eventtype.debtPaymentDueDate";
        public static final String CALENDAR_EVENTTYPE_INCOME_RECEIPT_DATE =
                "calendar.eventtype.incomeReceiptDate";

        // Category Dialog Messages
        public static final String CATEGORY_DIALOG_CATEGORY_ADDED_TITLE =
                "category.dialog.categoryAdded.title";
        public static final String CATEGORY_DIALOG_CATEGORY_ADDED_MESSAGE =
                "category.dialog.categoryAdded.message";
        public static final String CATEGORY_DIALOG_ERROR_ADDING_CATEGORY_TITLE =
                "category.dialog.errorAddingCategory.title";
        public static final String CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_NAME_TITLE =
                "category.dialog.errorUpdatingCategoryName.title";
        public static final String CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_TITLE =
                "category.dialog.errorUpdatingCategory.title";
        public static final String CATEGORY_DIALOG_CATEGORY_UPDATED_TITLE =
                "category.dialog.categoryUpdated.title";
        public static final String CATEGORY_DIALOG_CATEGORY_NAME_AND_ARCHIVED_UPDATED_MESSAGE =
                "category.dialog.categoryNameAndArchivedUpdated.message";
        public static final String CATEGORY_DIALOG_CATEGORY_ARCHIVED_UPDATED_MESSAGE =
                "category.dialog.categoryArchivedUpdated.message";
        public static final String CATEGORY_DIALOG_CATEGORY_NAME_UPDATED_MESSAGE =
                "category.dialog.categoryNameUpdated.message";
        public static final String CATEGORY_DIALOG_NO_CATEGORY_SELECTED_TITLE =
                "category.dialog.noCategorySelected.title";
        public static final String CATEGORY_DIALOG_NO_CATEGORY_SELECTED_EDIT_MESSAGE =
                "category.dialog.noCategorySelectedEdit.message";
        public static final String CATEGORY_DIALOG_NO_CATEGORY_SELECTED_REMOVE_MESSAGE =
                "category.dialog.noCategorySelectedRemove.message";
        public static final String CATEGORY_DIALOG_CATEGORY_HAS_TRANSACTIONS_TITLE =
                "category.dialog.categoryHasTransactions.title";
        public static final String CATEGORY_DIALOG_CATEGORY_HAS_TRANSACTIONS_MESSAGE =
                "category.dialog.categoryHasTransactions.message";
        public static final String CATEGORY_DIALOG_REMOVE_CATEGORY_TITLE =
                "category.dialog.removeCategory.title";
        public static final String CATEGORY_DIALOG_REMOVE_CATEGORY_MESSAGE =
                "category.dialog.removeCategory.message";
        public static final String CATEGORY_DIALOG_ERROR_REMOVING_CATEGORY_TITLE =
                "category.dialog.errorRemovingCategory.title";
        public static final String CATEGORY_DIALOG_ADD_CATEGORY_TITLE =
                "category.dialog.addCategory.title";
        public static final String CATEGORY_DIALOG_EDIT_CATEGORY_TITLE =
                "category.dialog.editCategory.title";
        public static final String CATEGORY_TABLE_CATEGORY = "category.table.category";
        public static final String CATEGORY_TABLE_ARCHIVED = "category.table.archived";
        public static final String CATEGORY_TABLE_ASSOCIATED_TRANSACTIONS =
                "category.table.associatedTransactions";
        public static final String CATEGORY_TABLE_YES = "category.table.yes";
        public static final String CATEGORY_TABLE_NO = "category.table.no";
        public static final String WALLETTRANSACTION_TABLE_ASSOCIATED_TRANSACTIONS =
                "wallettransaction.table.associatedTransactions";
        public static final String WALLETTRANSACTION_LABEL_TYPE = "wallettransaction.label.type";
        public static final String WALLETTRANSACTION_LABEL_FREQUENCY =
                "wallettransaction.label.frequency";
    }

    // Prevent instantiation
    private Constants() {}

    /**
     * Get a regex that matches digits up to n
     *
     * @param n The maximum number of digits
     * @return The regex
     * @throws IllegalArgumentException If n is negative
     */
    public static String getDigitsRegexUpTo(Integer n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative");
        }

        return "\\d{0," + n + "}";
    }

    /**
     * Calculate the number of months until the target date
     *
     * @param beginDate  The start date
     * @param targetDate The target date
     * @return The number of months until the target date
     */
    public static Integer calculateMonthsUntilTarget(LocalDate beginDate, LocalDate targetDate) {
        return Math.toIntExact(ChronoUnit.MONTHS.between(beginDate, targetDate));
    }

    /**
     * Calculate the number of days until the target date
     *
     * @param beginDate  The begin date
     * @param targetDate The target date
     * @return The number of days until the target date
     */
    public static Integer calculateDaysUntilTarget(LocalDate beginDate, LocalDate targetDate) {
        return Math.toIntExact(ChronoUnit.DAYS.between(beginDate, targetDate));
    }

    /**
     * Round price according to the ticker type
     *
     * @param price      The price to be rounded
     * @param tickerType The ticker type
     */
    public static BigDecimal roundPrice(BigDecimal price, TickerType tickerType) {
        // Stocks and funds have two decimal places
        // Cryptocurrencies have MAX allowed by settings
        if (tickerType.equals(TickerType.STOCK) || tickerType.equals(TickerType.FUND)) {
            return price.setScale(2, RoundingMode.HALF_UP);
        } else {
            return price.setScale(INVESTMENT_CALCULATION_PRECISION, RoundingMode.HALF_UP);
        }
    }

    /**
     * Get the Python interpreter path based on the operating system
     * For Windows with embedded Python, use the bundled interpreter
     * For Linux/Mac, use the system Python
     */
    private static String getPythonInterpreter() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            // Check if running from jpackage installation (embedded Python)
            String appPath = System.getProperty("jpackage.app-path");

            if (appPath != null) {
                // Running from jpackage installation
                java.io.File appDir = new java.io.File(appPath).getParentFile();

                if (appDir != null) {
                    String embeddedPython =
                            new java.io.File(appDir, "python-embedded\\python.exe")
                                    .getAbsolutePath();

                    if (new java.io.File(embeddedPython).exists()) {
                        return embeddedPython;
                    }
                }
            }

            // Fallback to system Python on Windows
            return "python";
        } else {
            // Linux/Mac
            return "/usr/bin/python3";
        }
    }
}
