/*
 * Filename: GoalFullPaneController.java
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
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
import org.moinex.service.PreferencesService;
import org.moinex.service.goal.GoalService;
import org.moinex.service.wallet.WalletService;
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

    private WalletService walletService;

    private PreferencesService preferencesService;

    private Goal goal;

    /**
     * Constructor
     *
     * @param goalService Goal service
     * @param goalService Goal service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public GoalFullPaneController(
            GoalService goalService,
            ConfigurableApplicationContext springContext,
            GoalController goalController,
            WalletService walletService,
            PreferencesService preferencesService) {
        this.goalService = goalService;
        this.springContext = springContext;
        this.goalController = goalController;
        this.walletService = walletService;
        this.preferencesService = preferencesService;
    }

    /** Load goal information from the database */
    public void loadGoalInfo() {
        if (goal == null) {
            return;
        }

        // Reload goal from the database
        goal = goalService.getGoalById(goal.getId());
    }

    /**
     * Update the goal pane with the given goal
     *
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
        goalTargetDate.setText(
                UIUtils.formatDateForDisplay(goal.getTargetDate(), preferencesService));

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
        progressBar.setI18nService(preferencesService);

        double percentage;

        if (goal.isArchived()) {
            // Set the button text according to the goal status
            toggleArchiveGoal.setText(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_UNARCHIVE_GOAL));
        }

        if (goal.isCompleted()) {
            dateTitleLabel.setText(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_COMPLETION_DATE));
            goalTargetDate.setText(
                    UIUtils.formatDateForDisplay(goal.getCompletionDate(), preferencesService));

            daysTitleLabel.setText(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_MISSING_DAYS));
            missingDays.setText(
                    String.valueOf(
                            Constants.calculateDaysUntilTarget(
                                    goal.getCompletionDate(), goal.getTargetDate())));

            // Remove the fields that are not necessary
            infosVBox.getChildren().remove(currentHBox);
            infosVBox.getChildren().remove(idealPerMonthHBox);

            percentage = 100.0;

            // Set the button text according to the goal status
            toggleCompleteGoal.setText(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_UNCOMPLETE_GOAL));
        } else {
            // Show the current amount
            goalCurrentAmount.setText(UIUtils.formatCurrency(goal.getBalance()));

            // Calculate the number of months until the target date
            Integer monthsUntilTarget =
                    Constants.calculateMonthsUntilTarget(LocalDate.now(), goal.getTargetDate());

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
                                                        LocalDate.now(), goal.getTargetDate())),
                                        2,
                                        RoundingMode.HALF_UP);
            }

            goalIdealAMountPerMonth.setText(UIUtils.formatCurrency(idealAmountPerMonth));

            // Calculate the missing days
            Integer missingDaysValue =
                    Constants.calculateDaysUntilTarget(LocalDate.now(), goal.getTargetDate());

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
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_INCOME));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_INCOME_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.COMMON_GOAL_MODAL_ADD_INCOME),
                springContext,
                (AddIncomeController controller) -> controller.setWalletComboBox(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleAddExpense() {
        if (goal.isArchived()) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_EXPENSE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_EXPENSE_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.COMMON_GOAL_MODAL_ADD_EXPENSE),
                springContext,
                (AddExpenseController controller) -> controller.setWalletComboBox(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleAddTransfer() {
        if (goal.isArchived()) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .COMMON_GOAL_DIALOG_ARCHIVED_CANNOT_ADD_TRANSFER));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.ADD_TRANSFER_FXML,
                preferencesService.translate(
                        Constants.TranslationKeys.COMMON_GOAL_MODAL_ADD_TRANSFER),
                springContext,
                (AddTransferController controller) -> controller.setReceiverWalletComboBox(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleEditGoal() {
        WindowUtils.openModalWindow(
                Constants.EDIT_GOAL_FXML,
                preferencesService.translate(Constants.TranslationKeys.COMMON_GOAL_MODAL_EDIT_GOAL),
                springContext,
                (EditGoalController controller) -> controller.setGoal(goal),
                List.of(() -> goalController.updateDisplay()));
    }

    @FXML
    private void handleCompleteGoal() {
        if (goal.isCompleted()) {
            if (WindowUtils.showConfirmationDialog(
                    MessageFormat.format(
                            preferencesService.translate(
                                    Constants.TranslationKeys.COMMON_GOAL_DIALOG_REOPEN_TITLE),
                            goal.getName()),
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_REOPEN_MESSAGE),
                    preferencesService.getBundle())) {
                goalService.reopenGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        } else {
            if (WindowUtils.showConfirmationDialog(
                    MessageFormat.format(
                            preferencesService.translate(
                                    Constants.TranslationKeys.COMMON_GOAL_DIALOG_COMPLETE_TITLE),
                            goal.getName()),
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_COMPLETE_MESSAGE),
                    preferencesService.getBundle())) {
                try {
                    goalService.completeGoal(goal.getId());
                } catch (EntityNotFoundException | MoinexException.IncompleteGoalException e) {
                    WindowUtils.showErrorDialog(
                            preferencesService.translate(
                                    Constants.TranslationKeys.COMMON_GOAL_DIALOG_COMPLETE_ERROR),
                            e.getMessage());
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
                    MessageFormat.format(
                            preferencesService.translate(
                                    Constants.TranslationKeys.COMMON_GOAL_DIALOG_UNARCHIVE_TITLE),
                            goal.getName()),
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_UNARCHIVE_MESSAGE),
                    preferencesService.getBundle())) {
                goalService.unarchiveGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        } else {
            if (WindowUtils.showConfirmationDialog(
                    MessageFormat.format(
                            preferencesService.translate(
                                    Constants.TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVE_TITLE),
                            goal.getName()),
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_ARCHIVE_MESSAGE),
                    preferencesService.getBundle())) {
                goalService.archiveGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        }
    }

    @FXML
    private void handleDeleteGoal() {
        // Prevent the removal of a wallet with associated transactions
        if (walletService.getWalletTransactionAndTransferCountByWallet(goal.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .COMMON_GOAL_DIALOG_DELETE_HAS_TRANSACTIONS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .COMMON_GOAL_DIALOG_DELETE_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        // Create a message to show to the user
        String message =
                MessageFormat.format(
                        "{0}\n{1}\n{2}\n{3}\n{4}",
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys.COMMON_GOAL_DIALOG_DELETE_NAME),
                                goal.getName()),
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys
                                                .COMMON_GOAL_DIALOG_DELETE_INITIAL_AMOUNT),
                                UIUtils.formatCurrency(goal.getInitialBalance())),
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys
                                                .COMMON_GOAL_DIALOG_DELETE_CURRENT_AMOUNT),
                                UIUtils.formatCurrency(goal.getBalance())),
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys
                                                .COMMON_GOAL_DIALOG_DELETE_TARGET_AMOUNT),
                                UIUtils.formatCurrency(goal.getTargetBalance())),
                        MessageFormat.format(
                                preferencesService.translate(
                                        Constants.TranslationKeys
                                                .COMMON_GOAL_DIALOG_DELETE_TARGET_DATE),
                                UIUtils.formatDateForDisplay(
                                        goal.getTargetDate(), preferencesService)));

        Integer totalOfAssociatedVirtualWallets =
                walletService.getCountOfVirtualWalletsByMasterWalletId(goal.getId());

        if (!totalOfAssociatedVirtualWallets.equals(0)) {
            String virtualWalletsMessage =
                    "\n"
                            + MessageFormat.format(
                                    preferencesService.translate(
                                            Constants.TranslationKeys
                                                    .COMMON_GOAL_DIALOG_DELETE_VIRTUAL_WALLETS),
                                    totalOfAssociatedVirtualWallets)
                            + "\n";

            message = message + virtualWalletsMessage;
        }

        try {
            // Confirm the deletion
            if (WindowUtils.showConfirmationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_DELETE_TITLE),
                    message,
                    preferencesService.getBundle())) {
                goalService.deleteGoal(goal.getId());

                // Update goal display in the main window
                goalController.updateDisplay();
            }
        } catch (EntityNotFoundException | IllegalStateException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.COMMON_GOAL_DIALOG_DELETE_ERROR),
                    e.getMessage());
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
     *
     * @param valueLabel Label to set the value
     * @param value Value to set
     */
    private void setLabelValue(Label valueLabel, BigDecimal value) {
        valueLabel.setText(UIUtils.formatCurrency(value));
        UIUtils.setLabelStyle(valueLabel, Constants.NEUTRAL_BALANCE_STYLE);
    }
}
