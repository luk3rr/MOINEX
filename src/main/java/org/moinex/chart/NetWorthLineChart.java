package org.moinex.chart;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Side;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.Line;
import lombok.Setter;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;

public class NetWorthLineChart extends LineChart<String, Number> {

    @Setter private I18nService i18nService;
    private Line currentDateLine;
    private YearMonth currentMonth;
    private List<NetWorthDataPoint> dataPoints;

    public NetWorthLineChart() {
        super(new CategoryAxis(), new NumberAxis());
        this.currentMonth = YearMonth.now();
        setupChart();
        getStylesheets()
                .add(getClass().getResource(Constants.TIMELINE_CHART_STYLE_SHEET).toExternalForm());
    }

    private void setupChart() {
        setLegendVisible(true);
        setLegendSide(Side.BOTTOM);
        setAnimated(true);
        setCreateSymbols(true);

        NumberAxis yAxis = (NumberAxis) getYAxis();
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);

        CategoryAxis xAxis = (CategoryAxis) getXAxis();
        xAxis.setAutoRanging(true);
        xAxis.setTickLabelRotation(45);
    }

    public void updateData(List<NetWorthDataPoint> dataPoints) {
        getData().clear();

        if (dataPoints == null || dataPoints.isEmpty()) {
            return;
        }

        this.dataPoints = dataPoints;

        XYChart.Series<String, Number> assetsSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> liabilitiesSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> netWorthSeries = new XYChart.Series<>();

        assetsSeries.setName(i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_ASSETS));
        liabilitiesSeries.setName(
                i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_LIABILITIES));
        netWorthSeries.setName(i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_NET_WORTH));

        for (NetWorthDataPoint dataPoint : dataPoints) {
            String periodLabel = UIUtils.formatShortMonthYear(dataPoint.period(), i18nService);

            XYChart.Data<String, Number> assetsData =
                    new XYChart.Data<>(periodLabel, dataPoint.assets());
            XYChart.Data<String, Number> liabilitiesData =
                    new XYChart.Data<>(periodLabel, dataPoint.liabilities());
            XYChart.Data<String, Number> netWorthData =
                    new XYChart.Data<>(periodLabel, dataPoint.netWorth());

            assetsSeries.getData().add(assetsData);
            liabilitiesSeries.getData().add(liabilitiesData);
            netWorthSeries.getData().add(netWorthData);
        }

        getData().add(assetsSeries);
        getData().add(liabilitiesSeries);
        getData().add(netWorthSeries);

        applyStyling();

        Platform.runLater(
                () -> {
                    addTooltipsToAxisLabels(dataPoints);
                    addCurrentDateLine(dataPoints);
                });
    }

    private void applyStyling() {
        if (i18nService == null) return;

        for (int i = 0; i < getData().size(); i++) {
            XYChart.Series<String, Number> series = getData().get(i);

            String styleClass = "";
            if (i == 0) {
                styleClass = "assets-series";
            } else if (i == 1) {
                styleClass = "liabilities-series";
            } else if (i == 2) {
                styleClass = "net-worth-series";
            }

            if (series.getNode() != null && !styleClass.isEmpty()) {
                series.getNode().getStyleClass().add(styleClass);
            }
        }
    }

    private void addTooltipsToAxisLabels(List<NetWorthDataPoint> dataPoints) {
        CategoryAxis xAxis = (CategoryAxis) getXAxis();

        widthProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            Platform.runLater(
                                    () -> {
                                        reapplyTooltips(dataPoints, xAxis);
                                        addCurrentDateLine(dataPoints);
                                    });
                        });

        heightProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            Platform.runLater(
                                    () -> {
                                        reapplyTooltips(dataPoints, xAxis);
                                        addCurrentDateLine(dataPoints);
                                    });
                        });

        Platform.runLater(
                () -> {
                    reapplyTooltips(dataPoints, xAxis);
                    addCurrentDateLine(dataPoints);
                });
    }

    private void reapplyTooltips(List<NetWorthDataPoint> dataPoints, CategoryAxis xAxis) {
        for (NetWorthDataPoint dataPoint : dataPoints) {
            String periodLabel = UIUtils.formatShortMonthYear(dataPoint.period(), i18nService);
            String tooltipText = createTooltipText(dataPoint);
            UIUtils.addTooltipToAxisLabel(xAxis, periodLabel, tooltipText);
        }
    }

    private String createTooltipText(NetWorthDataPoint dataPoint) {
        String assetsLabel = i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_ASSETS);
        String liabilitiesLabel =
                i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_LIABILITIES);
        String netWorthLabel = i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_NET_WORTH);

        String periodLabel = UIUtils.formatFullMonthYear(dataPoint.period(), i18nService);

        return periodLabel
                + "\n"
                + assetsLabel
                + ": "
                + UIUtils.formatCurrency(dataPoint.assets())
                + "\n"
                + liabilitiesLabel
                + ": "
                + UIUtils.formatCurrency(dataPoint.liabilities())
                + "\n"
                + netWorthLabel
                + ": "
                + UIUtils.formatCurrency(dataPoint.netWorth());
    }

    private void addCurrentDateLine(List<NetWorthDataPoint> dataPoints) {
        if (currentDateLine != null) {
            getPlotChildren().remove(currentDateLine);
        }

        int currentIndex = -1;
        for (int i = 0; i < dataPoints.size(); i++) {
            if (dataPoints.get(i).period().equals(currentMonth)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex == -1) {
            return;
        }

        currentDateLine = new Line();
        currentDateLine.getStyleClass().add("current-date-line");
        currentDateLine.setStrokeWidth(2);
        currentDateLine.getStrokeDashArray().addAll(5.0, 5.0);

        final int index = currentIndex;

        // Use layoutBoundsProperty to listen for layout changes
        layoutBoundsProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            updateCurrentDateLinePosition(index, dataPoints.size());
                        });

        getPlotChildren().add(currentDateLine);

        // Initial position update
        layoutPlotChildren();
        updateCurrentDateLinePosition(index, dataPoints.size());
    }

    private void updateCurrentDateLinePosition(int currentIndex, int totalPoints) {
        if (currentDateLine == null || totalPoints == 0) {
            return;
        }

        CategoryAxis xAxis = (CategoryAxis) getXAxis();
        NumberAxis yAxis = (NumberAxis) getYAxis();

        if (xAxis.getCategories().isEmpty() || currentIndex >= xAxis.getCategories().size()) {
            return;
        }

        double xPosition = getXAxis().getDisplayPosition(xAxis.getCategories().get(currentIndex));

        currentDateLine.setStartX(xPosition);
        currentDateLine.setEndX(xPosition);
        currentDateLine.setStartY(0);
        currentDateLine.setEndY(yAxis.getHeight());
    }

    public record NetWorthDataPoint(
            YearMonth period, BigDecimal assets, BigDecimal liabilities, BigDecimal netWorth) {}
}
