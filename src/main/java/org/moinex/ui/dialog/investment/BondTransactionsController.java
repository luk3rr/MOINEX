/*
 * Filename: BondTransactionsController.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
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
import org.moinex.model.enums.OperationType;
import org.moinex.model.investment.BondOperation;
import org.moinex.service.BondService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class BondTransactionsController {
    @FXML private TableView<BondOperation> operationTableView;

    @FXML private TextField searchField;

    private ConfigurableApplicationContext springContext;

    private List<BondOperation> operations;

    private BondService bondService;
    private I18nService i18nService;

    private final String NA_ITEM = "N/A";

    @Autowired
    public BondTransactionsController(
            BondService bondService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.bondService = bondService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadOperationsFromDatabase();

        configureOperationTableView();

        updateOperationTableView();

        searchField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            updateOperationTableView();
                        });
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleEditOperation() {
        BondOperation selectedOperation = operationTableView.getSelectionModel().getSelectedItem();

        if (selectedOperation == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.BOND_DIALOG_NO_OPERATION_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_DIALOG_NO_OPERATION_SELECTED_EDIT_MESSAGE));
            return;
        }

        if (selectedOperation.getOperationType() == OperationType.BUY) {
            WindowUtils.openModalWindow(
                    Constants.EDIT_BOND_PURCHASE_FXML,
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_EDIT_PURCHASE_TITLE),
                    springContext,
                    (EditBondPurchaseController controller) ->
                            controller.setOperation(selectedOperation),
                    List.of(
                            () -> {
                                loadOperationsFromDatabase();
                                updateOperationTableView();
                            }));
        } else {
            WindowUtils.openModalWindow(
                    Constants.EDIT_BOND_SALE_FXML,
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_EDIT_SALE_TITLE),
                    springContext,
                    (EditBondSaleController controller) ->
                            controller.setOperation(selectedOperation),
                    List.of(
                            () -> {
                                loadOperationsFromDatabase();
                                updateOperationTableView();
                            }));
        }
    }

    @FXML
    private void handleDeleteOperation() {
        BondOperation selectedOperation = operationTableView.getSelectionModel().getSelectedItem();

        if (selectedOperation == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.BOND_DIALOG_NO_OPERATION_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_DIALOG_NO_OPERATION_SELECTED_DELETE_MESSAGE));
            return;
        }

        String operationType =
                UIUtils.translateOperationType(selectedOperation.getOperationType(), i18nService);
        String bondName = selectedOperation.getBond().getName();
        String symbol = selectedOperation.getBond().getSymbol();
        String bondDisplay =
                bondName + (symbol != null && !symbol.isBlank() ? " (" + symbol + ")" : "");

        String message =
                i18nService
                        .tr(Constants.TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_OPERATION_MESSAGE)
                        .replace("{operationType}", operationType)
                        .replace("{bond}", bondDisplay);

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_OPERATION_TITLE),
                message,
                i18nService.getBundle())) {
            try {
                bondService.deleteOperation(selectedOperation.getId());
                loadOperationsFromDatabase();
                updateOperationTableView();

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_OPERATION_DELETED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_OPERATION_DELETED_MESSAGE));
            } catch (Exception e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                        e.getMessage());
            }
        }
    }

    private void loadOperationsFromDatabase() {
        operations = bondService.getAllOperations();
    }

    private void updateOperationTableView() {
        String similarTextOrId = searchField.getText().toLowerCase();

        operationTableView.getItems().clear();

        if (similarTextOrId.isEmpty()) {
            operationTableView.getItems().setAll(operations);
        } else {
            operations.stream()
                    .filter(
                            op -> {
                                String id = op.getId().toString();
                                String bondName = op.getBond().getName().toLowerCase();
                                String bondSymbol =
                                        op.getBond().getSymbol() != null
                                                ? op.getBond().getSymbol().toLowerCase()
                                                : "";
                                String date =
                                        UIUtils.formatDateForDisplay(
                                                op.getWalletTransaction().getDate(), i18nService);
                                String quantity = op.getQuantity().toString();
                                String unitPrice = op.getUnitPrice().toString();
                                String fees = op.getFees() != null ? op.getFees().toString() : "";
                                String taxes =
                                        op.getTaxes() != null ? op.getTaxes().toString() : "";
                                String operationType = op.getOperationType().name().toLowerCase();
                                String walletName =
                                        op.getWalletTransaction() != null
                                                ? op.getWalletTransaction()
                                                        .getWallet()
                                                        .getName()
                                                        .toLowerCase()
                                                : "";

                                return id.contains(similarTextOrId)
                                        || bondName.contains(similarTextOrId)
                                        || bondSymbol.contains(similarTextOrId)
                                        || date.contains(similarTextOrId)
                                        || quantity.contains(similarTextOrId)
                                        || unitPrice.contains(similarTextOrId)
                                        || fees.contains(similarTextOrId)
                                        || taxes.contains(similarTextOrId)
                                        || operationType.contains(similarTextOrId)
                                        || walletName.contains(similarTextOrId);
                            })
                    .forEach(operationTableView.getItems()::add);
        }

        operationTableView.refresh();
    }

    private void configureOperationTableView() {
        TableColumn<BondOperation, Integer> idColumn = getOperationIdTableColumn();

        TableColumn<BondOperation, String> operationTypeColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.BOND_TABLE_OPERATION_TYPE));
        operationTypeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateOperationType(
                                        param.getValue().getOperationType(), i18nService)));

        TableColumn<BondOperation, String> bondNameColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_BOND));
        bondNameColumn.setCellValueFactory(
                param -> {
                    String symbol = param.getValue().getBond().getSymbol();
                    return new SimpleStringProperty(
                            param.getValue().getBond().getName()
                                    + (symbol != null && !symbol.isBlank()
                                            ? " (" + symbol + ")"
                                            : ""));
                });

        TableColumn<BondOperation, String> bondTypeColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_TYPE));
        bondTypeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateBondType(
                                        param.getValue().getBond().getType(), i18nService)));

        TableColumn<BondOperation, String> dateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateForDisplay(
                                        param.getValue().getWalletTransaction().getDate(),
                                        i18nService)));

        TableColumn<BondOperation, String> quantityColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getQuantity().toString()));

        TableColumn<BondOperation, String> unitPriceColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_UNIT_PRICE));
        unitPriceColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getUnitPrice())));

        TableColumn<BondOperation, String> feesColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_FEES));
        feesColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                param.getValue().getFees() != null
                                        ? UIUtils.formatCurrency(param.getValue().getFees())
                                        : NA_ITEM));

        TableColumn<BondOperation, String> taxesColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_TAXES));
        taxesColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                param.getValue().getTaxes() != null
                                        ? UIUtils.formatCurrency(param.getValue().getTaxes())
                                        : NA_ITEM));

        TableColumn<BondOperation, String> profitLossColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_PROFIT_LOSS));
        profitLossColumn.setCellValueFactory(
                param -> {
                    BondOperation op = param.getValue();
                    if (op.getOperationType() == OperationType.BUY) {
                        return new SimpleObjectProperty<>(NA_ITEM);
                    }
                    BigDecimal profitLoss = bondService.calculateOperationProfitLoss(op);
                    return new SimpleObjectProperty<>(UIUtils.formatCurrencySigned(profitLoss));
                });
        profitLossColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                    setStyle("");
                                } else {
                                    setText(item);
                                    getStyleClass()
                                            .removeAll(
                                                    Constants.POSITIVE_BALANCE_STYLE,
                                                    Constants.NEGATIVE_BALANCE_STYLE,
                                                    Constants.NEUTRAL_BALANCE_STYLE);

                                    if (!item.equals(NA_ITEM)) {
                                        if (item.startsWith("+")) {
                                            getStyleClass().add(Constants.POSITIVE_BALANCE_STYLE);
                                        } else if (item.startsWith("-")) {
                                            getStyleClass().add(Constants.NEGATIVE_BALANCE_STYLE);
                                        } else {
                                            getStyleClass().add(Constants.NEUTRAL_BALANCE_STYLE);
                                        }
                                    }
                                }
                            }
                        });

        TableColumn<BondOperation, String> walletNameColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_WALLET));
        walletNameColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWalletTransaction() != null
                                        ? param.getValue()
                                                .getWalletTransaction()
                                                .getWallet()
                                                .getName()
                                        : ""));

        operationTableView.getColumns().add(idColumn);
        operationTableView.getColumns().add(operationTypeColumn);
        operationTableView.getColumns().add(bondNameColumn);
        operationTableView.getColumns().add(bondTypeColumn);
        operationTableView.getColumns().add(dateColumn);
        operationTableView.getColumns().add(quantityColumn);
        operationTableView.getColumns().add(unitPriceColumn);
        operationTableView.getColumns().add(feesColumn);
        operationTableView.getColumns().add(taxesColumn);
        operationTableView.getColumns().add(profitLossColumn);
        operationTableView.getColumns().add(walletNameColumn);
    }

    private TableColumn<BondOperation, Integer> getOperationIdTableColumn() {
        TableColumn<BondOperation, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_ID));
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
