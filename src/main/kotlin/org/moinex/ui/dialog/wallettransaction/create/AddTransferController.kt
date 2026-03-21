/*
 * Filename: AddTransferController.kt (original filename: AddTransferController.java)
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.create

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.wallettransaction.base.BaseTransferManagement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalTime

@Controller
class AddTransferController(
    walletService: WalletService,
    calculatorService: CalculatorService,
    categoryService: CategoryService,
    preferencesService: PreferencesService,
    springContext: ConfigurableApplicationContext,
) : BaseTransferManagement(
        walletService,
        calculatorService,
        categoryService,
        preferencesService,
        springContext,
    ) {
    fun setSenderWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }
        senderWalletComboBox.value = wt
        updateSenderWalletBalance()
    }

    fun setReceiverWalletComboBox(wt: Wallet) {
        if (wallets.none { it.id == wt.id }) {
            return
        }
        receiverWalletComboBox.value = wt
        updateReceiverWalletBalance()
    }

    @FXML
    override fun initialize() {
        super.initialize()
    }

    @FXML
    override fun handleSave() {
        val senderWt = senderWalletComboBox.value
        val receiverWt = receiverWalletComboBox.value
        val transferValueString = transferValueField.text
        val description = descriptionField.text
        val transferDate = transferDatePicker.value
        val category = categoryComboBox.value

        if (senderWt == null ||
            receiverWt == null ||
            transferValueString.isNullOrBlank() ||
            description.isNullOrBlank() ||
            transferDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val transferValue = transferValueString.toBigDecimal()
            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = transferDate.atTime(currentTime)

            walletService.createTransfer(
                Transfer(
                    senderWallet = senderWt,
                    receiverWallet = receiverWt,
                    date = dateTimeWithCurrentHour,
                    amount = transferValue,
                    description = description,
                    category = category,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_MESSAGE),
            )

            (descriptionField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_VALUE_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_VALUE_MESSAGE,
                        ),
                    )
                }
                is MoinexException.SameSourceDestinationException,
                is IllegalArgumentException,
                is EntityNotFoundException,
                is IllegalStateException,
                is MoinexException.InsufficientResourcesException,
                is MoinexException.TransferFromMasterToVirtualWalletException,
                -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_CREATING_TRANSFER_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    override fun updateAfterBalance(
        mainWallet: Wallet?,
        otherWallet: Wallet?,
        afterBalanceLabel: Label,
        isSender: Boolean,
    ) {
        val transferValueString = transferValueField.text

        if (transferValueString.isNullOrBlank() ||
            mainWallet == null ||
            mainWallet == otherWallet
        ) {
            UIUtils.resetLabel(afterBalanceLabel)
            return
        }

        runCatching {
            val transferValue = transferValueString.toBigDecimal()

            if (transferValue < BigDecimal.ZERO) {
                UIUtils.resetLabel(afterBalanceLabel)
                return
            }

            val afterBalance = determineAfterBalance(mainWallet, otherWallet, transferValue, isSender)

            val style =
                if (afterBalance < BigDecimal.ZERO) {
                    Constants.NEGATIVE_BALANCE_STYLE
                } else {
                    Constants.NEUTRAL_BALANCE_STYLE
                }

            UIUtils.setLabelStyle(afterBalanceLabel, style)
            afterBalanceLabel.text = UIUtils.formatCurrency(afterBalance)
        }.onFailure {
            UIUtils.resetLabel(afterBalanceLabel)
        }
    }

    private fun determineAfterBalance(
        mainWallet: Wallet,
        otherWallet: Wallet?,
        transferValue: BigDecimal,
        isSender: Boolean,
    ): BigDecimal {
        return if (isSender) {
            if (mainWallet.isMaster() &&
                otherWallet?.isVirtual() == true &&
                otherWallet.masterWallet == mainWallet
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_TITLE,
                    ),
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_TRANSFER_MESSAGE,
                    ),
                )
                return BigDecimal.ZERO
            }
            mainWallet.balance - transferValue
        } else {
            if (otherWallet?.isVirtual() == true &&
                otherWallet.masterWallet == mainWallet
            ) {
                mainWallet.balance
            } else {
                mainWallet.balance + transferValue
            }
        }
    }
}
