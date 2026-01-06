package org.moinex.ui.dialog.financialplanning;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.NoArgsConstructor;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.service.FinancialPlanningService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.BudgetGroupTransactionFilter;
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
    @FXML private ToggleGroup templateToggleGroup;
    @FXML private RadioButton option1;
    @FXML private RadioButton option2;
    @FXML private RadioButton option3;
    @FXML private Label option1Description;
    @FXML private Label option2Description;
    @FXML private Label option3Description;

    private I18nService i18nService;

    @Autowired
    public AddPlanController(
            FinancialPlanningService financialPlanningService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        super(financialPlanningService, springContext);
        this.i18nService = i18nService;
        setI18nService(i18nService);
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
                    i18nService.tr(
                            Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        if (!isPlanValid()) return;

        try {
            BigDecimal baseIncome;
            baseIncome = new BigDecimal(baseIncomeText);

            financialPlanningService.createPlan(planName, baseIncome, budgetGroups);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_PLAN_CREATED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_PLAN_CREATED_MESSAGE));

            planNameField.getScene().getWindow().hide();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_BASE_INCOME_MESSAGE));
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_ERROR_CREATING_PLAN_TITLE),
                    e.getMessage());
        }
    }

    private void configureRadioButtons() {
        option1.setText(
                i18nService.tr(Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_50_30_20_NAME));
        option1Description.setText(
                i18nService.tr(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_50_30_20_DESCRIPTION));

        option2.setText(
                i18nService.tr(Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_30_30_40_NAME));
        option2Description.setText(
                i18nService.tr(
                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_30_30_40_DESCRIPTION));

        option3.setText(
                i18nService.tr(Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_CUSTOM_NAME));
        option3Description.setText(
                i18nService.tr(
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
                BudgetGroup.builder()
                        .name(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .FINANCIALPLANNING_TEMPLATE_ESSENTIALS))
                        .targetPercentage(BigDecimal.valueOf(50))
                        .transactionTypeFilter(BudgetGroupTransactionFilter.EXPENSE)
                        .build(),
                BudgetGroup.builder()
                        .name(
                                i18nService.tr(
                                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_WANTS))
                        .targetPercentage(BigDecimal.valueOf(30))
                        .transactionTypeFilter(BudgetGroupTransactionFilter.EXPENSE)
                        .build(),
                BudgetGroup.builder()
                        .name(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .FINANCIALPLANNING_TEMPLATE_INVESTMENTS))
                        .targetPercentage(BigDecimal.valueOf(20))
                        .transactionTypeFilter(BudgetGroupTransactionFilter.EXPENSE)
                        .build());
    }

    private List<BudgetGroup> getTemplate303040() {
        return List.of(
                BudgetGroup.builder()
                        .name(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .FINANCIALPLANNING_TEMPLATE_ESSENTIALS))
                        .targetPercentage(BigDecimal.valueOf(30))
                        .transactionTypeFilter(BudgetGroupTransactionFilter.EXPENSE)
                        .build(),
                BudgetGroup.builder()
                        .name(
                                i18nService.tr(
                                        Constants.TranslationKeys.FINANCIALPLANNING_TEMPLATE_WANTS))
                        .targetPercentage(BigDecimal.valueOf(30))
                        .transactionTypeFilter(BudgetGroupTransactionFilter.EXPENSE)
                        .build(),
                BudgetGroup.builder()
                        .name(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .FINANCIALPLANNING_TEMPLATE_INVESTMENTS))
                        .targetPercentage(BigDecimal.valueOf(40))
                        .transactionTypeFilter(BudgetGroupTransactionFilter.EXPENSE)
                        .build());
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
