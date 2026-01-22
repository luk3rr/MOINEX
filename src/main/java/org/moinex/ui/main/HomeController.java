/*
 * Filename: HomeController.java
 * Created on: September 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
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
import org.moinex.chart.NetWorthLineChart;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.investment.Ticker;
import org.moinex.model.investment.TickerPurchase;
import org.moinex.model.investment.TickerSale;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.*;
import org.moinex.ui.common.ResumePaneController;
import org.moinex.util.Animation;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
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

    @FXML private AnchorPane graphView;

    @FXML private JFXButton graphPrevButton;

    @FXML private JFXButton graphNextButton;

    @FXML private Label graphTitle;

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

    private TickerService tickerService;

    private BondService bondService;

    private Integer walletPaneCurrentPage = 0;

    private Integer creditCardPaneCurrentPage = 0;

    private Integer graphPaneCurrentPage = 0;

    /**
     * Constructor for injecting the wallet and credit card services
     * @param walletService The wallet service
     * @param walletTransactionService The wallet transaction service
     * @param recurringTransactionService The recurring transaction service
     * @param creditCardService The credit card service
     * @param springContext The spring context
     * @param i18nService The i18n service
     */
    @Autowired
    public HomeController(
            WalletService walletService,
            WalletTransactionService walletTransactionService,
            RecurringTransactionService recurringTransactionService,
            CreditCardService creditCardService,
            TickerService tickerService,
            BondService bondService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.walletService = walletService;
        this.walletTransactionService = walletTransactionService;
        this.recurringTransactionService = recurringTransactionService;
        this.creditCardService = creditCardService;
        this.tickerService = tickerService;
        this.bondService = bondService;
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
        updateDisplayGraphs();

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

        graphPrevButton.setOnAction(
                event -> {
                    if (graphPaneCurrentPage > 0) {
                        graphPaneCurrentPage--;
                        updateDisplayGraphs();
                    }
                });

        graphNextButton.setOnAction(
                event -> {
                    if (graphPaneCurrentPage < 1) {
                        graphPaneCurrentPage++;
                        updateDisplayGraphs();
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

                                    String walletName =
                                            transaction.getWallet() != null
                                                    ? transaction.getWallet().getName()
                                                    : "";
                                    Label walletLabel = new Label(walletName);
                                    walletLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_WALLET_LABEL_WIDTH);

                                    Label dateLabel =
                                            new Label(
                                                    UIUtils.formatDateForDisplay(
                                                            transaction.getDate(), i18nService));
                                    dateLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_DATE_LABEL_WIDTH);

                                    Label transactionStatusLabel =
                                            new Label(
                                                    UIUtils.translateTransactionStatus(
                                                            transaction.getStatus(), i18nService));
                                    transactionStatusLabel.setMinWidth(
                                            Constants.HOME_LAST_TRANSACTIONS_STATUS_LABEL_WIDTH);

                                    String categoryName =
                                            transaction.getCategory() != null
                                                    ? transaction.getCategory().getName()
                                                    : "";
                                    Label transactionCategoryLabel = new Label(categoryName);
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
        DateTimeFormatter formatter = UIUtils.getShortMonthYearFormatter(i18nService.getLocale());

        int totalMonths = Constants.XYBAR_CHART_MONTHS + Constants.XYBAR_CHART_FUTURE_MONTHS;

        // Collect data for the last months and the future months
        for (int i = 0; i < totalMonths; i++) {
            // Get the data from the oldest month to the most recent, to keep the order
            LocalDateTime date = maxMonth.minusMonths(totalMonths - i - 1L);
            Integer month = date.getMonthValue();
            Integer year = date.getYear();

            // Get transactions
            List<WalletTransaction> nonArchivedTransactions =
                    walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            month, year);

            // Get future transactions and merge with the current transactions
            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByMonthForAnalysis(
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

            monthResumePaneTitle.setText(
                    MessageFormat.format(
                            i18nService.tr(Constants.TranslationKeys.HOME_RESUME_TITLE),
                            UIUtils.formatShortMonthYear(currentDate, i18nService)));

            resumePaneController.updateResumePane(
                    currentDate.getMonthValue(), currentDate.getYear());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 0.0);
            AnchorPane.setRightAnchor(newContent, 0.0);

            monthResumeView.getChildren().clear();
            monthResumeView.getChildren().add(newContent);
        } catch (IOException e) {
            logger.error("Error loading resume pane FXML: '{}'", Constants.RESUME_PANE_FXML, e);
            logger.error("Failed to update month resume. Error: '{}'", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Root cause: {}", e.getCause().getMessage(), e.getCause());
            }
        } catch (Exception e) {
            logger.error("Unexpected error updating month resume", e);
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
                new Label(UIUtils.translateWalletType(wallet.getType(), i18nService));
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
    }

    /**
     * Update the display of graphs based on the current page
     */
    private void updateDisplayGraphs() {
        graphView.getChildren().clear();

        if (graphPaneCurrentPage == 0) {
            graphTitle.setText(i18nService.tr(Constants.TranslationKeys.HOME_MONEY_FLOW_TITLE));
            updateMoneyFlowBarChart();
            graphView.getChildren().add(moneyFlowBarChart);
            AnchorPane.setTopAnchor(moneyFlowBarChart, 0.0);
            AnchorPane.setBottomAnchor(moneyFlowBarChart, 0.0);
            AnchorPane.setLeftAnchor(moneyFlowBarChart, 0.0);
            AnchorPane.setRightAnchor(moneyFlowBarChart, 0.0);
        } else if (graphPaneCurrentPage == 1) {
            graphTitle.setText(i18nService.tr(Constants.TranslationKeys.HOME_NET_WORTH_TITLE));
            updateNetWorthLineChart();
        }

        graphPrevButton.setDisable(graphPaneCurrentPage == 0);
        graphNextButton.setDisable(graphPaneCurrentPage >= 1);
    }

    /**
     * Update the net worth line chart with assets, liabilities and net worth
     */
    private void updateNetWorthLineChart() {
        List<NetWorthLineChart.NetWorthDataPoint> dataPoints = calculateNetWorthData();

        NetWorthLineChart netWorthChart = new NetWorthLineChart();
        netWorthChart.setI18nService(i18nService);
        netWorthChart.updateData(dataPoints);

        UIUtils.applyDefaultChartStyle(netWorthChart);

        graphView.getChildren().add(netWorthChart);

        AnchorPane.setTopAnchor(netWorthChart, 0.0);
        AnchorPane.setBottomAnchor(netWorthChart, 0.0);
        AnchorPane.setLeftAnchor(netWorthChart, 0.0);
        AnchorPane.setRightAnchor(netWorthChart, 0.0);
    }

    /**
     * Calculate net worth data for the chart
     * @return List of net worth data points
     */
    private List<NetWorthLineChart.NetWorthDataPoint> calculateNetWorthData() {
        List<NetWorthLineChart.NetWorthDataPoint> dataPoints = new ArrayList<>();

        LocalDateTime maxMonth = LocalDateTime.now().plusMonths(Constants.PL_CHART_FUTURE_MONTHS);

        int totalMonths = Constants.PL_CHART_MONTHS + Constants.PL_CHART_FUTURE_MONTHS;

        for (int i = 0; i < totalMonths; i++) {
            LocalDateTime date = maxMonth.minusMonths(totalMonths - i - 1L);
            Integer month = date.getMonthValue();
            Integer year = date.getYear();
            YearMonth period = YearMonth.of(year, month);

            BigDecimal assets = calculateAssetsForMonth(month, year);
            BigDecimal liabilities = calculateLiabilitiesForMonth(month, year);
            BigDecimal netWorth = assets.subtract(liabilities);

            dataPoints.add(
                    new NetWorthLineChart.NetWorthDataPoint(period, assets, liabilities, netWorth));
        }

        return dataPoints;
    }

    /**
     * Calculate total assets for a given month
     * @param month The month
     * @param year The year
     * @return Total assets
     */
    private BigDecimal calculateAssetsForMonth(Integer month, Integer year) {
        BigDecimal totalAssets = BigDecimal.ZERO;

        BigDecimal walletBalances = calculateWalletBalancesForMonth(month, year);
        totalAssets = totalAssets.add(walletBalances);

        BigDecimal investmentValue = calculateInvestmentValueForMonth(month, year);
        totalAssets = totalAssets.add(investmentValue);

        return totalAssets;
    }

    /**
     * Calculate wallet balances for a given month
     * @param month The month
     * @param year The year
     * @return Total wallet balances
     */
    private BigDecimal calculateWalletBalancesForMonth(Integer month, Integer year) {
        BigDecimal totalBalance = BigDecimal.ZERO;

        YearMonth targetMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();

        if (targetMonth.isAfter(currentMonth)) { // Future projection
            totalBalance =
                    wallets.stream()
                            .map(Wallet::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<WalletTransaction> futureTransactions =
                    recurringTransactionService.getFutureTransactionsByMonthForAnalysis(
                            currentMonth.plusMonths(1), targetMonth);

            BigDecimal futureIncomesTotal =
                    futureTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.INCOME))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.add(futureIncomesTotal);

            BigDecimal futureExpensesTotal =
                    futureTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.subtract(futureExpensesTotal);
        } else if (targetMonth.equals(
                currentMonth)) { // Current month - include pending and scheduled
            totalBalance =
                    wallets.stream()
                            .map(Wallet::getBalance)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Add scheduled recurring transactions for current month
            List<WalletTransaction> scheduledTransactions =
                    recurringTransactionService.getFutureTransactionsByMonthForAnalysis(
                            currentMonth, currentMonth);

            BigDecimal scheduledIncomes =
                    scheduledTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.INCOME))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.add(scheduledIncomes);

            BigDecimal scheduledExpenses =
                    scheduledTransactions.stream()
                            .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.subtract(scheduledExpenses);

            // Add pending transactions for current month
            List<WalletTransaction> currentMonthTransactions =
                    walletTransactionService.getNonArchivedTransactionsByMonthForAnalysis(
                            month, year);

            BigDecimal pendingIncomes =
                    currentMonthTransactions.stream()
                            .filter(
                                    t ->
                                            t.getType().equals(TransactionType.INCOME)
                                                    && t.getStatus()
                                                            .equals(TransactionStatus.PENDING))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.add(pendingIncomes);

            BigDecimal pendingExpenses =
                    currentMonthTransactions.stream()
                            .filter(
                                    t ->
                                            t.getType().equals(TransactionType.EXPENSE)
                                                    && t.getStatus()
                                                            .equals(TransactionStatus.PENDING))
                            .map(WalletTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalBalance = totalBalance.subtract(pendingExpenses);
        } else { // Historical data - calculate retroactively from current balance
            for (Wallet wallet : wallets) {
                BigDecimal walletBalance = wallet.getBalance();

                // Revert wallet transactions after target month
                LocalDateTime startOfNextMonth = targetMonth.plusMonths(1).atDay(1).atStartOfDay();

                List<WalletTransaction> transactionsAfter =
                        walletTransactionService.getTransactionsByWalletAfterDate(
                                wallet.getId(), startOfNextMonth);

                for (WalletTransaction tx : transactionsAfter) {
                    if (tx.getStatus().equals(TransactionStatus.CONFIRMED)) {
                        if (tx.getType().equals(TransactionType.INCOME)) {
                            walletBalance = walletBalance.subtract(tx.getAmount());
                        } else if (tx.getType().equals(TransactionType.EXPENSE)) {
                            walletBalance = walletBalance.add(tx.getAmount());
                        }
                    }
                }

                // Revert credit card payments after target month
                // These payments decrease wallet balance but are not WalletTransactions
                for (int futureMonth = targetMonth.getMonthValue() + 1;
                        futureMonth <= 12;
                        futureMonth++) {
                    BigDecimal payments =
                            creditCardService.getEffectivePaidPaymentsByMonth(
                                    wallet.getId(), futureMonth, targetMonth.getYear());
                    walletBalance = walletBalance.add(payments); // Add back payments
                }

                // For months in next years
                int nextYear = targetMonth.getYear() + 1;
                YearMonth now = YearMonth.now();
                while (nextYear <= now.getYear()) {
                    int maxMonth = (nextYear == now.getYear()) ? now.getMonthValue() : 12;
                    for (int futureMonth = 1; futureMonth <= maxMonth; futureMonth++) {
                        BigDecimal payments =
                                creditCardService.getEffectivePaidPaymentsByMonth(
                                        wallet.getId(), futureMonth, nextYear);
                        walletBalance = walletBalance.add(payments);
                    }
                    nextYear++;
                }

                // Apply wallet transactions in target month
                List<WalletTransaction> transactionsInMonth =
                        walletTransactionService.getTransactionsByWalletAndMonth(
                                wallet.getId(), month, year);

                for (WalletTransaction tx : transactionsInMonth) {
                    if (tx.getStatus().equals(TransactionStatus.CONFIRMED)) {
                        if (tx.getType().equals(TransactionType.INCOME)) {
                            walletBalance = walletBalance.add(tx.getAmount());
                        } else if (tx.getType().equals(TransactionType.EXPENSE)) {
                            walletBalance = walletBalance.subtract(tx.getAmount());
                        }
                    }
                }

                // Apply credit card payments in target month
                BigDecimal paymentsInMonth =
                        creditCardService.getEffectivePaidPaymentsByMonth(
                                wallet.getId(), month, year);
                walletBalance = walletBalance.subtract(paymentsInMonth);

                totalBalance = totalBalance.add(walletBalance);
            }
        }

        return totalBalance;
    }

    /**
     * Calculate investment value for a given month
     * @param month The month
     * @param year The year
     * @return Total investment value
     */
    private BigDecimal calculateInvestmentValueForMonth(Integer month, Integer year) {
        YearMonth targetMonth = YearMonth.of(year, month);

        LocalDateTime endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59);

        BigDecimal tickerValue = calculateTickerValueAtDate(endOfMonth);

        BigDecimal bondValue = calculateBondValueAtDate(endOfMonth);

        return tickerValue.add(bondValue);
    }

    /**
     * Calculate total ticker value at a specific date
     * @param date The date to calculate value for
     * @return Total ticker value
     */
    private BigDecimal calculateTickerValueAtDate(LocalDateTime date) {
        // Get all purchases and sales AFTER the target date
        List<TickerPurchase> allPurchases = tickerService.getAllPurchases();
        List<TickerSale> allSales = tickerService.getAllSales();

        // Calculate quantity changes after target date for each ticker
        Map<Integer, BigDecimal> quantityChangesAfter = new HashMap<>();

        for (TickerPurchase purchase : allPurchases) {
            LocalDateTime purchaseDate = purchase.getWalletTransaction().getDate();
            if (purchaseDate.isAfter(date)) {
                Integer tickerId = purchase.getTicker().getId();
                quantityChangesAfter.merge(tickerId, purchase.getQuantity(), BigDecimal::add);
            }
        }

        for (TickerSale sale : allSales) {
            LocalDateTime saleDate = sale.getWalletTransaction().getDate();
            if (saleDate.isAfter(date)) {
                Integer tickerId = sale.getTicker().getId();
                quantityChangesAfter.merge(tickerId, sale.getQuantity(), BigDecimal::add);
            }
        }

        // Calculate historical quantity: current quantity - changes after date
        List<Ticker> allTickers = tickerService.getAllTickers();
        Map<Integer, BigDecimal> tickerQuantities = new HashMap<>();

        for (Ticker ticker : allTickers) {
            BigDecimal currentQty = ticker.getCurrentQuantity();
            BigDecimal changesAfter =
                    quantityChangesAfter.getOrDefault(ticker.getId(), BigDecimal.ZERO);
            BigDecimal historicalQty = currentQty.subtract(changesAfter);

            if (historicalQty.compareTo(BigDecimal.ZERO) > 0) {
                tickerQuantities.put(ticker.getId(), historicalQty);
            }
        }

        Map<Integer, Ticker> tickerMap =
                allTickers.stream().collect(Collectors.toMap(Ticker::getId, ticker -> ticker));

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> entry : tickerQuantities.entrySet()) {
            Ticker ticker = tickerMap.get(entry.getKey());
            if (ticker != null) {
                BigDecimal quantity = entry.getValue();
                BigDecimal currentPrice = ticker.getCurrentUnitValue();
                BigDecimal value = quantity.multiply(currentPrice);
                totalValue = totalValue.add(value);
            }
        }

        return totalValue;
    }

    /**
     * Calculate total bond value at a specific date
     * @param date The date to calculate value for
     * @return Total bond value
     */
    private BigDecimal calculateBondValueAtDate(LocalDateTime date) {
        List<BondOperation> operations = bondService.getOperationsByDateBefore(date);

        // Calculate quantity held for each bond
        Map<Integer, BigDecimal> bondQuantities = new HashMap<>();
        Map<Integer, BigDecimal> bondPrices = new HashMap<>();

        for (BondOperation operation : operations) {
            Integer bondId = operation.getBond().getId();

            if (operation.getOperationType().equals(OperationType.BUY)) {
                bondQuantities.merge(bondId, operation.getQuantity(), BigDecimal::add);
                bondPrices.put(bondId, operation.getUnitPrice());
            } else if (operation.getOperationType().equals(OperationType.SELL)) {
                bondQuantities.merge(bondId, operation.getQuantity().negate(), BigDecimal::add);
            }
        }

        // Calculate total value using the last known price for each bond
        BigDecimal totalValue = BigDecimal.ZERO;
        for (java.util.Map.Entry<Integer, BigDecimal> entry : bondQuantities.entrySet()) {
            BigDecimal quantity = entry.getValue();
            BigDecimal price = bondPrices.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            totalValue = totalValue.add(quantity.multiply(price));
        }

        return totalValue;
    }

    /**
     * Calculate total liabilities for a given month
     * @param month The month
     * @param year The year
     * @return Total liabilities
     */
    private BigDecimal calculateLiabilitiesForMonth(Integer month, Integer year) {
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        YearMonth targetMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();

        if (targetMonth.isAfter(currentMonth)) {
            // Future: pending invoices (debts not yet paid)
            BigDecimal crcPendingPayments =
                    creditCardService.getPendingPaymentsByMonth(month, year);
            totalLiabilities = totalLiabilities.add(crcPendingPayments);
        } else if (targetMonth.equals(currentMonth)) {
            // Current month: paid invoices + pending invoices
            BigDecimal crcPaidPayments =
                    creditCardService.getEffectivePaidPaymentsByMonth(month, year);
            totalLiabilities = totalLiabilities.add(crcPaidPayments);

            BigDecimal crcPendingPayments =
                    creditCardService.getPendingPaymentsByMonth(month, year);
            totalLiabilities = totalLiabilities.add(crcPendingPayments);
        } else {
            // Historical: paid invoices (debts that existed at that time)
            BigDecimal crcPaidPayments =
                    creditCardService.getEffectivePaidPaymentsByMonth(month, year);
            totalLiabilities = totalLiabilities.add(crcPaidPayments);
        }

        return totalLiabilities;
    }
}
