package org.moinex.common.constant

import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import org.moinex.model.enums.AssetType
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Constants {
    const val APP_NAME: String = "Moinex"

    val PYTHON_INTERPRETER: String = pythonInterpreter

    const val YAHOO_LOOKUP_URL: String = "https://finance.yahoo.com/research-hub/screener/most_actives"

    // Top Performers Table
    const val TOP_PERFORMERS_ASSET_COLUMN_WIDTH: Double = 90.0
    const val TOP_PERFORMERS_RETURN_COLUMN_WIDTH: Double = 90.0
    const val TOP_PERFORMERS_VALUE_COLUMN_WIDTH: Double = 90.0

    // Allocation Panel Thresholds
    const val ALLOCATION_CRITICAL_LOW_THRESHOLD: Double = 0.0
    const val ALLOCATION_WARNING_LOW_THRESHOLD: Double = 80.0
    const val ALLOCATION_ON_TARGET_LOW_THRESHOLD: Double = 95.0
    const val ALLOCATION_ON_TARGET_HIGH_THRESHOLD: Double = 105.0
    const val ALLOCATION_WARNING_HIGH_THRESHOLD: Double = 120.0

    // Icons sizes
    const val WALLET_TYPE_ICONS_SIZE: Int = 42 // 42x42 px
    const val CRC_OPERATOR_ICONS_SIZE: Int = 42 // 42x42 px
    const val HOME_LAST_TRANSACTIONS_ICON_SIZE: Int = 32 // 32x32 px

    // Home scene constant
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

    // AnimationUtils constant
    const val MENU_COLLAPSED_WIDTH: Double = 80.0
    const val MENU_EXPANDED_WIDTH: Double = 220.0

    const val XYBAR_CHART_MONTHS: Int = 12
    const val XYBAR_CHART_FUTURE_MONTHS: Int = 6
    const val PL_CHART_MONTHS: Int = 12 * 5
    const val PL_CHART_FUTURE_MONTHS: Int = 3
    const val MONTH_RESUME_FUTURE_MONTHS: Int = 6

    const val CRC_XYBAR_CHART_MAX_MONTHS: Int = 25
    const val WISHLIST_XYBAR_CHART_PAST_MONTHS: Int = 6
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

    val CALENDAR_WEEKDAY_FONT_CONFIG: Font? = Font.font("Arial", FontWeight.BOLD, 14.0)

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
    val RECURRING_TRANSACTION_DEFAULT_END_DATE: LocalDate = LocalDate.of(2100, 12, 31)

    const val NA_DATA: String = "N/A"

    // Date formats
    const val DB_DATE_FORMAT: String = "yyyy-MM-dd'T'HH:mm:ss"
    const val DATE_FORMAT_NO_TIME: String = "yyyy-MM-dd"
    const val SHORT_DATE_FORMAT_NO_TIME: String = "yy-MM-dd"
    const val DATE_FORMAT_WITH_TIME: String = "yyyy-MM-dd HH:mm:ss"
    const val DB_MONTH_YEAR_FORMAT: String = "yyyy-MM"
    const val BACEN_DATE_FORMAT: String = "dd/MM/yyyy"

    val DB_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DB_DATE_FORMAT)

    val DATE_FORMATTER_NO_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_NO_TIME)

    val SHORT_DATE_FORMATTER_NO_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern(SHORT_DATE_FORMAT_NO_TIME)

    val DATE_FORMATTER_WITH_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_WITH_TIME)

    val DB_MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DB_MONTH_YEAR_FORMAT)

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

    const val INVESTMENT_VALUE_REGEX: String = "\\d*\\.?\\d{0,$INVESTMENT_CALCULATION_PRECISION}"

    // Yahoo Finance API constant
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
    fun getDigitsRegexUpTo(n: Int): String {
        require(n >= 0) { "n must be non-negative" }

        return "\\d{0,$n}"
    }

    /**
     * Calculate the number of months until the target date
     *
     * @param beginDate The start date
     * @param targetDate The target date
     * @return The number of months until the target date
     */
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
