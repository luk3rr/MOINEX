/*
 * Filename: BaseDividendManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.investment.Ticker;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.entities.wallettransaction.WalletTransaction;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.SuggestionsHandlerHelper;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Base class to implement the common behavior of the Add and Edit Dividend
 */
@NoArgsConstructor
public abstract class BaseDividendManagement
{
    @FXML
    protected Label tickerNameLabel;

    @FXML
    protected Label walletAfterBalanceValueLabel;

    @FXML
    protected Label walletCurrentBalanceValueLabel;

    @FXML
    protected ComboBox<Wallet> walletComboBox;

    @FXML
    protected ComboBox<TransactionStatus> statusComboBox;

    @FXML
    protected ComboBox<Category> categoryComboBox;

    @FXML
    protected TextField dividendValueField;

    @FXML
    protected TextField descriptionField;

    @FXML
    protected DatePicker dividendDatePicker;

    @Autowired
    protected ConfigurableApplicationContext springContext;

    protected SuggestionsHandlerHelper<WalletTransaction> suggestionsHandler;

    protected WalletService walletService;

    protected WalletTransactionService walletTransactionService;

    protected CategoryService categoryService;

    protected CalculatorService calculatorService;

    protected TickerService tickerService;

    protected List<Wallet> wallets;

    protected List<Category> categories;

    protected Wallet wallet = null;

    protected Ticker ticker = null;

    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @param tickerService TickerService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseDividendManagement(WalletService            walletService,
                                     WalletTransactionService walletTransactionService,
                                     CategoryService          categoryService,
                                     CalculatorService        calculatorService,
                                     TickerService            tickerService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService          = categoryService;
        this.calculatorService        = calculatorService;
        this.tickerService            = tickerService;
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

    public void setTicker(Ticker tk)
    {
        this.ticker = tk;

        tickerNameLabel.setText(ticker.getName() + " (" + ticker.getSymbol() + ")");
    }

    @FXML
    protected void initialize()
    {
        configureListeners();
        configureSuggestions();
        configureComboBoxes();

        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();
        loadSuggestionsFromDatabase();

        populateComboBoxes();

        // Configure date picker
        UIUtils.setDatePickerFormat(dividendDatePicker);

        // Reset all labels
        UIUtils.resetLabel(walletAfterBalanceValueLabel);
        UIUtils.resetLabel(walletCurrentBalanceValueLabel);

        walletComboBox.setOnAction(e -> {
            updateWalletBalance();
            walletAfterBalance();
        });
    }

    @FXML
    protected void handleCancel()
    {
        Stage stage = (Stage)descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected abstract void handleSave();

    @FXML
    protected void handleOpenCalculator()
    {
        WindowUtils.openPopupWindow(
            Constants.CALCULATOR_FXML,
            "Calculator",
            springContext,
            (CalculatorController controller)
                -> {},
            List.of(
                () -> calculatorService.updateComponentWithResult(dividendValueField)));
    }

    protected void updateWalletBalance()
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

    protected void walletAfterBalance()
    {
        String dividendValueString = dividendValueField.getText();
        Wallet wallet              = walletComboBox.getValue();

        if (dividendValueString == null || dividendValueString.strip().isEmpty() ||
            wallet == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal dividendValue = new BigDecimal(dividendValueString);

            if (dividendValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            BigDecimal walletAfterBalanceValue = wallet.getBalance().add(dividendValue);

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

    protected void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    protected void loadCategoriesFromDatabase()
    {
        categories = categoryService.getNonArchivedCategoriesOrderedByName();
    }

    protected void loadSuggestionsFromDatabase()
    {
        suggestionsHandler.setSuggestions(
            walletTransactionService.getIncomeSuggestions());
    }

    protected void populateComboBoxes()
    {
        walletComboBox.getItems().setAll(wallets);
        statusComboBox.getItems().addAll(Arrays.asList(TransactionStatus.values()));
        categoryComboBox.getItems().setAll(categories);

        // If there are no categories, add a tooltip to the categoryComboBox
        // to inform the user that a category is needed
        if (categories.isEmpty())
        {
            UIUtils.addTooltipToNode(
                categoryComboBox,
                "You need to add a category before adding an dividend");
        }
    }

    protected void configureSuggestions()
    {
        Function<WalletTransaction, String> filterFunction =
            WalletTransaction::getDescription;

        // Format:
        //    Description
        //    Amount | Wallet | Category
        Function<WalletTransaction, String> displayFunction = wt
            -> String.format("%s\n%s | %s | %s ",
                             wt.getDescription(),
                             UIUtils.formatCurrency(wt.getAmount()),
                             wt.getWallet().getName(),
                             wt.getCategory().getName());

        Consumer<WalletTransaction> onSelectCallback =
            selectedTransaction -> fillFieldsWithTransaction(selectedTransaction);

        suggestionsHandler = new SuggestionsHandlerHelper<>(descriptionField,
                                                            filterFunction,
                                                            displayFunction,
                                                            onSelectCallback);

        suggestionsHandler.enable();
    }

    protected void configureComboBoxes()
    {
        UIUtils.configureComboBox(walletComboBox, Wallet::getName);
        UIUtils.configureComboBox(statusComboBox, TransactionStatus::name);
        UIUtils.configureComboBox(categoryComboBox, Category::getName);
    }

    protected void configureListeners()
    {
        // Update wallet after balance when the value field changes
        dividendValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    dividendValueField.setText(oldValue);
                }
                else
                {
                    walletAfterBalance();
                }
            });
    }

    protected void fillFieldsWithTransaction(WalletTransaction wt)
    {
        walletComboBox.setValue(wt.getWallet());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        suggestionsHandler.disable();

        descriptionField.setText(wt.getDescription());

        suggestionsHandler.enable();

        dividendValueField.setText(wt.getAmount().toString());
        statusComboBox.setValue(wt.getStatus());
        categoryComboBox.setValue(wt.getCategory());

        updateWalletBalance();
        walletAfterBalance();
    }
}
