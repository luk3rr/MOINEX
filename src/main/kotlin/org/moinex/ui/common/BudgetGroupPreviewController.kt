/*
 * Filename: BudgetGroupPreviewController.kt (original filename: BudgetGroupPreviewController.java)
 * Created on: September 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.control.Label
import org.moinex.common.util.UIUtils
import org.moinex.model.financialplanning.BudgetGroup
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Controller for the reusable budget group preview component. This controller manages the display of a
 * single budget group's preview information.
 */
@Controller
@Scope("prototype") // Each instance of this controller is unique
class BudgetGroupPreviewController {
    @FXML
    private lateinit var groupNameLabel: Label

    @FXML
    private lateinit var targetPercentageLabel: Label

    @FXML
    private lateinit var targetValueLabel: Label

    /**
     * Populates the preview pane with data from a BudgetGroup object
     *
     * @param budgetGroup The BudgetGroup to display
     * @param planTotal The total amount of the financial plan, used to calculate the target value
     */
    fun populate(
        budgetGroup: BudgetGroup,
        planTotal: BigDecimal,
    ) {
        groupNameLabel.text = budgetGroup.name
        targetPercentageLabel.text = UIUtils.formatPercentage(budgetGroup.targetPercentage)
        targetValueLabel.text =
            UIUtils.formatCurrency(
                budgetGroup
                    .targetPercentage
                    .multiply(
                        planTotal.divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP,
                        ),
                    ),
            )
    }
}
