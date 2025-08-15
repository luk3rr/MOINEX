package org.moinex.ui.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.util.UIUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("prototype") // Each instance of this controller is unique
@NoArgsConstructor
public class BudgetGroupPreviewController {

    @FXML @Getter private VBox rootPane;

    @FXML private Label groupNameLabel;

    @FXML private Label targetPercentageLabel;

    @FXML private Label targetValueLabel;

    /**
     * Populates the preview pane with data from a BudgetGroup object
     *
     * @param budgetGroup The BudgetGroup to display
     * @param planTotal The total amount of the financial plan, used to calculate the target value
     */
    public void populate(BudgetGroup budgetGroup, BigDecimal planTotal) {
        groupNameLabel.setText(budgetGroup.getName());
        targetPercentageLabel.setText(UIUtils.formatPercentage(budgetGroup.getTargetPercentage()));
        targetValueLabel.setText(
                UIUtils.formatCurrency(
                        budgetGroup
                                .getTargetPercentage()
                                .multiply(
                                        planTotal.divide(
                                                BigDecimal.valueOf(100),
                                                2,
                                                RoundingMode.HALF_UP))));
    }
}
