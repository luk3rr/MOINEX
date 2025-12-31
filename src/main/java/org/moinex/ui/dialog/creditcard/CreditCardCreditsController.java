/*
 * Filename: CreditCardCreditsController.java
 * Created on: October 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

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
import org.moinex.model.creditcard.CreditCardCredit;
import org.moinex.service.CreditCardService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Credit Card Credits dialog
 */
@Controller
@NoArgsConstructor
public class CreditCardCreditsController {
    @FXML private TableView<CreditCardCredit> creditCardCreditsTableView;

    @FXML private TextField searchField;

    private ConfigurableApplicationContext springContext;

    private List<CreditCardCredit> creditCardCredits;

    private CreditCardService creditCardService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param creditCardService CreditCardService
     * @param i18nService I18nService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public CreditCardCreditsController(
            CreditCardService creditCardService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.creditCardService = creditCardService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadCreditCardCreditsFromDatabase();

        configureTableView();

        updateCreditCardCreditsTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> updateCreditCardCreditsTableView());
    }

    @FXML
    private void handleDelete() {
        // Still empty
    }

    @FXML
    private void handleAdd() {
        WindowUtils.openModalWindow(
                Constants.ADD_CREDIT_CARD_CREDIT_FXML,
                i18nService.tr(Constants.TranslationKeys.CREDITCARD_CREDITS_ADD_TITLE),
                springContext,
                (AddCreditCardCreditController controller) -> {},
                List.of(
                        () -> {
                            loadCreditCardCreditsFromDatabase();
                            updateCreditCardCreditsTableView();
                        }));
    }

    @FXML
    private void handleEdit() {
        // Still empty
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Loads the credit cards from the database
     */
    private void loadCreditCardCreditsFromDatabase() {
        creditCardCredits = creditCardService.getAllCreditCardCredits();
    }

    /**
     * Updates the credit card table view
     */
    private void updateCreditCardCreditsTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        creditCardCreditsTableView.getItems().clear();

        // Populate the table view
        if (similarTextOrId.isEmpty()) {
            creditCardCreditsTableView.getItems().setAll(creditCardCredits);
        } else {
            creditCardCredits.stream()
                    .filter(
                            c -> {
                                String id = c.getId().toString();
                                String type = c.getType().name().toLowerCase();
                                String crcName = c.getCreditCard().getName().toLowerCase();
                                String date = c.getDate().toString().toLowerCase();
                                String amount = c.getAmount().toString().toLowerCase();
                                String description = c.getDescription().toLowerCase();

                                return id.contains(similarTextOrId)
                                        || type.contains(similarTextOrId)
                                        || crcName.contains(similarTextOrId)
                                        || date.contains(similarTextOrId)
                                        || amount.contains(similarTextOrId)
                                        || description.contains(similarTextOrId);
                            })
                    .forEach(creditCardCreditsTableView.getItems()::add);
        }

        creditCardCreditsTableView.refresh();
    }

    /**
     * Configures the table view columns
     */
    private void configureTableView() {
        TableColumn<CreditCardCredit, Integer> idColumn =
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

        TableColumn<CreditCardCredit, String> descriptionColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_DESCRIPTION));
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        TableColumn<CreditCardCredit, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<CreditCardCredit, String> typeColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateCreditCardCreditType(
                                        param.getValue().getType(), i18nService)));

        TableColumn<CreditCardCredit, String> crcColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_CREDIT_CARD));
        crcColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getCreditCard().getName()));

        TableColumn<CreditCardCredit, String> dateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.CREDITCARD_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME)));

        creditCardCreditsTableView.getColumns().add(idColumn);
        creditCardCreditsTableView.getColumns().add(descriptionColumn);
        creditCardCreditsTableView.getColumns().add(amountColumn);
        creditCardCreditsTableView.getColumns().add(dateColumn);
        creditCardCreditsTableView.getColumns().add(crcColumn);
        creditCardCreditsTableView.getColumns().add(typeColumn);
    }
}
