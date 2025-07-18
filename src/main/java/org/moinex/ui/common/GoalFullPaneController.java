/*
 * Filename: GoalFullPaneController.java
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import org.moinex.chart.CircularProgressBar;
import org.moinex.error.MoinexException;
import org.moinex.model.goal.Goal;
import org.moinex.service.GoalService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.dialog.goal.EditGoalController;
import org.moinex.ui.dialog.wallettransaction.AddExpenseController;
import org.moinex.ui.dialog.wallettransaction.AddIncomeController;
import org.moinex.ui.dialog.wallettransaction.AddTransferController;
import org.moinex.ui.main.GoalController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Goal Full Pane
 *
 * @note prototype is necessary so that each scene knows to which goal it belongs
 */
@Controller
@Scope("prototype") // Each instance of this controller is unique
@NoArgsConstructor
public class GoalFullPaneController {
    @FXML private VBox rootVBox;

    @FXML private VBox infosVBox;

    @FXML private ImageView goalIcon;

    @FXML private Label goalName;

    @FXML private Label goalMotivation;

    @FXML private Label goalTargetAmount;

    @FXML private Label goalCurrentAmount;

    @FXML private Label dateTitleLabel;

    @FXML private Label daysTitleLabel;

    @FXML private Label goalTargetDate;

    @FXML private Label goalIdealAMountPerMonth;

    @FXML private Label missingDays;

    @FXML private StackPane progressBarPane;

    @FXML private MenuItem toggleArchiveGoal;

    @FXML private MenuItem toggleCompleteGoal;

    @FXML private HBox currentHBox;

    @FXML private HBox idealPerMonthHBox;

    private ConfigurableApplicationContext springContext;

    private GoalController goalController;

    private GoalService goalService;

    private WalletTransactionService walletTransactionService;

    private Goal goal;

    /**
     * Constructor
     * @param goalService Goal service
     * @param walletTransactionService Wallet transaction service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public GoalFullPaneController(
            GoalService goalService,
            WalletTransactionService walletTransactionService,
            ConfigurableApplicationContext springContext,
            GoalController goalController) {
        this.goalService = goalService;
        this.walletTransactionService = walletTransactionService;
        this.springContext = springContext;
        this.goalController = goalController;
    }

    /**
     * Load goal information from the database
     */
    public void loadGoalInfo() {
        if (goal == null) {
            return;
        }

        // Reload goal from the database
        goal = goalService.getGoalById(goal.getId());
    }

    /**
     * Update the goal pane with the given goal
     * @param gl Goal to update the pane with
     * @return The updated VBox
     */
    public VBox updateGoalPane(Goal gl) {
        // If the goal is null, do not update the pane
        if (gl == null) {
            setDefaultValues();
            return rootVBox;
        }

        goal = gl;
        loadGoalInfo();

        goalName.setText(goal.getName());
        goalMotivation.setText(goal.getMotivation());

        // If the goal is archived, then is finished, so show the trophy icon
        // Otherwise, show the goal icon
        goalIcon.setImage(
                new Image(
                        goal.isCompleted()
                                ? Constants.TROPHY_ICON
                                : Constants.WALLET_TYPE_ICONS_PATH + goal.getType().getIcon()));

        goalTargetAmount.setText(UIUtils.formatCurrency(goal.getTargetBalance()));
        goalTargetDate.setText(goal.getTargetDate().format(Constants.DATE_FORMATTER_NO_TIME));

        // Create a tooltip for name and motivation
        UIUtils.addTooltipToNode(goalName, goal.getName());

        if (!goal.getMotivation().isEmpty()) {
            UIUtils.addTooltipToNode(goalMotivation, goal.getMotivation());
        }

        // Add the progress bar to the pane
        CircularProgressBar progressBar =
                new CircularProgressBar(
                        Constants.GOAL_PANE_PROGRESS_BAR_RADIUS,
                        Constants.GOAL_PANE_PROGRESS_BAR_WIDTH);

        double percentage;

        if (goal.isArchived()) {
            // Set the button text according to the goal status
            toggleArchiveGoal.setText("Unarchive Goal");
        }

        if (goal.isCompleted()) {
            dateTitleLabel.setText("Achieved");
            goalTargetDate.setText(
                    goal.getCompletionDate().format(Constants.DATE_FORMATTER_NO_TIME));

            daysTitleLabel.setText("Days ahead of target");
            missingDays.setText(
                    String.valueOf(
                            Constants.calculateDaysUntilTarget(
                                    goal.getCompletionDate().toLocalDate(),
                                    goal.getTargetDate().toLocalDate())));

            // Remove the fields that are not necessary
            infosVBox.getChildren().remove(currentHBox);
            infosVBox.getChildren().remove(idealPerMonthHBox);

            percentage = 100.0;

            // Set the button text according to the goal status
            toggleCompleteGoal.setText("Reopen Goal");
        } else {
            // Show the current amount
            goalCurrentAmount.setText(UIUtils.formatCurrency(goal.getBalance()));

            // Calculate the number of months until the target date
            Integer monthsUntilTarget =
                    Constants.calculateMonthsUntilTarget(
                            LocalDate.now(), goal.getTargetDate().toLocalDate());

            // Calculate the ideal amount per month
            BigDecimal idealAmountPerMonth;

            if (monthsUntilTarget <= 0) {
                idealAmountPerMonth = goal.getTargetBalance().subtract(goal.getBalance());
            } else {
                idealAmountPerMonth =
                        goal.getTargetBalance()
                                .subtract(goal.getBalance())
                                .divide(
                                        BigDecimal.valueOf(
                                                Constants.calculateMonthsUntilTarget(
                                                        LocalDate.now(),
                                                        goal.getTargetDate().toLocalDate())),
                                        2,
                                        RoundingMode.HALF_UP);
            }

            goalIdealAMountPerMonth.setText(UIUtils.formatCurrency(idealAmountPerMonth));

            // Calculate the missing days
            Integer missingDaysValue =
                    Constants.calculateDaysUntilTarget(
                            LocalDate.now(), goal.getTargetDate().toLocalDate());

            missingDays.setText(missingDaysValue.toString());

            if (goal.getTargetBalance().compareTo(BigDecimal.ZERO) == 0) {
                percentage = 0.0;
            } else {
                percentage =
                        goal.getBalance().doubleValue()
                                / goal.getTargetBalance().doubleValue()
                                * 100.0;
            }
        }

        progressBar.draw(percentage);

        progressBarPane.getChildren().clear();
        progressBarPane.getChildren().add(progressBar);

        return rootVBox;
    }

    @FXML
    private void initialize() {
        // Still empty
    }

    @FXML
    private void handleAddIncome() {
        if (goal.isArchived()) {
            WindowUtils.showInformationDialog(
                    "Goal is archived",
                    "Cannot add income to an archived goal. Unarchive the goal first");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_INCOME_FXML,
                "Add new income",
                springContext,
                (AddIncomeController controller) -> controller.setWalletComboBox(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleAddExpense() {
        if (goal.isArchived()) {
            WindowUtils.showInformationDialog(
                    "Goal is archived",
                    "Cannot add expense to an archived goal. Unarchive the goal first");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_EXPENSE_FXML,
                "Add new expense",
                springContext,
                (AddExpenseController controller) -> controller.setWalletComboBox(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleAddTransfer() {
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
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleEditGoal() {
        WindowUtils.openModalWindow(
                Constants.EDIT_GOAL_FXML,
                "Edit goal",
                springContext,
                (EditGoalController controller) -> controller.setGoal(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleCompleteGoal() {
        if (goal.isCompleted()) {
            if (WindowUtils.showConfirmationDialog(
                    "Reopen goal " + goal.getName(),
                    "Are you sure you want to reopen this goal?")) {
                goalService.reopenGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        } else {
            if (WindowUtils.showConfirmationDialog(
                    "Complete goal " + goal.getName(),
                    "Are you sure you want to complete this goal?")) {
                try {
                    goalService.completeGoal(goal.getId());
                } catch (EntityNotFoundException | MoinexException.IncompleteGoalException e) {
                    WindowUtils.showErrorDialog("Error completing goal", e.getMessage());
                    return;
                }

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        }
    }

    @FXML
    private void handleArchiveGoal() {
        if (goal.isArchived()) {
            if (WindowUtils.showConfirmationDialog(
                    "Unarchive goal " + goal.getName(),
                    "Are you sure you want to unarchive this goal?")) {
                goalService.unarchiveGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        } else {
            if (WindowUtils.showConfirmationDialog(
                    "Archive goal " + goal.getName(),
                    "Are you sure you want to archive this goal?")) {
                goalService.archiveGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        }
    }

    @FXML
    private void handleDeleteGoal() {
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

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        } catch (EntityNotFoundException | IllegalStateException e) {
            WindowUtils.showErrorDialog("Error deleting goal", e.getMessage());
        }
    }

    private void setDefaultValues() {
        goalName.setText("");
        goalMotivation.setText("");
        goalIcon.setImage(null);

        goalTargetDate.setText("YY-MM-DD");
        missingDays.setText("0");

        setLabelValue(goalTargetAmount, BigDecimal.ZERO);
        setLabelValue(goalIdealAMountPerMonth, BigDecimal.ZERO);
    }

    /**
     * Set the value of a label
     * @param valueLabel Label to set the value
     * @param value Value to set
     */
    private void setLabelValue(Label valueLabel, BigDecimal value) {
        valueLabel.setText(UIUtils.formatCurrency(value));
        UIUtils.setLabelStyle(valueLabel, Constants.NEUTRAL_BALANCE_STYLE);
    }
}
