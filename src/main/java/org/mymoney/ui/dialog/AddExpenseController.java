/*
 * Filename: AddExpenseController.java
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.mymoney.ui.dialog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.mymoney.entities.Category;
import org.mymoney.entities.Wallet;
import org.mymoney.services.CategoryService;
import org.mymoney.services.WalletService;
import org.mymoney.util.Constants;
import org.mymoney.util.LoggerConfig;
import org.mymoney.util.TransactionStatus;
import org.mymoney.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Expense dialog
 */
@Controller
public class AddExpenseController
{
    @FXML
    private Label walletAfterBalanceValueLabel;

    @FXML
    private Label walletCurrentBalanceValueLabel;

    @FXML
    private ComboBox<String> walletComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private TextField expenseValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker expenseDatePicker;

    private WalletService walletService;

    private CategoryService categoryService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private static final Logger m_logger = LoggerConfig.GetLogger();

    public AddExpenseController() { }

    /**
     * Constructor
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddExpenseController(WalletService   walletService,
                                CategoryService categoryService)
    {
        this.walletService   = walletService;
        this.categoryService = categoryService;
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        // Configure date picker
        UIUtils.SetDatePickerFormat(expenseDatePicker);

        // For each element in enum TransactionStatus, add its name to the
        // statusComboBox
        statusComboBox.getItems().addAll(
            Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());

        // Reset all labels
        ResetLabel(walletAfterBalanceValueLabel);
        ResetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            UpdateWalletBalance();
            WalletAfterBalance();
        });

        // Update wallet after balance when the value field changes
        expenseValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> { WalletAfterBalance(); });
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
        String    walletName         = walletComboBox.getValue();
        String    description        = descriptionField.getText();
        String    expenseValueString = expenseValueField.getText();
        String    statusString       = statusComboBox.getValue();
        String    categoryString     = categoryComboBox.getValue();
        LocalDate expenseDate        = expenseDatePicker.getValue();

        if (walletName == null || description == null || description.trim().isEmpty() ||
            expenseValueString == null || expenseValueString.trim().isEmpty() ||
            statusString == null || categoryString == null || expenseDate == null)
        {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Empty fields");
            alert.setContentText("Please fill all the fields.");
            alert.showAndWait();
            return;
        }

        try
        {
            Double expenseValue = Double.parseDouble(expenseValueString);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryString))
                                    .findFirst()
                                    .get();

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = expenseDate.atTime(currentTime);

            Long expenseId;
            expenseId = walletService.AddExpense(wallet.GetId(),
                                                 category,
                                                 dateTimeWithCurrentHour,
                                                 expenseValue,
                                                 description,
                                                 status);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);

            alert.setGraphic(new ImageView(new Image(
                this.getClass()
                    .getResource(Constants.COMMON_ICONS_PATH + "success.png")
                    .toString())));

            alert.setTitle("Success");
            alert.setHeaderText("Expense created");
            alert.setContentText("The expense was successfully created.");
            alert.showAndWait();

            m_logger.info("Expense created: " + expenseId);

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid expense value");
            alert.setContentText("Expense value must be a number.");
            alert.showAndWait();
        }
        catch (RuntimeException e)
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error while creating expense");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
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

        walletCurrentBalanceValueLabel.setText(
            UIUtils.FormatCurrency(wallet.GetBalance()));
    }

    private void WalletAfterBalance()
    {
        String expenseValueString = expenseValueField.getText();
        String walletName         = walletComboBox.getValue();

        if (expenseValueString == null || expenseValueString.trim().isEmpty() ||
            walletName == null)
        {
            ResetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            Double expenseValue = Double.parseDouble(expenseValueString);

            if (expenseValue < 0)
            {
                ResetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Double walletAfterBalanceValue = wallet.GetBalance() - expenseValue;

            // Episilon is used to avoid floating point arithmetic errors
            if (walletAfterBalanceValue < Constants.EPSILON)
            {
                // Remove old style and add negative style
                SetLabelStyle(walletAfterBalanceValueLabel,
                              Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                SetLabelStyle(walletAfterBalanceValueLabel,
                              Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(
                UIUtils.FormatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            ResetLabel(walletAfterBalanceValueLabel);
        }
    }

    private void LoadWallets()
    {
        wallets = walletService.GetAllWallets();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());
    }

    private void LoadCategories()
    {
        categories = categoryService.GetAllCategories();

        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::GetName).toList());
    }

    private void ResetLabel(Label label)
    {
        label.setText("-");
        SetLabelStyle(label, Constants.NEUTRAL_BALANCE_STYLE);
    }

    private void SetLabelStyle(Label label, String style)
    {
        label.getStyleClass().removeAll(Constants.NEGATIVE_BALANCE_STYLE,
                                        Constants.POSITIVE_BALANCE_STYLE,
                                        Constants.NEUTRAL_BALANCE_STYLE);

        label.getStyleClass().add(style);
    }

    public void SetWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.GetId() == wt.GetId()))
        {
            return;
        }

        walletComboBox.setValue(wt.GetName());

        UpdateWalletBalance();
    }
}