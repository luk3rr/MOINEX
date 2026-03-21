/*
 * Filename: WalletPaneController.kt (original filename: WalletPaneController.java)
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.VBox
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isConfirmed
import org.moinex.common.extension.isExpense
import org.moinex.common.extension.isIncome
import org.moinex.common.extension.isPending
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.wallettransaction.create.AddExpenseController
import org.moinex.ui.dialog.wallettransaction.create.AddIncomeController
import org.moinex.ui.dialog.wallettransaction.create.AddTransferController
import org.moinex.ui.dialog.wallettransaction.update.ChangeWalletBalanceController
import org.moinex.ui.dialog.wallettransaction.update.ChangeWalletTypeController
import org.moinex.ui.dialog.wallettransaction.update.RenameWalletController
import org.moinex.ui.main.WalletController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate
import java.time.YearMonth

@Controller
@Scope("prototype")
class WalletPaneController(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val springContext: ConfigurableApplicationContext,
    private val walletController: WalletController,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var rootVBox: VBox

    @FXML
    private lateinit var walletIcon: ImageView

    @FXML
    private lateinit var walletName: Label

    @FXML
    private lateinit var walletType: Label

    @FXML
    private lateinit var virtualWalletInfo: Label

    @FXML
    private lateinit var openingBalanceSign: Label

    @FXML
    private lateinit var openingBalanceValue: Label

    @FXML
    private lateinit var incomesValue: Label

    @FXML
    private lateinit var incomesSign: Label

    @FXML
    private lateinit var expensesSign: Label

    @FXML
    private lateinit var expensesValue: Label

    @FXML
    private lateinit var creditedTransfersSign: Label

    @FXML
    private lateinit var creditedTransfersValue: Label

    @FXML
    private lateinit var debitedTransfersSign: Label

    @FXML
    private lateinit var debitedTransfersValue: Label

    @FXML
    private lateinit var currentBalanceSign: Label

    @FXML
    private lateinit var currentBalanceValue: Label

    @FXML
    private lateinit var foreseenBalanceSign: Label

    @FXML
    private lateinit var foreseenBalanceValue: Label

    @FXML
    private lateinit var menuButton: MenuButton

    @FXML
    private lateinit var changeWalletTypeMenuItem: MenuItem

    private var crcPaidAmount = BigDecimal.ZERO
    private var crcPendingAmount = BigDecimal.ZERO
    private var transactions = mutableListOf<WalletTransaction>()
    private var transfers = mutableListOf<Transfer>()

    private lateinit var wallet: Wallet

    companion object {
        private const val NEGATIVE_SIGN = "-"
        private const val DEFAULT_SIGN = " "
    }

    @FXML
    private fun initialize() {
        // Still empty
    }

    fun loadWalletInfo() {
        wallet = walletService.getWalletById(wallet.id!!)

        val now = LocalDate.now()
        val yearMonth = YearMonth.of(now.year, now.monthValue)

        transactions =
            walletService
                .getAllNonArchivedWalletTransactionsByWalletAndMonth(wallet.id!!, yearMonth)
                .toMutableList()

        transfers =
            walletService
                .getTransfersByWalletAndMonth(wallet.id!!, yearMonth)
                .toMutableList()

        crcPaidAmount =
            creditCardService.getTotalEffectivePaidPaymentsByWalletAndMonth(
                wallet.id!!,
                yearMonth,
            )

        val payments = creditCardService.getPaymentsByMonth(yearMonth)

        crcPendingAmount =
            payments
                .filter {
                    it.creditCardDebt.creditCard.defaultBillingWallet
                        ?.id!! == wallet.id!!
                }.filter { !it.isPaid() && !it.isRefunded() }
                .sumOf { it.amount }
    }

    fun updateWalletPane(wt: Wallet): VBox {
        wallet = wt
        loadWalletInfo()

        val wallet = wallet

        setupDynamicVisibility()

        walletName.text = wallet.name
        walletType.text = UIUtils.translateWalletType(wallet.type)
        walletIcon.image = Image(Files.WALLET_TYPE_ICONS_PATH + wallet.type.icon)

        val confirmedIncomesSum =
            transactions
                .filter { it.isIncome() }
                .filter { it.isConfirmed() }
                .sumOf { it.amount }

        val pendingIncomesSum =
            transactions
                .filter { it.isIncome() }
                .filter { it.isPending() }
                .sumOf { it.amount }

        val confirmedExpensesSum =
            transactions
                .filter { it.isExpense() }
                .filter { it.isConfirmed() }
                .sumOf { it.amount } + crcPaidAmount

        val pendingExpensesSum =
            transactions
                .filter { it.isExpense() }
                .filter { it.isPending() }
                .sumOf { it.amount } + crcPendingAmount

        val creditedTransfersSum =
            transfers
                .filter { it.receiverWallet.id!! == wallet.id!! }
                .sumOf { it.amount }

        val debitedTransfersSum =
            transfers
                .filter { it.senderWallet.id!! == wallet.id!! }
                .sumOf { it.amount }

        val openingBalance =
            wallet.balance - confirmedIncomesSum + confirmedExpensesSum -
                creditedTransfersSum + debitedTransfersSum

        val foreseenBalance = wallet.balance + pendingIncomesSum - pendingExpensesSum

        setLabelValue(openingBalanceSign, openingBalanceValue, openingBalance)
        setLabelValue(incomesSign, incomesValue, confirmedIncomesSum)
        setLabelValue(expensesSign, expensesValue, confirmedExpensesSum)
        setLabelValue(creditedTransfersSign, creditedTransfersValue, creditedTransfersSum)
        setLabelValue(debitedTransfersSign, debitedTransfersValue, debitedTransfersSum)
        setLabelValue(currentBalanceSign, currentBalanceValue, wallet.balance)
        setLabelValue(foreseenBalanceSign, foreseenBalanceValue, foreseenBalance)

        if (wallet.type.name == Constants.GOAL_DEFAULT_WALLET_TYPE_NAME) {
            menuButton.items.remove(changeWalletTypeMenuItem)
        }

        return rootVBox
    }

    @FXML
    private fun handleAddIncome() {
        WindowUtils.openModalWindow(
            Files.ADD_INCOME_FXML,
            preferencesService.translate(TranslationKeys.COMMON_WALLET_MODAL_ADD_INCOME),
            springContext,
            { controller: AddIncomeController -> controller.setWalletComboBox(wallet) },
            listOf(Runnable { walletController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleAddExpense() {
        WindowUtils.openModalWindow(
            Files.ADD_EXPENSE_FXML,
            preferencesService.translate(TranslationKeys.COMMON_WALLET_MODAL_ADD_EXPENSE),
            springContext,
            { controller: AddExpenseController -> controller.setWalletComboBox(wallet) },
            listOf(Runnable { walletController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleAddTransfer() {
        WindowUtils.openModalWindow(
            Files.ADD_TRANSFER_FXML,
            preferencesService.translate(TranslationKeys.COMMON_WALLET_MODAL_ADD_TRANSFER),
            springContext,
            { controller: AddTransferController -> controller.setSenderWalletComboBox(wallet) },
            listOf(Runnable { walletController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleRenameWallet() {
        WindowUtils.openModalWindow(
            Files.RENAME_WALLET_FXML,
            preferencesService.translate(TranslationKeys.COMMON_WALLET_MODAL_RENAME),
            springContext,
            { controller: RenameWalletController -> controller.setWalletComboBox(wallet) },
            listOf(Runnable { walletController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleChangeWalletType() {
        WindowUtils.openModalWindow(
            Files.CHANGE_WALLET_TYPE_FXML,
            preferencesService.translate(TranslationKeys.COMMON_WALLET_MODAL_CHANGE_TYPE),
            springContext,
            { controller: ChangeWalletTypeController -> controller.setWalletComboBox(wallet) },
            listOf(Runnable { walletController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleChangeWalletBalance() {
        WindowUtils.openModalWindow(
            Files.CHANGE_WALLET_BALANCE_FXML,
            preferencesService.translate(TranslationKeys.COMMON_WALLET_MODAL_CHANGE_BALANCE),
            springContext,
            { controller: ChangeWalletBalanceController -> controller.setWalletComboBox(wallet) },
            listOf(Runnable { walletController.updateDisplay() }),
        )
    }

    @FXML
    private fun handleArchiveWallet() {
        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_ARCHIVE_TITLE),
                    wallet.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_ARCHIVE_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            walletService.archiveWallet(wallet.id!!)
            walletController.updateDisplay()
        }
    }

    @FXML
    private fun handleDeleteWallet() {
        if (walletService.getWalletTransactionAndTransferCountByWallet(wallet.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_HAS_TRANSACTIONS_TITLE),
                preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_HAS_TRANSACTIONS_MESSAGE),
            )
            return
        }

        val totalOfAssociatedVirtualWallets =
            walletService.getCountOfVirtualWalletsByMasterWalletId(wallet.id!!)

        val virtualWalletsMessage =
            if (totalOfAssociatedVirtualWallets == 0) {
                ""
            } else {
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_VIRTUAL_WALLETS),
                    totalOfAssociatedVirtualWallets,
                )
            }

        if (WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_TITLE),
                    wallet.name,
                ),
                preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_MESSAGE) +
                    "\n" + virtualWalletsMessage,
                preferencesService.bundle,
            )
        ) {
            runCatching {
                walletService.deleteWallet(wallet.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_SUCCESS_TITLE),
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_SUCCESS_MESSAGE),
                        wallet.name,
                    ),
                )

                walletController.updateDisplay()
            }.onFailure { e ->
                when (e) {
                    is EntityNotFoundException, is IllegalStateException ->
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.COMMON_WALLET_DIALOG_DELETE_ERROR),
                            e.message ?: "",
                        )
                    else -> throw e
                }
            }
        }
    }

    private fun setLabelValue(
        signLabel: Label,
        valueLabel: Label,
        value: BigDecimal,
    ) {
        if (value < BigDecimal.ZERO) {
            signLabel.text = NEGATIVE_SIGN
            valueLabel.text = UIUtils.formatCurrency(value.abs())
            UIUtils.setLabelStyle(signLabel, Styles.NEGATIVE_BALANCE_STYLE)
            UIUtils.setLabelStyle(valueLabel, Styles.NEGATIVE_BALANCE_STYLE)
        } else {
            signLabel.text = DEFAULT_SIGN
            valueLabel.text = UIUtils.formatCurrency(value)
            UIUtils.setLabelStyle(signLabel, Styles.NEUTRAL_BALANCE_STYLE)
            UIUtils.setLabelStyle(valueLabel, Styles.NEUTRAL_BALANCE_STYLE)
        }
    }

    private fun setupDynamicVisibility() {
        if (wallet.isMaster()) {
            virtualWalletInfo.apply {
                isVisible = false
                isManaged = false
            }
            return
        }

        virtualWalletInfo.apply {
            isVisible = true
            isManaged = true
            text = UIUtils.getVirtualWalletInfo(wallet)
        }
    }
}
