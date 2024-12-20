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

    public CreditCardInvoicePaymentController() { }

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

    public void SetCreditCard(CreditCard crc, YearMonth invoiceDate)
    {
        this.creditCard  = crc;
        this.invoiceDate = invoiceDate;

        crcNameLabel.setText(crc.GetName());

        BigDecimal invoiceAmount =
            creditCardService
                .GetPendingCreditCardPayments(crc.GetId(),
                                              invoiceDate.getMonthValue(),
                                              invoiceDate.getYear())
                .stream()
                .map(CreditCardPayment::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        crcInvoiceDueLabel.setText(UIUtils.FormatCurrency(invoiceAmount));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        crcInvoiceMonthLabel.setText(invoiceDate.format(formatter));

        if (crc.GetDefaultBillingWallet() != null)
        {
            walletComboBox.setValue(crc.GetDefaultBillingWallet().GetName());
            UpdateWalletBalance();
            WalletAfterBalance();
        }
    }

    @FXML
    private void initialize()
    {
        LoadWallets();

        // Reset all labels
        UIUtils.ResetLabel(walletAfterBalanceLabel);
        UIUtils.ResetLabel(walletCurrentBalanceLabel);
        UIUtils.ResetLabel(crcNameLabel);
        UIUtils.ResetLabel(crcInvoiceDueLabel);
        UIUtils.ResetLabel(crcInvoiceMonthLabel);

        walletComboBox.setOnAction(e -> {
            UpdateWalletBalance();
            WalletAfterBalance();
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
            WindowUtils.ShowErrorDialog("Error",
                                        "Wallet not selected",
                                        "Please select a wallet");
            return;
        }

        BigDecimal invoiceAmount =
            creditCardService
                .GetPendingCreditCardPayments(creditCard.GetId(),
                                              invoiceDate.getMonthValue(),
                                              invoiceDate.getYear())
                .stream()
                .map(CreditCardPayment::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (invoiceAmount.compareTo(BigDecimal.ZERO) == 0)
        {
            WindowUtils.ShowInformationDialog("Information",
                                              "Invoice already paid",
                                              "This invoice has already been paid");
        }
        else
        {
            try
            {
                Wallet wallet = wallets.stream()
                                    .filter(w -> w.GetName().equals(walletName))
                                    .findFirst()
                                    .get();

                creditCardService.PayInvoice(creditCard.GetId(),
                                             wallet.GetId(),
                                             invoiceDate.getMonthValue(),
                                             invoiceDate.getYear());

                WindowUtils.ShowSuccessDialog("Success",
                                              "Invoice paid",
                                              "Invoice was successfully paid");

                Stage stage = (Stage)crcNameLabel.getScene().getWindow();
                stage.close();
            }
            catch (RuntimeException e)
            {
                WindowUtils.ShowErrorDialog("Error",
                                            "Error paying invoice",
                                            e.getMessage());
            }
        }

        Stage stage = (Stage)crcNameLabel.getScene().getWindow();
        stage.close();
    }

    private void UpdateWalletBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            return;
        }

        Wallet wallet = wallets.stream()
                            .filter(w -> w.GetName().equals(walletName))
                            .findFirst()
                            .get();

        if (wallet.GetBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.SetLabelStyle(walletCurrentBalanceLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(walletCurrentBalanceLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        walletCurrentBalanceLabel.setText(UIUtils.FormatCurrency(wallet.GetBalance()));
    }

    private void WalletAfterBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            UIUtils.ResetLabel(walletAfterBalanceLabel);
            return;
        }

        BigDecimal invoiceAmount =
            creditCardService
                .GetPendingCreditCardPayments(creditCard.GetId(),
                                              invoiceDate.getMonthValue(),
                                              invoiceDate.getYear())
                .stream()
                .map(CreditCardPayment::GetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try
        {
            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            BigDecimal walletAfterBalanceValue =
                wallet.GetBalance().subtract(invoiceAmount);

            // Set the style according to the balance value after the expense
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(walletAfterBalanceLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(walletAfterBalanceLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceLabel.setText(
                UIUtils.FormatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(walletAfterBalanceLabel);
        }
    }

    private void LoadWallets()
    {
        wallets = walletService.GetAllNonArchivedWalletsOrderedByName();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());
    }
}
