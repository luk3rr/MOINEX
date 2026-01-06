package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.chart.BudgetGroupTimelineChart;
import org.moinex.chart.DoughnutChart;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.service.FinancialPlanningService;
import org.moinex.service.I18nService;
import org.moinex.ui.common.BudgetGroupPaneController;
import org.moinex.ui.dialog.financialplanning.AddPlanController;
import org.moinex.ui.dialog.financialplanning.EditPlanController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Financial Planning view
 */
@Controller
@NoArgsConstructor
public class PlanController {

    private static final Logger logger = LoggerFactory.getLogger(PlanController.class);
    private static final int ITEMS_PER_PAGE = 3;

    @FXML private ComboBox<YearMonth> periodComboBox;
    @FXML private Label baseMonthlyIncome;
    @FXML private AnchorPane pieChartAnchorPane;
    @FXML private JFXButton budgetGroupPrevButton;
    @FXML private JFXButton budgetGroupNextButton;
    @FXML private AnchorPane budgetGroupPane1;
    @FXML private AnchorPane budgetGroupPane2;
    @FXML private AnchorPane budgetGroupPane3;

    @FXML private AnchorPane timelineChartAnchorPane;

    private ConfigurableApplicationContext springContext;
    private FinancialPlanningService financialPlanningService;
    private I18nService i18nService;

    private FinancialPlan currentPlan;
    private List<FinancialPlanningService.PlanStatusDTO> currentPlanStatus;
    private int currentPage = 0;
    private final int historicalDataMonths = 12;

    @Autowired
    public PlanController(
            ConfigurableApplicationContext springContext,
            FinancialPlanningService financialPlanningService,
            I18nService i18nService) {
        this.springContext = springContext;
        this.financialPlanningService = financialPlanningService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        populatePeriodComboBox();
        setupListeners();

        try {
            this.currentPlan = financialPlanningService.getActivePlan();

            updateView();
        } catch (EntityNotFoundException e) {
            logger.warn("No active financial plan found. Please create a new plan.");
        }
    }

    @FXML
    private void handleNewPlan() {
        WindowUtils.openModalWindow(
                Constants.ADD_PLAN_FXML,
                i18nService.tr(Constants.TranslationKeys.PLAN_DIALOG_ADD_PLAN_TITLE),
                springContext,
                (AddPlanController controller) -> {},
                List.of(this::updateView));
    }

    @FXML
    private void handleEditPlan() {
        if (currentPlan == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.PLAN_DIALOG_NO_ACTIVE_PLAN_TITLE),
                    i18nService.tr(Constants.TranslationKeys.PLAN_DIALOG_NO_ACTIVE_PLAN_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_PLAN_FXML,
                i18nService.tr(Constants.TranslationKeys.PLAN_DIALOG_EDIT_PLAN_TITLE),
                springContext,
                (EditPlanController controller) -> controller.setPlan(currentPlan),
                List.of(this::updateView));
    }

    /**
     * Populates the period ComboBox with the last 12 months.
     */
    private void populatePeriodComboBox() {
        ObservableList<YearMonth> periods = FXCollections.observableArrayList();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            periods.add(current.minusMonths(i));
        }

        periodComboBox.setItems(periods);
        periodComboBox.setValue(current);

        periodComboBox.setConverter(
                new StringConverter<>() {
                    final DateTimeFormatter formatter =
                            UIUtils.getFullMonthYearFormatter(i18nService.getLocale());

                    @Override
                    public String toString(YearMonth yearMonth) {
                        return yearMonth.format(formatter);
                    }

                    @Override
                    public YearMonth fromString(String string) {
                        return YearMonth.parse(string, formatter);
                    }
                });
    }

    /**
     * Sets up listeners for UI components.
     */
    private void setupListeners() {
        periodComboBox
                .valueProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (newVal != null) {
                                updateView();
                            }
                        });

        budgetGroupPrevButton.setOnAction(
                event -> {
                    if (currentPage > 0) {
                        currentPage--;
                        updateBudgetGroupPanes();
                    }
                });

        budgetGroupNextButton.setOnAction(
                event -> {
                    int maxPages =
                            (int) Math.ceil((double) currentPlanStatus.size() / ITEMS_PER_PAGE);
                    if (currentPage < maxPages - 1) {
                        currentPage++;
                        updateBudgetGroupPanes();
                    }
                });
    }

    /**
     * Main method to refresh the entire view based on the selected period.
     */
    private void updateView() {
        YearMonth selectedPeriod = periodComboBox.getValue();
        if (selectedPeriod == null) return;

        try {
            this.currentPlan = financialPlanningService.getActivePlan();
            this.currentPlanStatus =
                    financialPlanningService.getPlanStatus(
                            this.currentPlan.getId(), selectedPeriod);

            this.currentPlanStatus.sort(
                    (a, b) ->
                            b.group()
                                    .getTargetPercentage()
                                    .compareTo(a.group().getTargetPercentage()));

            baseMonthlyIncome.setText(UIUtils.formatCurrency(currentPlan.getBaseIncome()));
            updateDoughnutChart();
            currentPage = 0;
            updateBudgetGroupPanes();
            updateTimelineChart();

        } catch (EntityNotFoundException e) {
            logger.warn("Financial plan not found. Please create a new plan.");
        }
    }

    /**
     * Updates the doughnut chart with the plan's target distribution.
     */
    private void updateDoughnutChart() {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (FinancialPlanningService.PlanStatusDTO status : currentPlanStatus) {
            pieChartData.add(
                    new PieChart.Data(
                            status.group().getName(),
                            status.group().getTargetPercentage().doubleValue()));
        }

        DoughnutChart doughnutChart = new DoughnutChart(pieChartData);
        doughnutChart.setI18nService(i18nService);
        doughnutChart.setShowCenterLabel(false);
        doughnutChart.setLegendVisible(true);
        doughnutChart.setLabelsVisible(false);

        for (PieChart.Data data : doughnutChart.getData()) {
            Node node = data.getNode();
            BigDecimal percentage = BigDecimal.valueOf(data.getPieValue());
            BigDecimal value =
                    currentPlan
                            .getBaseIncome()
                            .multiply(
                                    percentage.divide(
                                            BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

            String tooltipText =
                    data.getName()
                            + "\n"
                            + UIUtils.formatCurrency(value)
                            + " ("
                            + UIUtils.formatPercentage(percentage, i18nService)
                            + ")";

            UIUtils.addTooltipToNode(node, tooltipText);
        }

        UIUtils.applyDefaultChartStyle(doughnutChart);

        pieChartAnchorPane.getChildren().clear();
        pieChartAnchorPane.getChildren().add(doughnutChart);
        AnchorPane.setTopAnchor(doughnutChart, 0.0);
        AnchorPane.setBottomAnchor(doughnutChart, 0.0);
        AnchorPane.setLeftAnchor(doughnutChart, 0.0);
        AnchorPane.setRightAnchor(doughnutChart, 0.0);
    }

    /**
     * Updates the three AnchorPanes with the budget group cards for the current page.
     */
    private void updateBudgetGroupPanes() {
        List<AnchorPane> panes = List.of(budgetGroupPane1, budgetGroupPane2, budgetGroupPane3);
        panes.forEach(p -> p.getChildren().clear());

        int startIndex = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int dataIndex = startIndex + i;
            if (dataIndex < currentPlanStatus.size()) {
                AnchorPane currentPane = panes.get(i);
                FinancialPlanningService.PlanStatusDTO status = currentPlanStatus.get(dataIndex);
                loadBudgetGroupPane(currentPane, status);
            }
        }

        budgetGroupPrevButton.setDisable(currentPage == 0);
        int maxPages = (int) Math.ceil((double) currentPlanStatus.size() / ITEMS_PER_PAGE);
        budgetGroupNextButton.setDisable(currentPage >= maxPages - 1);
    }

    private void loadBudgetGroupPane(
            AnchorPane parent, FinancialPlanningService.PlanStatusDTO status) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(Constants.BUDGET_GROUP_PANE_FXML),
                            i18nService.getBundle());
            loader.setControllerFactory(springContext::getBean);
            Parent content = loader.load();

            BudgetGroupPaneController controller = loader.getController();
            controller.setData(status.group(), status.spentAmount(), currentPlan.getBaseIncome());

            parent.getChildren().clear();
            parent.getChildren().add(content);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);

        } catch (IOException e) {
            logger.error("Error loading budget group pane: {}", e.getMessage());
        }
    }

    private void updateTimelineChart() {
        if (currentPlan == null) {
            return;
        }

        YearMonth currentPeriod = periodComboBox.getValue();
        if (currentPeriod == null) {
            return;
        }

        YearMonth startPeriod = currentPeriod.minusMonths(historicalDataMonths);
        YearMonth endPeriod = currentPeriod;

        List<FinancialPlanningService.BudgetGroupHistoricalDataDTO> historicalData =
                financialPlanningService.getHistoricalData(
                        currentPlan.getId(), startPeriod, endPeriod);

        BudgetGroupTimelineChart timelineChart = new BudgetGroupTimelineChart();
        timelineChart.setI18nService(i18nService);
        timelineChart.setXAxisLabel(i18nService.tr(Constants.TranslationKeys.PLAN_TIMELINE_X_AXIS));
        timelineChart.setYAxisLabel(i18nService.tr(Constants.TranslationKeys.PLAN_TIMELINE_Y_AXIS));
        timelineChart.updateData(historicalData);

        UIUtils.applyDefaultChartStyle(timelineChart);

        timelineChartAnchorPane.getChildren().clear();
        timelineChartAnchorPane.getChildren().add(timelineChart);
        AnchorPane.setTopAnchor(timelineChart, 0.0);
        AnchorPane.setBottomAnchor(timelineChart, 0.0);
        AnchorPane.setLeftAnchor(timelineChart, 0.0);
        AnchorPane.setRightAnchor(timelineChart, 0.0);
    }
}
