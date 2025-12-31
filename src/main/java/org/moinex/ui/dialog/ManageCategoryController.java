/*
 * Filename: ManageCategoryController.java
 * Created on: October 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Remove category dialog
 */
@Controller
@NoArgsConstructor
public class ManageCategoryController {
    @FXML private TableView<Category> categoryTableView;

    @FXML private TextField searchField;

    private ConfigurableApplicationContext springContext;

    private List<Category> categories;

    private CategoryService categoryService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param categoryService The category service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ManageCategoryController(
            CategoryService categoryService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.categoryService = categoryService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadCategoryFromDatabase();

        configureTableView();

        updateCategoryTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateCategoryTableView());
    }

    @FXML
    private void handleCreate() {
        WindowUtils.openModalWindow(
                Constants.ADD_CATEGORY_FXML,
                i18nService.tr(Constants.TranslationKeys.CATEGORY_DIALOG_ADD_CATEGORY_TITLE),
                springContext,
                (AddCategoryController controller) -> {},
                List.of(
                        () -> {
                            loadCategoryFromDatabase();
                            updateCategoryTableView();
                        }));
    }

    @FXML
    private void handleEdit() {
        Category selectedCategory = categoryTableView.getSelectionModel().getSelectedItem();

        if (selectedCategory == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CATEGORY_DIALOG_NO_CATEGORY_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CATEGORY_DIALOG_NO_CATEGORY_SELECTED_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_CATEGORY_FXML,
                i18nService.tr(Constants.TranslationKeys.CATEGORY_DIALOG_EDIT_CATEGORY_TITLE),
                springContext,
                (EditCategoryController controller) -> controller.setCategory(selectedCategory),
                List.of(
                        () -> {
                            loadCategoryFromDatabase();
                            updateCategoryTableView();
                        }));
    }

    @FXML
    private void handleDelete() {
        Category selectedCategory = categoryTableView.getSelectionModel().getSelectedItem();

        if (selectedCategory == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CATEGORY_DIALOG_NO_CATEGORY_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CATEGORY_DIALOG_NO_CATEGORY_SELECTED_REMOVE_MESSAGE));
            return;
        }

        // Prevent the removal of categories with associated transactions
        if (categoryService.getCountTransactions(selectedCategory.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CATEGORY_DIALOG_CATEGORY_HAS_TRANSACTIONS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CATEGORY_DIALOG_CATEGORY_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(Constants.TranslationKeys.CATEGORY_DIALOG_REMOVE_CATEGORY_TITLE)
                        + " "
                        + selectedCategory.getName(),
                i18nService.tr(
                        Constants.TranslationKeys.CATEGORY_DIALOG_REMOVE_CATEGORY_MESSAGE))) {
            try {
                categoryService.deleteCategory(selectedCategory.getId());

                loadCategoryFromDatabase();
                updateCategoryTableView();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CATEGORY_DIALOG_ERROR_REMOVING_CATEGORY_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the categories from the database
     */
    private void loadCategoryFromDatabase() {
        categories = categoryService.getCategories();
    }

    /**
     * Updates the category table view
     */
    private void updateCategoryTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        categoryTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            categoryTableView.getItems().setAll(categories);
        } else {
            categories.stream()
                    .filter(
                            c -> {
                                String name = c.getName().toLowerCase();
                                String id = c.getId().toString();
                                String archived = c.isArchived() ? "yes" : "no";

                                return name.contains(similarTextOrId)
                                        || id.contains(similarTextOrId)
                                        || archived.contains(similarTextOrId);
                            })
                    .forEach(categoryTableView.getItems()::add);
        }

        categoryTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView() {
        TableColumn<Category, Integer> idColumn = getCategoryLongTableColumn();

        TableColumn<Category, String> categoryColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CATEGORY_TABLE_CATEGORY));
        categoryColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Category, Integer> numOfTransactionsColumn = getLongTableColumn();

        TableColumn<Category, String> archivedColumn = getCategoryStringTableColumn();

        categoryTableView.getColumns().add(idColumn);
        categoryTableView.getColumns().add(categoryColumn);
        categoryTableView.getColumns().add(archivedColumn);
        categoryTableView.getColumns().add(numOfTransactionsColumn);
    }

    private TableColumn<Category, String> getCategoryStringTableColumn() {
        TableColumn<Category, String> archivedColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CATEGORY_TABLE_ARCHIVED));
        archivedColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().isArchived()
                                        ? i18nService.tr(
                                                Constants.TranslationKeys.CATEGORY_TABLE_YES)
                                        : i18nService.tr(
                                                Constants.TranslationKeys.CATEGORY_TABLE_NO)));

        archivedColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item);
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });
        return archivedColumn;
    }

    private TableColumn<Category, Integer> getLongTableColumn() {
        TableColumn<Category, Integer> numOfTransactionsColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.CATEGORY_TABLE_ASSOCIATED_TRANSACTIONS));
        numOfTransactionsColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                categoryService.getCountTransactions(param.getValue().getId())));

        numOfTransactionsColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item.toString());
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });
        return numOfTransactionsColumn;
    }

    private TableColumn<Category, Integer> getCategoryLongTableColumn() {
        TableColumn<Category, Integer> idColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item.toString());
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });
        return idColumn;
    }
}
