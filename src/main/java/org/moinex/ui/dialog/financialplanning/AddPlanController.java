package org.moinex.ui.dialog.financialplanning;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.util.Pair;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.service.FinancialPlanningService;
import org.moinex.ui.common.BudgetGroupPreviewController;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Financial Plan dialog
 */
@Controller
@NoArgsConstructor
public class AddPlanController {

    private static final String OPTION_1 = "option1";
    private static final String OPTION_2 = "option2";
    private static final String OPTION_3 = "option3";
    private static final Integer ITEMS_PER_PAGE = 3;
    private static final Logger logger = LoggerFactory.getLogger(AddPlanController.class);
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
    @FXML private TextField planNameField;
    @FXML private TextField baseIncomeField;
    @FXML private AnchorPane pane1;
    @FXML private AnchorPane pane2;
    @FXML private AnchorPane pane3;
    private FinancialPlanningService financialPlanningService;
    private ConfigurableApplicationContext springContext;
    private List<BudgetGroup> budgetGroups = new ArrayList<>();
    private Integer paneCurrentPage = 0;

    @FXML private JFXButton prevButton;

    @FXML private JFXButton nextButton;

    @Autowired
    public AddPlanController(
            FinancialPlanningService financialPlanningService,
            ConfigurableApplicationContext springContext) {
        this.financialPlanningService = financialPlanningService;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        configureRadioButtons();
        configureListeners();
        configureButtonsActions();
    }

    /**
     * Handles the action of adding a new budget group to the container.
     * This is typically used when the "Custom" template is selected.
     */
    @FXML
    private void handleAddBudgetGroup() {
        WindowUtils.openModalWindow(
                Constants.ADD_BUDGET_GROUP_FXML,
                "Add Budget Group",
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
     * Retrieves the set of categories that are already assigned to budget groups.
     * This is used to prevent duplicate category assignments when adding new budget groups.
     *
     * @return A set of categories that are currently assigned to any budget group
     */
    private Set<Category> getAssignedCategories() {
        Set<Category> assignedCategories = new HashSet<>();

        budgetGroups.stream()
                .flatMap(budgetGroup -> budgetGroup.getCategories().stream())
                .forEach(assignedCategories::add);

        return assignedCategories;
    }

    /**
     * Updates the budget groups container with the current list of budget groups
     */
    private void updateBudgetGroupsContainer() {
        pane1.getChildren().clear();
        pane2.getChildren().clear();
        pane3.getChildren().clear();

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

                // Add style class to the wallet pane
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
                    case 0:
                        pane1.getChildren().add(newContent);
                        break;

                    case 1:
                        pane2.getChildren().add(newContent);
                        break;

                    case 2:
                        pane3.getChildren().add(newContent);
                        break;
                    default:
                        logger.warn("Invalid index: {}", i);
                        break;
                }
            } catch (IOException e) {
                logger.error("Error while loading wallet full pane");
            }
        }

        prevButton.setDisable(paneCurrentPage == 0);
        nextButton.setDisable(end >= budgetGroups.size());
    }

    /**
     * Adds a context menu (right-click) to a node to allow editing and deleting
     *
     * @param node  The Node to which the context menu will be added
     * @param group The BudgetGroup associated with the node
     */
    private void addContextMenu(Node node, BudgetGroup group) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(event -> handleEditBudgetGroup(group));

        MenuItem deleteItem = new MenuItem("Delete");
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
    private void handleEditBudgetGroup(BudgetGroup groupToEdit) {}

    /**
     * Handles the action of deleting a budget group from the container
     *
     * @param groupToRemove The BudgetGroup to be removed
     */
    private void handleDeleteBudgetGroup(BudgetGroup groupToRemove) {
        budgetGroups.remove(groupToRemove);
        updateBudgetGroupsContainer();
    }

    /**
     * Handles the cancel action, closing the dialog window.
     */
    @FXML
    private void handleCancel() {
        planNameField.getScene().getWindow().hide();
    }

    /**
     * Handles the save action, creating or updating the financial plan.
     */
    @FXML
    private void handleSave() {}

    private void configureRadioButtons() {
        option1.setText(budgetGroupOptionsSettings.get(OPTION_1).getKey());
        option1Description.setText(budgetGroupOptionsSettings.get(OPTION_1).getValue());

        option2.setText(budgetGroupOptionsSettings.get(OPTION_2).getKey());
        option2Description.setText(budgetGroupOptionsSettings.get(OPTION_2).getValue());

        option3.setText(budgetGroupOptionsSettings.get(OPTION_3).getKey());
        option3Description.setText(budgetGroupOptionsSettings.get(OPTION_3).getValue());
    }

    private void configureListeners() {
        templateToggleGroup
                .selectedToggleProperty()
                .addListener(
                        (observable, oldToggle, newToggle) -> {
                            if (newToggle != null) {
                                RadioButton selectedRadioButton = (RadioButton) newToggle;
                                handleTemplateSelection(selectedRadioButton);
                            }
                        });

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
                    if (paneCurrentPage < budgetGroups.size() / ITEMS_PER_PAGE) {
                        paneCurrentPage++;
                        updateBudgetGroupsContainer();
                    }
                });

        prevButton.setDisable(true);
        nextButton.setDisable(true);
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
