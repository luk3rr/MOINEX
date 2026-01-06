package org.moinex.chart;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import lombok.Setter;
import org.moinex.service.FinancialPlanningService.BudgetGroupHistoricalDataDTO;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;

public class BudgetGroupTimelineChart extends LineChart<String, Number> {

    @Setter private I18nService i18nService;

    public BudgetGroupTimelineChart() {
        super(new javafx.scene.chart.CategoryAxis(), new NumberAxis());
        setupChart();
        getStylesheets()
                .add(getClass().getResource(Constants.TIMELINE_CHART_STYLE_SHEET).toExternalForm());
    }

    private void setupChart() {
        setLegendVisible(true);
        setAnimated(true);
        setCreateSymbols(false);

        NumberAxis yAxis = (NumberAxis) getYAxis();
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(true);

        javafx.scene.chart.CategoryAxis xAxis = (javafx.scene.chart.CategoryAxis) getXAxis();
        xAxis.setAutoRanging(true);
        xAxis.setTickLabelRotation(45);
    }

    public void updateData(List<BudgetGroupHistoricalDataDTO> historicalData) {
        getData().clear();

        if (historicalData == null || historicalData.isEmpty()) {
            return;
        }

        Map<String, java.util.List<BudgetGroupHistoricalDataDTO>> groupedData = new HashMap<>();

        for (BudgetGroupHistoricalDataDTO data : historicalData) {
            groupedData
                    .computeIfAbsent(data.groupName(), k -> new java.util.ArrayList<>())
                    .add(data);
        }

        for (Map.Entry<String, java.util.List<BudgetGroupHistoricalDataDTO>> entry :
                groupedData.entrySet()) {
            String groupName = entry.getKey();
            XYChart.Series<String, Number> actualSeriesData = new XYChart.Series<>();
            XYChart.Series<String, Number> targetSeriesData = new XYChart.Series<>();

            String actualLabel = i18nService.tr(Constants.TranslationKeys.PLAN_TIMELINE_ACTUAL);
            String targetLabel = i18nService.tr(Constants.TranslationKeys.PLAN_TIMELINE_TARGET);

            actualSeriesData.setName(groupName + " (" + actualLabel + ")");
            targetSeriesData.setName(groupName + " (" + targetLabel + ")");

            for (BudgetGroupHistoricalDataDTO data : entry.getValue()) {
                String periodLabel = UIUtils.formatShortMonthYear(data.period(), i18nService);

                BigDecimal actualAmount = data.spentAmount();
                BigDecimal targetAmount = data.targetAmount();

                // Add small offset to target if it equals actual to prevent complete overlap
                if (actualAmount.compareTo(targetAmount) == 0) {
                    targetAmount = targetAmount.add(new BigDecimal("0.01"));
                }

                XYChart.Data<String, Number> actualDataPoint =
                        new XYChart.Data<>(periodLabel, actualAmount);
                XYChart.Data<String, Number> targetDataPoint =
                        new XYChart.Data<>(periodLabel, targetAmount);

                actualSeriesData.getData().add(actualDataPoint);
                targetSeriesData.getData().add(targetDataPoint);
            }

            getData().add(actualSeriesData);
            getData().add(targetSeriesData);
        }

        applyStyling();
    }

    private void applyStyling() {
        if (i18nService == null) return;

        String actualLabel = i18nService.tr(Constants.TranslationKeys.PLAN_TIMELINE_ACTUAL);
        String targetLabel = i18nService.tr(Constants.TranslationKeys.PLAN_TIMELINE_TARGET);

        for (int i = 0; i < getData().size(); i++) {
            XYChart.Series<String, Number> series = getData().get(i);

            // Add tooltip to the series line itself
            if (series.getNode() != null) {
                String seriesName = series.getName();
                String groupName =
                        seriesName
                                .replace(" (" + actualLabel + ")", "")
                                .replace(" (" + targetLabel + ")", "");

                boolean isActual = seriesName.contains(actualLabel);
                String typeLabel = isActual ? actualLabel : targetLabel;

                String tooltipText = groupName + " - " + typeLabel;
                UIUtils.addTooltipToNode(series.getNode(), tooltipText);
            }
        }
    }

    public void setXAxisLabel(String label) {
        javafx.scene.chart.CategoryAxis xAxis = (javafx.scene.chart.CategoryAxis) getXAxis();
        xAxis.setLabel(label);
    }

    public void setYAxisLabel(String label) {
        getYAxis().setLabel(label);
    }
}
