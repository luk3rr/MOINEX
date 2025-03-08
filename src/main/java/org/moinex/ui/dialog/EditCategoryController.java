/*
 * Filename: EditCategoryController.java
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.services.CategoryService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityExistsException;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Category dialog
 */
@Controller
@NoArgsConstructor
public class EditCategoryController
{
    @FXML
    private Label selectedCategoryLabel;

    @FXML
    private CheckBox archivedCheckBox;

    @FXML
    private TextField categoryNewNameField;

    private Category selectedCategory; // The category to be edited

    private CategoryService categoryService;

    @Autowired
    public EditCategoryController(CategoryService categoryService)
    {
        this.categoryService = categoryService;
    }

    @FXML
    public void initialize()
    { }

    public void setCategory(Category ct)
    {
        selectedCategoryLabel.setText(ct.getName());
        selectedCategory = ct;

        archivedCheckBox.setSelected(ct.isArchived());
    }

    @FXML
    private void handleSave()
    {
        String newName = categoryNewNameField.getText();

        boolean archived = archivedCheckBox.isSelected();

        boolean nameChanged     = false;
        boolean archivedChanged = false;

        if (newName == null ||
            !newName.isBlank() && !newName.equals(selectedCategory.getName()))
        {
            try
            {
                categoryService.renameCategory(selectedCategory.getId(), newName);

                nameChanged = true;
            }
            catch (IllegalArgumentException | EntityExistsException |
                   EntityNotFoundException e)
            {
                WindowUtils.showErrorDialog("Error",
                                            "Error updating category name",
                                            e.getMessage());
                return;
            }
        }

        if (archived && !selectedCategory.isArchived())
        {
            try
            {
                categoryService.archiveCategory(selectedCategory.getId());

                archivedChanged = true;
            }
            catch (EntityNotFoundException e)
            {
                WindowUtils.showErrorDialog("Error",
                                            "Error updating category",
                                            e.getMessage());
                return;
            }
        }
        else if (!archived && selectedCategory.isArchived())
        {
            try
            {
                categoryService.unarchiveCategory(selectedCategory.getId());

                archivedChanged = true;
            }
            catch (EntityNotFoundException e)
            {
                WindowUtils.showErrorDialog("Error",
                                            "Error updating category",
                                            e.getMessage());
                return;
            }
        }

        if (nameChanged || archivedChanged)
        {
            String msg = nameChanged && archivedChanged
                             ? "Category name and archived status updated"
                         : archivedChanged ? "Category archived status updated"
                                           : "Category name updated";

            WindowUtils.showSuccessDialog("Success", "Category updated", msg);
        }

        Stage stage = (Stage)categoryNewNameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)categoryNewNameField.getScene().getWindow();
        stage.close();
    }
}
