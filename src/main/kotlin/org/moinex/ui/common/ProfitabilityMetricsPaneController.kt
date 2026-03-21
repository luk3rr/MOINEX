/*
 * Filename: ProfitabilityMetricsPaneController.kt (original filename: ProfitabilityMetricsPaneController.java)
 * Created on: February 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.moinex.common.constant.Styles
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.ProfitabilityMetricsDTO
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class ProfitabilityMetricsPaneController {
    @FXML
    private lateinit var totalInvestedVariable: Label

    @FXML
    private lateinit var totalInvestedFixed: Label

    @FXML
    private lateinit var totalInvestedTotal: Label

    @FXML
    private lateinit var currentValueVariable: Label

    @FXML
    private lateinit var currentValueFixed: Label

    @FXML
    private lateinit var currentValueTotal: Label

    @FXML
    private lateinit var profitLossVariable: Label

    @FXML
    private lateinit var profitLossFixed: Label

    @FXML
    private lateinit var profitLossTotal: Label

    @FXML
    private lateinit var returnPercentageVariable: Label

    @FXML
    private lateinit var returnPercentageFixed: Label

    @FXML
    private lateinit var returnPercentageTotal: Label

    @FXML
    private lateinit var totalDividendsVariable: Label

    @FXML
    private lateinit var totalDividendsFixed: Label

    @FXML
    private lateinit var totalDividendsTotal: Label

    @FXML
    private lateinit var dividendYieldVariable: Label

    @FXML
    private lateinit var dividendYieldFixed: Label

    @FXML
    private lateinit var dividendYieldTotal: Label

    companion object {
        private const val DASH_PLACEHOLDER = "-"
        private const val POSITIVE_SIGN = "+ "
        private const val NEGATIVE_SIGN = "- "
    }

    fun setMetrics(
        variableMetrics: ProfitabilityMetricsDTO,
        fixedMetrics: ProfitabilityMetricsDTO,
        totalMetrics: ProfitabilityMetricsDTO,
    ) {
        setTotalInvested(
            variableMetrics.totalInvested,
            fixedMetrics.totalInvested,
            totalMetrics.totalInvested,
        )
        setCurrentValue(
            variableMetrics.currentValue,
            fixedMetrics.currentValue,
            totalMetrics.currentValue,
        )
        setProfitLoss(
            variableMetrics.profitLoss,
            fixedMetrics.profitLoss,
            totalMetrics.profitLoss,
        )
        setReturnPercentage(
            variableMetrics.returnPercentage,
            fixedMetrics.returnPercentage,
            totalMetrics.returnPercentage,
        )
        setTotalDividends(variableMetrics.totalDividends, totalMetrics.totalDividends)
        setDividendYield(variableMetrics.dividendYield, totalMetrics.dividendYield)
    }

    private fun setTotalInvested(
        variable: BigDecimal,
        fixed: BigDecimal,
        total: BigDecimal,
    ) {
        totalInvestedVariable.text = UIUtils.formatCurrency(variable)
        totalInvestedFixed.text = UIUtils.formatCurrency(fixed)
        totalInvestedTotal.text = UIUtils.formatCurrency(total)
    }

    private fun setCurrentValue(
        variable: BigDecimal,
        fixed: BigDecimal,
        total: BigDecimal,
    ) {
        currentValueVariable.text = UIUtils.formatCurrency(variable)
        currentValueFixed.text = UIUtils.formatCurrency(fixed)
        currentValueTotal.text = UIUtils.formatCurrency(total)
    }

    private fun setProfitLoss(
        variable: BigDecimal,
        fixed: BigDecimal,
        total: BigDecimal,
    ) {
        listOf(
            profitLossVariable to variable,
            profitLossFixed to fixed,
            profitLossTotal to total,
        ).forEach { (label, value) ->
            label.text = formatValueWithSign(value, isPercentage = false)
            addColorStyleClass(label, value)
        }
    }

    private fun setReturnPercentage(
        variable: BigDecimal,
        fixed: BigDecimal,
        total: BigDecimal,
    ) {
        listOf(
            returnPercentageVariable to variable,
            returnPercentageFixed to fixed,
            returnPercentageTotal to total,
        ).forEach { (label, value) ->
            label.text = formatValueWithSign(value, isPercentage = true)
            addColorStyleClass(label, value)
        }
    }

    private fun setTotalDividends(
        variable: BigDecimal,
        total: BigDecimal,
    ) {
        totalDividendsVariable.text = UIUtils.formatCurrency(variable)
        totalDividendsFixed.apply {
            text = DASH_PLACEHOLDER
            styleClass.apply {
                clear()
                add(Styles.INFO_LABEL_NEUTRAL_STYLE)
            }
        }
        totalDividendsTotal.text = UIUtils.formatCurrency(total)
    }

    private fun setDividendYield(
        variable: BigDecimal,
        total: BigDecimal,
    ) {
        dividendYieldVariable.text = UIUtils.formatPercentage(variable)
        dividendYieldFixed.apply {
            text = DASH_PLACEHOLDER
            styleClass.apply {
                clear()
                add(Styles.INFO_LABEL_NEUTRAL_STYLE)
            }
        }
        dividendYieldTotal.text = UIUtils.formatPercentage(total)
    }

    private fun formatValueWithSign(
        value: BigDecimal,
        isPercentage: Boolean,
    ): String {
        val sign =
            when {
                value > BigDecimal.ZERO -> POSITIVE_SIGN
                value < BigDecimal.ZERO -> NEGATIVE_SIGN
                else -> ""
            }

        val formattedValue =
            if (isPercentage) {
                UIUtils.formatPercentage(value.abs())
            } else {
                UIUtils.formatCurrency(value.abs())
            }

        return sign + formattedValue
    }

    private fun getColorStyleClass(
        value: BigDecimal,
        dynamicColor: Boolean,
    ): String =
        when {
            !dynamicColor -> Styles.INFO_LABEL_NEUTRAL_STYLE
            value < BigDecimal.ZERO -> Styles.INFO_LABEL_RED_STYLE
            value > BigDecimal.ZERO -> Styles.INFO_LABEL_GREEN_STYLE
            else -> Styles.INFO_LABEL_NEUTRAL_STYLE
        }

    private fun addColorStyleClass(
        label: Label,
        value: BigDecimal,
        dynamicColor: Boolean = true,
    ) {
        val colorClass = getColorStyleClass(value, dynamicColor)
        label.styleClass.apply {
            removeAll(
                Styles.INFO_LABEL_RED_STYLE,
                Styles.INFO_LABEL_GREEN_STYLE,
                Styles.INFO_LABEL_NEUTRAL_STYLE,
            )
            add(colorClass)
        }
    }
}
