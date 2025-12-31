/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Transfer dialog
 */
@Controller
@NoArgsConstructor
public class AddTransferController extends BaseTransferManagement {
    @Autowired
    public AddTransferController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CalculatorService calculatorService,
            CategoryService categoryService,
            I18nService i18nService,
            ConfigurableApplicationContext springContext) {
        super(
                walletService,
                walletTransactionService,
                calculatorService,
                categoryService,
                i18nService,
                springContext);
    }

    public void setSenderWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        senderWalletComboBox.setValue(wt);
        updateSenderWalletBalance();
    }

    public void setReceiverWalletComboBox(Wallet wt) {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId()))) {
            return;
        }

        receiverWalletComboBox.setValue(wt);
        updateReceiverWalletBalance();
    }

    @Override
    @FXML
    protected void initialize() {
        super.initialize();
    }

    @Override
    @FXML
    protected void handleSave() {
        Wallet senderWt = senderWalletComboBox.getValue();
        Wallet receiverWt = receiverWalletComboBox.getValue();
        String transferValueString = transferValueField.getText();
        String description = descriptionField.getText();
        LocalDate transferDate = transferDatePicker.getValue();

        Category category = categoryComboBox.getValue();

        if (senderWt == null
                || receiverWt == null
                || transferValueString == null
                || transferValueString.isBlank()
                || description == null
                || description.isBlank()
                || transferDate == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transferDate.atTime(currentTime);

            walletTransactionService.transferMoney(
                    senderWt.getId(),
                    receiverWt.getId(),
                    category,
                    dateTimeWithCurrentHour,
                    transferValue,
                    description);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_MESSAGE));

            Stage stage = (Stage) descriptionField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_VALUE_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_VALUE_MESSAGE));
        } catch (MoinexException.SameSourceDestinationException
                | IllegalArgumentException
                | EntityNotFoundException
                | IllegalStateException
                | MoinexException.InsufficientResourcesException
                | MoinexException.TransferFromMasterToVirtualWalletException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_ERROR_CREATING_TRANSFER_TITLE),
                    e.getMessage());
        }
    }

    /**
     * Updates the projected balance of a wallet (sender or receiver) after a transfer,
     * considering the rules for virtual and master wallets.
     *
     * @param currentWallet The wallet whose balance is being calculated (sender or receiver)
     * @param otherWallet   The other wallet involved in the transaction
     * @param label         The UI Label to be updated
     * @param isSender      True if the currentWallet is the sender, false if it is the receiver
     */
    @Override
    protected void updateAfterBalance(
            Wallet currentWallet, Wallet otherWallet, Label label, boolean isSender) {
        String transferValueString = transferValueField.getText();

        if (transferValueString == null
                || transferValueString.isBlank()
                || currentWallet == null
                || currentWallet.equals(otherWallet)) {
            UIUtils.resetLabel(label);
            return;
        }

        try {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            if (transferValue.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(label);
                return;
            }

            BigDecimal afterBalance;

            if (isSender) {
                if (currentWallet.isMaster()
                        && otherWallet.isVirtual()
                        && otherWallet.getMasterWallet().equals(currentWallet)) {
                    // If the current wallet is the SENDER and is a master wallet of the receiver,
                    // then an error is thrown... BUT be a buddy and show a message to the user :)
                    WindowUtils.showInformationDialog(
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_TITLE),
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_MESSAGE));
                    return;
                }

                afterBalance = currentWallet.getBalance().subtract(transferValue);
            } else {
                // If the current wallet is the RECEIVER, the logic depends on its relationship with
                // the sender
                // Scenario: Virtual -> Linked Master Wallet
                // If the sender is a virtual wallet and the receiver is its master wallet,
                // the money is already in the total fund. It's just an internal reorganization
                if (otherWallet != null
                        && otherWallet.isVirtual()
                        && otherWallet.getMasterWallet().equals(currentWallet)) {
                    // The total balance of the master wallet does not change.
                    afterBalance = currentWallet.getBalance();
                } else {
                    // For all other cases (Master -> Master, Master -> Virtual, etc.),
                    // the receiver's balance simply increases.
                    afterBalance = currentWallet.getBalance().add(transferValue);
                }
            }

            if (afterBalance.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.setLabelStyle(label, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                UIUtils.setLabelStyle(label, Constants.NEUTRAL_BALANCE_STYLE);
            }

            label.setText(UIUtils.formatCurrency(afterBalance));

        } catch (NumberFormatException e) {
            UIUtils.resetLabel(label);
        }
    }
}
