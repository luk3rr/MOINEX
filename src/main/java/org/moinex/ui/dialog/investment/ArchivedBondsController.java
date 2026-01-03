/*
 * Filename: ArchivedBondsController.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
import org.moinex.model.investment.Bond;
import org.moinex.service.BondService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class ArchivedBondsController {
    @FXML private TableView<Bond> bondTableView;

    @FXML private TextField searchField;

    private List<Bond> archivedBonds;

    private BondService bondService;
    private I18nService i18nService;

    @Autowired
    public ArchivedBondsController(BondService bondService, I18nService i18nService) {
        this.bondService = bondService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadArchivedBondsFromDatabase();

        configureTableView();

        updateBondTableView();

        // Add listener to the search field
        searchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateBondTableView());
    }

    @FXML
    private void handleUnarchive() {
        Bond selectedBond = bondTableView.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_UNARCHIVE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_CONFIRM_UNARCHIVE_TITLE),
                        selectedBond.getName()),
                i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_CONFIRM_UNARCHIVE_MESSAGE),
                i18nService.getBundle())) {
            try {
                bondService.unarchiveBond(selectedBond.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_BOND_UNARCHIVED_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .BOND_DIALOG_BOND_UNARCHIVED_MESSAGE),
                                selectedBond.getName()));

                // Remove this bond from the list and update the table view
                archivedBonds.remove(selectedBond);
                updateBondTableView();
            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_DIALOG_ERROR_UNARCHIVING_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleDelete() {
        Bond selectedBond = bondTableView.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_NO_BOND_SELECTED_DELETE));
            return;
        }

        // Prevent the removal of a bond with associated operations
        if (bondService.getOperationCountByBond(selectedBond.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_HAS_OPERATIONS_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_HAS_OPERATIONS_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_TITLE),
                        selectedBond.getName()),
                i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_CONFIRM_DELETE_MESSAGE),
                i18nService.getBundle())) {
            try {
                bondService.deleteBond(selectedBond.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_BOND_DELETED_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys.BOND_DIALOG_BOND_DELETED_MESSAGE),
                                selectedBond.getName()));

                // Remove this bond from the list and update the table view
                archivedBonds.remove(selectedBond);
                updateBondTableView();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ERROR_DELETING_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    private void loadArchivedBondsFromDatabase() {
        archivedBonds = bondService.getAllArchivedBonds();
    }

    private void updateBondTableView() {
        String searchText = searchField.getText().toLowerCase();

        bondTableView.getItems().clear();

        // Populate the table view
        if (searchText.isEmpty()) {
            bondTableView.getItems().setAll(archivedBonds);
        } else {
            archivedBonds.stream()
                    .filter(
                            b -> {
                                String name = b.getName().toLowerCase();
                                String symbol =
                                        b.getSymbol() != null ? b.getSymbol().toLowerCase() : "";
                                String type = b.getType().toString().toLowerCase();
                                String issuer =
                                        b.getIssuer() != null ? b.getIssuer().toLowerCase() : "";

                                return name.contains(searchText)
                                        || symbol.contains(searchText)
                                        || type.contains(searchText)
                                        || issuer.contains(searchText);
                            })
                    .forEach(bondTableView.getItems()::add);
        }

        bondTableView.refresh();
    }

    private void configureTableView() {
        TableColumn<Bond, Integer> idColumn = getBondIdTableColumn();

        TableColumn<Bond, String> nameColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_NAME));
        nameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getName()));

        TableColumn<Bond, String> symbolColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_SYMBOL));
        symbolColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSymbol()));

        TableColumn<Bond, String> typeColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_TYPE));
        typeColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.translateBondType(
                                        param.getValue().getType(), i18nService)));

        TableColumn<Bond, String> issuerColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_ISSUER));
        issuerColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getIssuer()));

        TableColumn<Bond, String> quantityColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                param -> {
                    Bond bond = param.getValue();
                    BigDecimal quantity = bondService.getCurrentQuantity(bond);
                    return new SimpleStringProperty(quantity.toString());
                });

        TableColumn<Bond, String> avgPriceColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_UNIT_PRICE));
        avgPriceColumn.setCellValueFactory(
                param -> {
                    Bond bond = param.getValue();
                    BigDecimal avgPrice = bondService.getAverageUnitPrice(bond);
                    return new SimpleStringProperty(UIUtils.formatCurrency(avgPrice));
                });

        TableColumn<Bond, String> investedValueColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.BOND_TABLE_INVESTED_VALUE));
        investedValueColumn.setCellValueFactory(
                param -> {
                    Bond bond = param.getValue();
                    BigDecimal invested = bondService.getInvestedValue(bond);
                    return new SimpleStringProperty(UIUtils.formatCurrency(invested));
                });

        TableColumn<Bond, String> maturityDateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_MATURITY_DATE));
        maturityDateColumn.setCellValueFactory(
                param -> {
                    Bond bond = param.getValue();
                    if (bond.getMaturityDate() != null) {
                        return new SimpleStringProperty(
                                UIUtils.formatDateForDisplay(bond.getMaturityDate(), i18nService));
                    }
                    return new SimpleStringProperty("-");
                });

        // Add the columns to the table view
        bondTableView.getColumns().add(idColumn);
        bondTableView.getColumns().add(nameColumn);
        bondTableView.getColumns().add(symbolColumn);
        bondTableView.getColumns().add(typeColumn);
        bondTableView.getColumns().add(issuerColumn);
        bondTableView.getColumns().add(quantityColumn);
        bondTableView.getColumns().add(avgPriceColumn);
        bondTableView.getColumns().add(investedValueColumn);
        bondTableView.getColumns().add(maturityDateColumn);
    }

    private TableColumn<Bond, Integer> getBondIdTableColumn() {
        TableColumn<Bond, Integer> idColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
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
