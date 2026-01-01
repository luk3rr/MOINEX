package org.moinex.ui.dialog.financialplanning;

import com.jfoenix.controls.JFXButton;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Base Budget Group dialog, which is used for both adding and editing budget groups
 */
@Controller
@NoArgsConstructor
public class BaseBudgetGroupController {

    @FXML protected TextField groupNameField;

    @FXML protected TextField targetPercentageField;

    @FXML protected ListView<Category> availableCategoriesListView;

    @FXML protected ListView<Category> selectedCategoriesListView;

    @FXML protected JFXButton addCategoryButton;

    @FXML protected JFXButton removeCategoryButton;

    protected Consumer<BudgetGroup> onSaveCallback;
    protected CategoryService categoryService;
    protected I18nService i18nService;
    protected Set<Category> assignedCategories;

    @Autowired
    public BaseBudgetGroupController(CategoryService categoryService, I18nService i18nService) {
        this.categoryService = categoryService;
        this.i18nService = i18nService;
    }

    /**
     * Method to be called by the parent controller to pass initial data.
     *
     * @param alreadyAssignedCategories A set of categories already used in the current plan.
     */
    public void setAssignedCategories(Set<Category> alreadyAssignedCategories) {
        this.assignedCategories = alreadyAssignedCategories;

        populateAvailableCategories();
    }

    public void setOnSave(Consumer<BudgetGroup> callback) {
        this.onSaveCallback = callback;
    }

    protected void populateAvailableCategories() {
        List<Category> allCategories = categoryService.getNonArchivedCategoriesOrderedByName();

        List<Category> availableCategories =
                allCategories.stream()
                        .filter(category -> !assignedCategories.contains(category))
                        .toList();

        availableCategoriesListView.setCellFactory(param -> createCategoryCell());
        selectedCategoriesListView.setCellFactory(param -> createCategoryCell());

        availableCategoriesListView.getItems().setAll(availableCategories);
    }

    protected ListCell<Category> createCategoryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        };
    }

    @FXML
    public void initialize() {
        setupButtonActions();
        configureListeners();
    }

    /**
     * Sets up the event handlers for the add and remove category buttons.
     */
    protected void setupButtonActions() {
        addCategoryButton.setOnAction(
                event -> moveCategory(availableCategoriesListView, selectedCategoriesListView));
        removeCategoryButton.setOnAction(
                event -> moveCategory(selectedCategoriesListView, availableCategoriesListView));

        availableCategoriesListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                addCategoryButton.setDisable(newValue == null));

        selectedCategoriesListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                removeCategoryButton.setDisable(newValue == null));

        addCategoryButton.setDisable(availableCategoriesListView.getItems().isEmpty());
        removeCategoryButton.setDisable(selectedCategoriesListView.getItems().isEmpty());
    }

    protected void configureListeners() {
        ChangeListener<String> targetPercentageListener =
                (observable, oldValue, newValue) -> {
                    if (!newValue.matches(Constants.BUDGET_GROUP_PERCENTAGE_REGEX)) {
                        targetPercentageField.setText(oldValue);
                    }
                };

        targetPercentageField.textProperty().addListener(targetPercentageListener);
    }

    /**
     * Moves the selected category from a source list to a destination list
     *
     * @param source      The source ListView
     * @param destination The destination ListView
     */
    protected void moveCategory(ListView<Category> source, ListView<Category> destination) {
        List<Category> selectedItems =
                new ArrayList<>(source.getSelectionModel().getSelectedItems());

        if (selectedItems.isEmpty()) {
            return;
        }

        for (Category category : selectedItems) {
            source.getItems().remove(category);
            destination.getItems().add(category);
        }

        destination
                .getItems()
                .sort(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER));
    }

    /**
     * Handles the cancel action, closing the dialog window.
     */
    @FXML
    protected void handleCancel() {
        groupNameField.getScene().getWindow().hide();
    }

    /**
     * Handles the save action, returning the configured budget group data.
     */
    @FXML
    protected void handleSave() {
        String groupName = groupNameField.getText().trim();
        String targetPercentageText = targetPercentageField.getText().trim();

        if (groupName.isEmpty() || targetPercentageText.isEmpty()) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_REQUIRED_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_REQUIRED_FIELDS_MESSAGE));
            return;
        }

        double targetPercentage;
        try {
            targetPercentage = Double.parseDouble(targetPercentageText);
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.FINANCIALPLANNING_DIALOG_INVALID_INPUT_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .FINANCIALPLANNING_DIALOG_INVALID_INPUT_MESSAGE));
            return;
        }

        List<Category> selectedCategories = selectedCategoriesListView.getItems();

        BudgetGroup budgetGroup =
                BudgetGroup.builder()
                        .name(groupName)
                        .targetPercentage(BigDecimal.valueOf(targetPercentage))
                        .categories(new HashSet<>(selectedCategories))
                        .build();

        if (onSaveCallback != null) {
            onSaveCallback.accept(budgetGroup);
        }

        handleCancel();
    }
}
