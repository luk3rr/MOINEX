/*
 * Filename: AnnualSummaryController.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.VBox
import javafx.util.Callback
import javafx.util.StringConverter
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.common.util.AnimationUtils
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.AnnualSummaryDTO
import org.moinex.model.dto.CategoryBreakdownDTO
import org.moinex.model.dto.FinancialInsightDTO
import org.moinex.model.dto.MonthlyFlowDTO
import org.moinex.model.dto.TopLineItemDTO
import org.moinex.model.enums.FinancialInsightSeverity
import org.moinex.model.enums.FinancialInsightType
import org.moinex.model.enums.SpendAccountingMode
import org.moinex.service.PreferencesService
import org.moinex.service.summary.AnnualSummaryService
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate
import java.time.YearMonth

@Controller
class AnnualSummaryController(
    private val annualSummaryService: AnnualSummaryService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var startDatePicker: DatePicker

    @FXML
    private lateinit var endDatePicker: DatePicker

    @FXML
    private lateinit var modeComboBox: ComboBox<String>

    @FXML
    private lateinit var totalIncomeLabel: Label

    @FXML
    private lateinit var totalExpenseLabel: Label

    @FXML
    private lateinit var netBalanceLabel: Label

    @FXML
    private lateinit var savingsRateLabel: Label

    @FXML
    private lateinit var chartTitleLabel: Label

    @FXML
    private lateinit var chartPrevButton: JFXButton

    @FXML
    private lateinit var chartNextButton: JFXButton

    @FXML
    private lateinit var monthlyFlowView: AnchorPane

    @FXML
    private lateinit var savingsRateChartView: AnchorPane

    @FXML
    private lateinit var expenseCategoryTable: TableView<CategoryBreakdownDTO>

    @FXML
    private lateinit var incomeCategoryTable: TableView<CategoryBreakdownDTO>

    @FXML
    private lateinit var topExpensesTable: TableView<TopLineItemDTO>

    @FXML
    private lateinit var insightsBox: VBox

    private var currentChartIndex = 0
    private val chartPages: List<Pair<String, () -> AnchorPane>> by lazy {
        listOf(
            TranslationKeys.ANNUAL_SUMMARY_MONTHLY_FLOW to { monthlyFlowView },
            TranslationKeys.ANNUAL_SUMMARY_SAVINGS_RATE_TREND to { savingsRateChartView },
        )
    }

    @FXML
    fun initialize() {
        endDatePicker.value = LocalDate.now()
        startDatePicker.value = LocalDate.now().minusMonths(11).withDayOfMonth(1)

        modeComboBox.items =
            FXCollections.observableArrayList(
                preferencesService.translate(TranslationKeys.ANNUAL_SUMMARY_MODE_ACCRUAL),
                preferencesService.translate(TranslationKeys.ANNUAL_SUMMARY_MODE_CASH_FLOW),
            )
        modeComboBox.selectionModel.select(1)
        UIUtils.addTooltipToNode(modeComboBox, translate(TranslationKeys.ANNUAL_SUMMARY_MODE_TOOLTIP))

        setupCategoryTable(expenseCategoryTable)
        setupCategoryTable(incomeCategoryTable)
        setupTopExpensesTable()

        startDatePicker.valueProperty().addListener { _, _, _ -> updateView() }
        endDatePicker.valueProperty().addListener { _, _, _ -> updateView() }
        modeComboBox.selectionModel.selectedIndexProperty().addListener { _, _, _ -> updateView() }

        chartPrevButton.setOnAction { showChart(currentChartIndex - 1) }
        chartNextButton.setOnAction { showChart(currentChartIndex + 1) }
        showChart(currentChartIndex)

        updateView()
    }

    private fun showChart(index: Int) {
        val pageCount = chartPages.size
        currentChartIndex = index.coerceIn(0, pageCount - 1)

        chartPages.forEachIndexed { i, (titleKey, view) ->
            val isSelected = i == currentChartIndex
            view().isVisible = isSelected
            view().isManaged = isSelected
            if (isSelected) chartTitleLabel.text = translate(titleKey)
        }

        chartPrevButton.isDisable = currentChartIndex == 0
        chartNextButton.isDisable = currentChartIndex >= pageCount - 1
    }

    private fun updateView() {
        val start = startDatePicker.value ?: return
        val end = endDatePicker.value ?: return
        if (end.isBefore(start)) return

        val mode =
            if (modeComboBox.selectionModel.selectedIndex == 1) {
                SpendAccountingMode.CASH_FLOW
            } else {
                SpendAccountingMode.ACCRUAL
            }

        val summary = annualSummaryService.buildSummary(start, end, mode)

        totalIncomeLabel.text = UIUtils.formatCurrency(summary.totalIncome)
        totalExpenseLabel.text = UIUtils.formatCurrency(summary.totalExpense)
        netBalanceLabel.text = UIUtils.formatCurrency(summary.netBalance)
        savingsRateLabel.text = "${summary.savingsRatePercentage.toPlainString()}%"

        expenseCategoryTable.items = FXCollections.observableArrayList(summary.expenseByCategory)
        incomeCategoryTable.items = FXCollections.observableArrayList(summary.incomeByCategory)
        topExpensesTable.items = FXCollections.observableArrayList(summary.topExpenses)

        updateMonthlyFlowChart(summary)
        updateSavingsRateChart(summary)
        updateInsights(summary.insights)
    }

    private fun setupCategoryTable(table: TableView<CategoryBreakdownDTO>) {
        val nameColumn =
            TableColumn<CategoryBreakdownDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_CATEGORY))
        nameColumn.cellValueFactory = Callback { SimpleStringProperty(it.value.categoryName) }

        val totalColumn =
            TableColumn<CategoryBreakdownDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_TOTAL))
        totalColumn.cellValueFactory = Callback { SimpleStringProperty(UIUtils.formatCurrency(it.value.total)) }

        val percentageColumn =
            TableColumn<CategoryBreakdownDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_PERCENTAGE))
        percentageColumn.cellValueFactory = Callback { SimpleStringProperty("${it.value.percentage.toPlainString()}%") }

        val averageColumn =
            TableColumn<CategoryBreakdownDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_MONTHLY_AVERAGE))
        averageColumn.cellValueFactory =
            Callback { SimpleStringProperty(UIUtils.formatCurrency(it.value.monthlyAverage)) }

        table.columns.setAll(nameColumn, totalColumn, percentageColumn, averageColumn)
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
    }

    private fun setupTopExpensesTable() {
        val dateColumn = TableColumn<TopLineItemDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_DATE))
        dateColumn.cellValueFactory =
            Callback {
                SimpleStringProperty(
                    it.value.date
                        .toLocalDate()
                        .toString(),
                )
            }

        val descriptionColumn =
            TableColumn<TopLineItemDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_DESCRIPTION))
        descriptionColumn.cellValueFactory = Callback { SimpleStringProperty(it.value.description) }

        val categoryColumn =
            TableColumn<TopLineItemDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_CATEGORY))
        categoryColumn.cellValueFactory = Callback { SimpleStringProperty(it.value.categoryName) }

        val amountColumn = TableColumn<TopLineItemDTO, String>(translate(TranslationKeys.ANNUAL_SUMMARY_TABLE_AMOUNT))
        amountColumn.cellValueFactory = Callback { SimpleStringProperty(UIUtils.formatCurrency(it.value.amount)) }

        topExpensesTable.columns.setAll(dateColumn, descriptionColumn, categoryColumn, amountColumn)
        topExpensesTable.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
    }

    private fun updateMonthlyFlowChart(summary: AnnualSummaryDTO) {
        val numberAxis = NumberAxis()
        UIUtils.formatCurrencyYAxis(numberAxis)

        val chart = BarChart(CategoryAxis(), numberAxis)
        chart.title = null
        chart.animated = false
        chart.verticalGridLinesVisible = false

        val incomeSeries = XYChart.Series<String, Number>()
        incomeSeries.name = translate(TranslationKeys.ANNUAL_SUMMARY_TOTAL_INCOME)
        val expenseSeries = XYChart.Series<String, Number>()
        expenseSeries.name = translate(TranslationKeys.ANNUAL_SUMMARY_TOTAL_EXPENSE)

        val flowByLabel = linkedMapOf<String, MonthlyFlowDTO>()
        var maxValue = 0.0

        summary.monthlyFlows.forEach { flow ->
            val label = monthLabel(flow)
            flowByLabel[label] = flow
            val plottedIncome = flow.projectedIncome ?: flow.income
            val plottedExpense = flow.projectedExpense ?: flow.expense
            incomeSeries.data.add(XYChart.Data(label, plottedIncome))
            expenseSeries.data.add(XYChart.Data(label, plottedExpense))
            maxValue = maxOf(maxValue, plottedIncome.toDouble(), plottedExpense.toDouble())
        }

        AnimationUtils.setDynamicYAxisBounds(numberAxis, maxValue)

        chart.data.setAll(expenseSeries, incomeSeries)
        UIUtils.applyDefaultChartStyle(chart, Styles.MONEY_FLOW_CHART_STYLE_CLASS)
        chart.setAnchorPaneConstraints()
        monthlyFlowView.children.setAll(chart)

        chart.data.forEach { series ->
            series.data.forEach { data ->
                val flow = flowByLabel[data.xValue]
                val value = (data.yValue as Number).toDouble()

                if (flow?.isCurrentMonth == true) {
                    data.node.styleClass.add(Styles.CURRENT_MONTH_BAR_STYLE_CLASS)
                }

                UIUtils.addTooltipToXYChartNode(
                    data.node,
                    flowTooltip(series.name, flow, isExpenseSeries = series === expenseSeries),
                )

                AnimationUtils.xyChartAnimation(data, value)
            }
        }
    }

    private fun monthLabel(flow: MonthlyFlowDTO): String = UIUtils.formatShortMonthYear(flow.period)

    private fun flowTooltip(
        seriesName: String,
        flow: MonthlyFlowDTO?,
        isExpenseSeries: Boolean,
    ): String {
        if (flow == null) return seriesName

        val realizedValue = if (isExpenseSeries) flow.expense else flow.income
        val projectedValue = if (isExpenseSeries) flow.projectedExpense else flow.projectedIncome
        val netLabel = translate(TranslationKeys.ANNUAL_SUMMARY_NET_BALANCE)

        if (!flow.isCurrentMonth || projectedValue == null) {
            return "$seriesName: ${UIUtils.formatCurrency(
                realizedValue,
            )}\n$netLabel: ${UIUtils.formatCurrency(flow.net)}"
        }

        val projectedLabel = translate(TranslationKeys.ANNUAL_SUMMARY_PROJECTED)
        val realizedLabel = translate(TranslationKeys.ANNUAL_SUMMARY_REALIZED)
        return "$seriesName ($projectedLabel): ${UIUtils.formatCurrency(projectedValue)}\n" +
            "$seriesName ($realizedLabel): ${UIUtils.formatCurrency(realizedValue)}\n" +
            "$netLabel ($projectedLabel): ${UIUtils.formatCurrency(flow.projectedNet ?: flow.net)}"
    }

    private fun updateSavingsRateChart(summary: AnnualSummaryDTO) {
        val numberAxis = NumberAxis()
        numberAxis.isForceZeroInRange = false
        numberAxis.tickLabelFormatter =
            object : StringConverter<Number>() {
                override fun toString(value: Number?): String = value?.let { "${it.toDouble().toInt()}%" } ?: ""

                override fun fromString(string: String?): Number = 0
            }

        val chart = LineChart(CategoryAxis(), numberAxis)
        chart.title = null
        chart.animated = false
        chart.createSymbols = true
        chart.verticalGridLinesVisible = false

        val rateSeries = XYChart.Series<String, Number>()
        rateSeries.name = translate(TranslationKeys.ANNUAL_SUMMARY_SAVINGS_RATE_TREND_SERIES)
        val targetSeries = XYChart.Series<String, Number>()
        targetSeries.name = translate(TranslationKeys.ANNUAL_SUMMARY_SAVINGS_RATE_TARGET)
        val projectedSeries = XYChart.Series<String, Number>()
        projectedSeries.name = translate(TranslationKeys.ANNUAL_SUMMARY_PROJECTED)

        val target = preferencesService.savingsRateTarget
        val flowByLabel = linkedMapOf<String, MonthlyFlowDTO>()

        summary.monthlyFlows.forEach { flow ->
            val label = monthLabel(flow)
            flowByLabel[label] = flow
            rateSeries.data.add(XYChart.Data(label, flow.savingsRatePercentage))
            targetSeries.data.add(XYChart.Data(label, target))

            flow.projectedSavingsRatePercentage?.let {
                projectedSeries.data.add(XYChart.Data(label, it))
            }
        }

        val seriesToShow = mutableListOf(rateSeries, targetSeries)
        if (projectedSeries.data.isNotEmpty()) seriesToShow.add(projectedSeries)
        chart.data.setAll(seriesToShow)

        UIUtils.applyDefaultChartStyle(chart, Styles.SAVINGS_RATE_CHART_STYLE_CLASS)
        chart.setAnchorPaneConstraints()
        savingsRateChartView.children.setAll(chart)

        targetSeries.data.forEach { data -> data.node.styleClass.add(Styles.HIDDEN_CHART_SYMBOL_STYLE_CLASS) }

        val periodLabel = translate(TranslationKeys.ANNUAL_SUMMARY_SAVINGS_RATE_TREND_TOOLTIP_PERIOD)
        val incomeLabel = translate(TranslationKeys.ANNUAL_SUMMARY_TOTAL_INCOME)
        val savedLabel = translate(TranslationKeys.ANNUAL_SUMMARY_SAVINGS_RATE_TREND_TOOLTIP_SAVED)
        val rateLabel = rateSeries.name
        val projectedLabel = projectedSeries.name

        rateSeries.data.forEach { data ->
            val flow = flowByLabel[data.xValue] ?: return@forEach

            if (flow.isCurrentMonth) {
                data.node.styleClass.add(Styles.CURRENT_MONTH_POINT_STYLE_CLASS)
            }

            UIUtils.addTooltipToXYChartNode(
                data.node,
                "$periodLabel: ${UIUtils.formatFullMonthYear(flow.period)}\n" +
                    "$incomeLabel: ${UIUtils.formatCurrency(flow.income)}\n" +
                    "$savedLabel: ${UIUtils.formatCurrency(flow.net)}\n" +
                    "$rateLabel: ${flow.savingsRatePercentage.toPlainString()}%",
            )
        }

        projectedSeries.data.forEach { data ->
            val flow = flowByLabel[data.xValue] ?: return@forEach
            val projectedIncome = flow.projectedIncome ?: return@forEach
            val projectedNet = flow.projectedNet ?: return@forEach
            val projectedRate = flow.projectedSavingsRatePercentage ?: return@forEach

            UIUtils.addTooltipToXYChartNode(
                data.node,
                "$periodLabel: ${UIUtils.formatFullMonthYear(flow.period)}\n" +
                    "$incomeLabel ($projectedLabel): ${UIUtils.formatCurrency(projectedIncome)}\n" +
                    "$savedLabel ($projectedLabel): ${UIUtils.formatCurrency(projectedNet)}\n" +
                    "$rateLabel ($projectedLabel): ${projectedRate.toPlainString()}%",
            )
        }
    }

    private fun updateInsights(insights: List<FinancialInsightDTO>) {
        insightsBox.children.clear()
        insights.forEach { insight ->
            val label = Label(formatInsight(insight))
            label.isWrapText = true
            label.styleClass.add("insight")
            label.styleClass.add(severityStyle(insight.severity))
            insightsBox.children.add(label)
        }
    }

    private fun formatInsight(insight: FinancialInsightDTO): String {
        val args =
            insight.messageArgs
                .map { arg ->
                    when (arg) {
                        is YearMonth -> UIUtils.formatShortMonthYear(arg)
                        is BigDecimal ->
                            if (insight.type == FinancialInsightType.SPENDING_SPIKE) {
                                UIUtils.formatCurrency(arg)
                            } else {
                                arg.toPlainString()
                            }
                        else -> arg.toString()
                    }
                }.toTypedArray()
        return MessageFormat.format(translate(insight.messageKey), *args)
    }

    private fun severityStyle(severity: FinancialInsightSeverity): String =
        when (severity) {
            FinancialInsightSeverity.POSITIVE -> "insight-positive"
            FinancialInsightSeverity.WARNING -> "insight-warning"
            FinancialInsightSeverity.NEUTRAL -> "insight-neutral"
        }

    private fun translate(key: String): String = preferencesService.translate(key)
}
