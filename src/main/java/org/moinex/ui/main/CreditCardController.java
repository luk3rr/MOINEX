/*
 * Filename: CreditCardController.java
 * Created on: October 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.*;
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
import org.moinex.service.I18nService;
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

    @FXML private ComboBox<Month> debtsListMonthFilterComboBox;

    @FXML private ComboBox<Year> debtsListYearFilterComboBox;

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

    private I18nService i18nService;

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
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.creditCardService = creditCardService;
        this.categoryService = categoryService;
        this.springContext = springContext;
        this.i18nService = i18nService;
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

        // Select the default values
        debtsListMonthFilterComboBox.setValue(now.getMonth());
        debtsListYearFilterComboBox.setValue(Year.of(now.getYear()));

        invoiceMonth.setText(UIUtils.formatShortMonthYear(getTableCurrentMonthYear(), i18nService));

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
                i18nService.tr(Constants.TranslationKeys.CREDIT_CARD_DIALOG_ADD_DEBT_TITLE),
                springContext,
                (AddCreditCardDebtController controller) -> {},
                List.of(this::updateDisplay),
                i18nService.getBundle());
    }

    @FXML
    private void handleAddCreditCard() {
        WindowUtils.openModalWindow(
                Constants.ADD_CREDIT_CARD_FXML,
                i18nService.tr(Constants.TranslationKeys.CREDIT_CARD_DIALOG_ADD_CREDIT_CARD_TITLE),
                springContext,
                (AddCreditCardController controller) -> {},
                List.of(this::updateDisplayCards),
                i18nService.getBundle());
    }

    @FXML
    private void handleEditDebt() {
        CreditCardPayment selectedPayment = debtsTableView.getSelectionModel().getSelectedItem();

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CREDIT_CARD_DIALOG_NO_SELECTION_EDIT_MESSAGE));

            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_CREDIT_CARD_DEBT_FXML,
                i18nService.tr(Constants.TranslationKeys.CREDIT_CARD_DIALOG_EDIT_DEBT_TITLE),
                springContext,
                (EditCreditCardDebtController controller) ->
                        controller.setCreditCardDebt(selectedPayment.getCreditCardDebt()),
                List.of(this::updateDisplay),
                i18nService.getBundle());
    }

    @FXML
    private void handleDeleteDebt() {
        CreditCardPayment selectedPayment = debtsTableView.getSelectionModel().getSelectedItem();

        if (selectedPayment == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.CREDIT_CARD_DIALOG_NO_SELECTION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CREDIT_CARD_DIALOG_NO_SELECTION_DELETE_MESSAGE));

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

        Integer installmentsPaid =
                Math.toIntExact(payments.stream().filter(p -> p.getWallet() != null).count());

        // Create a message to show the user
        StringBuilder message = new StringBuilder();

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_DESCRIPTION),
                                debt.getDescription()))
                .append("\n");

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_AMOUNT),
                                UIUtils.formatCurrency(debt.getAmount())))
                .append("\n");

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REGISTER_DATE),
                                UIUtils.formatDateForDisplay(debt.getDate(), i18nService)))
                .append("\n");

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS),
                                debt.getInstallments()))
                .append("\n");

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_INSTALLMENTS_PAID),
                                installmentsPaid))
                .append("\n");

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CATEGORY),
                                debt.getCategory().getName()))
                .append("\n");

        message.append(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_CREDIT_CARD),
                                debt.getCreditCard().getName()))
                .append("\n");

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            message.append(
                            MessageFormat.format(
                                    i18nService.tr(
                                            Constants.TranslationKeys
                                                    .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_REFUND_AMOUNT),
                                    UIUtils.formatCurrency(refundAmount)))
                    .append("\n");
        } else {
            message.append(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_NO_REFUND_AMOUNT));
        }

        // Confirm deletion
        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.CREDIT_CARD_DIALOG_CONFIRMATION_DELETE_TITLE),
                message.toString(),
                i18nService.getBundle())) {
            creditCardService.deleteDebt(debt.getId());
            updateDisplay();
        }
    }

    @FXML
    private void handleViewArchivedCreditCards() {
        WindowUtils.openModalWindow(
                Constants.ARCHIVED_CREDIT_CARDS_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.CREDIT_CARD_DIALOG_CREDIT_CARD_ARCHIVE_TITLE),
                springContext,
                (ArchivedCreditCardsController controller) -> {},
                List.of(this::updateDisplay),
                i18nService.getBundle());
    }

    @FXML
    private void handleTablePrevMonth() {
        updateTableCurrentMonthYear(-1);
    }

    @FXML
    private void handleTableNextMonth() {
        updateTableCurrentMonthYear(1);
    }

    private YearMonth getTableCurrentMonthYear() {
        return YearMonth.of(
                debtsListYearFilterComboBox.getValue().getValue(),
                debtsListMonthFilterComboBox.getValue().getValue());
    }

    private void updateTableCurrentMonthYear(Integer offset) {
        YearMonth nextYearMonth = getTableCurrentMonthYear().plusMonths(offset);
        debtsListMonthFilterComboBox.setValue(nextYearMonth.getMonth());
        debtsListYearFilterComboBox.setValue(Year.of(nextYearMonth.getYear()));
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
        YearMonth selectedMonth = getTableCurrentMonthYear();

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
                new Label(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .CREDIT_CARD_TOTAL_DEBTS_PENDING_PAYMENTS),
                                UIUtils.formatCurrency(totalPendingPayments)));

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
                FXMLLoader loader =
                        new FXMLLoader(
                                getClass().getResource(Constants.CRC_PANE_FXML),
                                i18nService.getBundle());
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
                logger.error(
                        "Error loading credit card pane FXML: '{}' for card ID: {}",
                        Constants.CRC_PANE_FXML,
                        crc.getId(),
                        e);
                logger.error(
                        "Credit card details - Name: '{}', Last 4 digits: '{}'",
                        crc.getName(),
                        crc.getLastFourDigits());
            } catch (Exception e) {
                logger.error(
                        "Unexpected error loading credit card pane for card ID: {}",
                        crc.getId(),
                        e);
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
        DateTimeFormatter formatter = UIUtils.getShortMonthYearFormatter(i18nService.getLocale());

        List<Category> categories = categoryService.getNonArchivedCategoriesOrderedByName();
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

        UIUtils.applyDefaultChartStyle(debtsFlowStackedBarChart);

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
                                + UIUtils.formatPercentage(percentage, i18nService)
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

        ObservableList<Year> years =
                FXCollections.observableArrayList(
                        yearMonthList.stream()
                                .map(YearMonth::getYear)
                                .distinct()
                                .sorted(Comparator.reverseOrder())
                                .map(Year::of)
                                .toList());

        debtsListYearFilterComboBox.setItems(years);
        debtsListYearFilterComboBox.setValue(years.getFirst());

        ObservableList<Month> uniqueMonths =
                FXCollections.observableArrayList(
                        yearMonthList.stream()
                                .map(YearMonth::getMonth)
                                .distinct()
                                .sorted(Comparator.comparingInt(Month::getValue))
                                .toList());

        debtsListMonthFilterComboBox.setItems(uniqueMonths);
        debtsListMonthFilterComboBox.setValue(uniqueMonths.getFirst());

        debtsListMonthFilterComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Month month) {
                        return month != null ? UIUtils.getMonthDisplayName(month, i18nService) : "";
                    }

                    @Override
                    public Month fromString(String string) {
                        return Month.valueOf(string.toUpperCase());
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
                    private final DateTimeFormatter formatter =
                            UIUtils.getYearFormatter(i18nService.getLocale());

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
                            if (newMonth != null
                                    && debtsListYearFilterComboBox.getValue() != null) {
                                invoiceMonth.setText(
                                        UIUtils.formatShortMonthYear(
                                                getTableCurrentMonthYear(), i18nService));
                            }

                            updateDebtsTableView();
                        });

        debtsListYearFilterComboBox
                .valueProperty()
                .addListener(
                        (observable, oldValue, newYear) -> {
                            if (newYear != null
                                    && debtsListMonthFilterComboBox.getValue() != null) {
                                invoiceMonth.setText(
                                        UIUtils.formatShortMonthYear(
                                                getTableCurrentMonthYear(), i18nService));
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
        TableColumn<CreditCardPayment, Integer> idColumn =
                getCreditCardPaymentLongTableColumn(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_DEBT_ID));

        TableColumn<CreditCardPayment, String> descriptionColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDIT_CARD_DEBTS_LIST_HEADER_DESCRIPTION));
        descriptionColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getCreditCardDebt().getDescription()));

        TableColumn<CreditCardPayment, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<CreditCardPayment, String> installmentColumn =
                getCreditCardPaymentStringTableColumn(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDIT_CARD_DEBTS_LIST_HEADER_INSTALLMENT));

        TableColumn<CreditCardPayment, String> crcColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDIT_CARD_DEBTS_LIST_HEADER_CREDIT_CARD));
        crcColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getCreditCardDebt().getCreditCard().getName()));

        TableColumn<CreditCardPayment, String> categoryColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_CATEGORY));
        categoryColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getCreditCardDebt().getCategory().getName()));

        TableColumn<CreditCardPayment, String> dateColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .CREDIT_CARD_DEBTS_LIST_HEADER_INVOICE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateForDisplay(
                                        param.getValue().getDate(), i18nService)));

        TableColumn<CreditCardPayment, String> statusColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.CREDIT_CARD_DEBTS_LIST_HEADER_STATUS));
        statusColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue().getWallet() == null
                                        ? i18nService.tr(
                                                Constants.TranslationKeys
                                                        .CREDIT_CARD_DEBTS_LIST_STATUS_PENDING)
                                        : i18nService.tr(
                                                Constants.TranslationKeys
                                                        .CREDIT_CARD_DEBTS_LIST_STATUS_PAID)));

        debtsTableView
                .getColumns()
                .addAll(
                        idColumn,
                        descriptionColumn,
                        amountColumn,
                        installmentColumn,
                        crcColumn,
                        categoryColumn,
                        dateColumn,
                        statusColumn);
    }

    private static TableColumn<CreditCardPayment, String> getCreditCardPaymentStringTableColumn(
            String header) {

        TableColumn<CreditCardPayment, String> installmentColumn = new TableColumn<>(header);

        installmentColumn.setCellValueFactory(
                param ->
                        new SimpleObjectProperty<>(
                                param.getValue().getInstallment()
                                        + "/"
                                        + param.getValue().getCreditCardDebt().getInstallments()));

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
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });

        return installmentColumn;
    }

    private static TableColumn<CreditCardPayment, Integer> getCreditCardPaymentLongTableColumn(
            String header) {

        TableColumn<CreditCardPayment, Integer> idColumn = new TableColumn<>(header);

        idColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue().getCreditCardDebt().getId()));

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
                                    setStyle("-fx-padding: 0;");
                                }
                            }
                        });

        return idColumn;
    }
}
