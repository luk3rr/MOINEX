/*
 * Filename: BudgetGroupPaneController.kt (original filename: BudgetGroupPaneController.java)
 * Created on:
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.service.PreferencesService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode

@Controller
@Scope("prototype")
class BudgetGroupPaneController(
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var groupNameLabel: Label

    @FXML
    private lateinit var targetPercentageLabel: Label

    @FXML
    private lateinit var targetAmountLabel: Label

    @FXML
    private lateinit var spentAmountLabel: Label

    @FXML
    private lateinit var progressBar: ProgressBar

    @FXML
    private lateinit var statusPrefixLabel: Label

    @FXML
    private lateinit var statusAmountLabel: Label

    @FXML
    private lateinit var progressPercentageLabel: Label

    companion object {
        const val PROGRESS_BAR_PRECISION = 4
        val PERCENTAGE_DIVISOR = BigDecimal(100)
        val REMAINING_OVERSPENT_THRESHOLD = BigDecimal(0)
        val PROGRESS_WARNING_THRESHOLD = BigDecimal(75)
    }

    /**
     * Populates the pane with data from a budget group and its current status.
     *
     * @param group The BudgetGroup to display.
     * @param spentAmount The total amount spent in this group for the period.
     * @param baseIncome The total base income of the financial plan.
     */
    fun setData(
        group: BudgetGroup,
        spentAmount: BigDecimal,
        baseIncome: BigDecimal,
    ) {
        val targetAmount =
            baseIncome
                .multiply(group.targetPercentage)
                .divide(PERCENTAGE_DIVISOR, 2, RoundingMode.HALF_UP)

        var progress = BigDecimal.ZERO
        if (targetAmount > REMAINING_OVERSPENT_THRESHOLD) {
            progress =
                spentAmount
                    .divide(targetAmount, PROGRESS_BAR_PRECISION, RoundingMode.HALF_UP)
                    .multiply(PERCENTAGE_DIVISOR)
        }

        groupNameLabel.text = group.name
        targetPercentageLabel.text =
            UIUtils.formatPercentage(group.targetPercentage)
        targetAmountLabel.text = UIUtils.formatCurrency(targetAmount)
        spentAmountLabel.text = UIUtils.formatCurrency(spentAmount)
        progressPercentageLabel.text = UIUtils.formatPercentage(progress)

        progressBar.progress =
            progress.divide(PERCENTAGE_DIVISOR, PROGRESS_BAR_PRECISION, RoundingMode.HALF_UP).toDouble()

        updateStatus(spentAmount, targetAmount, progress)
    }

    private fun updateStatus(
        spentAmount: BigDecimal,
        targetAmount: BigDecimal,
        progress: BigDecimal,
    ) {
        statusPrefixLabel.styleClass.removeAll(
            Styles.INFO_LABEL_RED_STYLE,
            Styles.INFO_LABEL_YELLOW_STYLE,
            Styles.INFO_LABEL_GREEN_STYLE,
        )
        statusAmountLabel.styleClass.removeAll(
            Styles.INFO_LABEL_RED_STYLE,
            Styles.INFO_LABEL_YELLOW_STYLE,
            Styles.INFO_LABEL_GREEN_STYLE,
        )
        progressBar.styleClass.removeAll(
            Styles.PROGRESS_BAR_RED_COLOR_STYLE,
            Styles.PROGRESS_BAR_YELLOW_COLOR_STYLE,
            Styles.PROGRESS_BAR_GREEN_COLOR_STYLE,
        )

        val remaining = targetAmount.subtract(spentAmount)

        if (remaining < REMAINING_OVERSPENT_THRESHOLD) {
            statusPrefixLabel.text =
                preferencesService.translate(TranslationKeys.COMMON_BUDGET_GROUP_OVERSPENT)
            statusAmountLabel.text = UIUtils.formatCurrency(remaining.abs())
            statusPrefixLabel.styleClass.add(Styles.INFO_LABEL_RED_STYLE)
            statusAmountLabel.styleClass.add(Styles.INFO_LABEL_RED_STYLE)
            progressBar.styleClass.add(Styles.PROGRESS_BAR_RED_COLOR_STYLE)
        } else {
            statusPrefixLabel.text =
                preferencesService.translate(TranslationKeys.COMMON_BUDGET_GROUP_REMAINING)
            statusAmountLabel.text = UIUtils.formatCurrency(remaining)

            if (progress > PROGRESS_WARNING_THRESHOLD) {
                statusPrefixLabel.styleClass.add(Styles.INFO_LABEL_YELLOW_STYLE)
                statusAmountLabel.styleClass.add(Styles.INFO_LABEL_YELLOW_STYLE)
                progressBar.styleClass.add(Styles.PROGRESS_BAR_YELLOW_COLOR_STYLE)
            } else {
                statusPrefixLabel.styleClass.add(Styles.INFO_LABEL_GREEN_STYLE)
                statusAmountLabel.styleClass.add(Styles.INFO_LABEL_GREEN_STYLE)
                progressBar.styleClass.add(Styles.PROGRESS_BAR_GREEN_COLOR_STYLE)
            }
        }
    }
}
