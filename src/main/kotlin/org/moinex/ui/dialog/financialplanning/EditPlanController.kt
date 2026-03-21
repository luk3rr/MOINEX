/*
 * Filename: EditPlanController.kt (original filename: EditPlanController.java)
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.financialplanning

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.WindowUtils
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.service.PreferencesService
import org.moinex.service.financialplanning.FinancialPlanningService
import org.moinex.ui.dialog.financialplanning.base.BasePlanManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class EditPlanController(
    financialPlanningService: FinancialPlanningService,
    applicationContext: ConfigurableApplicationContext,
    preferencesService: PreferencesService,
) : BasePlanManagement(financialPlanningService, applicationContext, preferencesService) {
    private lateinit var financialPlan: FinancialPlan

    fun setPlan(plan: FinancialPlan) {
        financialPlan = plan

        planNameField.text = plan.name
        baseIncomeField.text = plan.baseIncome.toString()
        budgetGroups = deepCopyBudgetGroups(plan.budgetGroups)

        updateBudgetGroupsContainer()
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

            if (financialPlan.name == formData.name &&
                baseIncome.isEqual(financialPlan.baseIncome) &&
                areBudgetGroupsEqual()
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_NO_CHANGES_MESSAGE),
                )
                return
            }

            financialPlan.name = formData.name
            financialPlan.baseIncome = baseIncome
            financialPlan.budgetGroups = budgetGroups

            financialPlanningService.updatePlan(financialPlan)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_UPDATED_TITLE),
                preferencesService.translate(TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_UPDATED_MESSAGE),
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
                            TranslationKeys.FINANCIALPLANNING_DIALOG_ERROR_UPDATING_PLAN_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    private fun areBudgetGroupsEqual(): Boolean {
        val list1 = budgetGroups.toList()
        val list2 = financialPlan.budgetGroups.toList()

        if (list1.size != list2.size) {
            return false
        }

        val matched = BooleanArray(list2.size)

        for (bg1 in list1) {
            var found = false
            for (i in list2.indices) {
                if (!matched[i] && bg1.isSame(list2[i])) {
                    matched[i] = true
                    found = true
                    break
                }
            }
            if (!found) {
                return false
            }
        }

        return true
    }

    private fun deepCopyBudgetGroups(groups: List<BudgetGroup>): MutableList<BudgetGroup> =
        groups
            .map { group ->
                BudgetGroup(
                    id = group.id,
                    name = group.name,
                    targetPercentage = group.targetPercentage,
                    plan = group.plan,
                    categories = group.categories,
                    transactionTypeFilter = group.transactionTypeFilter,
                )
            }.toMutableList()
}
