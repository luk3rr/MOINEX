/*
 * Filename: GoalController.java
 * Created on: December  8, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import lombok.NoArgsConstructor;
import org.moinex.model.goal.Goal;
import org.moinex.service.GoalService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.common.GoalFullPaneController;
import org.moinex.ui.dialog.goal.AddGoalController;
import org.moinex.ui.dialog.goal.EditGoalController;
import org.moinex.ui.dialog.wallettransaction.AddTransferController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.GoalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller class for the goal view
 */
@Controller
@NoArgsConstructor
public class GoalController {
    private static final Logger logger = LoggerFactory.getLogger(GoalController.class);
    private static final Integer IN_PROGRESS_ITEMS_PER_PAGE = 2;
    private static final Integer ACCOMPLISHED_ITEMS_PER_PAGE = 2;
    @FXML private AnchorPane inProgressPane1;
    @FXML private AnchorPane inProgressPane2;
    @FXML private AnchorPane accomplishedPane1;
    @FXML private AnchorPane accomplishedPane2;
    @FXML private JFXButton inProgressPrevButton;
    @FXML private JFXButton inProgressNextButton;
    @FXML private JFXButton accomplishedPrevButton;
    @FXML private JFXButton accomplishedNextButton;
    @FXML private TableView<Goal> goalTableView;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextField goalSearchField;
    private ConfigurableApplicationContext springContext;
    private GoalService goalService;
    private WalletTransactionService walletTransactionService;
    private List<Goal> goals;
    private Integer inProgressCurrentPage = 0;
    private Integer accomplishedCurrentPage = 0;

    /**
     * Constructor
     *
     * @param goalService              The goal service
     * @param walletTransactionService The wallet transaction service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public GoalController(
            GoalService goalService,
            WalletTransactionService walletTransactionService,
            ConfigurableApplicationContext springContext) {
        this.goalService = goalService;
        this.walletTransactionService = walletTransactionService;
        this.springContext = springContext;
    }

    private static TableColumn<Goal, String> getColumn() {
        TableColumn<Goal, String> recommendedMonthlyDepositColumn =
                new TableColumn<>("Recommended Monthly Deposit");
        recommendedMonthlyDepositColumn.setCellValueFactory(
                param -> {
                    // If the goal is archived, return an empty string
                    if (param.getValue().isCompleted()) return new SimpleObjectProperty<>("-");

                    // Calculate the number of months until the target date
                    long monthsUntilTarget =
                            Constants.calculateMonthsUntilTarget(
                                    LocalDate.now(),
                                    param.getValue().getTargetDate().toLocalDate());

                    // Calculate the recommended monthly deposit
                    double recommendedMonthlyDeposit =
                            getRecommendedMonthlyDeposit(param, monthsUntilTarget);

                    return new SimpleObjectProperty<>(
                            UIUtils.formatCurrency(recommendedMonthlyDeposit));
                });
        return recommendedMonthlyDepositColumn;
    }

    private static TableColumn<Goal, String> getTableColumn() {
        TableColumn<Goal, String> monthsUntilTargetColumn =
                new TableColumn<>("Months Until Target");
        monthsUntilTargetColumn.setCellValueFactory(
                param -> {
                    // If the goal is archived, return an empty string
                    if (param.getValue().isCompleted()) return new SimpleObjectProperty<>("-");

                    // Calculate the number of months until the target date
                    Integer monthsUntilTarget =
                            Constants.calculateMonthsUntilTarget(
                                    LocalDate.now(),
                                    param.getValue().getTargetDate().toLocalDate());

                    return new SimpleObjectProperty<>(monthsUntilTarget.toString());
                });
        return monthsUntilTargetColumn;
    }

    private static TableColumn<Goal, String> getStringTableColumn() {
        TableColumn<Goal, String> completionDateColumn = new TableColumn<>("Completion Date");
        completionDateColumn.setCellValueFactory(
                param -> {
                    // If the goal is archived and has a completion date, return it
                    // formatted, otherwise return an empty string
                    if (param.getValue().isCompleted()
                            && param.getValue().getCompletionDate() != null) {
                        return new SimpleStringProperty(
                                param.getValue()
                                        .getCompletionDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME));
                    }

                    return new SimpleObjectProperty<>("-");
                });
        return completionDateColumn;
    }

    private static TableColumn<Goal, String> getGoalStringTableColumn() {
        TableColumn<Goal, String> progressColumn = new TableColumn<>("Progress");
        progressColumn.setCellValueFactory(
                param -> {
                    // If the goal is archived, return 100 %
                    if (param.getValue().isCompleted())
                        return new SimpleObjectProperty<>(UIUtils.formatPercentage(100));

                    return new SimpleObjectProperty<>(
                            UIUtils.formatPercentage(
                                    // Calculate the progress, avoiding division by zero
                                    param.getValue().getBalance().compareTo(BigDecimal.ZERO) == 0
                                            ? BigDecimal.ZERO
                                            : param.getValue().getBalance().doubleValue()
                                                    / param.getValue()
                                                            .getTargetBalance()
                                                            .doubleValue()
                                                    * 100));
                });
        return progressColumn;
    }

    private static TableColumn<Goal, Integer> getGoalLongTableColumn() {
        TableColumn<Goal, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item.toString());
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;"); // set padding to zero to
                                    // ensure the text is centered
                                }
                            }
                        });
        return idColumn;
    }

    private static double getRecommendedMonthlyDeposit(
            TableColumn.CellDataFeatures<Goal, String> param, long monthsUntilTarget) {
        double recommendedMonthlyDeposit;

        if (monthsUntilTarget <= 0) {
            recommendedMonthlyDeposit =
                    param.getValue()
                            .getTargetBalance()
                            .subtract(param.getValue().getBalance())
                            .doubleValue();
        } else {
            recommendedMonthlyDeposit =
                    param.getValue()
                                    .getTargetBalance()
                                    .subtract(param.getValue().getBalance())
                                    .doubleValue()
                            / BigDecimal.valueOf(monthsUntilTarget).doubleValue();
        }
        return recommendedMonthlyDeposit;
    }

    @FXML
    private void initialize() {
        populateStatusComboBox();

        configureTableView();

        loadGoalsFromDatabase();

        updateDisplayInProgressGoals();
        updateDisplayAccomplishedGoals();

        updateGoalTableView();

        statusComboBox.setOnAction(event -> updateGoalTableView());

        // Add listener to the search field
        goalSearchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateGoalTableView());

        setButtonsActions();
    }

    @FXML
    private void handleAddGoal() {
        WindowUtils.openModalWindow(
                Constants.ADD_GOAL_FXML,
                "Add Goal",
                springContext,
                (AddGoalController controller) -> {},
                List.of(
                        () -> {
                            loadGoalsFromDatabase();
                            updateDisplayInProgressGoals();
                            updateDisplayAccomplishedGoals();
                            updateGoalTableView();
                        }));
    }

    @FXML
    private void handleAddDeposit() {
        // Get the selected goal
        Goal goal = goalTableView.getSelectionModel().getSelectedItem();

        if (goal == null) {
            WindowUtils.showInformationDialog(
                    "No goal selected", "Please select a goal to add a deposit");
            return;
        }

        if (goal.isArchived()) {
            WindowUtils.showInformationDialog(
                    "Goal is archived",
                    "Cannot add transfer to an archived goal. Unarchive the goal first");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_TRANSFER_FXML,
                "Add new transfer",
                springContext,
                (AddTransferController controller) -> controller.setReceiverWalletComboBox(goal),
                List.of(
                        () -> {
                            loadGoalsFromDatabase();
                            updateDisplayInProgressGoals();
                            updateDisplayAccomplishedGoals();
                            updateGoalTableView();
                        }));
    }

    @FXML
    private void handleEditGoal() {
        // Get the selected goal
        Goal goal = goalTableView.getSelectionModel().getSelectedItem();

        if (goal == null) {
            WindowUtils.showInformationDialog("No goal selected", "Please select a goal to edit");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_GOAL_FXML,
                "Edit Goal",
                springContext,
                (EditGoalController controller) -> controller.setGoal(goal),
                List.of(
                        () -> {
                            loadGoalsFromDatabase();
                            updateDisplayInProgressGoals();
                            updateDisplayAccomplishedGoals();
                            updateGoalTableView();
                        }));
    }

    @FXML
    private void handleDeleteGoal() {
        // Get the selected goal
        Goal goal = goalTableView.getSelectionModel().getSelectedItem();

        if (goal == null) {
            WindowUtils.showInformationDialog("No goal selected", "Please select a goal to delete");
            return;
        }

        // Prevent the removal of a wallet with associated transactions
        if (walletTransactionService.getTransactionCountByWallet(goal.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    "Goal wallet has transactions",
                    "Cannot delete a goal wallet with associated transactions. "
                            + "Remove the transactions first or archive the goal");
            return;
        }

        // Create a message to show to the user
        StringBuilder message = new StringBuilder();

        message.append("Name: ").append(goal.getName()).append("\n");
        message.append("Initial Amount: ")
                .append(UIUtils.formatCurrency(goal.getInitialBalance()))
                .append("\n");
        message.append("Current Amount: ")
                .append(UIUtils.formatCurrency(goal.getBalance()))
                .append("\n");
        message.append("Target Amount: ")
                .append(UIUtils.formatCurrency(goal.getTargetBalance()))
                .append("\n");
        message.append("Target Date: ")
                .append(goal.getTargetDate().format(Constants.DATE_FORMATTER_NO_TIME))
                .append("\n");

        try {
            // Confirm the deletion
            if (WindowUtils.showConfirmationDialog(
                    "Are you sure you want to delete this goal?", message.toString())) {
                goalService.deleteGoal(goal.getId());
                goals.remove(goal);

                updateDisplayInProgressGoals();
                updateDisplayAccomplishedGoals();
                updateGoalTableView();
            }
        } catch (EntityNotFoundException | IllegalStateException e) {
            WindowUtils.showErrorDialog("Error deleting goal", e.getMessage());
        }
    }

    private void loadGoalsFromDatabase() {
        goals = goalService.getGoals();
    }

    /**
     * Update the display
     *
     * @note: This method can be called by other controllers to update the screen when
     * there is a change
     */
    public void updateDisplay() {
        loadGoalsFromDatabase();

        updateDisplayInProgressGoals();
        updateDisplayAccomplishedGoals();
        updateGoalTableView();
    }

    /**
     * Update the display of in progress goals
     */
    private void updateDisplayInProgressGoals() {
        inProgressPane1.getChildren().clear();
        inProgressPane2.getChildren().clear();

        List<Goal> inProgressGoals =
                goals.stream().filter(g -> !g.isCompleted() && !g.isArchived()).toList();

        Integer start = inProgressCurrentPage * IN_PROGRESS_ITEMS_PER_PAGE;
        int end = Math.min(start + IN_PROGRESS_ITEMS_PER_PAGE, inProgressGoals.size());

        for (Integer i = start; i < end; i++) {
            Goal goal = inProgressGoals.get(i);

            try {
                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource(Constants.GOAL_FULL_PANE_FXML));
                loader.setControllerFactory(springContext::getBean);

                Parent newContent = loader.load();

                // Add style class to the wallet pane
                newContent
                        .getStylesheets()
                        .add(
                                Objects.requireNonNull(
                                                getClass()
                                                        .getResource(Constants.COMMON_STYLE_SHEET))
                                        .toExternalForm());

                GoalFullPaneController goalFullPaneController = loader.getController();

                goalFullPaneController.updateGoalPane(goal);

                AnchorPane.setTopAnchor(newContent, 0.0);
                AnchorPane.setBottomAnchor(newContent, 0.0);
                AnchorPane.setLeftAnchor(newContent, 0.0);
                AnchorPane.setRightAnchor(newContent, 0.0);

                switch (i % IN_PROGRESS_ITEMS_PER_PAGE) {
                    case 0:
                        inProgressPane1.getChildren().add(newContent);
                        break;

                    case 1:
                        inProgressPane2.getChildren().add(newContent);
                        break;
                    default:
                        logger.error("Error while loading goal full pane");
                        break;
                }
            } catch (IOException e) {
                logger.error("Error while loading goal full pane");
            }
        }

        inProgressPrevButton.setDisable(inProgressCurrentPage == 0);
        inProgressNextButton.setDisable(end >= inProgressGoals.size());
    }

    /**
     * Update the display of accomplished goals
     */
    private void updateDisplayAccomplishedGoals() {
        accomplishedPane1.getChildren().clear();
        accomplishedPane2.getChildren().clear();

        List<Goal> accomplishedGoals = goals.stream().filter(Goal::isCompleted).toList();

        Integer start = accomplishedCurrentPage * ACCOMPLISHED_ITEMS_PER_PAGE;
        int end = Math.min(start + ACCOMPLISHED_ITEMS_PER_PAGE, accomplishedGoals.size());

        for (Integer i = start; i < end; i++) {
            Goal goal = accomplishedGoals.get(i);

            try {
                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource(Constants.GOAL_FULL_PANE_FXML));
                loader.setControllerFactory(springContext::getBean);

                Parent newContent = loader.load();

                // Add style class to the wallet pane
                newContent
                        .getStylesheets()
                        .add(
                                Objects.requireNonNull(
                                                getClass()
                                                        .getResource(Constants.COMMON_STYLE_SHEET))
                                        .toExternalForm());

                GoalFullPaneController goalFullPaneController = loader.getController();

                goalFullPaneController.updateGoalPane(goal);

                AnchorPane.setTopAnchor(newContent, 0.0);
                AnchorPane.setBottomAnchor(newContent, 0.0);
                AnchorPane.setLeftAnchor(newContent, 0.0);
                AnchorPane.setRightAnchor(newContent, 0.0);

                switch (i % ACCOMPLISHED_ITEMS_PER_PAGE) {
                    case 0:
                        accomplishedPane1.getChildren().add(newContent);
                        break;

                    case 1:
                        accomplishedPane2.getChildren().add(newContent);
                        break;
                    default:
                        logger.error("Error while loading accomplished goal full pane");
                        break;
                }
            } catch (IOException e) {
                logger.error("Error while loading accomplished goal full pane");
            }
        }

        accomplishedPrevButton.setDisable(accomplishedCurrentPage == 0);
        accomplishedNextButton.setDisable(end >= accomplishedGoals.size());
    }

    private Stream<Goal> filterGoalsByStatus(String selectedGoalStatus) {
        return goals.stream()
                .filter(
                        g -> {
                            if ("ALL".equals(selectedGoalStatus)) {
                                return true;
                            }
                            GoalStatus status = GoalStatus.valueOf(selectedGoalStatus);
                            return switch (status) {
                                case COMPLETED -> g.isCompleted() && !g.isArchived();
                                case ACTIVE -> !g.isCompleted() && !g.isArchived();
                                case ARCHIVED -> g.isArchived();
                            };
                        });
    }

    private void updateGoalTableView() {
        // Get the search text
        String searchText = goalSearchField.getText();

        // Get the selected status
        String selectedGoalStatus = statusComboBox.getSelectionModel().getSelectedItem();

        goalTableView.getItems().clear();

        if (searchText.isEmpty()) {
            filterGoalsByStatus(selectedGoalStatus).forEach(goalTableView.getItems()::add);
        } else {
            filterGoalsByStatus(selectedGoalStatus)
                    .filter(
                            g -> {
                                String name = g.getName().toLowerCase();
                                String initialAmount = g.getInitialBalance().toString();
                                String currentAmount = g.getBalance().toString();
                                String targetAmount = g.getTargetBalance().toString();
                                String targetDate =
                                        g.getTargetDate().format(Constants.DATE_FORMATTER_NO_TIME);

                                String completionDate =
                                        g.getCompletionDate() != null
                                                ? g.getCompletionDate()
                                                        .format(Constants.DATE_FORMATTER_NO_TIME)
                                                : "-";

                                String status = g.isCompleted() ? "completed" : "active";

                                String monthsUntilTarget =
                                        Constants.calculateMonthsUntilTarget(
                                                        LocalDate.now(),
                                                        g.getTargetDate().toLocalDate())
                                                .toString();

                                String recommendedMonthlyDeposit =
                                        g.getTargetBalance()
                                                .subtract(g.getBalance())
                                                .divide(
                                                        BigDecimal.valueOf(
                                                                Constants
                                                                        .calculateMonthsUntilTarget(
                                                                                LocalDate.now(),
                                                                                g.getTargetDate()
                                                                                        .toLocalDate())),
                                                        2,
                                                        RoundingMode.HALF_UP)
                                                .toString();

                                return name.contains(searchText.toLowerCase())
                                        || initialAmount.contains(searchText.toLowerCase())
                                        || currentAmount.contains(searchText.toLowerCase())
                                        || targetAmount.contains(searchText.toLowerCase())
                                        || targetDate.contains(searchText.toLowerCase())
                                        || monthsUntilTarget.contains(searchText.toLowerCase())
                                        || recommendedMonthlyDeposit.contains(
                                                searchText.toLowerCase())
                                        || completionDate.contains(searchText.toLowerCase())
                                        || status.contains(searchText.toLowerCase());
                            })
                    .forEach(goalTableView.getItems()::add);
        }

        goalTableView.refresh();
    }

    /**
     * Set the actions for the buttons
     */
    private void setButtonsActions() {
        Integer inProgressGoalsSize =
                goals.stream().filter(g -> !g.isCompleted() && !g.isArchived()).toList().size();

        inProgressPrevButton.setOnAction(
                event -> {
                    if (inProgressCurrentPage > 0) {
                        inProgressCurrentPage--;
                        updateDisplayInProgressGoals();
                    }
                });

        inProgressNextButton.setOnAction(
                event -> {
                    if (inProgressCurrentPage < inProgressGoalsSize / IN_PROGRESS_ITEMS_PER_PAGE) {
                        inProgressCurrentPage++;
                        updateDisplayInProgressGoals();
                    }
                });

        Integer accomplishedGoalsSize = goals.stream().filter(Goal::isCompleted).toList().size();

        accomplishedPrevButton.setOnAction(
                event -> {
                    if (accomplishedCurrentPage > 0) {
                        accomplishedCurrentPage--;
                        updateDisplayAccomplishedGoals();
                    }
                });

        accomplishedNextButton.setOnAction(
                event -> {
                    if (accomplishedCurrentPage
                            < accomplishedGoalsSize / ACCOMPLISHED_ITEMS_PER_PAGE) {
                        accomplishedCurrentPage++;
                        updateDisplayAccomplishedGoals();
                    }
                });
    }

    private void configureTableView() {
        TableColumn<Goal, Integer> idColumn = getGoalLongTableColumn();

        TableColumn<Goal, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getName()));

        TableColumn<Goal, String> initialAmountColumn = new TableColumn<>("Initial Amount");
        initialAmountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getInitialBalance())));

        TableColumn<Goal, String> currentAmountColumn = new TableColumn<>("Current Amount");
        currentAmountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getBalance())));

        TableColumn<Goal, String> targetAmountColumn = new TableColumn<>("Target Amount");
        targetAmountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getTargetBalance())));

        TableColumn<Goal, String> progressColumn = getGoalStringTableColumn();

        TableColumn<Goal, String> targetDateColumn = new TableColumn<>("Target Date");
        targetDateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getTargetDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME)));

        TableColumn<Goal, String> completionDateColumn = getStringTableColumn();

        TableColumn<Goal, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                param -> {
                    Goal goal = param.getValue();
                    String status =
                            goal.isArchived()
                                    ? "ARCHIVED"
                                    : goal.isCompleted() ? "COMPLETED" : "ACTIVE";
                    return new SimpleObjectProperty<>(status);
                });

        TableColumn<Goal, String> monthsUntilTargetColumn = getTableColumn();

        TableColumn<Goal, String> recommendedMonthlyDepositColumn = getColumn();

        goalTableView.getColumns().add(idColumn);
        goalTableView.getColumns().add(nameColumn);
        goalTableView.getColumns().add(statusColumn);
        goalTableView.getColumns().add(initialAmountColumn);
        goalTableView.getColumns().add(currentAmountColumn);
        goalTableView.getColumns().add(targetAmountColumn);
        goalTableView.getColumns().add(progressColumn);
        goalTableView.getColumns().add(targetDateColumn);
        goalTableView.getColumns().add(completionDateColumn);
        goalTableView.getColumns().add(monthsUntilTargetColumn);
        goalTableView.getColumns().add(recommendedMonthlyDepositColumn);

        // Show motivation as a tooltip
        goalTableView.setRowFactory(
                tv -> {
                    TableRow<Goal> row = new TableRow<>();
                    row.itemProperty()
                            .addListener(
                                    (obs, oldItem, newItem) -> {
                                        if (newItem != null) {
                                            Tooltip tooltip =
                                                    new Tooltip(
                                                            "Motivation: "
                                                                    + newItem.getMotivation());
                                            Tooltip.install(row, tooltip);
                                        }
                                    });
                    return row;
                });
    }

    private void populateStatusComboBox() {
        statusComboBox.getItems().add("ALL");
        statusComboBox.getItems().add("ACTIVE");
        statusComboBox.getItems().add("COMPLETED");
        statusComboBox.getItems().add("ARCHIVED");
        statusComboBox.getSelectionModel().selectFirst();
    }
}
