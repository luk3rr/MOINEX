/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CalculatorService;
import org.moinex.service.CategoryService;
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
public class EditTransferController extends BaseTransferManagement {
    private Transfer oldTransfer;

    /**
     * Constructor
     *
     * @param walletService            WalletService
     * @param walletTransactionService WalletTransactionService
     * @param calculatorService        CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTransferController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            CalculatorService calculatorService,
            CategoryService categoryService,
            ConfigurableApplicationContext springContext) {
        super(
                walletService,
                walletTransactionService,
                calculatorService,
                categoryService,
                springContext);
    }

    public void setTransfer(Transfer transfer) {
        if (transfer == null) {
            throw new IllegalArgumentException("Transfer cannot be null");
        }

        disableTransferValueListener();
        suggestionsHandler.disable();

        Wallet senderWallet = transfer.getSenderWallet();
        Wallet receiverWallet = transfer.getReceiverWallet();
        BigDecimal value = transfer.getAmount();
        String description = transfer.getDescription();
        LocalDate transferDate = LocalDate.from(transfer.getDate());
        Category category = transfer.getCategory();

        try {
            senderWalletComboBox.setValue(senderWallet);
            receiverWalletComboBox.setValue(receiverWallet);
            transferValueField.setText(value.toPlainString());
            descriptionField.setText(description);
            transferDatePicker.setValue(transferDate);
            categoryComboBox.setValue(category);

            senderWalletComboBox.setValue(senderWallet);
            updateSenderWalletBalance();

            receiverWalletComboBox.setValue(receiverWallet);
            updateReceiverWalletBalance();
        } finally {
            enableTransferValueListener();
            suggestionsHandler.enable();
        }

        oldTransfer = transfer;
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
                    "Empty fields", "Please fill all required fields before saving");
            return;
        }

        try {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            LocalTime currentTime = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transferDate.atTime(currentTime);

            // Check if it has any modification
            if (oldTransfer.getSenderWallet().equals(senderWt)
                    && oldTransfer.getReceiverWallet().equals(receiverWt)
                    && oldTransfer.getAmount().compareTo(transferValue) == 0
                    && oldTransfer.getDescription().equals(description)
                    && Objects.equals(
                            oldTransfer.getCategory() != null
                                    ? oldTransfer.getCategory().getId()
                                    : null,
                            category != null ? category.getId() : null)
                    && LocalDate.from(oldTransfer.getDate()).equals(transferDate)) {
                WindowUtils.showInformationDialog(
                        "No changes made", "No changes were made to the transfer.");
                return;
            } else // If there is any modification, update the transaction
            {
                oldTransfer.setAmount(transferValue);
                oldTransfer.setSenderWallet(senderWt);
                oldTransfer.setReceiverWallet(receiverWt);
                oldTransfer.setDescription(description);
                oldTransfer.setCategory(category);
                oldTransfer.setDate(dateTimeWithCurrentHour);
                walletTransactionService.updateTransfer(oldTransfer);

                WindowUtils.showSuccessDialog(
                        "Transfer created", "The transfer was successfully created.");
            }
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    "Invalid transfer value", "Transfer value must be a number.");
        } catch (MoinexException.SameSourceDestinationException
                | IllegalArgumentException
                | EntityNotFoundException
                | IllegalStateException
                | MoinexException.InsufficientResourcesException
                | MoinexException.TransferFromMasterToVirtualWalletException e) {
            WindowUtils.showErrorDialog("Error while creating transfer", e.getMessage());
        }

        Stage stage = (Stage) descriptionField.getScene().getWindow();
        stage.close();
    }

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
            BigDecimal newAmount =
                    new BigDecimal(transferValueString).setScale(2, RoundingMode.HALF_UP);
            if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
                UIUtils.resetLabel(label);
                return;
            }

            BigDecimal balance = currentWallet.getBalance();
            BigDecimal oldAmount = oldTransfer.getAmount();
            Wallet oldSender = oldTransfer.getSenderWallet();
            Wallet oldReceiver = oldTransfer.getReceiverWallet();
            boolean sameWallet =
                    (isSender && currentWallet.equals(oldSender))
                            || (!isSender && currentWallet.equals(oldReceiver));

            BigDecimal afterBalance;

            if (sameWallet) {
                BigDecimal diff = newAmount.subtract(oldAmount);

                if (isSender) {
                    afterBalance = balance.subtract(diff);
                } else {
                    afterBalance = balance.add(diff);
                }
            } else {
                if (isSender) {
                    if (currentWallet.isMaster()
                            && otherWallet.isVirtual()
                            && otherWallet.getMasterWallet().equals(currentWallet)) {
                        WindowUtils.showInformationDialog(
                                "Invalid Transfer",
                                "Cannot transfer from a master wallet to its virtual wallet.");
                        return;
                    }
                    afterBalance = balance.subtract(newAmount);
                } else {
                    if (otherWallet != null
                            && otherWallet.isVirtual()
                            && otherWallet.getMasterWallet().equals(currentWallet)) {
                        afterBalance = balance;
                    } else {
                        afterBalance = balance.add(newAmount);
                    }
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
