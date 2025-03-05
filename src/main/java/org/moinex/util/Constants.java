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

/**
 * Constants used in the application
 */
public final class Constants
{
    public static final String LOG_FILE =
        System.getProperty("user.home") + "/.local/state/moinex/moinex.log";
    public static final String APP_NAME = "Moinex";

    public static final String SCRIPT_PATH = "/scripts/";

    public static final String PYTHON_INTERPRETER = "/usr/bin/python3";

    public static final String GET_STOCK_PRICE_SCRIPT = "get_stock_price.py";

    public static final String GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT =
        "get_brazilian_market_indicators.py";

    // Paths
    public static final String WALLET_TYPE_ICONS_PATH  = "/icon/wallet_type/";
    public static final String SIDEBAR_ICONS_PATH      = "/icon/sidebar/";
    public static final String CRC_OPERATOR_ICONS_PATH = "/icon/crc_operator/";
    public static final String COMMON_ICONS_PATH       = "/icon/common/";
    public static final String GIF_PATH                = "/icon/gif/";

    public static final String UI_MAIN_PATH   = "/ui/main/";
    public static final String UI_DIALOG_PATH = "/ui/dialog/";
    public static final String UI_COMMON_PATH = "/ui/common/";

    public static final String CSS_SCENE_PATH  = "/css/scene/";
    public static final String CSS_COMMON_PATH = "/css/common/";

    // FXML
    public static final String MAIN_FXML          = UI_MAIN_PATH + "main.fxml";
    public static final String HOME_FXML          = UI_MAIN_PATH + "home.fxml";
    public static final String WALLET_FXML        = UI_MAIN_PATH + "wallet.fxml";
    public static final String CREDIT_CARD_FXML   = UI_MAIN_PATH + "credit_card.fxml";
    public static final String TRANSACTION_FXML   = UI_MAIN_PATH + "transaction.fxml";
    public static final String GOALS_FXML         = UI_MAIN_PATH + "goals.fxml";
    public static final String SAVINGS_FXML       = UI_MAIN_PATH + "savings.fxml";
    public static final String CSV_IMPORT_FXML    = UI_MAIN_PATH + "csv_import.fxml";
    public static final String SETTINGS_FXML      = UI_MAIN_PATH + "settings.fxml";
    public static final String SPLASH_SCREEN_FXML = UI_MAIN_PATH + ("splash_screen."
                                                                    + "fxml");

    public static final String ADD_WALLET_FXML   = UI_DIALOG_PATH + "add_wallet.fxml";
    public static final String ADD_INCOME_FXML   = UI_DIALOG_PATH + "add_income.fxml";
    public static final String ADD_TRANSFER_FXML = UI_DIALOG_PATH + ("add_transfer."
                                                                     + "fxml");
    public static final String ADD_EXPENSE_FXML  = UI_DIALOG_PATH + "add_expense.fxml";
    public static final String ADD_CATEGORY_FXML = UI_DIALOG_PATH + ("add_category."
                                                                     + "fxml");

    public static final String ADD_CREDIT_CARD_DEBT_FXML =
        UI_DIALOG_PATH + "add_credit_card_debt.fxml";

    public static final String ADD_CREDIT_CARD_CREDIT_FXML =
        UI_DIALOG_PATH + "add_credit_card_credit.fxml";

    public static final String ADD_CREDIT_CARD_FXML =
        UI_DIALOG_PATH + "add_credit_card.fxml";

    public static final String BUY_TICKER_FXML   = UI_DIALOG_PATH + "buy_ticker.fxml";
    public static final String SALE_TICKER_FXML  = UI_DIALOG_PATH + "sale_ticker.fxml";
    public static final String ADD_TICKER_FXML   = UI_DIALOG_PATH + "add_ticker.fxml";
    public static final String ADD_DIVIDEND_FXML = UI_DIALOG_PATH + ("add_dividend."
                                                                     + "fxml");
    public static final String ADD_CRYPTO_EXCHANGE_FXML =
        UI_DIALOG_PATH + "add_crypto_exchange.fxml";

    public static final String ARCHIVED_WALLETS_FXML =
        UI_DIALOG_PATH + "archived_wallets.fxml";

    public static final String ARCHIVED_CREDIT_CARDS_FXML =
        UI_DIALOG_PATH + "archived_credit_cards.fxml";

    public static final String ARCHIVED_TICKERS_FXML =
        UI_DIALOG_PATH + "archived_tickers.fxml";

    public static final String REMOVE_TRANSACTION_FXML =
        UI_DIALOG_PATH + "remove_transaction.fxml";
    public static final String REMOVE_CATEGORY_FXML =
        UI_DIALOG_PATH + "remove_category.fxml";

    public static final String CHANGE_WALLET_TYPE_FXML =
        UI_DIALOG_PATH + "change_wallet_type.fxml";
    public static final String CHANGE_WALLET_BALANCE_FXML =
        UI_DIALOG_PATH + "change_wallet_balance.fxml";
    public static final String RENAME_WALLET_FXML =
        UI_DIALOG_PATH + "rename_wallet.fxml";
    public static final String MANAGE_CATEGORY_FXML =
        UI_DIALOG_PATH + "manage_category.fxml";
    public static final String EDIT_CATEGORY_FXML =
        UI_DIALOG_PATH + "edit_category.fxml";
    public static final String EDIT_TRANSACTION_FXML =
        UI_DIALOG_PATH + "edit_transaction.fxml";
    public static final String EDIT_CREDIT_CARD_FXML =
        UI_DIALOG_PATH + "edit_credit_card.fxml";
    public static final String EDIT_CREDIT_CARD_DEBT_FXML =
        UI_DIALOG_PATH + "edit_credit_card_debt.fxml";

    public static final String EDIT_GOAL_FXML   = UI_DIALOG_PATH + "edit_goal.fxml";
    public static final String EDIT_TICKER_FXML = UI_DIALOG_PATH + "edit_ticker.fxml";
    public static final String EDIT_TICKER_PURCHASE_FXML =
        UI_DIALOG_PATH + "edit_ticker_purchase.fxml";
    public static final String EDIT_TICKER_SALE_FXML =
        UI_DIALOG_PATH + "edit_ticker_sale.fxml";
    public static final String EDIT_DIVIDEND_FXML =
        UI_DIALOG_PATH + "edit_dividend.fxml";
    public static final String EDIT_CRYPTO_EXCHANGE_FXML =
        UI_DIALOG_PATH + "edit_crypto_exchange.fxml";

    public static final String CREDIT_CARD_INVOICE_PAYMENT_FXML =
        UI_DIALOG_PATH + "credit_card_invoice_payment.fxml";

    public static final String RECURRING_TRANSACTIONS_FXML =
        UI_DIALOG_PATH + "recurring_transaction.fxml";
    public static final String INVESTMENT_TRANSACTIONS_FXML =
        UI_DIALOG_PATH + "investment_transactions.fxml";
    public static final String ADD_RECURRING_TRANSACTION_FXML =
        UI_DIALOG_PATH + "add_recurring_transaction.fxml";
    public static final String EDIT_RECURRING_TRANSACTION_FXML =
        UI_DIALOG_PATH + "edit_recurring_transaction.fxml";

    public static final String ADD_GOAL_FXML = UI_DIALOG_PATH + "add_goal.fxml";

    public static final String ADD_CALENDAR_EVENT_FXML =
        UI_DIALOG_PATH + "add_calendar_event.fxml";

    public static final String WALLET_FULL_PANE_FXML =
        UI_COMMON_PATH + "wallet_full_pane.fxml";
    public static final String GOAL_FULL_PANE_FXML =
        UI_COMMON_PATH + "goal_full_pane.fxml";

    public static final String RESUME_PANE_FXML = UI_COMMON_PATH + "resume_pane.fxml";
    public static final String CRC_PANE_FXML    = UI_COMMON_PATH + ("credit_card_pane."
                                                                 + "fxml");

    public static final String CALCULATOR_FXML = UI_COMMON_PATH + "calculator.fxml";
    public static final String CALENDAR_FXML   = UI_COMMON_PATH + "calendar.fxml";

    // Icons
    public static final String HOME_EXPENSE_ICON = COMMON_ICONS_PATH + "expense.png";
    public static final String HOME_INCOME_ICON  = COMMON_ICONS_PATH + "income.png";
    public static final String SUCCESS_ICON      = COMMON_ICONS_PATH + "success.png";
    public static final String DEFAULT_ICON      = COMMON_ICONS_PATH + "default.png";
    public static final String TROPHY_ICON       = COMMON_ICONS_PATH + "trophy.png";

    // GIFs
    public static final String LOADING_GIF = GIF_PATH + "loading.gif";
    public static final String SAVINGS_SCREEN_SYNC_PRICES_BUTTON_DEFAULT_ICON =
        COMMON_ICONS_PATH + "synchronize.png";

    // CSS
    public static final String MAIN_STYLE_SHEET   = CSS_SCENE_PATH + "main.css";
    public static final String HOME_STYLE_SHEET   = CSS_SCENE_PATH + "home.css";
    public static final String WALLET_STYLE_SHEET = CSS_SCENE_PATH + "wallet.css";
    public static final String CREDIT_CARD_STYLE_SHEET =
        CSS_SCENE_PATH + "credit-card.css";
    public static final String TRANSACTION_STYLE_SHEET =
        CSS_SCENE_PATH + "transaction.css";
    public static final String GOALS_STYLE_SHEET   = CSS_SCENE_PATH + "goals.css";
    public static final String SAVINGS_STYLE_SHEET = CSS_SCENE_PATH + "savings.css";
    public static final String CSV_IMPORT_STYLE_SHEET =
        CSS_SCENE_PATH + "csv_import.css";
    public static final String SETTINGS_STYLE_SHEET = CSS_SCENE_PATH + "settings.css";

    public static final String COMMON_STYLE_SHEET =
        CSS_COMMON_PATH + "common-styles.css";

    // Main pane styles
    public static final String SIDEBAR_SELECTED_BUTTON_STYLE =
        "sidebar-button-selected";
    public static final String NEGATIVE_BALANCE_STYLE = "negative-balance";
    public static final String POSITIVE_BALANCE_STYLE = "positive-balance";
    public static final String NEUTRAL_BALANCE_STYLE  = "neutral-balance";

    // Home pane styles
    public static final String HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE = "income-item";
    public static final String HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE =
        "expense-item";

    public static final String HOME_CREDIT_CARD_ITEM_STYLE = "credit-card-item";
    public static final String HOME_CREDIT_CARD_ITEM_NAME_STYLE =
        "credit-card-item-name";
    public static final String HOME_CREDIT_CARD_ITEM_BALANCE_STYLE =
        "credit-card-item-balance";
    public static final String HOME_CREDIT_CARD_ITEM_DIGITS_STYLE =
        "credit-card-item-digits";
    public static final String HOME_CREDIT_CARD_ITEM_OPERATOR_STYLE =
        "credit-card-item-operator";

    public static final String HOME_WALLET_ITEM_STYLE         = "wallet-item";
    public static final String HOME_WALLET_ITEM_NAME_STYLE    = "wallet-item-name";
    public static final String HOME_WALLET_ITEM_BALANCE_STYLE = "wallet-item-balance";
    public static final String HOME_WALLET_TYPE_STYLE         = "wallet-item-type";

    public static final String TOOLTIP_STYLE = "tooltip";

    public static final String TOTAL_BALANCE_VALUE_LABEL_STYLE =
        "total-balance-value-label";
    public static final String TOTAL_BALANCE_FORESEEN_LABEL_STYLE =
        "total-balance-foreseen-label";

    // Wallet pane styles
    public static final String WALLET_TOTAL_BALANCE_WALLETS_LABEL_STYLE =
        "total-balance-wallets-label";
    public static final String WALLET_CHECK_BOX_STYLE = "check-box";

    // Icons sizes
    public static final Integer WALLET_TYPE_ICONS_SIZE           = 42; // 42x42 px
    public static final Integer CRC_OPERATOR_ICONS_SIZE          = 42; // 42x42 px
    public static final Integer HOME_LAST_TRANSACTIONS_ICON_SIZE = 32; // 32x32 px

    // Home scene constants
    public static final Integer HOME_LAST_TRANSACTIONS_SIZE                    = 15;
    public static final Integer HOME_LAST_TRANSACTIONS_DESCRIPTION_LABEL_WIDTH = 280;
    public static final Integer HOME_LAST_TRANSACTIONS_VALUE_LABEL_WIDTH       = 70;
    public static final Integer HOME_LAST_TRANSACTIONS_DATE_LABEL_WIDTH        = 80;
    public static final Integer HOME_LAST_TRANSACTIONS_WALLET_LABEL_WIDTH      = 100;
    public static final Integer HOME_LAST_TRANSACTIONS_CATEGORY_LABEL_WIDTH    = 100;
    public static final Integer HOME_LAST_TRANSACTIONS_STATUS_LABEL_WIDTH      = 80;
    public static final Integer HOME_PANES_ITEMS_PER_PAGE                      = 2;

    public static final Double EPSILON          = 1e-6;
    public static final Double ONE_SECOND_IN_NS = 1_000_000_000.0;

    // Credit card
    public static final Integer MAX_BILLING_DUE_DAY           = 28;
    public static final Integer INSTALLMENTS_FIELD_MAX_DIGITS = 3;
    public static final Short   MAX_INSTALLMENTS              = 999;

    // Animation constants
    public static final Double MENU_COLLAPSED_WIDTH = 80.0;
    public static final Double MENU_EXPANDED_WIDTH  = 220.0;

    public static final Integer XYBAR_CHART_MONTHS         = 12;
    public static final Integer XYBAR_CHART_FUTURE_MONTHS  = 6;
    public static final Integer MONTH_RESUME_FUTURE_MONTHS = 6;

    public static final Integer CRC_XYBAR_CHART_MAX_MONTHS = 25;
    public static final Integer XYBAR_CHART_TICKS          = 6;

    public static final Double FADE_IN_ANIMATION_DURATION  = 1.0; // s
    public static final Double FADE_OUT_ANIMATION_DURATION = 1.0; // s
    public static final Double SLIDE_ANIMATION_DURANTION   = 1.0; // s

    public static final Double  MENU_ANIMATION_DURATION        = 200.0; // ms
    public static final Integer XYBAR_CHART_ANIMATION_FRAMES   = 30;
    public static final Double  XYBAR_CHART_ANIMATION_DURATION = 0.3; // s
    public static final Double  TOOLTIP_ANIMATION_DURATION     = 0.5; // s
    public static final Double  TOOLTIP_ANIMATION_DELAY        = 0.5; // s

    public static final Integer HOME_ITEM_NODE_NAME_MAX_LENGTH = 100;

    // Calendar config
    public static final Integer YEAR_RESUME_FUTURE_YEARS    = 2;
    public static final Integer NON_LEAP_YEAR_FEBRUARY_DAYS = 28;
    public static final Integer WEEK_DAYS                   = 7;

    public static final String[] WEEKDAY_ABREVIATIONS = { "Sun", "Mon", "Tue", "Wed",
                                                          "Thu", "Fri", "Sat" };

    public static final Font CALENDAR_WEEKDAY_FONT_CONFIG =
        Font.font("Arial", FontWeight.BOLD, 14);

    public static final Font CALENDAR_DATE_FONT_CONFIG =
        Font.font("Arial", FontWeight.BOLD, 14);

    public static final Double CALENDAR_CELL_BORDER_WIDTH          = 0.5;
    public static final Double CALENDAR_CELL_EXTERNAL_BORDER_WIDTH = 2.0;

    // Circular progress bar on the goal pane
    public static final Double GOAL_PANE_PROGRESS_BAR_RADIUS = 80.0;
    public static final Double GOAL_PANE_PROGRESS_BAR_WIDTH  = 8.0;

    public static final Integer SUGGESTIONS_MAX_ITEMS = 5;

    // WARNING: Do not change this value. If you do, update too on the database
    public static final String GOAL_DEFAULT_WALLET_TYPE_NAME = "Goal";

    // Enough time for you to become poor :)
    // Or rich, who knows?
    // WARNING: Do not change this value. If you do, update too on the database
    public static final LocalDate RECURRING_TRANSACTION_DEFAULT_END_DATE =
        LocalDate.of(2100, 12, 31);

    public static final LocalTime RECURRING_TRANSACTION_DEFAULT_TIME =
        LocalTime.of(23, 59, 59, 0);

    public static final LocalTime RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME =
        LocalTime.of(0, 0, 0, 0);

    // Date formats
    public static final String DB_DATE_FORMAT            = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_FORMAT_NO_TIME       = "yyyy-MM-dd";
    public static final String SHORT_DATE_FORMAT_NO_TIME = "yy-MM-dd";
    public static final String DATE_FORMAT_WITH_TIME     = "yyyy-MM-dd HH:mm:ss";
    public static final String DB_MONTH_YEAR_FORMAT      = "yyyy-MM";

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
    public static final String CURRENCY_FORMAT = "$ #,##0.00; - $ #,##0.00";

    // Percentage with two decimal places
    public static final String PERCENTAGE_FORMAT = "0.00";

    public static final String CREDIT_CARD_NUMBER_FORMAT = "**** **** **** ####";

    // Regex
    public static final String DIGITS_ONLY_REGEX    = "\\d*";
    public static final String MONETARY_VALUE_REGEX = "\\d*\\.?\\d{0,2}";

    public static final Integer INVESTMENT_CALCULATION_PRECISION = 8;
    public static final String  INVESTMENT_VALUE_REGEX =
        "\\d*\\.?\\d{0," + INVESTMENT_CALCULATION_PRECISION + "}";

    // Yahoo Finance API constants
    public static final String IBOVESPA_TICKER = "^BVSP";
    public static final String DOLLAR_TICKER   = "USDBRL=X";
    public static final String EURO_TICKER     = "EURBRL=X";

    public static final String GOLD_TICKER           = "GC=F";
    public static final String SOYBEAN_TICKER        = "ZS=F";
    public static final String COFFEE_ARABICA_TICKER = "KC=F";
    public static final String WHEAT_TICKER          = "ZW=F";
    public static final String OIL_BRENT_TICKER      = "BZ=F";

    public static final String BITCOIN_TICKER  = "BTC-USD";
    public static final String ETHEREUM_TICKER = "ETH-USD";

    /**
     * Get a regex that matches digits up to n
     * @param n The maximum number of digits
     * @return The regex
     * @throws IllegalArgumentException If n is negative
     */
    public static String getDigitsRegexUpTo(Integer n)
    {
        if (n < 0)
        {
            throw new IllegalArgumentException("n must be non-negative");
        }

        return "\\d{0," + n + "}";
    }

    /**
     * Calculate the number of months until the target date
     * @param beginDate The begin date
     * @param targetDate The target date
     * @return The number of months until the target date
     */
    public static Long calculateMonthsUntilTarget(LocalDate beginDate,
                                                  LocalDate targetDate)
    {
        return ChronoUnit.MONTHS.between(beginDate, targetDate);
    }

    /**
     * Calculate the number of days until the target date
     * @param beginDate The begin date
     * @param targetDate The target date
     * @return The number of days until the target date
     */
    public static Long calculateDaysUntilTarget(LocalDate beginDate,
                                                LocalDate targetDate)
    {
        return ChronoUnit.DAYS.between(beginDate, targetDate);
    }

    /**
     * Round price according to the ticker type
     * @param price The price to be rounded
     * @param tickerType The ticker type
     */
    public static BigDecimal roundPrice(BigDecimal price, TickerType tickerType)
    {
        // Stocks and funds have two decimal places
        // Cryptocurrencies have MAX allowed by settings
        if (tickerType.equals(TickerType.STOCK) || tickerType.equals(TickerType.FUND))
        {
            return price.setScale(2, RoundingMode.HALF_UP);
        }
        else
        {
            return price.setScale(INVESTMENT_CALCULATION_PRECISION,
                                  RoundingMode.HALF_UP);
        }
    }

    // Prevent instantiation
    private Constants() { }
}
