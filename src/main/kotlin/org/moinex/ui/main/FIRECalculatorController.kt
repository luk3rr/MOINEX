package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.fxml.FXML
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.TextField
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.FIREProjectionResultDTO
import org.moinex.model.financialplanning.FIRECalculatorSettings
import org.moinex.service.PreferencesService
import org.moinex.service.financialplanning.FIRECalculatorService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate

@Controller
class FIRECalculatorController(
    private val fireCalculatorService: FIRECalculatorService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var currentNetWorthField: TextField

    @FXML
    private lateinit var monthlyContributionField: TextField

    @FXML
    private lateinit var annualReturnRateField: TextField

    @FXML
    private lateinit var monthlyExpenseField: TextField

    @FXML
    private lateinit var withdrawalRateField: TextField

    @FXML
    private lateinit var currentAgeField: TextField

    @FXML
    private lateinit var calculateButton: JFXButton

    @FXML
    private lateinit var fireTargetLabel: Label

    @FXML
    private lateinit var timeToFireLabel: Label

    @FXML
    private lateinit var ageAtFireLabel: Label

    @FXML
    private lateinit var noConvergenceLabel: Label

    @FXML
    private lateinit var projectionChart: LineChart<String, Number>

    @FXML
    private lateinit var xAxis: CategoryAxis

    @FXML
    private lateinit var yAxis: NumberAxis

    // Stores year -> (patrimony, fireTarget) for tooltip reapplication after resize
    private var chartTooltipData: Map<String, Pair<BigDecimal, BigDecimal>> = emptyMap()

    companion object {
        private val logger = LoggerFactory.getLogger(FIRECalculatorController::class.java)
    }

    @FXML
    fun initialize() {
        configureChart()
        configureListeners()

        fireCalculatorService.getSettings()?.let { settings ->
            populateFields(settings)
            recalculate(settings)
        }
    }

    @FXML
    fun handleCalculate() {
        val settings = buildSettingsFromFields() ?: return

        runCatching {
            fireCalculatorService.saveSettings(settings)
            recalculate(settings)
        }.onFailure { e ->
            logger.error("Error saving FIRE settings: {}", e.message, e)
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                e.message ?: "Unknown error",
            )
        }
    }

    private fun recalculate(settings: FIRECalculatorSettings) {
        runCatching {
            val result = fireCalculatorService.calculate(settings)
            updateResultCards(result)
            updateChart(result)
        }.onFailure { e ->
            logger.error("Error calculating FIRE projection: {}", e.message, e)
        }
    }

    private fun buildSettingsFromFields(): FIRECalculatorSettings? {
        val fields =
            listOf(
                currentNetWorthField,
                monthlyContributionField,
                annualReturnRateField,
                monthlyExpenseField,
                withdrawalRateField,
                currentAgeField,
            )

        if (fields.any { it.text.isNullOrBlank() }) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.DIALOG_INFO_TITLE),
                preferencesService.translate(TranslationKeys.DIALOG_BUTTON_SAVE) + ": " +
                    "All fields are required.",
            )
            return null
        }

        return runCatching {
            FIRECalculatorSettings(
                currentNetWorth = BigDecimal(currentNetWorthField.text.trim()),
                monthlyContribution = BigDecimal(monthlyContributionField.text.trim()),
                annualReturnRate = BigDecimal(annualReturnRateField.text.trim()),
                monthlyExpense = BigDecimal(monthlyExpenseField.text.trim()),
                withdrawalRate = BigDecimal(withdrawalRateField.text.trim()),
                currentAge = currentAgeField.text.trim().toInt(),
            )
        }.getOrElse { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                e.message ?: "Invalid input",
            )
            null
        }
    }

    private fun populateFields(settings: FIRECalculatorSettings) {
        currentNetWorthField.text = settings.currentNetWorth.toPlainString()
        monthlyContributionField.text = settings.monthlyContribution.toPlainString()
        annualReturnRateField.text = settings.annualReturnRate.toPlainString()
        monthlyExpenseField.text = settings.monthlyExpense.toPlainString()
        withdrawalRateField.text = settings.withdrawalRate.toPlainString()
        currentAgeField.text = settings.currentAge.toString()
    }

    private fun updateResultCards(result: FIREProjectionResultDTO) {
        fireTargetLabel.text = UIUtils.formatCurrency(result.fireTarget)

        val noConvergence = result.monthsToFire == null

        noConvergenceLabel.isVisible = noConvergence
        noConvergenceLabel.isManaged = noConvergence

        if (noConvergence) {
            timeToFireLabel.text = "-"
            ageAtFireLabel.text = "-"
            return
        }

        val months = result.monthsToFire!!
        timeToFireLabel.text =
            if (months == 0) {
                preferencesService.translate(TranslationKeys.FIRE_RESULT_LESS_THAN_ONE_YEAR)
            } else {
                val years = months / Constants.YEAR_MONTHS
                val remainingMonths = months % Constants.YEAR_MONTHS

                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.FIRE_RESULT_YEARS),
                    years,
                    remainingMonths,
                )
            }

        ageAtFireLabel.text = result.ageAtFire?.toString() ?: "-"
    }

    private fun updateChart(result: FIREProjectionResultDTO) {
        projectionChart.data.clear()

        if (result.dataPoints.isEmpty()) return

        val patrimonyLabel = preferencesService.translate(TranslationKeys.FIRE_CHART_SERIES_PATRIMONY)
        val targetLabel = preferencesService.translate(TranslationKeys.FIRE_CHART_SERIES_TARGET)

        val patrimonySeries = XYChart.Series<String, Number>()
        patrimonySeries.name = patrimonyLabel

        val targetSeries = XYChart.Series<String, Number>()
        targetSeries.name = targetLabel

        val today = LocalDate.now()
        val yearlyPoints =
            result.dataPoints
                .filter { (month, _) -> month % Constants.YEAR_MONTHS == 0 || month == result.dataPoints.last().first }
                .associate { (month, value) ->
                    val year = today.plusMonths(month.toLong()).year
                    year.toString() to value
                }

        yearlyPoints.forEach { (year, value) ->
            patrimonySeries.data.add(XYChart.Data(year, value as Number))
            targetSeries.data.add(XYChart.Data(year, result.fireTarget as Number))
        }

        projectionChart.data.addAll(patrimonySeries, targetSeries)

        chartTooltipData = yearlyPoints.mapValues { (_, patrimony) -> patrimony to result.fireTarget }
        addTooltipsToChart()
    }

    private fun addTooltipsToChart() {
        projectionChart.widthProperty().addListener { _, _, _ ->
            FxUtils.launchOnFxThread { reapplyTooltips() }
        }
        projectionChart.heightProperty().addListener { _, _, _ ->
            FxUtils.launchOnFxThread { reapplyTooltips() }
        }
        FxUtils.launchOnFxThread { reapplyTooltips() }
    }

    private fun reapplyTooltips() {
        val patrimonyLabel = preferencesService.translate(TranslationKeys.FIRE_CHART_SERIES_PATRIMONY)
        val targetLabel = preferencesService.translate(TranslationKeys.FIRE_CHART_SERIES_TARGET)

        chartTooltipData.forEach { (year, values) ->
            val (patrimony, target) = values
            val tooltipText =
                "$year\n" +
                    "$patrimonyLabel: ${UIUtils.formatCurrency(patrimony)}\n" +
                    "$targetLabel: ${UIUtils.formatCurrency(target)}"
            UIUtils.addTooltipToAxisLabel(xAxis, year, tooltipText)
        }
    }

    private fun configureChart() {
        UIUtils.formatCurrencyYAxis(yAxis)
        projectionChart.isLegendVisible = true
        projectionChart.animated = false
        projectionChart.createSymbols = false
    }

    private fun configureListeners() {
        listOf(currentNetWorthField, monthlyContributionField, monthlyExpenseField).forEach {
            UIUtils.configureTextFieldListener(it, Constants.MONETARY_VALUE_REGEX)
        }

        listOf(annualReturnRateField, withdrawalRateField).forEach {
            UIUtils.configureTextFieldListener(it, Constants.INTEREST_RATE_REGEX)
        }

        UIUtils.configureTextFieldListener(currentAgeField, Constants.DIGITS_ONLY_REGEX)
    }
}
