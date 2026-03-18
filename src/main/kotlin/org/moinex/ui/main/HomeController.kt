/*
 * Filename: HomeController.kt (original filename: HomeController.java)
 * Created on: September 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.moinex.common.chart.ChartFactory
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.AnimationUtils
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.dto.NetWorthDataPointDTO
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.networth.NetWorthCalculationService
import org.moinex.service.networth.NetWorthService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.ResumePaneController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.YearMonth

@Controller
class HomeController(
    private val walletService: WalletService,
    private val recurringTransactionService: RecurringTransactionService,
    private val creditCardService: CreditCardService,
    private val netWorthService: NetWorthService,
    private val netWorthCalculationService: NetWorthCalculationService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
    private val chartFactory: ChartFactory,
) {
    @FXML
    private lateinit var walletPrevButton: JFXButton

    @FXML
    private lateinit var walletNextButton: JFXButton

    @FXML
    private lateinit var walletView1: AnchorPane

    @FXML
    private lateinit var walletView2: AnchorPane

    @FXML
    private lateinit var creditCardView1: AnchorPane

    @FXML
    private lateinit var creditCardView2: AnchorPane

    @FXML
    private lateinit var monthResumeView: AnchorPane

    @FXML
    private lateinit var graphView: AnchorPane

    @FXML
    private lateinit var graphPrevButton: JFXButton

    @FXML
    private lateinit var graphNextButton: JFXButton

    @FXML
    private lateinit var graphTitle: Label

    @FXML
    private lateinit var creditCardPrevButton: JFXButton

    @FXML
    private lateinit var creditCardNextButton: JFXButton

    @FXML
    private var moneyFlowBarChart: BarChart<String, Number>? = null

    @FXML
    private lateinit var monthResumePaneTitle: Label

    @FXML
    private lateinit var transactionsTableView: TableView<WalletTransaction>

    @FXML
    private lateinit var recalculateNetWorthButton: JFXButton

    @FXML
    private lateinit var recalculateNetWorthButtonIcon: ImageView

    private var wallets: List<Wallet> = emptyList()
    private var creditCards: List<CreditCard> = emptyList()
    private var transactions: List<WalletTransaction> = emptyList()

    private var walletPaneCurrentPage = 0
    private var creditCardPaneCurrentPage = 0
    private var graphPaneCurrentPage = 0

    companion object {
        private val logger = LoggerFactory.getLogger(HomeController::class.java)
    }

    @FXML
    fun initialize() {
        loadWalletsFromDatabase()
        loadCreditCardsFromDatabase()
        loadLastTransactionsFromDatabase()

        logger.debug("Loaded {} wallets from the database", wallets.size)
        logger.debug("Loaded {} credit cards from the database", creditCards.size)

        updateDisplayWallets()
        updateDisplayCreditCards()
        updateDisplayLastTransactions()
        updateMonthResume()
        updateDisplayGraphs()

        setButtonsActions()
    }

    @FXML
    fun handleRecalculateNetWorth() {
        setOffRecalculateButton()

        FxUtils.launchCpuThenUI(
            background = {
                netWorthCalculationService.recalculateAllSnapshots { current, total ->
                    FxUtils.withFxThread {
                        updateRecalculateButtonProgress(current, total)
                    }
                }
            },
            onUI = {
                logger.info("Recalculation completed, updating chart...")
                updateNetWorthLineChart()
                setOnRecalculateButton()
            },
        )
    }

    private fun setOffRecalculateButton() {
        recalculateNetWorthButtonIcon.image =
            Image(javaClass.getResource(Constants.LOADING_GIF)!!.toExternalForm())
        recalculateNetWorthButton.isDisable = true
        recalculateNetWorthButton.text =
            preferencesService.translate(
                TranslationKeys.HOME_RECALCULATE_NET_WORTH_BUTTON_RECALCULATING,
            )
    }

    private fun updateRecalculateButtonProgress(
        current: Int,
        total: Int,
    ) {
        val percentage = (current.toDouble() / total * 100).toInt()
        val baseText =
            preferencesService.translate(
                TranslationKeys.HOME_RECALCULATE_NET_WORTH_BUTTON_RECALCULATING,
            )
        recalculateNetWorthButton.text = "$baseText ($current/$total - $percentage%)"
    }

    private fun setOnRecalculateButton() {
        recalculateNetWorthButton.isDisable = false
        recalculateNetWorthButtonIcon.image =
            Image(javaClass.getResource(Constants.RELOAD_ICON)!!.toExternalForm())
        recalculateNetWorthButton.text =
            preferencesService.translate(TranslationKeys.HOME_RECALCULATE_NET_WORTH_BUTTON)
    }

    private fun setButtonsActions() {
        walletPrevButton.setOnAction {
            if (walletPaneCurrentPage > 0) {
                walletPaneCurrentPage--
                updateDisplayWallets()
            }
        }

        walletNextButton.setOnAction {
            if (walletPaneCurrentPage < wallets.size / Constants.HOME_PANES_ITEMS_PER_PAGE) {
                walletPaneCurrentPage++
                updateDisplayWallets()
            }
        }

        creditCardPrevButton.setOnAction {
            if (creditCardPaneCurrentPage > 0) {
                creditCardPaneCurrentPage--
                updateDisplayCreditCards()
            }
        }

        creditCardNextButton.setOnAction {
            if (creditCardPaneCurrentPage < creditCards.size / Constants.HOME_PANES_ITEMS_PER_PAGE) {
                creditCardPaneCurrentPage++
                updateDisplayCreditCards()
            }
        }

        graphPrevButton.setOnAction {
            if (graphPaneCurrentPage > 0) {
                graphPaneCurrentPage--
                updateDisplayGraphs()
            }
        }

        graphNextButton.setOnAction {
            if (graphPaneCurrentPage < 1) {
                graphPaneCurrentPage++
                updateDisplayGraphs()
            }
        }
    }

    private fun loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName()
    }

    private fun loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName()
    }

    private fun loadLastTransactionsFromDatabase() {
        transactions = walletService.getAllNonArchivedLastWalletTransactions(Constants.HOME_LAST_TRANSACTIONS_SIZE)
    }

    private fun updateDisplayWallets() {
        walletView1.children.clear()
        walletView2.children.clear()

        val start = walletPaneCurrentPage * Constants.HOME_PANES_ITEMS_PER_PAGE
        val end = minOf(start + Constants.HOME_PANES_ITEMS_PER_PAGE, wallets.size)

        for (i in start until end) {
            val wallet = wallets[i]
            val walletHBox = createWalletItemNode(wallet)

            AnchorPane.setTopAnchor(walletHBox, 0.0)
            AnchorPane.setBottomAnchor(walletHBox, 0.0)

            if (i % 2 == 0) {
                walletView1.children.add(walletHBox)
                AnchorPane.setLeftAnchor(walletHBox, 0.0)
                AnchorPane.setRightAnchor(walletHBox, 10.0)
            } else {
                walletView2.children.add(walletHBox)
                AnchorPane.setLeftAnchor(walletHBox, 10.0)
                AnchorPane.setRightAnchor(walletHBox, 0.0)
            }
        }

        walletPrevButton.isDisable = walletPaneCurrentPage == 0
        walletNextButton.isDisable = end >= wallets.size
    }

    private fun updateDisplayCreditCards() {
        creditCardView1.children.clear()
        creditCardView2.children.clear()

        val start = creditCardPaneCurrentPage * Constants.HOME_PANES_ITEMS_PER_PAGE
        val end = minOf(start + Constants.HOME_PANES_ITEMS_PER_PAGE, creditCards.size)

        for (i in start until end) {
            val creditCard = creditCards[i]
            val crcHbox = createCreditCardItemNode(creditCard)

            AnchorPane.setTopAnchor(crcHbox, 0.0)
            AnchorPane.setBottomAnchor(crcHbox, 0.0)

            if (i % 2 == 0) {
                creditCardView1.children.add(crcHbox)
                AnchorPane.setLeftAnchor(crcHbox, 0.0)
                AnchorPane.setRightAnchor(crcHbox, 10.0)
            } else {
                creditCardView2.children.add(crcHbox)
                AnchorPane.setLeftAnchor(crcHbox, 10.0)
                AnchorPane.setRightAnchor(crcHbox, 0.0)
            }
        }

        creditCardPrevButton.isDisable = creditCardPaneCurrentPage == 0
        creditCardNextButton.isDisable = end >= creditCards.size
    }

    private fun updateDisplayLastTransactions() {
        transactionsTableView.columns.clear()

        val transactionColumn =
            TableColumn<WalletTransaction, WalletTransaction>(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.HOME_TRANSACTIONS_TABLE_TITLE),
                    Constants.HOME_LAST_TRANSACTIONS_SIZE,
                ),
            )

        transactionColumn.setCellValueFactory { SimpleObjectProperty(it.value) }

        transactionColumn.setCellFactory {
            object : TableCell<WalletTransaction, WalletTransaction>() {
                override fun updateItem(
                    transaction: WalletTransaction?,
                    empty: Boolean,
                ) {
                    super.updateItem(transaction, empty)
                    graphic =
                        if (empty || transaction == null) {
                            null
                        } else {
                            createTransactionCell(transaction)
                        }
                }
            }
        }

        transactionsTableView.columns.add(transactionColumn)
        transactionsTableView.items = FXCollections.observableArrayList(transactions)
    }

    private fun createTransactionCell(transaction: WalletTransaction): HBox {
        val icon =
            ImageView(
                if (transaction.type == WalletTransactionType.INCOME) {
                    Constants.HOME_INCOME_ICON
                } else {
                    Constants.HOME_EXPENSE_ICON
                },
            ).apply {
                fitHeight = Constants.HOME_LAST_TRANSACTIONS_ICON_SIZE.toDouble()
                fitWidth = Constants.HOME_LAST_TRANSACTIONS_ICON_SIZE.toDouble()
            }

        val descriptionLabel =
            Label(transaction.description).apply {
                minWidth = Constants.HOME_LAST_TRANSACTIONS_DESCRIPTION_LABEL_WIDTH.toDouble()
            }

        val valueLabel =
            Label(UIUtils.formatCurrency(transaction.amount)).apply {
                minWidth = Constants.HOME_LAST_TRANSACTIONS_VALUE_LABEL_WIDTH.toDouble()
            }

        val walletLabel =
            Label(transaction.wallet.name).apply {
                minWidth = Constants.HOME_LAST_TRANSACTIONS_WALLET_LABEL_WIDTH.toDouble()
            }

        val dateLabel =
            Label(UIUtils.formatDateForDisplay(transaction.date, preferencesService)).apply {
                minWidth = Constants.HOME_LAST_TRANSACTIONS_DATE_LABEL_WIDTH.toDouble()
            }

        val transactionStatusLabel =
            Label(UIUtils.translateTransactionStatus(transaction.status, preferencesService)).apply {
                minWidth = Constants.HOME_LAST_TRANSACTIONS_STATUS_LABEL_WIDTH.toDouble()
            }

        val transactionCategoryLabel =
            Label(transaction.category.name).apply {
                minWidth = Constants.HOME_LAST_TRANSACTIONS_CATEGORY_LABEL_WIDTH.toDouble()
            }

        val descriptionValueBox =
            HBox(descriptionLabel, valueLabel).apply {
                alignment = Pos.CENTER_LEFT
            }

        val walletCategoryStatusDateBox =
            HBox(walletLabel, transactionCategoryLabel, transactionStatusLabel, dateLabel).apply {
                alignment = Pos.CENTER_LEFT
            }

        val vbox = VBox(5.0, descriptionValueBox, walletCategoryStatusDateBox)

        return HBox(10.0, icon, vbox).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add(
                if (transaction.type == WalletTransactionType.INCOME) {
                    Constants.HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE
                } else {
                    Constants.HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE
                },
            )
        }
    }

    private fun updateMoneyFlowBarChart() {
        createMoneyFlowBarChart()

        val monthlyExpenses = linkedMapOf<String, Double>()
        val monthlyIncomes = linkedMapOf<String, Double>()

        val maxMonth = LocalDateTime.now().plusMonths(Constants.XYBAR_CHART_FUTURE_MONTHS.toLong())
        val formatter = UIUtils.getShortMonthYearFormatter(preferencesService.locale)
        val totalMonths = Constants.XYBAR_CHART_MONTHS + Constants.XYBAR_CHART_FUTURE_MONTHS

        for (i in 0 until totalMonths) {
            val date = maxMonth.minusMonths((totalMonths - i - 1).toLong())
            val month = date.monthValue
            val year = date.year
            val yearMonth = YearMonth.of(year, month)

            val nonArchivedTransactions =
                walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(yearMonth)

            val futureTransactions =
                recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(
                    yearMonth,
                    yearMonth,
                )

            val allTransactions = nonArchivedTransactions + futureTransactions

            logger.debug(
                "Found {} ({} future) transactions for {}/{}",
                allTransactions.size,
                futureTransactions.size,
                month,
                year,
            )

            val crcPaidPayments = creditCardService.getTotalEffectivePaidPaymentsByMonth(yearMonth)
            val crcPendingPayments = creditCardService.getTotalPendingPaymentsByMonth(yearMonth)

            val totalExpenses =
                allTransactions
                    .filter { it.type == WalletTransactionType.EXPENSE }
                    .map { it.amount }
                    .fold(BigDecimal.ZERO, BigDecimal::add)
                    .add(crcPaidPayments)
                    .add(crcPendingPayments)

            val totalIncomes =
                allTransactions
                    .filter { it.type == WalletTransactionType.INCOME }
                    .map { it.amount }
                    .fold(BigDecimal.ZERO, BigDecimal::add)

            monthlyExpenses[date.format(formatter)] = totalExpenses.toDouble()
            monthlyIncomes[date.format(formatter)] = totalIncomes.toDouble()
        }

        val expensesSeries =
            XYChart.Series<String, Number>().apply {
                name = preferencesService.translate(TranslationKeys.TRANSACTION_TYPE_EXPENSES)
            }

        val incomesSeries =
            XYChart.Series<String, Number>().apply {
                name = preferencesService.translate(TranslationKeys.TRANSACTION_TYPE_INCOMES)
            }

        var maxValue = 0.0

        monthlyExpenses.forEach { (month, expenseValue) ->
            val incomeValue = monthlyIncomes.getOrDefault(month, 0.0)
            expensesSeries.data.add(XYChart.Data(month, 0.0))
            incomesSeries.data.add(XYChart.Data(month, 0.0))
            maxValue = maxOf(maxValue, maxOf(expenseValue, incomeValue))
        }

        (moneyFlowBarChart!!.yAxis as? NumberAxis)?.let { numberAxis ->
            AnimationUtils.setDynamicYAxisBounds(numberAxis, maxValue)
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
            val targetExpenseValue = monthlyExpenses.getOrDefault(expenseData.xValue, 0.0)
            val targetIncomeValue = monthlyIncomes.getOrDefault(expenseData.xValue, 0.0)

            UIUtils.addTooltipToXYChartNode(expenseData.node, UIUtils.formatCurrency(targetExpenseValue))
            UIUtils.addTooltipToXYChartNode(incomeData.node, UIUtils.formatCurrency(targetIncomeValue))

            AnimationUtils.xyChartAnimation(expenseData, targetExpenseValue)
            AnimationUtils.xyChartAnimation(incomeData, targetIncomeValue)
        }
    }

    private fun updateMonthResume() {
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
            val currentDate = LocalDateTime.now()

            monthResumePaneTitle.text =
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.HOME_RESUME_TITLE),
                    UIUtils.formatShortMonthYear(currentDate, preferencesService),
                )

            resumePaneController.updateResumePane(currentDate.monthValue, currentDate.year)

            AnchorPane.setTopAnchor(newContent, 0.0)
            AnchorPane.setBottomAnchor(newContent, 0.0)
            AnchorPane.setLeftAnchor(newContent, 0.0)
            AnchorPane.setRightAnchor(newContent, 0.0)

            monthResumeView.children.clear()
            monthResumeView.children.add(newContent)
        }.onFailure { e ->
            logger.error("Error loading resume pane FXML: '{}'", Constants.RESUME_PANE_FXML, e)
            e.cause?.let {
                logger.error("Root cause: {}", it.message, it)
            }
        }
    }

    private fun createCreditCardItemNode(creditCard: CreditCard): HBox {
        val nameLabel =
            Label(creditCard.name).apply {
                maxWidth = Constants.HOME_ITEM_NODE_NAME_MAX_LENGTH.toDouble()
                textOverrun = OverrunStyle.ELLIPSIS
                styleClass.add(Constants.HOME_CREDIT_CARD_ITEM_NAME_STYLE)
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(
                        TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_NAME,
                    ),
                )
            }

        val crcOperatorLabel =
            Label(creditCard.operator.name).apply {
                styleClass.add(Constants.HOME_CREDIT_CARD_ITEM_OPERATOR_STYLE)
                alignment = Pos.TOP_LEFT
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(
                        TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_OPERATOR,
                    ),
                )
            }

        val availableCredit =
            Label(UIUtils.formatCurrency(creditCardService.getAvailableCredit(creditCard.id!!))).apply {
                styleClass.add(Constants.HOME_CREDIT_CARD_ITEM_BALANCE_STYLE)
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(
                        TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_AVAILABLE_CREDIT,
                    ),
                )
            }

        val digitsLabel =
            Label(UIUtils.formatCreditCardNumber(creditCard.lastFourDigits)).apply {
                styleClass.add(Constants.HOME_CREDIT_CARD_ITEM_DIGITS_STYLE)
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(
                        TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_NUMBER,
                    ),
                )
            }

        val infoVbox =
            VBox(10.0).apply {
                alignment = Pos.CENTER_LEFT
                children.addAll(nameLabel, crcOperatorLabel, availableCredit, digitsLabel)
            }

        val icon =
            ImageView(Constants.CRC_OPERATOR_ICONS_PATH + creditCard.operator.icon).apply {
                fitHeight = Constants.CRC_OPERATOR_ICONS_SIZE.toDouble()
                fitWidth = Constants.CRC_OPERATOR_ICONS_SIZE.toDouble()
            }

        val iconVBox =
            VBox().apply {
                alignment = Pos.CENTER_RIGHT
                children.add(icon)
            }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        return HBox(10.0).apply {
            styleClass.add(Constants.HOME_CREDIT_CARD_ITEM_STYLE)
            children.addAll(infoVbox, spacer, iconVBox)
        }
    }

    private fun createWalletItemNode(wallet: Wallet): HBox {
        val nameLabel =
            Label(wallet.name).apply {
                maxWidth = Constants.HOME_ITEM_NODE_NAME_MAX_LENGTH.toDouble()
                textOverrun = OverrunStyle.ELLIPSIS
                styleClass.add(Constants.HOME_WALLET_ITEM_NAME_STYLE)
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(TranslationKeys.HOME_WALLET_TOOLTIP_WALLET_NAME),
                )
            }

        val walletTypeLabel =
            Label(UIUtils.translateWalletType(wallet.type, preferencesService)).apply {
                styleClass.add(Constants.HOME_WALLET_TYPE_STYLE)
                alignment = Pos.TOP_LEFT
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(TranslationKeys.HOME_WALLET_TOOLTIP_WALLET_TYPE),
                )
            }

        val balanceLabel =
            Label(UIUtils.formatCurrency(wallet.balance)).apply {
                styleClass.add(Constants.HOME_WALLET_ITEM_BALANCE_STYLE)
                UIUtils.addTooltipToNode(
                    this,
                    preferencesService.translate(TranslationKeys.HOME_WALLET_TOOLTIP_WALLET_BALANCE),
                )
            }

        val infoVbox =
            VBox(10.0).apply {
                alignment = Pos.CENTER_LEFT
                children.addAll(nameLabel, walletTypeLabel, balanceLabel)

                if (wallet.isVirtual()) {
                    val virtualWalletLabel =
                        Label(
                            preferencesService.translate(
                                TranslationKeys.HOME_WALLET_VIRTUAL_WALLET,
                            ),
                        ).apply {
                            alignment = Pos.BOTTOM_LEFT
                            styleClass.add(Constants.HOME_VIRTUAL_WALLET_INFO_STYLE)
                            UIUtils.addTooltipToNode(
                                this,
                                UIUtils.getVirtualWalletInfo(wallet, preferencesService),
                            )
                        }
                    children.add(virtualWalletLabel)
                }
            }

        val icon =
            ImageView(Constants.WALLET_TYPE_ICONS_PATH + wallet.type.icon).apply {
                fitHeight = Constants.WALLET_TYPE_ICONS_SIZE.toDouble()
                fitWidth = Constants.WALLET_TYPE_ICONS_SIZE.toDouble()
            }

        val iconVBox =
            VBox().apply {
                alignment = Pos.CENTER_RIGHT
                children.add(icon)
            }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        return HBox(10.0).apply {
            styleClass.add(Constants.HOME_WALLET_ITEM_STYLE)
            children.addAll(infoVbox, spacer, iconVBox)
        }
    }

    private fun createMoneyFlowBarChart() {
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        moneyFlowBarChart = BarChart(xAxis, yAxis)
    }

    private fun updateDisplayGraphs() {
        graphView.children.clear()

        when (graphPaneCurrentPage) {
            0 -> {
                graphTitle.text =
                    preferencesService.translate(TranslationKeys.HOME_MONEY_FLOW_TITLE)
                updateMoneyFlowBarChart()
                graphView.children.add(moneyFlowBarChart!!)
                AnchorPane.setTopAnchor(moneyFlowBarChart!!, 0.0)
                AnchorPane.setBottomAnchor(moneyFlowBarChart!!, 0.0)
                AnchorPane.setLeftAnchor(moneyFlowBarChart!!, 0.0)
                AnchorPane.setRightAnchor(moneyFlowBarChart!!, 0.0)
                recalculateNetWorthButton.isVisible = false
            }
            1 -> {
                graphTitle.text =
                    preferencesService.translate(TranslationKeys.HOME_NET_WORTH_TITLE)
                recalculateNetWorthButton.isVisible = true

                if (netWorthCalculationService.isCalculating) {
                    setOffRecalculateButton()
                } else {
                    setOnRecalculateButton()
                }

                updateNetWorthLineChart()
            }
        }

        graphPrevButton.isDisable = graphPaneCurrentPage == 0
        graphNextButton.isDisable = graphPaneCurrentPage >= 1
    }

    private fun updateNetWorthLineChart() {
        val dataPoints = calculateNetWorthData()

        val netWorthChart =
            chartFactory.createNetWorthLineChart().apply {
                updateData(dataPoints)
            }

        UIUtils.applyDefaultChartStyle(netWorthChart)

        graphView.children.add(netWorthChart)

        AnchorPane.setTopAnchor(netWorthChart, 0.0)
        AnchorPane.setBottomAnchor(netWorthChart, 0.0)
        AnchorPane.setLeftAnchor(netWorthChart, 0.0)
        AnchorPane.setRightAnchor(netWorthChart, 0.0)
    }

    private fun calculateNetWorthData(): List<NetWorthDataPointDTO> {
        val dataPoints = mutableListOf<NetWorthDataPointDTO>()
        val maxMonth = LocalDateTime.now().plusMonths(Constants.PL_CHART_FUTURE_MONTHS.toLong())
        val totalMonths = Constants.PL_CHART_MONTHS + Constants.PL_CHART_FUTURE_MONTHS

        for (i in 0 until totalMonths) {
            val date = maxMonth.minusMonths((totalMonths - i - 1).toLong())
            val period = YearMonth.of(date.year, date.monthValue)

            netWorthService.getSnapshot(period)?.let { snapshot ->
                dataPoints.add(
                    NetWorthDataPointDTO(
                        period,
                        snapshot.assets,
                        snapshot.liabilities,
                        snapshot.netWorth,
                    ),
                )
            }
        }

        return dataPoints
    }
}
