/*
 * Filename: RenameWalletController.java
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
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
public class RenameWalletController {
    @FXML private ComboBox<Wallet> walletComboBox;

    @FXML private TextField walletNewNameField;

    private List<Wallet> wallets;

    private WalletService walletService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public RenameWalletController(WalletService walletService, I18nService i18nService) {
        this.walletService = walletService;
        this.i18nService = i18nService;
    }

    public void setWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        walletComboBox.setValue(wt);
    }

    @FXML
    private void initialize() {
        configureComboBoxes();
        loadWalletsFromDatabase();
        populateWalletComboBox();
    }

    @FXML
    private void handleSave() {
        Wallet wt = walletComboBox.getValue();
        String walletNewName = walletNewNameField.getText();

        if (wt == null || walletNewName.isBlank()) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            walletService.renameWallet(wt.getId(), walletNewName);
        } catch (IllegalArgumentException | EntityNotFoundException | EntityExistsException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_ERROR_RENAMING_WALLET_TITLE),
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
