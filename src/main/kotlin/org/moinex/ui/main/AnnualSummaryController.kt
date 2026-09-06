/*
 * Filename: AnnualSummaryController.kt
 * Created on: September 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
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
    private lateinit var monthlyFlowView: AnchorPane

    @FXML
    private lateinit var expenseCategoryTable: TableView<CategoryBreakdownDTO>

    @FXML
    private lateinit var incomeCategoryTable: TableView<CategoryBreakdownDTO>

    @FXML
    private lateinit var topExpensesTable: TableView<TopLineItemDTO>

    @FXML
    private lateinit var insightsBox: VBox

    @FXML
    fun initialize() {
        endDatePicker.value = LocalDate.now()
        startDatePicker.value = LocalDate.now().minusMonths(11).withDayOfMonth(1)

        modeComboBox.items =
            FXCollections.observableArrayList(
                preferencesService.translate(TranslationKeys.ANNUAL_SUMMARY_MODE_ACCRUAL),
                preferencesService.translate(TranslationKeys.ANNUAL_SUMMARY_MODE_CASH_FLOW),
            )
        modeComboBox.selectionModel.selectFirst()
        UIUtils.addTooltipToNode(modeComboBox, translate(TranslationKeys.ANNUAL_SUMMARY_MODE_TOOLTIP))

        setupCategoryTable(expenseCategoryTable)
        setupCategoryTable(incomeCategoryTable)
        setupTopExpensesTable()

        startDatePicker.valueProperty().addListener { _, _, _ -> updateView() }
        endDatePicker.valueProperty().addListener { _, _, _ -> updateView() }
        modeComboBox.selectionModel.selectedIndexProperty().addListener { _, _, _ -> updateView() }

        updateView()
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
            val label = UIUtils.formatShortMonthYear(flow.period)
            flowByLabel[label] = flow
            incomeSeries.data.add(XYChart.Data(label, flow.income))
            expenseSeries.data.add(XYChart.Data(label, flow.expense))
            maxValue = maxOf(maxValue, flow.income.toDouble(), flow.expense.toDouble())
        }

        AnimationUtils.setDynamicYAxisBounds(numberAxis, maxValue)

        chart.data.setAll(expenseSeries, incomeSeries)
        UIUtils.applyDefaultChartStyle(chart, Styles.MONEY_FLOW_CHART_STYLE_CLASS)
        chart.setAnchorPaneConstraints()
        monthlyFlowView.children.setAll(chart)

        chart.data.forEach { series ->
            series.data.forEach { data ->
                val value = (data.yValue as Number).toDouble()
                val net =
                    flowByLabel[data.xValue]?.let {
                        "\n${translate(TranslationKeys.ANNUAL_SUMMARY_NET_BALANCE)}: ${UIUtils.formatCurrency(it.net)}"
                    } ?: ""

                UIUtils.addTooltipToXYChartNode(
                    data.node,
                    "${series.name}: ${UIUtils.formatCurrency(value)}$net",
                )

                AnimationUtils.xyChartAnimation(data, value)
            }
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
