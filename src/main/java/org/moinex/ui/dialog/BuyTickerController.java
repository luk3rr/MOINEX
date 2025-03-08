/*
 * Filename: BuyTicker.java
 * Created on: January  9, 2025
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
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.CategoryService;
import org.moinex.services.TickerService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.TransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Buy Ticker dialog
 */
@Controller
@NoArgsConstructor
public class BuyTickerController
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

    private Popup suggestionsPopup;

    private ListView<WalletTransaction> suggestionListView;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CategoryService categoryService;

    private TickerService tickerService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private List<WalletTransaction> suggestions;

    private ChangeListener<String> descriptionFieldListener;

    private Ticker ticker;

    /**
     * Constructor
     * @param walletService Wallet service
     * @param walletTransactionService Wallet transaction service
     * @param categoryService Category service
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public BuyTickerController(WalletService            walletService,
                               WalletTransactionService walletTransactionService,
                               CategoryService          categoryService,
                               TickerService            tickerService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService          = categoryService;
        this.tickerService            = tickerService;
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

    public void setTicker(Ticker ticker)
    {
        this.ticker = ticker;

        tickerNameLabel.setText(ticker.getName() + " (" + ticker.getSymbol() + ")");
        unitPriceField.setText(ticker.getCurrentUnitValue().toString());
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();
        loadSuggestionsFromDatabase();

        // Configure date picker
        UIUtils.setDatePickerFormat(buyDatePicker);

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

        configureSuggestionsListView();
        configureSuggestionsPopup();
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
        LocalDate buyDate        = buyDatePicker.getValue();

        if (walletName == null || walletName.strip().isEmpty() || description == null ||
            description.strip().isEmpty() || statusString == null ||
            categoryString == null || unitPriceStr == null ||
            unitPriceStr.strip().isEmpty() || quantityStr == null ||
            quantityStr.strip().isEmpty() || buyDate == null)
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
                                                     "Wallet with name " + walletName +
                                                     " not found"));

            Category category =
                categories.stream()
                    .filter(c -> c.getName().equals(categoryString))
                    .findFirst()
                    .orElseThrow(
                        ()
                            -> new EntityNotFoundException(
                                "Category with name " + categoryString + " not found"));

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = buyDate.atTime(currentTime);

            tickerService.addPurchase(ticker.getId(),
                                      wallet.getId(),
                                      quantity,
                                      unitPrice,
                                      category,
                                      dateTimeWithCurrentHour,
                                      description,
                                      status);

            WindowUtils.showSuccessDialog("Success",
                                          "Purchase added",
                                          "Purchase added successfully");

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
                                        "Error while buying ticker",
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
                                     "Wallet with name " + walletName + " not found"));

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
            BigDecimal buyValue =
                new BigDecimal(unitPriceStr).multiply(new BigDecimal(quantityStr));

            if (buyValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet = wallets.stream()
                                .filter(w -> w.getName().equals(walletName))
                                .findFirst()
                                .orElseThrow(()
                                                 -> new EntityNotFoundException(
                                                     "Wallet with name " + walletName +
                                                     " not found"));

            BigDecimal walletAfterBalanceValue = wallet.getBalance().subtract(buyValue);

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

        walletComboBox.getItems().addAll(
            wallets.stream().map(Wallet::getName).toList());
    }

    private void loadCategoriesFromDatabase()
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

    private void loadSuggestionsFromDatabase()
    {
        suggestions = walletTransactionService.getExpenseSuggestions();
    }

    private void configureListeners()
    {
        // Store the listener in a variable to be able to disable and enable it
        // when needed
        descriptionFieldListener = (observable, oldValue, newValue) ->
        {
            if (newValue.strip().isEmpty())
            {
                suggestionsPopup.hide();
                return;
            }

            suggestionListView.getItems().clear();

            // Filter the suggestions list to show only the transactions that
            // contain similar descriptions to the one typed by the user
            List<WalletTransaction> filteredSuggestions =
                suggestions.stream()
                    .filter(tx
                            -> tx.getDescription().toLowerCase().contains(
                                newValue.toLowerCase()))
                    .toList();

            if (filteredSuggestions.size() > Constants.SUGGESTIONS_MAX_ITEMS)
            {
                filteredSuggestions =
                    filteredSuggestions.subList(0, Constants.SUGGESTIONS_MAX_ITEMS);
            }

            suggestionListView.getItems().addAll(filteredSuggestions);

            if (!filteredSuggestions.isEmpty())
            {
                adjustPopupWidth();
                adjustPopupHeight();

                suggestionsPopup.show(
                    descriptionField,
                    descriptionField.localToScene(0, 0).getX() +
                        descriptionField.getScene().getWindow().getX() +
                        descriptionField.getScene().getX(),
                    descriptionField.localToScene(0, 0).getY() +
                        descriptionField.getScene().getWindow().getY() +
                        descriptionField.getScene().getY() +
                        descriptionField.getHeight());
            }
            else
            {
                suggestionsPopup.hide();
            }
        };

        descriptionField.textProperty().addListener(descriptionFieldListener);

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

    private void configureSuggestionsListView()
    {
        suggestionListView = new ListView<>();

        // Set the cell factory to display the description, amount, wallet and
        // category of the transaction
        // Format:
        //    Description
        //    Amount | Wallet | Category
        suggestionListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(WalletTransaction item, boolean empty)
            {
                super.updateItem(item, empty);
                if (empty || item == null)
                {
                    setText(null);
                }
                else
                {
                    VBox cellContent = new VBox();
                    cellContent.setSpacing(2);

                    Label descriptionLabel = new Label(item.getDescription());

                    String infoString = UIUtils.formatCurrency(item.getAmount()) +
                                        " | " + item.getWallet().getName() + " | " +
                                        item.getCategory().getName();

                    Label infoLabel = new Label(infoString);

                    cellContent.getChildren().addAll(descriptionLabel, infoLabel);

                    setGraphic(cellContent);
                }
            }
        });

        suggestionListView.setPrefWidth(Region.USE_COMPUTED_SIZE);
        suggestionListView.setPrefHeight(Region.USE_COMPUTED_SIZE);

        // Add a listener to the ListView to fill the fields with the selected
        suggestionListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null)
                {
                    fillFieldsWithTransaction(newValue);
                    suggestionsPopup.hide();
                }
            });
    }

    private void configureSuggestionsPopup()
    {
        if (suggestionsPopup == null)
        {
            configureSuggestionsListView();
        }

        suggestionsPopup = new Popup();
        suggestionsPopup.setAutoHide(true);
        suggestionsPopup.setHideOnEscape(true);
        suggestionsPopup.getContent().add(suggestionListView);
    }

    private void adjustPopupWidth()
    {
        suggestionListView.setPrefWidth(descriptionField.getWidth());
    }

    private void adjustPopupHeight()
    {
        Integer itemCount = suggestionListView.getItems().size();

        Double cellHeight = 45.0;

        itemCount = Math.min(itemCount, Constants.SUGGESTIONS_MAX_ITEMS);

        Double totalHeight = itemCount * cellHeight;

        suggestionListView.setPrefHeight(totalHeight);
    }

    private void fillFieldsWithTransaction(WalletTransaction wt)
    {
        walletComboBox.setValue(wt.getWallet().getName());

        // Deactivate the listener to avoid the event of changing the text of
        // the descriptionField from being triggered. After changing the text,
        // the listener is activated again
        descriptionField.textProperty().removeListener(descriptionFieldListener);

        descriptionField.setText(wt.getDescription());

        descriptionField.textProperty().addListener(descriptionFieldListener);

        statusComboBox.setValue(wt.getStatus().name());
        categoryComboBox.setValue(wt.getCategory().getName());

        updateWalletBalance();
        walletAfterBalance();
    }
}
