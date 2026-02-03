/*
 * Filename: WalletFullPaneController.java
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.service.CreditCardService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.service.WalletTransactionService;
import org.moinex.ui.dialog.wallettransaction.*;
import org.moinex.ui.main.WalletController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Wallet Full Pane
 *
 * @note prototype is necessary so that each scene knows to which wallet it belongs
 */
@Controller
@Scope("prototype") // Each instance of this controller is unique
@NoArgsConstructor
public class WalletFullPaneController {
    @FXML private VBox rootVBox;

    @FXML private ImageView walletIcon;

    @FXML private Label walletName;

    @FXML private Label walletType;

    @FXML private Label virtualWalletInfo;

    @FXML private Label openingBalanceSign;

    @FXML private Label openingBalanceValue;

    @FXML private Label incomesValue;

    @FXML private Label incomesSign;

    @FXML private Label expensesSign;

    @FXML private Label expensesValue;

    @FXML private Label creditedTransfersSign;

    @FXML private Label creditedTransfersValue;

    @FXML private Label debitedTransfersSign;

    @FXML private Label debitedTransfersValue;

    @FXML private Label currentBalanceSign;

    @FXML private Label currentBalanceValue;

    @FXML private Label foreseenBalanceSign;

    @FXML private Label foreseenBalanceValue;

    @FXML private MenuButton menuButton;

    @FXML private MenuItem changeWalletTypeMenuItem;

    private I18nService i18nService;

    private ConfigurableApplicationContext springContext;

    private WalletController walletController;

    private WalletService walletService;

    private CreditCardService creditCardService;

    private WalletTransactionService walletTransactionService;

    private Wallet wallet;

    private BigDecimal crcPaidAmount;

    private BigDecimal crcPendingAmount;

    private List<WalletTransaction> transactions;

    private List<Transfer> transfers;

    /**
     * Constructor
     *
     * @param walletService            WalletService
     * @param creditCardService        CreditCardService
     * @param walletTransactionService WalletTransactionService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public WalletFullPaneController(
            WalletService walletService,
            CreditCardService creditCardService,
            WalletTransactionService walletTransactionService,
            ConfigurableApplicationContext springContext,
            WalletController walletController,
            I18nService i18nService) {
        this.walletService = walletService;
        this.creditCardService = creditCardService;
        this.walletTransactionService = walletTransactionService;
        this.springContext = springContext;
        this.walletController = walletController;
        this.i18nService = i18nService;
    }

    /**
     * Load wallet information from the database
     */
    public void loadWalletInfo() {
        if (wallet == null) {
            transactions.clear();
            transfers.clear();
            return;
        }

        // Reload wallet from the database
        wallet = walletService.getWalletById(wallet.getId());

        LocalDate now = LocalDate.now();

        transactions =
                walletTransactionService.getNonArchivedTransactionsByWalletAndMonth(
                        wallet.getId(), now.getMonthValue(), now.getYear());

        transfers =
                walletTransactionService.getTransfersByWalletAndMonth(
                        wallet.getId(), now.getMonthValue(), now.getYear());

        crcPaidAmount =
                creditCardService.getEffectivePaidPaymentsByMonth(
                        wallet.getId(), now.getMonthValue(), now.getYear());

        List<CreditCardPayment> payments =
                creditCardService.getCreditCardPayments(now.getMonthValue(), now.getYear());

        // Filter payments that are related to the wallet and are not paid
        crcPendingAmount =
                payments.stream()
                        .filter(
                                p ->
                                        p.getCreditCardDebt()
                                                .getCreditCard()
                                                .getDefaultBillingWallet()
                                                .getId()
                                                .equals(wallet.getId()))
                        .filter(p -> !p.isPaid() && !p.isRefunded())
                        .map(CreditCardPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Load wallet information
     *
     * @param wt Wallet name to find in the database
     * @return The updated VBox
     */
    public VBox updateWalletPane(Wallet wt) {
        // If the wallet is null, do not update the pane
        if (wt == null) {
            setDefaultValues();
            return rootVBox;
        }

        wallet = wt;
        loadWalletInfo();

        setupDynamicVisibility();

        walletName.setText(wallet.getName());
        walletType.setText(UIUtils.translateWalletType(wallet.getType(), i18nService));
        walletIcon.setImage(
                new Image(Constants.WALLET_TYPE_ICONS_PATH + wallet.getType().getIcon()));

        BigDecimal confirmedIncomesSum =
                transactions.stream()
                        .filter(t -> t.getType().equals(TransactionType.INCOME))
                        .filter(t -> t.getStatus().equals(TransactionStatus.CONFIRMED))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingIncomesSum =
                transactions.stream()
                        .filter(t -> t.getType().equals(TransactionType.INCOME))
                        .filter(t -> t.getStatus().equals(TransactionStatus.PENDING))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal confirmedExpensesSum =
                transactions.stream()
                        .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                        .filter(t -> t.getStatus().equals(TransactionStatus.CONFIRMED))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the paid amount of the credit card
        confirmedExpensesSum = confirmedExpensesSum.add(crcPaidAmount);

        BigDecimal pendingExpensesSum =
                transactions.stream()
                        .filter(t -> t.getType().equals(TransactionType.EXPENSE))
                        .filter(t -> t.getStatus().equals(TransactionStatus.PENDING))
                        .map(WalletTransaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Consider the pending amount of the credit card
        pendingExpensesSum = pendingExpensesSum.add(crcPendingAmount);

        BigDecimal creditedTransfersSum =
                transfers.stream()
                        .filter(t -> t.getReceiverWallet().getId().equals(wallet.getId()))
                        .map(Transfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debitedTransfersSum =
                transfers.stream()
                        .filter(t -> t.getSenderWallet().getId().equals(wallet.getId()))
                        .map(Transfer::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal openingBalance =
                wallet.getBalance()
                        .subtract(confirmedIncomesSum)
                        .add(confirmedExpensesSum)
                        .subtract(creditedTransfersSum)
                        .add(debitedTransfersSum);

        BigDecimal foreseenBalance =
                wallet.getBalance().add(pendingIncomesSum).subtract(pendingExpensesSum);

        setLabelValue(openingBalanceSign, openingBalanceValue, openingBalance);
        setLabelValue(incomesSign, incomesValue, confirmedIncomesSum);
        setLabelValue(expensesSign, expensesValue, confirmedExpensesSum);
        setLabelValue(creditedTransfersSign, creditedTransfersValue, creditedTransfersSum);
        setLabelValue(debitedTransfersSign, debitedTransfersValue, debitedTransfersSum);
        setLabelValue(currentBalanceSign, currentBalanceValue, wallet.getBalance());
        setLabelValue(foreseenBalanceSign, foreseenBalanceValue, foreseenBalance);

        // If the wallet type is Goal, remove the option to change the wallet type to
        // prevent changing the wallet type of a Goal wallet
        if (wallet.getType().getName().equals(Constants.GOAL_DEFAULT_WALLET_TYPE_NAME)) {
            menuButton.getItems().remove(changeWalletTypeMenuItem);
        }

        return rootVBox;
    }

    @FXML
    private void initialize() {
        // Still empty
    }

    @FXML
    private void handleAddIncome() {
        WindowUtils.openModalWindow(
                Constants.ADD_INCOME_FXML,
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_MODAL_ADD_INCOME),
                springContext,
                (AddIncomeController controller) -> controller.setWalletComboBox(wallet),
                List.of(() -> walletController.updateDisplay()));
    }

    @FXML
    private void handleAddExpense() {
        WindowUtils.openModalWindow(
                Constants.ADD_EXPENSE_FXML,
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_MODAL_ADD_EXPENSE),
                springContext,
                (AddExpenseController controller) -> controller.setWalletComboBox(wallet),
                List.of(() -> walletController.updateDisplay()));
    }

    @FXML
    private void handleAddTransfer() {
        WindowUtils.openModalWindow(
                Constants.ADD_TRANSFER_FXML,
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_MODAL_ADD_TRANSFER),
                springContext,
                (AddTransferController controller) -> controller.setSenderWalletComboBox(wallet),
                List.of(() -> walletController.updateDisplay()));
    }

    @FXML
    private void handleRenameWallet() {
        WindowUtils.openModalWindow(
                Constants.RENAME_WALLET_FXML,
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_MODAL_RENAME),
                springContext,
                (RenameWalletController controller) -> controller.setWalletComboBox(wallet),
                List.of(() -> walletController.updateDisplay()));
    }

    @FXML
    private void handleChangeWalletType() {
        WindowUtils.openModalWindow(
                Constants.CHANGE_WALLET_TYPE_FXML,
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_MODAL_CHANGE_TYPE),
                springContext,
                (ChangeWalletTypeController controller) -> controller.setWalletComboBox(wallet),
                List.of(() -> walletController.updateDisplay()));
    }

    @FXML
    private void handleChangeWalletBalance() {
        WindowUtils.openModalWindow(
                Constants.CHANGE_WALLET_BALANCE_FXML,
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_MODAL_CHANGE_BALANCE),
                springContext,
                (ChangeWalletBalanceController controller) -> controller.setWalletComboBox(wallet),
                List.of(() -> walletController.updateDisplay()));
    }

    @FXML
    private void handleArchiveWallet() {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        i18nService.tr(
                                Constants.TranslationKeys.COMMON_WALLET_DIALOG_ARCHIVE_TITLE),
                        wallet.getName()),
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_DIALOG_ARCHIVE_MESSAGE),
                i18nService.getBundle())) {
            walletService.archiveWallet(wallet.getId());

            // Update wallet display in the main window
            walletController.updateDisplay();
        }
    }

    @FXML
    private void handleDeleteWallet() {
        // Prevent the removal of a wallet with associated transactions
        if (walletTransactionService.getTransactionCountByWallet(wallet.getId()) > 0) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .COMMON_WALLET_DIALOG_DELETE_HAS_TRANSACTIONS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .COMMON_WALLET_DIALOG_DELETE_HAS_TRANSACTIONS_MESSAGE));
            return;
        }

        Integer totalOfAssociatedVirtualWallets =
                walletService.getCountOfVirtualWalletsByMasterWalletId(wallet.getId());

        String virtualWalletsMessage =
                totalOfAssociatedVirtualWallets.equals(0)
                        ? ""
                        : MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .COMMON_WALLET_DIALOG_DELETE_VIRTUAL_WALLETS),
                                totalOfAssociatedVirtualWallets);

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                        i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_DIALOG_DELETE_TITLE),
                        wallet.getName()),
                i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_DIALOG_DELETE_MESSAGE)
                        + "\n"
                        + virtualWalletsMessage,
                i18nService.getBundle())) {
            try {
                walletService.deleteWallet(wallet.getId());

                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .COMMON_WALLET_DIALOG_DELETE_SUCCESS_TITLE),
                        MessageFormat.format(
                                i18nService.tr(
                                        Constants.TranslationKeys
                                                .COMMON_WALLET_DIALOG_DELETE_SUCCESS_MESSAGE),
                                wallet.getName()));

                // Update wallet display in the main window
                walletController.updateDisplay();
            } catch (EntityNotFoundException | IllegalStateException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(Constants.TranslationKeys.COMMON_WALLET_DIALOG_DELETE_ERROR),
                        e.getMessage());
            }
        }
    }

    private void setDefaultValues() {
        walletName.setText("");
        walletType.setText("");
        walletIcon.setImage(null);

        setLabelValue(openingBalanceSign, openingBalanceValue, BigDecimal.ZERO);
        setLabelValue(incomesSign, incomesValue, BigDecimal.ZERO);
        setLabelValue(expensesSign, expensesValue, BigDecimal.ZERO);
        setLabelValue(creditedTransfersSign, creditedTransfersValue, BigDecimal.ZERO);
        setLabelValue(debitedTransfersSign, debitedTransfersValue, BigDecimal.ZERO);
        setLabelValue(currentBalanceSign, currentBalanceValue, BigDecimal.ZERO);
        setLabelValue(foreseenBalanceSign, foreseenBalanceValue, BigDecimal.ZERO);
    }

    /**
     * Set the value of a label
     *
     * @param signLabel  Label to set the sign
     * @param valueLabel Label to set the value
     * @param value      Value to set
     */
    private void setLabelValue(Label signLabel, Label valueLabel, BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            signLabel.setText("-");
            valueLabel.setText(UIUtils.formatCurrency(value.abs()));
            UIUtils.setLabelStyle(signLabel, Constants.NEGATIVE_BALANCE_STYLE);
            UIUtils.setLabelStyle(valueLabel, Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            signLabel.setText(" ");
            valueLabel.setText(UIUtils.formatCurrency(value));
            UIUtils.setLabelStyle(signLabel, Constants.NEUTRAL_BALANCE_STYLE);
            UIUtils.setLabelStyle(valueLabel, Constants.NEUTRAL_BALANCE_STYLE);
        }
    }

    private void setupDynamicVisibility() {
        if (wallet.isMaster()) {
            virtualWalletInfo.setVisible(false);
            virtualWalletInfo.setManaged(false);
            return;
        }

        virtualWalletInfo.setVisible(true);
        virtualWalletInfo.setManaged(true);

        virtualWalletInfo.setText(UIUtils.getVirtualWalletInfo(wallet, i18nService));
    }
}
