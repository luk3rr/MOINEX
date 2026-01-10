/*
 * Filename: FundamentalMetricPaneController.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import lombok.NoArgsConstructor;
import org.json.JSONObject;
import org.moinex.service.I18nService;
import org.moinex.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * @param metricName Name of the metric
     * @param metricData JSON object containing metric data
     * @return The updated VBox
     */
    public VBox updateMetricPane(String metricName, Object metricData) {
        metricNameText.setText(metricName);

        if (metricData instanceof JSONObject) {
            JSONObject metric = (JSONObject) metricData;
            
            // Set main value
            String valueStr = formatMetricValue(metric);
            metricValueText.setText(valueStr);

            // Clear and populate metadata
            metadataBox.getChildren().clear();

            if (metric.has("data_temporality")) {
                Label typeLabel = new Label("Tipo: " + translateTemporality(metric.getString("data_temporality")));
                typeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                metadataBox.getChildren().add(typeLabel);
            }

            if (metric.has("reference_date")) {
                Label dateLabel = new Label("Data: " + formatDate(metric.getString("reference_date")));
                dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                metadataBox.getChildren().add(dateLabel);
            }
        } else {
            // Simple value
            metricValueText.setText(metricData != null ? metricData.toString() : "N/A");
            metadataBox.getChildren().clear();
        }

        return rootVBox;
    }

    /**
     * Format metric value based on type
     */
    private String formatMetricValue(JSONObject metric) {
        if (!metric.has("value")) {
            return "N/A";
        }

        Object value = metric.get("value");
        String type = metric.optString("type", "number");

        if (value == null || value.toString().equals("null")) {
            return "N/A";
        }

        try {
            BigDecimal numValue = new BigDecimal(value.toString());

            switch (type) {
                case "percent":
                    return UIUtils.formatPercentage(numValue, i18nService);
                case "currency":
                    return UIUtils.formatCurrency(numValue);
                case "ratio":
                    return String.format("%.2f", numValue);
                case "number":
                default:
                    return String.format("%.2f", numValue);
            }
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * Translate data temporality
     */
    private String translateTemporality(String temporality) {
        switch (temporality) {
            case "real_time":
                return "Tempo Real";
            case "historical":
                return "Histórico";
            case "calculated":
                return "Calculado";
            default:
                return temporality;
        }
    }

    /**
     * Format date string
     */
    private String formatDate(String dateStr) {
        try {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
