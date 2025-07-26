/*
 * Filename: EditGoalController.java
 * Created on: December 14, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.goal.Goal;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.GoalService;
import org.moinex.service.WalletService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Goal dialog
 */
@Controller
@NoArgsConstructor
public final class EditGoalController extends BaseGoalManagement {
    @FXML private CheckBox archivedCheckBox;

    @FXML private CheckBox completedCheckBox;

    private Goal goal = null;

    /**
     * Constructor
     *
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditGoalController(GoalService goalService, WalletService walletService) {
        super(goalService, walletService);
    }

    public void setGoal(Goal goal) {
        this.goal = goal;

        nameField.setText(goal.getName());
        balanceField.setText(goal.getBalance().toString());
        targetBalanceField.setText(goal.getTargetBalance().toString());
        targetDatePicker.setValue(goal.getTargetDate().toLocalDate());
        motivationTextArea.setText(goal.getMotivation());
        archivedCheckBox.setSelected(goal.isArchived());
        completedCheckBox.setSelected(goal.isCompleted());
        masterWalletComboBox.setValue(goal.getMasterWallet());
    }

    @Override
    @FXML
    protected void initialize() {
        super.initialize();
        setupDisableMasterWalletComboBox();
    }

    @Override
    @FXML
    protected void handleSave() {
        String goalName = nameField.getText();
        goalName = goalName.strip(); // Remove leading and trailing whitespaces

        String balanceStr = balanceField.getText();
        String targetBalanceStr = targetBalanceField.getText();
        LocalDate targetDate = targetDatePicker.getValue();
        String motivation = motivationTextArea.getText();
        boolean archived = archivedCheckBox.isSelected();
        boolean completed = completedCheckBox.isSelected();

        Wallet masterWallet = masterWalletComboBox.getValue();

        if (goalName.isEmpty() || targetBalanceStr.isEmpty() || targetDate == null) {
            WindowUtils.showInformationDialog(
                    "Empty fields", "Please fill all required fields before saving");

            return;
        }

        try {
            BigDecimal currentBalance = new BigDecimal(balanceStr.isEmpty() ? "0" : balanceStr);
            BigDecimal targetBalance = new BigDecimal(targetBalanceStr);

            // Check if it has any modification
            if (goal.getName().equals(goalName)
                    && goal.getBalance().equals(currentBalance)
                    && (goal.isVirtual() && goal.getMasterWallet().equals(masterWallet))
                    && goal.getTargetBalance().equals(targetBalance)
                    && goal.getTargetDate().toLocalDate().equals(targetDate)
                    && goal.getMotivation().equals(motivation)
                    && goal.isArchived() == archived
                    && goal.isCompleted() == completed) {
                WindowUtils.showInformationDialog(
                        "No changes", "No changes were made to the goal.");
            } else // If there is any modification, update the goal
            {
                goal.setName(goalName);
                goal.setBalance(currentBalance);
                goal.setTargetBalance(targetBalance);
                goal.setTargetDate(targetDate.atStartOfDay());
                goal.setMotivation(motivation);
                goal.setArchived(archived);
                goal.setMasterWallet(masterWallet);

                // If the goal was completed and the user unchecked the completed
                // checkbox, set the completion date to null, otherwise set the
                // completion date to the current date, This is necessary for UpdateGoal
                // identify if the completed field was changed
                if (completed && !goal.isCompleted()) {
                    goal.setCompletionDate(LocalDate.now().atStartOfDay());
                } else {
                    goal.setCompletionDate(null);
                }

                goalService.updateGoal(goal);

                WindowUtils.showSuccessDialog("Goal updated", "The goal was successfully updated.");
            }

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog("Invalid balance", "Please enter a valid balance.");
        } catch (EntityNotFoundException
                | IllegalArgumentException
                | EntityExistsException
                | MoinexException.IncompleteGoalException e) {
            WindowUtils.showErrorDialog("Error creating goal", e.getMessage());
        }
    }

    private void setupDisableMasterWalletComboBox() {
        archivedCheckBox
                .selectedProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            masterWalletComboBox.setValue(null);
                            masterWalletComboBox.setDisable(
                                    newValue || completedCheckBox.isSelected());
                        });

        completedCheckBox
                .selectedProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            masterWalletComboBox.setValue(null);
                            masterWalletComboBox.setDisable(
                                    newValue || archivedCheckBox.isSelected());
                        });
    }
}
