/*
 * Filename: WalletController.kt (original filename: WalletController.java)
 * Created on: September 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.PieChart
import javafx.scene.chart.XYChart
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.moinex.chart.DoughnutChart
import org.moinex.common.extension.isExpense
import org.moinex.common.extension.isIncome
import org.moinex.common.extension.isPending
import org.moinex.model.dto.BalanceDataDTO
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.WalletFullPaneController
import org.moinex.ui.dialog.wallettransaction.AddTransferController
import org.moinex.ui.dialog.wallettransaction.AddWalletController
import org.moinex.ui.dialog.wallettransaction.ArchivedWalletsController
import org.moinex.ui.dialog.wallettransaction.TransferController
import org.moinex.util.Animation
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import org.moinex.util.WindowUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.MessageFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Controller
class WalletController(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val recurringTransactionService: RecurringTransactionService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletPane1: AnchorPane

    @FXML
    private lateinit var walletPane2: AnchorPane

    @FXML
    private lateinit var walletPane3: AnchorPane

    @FXML
    private lateinit var moneyFlowBarChartAnchorPane: AnchorPane

    @FXML
    private lateinit var balanceByWalletTypePieChartAnchorPane: AnchorPane

    @FXML
    private lateinit var totalBalanceByWalletTypeVBox: VBox

    @FXML
    private lateinit var totalBalancePaneInfoVBox: VBox

    @FXML
    private lateinit var walletPrevButton: JFXButton

    @FXML
    private lateinit var walletNextButton: JFXButton

    @FXML
    private lateinit var totalBalancePaneWalletTypeComboBox: ComboBox<String>

    @FXML
    private lateinit var moneyFlowPaneWalletTypeComboBox: ComboBox<String>

    private var moneyFlowBarChart: BarChart<String, Number>? = null
    private var doughnutChartCheckBoxes: List<CheckBox> = emptyList()
    private var transactions: List<WalletTransaction> = emptyList()
    private var walletTypes: List<WalletType> = emptyList()
    private var wallets: List<Wallet> = emptyList()
    private var totalBalanceSelectedMonth: Int = 0
    private var totalBalanceSelectedYear: Int = 0
    private var walletPaneCurrentPage = 0

    companion object {
        private const val ITEMS_PER_PAGE = 3
        private val logger = LoggerFactory.getLogger(WalletController::class.java)
    }

    @FXML
    fun initialize() {
        val now = LocalDate.now()
        totalBalanceSelectedMonth = now.monthValue
        totalBalanceSelectedYear = now.year

        loadAllDataFromDatabase()

        val allWalletsLabel =
            preferencesService.translate(Constants.TranslationKeys.WALLET_ALL_WALLETS)

        totalBalancePaneWalletTypeComboBox.items.addAll(
            walletTypes.map { UIUtils.translateWalletType(it, preferencesService) },
        )

        moneyFlowPaneWalletTypeComboBox.items.addAll(
            walletTypes.map { UIUtils.translateWalletType(it, preferencesService) },
        )

        totalBalancePaneWalletTypeComboBox.items.addFirst(allWalletsLabel)
        totalBalancePaneWalletTypeComboBox.selectionModel.selectFirst()

        moneyFlowPaneWalletTypeComboBox.items.addFirst(allWalletsLabel)
        moneyFlowPaneWalletTypeComboBox.selectionModel.selectFirst()

        createDoughnutChartCheckBoxes()

        updateTotalBalanceView()
        updateDisplayWallets()
        updateMoneyFlowBarChart()
        updateDoughnutChart()

        setButtonsActions()
    }

    @FXML
    private fun handleAddTransfer() {
        WindowUtils.openModalWindow(
            Constants.ADD_TRANSFER_FXML,
            preferencesService.translate(
                Constants.TranslationKeys.WALLET_DIALOG_ADD_TRANSFER_TITLE,
            ),
            springContext,
            { _: AddTransferController -> },
            listOf(
                Runnable {
                    loadWalletsFromDatabase()
                    loadWalletTransactionsFromDatabase()
                    updateDisplayWallets()
                    updateTotalBalanceView()
                    updateDoughnutChart()
                },
            ),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleAddWallet() {
        WindowUtils.openModalWindow(
            Constants.ADD_WALLET_FXML,
            preferencesService.translate(
                Constants.TranslationKeys.WALLET_DIALOG_ADD_WALLET_TITLE,
            ),
            springContext,
            { _: AddWalletController -> },
            listOf(
                Runnable {
                    loadWalletsFromDatabase()
                    loadWalletTransactionsFromDatabase()
                    updateDisplayWallets()
                    updateTotalBalanceView()
                    updateDoughnutChart()
                },
            ),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleViewArchivedWallets() {
        WindowUtils.openModalWindow(
            Constants.ARCHIVED_WALLETS_FXML,
            preferencesService.translate(
                Constants.TranslationKeys.WALLET_DIALOG_ARCHIVED_WALLETS_TITLE,
            ),
            springContext,
            { _: ArchivedWalletsController -> },
            listOf(
                Runnable {
                    loadAllDataFromDatabase()
                    updateDisplayWallets()
                    updateTotalBalanceView()
                    updateMoneyFlowBarChart()
                    updateDoughnutChart()
                },
            ),
            preferencesService.getBundle(),
        )
    }

    @FXML
    private fun handleViewTransfers() {
        WindowUtils.openModalWindow(
            Constants.TRANSFERS_FXML,
            preferencesService.translate(
                Constants.TranslationKeys.WALLET_DIALOG_VIEW_TRANSFERS_TITLE,
            ),
            springContext,
            { _: TransferController -> },
            listOf(
                Runnable {
                    loadWalletsFromDatabase()
                    updateDisplayWallets()
                    updateTotalBalanceView()
                    updateDoughnutChart()
                },
            ),
            preferencesService.getBundle(),
        )
    }

    fun updateDisplay() {
        loadAllDataFromDatabase()

        updateTotalBalanceView()
        updateDisplayWallets()
        updateMoneyFlowBarChart()
        updateDoughnutChart()
    }

    private fun setButtonsActions() {
        totalBalancePaneWalletTypeComboBox.setOnAction { updateTotalBalanceView() }
        moneyFlowPaneWalletTypeComboBox.setOnAction { updateMoneyFlowBarChart() }

        walletPrevButton.setOnAction {
            if (walletPaneCurrentPage > 0) {
                walletPaneCurrentPage--
                updateDisplayWallets()
            }
        }

        walletNextButton.setOnAction {
            if (walletPaneCurrentPage < wallets.size / ITEMS_PER_PAGE) {
                walletPaneCurrentPage++
                updateDisplayWallets()
            }
        }
    }

    private fun loadAllDataFromDatabase() {
        loadWalletTransactionsFromDatabase()
        loadWalletTypesFromDatabase()
        loadWalletsFromDatabase()
    }

    private fun loadWalletTransactionsFromDatabase() {
        transactions =
            walletService.getAllWalletTransactionsByMonth(
                YearMonth.of(totalBalanceSelectedYear, totalBalanceSelectedMonth),
            )
    }

    private fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByTransactionCountDesc()
    }

    private fun loadWalletTypesFromDatabase() {
        walletTypes = walletService.getAllWalletTypes().toMutableList()

        val nameToMove = "Others"

        walletTypes
            .find { it.name == nameToMove }
            ?.let { othersType ->
                walletTypes =
                    walletTypes.toMutableList().apply {
                        remove(othersType)
                        add(othersType)
                    }
            }
    }

    private fun updateTotalBalanceView() {
        val selectedIndex = totalBalancePaneWalletTypeComboBox.selectionModel.selectedIndex

        val (walletsCurrentBalance, pendingExpenses, pendingIncomes, totalAmountInWallets) =
            when {
                selectedIndex == 0 -> {
                    logger.info("Selected: {}", selectedIndex)

                    val balance =
                        wallets
                            .filter { it.isMaster() }
                            .map { it.balance }
                            .fold(BigDecimal.ZERO, BigDecimal::add)

                    val expenses =
                        transactions
                            .filter { it.isExpense() && it.isPending() }
                            .map { it.amount }
                            .fold(BigDecimal.ZERO, BigDecimal::add)

                    val incomes =
                        transactions
                            .filter { it.isIncome() && it.isPending() }
                            .map { it.amount }
                            .fold(BigDecimal.ZERO, BigDecimal::add)

                    val count =
                        wallets
                            .filter { it.isMaster() }
                            .distinctBy { it.id!! }
                            .count()
                            .toLong()

                    BalanceDataDTO(balance, expenses, incomes, count)
                }

                selectedIndex > 0 && selectedIndex - 1 < walletTypes.size -> {
                    val selectedWalletType = walletTypes[selectedIndex - 1]

                    logger.info("Selected: {}", selectedWalletType.name)

                    val balance =
                        wallets
                            .filter { it.isMaster() }
                            .filter { it.type.id!! == selectedWalletType.id!! }
                            .map { it.balance }
                            .fold(BigDecimal.ZERO, BigDecimal::add)

                    val expenses =
                        transactions
                            .filter { it.wallet.type.id!! == selectedWalletType.id!! }
                            .filter { it.isExpense() && it.isPending() }
                            .map { it.amount }
                            .fold(BigDecimal.ZERO, BigDecimal::add)

                    val incomes =
                        transactions
                            .filter { it.wallet.type.id!! == selectedWalletType.id!! }
                            .filter { it.isIncome() && it.isPending() }
                            .map { it.amount }
                            .fold(BigDecimal.ZERO, BigDecimal::add)

                    val count =
                        wallets
                            .filter { it.isMaster() }
                            .filter { it.type.id!! == selectedWalletType.id!! }
                            .distinctBy { it.id!! }
                            .count()
                            .toLong()

                    BalanceDataDTO(balance, expenses, incomes, count)
                }

                else -> {
                    logger.warn("Invalid index: {}", selectedIndex)
                    BalanceDataDTO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L)
                }
            }

        val foreseenBalance = walletsCurrentBalance.add(pendingExpenses).subtract(pendingIncomes)

        val totalBalanceValueLabel =
            Label(UIUtils.formatCurrency(walletsCurrentBalance)).apply {
                styleClass.add(Constants.TOTAL_BALANCE_VALUE_LABEL_STYLE)
            }

        val balanceForeseenLabel =
            Label(
                MessageFormat.format(
                    preferencesService.translate(
                        Constants.TranslationKeys.WALLET_TOTAL_BALANCE_FORESEEN,
                    ),
                    UIUtils.formatCurrency(foreseenBalance),
                ),
            ).apply {
                styleClass.add(Constants.TOTAL_BALANCE_FORESEEN_LABEL_STYLE)
            }

        val totalWalletsLabel =
            Label(
                MessageFormat.format(
                    preferencesService.translate(
                        Constants.TranslationKeys.WALLET_TOTAL_BALANCE_CORRESPONDS_TO,
                    ),
                    totalAmountInWallets,
                ),
            ).apply {
                styleClass.add(Constants.WALLET_TOTAL_BALANCE_WALLETS_LABEL_STYLE)
            }

        totalBalancePaneInfoVBox.children.setAll(
            totalBalanceValueLabel,
            balanceForeseenLabel,
            totalWalletsLabel,
        )
    }

    private fun updateDisplayWallets() {
        walletPane1.children.clear()
        walletPane2.children.clear()
        walletPane3.children.clear()

        val start = walletPaneCurrentPage * ITEMS_PER_PAGE
        val end = minOf(start + ITEMS_PER_PAGE, wallets.size)

        for (i in start until end) {
            val wallet = wallets[i]

            runCatching {
                val loader =
                    FXMLLoader(
                        javaClass.getResource(Constants.WALLET_FULL_PANE_FXML),
                        preferencesService.getBundle(),
                    )
                loader.setControllerFactory { springContext.getBean(it) }
                val newContent = loader.load<Parent>()

                newContent.stylesheets.add(
                    javaClass.getResource(Constants.COMMON_STYLE_SHEET)!!.toExternalForm(),
                )

                val walletFullPaneController = loader.getController<WalletFullPaneController>()
                walletFullPaneController.updateWalletPane(wallet)

                AnchorPane.setTopAnchor(newContent, 0.0)
                AnchorPane.setBottomAnchor(newContent, 0.0)
                AnchorPane.setLeftAnchor(newContent, 0.0)
                AnchorPane.setRightAnchor(newContent, 0.0)

                when (i % ITEMS_PER_PAGE) {
                    0 -> walletPane1.children.add(newContent)
                    1 -> walletPane2.children.add(newContent)
                    2 -> walletPane3.children.add(newContent)
                    else -> logger.warn("Invalid index: {}", i)
                }
            }.onFailure { e ->
                logger.error(
                    "Error loading wallet full pane FXML: '{}' for {}:",
                    Constants.WALLET_FULL_PANE_FXML,
                    wallet,
                    e,
                )
            }
        }

        walletPrevButton.isDisable = walletPaneCurrentPage == 0
        walletNextButton.isDisable = end >= wallets.size
    }

    private fun updateDoughnutChart() {
        val pieChartData = FXCollections.observableArrayList<PieChart.Data>()
        var totalBalance = BigDecimal.ZERO

        doughnutChartCheckBoxes
            .filter { it.isSelected }
            .forEach { checkBox ->
                checkBox.styleClass.add(Constants.WALLET_CHECK_BOX_STYLE)

                val walletTypeName = checkBox.text
                walletTypes
                    .find { UIUtils.translateWalletType(it, preferencesService) == walletTypeName }
                    ?.let { walletType ->
                        val totalBalanceGroup =
                            wallets
                                .filter { it.isMaster() }
                                .filter { it.type.id!! == walletType.id!! }
                                .map { it.balance }
                                .fold(BigDecimal.ZERO, BigDecimal::add)

                        totalBalance = totalBalance.add(totalBalanceGroup)

                        pieChartData.add(
                            PieChart.Data(
                                UIUtils.translateWalletType(walletType, preferencesService),
                                totalBalanceGroup.toDouble(),
                            ),
                        )
                    }
            }

        val doughnutChart =
            DoughnutChart(pieChartData).apply {
                setI18nService(preferencesService)
                labelsVisible = false
            }

        doughnutChart.data.forEach { data ->
            val node = data.node
            val value = BigDecimal(data.pieValue)
            val percentage =
                if (totalBalance > BigDecimal.ZERO) {
                    value
                        .divide(totalBalance, 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal(100))
                } else {
                    BigDecimal.ZERO
                }

            val tooltipText =
                "${data.name}\n${UIUtils.formatCurrency(value)} (${
                    UIUtils.formatPercentage(
                        percentage,
                        preferencesService,
                    )
                })"

            UIUtils.addTooltipToNode(node, tooltipText)
        }

        UIUtils.applyDefaultChartStyle(doughnutChart)

        balanceByWalletTypePieChartAnchorPane.children.removeIf { it is DoughnutChart }
        balanceByWalletTypePieChartAnchorPane.children.add(doughnutChart)

        AnchorPane.setTopAnchor(doughnutChart, 0.0)
        AnchorPane.setBottomAnchor(doughnutChart, 0.0)
        AnchorPane.setLeftAnchor(doughnutChart, 0.0)
        AnchorPane.setRightAnchor(doughnutChart, 0.0)
    }

    private fun updateMoneyFlowBarChart() {
        createMoneyFlowBarChart()

        val monthlyExpenses = linkedMapOf<String, Double>()
        val monthlyIncomes = linkedMapOf<String, Double>()

        val maxMonth = LocalDateTime.now().plusMonths(Constants.XYBAR_CHART_FUTURE_MONTHS.toLong())
        val formatter = UIUtils.getShortMonthYearFormatter(preferencesService.locale)
        val totalMonths = Constants.XYBAR_CHART_MONTHS + Constants.XYBAR_CHART_FUTURE_MONTHS

        val selectedIndex = moneyFlowPaneWalletTypeComboBox.selectionModel.selectedIndex

        for (i in 0 until totalMonths) {
            val date = maxMonth.minusMonths((totalMonths - i - 1).toLong())
            val month = date.monthValue
            val year = date.year
            val yearMonth = YearMonth.of(year, month)

            val transactionsList =
                walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(yearMonth)

            val futureTransactions =
                recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(
                    yearMonth,
                    yearMonth,
                )

            val crcPayments = creditCardService.getPaymentsByMonth(yearMonth)

            val allTransactions = transactionsList + futureTransactions

            logger.info(
                "Found {} ({} future) transactions for {}/{}",
                allTransactions.size,
                futureTransactions.size,
                month,
                year,
            )

            val (totalExpenses, totalIncomes) =
                when {
                    selectedIndex == 0 -> {
                        val expenses =
                            allTransactions
                                .filter { it.isExpense() }
                                .map { it.amount }
                                .fold(BigDecimal.ZERO, BigDecimal::add)

                        val crcPaidPayments =
                            creditCardService.getTotalEffectivePaidPaymentsByMonth(yearMonth)

                        val crcPendingPayments =
                            creditCardService.getTotalPendingPaymentsByMonth(yearMonth)

                        val totalExp = expenses.add(crcPaidPayments).add(crcPendingPayments)

                        val incomes =
                            allTransactions
                                .filter { it.isIncome() }
                                .map { it.amount }
                                .fold(BigDecimal.ZERO, BigDecimal::add)

                        totalExp to incomes
                    }

                    selectedIndex > 0 && selectedIndex - 1 < walletTypes.size -> {
                        val selectedWalletType = walletTypes[selectedIndex - 1]

                        val expenses =
                            allTransactions
                                .filter { it.wallet.type.id!! == selectedWalletType.id!! }
                                .filter { it.isExpense() }
                                .map { it.amount }
                                .fold(BigDecimal.ZERO, BigDecimal::add)

                        val crcPaymentsFiltered =
                            crcPayments
                                .filter { crcPayments ->
                                    when {
                                        crcPayments.isPaid() -> crcPayments.wallet!!.type.id!! == selectedWalletType.id!!

                                        crcPayments.hasDefaultBillingWallet() && !crcPayments.isRefunded() ->
                                            crcPayments.getDefaultBillingWallet()!!.type.id!! ==
                                                selectedWalletType.id!!

                                        else -> false
                                    }
                                }.map { it.amount }
                                .fold(BigDecimal.ZERO, BigDecimal::add)

                        val totalExp = expenses.add(crcPaymentsFiltered)

                        val incomes =
                            allTransactions
                                .filter { it.wallet.type.id!! == selectedWalletType.id!! }
                                .filter { it.isIncome() }
                                .map { it.amount }
                                .fold(BigDecimal.ZERO, BigDecimal::add)

                        totalExp to incomes
                    }

                    else -> {
                        logger.warn("Invalid index: {}", selectedIndex)
                        BigDecimal.ZERO to BigDecimal.ZERO
                    }
                }

            monthlyExpenses[date.format(formatter)] = totalExpenses.toDouble()
            monthlyIncomes[date.format(formatter)] = totalIncomes.toDouble()
        }

        val expensesSeries =
            XYChart.Series<String, Number>().apply {
                name = preferencesService.translate(Constants.TranslationKeys.TRANSACTION_TYPE_EXPENSES)
            }

        val incomesSeries =
            XYChart.Series<String, Number>().apply {
                name = preferencesService.translate(Constants.TranslationKeys.TRANSACTION_TYPE_INCOMES)
            }

        var maxValue = 0.0

        monthlyExpenses.forEach { (month, expenseValue) ->
            val incomeValue = monthlyIncomes.getOrDefault(month, 0.0)
            expensesSeries.data.add(XYChart.Data(month, 0.0))
            incomesSeries.data.add(XYChart.Data(month, 0.0))
            maxValue = maxOf(maxValue, maxOf(expenseValue, incomeValue))
        }

        (moneyFlowBarChart!!.yAxis as? NumberAxis)?.let { numberAxis ->
            Animation.setDynamicYAxisBounds(numberAxis, maxValue)
            numberAxis.tickLabelFormatter =
                object : StringConverter<Number>() {
                    override fun toString(value: Number): String = UIUtils.formatCurrency(value)

                    override fun fromString(string: String): Number = 0
                }
        }

        moneyFlowBarChart!!.verticalGridLinesVisible = false
        moneyFlowBarChart!!.data.addAll(expensesSeries, incomesSeries)

        expensesSeries.data.forEachIndexed { i, expenseData ->
            val incomeData = incomesSeries.data[i]
            val targetExpenseValue = monthlyExpenses[expenseData.xValue]
            val targetIncomeValue = monthlyIncomes.getOrDefault(expenseData.xValue, 0.0)

            UIUtils.addTooltipToXYChartNode(expenseData.node, UIUtils.formatCurrency(targetExpenseValue))
            UIUtils.addTooltipToXYChartNode(incomeData.node, UIUtils.formatCurrency(targetIncomeValue))

            Animation.xyChartAnimation(expenseData, targetExpenseValue)
            Animation.xyChartAnimation(incomeData, targetIncomeValue)
        }
    }

    private fun createDoughnutChartCheckBoxes() {
        balanceByWalletTypePieChartAnchorPane.children.clear()
        totalBalanceByWalletTypeVBox.children.clear()

        doughnutChartCheckBoxes =
            walletTypes.map { wt ->
                CheckBox(UIUtils.translateWalletType(wt, preferencesService)).apply {
                    styleClass.add(Constants.WALLET_CHECK_BOX_STYLE)
                    isSelected = true
                    selectedProperty().addListener { _, _, _ -> updateDoughnutChart() }
                }
            }

        totalBalanceByWalletTypeVBox.children.addAll(doughnutChartCheckBoxes)
    }

    private fun createMoneyFlowBarChart() {
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()

        moneyFlowBarChart = BarChart(xAxis, yAxis)

        moneyFlowBarChartAnchorPane.children.clear()
        moneyFlowBarChartAnchorPane.children.add(moneyFlowBarChart!!)

        AnchorPane.setTopAnchor(moneyFlowBarChart!!, 0.0)
        AnchorPane.setBottomAnchor(moneyFlowBarChart!!, 0.0)
        AnchorPane.setLeftAnchor(moneyFlowBarChart!!, 0.0)
        AnchorPane.setRightAnchor(moneyFlowBarChart!!, 0.0)
    }
}
