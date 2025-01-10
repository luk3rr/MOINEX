/*
 * Filename: EditPurchaseController.java
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.entities.investment.Purchase;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Purchase dialog
 */
@Controller
public class EditPurchaseController
{
    @FXML
    private Label tickerNameLabel;

    @FXML
    private ComboBox<String> walletComboBox;

    @FXML
    private Label walletAfterBalanceValueLabel;

    @FXML
    private Label walletCurrentBalanceValueLabel;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField unitPriceField;

    @FXML
    private TextField quantityField;

    @FXML
    private Label totalPriceLabel;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private DatePicker buyDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private WalletService walletService;

    private CategoryService categoryService;

    private TickerService tickerService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private Purchase purchase;

    /**
     * Constructor
     * @param walletService Wallet service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditPurchaseController(WalletService   walletService,
                                  CategoryService categoryService,
                                  TickerService   tickerService)
    {
        this.walletService   = walletService;
        this.categoryService = categoryService;
        this.tickerService   = tickerService;
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

    public void SetPurchase(Purchase p)
    {
        this.purchase = p;

        tickerNameLabel.setText(p.GetTicker().GetName() + " (" +
                                p.GetTicker().GetSymbol() + ")");
        unitPriceField.setText(p.GetTicker().GetCurrentUnitValue().toString());

        SetWalletComboBox(p.GetWalletTransaction().GetWallet());

        descriptionField.setText(p.GetWalletTransaction().GetDescription());
        unitPriceField.setText(p.GetUnitPrice().toString());
        quantityField.setText(p.GetQuantity().toString());
        statusComboBox.setValue(p.GetWalletTransaction().GetStatus().name());
        categoryComboBox.setValue(p.GetWalletTransaction().GetCategory().GetName());
        buyDatePicker.setValue(p.GetWalletTransaction().GetDate().toLocalDate());

        totalPriceLabel.setText(
            UIUtils.FormatCurrency(p.GetWalletTransaction().GetAmount()));
    }

    @FXML
    private void initialize()
    {
        LoadWallets();
        LoadCategories();

        // Configure date picker
        UIUtils.SetDatePickerFormat(buyDatePicker);

        // For each element in enum TransactionStatus, add its name to the
        // statusComboBox
        statusComboBox.getItems().addAll(
            Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());

        // Reset all labels
        UIUtils.ResetLabel(walletAfterBalanceValueLabel);
        UIUtils.ResetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            UpdateWalletBalance();
            WalletAfterBalance();
        });

        ConfigureListeners();
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)tickerNameLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String    walletName     = walletComboBox.getValue();
        String    description    = descriptionField.getText();
        String    statusString   = statusComboBox.getValue();
        String    categoryString = categoryComboBox.getValue();
        String    unitPriceStr   = unitPriceField.getText();
        String    quantityStr    = quantityField.getText();
        LocalDate buyDate        = buyDatePicker.getValue();

        if (walletName == null || walletName.strip().isEmpty() || description == null ||
            description.strip().isEmpty() || statusString == null ||
            categoryString == null || unitPriceStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr == null ||
            quantityStr.strip().isEmpty() || buyDate == null)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields");

            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            Category category = categories.stream()
                                    .filter(c -> c.GetName().equals(categoryString))
                                    .findFirst()
                                    .get();

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            // Check if has any modification
            if (purchase.GetWalletTransaction().GetWallet().GetId() == wallet.GetId() &&
                purchase.GetWalletTransaction().GetDescription().equals(description) &&
                purchase.GetWalletTransaction().GetStatus().equals(status) &&
                purchase.GetWalletTransaction().GetCategory().GetId() ==
                    category.GetId() &&
                purchase.GetUnitPrice().compareTo(unitPrice) == 0 &&
                purchase.GetQuantity().compareTo(quantity) == 0 &&
                purchase.GetWalletTransaction().GetDate().toLocalDate().equals(buyDate))
            {
                WindowUtils.ShowInformationDialog(
                    "Information",
                    "No changes",
                    "No changes were made to the purchase.");
            }
            else // If there is any modification, update the transaction
            {
                LocalTime     currentTime             = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

                purchase.GetWalletTransaction().SetWallet(wallet);
                purchase.GetWalletTransaction().SetDescription(description);
                purchase.GetWalletTransaction().SetStatus(status);
                purchase.GetWalletTransaction().SetCategory(category);
                purchase.SetUnitPrice(unitPrice);
                purchase.SetQuantity(quantity);
                purchase.GetWalletTransaction().SetDate(dateTimeWithCurrentHour);

                tickerService.UpdatePurchase(purchase);

                WindowUtils.ShowSuccessDialog("Success",
                                              "Purchase updated",
                                              "Purchase updated successfully");
            }

            Stage stage = (Stage)tickerNameLabel.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while updating purchase",
                                        e.getMessage());
        }
    }

    private void UpdateTotalPrice()
    {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr  = quantityField.getText();

        BigDecimal totalPrice = new BigDecimal("0.00");

        if (unitPriceStr == null || quantityStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr.strip().isEmpty())
        {
            totalPriceLabel.setText(UIUtils.FormatCurrency(totalPrice));
            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity  = new BigDecimal(quantityStr);

            totalPrice = unitPrice.multiply(quantity);

            totalPriceLabel.setText(UIUtils.FormatCurrency(totalPrice));
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");

            totalPrice = new BigDecimal("0.00");

            totalPriceLabel.setText(UIUtils.FormatCurrency(totalPrice));
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

        if (wallet.GetBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.SetLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.SetLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        walletCurrentBalanceValueLabel.setText(
            UIUtils.FormatCurrency(wallet.GetBalance()));
    }

    private void WalletAfterBalance()
    {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr  = quantityField.getText();
        String walletName   = walletComboBox.getValue();

        if (unitPriceStr == null || unitPriceStr.strip().isEmpty() ||
            quantityStr == null || quantityStr.strip().isEmpty() || walletName == null)
        {
            UIUtils.ResetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal buyValue =
                new BigDecimal(unitPriceStr).multiply(new BigDecimal(quantityStr));

            if (buyValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.ResetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.GetName().equals(walletName))
                                .findFirst()
                                .get();

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (purchase.GetWalletTransaction().GetStatus().equals(
                    TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    buyValue.subtract(purchase.GetWalletTransaction().GetAmount());

                walletAfterBalanceValue = wallet.GetBalance().subtract(diff);
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                walletAfterBalanceValue = wallet.GetBalance().subtract(buyValue);
            }

            // Episilon is used to avoid floating point arithmetic errors
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.SetLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.SetLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(
                UIUtils.FormatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.ResetLabel(walletAfterBalanceValueLabel);
        }
    }

    private void LoadWallets()
    {
        wallets = walletService.GetAllNonArchivedWalletsOrderedByName();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::GetName).toList());
    }

    private void LoadCategories()
    {
        categories = categoryService.GetNonArchivedCategoriesOrderedByName();

        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::GetName).toList());

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.size() == 0)
        {
            UIUtils.AddTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an dividend");
        }
    }

    private void ConfigureListeners()
    {
        // Update wallet after balance when the value field changes
        unitPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
            {
                unitPriceField.setText(oldValue);
            }
            else
            {
                UpdateTotalPrice();
                WalletAfterBalance();
            }
        });

        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
            {
                quantityField.setText(oldValue);
            }
            else
            {
                UpdateTotalPrice();
                WalletAfterBalance();
            }
        });

        unitPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            UpdateTotalPrice();
            WalletAfterBalance();
        });

        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            UpdateTotalPrice();
            WalletAfterBalance();
        });
    }
}
