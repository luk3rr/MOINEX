/*
 * Filename: ProfitabilityMetricsPaneController.java
 * Created on: February 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.NoArgsConstructor;
import org.moinex.model.dto.ProfitabilityMetricsDTO;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class ProfitabilityMetricsPaneController {

    @FXML private Label totalInvestedVariable;
    @FXML private Label totalInvestedFixed;
    @FXML private Label totalInvestedTotal;

    @FXML private Label currentValueVariable;
    @FXML private Label currentValueFixed;
    @FXML private Label currentValueTotal;

    @FXML private Label profitLossVariable;
    @FXML private Label profitLossFixed;
    @FXML private Label profitLossTotal;

    @FXML private Label returnPercentageVariable;
    @FXML private Label returnPercentageFixed;
    @FXML private Label returnPercentageTotal;

    @FXML private Label totalDividendsVariable;
    @FXML private Label totalDividendsFixed;
    @FXML private Label totalDividendsTotal;

    @FXML private Label dividendYieldVariable;
    @FXML private Label dividendYieldFixed;
    @FXML private Label dividendYieldTotal;

    private I18nService i18nService;

    @Autowired
    public ProfitabilityMetricsPaneController(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    public void setMetrics(
            ProfitabilityMetricsDTO variableMetrics,
            ProfitabilityMetricsDTO fixedMetrics,
            ProfitabilityMetricsDTO totalMetrics) {
        setTotalInvested(
                variableMetrics.totalInvested(),
                fixedMetrics.totalInvested(),
                totalMetrics.totalInvested());
        setCurrentValue(
                variableMetrics.currentValue(),
                fixedMetrics.currentValue(),
                totalMetrics.currentValue());
        setProfitLoss(
                variableMetrics.profitLoss(), fixedMetrics.profitLoss(), totalMetrics.profitLoss());
        setReturnPercentage(
                variableMetrics.returnPercentage(),
                fixedMetrics.returnPercentage(),
                totalMetrics.returnPercentage());
        setTotalDividends(variableMetrics.totalDividends(), totalMetrics.totalDividends());
        setDividendYield(variableMetrics.dividendYield(), totalMetrics.dividendYield());
    }

    private void setTotalInvested(BigDecimal variable, BigDecimal fixed, BigDecimal total) {
        totalInvestedVariable.setText(UIUtils.formatCurrency(variable));
        totalInvestedFixed.setText(UIUtils.formatCurrency(fixed));
        totalInvestedTotal.setText(UIUtils.formatCurrency(total));
    }

    private void setCurrentValue(BigDecimal variable, BigDecimal fixed, BigDecimal total) {
        currentValueVariable.setText(UIUtils.formatCurrency(variable));
        currentValueFixed.setText(UIUtils.formatCurrency(fixed));
        currentValueTotal.setText(UIUtils.formatCurrency(total));
    }

    private void setProfitLoss(BigDecimal variable, BigDecimal fixed, BigDecimal total) {
        profitLossVariable.setText(formatValueWithSign(variable, false));
        addColorStyleClass(profitLossVariable, variable, true);

        profitLossFixed.setText(formatValueWithSign(fixed, false));
        addColorStyleClass(profitLossFixed, fixed, true);

        profitLossTotal.setText(formatValueWithSign(total, false));
        addColorStyleClass(profitLossTotal, total, true);
    }

    private void setReturnPercentage(BigDecimal variable, BigDecimal fixed, BigDecimal total) {
        returnPercentageVariable.setText(formatValueWithSign(variable, true));
        addColorStyleClass(returnPercentageVariable, variable, true);

        returnPercentageFixed.setText(formatValueWithSign(fixed, true));
        addColorStyleClass(returnPercentageFixed, fixed, true);

        returnPercentageTotal.setText(formatValueWithSign(total, true));
        addColorStyleClass(returnPercentageTotal, total, true);
    }

    private void setTotalDividends(BigDecimal variable, BigDecimal total) {
        totalDividendsVariable.setText(UIUtils.formatCurrency(variable));
        totalDividendsFixed.setText("-");
        totalDividendsFixed.getStyleClass().clear();
        totalDividendsFixed.getStyleClass().add(Constants.INFO_LABEL_NEUTRAL_STYLE);
        totalDividendsTotal.setText(UIUtils.formatCurrency(total));
    }

    private void setDividendYield(BigDecimal variable, BigDecimal total) {
        dividendYieldVariable.setText(UIUtils.formatPercentage(variable, i18nService));
        dividendYieldFixed.setText("-");
        dividendYieldFixed.getStyleClass().clear();
        dividendYieldFixed.getStyleClass().add(Constants.INFO_LABEL_NEUTRAL_STYLE);
        dividendYieldTotal.setText(UIUtils.formatPercentage(total, i18nService));
    }

    private String formatValueWithSign(BigDecimal value, boolean isPercentage) {
        String sign = "";
        if (value.compareTo(BigDecimal.ZERO) > 0) {
            sign = "+ ";
        } else if (value.compareTo(BigDecimal.ZERO) < 0) {
            sign = "- ";
        }

        String formattedValue =
                isPercentage
                        ? UIUtils.formatPercentage(value.abs(), i18nService)
                        : UIUtils.formatCurrency(value.abs());

        return sign + formattedValue;
    }

    private String getColorStyleClass(BigDecimal value, boolean dynamicColor) {
        if (!dynamicColor) {
            return Constants.INFO_LABEL_NEUTRAL_STYLE;
        }

        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return Constants.INFO_LABEL_RED_STYLE;
        } else if (value.compareTo(BigDecimal.ZERO) > 0) {
            return Constants.INFO_LABEL_GREEN_STYLE;
        } else {
            return Constants.INFO_LABEL_NEUTRAL_STYLE;
        }
    }

    private void addColorStyleClass(Label label, BigDecimal value, boolean dynamicColor) {
        String colorClass = getColorStyleClass(value, dynamicColor);
        label.getStyleClass()
                .removeAll(
                        Constants.INFO_LABEL_RED_STYLE,
                        Constants.INFO_LABEL_GREEN_STYLE,
                        Constants.INFO_LABEL_NEUTRAL_STYLE);
        label.getStyleClass().add(colorClass);
    }
}
