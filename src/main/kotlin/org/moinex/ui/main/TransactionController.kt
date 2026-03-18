/*
 * Filename: TransactionController.kt (original filename: TransactionController.java)
 * Created on: October 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

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
import javafx.scene.control.DatePicker
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.util.StringConverter
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.extension.atEndOfDay
import org.moinex.common.extension.isBeforeOrEqual
import org.moinex.common.extension.isConfirmed
import org.moinex.common.extension.isExpense
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.common.util.AnimationUtils
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.ResumePaneController
import org.moinex.ui.dialog.ManageCategoryController
import org.moinex.ui.dialog.wallettransaction.AddExpenseController
import org.moinex.ui.dialog.wallettransaction.AddIncomeController
import org.moinex.ui.dialog.wallettransaction.EditTransactionController
import org.moinex.ui.dialog.wallettransaction.RecurringTransactionController
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
class TransactionController(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val categoryService: CategoryService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var monthYearResumeView: AnchorPane

    @FXML
    private lateinit var yearResumeView: AnchorPane

    @FXML
    private lateinit var monthYearResumeYearComboBox: ComboBox<Year>

    @FXML
    private lateinit var monthYearResumeMonthComboBox: ComboBox<Month>

    @FXML
    private lateinit var yearResumeComboBox: ComboBox<Year>

    @FXML
    private lateinit var moneyFlowComboBox: ComboBox<WalletTransactionType>

    @FXML
    private lateinit var transactionsTypeComboBox: ComboBox<WalletTransactionType?>

    @FXML
    private lateinit var transactionsEndDatePicker: DatePicker

    @FXML
    private lateinit var transactionsStartDatePicker: DatePicker

    @FXML
    private lateinit var transactionsTableView: TableView<WalletTransaction>

    @FXML
    private lateinit var moneyFlowView: AnchorPane

    @FXML
    private lateinit var transactionsSearchField: TextField

    companion object {
        private val logger = LoggerFactory.getLogger(TransactionController::class.java)
        private const val DAYS_BEFORE_OFFSET = 30
    }

    @FXML
    fun initialize() {
        configureTableView()

        populateMonthYearResumeComboBoxes()
        populateYearComboBox()
        populateTransactionTypeComboBox()

        UIUtils.setDatePickerFormat(transactionsStartDatePicker, preferencesService)
        UIUtils.setDatePickerFormat(transactionsEndDatePicker, preferencesService)

        val currentDate = LocalDateTime.now()

        monthYearResumeYearComboBox.value = Year.of(currentDate.year)
        monthYearResumeMonthComboBox.value = currentDate.month
        yearResumeComboBox.value = Year.of(currentDate.year)
        moneyFlowComboBox.value = WalletTransactionType.EXPENSE
        transactionsTypeComboBox.value = null

        val startDate = currentDate.minusDays(DAYS_BEFORE_OFFSET.toLong())
        val lastDayOfMonth =
            currentDate.withDayOfMonth(
                currentDate.month.length(currentDate.toLocalDate().isLeapYear),
            )

        transactionsStartDatePicker.value = startDate.toLocalDate()
        transactionsEndDatePicker.value = lastDayOfMonth.toLocalDate()

        updateMonthYearResume()
        updateYearResume()
        updateMoneyFlow()
        updateTransactionTableView()

        monthYearResumeYearComboBox.setOnAction { updateMonthYearResume() }
        monthYearResumeMonthComboBox.setOnAction { updateMonthYearResume() }
        yearResumeComboBox.setOnAction { updateYearResume() }
        moneyFlowComboBox.setOnAction { updateMoneyFlow() }
        transactionsTypeComboBox.setOnAction { updateTransactionTableView() }
        transactionsStartDatePicker.setOnAction { updateTransactionTableView() }
        transactionsEndDatePicker.setOnAction { updateTransactionTableView() }
        transactionsSearchField.textProperty().addListener { _, _, _ -> updateTransactionTableView() }
    }

    @FXML
    private fun handleAddIncome() {
        WindowUtils.openModalWindow(
            Constants.ADD_INCOME_FXML,
            preferencesService.translate(
                TranslationKeys.TRANSACTION_DIALOG_ADD_INCOME_TITLE,
            ),
            springContext,
            { _: AddIncomeController -> },
            listOf(
                Runnable {
                    updateMonthYearResume()
                    updateYearResume()
                    updateTransactionTableView()
                    updateMoneyFlow()
                },
            ),
        )
    }

    @FXML
    private fun handleAddExpense() {
        WindowUtils.openModalWindow(
            Constants.ADD_EXPENSE_FXML,
            preferencesService.translate(
                TranslationKeys.TRANSACTION_DIALOG_ADD_EXPENSE_TITLE,
            ),
            springContext,
            { _: AddExpenseController -> },
            listOf(
                Runnable {
                    updateMonthYearResume()
                    updateYearResume()
                    updateTransactionTableView()
                    updateMoneyFlow()
                },
            ),
        )
    }

    @FXML
    private fun handleEditTransaction() {
        val selectedTransaction = transactionsTableView.selectionModel.selectedItem

        if (selectedTransaction == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.TRANSACTION_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.TRANSACTION_DIALOG_NO_SELECTION_EDIT_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_TRANSACTION_FXML,
            preferencesService.translate(
                TranslationKeys.TRANSACTION_DIALOG_EDIT_TRANSACTION_TITLE,
            ),
            springContext,
            { controller: EditTransactionController ->
                controller.setTransaction(selectedTransaction)
            },
            listOf(
                Runnable {
                    updateMonthYearResume()
                    updateYearResume()
                    updateTransactionTableView()
                    updateMoneyFlow()
                },
            ),
        )
    }

    @FXML
    private fun handleDeleteTransaction() {
        val selectedTransaction = transactionsTableView.selectionModel.selectedItem

        if (selectedTransaction == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.TRANSACTION_DIALOG_NO_SELECTION_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.TRANSACTION_DIALOG_NO_SELECTION_DELETE_MESSAGE,
                ),
            )
            return
        }

        val message =
            buildString {
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_DESCRIPTION,
                        ),
                        selectedTransaction.description,
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_AMOUNT,
                        ),
                        UIUtils.formatCurrency(selectedTransaction.amount),
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_REGISTER_DATE,
                        ),
                        UIUtils.formatDateTimeForDisplay(
                            selectedTransaction.date,
                            preferencesService,
                        ),
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_STATUS,
                        ),
                        UIUtils.translateTransactionStatus(
                            selectedTransaction.status,
                            preferencesService,
                        ),
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_WALLET,
                        ),
                        selectedTransaction.wallet.name,
                    ),
                )
                append("\n")
                append(
                    MessageFormat.format(
                        preferencesService.translate(
                            TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_WALLET_BALANCE,
                        ),
                        UIUtils.formatCurrency(selectedTransaction.wallet.balance),
                    ),
                )
                append("\n")
                append(
                    preferencesService.translate(
                        TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_WALLET_BALANCE_AFTER_TRANSACTION,
                    ),
                )
                append(" ")

                if (selectedTransaction.isConfirmed()) {
                    val balanceAfter =
                        when {
                            selectedTransaction.isExpense() -> selectedTransaction.wallet.balance.add(selectedTransaction.amount)
                            else -> selectedTransaction.wallet.balance.subtract(selectedTransaction.amount)
                        }
                    append(UIUtils.formatCurrency(balanceAfter))
                } else {
                    append(UIUtils.formatCurrency(selectedTransaction.wallet.balance))
                }
                append("\n")
            }

        if (
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(
                    TranslationKeys.TRANSACTION_DIALOG_CONFIRMATION_DELETE_TITLE,
                ),
                message,
                preferencesService.getBundle(),
            )
        ) {
            walletService.deleteWalletTransaction(selectedTransaction.id!!)

            updateMonthYearResume()
            updateYearResume()
            updateTransactionTableView()
            updateMoneyFlow()
        }
    }

    @FXML
    private fun handleRecurringTransactions() {
        WindowUtils.openModalWindow(
            Constants.RECURRING_TRANSACTIONS_FXML,
            preferencesService.translate(
                TranslationKeys.TRANSACTION_DIALOG_PERIODIC_TRANSACTION_TITLE,
            ),
            springContext,
            { _: RecurringTransactionController -> },
            listOf(
                Runnable {
                    updateMonthYearResume()
                    updateYearResume()
                    updateTransactionTableView()
                    updateMoneyFlow()
                },
            ),
        )
    }

    @FXML
    private fun handleManageCategories() {
        WindowUtils.openModalWindow(
            Constants.MANAGE_CATEGORY_FXML,
            preferencesService.translate(
                TranslationKeys.TRANSACTION_DIALOG_MANAGE_CATEGORIES_TITLE,
            ),
            springContext,
            { _: ManageCategoryController -> },
            listOf(
                Runnable {
                    updateTransactionTableView()
                    updateMoneyFlow()
                },
            ),
        )
    }

    private fun updateTransactionTableView() {
        val similarTextOrId = transactionsSearchField.text.lowercase()
        val selectedWalletTransactionType = transactionsTypeComboBox.value

        val startDate = transactionsStartDatePicker.value.atStartOfDay()
        val endDate = transactionsEndDatePicker.value.atEndOfDay()

        transactionsTableView.items.clear()

        val transactions =
            walletService
                .getAllNonArchivedWalletTransactionsBetweenDates(startDate, endDate)
                .filter { transaction ->
                    selectedWalletTransactionType == null ||
                        transaction.type == selectedWalletTransactionType
                }

        val filteredTransactions =
            if (similarTextOrId.isEmpty()) {
                transactions
            } else {
                transactions.filter { t ->
                    val description = t.description?.lowercase()
                    val id = t.id.toString()
                    val category = t.category.name.lowercase()
                    val wallet = t.wallet.name.lowercase()
                    val amount = t.amount.toString()
                    val type = t.type.toString().lowercase()
                    val status = t.status.toString().lowercase()

                    description?.contains(similarTextOrId) ?: true ||
                        id.contains(similarTextOrId) ||
                        category.contains(similarTextOrId) ||
                        wallet.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        status.contains(similarTextOrId)
                }
            }

        transactionsTableView.items.addAll(filteredTransactions)
        transactionsTableView.refresh()
    }

    private fun updateMoneyFlow() {
        val selectedWalletTransactionType = moneyFlowComboBox.value

        val categoryAxis = CategoryAxis()
        val numberAxis = NumberAxis()
        val moneyFlowStackedBarChart = StackedBarChart(categoryAxis, numberAxis)

        moneyFlowStackedBarChart.verticalGridLinesVisible = false
        moneyFlowView.children.setAll(moneyFlowStackedBarChart)
        moneyFlowStackedBarChart.setAnchorPaneConstraints()

        moneyFlowStackedBarChart.data.clear()

        val currentDate = LocalDateTime.now()
        val formatter = UIUtils.getShortMonthYearFormatter(preferencesService.locale)

        val categories = categoryService.getNonArchivedCategoriesOrderedByName()
        val monthlyTotals = linkedMapOf<YearMonth, MutableMap<Category, Double>>()

        (0 until Constants.XYBAR_CHART_MONTHS).forEach { i ->
            val date = currentDate.minusMonths((Constants.XYBAR_CHART_MONTHS - i - 1).toLong())
            val yearMonth = YearMonth.of(date.year, date.monthValue)

            val transactions =
                walletService.getAllNonArchivedConfirmedWalletTransactionsByMonth(yearMonth)

            val creditCardPayments =
                when {
                    selectedWalletTransactionType == WalletTransactionType.EXPENSE -> creditCardService.getAllPaidPaymentsByMonth(yearMonth)
                    else -> emptyList()
                }

            categories.forEach { category ->
                val totalWalletTransaction =
                    transactions
                        .filter { it.type == selectedWalletTransactionType }
                        .filter { it.category.id!! == category.id!! }
                        .map { it.amount }
                        .fold(BigDecimal.ZERO, BigDecimal::add)

                val totalCreditCardPayment =
                    creditCardPayments
                        .filter { it.creditCardDebt.category.id!! == category.id!! }
                        .map { it.amount }
                        .fold(BigDecimal.ZERO, BigDecimal::add)

                val total = totalWalletTransaction.add(totalCreditCardPayment)

                if (total > BigDecimal.ZERO) {
                    monthlyTotals.getOrPut(yearMonth) { linkedMapOf() }[category] = total.toDouble()
                }
            }
        }

        categories.forEach { category ->
            val series = XYChart.Series<String, Number>()
            series.name = category.name

            monthlyTotals.keys.forEach { yearMonth ->
                val total =
                    monthlyTotals
                        .getOrDefault(yearMonth, linkedMapOf())
                        .getOrDefault(category, 0.0)

                series.data.add(XYChart.Data(yearMonth.format(formatter), total))
            }

            if (series.data.any { (it.yValue as Double) > 0 }) {
                moneyFlowStackedBarChart.data.add(series)
            }
        }

        val maxTotal =
            monthlyTotals.values.maxOfOrNull { it.values.sum() } ?: 0.0

        AnimationUtils.setDynamicYAxisBounds(numberAxis, maxTotal)

        numberAxis.tickLabelFormatter =
            object : StringConverter<Number>() {
                override fun toString(value: Number): String = UIUtils.formatCurrency(value)

                override fun fromString(string: String): Number = 0
            }

        UIUtils.applyDefaultChartStyle(moneyFlowStackedBarChart)

        moneyFlowStackedBarChart.data.forEach { series ->
            series.data.forEach { data ->
                val yearMonth = YearMonth.parse(data.xValue, formatter)
                val monthTotal =
                    monthlyTotals
                        .getOrDefault(yearMonth, linkedMapOf())
                        .values
                        .sum()

                val value = data.yValue as Double
                val percentage = if (monthTotal > 0) (value / monthTotal) * 100 else 0.0

                UIUtils.addTooltipToXYChartNode(
                    data.node,
                    "${series.name}: ${UIUtils.formatCurrency(value)} " +
                        "(${UIUtils.formatPercentage(percentage, preferencesService)})\n" +
                        "Total: ${UIUtils.formatCurrency(monthTotal)}",
                )

                AnimationUtils.stackedXYChartAnimation(listOf(data), listOf(value))
            }
        }
    }

    private fun updateYearResume() {
        val selectedYear = yearResumeComboBox.value

        runCatching {
            val loader =
                FXMLLoader(
                    javaClass.getResource(Constants.RESUME_PANE_FXML),
                    preferencesService.getBundle(),
                )
            loader.setControllerFactory { springContext.getBean(it) }
            val newContent = loader.load<Parent>()

            newContent.stylesheets.add(
                javaClass.getResource(Constants.COMMON_STYLE_SHEET)!!.toExternalForm(),
            )

            val resumePaneController = loader.getController<ResumePaneController>()
            resumePaneController.updateResumePane(selectedYear.value)

            newContent.setAnchorPaneConstraints(left = 10.0, right = 10.0)

            yearResumeView.children.setAll(newContent)
        }.onFailure { e ->
            logger.error(
                "Error loading resume pane FXML: '{}' for year: {}",
                Constants.RESUME_PANE_FXML,
                selectedYear.value,
                e,
            )
            logger.error("Failed to update year resume. Error: '{}'", e.message)
            e.cause?.let {
                logger.error("Root cause: {}", it.message, it)
            }
        }
    }

    private fun updateMonthYearResume() {
        val selectedYearMonth =
            YearMonth.of(
                monthYearResumeYearComboBox.value.value,
                monthYearResumeMonthComboBox.value.value,
            )

        runCatching {
            val loader =
                FXMLLoader(
                    javaClass.getResource(Constants.RESUME_PANE_FXML),
                    preferencesService.getBundle(),
                )
            loader.setControllerFactory { springContext.getBean(it) }
            val newContent = loader.load<Parent>()

            newContent.stylesheets.add(
                javaClass.getResource(Constants.COMMON_STYLE_SHEET)!!.toExternalForm(),
            )

            val resumePaneController = loader.getController<ResumePaneController>()
            resumePaneController.updateResumePane(
                selectedYearMonth.monthValue,
                selectedYearMonth.year,
            )

            newContent.setAnchorPaneConstraints(left = 10.0, right = 10.0)

            monthYearResumeView.children.setAll(newContent)
        }.onFailure { e ->
            logger.error(
                "Error loading resume pane FXML: '{}' for month/year: {}/{}",
                Constants.RESUME_PANE_FXML,
                selectedYearMonth.monthValue,
                selectedYearMonth.year,
                e,
            )
            logger.error("Failed to update month/year resume. Error: '{}'", e.message)
            e.cause?.let {
                logger.error("Root cause: {}", it.message, it)
            }
        }
    }

    private fun populateYearComboBox() {
        val oldestWalletTransaction = walletService.getOldestWalletTransactionDate()
        val oldestCreditCard = creditCardService.getEarliestPaymentDate()

        val oldest =
            when {
                oldestCreditCard.isBefore(oldestWalletTransaction) -> oldestCreditCard
                else -> oldestWalletTransaction
            }

        val youngest = LocalDate.now().plusYears(Constants.YEAR_RESUME_FUTURE_YEARS.toLong())

        val startYear = Year.from(oldest)
        var currentYear = Year.from(youngest)

        val years = mutableListOf<Year>()
        while (startYear.isBeforeOrEqual(currentYear)) {
            years.add(currentYear)
            currentYear = currentYear.minusYears(1)
        }

        yearResumeComboBox.items = FXCollections.observableArrayList(years)

        val formatter = UIUtils.getYearFormatter(preferencesService.locale)
        yearResumeComboBox.converter =
            object : StringConverter<Year>() {
                override fun toString(year: Year?): String = year?.format(formatter) ?: ""

                override fun fromString(string: String): Year = Year.parse(string, formatter)
            }
    }

    private fun populateTransactionTypeComboBox() {
        val walletTransactionTypes =
            FXCollections.observableArrayList(WalletTransactionType.entries)

        moneyFlowComboBox.items = walletTransactionTypes

        val walletTransactionTypesWithNull =
            FXCollections.observableArrayList(
                WalletTransactionType.entries,
            )
        walletTransactionTypesWithNull.addFirst(null)

        transactionsTypeComboBox.items = walletTransactionTypesWithNull

        moneyFlowComboBox.converter =
            object : StringConverter<WalletTransactionType>() {
                override fun toString(walletTransactionType: WalletTransactionType?): String =
                    when (walletTransactionType) {
                        null -> ""
                        WalletTransactionType.EXPENSE ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_TYPE_EXPENSES,
                            )

                        WalletTransactionType.INCOME ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_TYPE_INCOMES,
                            )
                    }

                override fun fromString(string: String): WalletTransactionType? = null
            }

        transactionsTypeComboBox.converter =
            object : StringConverter<WalletTransactionType?>() {
                override fun toString(walletTransactionType: WalletTransactionType?): String =
                    when (walletTransactionType) {
                        null ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_FILTER_ALL,
                            )

                        WalletTransactionType.EXPENSE ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_TYPE_EXPENSES,
                            )

                        WalletTransactionType.INCOME ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_TYPE_INCOMES,
                            )
                    }

                override fun fromString(string: String): WalletTransactionType? = null
            }
    }

    private fun populateMonthYearResumeComboBoxes() {
        val oldestWalletTransaction = walletService.getOldestWalletTransactionDate()
        val oldestCreditCard = creditCardService.getEarliestPaymentDate()

        val oldest =
            when {
                oldestCreditCard.isBefore(oldestWalletTransaction) -> oldestCreditCard
                else -> oldestWalletTransaction
            }

        val future = LocalDate.now().plusMonths(Constants.MONTH_RESUME_FUTURE_MONTHS.toLong())

        val startMonth = YearMonth.from(oldest)
        var currentMonth = YearMonth.from(future)

        val months = mutableListOf<YearMonth>()
        while (startMonth.isBeforeOrEqual(currentMonth)) {
            months.add(currentMonth)
            currentMonth = currentMonth.minusMonths(1)
        }

        val monthYearList = FXCollections.observableArrayList(months)

        val years =
            FXCollections.observableArrayList(
                monthYearList
                    .map { it.year }
                    .distinct()
                    .sortedDescending()
                    .map { Year.of(it) },
            )

        monthYearResumeYearComboBox.items = years
        monthYearResumeYearComboBox.value = years.first()

        val uniqueMonths =
            FXCollections.observableArrayList(
                monthYearList
                    .map { it.month }
                    .distinct()
                    .sortedBy { it.value },
            )

        monthYearResumeMonthComboBox.items = uniqueMonths
        monthYearResumeMonthComboBox.value = uniqueMonths.first()

        monthYearResumeMonthComboBox.converter =
            object : StringConverter<Month>() {
                override fun toString(month: Month?): String = month?.let { UIUtils.getMonthDisplayName(it, preferencesService) } ?: ""

                override fun fromString(string: String): Month = Month.valueOf(string.uppercase())
            }
    }

    private fun configureTableView() {
        val idColumn = createIdColumn()
        val categoryColumn = createCategoryColumn()
        val typeColumn = createTypeColumn()
        val statusColumn = createStatusColumn()
        val dateColumn = createDateColumn()
        val amountColumn = createAmountColumn()
        val descriptionColumn = createDescriptionColumn()
        val walletNameColumn = createWalletNameColumn()

        transactionsTableView.columns.addAll(
            idColumn,
            descriptionColumn,
            amountColumn,
            walletNameColumn,
            dateColumn,
            typeColumn,
            categoryColumn,
            statusColumn,
        )
    }

    private fun createIdColumn(): TableColumn<WalletTransaction, Int> =
        TableColumn<WalletTransaction, Int>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_ID,
            ),
        ).apply {
            setCellValueFactory { SimpleObjectProperty(it.value.id) }
            setCellFactory {
                object : TableCell<WalletTransaction, Int>() {
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
        }

    private fun createCategoryColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_CATEGORY,
            ),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.category.name) }
        }

    private fun createTypeColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_TYPE,
            ),
        ).apply {
            setCellValueFactory { param ->
                val translatedType =
                    when (param.value.type) {
                        WalletTransactionType.EXPENSE ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_TYPE_EXPENSES,
                            )

                        WalletTransactionType.INCOME ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_TYPE_INCOMES,
                            )
                    }
                SimpleStringProperty(translatedType)
            }
        }

    private fun createStatusColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_STATUS,
            ),
        ).apply {
            setCellValueFactory { param ->
                val translatedStatus =
                    when (param.value.status) {
                        WalletTransactionStatus.PENDING ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_STATUS_PENDING,
                            )

                        WalletTransactionStatus.CONFIRMED ->
                            preferencesService.translate(
                                TranslationKeys.TRANSACTION_STATUS_CONFIRMED,
                            )
                    }
                SimpleStringProperty(translatedStatus)
            }
        }

    private fun createDateColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_DATE,
            ),
        ).apply {
            setCellValueFactory {
                SimpleStringProperty(
                    UIUtils.formatDateTimeForDisplay(it.value.date, preferencesService),
                )
            }
        }

    private fun createAmountColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_AMOUNT,
            ),
        ).apply {
            setCellValueFactory {
                SimpleObjectProperty(UIUtils.formatCurrency(it.value.amount))
            }
        }

    private fun createDescriptionColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_DESCRIPTION,
            ),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.description) }
        }

    private fun createWalletNameColumn(): TableColumn<WalletTransaction, String> =
        TableColumn<WalletTransaction, String>(
            preferencesService.translate(
                TranslationKeys.TRANSACTION_TRANSACTION_LIST_HEADER_WALLET,
            ),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.wallet.name) }
        }
}
