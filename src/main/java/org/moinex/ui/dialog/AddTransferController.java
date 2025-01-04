/*
 * Filename: AddTransferController.java
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.Wallet;
import org.moinex.services.CalculatorService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
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
public class AddTransferController
{
    @FXML
    private Label senderWalletAfterBalanceValueLabel;

    @FXML
    private Label receiverWalletAfterBalanceValueLabel;

    @FXML
    private Label senderWalletCurrentBalanceValueLabel;

    @FXML
    private Label receiverWalletCurrentBalanceValueLabel;

    @FXML
    private ComboBox<String> senderWalletComboBox;

    @FXML
    private ComboBox<String> receiverWalletComboBox;

    @FXML
    private TextField transferValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker transferDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CalculatorService calculatorService;

    private List<Wallet> wallets;

    public AddTransferController() { }

    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddTransferController(WalletService            walletService,
                                 WalletTransactionService walletTransactionService,
                                 CalculatorService        calculatorService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.calculatorService        = calculatorService;
    }

    public void SetSenderWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.GetId() == wt.GetId()))
        {
            return;
        }

        senderWalletComboBox.setValue(wt.GetName());

        UpdateSenderWalletBalance();
    }

    public void SetReceiverWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.GetId() == wt.GetId()))
        {
            return;
        }

        receiverWalletComboBox.setValue(wt.GetName());

        UpdateReceiverWalletBalance();
    }

    @FXML
    private void initialize()
    {
        LoadWallets();

        // Configure the date picker
        UIUtils.SetDatePickerFormat(transferDatePicker);

        // Reset all labels
        UIUtils.ResetLabel(senderWalletAfterBalanceValueLabel);
        UIUtils.ResetLabel(receiverWalletAfterBalanceValueLabel);
        UIUtils.ResetLabel(senderWalletCurrentBalanceValueLabel);
        UIUtils.ResetLabel(receiverWalletCurrentBalanceValueLabel);

        senderWalletComboBox.setOnAction(e -> {
            UpdateSenderWalletBalance();
            UpdateSenderWalletAfterBalance();
        });

        receiverWalletComboBox.setOnAction(e -> {
            UpdateReceiverWalletBalance();
            UpdateReceiverWalletAfterBalance();
        });

        // Update sender wallet after balance when transfer value changes
        transferValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    transferValueField.setText(oldValue);
                }
                else
                {
                    UpdateSenderWalletAfterBalance();
                    UpdateReceiverWalletAfterBalance();
                }
            });
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String    senderWalletName    = senderWalletComboBox.getValue();
        String    receiverWalletName  = receiverWalletComboBox.getValue();
        String    transferValueString = transferValueField.getText();
        String    description         = descriptionField.getText();
        LocalDate transferDate        = transferDatePicker.getValue();

        if (senderWalletName == null || receiverWalletName == null ||
            transferValueString == null || transferValueString.strip().isEmpty() ||
            description == null || description.strip().isEmpty() ||
            transferDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields.");
            return;
        }

        try
        {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            Wallet senderWallet = wallets.stream()
                                      .filter(w -> w.GetName().equals(senderWalletName))
                                      .findFirst()
                                      .get();

            Wallet receiverWallet =
                wallets.stream()
                    .filter(w -> w.GetName().equals(receiverWalletName))
                    .findFirst()
                    .get();

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = transferDate.atTime(currentTime);

            walletTransactionService.TransferMoney(senderWallet.GetId(),
                                                   receiverWallet.GetId(),
                                                   dateTimeWithCurrentHour,
                                                   transferValue,
                                                   description);

            WindowUtils.ShowSuccessDialog("Success",
                                          "Transfer created",
                                          "The transfer was successfully created.");

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid transfer value",
                                        "Transfer value must be a number.");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while creating transfer",
                                        e.getMessage());
            return;
        }
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.OpenPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> { GetResultFromCalculator(); }));
    }

    private void GetResultFromCalculator()
    {
        // If the user saved the result, set it in the transferValueField
        String result = calculatorService.GetResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.ShowErrorDialog("Error",
                                                "Invalid value",
                                                "The value must be positive");
                    return;
                }

                // Round the result to 2 decimal places
                result = resultValue.setScale(2, RoundingMode.HALF_UP).toString();

                transferValueField.setText(result);
            }
            catch (NumberFormatException e)
            {
                // Must be unreachable
                WindowUtils.ShowErrorDialog("Error",
                                            "Invalid value",
                                            "The value must be a number");
            }
        }
    }

    private void UpdateSenderWalletBalance()
    {
        String senderWalletName = senderWalletComboBox.getValue();

        if (senderWalletName == null)
        {
            return;
        }

        Wallet senderWallet = wallets.stream()
                                  .filter(w -> w.GetName().equals(senderWalletName))
                                  .findFirst()
                                  .get();

        if (senderWallet.GetBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.SetLabelStyle(senderWalletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(senderWalletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        senderWalletCurrentBalanceValueLabel.setText(
            UIUtils.FormatCurrency(senderWallet.GetBalance()));
    }

    private void UpdateReceiverWalletBalance()
    {
        String receiverWalletName = receiverWalletComboBox.getValue();

        if (receiverWalletName == null)
        {
            return;
        }

        Wallet receiverWallet = wallets.stream()
                                    .filter(w -> w.GetName().equals(receiverWalletName))
                                    .findFirst()
                                    .get();

        if (receiverWallet.GetBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.SetLabelStyle(receiverWalletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(receiverWalletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        receiverWalletCurrentBalanceValueLabel.setText(
            UIUtils.FormatCurrency(receiverWallet.GetBalance()));
    }

    private void UpdateSenderWalletAfterBalance()
    {
        String transferValueString = transferValueField.getText();
        String senderWalletName    = senderWalletComboBox.getValue();

        if (transferValueString == null || transferValueString.strip().isEmpty() ||
            senderWalletName == null)
        {
            UIUtils.ResetLabel(senderWalletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            if (transferValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(senderWalletAfterBalanceValueLabel);
                return;
            }

            Wallet senderWallet = wallets.stream()
                                      .filter(w -> w.GetName().equals(senderWalletName))
                                      .findFirst()
                                      .get();

            BigDecimal senderWalletAfterBalance =
                senderWallet.GetBalance().subtract(transferValue);

            // Episilon is used to avoid floating point arithmetic errors
            if (senderWalletAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(senderWalletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(senderWalletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            senderWalletAfterBalanceValueLabel.setText(
                UIUtils.FormatCurrency(senderWalletAfterBalance));
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(senderWalletAfterBalanceValueLabel);
        }
    }

    private void UpdateReceiverWalletAfterBalance()
    {
        String transferValueString = transferValueField.getText();
        String receiverWalletName  = receiverWalletComboBox.getValue();

        if (transferValueString == null || transferValueString.strip().isEmpty() ||
            receiverWalletName == null)
        {
            UIUtils.ResetLabel(receiverWalletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal transferValue = new BigDecimal(transferValueString);

            if (transferValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(receiverWalletAfterBalanceValueLabel);
                return;
            }

            Wallet receiverWallet =
                wallets.stream()
                    .filter(w -> w.GetName().equals(receiverWalletName))
                    .findFirst()
                    .get();

            BigDecimal receiverWalletAfterBalance =
                receiverWallet.GetBalance().add(transferValue);

            // Episilon is used to avoid floating point arithmetic errors
            if (receiverWalletAfterBalance.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(receiverWalletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(receiverWalletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            receiverWalletAfterBalanceValueLabel.setText(
                UIUtils.FormatCurrency(receiverWalletAfterBalance));
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(receiverWalletAfterBalanceValueLabel);
        }
    }

    private void LoadWallets()
    {
        wallets = walletService.GetAllNonArchivedWalletsOrderedByName();

        senderWalletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());

        receiverWalletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());
    }
}
