/*
 * Filename: WalletController.java
 * Created on: September 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.charts.DoughnutChart;
import org.moinex.entities.creditcard.CreditCardPayment;
import org.moinex.entities.wallettransaction.Wallet;
import org.moinex.entities.wallettransaction.WalletTransaction;
import org.moinex.entities.wallettransaction.WalletType;
import org.moinex.services.CreditCardService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.WalletFullPaneController;
import org.moinex.ui.dialog.wallettransaction.AddTransferController;
import org.moinex.ui.dialog.wallettransaction.AddWalletController;
import org.moinex.ui.dialog.wallettransaction.ArchivedWalletsController;
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
 * Controller for the wallet view
 */
@Controller
@NoArgsConstructor
public class WalletController
{
    @FXML
    private AnchorPane totalBalanceView;

    @FXML
    private AnchorPane walletPane1;

    @FXML
    private AnchorPane walletPane2;

    @FXML
    private AnchorPane walletPane3;

    @FXML
    private AnchorPane moneyFlowBarChartAnchorPane;

    @FXML
    private AnchorPane balanceByWalletTypePieChartAnchorPane;

    @FXML
    private VBox totalBalanceByWalletTypeVBox;

    @FXML
    private VBox totalBalancePaneInfoVBox;

    @FXML
    private JFXButton totalBalancePaneTransferButton;

    @FXML
    private JFXButton totalBalancePaneAddWalletButton;

    @FXML
    private JFXButton totalBalancePaneViewArchivedWalletsButton;

    @FXML
    private JFXButton walletPrevButton;

    @FXML
    private JFXButton walletNextButton;

    @FXML
    private ComboBox<String> totalBalancePaneWalletTypeComboBox;

    @FXML
    private ComboBox<String> moneyFlowPaneWalletTypeComboBox;

    private ConfigurableApplicationContext springContext;

    private BarChart<String, Number> moneyFlowBarChart;

    private WalletService walletService;

    private CreditCardService creditCardService;

    private WalletTransactionService walletTransactionService;

    private RecurringTransactionService recurringTransactionService;

    private List<CheckBox> doughnutChartCheckBoxes;

    private List<WalletTransaction> transactions;

    private List<WalletType> walletTypes;

    private List<Wallet> wallets;

    private Integer totalBalanceSelectedMonth;

    private Integer totalBalanceSelectedYear;

    private Integer walletPaneCurrentPage = 0;

    private final Integer itemsPerPage = 3;

    private static final Logger logger =
        LoggerFactory.getLogger(WalletController.class);

    /**
     * Constructor
     * @param walletService WalletService
     * @param creditCardService CreditCardService
     * @param walletTransactionService WalletTransactionService
     * @param recurringTransactionService RecurringTransactionService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public WalletController(WalletService               walletService,
                            CreditCardService           creditCardService,
                            WalletTransactionService    walletTransactionService,
                            RecurringTransactionService recurringTransactionService, ConfigurableApplicationContext springContext)

    {
        this.walletService               = walletService;
        this.creditCardService           = creditCardService;
        this.walletTransactionService    = walletTransactionService;
        this.recurringTransactionService = recurringTransactionService;
        this.springContext = springContext;
    }

    @FXML
    public void initialize()
    {
        totalBalanceSelectedMonth = LocalDate.now().getMonthValue();
        totalBalanceSelectedYear  = LocalDate.now().getYear();

        loadAllDataFromDatabase();

        totalBalancePaneWalletTypeComboBox.getItems().addAll(
            walletTypes.stream().map(WalletType::getName).toList());

        moneyFlowPaneWalletTypeComboBox.getItems().addAll(
            walletTypes.stream().map(WalletType::getName).toList());

        // Add the default wallet type and select it
        totalBalancePaneWalletTypeComboBox.getItems().addFirst("All Wallets");
        totalBalancePaneWalletTypeComboBox.getSelectionModel().selectFirst();

        moneyFlowPaneWalletTypeComboBox.getItems().addFirst("All Wallets");
        moneyFlowPaneWalletTypeComboBox.getSelectionModel().selectFirst();

        createDoughnutChartCheckBoxes();

        updateTotalBalanceView();
        updateDisplayWallets();
        updateMoneyFlowBarChart();
        updateDoughnutChart();

        setButtonsActions();
    }

    @FXML
    private void handleAddTransfer()
    {
        WindowUtils.openModalWindow(Constants.ADD_TRANSFER_FXML,
                                    "Add Transfer",
                                    springContext,
                                    (AddTransferController controller)
                                        -> {},
                                    List.of(() -> {
                                        loadWalletsFromDatabase();
                                        loadWalletTransactionsFromDatabase();
                                        updateDisplayWallets();
                                        updateTotalBalanceView();
                                        updateDoughnutChart();
                                    }));
    }

    @FXML
    private void handleAddWallet()
    {
        WindowUtils.openModalWindow(Constants.ADD_WALLET_FXML,
                                    "Add Wallet",
                                    springContext,
                                    (AddWalletController controller)
                                        -> {},
                                    List.of(() -> {
                                        loadWalletsFromDatabase();
                                        loadWalletTransactionsFromDatabase();
                                        updateDisplayWallets();
                                        updateTotalBalanceView();
                                        updateDoughnutChart();
                                    }));
    }

    @FXML
    private void handleViewArchivedWallets()
    {
        WindowUtils.openModalWindow(Constants.ARCHIVED_WALLETS_FXML,
                                    "Archived Wallets",
                                    springContext,
                                    (ArchivedWalletsController controller)
                                        -> {},
                                    List.of(() -> {
                                        loadAllDataFromDatabase();
                                        updateDisplayWallets();
                                        updateTotalBalanceView();
                                        updateMoneyFlowBarChart();
                                        updateDoughnutChart();
                                    }));
    }

    /**
     * Update the display
     * @note: This method can be called by other controllers to update the screen when
     * there is a change
     */
    public void updateDisplay()
    {
        loadAllDataFromDatabase();

        updateTotalBalanceView();
        updateDisplayWallets();
        updateMoneyFlowBarChart();
        updateDoughnutChart();
    }

    /**
     * Set the actions for the buttons
     */
    private void setButtonsActions()
    {
        totalBalancePaneWalletTypeComboBox.setOnAction(e -> updateTotalBalanceView());
        moneyFlowPaneWalletTypeComboBox.setOnAction(
            e -> updateMoneyFlowBarChart());

        walletPrevButton.setOnAction(event -> {
            if (walletPaneCurrentPage > 0)
            {
                walletPaneCurrentPage--;
                updateDisplayWallets();
            }
        });

        walletNextButton.setOnAction(event -> {
            if (walletPaneCurrentPage < wallets.size() / itemsPerPage)
            {
                walletPaneCurrentPage++;
                updateDisplayWallets();
            }
        });
    }

    private void loadAllDataFromDatabase()
    {
        loadWalletTransactionsFromDatabase();
        loadWalletTypesFromDatabase();
        loadWalletsFromDatabase();
    }

    /**
     * Load the wallet transactions
     */
    private void loadWalletTransactionsFromDatabase()
    {
        transactions =
            walletTransactionService.getTransactionsByMonth(totalBalanceSelectedMonth,
                                                            totalBalanceSelectedYear);
    }

    /**
     * Load the wallets
     */
    private void loadWalletsFromDatabase()
    {
        wallets = walletService.getAllNonArchivedWalletsOrderedByTransactionCountDesc();
    }

    /**
     * Load the wallet types
     */
    private void loadWalletTypesFromDatabase()
    {
        walletTypes = walletService.getAllWalletTypes();

        String nameToMove = "Others";

        // Move the "Others" wallet type to the end of the list
        if (walletTypes.stream()
                .anyMatch(n -> n.getName().equals(nameToMove)))
        {
            WalletType wt =
                walletTypes.stream()
                    .filter(n -> n.getName().equals(nameToMove))
                    .findFirst()
                    .orElseThrow(
                        () -> new IllegalStateException("Wallet type not found"));

            walletTypes.remove(wt);
            walletTypes.add(wt);
        }
    }

    /**
     * Update the display of the total balance pane
     */
    private void updateTotalBalanceView()
    {
        BigDecimal pendingExpenses       = BigDecimal.ZERO;
        BigDecimal pendingIncomes        = BigDecimal.ZERO;
        BigDecimal walletsCurrentBalance = BigDecimal.ZERO;
        long totalWallets          = 0L;

        // Filter wallet types, according to the selected item,
        // If "All Wallets" is selected, show all transactions
        int selectedIndex =
            totalBalancePaneWalletTypeComboBox.getSelectionModel().getSelectedIndex();

        if (selectedIndex == 0)
        {
            logger.info("Selected: {}", totalBalancePaneWalletTypeComboBox.getSelectionModel()
                    .getSelectedIndex());

            walletsCurrentBalance = wallets.stream()
                                        .map(Wallet::getBalance)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingExpenses =
                transactions.stream()
                    .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                    .filter(t -> t.getStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingIncomes =
                transactions.stream()
                    .filter(t -> t.getType().equals(TransactionType.INCOME))
                    .filter(t -> t.getStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalWallets = wallets.stream().map(Wallet::getId).distinct().count();
        }
        else if (selectedIndex > 0 && selectedIndex - 1 < walletTypes.size())
        {
            WalletType selectedWalletType = walletTypes.get(selectedIndex - 1);

            logger.info("Selected: {}", selectedWalletType.getName());

            walletsCurrentBalance =
                wallets.stream()
                    .filter(w -> w.getType().getId().equals(selectedWalletType.getId()))
                    .map(Wallet::getBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingExpenses =
                transactions.stream()
                    .filter(t
                            -> t.getWallet().getType().getId().equals(
                                selectedWalletType.getId()))
                    .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                    .filter(t -> t.getStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingIncomes =
                transactions.stream()
                    .filter(t
                            -> t.getWallet().getType().getId().equals(
                                selectedWalletType.getId()))
                    .filter(t -> t.getType().equals(TransactionType.INCOME))
                    .filter(t -> t.getStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalWallets =
                wallets.stream()
                    .filter(w -> w.getType().getId().equals(selectedWalletType.getId()))
                    .map(Wallet::getId)
                    .distinct()
                    .count();
        }
        else
        {
            logger.warn("Invalid index: {}", selectedIndex);
        }

        BigDecimal foreseenBalance =
            walletsCurrentBalance.add(pendingExpenses).subtract(pendingIncomes);

        Label totalBalanceValueLabel =
            new Label(UIUtils.formatCurrency(walletsCurrentBalance));

        totalBalanceValueLabel.getStyleClass().add(
            Constants.TOTAL_BALANCE_VALUE_LABEL_STYLE);

        Label balanceForeseenLabel =
            new Label("Foreseen: " + UIUtils.formatCurrency(foreseenBalance));

        balanceForeseenLabel.getStyleClass().add(
            Constants.TOTAL_BALANCE_FORESEEN_LABEL_STYLE);

        Label totalWalletsLabel =
            new Label("Balance corresponds to " + totalWallets + " wallets");
        totalWalletsLabel.getStyleClass().add(
            Constants.WALLET_TOTAL_BALANCE_WALLETS_LABEL_STYLE);

        totalBalancePaneInfoVBox.getChildren().clear();
        totalBalancePaneInfoVBox.getChildren().add(totalBalanceValueLabel);
        totalBalancePaneInfoVBox.getChildren().add(balanceForeseenLabel);
        totalBalancePaneInfoVBox.getChildren().add(totalWalletsLabel);
    }

    /**
     * Update the display of wallets
     */
    private void updateDisplayWallets()
    {
        walletPane1.getChildren().clear();
        walletPane2.getChildren().clear();
        walletPane3.getChildren().clear();

        Integer start = walletPaneCurrentPage * itemsPerPage;
        int end   = Math.min(start + itemsPerPage, wallets.size());

        for (Integer i = start; i < end; i++)
        {
            Wallet wallet = wallets.get(i);

            try
            {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(Constants.WALLET_FULL_PANE_FXML));
                loader.setControllerFactory(springContext::getBean);
                Parent newContent = loader.load();

                // Add style class to the wallet pane
                newContent.getStylesheets().add(
                    Objects.requireNonNull(getClass()
                                    .getResource(Constants.COMMON_STYLE_SHEET))
                        .toExternalForm());

                WalletFullPaneController walletFullPaneController =
                    loader.getController();

                walletFullPaneController.updateWalletPane(wallet);

                AnchorPane.setTopAnchor(newContent, 0.0);
                AnchorPane.setBottomAnchor(newContent, 0.0);
                AnchorPane.setLeftAnchor(newContent, 0.0);
                AnchorPane.setRightAnchor(newContent, 0.0);

                switch (i % itemsPerPage)
                {
                    case 0:
                        walletPane1.getChildren().add(newContent);
                        break;

                    case 1:
                        walletPane2.getChildren().add(newContent);
                        break;

                    case 2:
                        walletPane3.getChildren().add(newContent);
                        break;
                }
            }
            catch (IOException e)
            {
                logger.error("Error while loading wallet full pane");
            }
        }

        walletPrevButton.setDisable(walletPaneCurrentPage == 0);
        walletNextButton.setDisable(end >= wallets.size());
    }

    /**
     * Update the chart with the selected wallet types
     */
    private void updateDoughnutChart()
    {
        ObservableList<PieChart.Data> pieChartData =
            FXCollections.observableArrayList();

        for (CheckBox checkBox : doughnutChartCheckBoxes)
        {
            checkBox.getStyleClass().add(Constants.WALLET_CHECK_BOX_STYLE);

            if (checkBox.isSelected())
            {
                String     walletTypeName = checkBox.getText();
                WalletType wt =
                    walletTypes.stream()
                        .filter(type -> type.getName().equals(walletTypeName))
                        .findFirst()
                        .orElse(null);

                // If the wallet type is not found, skip
                if (wt != null)
                {
                    BigDecimal totalBalance =
                        wallets.stream()
                            .filter(w -> w.getType().getId().equals(wt.getId()))
                            .map(Wallet::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    pieChartData.add(
                        new PieChart.Data(wt.getName(), totalBalance.doubleValue()));
                }
            }
        }

        DoughnutChart doughnutChart = new DoughnutChart(pieChartData);
        doughnutChart.setLabelsVisible(false);

        // Remove the previous chart and add the new one
        balanceByWalletTypePieChartAnchorPane.getChildren().removeIf(
            node -> node instanceof DoughnutChart);

        balanceByWalletTypePieChartAnchorPane.getChildren().add(doughnutChart);

        AnchorPane.setTopAnchor(doughnutChart, 0.0);
        AnchorPane.setBottomAnchor(doughnutChart, 0.0);
        AnchorPane.setLeftAnchor(doughnutChart, 0.0);
        AnchorPane.setRightAnchor(doughnutChart, 0.0);
    }

    /**
     * Update the chart with incomes and expenses for the last months
     */
    private void updateMoneyFlowBarChart()
    {
        // Create a new bar chart
        // This is necessary to clear the previous data
        createMoneyFlowBarChart();

        // LinkedHashMap to keep the order of the months
        Map<String, Double> monthlyExpenses = new LinkedHashMap<>();
        Map<String, Double> monthlyIncomes  = new LinkedHashMap<>();

        LocalDateTime maxMonth =
            LocalDateTime.now().plusMonths(Constants.XYBAR_CHART_FUTURE_MONTHS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        int totalMonths =
            Constants.XYBAR_CHART_MONTHS + Constants.XYBAR_CHART_FUTURE_MONTHS;

        // Filter wallet types, according to the selected item,
        // If "All Wallets" is selected, show all transactions
        int selectedIndex =
            moneyFlowPaneWalletTypeComboBox.getSelectionModel().getSelectedIndex();

        // Collect data for the last months
        for (int i = 0; i < totalMonths; i++)
        {
            // Get the data from the oldest month to the most recent, to keep the order
            LocalDateTime date  = maxMonth.minusMonths(totalMonths - i - 1);
            int month = date.getMonthValue();
            int year  = date.getYear();

            // Get transactions
            List<WalletTransaction> transactions =
                walletTransactionService.getNonArchivedTransactionsByMonth(month, year);

            // Get future transactions and merge with the current transactions
            List<WalletTransaction> futureTransactions =
                recurringTransactionService.getFutureTransactionsByMonth(
                    YearMonth.of(year, month),
                    YearMonth.of(year, month));

            List<CreditCardPayment> crcPayments =
                creditCardService.getCreditCardPayments(month, year);

            transactions.addAll(futureTransactions);

            logger.info("Found {} ({} future) transactions for {}/{}", transactions.size(), futureTransactions.size(), month, year);

            BigDecimal totalExpenses = BigDecimal.ZERO;
            BigDecimal totalIncomes  = BigDecimal.ZERO;

            if (selectedIndex == 0)
            {
                // Calculate total expenses for the month
                totalExpenses =
                    transactions.stream()
                        .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalExpenses =
                    totalExpenses.add(crcPayments.stream()
                                          .map(CreditCardPayment::getAmount)
                                          .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Calculate total incomes for the month
                totalIncomes =
                    transactions.stream()
                        .filter(t -> t.getType().equals(TransactionType.INCOME))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            else if (selectedIndex > 0 && selectedIndex - 1 < walletTypes.size())
            {
                WalletType selectedWalletType = walletTypes.get(selectedIndex - 1);

                // Calculate total expenses for the month
                totalExpenses =
                    transactions.stream()
                        .filter(t
                                -> t.getWallet().getType().getId().equals(
                                    selectedWalletType.getId()))
                        .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalExpenses = totalExpenses.add(
                    crcPayments.stream()
                        .filter(p -> {
                            if (p.getWallet() != null)
                            {
                                return p.getWallet().getType().getId().equals(
                                    selectedWalletType.getId());
                            }
                            else if (p.getCreditCardDebt()
                                         .getCreditCard()
                                         .getDefaultBillingWallet() != null)
                            {
                                return p.getCreditCardDebt()
                                    .getCreditCard()
                                    .getDefaultBillingWallet()
                                    .getType()
                                    .getId()
                                    .equals(selectedWalletType.getId());
                            }
                            else
                            {
                                return false;
                            }
                        })
                        .map(CreditCardPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Calculate total incomes for the month
                totalIncomes =
                    transactions.stream()
                        .filter(t
                                -> t.getWallet().getType().getId().equals(
                                    selectedWalletType.getId()))
                        .filter(t -> t.getType().equals(TransactionType.INCOME))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            else
            {
                logger.warn("Invalid index: {}", selectedIndex);
            }

            monthlyExpenses.put(date.format(formatter), totalExpenses.doubleValue());
            monthlyIncomes.put(date.format(formatter), totalIncomes.doubleValue());
        }

        // Create two series: one for incomes and one for expenses
        XYChart.Series<String, Number> expensesSeries = new XYChart.Series<>();
        expensesSeries.setName("Expenses");

        XYChart.Series<String, Number> incomesSeries = new XYChart.Series<>();
        incomesSeries.setName("Incomes");

        double maxValue = 0.0;

        // Add data to each series
        for (Map.Entry<String, Double> entry : monthlyExpenses.entrySet())
        {
            String month        = entry.getKey();
            Double expenseValue = entry.getValue();
            Double incomeValue  = monthlyIncomes.getOrDefault(month, 0.0);

            expensesSeries.getData().add(
                new XYChart.Data<>(month, 0.0)); // Start at 0 for animation
            incomesSeries.getData().add(
                new XYChart.Data<>(month, 0.0)); // Start at 0 for animation

            maxValue = Math.max(maxValue, Math.max(expenseValue, incomeValue));
        }

        // Set Y-axis limits based on the maximum value found
        Axis<?> yAxis = moneyFlowBarChart.getYAxis();
        if (yAxis instanceof NumberAxis numberAxis)
        {
            Animation.setDynamicYAxisBounds(numberAxis, maxValue);

            numberAxis.setTickLabelFormatter(new StringConverter<>() {
                @Override
                public String toString(Number value) {
                    return UIUtils.formatCurrency(value);
                }

                @Override
                public Number fromString(String string) {
                    return 0;
                }
            });
        }

        moneyFlowBarChart.setVerticalGridLinesVisible(false);

        moneyFlowBarChart.getData().add(expensesSeries);
        moneyFlowBarChart.getData().add(incomesSeries);

        for (int i = 0; i < expensesSeries.getData().size(); i++)
        {
            XYChart.Data<String, Number> expenseData = expensesSeries.getData().get(i);
            XYChart.Data<String, Number> incomeData  = incomesSeries.getData().get(i);

            Double targetExpenseValue = monthlyExpenses.get(expenseData.getXValue());

            // Add tooltip to the bars
            UIUtils.addTooltipToXYChartNode(expenseData.getNode(),
                                            UIUtils.formatCurrency(targetExpenseValue));

            Double targetIncomeValue =
                monthlyIncomes.getOrDefault(expenseData.getXValue(), 0.0);

            UIUtils.addTooltipToXYChartNode(incomeData.getNode(),
                                            UIUtils.formatCurrency(targetIncomeValue));

            // Animation for the bars
            Animation.xyChartAnimation(expenseData, targetExpenseValue);
            Animation.xyChartAnimation(incomeData, targetIncomeValue);
        }
    }

    /**
     * Update the doughnut chart
     */
    private void createDoughnutChartCheckBoxes()
    {
        balanceByWalletTypePieChartAnchorPane.getChildren().clear();
        totalBalanceByWalletTypeVBox.getChildren().clear();

        // Create a checkbox for each wallet type
        doughnutChartCheckBoxes = new ArrayList<>();

        for (WalletType wt : walletTypes)
        {
            CheckBox checkBox = new CheckBox(wt.getName());
            checkBox.getStyleClass().add(Constants.WALLET_CHECK_BOX_STYLE);
            checkBox.setSelected(true);
            doughnutChartCheckBoxes.add(checkBox);
            totalBalanceByWalletTypeVBox.getChildren().add(checkBox);

            // Add listener to the checkbox
            checkBox.selectedProperty().addListener(
                (obs, wasSelected, isNowSelected) -> updateDoughnutChart());
        }
    }

    /**
     * Create a new bar chart
     */
    private void createMoneyFlowBarChart()
    {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();

        moneyFlowBarChart = new BarChart<>(xAxis, yAxis);

        moneyFlowBarChartAnchorPane.getChildren().clear();

        moneyFlowBarChartAnchorPane.getChildren().add(moneyFlowBarChart);

        AnchorPane.setTopAnchor(moneyFlowBarChart, 0.0);
        AnchorPane.setBottomAnchor(moneyFlowBarChart, 0.0);
        AnchorPane.setLeftAnchor(moneyFlowBarChart, 0.0);
        AnchorPane.setRightAnchor(moneyFlowBarChart, 0.0);
    }
}
