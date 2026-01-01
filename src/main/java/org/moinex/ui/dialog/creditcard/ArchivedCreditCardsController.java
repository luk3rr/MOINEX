/*
 * Filename: ArchivedCreditCardsController.java
 * Created on: October 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityNotFoundException;
import java.text.MessageFormat;
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
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Archived Credit Cards dialog
 */
@Controller
@NoArgsConstructor
public class ArchivedCreditCardsController {
    @FXML private TableView<CreditCard> creditCardTableView;

    @FXML private TextField searchField;

    private List<CreditCard> archivedCreditCards;

    private CreditCardService creditCardService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param creditCardService CreditCardService
     * @param i18nService I18nService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ArchivedCreditCardsController(
            CreditCardService creditCardService, I18nService i18nService) {
        this.creditCardService = creditCardService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadArchivedCreditCardsFromDatabase();

        configureTableView();

        updateCreditCardTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateCreditCardTableView());
    }

    @FXML
    private void handleUnarchive() {
        CreditCard selectedCrc = creditCardTableView.getSelectionModel().getSelectedItem();

        if (selectedCrc == null) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_UNARCHIVE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_UNARCHIVE_TITLE),
                        selectedCrc.getName()),
                i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_UNARCHIVE_MESSAGE))) {
            try {
                creditCardService.unarchiveCreditCard(selectedCrc.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_UNARCHIVED_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDITCARD_DIALOG_UNARCHIVED_MESSAGE),
                                selectedCrc.getName()));

                // Remove this credit card from the list and update the table view
                archivedCreditCards.remove(selectedCrc);
                updateCreditCardTableView();
            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDITCARD_DIALOG_ERROR_UNARCHIVING_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        CreditCard selectedCrc = creditCardTableView.getSelectionModel().getSelectedItem();

        if (selectedCrc == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_DELETE));
            return;
        }

        // Prevent the removal of a credit card with associated transactions
        if (creditCardService.getDebtCountByCreditCard(selectedCrc.getId()) > 0) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_HAS_DEBTS_TITLE),
                    i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_HAS_DEBTS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_DELETE_TITLE),
                        selectedCrc.getName()),
                i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_DELETE_MESSAGE))) {
            try {
                creditCardService.deleteCreditCard(selectedCrc.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_DIALOG_DELETED_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDITCARD_DIALOG_DELETED_MESSAGE),
                                selectedCrc.getName()));

                // Remove this credit card from the list and update the table view
                archivedCreditCards.remove(selectedCrc);
                updateCreditCardTableView();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_ERROR_DELETING_TITLE),
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
     * Loads the credit cards from the database
     */
    private void loadArchivedCreditCardsFromDatabase() {
        archivedCreditCards = creditCardService.getAllArchivedCreditCards();
    }

    /**
     * Updates the credit card table view
     */
    private void updateCreditCardTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        creditCardTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            creditCardTableView.getItems().setAll(archivedCreditCards);
        } else {
            archivedCreditCards.stream()
                    .filter(
                            c -> {
                                String operatorName = c.getOperator().getName().toLowerCase();
                                String id = c.getId().toString();
                                String name = c.getName().toLowerCase();

                                return operatorName.contains(similarTextOrId)
                                        || id.contains(similarTextOrId)
                                        || name.contains(similarTextOrId);
                            })
                    .forEach(creditCardTableView.getItems()::add);
        }

        creditCardTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView() {
        TableColumn<CreditCard, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_ID));
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

        TableColumn<CreditCard, String> crcColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_CREDIT_CARD));
        crcColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<CreditCard, String> operatorColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_OPERATOR));
        operatorColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getOperator().getName()));

        TableColumn<CreditCard, Integer> numOfDebtsColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDITCARD_TABLE_ASSOCIATED_DEBTS));
        numOfDebtsColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                creditCardService.getDebtCountByCreditCard(
                                        param.getValue().getId())));

        numOfDebtsColumn.setCellFactory(
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

        creditCardTableView.getColumns().add(idColumn);
        creditCardTableView.getColumns().add(crcColumn);
        creditCardTableView.getColumns().add(operatorColumn);
        creditCardTableView.getColumns().add(numOfDebtsColumn);
    }
}
