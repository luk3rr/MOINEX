/*
 * Filename: ArchivedCreditCardsController.java
 * Created on: October 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

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
import org.moinex.model.creditcard.CreditCard;
import org.moinex.service.CreditCardService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Archived Credit Cards dialog
 */
@Controller
@NoArgsConstructor
public class ArchivedCreditCardsController
{
    @FXML
    private TableView<CreditCard> creditCardTableView;

    @FXML
    private TextField searchField;

    private List<CreditCard> archivedCreditCards;

    private CreditCardService creditCardService;

    /**
     * Constructor
     * @param creditCardService CreditCardService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ArchivedCreditCardsController(CreditCardService creditCardService)
    {
        this.creditCardService = creditCardService;
    }

    @FXML
    public void initialize()
    {
        loadArchivedCreditCardsFromDatabase();

        configureTableView();

        updateCreditCardTableView();

        // Add listener to the search field
        searchField.textProperty().addListener(
            (observable, oldValue, newValue) -> updateCreditCardTableView());
    }

    @FXML
    private void handleUnarchive()
    {
        CreditCard selectedCrc =
            creditCardTableView.getSelectionModel().getSelectedItem();

        if (selectedCrc == null)
        {
            WindowUtils.showErrorDialog("No credit card selected",
                                        "Please select a credit card to unarchive");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Unarchive credit card " + selectedCrc.getName(),
                "Are you sure you want to unarchive this credit card?"))
        {
            try
            {
                creditCardService.unarchiveCreditCard(selectedCrc.getId());

                WindowUtils.showSuccessDialog("Credit Card unarchived",
                                              "Credit Card " + selectedCrc.getName() +
                                                  " has been unarchived");

                // Remove this credit card from the list and update the table view
                archivedCreditCards.remove(selectedCrc);
                updateCreditCardTableView();
            }
            catch (EntityNotFoundException e)
            {
                WindowUtils.showErrorDialog("Error unarchiving credit card",
                                            e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete()
    {
        CreditCard selectedCrc =
            creditCardTableView.getSelectionModel().getSelectedItem();

        if (selectedCrc == null)
        {
            WindowUtils.showInformationDialog("No credit card selected",
                                              "Please select a credit card to delete");
            return;
        }

        // Prevent the removal of a credit card with associated transactions
        if (creditCardService.getDebtCountByCreditCard(selectedCrc.getId()) > 0)
        {
            WindowUtils.showErrorDialog(
                "Credit Card has debts",
                "Cannot delete a credit card with associated debts. You can "
                    + "archive it instead.");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Delete credit card " + selectedCrc.getName(),
                "Are you sure you want to remove this credit card?"))
        {
            try
            {
                creditCardService.deleteCreditCard(selectedCrc.getId());

                WindowUtils.showSuccessDialog("Credit card deleted",
                                              "Credit card " + selectedCrc.getName() +
                                                  " has been deleted");

                // Remove this credit card from the list and update the table view
                archivedCreditCards.remove(selectedCrc);
                updateCreditCardTableView();
            }
            catch (EntityNotFoundException | IllegalStateException e)
            {
                WindowUtils.showErrorDialog("Error removing credit card",
                                            e.getMessage());
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
     * Loads the credit cards from the database
     */
    private void loadArchivedCreditCardsFromDatabase()
    {
        archivedCreditCards = creditCardService.getAllArchivedCreditCards();
    }

    /**
     * Updates the credit card table view
     */
    private void updateCreditCardTableView()
    {
        String similarTextOrId = searchField.getText().toLowerCase();

        creditCardTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty())
        {
            creditCardTableView.getItems().setAll(archivedCreditCards);
        }
        else
        {
            archivedCreditCards.stream()
                .filter(c -> {
                    String operatorName = c.getOperator().getName().toLowerCase();
                    String id           = c.getId().toString();
                    String name         = c.getName().toLowerCase();

                    return operatorName.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) || name.contains(similarTextOrId);
                })
                .forEach(creditCardTableView.getItems()::add);
        }

        creditCardTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView()
    {
        TableColumn<CreditCard, Long> idColumn = getCreditCardLongTableColumn();

        TableColumn<CreditCard, String> crcColumn = new TableColumn<>("Credit Card");
        crcColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<CreditCard, String> operatorColumn = new TableColumn<>("Operator");
        operatorColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().getOperator().getName()));

        TableColumn<CreditCard, Long> numOfDebtsColumn =
            new TableColumn<>("Associated Debts");
        numOfDebtsColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                creditCardService.getDebtCountByCreditCard(param.getValue().getId())));

        numOfDebtsColumn.setCellFactory(column -> new TableCell<>() {
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

        creditCardTableView.getColumns().add(idColumn);
        creditCardTableView.getColumns().add(crcColumn);
        creditCardTableView.getColumns().add(operatorColumn);
        creditCardTableView.getColumns().add(numOfDebtsColumn);
    }

    private static TableColumn<CreditCard, Long> getCreditCardLongTableColumn() {
        TableColumn<CreditCard, Long> idColumn = new TableColumn<>("ID");
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
