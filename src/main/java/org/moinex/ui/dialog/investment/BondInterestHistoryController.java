/*
 * Filename: BondInterestHistoryController.java
 * Created on: February 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import lombok.NoArgsConstructor;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondInterestCalculation;
import org.moinex.service.BondInterestCalculationService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class BondInterestHistoryController {

    @FXML private Label bondNameLabel;
    @FXML private Label bondTypeLabel;
    @FXML private Label bondInterestTypeLabel;
    @FXML private TableView<BondInterestCalculation> historyTable;
    @FXML private Button editButton;
    @FXML private Button resetButton;
    @FXML private Button recalculateButton;
    @FXML private Button closeButton;

    private Bond bond;
    private BondInterestCalculationService bondInterestCalculationService;
    private I18nService i18nService;

    private static final Logger logger =
            LoggerFactory.getLogger(BondInterestHistoryController.class);

    @Autowired
    public BondInterestHistoryController(
            BondInterestCalculationService bondInterestCalculationService,
            I18nService i18nService) {
        this.bondInterestCalculationService = bondInterestCalculationService;
        this.i18nService = i18nService;
    }

    public void setBond(Bond bond) {
        this.bond = bond;
        initialize();
    }

    @FXML
    private void initialize() {
        if (bond == null) {
            return;
        }

        setupBondInfo();
        setupTableColumns();
        loadHistoryData();
        setupTableListeners();
    }

    private void setupBondInfo() {
        bondNameLabel.setText(bond.getName());
        bondTypeLabel.setText(bond.getType().toString());
        bondInterestTypeLabel.setText(bond.getInterestType() + " - " + bond.getInterestIndex());
    }

    private void setupTableColumns() {
        historyTable.getColumns().clear();

        TableColumn<BondInterestCalculation, String> monthColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_INTEREST_HISTORY_TABLE_MONTH));
        monthColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                cellData.getValue().getReferenceMonth().toString()));

        TableColumn<BondInterestCalculation, String> quantityColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_INTEREST_HISTORY_TABLE_QUANTITY));
        quantityColumn.setCellValueFactory(
                cellData -> {
                    BigDecimal quantity = cellData.getValue().getQuantity();
                    return new SimpleStringProperty(UIUtils.formatCurrency(quantity));
                });

        TableColumn<BondInterestCalculation, String> investedAmountColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_TABLE_INVESTED_AMOUNT));
        investedAmountColumn.setCellValueFactory(
                cellData -> {
                    BigDecimal investedAmount = cellData.getValue().getInvestedAmount();
                    return new SimpleStringProperty(UIUtils.formatCurrency(investedAmount));
                });

        TableColumn<BondInterestCalculation, String> monthlyInterestColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_TABLE_MONTHLY_INTEREST));
        monthlyInterestColumn.setCellValueFactory(
                cellData -> {
                    BigDecimal monthlyInterest = cellData.getValue().getMonthlyInterest();
                    return new SimpleStringProperty(UIUtils.formatCurrency(monthlyInterest));
                });

        TableColumn<BondInterestCalculation, String> accumulatedInterestColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_TABLE_ACCUMULATED_INTEREST));
        accumulatedInterestColumn.setCellValueFactory(
                cellData -> {
                    BigDecimal accumulatedInterest = cellData.getValue().getAccumulatedInterest();
                    return new SimpleStringProperty(UIUtils.formatCurrency(accumulatedInterest));
                });

        TableColumn<BondInterestCalculation, String> finalValueColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_INTEREST_HISTORY_TABLE_FINAL_VALUE));
        finalValueColumn.setCellValueFactory(
                cellData -> {
                    BigDecimal finalValue = cellData.getValue().getFinalValue();
                    return new SimpleStringProperty(UIUtils.formatCurrency(finalValue));
                });

        TableColumn<BondInterestCalculation, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.BOND_INTEREST_HISTORY_TABLE_STATUS));
        statusColumn.setCellValueFactory(
                cellData -> {
                    if (cellData.getValue().isManuallyAdjusted()) {
                        return new SimpleStringProperty(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .BOND_INTEREST_HISTORY_STATUS_ADJUSTED));
                    }
                    return new SimpleStringProperty(
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .BOND_INTEREST_HISTORY_STATUS_AUTOMATIC));
                });

        historyTable
                .getColumns()
                .addAll(
                        monthColumn,
                        quantityColumn,
                        investedAmountColumn,
                        monthlyInterestColumn,
                        accumulatedInterestColumn,
                        finalValueColumn,
                        statusColumn);
    }

    private void loadHistoryData() {
        List<BondInterestCalculation> history =
                bondInterestCalculationService.getMonthlyInterestHistory(bond);
        ObservableList<BondInterestCalculation> observableList =
                FXCollections.observableArrayList(history);
        historyTable.setItems(observableList);
    }

    private void setupTableListeners() {
        historyTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            boolean hasSelection = newVal != null;
                            editButton.setDisable(!hasSelection);
                            resetButton.setDisable(!hasSelection || !newVal.isManuallyAdjusted());
                        });

        editButton.setDisable(true);
        resetButton.setDisable(true);
    }

    @FXML
    private void handleEditInterest() {
        BondInterestCalculation selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_NO_MONTH_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_NO_MONTH_SELECTED_MESSAGE));
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getMonthlyInterest().toString());
        dialog.setTitle(
                i18nService.tr(
                        Constants.TranslationKeys
                                .BOND_INTEREST_HISTORY_DIALOG_EDIT_INTEREST_TITLE));
        dialog.setHeaderText(
                MessageFormat.format(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_DIALOG_EDIT_INTEREST_HEADER),
                        selected.getReferenceMonth().toString()));
        dialog.setContentText(
                i18nService.tr(
                        Constants.TranslationKeys
                                .BOND_INTEREST_HISTORY_DIALOG_EDIT_INTEREST_CONTENT));

        var result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            BigDecimal newValue = new BigDecimal(result.get());

            if (newValue.compareTo(BigDecimal.ZERO) < 0) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_DIALOG_INVALID_VALUE_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_DIALOG_INVALID_VALUE_MESSAGE));
                return;
            }

            bondInterestCalculationService.adjustMonthlyInterest(
                    bond.getId(), selected.getReferenceMonth(), newValue);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_INTEREST_ADJUSTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_INTEREST_ADJUSTED_MESSAGE));

            loadHistoryData();
            logger.info(
                    "Interest adjusted for bond {} month {}: {}",
                    bond.getName(),
                    selected.getReferenceMonth(),
                    newValue);

        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_INVALID_NUMBER_MESSAGE));
            logger.warn("Invalid number format for interest adjustment", e);
        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_ERROR_ADJUSTING_TITLE),
                    e.getMessage());
            logger.error("Error adjusting interest", e);
        }
    }

    @FXML
    private void handleResetToAutomatic() {
        BondInterestCalculation selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.isManuallyAdjusted()) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_NO_ADJUSTED_MONTH_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_NO_ADJUSTED_MONTH_MESSAGE));
            return;
        }

        if (!WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys
                                .BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RESET_HEADER),
                MessageFormat.format(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RESET_MESSAGE),
                        selected.getReferenceMonth().toString()))) {
            return;
        }

        try {
            bondInterestCalculationService.resetToAutomaticCalculation(
                    bond.getId(), selected.getReferenceMonth());

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_INTEREST_RESET_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_INTEREST_RESET_MESSAGE));

            loadHistoryData();
            logger.info(
                    "Interest reset to automatic for bond {} month {}",
                    bond.getName(),
                    selected.getReferenceMonth());

        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_ERROR_RESETTING_TITLE),
                    e.getMessage());
            logger.error("Error resetting interest to automatic", e);
        }
    }

    @FXML
    private void handleRecalculateHistory() {
        if (!WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys
                                .BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RECALCULATE_HEADER),
                i18nService.tr(
                        Constants.TranslationKeys
                                .BOND_INTEREST_HISTORY_DIALOG_CONFIRM_RECALCULATE_MESSAGE))) {
            return;
        }

        try {
            bondInterestCalculationService.recalculateAllMonthlyInterest(bond.getId());

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_HISTORY_RECALCULATED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_HISTORY_RECALCULATED_MESSAGE));

            loadHistoryData();
            logger.info("Interest history recalculated for bond {}", bond.getName());

        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .BOND_INTEREST_HISTORY_DIALOG_ERROR_RECALCULATING_TITLE),
                    e.getMessage());
            logger.error("Error recalculating interest history", e);
        }
    }

    @FXML
    private void handleClose() {
        closeButton.getScene().getWindow().hide();
    }
}
