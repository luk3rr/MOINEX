/*
 * Filename: HomeController.java
 * Created on: September 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.CreditCardService;
import org.moinex.service.I18nService;
import org.moinex.service.RecurringTransactionService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.common.ResumePaneController;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.enums.TransactionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the home view
 */
@Controller
@NoArgsConstructor
public class HomeController {
    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @FXML private JFXButton walletPrevButton;

    @FXML private JFXButton walletNextButton;

    @FXML private AnchorPane walletView1;

    @FXML private AnchorPane walletView2;

    @FXML private AnchorPane creditCardView1;

    @FXML private AnchorPane creditCardView2;

    @FXML private AnchorPane monthResumeView;

    @FXML private AnchorPane moneyFlowBarChartAnchorPane;

    @FXML private JFXButton creditCardPrevButton;

    @FXML private JFXButton creditCardNextButton;

    @FXML private BarChart<String, Number> moneyFlowBarChart;

    @FXML private Label monthResumePaneTitle;

    @FXML private TableView<WalletTransaction> transactionsTableView;

    private ConfigurableApplicationContext springContext;

    private List<Wallet> wallets;

    private List<CreditCard> creditCards;

    private List<WalletTransaction> transactions;

    private WalletService walletService;

    private WalletTransactionService walletTransactionService;

    private I18nService i18nService;

    private RecurringTransactionService recurringTransactionService;

    private CreditCardService creditCardService;

    private Integer walletPaneCurrentPage = 0;

    private Integer creditCardPaneCurrentPage = 0;

    /**
     * Constructor for injecting the wallet and credit card services
     * @param walletService The wallet service
     * @param walletTransactionService The wallet transaction service
     * @param recurringTransactionService The recurring transaction service
     * @param creditCardService The credit card service
     * @param springContext The spring context
     * @param i18nService The i18n service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public HomeController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            RecurringTransactionService recurringTransactionService,
            CreditCardService creditCardService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.recurringTransactionService = recurringTransactionService;
        this.creditCardService = creditCardService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadWalletsFromDatabase();
        loadCreditCardsFromDatabase();
        loadLastTransactionsFromDatabase(Constants.HOME_LAST_TRANSACTIONS_SIZE);

        logger.info("Loaded {} wallets from the database", wallets.size());

        logger.info("Loaded {} credit cards from the database", creditCards.size());

        // Update the display with the loaded data
        updateDisplayWallets();
        updateDisplayCreditCards();
        updateDisplayLastTransactions();
        updateMonthResume();
        updateMoneyFlowBarChart();

        setButtonsActions();
    }

    /**
     * Set the actions for the buttons
     */
    private void setButtonsActions() {
        walletPrevButton.setOnAction(
                event -> {
                    if (walletPaneCurrentPage > 0) {
                        walletPaneCurrentPage--;
                        updateDisplayWallets();
                    }
                });

        walletNextButton.setOnAction(
                event -> {
                    if (walletPaneCurrentPage
                            < wallets.size() / Constants.HOME_PANES_ITEMS_PER_PAGE) {
                        walletPaneCurrentPage++;
                        updateDisplayWallets();
                    }
                });

        creditCardPrevButton.setOnAction(
                event -> {
                    if (creditCardPaneCurrentPage > 0) {
                        creditCardPaneCurrentPage--;
                        updateDisplayCreditCards();
                    }
                });

        creditCardNextButton.setOnAction(
                event -> {
                    if (creditCardPaneCurrentPage
                            < creditCards.size() / Constants.HOME_PANES_ITEMS_PER_PAGE) {
                        creditCardPaneCurrentPage++;
                        updateDisplayCreditCards();
                    }
                });
    }

    /**
     * Load wallets from the database
     */
    private void loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    /**
     * Load credit cards from the database
     */
    private void loadCreditCardsFromDatabase() {
        creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName();
    }

    /**
     * Load the last transactions from the database
     * @param n The number of transactions to be loaded
     */
    private void loadLastTransactionsFromDatabase(Integer n) {
        transactions = walletTransactionService.getNonArchivedLastTransactions(n);
    }

    /**
     * Update the display of wallets
     */
    private void updateDisplayWallets() {
        walletView1.getChildren().clear();
        walletView2.getChildren().clear();

        Integer start = walletPaneCurrentPage * Constants.HOME_PANES_ITEMS_PER_PAGE;
        int end = Math.min(start + Constants.HOME_PANES_ITEMS_PER_PAGE, wallets.size());

        for (int i = start; i < end; i++) {
            Wallet wallet = wallets.get(i);
            HBox walletHBox = createWalletItemNode(wallet);

            AnchorPane.setTopAnchor(walletHBox, 0.0);
            AnchorPane.setBottomAnchor(walletHBox, 0.0);

            if (i % 2 == 0) {
                walletView1.getChildren().add(walletHBox);
                AnchorPane.setLeftAnchor(walletHBox, 0.0);
                AnchorPane.setRightAnchor(walletHBox, 10.0);
            } else {
                walletView2.getChildren().add(walletHBox);
                AnchorPane.setLeftAnchor(walletHBox, 10.0);
                AnchorPane.setRightAnchor(walletHBox, 0.0);
            }
        }

        walletPrevButton.setDisable(walletPaneCurrentPage == 0);
        walletNextButton.setDisable(end >= wallets.size());
    }

    /**
     * Update the display of credit cards
     */
    private void updateDisplayCreditCards() {
        creditCardView1.getChildren().clear();
        creditCardView2.getChildren().clear();

        Integer start = creditCardPaneCurrentPage * Constants.HOME_PANES_ITEMS_PER_PAGE;
        int end = Math.min(start + Constants.HOME_PANES_ITEMS_PER_PAGE, creditCards.size());

        for (int i = start; i < end; i++) {
            CreditCard creditCard = creditCards.get(i);
            HBox crcHbox = createCreditCardItemNode(creditCard);

            AnchorPane.setTopAnchor(crcHbox, 0.0);
            AnchorPane.setBottomAnchor(crcHbox, 0.0);

            if (i % 2 == 0) {
                creditCardView1.getChildren().add(crcHbox);
                AnchorPane.setLeftAnchor(crcHbox, 0.0);
                AnchorPane.setRightAnchor(crcHbox, 10.0);
            } else {
                creditCardView2.getChildren().add(crcHbox);
                AnchorPane.setLeftAnchor(crcHbox, 10.0);
                AnchorPane.setRightAnchor(crcHbox, 0.0);
            }
        }

        creditCardPrevButton.setDisable(creditCardPaneCurrentPage == 0);
        creditCardNextButton.setDisable(end >= creditCards.size());
    }

    /**
     * Update the display of the last transactions using VBox
     */
    private void updateDisplayLastTransactions() {
        transactionsTableView.getColumns().clear();

        TableColumn<WalletTransaction, WalletTransaction> transactionColumn =
                new TableColumn<>(
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys.HOME_TRANSACTIONS_TABLE_TITLE),
                                Constants.HOME_LAST_TRANSACTIONS_SIZE));

        transactionColumn.setCellValueFactory(
                param -> new SimpleObjectProperty<>(param.getValue()));

        // Set the cell factory to display the transaction information
        transactionColumn.setCellFactory(
                column ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(
                                    WalletTransaction transaction, boolean empty) {
                                super.updateItem(transaction, empty);
                                if (empty || transaction == null) {
                                    setGraphic(null);
                                } else {
                                    ImageView icon =
                                            transaction.getType() == TransactionType.INCOME
                                                    ? new ImageView(Constants.HOME_INCOME_ICON)
                                                    : new ImageView(Constants.HOME_EXPENSE_ICON);

                                    icon.setFitHeight(Constants.HOME_LAST_TRANSACTIONS_ICON_SIZE);
                                    icon.setFitWidth(Constants.HOME_LAST_TRANSACTIONS_ICON_SIZE);

                                    Label descriptionLabel =
                                            new Label(transaction.getDescription());
                                    descriptionLabel.setMinWidth(
                                            Constants
                                                    .HOME_LAST_TRANSACTIONS_DESCRIPTION_LABEL_WIDTH);

                                    Label valueLabel =
                                            new Label(
                                                    UIUtils.formatCurrency(
                                                            transaction.getAmount()));
                                    valueLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_VALUE_LABEL_WIDTH);

                                    Label walletLabel =
                                            new Label(transaction.getWallet().getName());
                                    walletLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_WALLET_LABEL_WIDTH);

                                    Label dateLabel =
                                            new Label(
                                                    transaction
                                                            .getDate()
                                                            .format(
                                                                    DateTimeFormatter.ofPattern(
                                                                            Constants
                                                                                    .DATE_FORMAT_NO_TIME)));
                                    dateLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_DATE_LABEL_WIDTH);

                                    Label transactionStatusLabel =
                                            new Label(
                                                    UIUtils.translateTransactionStatus(
                                                            transaction.getStatus(), i18nService));
                                    transactionStatusLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_STATUS_LABEL_WIDTH);

                                    Label transactionCategoryLabel =
                                            new Label(transaction.getCategory().getName());
                                    transactionCategoryLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_CATEGORY_LABEL_WIDTH);

                                    HBox descriptionValueBox =
                                            new HBox(descriptionLabel, valueLabel);
                                    descriptionValueBox.setAlignment(Pos.CENTER_LEFT);

                                    HBox walletCategoryStatusDateBox =
                                            new HBox(
                                                    walletLabel,
                                                    transactionCategoryLabel,
                                                    transactionStatusLabel,
                                                    dateLabel);
                                    walletCategoryStatusDateBox.setAlignment(Pos.CENTER_LEFT);

                                    VBox vbox =
                                            new VBox(
                                                    5,
                                                    descriptionValueBox,
                                                    walletCategoryStatusDateBox);
                                    HBox hbox = new HBox(10, icon, vbox);
                                    hbox.setAlignment(Pos.CENTER_LEFT);

                                    // Set style class based on the transaction type
                                    if (transaction.getType() == TransactionType.INCOME) {
                                        hbox.getStyleClass()
                                                .add(
                                                        Constants
                                                                .HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE);
                                    } else {
                                        hbox.getStyleClass()
                                                .add(
                                                        Constants
                                                                .HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE);
                                    }

                                    setGraphic(hbox);
                                }
                            }
                        });

        transactionsTableView.getColumns().add(transactionColumn);

        transactionsTableView.setItems(FXCollections.observableArrayList(transactions));
    }

    /**
     * Update the chart with incomes and expenses for the last months
     */
    private void updateMoneyFlowBarChart() {
        createMoneyFlowBarChart();

        // LinkedHashMap to keep the order of the months
        Map<String, Double> monthlyExpenses = new LinkedHashMap<>();
        Map<String, Double> monthlyIncomes = new LinkedHashMap<>();

        LocalDateTime maxMonth =
                LocalDateTime.now().plusMonths(Constants.XYBAR_CHART_FUTURE_MONTHS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        int totalMonths = Constants.XYBAR_CHART_MONTHS + Constants.XYBAR_CHART_FUTURE_MONTHS;

        // Collect data for the last months and the future months
        for (int i = 0; i < totalMonths; i++) {
            // Get the data from the oldest month to the most recent, to keep the order
            LocalDateTime date = maxMonth.minusMonths(totalMonths - i - 1L);
            Integer month = date.getMonthValue();
            Integer year = date.getYear();

            // Get transactions
            List<WalletTransaction> nonArchivedTransactions =
                    walletTransactionService.getNonArchivedTransactionsByMonth(month, year);

            // Get future transactions and merge with the current transactions
            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByMonth(
                            YearMonth.of(year, month), YearMonth.of(year, month));

            nonArchivedTransactions.addAll(futureTransactions);

            logger.info(
                    "Found {} ({} future) transactions for {}/{}",
                    nonArchivedTransactions.size(),
                    futureTransactions.size(),
                    month,
                    year);

            BigDecimal crcPaidPayments =
                    creditCardService.getEffectivePaidPaymentsByMonth(month, year);

            BigDecimal crcPendingPayments =
                    creditCardService.getPendingPaymentsByMonth(month, year);

            // Calculate total expenses for the month
            BigDecimal totalExpenses =
                    nonArchivedTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Consider credit card payments as expenses
            totalExpenses = totalExpenses.add(crcPaidPayments).add(crcPendingPayments);

            // Calculate total incomes for the month
            BigDecimal totalIncomes =
                    nonArchivedTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.INCOME))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyExpenses.put(date.format(formatter), totalExpenses.doubleValue());
            monthlyIncomes.put(date.format(formatter), totalIncomes.doubleValue());
        }

        // Create two series: one for incomes and one for expenses
        XYChart.Series<String, Number> expensesSeries = new XYChart.Series<>();
        expensesSeries.setName(i18nService.tr(Constants.TranslationKeys.TRANSACTION_TYPE_EXPENSES));

        XYChart.Series<String, Number> incomesSeries = new XYChart.Series<>();
        incomesSeries.setName(i18nService.tr(Constants.TranslationKeys.TRANSACTION_TYPE_INCOMES));

        double maxValue = 0.0;

        // Add data to each series
        for (Map.Entry<String, Double> entry : monthlyExpenses.entrySet()) {
            String month = entry.getKey();
            Double expenseValue = entry.getValue();
            Double incomeValue = monthlyIncomes.getOrDefault(month, 0.0);

            expensesSeries
                    .getData()
                    .add(new XYChart.Data<>(month, 0.0)); // start at 0 for animation
            incomesSeries.getData().add(new XYChart.Data<>(month, 0.0)); // start at 0 for animation

            maxValue = Math.max(maxValue, Math.max(expenseValue, incomeValue));
        }

        // Set Y-axis limits based on the maximum value found
        Axis<?> yAxis = moneyFlowBarChart.getYAxis();
        if (yAxis instanceof NumberAxis numberAxis) {
            Animation.setDynamicYAxisBounds(numberAxis, maxValue);

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
        }

        moneyFlowBarChart.setVerticalGridLinesVisible(false);

        // Clear previous data and add the new series (expenses and incomes)
        moneyFlowBarChart.getData().add(expensesSeries);
        moneyFlowBarChart.getData().add(incomesSeries);

        // Add tooltips and animations to the bars
        // expensesSeries and incomesSeries have the same size
        for (int i = 0; i < expensesSeries.getData().size(); i++) {
            XYChart.Data<String, Number> expenseData = expensesSeries.getData().get(i);
            XYChart.Data<String, Number> incomeData = incomesSeries.getData().get(i);

            Double targetExpenseValue = monthlyExpenses.get(expenseData.getXValue());

            // Add tooltip to the bars
            UIUtils.addTooltipToXYChartNode(
                    expenseData.getNode(), UIUtils.formatCurrency(targetExpenseValue));

            Double targetIncomeValue = monthlyIncomes.getOrDefault(expenseData.getXValue(), 0.0);

            UIUtils.addTooltipToXYChartNode(
                    incomeData.getNode(), UIUtils.formatCurrency(targetIncomeValue));

            // Animation for the bars
            Animation.xyChartAnimation(expenseData, targetExpenseValue);
            Animation.xyChartAnimation(incomeData, targetIncomeValue);
        }
    }

    /**
     * Update the display of the month resume
     */
    private void updateMonthResume() {
        try {
            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(Constants.RESUME_PANE_FXML),
                            i18nService.getBundle());
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            // Add style class to the wallet pane
            newContent
                    .getStylesheets()
                    .add(
                            Objects.requireNonNull(
                                            getClass().getResource(Constants.COMMON_STYLE_SHEET))
                                    .toExternalForm());

            ResumePaneController resumePaneController = loader.getController();

            LocalDateTime currentDate = LocalDateTime.now();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");
            monthResumePaneTitle.setText(
                    MessageFormat.format(
                            i18nService.tr(Constants.TranslationKeys.HOME_RESUME_TITLE),
                            currentDate.format(formatter)));

            resumePaneController.updateResumePane(
                    currentDate.getMonthValue(), currentDate.getYear());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 0.0);
            AnchorPane.setRightAnchor(newContent, 0.0);

            monthResumeView.getChildren().clear();
            monthResumeView.getChildren().add(newContent);
        } catch (Exception e) {
            logger.error("Error updating month resume: {}", e.getMessage());
        }
    }

    /**
     * Create a node for a credit card
     * @param creditCard The credit card to be displayed
     * @return The HBox containing the credit card information
     */
    private HBox createCreditCardItemNode(CreditCard creditCard) {
        HBox rootHbox = new HBox(10);
        rootHbox.getStyleClass().add(Constants.HOME_CREDIT_CARD_ITEM_STYLE);

        VBox infoVbox = new VBox(10);
        infoVbox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(creditCard.getName());
        nameLabel.setMaxWidth(Constants.HOME_ITEM_NODE_NAME_MAX_LENGTH);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.getStyleClass().add(Constants.HOME_CREDIT_CARD_ITEM_NAME_STYLE);
        UIUtils.addTooltipToNode(
                nameLabel,
                i18nService.tr(
                        Constants.TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_NAME));

        Label crcOperatorLabel = new Label(creditCard.getOperator().getName());
        crcOperatorLabel.getStyleClass().add(Constants.HOME_CREDIT_CARD_ITEM_OPERATOR_STYLE);
        crcOperatorLabel.setAlignment(Pos.TOP_LEFT);
        UIUtils.addTooltipToNode(
                crcOperatorLabel,
                i18nService.tr(
                        Constants.TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_OPERATOR));

        Label availableCredit =
                new Label(
                        UIUtils.formatCurrency(
                                creditCardService.getAvailableCredit(creditCard.getId())));

        availableCredit.getStyleClass().add(Constants.HOME_CREDIT_CARD_ITEM_BALANCE_STYLE);

        UIUtils.addTooltipToNode(
                availableCredit,
                i18nService.tr(
                        Constants.TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_AVAILABLE_CREDIT));

        Label digitsLabel =
                new Label(UIUtils.formatCreditCardNumber(creditCard.getLastFourDigits()));
        digitsLabel.getStyleClass().add(Constants.HOME_CREDIT_CARD_ITEM_DIGITS_STYLE);
        UIUtils.addTooltipToNode(
                digitsLabel,
                i18nService.tr(
                        Constants.TranslationKeys.HOME_CREDIT_CARD_TOOLTIP_CREDIT_CARD_NUMBER));

        infoVbox.getChildren().addAll(nameLabel, crcOperatorLabel, availableCredit, digitsLabel);

        ImageView icon =
                new ImageView(
                        Constants.CRC_OPERATOR_ICONS_PATH + creditCard.getOperator().getIcon());

        icon.setFitHeight(Constants.CRC_OPERATOR_ICONS_SIZE);
        icon.setFitWidth(Constants.CRC_OPERATOR_ICONS_SIZE);

        VBox iconVBox = new VBox();
        iconVBox.setAlignment(Pos.CENTER_RIGHT);
        iconVBox.getChildren().add(icon);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rootHbox.getChildren().addAll(infoVbox, spacer, iconVBox);

        return rootHbox;
    }

    /**
     * Create a node for a wallet
     * @param wallet The wallet to be displayed
     * @return The HBox containing the wallet information
     */
    private HBox createWalletItemNode(Wallet wallet) {
        HBox rootHbox = new HBox(10);
        rootHbox.getStyleClass().add(Constants.HOME_WALLET_ITEM_STYLE);

        VBox infoVbox = new VBox(10);
        infoVbox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(wallet.getName());
        nameLabel.setMaxWidth(Constants.HOME_ITEM_NODE_NAME_MAX_LENGTH);
        nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        nameLabel.getStyleClass().add(Constants.HOME_WALLET_ITEM_NAME_STYLE);
        UIUtils.addTooltipToNode(
                nameLabel,
                i18nService.tr(Constants.TranslationKeys.HOME_WALLET_TOOLTIP_WALLET_NAME));

        Label walletTypeLabel =
                new Label(UIUtils.translateWalletTypeName(wallet.getType(), i18nService));
        walletTypeLabel.getStyleClass().add(Constants.HOME_WALLET_TYPE_STYLE);
        walletTypeLabel.setAlignment(Pos.TOP_LEFT);
        UIUtils.addTooltipToNode(
                walletTypeLabel,
                i18nService.tr(Constants.TranslationKeys.HOME_WALLET_TOOLTIP_WALLET_TYPE));

        Label balanceLabel = new Label(UIUtils.formatCurrency(wallet.getBalance()));
        balanceLabel.getStyleClass().add(Constants.HOME_WALLET_ITEM_BALANCE_STYLE);
        UIUtils.addTooltipToNode(
                balanceLabel,
                i18nService.tr(Constants.TranslationKeys.HOME_WALLET_TOOLTIP_WALLET_BALANCE));

        if (wallet.isVirtual()) {
            Label virtualWalletLabel =
                    new Label(i18nService.tr(Constants.TranslationKeys.HOME_WALLET_VIRTUAL_WALLET));

            virtualWalletLabel.setAlignment(Pos.BOTTOM_LEFT);
            virtualWalletLabel.getStyleClass().add(Constants.HOME_VIRTUAL_WALLET_INFO_STYLE);

            UIUtils.addTooltipToNode(
                    virtualWalletLabel, UIUtils.getVirtualWalletInfo(wallet, i18nService));

            infoVbox.getChildren()
                    .addAll(nameLabel, walletTypeLabel, balanceLabel, virtualWalletLabel);
        } else {
            infoVbox.getChildren().addAll(nameLabel, walletTypeLabel, balanceLabel);
        }

        ImageView icon =
                new ImageView(Constants.WALLET_TYPE_ICONS_PATH + wallet.getType().getIcon());

        icon.setFitHeight(Constants.WALLET_TYPE_ICONS_SIZE);
        icon.setFitWidth(Constants.WALLET_TYPE_ICONS_SIZE);

        VBox iconVBox = new VBox();
        iconVBox.setAlignment(Pos.CENTER_RIGHT);
        iconVBox.getChildren().add(icon);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rootHbox.getChildren().addAll(infoVbox, spacer, iconVBox);

        return rootHbox;
    }

    /**
     * Create a new bar chart
     */
    private void createMoneyFlowBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        moneyFlowBarChart = new BarChart<>(xAxis, yAxis);

        moneyFlowBarChartAnchorPane.getChildren().clear();

        moneyFlowBarChartAnchorPane.getChildren().add(moneyFlowBarChart);

        AnchorPane.setTopAnchor(moneyFlowBarChart, 0.0);
        AnchorPane.setBottomAnchor(moneyFlowBarChart, 0.0);
        AnchorPane.setLeftAnchor(moneyFlowBarChart, 0.0);
        AnchorPane.setRightAnchor(moneyFlowBarChart, 0.0);
    }
}
