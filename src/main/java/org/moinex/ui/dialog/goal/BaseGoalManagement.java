/*
 * Filename: BaseGoalManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.service.GoalService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class to implement the common behavior of the Add and Edit Goal
 */
@NoArgsConstructor
public abstract class BaseGoalManagement {
    @FXML protected TextField nameField;

    @FXML protected TextField balanceField;

    @FXML protected TextField targetBalanceField;

    @FXML protected DatePicker targetDatePicker;

    @FXML protected TextArea motivationTextArea;

    protected GoalService goalService;

    /**
     * Constructor
     * @param goalService GoalService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseGoalManagement(GoalService goalService) {
        this.goalService = goalService;
    }

    @FXML
    protected void initialize() {
        UIUtils.setDatePickerFormat(targetDatePicker);

        // Ensure that the balance fields only accept monetary values
        balanceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                balanceField.setText(oldValue);
                            }
                        });

        targetBalanceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                targetBalanceField.setText(oldValue);
                            }
                        });
    }

    @FXML
    protected abstract void handleSave();

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
