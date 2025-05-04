/*
 * Filename: AddCategoryController.java
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityExistsException;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.service.CategoryService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Category dialog
 */
@Controller
@NoArgsConstructor
public class AddCategoryController {
    @FXML private TextField categoryNameField;

    private CategoryService categoryService;

    @Autowired
    public AddCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @FXML
    private void initialize() {
        // Still empty
    }

    @FXML
    public void handleSave() {
        String name = categoryNameField.getText();

        try {
            categoryService.addCategory(name);

            WindowUtils.showSuccessDialog(
                    "Category added", "Category " + name + " added successfully");

            Stage stage = (Stage) categoryNameField.getScene().getWindow();
            stage.close();
        } catch (IllegalArgumentException | EntityExistsException e) {
            WindowUtils.showErrorDialog("Error adding category", e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        Stage stage = (Stage) categoryNameField.getScene().getWindow();
        stage.close();
    }
}
