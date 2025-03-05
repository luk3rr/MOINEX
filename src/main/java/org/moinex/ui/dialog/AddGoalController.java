/*
 * Filename: AddGoalController.java
 * Created on: December 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.services.GoalService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Goal dialog
 */
@Controller
@NoArgsConstructor
public class AddGoalController
{
    @FXML
    private TextField nameField;

    @FXML
    private TextField initialBalanceField;

    @FXML
    private TextField targetBalanceField;

    @FXML
    private DatePicker targetDatePicker;

    @FXML
    private TextArea motivationTextArea;

    private GoalService goalService;

    /**
     * Constructor
     * @param goalService GoalService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddGoalController(GoalService goalService)
    {
        this.goalService = goalService;
    }

    @FXML
    private void initialize()
    {
        UIUtils.setDatePickerFormat(targetDatePicker);

        // Ensure that the balance fields only accept monetary values
        initialBalanceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    initialBalanceField.setText(oldValue);
                }
            });

        targetBalanceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    targetBalanceField.setText(oldValue);
                }
            });
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)nameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
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
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all required fields.");

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

            WindowUtils.showSuccessDialog("Success",
                                          "Goal created",
                                          "The goal was successfully created.");

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid balance",
                                        "Please enter a valid balance.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.showErrorDialog("Error", "Error creating goal", e.getMessage());
        }
    }
}
