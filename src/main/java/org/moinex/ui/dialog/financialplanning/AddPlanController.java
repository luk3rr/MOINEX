package org.moinex.ui.dialog.financialplanning;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Pair;
import lombok.NoArgsConstructor;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.service.FinancialPlanningService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Financial Plan dialog
 */
@Controller
@NoArgsConstructor
public class AddPlanController extends BasePlanManagement {

    private static final String OPTION_1 = "option1";
    private static final String OPTION_2 = "option2";
    private static final String OPTION_3 = "option3";
    // <fx:id> -> <Name, Description>
    private final Map<String, Pair<String, String>> budgetGroupOptionsSettings =
            Map.of(
                    OPTION_1,
                    new Pair<>(
                            "50/30/20",
                            "A balanced approach, ideal for most people.\n"
                                    + "Allocates 50% to needs, 30% to wants, and 20% to savings"
                                    + " and investments."),
                    OPTION_2,
                    new Pair<>(
                            "30/30/40",
                            "An investment-focused plan for those who can allocate more"
                                    + " towards their financial goals.\n"
                                    + "Allocates 30% for essentials, 30% for wants, and 40% for"
                                    + " investments."),
                    OPTION_3,
                    new Pair<>(
                            "Custom",
                            "Build your own plan from scratch.\n"
                                    + "Create your own budget groups and define their"
                                    + " percentage allocations."));
    // <fx:id> -> <List of BudgetGroup>
    private final Map<String, List<BudgetGroup>> budgetGroupTemplates =
            Map.of(
                    OPTION_1,
                    List.of(
                            BudgetGroup.builder()
                                    .name("Essentials")
                                    .targetPercentage(BigDecimal.valueOf(50))
                                    .build(),
                            BudgetGroup.builder()
                                    .name("Wants")
                                    .targetPercentage(BigDecimal.valueOf(30))
                                    .build(),
                            BudgetGroup.builder()
                                    .name("Investments")
                                    .targetPercentage(BigDecimal.valueOf(20))
                                    .build()),
                    OPTION_2,
                    List.of(
                            BudgetGroup.builder()
                                    .name("Essentials")
                                    .targetPercentage(BigDecimal.valueOf(30))
                                    .build(),
                            BudgetGroup.builder()
                                    .name("Wants")
                                    .targetPercentage(BigDecimal.valueOf(30))
                                    .build(),
                            BudgetGroup.builder()
                                    .name("Investments")
                                    .targetPercentage(BigDecimal.valueOf(40))
                                    .build()));
    @FXML private ToggleGroup templateToggleGroup;
    @FXML private RadioButton option1;
    @FXML private RadioButton option2;
    @FXML private RadioButton option3;
    @FXML private Label option1Description;
    @FXML private Label option2Description;
    @FXML private Label option3Description;

    @Autowired
    public AddPlanController(
            FinancialPlanningService financialPlanningService,
            ConfigurableApplicationContext springContext) {
        super(financialPlanningService, springContext);
    }

    @FXML
    public void initialize() {
        super.initialize();
        configureTemplateToggleGroupListener();
        configureRadioButtons();
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

        if (budgetGroups.isEmpty() || budgetGroups.size() < 2) {
            WindowUtils.showInformationDialog(
                    "Insufficient Budget Groups",
                    "You must have at least two budget groups to create a financial plan.");
            return;
        }

        if (calculateTotalPercentage().compareTo(new BigDecimal("100")) != 0) {
            WindowUtils.showInformationDialog(
                    "Invalid Budget Group Percentages",
                    "Total percentage must equal 100%. Please adjust the budget group"
                            + " percentages.");
            return;
        }

        if (hasEmptyGroups()) {
            WindowUtils.showInformationDialog(
                    "Empty Budget Groups",
                    "One or more budget groups have no categories assigned. Please edit them.");
            return;
        }

        try {
            BigDecimal baseIncome;
            baseIncome = new BigDecimal(baseIncomeText);

            financialPlanningService.createPlan(planName, baseIncome, budgetGroups);

            WindowUtils.showSuccessDialog(
                    "Financial Plan Created", "Your financial plan has been successfully created.");

            planNameField.getScene().getWindow().hide();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    "Invalid Base Income",
                    "Base income must be a valid monetary value. Please check your input.");
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog("Error while creating financial plan", e.getMessage());
        }
    }

    private void configureRadioButtons() {
        option1.setText(budgetGroupOptionsSettings.get(OPTION_1).getKey());
        option1Description.setText(budgetGroupOptionsSettings.get(OPTION_1).getValue());

        option2.setText(budgetGroupOptionsSettings.get(OPTION_2).getKey());
        option2Description.setText(budgetGroupOptionsSettings.get(OPTION_2).getValue());

        option3.setText(budgetGroupOptionsSettings.get(OPTION_3).getKey());
        option3Description.setText(budgetGroupOptionsSettings.get(OPTION_3).getValue());
    }

    private void configureTemplateToggleGroupListener() {
        templateToggleGroup
                .selectedToggleProperty()
                .addListener(
                        (observable, oldToggle, newToggle) -> {
                            if (newToggle != null) {
                                RadioButton selectedRadioButton = (RadioButton) newToggle;
                                handleTemplateSelection(selectedRadioButton);
                            }
                        });
    }

    /**
     * Handles the logic when a budget template is selected.
     *
     * @param selectedRadioButton The RadioButton that was selected
     */
    private void handleTemplateSelection(RadioButton selectedRadioButton) {
        pane1.getChildren().clear();
        pane2.getChildren().clear();
        pane3.getChildren().clear();

        switch (selectedRadioButton.getId()) {
            case OPTION_1:
                createBudgetGroupFromTemplate(budgetGroupTemplates.get(OPTION_1));
                break;
            case OPTION_2:
                createBudgetGroupFromTemplate(budgetGroupTemplates.get(OPTION_2));
                break;
            case OPTION_3:
                createCustomTemplate();
                break;
            default:
                break;
        }
    }

    private void createBudgetGroupFromTemplate(List<BudgetGroup> template) {
        this.budgetGroups = new ArrayList<>(template);
        updateBudgetGroupsContainer();
    }

    private void createCustomTemplate() {
        this.budgetGroups = new ArrayList<>();
        updateBudgetGroupsContainer();
    }
}
