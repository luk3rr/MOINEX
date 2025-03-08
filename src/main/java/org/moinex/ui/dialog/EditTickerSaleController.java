/*
 * Filename: EditTickerSaleController.java
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
import org.moinex.entities.investment.TickerSale;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Ticker Sale dialog
 */
@Controller
@NoArgsConstructor
public class EditTickerSaleController
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
    private DatePicker saleDatePicker;

    private WalletService walletService;

    private CategoryService categoryService;

    private TickerService tickerService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private TickerSale sale;

    /**
     * Constructor
     * @param walletService Wallet service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTickerSaleController(WalletService   walletService,
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

        walletComboBox.setValue(wt.getName());

        updateWalletBalance();
    }

    public void setSale(TickerSale s)
    {
        this.sale = s;

        tickerNameLabel.setText(s.getTicker().getName() + " (" +
                                s.getTicker().getSymbol() + ")");
        unitPriceField.setText(s.getTicker().getCurrentUnitValue().toString());

        setWalletComboBox(s.getWalletTransaction().getWallet());

        descriptionField.setText(s.getWalletTransaction().getDescription());
        unitPriceField.setText(s.getUnitPrice().toString());
        quantityField.setText(s.getQuantity().toString());
        statusComboBox.setValue(s.getWalletTransaction().getStatus().name());
        categoryComboBox.setValue(s.getWalletTransaction().getCategory().getName());
        saleDatePicker.setValue(s.getWalletTransaction().getDate().toLocalDate());

        totalPriceLabel.setText(
            UIUtils.formatCurrency(s.getWalletTransaction().getAmount()));
    }

    @FXML
    private void initialize()
    {
        loadwallets();
        loadcategories();

        // Configure date picker
        UIUtils.setDatePickerFormat(saleDatePicker);

        // For each element in enum TransactionStatus, add its name to the
        // statusComboBox
        statusComboBox.getItems().addAll(
            Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());

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
        String    walletName     = walletComboBox.getValue();
        String    description    = descriptionField.getText();
        String    statusString   = statusComboBox.getValue();
        String    categoryString = categoryComboBox.getValue();
        String    unitPriceStr   = unitPriceField.getText();
        String    quantityStr    = quantityField.getText();
        LocalDate saleDate       = saleDatePicker.getValue();

        if (walletName == null || walletName.strip().isEmpty() || description == null ||
            description.strip().isEmpty() || statusString == null ||
            categoryString == null || unitPriceStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr == null ||
            quantityStr.strip().isEmpty() || saleDate == null)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all the fields");

            return;
        }

        try
        {
            BigDecimal unitPrice = new BigDecimal(unitPriceStr);

            BigDecimal quantity = new BigDecimal(quantityStr);

            Wallet wallet = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .orElseThrow(()
                                                 -> new EntityNotFoundException(
                                                     "Wallet with name: " + walletName +
                                                     " not found"));

            Category category =
                categories.stream()
                    .filter(c -> c.getName().equals(categoryString))
                    .findFirst()
                    .orElseThrow(()
                                     -> new EntityNotFoundException(
                                         "Category with name: " + categoryString +
                                         " not found"));

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            // Check if has any modification
            if (sale.getWalletTransaction().getWallet().getId().equals(
                    wallet.getId()) &&
                sale.getWalletTransaction().getDescription().equals(description) &&
                sale.getWalletTransaction().getStatus().equals(status) &&
                sale.getWalletTransaction().getCategory().getId().equals(
                    category.getId()) &&
                sale.getUnitPrice().compareTo(unitPrice) == 0 &&
                sale.getQuantity().compareTo(quantity) == 0 &&
                sale.getWalletTransaction().getDate().toLocalDate().equals(saleDate))
            {
                WindowUtils.showInformationDialog("Info",
                                                  "No changes",
                                                  "No changes were made to the sale");
            }
            else // If there is any modification, update the transaction
            {
                LocalTime     currentTime             = LocalTime.now();
                LocalDateTime dateTimeWithCurrentHour = saleDate.atTime(currentTime);

                sale.getWalletTransaction().setWallet(wallet);
                sale.getWalletTransaction().setDescription(description);
                sale.getWalletTransaction().setStatus(status);
                sale.getWalletTransaction().setCategory(category);
                sale.setUnitPrice(unitPrice);
                sale.setQuantity(quantity);
                sale.getWalletTransaction().setDate(dateTimeWithCurrentHour);

                tickerService.updateSale(sale);

                WindowUtils.showSuccessDialog("Success",
                                              "TickerSale updated",
                                              "TickerSale updated successfully");
            }

            Stage stage = (Stage)tickerNameLabel.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error",
                                        "Error while updating sale",
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
            WindowUtils.showErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");

            totalPrice = new BigDecimal("0.00");

            totalPriceLabel.setText(UIUtils.formatCurrency(totalPrice));
        }
    }

    private void updateWalletBalance()
    {
        String walletName = walletComboBox.getValue();

        if (walletName == null)
        {
            return;
        }

        Wallet wallet =
            wallets.stream()
                .filter(w -> w.getName().equals(walletName))
                .findFirst()
                .orElseThrow(()
                                 -> new EntityNotFoundException(
                                     "Wallet with name: " + walletName + " not found"));

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
        String walletName   = walletComboBox.getValue();

        if (unitPriceStr == null || unitPriceStr.strip().isEmpty() ||
            quantityStr == null || quantityStr.strip().isEmpty() || walletName == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal saleValue =
                new BigDecimal(unitPriceStr).multiply(new BigDecimal(quantityStr));

            if (saleValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .orElseThrow(()
                                                 -> new EntityNotFoundException(
                                                     "Wallet with name: " + walletName +
                                                     " not found"));

            BigDecimal walletAfterBalanceValue = BigDecimal.ZERO;

            if (sale.getWalletTransaction().getStatus().equals(
                    TransactionStatus.CONFIRMED))
            {
                // If the transaction is confirmed, the balance will be updated
                // based on the difference between the new and the old value
                BigDecimal diff =
                    saleValue.subtract(sale.getWalletTransaction().getAmount());

                walletAfterBalanceValue = wallet.getBalance().add(diff);
            }
            else
            {
                // If the transaction is not confirmed, the balance will be
                // updated based on the new value
                walletAfterBalanceValue = wallet.getBalance().add(saleValue);
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

    private void loadwallets()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::getName).toList());
    }

    private void loadcategories()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();

        categoryComboBox.getItems().addAll(
            categories.stream().map(Category::getName).toList());

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty())
        {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an dividend");
        }
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
