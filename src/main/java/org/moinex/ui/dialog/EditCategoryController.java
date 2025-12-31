/*
 * Filename: EditCategoryController.java
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Category dialog
 */
@Controller
@NoArgsConstructor
public class EditCategoryController {
    @FXML private Label selectedCategoryLabel;

    @FXML private CheckBox archivedCheckBox;

    @FXML private TextField categoryNewNameField;

    private Category selectedCategory; // The category to be edited

    private CategoryService categoryService;

    private I18nService i18nService;

    @Autowired
    public EditCategoryController(CategoryService categoryService, I18nService i18nService) {
        this.categoryService = categoryService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        // Still empty
    }

    public void setCategory(Category ct) {
        selectedCategoryLabel.setText(ct.getName());
        selectedCategory = ct;

        archivedCheckBox.setSelected(ct.isArchived());
    }

    @FXML
    private void handleSave() {
        String newName = categoryNewNameField.getText();

        boolean archived = archivedCheckBox.isSelected();

        boolean nameChanged = false;
        boolean archivedChanged = false;

        if (newName == null || !newName.isBlank() && !newName.equals(selectedCategory.getName())) {
            try {
                categoryService.renameCategory(selectedCategory.getId(), newName);

                nameChanged = true;
            } catch (IllegalArgumentException | EntityExistsException | EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_NAME_TITLE),
                        e.getMessage());
                return;
            }
        }

        if (archived && !selectedCategory.isArchived()) {
            try {
                categoryService.archiveCategory(selectedCategory.getId());

                archivedChanged = true;
            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_TITLE),
                        e.getMessage());
                return;
            }
        } else if (!archived && selectedCategory.isArchived()) {
            try {
                categoryService.unarchiveCategory(selectedCategory.getId());

                archivedChanged = true;
            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_TITLE),
                        e.getMessage());
                return;
            }
        }

        if (nameChanged || archivedChanged) {
            String msg;
            if (nameChanged && archivedChanged) {
                msg =
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_CATEGORY_NAME_AND_ARCHIVED_UPDATED_MESSAGE);
            } else if (archivedChanged) {
                msg =
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_CATEGORY_ARCHIVED_UPDATED_MESSAGE);
            } else {
                msg =
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_CATEGORY_NAME_UPDATED_MESSAGE);
            }

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CATEGORY_DIALOG_CATEGORY_UPDATED_TITLE),
                    msg);
        }

        Stage stage = (Stage) categoryNewNameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) categoryNewNameField.getScene().getWindow();
        stage.close();
    }
}
