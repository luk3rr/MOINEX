package org.moinex.ui.dialog.financialplanning;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.BudgetGroupTransactionFilter;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.service.PreferencesService;
import org.moinex.service.financialplanning.FinancialPlanningService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/** Controller for the Add Financial Plan dialog */
@Controller
@NoArgsConstructor
public class AddPlanController extends BasePlanManagement {

    private static final String OPTION_1 = "option1";
    private static final String OPTION_2 = "option2";
    private static final String OPTION_3 = "option3";
    @FXML private ToggleGroup templateToggleGroup;
    @FXML private RadioButton option1;
    @FXML private RadioButton option2;
    @FXML private RadioButton option3;
    @FXML private Label option1Description;
    @FXML private Label option2Description;
    @FXML private Label option3Description;

    private PreferencesService preferencesService;

    @Autowired
    public AddPlanController(
            FinancialPlanningService financialPlanningService,
            ConfigurableApplicationContext springContext,
            PreferencesService preferencesService) {
        super(financialPlanningService, springContext);
        this.preferencesService = preferencesService;
        setPreferencesService(preferencesService);
    }

    @Override
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
                    preferencesService.translate(
                            Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        if (!isPlanValid()) return;

        try {
            BigDecimal baseIncome;
            baseIncome = new BigDecimal(baseIncomeText);

            FinancialPlan plan = new FinancialPlan(null, planName, baseIncome, budgetGroups, false);
            financialPlanningService.createPlan(plan);

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_CREATED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_PLAN_CREATED_MESSAGE));

            planNameField.getScene().getWindow().hide();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_ERROR_CREATING_PLAN_TITLE),
                    e.getMessage());
        }
    }

    private void configureRadioButtons() {
        option1.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_50_30_20_NAME));
        option1Description.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_50_30_20_DESCRIPTION));

        option2.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_30_30_40_NAME));
        option2Description.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_30_30_40_DESCRIPTION));

        option3.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_CUSTOM_NAME));
        option3Description.setText(
                preferencesService.translate(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_CUSTOM_DESCRIPTION));
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
                createBudgetGroupFromTemplate(getTemplate503020());
                break;
            case OPTION_2:
                createBudgetGroupFromTemplate(getTemplate303040());
                break;
            case OPTION_3:
                createCustomTemplate();
                break;
            default:
                break;
        }
    }

    private List<BudgetGroup> getTemplate503020() {
        return List.of(
                new BudgetGroup(
                        null,
                        preferencesService.translate(
                                Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_ESSENTIALS),
                        BigDecimal.valueOf(50),
                        null,
                        new HashSet<>(),
                        BudgetGroupTransactionFilter.EXPENSE),
                new BudgetGroup(
                        null,
                        preferencesService.translate(
                                Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_WANTS),
                        BigDecimal.valueOf(30),
                        null,
                        new HashSet<>(),
                        BudgetGroupTransactionFilter.EXPENSE),
                new BudgetGroup(
                        null,
                        preferencesService.translate(
                                Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_INVESTMENTS),
                        BigDecimal.valueOf(20),
                        null,
                        new HashSet<>(),
                        BudgetGroupTransactionFilter.EXPENSE));
    }

    private List<BudgetGroup> getTemplate303040() {
        return List.of(
                new BudgetGroup(
                        null,
                        preferencesService.translate(
                                Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_ESSENTIALS),
                        BigDecimal.valueOf(30),
                        null,
                        new HashSet<>(),
                        BudgetGroupTransactionFilter.EXPENSE),
                new BudgetGroup(
                        null,
                        preferencesService.translate(
                                Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_WANTS),
                        BigDecimal.valueOf(30),
                        null,
                        new HashSet<>(),
                        BudgetGroupTransactionFilter.EXPENSE),
                new BudgetGroup(
                        null,
                        preferencesService.translate(
                                Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_INVESTMENTS),
                        BigDecimal.valueOf(40),
                        null,
                        new HashSet<>(),
                        BudgetGroupTransactionFilter.EXPENSE));
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
