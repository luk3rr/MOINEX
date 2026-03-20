/*
 * Filename: EditCategoryController.kt (original filename: EditCategoryController.java)
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog

import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Controller

@Controller
class EditCategoryController(
    private val categoryService: CategoryService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var selectedCategoryLabel: Label

    @FXML
    private lateinit var archivedCheckBox: CheckBox

    @FXML
    private lateinit var categoryNewNameField: TextField

    private lateinit var selectedCategory: Category

    companion object {
        private const val DEFAULT_UNKNOWN_ERROR_MESSAGE = "Unknown error"
    }

    @FXML
    private fun initialize() {
        // Still empty
    }

    fun setCategory(ct: Category) {
        selectedCategoryLabel.text = ct.name
        selectedCategory = ct
        archivedCheckBox.isSelected = ct.isArchived
    }

    @FXML
    private fun handleSave() {
        val newName = categoryNewNameField.text
        val archived = archivedCheckBox.isSelected

        var nameChanged = false
        var archivedChanged = false

        if (newName != null && newName.isNotBlank() && newName != selectedCategory.name) {
            runCatching {
                selectedCategory.name = newName
                categoryService.renameCategory(selectedCategory)
                nameChanged = true
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_NAME_TITLE),
                    e.message ?: DEFAULT_UNKNOWN_ERROR_MESSAGE,
                )
                return
            }
        }

        when {
            archived && !selectedCategory.isArchived -> {
                runCatching {
                    categoryService.archiveCategory(selectedCategory.id!!)
                    archivedChanged = true
                }.onFailure { e ->
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_TITLE),
                        e.message ?: DEFAULT_UNKNOWN_ERROR_MESSAGE,
                    )
                    return
                }
            }
            !archived && selectedCategory.isArchived -> {
                runCatching {
                    categoryService.unarchiveCategory(selectedCategory.id!!)
                    archivedChanged = true
                }.onFailure { e ->
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_ERROR_UPDATING_CATEGORY_TITLE),
                        e.message ?: DEFAULT_UNKNOWN_ERROR_MESSAGE,
                    )
                    return
                }
            }
        }

        if (nameChanged || archivedChanged) {
            val msg =
                when {
                    nameChanged && archivedChanged ->
                        preferencesService.translate(
                            TranslationKeys.CATEGORY_DIALOG_CATEGORY_NAME_AND_ARCHIVED_UPDATED_MESSAGE,
                        )
                    archivedChanged ->
                        preferencesService.translate(
                            TranslationKeys.CATEGORY_DIALOG_CATEGORY_ARCHIVED_UPDATED_MESSAGE,
                        )
                    else ->
                        preferencesService.translate(
                            TranslationKeys.CATEGORY_DIALOG_CATEGORY_NAME_UPDATED_MESSAGE,
                        )
                }

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_UPDATED_TITLE),
                msg,
            )
        }

        (categoryNewNameField.scene.window as Stage).close()
    }

    @FXML
    private fun handleCancel() {
        (categoryNewNameField.scene.window as Stage).close()
    }
}
