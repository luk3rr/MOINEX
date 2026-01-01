/*
 * Filename: CreditCardInvoicePaymentController.java
 * Created on: October 30, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.creditcard;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.CalculatorService;
import org.moinex.service.CreditCardService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Credit card invoice payment dialog
 */
@Controller
@NoArgsConstructor
public class CreditCardInvoicePaymentController {
    @FXML private Label crcNameLabel;

    @FXML private Label crcInvoiceDueLabel;

    @FXML private Label crcInvoiceMonthLabel;

    @FXML private Label crcAvailableRebateLabel;

    @FXML private Label walletAfterBalanceLabel;

    @FXML private Label walletCurrentBalanceLabel;

    @FXML private Label totalToPayLabel;

    @FXML private ComboBox<Wallet> walletComboBox;

    @FXML private TextField useRebateValueField;

    private ConfigurableApplicationContext springContext;

    private WalletService walletService;

    private CreditCardService creditCardService;

    private CalculatorService calculatorService;

    private I18nService i18nService;

    private List<Wallet> wallets;

    private CreditCard creditCard = null;

    private YearMonth invoiceDate = null;

    /**
     * Constructor
     * @param walletService WalletService
     * @param creditCardService CreditCardService
     * @param calculatorService CalculatorService
     * @param i18nService I18nService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public CreditCardInvoicePaymentController(
            WalletService walletService,
            CreditCardService creditCardService,
            CalculatorService calculatorService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.walletService = walletService;
        this.creditCardService = creditCardService;
        this.calculatorService = calculatorService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    public void setCreditCard(CreditCard crc, YearMonth invoiceDate) {
        this.creditCard = crc;
        this.invoiceDate = invoiceDate;
        configureComboBoxes();

        crcNameLabel.setText(creditCard.getName());

        BigDecimal invoiceAmount =
                creditCardService
                        .getPendingCreditCardPayments(
                                creditCard.getId(),
                                invoiceDate.getMonthValue(),
                                invoiceDate.getYear())
                        .stream()
                        .map(CreditCardPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        crcInvoiceDueLabel.setText(UIUtils.formatCurrency(invoiceAmount));

        totalToPayLabel.setText(UIUtils.formatCurrency(invoiceAmount));

        crcInvoiceMonthLabel.setText(UIUtils.formatShortMonthYear(invoiceDate, i18nService));

        crcAvailableRebateLabel.setText(UIUtils.formatCurrency(creditCard.getAvailableRebate()));

        if (creditCard.getAvailableRebate().compareTo(BigDecimal.ZERO) > 0) {
            crcAvailableRebateLabel.setStyle("-fx-text-fill: green;");
        } else {
            crcAvailableRebateLabel.setStyle("-fx-text-fill: black;");
        }

        if (creditCard.getDefaultBillingWallet() != null) {
            walletComboBox.setValue(creditCard.getDefaultBillingWallet());
            UIUtils.updateWalletBalanceLabelStyle(
                    walletComboBox.getValue(), walletCurrentBalanceLabel);
            walletAfterBalance();
        }
    }

    @FXML
    private void initialize() {
        configureComboBoxes();

        loadWalletsFromDatabase();

        populateWalletComboBox();

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceLabel);
        UIUtils.resetLabel(walletCurrentBalanceLabel);
        UIUtils.resetLabel(crcNameLabel);
        UIUtils.resetLabel(crcInvoiceDueLabel);
        UIUtils.resetLabel(crcInvoiceMonthLabel);

        walletComboBox.setOnAction(
                e -> {
                    UIUtils.updateWalletBalanceLabelStyle(
                            walletComboBox.getValue(), walletCurrentBalanceLabel);
                    walletAfterBalance();
                });

        // Update wallet after balance when the value field changes
        useRebateValueField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                useRebateValueField.setText(oldValue);
                            } else {
                                walletAfterBalance();
                            }
                        });
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) crcNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave() {
        Wallet wallet = walletComboBox.getValue();

        if (wallet == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_WALLET_NOT_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CREDITCARD_DIALOG_WALLET_NOT_SELECTED_MESSAGE));
            return;
        }

        BigDecimal invoiceAmount =
                creditCardService
                        .getPendingCreditCardPayments(
                                creditCard.getId(),
                                invoiceDate.getMonthValue(),
                                invoiceDate.getYear())
                        .stream()
                        .map(CreditCardPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rebateValue =
                useRebateValueField.getText().isEmpty()
                        ? BigDecimal.ZERO
                        : new BigDecimal(useRebateValueField.getText());

        if (invoiceAmount.compareTo(BigDecimal.ZERO) == 0) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CREDITCARD_DIALOG_INVOICE_ALREADY_PAID_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CREDITCARD_DIALOG_INVOICE_ALREADY_PAID_MESSAGE));
        } else {
            try {
                creditCardService.payInvoice(
                        creditCard.getId(),
                        wallet.getId(),
                        invoiceDate.getMonthValue(),
                        invoiceDate.getYear(),
                        rebateValue);

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_INVOICE_PAID_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.CREDITCARD_DIALOG_INVOICE_PAID_MESSAGE));

                Stage stage = (Stage) crcNameLabel.getScene().getWindow();
                stage.close();
            } catch (EntityNotFoundException
                    | IllegalArgumentException
                    | MoinexException.InsufficientResourcesException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDITCARD_DIALOG_ERROR_PAYING_INVOICE_TITLE),
                        e.getMessage());
            }
        }

        Stage stage = (Stage) crcNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleOpenCalculator() {
        WindowUtils.openPopupWindow(
                Constants.CALCULATOR_FXML,
                "Calculator",
                springContext,
                (CalculatorController controller) -> {},
                List.of(() -> calculatorService.updateComponentWithResult(useRebateValueField)));
    }

    private void walletAfterBalance() {
        Wallet wallet = walletComboBox.getValue();

        if (wallet == null) {
            UIUtils.resetLabel(walletAfterBalanceLabel);
            return;
        }

        BigDecimal rebateValue =
                useRebateValueField.getText().isEmpty()
                        ? BigDecimal.ZERO
                        : new BigDecimal(useRebateValueField.getText());

        BigDecimal invoiceAmount =
                creditCardService
                        .getPendingCreditCardPayments(
                                creditCard.getId(),
                                invoiceDate.getMonthValue(),
                                invoiceDate.getYear())
                        .stream()
                        .map(CreditCardPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .subtract(rebateValue);

        if (invoiceAmount.compareTo(BigDecimal.ZERO) < 0) {
            invoiceAmount = BigDecimal.ZERO;
        }

        totalToPayLabel.setText(UIUtils.formatCurrency(invoiceAmount));

        try {
            BigDecimal walletAfterBalanceValue = wallet.getBalance().subtract(invoiceAmount);

            // Set the style according to the balance value after the expense
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0) {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(walletAfterBalanceLabel, Constants.NEGATIVE_BALANCE_STYLE);
            } else {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(walletAfterBalanceLabel, Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceLabel.setText(UIUtils.formatCurrency(walletAfterBalanceValue));
        } catch (NumberFormatException e) {
            UIUtils.resetLabel(walletAfterBalanceLabel);
        }
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
