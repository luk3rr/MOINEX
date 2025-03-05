/*
 * Filename: CreditCardInvoicePaymentController.java
 * Created on: October 30, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;

import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardPayment;
import org.moinex.entities.Wallet;
import org.moinex.services.CreditCardService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Credit card invoice payment dialog
 */
@Controller
@NoArgsConstructor
public class CreditCardInvoicePaymentController
{
    @FXML
    private Label crcNameLabel;

    @FXML
    private Label crcInvoiceDueLabel;

    @FXML
    private Label crcInvoiceMonthLabel;

    @FXML
    private Label walletAfterBalanceLabel;

    @FXML
    private Label walletCurrentBalanceLabel;

    @FXML
    private ComboBox<String> walletComboBox;

    private WalletService walletService;

    private CreditCardService creditCardService;

    private List<Wallet> wallets;

    private CreditCard creditCard;

    private YearMonth invoiceDate;

    /**
     * Constructor
     * @param walletService WalletService
     * @param creditCardService CreditCardService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public CreditCardInvoicePaymentController(WalletService     walletService,
                                              CreditCardService creditCardService)
    {
        this.walletService     = walletService;
        this.creditCardService = creditCardService;
    }

    public void setCreditCard(CreditCard crc, YearMonth invoiceDate)
    {
        this.creditCard  = crc;
        this.invoiceDate = invoiceDate;

        crcNameLabel.setText(crc.getName());

        BigDecimal invoiceAmount =
            creditCardService
                .getPendingCreditCardPayments(crc.getId(),
                                              invoiceDate.getMonthValue(),
                                              invoiceDate.getYear())
                .stream()
                .map(CreditCardPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        crcInvoiceDueLabel.setText(UIUtils.formatCurrency(invoiceAmount));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        crcInvoiceMonthLabel.setText(invoiceDate.format(formatter));

        if (crc.getDefaultBillingWallet() != null)
        {
            walletComboBox.setValue(crc.getDefaultBillingWallet().getName());
            updateWalletBalance();
            walletAfterBalance();
        }
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceLabel);
        UIUtils.resetLabel(walletCurrentBalanceLabel);
        UIUtils.resetLabel(crcNameLabel);
        UIUtils.resetLabel(crcInvoiceDueLabel);
        UIUtils.resetLabel(crcInvoiceMonthLabel);

        walletComboBox.setOnAction(e -> {
            updateWalletBalance();
            walletAfterBalance();
        });
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)crcNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Wallet not selected",
                                        "Please select a wallet");
            return;
        }

        BigDecimal invoiceAmount =
            creditCardService
                .getPendingCreditCardPayments(creditCard.getId(),
                                              invoiceDate.getMonthValue(),
                                              invoiceDate.getYear())
                .stream()
                .map(CreditCardPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (invoiceAmount.compareTo(BigDecimal.ZERO) == 0)
        {
            WindowUtils.showInformationDialog("Information",
                                              "Invoice already paid",
                                              "This invoice has already been paid");
        }
        else
        {
            try
            {
                Wallet wallet = wallets.stream()
                                    .filter(w -> w.getName().equals(walletName))
                                    .findFirst()
                                    .get();

                creditCardService.payInvoice(creditCard.getId(),
                                             wallet.getId(),
                                             invoiceDate.getMonthValue(),
                                             invoiceDate.getYear());

                WindowUtils.showSuccessDialog("Success",
                                              "Invoice paid",
                                              "Invoice was successfully paid");

                Stage stage = (Stage)crcNameLabel.getScene().getWindow();
                stage.close();
            }
            catch (RuntimeException e)
            {
                WindowUtils.showErrorDialog("Error",
                                            "Error paying invoice",
                                            e.getMessage());
            }
        }

        Stage stage = (Stage)crcNameLabel.getScene().getWindow();
        stage.close();
    }

    private void updateWalletBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            return;
        }

        Wallet wallet = wallets.stream()
                            .filter(w -> w.getName().equals(walletName))
                            .findFirst()
                            .get();

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.setLabelStyle(walletCurrentBalanceLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(walletCurrentBalanceLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        walletCurrentBalanceLabel.setText(UIUtils.formatCurrency(wallet.getBalance()));
    }

    private void walletAfterBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            UIUtils.resetLabel(walletAfterBalanceLabel);
            return;
        }

        BigDecimal invoiceAmount =
            creditCardService
                .getPendingCreditCardPayments(creditCard.getId(),
                                              invoiceDate.getMonthValue(),
                                              invoiceDate.getYear())
                .stream()
                .map(CreditCardPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try
        {
            Wallet wallet = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .get();

            BigDecimal walletAfterBalanceValue =
                wallet.getBalance().subtract(invoiceAmount);

            // Set the style according to the balance value after the expense
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(walletAfterBalanceLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(walletAfterBalanceLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceLabel.setText(
                UIUtils.formatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(walletAfterBalanceLabel);
        }
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::getName).toList());
    }
}
