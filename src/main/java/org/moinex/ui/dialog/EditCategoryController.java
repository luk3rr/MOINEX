/*
 * Filename: EditCategoryController.java
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.Category;
import org.moinex.services.CategoryService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Category dialog
 */
@Controller
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

    public void SetCategory(Category ct)
    {
        selectedCategoryLabel.setText(ct.GetName());
        selectedCategory = ct;

        archivedCheckBox.setSelected(ct.IsArchived());
    }

    @FXML
    private void handleSave()
    {
        String newName = categoryNewNameField.getText();

        Boolean archived = archivedCheckBox.isSelected();

        Boolean nameChanged     = false;
        Boolean archivedChanged = false;

        if (newName == null ||
            !newName.isBlank() && !newName.equals(selectedCategory.GetName()))
        {
            try
            {
                categoryService.RenameCategory(selectedCategory.GetId(), newName);

                nameChanged = true;
            }
            catch (RuntimeException e)
            {
                WindowUtils.ShowErrorDialog("Error",
                                            "Error updating category name",
                                            e.getMessage());
                return;
            }
        }

        if (archived && !selectedCategory.IsArchived())
        {
            try
            {
                categoryService.ArchiveCategory(selectedCategory.GetId());

                archivedChanged = true;
            }
            catch (RuntimeException e)
            {
                WindowUtils.ShowErrorDialog("Error",
                                            "Error updating category",
                                            e.getMessage());
                return;
            }
        }
        else if (!archived && selectedCategory.IsArchived())
        {
            try
            {
                categoryService.UnarchiveCategory(selectedCategory.GetId());

                archivedChanged = true;
            }
            catch (RuntimeException e)
            {
                WindowUtils.ShowErrorDialog("Error",
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

            WindowUtils.ShowSuccessDialog("Success", "Category updated", msg);
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
