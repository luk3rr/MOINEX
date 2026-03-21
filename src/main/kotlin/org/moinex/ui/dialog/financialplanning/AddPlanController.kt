/*
 * Filename: AddPlanController.kt (original filename: AddPlanController.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.financialplanning

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.BudgetGroupTransactionFilter
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.service.PreferencesService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.ui.dialog.financialplanning.base.BasePlanManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class AddPlanController(
    financialPlanningService: FinancialPlanningService,
    springContext: ConfigurableApplicationContext,
    preferencesService: PreferencesService,
) : BasePlanManagement(financialPlanningService, springContext, preferencesService) {
    @FXML
    private lateinit var templateToggleGroup: ToggleGroup

    @FXML
    private lateinit var option1: RadioButton

    @FXML
    private lateinit var option2: RadioButton

    @FXML
    private lateinit var option3: RadioButton

    @FXML
    private lateinit var option1Description: Label

    @FXML
    private lateinit var option2Description: Label

    @FXML
    private lateinit var option3Description: Label

    companion object {
        private const val OPTION_1 = "option1"
        private const val OPTION_2 = "option2"
        private const val OPTION_3 = "option3"
    }

    @FXML
    override fun initialize() {
        super.initialize()
        configureTemplateToggleGroupListener()
        configureRadioButtons()
    }

    @FXML
    override fun handleSave() {
        val formData = getFieldsFromInterface()

        if (!formData.isValid()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        if (!isPlanValid()) return

        runCatching {
            val baseIncome = formData.baseIncome.toBigDecimal()

            val plan = FinancialPlan(name = formData.name, baseIncome = baseIncome, budgetGroups = budgetGroups)
            financialPlanningService.createPlan(plan)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_CREATED_MESSAGE),
            )

            planNameField.scene.window.hide()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_MESSAGE,
                        ),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.FINANCIALPLANNING_DIALOG_ERROR_CREATING_PLAN_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    private fun configureRadioButtons() {
        option1.text = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_50_30_20_NAME)
        option1Description.text =
            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_50_30_20_DESCRIPTION)

        option2.text = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_30_30_40_NAME)
        option2Description.text =
            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_30_30_40_DESCRIPTION)

        option3.text = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_CUSTOM_NAME)
        option3Description.text =
            preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_CUSTOM_DESCRIPTION)
    }

    private fun configureTemplateToggleGroupListener() {
        templateToggleGroup.selectedToggleProperty().addListener { _, _, newToggle ->
            if (newToggle != null) {
                val selectedRadioButton = newToggle as RadioButton
                handleTemplateSelection(selectedRadioButton)
            }
        }
    }

    private fun handleTemplateSelection(selectedRadioButton: RadioButton) {
        pane1.children.clear()
        pane2.children.clear()
        pane3.children.clear()

        when (selectedRadioButton.id) {
            OPTION_1 -> createBudgetGroupFromTemplate(getTemplate503020())
            OPTION_2 -> createBudgetGroupFromTemplate(getTemplate303040())
            OPTION_3 -> createCustomTemplate()
        }
    }

    private fun getTemplate503020(): List<BudgetGroup> =
        listOf(
            BudgetGroup(
                name = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_ESSENTIALS),
                targetPercentage = BigDecimal.valueOf(50),
                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
            ),
            BudgetGroup(
                name = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_WANTS),
                targetPercentage = BigDecimal.valueOf(30),
                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
            ),
            BudgetGroup(
                name = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_INVESTMENTS),
                targetPercentage = BigDecimal.valueOf(20),
                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
            ),
        )

    private fun getTemplate303040(): List<BudgetGroup> =
        listOf(
            BudgetGroup(
                name = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_ESSENTIALS),
                targetPercentage = BigDecimal.valueOf(30),
                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
            ),
            BudgetGroup(
                name = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_WANTS),
                targetPercentage = BigDecimal.valueOf(30),
                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
            ),
            BudgetGroup(
                name = preferencesService.translate(TranslationKeys.FINANCIALPLANNING_TEMPLATE_INVESTMENTS),
                targetPercentage = BigDecimal.valueOf(40),
                transactionTypeFilter = BudgetGroupTransactionFilter.EXPENSE,
            ),
        )

    private fun createBudgetGroupFromTemplate(template: List<BudgetGroup>) {
        budgetGroups = template.toMutableList()
        updateBudgetGroupsContainer()
    }

    private fun createCustomTemplate() {
        budgetGroups = mutableListOf()
        updateBudgetGroupsContainer()
    }
}
