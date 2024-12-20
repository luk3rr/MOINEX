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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
import org.moinex.charts.DoughnutChart;
import org.moinex.entities.CreditCardPayment;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletTransaction;
import org.moinex.entities.WalletType;
import org.moinex.services.CreditCardService;
import org.moinex.services.RecurringTransactionService;
import org.moinex.services.WalletService;
import org.moinex.services.WalletTransactionService;
import org.moinex.ui.common.WalletFullPaneController;
import org.moinex.ui.dialog.AddTransferController;
import org.moinex.ui.dialog.AddWalletController;
import org.moinex.ui.dialog.ArchivedWalletsController;
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
 * Controller for the wallet view
 */
@Controller
public class WalletController
{
    private static final Logger logger = LoggerConfig.GetLogger();

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

    @Autowired
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

    private Integer itemsPerPage = 3;

    public WalletController() { }

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
                            RecurringTransactionService recurringTransactionService)

    {
        this.walletService               = walletService;
        this.creditCardService           = creditCardService;
        this.walletTransactionService    = walletTransactionService;
        this.recurringTransactionService = recurringTransactionService;
    }

    @FXML
    public void initialize()
    {
        totalBalanceSelectedMonth = LocalDate.now().getMonthValue();
        totalBalanceSelectedYear  = LocalDate.now().getYear();

        LoadAllDataFromDatabase();

        totalBalancePaneWalletTypeComboBox.getItems().addAll(
            walletTypes.stream().map(WalletType::GetName).toList());

        moneyFlowPaneWalletTypeComboBox.getItems().addAll(
            walletTypes.stream().map(WalletType::GetName).toList());

        // Add default wallet type and select it
        totalBalancePaneWalletTypeComboBox.getItems().add(0, "All Wallets");
        totalBalancePaneWalletTypeComboBox.getSelectionModel().selectFirst();

        moneyFlowPaneWalletTypeComboBox.getItems().add(0, "All Wallets");
        moneyFlowPaneWalletTypeComboBox.getSelectionModel().selectFirst();

        CreateDoughnutChartCheckBoxes();

        UpdateTotalBalanceView();
        UpdateDisplayWallets();
        UpdateMoneyFlowBarChart();
        UpdateDoughnutChart();

        SetButtonsActions();
    }

    @FXML
    private void handleAddTransfer()
    {
        WindowUtils.OpenModalWindow(Constants.ADD_TRANSFER_FXML,
                                    "Add Transfer",
                                    springContext,
                                    (AddTransferController controller)
                                        -> {},
                                    List.of(() -> {
                                        LoadWalletsFromDatabase();
                                        LoadWalletTransactionsFromDatabase();
                                        UpdateDisplayWallets();
                                        UpdateTotalBalanceView();
                                        UpdateDoughnutChart();
                                    }));
    }

    @FXML
    private void handleAddWallet()
    {
        WindowUtils.OpenModalWindow(Constants.ADD_WALLET_FXML,
                                    "Add Wallet",
                                    springContext,
                                    (AddWalletController controller)
                                        -> {},
                                    List.of(() -> {
                                        LoadWalletsFromDatabase();
                                        LoadWalletTransactionsFromDatabase();
                                        UpdateDisplayWallets();
                                        UpdateTotalBalanceView();
                                        UpdateDoughnutChart();
                                    }));
    }

    @FXML
    private void handleViewArchivedWallets()
    {
        WindowUtils.OpenModalWindow(Constants.ARCHIVED_WALLETS_FXML,
                                    "Archived Wallets",
                                    springContext,
                                    (ArchivedWalletsController controller)
                                        -> {},
                                    List.of(() -> {
                                        LoadAllDataFromDatabase();
                                        UpdateDisplayWallets();
                                        UpdateTotalBalanceView();
                                        UpdateMoneyFlowBarChart();
                                        UpdateDoughnutChart();
                                    }));
    }

    /**
     * Update the display
     * @note: This method can be called by other controllers to update the screen when
     * there is a change
     */
    public void UpdateDisplay()
    {
        LoadAllDataFromDatabase();

        UpdateTotalBalanceView();
        UpdateDisplayWallets();
        UpdateMoneyFlowBarChart();
        UpdateDoughnutChart();
    }

    /**
     * Set the actions for the buttons
     */
    private void SetButtonsActions()
    {
        totalBalancePaneWalletTypeComboBox.setOnAction(e -> UpdateTotalBalanceView());
        moneyFlowPaneWalletTypeComboBox.setOnAction(
            e -> { UpdateMoneyFlowBarChart(); });

        walletPrevButton.setOnAction(event -> {
            if (walletPaneCurrentPage > 0)
            {
                walletPaneCurrentPage--;
                UpdateDisplayWallets();
            }
        });

        walletNextButton.setOnAction(event -> {
            if (walletPaneCurrentPage < wallets.size() / itemsPerPage)
            {
                walletPaneCurrentPage++;
                UpdateDisplayWallets();
            }
        });
    }

    private void LoadAllDataFromDatabase()
    {
        LoadWalletTransactionsFromDatabase();
        LoadWalletTypesFromDatabase();
        LoadWalletsFromDatabase();
    }

    /**
     * Load the wallet transactions
     */
    private void LoadWalletTransactionsFromDatabase()
    {
        transactions =
            walletTransactionService.GetTransactionsByMonth(totalBalanceSelectedMonth,
                                                            totalBalanceSelectedYear);
    }

    /**
     * Load the wallets
     */
    private void LoadWalletsFromDatabase()
    {
        wallets = walletService.GetAllNonArchivedWalletsOrderedByTransactionCountDesc();
    }

    /**
     * Load the wallet types
     */
    private void LoadWalletTypesFromDatabase()
    {
        walletTypes = walletService.GetAllWalletTypes();

        String nameToMove = "Others";

        // Move the "Others" wallet type to the end of the list
        if (walletTypes.stream()
                .filter(n -> n.GetName().equals(nameToMove))
                .findFirst()
                .isPresent())
        {
            WalletType wt = walletTypes.stream()
                                .filter(n -> n.GetName().equals(nameToMove))
                                .findFirst()
                                .get();

            walletTypes.remove(wt);
            walletTypes.add(wt);
        }
    }

    /**
     * Update the display of the total balance pane
     */
    private void UpdateTotalBalanceView()
    {
        BigDecimal pendingExpenses       = BigDecimal.ZERO;
        BigDecimal pendingIncomes        = BigDecimal.ZERO;
        BigDecimal walletsCurrentBalance = BigDecimal.ZERO;
        Long       totalWallets          = 0L;

        // Filter wallet type according to the selected item
        // If "All Wallets" is selected, show all transactions
        Integer selectedIndex =
            totalBalancePaneWalletTypeComboBox.getSelectionModel().getSelectedIndex();

        if (selectedIndex == 0)
        {
            logger.info("Selected: " +
                        totalBalancePaneWalletTypeComboBox.getSelectionModel()
                            .getSelectedIndex());

            walletsCurrentBalance = wallets.stream()
                                        .map(Wallet::GetBalance)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingExpenses =
                transactions.stream()
                    .filter(t -> t.GetType().equals(TransactionType.EXPENSE))
                    .filter(t -> t.GetStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::GetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingIncomes =
                transactions.stream()
                    .filter(t -> t.GetType().equals(TransactionType.INCOME))
                    .filter(t -> t.GetStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::GetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalWallets = wallets.stream().map(w -> w.GetId()).distinct().count();
        }
        else if (selectedIndex > 0 && selectedIndex - 1 < walletTypes.size())
        {
            WalletType selectedWalletType = walletTypes.get(selectedIndex - 1);

            logger.info("Selected: " + selectedWalletType.GetName());

            walletsCurrentBalance =
                wallets.stream()
                    .filter(w -> w.GetType().GetId() == selectedWalletType.GetId())
                    .map(Wallet::GetBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingExpenses =
                transactions.stream()
                    .filter(t
                            -> t.GetWallet().GetType().GetId() ==
                                   selectedWalletType.GetId())
                    .filter(t -> t.GetType().equals(TransactionType.EXPENSE))
                    .filter(t -> t.GetStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::GetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            pendingIncomes =
                transactions.stream()
                    .filter(t
                            -> t.GetWallet().GetType().GetId() ==
                                   selectedWalletType.GetId())
                    .filter(t -> t.GetType().equals(TransactionType.INCOME))
                    .filter(t -> t.GetStatus().equals(TransactionStatus.PENDING))
                    .map(WalletTransaction::GetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalWallets =
                wallets.stream()
                    .filter(w -> w.GetType().GetId() == selectedWalletType.GetId())
                    .map(w -> w.GetId())
                    .distinct()
                    .count();
        }
        else
        {
            logger.warning("Invalid index: " + selectedIndex);
        }

        BigDecimal foreseenBalance =
            walletsCurrentBalance.add(pendingExpenses).subtract(pendingIncomes);

        Label totalBalanceValueLabel =
            new Label(UIUtils.FormatCurrency(walletsCurrentBalance));

        totalBalanceValueLabel.getStyleClass().add(
            Constants.TOTAL_BALANCE_VALUE_LABEL_STYLE);

        Label balanceForeseenLabel =
            new Label("Foreseen: " + UIUtils.FormatCurrency(foreseenBalance));

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
    private void UpdateDisplayWallets()
    {
        walletPane1.getChildren().clear();
        walletPane2.getChildren().clear();
        walletPane3.getChildren().clear();

        Integer start = walletPaneCurrentPage * itemsPerPage;
        Integer end   = Math.min(start + itemsPerPage, wallets.size());

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
                    getClass()
                        .getResource(Constants.COMMON_STYLE_SHEET)
                        .toExternalForm());

                WalletFullPaneController walletFullPaneController =
                    loader.getController();

                walletFullPaneController.UpdateWalletPane(wallet);

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
                logger.severe("Error while loading wallet full pane");
                e.printStackTrace();
                continue;
            }
        }

        walletPrevButton.setDisable(walletPaneCurrentPage == 0);
        walletNextButton.setDisable(end >= wallets.size());
    }

    /**
     * Update the chart with the selected wallet types
     */
    private void UpdateDoughnutChart()
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
                        .filter(type -> type.GetName().equals(walletTypeName))
                        .findFirst()
                        .orElse(null);

                // If the wallet type is not found, skip
                if (wt != null)
                {
                    BigDecimal totalBalance =
                        wallets.stream()
                            .filter(w -> w.GetType().GetId() == wt.GetId())
                            .map(Wallet::GetBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    pieChartData.add(
                        new PieChart.Data(wt.GetName(), totalBalance.doubleValue()));
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
    private void UpdateMoneyFlowBarChart()
    {
        // Create a new bar chart
        // This is necessary to clear the previous data
        CreateMoneyFlowBarChart();

        // LinkedHashMap to keep the order of the months
        Map<String, Double> monthlyExpenses = new LinkedHashMap<>();
        Map<String, Double> monthlyIncomes  = new LinkedHashMap<>();

        LocalDateTime maxMonth =
            LocalDateTime.now().plusMonths(Constants.XYBAR_CHART_FUTURE_MONTHS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        Integer totalMonths =
            Constants.XYBAR_CHART_MONTHS + Constants.XYBAR_CHART_FUTURE_MONTHS;

        // Filter wallet type according to the selected item
        // If "All Wallets" is selected, show all transactions
        Integer selectedIndex =
            moneyFlowPaneWalletTypeComboBox.getSelectionModel().getSelectedIndex();

        // Collect data for the last months
        for (Integer i = 0; i < totalMonths; i++)
        {
            // Get the data from the oldest month to the most recent, to keep the order
            LocalDateTime date  = maxMonth.minusMonths(totalMonths - i - 1);
            Integer       month = date.getMonthValue();
            Integer       year  = date.getYear();

            // Get transactions
            List<WalletTransaction> transactions =
                walletTransactionService.GetNonArchivedTransactionsByMonth(month, year);

            // Get future transactions and merge with the current transactions
            List<WalletTransaction> futureTransactions =
                recurringTransactionService.GetFutureTransactionsByMonth(
                    YearMonth.of(year, month),
                    YearMonth.of(year, month));

            List<CreditCardPayment> crcPayments =
                creditCardService.GetCreditCardPayments(month, year);

            transactions.addAll(futureTransactions);

            logger.info("Found " + transactions.size() + " (" +
                        futureTransactions.size() + " future) transactions for " +
                        month + "/" + year);

            BigDecimal totalExpenses = BigDecimal.ZERO;
            BigDecimal totalIncomes  = BigDecimal.ZERO;

            if (selectedIndex == 0)
            {
                // Calculate total expenses for the month
                totalExpenses = transactions.stream()
                                    .filter(t -> t.GetType() == TransactionType.EXPENSE)
                                    .map(WalletTransaction::GetAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalExpenses =
                    totalExpenses.add(crcPayments.stream()
                                          .map(CreditCardPayment::GetAmount)
                                          .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Calculate total incomes for the month
                totalIncomes = transactions.stream()
                                   .filter(t -> t.GetType() == TransactionType.INCOME)
                                   .map(WalletTransaction::GetAmount)
                                   .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            else if (selectedIndex > 0 && selectedIndex - 1 < walletTypes.size())
            {
                WalletType selectedWalletType = walletTypes.get(selectedIndex - 1);

                // Calculate total expenses for the month
                totalExpenses = transactions.stream()
                                    .filter(t
                                            -> t.GetWallet().GetType().GetId() ==
                                                   selectedWalletType.GetId())
                                    .filter(t -> t.GetType() == TransactionType.EXPENSE)
                                    .map(WalletTransaction::GetAmount)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalExpenses = totalExpenses.add(
                    crcPayments.stream()
                        .filter(p -> {
                            if (p.GetWallet() != null)
                            {
                                return p.GetWallet().GetType().GetId() ==
                                    selectedWalletType.GetId();
                            }
                            else if (p.GetCreditCardDebt()
                                         .GetCreditCard()
                                         .GetDefaultBillingWallet() != null)
                            {
                                return p.GetCreditCardDebt()
                                           .GetCreditCard()
                                           .GetDefaultBillingWallet()
                                           .GetType()
                                           .GetId() == selectedWalletType.GetId();
                            }
                            else
                            {
                                return false;
                            }
                        })
                        .map(CreditCardPayment::GetAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                // Calculate total incomes for the month
                totalIncomes = transactions.stream()
                                   .filter(t
                                           -> t.GetWallet().GetType().GetId() ==
                                                  selectedWalletType.GetId())
                                   .filter(t -> t.GetType() == TransactionType.INCOME)
                                   .map(WalletTransaction::GetAmount)
                                   .reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            else
            {
                logger.warning("Invalid index: " + selectedIndex);
            }

            monthlyExpenses.put(date.format(formatter), totalExpenses.doubleValue());
            monthlyIncomes.put(date.format(formatter), totalIncomes.doubleValue());
        }

        // Create two series: one for incomes and one for expenses
        XYChart.Series<String, Number> expensesSeries = new XYChart.Series<>();
        expensesSeries.setName("Expenses");

        XYChart.Series<String, Number> incomesSeries = new XYChart.Series<>();
        incomesSeries.setName("Incomes");

        Double maxValue = 0.0;

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
        if (yAxis instanceof NumberAxis)
        {
            NumberAxis numberAxis = (NumberAxis)yAxis;
            Animation.SetDynamicYAxisBounds(numberAxis, maxValue);
        }

        moneyFlowBarChart.setVerticalGridLinesVisible(false);

        moneyFlowBarChart.getData().add(expensesSeries);
        moneyFlowBarChart.getData().add(incomesSeries);

        for (Integer i = 0; i < expensesSeries.getData().size(); i++)
        {
            XYChart.Data<String, Number> expenseData = expensesSeries.getData().get(i);
            XYChart.Data<String, Number> incomeData  = incomesSeries.getData().get(i);

            Double targetExpenseValue = monthlyExpenses.get(expenseData.getXValue());

            // Add tooltip to the bars
            UIUtils.AddTooltipToXYChartNode(expenseData.getNode(),
                                            UIUtils.FormatCurrency(targetExpenseValue));

            Double targetIncomeValue =
                monthlyIncomes.getOrDefault(expenseData.getXValue(), 0.0);

            UIUtils.AddTooltipToXYChartNode(incomeData.getNode(),
                                            UIUtils.FormatCurrency(targetIncomeValue));

            // Animation for the bars
            Animation.XYChartAnimation(expenseData, targetExpenseValue);
            Animation.XYChartAnimation(incomeData, targetIncomeValue);
        }
    }

    /**
     * Update the doughnut chart
     */
    private void CreateDoughnutChartCheckBoxes()
    {
        balanceByWalletTypePieChartAnchorPane.getChildren().clear();
        totalBalanceByWalletTypeVBox.getChildren().clear();

        // Create a checkbox for each wallet type
        doughnutChartCheckBoxes = new ArrayList<>();

        for (WalletType wt : walletTypes)
        {
            CheckBox checkBox = new CheckBox(wt.GetName());
            checkBox.getStyleClass().add(Constants.WALLET_CHECK_BOX_STYLE);
            checkBox.setSelected(true);
            doughnutChartCheckBoxes.add(checkBox);
            totalBalanceByWalletTypeVBox.getChildren().add(checkBox);

            // Add listener to the checkbox
            checkBox.selectedProperty().addListener(
                (obs, wasSelected, isNowSelected) -> { UpdateDoughnutChart(); });
        }
    }

    /**
     * Create a new bar chart
     */
    private void CreateMoneyFlowBarChart()
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
