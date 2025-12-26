/*
 * Filename: TransactionController.java
 * Created on: October 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
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
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.CategoryService;
import org.moinex.service.CreditCardService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.common.ResumePaneController;
import org.moinex.ui.dialog.ManageCategoryController;
import org.moinex.ui.dialog.wallettransaction.AddExpenseController;
import org.moinex.ui.dialog.wallettransaction.AddIncomeController;
import org.moinex.ui.dialog.wallettransaction.EditTransactionController;
import org.moinex.ui.dialog.wallettransaction.RecurringTransactionController;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TransactionStatus;
import org.moinex.util.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller class for the transaction view
 */
@Controller
@NoArgsConstructor
public class TransactionController {
    @FXML private AnchorPane monthYearResumeView;

    @FXML private AnchorPane yearResumeView;

    @FXML private ComboBox<Year> monthYearResumeYearComboBox;

    @FXML private ComboBox<Month> monthYearResumeMonthComboBox;

    @FXML private ComboBox<Year> yearResumeComboBox;

    @FXML private ComboBox<TransactionType> moneyFlowComboBox;

    @FXML private ComboBox<TransactionType> transactionsTypeComboBox;

    @FXML private DatePicker transactionsEndDatePicker;

    @FXML private DatePicker transactionsStartDatePicker;

    @FXML private TableView<WalletTransaction> transactionsTableView;

    @FXML private AnchorPane moneyFlowView;

    @FXML private TextField transactionsSearchField;

    private ConfigurableApplicationContext springContext;

    private WalletTransactionService walletTransactionService;

    private CreditCardService creditCardService;

    private CategoryService categoryService;

    private static final Integer DAYS_BEFORE_OFFSET = 30;

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    /**
     * Constructor
     * @param walletTransactionService WalletTransactionService
     * @param creditCardService CreditCardService
     * @param categoryService CategoryService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public TransactionController(
            WalletTransactionService walletTransactionService,
            CreditCardService creditCardService,
            CategoryService categoryService,
            ConfigurableApplicationContext springContext) {
        this.walletTransactionService = walletTransactionService;
        this.creditCardService = creditCardService;
        this.categoryService = categoryService;
        this.springContext = springContext;
    }

    @FXML
    private void initialize() {
        configureTableView();

        populateMonthYearResumeComboBoxes();
        populateYearComboBox();
        populateTransactionTypeComboBox();

        // Format the date pickers
        UIUtils.setDatePickerFormat(transactionsStartDatePicker);
        UIUtils.setDatePickerFormat(transactionsEndDatePicker);

        LocalDateTime currentDate = LocalDateTime.now();

        // Select the default values
        monthYearResumeYearComboBox.setValue(Year.of(currentDate.getYear()));
        monthYearResumeMonthComboBox.setValue(currentDate.getMonth());

        yearResumeComboBox.setValue(Year.of(currentDate.getYear()));

        moneyFlowComboBox.setValue(TransactionType.EXPENSE);

        transactionsTypeComboBox.setValue(null); // All transactions

        LocalDateTime startDate = currentDate.minusDays(DAYS_BEFORE_OFFSET);
        LocalDateTime lastDayOfMonth =
                currentDate.withDayOfMonth(
                        currentDate.getMonth().length(currentDate.toLocalDate().isLeapYear()));

        transactionsStartDatePicker.setValue(startDate.toLocalDate());
        transactionsEndDatePicker.setValue(lastDayOfMonth.toLocalDate());

        // Update the resumes
        updateMonthYearResume();
        updateYearResume();
        updateMoneyFlow();
        updateTransactionTableView();

        // Add a listener to handle user selection
        monthYearResumeYearComboBox.setOnAction(event -> updateMonthYearResume());
        monthYearResumeMonthComboBox.setOnAction(event -> updateMonthYearResume());

        yearResumeComboBox.setOnAction(event -> updateYearResume());

        moneyFlowComboBox.setOnAction(event -> updateMoneyFlow());

        transactionsTypeComboBox.setOnAction(event -> updateTransactionTableView());

        transactionsStartDatePicker.setOnAction(event -> updateTransactionTableView());

        transactionsEndDatePicker.setOnAction(event -> updateTransactionTableView());

        // Add listener to the search field
        transactionsSearchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateTransactionTableView());
    }

    @FXML
    private void handleAddIncome() {
        WindowUtils.openModalWindow(
                Constants.ADD_INCOME_FXML,
                "Add new income",
                springContext,
                (AddIncomeController controller) -> {},
                List.of(
                        () -> {
                            updateMonthYearResume();
                            updateYearResume();
                            updateTransactionTableView();
                            updateMoneyFlow();
                        }));
    }

    @FXML
    private void handleAddExpense() {
        WindowUtils.openModalWindow(
                Constants.ADD_EXPENSE_FXML,
                "Add new expense",
                springContext,
                (AddExpenseController controller) -> {},
                List.of(
                        () -> {
                            updateMonthYearResume();
                            updateYearResume();
                            updateTransactionTableView();
                            updateMoneyFlow();
                        }));
    }

    @FXML
    private void handleEditTransaction() {
        WalletTransaction selectedTransaction =
                transactionsTableView.getSelectionModel().getSelectedItem();

        if (selectedTransaction == null) {
            WindowUtils.showInformationDialog(
                    "No transaction selected", "Please select a transaction to edit.");

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TRANSACTION_FXML,
                "Edit transaction",
                springContext,
                (EditTransactionController controller) ->
                        controller.setTransaction(selectedTransaction),
                List.of(
                        () -> {
                            updateMonthYearResume();
                            updateYearResume();
                            updateTransactionTableView();
                            updateMoneyFlow();
                        }));
    }

    @FXML
    private void handleDeleteTransaction() {
        WalletTransaction selectedTransaction =
                transactionsTableView.getSelectionModel().getSelectedItem();

        if (selectedTransaction == null) {
            WindowUtils.showInformationDialog(
                    "No transaction selected", "Please select a transaction to remove.");

            return;
        }

        // Create a message to show to the user
        StringBuilder message = new StringBuilder();
        message.append("Description: ")
                .append(selectedTransaction.getDescription())
                .append("\n")
                .append("Amount: ")
                .append(UIUtils.formatCurrency(selectedTransaction.getAmount()))
                .append("\n")
                .append("Date: ")
                .append(selectedTransaction.getDate().format(Constants.DATE_FORMATTER_WITH_TIME))
                .append("\n")
                .append("Status: ")
                .append(selectedTransaction.getStatus().toString())
                .append("\n")
                .append("Wallet: ")
                .append(selectedTransaction.getWallet().getName())
                .append("\n")
                .append("Wallet balance: ")
                .append(UIUtils.formatCurrency(selectedTransaction.getWallet().getBalance()))
                .append("\n")
                .append("Wallet balance after deletion: ");

        if (selectedTransaction.getStatus().equals(TransactionStatus.CONFIRMED)) {
            if (selectedTransaction.getType().equals(TransactionType.EXPENSE)) {
                message.append(
                                UIUtils.formatCurrency(
                                        selectedTransaction
                                                .getWallet()
                                                .getBalance()
                                                .add(selectedTransaction.getAmount())))
                        .append("\n");
            } else {
                message.append(
                                UIUtils.formatCurrency(
                                        selectedTransaction
                                                .getWallet()
                                                .getBalance()
                                                .subtract(selectedTransaction.getAmount())))
                        .append("\n");
            }
        } else {
            message.append(UIUtils.formatCurrency(selectedTransaction.getWallet().getBalance()))
                    .append("\n");
        }

        // Confirm deletion
        if (WindowUtils.showConfirmationDialog(
                "Are you sure you want to remove this "
                        + selectedTransaction.getType().toString().toLowerCase()
                        + "?",
                message.toString())) {
            walletTransactionService.deleteTransaction(selectedTransaction.getId());

            updateMonthYearResume();
            updateYearResume();
            updateTransactionTableView();
            updateMoneyFlow();
        }
    }

    @FXML
    private void handleRecurringTransactions() {
        WindowUtils.openModalWindow(
                Constants.RECURRING_TRANSACTIONS_FXML,
                "Recurring transactions",
                springContext,
                (RecurringTransactionController controller) -> {},
                List.of(
                        () -> {
                            updateMonthYearResume();
                            updateYearResume();
                            updateTransactionTableView();
                            updateMoneyFlow();
                        }));
    }

    @FXML
    private void handleManageCategories() {
        WindowUtils.openModalWindow(
                Constants.MANAGE_CATEGORY_FXML,
                "Manage categories",
                springContext,
                (ManageCategoryController controller) -> {},
                List.of(
                        () -> {
                            updateTransactionTableView();
                            updateMoneyFlow();
                        }));
    }

    /**
     * Update the transaction table view
     */
    private void updateTransactionTableView() {
        // Get the search text
        String similarTextOrId = transactionsSearchField.getText().toLowerCase();

        // Get selected values from the combo boxes
        TransactionType selectedTransactionType = transactionsTypeComboBox.getValue();

        LocalDateTime startDate = transactionsStartDatePicker.getValue().atStartOfDay();
        LocalDateTime endDate = transactionsEndDatePicker.getValue().atTime(23, 59, 59);

        // Clear the transaction list view
        transactionsTableView.getItems().clear();

        // Fetch all transactions within the selected range and filter by transaction
        // type.
        // If the transaction type is null, all transactions are fetched
        if (similarTextOrId.isEmpty()) {
            walletTransactionService
                    .getNonArchivedTransactionsBetweenDates(startDate, endDate)
                    .stream()
                    .filter(
                            t ->
                                    selectedTransactionType == null
                                            || t.getType().equals(selectedTransactionType))
                    .forEach(transactionsTableView.getItems()::add);
        } else {
            walletTransactionService
                    .getNonArchivedTransactionsBetweenDates(startDate, endDate)
                    .stream()
                    .filter(
                            t ->
                                    selectedTransactionType == null
                                            || t.getType().equals(selectedTransactionType))
                    .filter(
                            t -> {
                                String description = t.getDescription().toLowerCase();
                                String id = t.getId().toString();
                                String category = t.getCategory().getName().toLowerCase();
                                String wallet = t.getWallet().getName().toLowerCase();
                                String amount = t.getAmount().toString();
                                String type = t.getType().toString().toLowerCase();
                                String status = t.getStatus().toString().toLowerCase();

                                return description.contains(similarTextOrId)
                                        || id.contains(similarTextOrId)
                                        || category.contains(similarTextOrId)
                                        || wallet.contains(similarTextOrId)
                                        || amount.contains(similarTextOrId)
                                        || type.contains(similarTextOrId)
                                        || status.contains(similarTextOrId);
                            })
                    .forEach(transactionsTableView.getItems()::add);
        }

        transactionsTableView.refresh();
    }

    /**
     * Update the money flow bar chart
     */
    private void updateMoneyFlow() {
        // Get the selected transaction type
        TransactionType selectedTransactionType = moneyFlowComboBox.getValue();

        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis numberAxis = new NumberAxis();
        StackedBarChart<String, Number> moneyFlowStackedBarChart =
                new StackedBarChart<>(categoryAxis, numberAxis);

        moneyFlowStackedBarChart.setVerticalGridLinesVisible(false);
        moneyFlowView.getChildren().clear();
        moneyFlowView.getChildren().add(moneyFlowStackedBarChart);

        AnchorPane.setTopAnchor(moneyFlowStackedBarChart, 0.0);
        AnchorPane.setBottomAnchor(moneyFlowStackedBarChart, 0.0);
        AnchorPane.setLeftAnchor(moneyFlowStackedBarChart, 0.0);
        AnchorPane.setRightAnchor(moneyFlowStackedBarChart, 0.0);

        moneyFlowStackedBarChart.getData().clear();

        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        List<Category> categories = categoryService.getNonArchivedCategoriesOrderedByName();
        Map<YearMonth, Map<Category, Double>> monthlyTotals = new LinkedHashMap<>();

        // Loop through the last few months
        for (int i = 0; i < Constants.XYBAR_CHART_MONTHS; i++) {
            // Get the date for the current month
            LocalDateTime date = currentDate.minusMonths(Constants.XYBAR_CHART_MONTHS - i - 1L);
            YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonthValue());

            // Get confirmed transactions for the month
            List<WalletTransaction> transactions =
                    walletTransactionService.getNonArchivedConfirmedTransactionsByMonth(
                            date.getMonthValue(), date.getYear());

            // Get paid credit card payments for the month
            // Only get paid payments if the selected transaction type is expense
            // Otherwise, create an empty list
            List<CreditCardPayment> creditCardPayments =
                    selectedTransactionType.equals(TransactionType.EXPENSE)
                            ? creditCardService.getAllPaidPaymentsByMonth(
                                    date.getMonthValue(), date.getYear())
                            : new ArrayList<>();

            // Calculate total for each category
            for (Category category : categories) {
                BigDecimal totalWalletTransaction =
                        transactions.stream()
                                .filter(t -> t.getType().equals(selectedTransactionType))
                                .filter(t -> t.getCategory().getId().equals(category.getId()))
                                .map(WalletTransaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalCreditCardPayment =
                        creditCardPayments.stream()
                                .filter(
                                        p ->
                                                p.getCreditCardDebt()
                                                        .getCategory()
                                                        .getId()
                                                        .equals(category.getId()))
                                .map(CreditCardPayment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal total = totalWalletTransaction.add(totalCreditCardPayment);

                // Store total if it's greater than zero
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    monthlyTotals.putIfAbsent(yearMonth, new LinkedHashMap<>());
                    monthlyTotals.get(yearMonth).put(category, total.doubleValue());
                }
            }
        }

        // Add series to the chart
        for (Category category : categories) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(category.getName());

            // Loop through the months in the order they were added
            for (YearMonth yearMonth : monthlyTotals.keySet()) {
                Double total =
                        monthlyTotals
                                .getOrDefault(yearMonth, new LinkedHashMap<>())
                                .getOrDefault(category, 0.0);

                // Add total to the series if it's greater than zero
                if (total > 0) {
                    series.getData().add(new XYChart.Data<>(yearMonth.format(formatter), total));
                } else {
                    // Add zero value to keep the structure and order of the series
                    series.getData().add(new XYChart.Data<>(yearMonth.format(formatter), 0.0));
                }
            }

            // Only add a series to the chart if it has data greater than zero
            if (series.getData().stream().anyMatch(data -> (Double) data.getYValue() > 0)) {
                moneyFlowStackedBarChart.getData().add(series);
            }
        }

        // Calculate the maximum total for each month
        Double maxTotal =
                monthlyTotals.values().stream()
                        .map(
                                monthData ->
                                        monthData.values().stream()
                                                .mapToDouble(Double::doubleValue)
                                                .sum())
                        .max(Double::compare)
                        .orElse(0.0);

        // Set the Y-axis properties only if maxTotal is greater than 0
        Animation.setDynamicYAxisBounds(numberAxis, maxTotal);

        numberAxis.setTickLabelFormatter(
                new StringConverter<>() {
                    @Override
                    public String toString(Number value) {
                        return UIUtils.formatCurrency(value);
                    }

                    @Override
                    public Number fromString(String string) {
                        return 0;
                    }
                });

        UIUtils.applyDefaultChartStyle(moneyFlowStackedBarChart);

        for (XYChart.Series<String, Number> series : moneyFlowStackedBarChart.getData()) {
            for (XYChart.Data<String, Number> data : series.getData()) {
                // Calculate the total for the month to find the percentage
                YearMonth yearMonth = YearMonth.parse(data.getXValue(), formatter);
                Double monthTotal =
                        monthlyTotals
                                .getOrDefault(yearMonth, new LinkedHashMap<>())
                                .values()
                                .stream()
                                .mapToDouble(Double::doubleValue)
                                .sum();

                // Calculate the percentage
                Double value = (Double) data.getYValue();
                Double percentage = (monthTotal > 0) ? (value / monthTotal) * 100 : 0;

                // Add tooltip with value and percentage
                UIUtils.addTooltipToXYChartNode(
                        data.getNode(),
                        series.getName()
                                + ": "
                                + UIUtils.formatCurrency(value)
                                + " ("
                                + UIUtils.formatPercentage(percentage)
                                + ")\nTotal: "
                                + UIUtils.formatCurrency(monthTotal));

                // Animate the data after setting up the tooltip
                Animation.stackedXYChartAnimation(
                        Collections.singletonList(data), Collections.singletonList(value));
            }
        }
    }

    /**
     * Update the year resume view
     */
    private void updateYearResume() {
        Year selectedYear = yearResumeComboBox.getValue();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.RESUME_PANE_FXML));
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            newContent
                    .getStylesheets()
                    .add(
                            Objects.requireNonNull(
                                            getClass().getResource(Constants.COMMON_STYLE_SHEET))
                                    .toExternalForm());

            ResumePaneController resumePaneController = loader.getController();
            resumePaneController.updateResumePane(selectedYear.getValue());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 10.0);
            AnchorPane.setRightAnchor(newContent, 10.0);

            yearResumeView.getChildren().clear();
            yearResumeView.getChildren().add(newContent);
        } catch (Exception e) {
            logger.error("Error updating year resume: {}", e.getMessage());
        }
    }

    /**
     * Update the month resume view
     */
    private void updateMonthYearResume() {
        YearMonth selectedYearMonth =
                YearMonth.of(
                        monthYearResumeYearComboBox.getValue().getValue(),
                        monthYearResumeMonthComboBox.getValue().getValue());

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.RESUME_PANE_FXML));
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            newContent
                    .getStylesheets()
                    .add(
                            Objects.requireNonNull(
                                            getClass().getResource(Constants.COMMON_STYLE_SHEET))
                                    .toExternalForm());

            ResumePaneController resumePaneController = loader.getController();
            resumePaneController.updateResumePane(
                    selectedYearMonth.getMonthValue(), selectedYearMonth.getYear());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 10.0);
            AnchorPane.setRightAnchor(newContent, 10.0);

            monthYearResumeView.getChildren().clear();
            monthYearResumeView.getChildren().add(newContent);
        } catch (Exception e) {
            logger.error("Error updating month resume: {}", e.getMessage());
        }
    }

    /**
     * Populate the year combo box with the years between the oldest transaction
     * date and the current date
     */
    private void populateYearComboBox() {
        LocalDateTime oldestWalletTransaction = walletTransactionService.getOldestTransactionDate();
        LocalDateTime oldestCreditCard = creditCardService.getEarliestPaymentDate();

        LocalDateTime oldest =
                oldestCreditCard.isBefore(oldestWalletTransaction)
                        ? oldestCreditCard
                        : oldestWalletTransaction;

        LocalDate youngest = LocalDate.now().plusYears(Constants.YEAR_RESUME_FUTURE_YEARS);

        // Generate a list of Year objects from the oldest transaction date to the
        // current date
        Year startYear = Year.from(oldest);
        Year currentYear = Year.from(youngest);

        // Generate the list of years between the oldest and the current date
        List<Year> years = new ArrayList<>();
        while (!startYear.isAfter(currentYear)) {
            years.add(currentYear);
            currentYear = currentYear.minusYears(1);
        }

        ObservableList<Year> yearList = FXCollections.observableArrayList(years);

        yearResumeComboBox.setItems(yearList);

        // Custom string converter to format the Year as "Year"
        yearResumeComboBox.setConverter(
                new StringConverter<>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");

                    @Override
                    public String toString(Year year) {
                        return year != null ? year.format(formatter) : "";
                    }

                    @Override
                    public Year fromString(String string) {
                        return Year.parse(string, formatter);
                    }
                });
    }

    /**
     * Populate the transaction type combo box with the available transaction types
     */
    private void populateTransactionTypeComboBox() {
        ObservableList<TransactionType> transactionTypes =
                FXCollections.observableArrayList(TransactionType.values());

        moneyFlowComboBox.setItems(transactionTypes);

        // Make a copy of the list to add the 'All' option
        // Add 'All' option to the transaction type combo box
        // All is the first element in the list and is represented by a null value
        ObservableList<TransactionType> transactionTypesWithNull =
                FXCollections.observableArrayList(TransactionType.values());
        transactionTypesWithNull.addFirst(null);

        transactionsTypeComboBox.setItems(transactionTypesWithNull);

        // Custom string converter to format the TransactionType as
        // "TransactionType"
        moneyFlowComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(TransactionType transactionType) {
                        return transactionType != null ? transactionType.toString() : "";
                    }

                    @Override
                    public TransactionType fromString(String string) {
                        return TransactionType.valueOf(string);
                    }
                });

        transactionsTypeComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(TransactionType transactionType) {
                        return transactionType != null
                                ? transactionType.toString()
                                : "ALL"; // Show "All" instead of null
                    }

                    @Override
                    public TransactionType fromString(String string) {
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
    private void populateMonthYearResumeComboBoxes() {
        LocalDateTime oldestWalletTransaction = walletTransactionService.getOldestTransactionDate();

        LocalDateTime oldestCreditCard = creditCardService.getEarliestPaymentDate();

        LocalDateTime oldest =
                oldestCreditCard.isBefore(oldestWalletTransaction)
                        ? oldestCreditCard
                        : oldestWalletTransaction;

        LocalDate future = LocalDate.now().plusMonths(Constants.MONTH_RESUME_FUTURE_MONTHS);

        // Generate a list of YearMonth objects from the oldest transaction date to
        // the current date
        YearMonth startMonth = YearMonth.from(oldest);
        YearMonth currentMonth = YearMonth.from(future);

        // Generate the list of months between the oldest and the current date
        List<YearMonth> months = new ArrayList<>();
        while (!startMonth.isAfter(currentMonth)) {
            months.add(currentMonth);
            currentMonth = currentMonth.minusMonths(1);
        }

        ObservableList<YearMonth> monthYearList = FXCollections.observableArrayList(months);

        ObservableList<Year> years =
                FXCollections.observableArrayList(
                        monthYearList.stream()
                                .map(YearMonth::getYear)
                                .distinct()
                                .sorted(Comparator.reverseOrder())
                                .map(Year::of)
                                .toList());

        monthYearResumeYearComboBox.setItems(years);
        monthYearResumeYearComboBox.setValue(years.getFirst());

        ObservableList<Month> uniqueMonths =
                FXCollections.observableArrayList(
                        monthYearList.stream()
                                .map(YearMonth::getMonth)
                                .distinct()
                                .sorted(Comparator.comparingInt(Month::getValue))
                                .toList());

        monthYearResumeMonthComboBox.setItems(uniqueMonths);
        monthYearResumeMonthComboBox.setValue(uniqueMonths.getFirst());

        monthYearResumeMonthComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Month month) {
                        return month != null
                                ? month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                                : "";
                    }

                    @Override
                    public Month fromString(String string) {
                        return Month.valueOf(string.toUpperCase());
                    }
                });
    }

    /**
     * Configure the table view columns
     */
    private void configureTableView() {
        TableColumn<WalletTransaction, Integer> idColumn = getWalletTransactionLongTableColumn();

        TableColumn<WalletTransaction, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getCategory().getName()));

        TableColumn<WalletTransaction, String> typeColumn = new TableColumn<>("Type");
        typeColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getType().name()));

        TableColumn<WalletTransaction, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getStatus().name()));

        TableColumn<WalletTransaction, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_WITH_TIME)));

        TableColumn<WalletTransaction, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<WalletTransaction, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        TableColumn<WalletTransaction, String> walletNameColumn = new TableColumn<>("Wallet");
        walletNameColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getWallet().getName()));

        transactionsTableView.getColumns().add(idColumn);
        transactionsTableView.getColumns().add(descriptionColumn);
        transactionsTableView.getColumns().add(amountColumn);
        transactionsTableView.getColumns().add(walletNameColumn);
        transactionsTableView.getColumns().add(dateColumn);
        transactionsTableView.getColumns().add(typeColumn);
        transactionsTableView.getColumns().add(categoryColumn);
        transactionsTableView.getColumns().add(statusColumn);
    }

    private static TableColumn<WalletTransaction, Integer> getWalletTransactionLongTableColumn() {
        TableColumn<WalletTransaction, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Integer item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item.toString());
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;"); // set padding to zero to
                                    // ensure the text is centered
                                }
                            }
                        });
        return idColumn;
    }
}
