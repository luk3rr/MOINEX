/*
 * Filename: TransactionController.java
 * Created on: October 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import org.moinex.entities.Category;
import org.moinex.entities.CreditCardPayment;
import org.moinex.entities.WalletTransaction;
import org.moinex.services.CategoryService;
import org.moinex.services.CreditCardService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.ResumePaneController;
import org.moinex.ui.dialog.AddExpenseController;
import org.moinex.ui.dialog.AddIncomeController;
import org.moinex.ui.dialog.EditTransactionController;
import org.moinex.ui.dialog.ManageCategoryController;
import org.moinex.ui.dialog.RecurringTransactionController;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.LoggerConfig;
import org.moinex.util.TransactionStatus;
import org.moinex.util.TransactionType;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller class for the transaction view
 * TODO: Load information from the database only when necessary
 */
@Controller
public class TransactionController
{
    private static final Logger logger = LoggerConfig.GetLogger();

    @FXML
    private AnchorPane monthResumeView;

    @FXML
    private AnchorPane yearResumeView;

    @FXML
    private ComboBox<YearMonth> monthResumeComboBox;

    @FXML
    private ComboBox<Year> yearResumeComboBox;

    @FXML
    private ComboBox<TransactionType> moneyFlowComboBox;

    @FXML
    private ComboBox<TransactionType> transactionsTypeComboBox;

    @FXML
    private DatePicker transactionsEndDatePicker;

    @FXML
    private DatePicker transactionsStartDatePicker;

    @FXML
    private TableView<WalletTransaction> transactionsTableView;

    @FXML
    private AnchorPane moneyFlowView;

    @FXML
    private TextField transactionsSearchField;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private StackedBarChart<String, Number> moneyFlowStackedBarChart;

    private WalletTransactionService walletTransactionService;

    private CreditCardService creditCardService;

    private CategoryService categoryService;

    /**
     * Constructor
     * @param walletTransactionService WalletTransactionService
     * @param creditCardService CreditCardService
     * @param categoryService CategoryService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public TransactionController(WalletTransactionService walletTransactionService,
                                 CreditCardService        creditCardService,
                                 CategoryService          categoryService)
    {
        this.walletTransactionService = walletTransactionService;
        this.creditCardService        = creditCardService;
        this.categoryService          = categoryService;
    }

    @FXML
    private void initialize()
    {
        ConfigureTableView();

        PopulateMonthResumeComboBox();
        PopulateYearComboBox();
        PopulateTransactionTypeComboBox();

        // Format the date pickers
        UIUtils.SetDatePickerFormat(transactionsStartDatePicker);
        UIUtils.SetDatePickerFormat(transactionsEndDatePicker);

        LocalDateTime currentDate = LocalDateTime.now();

        // Select the default values
        monthResumeComboBox.setValue(
            YearMonth.of(currentDate.getYear(), currentDate.getMonthValue()));

        yearResumeComboBox.setValue(Year.of(currentDate.getYear()));

        moneyFlowComboBox.setValue(TransactionType.EXPENSE);

        transactionsTypeComboBox.setValue(null); // All transactions

        // Set the start and end date pickers to the first and last day of the current
        // month
        LocalDateTime firstDayOfMonth = currentDate.withDayOfMonth(1);
        LocalDateTime lastDayOfMonth  = currentDate.withDayOfMonth(
            currentDate.getMonth().length(currentDate.toLocalDate().isLeapYear()));

        transactionsStartDatePicker.setValue(firstDayOfMonth.toLocalDate());
        transactionsEndDatePicker.setValue(lastDayOfMonth.toLocalDate());

        // Update the resumes
        UpdateMonthResume();
        UpdateYearResume();
        UpdateMoneyFlow();
        UpdateTransactionTableView();

        // Add a listener to handle user selection
        monthResumeComboBox.setOnAction(event -> { UpdateMonthResume(); });

        yearResumeComboBox.setOnAction(event -> { UpdateYearResume(); });

        moneyFlowComboBox.setOnAction(event -> { UpdateMoneyFlow(); });

        transactionsTypeComboBox.setOnAction(
            event -> { UpdateTransactionTableView(); });

        transactionsStartDatePicker.setOnAction(
            event -> { UpdateTransactionTableView(); });

        transactionsEndDatePicker.setOnAction(
            event -> { UpdateTransactionTableView(); });

        // Add listener to the search field
        transactionsSearchField.textProperty().addListener(
            (observable, oldValue, newValue) -> { UpdateTransactionTableView(); });
    }

    @FXML
    private void handleAddIncome()
    {
        WindowUtils.OpenModalWindow(Constants.ADD_INCOME_FXML,
                                    "Add new income",
                                    springContext,
                                    (AddIncomeController controller)
                                        -> {},
                                    List.of(() -> {
                                        UpdateMonthResume();
                                        UpdateYearResume();
                                        UpdateTransactionTableView();
                                        UpdateMoneyFlow();
                                    }));
    }

    @FXML
    private void handleAddExpense()
    {
        WindowUtils.OpenModalWindow(Constants.ADD_EXPENSE_FXML,
                                    "Add new expense",
                                    springContext,
                                    (AddExpenseController controller)
                                        -> {},
                                    List.of(() -> {
                                        UpdateMonthResume();
                                        UpdateYearResume();
                                        UpdateTransactionTableView();
                                        UpdateMoneyFlow();
                                    }));
    }

    @FXML
    private void handleEditTransaction()
    {
        WalletTransaction selectedTransaction =
            transactionsTableView.getSelectionModel().getSelectedItem();

        if (selectedTransaction == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No transaction selected",
                                              "Please select a transaction to edit.");

            return;
        }

        WindowUtils.OpenModalWindow(
            Constants.EDIT_TRANSACTION_FXML,
            "Edit transaction",
            springContext,
            (EditTransactionController controller)
                -> controller.SetTransaction(selectedTransaction),
            List.of(() -> {
                UpdateMonthResume();
                UpdateYearResume();
                UpdateTransactionTableView();
                UpdateMoneyFlow();
            }));
    }

    @FXML
    private void handleDeleteTransaction()
    {
        WalletTransaction selectedTransaction =
            transactionsTableView.getSelectionModel().getSelectedItem();

        if (selectedTransaction == null)
        {
            WindowUtils.ShowInformationDialog("Info",
                                              "No transaction selected",
                                              "Please select a transaction to remove.");

            return;
        }

        // Create a message to show to the user
        StringBuilder message = new StringBuilder();
        message.append("Description: ")
            .append(selectedTransaction.GetDescription())
            .append("\n")
            .append("Amount: ")
            .append(UIUtils.FormatCurrency(selectedTransaction.GetAmount()))
            .append("\n")
            .append("Date: ")
            .append(selectedTransaction.GetDate().format(
                Constants.DATE_FORMATTER_WITH_TIME))
            .append("\n")
            .append("Status: ")
            .append(selectedTransaction.GetStatus().toString())
            .append("\n")
            .append("Wallet: ")
            .append(selectedTransaction.GetWallet().GetName())
            .append("\n")
            .append("Wallet balance: ")
            .append(
                UIUtils.FormatCurrency(selectedTransaction.GetWallet().GetBalance()))
            .append("\n")
            .append("Wallet balance after deletion: ");

        if (selectedTransaction.GetStatus().equals(TransactionStatus.CONFIRMED))
        {
            if (selectedTransaction.GetType().equals(TransactionType.EXPENSE))
            {
                message
                    .append(UIUtils.FormatCurrency(
                        selectedTransaction.GetWallet().GetBalance().add(
                            selectedTransaction.GetAmount())))
                    .append("\n");
            }
            else
            {
                message
                    .append(UIUtils.FormatCurrency(
                        selectedTransaction.GetWallet().GetBalance().subtract(
                            selectedTransaction.GetAmount())))
                    .append("\n");
            }
        }
        else
        {
            message
                .append(UIUtils.FormatCurrency(
                    selectedTransaction.GetWallet().GetBalance()))
                .append("\n");
        }

        // Confirm deletion
        if (WindowUtils.ShowConfirmationDialog(
                "Confirm Deletion",
                "Are you sure you want to remove this " +
                    selectedTransaction.GetType().toString().toLowerCase() + "?",
                message.toString()))
        {
            walletTransactionService.DeleteTransaction(selectedTransaction.GetId());

            UpdateMonthResume();
            UpdateYearResume();
            UpdateTransactionTableView();
            UpdateMoneyFlow();
        }
    }

    @FXML
    private void handleRecurringTransactions()
    {
        WindowUtils.OpenModalWindow(Constants.RECURRING_TRANSACTIONS_FXML,
                                    "Recurring transactions",
                                    springContext,
                                    (RecurringTransactionController controller)
                                        -> {},
                                    List.of(() -> {
                                        UpdateMonthResume();
                                        UpdateYearResume();
                                        UpdateTransactionTableView();
                                        UpdateMoneyFlow();
                                    }));
    }

    @FXML
    private void handleManageCategories()
    {
        WindowUtils.OpenModalWindow(Constants.MANAGE_CATEGORY_FXML,
                                    "Manage categories",
                                    springContext,
                                    (ManageCategoryController controller)
                                        -> {},
                                    List.of(() -> {
                                        UpdateTransactionTableView();
                                        UpdateMoneyFlow();
                                    }));
    }

    /**
     * Update the transaction table view
     */
    private void UpdateTransactionTableView()
    {
        // Get the search text
        String similarTextOrId = transactionsSearchField.getText().toLowerCase();

        // Get selected values from the comboboxes
        TransactionType selectedTransactionType = transactionsTypeComboBox.getValue();

        LocalDateTime startDate = transactionsStartDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = transactionsEndDatePicker.getValue().atTime(23, 59, 59);

        // Clear the transaction list view
        transactionsTableView.getItems().clear();

        // Fetch all transactions within the selected range and filter by transaction
        // type. If transaction type is null, all transactions are fetched
        if (similarTextOrId.isEmpty())
        {
            walletTransactionService
                .GetNonArchivedTransactionsBetweenDates(startDate, endDate)
                .stream()
                .filter(t
                        -> selectedTransactionType == null ||
                               t.GetType().equals(selectedTransactionType))
                .forEach(transactionsTableView.getItems()::add);
        }
        else
        {
            walletTransactionService
                .GetNonArchivedTransactionsBetweenDates(startDate, endDate)
                .stream()
                .filter(t
                        -> selectedTransactionType == null ||
                               t.GetType().equals(selectedTransactionType))
                .filter(t -> {
                    String description = t.GetDescription().toLowerCase();
                    String id          = t.GetId().toString();
                    String category    = t.GetCategory().GetName().toLowerCase();
                    String wallet      = t.GetWallet().GetName().toLowerCase();
                    String amount      = t.GetAmount().toString();
                    String type        = t.GetType().toString().toLowerCase();
                    String status      = t.GetStatus().toString().toLowerCase();

                    return description.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        category.contains(similarTextOrId) ||
                        wallet.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        status.contains(similarTextOrId);
                })
                .forEach(transactionsTableView.getItems()::add);
        }

        transactionsTableView.refresh();
    }

    /**
     * Update the money flow bar chart
     */
    private void UpdateMoneyFlow()
    {
        // Get the selected transaction type
        TransactionType selectedTransactionType = moneyFlowComboBox.getValue();

        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis   numberAxis   = new NumberAxis();
        moneyFlowStackedBarChart  = new StackedBarChart<>(categoryAxis, numberAxis);

        moneyFlowStackedBarChart.setVerticalGridLinesVisible(false);
        moneyFlowView.getChildren().clear();
        moneyFlowView.getChildren().add(moneyFlowStackedBarChart);

        AnchorPane.setTopAnchor(moneyFlowStackedBarChart, 0.0);
        AnchorPane.setBottomAnchor(moneyFlowStackedBarChart, 0.0);
        AnchorPane.setLeftAnchor(moneyFlowStackedBarChart, 0.0);
        AnchorPane.setRightAnchor(moneyFlowStackedBarChart, 0.0);

        moneyFlowStackedBarChart.getData().clear();

        LocalDateTime     currentDate = LocalDateTime.now();
        DateTimeFormatter formatter   = DateTimeFormatter.ofPattern("MMM/yy");

        List<Category> categories =
            categoryService.GetNonArchivedCategoriesOrderedByName();
        Map<YearMonth, Map<Category, Double>> monthlyTotals = new LinkedHashMap<>();

        // Loop through the last few months
        for (Integer i = 0; i < Constants.XYBAR_CHART_MONTHS; i++)
        {
            // Get the date for the current month
            LocalDateTime date =
                currentDate.minusMonths(Constants.XYBAR_CHART_MONTHS - i - 1);
            YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonthValue());

            // Get confirmed transactions for the month
            List<WalletTransaction> transactions =
                walletTransactionService.GetNonArchivedConfirmedTransactionsByMonth(
                    date.getMonthValue(),
                    date.getYear());

            // Get paid credit card payments for the month
            // Only get paid payments if the selected transaction type is expense
            // Otherwise, create an empty list
            List<CreditCardPayment> creditCardPayments =
                selectedTransactionType.equals(TransactionType.EXPENSE)
                    ? creditCardService.GetAllPaidPaymentsByMonth(date.getMonthValue(),
                                                                  date.getYear())
                    : new ArrayList<>();

            // Calculate total for each category
            for (Category category : categories)
            {
                BigDecimal totalWalletTransaction =
                    transactions.stream()
                        .filter(t -> t.GetType().equals(selectedTransactionType))
                        .filter(t -> t.GetCategory().GetId() == category.GetId())
                        .map(WalletTransaction::GetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCreditCardPayment =
                    creditCardPayments.stream()
                        .filter(p
                                -> p.GetCreditCardDebt().GetCategory().GetId() ==
                                       category.GetId())
                        .map(CreditCardPayment::GetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal total = totalWalletTransaction.add(totalCreditCardPayment);

                // Store total if it's greater than zero
                if (total.compareTo(BigDecimal.ZERO) > 0)
                {
                    monthlyTotals.putIfAbsent(yearMonth, new LinkedHashMap<>());
                    monthlyTotals.get(yearMonth).put(category, total.doubleValue());
                }
            }
        }

        // Add series to the chart
        for (Category category : categories)
        {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(category.GetName());

            // Loop through the months in the order they were added
            for (YearMonth yearMonth : monthlyTotals.keySet())
            {
                Double total =
                    monthlyTotals.getOrDefault(yearMonth, new LinkedHashMap<>())
                        .getOrDefault(category, 0.0);

                // Add total to the series if it's greater than zero
                if (total > 0)
                {
                    series.getData().add(
                        new XYChart.Data<>(yearMonth.format(formatter), total));
                }
                else
                {
                    // Add zero value to keep the structure and order of the series
                    series.getData().add(
                        new XYChart.Data<>(yearMonth.format(formatter), 0.0));
                }
            }

            // Only add series to the chart if it has data greater than zero
            if (series.getData().stream().anyMatch(
                    data -> (Double)data.getYValue() > 0))
            {
                moneyFlowStackedBarChart.getData().add(series);
            }
        }

        // Calculate the maximum total for each month
        Double maxTotal = monthlyTotals.values()
                              .stream()
                              .map(monthData
                                   -> monthData.values()
                                          .stream()
                                          .mapToDouble(Double::doubleValue)
                                          .sum())
                              .max(Double::compare)
                              .orElse(0.0);

        // Set the Y-axis properties only if maxTotal is greater than 0
        Animation.SetDynamicYAxisBounds(numberAxis, maxTotal);

        for (XYChart.Series<String, Number> series : moneyFlowStackedBarChart.getData())
        {
            for (XYChart.Data<String, Number> data : series.getData())
            {
                // Calculate total for the month to find the percentage
                YearMonth yearMonth = YearMonth.parse(data.getXValue(), formatter);
                Double    monthTotal =
                    monthlyTotals.getOrDefault(yearMonth, new LinkedHashMap<>())
                        .values()
                        .stream()
                        .mapToDouble(Double::doubleValue)
                        .sum();

                // Calculate the percentage
                Double value      = (Double)data.getYValue();
                Double percentage = (monthTotal > 0) ? (value / monthTotal) * 100 : 0;

                // Add tooltip with value and percentage
                UIUtils.AddTooltipToXYChartNode(
                    data.getNode(),
                    series.getName() + ": " + UIUtils.FormatCurrency(value) + " (" +
                        UIUtils.FormatPercentage(percentage) +
                        ")\nTotal: " + UIUtils.FormatCurrency(monthTotal));

                // Animate the data after setting up the tooltip
                Animation.StackedXYChartAnimation(Collections.singletonList(data),
                                                  Collections.singletonList(value));
            }
        }
    }

    /**
     * Update the year resume view
     */
    private void UpdateYearResume()
    {
        Year selectedYear = yearResumeComboBox.getValue();

        try
        {
            FXMLLoader loader =
                new FXMLLoader(getClass().getResource(Constants.RESUME_PANE_FXML));
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            newContent.getStylesheets().add(
                getClass().getResource(Constants.COMMON_STYLE_SHEET).toExternalForm());

            ResumePaneController resumePaneController = loader.getController();
            resumePaneController.UpdateResumePane(selectedYear.getValue());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 10.0);
            AnchorPane.setRightAnchor(newContent, 10.0);

            yearResumeView.getChildren().clear();
            yearResumeView.getChildren().add(newContent);
        }
        catch (Exception e)
        {
            logger.severe("Error updating year resume: " + e.getMessage());
        }
    }

    /**
     * Update the month resume view
     */
    private void UpdateMonthResume()
    {
        YearMonth selectedYearMonth = monthResumeComboBox.getValue();

        try
        {
            FXMLLoader loader =
                new FXMLLoader(getClass().getResource(Constants.RESUME_PANE_FXML));
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            newContent.getStylesheets().add(
                getClass().getResource(Constants.COMMON_STYLE_SHEET).toExternalForm());

            ResumePaneController resumePaneController = loader.getController();
            resumePaneController.UpdateResumePane(selectedYearMonth.getMonthValue(),
                                                  selectedYearMonth.getYear());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 10.0);
            AnchorPane.setRightAnchor(newContent, 10.0);

            monthResumeView.getChildren().clear();
            monthResumeView.getChildren().add(newContent);
        }
        catch (Exception e)
        {
            logger.severe("Error updating month resume: " + e.getMessage());
        }
    }

    /**
     * Populate the year combo box with the years between the oldest transaction
     * date and the current date
     */
    private void PopulateYearComboBox()
    {
        LocalDateTime oldestWalletTransaction =
            walletTransactionService.GetOldestTransactionDate();
        LocalDateTime oldestCreditCard = creditCardService.GetEarliestPaymentDate();

        LocalDateTime oldest = oldestCreditCard.isBefore(oldestWalletTransaction)
                                   ? oldestCreditCard
                                   : oldestWalletTransaction;

        LocalDate youngest =
            LocalDate.now().plusYears(Constants.YEAR_RESUME_FUTURE_YEARS);

        // Generate a list of Year objects from the oldest transaction date to the
        // current date
        Year startYear   = Year.from(oldest);
        Year currentYear = Year.from(youngest);

        // Generate the list of years between the oldest and the current date
        List<Year> years = new ArrayList<>();
        while (!startYear.isAfter(currentYear))
        {
            years.add(currentYear);
            currentYear = currentYear.minusYears(1);
        }

        ObservableList<Year> yearList = FXCollections.observableArrayList(years);

        yearResumeComboBox.setItems(yearList);

        // Custom string converter to format the Year as "Year"
        yearResumeComboBox.setConverter(new StringConverter<Year>() {
            private final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy");

            @Override
            public String toString(Year year)
            {
                return year != null ? year.format(formatter) : "";
            }

            @Override
            public Year fromString(String string)
            {
                return Year.parse(string, formatter);
            }
        });
    }

    /**
     * Populate the transaction type combo box with the available transaction types
     */
    private void PopulateTransactionTypeComboBox()
    {
        ObservableList<TransactionType> transactionTypes =
            FXCollections.observableArrayList(TransactionType.values());

        moneyFlowComboBox.setItems(transactionTypes);

        // Make a copy of the list to add the 'All' option
        // Add 'All' option to the transaction type combo box
        // All is the first element in the list and is represented by a null value
        ObservableList<TransactionType> transactionTypesWithNull =
            FXCollections.observableArrayList(TransactionType.values());
        transactionTypesWithNull.add(0, null);

        transactionsTypeComboBox.setItems(transactionTypesWithNull);

        // Custom string converter to format the TransactionType as
        // "TransactionType"
        moneyFlowComboBox.setConverter(new StringConverter<TransactionType>() {
            @Override
            public String toString(TransactionType transactionType)
            {
                return transactionType != null ? transactionType.toString() : "";
            }

            @Override
            public TransactionType fromString(String string)
            {
                return TransactionType.valueOf(string);
            }
        });

        transactionsTypeComboBox.setConverter(new StringConverter<TransactionType>() {
            @Override
            public String toString(TransactionType transactionType)
            {
                return transactionType != null ? transactionType.toString()
                                               : "ALL"; // Show "All" instead of null
            }

            @Override
            public TransactionType fromString(String string)
            {
                return string.equals("ALL")
                    ? null
                    : TransactionType.valueOf(
                          string); // Return null if "All" is selected
            }
        });
    }

    /**
     * Populate the month resume combo box with the months between the oldest
     * transaction date and the current date
     */
    private void PopulateMonthResumeComboBox()
    {
        LocalDateTime oldestWalletTransaction =
            walletTransactionService.GetOldestTransactionDate();

        LocalDateTime oldestCreditCard = creditCardService.GetEarliestPaymentDate();

        LocalDateTime oldest = oldestCreditCard.isBefore(oldestWalletTransaction)
                                   ? oldestCreditCard
                                   : oldestWalletTransaction;

        LocalDate future =
            LocalDate.now().plusMonths(Constants.MONTH_RESUME_FUTURE_MONTHS);

        // Generate a list of YearMonth objects from the oldest transaction date to
        // the current date
        YearMonth startMonth   = YearMonth.from(oldest);
        YearMonth currentMonth = YearMonth.from(future);

        // Generate the list of months between the oldest and the current date
        List<YearMonth> months = new ArrayList<>();
        while (!startMonth.isAfter(currentMonth))
        {
            months.add(currentMonth);
            currentMonth = currentMonth.minusMonths(1);
        }

        ObservableList<YearMonth> monthYearList =
            FXCollections.observableArrayList(months);
        monthResumeComboBox.setItems(monthYearList);

        // Custom string converter to format the YearMonth as "Month/Year"
        monthResumeComboBox.setConverter(new StringConverter<YearMonth>() {
            private final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy MMM");

            @Override
            public String toString(YearMonth yearMonth)
            {
                return yearMonth != null ? yearMonth.format(formatter) : "";
            }

            @Override
            public YearMonth fromString(String string)
            {
                return YearMonth.parse(string, formatter);
            }
        });
    }

    /**
     * Configure the table view columns
     */
    private void ConfigureTableView()
    {
        TableColumn<WalletTransaction, Long> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(
            param -> new SimpleObjectProperty<>(param.getValue().GetId()));

        // Align the ID column to the center
        idColumn.setCellFactory(column -> {
            return new TableCell<WalletTransaction, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty)
                {
                    super.updateItem(item, empty);
                    if (item == null || empty)
                    {
                        setText(null);
                    }
                    else
                    {
                        setText(item.toString());
                        setAlignment(Pos.CENTER);
                        setStyle("-fx-padding: 0;"); // set padding to zero to
                                                     // ensure the text is centered
                    }
                }
            };
        });

        TableColumn<WalletTransaction, String> categoryColumn =
            new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(param.getValue().GetCategory().GetName()));

        TableColumn<WalletTransaction, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetType().name()));

        TableColumn<WalletTransaction, String> statusColumn =
            new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetStatus().name()));

        TableColumn<WalletTransaction, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
            param
            -> new SimpleStringProperty(
                param.getValue().GetDate().format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<WalletTransaction, String> amountColumn =
            new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
            param
            -> new SimpleObjectProperty<>(
                UIUtils.FormatCurrency(param.getValue().GetAmount())));

        TableColumn<WalletTransaction, String> descriptionColumn =
            new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetDescription()));

        TableColumn<WalletTransaction, String> walletNameColumn =
            new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
            param -> new SimpleStringProperty(param.getValue().GetWallet().GetName()));

        transactionsTableView.getColumns().add(idColumn);
        transactionsTableView.getColumns().add(descriptionColumn);
        transactionsTableView.getColumns().add(amountColumn);
        transactionsTableView.getColumns().add(walletNameColumn);
        transactionsTableView.getColumns().add(dateColumn);
        transactionsTableView.getColumns().add(typeColumn);
        transactionsTableView.getColumns().add(categoryColumn);
        transactionsTableView.getColumns().add(statusColumn);
    }
}
