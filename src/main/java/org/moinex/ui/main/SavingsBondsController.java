/*
 * Filename: SavingsBondsController.java
 * Created on: February 18, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.BondType;
import org.moinex.model.investment.Bond;
import org.moinex.service.BondService;
import org.moinex.service.I18nService;
import org.moinex.ui.dialog.investment.AddBondController;
import org.moinex.ui.dialog.investment.AddBondPurchaseController;
import org.moinex.ui.dialog.investment.AddBondSaleController;
import org.moinex.ui.dialog.investment.ArchivedBondsController;
import org.moinex.ui.dialog.investment.BondTransactionsController;
import org.moinex.ui.dialog.investment.EditBondController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class SavingsBondsController {

    @FXML private Text bondsTabTotalInvestedField;
    @FXML private Text bondsTabCurrentValueField;
    @FXML private Text bondsTabProfitLossField;
    @FXML private Text bondsTabInterestReceivedField;
    @FXML private TableView<Bond> bondsTabBondTable;
    @FXML private TextField bondsTabBondSearchField;
    @FXML private ComboBox<BondType> bondsTabBondTypeComboBox;

    private ConfigurableApplicationContext springContext;
    private BondService bondService;
    private I18nService i18nService;

    private final Map<Integer, BigDecimal> currentMonthInterestCache = new HashMap<>();
    private final Map<Integer, BigDecimal> totalAccumulatedInterestCache = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(SavingsBondsController.class);

    @Autowired
    public SavingsBondsController(
            BondService bondService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.bondService = bondService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    private void initialize() {
        configureBondTableView();
        updateBondTableView();
        updateBondTabFields();
        configureBondListeners();
    }

    @FXML
    private void handleRegisterBond() {
        WindowUtils.openModalWindow(
                Constants.ADD_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_ADD_BOND_TITLE),
                springContext,
                (AddBondController controller) -> {},
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                        }));
    }

    @FXML
    private void handleEditBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_EDIT_BOND_TITLE),
                springContext,
                (EditBondController controller) -> controller.setBond(selectedBond),
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                        }));
    }

    @FXML
    private void handleDeleteBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_DELETE_MESSAGE));
            return;
        }

        if (bondService.getOperationCountByBond(selectedBond.getId()) > 0) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_HAS_OPERATIONS_MESSAGE));
            return;
        }

        boolean confirmed =
                WindowUtils.showConfirmationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .SAVINGS_BONDS_DIALOG_CONFIRM_DELETE_MESSAGE),
                                selectedBond.getName()));

        if (confirmed) {
            try {
                bondService.deleteBond(selectedBond.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_DELETED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_DIALOG_BOND_DELETED_MESSAGE));

                updateBondTableView();
                updateBondTabFields();
            } catch (IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_DIALOG_ERROR_DELETING_BOND_TITLE),
                        e.getMessage());
            } catch (Exception e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleOpenBondArchive() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_BONDS_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_BOND_ARCHIVE_TITLE),
                springContext,
                (ArchivedBondsController controller) -> {},
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                        }));
    }

    @FXML
    private void handleBuyBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_BUY_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.BUY_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_BUY_BOND_TITLE),
                springContext,
                (AddBondPurchaseController controller) -> {
                    controller.setBond(selectedBond);
                },
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                        }));
    }

    @FXML
    private void handleSellBond() {
        Bond selectedBond = bondsTabBondTable.getSelectionModel().getSelectedItem();

        if (selectedBond == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .SAVINGS_BONDS_DIALOG_NO_SELECTION_SELL_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.SALE_BOND_FXML,
                i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_DIALOG_SELL_BOND_TITLE),
                springContext,
                (AddBondSaleController controller) -> controller.setBond(selectedBond),
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                        }));
    }

    @FXML
    private void handleShowBondTransactions() {
        WindowUtils.openModalWindow(
                Constants.BOND_TRANSACTIONS_FXML,
                i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_TRANSACTIONS_TITLE),
                springContext,
                (BondTransactionsController controller) -> {},
                List.of(
                        () -> {
                            updateBondTableView();
                            updateBondTabFields();
                        }));
    }

    private void configureBondTableView() {
        TableColumn<Bond, String> nameColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_NAME));
        nameColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getName()));

        TableColumn<Bond, String> symbolColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_SYMBOL));
        symbolColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(cellData.getValue().getSymbol()));

        TableColumn<Bond, String> typeColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.SAVINGS_BONDS_TABLE_HEADER_TYPE));
        typeColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                UIUtils.translateBondType(
                                        cellData.getValue().getType(), i18nService)));

        TableColumn<Bond, String> quantityColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    return new SimpleStringProperty(
                            bondService.getCurrentQuantity(bond).toString());
                });

        TableColumn<Bond, String> avgPriceColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_TABLE_UNIT_PRICE));
        avgPriceColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    return new SimpleStringProperty(
                            UIUtils.formatCurrency(bondService.getAverageUnitPrice(bond)));
                });

        TableColumn<Bond, String> currentValueColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_TABLE_HEADER_CURRENT_VALUE));
        currentValueColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    return new SimpleStringProperty(
                            UIUtils.formatCurrency(bondService.getInvestedValue(bond).add(bondService.getTotalAccumulatedInterestByBondId(bond.getId()))));
                });

        TableColumn<Bond, String> investedValueColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_TABLE_HEADER_INVESTED_VALUE));
        investedValueColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    return new SimpleStringProperty(
                            UIUtils.formatCurrency(bondService.getInvestedValue(bond)));
                });

        TableColumn<Bond, String> maturityDateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_MATURITY_DATE));
        maturityDateColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    if (bond.getMaturityDate() != null) {
                        return new SimpleStringProperty(
                                UIUtils.formatDateForDisplay(bond.getMaturityDate(), i18nService));
                    }
                    return new SimpleStringProperty("-");
                });

        TableColumn<Bond, String> interestRateColumn =
                new TableColumn<>(i18nService.tr(Constants.TranslationKeys.BOND_INTEREST_RATE));
        interestRateColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    if (bond.getInterestRate() != null) {
                        return new SimpleStringProperty(bond.getInterestRate() + "%");
                    }
                    return new SimpleStringProperty("-");
                });

        TableColumn<Bond, String> currentMonthInterestColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_TABLE_HEADER_CURRENT_MONTH_INTEREST));
        currentMonthInterestColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal interest =
                            currentMonthInterestCache.getOrDefault(bond.getId(), BigDecimal.ZERO);
                    return new SimpleStringProperty(UIUtils.formatCurrency(interest));
                });

        TableColumn<Bond, String> totalAccumulatedInterestColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .SAVINGS_BONDS_TABLE_HEADER_TOTAL_ACCUMULATED_INTEREST));
        totalAccumulatedInterestColumn.setCellValueFactory(
                cellData -> {
                    Bond bond = cellData.getValue();
                    BigDecimal interest =
                            totalAccumulatedInterestCache.getOrDefault(
                                    bond.getId(), BigDecimal.ZERO);
                    return new SimpleStringProperty(UIUtils.formatCurrency(interest));
                });

        bondsTabBondTable
                .getColumns()
                .addAll(
                        nameColumn,
                        symbolColumn,
                        typeColumn,
                        quantityColumn,
                        avgPriceColumn,
                        investedValueColumn,
                        currentValueColumn,
                        maturityDateColumn,
                        interestRateColumn,
                        currentMonthInterestColumn,
                        totalAccumulatedInterestColumn);
    }

    private void updateBondTableView() {
        List<Bond> bonds = bondService.getAllNonArchivedBonds();

        currentMonthInterestCache.clear();
        totalAccumulatedInterestCache.clear();
        for (Bond bond : bonds) {
            currentMonthInterestCache.put(
                    bond.getId(), bondService.getCurrentMonthInterest(bond.getId()));
            totalAccumulatedInterestCache.put(
                    bond.getId(), bondService.getTotalAccumulatedInterestByBondId(bond.getId()));
        }

        BondType selectedType = bondsTabBondTypeComboBox.getValue();
        if (selectedType != null) {
            bonds = bonds.stream().filter(bond -> bond.getType().equals(selectedType)).toList();
        }

        String searchText = bondsTabBondSearchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            bonds =
                    bonds.stream()
                            .filter(
                                    bond -> {
                                        String name = bond.getName().toLowerCase();
                                        String symbol =
                                                bond.getSymbol() != null
                                                        ? bond.getSymbol().toLowerCase()
                                                        : "";
                                        String type =
                                                UIUtils.translateBondType(
                                                                bond.getType(), i18nService)
                                                        .toLowerCase();
                                        String issuer =
                                                bond.getIssuer() != null
                                                        ? bond.getIssuer().toLowerCase()
                                                        : "";
                                        String quantity =
                                                bondService.getCurrentQuantity(bond).toString();
                                        String avgPrice =
                                                bondService.getAverageUnitPrice(bond).toString();
                                        String investedValue =
                                                bondService.getInvestedValue(bond).toString();
                                        String profitLoss =
                                                bondService.calculateProfit(bond).toString();
                                        String maturityDate =
                                                bond.getMaturityDate() != null
                                                        ? UIUtils.formatDateForDisplay(
                                                                        bond.getMaturityDate(),
                                                                        i18nService)
                                                                .toLowerCase()
                                                        : "";
                                        String interestRate =
                                                bond.getInterestRate() != null
                                                        ? bond.getInterestRate().toString()
                                                        : "";

                                        return name.contains(searchText)
                                                || symbol.contains(searchText)
                                                || type.contains(searchText)
                                                || issuer.contains(searchText)
                                                || quantity.contains(searchText)
                                                || avgPrice.contains(searchText)
                                                || investedValue.contains(searchText)
                                                || profitLoss.contains(searchText)
                                                || maturityDate.contains(searchText)
                                                || interestRate.contains(searchText);
                                    })
                            .toList();
        }

        bondsTabBondTable.getItems().setAll(bonds);
    }

    private void updateBondTabFields() {
        List<Bond> bonds = bondService.getAllNonArchivedBonds();

        BigDecimal totalInvested = bondService.getTotalInvestedValue();

        BigDecimal profitLoss =
                bonds.stream()
                        .map(bondService::calculateProfit)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal interestReceived = bondService.getAllBondsTotalAccumulatedInterest();

        BigDecimal currentValue = totalInvested.add(interestReceived);

        bondsTabTotalInvestedField.setText(UIUtils.formatCurrency(totalInvested));
        bondsTabCurrentValueField.setText(UIUtils.formatCurrency(currentValue));
        bondsTabProfitLossField.setText(UIUtils.formatCurrencySigned(profitLoss));
        bondsTabInterestReceivedField.setText(UIUtils.formatCurrency(interestReceived));
    }

    private void configureBondListeners() {
        bondsTabBondTypeComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(BondType bondType) {
                        if (bondType == null) {
                            return i18nService.tr(
                                    Constants.TranslationKeys.SAVINGS_STOCKS_FUNDS_FILTER_ALL);
                        }
                        return UIUtils.translateBondType(bondType, i18nService);
                    }

                    @Override
                    public BondType fromString(String string) {
                        return null;
                    }
                });

        bondsTabBondTypeComboBox.getItems().clear();
        bondsTabBondTypeComboBox.getItems().add(null);
        bondsTabBondTypeComboBox.getItems().addAll(BondType.values());
        bondsTabBondTypeComboBox.setValue(null);

        bondsTabBondTypeComboBox
                .valueProperty()
                .addListener((observable, oldValue, newValue) -> updateBondTableView());

        bondsTabBondSearchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateBondTableView());
    }
}
