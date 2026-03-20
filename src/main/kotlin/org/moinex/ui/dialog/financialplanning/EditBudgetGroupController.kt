/*
 * Filename: EditBudgetGroupController.kt (original filename: EditBudgetGroupController.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.financialplanning

import javafx.fxml.FXML
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.ui.dialog.financialplanning.base.BaseBudgetGroupController
import org.springframework.stereotype.Controller

@Controller
class EditBudgetGroupController(
    categoryService: CategoryService,
    preferencesService: PreferencesService,
) : BaseBudgetGroupController(categoryService, preferencesService) {
    fun setGroup(group: BudgetGroup) {
        groupNameField.text = group.name
        targetPercentageField.text = group.targetPercentage.toString()

        selectedCategoriesListView.items.setAll(group.categories)

        populateAvailableCategories()

        availableCategoriesListView.items.removeAll(group.categories.toSet())

        transactionTypeFilterComboBox.value = group.transactionTypeFilter
    }

    @FXML
    override fun initialize() {
        super.initialize()
    }
}
