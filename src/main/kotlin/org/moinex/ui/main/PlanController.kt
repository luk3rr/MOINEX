/*
 * Filename: PlanController.kt (original filename: PlanController.java)
 * Created on: July 26, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.chart.PieChart
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.util.StringConverter
import org.moinex.chart.ChartFactory
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.constants.TranslationKeys
import org.moinex.model.dto.PlanStatusDTO
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.service.PreferencesService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.ui.common.BudgetGroupPaneController
import org.moinex.ui.dialog.financialplanning.AddPlanController
import org.moinex.ui.dialog.financialplanning.EditPlanController
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import org.moinex.util.WindowUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@Controller
class PlanController(
    private val springContext: ConfigurableApplicationContext,
    private val financialPlanningService: FinancialPlanningService,
    private val preferencesService: PreferencesService,
    private val chartFactory: ChartFactory,
) {
    @FXML
    private lateinit var periodComboBox: ComboBox<YearMonth>

    @FXML
    private lateinit var baseMonthlyIncome: Label

    @FXML
    private lateinit var pieChartAnchorPane: AnchorPane

    @FXML
    private lateinit var budgetGroupPrevButton: JFXButton

    @FXML
    private lateinit var budgetGroupNextButton: JFXButton

    @FXML
    private lateinit var budgetGroupPane1: AnchorPane

    @FXML
    private lateinit var budgetGroupPane2: AnchorPane

    @FXML
    private lateinit var budgetGroupPane3: AnchorPane

    @FXML
    private lateinit var timelineChartAnchorPane: AnchorPane

    private var currentPlan: FinancialPlan? = null
    private var currentPlanStatus: List<PlanStatusDTO> = emptyList()
    private var currentPage = 0

    companion object {
        private val logger = LoggerFactory.getLogger(PlanController::class.java)
        private const val ITEMS_PER_PAGE = 3
        private const val HISTORICAL_DATA_MONTHS = 12
    }

    @FXML
    fun initialize() {
        populatePeriodComboBox()
        setupListeners()
        updateView()
    }

    @FXML
    private fun handleNewPlan() {
        WindowUtils.openModalWindow(
            Constants.ADD_PLAN_FXML,
            preferencesService.translate(TranslationKeys.PLAN_DIALOG_ADD_PLAN_TITLE),
            springContext,
            { _: AddPlanController -> },
            listOf(Runnable { updateView() }),
        )
    }

    @FXML
    private fun handleEditPlan() {
        if (currentPlan == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(
                    TranslationKeys.PLAN_DIALOG_NO_ACTIVE_PLAN_TITLE,
                ),
                preferencesService.translate(
                    TranslationKeys.PLAN_DIALOG_NO_ACTIVE_PLAN_MESSAGE,
                ),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_PLAN_FXML,
            preferencesService.translate(TranslationKeys.PLAN_DIALOG_EDIT_PLAN_TITLE),
            springContext,
            { controller: EditPlanController -> controller.setPlan(currentPlan) },
            listOf(Runnable { updateView() }),
        )
    }

    private fun populatePeriodComboBox() {
        val current = YearMonth.now()
        val periods = (0 until HISTORICAL_DATA_MONTHS).map { current.minusMonths(it.toLong()) }

        periodComboBox.items = FXCollections.observableArrayList(periods)
        periodComboBox.value = current

        val formatter = UIUtils.getFullMonthYearFormatter(preferencesService.locale)
        periodComboBox.converter =
            object : StringConverter<YearMonth>() {
                override fun toString(yearMonth: YearMonth?): String = yearMonth?.format(formatter) ?: ""

                override fun fromString(string: String): YearMonth = YearMonth.parse(string, formatter)
            }
    }

    private fun setupListeners() {
        periodComboBox.valueProperty().addListener { _, _, newVal ->
            newVal?.let { updateView() }
        }

        budgetGroupPrevButton.setOnAction {
            if (currentPage > 0) {
                currentPage--
                updateBudgetGroupPanes()
            }
        }

        budgetGroupNextButton.setOnAction {
            val maxPages = (currentPlanStatus.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
            if (currentPage < maxPages - 1) {
                currentPage++
                updateBudgetGroupPanes()
            }
        }
    }

    private fun updateView() {
        val selectedPeriod = periodComboBox.value ?: return

        currentPlan = financialPlanningService.getNonArchivedPlan()

        currentPlan?.let {
            currentPlanStatus =
                financialPlanningService
                    .getPlanStatus(currentPlan!!.id!!, selectedPeriod)
                    .sortedByDescending { it.group.targetPercentage }

            baseMonthlyIncome.text = UIUtils.formatCurrency(currentPlan!!.baseIncome)

            updateDoughnutChart()

            currentPage = 0

            updateBudgetGroupPanes()
            updateTimelineChart()
        } ?: logger.warn("No active financial plan found. Please create a new plan.")
    }

    private fun updateDoughnutChart() {
        val pieChartData =
            currentPlanStatus.map { status ->
                PieChart.Data(
                    status.group.name,
                    status.group.targetPercentage.toDouble(),
                )
            }

        val doughnutChart =
            chartFactory.createDoughnutChart(FXCollections.observableArrayList(pieChartData)).apply {
                setShowCenterLabel(false)
                isLegendVisible = true
                labelsVisible = false
            }

        doughnutChart.data.forEach { data ->
            val percentage = BigDecimal.valueOf(data.pieValue)
            val value =
                currentPlan!!.baseIncome.multiply(
                    percentage.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP),
                )

            val tooltipText =
                "${data.name}\n${UIUtils.formatCurrency(value)} (${
                    UIUtils.formatPercentage(
                        percentage,
                        preferencesService,
                    )
                })"

            UIUtils.addTooltipToNode(data.node, tooltipText)
        }

        UIUtils.applyDefaultChartStyle(doughnutChart)

        pieChartAnchorPane.children.setAll(doughnutChart)
        doughnutChart.setAnchorPaneConstraints()
    }

    private fun updateBudgetGroupPanes() {
        val panes = listOf(budgetGroupPane1, budgetGroupPane2, budgetGroupPane3)
        panes.forEach { it.children.clear() }

        val startIndex = currentPage * ITEMS_PER_PAGE

        (0 until ITEMS_PER_PAGE)
            .map { startIndex + it }
            .filter { it < currentPlanStatus.size }
            .forEach { dataIndex ->
                val paneIndex = dataIndex - startIndex
                loadBudgetGroupPane(panes[paneIndex], currentPlanStatus[dataIndex])
            }

        budgetGroupPrevButton.isDisable = currentPage == 0
        val maxPages = (currentPlanStatus.size + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE
        budgetGroupNextButton.isDisable = currentPage >= maxPages - 1
    }

    private fun loadBudgetGroupPane(
        parent: AnchorPane,
        status: PlanStatusDTO,
    ) {
        runCatching {
            val loader =
                FXMLLoader(
                    javaClass.getResource(Constants.BUDGET_GROUP_PANE_FXML),
                    preferencesService.getBundle(),
                )
            loader.setControllerFactory { springContext.getBean(it) }
            val content = loader.load<Parent>()

            val controller = loader.getController<BudgetGroupPaneController>()
            controller.setData(
                status.group,
                status.spentAmount,
                currentPlan!!.baseIncome,
            )

            parent.children.setAll(content)
            content.setAnchorPaneConstraints()
        }.onFailure { e ->
            logger.error(
                "Error loading budget group pane FXML: '{}' for group: '{}'",
                Constants.BUDGET_GROUP_PANE_FXML,
                status.group.name,
                e,
            )
            logger.error(
                "Budget group details - Spent: {}, Base income: {}",
                status.spentAmount,
                currentPlan!!.baseIncome,
            )
            e.cause?.let {
                logger.error("Root cause: {}", it.message, it)
            }
        }
    }

    private fun updateTimelineChart() {
        currentPlan ?: return

        val currentPeriod = periodComboBox.value ?: return

        val startPeriod = currentPeriod.minusMonths(HISTORICAL_DATA_MONTHS.toLong())

        val historicalData =
            financialPlanningService.getHistoricalData(
                currentPlan!!.id!!,
                startPeriod,
                currentPeriod,
            )

        val timelineChart =
            chartFactory.createBudgetGroupTimelineChart().apply {
                setXAxisLabel(
                    this@PlanController.preferencesService.translate(TranslationKeys.PLAN_TIMELINE_X_AXIS),
                )
                setYAxisLabel(
                    this@PlanController.preferencesService.translate(TranslationKeys.PLAN_TIMELINE_Y_AXIS),
                )
                updateData(historicalData)
            }

        UIUtils.applyDefaultChartStyle(timelineChart)

        timelineChartAnchorPane.children.setAll(timelineChart)
        timelineChart.setAnchorPaneConstraints()
    }
}
