package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.chart.DoughnutChart;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.service.FinancialPlanningService;
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

    @FXML private VBox budgetGroupVBox;

    private ConfigurableApplicationContext springContext;
    private FinancialPlanningService financialPlanningService;

    private FinancialPlan currentPlan;
    private List<FinancialPlanningService.PlanStatusDTO> currentPlanStatus;
    private int currentPage = 0;

    @Autowired
    public PlanController(
            ConfigurableApplicationContext springContext,
            FinancialPlanningService financialPlanningService) {
        this.springContext = springContext;
        this.financialPlanningService = financialPlanningService;
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
                "Add New Financial Plan",
                springContext,
                (AddPlanController controller) -> {},
                List.of(this::updateView));
    }

    @FXML
    private void handleEditPlan() {
        if (currentPlan == null) {
            WindowUtils.showInformationDialog(
                    "No Active Plan", "Please create a financial plan before editing.");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_PLAN_FXML,
                "Edit Financial Plan",
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
                    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");

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

            baseMonthlyIncome.setText(UIUtils.formatCurrency(currentPlan.getBaseIncome()));
            updateDoughnutChart();
            currentPage = 0;
            updateBudgetGroupPanes();

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
        doughnutChart.setShowCenterLabel(false);
        doughnutChart.setLegendVisible(false);
        doughnutChart.setLabelsVisible(false);

        pieChartAnchorPane.getChildren().clear();
        pieChartAnchorPane.getChildren().add(doughnutChart);
        AnchorPane.setTopAnchor(doughnutChart, 0.0);
        AnchorPane.setBottomAnchor(doughnutChart, 0.0);
        AnchorPane.setLeftAnchor(doughnutChart, 0.0);
        AnchorPane.setRightAnchor(doughnutChart, 0.0);

        int colorIndex = 0;
        for (PieChart.Data data : doughnutChart.getData()) {
            data.getNode()
                    .getStyleClass()
                    .add(
                            Constants.CHARTS_COLORS_PREFIX
                                    + (colorIndex % Constants.CHARTS_COLORS_COUNT));
            colorIndex++;
        }

        updateChartLegend();
    }

    /**
     * Updates the three AnchorPanes with the budget group cards for the current page.
     */
    private void updateBudgetGroupPanes() {
        List<AnchorPane> panes = List.of(budgetGroupPane1, budgetGroupPane2, budgetGroupPane3);
        panes.forEach(p -> p.getChildren().clear());

        currentPlanStatus.sort(
                (a, b) ->
                        b.group().getTargetPercentage().compareTo(a.group().getTargetPercentage()));

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

    /**
     * Loads the budget_group_pane.fxml, populates it with data, and adds it to a parent pane.
     */
    private void loadBudgetGroupPane(
            AnchorPane parent, FinancialPlanningService.PlanStatusDTO status) {
        try {
            FXMLLoader loader =
                    new FXMLLoader(getClass().getResource(Constants.BUDGET_GROUP_PANE_FXML));
            loader.setControllerFactory(springContext::getBean);
            Parent content = loader.load();

            BudgetGroupPaneController controller = loader.getController();
            controller.setData(status.group(), status.spentAmount(), currentPlan.getBaseIncome());

            parent.getChildren().add(content);
            AnchorPane.setTopAnchor(content, 0.0);
            AnchorPane.setBottomAnchor(content, 0.0);
            AnchorPane.setLeftAnchor(content, 0.0);
            AnchorPane.setRightAnchor(content, 0.0);

        } catch (IOException e) {
            logger.error("Error loading budget group pane: {}", e.getMessage());
        }
    }

    /**
     * Creates a custom legend and populates it in the designated VBox.
     */
    private void updateChartLegend() {
        budgetGroupVBox.getChildren().clear();

        int colorIndex = 0;

        for (FinancialPlanningService.PlanStatusDTO status : currentPlanStatus) {
            HBox legendItem = new HBox(5);
            legendItem.setAlignment(Pos.CENTER_LEFT);

            Rectangle colorRect = new Rectangle(10, 10);
            colorRect
                    .getStyleClass()
                    .addAll(
                            Constants.CHARTS_LEGEND_RECT_STYLE,
                            Constants.CHARTS_COLORS_PREFIX
                                    + (colorIndex % Constants.CHARTS_COLORS_COUNT));

            String labelText =
                    String.format(
                            "%s (%.0f%%)",
                            status.group().getName(), status.group().getTargetPercentage());
            Label legendLabel = new Label(labelText);

            legendItem.getChildren().addAll(colorRect, legendLabel);
            budgetGroupVBox.getChildren().add(legendItem);

            colorIndex++;
        }
    }
}
