/*
 * Filename: AddBudgetGroupController.kt (original filename: AddBudgetGroupController.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.financialplanning

import javafx.fxml.FXML
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.ui.dialog.financialplanning.base.BaseBudgetGroupController
import org.springframework.stereotype.Controller

@Controller
class AddBudgetGroupController(
    categoryService: CategoryService,
    preferencesService: PreferencesService,
) : BaseBudgetGroupController(categoryService, preferencesService) {
    @FXML
    override fun initialize() {
        super.initialize()
    }
}
