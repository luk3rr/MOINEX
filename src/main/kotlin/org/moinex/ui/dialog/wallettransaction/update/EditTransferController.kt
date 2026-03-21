/*
 * Filename: EditTransferController.kt (original filename: AddTransferController.java)
 * Created on: October  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.stage.Stage
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.extension.toRounded
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
import java.time.LocalDate
import java.time.LocalTime

@Controller
class EditTransferController(
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
    private var oldTransfer: Transfer? = null

    fun setTransfer(transfer: Transfer) {
        disableTransferValueListener()
        suggestionsHandler.disable()

        val senderWallet = transfer.senderWallet
        val receiverWallet = transfer.receiverWallet
        val value = transfer.amount
        val description = transfer.description
        val transferDate = LocalDate.from(transfer.date)
        val category = transfer.category

        senderWalletComboBox.value = senderWallet
        receiverWalletComboBox.value = receiverWallet
        transferValueField.text = value.toPlainString()
        descriptionField.text = description
        transferDatePicker.value = transferDate
        categoryComboBox.value = category

        senderWalletComboBox.value = senderWallet
        updateSenderWalletBalance()

        receiverWalletComboBox.value = receiverWallet
        updateReceiverWalletBalance()
        enableTransferValueListener()
        suggestionsHandler.enable()

        oldTransfer = transfer
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

            if (oldTransfer!!.senderWallet == senderWt &&
                oldTransfer!!.receiverWallet == receiverWt &&
                oldTransfer!!.amount.isEqual(transferValue) &&
                oldTransfer!!.description == description &&
                oldTransfer!!.category?.id!! == category?.id!! &&
                LocalDate.from(oldTransfer!!.date) == transferDate
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TITLE),
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TRANSFER_MESSAGE,
                    ),
                )
                return
            }

            oldTransfer!!.amount = transferValue
            oldTransfer!!.senderWallet = senderWt
            oldTransfer!!.receiverWallet = receiverWt
            oldTransfer!!.description = description
            oldTransfer!!.category = category
            oldTransfer!!.date = dateTimeWithCurrentHour
            walletService.updateTransfer(oldTransfer!!)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_UPDATED_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_UPDATED_MESSAGE),
            )
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
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_UPDATING_TRANSFER_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }

        (descriptionField.scene.window as Stage).close()
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
            val newAmount = transferValueString.toBigDecimal().toRounded()
            if (newAmount < BigDecimal.ZERO) {
                UIUtils.resetLabel(afterBalanceLabel)
                return
            }

            val afterBalance = calculateAfterBalance(mainWallet, isSender, newAmount, otherWallet)

            val style =
                if (afterBalance < BigDecimal.ZERO) {
                    Styles.NEGATIVE_BALANCE_STYLE
                } else {
                    Styles.NEUTRAL_BALANCE_STYLE
                }

            UIUtils.setLabelStyle(afterBalanceLabel, style)
            afterBalanceLabel.text = UIUtils.formatCurrency(afterBalance)
        }.onFailure {
            UIUtils.resetLabel(afterBalanceLabel)
        }
    }

    private fun calculateAfterBalance(
        mainWallet: Wallet,
        isSender: Boolean,
        newAmount: BigDecimal,
        otherWallet: Wallet?,
    ): BigDecimal {
        val balance = mainWallet.balance
        val oldAmount = oldTransfer!!.amount
        val oldSender = oldTransfer!!.senderWallet
        val oldReceiver = oldTransfer!!.receiverWallet
        val sameWallet =
            (isSender && mainWallet == oldSender) ||
                (!isSender && mainWallet == oldReceiver)

        return if (sameWallet) {
            val diff = newAmount - oldAmount
            if (isSender) {
                balance - diff
            } else {
                balance + diff
            }
        } else {
            if (isSender) {
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
                balance - newAmount
            } else {
                if (otherWallet?.isVirtual() == true &&
                    otherWallet.masterWallet == mainWallet
                ) {
                    balance
                } else {
                    balance + newAmount
                }
            }
        }
    }
}
