package org.moinex.ui.dialog.financialplanning;

import javafx.fxml.FXML;
import lombok.NoArgsConstructor;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Budget Group dialog
 */
@Controller
@NoArgsConstructor
public class EditBudgetGroupController extends BaseBudgetGroupController {

    @Autowired
    public EditBudgetGroupController(CategoryService categoryService) {
        super(categoryService);
    }

    public void setGroup(BudgetGroup group) {
        groupNameField.setText(group.getName());
        targetPercentageField.setText(group.getTargetPercentage().toString());

        selectedCategoriesListView.getItems().setAll(group.getCategories());

        populateAvailableCategories();

        availableCategoriesListView.getItems().removeAll(group.getCategories());
    }

    @FXML
    public void initialize() {
        super.initialize();
    }
}
