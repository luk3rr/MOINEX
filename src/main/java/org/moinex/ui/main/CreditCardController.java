/*
 * Filename: CreditCardController.java
 * Created on: October 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardDebt;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.service.CategoryService;
import org.moinex.service.CreditCardService;
import org.moinex.ui.common.CreditCardPaneController;
import org.moinex.ui.dialog.creditcard.AddCreditCardController;
import org.moinex.ui.dialog.creditcard.AddCreditCardDebtController;
import org.moinex.ui.dialog.creditcard.ArchivedCreditCardsController;
import org.moinex.ui.dialog.creditcard.EditCreditCardDebtController;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Credit Card screen
 */
@Controller
@NoArgsConstructor
public class CreditCardController {
    @FXML private VBox totalDebtsInfoVBox;

    @FXML private ComboBox<Year> totalDebtsYearFilterComboBox;

    @FXML private ComboBox<YearMonth> debtsListMonthFilterComboBox;

    @FXML private TableView<CreditCardPayment> debtsTableView;

    @FXML private TextField debtSearchField;

    @FXML private AnchorPane crcPane1;

    @FXML private AnchorPane debtsFlowPane;

    @FXML private JFXButton crcNextButton;

    @FXML private JFXButton crcPrevButton;

    @FXML private Label invoiceMonth;

    private ConfigurableApplicationContext springContext;

    private CreditCardService creditCardService;

    private CategoryService categoryService;

    private List<CreditCard> creditCards;

    private Integer crcPaneCurrentPage = 0;

    private static final Logger logger = LoggerFactory.getLogger(CreditCardController.class);

    /**
     * Constructor
     * @param creditCardService CreditCardService
     * @param categoryService CategoryService
     */
    @Autowired
    public CreditCardController(
            CreditCardService creditCardService,
            CategoryService categoryService,
            ConfigurableApplicationContext springContext) {
        this.creditCardService = creditCardService;
        this.categoryService = categoryService;
        this.springContext = springContext;
    }

    @FXML
    private void initialize() {
        loadCreditCardsFromDatabase();

        populateDebtsListMonthFilterComboBox();
        populateYearFilterComboBox();
        configureTableView();
        configureListeners();

        // Select the default values
        LocalDateTime now = LocalDateTime.now();

        totalDebtsYearFilterComboBox.setValue(Year.from(now));

        YearMonth currentYearMonth = YearMonth.of(now.getYear(), now.getMonthValue());

        // Select the default values
        debtsListMonthFilterComboBox.setValue(currentYearMonth);

        debtsListMonthFilterComboBox.setOnAction(event -> updateDebtsTableView());

        updateTotalDebtsInfo();
        updateDisplayCards();
        updateMoneyFlow();
        updateDebtsTableView();

        setButtonsActions();
    }

    @FXML
    private void handleAddDebt() {
        WindowUtils.openModalWindow(
                Constants.ADD_CREDIT_CARD_DEBT_FXML,
                "Add Credit Card Debt",
                springContext,
                (AddCreditCardDebtController controller) -> {},
                List.of(this::updateDisplay));
    }

    @FXML
    private void handleAddCreditCard() {
        WindowUtils.openModalWindow(
                Constants.ADD_CREDIT_CARD_FXML,
                "Add Credit Card",
                springContext,
                (AddCreditCardController controller) -> {},
                List.of(this::updateDisplayCards));
    }

    @FXML
    private void handleEditDebt() {
        CreditCardPayment selectedPayment = debtsTableView.getSelectionModel().getSelectedItem();

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                    "No payment selected", "Please select a payment to edit.");

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_CREDIT_CARD_DEBT_FXML,
                "Edit Credit Card Debt",
                springContext,
                (EditCreditCardDebtController controller) ->
                        controller.setCreditCardDebt(selectedPayment.getCreditCardDebt()),
                List.of(this::updateDisplay));
    }

    @FXML
    private void handleDeleteDebt() {
        CreditCardPayment selectedPayment = debtsTableView.getSelectionModel().getSelectedItem();

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                    "No payment selected",
                    "Please select a payment to delete the associated debt.");

            return;
        }

        CreditCardDebt debt = selectedPayment.getCreditCardDebt();

        List<CreditCardPayment> payments = creditCardService.getPaymentsByDebtId(debt.getId());

        // Get the amount paid for the debt
        BigDecimal refundAmount =
                payments.stream()
                        .filter(p -> p.getWallet() != null)
                        .map(CreditCardPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long installmentsPaid = payments.stream().filter(p -> p.getWallet() != null).count();

        // Create a message to show the user
        StringBuilder message = new StringBuilder();
        message.append("Description: ")
                .append(debt.getDescription())
                .append("\n")
                .append("Amount: ")
                .append(UIUtils.formatCurrency(debt.getAmount()))
                .append("\n")
                .append("Register date: ")
                .append(debt.getDate().format(Constants.DATE_FORMATTER_NO_TIME))
                .append("\n")
                .append("Installments: ")
                .append(debt.getInstallments())
                .append("\n")
                .append("Installments paid: ")
                .append(installmentsPaid)
                .append("\n")
                .append("Category: ")
                .append(debt.getCategory().getName())
                .append("\n")
                .append("Credit card: ")
                .append(debt.getCreditCard().getName())
                .append("\n");

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            message.append("Refund amount: ")
                    .append(UIUtils.formatCurrency(refundAmount))
                    .append("\n");
        } else {
            message.append("No refund amount");
        }

        // Confirm deletion
        if (WindowUtils.showConfirmationDialog(
                "Are you sure you want to delete the debt?", message.toString())) {
            creditCardService.deleteDebt(debt.getId());
            updateDisplay();
        }
    }

    @FXML
    private void handleViewArchivedCreditCards() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_CREDIT_CARDS_FXML,
                "Archived Credit Cards",
                springContext,
                (ArchivedCreditCardsController controller) -> {},
                List.of(this::updateDisplay));
    }

    @FXML
    private void handleTablePrevMonth() {
        YearMonth nowMonth = debtsListMonthFilterComboBox.getValue().minusMonths(1);

        // Set the previous month as current month
        debtsListMonthFilterComboBox.setValue(nowMonth);

        updateDebtsTableView();
    }

    @FXML
    private void handleTableNextMonth() {
        YearMonth newMonth = debtsListMonthFilterComboBox.getValue().plusMonths(1);

        // Set next month as current month
        debtsListMonthFilterComboBox.setValue(newMonth);

        updateDebtsTableView();
    }

    /**
     * Update the display
     * @param yearMonth YearMonth - The year and month to be displayed in credit card
     * resume and table view
     * @note: This method can be called by other controllers to update the screen when
     * there is a change
     */
    public void updateDisplay(YearMonth yearMonth) {
        loadCreditCardsFromDatabase();

        updateDebtsTableView();
        updateTotalDebtsInfo();
        updateMoneyFlow();
        updateDisplayCards(yearMonth);
    }

    /**
     * Update the display
     * @note: This method can be called by other controllers to update the screen when
     * there is a change
     */
    public void updateDisplay() {
        loadCreditCardsFromDatabase();

        updateDebtsTableView();
        updateTotalDebtsInfo();
        updateMoneyFlow();
        updateDisplayCards();
    }

    /**
     * Load credit cards from the database
     */
    private void loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByTransactionCountDesc();
    }

    /**
     * Update the debt table view
     */
    private void updateDebtsTableView() {
        YearMonth selectedMonth = debtsListMonthFilterComboBox.getValue();

        // Get the search text
        String similarTextOrId = debtSearchField.getText().toLowerCase();

        // Clear the transaction list view
        debtsTableView.getItems().clear();

        // Fetch all transactions within the selected range and filter by transaction
        // type.
        // If the transaction type is null, all transactions are fetched
        if (similarTextOrId.isEmpty()) {
            creditCardService
                    .getCreditCardPayments(selectedMonth.getMonthValue(), selectedMonth.getYear())
                    .forEach(debtsTableView.getItems()::add);
        } else {
            creditCardService
                    .getCreditCardPayments(selectedMonth.getMonthValue(), selectedMonth.getYear())
                    .stream()
                    .filter(
                            p -> {
                                String description =
                                        p.getCreditCardDebt().getDescription().toLowerCase();
                                String id = String.valueOf(p.getCreditCardDebt().getId());
                                String category =
                                        p.getCreditCardDebt().getCategory().getName().toLowerCase();
                                String cardName =
                                        p.getCreditCardDebt()
                                                .getCreditCard()
                                                .getName()
                                                .toLowerCase();
                                String value = p.getAmount().toString();

                                return description.contains(similarTextOrId)
                                        || id.contains(similarTextOrId)
                                        || category.contains(similarTextOrId)
                                        || cardName.contains(similarTextOrId)
                                        || value.contains(similarTextOrId);
                            })
                    .forEach(debtsTableView.getItems()::add);
        }

        debtsTableView.refresh();
    }

    /**
     * Update the display of the total debts information
     */
    private void updateTotalDebtsInfo() {
        // Get the selected year from the year filter combo box
        Year selectedYear = totalDebtsYearFilterComboBox.getValue();

        BigDecimal totalDebts = creditCardService.getTotalDebtAmount(selectedYear.getValue());

        BigDecimal totalPendingPayments = creditCardService.getTotalPendingPayments();

        Label totalTotalDebtsLabel = new Label(UIUtils.formatCurrency(totalDebts));

        Label totalPendingPaymentsLabel =
                new Label("Pending payments: " + UIUtils.formatCurrency(totalPendingPayments));

        totalTotalDebtsLabel.getStyleClass().add(Constants.TOTAL_BALANCE_VALUE_LABEL_STYLE);

        totalPendingPaymentsLabel.getStyleClass().add(Constants.TOTAL_BALANCE_FORESEEN_LABEL_STYLE);

        totalDebtsInfoVBox.getChildren().clear();
        totalDebtsInfoVBox.getChildren().add(totalTotalDebtsLabel);
        totalDebtsInfoVBox.getChildren().add(totalPendingPaymentsLabel);
    }

    /**
     * Update the display of the credit cards
     */
    private void updateDisplayCards() {
        updateDisplayCards(YearMonth.now());
    }

    /**
     * Update the display of the credit cards
     */
    private void updateDisplayCards(YearMonth defaultMonth) {
        crcPane1.getChildren().clear();

        if (!creditCards.isEmpty()) {
            CreditCard crc = creditCards.get(crcPaneCurrentPage);

            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(Constants.CRC_PANE_FXML));
                loader.setControllerFactory(springContext::getBean);
                Parent newContent = loader.load();

                // Add style class to the wallet pane
                newContent
                        .getStylesheets()
                        .add(
                                Objects.requireNonNull(
                                                getClass()
                                                        .getResource(Constants.COMMON_STYLE_SHEET))
                                        .toExternalForm());

                CreditCardPaneController crcPaneController = loader.getController();

                crcPaneController.updateCreditCardPane(crc, defaultMonth);

                AnchorPane.setTopAnchor(newContent, 0.0);
                AnchorPane.setBottomAnchor(newContent, 0.0);
                AnchorPane.setLeftAnchor(newContent, 0.0);
                AnchorPane.setRightAnchor(newContent, 0.0);

                crcPane1.getChildren().add(newContent);
            } catch (IOException e) {
                logger.error("Error while loading credit card pane");
            }
        }

        crcPrevButton.setDisable(crcPaneCurrentPage == 0);
        crcNextButton.setDisable(
                crcPaneCurrentPage == creditCards.size() - 1 || creditCards.isEmpty());
    }

    /**
     * Update money flow chart
     */
    private void updateMoneyFlow() {
        CategoryAxis categoryAxis = new CategoryAxis();
        NumberAxis numberAxis = new NumberAxis();
        StackedBarChart<String, Number> debtsFlowStackedBarChart =
                new StackedBarChart<>(categoryAxis, numberAxis);

        debtsFlowStackedBarChart.setVerticalGridLinesVisible(false);
        debtsFlowPane.getChildren().clear();
        debtsFlowPane.getChildren().add(debtsFlowStackedBarChart);

        AnchorPane.setTopAnchor(debtsFlowStackedBarChart, 0.0);
        AnchorPane.setBottomAnchor(debtsFlowStackedBarChart, 0.0);
        AnchorPane.setLeftAnchor(debtsFlowStackedBarChart, 0.0);
        AnchorPane.setRightAnchor(debtsFlowStackedBarChart, 0.0);

        debtsFlowStackedBarChart.getData().clear();

        LocalDateTime currentDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        List<Category> categories = categoryService.getCategories();
        Map<YearMonth, Map<Category, Double>> monthlyTotals = new LinkedHashMap<>();

        // Loop through the months
        int halfMonths = Constants.CRC_XYBAR_CHART_MAX_MONTHS / 2;

        // Positive to negative to keep the order of the months
        for (int i = halfMonths; i >= -halfMonths; i--) {
            // Get the date for the current month
            LocalDateTime date = currentDate.minusMonths(i);

            YearMonth yearMonth = YearMonth.of(date.getYear(), date.getMonthValue());

            // Get confirmed transactions for the month
            List<CreditCardPayment> payments =
                    creditCardService.getCreditCardPayments(date.getMonthValue(), date.getYear());

            // Calculate total for each category
            for (Category category : categories) {
                BigDecimal total =
                        payments.stream()
                                .filter(
                                        t ->
                                                t.getCreditCardDebt()
                                                        .getCategory()
                                                        .getId()
                                                        .equals(category.getId()))
                                .map(CreditCardPayment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                monthlyTotals.putIfAbsent(yearMonth, new LinkedHashMap<>());
                monthlyTotals.get(yearMonth).put(category, total.doubleValue());
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

                series.getData().add(new XYChart.Data<>(yearMonth.format(formatter), total));
            }

            // Only add a series to the chart if it has data greater than zero
            if (series.getData().stream().anyMatch(data -> (Double) data.getYValue() > 0)) {
                debtsFlowStackedBarChart.getData().add(series);
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

        // Set the maximum total as the upper bound of the y-axis
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

        for (XYChart.Series<String, Number> series : debtsFlowStackedBarChart.getData()) {
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
     * Populate the debt list month filter combo box
     */
    private void populateDebtsListMonthFilterComboBox() {
        debtsListMonthFilterComboBox.getItems().clear();

        // Get the oldest and newest debt date
        LocalDateTime oldestDebtDate = creditCardService.getEarliestPaymentDate();

        LocalDateTime newestDebtDate = creditCardService.getLatestPaymentDate();

        // Generate a list of YearMonth objects from the oldest transaction date to the
        // newest transaction date
        YearMonth startYearMonth = YearMonth.from(oldestDebtDate);
        YearMonth endYearMonth = YearMonth.from(newestDebtDate);

        // Generate the list of years between the oldest and the current date
        List<YearMonth> yearMonths = new ArrayList<>();

        while (endYearMonth.isAfter(startYearMonth) || endYearMonth.equals(startYearMonth)) {
            yearMonths.add(endYearMonth);
            endYearMonth = endYearMonth.minusMonths(1);
        }

        ObservableList<YearMonth> yearMonthList = FXCollections.observableArrayList(yearMonths);

        debtsListMonthFilterComboBox.setItems(yearMonthList);

        // Custom string converter to format the YearMonth as "MMM/yy"
        debtsListMonthFilterComboBox.setConverter(
                new StringConverter<>() {
                    private final DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("MMM/yy");

                    @Override
                    public String toString(YearMonth yearMonth) {
                        return yearMonth != null ? yearMonth.format(formatter) : "";
                    }

                    @Override
                    public YearMonth fromString(String string) {
                        return YearMonth.parse(string, formatter);
                    }
                });
    }

    /**
     * Populate the year filter combo box
     */
    private void populateYearFilterComboBox() {
        LocalDateTime oldestDebtDate = creditCardService.getEarliestPaymentDate();

        LocalDate youngest = LocalDate.now().plusYears(Constants.YEAR_RESUME_FUTURE_YEARS);

        // Generate a list of Year objects from the oldest transaction date to the
        // current date
        Year startYear = Year.from(oldestDebtDate);
        Year currentYear = Year.from(youngest);

        // Generate the list of years between the oldest and the current date
        List<Year> years = new ArrayList<>();
        while (!startYear.isAfter(currentYear)) {
            years.add(currentYear);
            currentYear = currentYear.minusYears(1);
        }

        ObservableList<Year> yearList = FXCollections.observableArrayList(years);

        totalDebtsYearFilterComboBox.setItems(yearList);

        // Custom string converter to format the Year as "Year"
        totalDebtsYearFilterComboBox.setConverter(
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
     * Set the actions for the buttons
     */
    private void setButtonsActions() {
        crcPrevButton.setOnAction(
                event -> {
                    if (crcPaneCurrentPage > 0) {
                        crcPaneCurrentPage--;
                        updateDisplayCards();
                    }
                });

        crcNextButton.setOnAction(
                event -> {
                    if (crcPaneCurrentPage < creditCards.size() - 1) {
                        crcPaneCurrentPage++;
                        updateDisplayCards();
                    }
                });
    }

    /**
     * Configure the listeners
     */
    private void configureListeners() {
        totalDebtsYearFilterComboBox
                .valueProperty()
                .addListener((observable, oldValue, newYear) -> updateTotalDebtsInfo());

        debtsListMonthFilterComboBox
                .valueProperty()
                .addListener(
                        (observable, oldValue, newMonth) -> {
                            final DateTimeFormatter formatter =
                                    DateTimeFormatter.ofPattern("MMM/yy");

                            if (newMonth != null) {
                                invoiceMonth.setText(newMonth.format(formatter));
                            }

                            updateDebtsTableView();
                        });

        debtSearchField
                .textProperty()
                .addListener((observable, oldValue, newValue) -> updateDebtsTableView());
    }

    /**
     * Configure the table view columns
     */
    private void configureTableView() {
        TableColumn<CreditCardPayment, Long> idColumn = getCreditCardPaymentLongTableColumn();

        TableColumn<CreditCardPayment, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getCreditCardDebt().getDescription()));

        TableColumn<CreditCardPayment, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<CreditCardPayment, String> installmentColumn =
                getCreditCardPaymentStringTableColumn();

        TableColumn<CreditCardPayment, String> crcColumn = new TableColumn<>("Credit Card");
        crcColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getCreditCardDebt().getCreditCard().getName()));

        TableColumn<CreditCardPayment, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getCreditCardDebt().getCategory().getName()));

        TableColumn<CreditCardPayment, String> dateColumn = new TableColumn<>("Invoice date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME)));

        TableColumn<CreditCardPayment, String> statusColumn = new TableColumn<>("Status");
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWallet() == null ? "Pending" : "Paid"));

        debtsTableView.getColumns().add(idColumn);
        debtsTableView.getColumns().add(descriptionColumn);
        debtsTableView.getColumns().add(amountColumn);
        debtsTableView.getColumns().add(installmentColumn);
        debtsTableView.getColumns().add(crcColumn);
        debtsTableView.getColumns().add(categoryColumn);
        debtsTableView.getColumns().add(dateColumn);
        debtsTableView.getColumns().add(statusColumn);
    }

    private static TableColumn<CreditCardPayment, String> getCreditCardPaymentStringTableColumn() {
        TableColumn<CreditCardPayment, String> installmentColumn = new TableColumn<>("Installment");
        installmentColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                param.getValue().getInstallment().toString()
                                        + "/"
                                        + param.getValue().getCreditCardDebt().getInstallments()));

        // Align the installment column to the center
        installmentColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                } else {
                                    setText(item);
                                    setAlignment(Pos.CENTER);
                                    setStyle("-fx-padding: 0;"); // set padding to zero to
                                    // ensure the text is centered
                                }
                            }
                        });
        return installmentColumn;
    }

    private static TableColumn<CreditCardPayment, Long> getCreditCardPaymentLongTableColumn() {
        TableColumn<CreditCardPayment, Long> idColumn = new TableColumn<>("Debt ID");
        idColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getCreditCardDebt().getId()));

        // Align the ID column to the center
        idColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(Long item, boolean empty) {
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
