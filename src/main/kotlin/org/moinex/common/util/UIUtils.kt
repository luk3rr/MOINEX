/*
 * Filename: UIUtils.kt (original filename: UIUtils.java)
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.common.util

import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.Chart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.text.Text
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.StringConverter
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.BondType
import org.moinex.model.enums.CalendarEventType
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.model.enums.CreditCardInvoiceStatus
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.PeriodType
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.Ticker
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.service.PreferencesService
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DecimalFormat
import java.text.MessageFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import java.util.ResourceBundle
import kotlin.math.absoluteValue

@Component
class UIUtils(
    preferencesService: PreferencesService,
) {
    init {
        Companion.preferencesService = preferencesService
    }

    companion object {
        private val currencyFormat = DecimalFormat(Constants.CURRENCY_FORMAT)
        private val percentageFormat = DecimalFormat(Constants.PERCENTAGE_FORMAT)
        private lateinit var preferencesService: PreferencesService

        @JvmStatic
        fun addTooltipToXYChartNode(
            node: Node,
            text: String,
        ) {
            node.setOnMouseEntered { node.style = "-fx-opacity: 0.7;" }
            node.setOnMouseExited { node.style = "-fx-opacity: 1;" }
            addTooltipToNode(node, text)
        }

        @JvmStatic
        fun addTooltipToNode(
            node: Node?,
            text: String?,
        ) {
            if (node == null || text == null) return

            Tooltip(text)
                .apply {
                    styleClass.add(Constants.TOOLTIP_STYLE)
                    showDelay = Duration.seconds(Constants.TOOLTIP_ANIMATION_DELAY)
                    showDuration = Duration.hours(1.0)
                    hideDelay = Duration.seconds(Constants.TOOLTIP_ANIMATION_DURATION)
                }.also { Tooltip.install(node, it) }
        }

        @JvmStatic
        fun addTooltipToAxisLabel(
            axis: CategoryAxis,
            labelText: String,
            tooltipText: String,
        ) {
            axis.childrenUnmodifiable
                .filterIsInstance<Text>()
                .firstOrNull { it.text == labelText }
                ?.let { addTooltipToNode(it, tooltipText) }
        }

        @JvmStatic
        fun removeTooltipFromNode(node: Node) {
            Tooltip.install(node, null)
        }

        @JvmStatic
        fun formatCurrency(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"
            return currencyFormat.format(value)
        }

        @JvmStatic
        fun formatCurrencySigned(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"

            val formatted = currencyFormat.format(value)
            return if (value.toDouble() > 0) "+ $formatted" else formatted
        }

        @JvmStatic
        fun formatCurrencyDynamic(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"

            val fractionDigits =
                when (value) {
                    is BigDecimal -> determineFractionDigits(value)
                    else -> 2
                }

            return DecimalFormat(Constants.CURRENCY_FORMAT)
                .apply {
                    minimumFractionDigits = fractionDigits
                    maximumFractionDigits = fractionDigits
                }.format(value)
        }

        private fun determineFractionDigits(value: BigDecimal): Int {
            if (value >= BigDecimal.ONE) return 2

            val absValue = value.stripTrailingZeros().abs()
            return absValue.scale().coerceAtLeast(2)
        }

        @JvmStatic
        fun formatPercentage(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"

            if (value.toDouble() < Constants.NEGATIVE_PERCENTAGE_THRESHOLD) {
                return preferencesService.translate(
                    TranslationKeys.UIUTILS_FORMAT_PERCENTAGE_TOO_MUCH_NEGATIVE,
                )
            }

            return "${percentageFormat.format(value.toDouble().absoluteValue)} %"
        }

        @JvmStatic
        fun formatPercentageForFundamentalAnalysis(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"

            return DecimalFormat("#,##0.00")
                .apply {
                    roundingMode = RoundingMode.HALF_UP
                }.format(value.toDouble()) + " %"
        }

        @JvmStatic
        fun formatNumWithDecimalPlaces(
            value: Number?,
            decimalPlaces: Int,
        ): String {
            if (value == null) return "-"
            return DecimalFormat("#,##0.${"0".repeat(decimalPlaces)}")
                .apply {
                    roundingMode = RoundingMode.HALF_UP
                }.format(value)
        }

        @JvmStatic
        fun setDatePickerFormat(datePicker: DatePicker) {
            val locale = preferencesService.locale
            Locale.setDefault(locale)

            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)

            datePicker.apply {
                converter =
                    object : StringConverter<LocalDate>() {
                        override fun toString(date: LocalDate?): String = date?.format(formatter) ?: ""

                        override fun fromString(string: String?): LocalDate? {
                            if (string.isNullOrBlank()) return null
                            return runCatching { LocalDate.parse(string, formatter) }.getOrNull()
                        }
                    }
                chronology = IsoChronology.INSTANCE
            }
        }

        @JvmStatic
        fun formatCreditCardNumber(lastFourDigits: String?): String {
            if (lastFourDigits == null) return "-"
            require(lastFourDigits.length == 4) {
                "The input must contain exactly 4 digits."
            }
            return Constants.CREDIT_CARD_NUMBER_FORMAT.replace("####", lastFourDigits)
        }

        @JvmStatic
        fun formatCurrencyYAxis(yAxis: NumberAxis) {
            yAxis.tickLabelFormatter =
                object : StringConverter<Number>() {
                    override fun toString(value: Number?): String = value?.let { formatCurrency(it) } ?: ""

                    override fun fromString(string: String?): Number {
                        if (string.isNullOrBlank() || string == "****") return 0

                        return runCatching {
                            currencyFormat.parse(string)
                        }.getOrElse {
                            throw IllegalArgumentException("Invalid currency value: $string", it)
                        }
                    }
                }
        }

        @JvmStatic
        fun resetLabel(label: Label) {
            label.text = "-"
            setLabelStyle(label, Constants.NEUTRAL_BALANCE_STYLE)
        }

        @JvmStatic
        fun setLabelStyle(
            label: Label,
            style: String,
        ) {
            label.styleClass.removeAll(
                Constants.NEGATIVE_BALANCE_STYLE,
                Constants.POSITIVE_BALANCE_STYLE,
                Constants.NEUTRAL_BALANCE_STYLE,
            )
            label.styleClass.add(style)
        }

        @JvmStatic
        fun <T> configureComboBox(
            comboBox: ComboBox<T>,
            displayFunction: (T) -> String,
        ) {
            comboBox.apply {
                cellFactory =
                    Callback {
                        object : ListCell<T>() {
                            override fun updateItem(
                                item: T?,
                                empty: Boolean,
                            ) {
                                super.updateItem(item, empty)
                                text = if (item == null || empty) null else displayFunction(item)
                            }
                        }
                    }

                buttonCell =
                    object : ListCell<T>() {
                        override fun updateItem(
                            item: T?,
                            empty: Boolean,
                        ) {
                            super.updateItem(item, empty)
                            text = if (item == null || empty) null else displayFunction(item)
                        }
                    }

                converter =
                    object : StringConverter<T>() {
                        override fun toString(item: T?): String? = item?.let { displayFunction(it) }

                        override fun fromString(string: String?): T? = null
                    }
            }
        }

        @JvmStatic
        fun updateWalletBalanceLabelStyle(
            wallet: Wallet,
            balanceLabel: Label,
        ) {
            val balance = wallet.balance
            balanceLabel.text = formatCurrencyDynamic(balance)

            val style =
                if (balance < BigDecimal.ZERO) {
                    Constants.NEGATIVE_BALANCE_STYLE
                } else {
                    Constants.NEUTRAL_BALANCE_STYLE
                }
            setLabelStyle(balanceLabel, style)
        }

        @JvmStatic
        @JvmOverloads
        @Throws(IOException::class)
        fun loadContentIntoTab(
            tab: Tab,
            fxmlPath: String,
            cssPath: String,
            springContext: ConfigurableApplicationContext,
            resourceClass: Class<*>,
            resources: ResourceBundle? = null,
        ) {
            val loader = FXMLLoader(resourceClass.getResource(fxmlPath), resources)
            loader.setControllerFactory { springContext.getBean(it) }
            val content = loader.load<Parent>()

            content.stylesheets.add(
                resourceClass.getResource(cssPath)?.toExternalForm()
                    ?: throw IllegalStateException("CSS not found: $cssPath"),
            )

            tab.content = content
        }

        @JvmStatic
        fun <S, T> alignTableColumn(
            column: TableColumn<S, T>,
            alignment: Pos,
            style: String = "-fx-padding: 0;",
        ) {
            column.cellFactory =
                Callback {
                    object : TableCell<S, T>() {
                        override fun updateItem(
                            item: T?,
                            empty: Boolean,
                        ) {
                            super.updateItem(item, empty)
                            if (item == null || empty) {
                                text = null
                                this.style = ""
                            } else {
                                text = item.toString()
                                this.alignment = alignment
                                this.style = style
                            }
                        }
                    }
                }
        }

        @JvmStatic
        fun alignTableColumn(
            columns: List<TableColumn<*, *>>,
            alignment: Pos,
            style: String = "",
        ) {
            columns.forEach { alignTableColumn(it, alignment, style) }
        }

        @JvmStatic
        fun applyDefaultChartStyle(chart: Chart) {
            chart.stylesheets.add(
                UIUtils::class.java
                    .getResource(Constants.CHARTS_COLORS_STYLE_SHEET)
                    ?.toExternalForm()
                    ?: throw IllegalStateException("Chart stylesheet not found"),
            )
        }

        // Translation methods
        @JvmStatic
        fun translateWalletType(walletType: WalletType): String {
            val name = walletType.name.lowercase().replace(" ", "")

            val typeKeyMap =
                mapOf(
                    "checkingaccount" to TranslationKeys.WALLET_TYPE_CHECKING,
                    "savingsaccount" to TranslationKeys.WALLET_TYPE_SAVINGS,
                    "broker" to TranslationKeys.WALLET_TYPE_BROKER,
                    "criptocurrency" to TranslationKeys.WALLET_TYPE_CRIPTOCURRENCY,
                    "foodvoucher" to TranslationKeys.WALLET_TYPE_FOOD_VOUCHER,
                    "mealvoucher" to TranslationKeys.WALLET_TYPE_MEAL_VOUCHER,
                    "wallet" to TranslationKeys.WALLET_TYPE_WALLET,
                    "goal" to TranslationKeys.WALLET_TYPE_GOAL,
                    "others" to TranslationKeys.WALLET_TYPE_OTHERS,
                )

            return typeKeyMap[name]?.let { preferencesService.translate(it) }
                ?: walletType.name
        }

        @JvmStatic
        fun translateTransactionStatus(status: WalletTransactionStatus): String =
            mapOf(
                "pending" to TranslationKeys.TRANSACTION_STATUS_PENDING,
                "confirmed" to TranslationKeys.TRANSACTION_STATUS_CONFIRMED,
            )[status.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: status.name

        @JvmStatic
        fun translateTransactionType(type: WalletTransactionType): String =
            mapOf(
                "income" to TranslationKeys.TRANSACTION_TYPE_INCOMES,
                "expense" to TranslationKeys.TRANSACTION_TYPE_EXPENSES,
            )[type.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: type.name

        @JvmStatic
        fun translateRecurringTransactionStatus(status: RecurringTransactionStatus): String =
            mapOf(
                "active" to TranslationKeys.RECURRING_TRANSACTION_STATUS_ACTIVE,
                "inactive" to TranslationKeys.RECURRING_TRANSACTION_STATUS_INACTIVE,
            )[status.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: status.name

        @JvmStatic
        fun translateRecurringTransactionFrequency(frequency: RecurringTransactionFrequency): String =
            mapOf(
                "daily" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_DAILY,
                "weekly" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_WEEKLY,
                "monthly" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_MONTHLY,
                "yearly" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_YEARLY,
            )[frequency.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: frequency.name

        @JvmStatic
        fun translateCalendarEventType(eventType: CalendarEventType): String =
            mapOf(
                "credit_card_statement_closing" to
                    TranslationKeys.CALENDAR_EVENTTYPE_CREDIT_CARD_STATEMENT_CLOSING,
                "credit_card_due_date" to
                    TranslationKeys.CALENDAR_EVENTTYPE_CREDIT_CARD_DUE_DATE,
                "debt_payment_due_date" to
                    TranslationKeys.CALENDAR_EVENTTYPE_DEBT_PAYMENT_DUE_DATE,
                "income_receipt_date" to
                    TranslationKeys.CALENDAR_EVENTTYPE_INCOME_RECEIPT_DATE,
            )[eventType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: eventType.description

        @JvmStatic
        fun translateAssetType(assetType: AssetType): String =
            mapOf(
                "stock" to TranslationKeys.ASSET_TYPE_STOCK,
                "fund" to TranslationKeys.ASSET_TYPE_FUND,
                "cryptocurrency" to TranslationKeys.ASSET_TYPE_CRYPTO,
                "reit" to TranslationKeys.ASSET_TYPE_REIT,
                "etf" to TranslationKeys.ASSET_TYPE_ETF,
                "bond" to TranslationKeys.ASSET_TYPE_BOND,
            )[assetType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: assetType.name

        @JvmStatic
        fun translatePeriodType(periodType: PeriodType): String =
            mapOf(
                "annual" to TranslationKeys.PERIOD_TYPE_ANNUAL,
                "quarterly" to TranslationKeys.PERIOD_TYPE_QUARTERLY,
            )[periodType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: periodType.name

        @JvmStatic
        fun translateBondType(bondType: BondType): String =
            mapOf(
                "cdb" to TranslationKeys.BOND_TYPE_CDB,
                "lci" to TranslationKeys.BOND_TYPE_LCI,
                "lca" to TranslationKeys.BOND_TYPE_LCA,
                "treasury_prefixed" to TranslationKeys.BOND_TYPE_TREASURY_PREFIXED,
                "treasury_postfixed" to TranslationKeys.BOND_TYPE_TREASURY_POSTFIXED,
                "international" to TranslationKeys.BOND_TYPE_INTERNATIONAL,
                "other" to TranslationKeys.BOND_TYPE_OTHER,
            )[bondType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: bondType.name

        @JvmStatic
        fun translateInterestType(interestType: InterestType): String =
            mapOf(
                "fixed" to TranslationKeys.INTEREST_TYPE_FIXED,
                "floating" to TranslationKeys.INTEREST_TYPE_FLOATING,
                "zero_coupon" to TranslationKeys.INTEREST_TYPE_ZERO_COUPON,
            )[interestType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: interestType.name

        @JvmStatic
        fun translateInterestIndex(interestIndex: InterestIndex): String =
            mapOf(
                "cdi" to TranslationKeys.INTEREST_INDEX_CDI,
                "selic" to TranslationKeys.INTEREST_INDEX_SELIC,
                "ipca" to TranslationKeys.INTEREST_INDEX_IPCA,
                "libor" to TranslationKeys.INTEREST_INDEX_LIBOR,
                "sofr" to TranslationKeys.INTEREST_INDEX_SOFR,
                "other" to TranslationKeys.INTEREST_INDEX_OTHER,
            )[interestIndex.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: interestIndex.name

        @JvmStatic
        fun translateCreditCardInvoiceStatus(status: CreditCardInvoiceStatus): String =
            mapOf(
                "open" to TranslationKeys.COMMON_CREDIT_CARD_OPEN,
                "closed" to TranslationKeys.COMMON_CREDIT_CARD_CLOSED,
            )[status.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: status.name

        @JvmStatic
        fun translateCreditCardCreditType(creditType: CreditCardCreditType): String =
            mapOf(
                "cashback" to TranslationKeys.CREDIT_CARD_CREDIT_TYPE_CASHBACK,
                "refund" to TranslationKeys.CREDIT_CARD_CREDIT_TYPE_REFUND,
                "reward" to TranslationKeys.CREDIT_CARD_CREDIT_TYPE_REWARD,
            )[creditType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: creditType.name

        @JvmStatic
        fun translateOperationType(operationType: OperationType): String =
            mapOf(
                "buy" to TranslationKeys.OPERATION_TYPE_BUY,
                "sell" to TranslationKeys.OPERATION_TYPE_SELL,
            )[operationType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: operationType.name

        @JvmStatic
        fun getVirtualWalletInfo(wallet: Wallet): String =
            if (wallet.isMaster()) {
                preferencesService.translate(
                    TranslationKeys.HOME_WALLET_TOOLTIP_NOT_VIRTUAL_WALLET,
                )
            } else {
                MessageFormat.format(
                    preferencesService.translate(
                        TranslationKeys.HOME_WALLET_TOOLTIP_IS_VIRTUAL_WALLET,
                    ),
                    wallet.masterWallet?.name,
                )
            }

        // Date formatting methods
        @JvmStatic
        fun getShortMonthYearFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM/yy", locale)

        @JvmStatic
        fun getFullMonthYearFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", locale)

        @JvmStatic
        fun getYearFormatter(locale: Locale): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy", locale)

        @JvmStatic
        fun formatShortMonthYear(dateTime: LocalDateTime): String =
            dateTime.format(getShortMonthYearFormatter(preferencesService.locale))

        @JvmStatic
        fun formatShortMonthYear(yearMonth: YearMonth): String =
            yearMonth.format(getShortMonthYearFormatter(preferencesService.locale))

        @JvmStatic
        fun formatFullMonthYear(yearMonth: YearMonth): String =
            yearMonth.format(getFullMonthYearFormatter(preferencesService.locale))

        @JvmStatic
        fun formatYear(year: Year): String = year.format(getYearFormatter(preferencesService.locale))

        @JvmStatic
        fun getWeekdayAbbreviations(): Array<String> {
            val locale = preferencesService.locale
            return arrayOf(
                DayOfWeek.SUNDAY.getDisplayName(TextStyle.SHORT, locale),
                DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, locale),
                DayOfWeek.TUESDAY.getDisplayName(TextStyle.SHORT, locale),
                DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.SHORT, locale),
                DayOfWeek.THURSDAY.getDisplayName(TextStyle.SHORT, locale),
                DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, locale),
                DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, locale),
            )
        }

        @JvmStatic
        fun getMonthDisplayName(month: Month): String = month.getDisplayName(TextStyle.FULL, preferencesService.locale)

        @JvmStatic
        fun getMonthShortDisplayName(month: Month): String =
            month.getDisplayName(TextStyle.SHORT, preferencesService.locale)

        @JvmStatic
        fun formatDateForDisplay(date: LocalDate?): String {
            if (date == null) return ""
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(preferencesService.locale)
            return date.format(formatter)
        }

        @JvmStatic
        fun formatDateForDisplay(dateTime: LocalDateTime?): String {
            if (dateTime == null) return ""
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(preferencesService.locale)
            return dateTime.format(formatter)
        }

        @JvmStatic
        fun formatDateTimeForDisplay(dateTime: LocalDateTime?): String {
            if (dateTime == null) return ""
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(preferencesService.locale)
            return dateTime.format(formatter)
        }

        @JvmStatic
        fun getOrDefault(
            value: Any?,
            default: Any,
        ): Any = if (value == null || value.toString() == "null") default else value

        @JvmStatic
        fun loadTickerLogo(
            ticker: Ticker?,
            size: Double,
        ): ImageView? {
            if (ticker?.domain.isNullOrEmpty()) return null

            return runCatching {
                val domain =
                    ticker.domain!!
                        .replace("https://", "")
                        .replace("http://", "")
                        .replace("www.", "")
                        .split("/")[0]

                val filename = "$domain.png"
                val logoPath = Paths.get(Constants.LOGOS_DIR, filename)

                if (Files.exists(logoPath)) {
                    val imageUrl = "file://${logoPath.toAbsolutePath()}"
                    val image = Image(imageUrl, size, size, true, true)

                    if (!image.isError) {
                        ImageView(image).apply {
                            fitWidth = size
                            fitHeight = size
                            isPreserveRatio = true
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
            }.getOrNull()
        }
    }
}
