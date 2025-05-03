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
public class ManageCategoryController
{
    @FXML
    private TableView<Category> categoryTableView;

    @FXML
    private TextField searchField;

    private ConfigurableApplicationContext springContext;

    private List<Category> categories;

    private CategoryService categoryService;

    /**
     * Constructor
     * @param categoryService The category service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ManageCategoryController(CategoryService categoryService, ConfigurableApplicationContext springContext)
    {
        this.categoryService = categoryService;
        this.springContext = springContext;
    }

    @FXML
    public void initialize()
    {
        loadCategoryFromDatabase();

        configureTableView();

        updateCategoryTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> updateCategoryTableView());
    }

    @FXML
    private void handleCreate()
    {
        WindowUtils.openModalWindow(Constants.ADD_CATEGORY_FXML,
                                    "Add Category",
                                    springContext,
                                    (AddCategoryController controller)
                                        -> {},
                                    List.of(() -> {
                                        loadCategoryFromDatabase();
                                        updateCategoryTableView();
                                    }));
    }

    @FXML
    private void handleEdit()
    {
        Category selectedCategory =
            categoryTableView.getSelectionModel().getSelectedItem();

        if (selectedCategory == null)
        {
            WindowUtils.showInformationDialog("No category selected",
                                              "Please select a category to edit");
            return;
        }

        WindowUtils.openModalWindow(Constants.EDIT_CATEGORY_FXML,
                                    "Edit Category",
                                    springContext,
                                    (EditCategoryController controller)
                                        -> controller.setCategory(selectedCategory),
                                    List.of(() -> {
                                        loadCategoryFromDatabase();
                                        updateCategoryTableView();
                                    }));
    }

    @FXML
    private void handleDelete()
    {
        Category selectedCategory =
            categoryTableView.getSelectionModel().getSelectedItem();

        if (selectedCategory == null)
        {
            WindowUtils.showInformationDialog("No category selected",
                                              "Please select a category to remove");
            return;
        }

        // Prevent the removal of categories with associated transactions
        if (categoryService.getCountTransactions(selectedCategory.getId()) > 0)
        {
            WindowUtils.showInformationDialog(
                "Category has transactions",
                "Cannot remove a category with transactions");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Remove category " + selectedCategory.getName(),
                "Are you sure you want to remove this category?"))
        {
            try
            {
                categoryService.deleteCategory(selectedCategory.getId());

                loadCategoryFromDatabase();
                updateCategoryTableView();
            }
            catch (EntityNotFoundException | IllegalStateException e)
            {
                WindowUtils.showErrorDialog("Error removing category", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the categories from the database
     */
    private void loadCategoryFromDatabase()
    {
        categories = categoryService.getCategories();
    }

    /**
     * Updates the category table view
     */
    private void updateCategoryTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        categoryTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            categoryTableView.getItems().setAll(categories);
        }
        else
        {
            categories.stream()
                .filter(c -> {
                    String name     = c.getName().toLowerCase();
                    String id       = c.getId().toString();
                    String archived = c.isArchived() ? "yes" : "no";

                    return name.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        archived.contains(similarTextOrId);
                })
                .forEach(categoryTableView.getItems()::add);
        }

        categoryTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView()
    {
        TableColumn<Category, Long> idColumn = getCategoryLongTableColumn();

        TableColumn<Category, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Category, Long> numOfTransactionsColumn = getLongTableColumn();

        TableColumn<Category, String> archivedColumn = getCategoryStringTableColumn();

        categoryTableView.getColumns().add(idColumn);
        categoryTableView.getColumns().add(categoryColumn);
        categoryTableView.getColumns().add(archivedColumn);
        categoryTableView.getColumns().add(numOfTransactionsColumn);
    }

    private static TableColumn<Category, String> getCategoryStringTableColumn() {
        TableColumn<Category, String> archivedColumn = new TableColumn<>("Archived");
        archivedColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().isArchived() ? "Yes" : "No"));

        archivedColumn.setCellFactory(column -> new TableCell<>() {
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

    private TableColumn<Category, Long> getLongTableColumn() {
        TableColumn<Category, Long> numOfTransactionsColumn =
            new TableColumn<>("Associated Transactions");
        numOfTransactionsColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                categoryService.getCountTransactions(param.getValue().getId())));

        numOfTransactionsColumn.setCellFactory(
            column -> new TableCell<>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
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

    private static TableColumn<Category, Long> getCategoryLongTableColumn() {
        TableColumn<Category, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().getId()));

        idColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
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
