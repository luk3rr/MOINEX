package org.moinex.ui.dialog.financialplanning;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.service.FinancialPlanningService;
import org.moinex.service.I18nService;
import org.moinex.ui.common.BudgetGroupPreviewController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base class for Add/Edit Financial Plan dialog controllers.
 */
@NoArgsConstructor
public abstract class BasePlanManagement {

    private static final Integer ITEMS_PER_PAGE = 3;
    private static final Logger logger = LoggerFactory.getLogger(BasePlanManagement.class);

    @FXML protected TextField planNameField;
    @FXML protected TextField baseIncomeField;
    @FXML protected Label budgetGroupInfo;
    @FXML protected AnchorPane pane1;
    @FXML protected AnchorPane pane2;
    @FXML protected AnchorPane pane3;
    @FXML protected JFXButton prevButton;
    @FXML protected JFXButton nextButton;

    protected FinancialPlanningService financialPlanningService;
    protected ConfigurableApplicationContext springContext;
    protected I18nService i18nService;

    protected List<BudgetGroup> budgetGroups = new ArrayList<>();
    protected Integer paneCurrentPage = 0;

    @Autowired
    protected BasePlanManagement(
            FinancialPlanningService financialPlanningService,
            ConfigurableApplicationContext springContext) {
        this.financialPlanningService = financialPlanningService;
        this.springContext = springContext;
    }

    protected void setI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        configureBaseIncomeListener();
        configureButtonsActions();
        budgetGroupInfo.setVisible(false);
    }

    @FXML
    protected abstract void handleSave();

    @FXML
    protected void handleCancel() {
        planNameField.getScene().getWindow().hide();
    }

    /**
     * Handles the action of adding a new budget group to the container.
     * This is typically used when the "Custom" template is selected.
     */
    @FXML
    private void handleAddBudgetGroup() {
        WindowUtils.openModalWindow(
                Constants.ADD_BUDGET_GROUP_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_ADD_BUDGET_GROUP_TITLE),
                springContext,
                (AddBudgetGroupController controller) -> {
                    controller.setAssignedCategories(getAssignedCategories());

                    controller.setOnSave(
                            newBudgetGroup -> {
                                budgetGroups.add(newBudgetGroup);
                                updateBudgetGroupsContainer();
                            });
                },
                List.of());
    }

    /**
     * Updates the budget groups container with the current list of budget groups
     */
    protected void updateBudgetGroupsContainer() {
        pane1.getChildren().clear();
        pane2.getChildren().clear();
        pane3.getChildren().clear();

        budgetGroups.sort(
                (a, b) -> {
                    if (b.getTargetPercentage() == null || a.getTargetPercentage() == null) {
                        return 0; // Handle null values gracefully
                    }
                    return b.getTargetPercentage().compareTo(a.getTargetPercentage());
                });

        Integer start = paneCurrentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, budgetGroups.size());

        for (Integer i = start; i < end; i++) {
            BudgetGroup budgetGroup = budgetGroups.get(i);
            try {
                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(Constants.BUDGET_GROUP_PREVIEW_PANE_FXML));
                loader.setControllerFactory(springContext::getBean);
                Parent newContent = loader.load();
                newContent
                        .getStylesheets()
                        .add(
                                Objects.requireNonNull(
                                                getClass()
                                                        .getResource(Constants.COMMON_STYLE_SHEET))
                                        .toExternalForm());

                BigDecimal planTotal =
                        baseIncomeField.getText().isEmpty()
                                ? BigDecimal.ZERO
                                : new BigDecimal(baseIncomeField.getText());
                BudgetGroupPreviewController previewController = loader.getController();
                previewController.populate(budgetGroup, planTotal);

                addContextMenu(newContent, budgetGroup);

                AnchorPane.setTopAnchor(newContent, 0.0);
                AnchorPane.setBottomAnchor(newContent, 0.0);
                AnchorPane.setLeftAnchor(newContent, 0.0);
                AnchorPane.setRightAnchor(newContent, 0.0);

                switch (i % ITEMS_PER_PAGE) {
                    case 0 -> pane1.getChildren().add(newContent);
                    case 1 -> pane2.getChildren().add(newContent);
                    case 2 -> pane3.getChildren().add(newContent);
                    default -> logger.warn("Invalid index: {}", i);
                }
            } catch (IOException e) {
                logger.error("Error while loading budget group preview pane", e);
            }
        }

        prevButton.setDisable(paneCurrentPage == 0);
        nextButton.setDisable(end >= budgetGroups.size());
        validateAndDisplayBudgetInfo();
    }

    /**
     * Retrieves the set of categories that are already assigned to budget groups.
     * This is used to prevent duplicate category assignments when adding new budget groups.
     *
     * @return A set of categories that are currently assigned to any budget group
     */
    protected Set<Category> getAssignedCategories() {
        Set<Category> assignedCategories = new HashSet<>();
        if (budgetGroups != null) {
            budgetGroups.stream()
                    .flatMap(budgetGroup -> budgetGroup.getCategories().stream())
                    .forEach(assignedCategories::add);
        }
        return assignedCategories;
    }

    protected boolean hasEmptyGroups() {
        return budgetGroups.stream()
                .anyMatch(
                        group -> group.getCategories() == null || group.getCategories().isEmpty());
    }

    protected BigDecimal calculateTotalPercentage() {
        return budgetGroups.stream()
                .map(BudgetGroup::getTargetPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected boolean isPlanValid() {
        if (budgetGroups.isEmpty() || budgetGroups.size() < 2) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INSUFFICIENT_GROUPS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INSUFFICIENT_GROUPS_MESSAGE));

            return false;
        }

        if (calculateTotalPercentage().compareTo(new BigDecimal("100")) != 0) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_PERCENTAGES_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_PERCENTAGES_MESSAGE));
            return false;
        }

        if (hasEmptyGroups()) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_EMPTY_GROUPS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_EMPTY_GROUPS_MESSAGE));
            return false;
        }

        return true;
    }

    /**
     * Adds a context menu (right-click) to a node to allow editing and deleting
     *
     * @param node  The Node to which the context menu will be added
     * @param group The BudgetGroup associated with the node
     */
    private void addContextMenu(Node node, BudgetGroup group) {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem =
                new MenuItem(
                        i18nService.tr(
                                Constants.TranslationKeys.FINANCIALPLANNING_CONTEXT_MENU_EDIT));
        editItem.setOnAction(event -> handleEditBudgetGroup(group));
        MenuItem deleteItem =
                new MenuItem(
                        i18nService.tr(
                                Constants.TranslationKeys.FINANCIALPLANNING_CONTEXT_MENU_DELETE));
        deleteItem.setOnAction(event -> handleDeleteBudgetGroup(group));
        contextMenu.getItems().addAll(editItem, deleteItem);

        node.setOnMouseClicked(
                event -> {
                    if (event.getButton() == MouseButton.SECONDARY) {
                        contextMenu.show(node, event.getScreenX(), event.getScreenY());
                    } else {
                        contextMenu.hide();
                    }
                });
    }

    /**
     * Handles the action of editing a budget group
     *
     * @param groupToEdit The BudgetGroup to be edited
     */
    private void handleEditBudgetGroup(BudgetGroup groupToEdit) {
        WindowUtils.openModalWindow(
                Constants.EDIT_BUDGET_GROUP_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_EDIT_BUDGET_GROUP_TITLE),
                springContext,
                (EditBudgetGroupController controller) -> {
                    controller.setAssignedCategories(getAssignedCategories());
                    controller.setGroup(groupToEdit);
                    controller.setOnSave(
                            newBudgetGroup -> {
                                int index = budgetGroups.indexOf(groupToEdit);
                                if (index != -1) {
                                    budgetGroups.set(index, newBudgetGroup);
                                    updateBudgetGroupsContainer();
                                }
                            });
                },
                List.of());
    }

    /**
     * Handles the action of deleting a budget group from the container
     *
     * @param groupToRemove The BudgetGroup to be removed
     */
    private void handleDeleteBudgetGroup(BudgetGroup groupToRemove) {
        budgetGroups.remove(groupToRemove);
        updateBudgetGroupsContainer();
    }

    private void configureBaseIncomeListener() {
        baseIncomeField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                baseIncomeField.setText(newValue);
                                updateBudgetGroupsContainer();
                            } else {
                                baseIncomeField.setText(oldValue);
                            }
                        });
    }

    private void configureButtonsActions() {
        prevButton.setOnAction(
                event -> {
                    if (paneCurrentPage > 0) {
                        paneCurrentPage--;
                        updateBudgetGroupsContainer();
                    }
                });
        nextButton.setOnAction(
                event -> {
                    if (paneCurrentPage < (budgetGroups.size() - 1) / ITEMS_PER_PAGE) {
                        paneCurrentPage++;
                        updateBudgetGroupsContainer();
                    }
                });
        prevButton.setDisable(true);
        nextButton.setDisable(true);
    }

    private void validateAndDisplayBudgetInfo() {
        budgetGroupInfo
                .getStyleClass()
                .removeAll(
                        Constants.INFO_LABEL_RED_STYLE,
                        Constants.INFO_LABEL_YELLOW_STYLE,
                        Constants.INFO_LABEL_GREEN_STYLE);

        if (budgetGroups == null || budgetGroups.isEmpty()) {
            budgetGroupInfo.setVisible(false);
            return;
        }

        BigDecimal totalPercentage =
                budgetGroups.stream()
                        .map(BudgetGroup::getTargetPercentage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasEmptyGroups =
                budgetGroups.stream()
                        .anyMatch(
                                group ->
                                        group.getCategories() == null
                                                || group.getCategories().isEmpty());

        if (totalPercentage.compareTo(new BigDecimal("100")) > 0) {
            budgetGroupInfo.setText(
                    MessageFormat.format(
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .FINANCIALPLANNING_INFO_PERCENTAGE_EXCEEDS),
                            UIUtils.formatPercentage(totalPercentage)));
            budgetGroupInfo.getStyleClass().add(Constants.INFO_LABEL_RED_STYLE);
        } else if (hasEmptyGroups) {
            budgetGroupInfo.setText(
                    i18nService.tr(Constants.TranslationKeys.FINANCIALPLANNING_INFO_EMPTY_GROUPS));
            budgetGroupInfo.getStyleClass().add(Constants.INFO_LABEL_YELLOW_STYLE);
        } else if (totalPercentage.compareTo(new BigDecimal("100")) < 0) {
            BigDecimal remaining = new BigDecimal("100").subtract(totalPercentage);
            budgetGroupInfo.setText(
                    MessageFormat.format(
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .FINANCIALPLANNING_INFO_PERCENTAGE_BELOW),
                            UIUtils.formatPercentage(totalPercentage),
                            UIUtils.formatPercentage(remaining)));
            budgetGroupInfo.getStyleClass().add(Constants.INFO_LABEL_YELLOW_STYLE);
        } else {
            budgetGroupInfo.setText(
                    i18nService.tr(
                            Constants.TranslationKeys.FINANCIALPLANNING_INFO_CORRECTLY_CONFIGURED));
            budgetGroupInfo.getStyleClass().add(Constants.INFO_LABEL_GREEN_STYLE);
        }
        budgetGroupInfo.setVisible(true);
    }
}
