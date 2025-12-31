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
import org.moinex.util.enums.TickerType;

/**
 * Constants used in the application
 */
public final class Constants {
    public static final String APP_NAME = "Moinex";

    public static final String SCRIPT_PATH = "/scripts/";

    public static final String PYTHON_INTERPRETER = "/usr/bin/python3";

    public static final String GET_STOCK_PRICE_SCRIPT = "get_stock_price.py";

    public static final String GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT =
            "get_brazilian_market_indicators.py";

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
    public static final String ADD_DIVIDEND_FXML =
            UI_DIALOG_INVESTMENT_PATH + ("add_dividend." + "fxml");
    public static final String ADD_CRYPTO_EXCHANGE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "add_crypto_exchange.fxml";
    public static final String ARCHIVED_TICKERS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "archived_tickers.fxml";
    public static final String EDIT_TICKER_FXML = UI_DIALOG_INVESTMENT_PATH + "edit_ticker.fxml";
    public static final String EDIT_TICKER_PURCHASE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_ticker_purchase.fxml";
    public static final String EDIT_TICKER_SALE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_ticker_sale.fxml";
    public static final String EDIT_DIVIDEND_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_dividend.fxml";
    public static final String EDIT_CRYPTO_EXCHANGE_FXML =
            UI_DIALOG_INVESTMENT_PATH + "edit_crypto_exchange.fxml";
    public static final String INVESTMENT_TRANSACTIONS_FXML =
            UI_DIALOG_INVESTMENT_PATH + "investment_transactions.fxml";
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

    // Info styles from common-styles.css
    public static final String INFO_LABEL_RED_STYLE = "info-label-red";
    public static final String INFO_LABEL_GREEN_STYLE = "info-label-green";
    public static final String INFO_LABEL_YELLOW_STYLE = "info-label-yellow";

    // Main pane styles
    public static final String SIDEBAR_SELECTED_BUTTON_STYLE = "sidebar-button-selected";
    public static final String NEGATIVE_BALANCE_STYLE = "negative-balance";
    public static final String POSITIVE_BALANCE_STYLE = "positive-balance";
    public static final String NEUTRAL_BALANCE_STYLE = "neutral-balance";

    // Home pane styles
    public static final String HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE = "income-item";
    public static final String HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE = "expense-item";

    public static final String HOME_CREDIT_CARD_ITEM_STYLE = "credit-card-item";
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

    public static final String[] WEEKDAY_ABBREVIATIONS = {
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

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
    public static final String CURRENCY_FORMAT = "R$ #,##0.00; - R$ #,##0.00";

    // Percentage with two decimal places
    public static final String PERCENTAGE_FORMAT = "0.00";

    public static final String CREDIT_CARD_NUMBER_FORMAT = "**** **** **** ####";

    // Regex
    public static final String DIGITS_ONLY_REGEX = "\\d*";
    public static final String MONETARY_VALUE_REGEX = "\\d*\\.?\\d{0,2}";
    public static final String BUDGET_GROUP_PERCENTAGE_REGEX = "\\d{0,2}(\\.\\d{0,2})?|100(\\.00)?";

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

        // Dialogs
        public static final String DIALOG_CONFIRMATION_TITLE = "dialog.confirmation.title";
        public static final String DIALOG_INFO_TITLE = "dialog.info.title";
        public static final String DIALOG_ERROR_TITLE = "dialog.error.title";
        public static final String DIALOG_SUCCESS_TITLE = "dialog.success.title";
        public static final String DIALOG_BUTTON_YES = "dialog.button.yes";
        public static final String DIALOG_BUTTON_NO = "dialog.button.no";
        public static final String DIALOG_BUTTON_OK = "dialog.button.ok";
        public static final String DIALOG_BUTTON_CANCEL = "dialog.button.cancel";

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

        // Ticker types
        public static final String TICKER_TYPE_STOCK = "ticker.type.stock";
        public static final String TICKER_TYPE_FUND = "ticker.type.fund";
        public static final String TICKER_TYPE_CRYPTO = "ticker.type.crypto";

        // Transaction status
        public static final String TRANSACTION_STATUS_PENDING = "transaction.status.pending";
        public static final String TRANSACTION_STATUS_CONFIRMED = "transaction.status.confirmed";

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
}
