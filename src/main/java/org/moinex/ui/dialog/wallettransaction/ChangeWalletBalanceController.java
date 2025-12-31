/*
 * Filename: ChangeWalletBalanceController.java
 * Created on: October 30, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Rename Wallet dialog
 */
@Controller
@NoArgsConstructor
public class ChangeWalletBalanceController {
    @FXML private ComboBox<Wallet> walletComboBox;

    @FXML private TextField balanceField;

    private List<Wallet> wallets;

    private WalletService walletService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public ChangeWalletBalanceController(WalletService walletService, I18nService i18nService) {
        this.walletService = walletService;
        this.i18nService = i18nService;
    }

    public void setWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        walletComboBox.setValue(wt);
        balanceField.setText(wt.getBalance().toString());
    }

    @FXML
    private void initialize() {
        configureComboBoxes();

        loadWalletsFromDatabase();

        populateWalletComboBox();

        balanceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                balanceField.setText(oldValue);
                            }
                        });
    }

    @FXML
    private void handleSave() {
        Wallet wt = walletComboBox.getValue();
        String newBalanceStr = balanceField.getText();

        if (wt == null || newBalanceStr.isBlank()) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_INPUT_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal newBalance = new BigDecimal(newBalanceStr);

            // Check if it has modification
            if (wt.getBalance().compareTo(newBalance) == 0) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_NO_CHANGES_BALANCE_MESSAGE));
                return;
            } else // Update balance
            {
                walletService.updateWalletBalance(wt.getId(), newBalance);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_WALLET_UPDATED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_WALLET_BALANCE_UPDATED_MESSAGE));
            }

            Stage stage = (Stage) balanceField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_INPUT_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_INVALID_BALANCE_MESSAGE));
            return;
        } catch (EntityNotFoundException | IllegalStateException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_ERROR_UPDATING_BALANCE_TITLE),
                    e.getMessage());
            return;
        }

        Stage stage = (Stage) walletComboBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) walletComboBox.getScene().getWindow();
        stage.close();
    }

    private void loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void populateWalletComboBox() {
        walletComboBox.getItems().addAll(wallets);
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
    }
}
