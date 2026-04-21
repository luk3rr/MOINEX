/*
 * Filename: UIUtils.kt (original filename: UIUtils.java)
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.common.util

import javafx.animation.FadeTransition
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.Chart
import javafx.scene.chart.NumberAxis
import javafx.scene.control.ComboBox
import javafx.scene.control.Control
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.StackPane
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import javafx.stage.Popup
import javafx.util.Callback
import javafx.util.Duration
import javafx.util.StringConverter
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.BondType
import org.moinex.model.enums.CalendarEventType
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.model.enums.CreditCardInvoiceStatus
import org.moinex.model.enums.CreditCardRecurringFrequency
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
import org.moinex.common.constant.Files as FilesConstant

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

        fun addTooltipToXYChartNode(
            node: Node,
            text: String,
        ) {
            node.setOnMouseEntered { node.style = "-fx-opacity: 0.7;" }
            node.setOnMouseExited { node.style = "-fx-opacity: 1;" }
            addTooltipToNode(node, text)
        }

        fun addTooltipToNode(
            node: Node?,
            text: String?,
        ) {
            if (node == null || text == null) return

            val tooltip =
                Tooltip(text).apply {
                    styleClass.add(Styles.TOOLTIP_STYLE)
                    showDelay = Duration.seconds(Constants.TOOLTIP_ANIMATION_DELAY)
                    showDuration = Duration.hours(1.0)
                    hideDelay = Duration.seconds(Constants.TOOLTIP_ANIMATION_DURATION)
                }

            when (node) {
                is Control -> node.tooltip = tooltip
                else -> Tooltip.install(node, tooltip)
            }
        }

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

        fun removeTooltipFromNode(node: Node) {
            Tooltip.install(node, null)
        }

        fun showTemporaryNotification(
            message: String,
            targetNode: Node,
            offsetY: Double = -25.0,
            fadeInDuration: Double = 200.0,
            displayDuration: Double = 1000.0,
            fadeOutDuration: Double = 200.0,
        ) {
            val window = targetNode.scene?.window ?: return
            val screenBounds = targetNode.localToScreen(targetNode.boundsInLocal) ?: return

            val notification =
                Label(message).apply {
                    style = Styles.NOTIFICATION_STYLE
                    opacity = 0.0
                    isMouseTransparent = true
                }

            val popup =
                Popup().apply {
                    content.add(notification)
                    isAutoFix = false
                    isAutoHide = false
                }

            FxUtils.launchOnFxThread {
                val notifWidth = notification.width.takeIf { it > 0 } ?: notification.prefWidth(-1.0)
                popup.x = screenBounds.minX + (screenBounds.width - notifWidth) / 2
                popup.y = screenBounds.minY + offsetY

                val fadeIn =
                    FadeTransition(Duration.millis(fadeInDuration), notification).apply {
                        fromValue = 0.0
                        toValue = 1.0
                        setOnFinished { popup.show(window) }
                    }

                val pause = PauseTransition(Duration.millis(displayDuration))

                val fadeOut =
                    FadeTransition(Duration.millis(fadeOutDuration), notification).apply {
                        fromValue = 1.0
                        toValue = 0.0
                        setOnFinished { popup.hide() }
                    }

                SequentialTransition(fadeIn, pause, fadeOut).play()
            }
        }

        fun formatCurrency(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"
            return currencyFormat.format(value)
        }

        fun formatCurrencySigned(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"

            val formatted = currencyFormat.format(value)
            return if (value.toDouble() > 0) "+ $formatted" else formatted
        }

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

        fun formatPercentageForFundamentalAnalysis(value: Number?): String {
            if (value == null) return "-"
            if (preferencesService.hideMonetaryValues) return "****"

            return DecimalFormat("#,##0.00")
                .apply {
                    roundingMode = RoundingMode.HALF_UP
                }.format(value.toDouble()) + " %"
        }

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

        fun formatCreditCardNumber(lastFourDigits: String?): String {
            if (lastFourDigits == null) return "-"
            require(lastFourDigits.length == 4) {
                "The input must contain exactly 4 digits."
            }
            return Constants.CREDIT_CARD_NUMBER_FORMAT.replace("####", lastFourDigits)
        }

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

        fun resetLabel(label: Label) {
            label.text = "-"
            setLabelStyle(label, Styles.NEUTRAL_BALANCE_STYLE)
        }

        fun setLabelStyle(
            label: Label,
            style: String,
        ) {
            label.styleClass.removeAll(
                Styles.NEGATIVE_BALANCE_STYLE,
                Styles.POSITIVE_BALANCE_STYLE,
                Styles.NEUTRAL_BALANCE_STYLE,
            )
            label.styleClass.add(style)
        }

        private fun createCopyableDetailGraphic(
            keyText: Text,
            separator: Text,
            valueText: Text,
            value: String,
        ): StackPane =
            StackPane().apply {
                val textFlow = TextFlow(keyText, separator, valueText)
                children.add(textFlow)
                alignment = Pos.CENTER_LEFT

                valueText.setOnMouseClicked {
                    Clipboard.getSystemClipboard().setContent(
                        ClipboardContent().apply { putString(value) },
                    )
                    showTemporaryNotification(
                        preferencesService.translate(TranslationKeys.UIUTILS_COPIED_TO_CLIPBOARD),
                        textFlow,
                    )
                }
                valueText.setOnMouseEntered {
                    valueText.style = Styles.DETAIL_VALUE_COPYABLE_HOVER_STYLE
                }
                valueText.setOnMouseExited {
                    valueText.style = Styles.DETAIL_VALUE_COPYABLE_STYLE
                }
            }

        private fun createDetailGraphic(
            keyText: Text,
            separator: Text,
            valueText: Text,
            value: String,
            showColon: Boolean,
            copyable: Boolean,
        ): Node {
            if (value.isEmpty() && !showColon) {
                return TextFlow(keyText)
            }

            if (copyable && !showColon && value.isNotEmpty()) {
                return createCopyableDetailGraphic(keyText, separator, valueText, value)
            }

            return TextFlow(
                keyText,
                separator,
                if (!showColon) valueText else Text(""),
            )
        }

        fun createDetailLabel(
            labelKey: String,
            value: String,
            showColon: Boolean = false,
            copyable: Boolean = false,
        ): Label {
            val labelText = preferencesService.translate(labelKey)

            return Label().apply {
                val keyText = Text(labelText).apply { style = Styles.DETAIL_KEY_LABEL_STYLE }
                val separator = if (showColon) Text(":") else Text(": ")
                val valueText =
                    Text(value).apply {
                        style = if (copyable) Styles.DETAIL_VALUE_COPYABLE_STYLE else Styles.DETAIL_VALUE_LABEL_STYLE
                    }

                graphic = createDetailGraphic(keyText, separator, valueText, value, showColon, copyable)
            }
        }

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

        fun updateWalletBalanceLabelStyle(
            wallet: Wallet,
            balanceLabel: Label,
        ) {
            val balance = wallet.balance
            balanceLabel.text = formatCurrencyDynamic(balance)

            val style =
                if (balance < BigDecimal.ZERO) {
                    Styles.NEGATIVE_BALANCE_STYLE
                } else {
                    Styles.NEUTRAL_BALANCE_STYLE
                }
            setLabelStyle(balanceLabel, style)
        }

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

        fun <S, T> createCellFactory(alignment: Pos): Callback<TableColumn<S, T>, TableCell<S, T>> =
            Callback {
                object : TableCell<S, T>() {
                    override fun updateItem(
                        item: T?,
                        empty: Boolean,
                    ) {
                        super.updateItem(item, empty)
                        when {
                            item == null || empty -> {
                                text = null
                                graphic = null
                            }

                            item is ImageView -> {
                                graphic = item
                                style = "-fx-padding: 0; -fx-alignment: CENTER;"
                            }

                            else -> {
                                text = item.toString()
                                val cssAlignment =
                                    if (alignment == Pos.CENTER) "CENTER" else "CENTER-LEFT"
                                style = "-fx-padding: 0 10px; -fx-alignment: $cssAlignment;"
                            }
                        }
                    }
                }
            }

        fun <T> createListCell(textExtractor: (T) -> String = { it.toString() }): ListCell<T> =
            object : ListCell<T>() {
                override fun updateItem(
                    item: T?,
                    empty: Boolean,
                ) {
                    super.updateItem(item, empty)
                    text = if (item == null || empty) null else textExtractor(item)
                }
            }

        fun alignTableColumn(
            columns: List<TableColumn<*, *>>,
            alignment: Pos,
            style: String = "",
        ) {
            columns.forEach { alignTableColumn(it, alignment, style) }
        }

        fun applyDefaultChartStyle(chart: Chart) {
            chart.stylesheets.add(
                UIUtils::class.java
                    .getResource(FilesConstant.CHARTS_COLORS_STYLE_SHEET)
                    ?.toExternalForm()
                    ?: throw IllegalStateException("Chart stylesheet not found"),
            )
        }

        // Translation methods
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

        fun translateTransactionStatus(status: WalletTransactionStatus): String =
            mapOf(
                "pending" to TranslationKeys.TRANSACTION_STATUS_PENDING,
                "confirmed" to TranslationKeys.TRANSACTION_STATUS_CONFIRMED,
            )[status.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: status.name

        fun translateTransactionType(type: WalletTransactionType): String =
            mapOf(
                "income" to TranslationKeys.TRANSACTION_TYPE_INCOMES,
                "expense" to TranslationKeys.TRANSACTION_TYPE_EXPENSES,
            )[type.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: type.name

        fun translateRecurringTransactionStatus(status: RecurringTransactionStatus): String =
            mapOf(
                "active" to TranslationKeys.RECURRING_TRANSACTION_STATUS_ACTIVE,
                "inactive" to TranslationKeys.RECURRING_TRANSACTION_STATUS_INACTIVE,
            )[status.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: status.name

        fun translateRecurringTransactionFrequency(frequency: RecurringTransactionFrequency): String =
            mapOf(
                "daily" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_DAILY,
                "weekly" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_WEEKLY,
                "monthly" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_MONTHLY,
                "yearly" to TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_YEARLY,
            )[frequency.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: frequency.name

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

        fun translatePeriodType(periodType: PeriodType): String =
            mapOf(
                "annual" to TranslationKeys.PERIOD_TYPE_ANNUAL,
                "quarterly" to TranslationKeys.PERIOD_TYPE_QUARTERLY,
            )[periodType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: periodType.name

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

        fun translateInterestType(interestType: InterestType): String =
            mapOf(
                "fixed" to TranslationKeys.INTEREST_TYPE_FIXED,
                "floating" to TranslationKeys.INTEREST_TYPE_FLOATING,
                "zero_coupon" to TranslationKeys.INTEREST_TYPE_ZERO_COUPON,
            )[interestType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: interestType.name

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

        fun translateCreditCardInvoiceStatus(status: CreditCardInvoiceStatus): String =
            mapOf(
                "open" to TranslationKeys.COMMON_CREDIT_CARD_OPEN,
                "closed" to TranslationKeys.COMMON_CREDIT_CARD_CLOSED,
            )[status.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: status.name

        fun translateCreditCardRecurringFrequency(frequency: CreditCardRecurringFrequency): String =
            mapOf(
                "monthly" to TranslationKeys.CREDIT_CARD_RECURRING_FREQUENCY_MONTHLY,
                "yearly" to TranslationKeys.CREDIT_CARD_RECURRING_FREQUENCY_YEARLY,
            )[frequency.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: frequency.name

        fun translateCreditCardCreditType(creditType: CreditCardCreditType): String =
            mapOf(
                "cashback" to TranslationKeys.CREDIT_CARD_CREDIT_TYPE_CASHBACK,
                "refund" to TranslationKeys.CREDIT_CARD_CREDIT_TYPE_REFUND,
                "reward" to TranslationKeys.CREDIT_CARD_CREDIT_TYPE_REWARD,
            )[creditType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: creditType.name

        fun translateOperationType(operationType: OperationType): String =
            mapOf(
                "buy" to TranslationKeys.OPERATION_TYPE_BUY,
                "sell" to TranslationKeys.OPERATION_TYPE_SELL,
            )[operationType.name.lowercase()]?.let { preferencesService.translate(it) }
                ?: operationType.name

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
        fun getShortMonthYearFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM/yy", locale)

        fun getFullMonthYearFormatter(locale: Locale): DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMMM yyyy", locale)

        fun getYearFormatter(locale: Locale): DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy", locale)

        fun formatShortMonthYear(dateTime: LocalDateTime): String =
            dateTime.format(getShortMonthYearFormatter(preferencesService.locale))

        fun formatShortMonthYear(yearMonth: YearMonth): String =
            yearMonth.format(getShortMonthYearFormatter(preferencesService.locale))

        fun formatFullMonthYear(yearMonth: YearMonth): String =
            yearMonth.format(getFullMonthYearFormatter(preferencesService.locale))

        fun formatYear(year: Year): String = year.format(getYearFormatter(preferencesService.locale))

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

        fun getMonthDisplayName(month: Month): String = month.getDisplayName(TextStyle.FULL, preferencesService.locale)

        fun getMonthShortDisplayName(month: Month): String =
            month.getDisplayName(TextStyle.SHORT, preferencesService.locale)

        fun formatDateForDisplay(date: LocalDate?): String {
            if (date == null) return ""
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(preferencesService.locale)
            return date.format(formatter)
        }

        fun formatDateForDisplay(dateTime: LocalDateTime?): String {
            if (dateTime == null) return ""
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDate(FormatStyle.SHORT)
                    .withLocale(preferencesService.locale)
            return dateTime.format(formatter)
        }

        fun formatDateTimeForDisplay(dateTime: LocalDateTime?): String {
            if (dateTime == null) return ""
            val formatter =
                DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(preferencesService.locale)
            return dateTime.format(formatter)
        }

        fun getOrDefault(
            value: Any?,
            default: Any,
        ): Any = if (value == null || value.toString() == "null") default else value

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
                val logoPath = Paths.get(FilesConstant.LOGOS_DIR, filename)

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
