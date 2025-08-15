package org.moinex.ui.dialog.financialplanning;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javafx.fxml.FXML;
import lombok.NoArgsConstructor;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.service.FinancialPlanningService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Financial Plan dialog
 */
@Controller
@NoArgsConstructor
public class EditPlanController extends BasePlanManagement {

    private FinancialPlan financialPlan;

    @Autowired
    public EditPlanController(
            FinancialPlanningService financialPlanningService,
            ConfigurableApplicationContext applicationContext) {
        super(financialPlanningService, applicationContext);
    }

    public void setPlan(FinancialPlan plan) {
        this.financialPlan = plan;

        planNameField.setText(plan.getName());
        baseIncomeField.setText(plan.getBaseIncome().toString());
        budgetGroups = deepCopyBudgetGroups(plan.getBudgetGroups());

        updateBudgetGroupsContainer();
    }

    @Override
    @FXML
    protected void handleSave() {
        String planName = planNameField.getText().trim();
        String baseIncomeText = baseIncomeField.getText().trim();

        if (planName.isEmpty() || baseIncomeText.isEmpty()) {
            WindowUtils.showInformationDialog(
                    "Empty Fields", "Please fill in all required fields.");
            return;
        }

        if (!isPlanValid()) return;

        try {
            BigDecimal baseIncome;
            baseIncome = new BigDecimal(baseIncomeText);

            // Check if it has any modification
            if (financialPlan.getName().equals(planName)
                    && baseIncome.compareTo(financialPlan.getBaseIncome()) == 0
                    && areBudgetGroupsEqual()) {
                WindowUtils.showInformationDialog(
                        "No Changes Detected", "No changes were made to the financial plan.");
                return;
            }

            financialPlan.setName(planName);
            financialPlan.setBaseIncome(baseIncome);
            financialPlan.setBudgetGroups(budgetGroups);

            financialPlanningService.updatePlan(financialPlan);

            WindowUtils.showSuccessDialog(
                    "Financial Plan Updated", "The financial plan has been successfully updated.");

            planNameField.getScene().getWindow().hide();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    "Invalid Base Income",
                    "Base income must be a valid monetary value. Please check your input.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog("Error while updating financial plan", e.getMessage());
        }
    }

    /**
     * Checks if the budget groups in the current editing session are equal to those in the
     * original financial plan
     **/
    private boolean areBudgetGroupsEqual() {
        List<BudgetGroup> list1 = new ArrayList<>(budgetGroups);
        List<BudgetGroup> list2 = new ArrayList<>(financialPlan.getBudgetGroups());

        if (list1.size() != list2.size()) {
            return false;
        }

        boolean[] matched = new boolean[list2.size()];

        for (BudgetGroup bg1 : list1) {
            boolean found = false;
            for (int i = 0; i < list2.size(); i++) {
                if (!matched[i] && bg1.isSame(list2.get(i))) {
                    matched[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        return true;
    }

    /**
     * Creates a deep copy of the list of budget groups to avoid modifying the original plan's
     * groups during editing
     *
     * @param groups The list of budget groups to copy
     * @return A deep copy of the provided list of budget groups
     */
    private List<BudgetGroup> deepCopyBudgetGroups(List<BudgetGroup> groups) {
        List<BudgetGroup> copy = new ArrayList<>();
        for (BudgetGroup group : groups) {
            BudgetGroup newGroup =
                    BudgetGroup.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .targetPercentage(group.getTargetPercentage())
                            .categories(new HashSet<>(group.getCategories()))
                            .build();
            copy.add(newGroup);
        }
        return copy;
    }
}
