package org.moinex.ui.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import lombok.NoArgsConstructor;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the reusable budget group pane component.
 * This controller manages the display of a single budget group's status.
 */
@Controller
@Scope("prototype") // Each instance of this controller is unique
@NoArgsConstructor
public class BudgetGroupPaneController {

    @FXML private Label groupNameLabel;
    @FXML private Label targetPercentageLabel;
    @FXML private Label targetAmountLabel;
    @FXML private Label spentAmountLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusPrefixLabel;
    @FXML private Label statusAmountLabel;
    @FXML private Label progressPercentageLabel;

    /**
     * Populates the pane with data from a budget group and its current status.
     *
     * @param group       The BudgetGroup to display.
     * @param spentAmount The total amount spent in this group for the period.
     * @param baseIncome  The total base income of the financial plan.
     */
    public void setData(BudgetGroup group, BigDecimal spentAmount, BigDecimal baseIncome) {
        BigDecimal targetAmount =
                baseIncome
                        .multiply(group.getTargetPercentage())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal progress = BigDecimal.ZERO;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            progress =
                    spentAmount
                            .divide(targetAmount, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100"));
        }

        groupNameLabel.setText(group.getName());
        targetPercentageLabel.setText(UIUtils.formatPercentage(group.getTargetPercentage()));
        targetAmountLabel.setText(UIUtils.formatCurrency(targetAmount));
        spentAmountLabel.setText(UIUtils.formatCurrency(spentAmount));
        progressPercentageLabel.setText(UIUtils.formatPercentage(progress));

        progressBar.setProgress(
                progress.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP).doubleValue());

        updateStatus(spentAmount, targetAmount, progress);
    }

    /**
     * Updates the status labels and progress bar color based on the budget progress.
     */
    private void updateStatus(
            BigDecimal spentAmount, BigDecimal targetAmount, BigDecimal progress) {
        statusPrefixLabel
                .getStyleClass()
                .removeAll(
                        Constants.INFO_LABEL_RED_STYLE,
                        Constants.INFO_LABEL_YELLOW_STYLE,
                        Constants.INFO_LABEL_GREEN_STYLE);
        statusAmountLabel
                .getStyleClass()
                .removeAll(
                        Constants.INFO_LABEL_RED_STYLE,
                        Constants.INFO_LABEL_YELLOW_STYLE,
                        Constants.INFO_LABEL_GREEN_STYLE);
        progressBar
                .getStyleClass()
                .removeAll(
                        Constants.PROGRESS_BAR_RED_COLOR_STYLE,
                        Constants.PROGRESS_BAR_YELLOW_COLOR_STYLE,
                        Constants.PROGRESS_BAR_GREEN_COLOR_STYLE);

        BigDecimal remaining = targetAmount.subtract(spentAmount);

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            statusPrefixLabel.setText("Overspent: ");
            statusAmountLabel.setText(UIUtils.formatCurrency(remaining.abs()));
            statusPrefixLabel.getStyleClass().add(Constants.INFO_LABEL_RED_STYLE);
            statusAmountLabel.getStyleClass().add(Constants.INFO_LABEL_RED_STYLE);
            progressBar.getStyleClass().add(Constants.PROGRESS_BAR_RED_COLOR_STYLE);
        } else {
            statusPrefixLabel.setText("Available: ");
            statusAmountLabel.setText(UIUtils.formatCurrency(remaining));

            if (progress.compareTo(new BigDecimal("75")) > 0) {
                statusPrefixLabel.getStyleClass().add(Constants.INFO_LABEL_YELLOW_STYLE);
                statusAmountLabel.getStyleClass().add(Constants.INFO_LABEL_YELLOW_STYLE);
                progressBar.getStyleClass().add(Constants.PROGRESS_BAR_YELLOW_COLOR_STYLE);
            } else {
                statusPrefixLabel.getStyleClass().add(Constants.INFO_LABEL_GREEN_STYLE);
                statusAmountLabel.getStyleClass().add(Constants.INFO_LABEL_GREEN_STYLE);
                progressBar.getStyleClass().add(Constants.PROGRESS_BAR_GREEN_COLOR_STYLE);
            }
        }
    }
}
