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
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
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

    private I18nService i18nService;

    @Autowired
    public AddCategoryController(CategoryService categoryService, I18nService i18nService) {
        this.categoryService = categoryService;
        this.i18nService = i18nService;
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
                    i18nService.tr(Constants.TranslationKeys.CATEGORY_DIALOG_CATEGORY_ADDED_TITLE),
                    MessageFormat.format(
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .CATEGORY_DIALOG_CATEGORY_ADDED_MESSAGE),
                            name));

            Stage stage = (Stage) categoryNameField.getScene().getWindow();
            stage.close();
        } catch (IllegalArgumentException | EntityExistsException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
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
