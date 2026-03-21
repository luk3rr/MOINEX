/*
 * Filename: EditTransactionController.kt (original filename: EditTransactionController.java)
 * Created on: October 18, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.stage.Stage
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CalculatorService
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.wallettransaction.base.BaseWalletTransactionManagement
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalTime

@Controller
class EditTransactionController(
    walletService: WalletService,
    categoryService: CategoryService,
    calculatorService: CalculatorService,
    preferencesService: PreferencesService,
    springContext: ConfigurableApplicationContext,
) : BaseWalletTransactionManagement(
        walletService,
        categoryService,
        calculatorService,
        preferencesService,
        springContext,
    ) {
    @FXML
    private lateinit var typeComboBox: ComboBox<WalletTransactionType>

    private var walletTransaction: WalletTransaction? = null

    companion object {
        private val logger = LoggerFactory.getLogger(EditTransactionController::class.java)
    }

    fun setTransaction(wt: WalletTransaction) {
        walletTransaction = wt

        disableTransactionValueListener()
        suggestionsHandler.disable()

        typeComboBox.value = wt.type
        walletTransactionType = wt.type
        walletComboBox.value = wt.wallet
        statusComboBox.value = wt.status
        categoryComboBox.value = wt.category
        descriptionField.text = wt.description
        transactionDatePicker.value = wt.date.toLocalDate()
        transactionValueField.text = wt.amount.toString()
        includeInAnalysisCheckBox.isSelected = wt.includeInAnalysis

        enableTransactionValueListener()
        suggestionsHandler.enable()

        UIUtils.updateWalletBalanceLabelStyle(walletComboBox.value, walletCurrentBalanceValueLabel)
        walletAfterBalance()
        loadSuggestionsFromDatabase()
    }

    @FXML
    override fun initialize() {
        super.initialize()

        typeComboBox.setOnAction {
            walletTransactionType = typeComboBox.value
            walletAfterBalance()
            loadSuggestionsFromDatabase()
        }

        typeComboBox.items.setAll(WalletTransactionType.entries)
        UIUtils.configureComboBox(typeComboBox) { UIUtils.translateTransactionType(it) }
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val type = typeComboBox.value
        val description = descriptionField.text.trim()
        val transactionValueString = transactionValueField.text
        val status = statusComboBox.value
        val category = categoryComboBox.value
        val transactionDate = transactionDatePicker.value

        if (wallet == null ||
            type == null ||
            transactionValueString == null ||
            status == null ||
            category == null ||
            transactionDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val transactionValue = transactionValueString.toBigDecimal()
            val currentTime = LocalTime.now()
            val dateTimeWithCurrentHour = transactionDate.atTime(currentTime)

            val includeInAnalysisChanged =
                includeInAnalysisCheckBox.isSelected != walletTransaction!!.includeInAnalysis

            if (wallet.name == walletTransaction!!.wallet.name &&
                category.name == walletTransaction!!.category.name &&
                transactionValue.isEqual(walletTransaction!!.amount) &&
                description == walletTransaction!!.description &&
                status == walletTransaction!!.status &&
                type == walletTransaction!!.type &&
                dateTimeWithCurrentHour.toLocalDate() == walletTransaction!!.date.toLocalDate() &&
                !includeInAnalysisChanged
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_TITLE),
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_CHANGES_MADE_MESSAGE),
                )
            } else {
                walletTransaction!!.wallet = wallet
                walletTransaction!!.category = category
                walletTransaction!!.date = dateTimeWithCurrentHour
                walletTransaction!!.amount = transactionValue
                walletTransaction!!.description = description
                walletTransaction!!.status = status
                walletTransaction!!.type = type
                walletTransaction!!.includeInAnalysis = includeInAnalysisCheckBox.isSelected

                walletService.updateWalletTransaction(walletTransaction!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSACTION_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSACTION_UPDATED_MESSAGE),
                )
            }

            (descriptionField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_TRANSACTION_VALUE_TITLE,
                        ),
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_INVALID_TRANSACTION_VALUE_MESSAGE,
                        ),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException, is IllegalStateException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_UPDATING_TRANSACTION_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                }
                else -> throw e
            }
        }
    }

    override fun walletAfterBalance() {
        val transactionValueString = transactionValueField.text
        val currentType = typeComboBox.value
        val wallet = walletComboBox.value

        if (transactionValueString.isNullOrBlank() || wallet == null || currentType == null) {
            logger.warn(
                "Some fields are null: transactionValueString={}, currentType={}, wallet={}",
                transactionValueString,
                currentType,
                wallet,
            )
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
            return
        }

        runCatching {
            val newAmount = BigDecimal(transactionValueString)

            if (newAmount < BigDecimal.ZERO) {
                logger.warn("After balance calculation with negative amount")
                UIUtils.resetLabel(walletAfterBalanceValueLabel)
                return
            }

            val oldAmount = walletTransaction!!.amount
            val diff = (newAmount - oldAmount).abs()
            val balance = wallet.balance
            val oldType = walletTransaction!!.type
            val oldStatus = walletTransaction!!.status
            val oldWallet = walletTransaction!!.wallet

            val walletAfterBalanceValue =
                if (wallet == oldWallet) {
                    if (oldStatus == WalletTransactionStatus.CONFIRMED) {
                        when (oldType) {
                            WalletTransactionType.EXPENSE -> {
                                when (currentType) {
                                    WalletTransactionType.EXPENSE -> {
                                        if (oldAmount > newAmount) {
                                            balance + diff
                                        } else {
                                            balance - diff
                                        }
                                    }
                                    WalletTransactionType.INCOME -> balance + oldAmount + newAmount
                                }
                            }
                            WalletTransactionType.INCOME -> {
                                when (currentType) {
                                    WalletTransactionType.INCOME -> {
                                        if (oldAmount > newAmount) {
                                            balance - diff
                                        } else {
                                            balance + diff
                                        }
                                    }
                                    WalletTransactionType.EXPENSE -> balance - oldAmount - newAmount
                                }
                            }
                        }
                    } else {
                        when (currentType) {
                            WalletTransactionType.EXPENSE -> balance - newAmount
                            WalletTransactionType.INCOME -> balance + newAmount
                        }
                    }
                } else {
                    when (currentType) {
                        WalletTransactionType.EXPENSE -> balance - newAmount
                        WalletTransactionType.INCOME -> balance + newAmount
                    }
                }

            val style =
                if (walletAfterBalanceValue < BigDecimal.ZERO) {
                    Styles.NEGATIVE_BALANCE_STYLE
                } else {
                    Styles.NEUTRAL_BALANCE_STYLE
                }

            UIUtils.setLabelStyle(walletAfterBalanceValueLabel, style)
            walletAfterBalanceValueLabel.text = UIUtils.formatCurrency(walletAfterBalanceValue)
        }.onFailure { e ->
            logger.error("Invalid transaction value: {}", transactionValueString, e)
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
        }
    }

    override fun loadSuggestionsFromDatabase() {
        val type = typeComboBox.value
        if (type == null) {
            logger.warn("Type not selected. Suggestions will not be loaded.")
            suggestionsHandler.suggestions = emptyList()
            return
        }

        suggestionsHandler.suggestions = walletService.getWalletTransactionSuggestionsByType(type)
    }
}
