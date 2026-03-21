/*
 * Filename: BaseBudgetGroupController.kt (original filename: BaseBudgetGroupController.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.financialplanning.base

import com.jfoenix.controls.JFXButton
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.util.StringConverter
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.enums.BudgetGroupTransactionFilter
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import java.util.function.Consumer

abstract class BaseBudgetGroupController(
    protected val categoryService: CategoryService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var groupNameField: TextField

    @FXML
    protected lateinit var targetPercentageField: TextField

    @FXML
    protected lateinit var transactionTypeFilterComboBox: ComboBox<BudgetGroupTransactionFilter>

    @FXML
    protected lateinit var availableCategoriesListView: ListView<Category>

    @FXML
    protected lateinit var selectedCategoriesListView: ListView<Category>

    @FXML
    protected lateinit var addCategoryButton: JFXButton

    @FXML
    protected lateinit var removeCategoryButton: JFXButton

    protected var onSaveCallback: Consumer<BudgetGroup>? = null

    var assignedCategories: Set<Category> = emptySet()
        set(value) {
            field = value
            populateAvailableCategories()
        }

    fun setOnSave(callback: Consumer<BudgetGroup>) {
        onSaveCallback = callback
    }

    protected fun populateAvailableCategories() {
        val allCategories = categoryService.getNonArchivedCategoriesOrderedByName()

        val availableCategories = allCategories.filter { category -> category !in assignedCategories }

        availableCategoriesListView.setCellFactory { UIUtils.createListCell { it.name } }
        selectedCategoriesListView.setCellFactory { UIUtils.createListCell { it.name } }

        availableCategoriesListView.items.setAll(availableCategories)
    }

    @FXML
    open fun initialize() {
        setupButtonActions()
        configureListeners()
        setupTransactionTypeFilterComboBox()
    }

    protected fun setupButtonActions() {
        addCategoryButton.setOnAction {
            moveCategory(availableCategoriesListView, selectedCategoriesListView)
        }

        removeCategoryButton.setOnAction {
            moveCategory(selectedCategoriesListView, availableCategoriesListView)
        }

        availableCategoriesListView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            addCategoryButton.isDisable = newValue == null
        }

        selectedCategoriesListView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            removeCategoryButton.isDisable = newValue == null
        }

        addCategoryButton.isDisable = availableCategoriesListView.items.isEmpty()
        removeCategoryButton.isDisable = selectedCategoriesListView.items.isEmpty()
    }

    protected fun configureListeners() {
        targetPercentageField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.PERCENTAGE_REGEX))) {
                targetPercentageField.text = oldValue
            }
        }
    }

    protected fun setupTransactionTypeFilterComboBox() {
        transactionTypeFilterComboBox.items.addAll(BudgetGroupTransactionFilter.entries)
        transactionTypeFilterComboBox.value = BudgetGroupTransactionFilter.EXPENSE
        transactionTypeFilterComboBox.converter =
            object : StringConverter<BudgetGroupTransactionFilter>() {
                override fun toString(filter: BudgetGroupTransactionFilter?): String {
                    if (filter == null) return ""
                    return when (filter) {
                        BudgetGroupTransactionFilter.INCOME ->
                            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_FILTER_INCOME)
                        BudgetGroupTransactionFilter.EXPENSE ->
                            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_FILTER_EXPENSE)
                        BudgetGroupTransactionFilter.BOTH ->
                            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_FILTER_BOTH)
                    }
                }

                override fun fromString(string: String?): BudgetGroupTransactionFilter? {
                    if (string.isNullOrEmpty()) return null

                    val income = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_FILTER_INCOME)
                    val expense = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_FILTER_EXPENSE)
                    val both = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_FILTER_BOTH)

                    return when (string) {
                        income -> BudgetGroupTransactionFilter.INCOME
                        expense -> BudgetGroupTransactionFilter.EXPENSE
                        both -> BudgetGroupTransactionFilter.BOTH
                        else -> null
                    }
                }
            }
    }

    protected fun moveCategory(
        source: ListView<Category>,
        destination: ListView<Category>,
    ) {
        val selectedItems = source.selectionModel.selectedItems.toList()

        if (selectedItems.isEmpty()) {
            return
        }

        selectedItems.forEach { category ->
            source.items.remove(category)
            destination.items.add(category)
        }

        destination.items.sortBy { it.name.lowercase() }
    }

    @FXML
    protected fun handleCancel() {
        groupNameField.scene.window.hide()
    }

    @FXML
    protected fun handleSave() {
        val groupName = groupNameField.text.trim()
        val targetPercentageText = targetPercentageField.text.trim()

        if (groupName.isEmpty() || targetPercentageText.isEmpty()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_REQUIRED_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_REQUIRED_FIELDS_MESSAGE),
            )
            return
        }

        val targetPercentage =
            runCatching {
                targetPercentageText.toBigDecimal()
            }.getOrElse {
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_INPUT_TITLE),
                    preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_INPUT_MESSAGE),
                )
                return
            }

        val selectedCategories = selectedCategoriesListView.items.toMutableSet()
        val filter = transactionTypeFilterComboBox.value

        val budgetGroup =
            BudgetGroup(
                name = groupName,
                targetPercentage = targetPercentage,
                categories = selectedCategories,
                transactionTypeFilter = filter,
            )

        onSaveCallback?.accept(budgetGroup)
        handleCancel()
    }
}
