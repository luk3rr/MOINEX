/*
 * Filename: ManageCategoryController.kt (original filename: ManageCategoryController.java)
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class ManageCategoryController(
    private val categoryService: CategoryService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var categoryTableView: TableView<Category>

    @FXML
    private lateinit var searchField: TextField

    private var categories: List<Category> = emptyList()

    companion object {
        private const val YES = "yes"
        private const val NO = "no"
    }

    @FXML
    private fun initialize() {
        loadCategoryFromDatabase()
        configureTableView()
        updateCategoryTableView()

        searchField.textProperty().addListener { _, _, _ -> updateCategoryTableView() }
    }

    @FXML
    private fun handleCreate() {
        WindowUtils.openModalWindow(
            Constants.ADD_CATEGORY_FXML,
            preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_ADD_CATEGORY_TITLE),
            springContext,
            { _: AddCategoryController -> },
            listOf(
                Runnable {
                    loadCategoryFromDatabase()
                    updateCategoryTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleEdit() {
        val selectedCategory = categoryTableView.selectionModel.selectedItem

        if (selectedCategory == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_NO_CATEGORY_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_NO_CATEGORY_SELECTED_EDIT_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Constants.EDIT_CATEGORY_FXML,
            preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_EDIT_CATEGORY_TITLE),
            springContext,
            { controller: EditCategoryController -> controller.setCategory(selectedCategory) },
            listOf(
                Runnable {
                    loadCategoryFromDatabase()
                    updateCategoryTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleDelete() {
        val selectedCategory = categoryTableView.selectionModel.selectedItem

        if (selectedCategory == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_NO_CATEGORY_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_NO_CATEGORY_SELECTED_REMOVE_MESSAGE),
            )
            return
        }

        if (categoryService.getTransactionCountByCategory(selectedCategory.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_HAS_TRANSACTIONS_TITLE),
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_HAS_TRANSACTIONS_MESSAGE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_REMOVE_CATEGORY_TITLE) + " " +
                    selectedCategory.name,
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_REMOVE_CATEGORY_MESSAGE),
            )
        ) {
            runCatching {
                categoryService.deleteCategory(selectedCategory.id!!)
                loadCategoryFromDatabase()
                updateCategoryTableView()
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_ERROR_REMOVING_CATEGORY_TITLE),
                    e.message ?: "Unknown error",
                )
            }
        }
    }

    @FXML
    private fun handleCancel() {
        (searchField.scene.window as Stage).close()
    }

    private fun loadCategoryFromDatabase() {
        categories = categoryService.getCategories()
    }

    private fun updateCategoryTableView() {
        val similarTextOrId = searchField.text.lowercase()

        categoryTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            categoryTableView.items.setAll(categories)
        } else {
            categories
                .filter { c ->
                    val name = c.name.lowercase()
                    val id = c.id.toString()
                    val archived = if (c.isArchived) YES else NO

                    name.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        archived.contains(similarTextOrId)
                }.forEach { categoryTableView.items.add(it) }
        }

        categoryTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn = createIdColumn()
        val categoryColumn = createCategoryColumn()
        val archivedColumn = createArchivedColumn()
        val numOfTransactionsColumn = createTransactionsColumn()

        categoryTableView.columns.addAll(idColumn, categoryColumn, archivedColumn, numOfTransactionsColumn)
    }

    private fun createIdColumn(): TableColumn<Category, Int> =
        TableColumn<Category, Int>(
            preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_ID),
        ).apply {
            setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createCategoryColumn(): TableColumn<Category, String> =
        TableColumn<Category, String>(
            preferencesService.translate(TranslationKeys.CATEGORY_TABLE_CATEGORY),
        ).apply {
            setCellValueFactory { param -> SimpleStringProperty(param.value.name) }
        }

    private fun createArchivedColumn(): TableColumn<Category, String> =
        TableColumn<Category, String>(
            preferencesService.translate(TranslationKeys.CATEGORY_TABLE_ARCHIVED),
        ).apply {
            setCellValueFactory { param ->
                SimpleStringProperty(
                    if (param.value.isArchived) {
                        preferencesService.translate(TranslationKeys.CATEGORY_TABLE_YES)
                    } else {
                        preferencesService.translate(TranslationKeys.CATEGORY_TABLE_NO)
                    },
                )
            }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createTransactionsColumn(): TableColumn<Category, Int> =
        TableColumn<Category, Int>(
            preferencesService.translate(TranslationKeys.CATEGORY_TABLE_ASSOCIATED_TRANSACTIONS),
        ).apply {
            setCellValueFactory { param ->
                SimpleObjectProperty(categoryService.getTransactionCountByCategory(param.value.id!!))
            }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }
}
