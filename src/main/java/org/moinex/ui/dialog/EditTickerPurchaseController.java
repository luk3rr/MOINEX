/*
 * Filename: EditTickerPurchaseController.java
 * Created on: January 11, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
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
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.entities.investment.TickerPurchase;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Ticker Purchase dialog
 */
@Controller
@NoArgsConstructor
public class EditTickerPurchaseController
{
    @FXML
    private Label tickerNameLabel;

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
    private ComboBox<Wallet> walletComboBox;

    @FXML
    private ComboBox<TransactionStatus> statusComboBox;

    @FXML
    private ComboBox<Category> categoryComboBox;

    @FXML
    private DatePicker buyDatePicker;

    private WalletService walletService;

    private CategoryService categoryService;

    private TickerService tickerService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private TickerPurchase purchase = null;

    private Wallet wallet = null;

    /**
     * Constructor
     * @param walletService Wallet service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTickerPurchaseController(WalletService   walletService,
                                        CategoryService categoryService,
                                        TickerService   tickerService)
    {
        this.walletService   = walletService;
        this.categoryService = categoryService;
        this.tickerService   = tickerService;
    }

    public void setWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId().equals(wt.getId())))
        {
            return;
        }

        this.wallet = wt;
        walletComboBox.setValue(wallet);
        updateWalletBalance();
    }

    public void setPurchase(TickerPurchase p)
    {
        this.purchase = p;
        tickerNameLabel.setText(purchase.getTicker().getName() + " (" +
                                purchase.getTicker().getSymbol() + ")");
        unitPriceField.setText(purchase.getTicker().getCurrentUnitValue().toString());

        setWalletComboBox(purchase.getWalletTransaction().getWallet());

        descriptionField.setText(purchase.getWalletTransaction().getDescription());
        unitPriceField.setText(purchase.getUnitPrice().toString());
        quantityField.setText(purchase.getQuantity().toString());
        statusComboBox.setValue(purchase.getWalletTransaction().getStatus());
        categoryComboBox.setValue(purchase.getWalletTransaction().getCategory());
        buyDatePicker.setValue(purchase.getWalletTransaction().getDate().toLocalDate());

        totalPriceLabel.setText(
            UIUtils.formatCurrency(purchase.getWalletTransaction().getAmount()));
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();

        populateComboBoxes();

        // Configure date picker
        UIUtils.setDatePickerFormat(buyDatePicker);

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceValueLabel);
        UIUtils.resetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            updateWalletBalance();
            walletAfterBalance();
        });

        configureListeners();
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
        Wallet            wallet       = walletComboBox.getValue();
        String            description  = descriptionField.getText();
        TransactionStatus status       = statusComboBox.getValue();
        Category          category     = categoryComboBox.getValue();
        String            unitPriceStr = unitPriceField.getText();
        String            quantityStr  = quantityField.getText();
        LocalDate         buyDate      = buyDatePicker.getValue();

        if (wallet == null || description == null || description.strip().isEmpty() ||
            status == null || category == null || unitPriceStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr == null ||
            quantityStr.strip().isEmpty() || buyDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            // Check if has any modification
            if (purchase.getWalletTransaction().getWallet().getId().equals(
                    wallet.getId()) &&
                purchase.getWalletTransaction().getDescription().equals(description) &&
                purchase.getWalletTransaction().getStatus().equals(status) &&
                purchase.getWalletTransaction().getCategory().getId().equals(
                    category.getId()) &&
                purchase.getUnitPrice().compareTo(unitPrice) == 0 &&
                purchase.getQuantity().compareTo(quantity) == 0 &&
                purchase.getWalletTransaction().getDate().toLocalDate().equals(buyDate))
            {
                WindowUtils.showInformationDialog(
                    "No changes",
                    "No changes were made to the purchase.");
            }
            else // If there is any modification, update the transaction
            {
                LocalTime     currentTime             = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

                purchase.getWalletTransaction().setWallet(wallet);
                purchase.getWalletTransaction().setDescription(description);
                purchase.getWalletTransaction().setStatus(status);
                purchase.getWalletTransaction().setCategory(category);
                purchase.setUnitPrice(unitPrice);
                purchase.setQuantity(quantity);
                purchase.getWalletTransaction().setDate(dateTimeWithCurrentHour);

                tickerService.updatePurchase(purchase);

                WindowUtils.showSuccessDialog("TickerPurchase updated",
                                              "TickerPurchase updated successfully");
            }

            Stage stage = (Stage)tickerNameLabel.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid number", "Invalid price or quantity");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error while updating purchase",
                                        e.getMessage());
        }
    }

    private void updateTotalPrice()
    {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr  = quantityField.getText();

        BigDecimal totalPrice = new BigDecimal("0.00");

        if (unitPriceStr == null || quantityStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr.strip().isEmpty())
        {
            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);
            BigDecimal quantity  = new BigDecimal(quantityStr);

            totalPrice = unitPrice.multiply(quantity);

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid number", "Invalid price or quantity");

            totalPrice = new BigDecimal("0.00");

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        }
    }

    private void updateWalletBalance()
    {
        Wallet wallet = walletComboBox.getValue();

        if (wallet == null)
        {
            return;
        }

        if (wallet.getBalance().compareTo(BigDecimal.ZERO) < 0)
        {
            UIUtils.setLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEGATIVE_BALANCE_STYLE);
        }
        else
        {
            UIUtils.setLabelStyle(walletCurrentBalanceValueLabel,
                                  Constants.NEUTRAL_BALANCE_STYLE);
        }

        walletCurrentBalanceValueLabel.setText(
            UIUtils.formatCurrency(wallet.getBalance()));
    }

    private void walletAfterBalance()
    {
        String unitPriceStr = unitPriceField.getText();
        String quantityStr  = quantityField.getText();
        Wallet wallet       = walletComboBox.getValue();

        if (unitPriceStr == null || unitPriceStr.strip().isEmpty() ||
            quantityStr == null || quantityStr.strip().isEmpty() || wallet == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal buyValue =
                new BigDecimal(unitPriceStr).multiply(new BigDecimal(quantityStr));

            if (buyValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (purchase.getWalletTransaction().getStatus().equals(
                    TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    buyValue.subtract(purchase.getWalletTransaction().getAmount());

                walletAfterBalanceValue = wallet.getBalance().subtract(diff);
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                walletAfterBalanceValue = wallet.getBalance().subtract(buyValue);
            }

            // Episilon is used to avoid floating point arithmetic errors
            if (walletAfterBalanceValue.compareTo(BigDecimal.ZERO) < 0)
            {
                // Remove old style and add negative style
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEGATIVE_BALANCE_STYLE);
            }
            else
            {
                // Remove old style and add neutral style
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel,
                                      Constants.NEUTRAL_BALANCE_STYLE);
            }

            walletAfterBalanceValueLabel.setText(
                UIUtils.formatCurrency(walletAfterBalanceValue));
        }
        catch (NumberFormatException e)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
        }
    }

    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void loadCategoriesFromDatabase()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    private void populateComboBoxes()
    {
        walletComboBox.getItems().addAll(wallets);
        statusComboBox.getItems().addAll(Arrays.asList(TransactionStatus.values()));
        categoryComboBox.getItems().addAll(categories);

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty())
        {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an dividend");
        }
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(statusComboBox, TransactionStatus::name);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }

    private void configureListeners()
    {
        // Update wallet after balance when the value field changes
        unitPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
            {
                unitPriceField.setText(oldValue);
            }
            else
            {
                updateTotalPrice();
                walletAfterBalance();
            }
        });

        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
            {
                quantityField.setText(oldValue);
            }
            else
            {
                updateTotalPrice();
                walletAfterBalance();
            }
        });

        unitPriceField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateTotalPrice();
            walletAfterBalance();
        });

        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateTotalPrice();
            walletAfterBalance();
        });
    }
}
