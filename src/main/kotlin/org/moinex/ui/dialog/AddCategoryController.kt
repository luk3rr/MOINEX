/*
 * Filename: AddCategoryController.kt (original filename: AddCategoryController.java)
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Controller

@Controller
class AddCategoryController(
    private val categoryService: CategoryService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var categoryNameField: TextField

    @FXML
    private fun initialize() {
        // Still empty
    }

    @FXML
    private fun handleSave() {
        val name = categoryNameField.text

        runCatching {
            categoryService.createCategory(Category(name = name, isArchived = false))

            (categoryNameField.scene.window as Stage).close()
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_ERROR_ADDING_CATEGORY_TITLE),
                e.message ?: "Unknown error",
            )
        }
    }

    @FXML
    private fun handleCancel() {
        (categoryNameField.scene.window as Stage).close()
    }
}
