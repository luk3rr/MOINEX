/*
 * Filename: AddCategoryController.java
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityExistsException;
import java.text.MessageFormat;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.service.CategoryService;
import org.moinex.service.PreferencesService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/** Controller for the Add Category dialog */
@Controller
@NoArgsConstructor
public class AddCategoryController {
    @FXML private TextField categoryNameField;

    private CategoryService categoryService;

    private PreferencesService preferencesService;

    @Autowired
    public AddCategoryController(
            CategoryService categoryService, PreferencesService preferencesService) {
        this.categoryService = categoryService;
        this.preferencesService = preferencesService;
    }

    @FXML
    private void initialize() {
        // Still empty
    }

    @FXML
    public void handleSave() {
        String name = categoryNameField.getText();

        try {
            categoryService.createCategory(new Category(null, name, false));

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CATEGORY_DIALOG_CATEGORY_ADDED_TITLE),
                    MessageFormat.format(
                            preferencesService.translate(
                                    Constants.TranslationKeys
                                            .CATEGORY_DIALOG_CATEGORY_ADDED_MESSAGE),
                            name));

            Stage stage = (Stage) categoryNameField.getScene().getWindow();
            stage.close();
        } catch (IllegalArgumentException | EntityExistsException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.CATEGORY_DIALOG_ERROR_ADDING_CATEGORY_TITLE),
                    e.getMessage());
        }
    }

    @FXML
    public void handleCancel() {
        Stage stage = (Stage) categoryNameField.getScene().getWindow();
        stage.close();
    }
}
