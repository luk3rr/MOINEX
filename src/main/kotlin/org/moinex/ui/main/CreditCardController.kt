/*
 * Filename: CreditCardController.kt (original filename: CreditCardController.java)
 * Created on: October 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.moinex.constants.TranslationKeys
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.ui.common.CreditCardPaneController
import org.moinex.ui.dialog.creditcard.AddCreditCardController
import org.moinex.ui.dialog.creditcard.AddCreditCardDebtController
import org.moinex.ui.dialog.creditcard.ArchivedCreditCardsController
import org.moinex.ui.dialog.creditcard.EditCreditCardDebtController
import org.moinex.util.Animation
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import org.moinex.util.WindowUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.Year
import java.time.YearMonth

@Controller
class CreditCardController(
    private val creditCardService: CreditCardService,
    private val categoryService: CategoryService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var totalDebtsInfoVBox: VBox

    @FXML
    private lateinit var totalDebtsYearFilterComboBox: ComboBox<Year>

    @FXML
    private lateinit var debtsListMonthFilterComboBox: ComboBox<Month>

    @FXML
    private lateinit var debtsListYearFilterComboBox: ComboBox<Year>

    @FXML
    private lateinit var debtsTableView: TableView<CreditCardPayment>

    @FXML
    private lateinit var debtSearchField: TextField

    @FXML
    private lateinit var crcPane1: AnchorPane

    @FXML
    private lateinit var debtsFlowPane: AnchorPane

    @FXML
    private lateinit var crcNextButton: JFXButton

    @FXML
    private lateinit var crcPrevButton: JFXButton

    @FXML
    private lateinit var invoiceMonth: Label

    private var creditCards: List<CreditCard> = emptyList()
    private var crcPaneCurrentPage = 0

    companion object {
        private val logger = LoggerFactory.getLogger(CreditCardController::class.java)
    }

    @FXML
    fun initialize() {
        loadCreditCardsFromDatabase()

        populateDebtsListMonthFilterComboBox()
        populateYearFilterComboBox()
        configureTableView()
        configureListeners()

        val now = LocalDate.now()

        totalDebtsYearFilterComboBox.value = Year.from(now)
        debtsListMonthFilterComboBox.value = now.month
        debtsListYearFilterComboBox.value = Year.of(now.year)

        invoiceMonth.text = UIUtils.formatShortMonthYear(getTableCurrentMonthYear(), preferencesService)

        debtsListMonthFilterComboBox.setOnAction { updateDebtsTableView() }

        updateTotalDebtsInfo()
        updateDisplayCards()
        updateMoneyFlow()
        updateDebtsTableView()

        setButtonsActions()
    }

    @FXML
    private fun handleAddDebt() {
        WindowUtils.openModalWindow(
            Constants.ADD_CREDIT_CARD_DEBT_FXML,
            preferencesService.translate(
                TranslationKeys.CREDIT_CARD_DIALOG_ADD_DEBT_TITLE,
            ),
            springContext,
            { _: AddCreditCardDebtController -> },
            listOf(Runnable { updateDisplay() }),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleAddCreditCard() {
        WindowUtils.openModalWindow(
            Constants.ADD_CREDIT_CARD_FXML,
            preferencesService.translate(
                TranslationKeys.CREDIT_CARD_DIALOG_ADD_CREDIT_CARD_TITLE,
            ),
            springContext,
            { _: AddCreditCardController -> },
            listOf(Runnable { updateDisplayCards() }),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleEditDebt() {
        val selectedPayment = debtsTableView.selectionModel.selectedItem

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_EDIT_MESSAGE,
                ),
            )
            return
        }

        val debt = selectedPayment.creditCardDebt
        val payments = creditCardService.getPaymentsByDebtOrderedByInstallment(debt.id!!)

        if (payments.any { it.isRefunded() }) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_ALREADY_REFUNDED_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_ALREADY_REFUNDED_EDIT_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_CREDIT_CARD_DEBT_FXML,
            preferencesService.translate(
                TranslationKeys.CREDIT_CARD_DIALOG_EDIT_DEBT_TITLE,
            ),
            springContext,
            { controller: EditCreditCardDebtController -> controller.setCreditCardDebt(debt) },
            listOf(Runnable { updateDisplay() }),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleRefundDebt() {
        val selectedPayment = debtsTableView.selectionModel.selectedItem

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_REFUND_MESSAGE,
                ),
            )
            return
        }

        val debt = selectedPayment.creditCardDebt
        val payments = creditCardService.getPaymentsByDebtOrderedByInstallment(debt.id!!)

        if (payments.any { it.isRefunded() }) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_ALREADY_REFUNDED_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_ALREADY_REFUNDED_REFUND_MESSAGE,
                ),
            )
            return
        }

        val installmentsPaid = payments.count { it.isPaid() }
        val installmentsPending = payments.count { !it.isPaid() }

        val message =
            buildString {
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_DESCRIPTION,
                        ),
                        debt.description,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_AMOUNT,
                        ),
                        UIUtils.formatCurrency(debt.amount),
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS,
                        ),
                        debt.installments,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS_PAID,
                        ),
                        installmentsPaid,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS_PENDING,
                        ),
                        installmentsPending,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CATEGORY,
                        ),
                        debt.category.name,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CREDIT_CARD,
                        ),
                        debt.creditCard.name,
                    ),
                )
                append("\n\n")

                append(
                    preferencesService.translate(
                        TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_REFUND_MESSAGE,
                    ),
                )
            }

        if (
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_REFUND_TITLE,
                ),
                message,
                preferencesService.getBundle(),
            )
        ) {
            creditCardService.refundDebt(debt.id!!, null)
            updateDisplay()

            WindowUtils.showSuccessDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_REFUND_SUCCESS_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_REFUND_SUCCESS_MESSAGE,
                ),
            )
        }
    }

    @FXML
    private fun handleDeleteDebt() {
        val selectedPayment = debtsTableView.selectionModel.selectedItem

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_DELETE_MESSAGE,
                ),
            )
            return
        }

        val debt = selectedPayment.creditCardDebt
        val payments = creditCardService.getPaymentsByDebtOrderedByInstallment(debt.id!!)

        val refundAmount =
            payments
                .filter { it.isPaid() }
                .map { it.amount }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val installmentsPaid = payments.count { it.isPaid() }

        val message =
            buildString {
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_DESCRIPTION,
                        ),
                        debt.description,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_AMOUNT,
                        ),
                        UIUtils.formatCurrency(debt.amount),
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REGISTER_DATE,
                        ),
                        UIUtils.formatDateForDisplay(debt.date, preferencesService),
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS,
                        ),
                        debt.installments,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS_PAID,
                        ),
                        installmentsPaid,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CATEGORY,
                        ),
                        debt.category.name,
                    ),
                )
                append("\n")

                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CREDIT_CARD,
                        ),
                        debt.creditCard.name,
                    ),
                )
                append("\n")

                if (refundAmount > BigDecimal.ZERO) {
                    append(
                        MessageFormat.format(
                            preferencesService.translate(
                                TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REFUND_AMOUNT,
                            ),
                            UIUtils.formatCurrency(refundAmount),
                        ),
                    )
                    append("\n")

                    val walletNames =
                        payments
                            .filter { it.isPaid() }
                            .map { it.wallet!!.name }
                            .distinct()
                            .joinToString(", ")

                    append("\n")
                    append(
                        MessageFormat.format(
                            preferencesService.translate(
                                TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REFUND_WALLET,
                            ),
                            walletNames,
                        ),
                    )
                } else {
                    append(
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_NO_REFUND_AMOUNT,
                        ),
                    )
                }
            }

        if (
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_TITLE,
                ),
                message,
                preferencesService.getBundle(),
            )
        ) {
            creditCardService.deleteDebt(debt.id!!)
            updateDisplay()

            WindowUtils.showSuccessDialog(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_DELETE_SUCCESS_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DIALOG_DELETE_SUCCESS_MESSAGE,
                ),
            )
        }
    }

    @FXML
    private fun handleViewArchivedCreditCards() {
        WindowUtils.openModalWindow(
            Constants.ARCHIVED_CREDIT_CARDS_FXML,
            preferencesService.translate(
                TranslationKeys.CREDIT_CARD_DIALOG_CREDIT_CARD_ARCHIVE_TITLE,
            ),
            springContext,
            { _: ArchivedCreditCardsController -> },
            listOf(Runnable { updateDisplay() }),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleTablePrevMonth() {
        updateTableCurrentMonthYear(-1)
    }

    @FXML
    private fun handleTableNextMonth() {
        updateTableCurrentMonthYear(1)
    }

    private fun getTableCurrentMonthYear(): YearMonth =
        YearMonth.of(
            debtsListYearFilterComboBox.value.value,
            debtsListMonthFilterComboBox.value.value,
        )

    private fun updateTableCurrentMonthYear(offset: Int) {
        val nextYearMonth = getTableCurrentMonthYear().plusMonths(offset.toLong())
        debtsListMonthFilterComboBox.value = nextYearMonth.month
        debtsListYearFilterComboBox.value = Year.of(nextYearMonth.year)
        updateDebtsTableView()
    }

    fun updateDisplay(yearMonth: YearMonth? = null) {
        loadCreditCardsFromDatabase()

        updateDebtsTableView()
        updateTotalDebtsInfo()
        updateMoneyFlow()
        yearMonth?.let { updateDisplayCards(yearMonth) } ?: updateDisplayCards()
    }

    private fun loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByDebtCountDesc()
    }

    private fun updateDebtsTableView() {
        val selectedMonth = getTableCurrentMonthYear()
        val similarTextOrId = debtSearchField.text.lowercase()

        debtsTableView.items.clear()

        val payments = creditCardService.getPaymentsByMonth(selectedMonth)

        if (similarTextOrId.isEmpty()) {
            debtsTableView.items.addAll(payments)
        } else {
            payments
                .filter { payment ->
                    val description = payment.creditCardDebt.description?.lowercase()
                    val id = payment.creditCardDebt.id.toString()
                    val category =
                        payment.creditCardDebt.category.name
                            .lowercase()
                    val cardName =
                        payment.creditCardDebt.creditCard.name
                            .lowercase()
                    val value = payment.amount.toString()

                    description?.contains(similarTextOrId) ?: true ||
                        id.contains(similarTextOrId) ||
                        category.contains(similarTextOrId) ||
                        cardName.contains(similarTextOrId) ||
                        value.contains(similarTextOrId)
                }.forEach { debtsTableView.items.add(it) }
        }

        debtsTableView.refresh()
    }

    private fun updateTotalDebtsInfo() {
        val selectedYear = totalDebtsYearFilterComboBox.value

        val totalDebts = creditCardService.getTotalDebtAmountByYear(selectedYear)
        val totalPendingPayments = creditCardService.getTotalPendingPayments()

        val totalTotalDebtsLabel =
            Label(UIUtils.formatCurrency(totalDebts)).apply {
                styleClass.add(Constants.TOTAL_BALANCE_VALUE_LABEL_STYLE)
            }

        val totalPendingPaymentsLabel =
            Label(
                MessageFormat.format(
                    preferencesService.translate(
                        TranslationKeys.CREDIT_CARD_TOTAL_DEBTS_PENDING_PAYMENTS,
                    ),
                    UIUtils.formatCurrency(totalPendingPayments),
                ),
            ).apply {
                styleClass.add(Constants.TOTAL_BALANCE_FORESEEN_LABEL_STYLE)
            }

        totalDebtsInfoVBox.children.setAll(totalTotalDebtsLabel, totalPendingPaymentsLabel)
    }

    private fun updateDisplayCards() {
        updateDisplayCards(YearMonth.now())
    }

    private fun updateDisplayCards(defaultMonth: YearMonth) {
        crcPane1.children.clear()

        if (creditCards.isNotEmpty()) {
            if (crcPaneCurrentPage >= creditCards.size) {
                crcPaneCurrentPage = creditCards.size - 1
            }
            val crc = creditCards[crcPaneCurrentPage]

            runCatching {
                val loader =
                    FXMLLoader(
                        javaClass.getResource(Constants.CRC_PANE_FXML),
                        preferencesService.getBundle(),
                    )
                loader.setControllerFactory { springContext.getBean(it) }
                val newContent = loader.load<Parent>()

                newContent.stylesheets.add(
                    javaClass.getResource(Constants.COMMON_STYLE_SHEET)!!.toExternalForm(),
                )

                val crcPaneController = loader.getController<CreditCardPaneController>()
                crcPaneController.updateCreditCardPane(crc, defaultMonth)

                AnchorPane.setTopAnchor(newContent, 0.0)
                AnchorPane.setBottomAnchor(newContent, 0.0)
                AnchorPane.setLeftAnchor(newContent, 0.0)
                AnchorPane.setRightAnchor(newContent, 0.0)

                crcPane1.children.add(newContent)
            }.onFailure { e ->
                logger.error(
                    "Error loading credit card pane FXML: '{}' for card ID: {}",
                    Constants.CRC_PANE_FXML,
                    crc.id,
                    e,
                )
                logger.error(
                    "Credit card details - Name: '{}', Last 4 digits: '{}'",
                    crc.name,
                    crc.lastFourDigits,
                )
            }
        }

        crcPrevButton.isDisable = crcPaneCurrentPage == 0
        crcNextButton.isDisable = crcPaneCurrentPage == creditCards.size - 1 || creditCards.isEmpty()
    }

    private fun updateMoneyFlow() {
        val categoryAxis = CategoryAxis()
        val numberAxis = NumberAxis()
        val debtsFlowStackedBarChart = StackedBarChart(categoryAxis, numberAxis)

        debtsFlowStackedBarChart.verticalGridLinesVisible = false
        debtsFlowPane.children.clear()
        debtsFlowPane.children.add(debtsFlowStackedBarChart)

        AnchorPane.setTopAnchor(debtsFlowStackedBarChart, 0.0)
        AnchorPane.setBottomAnchor(debtsFlowStackedBarChart, 0.0)
        AnchorPane.setLeftAnchor(debtsFlowStackedBarChart, 0.0)
        AnchorPane.setRightAnchor(debtsFlowStackedBarChart, 0.0)

        debtsFlowStackedBarChart.data.clear()

        val currentDate = LocalDateTime.now()
        val formatter = UIUtils.getShortMonthYearFormatter(preferencesService.locale)

        val categories = categoryService.getNonArchivedCategoriesOrderedByName()
        val monthlyTotals = linkedMapOf<YearMonth, MutableMap<Category, Double>>()

        val halfMonths = Constants.CRC_XYBAR_CHART_MAX_MONTHS / 2

        for (i in halfMonths downTo -halfMonths) {
            val date = currentDate.minusMonths(i.toLong())
            val yearMonth = YearMonth.of(date.year, date.monthValue)

            val payments = creditCardService.getPaymentsByMonth(yearMonth)

            categories.forEach { category ->
                val total =
                    payments
                        .filter { it.creditCardDebt.category.id == category.id }
                        .filter { !it.isRefunded() || it.isPaid() }
                        .map { it.amount }
                        .fold(BigDecimal.ZERO, BigDecimal::add)

                monthlyTotals.getOrPut(yearMonth) { linkedMapOf() }[category] = total.toDouble()
            }
        }

        categories.forEach { category ->
            val series = XYChart.Series<String, Number>()
            series.name = category.name

            monthlyTotals.keys.forEach { yearMonth ->
                val total = monthlyTotals[yearMonth]?.get(category) ?: 0.0
                series.data.add(XYChart.Data(yearMonth.format(formatter), total))
            }

            if (series.data.any { it.yValue.toDouble() > 0 }) {
                debtsFlowStackedBarChart.data.add(series)
            }
        }

        val maxTotal =
            monthlyTotals.values.maxOfOrNull { monthData ->
                monthData.values.sumOf { it }
            } ?: 0.0

        Animation.setDynamicYAxisBounds(numberAxis, maxTotal)

        numberAxis.tickLabelFormatter =
            object : StringConverter<Number>() {
                override fun toString(value: Number): String = UIUtils.formatCurrency(value)

                override fun fromString(string: String): Number = 0
            }

        UIUtils.applyDefaultChartStyle(debtsFlowStackedBarChart)

        debtsFlowStackedBarChart.data.forEach { series ->
            series.data.forEach { data ->
                val yearMonth = YearMonth.parse(data.xValue, formatter)
                val monthTotal =
                    monthlyTotals[yearMonth]?.values?.sumOf { it } ?: 0.0

                val value = data.yValue.toDouble()
                val percentage = if (monthTotal > 0) (value / monthTotal) * 100 else 0.0

                UIUtils.addTooltipToXYChartNode(
                    data.node,
                    "${series.name}: ${UIUtils.formatCurrency(value)} " +
                        "(${UIUtils.formatPercentage(percentage, preferencesService)})\n" +
                        "Total: ${UIUtils.formatCurrency(monthTotal)}",
                )

                Animation.stackedXYChartAnimation(listOf(data), listOf(value))
            }
        }
    }

    private fun populateDebtsListMonthFilterComboBox() {
        debtsListMonthFilterComboBox.items.clear()

        val oldestDebtDate = creditCardService.getEarliestPaymentDate()
        val newestDebtDate = creditCardService.getLatestPaymentDate()

        var startYearMonth = YearMonth.from(oldestDebtDate)
        var endYearMonth = YearMonth.from(newestDebtDate)

        val yearMonths = mutableListOf<YearMonth>()

        while (endYearMonth.isAfter(startYearMonth) || endYearMonth == startYearMonth) {
            yearMonths.add(endYearMonth)
            endYearMonth = endYearMonth.minusMonths(1)
        }

        val yearMonthList = FXCollections.observableArrayList(yearMonths)

        val years =
            FXCollections.observableArrayList(
                yearMonthList
                    .map { it.year }
                    .distinct()
                    .sortedDescending()
                    .map { Year.of(it) },
            )

        debtsListYearFilterComboBox.items = years
        debtsListYearFilterComboBox.value = years.firstOrNull()

        val uniqueMonths =
            FXCollections.observableArrayList(
                yearMonthList
                    .map { it.month }
                    .distinct()
                    .sortedBy { it.value },
            )

        debtsListMonthFilterComboBox.items = uniqueMonths
        debtsListMonthFilterComboBox.value = uniqueMonths.firstOrNull()

        debtsListMonthFilterComboBox.converter =
            object : StringConverter<Month>() {
                override fun toString(month: Month?): String = month?.let { UIUtils.getMonthDisplayName(it, preferencesService) } ?: ""

                override fun fromString(string: String): Month = Month.valueOf(string.uppercase())
            }
    }

    private fun populateYearFilterComboBox() {
        val oldestDebtDate = creditCardService.getEarliestPaymentDate()
        val youngest = LocalDate.now().plusYears(Constants.YEAR_RESUME_FUTURE_YEARS.toLong())

        var startYear = Year.from(oldestDebtDate)
        var currentYear = Year.from(youngest)

        val years = mutableListOf<Year>()
        while (!startYear.isAfter(currentYear)) {
            years.add(currentYear)
            currentYear = currentYear.minusYears(1)
        }

        val yearList = FXCollections.observableArrayList(years)

        totalDebtsYearFilterComboBox.items = yearList

        totalDebtsYearFilterComboBox.converter =
            object : StringConverter<Year>() {
                private val formatter = UIUtils.getYearFormatter(preferencesService.locale)

                override fun toString(year: Year?): String = year?.format(formatter) ?: ""

                override fun fromString(string: String): Year = Year.parse(string, formatter)
            }
    }

    private fun setButtonsActions() {
        crcPrevButton.setOnAction {
            if (crcPaneCurrentPage > 0) {
                crcPaneCurrentPage--
                updateDisplayCards()
            }
        }

        crcNextButton.setOnAction {
            if (crcPaneCurrentPage < creditCards.size - 1) {
                crcPaneCurrentPage++
                updateDisplayCards()
            }
        }
    }

    private fun configureListeners() {
        totalDebtsYearFilterComboBox
            .valueProperty()
            .addListener { _, _, _ -> updateTotalDebtsInfo() }

        debtsListMonthFilterComboBox
            .valueProperty()
            .addListener { _, _, newMonth ->
                if (newMonth != null && debtsListYearFilterComboBox.value != null) {
                    invoiceMonth.text =
                        UIUtils.formatShortMonthYear(
                            getTableCurrentMonthYear(),
                            preferencesService,
                        )
                }

                updateDebtsTableView()
            }

        debtsListYearFilterComboBox
            .valueProperty()
            .addListener { _, _, newYear ->
                if (newYear != null && debtsListMonthFilterComboBox.value != null) {
                    invoiceMonth.text =
                        UIUtils.formatShortMonthYear(
                            getTableCurrentMonthYear(),
                            preferencesService,
                        )
                }

                updateDebtsTableView()
            }

        debtSearchField
            .textProperty()
            .addListener { _, _, _ -> updateDebtsTableView() }
    }

    private fun configureTableView() {
        val idColumn =
            createCreditCardPaymentIdTableColumn(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_DEBT_ID,
                ),
            )

        val descriptionColumn =
            TableColumn<CreditCardPayment, String>(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_DESCRIPTION,
                ),
            )
        descriptionColumn.setCellValueFactory { param ->
            SimpleStringProperty(param.value.creditCardDebt.description)
        }

        val amountColumn =
            TableColumn<CreditCardPayment, String>(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_AMOUNT,
                ),
            )
        amountColumn.setCellValueFactory { param ->
            SimpleObjectProperty(UIUtils.formatCurrency(param.value.amount))
        }

        val installmentColumn =
            createCreditCardPaymentInstallmentTableColumn(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_INSTALLMENT,
                ),
            )

        val crcColumn =
            TableColumn<CreditCardPayment, String>(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_CREDIT_CARD,
                ),
            )
        crcColumn.setCellValueFactory { param ->
            SimpleStringProperty(param.value.creditCardDebt.creditCard.name)
        }

        val categoryColumn =
            TableColumn<CreditCardPayment, String>(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_CATEGORY,
                ),
            )
        categoryColumn.setCellValueFactory { param ->
            SimpleStringProperty(param.value.creditCardDebt.category.name)
        }

        val dateColumn =
            TableColumn<CreditCardPayment, String>(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_INVOICE_DATE,
                ),
            )
        dateColumn.setCellValueFactory { param ->
            SimpleStringProperty(
                UIUtils.formatDateForDisplay(param.value.date, preferencesService),
            )
        }

        val statusColumn =
            TableColumn<CreditCardPayment, String>(
                preferencesService.translate(
                    TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_STATUS,
                ),
            )
        statusColumn.setCellValueFactory { param ->
            val payment = param.value
            val status =
                when {
                    payment.isRefunded() && !payment.isPaid() ->
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DEBTS_LIST_STATUS_ONLY_REFUNDED,
                        )

                    payment.isRefunded() && payment.isPaid() ->
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DEBTS_LIST_STATUS_PAID_BUT_REFUNDED,
                        )

                    !payment.isPaid() ->
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DEBTS_LIST_STATUS_PENDING,
                        )

                    else ->
                        preferencesService.translate(
                            TranslationKeys.CREDIT_CARD_DEBTS_LIST_STATUS_PAID,
                        )
                }
            SimpleStringProperty(status)
        }

        debtsTableView.columns.addAll(
            idColumn,
            descriptionColumn,
            amountColumn,
            installmentColumn,
            crcColumn,
            categoryColumn,
            dateColumn,
            statusColumn,
        )
    }

    private fun createCreditCardPaymentInstallmentTableColumn(header: String): TableColumn<CreditCardPayment, String> {
        val installmentColumn = TableColumn<CreditCardPayment, String>(header)

        installmentColumn.setCellValueFactory { param ->
            SimpleObjectProperty(
                "${param.value.installment}/${param.value.creditCardDebt.installments}",
            )
        }

        installmentColumn.setCellFactory {
            object : TableCell<CreditCardPayment, String>() {
                override fun updateItem(
                    item: String?,
                    empty: Boolean,
                ) {
                    super.updateItem(item, empty)
                    if (item == null || empty) {
                        text = null
                    } else {
                        text = item
                        alignment = Pos.CENTER
                        style = "-fx-padding: 0;"
                    }
                }
            }
        }

        return installmentColumn
    }

    private fun createCreditCardPaymentIdTableColumn(header: String): TableColumn<CreditCardPayment, Int> {
        val idColumn = TableColumn<CreditCardPayment, Int>(header)

        idColumn.setCellValueFactory { param ->
            SimpleObjectProperty(param.value.creditCardDebt.id)
        }

        idColumn.setCellFactory {
            object : TableCell<CreditCardPayment, Int>() {
                override fun updateItem(
                    item: Int?,
                    empty: Boolean,
                ) {
                    super.updateItem(item, empty)
                    if (item == null || empty) {
                        text = null
                    } else {
                        text = item.toString()
                        alignment = Pos.CENTER
                        style = "-fx-padding: 0;"
                    }
                }
            }
        }

        return idColumn
    }
}
