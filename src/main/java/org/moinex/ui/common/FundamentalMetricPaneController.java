/*
 * Filename: FundamentalMetricPaneController.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Fundamental Metric Pane
 * Displays a single fundamental analysis metric in a card format
 *
 * @note prototype is necessary so that each pane instance is unique
 */
@Controller
@Scope("prototype")
@NoArgsConstructor
public class FundamentalMetricPaneController {
    @FXML private VBox rootVBox;
    @FXML private Text metricNameText;
    @FXML private Text metricValueText;
    @FXML private HBox metadataBox;

    private I18nService i18nService;

    @Autowired
    public FundamentalMetricPaneController(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    /**
     * Update the metric pane with data
     *
     * @param metricName     Name of the metric
     * @param metricData     JSON object containing metric data
     * @param lastUpdateDate Last update date to use for real-time data without reference_date
     */
    public void updateMetricPane(String metricName, Object metricData, String lastUpdateDate) {
        metricNameText.setText(metricName);

        if (metricData instanceof JSONObject) {
            JSONObject metric = (JSONObject) metricData;

            // Set main value
            String valueStr = formatMetricValue(metric);
            metricValueText.setText(valueStr);

            // Clear and populate metadata
            metadataBox.getChildren().clear();

            // Use reference_date if available, otherwise use lastUpdateDate for real-time data
            String dateToShow = null;
            if (metric.has("reference_date")) {
                dateToShow = metric.getString("reference_date");
            } else if (metric.has("data_temporality")
                    && "real_time".equals(metric.getString("data_temporality"))
                    && lastUpdateDate != null) {
                dateToShow = lastUpdateDate;
            }

            if (dateToShow != null) {
                Label dateLabel =
                        new Label(
                                i18nService.tr(
                                                Constants.TranslationKeys
                                                        .FUNDAMENTAL_ANALYSIS_REFERENCE_DATE)
                                        + ": "
                                        + formatDate(dateToShow));
                dateLabel.getStyleClass().add("metric-metadata-label");
                metadataBox.getChildren().add(dateLabel);
            }
        } else {
            // Simple value
            metricValueText.setText(UIUtils.getOrDefault(metricData, Constants.NA_DATA).toString());
            metadataBox.getChildren().clear();
        }
    }

    /**
     * Format metric value based on its type
     */
    private String formatMetricValue(JSONObject metric) {
        Object value = metric.opt("value");
        String type = metric.optString("type", "number");

        if (value == null || value.toString().equals("null")) {
            return Constants.NA_DATA;
        }

        try {
            BigDecimal numValue = new BigDecimal(value.toString());

            return switch (type) {
                case "percent" -> UIUtils.formatPercentageForFundamentalAnalysis(numValue);
                case "currency" -> UIUtils.formatCurrency(numValue);
                default -> UIUtils.formatNumWithDecimalPlaces(numValue, 2);
            };
        } catch (Exception e) {
            return UIUtils.getOrDefault(value, Constants.NA_DATA).toString();
        }
    }

    /**
     * Format date string
     */
    private String formatDate(String dateStr) {
        try {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            return UIUtils.formatDateForDisplay(date, i18nService);
        } catch (Exception e) {
            return dateStr;
        }
    }
}
