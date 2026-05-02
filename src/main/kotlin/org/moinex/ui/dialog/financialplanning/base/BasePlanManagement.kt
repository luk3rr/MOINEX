/*
 * Filename: BasePlanManagement.kt (original filename: BasePlanManagement.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.financialplanning.base

import com.jfoenix.controls.JFXButton
import javafx.beans.value.ChangeListener
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isNotEqual
import org.moinex.common.extension.setAnchorPaneConstraints
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.dto.form.PlanFormDTO
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.ui.common.BudgetGroupPreviewController
import org.moinex.ui.dialog.financialplanning.AddBudgetGroupController
import org.moinex.ui.dialog.financialplanning.EditBudgetGroupController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigDecimal
import java.text.MessageFormat

abstract class BasePlanManagement(
    protected val financialPlanningService: FinancialPlanningService,
    protected val springContext: ConfigurableApplicationContext,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var planNameField: TextField

    @FXML
    protected lateinit var baseIncomeField: TextField

    @FXML
    protected lateinit var startDatePicker: DatePicker

    @FXML
    protected lateinit var budgetGroupInfo: Label

    @FXML
    protected lateinit var pane1: AnchorPane

    @FXML
    protected lateinit var pane2: AnchorPane

    @FXML
    protected lateinit var pane3: AnchorPane

    @FXML
    protected lateinit var prevButton: JFXButton

    @FXML
    protected lateinit var nextButton: JFXButton

    protected var budgetGroups = mutableListOf<BudgetGroup>()
    protected var paneCurrentPage = 0
    private var baseIncomeListener: ChangeListener<String>? = null

    companion object {
        private const val ITEMS_PER_PAGE = 3
        private val logger = LoggerFactory.getLogger(BasePlanManagement::class.java)
    }

    @FXML
    open fun initialize() {
        configureBaseIncomeListener()
        configureButtonsActions()
        UIUtils.setDatePickerFormat(startDatePicker)
        budgetGroupInfo.isVisible = false
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected open fun handleCancel() {
        planNameField.scene.window.hide()
    }

    @FXML
    private fun handleAddBudgetGroup() {
        WindowUtils.openModalWindow(
            Files.ADD_BUDGET_GROUP_FXML,
            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_ADD_BUDGET_GROUP_TITLE),
            springContext,
            { controller: AddBudgetGroupController ->
                controller.assignedCategories = getAssignedCategories()
                controller.setOnSave { newBudgetGroup ->
                    budgetGroups.add(newBudgetGroup)
                    updateBudgetGroupsContainer()
                }
            },
            emptyList(),
        )
    }

    protected fun updateBudgetGroupsContainer() {
        pane1.children.clear()
        pane2.children.clear()
        pane3.children.clear()

        budgetGroups.sortByDescending { it.targetPercentage }

        val start = paneCurrentPage * ITEMS_PER_PAGE
        val end = minOf(start + ITEMS_PER_PAGE, budgetGroups.size)

        for (i in start until end) {
            val budgetGroup = budgetGroups[i]
            runCatching {
                val loader = FXMLLoader(javaClass.getResource(Files.BUDGET_GROUP_PREVIEW_PANE_FXML))
                loader.setControllerFactory { clazz -> springContext.getBean(clazz) }
                val newContent = loader.load<AnchorPane>()
                newContent.stylesheets.add(
                    javaClass.getResource(Files.COMMON_STYLE_SHEET)?.toExternalForm()
                        ?: throw IllegalStateException("CSS not found"),
                )

                val planTotal =
                    if (baseIncomeField.text.isEmpty()) {
                        BigDecimal.ZERO
                    } else {
                        BigDecimal(baseIncomeField.text)
                    }

                val previewController = loader.getController<BudgetGroupPreviewController>()
                previewController.populate(budgetGroup, planTotal)
                springContext.getBean<ThemeService>().applyIconsTo(newContent)

                addContextMenu(newContent, budgetGroup)

                newContent.setAnchorPaneConstraints()

                when (i % ITEMS_PER_PAGE) {
                    0 -> pane1.children.add(newContent)
                    1 -> pane2.children.add(newContent)
                    2 -> pane3.children.add(newContent)
                    else -> logger.warn("Invalid index: {}", i)
                }
            }.onFailure { e ->
                logger.error("Error while loading budget group preview pane", e)
            }
        }

        prevButton.isDisable = paneCurrentPage == 0
        nextButton.isDisable = end >= budgetGroups.size
        validateAndDisplayBudgetInfo()
    }

    protected fun getAssignedCategories(): Set<Category> =
        budgetGroups
            .flatMap { it.categories }
            .toSet()

    protected fun hasEmptyGroups(): Boolean = budgetGroups.any { it.categories.isEmpty() }

    protected fun calculateTotalPercentage(): BigDecimal = budgetGroups.sumOf { it.targetPercentage }

    protected fun isPlanValid(): Boolean {
        if (budgetGroups.size < 2) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_INSUFFICIENT_GROUPS_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_INSUFFICIENT_GROUPS_MESSAGE),
            )
            return false
        }

        if (calculateTotalPercentage().isNotEqual(100)) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_PERCENTAGES_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_PERCENTAGES_MESSAGE),
            )
            return false
        }

        if (hasEmptyGroups()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_GROUPS_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_GROUPS_MESSAGE),
            )
            return false
        }

        return true
    }

    private fun addContextMenu(
        node: Node,
        group: BudgetGroup,
    ) {
        val contextMenu = ContextMenu()
        val editItem =
            MenuItem(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_CONTEXT_MENU_EDIT),
            )
        editItem.setOnAction { handleEditBudgetGroup(group) }

        val deleteItem =
            MenuItem(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_CONTEXT_MENU_DELETE),
            )
        deleteItem.setOnAction { handleDeleteBudgetGroup(group) }

        contextMenu.items.addAll(editItem, deleteItem)

        node.setOnMouseClicked { event ->
            if (event.button == MouseButton.SECONDARY) {
                contextMenu.show(node, event.screenX, event.screenY)
            } else {
                contextMenu.hide()
            }
        }
    }

    private fun handleEditBudgetGroup(groupToEdit: BudgetGroup) {
        WindowUtils.openModalWindow(
            Files.EDIT_BUDGET_GROUP_FXML,
            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_EDIT_BUDGET_GROUP_TITLE),
            springContext,
            { controller: EditBudgetGroupController ->
                controller.assignedCategories = getAssignedCategories()
                controller.setGroup(groupToEdit)
                controller.setOnSave { newBudgetGroup ->
                    val index = budgetGroups.indexOf(groupToEdit)
                    if (index != -1) {
                        budgetGroups[index] = newBudgetGroup
                        updateBudgetGroupsContainer()
                    }
                }
            },
            emptyList(),
        )
    }

    private fun handleDeleteBudgetGroup(groupToRemove: BudgetGroup) {
        budgetGroups.remove(groupToRemove)
        updateBudgetGroupsContainer()
    }

    private fun configureBaseIncomeListener() {
        baseIncomeListener =
            ChangeListener { _, oldValue, newValue ->
                if (newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                    baseIncomeField.text = newValue
                    updateBudgetGroupsContainer()
                } else {
                    baseIncomeField.text = oldValue
                }
            }
    }

    protected fun enableBaseIncomeListener() {
        baseIncomeListener?.let { listener ->
            baseIncomeField.textProperty().addListener(listener)
        }
    }

    private fun configureButtonsActions() {
        prevButton.setOnAction {
            if (paneCurrentPage > 0) {
                paneCurrentPage--
                updateBudgetGroupsContainer()
            }
        }

        nextButton.setOnAction {
            if (paneCurrentPage < (budgetGroups.size - 1) / ITEMS_PER_PAGE) {
                paneCurrentPage++
                updateBudgetGroupsContainer()
            }
        }

        prevButton.isDisable = true
        nextButton.isDisable = true
    }

    private fun validateAndDisplayBudgetInfo() {
        budgetGroupInfo.styleClass.removeAll(
            Styles.INFO_LABEL_RED_STYLE,
            Styles.INFO_LABEL_YELLOW_STYLE,
            Styles.INFO_LABEL_GREEN_STYLE,
        )

        if (budgetGroups.isEmpty()) {
            budgetGroupInfo.isVisible = false
            return
        }

        val totalPercentage = budgetGroups.sumOf { it.targetPercentage }
        val hasEmptyGroups = budgetGroups.any { it.categories.isEmpty() }

        when {
            totalPercentage > BigDecimal(100) -> {
                budgetGroupInfo.text =
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.FINANCIALPLANNING_INFO_PERCENTAGE_EXCEEDS),
                        UIUtils.formatPercentage(totalPercentage),
                    )
                budgetGroupInfo.styleClass.add(Styles.INFO_LABEL_RED_STYLE)
            }

            hasEmptyGroups -> {
                budgetGroupInfo.text =
                    preferencesService.translate(TranslationKeys.FINANCIALPLANNING_INFO_EMPTY_GROUPS)
                budgetGroupInfo.styleClass.add(Styles.INFO_LABEL_YELLOW_STYLE)
            }

            totalPercentage < BigDecimal(100) -> {
                val remaining = BigDecimal(100).subtract(totalPercentage)
                budgetGroupInfo.text =
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.FINANCIALPLANNING_INFO_PERCENTAGE_BELOW),
                        UIUtils.formatPercentage(totalPercentage),
                        UIUtils.formatPercentage(remaining),
                    )
                budgetGroupInfo.styleClass.add(Styles.INFO_LABEL_YELLOW_STYLE)
            }

            else -> {
                budgetGroupInfo.text =
                    preferencesService.translate(TranslationKeys.FINANCIALPLANNING_INFO_CORRECTLY_CONFIGURED)
                budgetGroupInfo.styleClass.add(Styles.INFO_LABEL_GREEN_STYLE)
            }
        }

        budgetGroupInfo.isVisible = true
    }

    protected fun getFieldsFromInterface(): PlanFormDTO =
        PlanFormDTO(
            name = planNameField.text.trim(),
            baseIncome = baseIncomeField.text.trim(),
            startDate = startDatePicker.value,
        )
}
