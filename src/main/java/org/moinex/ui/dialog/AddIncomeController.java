/*
 * Filename: AddIncomeController.java
 * Created on: October  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.services.CalculatorService;
import org.moinex.services.CategoryService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.util.Constants;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Income dialog
 */
@Controller
@NoArgsConstructor
public class AddIncomeController
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
    private TextField incomeValueField;

    @FXML
    private TextField descriptionField;

    @FXML
    private DatePicker incomeDatePicker;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private Popup suggestionsPopup;

    private ListView<WalletTransaction> suggestionListView;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private CategoryService categoryService;

    private CalculatorService calculatorService;

    private List<Wallet> wallets;

    private List<Category> categories;

    private List<WalletTransaction> suggestions;

    private ChangeListener<String> descriptionFieldListener;

    /**
     * Constructor
     * @param walletService WalletService
     * @param walletTransactionService WalletTransactionService
     * @param categoryService CategoryService
     * @param calculatorService CalculatorService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddIncomeController(WalletService            walletService,
                               WalletTransactionService walletTransactionService,
                               CategoryService          categoryService,
                               CalculatorService        calculatorService)
    {
        this.walletService            = walletService;
        this.walletTransactionService = walletTransactionService;
        this.categoryService          = categoryService;
        this.calculatorService        = calculatorService;
    }

    public void setWalletComboBox(Wallet wt)
    {
        if (wallets.stream().noneMatch(w -> w.getId() == wt.getId()))
        {
            return;
        }

        walletComboBox.setValue(wt.getName());

        updateWalletBalance();
    }

    @FXML
    private void initialize()
    {
        loadWalletsFromDatabase();
        loadCategoriesFromDatabase();
        loadSuggestionsFromDatabase();

        // Configure date picker
        UIUtils.setDatePickerFormat(incomeDatePicker);

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
        Stage stage = (Stage)descriptionField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String    walletName        = walletComboBox.getValue();
        String    description       = descriptionField.getText();
        String    incomeValueString = incomeValueField.getText();
        String    statusString      = statusComboBox.getValue();
        String    categoryString    = categoryComboBox.getValue();
        LocalDate incomeDate        = incomeDatePicker.getValue();

        if (walletName == null || description == null ||
            description.strip().isEmpty() || incomeValueString == null ||
            incomeValueString.strip().isEmpty() || statusString == null ||
            categoryString == null || incomeDate == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");
            return;
        }

        try
        {
            BigDecimal incomeValue = new BigDecimal(incomeValueString);

            Wallet wallet =
                wallets.stream()
                    .filter(w -> w.getName().equals(walletName))
                    .findFirst()
                    .orElseThrow(()
                                     -> new EntityNotFoundException(
                                         "Wallet not found with name: " + walletName));

            Category category =
                categories.stream()
                    .filter(c -> c.getName().equals(categoryString))
                    .findFirst()
                    .orElseThrow(
                        ()
                            -> new EntityNotFoundException(
                                "Category not found with name: " + categoryString));

            TransactionStatus status = TransactionStatus.valueOf(statusString);

            LocalTime     currentTime             = LocalTime.now();
            LocalDateTime dateTimeWithCurrentHour = incomeDate.atTime(currentTime);

            walletTransactionService.addIncome(wallet.getId(),
                                               category,
                                               dateTimeWithCurrentHour,
                                               incomeValue,
                                               description,
                                               status);

            WindowUtils.showSuccessDialog("Income created",
                                          "The income was successfully created.");

            Stage stage = (Stage)descriptionField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid income value",
                                        "Income value must be a number.");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error while creating income", e.getMessage());
        }
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.openPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> getResultFromCalculator()));
    }

    private void getResultFromCalculator()
    {
        // If the user saved the result, set it in the incomeValueField
        String result = calculatorService.getResult();

        if (result != null)
        {
            try
            {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0)
                {
                    WindowUtils.showInformationDialog("Invalid value",
                                                      "The value must be positive");
                    return;
                }

                // Round the result to 2 decimal places
                result = resultValue.setScale(2, RoundingMode.HALF_UP).toString();

                incomeValueField.setText(result);
            }
            catch (NumberFormatException e)
            {
                // Must be unreachable
                WindowUtils.showErrorDialog("Invalid value",
                                            "The value must be a number");
            }
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
                                     "Wallet not found with name: " + walletName));

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
        String incomeValueString = incomeValueField.getText();
        String walletName        = walletComboBox.getValue();

        if (incomeValueString == null || incomeValueString.strip().isEmpty() ||
            walletName == null)
        {
            UIUtils.resetLabel(walletAfterBalanceValueLabel);
            return;
        }

        try
        {
            BigDecimal incomeValue = new BigDecimal(incomeValueString);

            if (incomeValue.compareTo(BigDecimal.ZERO) < 0)
            {
                UIUtils.resetLabel(walletAfterBalanceValueLabel);
                return;
            }

            Wallet wallet =
                wallets.stream()
                    .filter(w -> w.getName().equals(walletName))
                    .findFirst()
                    .orElseThrow(()
                                     -> new EntityNotFoundException(
                                         "Wallet not found with name: " + walletName));

            BigDecimal walletAfterBalanceValue = wallet.getBalance().add(incomeValue);

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
                "You need to add a category before adding an income");
        }
    }

    private void loadSuggestionsFromDatabase()
    {
        suggestions = walletTransactionService.getIncomeSuggestions();
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
        incomeValueField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.MONETARY_VALUE_REGEX))
                {
                    incomeValueField.setText(oldValue);
                }
                else
                {
                    walletAfterBalance();
                }
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

        // By default, the SPACE key is used to select an item in the ListView.
        // This behavior is not desired in this case, so the event is consumed
        suggestionListView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE)
            {
                event.consume(); // Do not propagate the event
            }
        });

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

        incomeValueField.setText(wt.getAmount().toString());
        statusComboBox.setValue(wt.getStatus().name());
        categoryComboBox.setValue(wt.getCategory().getName());

        updateWalletBalance();
        walletAfterBalance();
    }
}
