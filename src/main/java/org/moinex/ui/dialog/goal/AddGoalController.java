/*
 * Filename: AddGoalController.java
 * Created on: December 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.services.GoalService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Goal dialog
 */
@Controller
@NoArgsConstructor
public final class AddGoalController extends BaseGoalManagement
{
    /**
     * Constructor
     * @param goalService GoalService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddGoalController(GoalService goalService)
    {
        super(goalService);
    }

    @Override
    @FXML
    protected void handleSave()
    {
        String goalName = nameField.getText();
        goalName        = goalName.strip(); // Remove leading and trailing whitespaces

        String    initialBalanceStr = initialBalanceField.getText();
        String    targetBalanceStr  = targetBalanceField.getText();
        LocalDate targetDate        = targetDatePicker.getValue();
        String    motivation        = motivationTextArea.getText();

        if (goalName.isEmpty() || initialBalanceStr.isEmpty() ||
            targetBalanceStr.isEmpty() || targetDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        try
        {
            BigDecimal initialBalance = new BigDecimal(initialBalanceStr);
            BigDecimal targetBalance  = new BigDecimal(targetBalanceStr);

            goalService.addGoal(goalName,
                                initialBalance,
                                targetBalance,
                                targetDate,
                                motivation);

            WindowUtils.showSuccessDialog("Goal created",
                                          "The goal was successfully created.");

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid balance",
                                        "Please enter a valid balance.");
        }
        catch (IllegalArgumentException | EntityExistsException |
               EntityNotFoundException e)
        {
            WindowUtils.showErrorDialog("Error creating goal", e.getMessage());
        }
    }
}
